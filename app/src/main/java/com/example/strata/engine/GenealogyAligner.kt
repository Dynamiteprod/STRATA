package com.example.strata.engine

import com.example.strata.domain.AlignmentPoint
import com.example.strata.domain.BeatPoint
import com.example.strata.domain.TrackAnalysis
import com.example.strata.domain.VersionLinkType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Beat-synchronous Dynamic Time Warping & Genealogy alignment engine.
 * Implements Section 6.2 (Features, DTW, 12-transposition search, subsequence DTW,
 * monotone smoothing) and Section 6.4 (downbeat-aligned transition calculation).
 */
object GenealogyAligner {

    data class BeatFeature(
        val timeSec: Double,
        val chroma: FloatArray, // 12 pitch classes (C..B)
        val onsetEnergy: Float
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BeatFeature) return false
            return timeSec == other.timeSec && chroma.contentEquals(other.chroma) && onsetEnergy == other.onsetEnergy
        }

        override fun hashCode(): Int {
            var result = timeSec.hashCode()
            result = 31 * result + chroma.contentHashCode()
            result = 31 * result + onsetEnergy.hashCode()
            return result
        }
    }

    data class AlignmentResult(
        val mapping: List<AlignmentPoint>,
        val confidence: Double,
        val coverage: Double,
        val bestTranspositionSemitones: Int
    )

    data class ScheduledTransition(
        val barTriggerTimeA: Double,
        val targetStartTimeB: Double,
        val crossfadeDurationSec: Double,
        val isFallbackToSampleStart: Boolean
    )

    /**
     * Extract beat-synchronous chroma + onset features from TrackAnalysis.
     */
    fun extractBeatFeatures(analysis: TrackAnalysis): List<BeatFeature> {
        return analysis.beats.map { beat ->
            val chord = analysis.chords.firstOrNull { beat.tSec >= it.startSec && beat.tSec < it.endSec }
            val chroma = chordLabelToChroma(chord?.label ?: analysis.key.display())
            val onset = if (beat.beatInBar == 1) 1.0f else 0.55f
            BeatFeature(timeSec = beat.tSec, chroma = chroma, onsetEnergy = onset)
        }
    }

    /**
     * Compute alignment mapping between source A and target B.
     * Supports standard DTW, 12-transposition search for covers, and subsequence DTW for samples.
     */
    fun align(
        source: List<BeatFeature>,
        target: List<BeatFeature>,
        linkType: VersionLinkType
    ): AlignmentResult {
        if (source.isEmpty() || target.isEmpty()) {
            return AlignmentResult(emptyList(), 0.0, 0.0, 0)
        }

        val shiftsToTest = if (linkType == VersionLinkType.COVER_OF || linkType == VersionLinkType.INTERPOLATION_OF) {
            0 until 12
        } else {
            0..0
        }

        var bestResult: AlignmentResult? = null
        for (shift in shiftsToTest) {
            val shiftedTarget = if (shift == 0) target else target.map {
                it.copy(chroma = transposeChroma(it.chroma, shift))
            }
            val candidate = if (linkType == VersionLinkType.SAMPLE_OF) {
                subsequenceDtw(source, shiftedTarget, shift)
            } else {
                globalDtw(source, shiftedTarget, shift)
            }
            if (bestResult == null || candidate.confidence > bestResult.confidence) {
                bestResult = candidate
            }
        }
        return bestResult ?: AlignmentResult(emptyList(), 0.0, 0.0, 0)
    }

    private fun globalDtw(
        source: List<BeatFeature>,
        target: List<BeatFeature>,
        shift: Int
    ): AlignmentResult {
        val n = source.size
        val m = target.size
        val dp = Array(n + 1) { FloatArray(m + 1) { Float.POSITIVE_INFINITY } }
        dp[0][0] = 0f

        for (i in 1..n) {
            for (j in 1..m) {
                val cost = featureDistance(source[i - 1], target[j - 1])
                val bestPrev = min(dp[i - 1][j - 1], min(dp[i - 1][j], dp[i][j - 1]))
                dp[i][j] = cost + bestPrev
            }
        }

        val path = mutableListOf<Pair<Int, Int>>()
        var i = n
        var j = m
        while (i > 0 && j > 0) {
            path.add((i - 1) to (j - 1))
            val diag = dp[i - 1][j - 1]
            val up = dp[i - 1][j]
            val left = dp[i][j - 1]
            when {
                diag <= up && diag <= left -> {
                    i--; j--
                }
                up <= left -> i--
                else -> j--
            }
        }
        path.reverse()

        val rawPoints = smoothMonotone(path.map { (si, ti) ->
            AlignmentPoint(source[si].timeSec, target[ti].timeSec)
        })
        val avgCost = dp[n][m] / max(1, path.size)
        val confidence = exp(-avgCost.toDouble() * 1.4).coerceIn(0.0, 1.0)
        return AlignmentResult(
            mapping = rawPoints,
            confidence = confidence,
            coverage = 1.0,
            bestTranspositionSemitones = shift
        )
    }

    private fun subsequenceDtw(
        source: List<BeatFeature>,
        target: List<BeatFeature>,
        shift: Int
    ): AlignmentResult {
        // Subsequence matching: segment of source aligns to full target sample (or vice versa)
        val n = source.size
        val m = target.size
        if (m >= n) {
            // Find where source matches inside longer target
            val dp = Array(n + 1) { FloatArray(m + 1) { Float.POSITIVE_INFINITY } }
            for (j in 0..m) dp[0][j] = 0f
            for (i in 1..n) {
                for (j in 1..m) {
                    val cost = featureDistance(source[i - 1], target[j - 1])
                    dp[i][j] = cost + min(dp[i - 1][j - 1], min(dp[i - 1][j], dp[i][j - 1]))
                }
            }
            var bestEndJ = 1
            var bestCost = Float.POSITIVE_INFINITY
            for (j in 1..m) {
                if (dp[n][j] < bestCost) {
                    bestCost = dp[n][j]
                    bestEndJ = j
                }
            }
            val path = mutableListOf<Pair<Int, Int>>()
            var i = n
            var j = bestEndJ
            while (i > 0 && j > 0) {
                path.add((i - 1) to (j - 1))
                val diag = dp[i - 1][j - 1]
                val up = dp[i - 1][j]
                val left = dp[i][j - 1]
                when {
                    diag <= up && diag <= left -> { i--; j-- }
                    up <= left -> i--
                    else -> j--
                }
            }
            path.reverse()
            val points = smoothMonotone(path.map { (si, ti) ->
                AlignmentPoint(source[si].timeSec, target[ti].timeSec)
            })
            val avgCost = bestCost / max(1, path.size)
            val confidence = exp(-avgCost.toDouble() * 1.4).coerceIn(0.0, 1.0)
            return AlignmentResult(points, confidence, 1.0, shift)
        } else {
            // Target is shorter sample; find best matching segment inside source
            val dp = Array(m + 1) { FloatArray(n + 1) { Float.POSITIVE_INFINITY } }
            for (i in 0..n) dp[0][i] = 0f
            for (j in 1..m) {
                for (i in 1..n) {
                    val cost = featureDistance(target[j - 1], source[i - 1])
                    dp[j][i] = cost + min(dp[j - 1][i - 1], min(dp[j - 1][i], dp[j][i - 1]))
                }
            }
            var bestEndI = 1
            var bestCost = Float.POSITIVE_INFINITY
            for (i in 1..n) {
                if (dp[m][i] < bestCost) {
                    bestCost = dp[m][i]
                    bestEndI = i
                }
            }
            val path = mutableListOf<Pair<Int, Int>>()
            var j = m
            var i = bestEndI
            while (j > 0 && i > 0) {
                path.add((i - 1) to (j - 1))
                val diag = dp[j - 1][i - 1]
                val up = dp[j - 1][i]
                val left = dp[j][i - 1]
                when {
                    diag <= up && diag <= left -> { j--; i-- }
                    up <= left -> j--
                    else -> i--
                }
            }
            path.reverse()
            val points = smoothMonotone(path.map { (si, ti) ->
                AlignmentPoint(source[si].timeSec, target[ti].timeSec)
            })
            val avgCost = bestCost / max(1, path.size)
            val confidence = exp(-avgCost.toDouble() * 1.4).coerceIn(0.0, 1.0)
            val coverage = (m.toDouble() / n.toDouble()).coerceIn(0.0, 1.0)
            return AlignmentResult(points, confidence, coverage, shift)
        }
    }

    private fun smoothMonotone(points: List<AlignmentPoint>): List<AlignmentPoint> {
        if (points.size <= 2) return points
        val out = mutableListOf<AlignmentPoint>()
        var lastTarget = -1.0
        for (pt in points) {
            if (out.isEmpty() || pt.sourceSec > out.last().sourceSec + 1e-4) {
                val safeTarget = max(pt.targetSec, lastTarget)
                out.add(AlignmentPoint(pt.sourceSec, safeTarget))
                lastTarget = safeTarget
            }
        }
        return out
    }

    /**
     * Linearly interpolate target position from mapping; returns null if outside mapped coverage.
     */
    fun interpolate(mapping: List<AlignmentPoint>, tA: Double): Double? {
        if (mapping.isEmpty()) return null
        val first = mapping.first()
        val last = mapping.last()
        if (tA < first.sourceSec - 0.5 || tA > last.sourceSec + 0.5) {
            return null
        }
        if (tA <= first.sourceSec) return first.targetSec
        if (tA >= last.sourceSec) return last.targetSec

        for (idx in 0 until mapping.size - 1) {
            val p0 = mapping[idx]
            val p1 = mapping[idx + 1]
            if (tA >= p0.sourceSec && tA <= p1.sourceSec) {
                val span = (p1.sourceSec - p0.sourceSec).coerceAtLeast(1e-6)
                val alpha = (tA - p0.sourceSec) / span
                return p0.targetSec + alpha * (p1.targetSec - p0.targetSec)
            }
        }
        return last.targetSec
    }

    /**
     * Find the next downbeat (beatInBar == 1) strictly after currentSec.
     */
    fun nextDownbeat(beats: List<BeatPoint>, currentSec: Double, bpmFallback: Double): Double {
        val next = beats.firstOrNull { it.beatInBar == 1 && it.tSec > currentSec + 0.04 }
        if (next != null) return next.tSec
        val barSec = (60.0 / bpmFallback.coerceAtLeast(40.0)) * 4.0
        val currentBarIdx = kotlin.math.floor(currentSec / barSec).toInt() + 1
        return currentBarIdx * barSec
    }

    /**
     * Implements Section 6.4 exact runtime transition math:
     * tB = interpolate(mapping, tA) # null if out of coverage
     * if tB is null -> jump to firstMappedPoint.target
     * bar = nextDownbeat(analysisA, tA)
     * tB' = tB + (bar - tA) * tempoRatio(B, A)
     */
    fun computeScheduledTransition(
        currentSecA: Double,
        analysisA: TrackAnalysis,
        analysisB: TrackAnalysis,
        mapping: List<AlignmentPoint>,
        crossfadeBeats: Int = 2
    ): ScheduledTransition {
        val rawTB = interpolate(mapping, currentSecA)
        val fallback = rawTB == null
        val tB = rawTB ?: (mapping.firstOrNull()?.targetSec ?: 0.0)
        val bar = nextDownbeat(analysisA.beats, currentSecA, analysisA.tempo.bpm)
        val tempoRatio = (analysisB.tempo.bpm / analysisA.tempo.bpm.coerceAtLeast(40.0))
        val targetStart = if (fallback) {
            tB
        } else {
            (tB + (bar - currentSecA) * tempoRatio).coerceIn(0.0, analysisB.durationMs / 1000.0)
        }
        val beatSecA = 60.0 / analysisA.tempo.bpm.coerceAtLeast(40.0)
        val crossfadeSec = (crossfadeBeats.coerceIn(1, 4)) * beatSecA
        return ScheduledTransition(
            barTriggerTimeA = bar,
            targetStartTimeB = targetStart,
            crossfadeDurationSec = crossfadeSec,
            isFallbackToSampleStart = fallback
        )
    }

    private fun featureDistance(a: BeatFeature, b: BeatFeature): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (k in 0 until 12) {
            dot += a.chroma[k] * b.chroma[k]
            normA += a.chroma[k] * a.chroma[k]
            normB += b.chroma[k] * b.chroma[k]
        }
        val denom = sqrt(normA * normB).coerceAtLeast(1e-5f)
        val cosineDist = 1f - (dot / denom)
        val onsetDist = abs(a.onsetEnergy - b.onsetEnergy) * 0.25f
        return cosineDist + onsetDist
    }

    private fun transposeChroma(chroma: FloatArray, semitones: Int): FloatArray {
        val out = FloatArray(12)
        for (i in 0 until 12) {
            out[(i + semitones) % 12] = chroma[i]
        }
        return out
    }

    fun chordLabelToChroma(label: String): FloatArray {
        val chroma = FloatArray(12)
        val clean = label.trim()
        if (clean.isEmpty()) {
            chroma[0] = 1f
            return chroma
        }
        val rootNote = when {
            clean.startsWith("C#") || clean.startsWith("Db") -> 1
            clean.startsWith("D#") || clean.startsWith("Eb") -> 3
            clean.startsWith("F#") || clean.startsWith("Gb") -> 6
            clean.startsWith("G#") || clean.startsWith("Ab") -> 8
            clean.startsWith("A#") || clean.startsWith("Bb") -> 10
            clean.startsWith("C") -> 0
            clean.startsWith("D") -> 2
            clean.startsWith("E") -> 4
            clean.startsWith("F") -> 5
            clean.startsWith("G") -> 7
            clean.startsWith("A") -> 9
            clean.startsWith("B") -> 11
            else -> 0
        }
        val isMinor = clean.drop(1).trimStart('#', 'b').startsWith("m") &&
            !clean.drop(1).trimStart('#', 'b').startsWith("maj")
        val third = if (isMinor) 3 else 4
        val fifth = 7
        chroma[rootNote] = 1.0f
        chroma[(rootNote + third) % 12] = 0.8f
        chroma[(rootNote + fifth) % 12] = 0.7f
        return chroma
    }

    fun createClickAlignmentErrorMs(predictedSec: Double, annotatedSec: Double): Double {
        return abs(predictedSec - annotatedSec) * 1000.0
    }
}

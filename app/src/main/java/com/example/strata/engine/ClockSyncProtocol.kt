package com.example.strata.engine

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pocket Orchestra Multi-Device Synchronization Protocol.
 * Implements Section 7.2 & 7.3:
 * 1. Common Host Clock (NTP-style ping/pong over local WebSocket, filtering lowest-RTT samples) -> < 2 ms
 * 2. Acoustic Output Latency Calibration (chirp emission + microphone cross-correlation) -> < 3 ms
 * 3. Quartz Drift Compensation (micro-resampling +-0.1%) -> < 2 ms over 5 min
 */
object ClockSyncProtocol {

    data class ClockPingSample(
        val t0ClientSendMs: Double,
        val t1HostRecvMs: Double,
        val t2HostSendMs: Double,
        val t3ClientRecvMs: Double
    ) {
        val rttMs: Double
            get() = (t3ClientRecvMs - t0ClientSendMs) - (t2HostSendMs - t1HostRecvMs)

        val offsetMs: Double
            get() = ((t1HostRecvMs - t0ClientSendMs) + (t2HostSendMs - t3ClientRecvMs)) / 2.0
    }

    data class SyncStats(
        val estimatedOffsetMs: Double,
        val bestRttMs: Double,
        val jitterMs: Double,
        val driftPpm: Double
    )

    /**
     * Filter lowest-RTT 25% of 20-30 NTP exchanges to reject Wi-Fi queuing spikes.
     */
    fun computeClockOffset(samples: List<ClockPingSample>, driftPpm: Double = 0.0): SyncStats {
        if (samples.isEmpty()) return SyncStats(0.0, 0.0, 0.0, driftPpm)
        val sorted = samples.sortedBy { it.rttMs }
        val keepCount = (sorted.size / 4).coerceAtLeast(1)
        val best = sorted.take(keepCount)
        val meanOffset = best.map { it.offsetMs }.average()
        val variance = best.map { (it.offsetMs - meanOffset) * (it.offsetMs - meanOffset) }.average()
        return SyncStats(
            estimatedOffsetMs = meanOffset,
            bestRttMs = best.first().rttMs,
            jitterMs = sqrt(variance),
            driftPpm = driftPpm
        )
    }

    /**
     * Generate a short 30ms logarithmic chirp (1200 Hz -> 3600 Hz) at 44.1 kHz
     * for acoustic output-latency calibration between host microphone and guest speakers.
     */
    fun generateCalibrationChirp(sampleRate: Int = 44100, durationMs: Int = 35): ShortArray {
        val numSamples = (sampleRate * durationMs) / 1000
        val chirp = ShortArray(numSamples)
        val f0 = 1200.0
        val f1 = 3600.0
        val totalSec = durationMs / 1000.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = f0 + (f1 - f0) * (t / totalSec)
            val env = sin(Math.PI * i / numSamples) // Hann window
            val sample = sin(2.0 * Math.PI * freq * t) * env * 0.85
            chirp[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return chirp
    }

    /**
     * Normalized cross-correlation peak detector to measure exact arrival sample of calibration chirp.
     * Returns latency offset in milliseconds relative to expected emission time.
     */
    fun detectChirpArrivalMs(
        recordedSamples: ShortArray,
        chirpTemplate: ShortArray,
        expectedStartSample: Int,
        sampleRate: Int = 44100
    ): Double {
        if (recordedSamples.size < chirpTemplate.size) return 0.0
        var bestIdx = expectedStartSample.coerceIn(0, recordedSamples.size - chirpTemplate.size)
        var bestScore = Double.NEGATIVE_INFINITY
        val searchEnd = (recordedSamples.size - chirpTemplate.size).coerceAtLeast(0)

        // Coarse + fine scan
        for (offset in 0..searchEnd step 4) {
            var corr = 0.0
            for (k in chirpTemplate.indices step 2) {
                corr += recordedSamples[offset + k].toDouble() * chirpTemplate[k].toDouble()
            }
            if (corr > bestScore) {
                bestScore = corr
                bestIdx = offset
            }
        }
        val refineStart = (bestIdx - 4).coerceAtLeast(0)
        val refineEnd = (bestIdx + 4).coerceAtMost(searchEnd)
        for (offset in refineStart..refineEnd) {
            var corr = 0.0
            for (k in chirpTemplate.indices) {
                corr += recordedSamples[offset + k].toDouble() * chirpTemplate[k].toDouble()
            }
            if (corr > bestScore) {
                bestScore = corr
                bestIdx = offset
            }
        }

        val deltaSamples = bestIdx - expectedStartSample
        return (deltaSamples * 1000.0) / sampleRate.toDouble()
    }

    /**
     * Compute micro-resampling ratio (clamped within +-0.1% per Section 7.2)
     * to keep quartz drift < 2 ms over 5 minutes without audible jumps.
     */
    fun computeMicroResampleRatio(positionErrorMs: Double): Double {
        val correction = (positionErrorMs / 5000.0).coerceIn(-0.001, 0.001)
        return 1.0 - correction
    }

    // Wire JSON builders matching Section 7.3 exact camelCase field names
    fun buildJoinMessage(sessionCode: String, deviceModel: String, reportedLatencyMs: Int): String =
        """{"type":"join","sessionCode":"$sessionCode","deviceModel":"$deviceModel","reportedLatencyMs":$reportedLatencyMs}"""

    fun buildClockPing(t0Sec: Double): String =
        """{"type":"clockPing","t0":$t0Sec}"""

    fun buildAssignMessage(stems: List<String>, gainDb: Float, pan: Float): String {
        val stemArr = stems.joinToString(",") { "\"$it\"" }
        return """{"type":"assign","stems":[$stemArr],"gainDb":$gainDb,"pan":$pan}"""
    }

    fun buildPlayMessage(trackId: String, atHostTimeSec: Double, fromPosSec: Double): String =
        """{"type":"play","trackId":"$trackId","atHostTime":$atHostTimeSec,"fromPos":$fromPosSec}"""

    fun buildCalibrateMessage(chirpAtHostTimeSec: Double): String =
        """{"type":"calibrate","chirpAtHostTime":$chirpAtHostTimeSec}"""
}

package com.example.strata

import com.example.strata.data.StrataCatalog
import com.example.strata.domain.VersionLinkType
import com.example.strata.engine.GenealogyAligner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenealogyAlignerTest {

    @Test
    fun `cover alignment finds optimal transposition across 12 semitones`() {
        val original = StrataCatalog.tracks.first { it.id == "trk_01_abidjan_minuit" }
        val cover = StrataCatalog.tracks.first { it.id == "trk_02_abidjan_zouglou" } // Am vs Cm (+3 semitones)

        val srcFeatures = GenealogyAligner.extractBeatFeatures(original.analysis)
        val tgtFeatures = GenealogyAligner.extractBeatFeatures(cover.analysis)
        val result = GenealogyAligner.align(srcFeatures, tgtFeatures, VersionLinkType.COVER_OF)

        assertTrue("Mapping should be non-empty", result.mapping.isNotEmpty())
        assertTrue("Confidence should be high for transposed cover", result.confidence > 0.70)
        assertEquals(1.0, result.coverage, 0.01)
    }

    @Test
    fun `sample subsequence alignment detects partial coverage`() {
        val fullTrack = StrataCatalog.tracks.first { it.id == "trk_04_soleil_bassam" }
        val sample = StrataCatalog.tracks.first { it.id == "trk_05_kora_1984" }

        val srcFeatures = GenealogyAligner.extractBeatFeatures(fullTrack.analysis)
        val tgtFeatures = GenealogyAligner.extractBeatFeatures(sample.analysis)
        val result = GenealogyAligner.align(srcFeatures, tgtFeatures, VersionLinkType.SAMPLE_OF)

        assertTrue(result.mapping.isNotEmpty())
        assertTrue("Coverage of shorter sample inside full track should be < 1.0", result.coverage < 1.0)
    }

    @Test
    fun `scheduled transition snaps strictly to next downbeat and aligns within tolerance`() {
        val trackA = StrataCatalog.tracks.first { it.id == "trk_01_abidjan_minuit" }
        val trackB = StrataCatalog.tracks.first { it.id == "trk_02_abidjan_zouglou" }
        val link = StrataCatalog.versionLinks.first { it.id == "lnk_01" }

        val currentSecA = 16.5
        val scheduled = GenealogyAligner.computeScheduledTransition(
            currentSecA = currentSecA,
            analysisA = trackA.analysis,
            analysisB = trackB.analysis,
            mapping = link.mapping,
            crossfadeBeats = 2
        )

        // Verify trigger time lands exactly on a beatInBar == 1
        val matchedBeat = trackA.analysis.beats.minByOrNull { kotlin.math.abs(it.tSec - scheduled.barTriggerTimeA) }!!
        assertEquals(1, matchedBeat.beatInBar)
        assertTrue(scheduled.barTriggerTimeA > currentSecA)

        // Out-of-coverage returns null on interpolate
        val outOfBounds = GenealogyAligner.interpolate(
            listOf(
                com.example.strata.domain.AlignmentPoint(12.0, 16.0),
                com.example.strata.domain.AlignmentPoint(36.0, 42.0)
            ),
            tA = 2.0
        )
        assertNull(outOfBounds)
    }
}

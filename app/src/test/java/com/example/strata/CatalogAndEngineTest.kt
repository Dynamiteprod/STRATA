package com.example.strata

import com.example.strata.data.StrataCatalog
import com.example.strata.domain.StemType
import com.example.strata.engine.MultiStemAudioEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogAndEngineTest {

    @Test
    fun `all catalog tracks have valid 10000 bp BURIDA splits and 4 stems`() {
        assertTrue(StrataCatalog.tracks.size >= 8)
        for (track in StrataCatalog.tracks) {
            val totalBp = track.splits.sumOf { it.shareBp }
            assertEquals("Splits for ${track.id} must total 10000 bp", 10000, totalBp)
            assertEquals("Each track must expose 4 stems", 4, track.analysis.stems.size)
            assertTrue("Beat grid must be non-empty", track.analysis.beats.isNotEmpty())
            assertTrue("Chords must be non-empty", track.analysis.chords.isNotEmpty())
        }
    }

    @Test
    fun `unverified live chords trigger low-confidence flag per CA 5_3`() {
        val liveTrack = StrataCatalog.tracks.first { it.id == "trk_03_abidjan_live" }
        assertFalse("Live unverified track should report hasVerifiedChords=false", liveTrack.analysis.hasVerifiedChords)

        val studioTrack = StrataCatalog.tracks.first { it.id == "trk_01_abidjan_minuit" }
        assertTrue(studioTrack.analysis.hasVerifiedChords)
    }

    @Test
    fun `multi-stem audio engine manages tempo, transposition, karaoke, and bar-locked loop`() {
        val engine = MultiStemAudioEngine()
        val track = StrataCatalog.tracks.first()
        engine.loadTrack(track, autoPlay = false)

        engine.setTempoMultiplier(0.75f)
        assertEquals(0.75f, engine.state.value.tempoMultiplier, 0.001f)

        engine.setTransposeSemitones(-3)
        assertEquals(-3, engine.state.value.transposeSemitones)

        engine.toggleKaraokeMode()
        assertTrue(engine.state.value.karaokeMode)
        assertTrue(engine.state.value.stemMix[StemType.VOCALS]?.muted == true)
        assertTrue(engine.state.value.stemsModeActive)

        engine.seekToMs(5000L)
        engine.setLoopPointA()
        engine.seekToMs(10000L)
        engine.setLoopPointB()
        assertNotNull(engine.state.value.loopStartMs)
        assertNotNull(engine.state.value.loopEndMs)
        assertTrue(engine.state.value.loopEndMs!! > engine.state.value.loopStartMs!!)

        engine.clearLoop()
        assertEquals(null, engine.state.value.loopStartMs)
        engine.release()
    }
}

package com.example.strata

import com.example.strata.data.LocalMusicRepository
import com.example.strata.data.StrataCatalog
import com.example.strata.domain.StemType
import com.example.strata.engine.MultiStemAudioEngine
import com.example.strata.engine.StrataPlaybackController
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

    @Test
    fun `local phone track builds valid 4 stems, 10000 bp split, and updates waveforms in engine`() {
        val localTrack = LocalMusicRepository.buildLocalTrack(
            id = "loc_42",
            title = "Mon Son Abidjan.mp3",
            artistName = "DJ Test",
            releaseTitle = "Téléphone",
            durationMs = 195_000L,
            uriString = "content://media/external/audio/media/42"
        )

        assertEquals("content://media/external/audio/media/42", localTrack.localUriString)
        assertTrue(localTrack.isDownloadedOffline)
        assertEquals(4, localTrack.analysis.stems.size)
        assertEquals(10000, localTrack.splits.sumOf { it.shareBp })
        assertTrue(localTrack.license.allowStems)

        val combined = listOf(localTrack) + StrataCatalog.tracks
        val onlyLocal = combined.filter { it.localUriString != null }
        assertEquals(1, onlyLocal.size)
        assertEquals("loc_42", onlyLocal.first().id)

        val engine = MultiStemAudioEngine()
        engine.loadTrack(localTrack, autoPlay = false)
        val customPeaks = List(64) { 0.88f }
        engine.updateTrackWaveforms("loc_42", customPeaks)
        assertEquals(0.88f, engine.state.value.currentTrack!!.analysis.stems.first().waveformPeaks.first(), 0.001f)
        engine.release()
    }

    @Test
    fun `playback controller cycles next, previous, pause, and play without context`() {
        StrataPlaybackController.init(null)
        val first = StrataCatalog.tracks[0]
        val second = StrataCatalog.tracks[1]
        StrataPlaybackController.updatePlaylist(listOf(first, second))

        StrataPlaybackController.playTrack(first, autoPlay = false)
        assertEquals(first.id, StrataPlaybackController.audioEngine.state.value.currentTrack?.id)

        StrataPlaybackController.playNext()
        assertEquals(second.id, StrataPlaybackController.audioEngine.state.value.currentTrack?.id)

        StrataPlaybackController.playPrevious()
        assertEquals(first.id, StrataPlaybackController.audioEngine.state.value.currentTrack?.id)

        StrataPlaybackController.pause()
        assertFalse(StrataPlaybackController.audioEngine.state.value.isPlaying)
        StrataPlaybackController.audioEngine.release()
    }
}

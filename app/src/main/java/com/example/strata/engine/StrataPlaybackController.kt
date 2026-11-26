package com.example.strata.engine

import android.content.Context
import com.example.strata.data.StrataCatalog
import com.example.strata.domain.ForkSpec
import com.example.strata.domain.Track
import com.example.strata.service.StrataPlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared playback coordinator bridging UI (StrataViewModel) and background
 * media notification service (StrataPlaybackService) so playback and queue
 * controls work seamlessly from both the app and the Android notification shade.
 */
object StrataPlaybackController {

    val audioEngine = MultiStemAudioEngine()

    private var appContext: Context? = null
    private var initialized = false

    private val _playlist = MutableStateFlow<List<Track>>(StrataCatalog.tracks)
    val playlist: StateFlow<List<Track>> = _playlist.asStateFlow()

    @Volatile var shuffleEnabled: Boolean = false
        private set

    @Volatile var repeatEnabled: Boolean = false
        private set

    var onTrackStarted: ((Track, ForkSpec?) -> Unit)? = null
    var onErrorMessage: ((String) -> Unit)? = null

    fun init(context: Context?) {
        if (context != null) {
            appContext = context.applicationContext
            audioEngine.attachContext(context.applicationContext)
        }
        if (initialized) return
        initialized = true
        audioEngine.setOnTrackEndedListener {
            val current = audioEngine.state.value.currentTrack
            if (repeatEnabled && current != null) {
                playTrack(current, autoPlay = true, fork = audioEngine.state.value.activeFork)
            } else {
                playNext()
            }
        }
        audioEngine.setOnPlaybackErrorListener { msg ->
            onErrorMessage?.invoke(msg)
            syncNotificationIfAttached()
        }
    }

    fun updatePlaylist(tracks: List<Track>) {
        if (tracks.isNotEmpty()) {
            _playlist.value = tracks
        }
    }

    fun setShuffle(enabled: Boolean) {
        shuffleEnabled = enabled
    }

    fun setRepeat(enabled: Boolean) {
        repeatEnabled = enabled
    }

    fun playTrack(track: Track, autoPlay: Boolean = true, fork: ForkSpec? = null) {
        audioEngine.loadTrack(track, autoPlay = autoPlay, fork = fork)
        if (autoPlay) {
            onTrackStarted?.invoke(track, fork)
            syncNotificationIfAttached(startForegroundIfNeeded = true)
        }
    }

    fun togglePlayPause() {
        if (audioEngine.state.value.currentTrack == null) {
            val first = _playlist.value.firstOrNull() ?: return
            playTrack(first, autoPlay = true)
            return
        }
        val wasPlaying = audioEngine.state.value.isPlaying
        audioEngine.togglePlayPause()
        syncNotificationIfAttached(startForegroundIfNeeded = !wasPlaying)
    }

    fun play() {
        if (audioEngine.state.value.currentTrack == null) {
            val first = _playlist.value.firstOrNull() ?: return
            playTrack(first, autoPlay = true)
            return
        }
        audioEngine.play()
        syncNotificationIfAttached(startForegroundIfNeeded = true)
    }

    fun pause() {
        audioEngine.pause()
        syncNotificationIfAttached(startForegroundIfNeeded = false)
    }

    fun seekToMs(ms: Long) {
        audioEngine.seekToMs(ms)
        syncNotificationIfAttached(startForegroundIfNeeded = false)
    }

    fun playNext() {
        val list = _playlist.value
        if (list.isEmpty()) return
        val currentId = audioEngine.state.value.currentTrack?.id
        val nextTrack = if (shuffleEnabled && list.size > 1) {
            list.filter { it.id != currentId }.random()
        } else {
            val idx = list.indexOfFirst { it.id == currentId }
            list[(idx + 1) % list.size]
        }
        playTrack(nextTrack, autoPlay = true)
    }

    fun playPrevious() {
        val pos = audioEngine.state.value.positionMs
        if (pos > 3500L) {
            seekToMs(0L)
            return
        }
        val list = _playlist.value
        if (list.isEmpty()) return
        val currentId = audioEngine.state.value.currentTrack?.id
        val idx = list.indexOfFirst { it.id == currentId }
        val prevIdx = if (idx <= 0) list.lastIndex else idx - 1
        playTrack(list[prevIdx], autoPlay = true)
    }

    fun syncNotificationIfAttached(startForegroundIfNeeded: Boolean = false) {
        val ctx = appContext ?: return
        StrataPlaybackService.sync(ctx, startForegroundIfNeeded = startForegroundIfNeeded)
    }
}

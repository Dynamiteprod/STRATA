package com.example.strata.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.strata.engine.MultiStemAudioEngine
import com.example.strata.engine.StrataPlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StrataPlaybackService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateJob: Job? = null
    private lateinit var mediaSession: MediaSession
    private lateinit var notificationManager: NotificationManager

    private var lastNotifiedTrackId: String? = null
    private var lastNotifiedPlaying: Boolean? = null
    private var lastNotifiedForkId: String? = null
    private var lastMediaStatePushMs: Long = 0L

    private val bitmapCache = HashMap<Int, Bitmap>()

    override fun onCreate() {
        super.onCreate()
        isServiceActive = true
        StrataPlaybackController.init(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureNotificationChannel()

        mediaSession = MediaSession(this, "StrataMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    StrataPlaybackController.play()
                }

                override fun onPause() {
                    StrataPlaybackController.pause()
                }

                override fun onSkipToNext() {
                    StrataPlaybackController.playNext()
                }

                override fun onSkipToPrevious() {
                    StrataPlaybackController.playPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    StrataPlaybackController.seekToMs(pos)
                }

                override fun onStop() {
                    stopPlaybackAndService()
                }
            })
            isActive = true
        }

        stateJob = serviceScope.launch {
            StrataPlaybackController.audioEngine.state.collectLatest { playback ->
                val track = playback.currentTrack ?: return@collectLatest
                val now = System.currentTimeMillis()
                val trackChanged = track.id != lastNotifiedTrackId ||
                    playback.activeFork?.forkId != lastNotifiedForkId
                val playChanged = playback.isPlaying != lastNotifiedPlaying

                if (trackChanged) {
                    updateMediaMetadata(playback)
                }
                if (trackChanged || playChanged || (now - lastMediaStatePushMs) >= 900L) {
                    lastMediaStatePushMs = now
                    updateMediaPlaybackState(playback)
                }
                if (trackChanged || playChanged) {
                    lastNotifiedTrackId = track.id
                    lastNotifiedForkId = playback.activeFork?.forkId
                    lastNotifiedPlaying = playback.isPlaying
                    postOrUpdateNotification(playback, forceForeground = playback.isPlaying)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        StrataPlaybackController.init(applicationContext)
        when (intent?.action) {
            ACTION_PREV -> StrataPlaybackController.playPrevious()
            ACTION_TOGGLE_PLAY -> StrataPlaybackController.togglePlayPause()
            ACTION_NEXT -> StrataPlaybackController.playNext()
            ACTION_STOP -> {
                stopPlaybackAndService()
                return START_NOT_STICKY
            }
        }

        val playback = StrataPlaybackController.audioEngine.state.value
        val mustStartForeground = playback.isPlaying ||
            intent?.getBooleanExtra(EXTRA_START_FG, false) == true

        if (playback.currentTrack != null) {
            updateMediaMetadata(playback)
            updateMediaPlaybackState(playback)
            lastNotifiedTrackId = playback.currentTrack.id
            lastNotifiedForkId = playback.activeFork?.forkId
            lastNotifiedPlaying = playback.isPlaying
            postOrUpdateNotification(playback, forceForeground = mustStartForeground)
        } else if (mustStartForeground) {
            // Safety fallback so startForegroundService contract is always honored
            val fallback = buildFallbackNotification()
            promoteForeground(fallback)
            stopSelf()
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!StrataPlaybackController.audioEngine.state.value.isPlaying) {
            stopPlaybackAndService()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        isServiceActive = false
        stateJob?.cancel()
        serviceScope.cancel()
        try {
            mediaSession.isActive = false
            mediaSession.release()
        } catch (_: Throwable) {
        }
        bitmapCache.values.forEach { bmp ->
            if (!bmp.isRecycled) bmp.recycle()
        }
        bitmapCache.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopPlaybackAndService() {
        StrataPlaybackController.audioEngine.pause()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        notificationManager.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Lecture audio STRATA",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Contrôles de lecture musicale dans la barre de notification"
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun updateMediaMetadata(playback: MultiStemAudioEngine.PlaybackState) {
        val track = playback.currentTrack ?: return
        val title = playback.activeFork?.title ?: track.title
        val album = if (track.localUriString != null) track.releaseTitle else track.genre.labelFr
        val art = loadCoverBitmap(track.coverResId)
        val builder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artistName)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, playback.durationMs.coerceAtLeast(1L))
        if (art != null) {
            builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art)
        }
        mediaSession.setMetadata(builder.build())
    }

    private fun updateMediaPlaybackState(playback: MultiStemAudioEngine.PlaybackState) {
        val stateCode = if (playback.isPlaying) {
            PlaybackState.STATE_PLAYING
        } else {
            PlaybackState.STATE_PAUSED
        }
        val stateObj = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SEEK_TO or
                    PlaybackState.ACTION_STOP
            )
            .setState(stateCode, playback.positionMs.coerceAtLeast(0L), playback.tempoMultiplier)
            .build()
        mediaSession.setPlaybackState(stateObj)
    }

    private fun postOrUpdateNotification(
        playback: MultiStemAudioEngine.PlaybackState,
        forceForeground: Boolean
    ) {
        val notification = buildMediaNotification(playback)
        if (forceForeground) {
            promoteForeground(notification)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
            try {
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
            }
        }
    }

    private fun promoteForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Throwable) {
            try {
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (_: Throwable) {
            }
        }
    }

    private fun buildMediaNotification(playback: MultiStemAudioEngine.PlaybackState): Notification {
        val track = playback.currentTrack!!
        val title = playback.activeFork?.title ?: track.title
        val subText = if (track.localUriString != null) "Téléphone" else track.genre.labelFr
        val art = loadCoverBitmap(track.coverResId)

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            100,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPendingIntent = serviceActionPendingIntent(ACTION_PREV, 101)
        val togglePendingIntent = serviceActionPendingIntent(ACTION_TOGGLE_PLAY, 102)
        val nextPendingIntent = serviceActionPendingIntent(ACTION_NEXT, 103)
        val stopPendingIntent = serviceActionPendingIntent(ACTION_STOP, 104)

        val prevAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_media_prev),
            "Précédent",
            prevPendingIntent
        ).build()

        val playPauseAction = Notification.Action.Builder(
            Icon.createWithResource(
                this,
                if (playback.isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play
            ),
            if (playback.isPlaying) "Pause" else "Lecture",
            togglePendingIntent
        ).build()

        val nextAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_media_next),
            "Suivant",
            nextPendingIntent
        ).build()

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        builder
            .setSmallIcon(R.drawable.ic_stat_strata)
            .setContentTitle(title)
            .setContentText(track.artistName)
            .setSubText(subText)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(playback.isPlaying)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        if (art != null) {
            builder.setLargeIcon(art)
        }
        return builder.build()
    }

    private fun buildFallbackNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(R.drawable.ic_stat_strata)
            .setContentTitle("STRATA")
            .setContentText("Lecteur studio")
            .build()
    }

    private fun serviceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, StrataPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun loadCoverBitmap(resId: Int): Bitmap? {
        bitmapCache[resId]?.let { if (!it.isRecycled) return it }
        return try {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            val decoded = BitmapFactory.decodeResource(resources, resId, opts)
            if (decoded != null) {
                bitmapCache[resId] = decoded
            }
            decoded
        } catch (_: Throwable) {
            null
        }
    }

    companion object {
        private const val CHANNEL_ID = "strata_media_playback"
        private const val NOTIFICATION_ID = 4401

        const val ACTION_SYNC = "com.example.strata.action.SYNC"
        const val ACTION_PREV = "com.example.strata.action.PREV"
        const val ACTION_TOGGLE_PLAY = "com.example.strata.action.TOGGLE_PLAY"
        const val ACTION_NEXT = "com.example.strata.action.NEXT"
        const val ACTION_STOP = "com.example.strata.action.STOP"
        private const val EXTRA_START_FG = "extra_start_fg"

        @Volatile
        var isServiceActive: Boolean = false
            private set

        fun sync(context: Context, startForegroundIfNeeded: Boolean) {
            if (!startForegroundIfNeeded && !isServiceActive) return
            val intent = Intent(context, StrataPlaybackService::class.java).apply {
                action = ACTION_SYNC
                putExtra(EXTRA_START_FG, startForegroundIfNeeded)
            }
            try {
                if (startForegroundIfNeeded) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Throwable) {
            }
        }
    }
}

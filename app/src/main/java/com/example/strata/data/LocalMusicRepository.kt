package com.example.strata.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.R
import com.example.strata.domain.BeatPoint
import com.example.strata.domain.ChordEvent
import com.example.strata.domain.GenreStyle
import com.example.strata.domain.HumanReviewed
import com.example.strata.domain.KeyInfo
import com.example.strata.domain.LicenseConfig
import com.example.strata.domain.LyricLine
import com.example.strata.domain.RightsSplit
import com.example.strata.domain.SectionSegment
import com.example.strata.domain.StemInfo
import com.example.strata.domain.StemSource
import com.example.strata.domain.StemType
import com.example.strata.domain.TempoInfo
import com.example.strata.domain.Track
import com.example.strata.domain.TrackAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sin

/**
 * Scans local audio tracks from Android MediaStore (when audio permission is granted)
 * or imports user-selected audio files via Storage Access Framework (OpenDocument).
 */
object LocalMusicRepository {

    suspend fun scanDeviceMusic(context: Context): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext() && tracks.size < 250) {
                    val mediaId = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Audio local"
                    val artistRaw = cursor.getString(artistCol)
                    val artist = if (artistRaw.isNullOrBlank() || artistRaw == "<unknown>") "Artiste inconnu" else artistRaw
                    val album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "Téléphone"
                    val durationMs = cursor.getLong(durCol).coerceAtLeast(10_000L)
                    val uri = ContentUris.withAppendedId(collection, mediaId)

                    tracks += buildLocalTrack(
                        id = "loc_$mediaId",
                        title = title,
                        artistName = artist,
                        releaseTitle = album,
                        durationMs = durationMs,
                        uri = uri
                    )
                }
            }
        } catch (_: SecurityException) {
            return@withContext emptyList()
        } catch (_: Throwable) {
            return@withContext emptyList()
        }
        tracks
    }

    suspend fun importPickedAudioUris(context: Context, uris: List<Uri>): List<Track> = withContext(Dispatchers.IO) {
        uris.mapIndexedNotNull { idx, uri ->
            try {
                var title: String? = null
                var artist: String? = null
                var album: String? = null
                var durationMs = 180_000L

                val mmr = MediaMetadataRetriever()
                try {
                    mmr.setDataSource(context, uri)
                    title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()?.coerceAtLeast(10_000L) ?: 180_000L
                } catch (_: Throwable) {
                } finally {
                    try {
                        mmr.release()
                    } catch (_: Throwable) {
                    }
                }

                if (title.isNullOrBlank()) {
                    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIdx >= 0) {
                                title = c.getString(nameIdx)?.substringBeforeLast('.')
                            }
                        }
                    }
                }

                buildLocalTrack(
                    id = "loc_pick_${System.currentTimeMillis()}_$idx",
                    title = title?.takeIf { it.isNotBlank() } ?: "Fichier audio ${idx + 1}",
                    artistName = artist?.takeIf { it.isNotBlank() } ?: "Fichier local",
                    releaseTitle = album?.takeIf { it.isNotBlank() } ?: "Importé",
                    durationMs = durationMs,
                    uri = uri
                )
            } catch (_: Throwable) {
                null
            }
        }
    }

    fun buildLocalTrack(
        id: String,
        title: String,
        artistName: String,
        releaseTitle: String,
        durationMs: Long,
        uri: Uri,
        estimatedBpm: Double = 112.0,
        waveformPeaks: List<Float>? = null
    ): Track = buildLocalTrack(
        id = id,
        title = title,
        artistName = artistName,
        releaseTitle = releaseTitle,
        durationMs = durationMs,
        uriString = uri.toString(),
        estimatedBpm = estimatedBpm,
        waveformPeaks = waveformPeaks
    )

    fun buildLocalTrack(
        id: String,
        title: String,
        artistName: String,
        releaseTitle: String,
        durationMs: Long,
        uriString: String,
        estimatedBpm: Double = 112.0,
        waveformPeaks: List<Float>? = null
    ): Track {
        val durationSec = durationMs / 1000.0
        val beatSec = 60.0 / estimatedBpm
        val totalBeats = (durationSec / beatSec).toInt().coerceIn(8, 2000)
        val beats = (0 until totalBeats).map { i ->
            BeatPoint(tSec = i * beatSec, beatInBar = (i % 4) + 1)
        }
        val sections = listOf(
            SectionSegment(0.0, durationSec * 0.15, "Intro"),
            SectionSegment(durationSec * 0.15, durationSec * 0.45, "Couplet"),
            SectionSegment(durationSec * 0.45, durationSec * 0.75, "Refrain"),
            SectionSegment(durationSec * 0.75, durationSec, "Outro")
        )
        val stems = StemType.entries.mapIndexed { sIdx, st ->
            val peaks = waveformPeaks ?: List(64) { i ->
                (abs(sin((i + sIdx * 5) * 0.31)) * 0.62f + 0.22f).toFloat().coerceIn(0.12f, 0.95f)
            }
            StemInfo(
                type = st,
                source = StemSource.AI,
                model = "realtime_midside_dsp",
                waveformPeaks = peaks
            )
        }

        return Track(
            id = id,
            workId = "wrk_$id",
            title = title,
            artistId = "art_local",
            artistName = artistName,
            releaseTitle = releaseTitle,
            genre = GenreStyle.AFROBEATS,
            isrc = "LOCAL-DEVICE",
            iswc = "LOCAL-FILE",
            durationMs = durationMs,
            coverResId = R.drawable.img_hero_strata,
            analysis = TrackAnalysis(
                trackId = id,
                durationMs = durationMs,
                tempo = TempoInfo(bpm = estimatedBpm, confidence = 0.60, isVariable = false),
                beats = beats,
                key = KeyInfo("C", "major", 0.55),
                chords = listOf(ChordEvent(0.0, durationSec, "Fichier local (analyse temps réel)", 0.50)),
                sections = sections,
                lyrics = listOf(
                    LyricLine(0.0, durationSec, "Lecture directe depuis ton téléphone — séparation DSP temps réel active au cran 3.")
                ),
                stems = stems,
                humanReviewed = HumanReviewed(chords = false, sections = false, lyrics = false)
            ),
            license = LicenseConfig(allowStream = true, allowStems = true, allowLoopExport = true, allowFork = true, allowOrchestra = true),
            splits = listOf(RightsSplit("pty_local", artistName, "performer", 10000, "Local")),
            rootPitchMidi = 60,
            isDownloadedOffline = true,
            localUriString = uriString
        )
    }
}

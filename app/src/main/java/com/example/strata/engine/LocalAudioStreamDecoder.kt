package com.example.strata.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * Streaming MediaExtractor + MediaCodec audio decoder producing 44.1 kHz Stereo float frames
 * from any local MP3 / M4A / AAC / WAV / FLAC / OGG file on the phone.
 */
class LocalAudioStreamDecoder(
    private val context: Context,
    private val uri: Uri,
    private val targetSampleRate: Int = 44100
) {
    private var extractor: MediaExtractor? = null
    private var codec: MediaCodec? = null
    private var inputSampleRate: Int = 44100
    private var inputChannels: Int = 2
    private var inputEos = false
    private var outputEos = false

    // Decoded stereo interleaved float buffer waiting to be consumed
    private var chunkL = FloatArray(0)
    private var chunkR = FloatArray(0)
    private var chunkSize = 0
    private var chunkCursor = 0

    // Resampling state
    private var resamplePhase = 0.0
    private var prevL = 0f
    private var prevR = 0f

    var isOpen: Boolean = false
        private set

    fun open(): Boolean {
        close()
        return try {
            val ex = MediaExtractor()
            ex.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var selectedFormat: MediaFormat? = null
            for (i in 0 until ex.trackCount) {
                val fmt = ex.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    selectedFormat = fmt
                    break
                }
            }
            if (audioTrackIndex < 0 || selectedFormat == null) {
                ex.release()
                return false
            }
            ex.selectTrack(audioTrackIndex)
            inputSampleRate = if (selectedFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                selectedFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100
            inputChannels = if (selectedFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                selectedFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            val mime = selectedFormat.getString(MediaFormat.KEY_MIME)!!
            val mc = MediaCodec.createDecoderByType(mime)
            mc.configure(selectedFormat, null, null, 0)
            mc.start()

            extractor = ex
            codec = mc
            inputEos = false
            outputEos = false
            chunkSize = 0
            chunkCursor = 0
            resamplePhase = 0.0
            isOpen = true
            true
        } catch (_: Throwable) {
            close()
            false
        }
    }

    fun seekToMs(ms: Long) {
        val ex = extractor ?: return
        val mc = codec ?: return
        try {
            ex.seekTo(ms * 1000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            mc.flush()
            inputEos = false
            outputEos = false
            chunkSize = 0
            chunkCursor = 0
            resamplePhase = 0.0
        } catch (_: Throwable) {
        }
    }

    /**
     * Fill [outL] and [outR] with [frameCount] stereo frames at [targetSampleRate].
     * Returns actual number of frames written (0 if end of stream reached).
     */
    private var outputPcmEncoding: Int = android.media.AudioFormat.ENCODING_PCM_16BIT

    fun readStereoFrames(outL: FloatArray, outR: FloatArray, frameCount: Int): Int {
        if (!isOpen) return 0
        var written = 0
        var emptyRetries = 0
        while (written < frameCount) {
            if (chunkCursor < chunkSize) {
                outL[written] = chunkL[chunkCursor]
                outR[written] = chunkR[chunkCursor]
                chunkCursor++
                written++
                emptyRetries = 0
            } else {
                if (outputEos) break
                val progressed = decodeNextChunk()
                if (!progressed) {
                    if (outputEos) break
                    emptyRetries++
                    if (emptyRetries > 60) break
                } else {
                    emptyRetries = 0
                }
            }
        }
        return written
    }

    private fun decodeNextChunk(): Boolean {
        val ex = extractor ?: return false
        val mc = codec ?: return false
        val info = MediaCodec.BufferInfo()

        // Feed input buffer
        if (!inputEos) {
            val inIdx = mc.dequeueInputBuffer(2000L)
            if (inIdx >= 0) {
                val buf = mc.getInputBuffer(inIdx)
                if (buf != null) {
                    val sampleSize = ex.readSampleData(buf, 0)
                    if (sampleSize < 0) {
                        mc.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputEos = true
                    } else {
                        val pts = ex.sampleTime.coerceAtLeast(0L)
                        mc.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
                        ex.advance()
                    }
                }
            }
        }

        // Drain output buffer
        val outIdx = mc.dequeueOutputBuffer(info, 4000L)
        when {
            outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                val newFmt = mc.outputFormat
                if (newFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    inputSampleRate = newFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }
                if (newFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    inputChannels = newFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N &&
                    newFmt.containsKey(MediaFormat.KEY_PCM_ENCODING)
                ) {
                    outputPcmEncoding = newFmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
                }
                return true
            }
            outIdx >= 0 -> {
                val outBuf = mc.getOutputBuffer(outIdx)
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputEos = true
                }
                if (outBuf != null && info.size > 0) {
                    outBuf.position(info.offset)
                    outBuf.limit(info.offset + info.size)
                    val ch = inputChannels.coerceAtLeast(1)
                    if (outputPcmEncoding == android.media.AudioFormat.ENCODING_PCM_FLOAT) {
                        val floatBuf = outBuf.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
                        val inFrames = floatBuf.remaining() / ch
                        val rawL = FloatArray(inFrames)
                        val rawR = FloatArray(inFrames)
                        for (i in 0 until inFrames) {
                            val l = floatBuf.get().coerceIn(-1f, 1f)
                            val r = if (ch >= 2) floatBuf.get().coerceIn(-1f, 1f) else l
                            for (extra in 2 until ch) floatBuf.get()
                            rawL[i] = l
                            rawR[i] = r
                        }
                        resampleToTarget(rawL, rawR)
                    } else {
                        val shortBuf = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        val inFrames = shortBuf.remaining() / ch
                        val rawL = FloatArray(inFrames)
                        val rawR = FloatArray(inFrames)
                        for (i in 0 until inFrames) {
                            val l = shortBuf.get() / 32768f
                            val r = if (ch >= 2) shortBuf.get() / 32768f else l
                            for (extra in 2 until ch) shortBuf.get()
                            rawL[i] = l
                            rawR[i] = r
                        }
                        resampleToTarget(rawL, rawR)
                    }
                }
                mc.releaseOutputBuffer(outIdx, false)
                return true
            }
            else -> return false
        }
    }

    private fun resampleToTarget(rawL: FloatArray, rawR: FloatArray) {
        if (rawL.isEmpty()) {
            chunkSize = 0
            chunkCursor = 0
            return
        }
        if (inputSampleRate == targetSampleRate) {
            chunkL = rawL
            chunkR = rawR
            chunkSize = rawL.size
            chunkCursor = 0
            return
        }
        val step = inputSampleRate.toDouble() / targetSampleRate.toDouble()
        val estOut = ((rawL.size / step).toInt() + 4).coerceAtLeast(1)
        val outL = FloatArray(estOut)
        val outR = FloatArray(estOut)
        var outCount = 0
        var pos = resamplePhase
        while (pos < rawL.size - 1 && outCount < estOut) {
            val idx = pos.toInt()
            val frac = (pos - idx).toFloat()
            val l0 = if (idx < 0) prevL else rawL[idx]
            val r0 = if (idx < 0) prevR else rawR[idx]
            val l1 = rawL[idx + 1]
            val r1 = rawR[idx + 1]
            outL[outCount] = l0 + (l1 - l0) * frac
            outR[outCount] = r0 + (r1 - r0) * frac
            outCount++
            pos += step
        }
        resamplePhase = pos - rawL.size
        prevL = rawL.last()
        prevR = rawR.last()
        chunkL = outL
        chunkR = outR
        chunkSize = outCount
        chunkCursor = 0
    }

    fun close() {
        isOpen = false
        try {
            codec?.stop()
            codec?.release()
        } catch (_: Throwable) {
        }
        try {
            extractor?.release()
        } catch (_: Throwable) {
        }
        codec = null
        extractor = null
    }

    companion object {
        /**
         * Fast waveform preview sampler (~first 20s) so local tracks show real amplitude peaks.
         */
        fun sampleQuickWaveform(context: Context, uri: Uri, bars: Int = 64): List<Float> {
            val dec = LocalAudioStreamDecoder(context, uri)
            if (!dec.open()) return List(bars) { 0.45f }
            return try {
                val frameBlock = 2048
                val bufL = FloatArray(frameBlock)
                val bufR = FloatArray(frameBlock)
                val peaks = FloatArray(bars)
                for (b in 0 until bars) {
                    val read = dec.readStereoFrames(bufL, bufR, frameBlock)
                    if (read <= 0) break
                    var maxAmp = 0f
                    for (i in 0 until read step 8) {
                        val a = abs(bufL[i] + bufR[i]) * 0.5f
                        if (a > maxAmp) maxAmp = a
                    }
                    peaks[b] = (maxAmp * 1.15f).coerceIn(0.12f, 0.96f)
                }
                peaks.toList()
            } catch (_: Throwable) {
                List(bars) { 0.45f }
            } finally {
                dec.close()
            }
        }
    }
}

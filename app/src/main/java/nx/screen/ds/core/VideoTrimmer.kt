package nx.screen.ds.core

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import java.nio.ByteBuffer

object VideoTrimmer {

    private const val TAG = "VideoTrimmer"

    fun durationMs(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
        } catch (_: Throwable) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun trim(
        context: Context,
        uri: Uri,
        startMs: Long,
        endMs: Long,
        out: String,
    ): Boolean {
        if (endMs <= startMs) return false
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) return false
        if (muxTracks(context, uri, startMs, endMs, out, includeAudio = true)) return true
        Log.e(TAG, "trim fallo con audio, reintentando solo video")
        return muxTracks(context, uri, startMs, endMs, out, includeAudio = false)
    }

    private fun muxTracks(
        context: Context,
        uri: Uri,
        startMs: Long,
        endMs: Long,
        out: String,
        includeAudio: Boolean,
    ): Boolean {
        val startUs = startMs * 1000
        val endUs = endMs * 1000
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val tracks = ArrayList<Int>()
            for (i in 0 until extractor.trackCount) {
                val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
                if (mime != null && (mime.startsWith("video/") ||
                        (includeAudio && mime.startsWith("audio/")))) {
                    tracks.add(i)
                }
            }
            if (tracks.isEmpty()) {
                Log.e(TAG, "sin pistas de video")
                return false
            }
            tracks.forEach { extractor.selectTrack(it) }
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val muxer = MediaMuxer(out, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            try {
                val indexOf = HashMap<Int, Int>()
                tracks.forEach { indexOf[it] = muxer.addTrack(extractor.getTrackFormat(it)) }
                muxer.start()
                var buffer = ByteBuffer.allocate(1 shl 20)
                val info = MediaCodec.BufferInfo()
                val wrotePerTrack = HashMap<Int, Boolean>()
                var firstPts = -1L
                while (true) {
                    val sampleTrack = extractor.sampleTrackIndex
                    if (sampleTrack < 0) break
                    val muxTrack = indexOf[sampleTrack]
                    if (muxTrack != null) {
                        val flags = extractor.sampleFlags
                        if (flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            val pts = extractor.sampleTime
                            if (pts >= 0 && pts <= endUs && (pts >= startUs || firstPts < 0)) {
                                val needed = extractor.sampleSize.toInt()
                                if (needed < 0) break
                                if (buffer.capacity() < needed) {
                                    buffer = ByteBuffer.allocate(needed)
                                }
                                buffer.rewind()
                                val read = extractor.readSampleData(buffer, 0)
                                if (read < 0) break
                                if (firstPts < 0) firstPts = pts
                                val outFlags = if (flags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                                } else {
                                    0
                                }
                                info.set(0, read, (pts - firstPts).coerceAtLeast(0L), outFlags)
                                muxer.writeSampleData(muxTrack, buffer, info)
                                wrotePerTrack[muxTrack] = true
                            }
                        }
                    }
                    extractor.advance()
                }
                if (wrotePerTrack.isEmpty() || wrotePerTrack.values.any { !it }) {
                    Log.e(TAG, "pistas sin datos: $wrotePerTrack")
                    return false
                }
                runCatching { muxer.stop() }
                    .getOrElse {
                        Log.e(TAG, "muxer.stop fallo: ${it.message}")
                        return false
                    }
                return true
            } finally {
                runCatching { muxer.release() }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "muxTracks fallo (audio=$includeAudio): ${e.message}")
            return false
        } finally {
            runCatching { extractor.release() }
        }
    }
}
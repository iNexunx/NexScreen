package nx.screen.ds.data

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class WallpaperKind { IMAGE, GIF, VIDEO }

data class WallpaperData(
    val kind: WallpaperKind,
    val filePath: String,
    val image: ImageBitmap?,
)

object WallpaperStore {

    private const val MAX_DIMENSION = 1024
    private const val MAX_FILE_BYTES = 512L * 1024 * 1024

    @Volatile
    private var cache: WallpaperData? = null

    private fun file(context: Context): File = File(context.filesDir, "wallpaper.bin")

    suspend fun load(context: Context): WallpaperData? = withContext(Dispatchers.IO) {
        if (cache == null) {
            val f = file(context)
            if (f.exists()) {
                val kind = detectKind(f)
                cache = WallpaperData(kind, f.absolutePath, thumbFor(kind, f))
            }
        }
        cache
    }

    suspend fun save(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext false
            var ok = false
            input.use { src ->
                val target = file(context)
                target.outputStream().use { out ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = src.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_FILE_BYTES) {
                            target.delete()
                            return@withContext false
                        }
                        out.write(buffer, 0, read)
                    }
                    ok = true
                }
            }
            if (ok) {
                val f = file(context)
                val kind = detectKind(f)
                cache = WallpaperData(kind, f.absolutePath, thumbFor(kind, f))
            }
            ok
        }.getOrDefault(false)
    }

    suspend fun saveFile(context: Context, src: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (!src.exists() || src.length() == 0L) return@withContext false
            val target = file(context)
            src.inputStream().use { input ->
                target.outputStream().use { out -> input.copyTo(out) }
            }
            val kind = detectKind(target)
            cache = WallpaperData(kind, target.absolutePath, thumbFor(kind, target))
            true
        }.getOrDefault(false)
    }

    suspend fun clear(context: Context) {
        withContext(Dispatchers.IO) {
            file(context).delete()
            cache = null
        }
    }

    private fun detectKind(f: File): WallpaperKind {
        val head = runCatching {
            f.inputStream().use { it.readBytes().copyOf(16 * 1024) }
        }.getOrElse { ByteArray(0) }
        val s = String(head, Charsets.ISO_8859_1)
        when {
            s.startsWith("GIF8") -> return WallpaperKind.GIF
            s.startsWith("\u001aE\u00df\u00a3") -> return WallpaperKind.VIDEO
            s.indexOf("ftyp") in 4..(head.size - 8) -> return WallpaperKind.VIDEO
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(f.absolutePath)
            if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes") {
                WallpaperKind.VIDEO
            } else {
                WallpaperKind.IMAGE
            }
        } catch (_: Throwable) {
            WallpaperKind.IMAGE
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun thumbFor(kind: WallpaperKind, f: File): ImageBitmap? = when (kind) {
        WallpaperKind.IMAGE, WallpaperKind.GIF -> decodeImage(f)
        WallpaperKind.VIDEO -> extractVideoFrame(f.absolutePath)
    }

    private fun decodeImage(f: File): ImageBitmap? {
        if (!f.exists()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= MAX_DIMENSION ||
                bounds.outHeight / (sample * 2) >= MAX_DIMENSION
            ) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(f.absolutePath, opts)?.asImageBitmap()
        }.getOrNull()
    }

    private fun extractVideoFrame(path: String): ImageBitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val frame = retriever.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            frame?.asImageBitmap()
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}

package nx.screen.ds.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class MusicData(
    val filePath: String,
    val ext: String?,
)

object MusicStore {

    private const val MAX_FILE_BYTES = 512L * 1024 * 1024

    @Volatile
    private var cache: MusicData? = null

    private fun file(context: Context): File = File(context.filesDir, "music.bin")

    private fun extFile(context: Context): File = File(context.filesDir, "music.ext")

    private fun readExt(context: Context): String? = runCatching {
        extFile(context).readText().trim().takeIf { it.isNotEmpty() }
    }.getOrNull()

    private fun writeExt(context: Context, ext: String?) {
        runCatching {
            if (ext.isNullOrBlank()) extFile(context).delete()
            else extFile(context).writeText(ext.trim())
        }
    }

    suspend fun load(context: Context): MusicData? = withContext(Dispatchers.IO) {
        if (cache == null) {
            val f = file(context)
            if (f.exists()) {
                cache = MusicData(f.absolutePath, readExt(context))
            }
        }
        cache
    }

    suspend fun save(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext false
            val ext = runCatching {
                val name = context.contentResolver
                    .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                name?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() && it.length <= 8 }
            }.getOrNull()?.lowercase()
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
                writeExt(context, ext)
                cache = MusicData(file(context).absolutePath, ext)
            }
            ok
        }.getOrDefault(false)
    }

    suspend fun clear(context: Context) {
        withContext(Dispatchers.IO) {
            file(context).delete()
            extFile(context).delete()
            cache = null
        }
    }
}
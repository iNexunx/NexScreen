package nx.screen.ds.data

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nx.screen.ds.R
import java.io.File

object FontStore {
    const val CUSTOM_FONT_FILE = "custom_font.ttf"

    private fun customFile(context: Context): File = File(context.filesDir, CUSTOM_FONT_FILE)

    fun fontFamily(context: Context, type: String, file: String?): FontFamily? = when (type) {
        "coming" -> FontFamily(Font(R.font.coming_soon, FontWeight.Normal))
        "mono" -> FontFamily(Font(R.font.cutive_mono, FontWeight.Normal))
        "gothic" -> FontFamily(Font(R.font.carrois_gothic, FontWeight.Normal))
        "condensed" -> systemFont("sans-serif-condensed")
        "light" -> systemFont("sans-serif-light")
        "thin" -> systemFont("sans-serif-thin")
        "medium" -> systemFont("sans-serif-medium")
        "black" -> systemFont("sans-serif-black")
        "rounded" -> systemFont("sans-serif-rounded")
        "serif" -> systemFont("serif")
        "monospace" -> systemFont("monospace")
        "cursive" -> systemFont("cursive")
        "casual" -> systemFont("casual")
        "smallcaps" -> systemFont("sans-serif-smallcaps")
        "custom" -> customFontFamily(context, file)
        else -> null
    }

    private fun systemFont(name: String): FontFamily? = runCatching {
        FontFamily(Typeface.create(name, Typeface.NORMAL))
    }.getOrNull()

    fun customFontFamily(context: Context, file: String?): FontFamily? {
        val name = file ?: return null
        val f = File(context.filesDir, name)
        if (!f.exists()) return null
        return runCatching { FontFamily(Font(f)) }.getOrNull()
    }

    suspend fun saveCustom(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val input = context.contentResolver.openInputStream(uri) ?: return@runCatching false
            input.use { src ->
                customFile(context).outputStream().use { src.copyTo(it) }
            }
            true
        }.getOrDefault(false)
    }

    fun hasCustom(context: Context): Boolean = customFile(context).exists()

    fun clearCustom(context: Context) {
        customFile(context).delete()
    }
}

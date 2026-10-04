package nx.screen.ds

import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

fun localeFor(language: String?): Locale =
    when (language) {
        "es" -> Locale("es")
        "en" -> Locale.ENGLISH
        "pt" -> Locale("pt", "BR")
        "fr" -> Locale.FRENCH
        "de" -> Locale.GERMAN
        "it" -> Locale.ITALIAN
        "ru" -> Locale("ru")
        "ar" -> Locale("ar")
        "hi" -> Locale("hi")
        "zh" -> Locale.CHINESE
        "ja" -> Locale.JAPANESE
        "ko" -> Locale.KOREAN
        "tr" -> Locale("tr")
        "pl" -> Locale("pl")
        "nl" -> Locale("nl")
        "id" -> Locale("id")
        else -> systemLocale()
    }

fun systemLocale(): Locale =
    if (android.os.Build.VERSION.SDK_INT >= 24) {
        Resources.getSystem().configuration.locales.get(0)
    } else {
        Resources.getSystem().configuration.locale
    }

@Suppress("DEPRECATION")
fun ContextWrapper.applyAppLocale(language: String?) {
    val locale = localeFor(language)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    if (android.os.Build.VERSION.SDK_INT >= 24) {
        config.setLocale(locale)
        config.setLocales(android.os.LocaleList(locale))
    } else {
        config.locale = locale
    }
    resources.updateConfiguration(config, null)
}
package nx.screen.ds.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK, DARK_PURE }

enum class UiSystem(val label: String) {
    MIUIX("Miuix"),
    MATERIAL("Material"),
}

enum class BackendMode(val label: String) {
    AUTO("Automático"),
    SHIZUKU("Shevery"),
    ROOT("Root"),
}

enum class UiStyle(val label: String) {
    TONAL_SPOT("Tonal Spot"),
    NEUTRAL("Neutral"),
    VIBRANT("Vibrante"),
    EXPRESSIVE("Expresivo"),
    RAINBOW("Arcoíris"),
    FRUIT_SALAD("Fruit Salad"),
    MONOCHROME("Monocromo"),
    FIDELITY("Fidelidad"),
    CONTENT("Content"),
}

enum class ColorSpec(val label: String) {
    C2021("2021"),
    C2025("2025"),
}

enum class ColorTheme(val label: String) {
    CLASSIC("Clásico"),
    CUSTOM("Personalizado"),
}

data class AppSettings(
    val uiSystem: UiSystem = UiSystem.MATERIAL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val accent: String? = null,
    val uiStyle: UiStyle = UiStyle.TONAL_SPOT,
    val colorSpec: ColorSpec = ColorSpec.C2025,
    val colorTheme: ColorTheme = ColorTheme.CLASSIC,
    val customWallpaper: Boolean = false,
    val glass: Boolean = false,
    val glassBlur: Float = 0.65f,
    val glassTransparency: Float = 0.65f,
    val glassGlow: Float = 0.65f,
    val fontScale: Float = 1f,
    val fontType: String = "system",
    val fontFile: String? = null,
    val backendMode: BackendMode = BackendMode.AUTO,
    val backendEnabled: Boolean = true,
    val monitoringEnabled: Boolean = false,
    val permissionPromptDone: Boolean = false,
    val language: String? = null,
    val alternateIcon: Boolean = false,
    val appTitle: String? = null,
    val appDpi: Int? = null,
    val amoled: Boolean = false,
    val batterySaver: Boolean = false,
    val floatingNavBar: Boolean = false,
    val videoVolume: Float = 1f,
    val musicVolume: Float = 1f,
    val musicPosition: Long = 0L,
    val autostartHandled: Boolean = false,
)

class SettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val storedMode = prefs[THEME_MODE]
        val legacyAmoled = storedMode == ThemeMode.DARK_PURE.name
        AppSettings(
            uiSystem = prefs[UI_SYSTEM]?.let {
                runCatching { UiSystem.valueOf(it) }.getOrDefault(UiSystem.MATERIAL)
            } ?: UiSystem.MATERIAL,
            themeMode = if (legacyAmoled) ThemeMode.DARK else prefs[THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
            } ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[DYNAMIC] ?: true,
            accent = prefs[ACCENT],
            uiStyle = prefs[UI_STYLE]?.let {
                runCatching { UiStyle.valueOf(it) }.getOrDefault(UiStyle.TONAL_SPOT)
            } ?: UiStyle.TONAL_SPOT,
            colorSpec = prefs[COLOR_SPEC]?.let {
                runCatching { ColorSpec.valueOf(it) }.getOrDefault(ColorSpec.C2025)
            } ?: ColorSpec.C2025,
            colorTheme = prefs[COLOR_THEME]?.let {
                runCatching { ColorTheme.valueOf(it) }.getOrDefault(ColorTheme.CLASSIC)
            } ?: ColorTheme.CLASSIC,
            customWallpaper = prefs[CUSTOM_WALLPAPER] ?: false,
            glass = prefs[GLASS] ?: false,
            glassBlur = if (prefs[GLASS_BLUR] == 0.5f || prefs[GLASS_BLUR] == 0.45f) 0.65f else (prefs[GLASS_BLUR] ?: prefs[GLASS_INTENSITY] ?: 0.65f).coerceIn(0f, 1f),
            glassTransparency = if (prefs[GLASS_TRANSPARENCY] == 0.5f) 0.65f else (prefs[GLASS_TRANSPARENCY] ?: 0.65f).coerceIn(0f, 1f),
            glassGlow = if (prefs[GLASS_GLOW] == 0.5f) 0.65f else (prefs[GLASS_GLOW] ?: 0.65f).coerceIn(0f, 1f),
            fontScale = (prefs[FONT_SCALE] ?: 1f).coerceIn(0.5f, 2f),
            fontType = prefs[FONT_TYPE] ?: "system",
            fontFile = prefs[FONT_FILE],
            backendMode = prefs[BACKEND_MODE]?.let {
                runCatching { BackendMode.valueOf(it) }.getOrDefault(BackendMode.AUTO)
            } ?: BackendMode.AUTO,
            backendEnabled = prefs[BACKEND_ENABLED] ?: true,
            monitoringEnabled = prefs[MONITORING_ENABLED] ?: false,
            permissionPromptDone = prefs[PERMISSION_PROMPT_DONE] ?: false,
            language = prefs[LANGUAGE],
            alternateIcon = prefs[ALTERNATE_ICON] ?: false,
            appTitle = prefs[APP_TITLE],
            appDpi = prefs[APP_DPI],
            amoled = prefs[AMOLED] ?: legacyAmoled,
            batterySaver = prefs[BATTERY_SAVER] ?: false,
            floatingNavBar = prefs[FLOATING_NAV_BAR] ?: false,
            videoVolume = (prefs[VIDEO_VOLUME] ?: 1f).coerceIn(0f, 1f),
            musicVolume = (prefs[MUSIC_VOLUME] ?: 1f).coerceIn(0f, 1f),
            musicPosition = (prefs[MUSIC_POSITION] ?: 0L).coerceAtLeast(0L),
            autostartHandled = prefs[AUTOSTART_HANDLED] ?: false,
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(settings.first())
        context.settingsDataStore.edit { prefs ->
            prefs[UI_SYSTEM] = next.uiSystem.name
            prefs[THEME_MODE] = next.themeMode.name
            prefs[DYNAMIC] = next.dynamicColor
            if (next.accent != null) prefs[ACCENT] = next.accent else prefs.remove(ACCENT)
            prefs[UI_STYLE] = next.uiStyle.name
            prefs[COLOR_SPEC] = next.colorSpec.name
            prefs[COLOR_THEME] = next.colorTheme.name
            prefs[CUSTOM_WALLPAPER] = next.customWallpaper
            prefs[GLASS] = next.glass
            prefs[GLASS_BLUR] = next.glassBlur.coerceIn(0f, 1f)
            prefs[GLASS_TRANSPARENCY] = next.glassTransparency.coerceIn(0f, 1f)
            prefs[GLASS_GLOW] = next.glassGlow.coerceIn(0f, 1f)
            prefs.remove(GLASS_INTENSITY)
            prefs[FONT_SCALE] = next.fontScale.coerceIn(0.5f, 2f)
            prefs[FONT_TYPE] = next.fontType
            if (next.fontFile != null) prefs[FONT_FILE] = next.fontFile else prefs.remove(FONT_FILE)
            prefs[BACKEND_MODE] = next.backendMode.name
            prefs[BACKEND_ENABLED] = next.backendEnabled
            prefs[MONITORING_ENABLED] = next.monitoringEnabled
            prefs[PERMISSION_PROMPT_DONE] = next.permissionPromptDone
            if (next.language != null) prefs[LANGUAGE] = next.language else prefs.remove(LANGUAGE)
            prefs[ALTERNATE_ICON] = next.alternateIcon
            if (next.appTitle != null) prefs[APP_TITLE] = next.appTitle else prefs.remove(APP_TITLE)
            if (next.appDpi != null) prefs[APP_DPI] = next.appDpi else prefs.remove(APP_DPI)
            prefs[AMOLED] = next.amoled
            prefs[BATTERY_SAVER] = next.batterySaver
            prefs[FLOATING_NAV_BAR] = next.floatingNavBar
            prefs[VIDEO_VOLUME] = next.videoVolume.coerceIn(0f, 1f)
            prefs[MUSIC_VOLUME] = next.musicVolume.coerceIn(0f, 1f)
            prefs[MUSIC_POSITION] = next.musicPosition.coerceAtLeast(0L)
            prefs[AUTOSTART_HANDLED] = next.autostartHandled
        }
    }

    suspend fun resetToDefaults() = update { AppSettings() }

    companion object {
        private val UI_SYSTEM = stringPreferencesKey("ui_system")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val DYNAMIC = booleanPreferencesKey("dynamic_color")
        private val ACCENT = stringPreferencesKey("accent")
        private val UI_STYLE = stringPreferencesKey("ui_style")
        private val COLOR_SPEC = stringPreferencesKey("color_spec")
        private val COLOR_THEME = stringPreferencesKey("color_theme")
        private val CUSTOM_WALLPAPER = booleanPreferencesKey("custom_wallpaper")
        private val GLASS = booleanPreferencesKey("glass")
        private val GLASS_BLUR = floatPreferencesKey("glass_blur")
        private val GLASS_TRANSPARENCY = floatPreferencesKey("glass_transparency")
        private val GLASS_GLOW = floatPreferencesKey("glass_glow")
        private val GLASS_INTENSITY = floatPreferencesKey("glass_intensity")
        private val FONT_SCALE = floatPreferencesKey("font_scale")
        private val FONT_TYPE = stringPreferencesKey("font_type")
        private val FONT_FILE = stringPreferencesKey("font_file")
        private val BACKEND_MODE = stringPreferencesKey("backend_mode")
        private val BACKEND_ENABLED = booleanPreferencesKey("backend_enabled")
        private val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        private val PERMISSION_PROMPT_DONE = booleanPreferencesKey("permission_prompt_done")
        private val LANGUAGE = stringPreferencesKey("language")
        private val ALTERNATE_ICON = booleanPreferencesKey("alternate_icon")
        private val APP_TITLE = stringPreferencesKey("app_title")
        private val APP_DPI = intPreferencesKey("app_dpi")
        private val AMOLED = booleanPreferencesKey("amoled")
        private val BATTERY_SAVER = booleanPreferencesKey("battery_saver")
        private val FLOATING_NAV_BAR = booleanPreferencesKey("floating_nav_bar")
        private val VIDEO_VOLUME = floatPreferencesKey("video_volume")
        private val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        private val MUSIC_POSITION = longPreferencesKey("music_position")
        private val AUTOSTART_HANDLED = booleanPreferencesKey("autostart_handled")
    }
}

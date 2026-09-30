package nx.screen.ds.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import nx.screen.ds.MainActivity
import nx.screen.ds.R
import nx.screen.ds.core.ProfileService
import nx.screen.ds.data.AppProfileStore
import nx.screen.ds.data.AppSettings
import nx.screen.ds.data.ColorSpec
import nx.screen.ds.data.ColorTheme
import nx.screen.ds.data.FontStore
import nx.screen.ds.data.MusicData
import nx.screen.ds.data.MusicStore
import nx.screen.ds.data.SettingsStore
import nx.screen.ds.data.ThemeMode
import nx.screen.ds.data.UiStyle
import nx.screen.ds.data.UiSystem
import nx.screen.ds.data.WallpaperData
import nx.screen.ds.data.WallpaperKind
import nx.screen.ds.data.WallpaperStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nx.screen.ds.core.VideoTrimmer
import java.io.File

data class AppTitlePreset(val key: String?, val labelRes: Int)

val APP_TITLE_PRESETS = listOf(
    AppTitlePreset(null, R.string.title_preset_default),
    AppTitlePreset("Pro", R.string.title_preset_pro),
    AppTitlePreset("Plus", R.string.title_preset_plus),
    AppTitlePreset("Ultra", R.string.title_preset_ultra),
    AppTitlePreset("Max", R.string.title_preset_max),
    AppTitlePreset("X", R.string.title_preset_x),
)

private val VALID_TITLE_KEYS = APP_TITLE_PRESETS.mapNotNull { it.key }.toSet()

private val LAUNCHER_COMPONENTS = listOf(
    "MainActivity",
    "MainActivityAlt",
    "MainActivityTitlePro",
    "MainActivityTitleProAlt",
    "MainActivityTitlePlus",
    "MainActivityTitlePlusAlt",
    "MainActivityTitleUltra",
    "MainActivityTitleUltraAlt",
    "MainActivityTitleMax",
    "MainActivityTitleMaxAlt",
    "MainActivityTitleX",
    "MainActivityTitleXAlt",
)

fun appTitleLabelRes(key: String?): Int? =
    APP_TITLE_PRESETS.firstOrNull { it.key == key }?.labelRes

fun isBatteryExempt(context: Context): Boolean = runCatching {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return@runCatching false
    pm.isIgnoringBatteryOptimizations(context.packageName)
}.getOrDefault(false)

fun openBatteryExemptSettings(context: Context) {
    val pm = context.packageManager
    val candidates = buildList {
        add(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${context.packageName}")),
        )
        add(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        addAll(brandBatteryIntents(pm))
    }
    val target = candidates.firstOrNull {
        runCatching { pm.resolveActivity(it, 0) != null }.getOrDefault(false)
    } ?: Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    runCatching {
        context.startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Pantallas de batería/autostart específicas por fabricante, en orden de prioridad. */
private fun brandBatteryIntents(pm: PackageManager): List<Intent> {
    val brand = Build.MANUFACTURER.lowercase()
    fun launcher(pkg: String): Intent? = pm.getLaunchIntentForPackage(pkg)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return when {
        brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("pooc") -> listOf(
            Intent("miui.intent.action.OP_BACKGROUND_STRT_BTN")
                .setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            Intent("miui.intent.action.OP_BATTERY_SAVER_OPTION")
                .setClassName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings"),
            launcher("com.miui.securitycenter"),
        ).filterNotNull()
        brand.contains("samsung") -> listOf(
            Intent().setClassName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
            launcher("com.samsung.android.lool"),
        ).filterNotNull()
        brand.contains("oppo") || brand.contains("realme") -> listOfNotNull(
            launcher("com.coloros.safecenter"),
            launcher("com.coloros.oppoguardelf"),
        )
        brand.contains("oneplus") -> listOfNotNull(
            launcher("com.oneplus.security"),
            launcher("com.coloros.safecenter"),
        )
        brand.contains("vivo") || brand.contains("iqoo") -> listOfNotNull(
            launcher("com.vivo.permissionmanager"),
            launcher("com.iqoo.secure"),
        )
        brand.contains("huawei") || brand.contains("honor") -> listOfNotNull(
            launcher("com.huawei.systemmanager"),
            launcher("com.hihonor.systemmanager"),
        )
        brand.contains("infinix") || brand.contains("tecno") || brand.contains("itel") -> listOfNotNull(
            launcher("com.transsion.phonemanager"),
        )
        else -> emptyList()
    }
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)

    val settings: StateFlow<AppSettings> = store.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings(),
    )

    val wallpaper: StateFlow<WallpaperData?> = sharedWallpaper
    val music: StateFlow<MusicData?> = sharedMusic
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded
    private val _message = MutableStateFlow<Int?>(null)
    val message: StateFlow<Int?> = _message

    init {
        viewModelScope.launch {
            try {
                store.settings.first()
                if (sharedWallpaper.value == null) {
                    sharedWallpaper.value = WallpaperStore.load(getApplication())
                }
                if (sharedMusic.value == null) {
                    sharedMusic.value = MusicStore.load(getApplication())
                }
            } catch (_: Throwable) {
            }
            try {
                applyLauncherEntry()
            } finally {
                _loaded.value = true
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun setUiSystem(system: UiSystem) = viewModelScope.launch {
        store.update { it.copy(uiSystem = system) }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        store.update { it.copy(themeMode = mode) }
    }

    fun setAccent(hex: String?) = viewModelScope.launch {
        store.update { it.copy(dynamicColor = hex == null, accent = hex) }
    }

    fun setUiStyle(style: UiStyle) = viewModelScope.launch {
        store.update { it.copy(uiStyle = style) }
    }

    fun setColorSpec(spec: ColorSpec) = viewModelScope.launch {
        store.update { it.copy(colorSpec = spec) }
    }

    fun setColorTheme(theme: ColorTheme) = viewModelScope.launch {
        store.update { it.copy(colorTheme = theme) }
    }

    fun setGlass(enabled: Boolean) = viewModelScope.launch {
        store.update { it.copy(glass = enabled) }
    }

    fun setGlassBlur(value: Float) = viewModelScope.launch {
        store.update { it.copy(glassBlur = value.coerceIn(0f, 1f)) }
    }

    fun setGlassTransparency(value: Float) = viewModelScope.launch {
        store.update { it.copy(glassTransparency = value.coerceIn(0f, 1f)) }
    }

    fun setGlassGlow(value: Float) = viewModelScope.launch {
        store.update { it.copy(glassGlow = value.coerceIn(0f, 1f)) }
    }

    fun setVideoVolume(value: Float) = viewModelScope.launch {
        store.update { it.copy(videoVolume = value.coerceIn(0f, 1f)) }
    }

    fun setMusicVolume(value: Float) = viewModelScope.launch {
        store.update { it.copy(musicVolume = value.coerceIn(0f, 1f)) }
    }

    fun setAutostartHandled(handled: Boolean) = viewModelScope.launch {
        store.update { it.copy(autostartHandled = handled) }
    }

    fun setAlternateIcon(enabled: Boolean) = viewModelScope.launch {
        store.update { it.copy(alternateIcon = enabled) }
        applyLauncherEntry()
        restartApp()
    }

    fun setAppTitle(key: String?) = viewModelScope.launch {
        val valid = key?.takeIf { it in VALID_TITLE_KEYS }
        store.update { it.copy(appTitle = valid) }
        applyLauncherEntry()
        restartApp()
    }

    private fun launcherTarget(titleKey: String?, alt: Boolean): String = when {
        titleKey == null && !alt -> "MainActivity"
        titleKey == null && alt -> "MainActivityAlt"
        else -> "MainActivityTitle$titleKey${if (alt) "Alt" else ""}"
    }

    private suspend fun restartApp() {
        withContext(Dispatchers.Main) {
            runCatching { MainActivity.current?.finishAffinity() }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                android.os.Process.killProcess(android.os.Process.myPid())
            }, 150)
        }
    }

    private suspend fun applyLauncherEntry() {
        val current = store.settings.first()
        val pkg = getApplication<Application>().packageName
        val pm = getApplication<Application>().packageManager
        val titleKey = current.appTitle?.takeIf { it in VALID_TITLE_KEYS }
        val alt = current.alternateIcon
        val target = launcherTarget(titleKey, alt)
        withContext(Dispatchers.IO) {
            LAUNCHER_COMPONENTS.forEach { name ->
                runCatching {
                    pm.setComponentEnabledSetting(
                        ComponentName(pkg, "$pkg.$name"),
                        if (name == target) {
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        } else {
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        },
                        PackageManager.DONT_KILL_APP,
                    )
                }
            }
        }
    }

    fun setAppDpi(dpi: Int?) = viewModelScope.launch {
        store.update { it.copy(appDpi = dpi?.coerceIn(APP_DPI_MIN, APP_DPI_MAX)) }
    }

    fun setAmoled(enabled: Boolean) = viewModelScope.launch {
        store.update { it.copy(amoled = enabled) }
    }

    fun setFontScale(value: Float) = viewModelScope.launch {
        store.update { it.copy(fontScale = value.coerceIn(0.5f, 2f)) }
    }

    fun setFontType(type: String) = viewModelScope.launch {
        store.update { it.copy(fontType = type) }
    }

    fun saveCustomFont(uri: Uri) {
        viewModelScope.launch {
            val ok = FontStore.saveCustom(getApplication(), uri)
            if (ok) {
                store.update {
                    it.copy(fontType = "custom", fontFile = FontStore.CUSTOM_FONT_FILE)
                }
            }
        }
    }

    fun removeCustomFont() {
        viewModelScope.launch {
            FontStore.clearCustom(getApplication())
            store.update { it.copy(fontType = "system", fontFile = null) }
        }
    }

    fun setBackendMode(mode: nx.screen.ds.data.BackendMode) = viewModelScope.launch {
        store.update { it.copy(backendMode = mode) }
    }

    fun setBackendEnabled(enabled: Boolean) = viewModelScope.launch {
        store.update { it.copy(backendEnabled = enabled) }
    }

    fun setLanguage(language: String?) = viewModelScope.launch {
        store.update { it.copy(language = language) }
    }

    fun setBatterySaver(enabled: Boolean) = viewModelScope.launch {
        store.update { it.copy(batterySaver = enabled) }
    }

    fun setFloatingNavBar(enabled: Boolean) = viewModelScope.launch {
        store.update { it.copy(floatingNavBar = enabled) }
    }

    fun resetSettings() = viewModelScope.launch {
        store.update {
            it.copy(
                uiSystem = UiSystem.MATERIAL,
                themeMode = ThemeMode.SYSTEM,
                dynamicColor = true,
                accent = null,
                uiStyle = UiStyle.TONAL_SPOT,
                colorSpec = ColorSpec.C2025,
                colorTheme = ColorTheme.CLASSIC,
                glass = false,
                glassBlur = 0.65f,
                glassTransparency = 0.65f,
                glassGlow = 0.65f,
                fontScale = 1f,
                fontType = "system",
                fontFile = null,
                backendMode = nx.screen.ds.data.BackendMode.AUTO,
                backendEnabled = true,
                language = null,
                alternateIcon = false,
                appTitle = null,
                appDpi = null,
                amoled = false,
                floatingNavBar = false,
            )
        }
        applyLauncherEntry()
    }

    fun resetGlass() = viewModelScope.launch {
        store.update {
            it.copy(glassBlur = 0.65f, glassTransparency = 0.65f, glassGlow = 0.65f)
        }
    }

    fun resetTheme() = viewModelScope.launch {
        WallpaperStore.clear(getApplication())
        MusicStore.clear(getApplication())
        FontStore.clearCustom(getApplication())
        sharedWallpaper.value = null
        sharedMusic.value = null
        store.update {
            it.copy(
                themeMode = ThemeMode.SYSTEM,
                dynamicColor = true,
                accent = null,
                uiStyle = UiStyle.TONAL_SPOT,
                colorSpec = ColorSpec.C2025,
                colorTheme = ColorTheme.CLASSIC,
                customWallpaper = false,
                glass = false,
                glassBlur = 0.65f,
                glassTransparency = 0.65f,
                glassGlow = 0.65f,
                fontScale = 1f,
                fontType = "system",
                fontFile = null,
                amoled = false,
            )
        }
    }

    fun resetFontScale() = viewModelScope.launch {
        store.update { it.copy(fontScale = 1f) }
    }

    fun saveWallpaper(uri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val limitMs = 30_000L
            val isAudio = runCatching {
                app.contentResolver.getType(uri)?.startsWith("audio/") == true
            }.getOrDefault(false)
            if (isAudio) {
                _message.value = R.string.file_not_supported
                return@launch
            }
            val duration = withContext(Dispatchers.IO) { VideoTrimmer.durationMs(app, uri) }
            val needsTrim = !isAudio && duration > limitMs
            var ok = false
            if (needsTrim) {
                ok = withContext(Dispatchers.IO) {
                    val tmp = File(app.cacheDir, "trim_auto_${System.currentTimeMillis()}.mp4")
                    val trimmed = runCatching {
                        VideoTrimmer.trim(app, uri, 0L, limitMs, tmp.absolutePath)
                    }.getOrDefault(false)
                    if (!trimmed) {
                        Log.e(TAG, "saveWallpaper: trim() fallo, duracion=$duration")
                    }
                    val lenOk = trimmed && tmp.length() > 1024
                    if (trimmed && !lenOk) {
                        Log.e(TAG, "saveWallpaper: salida demasiado pequena ${tmp.length()}")
                    }
                    val durOk = lenOk &&
                        VideoTrimmer.durationMs(app, Uri.fromFile(tmp)) in 1L..limitMs
                    if (lenOk && !durOk) {
                        Log.e(TAG, "saveWallpaper: duracion del recorte fuera de rango")
                    }
                    val saved = durOk && runCatching { WallpaperStore.saveFile(app, tmp) }
                        .getOrDefault(false)
                    if (durOk && !saved) {
                        Log.e(TAG, "saveWallpaper: saveFile() fallo")
                    }
                    tmp.delete()
                    saved
                }
            }
            if (!ok) {
                Log.e(TAG, "saveWallpaper: recorte no aplicado (needsTrim=$needsTrim), guardando original")
                ok = WallpaperStore.save(app, uri)
            }
            if (ok) {
                sharedWallpaper.value = WallpaperStore.load(app)
                store.update { it.copy(customWallpaper = true) }
            } else {
                Log.e(TAG, "saveWallpaper: el guardado directo tambien fallo")
                _message.value = R.string.trim_failed
            }
        }
    }

    fun clearWallpaper() {
        viewModelScope.launch {
            WallpaperStore.clear(getApplication())
            sharedWallpaper.value = null
            store.update { it.copy(customWallpaper = false) }
        }
    }

    fun saveMusic(uri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val ok = withContext(Dispatchers.IO) {
                val isAudio = runCatching {
                    app.contentResolver.getType(uri)?.startsWith("audio/") == true
                }.getOrDefault(false)
                isAudio && MusicStore.save(app, uri)
            }
            if (ok) {
                sharedMusic.value = MusicStore.load(app)
                store.update { it.copy(musicPosition = 0L) }
            } else {
                _message.value = R.string.trim_failed
            }
        }
    }

    fun saveMusicPosition(position: Long) {
        if (position <= 0L) return
        viewModelScope.launch {
            store.update { it.copy(musicPosition = position) }
        }
    }

    fun clearMusic() {
        viewModelScope.launch {
            MusicStore.clear(getApplication())
            sharedMusic.value = null
        }
    }

    fun clearAppData() {
        viewModelScope.launch {
            getApplication<Application>()
                .stopService(Intent(getApplication(), ProfileService::class.java))
            FontStore.clearCustom(getApplication())
            WallpaperStore.clear(getApplication())
            MusicStore.clear(getApplication())
            AppProfileStore(getApplication()).clearAll()
            sharedWallpaper.value = null
            sharedMusic.value = null
            store.resetToDefaults()
            applyLauncherEntry()
        }
    }

    companion object {
        private const val TAG = "SettingsVM"
        const val APP_DPI_MIN = 120
        const val APP_DPI_MAX = 640
        internal val sharedWallpaper = MutableStateFlow<WallpaperData?>(null)
        internal val sharedMusic = MutableStateFlow<MusicData?>(null)
    }
}

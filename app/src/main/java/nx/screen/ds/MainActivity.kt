package nx.screen.ds

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nx.screen.ds.core.ProfileService
import nx.screen.ds.data.SettingsStore
import nx.screen.ds.data.ThemeMode
import nx.screen.ds.ui.AppPickers
import nx.screen.ds.ui.LocalAppPickers
import nx.screen.ds.ui.LocalScaledInsets
import nx.screen.ds.ui.ScaledInsets
import nx.screen.ds.ui.SettingsViewModel
import nx.screen.ds.ui.theme.ScreenDensityTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    companion object {
        @Volatile
        var current: MainActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        current = this
        applyEdgeToEdgeForTheme()
        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            val loaded by settingsViewModel.loaded.collectAsState()
            val wallpaper by settingsViewModel.wallpaper.collectAsState()
            val localizedContext = remember(settings.language) { localizedContext(settings.language) }
            val baseDensity = LocalDensity.current
            val wallpaperSeed = remember(settings.accent, wallpaper?.image) {
                if (settings.accent == null) nx.screen.ds.ui.theme.dominantColorOf(wallpaper?.image) else null
            }
            val wallpaperLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri != null) {
                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                    settingsViewModel.saveWallpaper(uri)
                }
            }
            val wallpaperMime = remember { arrayOf("image/*", "video/*") }
            val fontLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent(),
            ) { uri ->
                if (uri != null) settingsViewModel.saveCustomFont(uri)
            }
            val musicLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri != null) {
                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                    settingsViewModel.saveMusic(uri)
                }
            }
            if (!loaded) {
                Box(modifier = Modifier.fillMaxSize())
                return@setContent
            }
            DefaultPermissionPrompt()
            val appDpi = settings.appDpi
            val densityScale = if (appDpi != null && appDpi > 0) {
                (appDpi / 160f) / baseDensity.density
            } else {
                1f
            }
            val scaledInsets = remember(appDpi, baseDensity.density) {
                if (appDpi != null && appDpi > 0) ScaledInsets(densityScale) else null
            }
            CompositionLocalProvider(
                LocalAppPickers provides AppPickers(wallpaperLauncher, fontLauncher, musicLauncher, wallpaperMime),
                LocalContext provides localizedContext,
                LocalScaledInsets provides scaledInsets,
                LocalDensity provides Density(
                    density = appDpi?.let { it / 160f } ?: baseDensity.density,
                    fontScale = baseDensity.fontScale * settings.fontScale.coerceIn(0.5f, 2f),
                ),
            ) {
                ScreenDensityTheme(settings, wallpaperSeed = wallpaperSeed) {
                    ScreenDensityApp(settings, wallpaper, activity = this@MainActivity)
                }
            }
        }
    }

    private fun applyEdgeToEdgeForTheme() {
        val themeMode = runCatching {
            runBlocking { SettingsStore(applicationContext).settings.first().themeMode }
        }.getOrDefault(ThemeMode.SYSTEM)
        val dark = when (themeMode) {
            ThemeMode.DARK, ThemeMode.DARK_PURE -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> {
                val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                mode == Configuration.UI_MODE_NIGHT_YES
            }
        }
        runCatching {
            window.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(
                    if (dark) 0xFF0B1220.toInt() else 0xFFFFFFFF.toInt(),
                ),
            )
        }
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ) { dark },
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ) { dark },
        )
    }

    private fun localizedContext(language: String?): Context {
        val locale = localeFor(language)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            config.setLocale(locale)
            config.setLocales(android.os.LocaleList(locale))
        } else {
            config.locale = locale
        }
        return createConfigurationContext(config)
    }
}

@androidx.compose.runtime.Composable
private fun DefaultPermissionPrompt() {
    val context = LocalContext.current
    val store = remember { SettingsStore(context.applicationContext) }
    var done by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        done = true
    }

    LaunchedEffect(Unit) {
        val settings = store.settings.first()
        if (settings.permissionPromptDone) return@LaunchedEffect
        delay(800)

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                while (!done) delay(200)
            }
        }

        if (!ProfileService.hasUsageAccess(context.applicationContext)) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            delay(1500)
        }

        store.update { it.copy(permissionPromptDone = true) }
    }
}

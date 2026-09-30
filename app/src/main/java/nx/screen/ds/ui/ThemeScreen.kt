package nx.screen.ds.ui

import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import nx.screen.ds.R
import nx.screen.ds.data.AppSettings
import nx.screen.ds.data.ColorSpec
import nx.screen.ds.data.ColorTheme
import nx.screen.ds.data.ThemeMode
import nx.screen.ds.data.UiStyle
import nx.screen.ds.data.UiSystem
import nx.screen.ds.data.WallpaperKind
import nx.screen.ds.ui.component.SelectableTile
import nx.screen.ds.ui.component.SettingRowHeader
import nx.screen.ds.ui.component.SplicedColumnGroup
import nx.screen.ds.ui.component.ThemeColorOption
import nx.screen.ds.ui.component.ThemeColorPicker
import nx.screen.ds.ui.component.ThemeModeSelector
import nx.screen.ds.ui.component.ToggleSettingCard
import nx.screen.ds.ui.theme.AppFilterChip
import nx.screen.ds.ui.theme.AppSlider
import nx.screen.ds.ui.theme.AppTextButton
import nx.screen.ds.ui.theme.colorFromHex
import nx.screen.ds.ui.theme.DarkModeIcon
import nx.screen.ds.ui.theme.DeleteIcon
import nx.screen.ds.ui.theme.FontIcon
import nx.screen.ds.ui.theme.FolderIcon
import nx.screen.ds.ui.theme.GlassIcon
import nx.screen.ds.ui.theme.M3eIcon
import nx.screen.ds.ui.theme.Md3Icon
import nx.screen.ds.ui.theme.MusicIcon
import nx.screen.ds.ui.theme.PaletteIcon
import nx.screen.ds.ui.theme.WallpaperIcon

class AppPickers(
    val wallpaper: ActivityResultLauncher<Array<String>>,
    val font: ActivityResultLauncher<String>,
    val music: ActivityResultLauncher<Array<String>>? = null,
    val wallpaperMime: Array<String>? = null,
)

val LocalAppPickers = staticCompositionLocalOf<AppPickers?> { null }

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        AppTextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 64.dp),
        )
    }
}

private enum class ThemeReset { WALLPAPER, ALL }

@Composable
fun ThemeScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var confirmReset by remember { mutableStateOf<ThemeReset?>(null) }

    val context = LocalContext.current
    var snackText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        val msg = message
        if (msg != null) {
            snackText = context.getString(msg)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(snackText) {
        val text = snackText
        if (text != null) {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
            snackText = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Transparent),
    ) {
        KeyboardScrollColumn(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = stringResource(R.string.settings_theme), onBack = onBack)

            SplicedColumnGroup {
                item(key = "mode", visible = !settings.amoled) {
                    ThemeModeSelector(
                        selectedMode = settings.themeMode,
                        onModeSelected = viewModel::setThemeMode,
                        flat = true,
                        title = stringResource(R.string.theme_mode),
                    )
                }

                item(key = "colors", visible = settings.uiSystem == UiSystem.MATERIAL) {
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    ThemeColorPicker(
                        selectedColorKey = ACCENT_PRESETS.firstOrNull { it.hex == settings.accent }?.key ?: "",
                        onColorSelected = { key ->
                            ACCENT_PRESETS.firstOrNull { it.key == key }?.hex?.let(viewModel::setAccent)
                        },
                        isDarkTheme = isDark,
                        isDynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        isDynamicColorEnabled = settings.dynamicColor && settings.accent == null,
                        onDynamicColorSelected = { viewModel.setAccent(null) },
                        titleRes = R.string.colors_personalizados,
                        options = accentColorOptions(),
                        flat = true,
                    )
                }

                item(key = "palette", visible = settings.uiSystem == UiSystem.MATERIAL) {
                    PaletteItem(settings = settings, viewModel = viewModel)
                }

                if (settings.colorTheme == ColorTheme.CUSTOM && settings.uiSystem == UiSystem.MATERIAL) {
                    item(key = "spec") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                ColorSpec.entries.forEach { spec ->
                                    SpecButton(
                                        icon = if (spec == ColorSpec.C2021) Md3Icon else M3eIcon,
                                        label = stringResource(specLabelRes(spec)),
                                        selected = settings.colorSpec == spec,
                                        onClick = { viewModel.setColorSpec(spec) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }

                    item(key = "styles") {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            UiStyle.entries.forEach { style ->
                                AppFilterChip(
                                    selected = settings.uiStyle == style,
                                    onClick = { viewModel.setUiStyle(style) },
                                    label = { Text(styleLabel(style), maxLines = 1) },
                                )
                            }
                        }
                    }
                }

                item(key = "amoled") {
                    ToggleSettingCard(
                        title = stringResource(R.string.theme_amoled),
                        description = stringResource(R.string.theme_amoled_desc),
                        checked = settings.amoled,
                        icon = DarkModeIcon,
                        onCheckedChange = viewModel::setAmoled,
                    )
                }
            }

            SplicedColumnGroup {
                item(key = "wallpaper") {
                    WallpaperItem(settings = settings, viewModel = viewModel, onRemove = { confirmReset = ThemeReset.WALLPAPER })
                }

                item(key = "music") {
                    MusicItem(settings = settings, viewModel = viewModel, onRemove = { viewModel.clearMusic() })
                }

                item(key = "font") {
                    FontItem(settings = settings, viewModel = viewModel)
                }

                item(key = "glass") {
                    GlassItem(settings = settings, viewModel = viewModel)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AppTextButton(onClick = { confirmReset = ThemeReset.ALL }) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(R.string.reset_theme),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    confirmReset?.let { kind ->
        AlertDialog(
            onDismissRequest = { confirmReset = null },
            title = {
                Text(
                    when (kind) {
                        ThemeReset.ALL -> stringResource(R.string.reset_theme)
                        else -> stringResource(R.string.remove)
                    },
                )
            },
            text = {
                Text(
                    when (kind) {
                        ThemeReset.ALL -> stringResource(R.string.confirm_reset_theme)
                        else -> stringResource(R.string.confirm_remove_wallpaper)
                    },
                )
            },
            confirmButton = {
                AppTextButton(onClick = {
                    confirmReset = null
                    when (kind) {
                        ThemeReset.WALLPAPER -> viewModel.clearWallpaper()
                        ThemeReset.ALL -> viewModel.resetTheme()
                    }
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                AppTextButton(onClick = { confirmReset = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun PaletteItem(settings: AppSettings, viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingRowHeader(
            icon = PaletteIcon,
            title = stringResource(R.string.colors_palette),
            description = "",
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ColorTheme.entries.forEach { theme ->
                SelectableTile(
                    icon = if (theme == ColorTheme.CLASSIC) Icons.Filled.ColorLens else Icons.Filled.Tune,
                    label = stringResource(themeLabelRes(theme)),
                    isSelected = settings.colorTheme == theme,
                    onClick = { viewModel.setColorTheme(theme) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SpecButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = container,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = content,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun themeLabelRes(theme: ColorTheme): Int = when (theme) {
    ColorTheme.CLASSIC -> R.string.colors_classic
    ColorTheme.CUSTOM -> R.string.colors_custom
}

private fun specLabelRes(spec: ColorSpec): Int = when (spec) {
    ColorSpec.C2021 -> R.string.colors_md3_2021
    ColorSpec.C2025 -> R.string.colors_m3e_2025
}

@Composable
private fun styleLabel(style: UiStyle): String = when (style) {
    UiStyle.TONAL_SPOT -> stringResource(R.string.style_tonal_spot)
    UiStyle.NEUTRAL -> stringResource(R.string.style_neutral)
    UiStyle.VIBRANT -> stringResource(R.string.style_vibrant)
    UiStyle.EXPRESSIVE -> stringResource(R.string.style_expressive)
    UiStyle.RAINBOW -> stringResource(R.string.style_rainbow)
    UiStyle.FRUIT_SALAD -> stringResource(R.string.style_fruit_salad)
    UiStyle.MONOCHROME -> stringResource(R.string.style_monochrome)
    UiStyle.FIDELITY -> stringResource(R.string.style_fidelity)
    UiStyle.CONTENT -> stringResource(R.string.style_content)
}

private data class AccentPreset(
    val key: String,
    val hex: String,
    val pairHex: String,
)

private val ACCENT_PRESETS = listOf(
    AccentPreset("azul", "3B82F6", "1D4ED8"),
    AccentPreset("violeta", "8B5CF6", "6D28D9"),
    AccentPreset("naranja", "F97316", "C2410C"),
    AccentPreset("verde", "22C55E", "15803D"),
    AccentPreset("rojo", "EF4444", "B91C1C"),
    AccentPreset("rosa", "EC4899", "BE185D"),
    AccentPreset("cian", "06B6D4", "0E7490"),
    AccentPreset("indigo", "6366F1", "4338CA"),
    AccentPreset("oro", "F59E0B", "B45309"),
    AccentPreset("menta", "34D399", "0F766E"),
    AccentPreset("esmeralda", "10B981", "047857"),
    AccentPreset("fucsia", "D946EF", "A21CAF"),
)

private fun accentLabelRes(key: String): Int = when (key) {
    "azul" -> R.string.color_azul
    "violeta" -> R.string.color_violeta
    "naranja" -> R.string.color_naranja
    "verde" -> R.string.color_verde
    "rojo" -> R.string.color_rojo
    "rosa" -> R.string.color_rosa
    "cian" -> R.string.color_cian
    "indigo" -> R.string.color_indigo
    "oro" -> R.string.color_oro
    "menta" -> R.string.color_menta
    "esmeralda" -> R.string.color_esmeralda
    "fucsia" -> R.string.color_fucsia
    else -> R.string.color_azul
}

@Composable
private fun accentColorOptions(): List<ThemeColorOption> = remember {
    ACCENT_PRESETS.map { preset ->
        val c = colorFromHex(preset.hex) ?: Color(0xFF3B82F6)
        ThemeColorOption(preset.key, accentLabelRes(preset.key), c, c)
    }
}

private fun Float.safeClamp(min: Float, max: Float, fallback: Float): Float =
    if (isNaN() || isInfinite()) fallback else coerceIn(min, max)

@Composable
private fun WallpaperItem(settings: AppSettings, viewModel: SettingsViewModel, onRemove: () -> Unit) {
    val wallpaper by viewModel.wallpaper.collectAsState()
    val wp = wallpaper
    val pickers = LocalAppPickers.current
    val music by viewModel.music.collectAsState()
    val pick: () -> Unit = { pickers?.wallpaper?.launch(pickers.wallpaperMime ?: arrayOf("image/*", "video/*")) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingRowHeader(
            icon = WallpaperIcon,
            title = stringResource(R.string.wallpaper),
            description = "",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = pick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            val thumb = wallpaper?.image
            if (thumb != null) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))) {
                    Image(
                        bitmap = thumb,
                        contentDescription = stringResource(R.string.wallpaper_chosen),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        shape = RoundedCornerShape(4.dp),
                        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f),
                    ) {
                        Text(
                            stringResource(
                                when (wallpaper?.kind) {
                                    WallpaperKind.VIDEO -> R.string.wallpaper_type_video
                                    WallpaperKind.GIF -> R.string.wallpaper_type_gif
                                    else -> R.string.wallpaper_type_image
                                },
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            } else if (wp != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (wp != null) stringResource(R.string.wallpaper_has)
                    else stringResource(R.string.wallpaper_pick),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    if (wallpaper != null) stringResource(R.string.wallpaper_change)
                    else stringResource(R.string.wallpaper_visible),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (wallpaper?.kind == WallpaperKind.VIDEO) {
            GlassSliderRow(
                label = stringResource(R.string.wallpaper_volume),
                value = settings.videoVolume.safeClamp(0f, 1f, 1f),
                enabled = true,
                onValueChange = viewModel::setVideoVolume,
                onReset = { viewModel.setVideoVolume(1f) },
                resetValue = 1f,
            )
        }
        if (wallpaper != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTextButton(onClick = onRemove) { Text(stringResource(R.string.remove)) }
            }
        }
    }
}

@Composable
private fun MusicItem(settings: AppSettings, viewModel: SettingsViewModel, onRemove: () -> Unit) {
    val music by viewModel.music.collectAsState()
    val pickers = LocalAppPickers.current
    val pick: () -> Unit = { pickers?.music?.launch(arrayOf("audio/*")) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingRowHeader(
            icon = MusicIcon,
            title = stringResource(R.string.music_title),
            description = "",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = pick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (music != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        MusicIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        shape = RoundedCornerShape(4.dp),
                        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f),
                    ) {
                        Text(
                            stringResource(R.string.music_type),
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (music != null) stringResource(R.string.music_has)
                    else stringResource(R.string.music_pick),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (music != null) MaterialTheme.colorScheme.onSurface else Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    if (music != null) stringResource(R.string.music_change)
                    else stringResource(R.string.music_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (music != null) {
            val musicData = music
            if (musicData?.ext != null) {
                Text(
                    stringResource(R.string.wallpaper_ext, musicData.ext!!.uppercase()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            GlassSliderRow(
                label = stringResource(R.string.music_volume),
                value = settings.musicVolume.safeClamp(0f, 1f, 1f),
                enabled = true,
                onValueChange = viewModel::setMusicVolume,
                onReset = { viewModel.setMusicVolume(1f) },
                resetValue = 1f,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTextButton(onClick = onRemove) { Text(stringResource(R.string.remove)) }
            }
        }
    }
}

@Composable
private fun FontItem(settings: AppSettings, viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingRowHeader(
            icon = FontIcon,
            title = stringResource(R.string.font),
            description = stringResource(R.string.font_desc),
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "system" to stringResource(R.string.font_system),
                "coming" to stringResource(R.string.font_coming),
                "mono" to stringResource(R.string.font_mono),
                "gothic" to stringResource(R.string.font_gothic),
                "condensed" to stringResource(R.string.font_condensed),
                "light" to stringResource(R.string.font_light),
                "thin" to stringResource(R.string.font_thin),
                "medium" to stringResource(R.string.font_medium),
                "black" to stringResource(R.string.font_black),
                "rounded" to stringResource(R.string.font_rounded),
                "serif" to stringResource(R.string.font_serif),
                "monospace" to stringResource(R.string.font_monospace),
                "cursive" to stringResource(R.string.font_cursive),
                "casual" to stringResource(R.string.font_casual),
                "smallcaps" to stringResource(R.string.font_smallcaps),
                "custom" to stringResource(R.string.font_custom),
            ).forEach { (key, label) ->
                AppFilterChip(
                    selected = settings.fontType == key,
                    onClick = { viewModel.setFontType(key) },
                    label = { Text(label) },
                )
            }
        }

        if (settings.fontType == "custom") {
            val pickers = LocalAppPickers.current
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextButton(
                    onClick = { pickers?.font?.launch("font/*") },
                ) {
                    Icon(
                        FolderIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(R.string.font_pick_ttf),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                AppTextButton(onClick = viewModel::removeCustomFont) {
                    Icon(
                        DeleteIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(R.string.font_custom_remove),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassItem(settings: AppSettings, viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingRowHeader(
            icon = GlassIcon,
            title = stringResource(R.string.glass_effect),
            description = stringResource(R.string.glass_effect_desc),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppFilterChip(
                selected = settings.glass,
                onClick = { viewModel.setGlass(true) },
                label = { Text(stringResource(R.string.glass_on)) },
            )
            AppFilterChip(
                selected = !settings.glass,
                onClick = { viewModel.setGlass(false) },
                label = { Text(stringResource(R.string.glass_off)) },
            )
        }
        GlassSliderRow(
            label = stringResource(R.string.glass_blur, (settings.glassBlur * 100).toInt()),
            value = settings.glassBlur.safeClamp(0f, 1f, 0.65f),
            enabled = settings.glass,
            onValueChange = viewModel::setGlassBlur,
            onReset = { viewModel.setGlassBlur(0.65f) },
        )
        GlassSliderRow(
            label = stringResource(R.string.glass_transparency, (settings.glassTransparency * 100).toInt()),
            value = settings.glassTransparency.safeClamp(0f, 1f, 0.65f),
            enabled = settings.glass,
            onValueChange = viewModel::setGlassTransparency,
            onReset = { viewModel.setGlassTransparency(0.65f) },
        )
        GlassSliderRow(
            label = stringResource(R.string.glass_glow, (settings.glassGlow * 100).toInt()),
            value = settings.glassGlow.safeClamp(0f, 1f, 0.65f),
            enabled = settings.glass,
            onValueChange = viewModel::setGlassGlow,
            onReset = { viewModel.setGlassGlow(0.65f) },
        )
    }
}

@Composable
private fun GlassSliderRow(
    label: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit,
    resetValue: Float = 0.65f,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${(value * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (value != resetValue) {
                AppTextButton(onClick = onReset, enabled = enabled) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.reset),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        AppSlider(
            value = (value * 20).roundToInt() / 20f,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            steps = 19,
            enabled = enabled,
        )
    }
}

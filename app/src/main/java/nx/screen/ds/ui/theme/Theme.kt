package nx.screen.ds.ui.theme

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Shapes
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nx.screen.ds.data.AppSettings
import nx.screen.ds.data.ColorSpec
import nx.screen.ds.data.ColorTheme
import nx.screen.ds.data.FontStore
import nx.screen.ds.data.ThemeMode
import nx.screen.ds.data.UiStyle
import nx.screen.ds.data.UiSystem
import nx.screen.ds.data.WallpaperData
import me.tatarka.google.material.dynamiccolor.DynamicScheme
import me.tatarka.google.material.dynamiccolor.Variant
import me.tatarka.google.material.hct.Hct
import me.tatarka.google.material.palettes.TonalPalette

private const val DEFAULT_SEED = 0xFF3B82F6L
private const val MIUI_SEED = 0xFF3482FFL

val NexShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val MiuixShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(26.dp),
    large = RoundedCornerShape(34.dp),
    extraLarge = RoundedCornerShape(48.dp),
)

val LocalUiSystem = staticCompositionLocalOf<UiSystem> { UiSystem.MATERIAL }

@Composable
fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (LocalUiSystem.current == UiSystem.MIUIX) {
        MiuixChip(selected, onClick, label, modifier, enabled)
    } else {
        MaterialPillChip(selected, onClick, label, modifier, enabled)
    }
}

@Composable
private fun MaterialPillChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = if (selected) scheme.primary else scheme.surfaceContainerHigh,
    ) {
        ProvideTextStyle(
            MaterialTheme.typography.labelLarge.copy(
                color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
            ),
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                label()
            }
        }
    }
}

@Composable
private fun MiuixChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) scheme.primary else scheme.surface,
        border = if (selected) null else BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        ProvideTextStyle(
            MaterialTheme.typography.labelLarge.copy(
                color = if (selected) scheme.onPrimary else scheme.onSurface,
            ),
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                label()
            }
        }
    }
}

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    if (LocalUiSystem.current == UiSystem.MIUIX) {
        MiuixButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
    } else {
        Button(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
    }
}

@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    if (LocalUiSystem.current == UiSystem.MIUIX) {
        MiuixTextButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
    } else {
        TextButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
    }
}

@Composable
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    if (LocalUiSystem.current == UiSystem.MIUIX) {
        MiuixOutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
    }
}

@Composable
private fun MiuixButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = scheme.primary,
        contentColor = scheme.onPrimary,
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        }
    }
}

@Composable
private fun MiuixTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
    ) {
        ProvideTextStyle(
            MaterialTheme.typography.labelLarge.copy(
                color = if (enabled) scheme.primary else scheme.onSurface.copy(alpha = 0.38f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        }
    }
}

@Composable
private fun MiuixOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, if (enabled) scheme.outline else scheme.outlineVariant),
    ) {
        ProvideTextStyle(
            MaterialTheme.typography.labelLarge.copy(
                color = if (enabled) scheme.primary else scheme.onSurface.copy(alpha = 0.38f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalUiSystem.current == UiSystem.MIUIX) {
        val scheme = MaterialTheme.colorScheme
        val shape = RoundedCornerShape(26.dp)
        val border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f))
        val cardContent: @Composable () -> Unit = { Column(content = content) }
        if (onClick != null) {
            Surface(
                onClick = onClick,
                modifier = modifier,
                shape = shape,
                color = scheme.surface,
                border = border,
                content = cardContent,
            )
        } else {
            Surface(
                modifier = modifier,
                shape = shape,
                color = scheme.surface,
                border = border,
                content = cardContent,
            )
        }
    } else {
        val scheme = MaterialTheme.colorScheme
        val shape = RoundedCornerShape(16.dp)
        val cardContent: @Composable () -> Unit = { Column(content = content) }
        if (onClick != null) {
            Surface(
                onClick = onClick,
                modifier = modifier,
                shape = shape,
                color = scheme.surfaceContainer,
                contentColor = scheme.onSurface,
                content = cardContent,
            )
        } else {
            Surface(
                modifier = modifier,
                shape = shape,
                color = scheme.surfaceContainer,
                contentColor = scheme.onSurface,
                content = cardContent,
            )
        }
    }
}

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (LocalUiSystem.current == UiSystem.MIUIX) {
        val scheme = MaterialTheme.colorScheme
        val trackHeight = 30.dp
        val thumb = trackHeight - 4.dp
        Surface(
            onClick = { onCheckedChange(!checked) },
            enabled = enabled,
            modifier = modifier.size(width = 52.dp, height = trackHeight),
            shape = RoundedCornerShape(trackHeight),
            color = if (checked) scheme.primary else scheme.surfaceContainerHighest,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                        .padding(2.dp)
                        .size(thumb)
                        .clip(CircleShape)
                        .background(if (checked) scheme.onPrimary else Color.White),
                )
            }
        }
    } else {
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
    }
}

@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        supportingText = supportingText,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        shape = if (LocalUiSystem.current == UiSystem.MIUIX) {
            RoundedCornerShape(20.dp)
        } else {
            RoundedCornerShape(14.dp)
        },
    )
}

@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        colors = if (LocalUiSystem.current == UiSystem.MIUIX) {
            SliderDefaults.colors(
                thumbColor = scheme.primary,
                activeTrackColor = scheme.primary,
                inactiveTrackColor = scheme.outlineVariant,
            )
        } else {
            SliderDefaults.colors()
        },
    )
}

@Composable
fun CardTitle(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconBadge(icon = icon, size = 34.dp, iconSize = 20.dp)
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    background: Color? = null,
    tint: Color? = null,
) {
    val bg = background ?: MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    val tc = tint ?: MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tc,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * M3 2021: las superficies vuelven al gris neutro (sin tinte del primario),
 * sin importar el palet dinámico del sistema. Es la diferencia visible con
 * M3 2025 cuando no hay acento ni wallpaper propio.
 */
private fun neutralSurfaces(scheme: ColorScheme): ColorScheme {
    fun gray(c: Color): Color {
        val cv = FloatArray(3)
        android.graphics.Color.colorToHSV(c.toArgb(), cv)
        cv[0] = 0f
        cv[1] = 0f
        return Color.hsv(cv[0], cv[1], cv[2])
    }
    return scheme.copy(
        background = gray(scheme.background),
        surface = gray(scheme.surface),
        surfaceVariant = gray(scheme.surfaceVariant),
        surfaceDim = gray(scheme.surfaceDim),
        surfaceBright = gray(scheme.surfaceBright),
        surfaceContainerLowest = gray(scheme.surfaceContainerLowest),
        surfaceContainerLow = gray(scheme.surfaceContainerLow),
        surfaceContainer = gray(scheme.surfaceContainer),
        surfaceContainerHigh = gray(scheme.surfaceContainerHigh),
        surfaceContainerHighest = gray(scheme.surfaceContainerHighest),
    )
}

@Composable
fun ScreenDensityTheme(settings: AppSettings, wallpaperSeed: Color? = null, content: @Composable () -> Unit) {
    val dark: Boolean
    val pure: Boolean
    when (settings.themeMode) {
        ThemeMode.SYSTEM -> {
            dark = isSystemInDarkTheme()
            pure = false
        }
        ThemeMode.LIGHT -> {
            dark = false
            pure = false
        }
        ThemeMode.DARK -> {
            dark = true
            pure = false
        }
        ThemeMode.DARK_PURE -> {
            dark = true
            pure = true
        }
    }

    val amoledDark = settings.amoled
    val effectiveDark = amoledDark || dark
    val effectivePure = amoledDark || pure

    val context = LocalContext.current
    val dynamicAvailable = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val accentSeed = remember(settings.accent) { seedFromAccent(settings.accent) }
    val fallbackSeed = accentSeed ?: wallpaperSeed ?: Color(DEFAULT_SEED)

    val colorScheme = remember(
        settings.uiSystem, settings.accent, settings.dynamicColor, settings.uiStyle,
        settings.colorSpec, settings.colorTheme, settings.themeMode, settings.glass,
        settings.glassTransparency,
        wallpaperSeed, accentSeed, fallbackSeed, effectiveDark, effectivePure,
        dynamicAvailable,
    ) {
        val built = when {
            settings.uiSystem == UiSystem.MIUIX -> {
                buildMiuixScheme(Color(MIUI_SEED), effectiveDark, effectivePure)
            }
            settings.colorTheme == ColorTheme.CLASSIC -> {
                normalizeText(
                    buildClassicScheme(fallbackSeed, effectiveDark, effectivePure),
                    effectiveDark,
                )
            }
            accentSeed == null && wallpaperSeed == null && dynamicAvailable -> {
                val base = if (effectiveDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                val specd = when (settings.colorSpec) {
                    ColorSpec.C2025 -> base
                    ColorSpec.C2021 -> neutralSurfaces(base)
                }
                if (effectivePure) applyPure(specd) else specd
            }
            else -> {
                normalizeText(
                    buildScheme(
                        seed = fallbackSeed,
                        dark = effectiveDark,
                        pure = effectivePure,
                        style = settings.uiStyle,
                        spec = settings.colorSpec,
                    ),
                    effectiveDark,
                )
            }
        }
        if (settings.glass) {
            glassColorScheme(built, settings.glassTransparency)
        } else {
            built
        }
    }

    val fontFamily = remember(settings.fontType, settings.fontFile) {
        FontStore.fontFamily(context, settings.fontType, settings.fontFile) ?: FontFamily.Default
    }
    val typography = remember(fontFamily) { Typography().withFontFamily(fontFamily) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = if (settings.uiSystem == UiSystem.MIUIX) MiuixShapes else NexShapes,
        content = {
            CompositionLocalProvider(
                LocalUiSystem provides settings.uiSystem,
                LocalContentColor provides colorScheme.onSurface,
            ) {
                content()
            }
        },
    )
}

private fun Typography.withFontFamily(fontFamily: FontFamily): Typography =
    Typography(
        displayLarge = displayLarge.copy(fontFamily = fontFamily),
        displayMedium = displayMedium.copy(fontFamily = fontFamily),
        displaySmall = displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = titleLarge.copy(fontFamily = fontFamily),
        titleMedium = titleMedium.copy(fontFamily = fontFamily),
        titleSmall = titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = bodySmall.copy(fontFamily = fontFamily),
        labelLarge = labelLarge.copy(fontFamily = fontFamily),
        labelMedium = labelMedium.copy(fontFamily = fontFamily),
        labelSmall = labelSmall.copy(fontFamily = fontFamily),
    )

fun colorFromHex(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return runCatching {
        val clean = hex.trim().removePrefix("#").removePrefix("0x")
        if (clean.length <= 6) {
            Color(0xFF000000L or clean.toLong(16))
        } else {
            Color(clean.toLong(16))
        }
    }.getOrNull()
}

fun accentSeedPair(accent: String?): Pair<Color?, Color?> {
    if (accent.isNullOrBlank()) return null to null
    val parts = accent.split("-")
    val a = colorFromHex(parts[0])
    val b = parts.getOrNull(1)?.let(::colorFromHex)
    return a to b
}

fun seedFromAccent(accent: String?): Color? = accentSeedPair(accent).first

fun dominantColorOf(bitmap: ImageBitmap?): Color? {
    if (bitmap == null) return null
    return runCatching {
        val bmp = bitmap.asAndroidBitmap()
        val w = bmp.width
        val h = bmp.height
        val stepX = (w / 12).coerceAtLeast(1)
        val stepY = (h / 12).coerceAtLeast(1)
        val buckets = IntArray(24)
        val satSum = FloatArray(24)
        val valSum = FloatArray(24)
        var total = 0
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val c = bmp.getPixel(x, y)
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(c, hsv)
                val bucket = (hsv[0] / 15f).toInt().coerceIn(0, 23)
                buckets[bucket]++
                satSum[bucket] += hsv[1]
                valSum[bucket] += hsv[2]
                total++
                x += stepX
            }
            y += stepY
        }
        if (total == 0) return@runCatching null
        var bestBucket = -1
        var bestScore = -1f
        var i = 0
        while (i < 24) {
            val count = buckets[i]
            if (count > 0) {
                val avgSat = satSum[i] / count
                val score = count * avgSat
                if (score > bestScore) {
                    bestScore = score
                    bestBucket = i
                }
            }
            i++
        }
        if (bestBucket < 0) return@runCatching null
        if (bestScore < 1f) {
            var vSum = 0f
            for (j in 0 until 24) vSum += valSum[j]
            return@runCatching Color.hsv(0f, 0f, (vSum / total).coerceIn(0.2f, 0.98f))
        }
        val count = buckets[bestBucket]
        Color.hsv(
            bestBucket * 15f + 7.5f,
            (satSum[bestBucket] / count).coerceIn(0.15f, 1f),
            (valSum[bestBucket] / count).coerceIn(0.2f, 0.98f),
        )
    }.getOrNull()
}

private data class Roles(
    val pH: Float,
    val sH: Float,
    val tH: Float,
    val sat: Float,
)

private fun rolesFor(seedHue: Float, style: UiStyle): Roles {
    val h = seedHue
    return when (style) {
        UiStyle.NEUTRAL -> Roles(h, h, h, 0.08f)
        UiStyle.TONAL_SPOT -> Roles(h, h, h, 0.5f)
        UiStyle.VIBRANT -> Roles(h, h, h, 0.82f)
        UiStyle.EXPRESSIVE -> Roles(h, (h + 30f) % 360f, (h - 30f + 360f) % 360f, 0.92f)
        UiStyle.RAINBOW -> Roles(h, (h + 120f) % 360f, (h - 120f + 360f) % 360f, 0.78f)
        UiStyle.FRUIT_SALAD -> Roles(h, (h + 60f) % 360f, (h - 60f + 360f) % 360f, 0.72f)
        UiStyle.MONOCHROME -> Roles(h, h, h, 0f)
        UiStyle.FIDELITY -> Roles(h, (h + 8f) % 360f, (h - 8f + 360f) % 360f, 0.72f)
        UiStyle.CONTENT -> Roles(h, (h + 24f) % 360f, (h - 24f + 360f) % 360f, 0.62f)
    }
}

private fun buildClassicScheme(seed: Color, dark: Boolean, pure: Boolean): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toArgb(), hsv)
    val hue = hsv[0]
    val sat = hsv[1].coerceIn(0.25f, 0.85f)
    val secondaryHue = (hue + 12f) % 360f
    val tertiaryHue = (hue + 45f) % 360f

    fun gray(v: Float) = Color.hsv(0f, 0f, v)
    fun clamp(v: Float) = v.coerceIn(0f, 1f)

    return if (dark) {
        darkColorScheme(
            primary = Color.hsv(hue, sat, 0.82f),
            onPrimary = Color.hsv(hue, sat, 0.18f),
            primaryContainer = Color.hsv(hue, clamp(sat * 0.7f), 0.32f),
            onPrimaryContainer = Color.hsv(hue, clamp(sat * 0.45f), 0.92f),
            secondary = Color.hsv(secondaryHue, clamp(sat * 0.35f), 0.78f),
            onSecondary = Color.hsv(secondaryHue, 0.6f, 0.16f),
            secondaryContainer = Color.hsv(secondaryHue, clamp(sat * 0.35f), 0.3f),
            onSecondaryContainer = Color.hsv(secondaryHue, 0.25f, 0.9f),
            tertiary = Color.hsv(tertiaryHue, clamp(sat * 0.45f), 0.78f),
            onTertiary = Color.hsv(tertiaryHue, 0.6f, 0.14f),
            background = if (pure) Color(0xFF000000) else gray(0.085f),
            onBackground = gray(0.92f),
            surface = if (pure) Color(0xFF000000) else gray(0.085f),
            onSurface = gray(0.92f),
            surfaceVariant = gray(0.23f),
            onSurfaceVariant = gray(0.72f),
            surfaceContainerLowest = gray(0.06f),
            surfaceContainerLow = gray(0.115f),
            surfaceContainer = gray(0.135f),
            surfaceContainerHigh = gray(0.175f),
            surfaceContainerHighest = gray(0.22f),
            outline = gray(0.42f),
            outlineVariant = gray(0.23f),
            surfaceTint = Color.hsv(hue, sat, 0.82f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
        )
    } else {
        lightColorScheme(
            primary = Color.hsv(hue, clamp(sat * 0.85f), 0.5f),
            onPrimary = Color.White,
            primaryContainer = Color.hsv(hue, clamp(sat * 0.45f), 0.94f),
            onPrimaryContainer = Color.hsv(hue, clamp(sat * 0.8f), 0.16f),
            secondary = Color.hsv(secondaryHue, clamp(sat * 0.4f), 0.5f),
            onSecondary = Color.White,
            secondaryContainer = Color.hsv(secondaryHue, clamp(sat * 0.35f), 0.9f),
            onSecondaryContainer = Color.hsv(secondaryHue, 0.6f, 0.15f),
            tertiary = Color.hsv(tertiaryHue, clamp(sat * 0.5f), 0.45f),
            onTertiary = Color.White,
            background = Color(0xFFFDF6F0),
            onBackground = gray(0.12f),
            surface = Color(0xFFFDF6F0),
            onSurface = gray(0.12f),
            surfaceVariant = Color(0xFFF0E0CF),
            onSurfaceVariant = Color(0xFF51453A),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF7F0E8),
            surfaceContainer = Color(0xFFF1EAE2),
            surfaceContainerHigh = Color(0xFFECE4DC),
            surfaceContainerHighest = Color(0xFFE6DED6),
            outline = Color(0xFF85786B),
            outlineVariant = Color(0xFFD9CCBD),
            surfaceTint = Color.hsv(hue, clamp(sat * 0.85f), 0.5f),
            error = Color(0xFFB3261E),
            onError = Color.White,
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
        )
    }
}

private fun buildScheme(
    seed: Color,
    dark: Boolean,
    pure: Boolean,
    style: UiStyle,
    spec: ColorSpec,
): ColorScheme {
    val hct = Hct.fromInt(seed.toArgb())
    val hue = hct.hue
    val (pC, sC, tC, sHue, tHue) = when (style) {
        UiStyle.TONAL_SPOT -> Chrt(48.0, 16.0, 24.0, hue, hue + 60.0)
        UiStyle.NEUTRAL -> Chrt(12.0, 8.0, 16.0, hue, hue + 60.0)
        UiStyle.MONOCHROME -> Chrt(0.0, 0.0, 0.0, hue, hue + 60.0)
        UiStyle.VIBRANT -> Chrt(125.0, 25.0, 40.0, hue, hue + 60.0)
        UiStyle.EXPRESSIVE -> Chrt(40.0, 35.0, 30.0, hue, hue + 120.0)
        UiStyle.FIDELITY -> Chrt(200.0, 24.0, 32.0, hue, hue + 10.0)
        UiStyle.CONTENT -> Chrt(200.0, 30.0, 35.0, hue, hue + 20.0)
        UiStyle.RAINBOW -> Chrt(48.0, 36.0, 16.0, hue + 120.0, hue - 120.0)
        UiStyle.FRUIT_SALAD -> Chrt(48.0, 36.0, 36.0, hue + 60.0, hue - 60.0)
    }
    val monet = DynamicScheme(
        hct,
        when (style) {
            UiStyle.TONAL_SPOT -> Variant.TONAL_SPOT
            UiStyle.NEUTRAL -> Variant.NEUTRAL
            UiStyle.MONOCHROME -> Variant.MONOCHROME
            UiStyle.VIBRANT -> Variant.VIBRANT
            UiStyle.EXPRESSIVE -> Variant.EXPRESSIVE
            UiStyle.FIDELITY -> Variant.FIDELITY
            UiStyle.CONTENT -> Variant.CONTENT
            UiStyle.RAINBOW -> Variant.RAINBOW
            UiStyle.FRUIT_SALAD -> Variant.FRUIT_SALAD
        },
        dark,
        0.0,
        TonalPalette.fromHueAndChroma(hue, pC),
        TonalPalette.fromHueAndChroma(sHue, sC),
        TonalPalette.fromHueAndChroma(tHue, tC),
        TonalPalette.fromHueAndChroma(hue, 6.0),
        TonalPalette.fromHueAndChroma(hue, 8.0),
    )
    val base = monet.toColorScheme()
    val scheme = when (spec) {
        ColorSpec.C2021 -> neutralSurfaces(base)
        ColorSpec.C2025 -> base
    }
    val pured = if (pure) applyPure(scheme) else scheme
    return normalizeText(pured, dark)
}

private data class Chrt(val pC: Double, val sC: Double, val tC: Double, val sHue: Double, val tHue: Double)

private fun DynamicScheme.toColorScheme(): ColorScheme {
    val scheme = darkColorScheme()
    return scheme.copy(
        primary = Color(getPrimary()),
        onPrimary = Color(getOnPrimary()),
        primaryContainer = Color(getPrimaryContainer()),
        onPrimaryContainer = Color(getOnPrimaryContainer()),
        secondary = Color(getSecondary()),
        onSecondary = Color(getOnSecondary()),
        secondaryContainer = Color(getSecondaryContainer()),
        onSecondaryContainer = Color(getOnSecondaryContainer()),
        tertiary = Color(getTertiary()),
        onTertiary = Color(getOnTertiary()),
        tertiaryContainer = Color(getTertiaryContainer()),
        onTertiaryContainer = Color(getOnTertiaryContainer()),
        error = Color(getError()),
        onError = Color(getOnError()),
        errorContainer = Color(getErrorContainer()),
        onErrorContainer = Color(getOnErrorContainer()),
        background = Color(getBackground()),
        onBackground = Color(getOnBackground()),
        surface = Color(getSurface()),
        onSurface = Color(getOnSurface()),
        surfaceVariant = Color(getSurfaceVariant()),
        onSurfaceVariant = Color(getOnSurfaceVariant()),
        outline = Color(getOutline()),
        outlineVariant = Color(getOutlineVariant()),
        scrim = Color(getScrim()),
        inverseSurface = Color(getInverseSurface()),
        inverseOnSurface = Color(getInverseOnSurface()),
        inversePrimary = Color(getInversePrimary()),
        surfaceBright = Color(getSurfaceBright()),
        surfaceDim = Color(getSurfaceDim()),
        surfaceContainerLowest = Color(getSurfaceContainerLowest()),
        surfaceContainerLow = Color(getSurfaceContainerLow()),
        surfaceContainer = Color(getSurfaceContainer()),
        surfaceContainerHigh = Color(getSurfaceContainerHigh()),
        surfaceContainerHighest = Color(getSurfaceContainerHighest()),
        surfaceTint = Color(getSurfaceTint()),
    )
}

private fun buildMiuixScheme(seed: Color, dark: Boolean, pure: Boolean): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toArgb(), hsv)
    val primary = if (dark) Color.hsv(hsv[0], hsv[1], hsv[2] * 0.9f) else seed
    val onPrimary = Color(0xFFFFFFFF)

    return if (dark) {
        val bg = if (pure) Color(0xFF000000) else Color(0xFF101010)
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = Color(0xFF338FE4),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFF505050),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFF434343),
            onSecondaryContainer = primary,
            tertiary = primary,
            onTertiary = onPrimary,
            tertiaryContainer = Color(0xFF2B3B54),
            onTertiaryContainer = Color(0xFF4788FF),
            error = Color(0xFFF12522),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF2E0603),
            onErrorContainer = Color(0xFFFFDAD6),
            background = bg,
            onBackground = Color(0xE6FFFFFF),
            surface = Color(0xFF161616),
            onSurface = Color(0xFFF2F2F2),
            surfaceVariant = Color(0xFF1E1E1E),
            onSurfaceVariant = Color(0xFF787E96),
            surfaceTint = primary,
            outline = Color(0xFF404040),
            outlineVariant = Color(0xFF2C2C2C),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFFF2F2F2),
            inverseOnSurface = Color(0xFF000000),
            inversePrimary = primary,
            surfaceBright = Color(0xFF222222),
            surfaceDim = bg,
            surfaceContainerLowest = if (pure) Color(0xFF000000) else Color(0xFF101010),
            surfaceContainerLow = if (pure) Color(0xFF030303) else Color(0xFF141414),
            surfaceContainer = if (pure) Color(0xFF070707) else Color(0xFF1A1A1A),
            surfaceContainerHigh = if (pure) Color(0xFF0C0C0C) else Color(0xFF202020),
            surfaceContainerHighest = if (pure) Color(0xFF121212) else Color(0xFF262626),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = Color(0xFF5D9BFF),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFFE6E6E6),
            onSecondary = Color(0xFF1A1A1A),
            secondaryContainer = Color(0xFFF0F0F0),
            onSecondaryContainer = primary,
            tertiary = primary,
            onTertiary = onPrimary,
            tertiaryContainer = Color(0xFFEAF2FF),
            onTertiaryContainer = Color(0xFF3482FF),
            error = Color(0xFFE94634),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFDF6F4),
            onErrorContainer = Color(0xFF410002),
            background = Color(0xFFF2F2F2),
            onBackground = Color(0xFF000000),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF000000),
            surfaceVariant = Color(0xFFF0F0F0),
            onSurfaceVariant = Color(0xFF666666),
            surfaceTint = primary,
            outline = Color(0xFFD9D9D9),
            outlineVariant = Color(0xFFE0E0E0),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFF000000),
            inverseOnSurface = Color(0xFFF2F2F2),
            inversePrimary = Color(0xFF5D9BFF),
            surfaceBright = Color(0xFFFFFFFF),
            surfaceDim = Color(0xFFE6E6E6),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFFFFFFF),
            surfaceContainer = Color(0xFFFFFFFF),
            surfaceContainerHigh = Color(0xFFF7F7F7),
            surfaceContainerHighest = Color(0xFFEFEFEF),
        )
    }
}

private fun applyPure(scheme: ColorScheme): ColorScheme {
    val black = Color(0xFF000000)
    val dark = Color(0xFF0D0D0D)
    return scheme.copy(
        background = black,
        surface = black,
        surfaceVariant = dark,
        surfaceDim = black,
        surfaceBright = dark,
        surfaceContainerLowest = black,
        surfaceContainerLow = dark,
        surfaceContainer = dark,
        surfaceContainerHigh = dark,
        surfaceContainerHighest = Color(0xFF141414),
    )
}

private fun normalizeText(scheme: ColorScheme, dark: Boolean): ColorScheme {
    val text = Color(if (dark) 0xFFF2F2F2 else 0xFF000000)
    val textVariant = Color(if (dark) 0xFFA6ADB6 else 0xFF565D66)
    return scheme.copy(
        onSurface = text,
        onBackground = text,
        onSurfaceVariant = textVariant,
        onPrimaryContainer = contrastText(scheme.primaryContainer),
        onSecondaryContainer = contrastText(scheme.secondaryContainer),
        onTertiaryContainer = contrastText(scheme.tertiaryContainer),
        onErrorContainer = contrastText(scheme.errorContainer),
    )
}

private fun contrastText(bg: Color): Color =
    if (bg.luminance() > 0.5f) Color(0xFF000000) else Color(0xFFFFFFFF)

@Composable
fun statusGreen(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF81C784) else Color(0xFF2E7D32)

@Composable
fun statusRed(): Color = MaterialTheme.colorScheme.error

private fun glassColorScheme(
    base: ColorScheme,
    transparency: Float,
): ColorScheme {
    val t = transparency.coerceIn(0f, 1f)
    fun glassy(c: Color): Color = c.copy(alpha = 1f - 0.35f * t)
    return base.copy(
        background = base.background,
        surface = glassy(base.surface),
        surfaceVariant = glassy(base.surfaceVariant),
        surfaceContainerLowest = glassy(base.surfaceContainerLowest),
        surfaceContainerLow = glassy(base.surfaceContainerLow),
        surfaceContainer = glassy(base.surfaceContainer),
        surfaceContainerHigh = glassy(base.surfaceContainerHigh),
        surfaceContainerHighest = glassy(base.surfaceContainerHighest),
        surfaceTint = base.surfaceTint,
    )
}

data class GlassEffect(val active: Boolean = false, val surfaceAlpha: Float = 1f)

val LocalGlassEffect = staticCompositionLocalOf { GlassEffect() }

@Composable
fun AppBackdrop(
    glass: Boolean,
    wallpaper: WallpaperData?,
    blur: Float = 0.5f,
    transparency: Float = 0.5f,
    glow: Float = 0.5f,
    static: Boolean = false,
    volume: Float = 1f,
) {
    LiquidBackdrop(
        glass = glass,
        wallpaper = wallpaper,
        blur = blur,
        transparency = transparency,
        glow = glow,
        static = static,
        volume = volume,
    )
}

@Composable
fun LiquidBackdrop(
    glass: Boolean,
    wallpaper: WallpaperData?,
    blur: Float = 0.5f,
    transparency: Float = 0.5f,
    glow: Float = 0.5f,
    static: Boolean = false,
    volume: Float = 1f,
) {
    val scheme = MaterialTheme.colorScheme

    val plain = remember(scheme) { scheme.background }
    val veil = remember(glass, transparency, scheme) {
        val alpha = 0.28f + 0.45f * (1f - transparency.coerceIn(0f, 1f))
        scheme.surfaceContainer.copy(alpha = alpha)
    }
    val glowTint = remember(glass, glow, scheme) {
        scheme.primary.copy(alpha = 0.02f + 0.10f * glow.coerceIn(0f, 1f))
    }
    val blurRadius = remember(blur) { (blur.coerceIn(0f, 1f) * 10f).dp }
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val glassBlurDp = if (canBlur && glass && blurRadius > 1.dp) blurRadius else 0.dp

    Box(modifier = Modifier.fillMaxSize().background(plain)) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (wallpaper != null) {
                WallpaperView(
                    wallpaper = wallpaper,
                    contentScale = ContentScale.Crop,
                    blurDp = glassBlurDp,
                    static = static,
                    volume = volume,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (glass) {
                val g = glow.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    scheme.primary.copy(alpha = 0.22f + 0.18f * g),
                                    scheme.secondary.copy(alpha = 0.10f + 0.06f * g),
                                    scheme.tertiary.copy(alpha = 0.18f + 0.14f * g),
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite,
                            ),
                        ),
                )
            }
        }
        if (glass) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(veil)
                        drawRect(glowTint)
                    },
            )
        } else if (wallpaper != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.background.copy(alpha = 0.35f)),
            )
        }
    }
}

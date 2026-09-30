package nx.screen.ds.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Ink = SolidColor(Color.Black)
private val Clear = SolidColor(Color.Transparent)
private const val DEG = (PI.toFloat() / 180f)

private fun PathBuilder.roundedRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float,
) {
    moveTo(left + radius, top)
    lineTo(right - radius, top)
    quadTo(right, top, right, top + radius)
    lineTo(right, bottom - radius)
    quadTo(right, bottom, right - radius, bottom)
    lineTo(left + radius, bottom)
    quadTo(left, bottom, left, bottom - radius)
    lineTo(left, top + radius)
    quadTo(left, top, left + radius, top)
    close()
}

private fun PathBuilder.circleAt(cx: Float, cy: Float, radius: Float) {
    arcBetween(cx, cy, radius, 0f, 360f)
    close()
}

private fun PathBuilder.arcBetween(
    cx: Float,
    cy: Float,
    radius: Float,
    fromDeg: Float,
    toDeg: Float,
) {
    val sweep = toDeg - fromDeg
    val steps = kotlin.math.ceil(sweep / 45f).toInt().coerceAtLeast(1)
    val step = sweep / steps
    var angle = fromDeg
    val halfRad = (step / 2f) * DEG
    val k = radius * (1f - cos(halfRad))
    repeat(steps) { i ->
        val angleEnd = fromDeg + step * (i + 1)
        val mid = (angle + angleEnd) / 2f
        val midRad = mid * DEG
        val cr = radius + k
        val cx0 = cx + cr * cos(midRad)
        val cy0 = cy + cr * sin(midRad)
        val endRad = angleEnd * DEG
        val ex = cx + radius * cos(endRad)
        val ey = cy + radius * sin(endRad)
        quadTo(cx0, cy0, ex, ey)
        angle = angleEnd
    }
}

private fun PathBuilder.gearBody() {
    val cx = 12f
    val cy = 12f
    val n = 8
    val valley = 8.2f
    val tip = 10.6f
    val step = 360f / n
    var first = true
    repeat(n) { i ->
        val a0 = i * step
        val aTip = a0 + step * 0.25f
        val a1 = a0 + step * 0.5f
        listOf(a0, aTip, a1).forEach { deg ->
            val rad = deg * DEG
            val r = if (deg == aTip) tip else valley
            val x = cx + r * cos(rad)
            val y = cy + r * sin(rad)
            if (first) {
                moveTo(x, y)
                first = false
            } else {
                lineTo(x, y)
            }
        }
    }
    close()
}

private val numRegex = Regex("""-?(?:\d+\.\d*|\.\d+|\d+)(?:[eE][-+]?\d+)?""")

/**
 * Parser mínimo de path SVG (comandos M/L/H/V/C/S/Q/T/Z, absolutos y relativos)
 * para poder usar los paths oficiales de Material Icons sin dependencias extra.
 */
private fun PathBuilder.parseMaterialPath(d: String) {
    var i = 0
    var cmd = 'M'
    var prevCmd = 'Z'
    var cx = 0f
    var cy = 0f
    var ctrlX = 0f
    var ctrlY = 0f

    fun skip() {
        while (i < d.length && (d[i] == ' ' || d[i] == ',' || d[i] == '\n' || d[i] == '\t' || d[i] == '\r')) i++
    }

    fun num(): Float {
        skip()
        val m = numRegex.find(d, i) ?: return 0f
        i = m.range.last + 1
        return m.value.toFloat()
    }

    fun isCmd(c: Char) = c in "MmLlHhVvCcSsQqTtZz"

    while (i < d.length) {
        skip()
        if (i >= d.length) break
        if (isCmd(d[i])) {
            cmd = d[i]
            i++
        } else if (prevCmd == 'Z' || prevCmd == 'z') {
            break
        }
        val relative = cmd.isLowerCase()
        when (cmd) {
            'M', 'm' -> {
                val x = num()
                val y = num()
                cx = if (relative) cx + x else x
                cy = if (relative) cy + y else y
                moveTo(cx, cy)
                prevCmd = cmd
                cmd = if (relative) 'l' else 'L'
            }
            'L', 'l' -> {
                val x = num()
                val y = num()
                cx = if (relative) cx + x else x
                cy = if (relative) cy + y else y
                lineTo(cx, cy)
                prevCmd = cmd
            }
            'H', 'h' -> {
                cx = if (relative) cx + num() else num()
                lineTo(cx, cy)
                prevCmd = cmd
            }
            'V', 'v' -> {
                cy = if (relative) cy + num() else num()
                lineTo(cx, cy)
                prevCmd = cmd
            }
            'C', 'c' -> {
                val x1 = num(); val y1 = num(); val x2 = num(); val y2 = num()
                val x = num(); val y = num()
                val ax1 = if (relative) cx + x1 else x1
                val ay1 = if (relative) cy + y1 else y1
                val ax2 = if (relative) cx + x2 else x2
                val ay2 = if (relative) cy + y2 else y2
                val ax = if (relative) cx + x else x
                val ay = if (relative) cy + y else y
                curveTo(ax1, ay1, ax2, ay2, ax, ay)
                ctrlX = ax2
                ctrlY = ay2
                cx = ax
                cy = ay
                prevCmd = cmd
            }
            'S', 's' -> {
                val x2 = num(); val y2 = num()
                val x = num(); val y = num()
                val smooth = prevCmd == 'C' || prevCmd == 'c' || prevCmd == 'S' || prevCmd == 's'
                val ax2 = if (relative) cx + x2 else x2
                val ay2 = if (relative) cy + y2 else y2
                val ax = if (relative) cx + x else x
                val ay = if (relative) cy + y else y
                reflectiveCurveTo(ax2, ay2, ax, ay)
                ctrlX = ax2
                ctrlY = ay2
                cx = ax
                cy = ay
                prevCmd = cmd
            }
            'Q', 'q' -> {
                val x1 = num(); val y1 = num()
                val x = num(); val y = num()
                val ax1 = if (relative) cx + x1 else x1
                val ay1 = if (relative) cy + y1 else y1
                val ax = if (relative) cx + x else x
                val ay = if (relative) cy + y else y
                quadTo(ax1, ay1, ax, ay)
                ctrlX = ax1
                ctrlY = ay1
                cx = ax
                cy = ay
                prevCmd = cmd
            }
            'T', 't' -> {
                val x = num(); val y = num()
                val smooth = prevCmd == 'Q' || prevCmd == 'q' || prevCmd == 'T' || prevCmd == 't'
                val ax1 = if (smooth) 2f * cx - ctrlX else cx
                val ay1 = if (smooth) 2f * cy - ctrlY else cy
                val ax = if (relative) cx + x else x
                val ay = if (relative) cy + y else y
                reflectiveQuadTo(ax, ay)
                ctrlX = ax1
                ctrlY = ay1
                cx = ax
                cy = ay
                prevCmd = cmd
            }
            'Z', 'z' -> {
                close()
                prevCmd = cmd
            }
        }
    }
}

private fun materialIcon(name: String, d: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = Ink) {
            parseMaterialPath(d)
        }
    }.build()

val HomeIcon: ImageVector = ImageVector.Builder(
    name = "Home",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = Ink, pathFillType = PathFillType.EvenOdd) {
        moveTo(12f, 3.4f)
        lineTo(21f, 11.6f)
        lineTo(19.1f, 11.6f)
        lineTo(19.1f, 20f)
        lineTo(4.9f, 20f)
        lineTo(4.9f, 11.6f)
        lineTo(3f, 11.6f)
        close()
        roundedRect(10.5f, 13f, 13.5f, 20f, 1.5f)
    }
}.build()

val AppsIcon: ImageVector = ImageVector.Builder(
    name = "Apps",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = Ink) {
        roundedRect(4f, 4f, 10f, 10f, 2.4f)
        roundedRect(14f, 4f, 20f, 10f, 2.4f)
        roundedRect(4f, 14f, 10f, 20f, 2.4f)
        roundedRect(14f, 14f, 20f, 20f, 2.4f)
    }
}.build()

val GearIcon: ImageVector = ImageVector.Builder(
    name = "Settings",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = Ink, pathFillType = PathFillType.EvenOdd) {
        gearBody()
        circleAt(12f, 12f, 3.2f)
    }
}.build()

val TuneIcon: ImageVector = materialIcon(
    "Tune",
    "M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z",
)

val InfoCircleIcon: ImageVector = ImageVector.Builder(
    name = "About",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = Ink, pathFillType = PathFillType.EvenOdd) {
        circleAt(12f, 12f, 9.6f)
        roundedRect(11f, 6.4f, 13f, 9.6f, 1.4f)
        roundedRect(11f, 10.8f, 13f, 17.6f, 1.4f)
    }
}.build()

val CopyIcon: ImageVector = materialIcon(
    "Copy",
    "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z",
)

val SpeedIcon: ImageVector = ImageVector.Builder(
    name = "Speed",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        fill = Clear,
        stroke = Ink,
        strokeLineWidth = 1.8f,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineCap = StrokeCap.Round,
    ) {
        arcBetween(12f, 14f, 8.4f, 150f, 390f)
    }
    path(
        fill = Clear,
        stroke = Ink,
        strokeLineWidth = 1.8f,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineCap = StrokeCap.Round,
    ) {
        moveTo(12f, 14f)
        lineTo(17.4f, 9.6f)
    }
    path(fill = Ink) {
        circleAt(12f, 14f, 1.3f)
    }
}.build()

val AnimationIcon: ImageVector = materialIcon(
    "Animation",
    "M14 2H4c-1.1 0-2 .9-2 2v10h2V4h10V2zm4 4H8c-1.1 0-2 .9-2 2v10h2V8h10V6zm2 4h-8c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2v-8c0-1.1-.9-2-2-2z",
)

val TerminalIcon: ImageVector = materialIcon(
    "Terminal",
    "M20 4H4c-1.11 0-2 .9-2 2v12c0 1.1.89 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.89-2-2-2zm0 14H4v-8h16v8zm-2-1h-6v-2h6v2zM7.5 17l-1.41-1.41L8.67 13l-2.59-2.59L7.5 9l4 4-4 4z",
)

val ShieldIcon: ImageVector = materialIcon(
    "Shield",
    "M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z",
)

val Md3Icon: ImageVector = ImageVector.Builder(
    name = "Md3",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = Ink) {
        roundedRect(3.5f, 7f, 8f, 18f, 2.2f)
        roundedRect(10f, 3f, 14.5f, 18f, 2.2f)
        roundedRect(16.5f, 7f, 21f, 18f, 2.2f)
    }
}.build()

val M3eIcon: ImageVector = materialIcon(
    "M3e",
    "M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z",
)

val TonalityIcon: ImageVector = materialIcon(
    "Tonality",
    "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm0-14c-3.31 0-6 2.69-6 6s2.69 6 6 6V6z",
)

val BalanceIcon: ImageVector = materialIcon(
    "Balance",
    "M13 7.83c.85-.3 1.53-.98 1.83-1.83H18l-3 7c0 1.66 1.57 3 3.5 3s3.5-1.34 3.5-3l-3-7h2V4h-6.17c-.41-1.17-1.52-2-2.83-2s-2.42.83-2.83 2H3v2h2l-3 7c0 1.66 1.57 3 3.5 3S9 14.66 9 13L6 6h2.17c.3.85.98 1.53 1.83 1.83V19H2v2h20v-2h-9V7.83zM20.37 13h-3.74l1.87-4.36L20.37 13zm-13 0H3.63L5.5 8.64 7.37 13zM12 6c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1z",
)

val VibrantIcon: ImageVector = materialIcon(
    "Vibrant",
    "M13.5.67s.74 2.65.74 4.8c0 2.06-1.35 3.73-3.41 3.73-2.07 0-3.63-1.67-3.63-3.73l.03-.36C5.21 7.51 4 10.62 4 14c0 4.42 3.58 8 8 8s8-3.58 8-8C20 8.61 17.41 3.8 13.5.67zM11.71 19c-1.78 0-3.22-1.4-3.22-3.14 0-1.62 1.05-2.76 2.81-3.12 1.77-.36 3.6-1.21 4.62-2.58.39 1.29.59 2.65.59 4.04 0 2.65-2.15 4.8-4.8 4.8z",
)

val ExpressiveIcon: ImageVector = materialIcon(
    "Expressive",
    "M7 14c-1.66 0-3 1.34-3 3 0 1.31-1.16 2-2 2 .92 1.22 2.49 2 4 2 2.21 0 4-1.79 4-4 0-1.66-1.34-3-3-3zm13.71-9.37l-1.34-1.34c-.39-.39-1.02-.39-1.41 0L9 12.25 11.75 15l8.96-8.96c.39-.39.39-1.02 0-1.41z",
)

val RainbowIcon: ImageVector = materialIcon(
    "Rainbow",
    "M11 9h2v2h-2zm-2 2h2v2H9zm4-4h2v2h-2zm2 2h2v2h-2zm-8 4h2v2H7zm2 2h2v2H9zm-2-8h2v2H7zm14-2H3v18h18V3zM7 15H5v2h2v-2zm10 0h2v2h-2v-2zm2 4h-2v2h2v-2zM5 19h2v-2H5v2z",
)

val FruitSaladIcon: ImageVector = materialIcon(
    "FruitSalad",
    "M6.05 8.05c-2.73 2.73-2.73 7.15-.02 9.88 1.47-3.4 4.09-6.24 7.36-7.93-2.77 2.34-4.71 5.61-5.39 9.32 2.6 1.23 5.8.78 7.95-1.37C19.43 14.47 20 4 20 4S9.53 4.57 6.05 8.05z",
)

val MonochromeIcon: ImageVector = materialIcon(
    "Monochrome",
    "M20 4h-3.17L15 2H9L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 14H4V6h4.05l1.83-2h4.24l1.83 2H20v12zM12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zm0 8.5c-1.93 0-3.5-1.57-3.5-3.5s1.57-3.5 3.5-3.5 3.5 1.57 3.5 3.5-1.57 3.5-3.5 3.5z",
)

val FidelityIcon: ImageVector = materialIcon(
    "Fidelity",
    "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z",
)

val AspectRatioIcon: ImageVector = materialIcon(
    "AspectRatio",
    "M19 12h-2v3h-3v2h5v-5zM7 9h3V7H5v5h2V9zm14-6H3c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16.01H3V4.99h18v14.02z",
)

val PaletteIcon: ImageVector = materialIcon(
    "Palette",
    "M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9c.83 0 1.5-.67 1.5-1.5 0-.39-.15-.74-.39-1.01-.23-.26-.38-.61-.38-.99 0-.83.67-1.5 1.5-1.5H16c2.76 0 5-2.24 5-5 0-4.42-4.03-8-9-8zm-5.5 9c-.83 0-1.5-.67-1.5-1.5S5.67 9 6.5 9 8 9.67 8 10.5 7.33 12 6.5 12zm3-4C8.67 8 8 7.33 8 6.5S8.67 5 9.5 5s1.5.67 1.5 1.5S10.33 8 9.5 8zm5 0c-.83 0-1.5-.67-1.5-1.5S13.67 5 14.5 5s1.5.67 1.5 1.5S15.33 8 14.5 8zm3 4c-.83 0-1.5-.67-1.5-1.5S16.67 9 17.5 9s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z",
)

val TranslateIcon: ImageVector = materialIcon(
    "Translate",
    "M12.87 15.07l-2.54-2.51.03-.03c1.74-1.94 2.98-4.17 3.71-6.53H17V4h-7V2H8v2H1v1.99h11.17C11.5 7.92 10.44 9.75 9 11.35 8.07 10.32 7.3 9.19 6.69 8h-2c.73 1.63 1.73 3.17 2.98 4.56l-5.09 5.02L4 19l5-5 3.11 3.11.76-2.04zM18.5 10h-2L12 22h2l1.12-3h4.75L21 22h2l-4.5-12zm-2.62 7l1.62-4.33L19.12 17h-3.24z",
)

val DarkModeIcon: ImageVector = materialIcon(
    "DarkMode",
    "M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9c0-.46-.04-.92-.1-1.36-.98 1.37-2.58 2.26-4.4 2.26-2.98 0-5.4-2.42-5.4-5.4 0-1.81.89-3.42 2.26-4.4-.44-.06-.9-.1-1.36-.1z",
)

val WallpaperIcon: ImageVector = materialIcon(
    "Wallpaper",
    "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z",
)

val FontIcon: ImageVector = materialIcon(
    "Font",
    "M9.93 13.5h4.14L12 7.98zM20 2H4c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-4.05 16.5l-1.14-3H9.17l-1.12 3H5.96l5.11-13h1.86l5.11 13h-2.09z",
)

val GlassIcon: ImageVector = materialIcon(
    "Glass",
    "M17.66 7.93L12 2.27 6.34 7.93c-3.12 3.12-3.12 8.19 0 11.31C7.9 20.8 9.95 21.58 12 21.58c2.05 0 4.1-.78 5.66-2.34 3.12-3.12 3.12-8.19 0-11.31zM12 19.59c-1.6 0-3.11-.62-4.24-1.76C6.62 16.69 6 15.19 6 13.59c0-1.6.62-3.11 1.76-4.24L12 5.1v14.49z",
)

val TimerIcon: ImageVector = materialIcon(
    "Timer",
    "M15 1H9v2h6V1zm-4 13h2V8h-2v6zm8.03-6.61l1.42-1.42c-.43-.51-.9-.99-1.41-1.41l-1.42 1.42C16.07 4.74 14.12 4 12 4c-4.97 0-9 4.03-9 9s4.02 9 9 9 9-4.03 9-9c0-2.12-.74-4.07-1.97-5.61zM12 20c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z",
)

val ZoomIcon: ImageVector = materialIcon(
    "Zoom",
    "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14zm.5-7H9v2H7v1h2v2h1v-2h2V9h-2z",
)

val AutoIcon: ImageVector = materialIcon(
    "Auto",
    "M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3c-.46-4.17-3.77-7.48-7.94-7.94V1h-2v2.06C6.83 3.52 3.52 6.83 3.06 11H1v2h2.06c.46 4.17 3.77 7.48 7.94 7.94V23h2v-2.06c4.17-.46 7.48-3.77 7.94-7.94H23v-2h-2.06zM12 19c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z",
)

val MonitorIcon: ImageVector = materialIcon(
    "Monitor",
    "M21 2H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h7v2H8v2h8v-2h-2v-2h7c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H3V4h18v12z",
)

val SuperuserIcon: ImageVector = materialIcon(
    "Superuser",
    "M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z",
)

val StyleIcon: ImageVector = materialIcon(
    "Style",
    "M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z",
)

val DownloadIcon: ImageVector = materialIcon(
    "Download",
    "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z",
)

val LaunchIcon: ImageVector = materialIcon(
    "Launch",
    "M19 19H5V5h7V3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z",
)

val DeleteIcon: ImageVector = materialIcon(
    "Delete",
    "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z",
)

val BatteryIcon: ImageVector = materialIcon(
    "Battery",
    "M15.67 4H14V2h-4v2H8.33C7.6 4 7 4.6 7 5.33v15.33C7 21.4 7.6 22 8.33 22h7.33c.74 0 1.34-.6 1.34-1.33V5.33C17 4.6 16.4 4 15.67 4z",
)

val FolderIcon: ImageVector = materialIcon(
    "Folder",
    "M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z",
)

val GamesIcon: ImageVector = materialIcon(
    "Games",
    "M21.58 16.09l-1.09-7.66C20.21 6.46 18.52 5 16.53 5H7.47C5.48 5 3.79 6.46 3.51 8.43l-1.09 7.66C2.2 17.63 3.39 19 4.94 19c.68 0 1.32-.27 1.8-.75L9 16h6l2.25 2.25c.48.48 1.13.75 1.8.75 1.55 0 2.74-1.37 2.53-2.91zM11 11H9v2H8v-2H6v-1h2V8h1v2h2v1zm4-1c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm2 3c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1z",
)

val DensityIcon: ImageVector = materialIcon(
    "Density",
    "M3 3v8h8V3H3zm6 6H5V5h4v4zm-6 4v8h8v-8H3zm6 6H5v-4h4v4zm4-16v8h8V3h-8zm6 6h-4V5h4v4zm-6 4v8h8v-8h-8zm6 6h-4v-4h4v4z",
)

val TrimIcon: ImageVector = materialIcon(
    "Trim",
    "M9.64 7.64c.23-.5.36-1.05.36-1.64 0-2.21-1.79-4-4-4S2 3.79 2 6s1.79 4 4 4c.59 0 1.14-.13 1.64-.36L10 12l-2.36 2.36C7.14 14.13 6.59 14 6 14c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4c0-.59-.13-1.14-.36-1.64L12 14l7 7h3v-1L9.64 7.64zM6 8c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm0 12c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm6-7.5c-.28 0-.5-.22-.5-.5s.22-.5.5-.5.5.22.5.5-.22.5-.5.5zM19 3l-6 6 2 2 7-7V3z",
)

val MusicIcon: ImageVector = materialIcon(
    "Music",
    "M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z",
)

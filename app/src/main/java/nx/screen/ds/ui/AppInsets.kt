package nx.screen.ds.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * Insets del sistema escalados a la densidad falsa de la app (appDpi).
 * Cuando la app se dibuja con una densidad distinta a la real, los insets
 * en px del sistema no escalan con el contenido y este termina debajo de
 * las barras (o con espacio de más). Estos insets compensan ese desfase
 * para que el área segura siga siendo proporcional al contenido.
 *
 * Los getters son composables y leen los insets actuales del sistema en
 * cada recomposición (IME, rotación, gestos), así que siempre van al día.
 */
data class ScaledInsets(val factor: Float) {

    val safe: WindowInsets @Composable get() = scaled(WindowInsets.safeDrawing)

    val navigationBars: WindowInsets @Composable get() = scaled(WindowInsets.navigationBars)

    val ime: WindowInsets @Composable get() = scaled(WindowInsets.ime)

    @Composable
    private fun scaled(insets: WindowInsets): WindowInsets {
        if (factor == 1f) return insets
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        return WindowInsets(
            left = (insets.getLeft(density, layoutDirection) * factor).toInt(),
            top = (insets.getTop(density) * factor).toInt(),
            right = (insets.getRight(density, layoutDirection) * factor).toInt(),
            bottom = (insets.getBottom(density) * factor).toInt(),
        )
    }
}

val LocalScaledInsets = staticCompositionLocalOf<ScaledInsets?> { null }

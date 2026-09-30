package nx.screen.ds.ui.component

import androidx.annotation.StringRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nx.screen.ds.R

data class ThemeColorOption(
    val key: String,
    @StringRes val labelId: Int,
    val lightPrimary: Color,
    val darkPrimary: Color,
)

val themeColorOptions = listOf(
    ThemeColorOption("indigo", R.string.color_indigo, Color(0xFF4355B9), Color(0xFFBAC3FF)),
    ThemeColorOption("azul", R.string.color_azul, Color(0xFF0061A4), Color(0xFF9ECAFF)),
    ThemeColorOption("cian", R.string.color_cian, Color(0xFF006876), Color(0xFF4FD8EB)),
    ThemeColorOption("esmeralda", R.string.color_esmeralda, Color(0xFF006A60), Color(0xFF52DEC9)),
    ThemeColorOption("verde", R.string.color_verde, Color(0xFF006E1A), Color(0xFF74DD69)),
    ThemeColorOption("menta", R.string.color_menta, Color(0xFF006C48), Color(0xFF6CFAAF)),
    ThemeColorOption("oro", R.string.color_oro, Color(0xFF785900), Color(0xFFF0C14A)),
    ThemeColorOption("naranja", R.string.color_naranja, Color(0xFF8B5000), Color(0xFFFFB870)),
    ThemeColorOption("rojo", R.string.color_rojo, Color(0xFFBB1614), Color(0xFFFFB4AA)),
    ThemeColorOption("rosa", R.string.color_rosa, Color(0xFFBC004B), Color(0xFFFFB1C3)),
    ThemeColorOption("fucsia", R.string.color_fucsia, Color(0xFF9A25AE), Color(0xFFEFB0FF)),
    ThemeColorOption("violeta", R.string.color_violeta, Color(0xFF6F43C0), Color(0xFFD4BBFF)),
)

@Composable
fun ThemeColorPicker(
    selectedColorKey: String,
    onColorSelected: (String) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    isDynamicColorSupported: Boolean = false,
    isDynamicColorEnabled: Boolean = false,
    onDynamicColorSelected: () -> Unit = {},
    bare: Boolean = false,
    titleRes: Int = R.string.color_theme,
    options: List<ThemeColorOption> = themeColorOptions,
) {
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(12.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(start = 8.dp),
            ) {
                if (isDynamicColorSupported) {
                    item(key = "system_dynamic") {
                        ThemeColorCircle(
                            label = stringResource(R.string.theme_system),
                            displayColor = MaterialTheme.colorScheme.primary,
                            isSelected = isDynamicColorEnabled,
                            isDynamic = true,
                            onClick = onDynamicColorSelected,
                        )
                    }
                }

                items(options, key = { it.key }) { theme ->
                    ThemeColorCircle(
                        label = stringResource(theme.labelId),
                        displayColor = if (isDarkTheme) theme.darkPrimary else theme.lightPrimary,
                        isSelected = !isDynamicColorEnabled && selectedColorKey == theme.key,
                        isDynamic = false,
                        onClick = { onColorSelected(theme.key) },
                    )
                }
            }
        }
    }

    if (bare) {
        content()
    } else {
        ExpressiveCard(modifier = modifier, flat = flat) {
            content()
        }
    }
}

@Composable
private fun ThemeColorCircle(
    label: String,
    displayColor: Color,
    isSelected: Boolean,
    isDynamic: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "colorScale",
    )

    val shape = if (isSelected) RoundedCornerShape(28.dp) else CircleShape

    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(scale)
                .clip(shape)
                .background(color = displayColor)
                .then(
                    if (isDynamic) {
                        Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.outline,
                            shape,
                        )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

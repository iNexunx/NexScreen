package nx.screen.ds.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nx.screen.ds.R
import nx.screen.ds.ui.component.SplicedColumnGroup
import nx.screen.ds.ui.theme.InfoCircleIcon
import nx.screen.ds.ui.theme.ShieldIcon
import nx.screen.ds.ui.theme.GamesIcon
import nx.screen.ds.ui.theme.PaletteIcon

@Composable
fun AboutScreen(appTitle: String? = null) {
    KeyboardScrollColumn(modifier = Modifier.fillMaxWidth()) {
        ScreenTitle(stringResource(R.string.tab_about))

        SplicedColumnGroup {
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val icon = appIcon()
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (icon != null) {
                            Image(
                                bitmap = icon,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            appTitleLabelRes(appTitle)?.let { stringResource(it) }
                                ?: stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            stringResource(R.string.about_version),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(R.string.about_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            item(key = "what") {
                AboutSectionItem(R.string.about_what, R.string.about_what_text, InfoCircleIcon)
            }
            item(key = "how") {
                AboutSectionItem(R.string.about_how, R.string.about_how_text, ShieldIcon)
            }
            item(key = "games") {
                AboutSectionItem(R.string.about_games, R.string.about_games_text, GamesIcon)
            }
            item(key = "permissions") {
                AboutSectionItem(R.string.about_permissions, R.string.about_permissions_text, Icons.Filled.Lock)
            }
            item(key = "themes") {
                AboutSectionItem(R.string.about_themes, R.string.about_themes_text, PaletteIcon)
            }
            item(key = "credits") {
                AboutSectionItem(R.string.about_credits, R.string.about_credits_text, Icons.Filled.Star, withVersion = true)
            }
        }
    }
}

@Composable
private fun appIcon(): ImageBitmap? {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    return remember {
        val size = (72 * density).toInt().coerceAtLeast(1)
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val inset = (size * 0.25f).toInt().coerceAtLeast(1)
            val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(-inset, -inset, size + inset, size + inset)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        }.getOrNull()
    }
}

@Composable
private fun AboutSectionItem(
    titleRes: Int,
    textRes: Int,
    icon: ImageVector,
    withVersion: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            if (withVersion) {
                stringResource(textRes, stringResource(R.string.about_version))
            } else {
                stringResource(textRes)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

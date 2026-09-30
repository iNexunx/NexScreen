package nx.screen.ds.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import nx.screen.ds.R
import nx.screen.ds.data.AppSettings
import nx.screen.ds.data.UiSystem
import nx.screen.ds.ui.component.NavigationSettingRow
import nx.screen.ds.ui.component.PresetSettingRow
import nx.screen.ds.ui.component.SplicedColumnGroup
import nx.screen.ds.ui.theme.AppFilterChip
import nx.screen.ds.ui.theme.TuneIcon
import nx.screen.ds.ui.theme.MonitorIcon
import nx.screen.ds.ui.theme.PaletteIcon
import nx.screen.ds.ui.theme.TranslateIcon

@Composable
fun SettingsScreen(
    onOpenTheme: () -> Unit,
    onOpenLanguages: () -> Unit,
    onOpenGeneral: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
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

    Box(modifier = Modifier.fillMaxSize()) {
        KeyboardScrollColumn(modifier = Modifier.fillMaxSize()) {
            ScreenTitle(stringResource(R.string.tab_settings))

            SplicedColumnGroup {
                item(key = "ui_system") {
                    PresetSettingRow(
                        icon = MonitorIcon,
                        title = stringResource(R.string.ui_style),
                        description = stringResource(R.string.ui_style_desc),
                    ) {
                        UiSystem.entries.forEach { system ->
                            AppFilterChip(
                                selected = settings.uiSystem == system,
                                onClick = { viewModel.setUiSystem(system) },
                                label = { Text(system.label) },
                            )
                        }
                    }
                }

                item(key = "nav_general") {
                    NavigationSettingRow(
                        icon = TuneIcon,
                        title = stringResource(R.string.settings_general),
                        description = stringResource(R.string.settings_general_desc),
                        onClick = onOpenGeneral,
                    )
                }

                item(key = "nav_theme") {
                    NavigationSettingRow(
                        icon = PaletteIcon,
                        title = stringResource(R.string.settings_theme),
                        description = stringResource(R.string.settings_theme_desc),
                        onClick = onOpenTheme,
                    )
                }

                item(key = "nav_languages") {
                    NavigationSettingRow(
                        icon = TranslateIcon,
                        title = stringResource(R.string.language),
                        description = stringResource(R.string.language_desc),
                        onClick = onOpenLanguages,
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

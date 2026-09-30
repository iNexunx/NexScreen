package nx.screen.ds.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import nx.screen.ds.ui.theme.AppOutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import nx.screen.ds.R
import nx.screen.ds.data.AppProfile
import nx.screen.ds.ui.component.ExpressiveCard
import nx.screen.ds.ui.component.ExpressiveSwitch
import nx.screen.ds.ui.component.SettingRowHeader
import nx.screen.ds.ui.component.SplicedColumnGroup
import nx.screen.ds.ui.theme.AppButton
import nx.screen.ds.ui.theme.AppFilterChip
import nx.screen.ds.ui.theme.AppOutlinedButton
import nx.screen.ds.ui.theme.AppSlider
import nx.screen.ds.ui.theme.statusGreen
import nx.screen.ds.ui.theme.statusRed
import nx.screen.ds.ui.theme.AppTextButton
import nx.screen.ds.ui.theme.AspectRatioIcon
import nx.screen.ds.ui.theme.AutoIcon
import nx.screen.ds.ui.theme.DeleteIcon
import nx.screen.ds.ui.theme.DensityIcon
import nx.screen.ds.ui.theme.LaunchIcon

@Composable
fun AppsScreen(viewModel: AppsViewModel = viewModel()) {
    val apps by viewModel.apps.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val usageAccess by viewModel.usageAccess.collectAsState()
    val serviceRunning by viewModel.serviceRunning.collectAsState()
    val backendReady by viewModel.backendReady.collectAsState()
    val message by viewModel.message.collectAsState()
    val failsafe by viewModel.failsafe.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var editingApp by remember { mutableStateOf<InstalledApp?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    BackHandler(enabled = editingApp != null) { editingApp = null }

    val context = LocalContext.current
    var snackText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        val m = message
        if (m != null) {
            snackText = if (m.arg != null) context.getString(m.res, m.arg) else context.getString(m.res)
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

    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.refreshOnResume()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val filtered = remember(apps, query, profiles) {
        val base = if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
        base.sortedWith(
            compareByDescending<InstalledApp> {
                profiles[it.packageName]?.let { p -> !p.isEmpty() } ?: false
            }.thenBy { it.label.lowercase() },
        )
    }

    androidx.compose.material3.Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        val app = editingApp
        if (app != null) {
            ProfileScreen(
                modifier = Modifier.padding(innerPadding),
                app = app,
                initial = profiles[app.packageName],
                backendReady = backendReady,
                onBack = { editingApp = null },
                onSave = { density, width, height, rotation ->
                    viewModel.saveProfile(app.packageName, density, width, height, rotation)
                    editingApp = null
                },
                onRemove = {
                    viewModel.removeProfile(app.packageName)
                    editingApp = null
                },
                onOpen = { viewModel.openApp(app.packageName) },
                onRestart = { viewModel.restartApp(app.packageName) },
                onForceStop = { viewModel.forceStop(app.packageName) },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ScreenTitle(stringResource(R.string.tab_apps))

                SplicedColumnGroup {
                    item(key = "auto") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            SettingRowHeader(
                                icon = AutoIcon,
                                title = stringResource(R.string.profiles_auto),
                                description = "",
                            )
                        }
                    }

                    item(key = "usage") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (!backendReady) {
                                Text(
                                    stringResource(R.string.backend_need_activation),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Text(
                                if (usageAccess) stringResource(R.string.usage_granted)
                                else stringResource(R.string.usage_denied),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (usageAccess) statusGreen() else statusRed(),
                            )
                            if (!usageAccess) {
                                AppButton(onClick = {
                                    viewModel.openUsageAccessSettings()
                                }) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        stringResource(R.string.grant_usage_access),
                                        modifier = Modifier.padding(start = 6.dp),
                                    )
                                }
                                Text(
                                    stringResource(R.string.usage_toggle_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    item(key = "monitor") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                if (serviceRunning) stringResource(R.string.monitoring_active)
                                else stringResource(R.string.monitoring_off),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (serviceRunning) statusGreen() else statusRed(),
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                            )
                            AppButton(
                                onClick = {
                                    if (serviceRunning) viewModel.stopService()
                                    else viewModel.startService()
                                },
                                enabled = usageAccess && backendReady,
                            ) {
                                Icon(
                                    if (serviceRunning) Icons.Filled.Close else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    if (serviceRunning) stringResource(R.string.stop) else stringResource(R.string.start),
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }

                    if (profiles.isNotEmpty()) {
                        item(key = "reset") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                AppTextButton(
                                    onClick = { confirmClear = true },
                                    enabled = backendReady,
                                ) {
                                    Icon(
                                        DeleteIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        stringResource(R.string.reset_profiles),
                                        modifier = Modifier.padding(start = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                AppOutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.setQuery(it) },
                    label = { Text(stringResource(R.string.search_app)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (apps.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            AppRow(
                                app = app,
                                profile = profiles[app.packageName],
                                onClick = ({ editingApp = app }).takeIf { backendReady },
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.reset_profiles)) },
            text = { Text(stringResource(R.string.confirm_reset_profiles)) },
            confirmButton = {
                AppTextButton(onClick = {
                    confirmClear = false
                    viewModel.clearAllProfiles()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                AppTextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    failsafe?.let { f ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelPending() },
            title = { Text(stringResource(R.string.confirm_dialog_title, f.label.lowercase())) },
            text = { Text(stringResource(R.string.confirm_dialog_text)) },
            confirmButton = {
                AppTextButton(onClick = { viewModel.confirmPending() }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                AppTextButton(onClick = { viewModel.cancelPending() }) { Text(stringResource(R.string.revert)) }
            },
        )
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    profile: AppProfile?,
    onClick: (() -> Unit)?,
) {
    ExpressiveCard(
        onClick = onClick,
        flat = true,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
            } else {
                Text("•", style = MaterialTheme.typography.headlineMedium)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    app.packageName + if (app.isSystem) stringResource(R.string.system_suffix) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (profile != null && !profile.isEmpty()) {
                Text(
                    profileSummary(profile),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    stringResource(R.string.no_profile),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    app: InstalledApp,
    initial: AppProfile?,
    backendReady: Boolean,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSave: (Int?, Int?, Int?) -> Unit,
    onRemove: () -> Unit,
    onOpen: () -> Unit,
    onRestart: () -> Unit,
    onForceStop: () -> Unit,
) {
    var densityEnabled by remember { mutableStateOf(initial?.density != null) }
    var density by remember { mutableStateOf(initial?.density ?: 440) }
    var densityField by remember { mutableStateOf(initial?.density?.toString() ?: "") }
    var sizeEnabled by remember { mutableStateOf(initial?.width != null || initial?.height != null) }
    var width by remember { mutableStateOf(initial?.width?.toString() ?: "") }
    var height by remember { mutableStateOf(initial?.height?.toString() ?: "") }
    var rotationEnabled by remember { mutableStateOf(initial?.rotation != null) }
    var rotation by remember { mutableStateOf(initial?.rotation ?: 1) }

    KeyboardScrollColumn(
        modifier = modifier,
        spacing = 12.dp,
    ) {
        ScreenHeader(title = app.label, onBack = onBack)
        Text(
            app.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SplicedColumnGroup {
            item(key = "actions") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppOutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
                            Icon(
                                LaunchIcon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                stringResource(R.string.open),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                        AppOutlinedButton(onClick = onRestart, enabled = backendReady, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                stringResource(R.string.restart),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                        AppOutlinedButton(onClick = onForceStop, enabled = backendReady, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                stringResource(R.string.close),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                    if (!backendReady) {
                        Text(
                            stringResource(R.string.restart_needs_backend),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            item(key = "density") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = DensityIcon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                stringResource(R.string.custom_density),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        ExpressiveSwitch(
                            checked = densityEnabled,
                            onCheckedChange = { densityEnabled = it },
                            enabled = backendReady,
                        )
                    }
                    AnimatedVisibility(visible = densityEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppSlider(
                                value = density.toFloat(),
                                onValueChange = { density = (it / 20).toInt() * 20 },
                                valueRange = MIN_DPI_SLIDER.toFloat()..MAX_DPI.toFloat(),
                                steps = 27,
                                enabled = backendReady,
                            )
                            Text(stringResource(R.string.density_value, density), style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                DENSITY_PRESETS.forEach { p ->
                                    AppFilterChip(
                                        selected = density == p,
                                        onClick = { density = p },
                                        enabled = backendReady,
                                        label = { Text("$p") },
                                    )
                                }
                            }
                            AppOutlinedTextField(
                                value = densityField,
                                onValueChange = {
                                    densityField = it
                                    it.toIntOrNull()?.let { v -> density = v.coerceIn(MIN_DPI, MAX_DPI) }
                                },
                                label = { Text(stringResource(R.string.custom_dpi)) },
                                supportingText = {
                                    Text(stringResource(R.string.dpi_range, MIN_DPI, MAX_DPI))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = backendReady,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            item(key = "rotation") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                stringResource(R.string.custom_rotation),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        ExpressiveSwitch(
                            checked = rotationEnabled,
                            onCheckedChange = { rotationEnabled = it },
                            enabled = backendReady,
                        )
                    }
                    AnimatedVisibility(visible = rotationEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ROTATION_OPTIONS.forEach { opt ->
                                    AppFilterChip(
                                        selected = rotation == opt.value,
                                        onClick = { rotation = opt.value },
                                        enabled = backendReady,
                                        label = { Text(opt.label) },
                                    )
                                }
                            }
                            Text(
                                stringResource(R.string.rotation_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item(key = "resolution") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = AspectRatioIcon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                stringResource(R.string.custom_resolution),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        ExpressiveSwitch(
                            checked = sizeEnabled,
                            onCheckedChange = { sizeEnabled = it },
                            enabled = backendReady,
                        )
                    }
                    AnimatedVisibility(visible = sizeEnabled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppOutlinedTextField(
                                value = width,
                                onValueChange = { width = it },
                                label = { Text(stringResource(R.string.width_short)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = backendReady,
                                modifier = Modifier.weight(1f),
                            )
                            AppOutlinedTextField(
                                value = height,
                                onValueChange = { height = it },
                                label = { Text(stringResource(R.string.height_short)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = backendReady,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            item(key = "save") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppButton(
                        onClick = {
                            onSave(
                                if (densityEnabled) density else null,
                                if (sizeEnabled) width.toIntOrNull() else null,
                                if (sizeEnabled) height.toIntOrNull() else null,
                                if (rotationEnabled) rotation else null,
                            )
                        },
                        enabled = backendReady,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            stringResource(R.string.save),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    AppTextButton(onClick = onRemove, enabled = backendReady) {
                        Icon(
                            DeleteIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            stringResource(R.string.remove_profile),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun profileSummary(p: AppProfile): String = buildList {
    p.density?.let { add("${it}dpi") }
    if (p.width != null && p.height != null) add("${p.width}x${p.height}")
    p.rotation?.let { add(rotationLabel(it)) }
}.joinToString(" · ")

/**
 * `wm density` no impone un minimo real: acepta valores muy por debajo de
 * ldpi (120). Por debajo de ~72 los layouts empiezan a romperse de verdad
 * (campos de texto recortados, elementos solapados), asi que ahi paramos.
 */
private const val MIN_DPI = 72
private const val MIN_DPI_SLIDER = 80
private const val MAX_DPI = 640

private val DENSITY_PRESETS = listOf(
    72, 80, 100, 120, 140, 160, 200, 240, 280,
    320, 360, 400, 440, 480, 520, 560, 640,
)

private fun rotationLabel(rotation: Int): String = when (rotation) {
    0 -> "Vertical"
    1 -> "Horizontal izquierda"
    3 -> "Horizontal derecha"
    2 -> "Invertida 180°"
    else -> "Auto"
}

private data class RotationOption(val value: Int, val label: String)

private val ROTATION_OPTIONS = listOf(
    RotationOption(1, "Izquierda"),
    RotationOption(3, "Derecha"),
    RotationOption(0, "Vertical"),
    RotationOption(2, "180°"),
)

package nx.screen.ds.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import nx.screen.ds.R
import nx.screen.ds.data.BackendMode
import nx.screen.ds.ui.component.ExpressiveSwitch
import nx.screen.ds.ui.component.SettingRowHeader
import nx.screen.ds.ui.component.SplicedColumnGroup
import nx.screen.ds.ui.component.SplicedGroupScope
import nx.screen.ds.ui.theme.AppButton
import nx.screen.ds.ui.theme.AppTextButton
import nx.screen.ds.ui.theme.AspectRatioIcon
import nx.screen.ds.ui.theme.DensityIcon
import nx.screen.ds.ui.theme.DownloadIcon
import nx.screen.ds.ui.theme.LaunchIcon
import nx.screen.ds.ui.theme.ShieldIcon
import nx.screen.ds.ui.theme.SuperuserIcon
import nx.screen.ds.ui.theme.statusGreen
import nx.screen.ds.ui.theme.statusRed
import rikka.shizuku.Shizuku

private enum class ResetKind { DENSITY, SIZE, SYSTEM }

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val density by viewModel.density.collectAsState()
    val physicalDensity by viewModel.physicalDensity.collectAsState()
    val size by viewModel.size.collectAsState()
    val physicalSize by viewModel.physicalSize.collectAsState()
    val backend by viewModel.backend.collectAsState()
    val backendReady by viewModel.backendReady.collectAsState()
    val enabled by viewModel.enabled.collectAsState()
    val backendMode by viewModel.backendMode.collectAsState()
    val shizukuStatus by viewModel.shizukuStatus.collectAsState()
    val rootAvailable by viewModel.rootAvailable.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val message by viewModel.message.collectAsState()
    val failsafe by viewModel.failsafe.collectAsState()
    val shizukuInstalled by viewModel.shizukuInstalled.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { requestCode, _ ->
            if (requestCode == HomeViewModel.PERMISSION_REQUEST_CODE) viewModel.refresh(force = true)
        }
        if (Build.VERSION.SDK_INT >= 24) {
            Shizuku.addRequestPermissionResultListener(listener)
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= 24) {
                Shizuku.removeRequestPermissionResultListener(listener)
            }
        }
    }

    var sliderDensity by rememberSaveable { mutableStateOf(density ?: 440) }
    var densityField by rememberSaveable { mutableStateOf(density?.toString() ?: "") }
    var widthField by rememberSaveable { mutableStateOf(size?.w?.toString() ?: "") }
    var heightField by rememberSaveable { mutableStateOf(size?.h?.toString() ?: "") }
    var confirmResetName by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var lastSyncedDensity by rememberSaveable { mutableStateOf(density) }
    var lastSyncedSize by rememberSaveable { mutableStateOf(size?.toString()) }

    val confirmReset = confirmResetName?.let { kind ->
        ResetKind.entries.firstOrNull { it.name == kind }
    }

    LaunchedEffect(density, failsafe) {
        val d = density
        if (d != null && failsafe == null && !editing && lastSyncedDensity != d) {
            sliderDensity = d
            densityField = d.toString()
            lastSyncedDensity = d
        }
    }
    LaunchedEffect(size, failsafe) {
        val s = size
        if (s != null && failsafe == null && !editing && lastSyncedSize != s.toString()) {
            widthField = s.w.toString()
            heightField = s.h.toString()
            lastSyncedSize = s.toString()
        }
    }
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
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val densityValid = sliderDensity > 0
    val applyWidth = widthField.toIntOrNull()
    val applyHeight = heightField.toIntOrNull()
    val sizeValid = applyWidth != null && applyHeight != null

    var homeTab by rememberSaveable { mutableStateOf(0) }
    var tabAutoSwitched by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(enabled, backendReady) {
        if (!tabAutoSwitched && enabled && backendReady) {
            tabAutoSwitched = true
            homeTab = 1
        }
    }

    androidx.compose.material3.Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        KeyboardScrollColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(
                selectedTabIndex = homeTab,
                containerColor = Color.Transparent,
                divider = {},
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                Tab(
                    selected = homeTab == 0,
                    onClick = { homeTab = 0 },
                    icon = { Icon(ShieldIcon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = { Text(stringResource(R.string.tab_backend)) },
                )
                Tab(
                    selected = homeTab == 1,
                    onClick = { homeTab = 1 },
                    icon = { Icon(DensityIcon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = { Text(stringResource(R.string.home_tab_screen)) },
                )
            }

            if (homeTab == 0) {
                SplicedColumnGroup {
                    BackendGroupItems(
                        scope = this,
                        backend = backend,
                        ready = backendReady,
                        enabled = enabled,
                        backendMode = backendMode,
                        shizukuStatus = shizukuStatus,
                        rootAvailable = rootAvailable,
                        busy = busy,
                        shizukuInstalled = shizukuInstalled,
                        onRefresh = { viewModel.detectNow() },
                        onSetEnabled = viewModel::setEnabled,
                        onActivate = viewModel::activateMode,
                        onRequestShizuku = { viewModel.requestShizukuPermission() },
                        onOpenShizuku = {
                            val intent = context.packageManager
                                .getLaunchIntentForPackage("com.hamondev.shevery")
                                ?: context.packageManager
                                    .getLaunchIntentForPackage("moe.shizuku.privileged.api")
                            if (intent != null) context.startActivity(intent)
                        },
                        onDownloadShizuku = { openUrl(context, "https://github.com/HmnDev-Tech/shevery") },
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                SplicedColumnGroup {
                    item(key = "density") {
                        DensityItem(
                            current = density,
                            physical = physicalDensity,
                            enabled = enabled && backendReady,
                            fieldValue = densityField,
                            onFieldChange = { input ->
                                val digits = input.filter(Char::isDigit).take(9)
                                densityField = digits
                                digits.toIntOrNull()
                                    ?.takeIf { it > 0 }
                                    ?.let { sliderDensity = it }
                            },
                            onFocusChange = { editing = it },
                            onReset = { confirmResetName = ResetKind.DENSITY.name },
                        )
                    }

                    item(key = "size") {
                        SizeItem(
                            current = size,
                            physical = physicalSize,
                            width = widthField,
                            height = heightField,
                            enabled = enabled && backendReady,
                            onWidthChange = { widthField = it },
                            onHeightChange = { heightField = it },
                            onFocusChange = { editing = it },
                            onReset = { confirmResetName = ResetKind.SIZE.name },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AppButton(
                        onClick = { viewModel.applyDensityAndSize(sliderDensity, applyWidth, applyHeight) },
                        enabled = enabled && backendReady && (densityValid || sizeValid),
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            stringResource(R.string.apply_all),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AppTextButton(
                        onClick = { confirmResetName = ResetKind.SYSTEM.name },
                        enabled = enabled && backendReady,
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            stringResource(R.string.reset_system),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                }
            }
        }
    }

    confirmReset?.let { kind ->
        AlertDialog(
            onDismissRequest = { confirmResetName = null },
            title = {
                Text(
                    when (kind) {
                        ResetKind.SYSTEM -> stringResource(R.string.reset_system)
                        else -> stringResource(R.string.reset)
                    },
                )
            },
            text = {
                Text(
                    when (kind) {
                        ResetKind.SYSTEM -> stringResource(R.string.confirm_reset_system)
                        else -> stringResource(R.string.confirm_reset)
                    },
                )
            },
            confirmButton = {
                AppTextButton(onClick = {
                    confirmResetName = null
                    when (kind) {
                        ResetKind.DENSITY -> physicalDensity?.let { p ->
                            sliderDensity = p
                            densityField = p.toString()
                            viewModel.applyDensity(p)
                        }
                        ResetKind.SIZE -> physicalSize?.let { p ->
                            widthField = p.w.toString()
                            heightField = p.h.toString()
                            viewModel.applySize(p.w, p.h)
                        }
                        ResetKind.SYSTEM -> viewModel.resetSystem()
                    }
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                AppTextButton(onClick = { confirmResetName = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    failsafe?.let { f ->
        val failsafeTitle = f.labelRes?.let { stringResource(it) } ?: f.label
        AlertDialog(
            onDismissRequest = { viewModel.cancelPending() },
            title = { Text(stringResource(R.string.confirm_dialog_title, failsafeTitle.lowercase())) },
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

private fun BackendGroupItems(
    scope: SplicedGroupScope,
    backend: String?,
    ready: Boolean,
    enabled: Boolean,
    backendMode: BackendMode,
    shizukuStatus: ShizukuStatus,
    rootAvailable: Boolean,
    busy: Boolean,
    shizukuInstalled: Boolean,
    onRefresh: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
    onActivate: (BackendMode) -> Unit,
    onRequestShizuku: () -> Unit,
    onOpenShizuku: () -> Unit,
    onDownloadShizuku: () -> Unit,
) {
    val selectedMode = if (backendMode == BackendMode.AUTO) {
        when (backend) {
            "Shevery" -> BackendMode.SHIZUKU
            "Root (su)" -> BackendMode.ROOT
            else -> null
        }
    } else {
        backendMode
    }
    val modes = listOf(BackendMode.SHIZUKU, BackendMode.ROOT)

    scope.item(key = "header") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = ShieldIcon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        stringResource(R.string.permission_method),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (enabled && ready) {
                            stringResource(R.string.backend_active, backend ?: "?")
                        } else {
                            stringResource(R.string.backend_off)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled && ready) {
                            statusGreen()
                        } else {
                            statusRed()
                        },
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                ExpressiveSwitch(checked = enabled, onCheckedChange = onSetEnabled)
            }
        }
    }

    modes.forEach { mode ->
        scope.item(key = "mode_${mode.name}") {
            val selected = selectedMode == mode && enabled
            val onSelect = {
                if (!selected) onActivate(mode)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelect)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    backendModeIcon(mode),
                    contentDescription = null,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        backendModeLabel(mode),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    modeStatusLabel(mode, ready, selectedMode, shizukuStatus, rootAvailable)?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = modeStatusColor(mode, ready, shizukuStatus, rootAvailable),
                        )
                    } ?: Text(
                        backendModeDesc(mode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onSelect),
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                    )
                }
            }
        }
    }

    scope.item(
        key = "help",
        visible = !enabled ||
            (backendMode == BackendMode.ROOT && !ready) ||
            backendMode == BackendMode.SHIZUKU ||
            rootAvailable,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                !enabled -> Text(
                    stringResource(R.string.backend_disabled_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                backendMode == BackendMode.ROOT && !ready -> Text(
                    stringResource(R.string.root_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                shizukuStatus == ShizukuStatus.READY && backendMode == BackendMode.SHIZUKU -> Text(stringResource(R.string.shizuku_ready))
                shizukuStatus == ShizukuStatus.NEEDS_PERMISSION && backendMode == BackendMode.SHIZUKU -> {
                    Text(stringResource(R.string.shizuku_needs_permission))
                    AppButton(onClick = onRequestShizuku) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            stringResource(R.string.grant_permission),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                shizukuStatus == ShizukuStatus.NOT_RUNNING && backendMode == BackendMode.SHIZUKU -> {
                    Text(stringResource(R.string.shizuku_not_running))
                    if (shizukuInstalled) {
                        AppButton(onClick = onOpenShizuku) {
                            Icon(
                                LaunchIcon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                stringResource(R.string.open_shizuku),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    } else {
                        AppButton(onClick = onDownloadShizuku) {
                            Icon(
                                DownloadIcon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                stringResource(R.string.download_shizuku),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                }
            }
            if (rootAvailable) Text(stringResource(R.string.root_detected))
        }
    }

    scope.item(key = "detect") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AppTextButton(onClick = onRefresh) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    stringResource(R.string.detect_again),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun backendModeIcon(mode: BackendMode): ImageVector = when (mode) {
    BackendMode.SHIZUKU -> ShieldIcon
    BackendMode.ROOT -> SuperuserIcon
    BackendMode.AUTO -> ShieldIcon
}

@Composable
private fun backendModeLabel(mode: BackendMode): String = when (mode) {
    BackendMode.SHIZUKU -> stringResource(R.string.backend_mode_shizuku)
    BackendMode.ROOT -> stringResource(R.string.backend_mode_root)
    BackendMode.AUTO -> stringResource(R.string.backend_mode_auto)
}

@Composable
private fun backendModeDesc(mode: BackendMode): String = when (mode) {
    BackendMode.SHIZUKU -> stringResource(R.string.backend_shizuku_desc)
    BackendMode.ROOT -> stringResource(R.string.backend_root_desc)
    BackendMode.AUTO -> stringResource(R.string.backend_auto_desc)
}

@Composable
private fun modeStatusLabel(
    mode: BackendMode,
    ready: Boolean,
    selectedMode: BackendMode?,
    shizukuStatus: ShizukuStatus,
    rootAvailable: Boolean,
): String? = when (mode) {
    BackendMode.SHIZUKU -> when (shizukuStatus) {
        ShizukuStatus.READY -> stringResource(R.string.backend_status_ready)
        ShizukuStatus.NEEDS_PERMISSION -> stringResource(R.string.backend_status_permission)
        ShizukuStatus.NOT_RUNNING -> stringResource(R.string.backend_status_not_running)
    }
    BackendMode.ROOT -> if (rootAvailable) {
        stringResource(R.string.backend_status_ready)
    } else {
        stringResource(R.string.backend_status_not_detected)
    }
    BackendMode.AUTO -> null
}

@Composable
private fun modeStatusColor(
    mode: BackendMode,
    ready: Boolean,
    shizukuStatus: ShizukuStatus,
    rootAvailable: Boolean,
): Color = when (mode) {
    BackendMode.SHIZUKU -> when (shizukuStatus) {
        ShizukuStatus.READY -> statusGreen()
        ShizukuStatus.NEEDS_PERMISSION -> statusRed()
        ShizukuStatus.NOT_RUNNING -> statusRed()
    }
    BackendMode.ROOT -> if (rootAvailable) statusGreen() else statusRed()
    BackendMode.AUTO -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun openUrl(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (runCatching { context.startActivity(intent) }.isFailure) {
        runCatching {
            val browser = context.packageManager
                .queryIntentActivities(intent, 0)
                .firstOrNull { it.activityInfo.packageName != context.packageName }
                ?.activityInfo?.packageName
            if (browser != null) {
                intent.setPackage(browser)
                context.startActivity(intent)
            } else {
                context.startActivity(Intent.createChooser(intent, null))
            }
        }
    }
}

@Composable
private fun DensityItem(
    current: Int?,
    physical: Int?,
    enabled: Boolean,
    fieldValue: String,
    onFieldChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
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
            SettingRowHeader(
                icon = DensityIcon,
                title = stringResource(R.string.density_title),
                description = "",
                modifier = Modifier.weight(1f),
            )
            // Compara contra el valor realmente aplicado, no contra el campo:
            // asi "reset" aparece solo cuando `wm` tiene un override puesto.
            if (current != null && current != physical) {
                AppTextButton(onClick = onReset, enabled = enabled) { Text(stringResource(R.string.reset)) }
            }
        }
        Text(
            if (current != null) stringResource(R.string.density_current, current, physical?.toString() ?: "?")
            else stringResource(R.string.unknown),
            style = MaterialTheme.typography.bodyMedium,
        )
        // Solo entrada manual: el valor se envia tal cual a `wm density`,
        // que es quien decide si lo acepta. Sin slider ni presets.
        AppOutlinedTextField(
            value = fieldValue,
            onValueChange = onFieldChange,
            label = { Text(stringResource(R.string.custom_value)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChange(it.isFocused) },
        )
    }
}

@Composable
private fun SizeItem(
    current: nx.screen.ds.core.Size?,
    physical: nx.screen.ds.core.Size?,
    width: String,
    height: String,
    enabled: Boolean,
    onWidthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
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
            SettingRowHeader(
                icon = AspectRatioIcon,
                title = stringResource(R.string.resolution_title),
                description = "",
                modifier = Modifier.weight(1f),
            )
            if (current != physical) {
                AppTextButton(onClick = onReset, enabled = enabled) { Text(stringResource(R.string.reset)) }
            }
        }
        Text(
            if (current != null) stringResource(R.string.resolution_current, current.toString(), physical?.toString() ?: "?")
            else stringResource(R.string.unknown),
            style = MaterialTheme.typography.bodyMedium,
        )
        // Solo entrada manual de ancho y alto. Sin presets: `wm size` valida.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppOutlinedTextField(
                value = width,
                onValueChange = onWidthChange,
                label = { Text(stringResource(R.string.width_field)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onFocusChange(it.isFocused) },
            )
            AppOutlinedTextField(
                value = height,
                onValueChange = onHeightChange,
                label = { Text(stringResource(R.string.height_field)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onFocusChange(it.isFocused) },
            )
        }
    }
}

@Composable
fun GlassProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(scheme.surfaceContainerHighest.copy(alpha = 0.5f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .background(scheme.primary.copy(alpha = 0.9f)),
        )
    }
}


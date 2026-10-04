package nx.screen.ds.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import nx.screen.ds.ui.theme.AppOutlinedTextField
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import nx.screen.ds.R
import nx.screen.ds.data.AppSettings
import nx.screen.ds.ui.component.PresetSettingRow
import nx.screen.ds.ui.component.SettingRowHeader
import nx.screen.ds.ui.component.SplicedColumnGroup
import nx.screen.ds.ui.component.ToggleSettingCard
import nx.screen.ds.ui.component.WarningCard
import nx.screen.ds.ui.theme.AppFilterChip
import nx.screen.ds.ui.theme.AppTextButton
import nx.screen.ds.ui.theme.AppsIcon
import nx.screen.ds.ui.theme.AspectRatioIcon
import nx.screen.ds.ui.theme.BatteryIcon
import nx.screen.ds.ui.theme.DeleteIcon
import nx.screen.ds.ui.theme.FontIcon
import nx.screen.ds.ui.theme.LaunchIcon
import nx.screen.ds.ui.theme.ZoomIcon
import nx.screen.ds.ui.theme.statusGreen
import nx.screen.ds.ui.theme.statusRed

@Composable
fun GeneralScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }

    KeyboardScrollColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color.Transparent),
    ) {
        ScreenHeader(title = stringResource(R.string.settings_general), onBack = onBack)

        SplicedColumnGroup {
            item(key = "app_title") {
                PresetSettingRow(
                    icon = FontIcon,
                    title = stringResource(R.string.app_title),
                    description = stringResource(R.string.app_title_desc),
                ) {
                    APP_TITLE_PRESETS.forEach { preset ->
                        AppFilterChip(
                            selected = settings.appTitle == preset.key,
                            onClick = { viewModel.setAppTitle(preset.key) },
                            label = { Text(stringResource(preset.labelRes)) },
                        )
                    }
                }
            }

            item(key = "battery_saver") {
                ToggleSettingCard(
                    title = stringResource(R.string.battery_saver),
                    description = stringResource(R.string.battery_saver_desc),
                    checked = settings.batterySaver,
                    icon = BatteryIcon,
                    onCheckedChange = viewModel::setBatterySaver,
                )
            }

            if (needsManualAutostart()) {
                item(key = "autostart_guide") {
                    AutostartGuideRow(
                        handled = settings.autostartHandled,
                        onHandled = { viewModel.setAutostartHandled(true) },
                    )
                }
            }

            item(key = "battery_exempt") {
                BatteryExemptRow()
            }

            item(key = "alternate_icon") {
                ToggleSettingCard(
                    title = stringResource(R.string.alternate_icon),
                    description = stringResource(R.string.alternate_icon_desc),
                    checked = settings.alternateIcon,
                    icon = AppsIcon,
                    onCheckedChange = viewModel::setAlternateIcon,
                )
            }

            item(key = "floating_nav") {
                ToggleSettingCard(
                    title = stringResource(R.string.floating_nav_bar),
                    description = stringResource(R.string.floating_nav_bar_desc),
                    checked = settings.floatingNavBar,
                    icon = AspectRatioIcon,
                    onCheckedChange = viewModel::setFloatingNavBar,
                )
            }

            item(key = "app_dpi") {
                AppDpiRow(settings = settings, viewModel = viewModel)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AppTextButton(onClick = { confirmClear = true }) {
                Icon(
                    DeleteIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    stringResource(R.string.clear_app_data),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_app_data)) },
            text = { Text(stringResource(R.string.confirm_clear_app_data)) },
            confirmButton = {
                AppTextButton(onClick = {
                    confirmClear = false
                    viewModel.clearAppData()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                AppTextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun BatteryExemptRow() {
    val context = LocalContext.current
    var exempt by remember { mutableStateOf(isBatteryExempt(context)) }
    var resumeTick by remember { mutableStateOf(0) }
    var recheckTick by remember { mutableStateOf(0) }
    val activity = context as? Activity
    val lifecycle = (activity as? LifecycleOwner)?.lifecycle ?: LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeTick++
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(resumeTick, recheckTick) {
        if (resumeTick > 0 || recheckTick > 0) {
            for (attempt in 1..12) {
                delay(400)
                exempt = isBatteryExempt(context)
                if (exempt) break
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingRowHeader(
            icon = BatteryIcon,
            title = stringResource(R.string.battery_exempt),
            description = stringResource(R.string.battery_exempt_desc),
        )
        Text(
            if (exempt) {
                stringResource(R.string.battery_exempt_granted)
            } else {
                stringResource(R.string.battery_exempt_denied)
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (exempt) {
                statusGreen()
            } else {
                statusRed()
            },
        )
        if (!exempt) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AppTextButton(onClick = { recheckTick++ }) {
                    Text(stringResource(R.string.battery_exempt_recheck))
                }
                AppTextButton(onClick = { openBatteryExemptSettings(context) }) {
                    Text(
                        stringResource(R.string.battery_exempt_open),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

/** Fabricantes que matan servicios en segundo plano sin "Autostart" manual. */
private val OEM_NEEDS_AUTOSTART = setOf(
    "xiaomi",
    "redmi",
    "pooc",
    "huawei",
    "honor",
    "oppo",
    "vivo",
    "oneplus",
    "realme",
    "infinix",
    "tecno",
    "itel",
)

fun needsManualAutostart(): Boolean {
    val brand = android.os.Build.MANUFACTURER.lowercase()
    return OEM_NEEDS_AUTOSTART.any { brand.contains(it) }
}

fun oemAutostartInstructions(): String = when {
    android.os.Build.MANUFACTURER.lowercase().contains("xiaomi") ->
        "MIUI/HyperOS: Ajustes > Apps > Administrar aplicaciones > NexScreen > Autostart: activar."
    android.os.Build.MANUFACTURER.lowercase().contains("huawei") ||
        android.os.Build.MANUFACTURER.lowercase().contains("honor") ->
        "EMUI: Ajustes > Batería > Inicio de aplicaciones > NexScreen > Gestionar manualmente: permitir."
    android.os.Build.MANUFACTURER.lowercase().contains("oppo") ||
        android.os.Build.MANUFACTURER.lowercase().contains("oneplus") ||
        android.os.Build.MANUFACTURER.lowercase().contains("realme") ->
        "ColorOS: Ajustes > Batería > Administración de aplicaciones > NexScreen > Permitir arranque automático."
    android.os.Build.MANUFACTURER.lowercase().contains("vivo") ->
        "Funtouch/OriginOS: Administrador de aplicaciones > Autostart > permitir NexScreen."
    android.os.Build.MANUFACTURER.lowercase().contains("infinix") ||
        android.os.Build.MANUFACTURER.lowercase().contains("tecno") ||
        android.os.Build.MANUFACTURER.lowercase().contains("itel") ->
        "XOS: Administrador de aplicaciones > Inicio automático > activar NexScreen."
    else ->
        "Activa el inicio automático (Autostart) de NexScreen en los ajustes de aplicaciones del sistema."
}

fun openOemAutostartSettings(context: Context) {
    val brand = android.os.Build.MANUFACTURER.lowercase()
    val candidates = buildList {
        when {
            brand.contains("xiaomi") -> {
                add(
                    Intent("miui.intent.action.OP_BACKGROUND_STRT_BTN")
                        .setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                )
            }
            brand.contains("huawei") || brand.contains("honor") -> {
                add(launchIntent(context, "com.huawei.systemmanager"))
                add(launchIntent(context, "com.hihonor.systemmanager"))
            }
            brand.contains("oppo") || brand.contains("realme") -> {
                add(launchIntent(context, "com.coloros.safecenter"))
                add(launchIntent(context, "com.coloros.oppoguardelf"))
            }
            brand.contains("oneplus") -> {
                add(launchIntent(context, "com.oneplus.security"))
                add(launchIntent(context, "com.coloros.safecenter"))
            }
            brand.contains("vivo") || brand.contains("iqoo") -> {
                add(launchIntent(context, "com.vivo.permissionmanager"))
            }
            brand.contains("infinix") || brand.contains("tecno") || brand.contains("itel") -> {
                add(launchIntent(context, "com.transsion.phonemanager"))
            }
        }
    }.filterNotNull()
    candidates.firstOrNull {
        runCatching { context.packageManager.resolveActivity(it, 0) != null }.getOrDefault(false)
    }?.let { intent ->
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

private fun launchIntent(context: Context, pkg: String): Intent? =
    context.packageManager.getLaunchIntentForPackage(pkg)

@Composable
private fun AutostartGuideRow(handled: Boolean, onHandled: () -> Unit) {
    val context = LocalContext.current
    var opened by remember { mutableStateOf(false) }
    val activity = context as? Activity
    val lifecycle = (activity as? LifecycleOwner)?.lifecycle ?: LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && opened) {
                onHandled()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingRowHeader(
            icon = LaunchIcon,
            title = stringResource(R.string.autostart_title),
            description = stringResource(R.string.autostart_desc),
        )
        if (handled) {
            Text(
                stringResource(R.string.autostart_done),
                style = MaterialTheme.typography.bodySmall,
                color = statusGreen(),
            )
        } else {
            WarningCard(
                message = oemAutostartInstructions(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AppTextButton(onClick = {
                    opened = true
                    openOemAutostartSettings(context)
                }) {
                    Icon(
                        LaunchIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(R.string.autostart_open),
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppDpiRow(
    settings: AppSettings,
    viewModel: SettingsViewModel,
) {
    var dpiText by remember { mutableStateOf(settings.appDpi?.toString().orEmpty()) }
    val context = LocalContext.current
    LaunchedEffect(dpiText) {
        val v = dpiText.toIntOrNull()
        if (v != null) {
            delay(700)
            val current = dpiText.toIntOrNull()
            if (current != null) viewModel.setAppDpi(current)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingRowHeader(
            icon = ZoomIcon,
            title = stringResource(R.string.app_dpi),
            description = stringResource(
                R.string.app_dpi_desc,
                settings.appDpi?.toString() ?: stringResource(R.string.app_dpi_system),
            ),
        )
        AppOutlinedTextField(
            value = dpiText,
            onValueChange = { new -> dpiText = new.filter { it.isDigit() }.take(9) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(stringResource(R.string.app_dpi_hint)) },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AppTextButton(onClick = { dpiText = ""; viewModel.setAppDpi(null) }) {
                Text(stringResource(R.string.reset))
            }
        }
    }
}


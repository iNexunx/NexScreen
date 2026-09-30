package nx.screen.ds.ui

import android.app.Application
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import nx.screen.ds.R
import nx.screen.ds.core.AppActions
import nx.screen.ds.core.AppShell
import nx.screen.ds.core.ProfileService
import nx.screen.ds.core.Size
import nx.screen.ds.core.WmController
import nx.screen.ds.data.AppProfile
import nx.screen.ds.data.AppProfileStore
import nx.screen.ds.data.BackendMode
import nx.screen.ds.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiMessage(val res: Int, val arg: String? = null)

data class InstalledApp(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap?,
    val isSystem: Boolean,
)

class AppsViewModel(app: Application) : AndroidViewModel(app) {

    private val pm: PackageManager = app.packageManager
    private val store = AppProfileStore(app)
    private val settingsStore = SettingsStore(app)

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    private val _profiles = MutableStateFlow<Map<String, AppProfile>>(emptyMap())
    val profiles: StateFlow<Map<String, AppProfile>> = _profiles
    private val _usageAccess = MutableStateFlow(false)
    val usageAccess: StateFlow<Boolean> = _usageAccess
    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning
    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message
    private val _backendReady = MutableStateFlow(false)
    val backendReady: StateFlow<Boolean> = _backendReady
    private val _failsafe = MutableStateFlow<Failsafe?>(null)
    val failsafe: StateFlow<Failsafe?> = _failsafe

    private var lastGoodDensity: Int? = null
    private var lastGoodSize: Size? = null

    private val backendListener: () -> Unit = { refreshBackend() }

    init {
        viewModelScope.launch {
            store.profiles.collect { _profiles.value = it }
        }
        loadApps()
        refreshUsageState()
        refreshBackend()
        AppShell.addRefreshListener(backendListener)
    }

    override fun onCleared() {
        AppShell.removeRefreshListener(backendListener)
        super.onCleared()
    }

    fun refresh() {
        viewModelScope.launch {
            loadAppsNow()
            refreshBackend()
            refreshUsageState()
        }
    }

    fun refreshBackend() {
        viewModelScope.launch {
            val prefs = settingsStore.settings.first()
            AppShell.setPreferMode(prefs.backendMode.name)
            AppShell.refresh()
            val active = AppShell.active
            val effectiveActive = when (prefs.backendMode) {
                BackendMode.SHIZUKU -> if (active?.name == "Shevery") active else null
                BackendMode.ROOT -> if (active?.name == "Root (su)") active else null
                BackendMode.AUTO -> active
            }
            _backendReady.value = prefs.backendEnabled && effectiveActive != null
        }
    }

    fun refreshOnResume() {
        refreshBackend()
        refreshUsageState()
    }

    fun loadApps() {
        viewModelScope.launch { loadAppsNow() }
    }

    private suspend fun loadAppsNow() {
        val list = withContext(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, 0)
                .mapNotNull { ri ->
                    val pkg = ri.activityInfo.packageName
                    val label = runCatching { ri.loadLabel(pm).toString() }.getOrNull()
                        ?: pkg
                    val icon = runCatching {
                        val drawable = ri.loadIcon(pm)
                        if (drawable is BitmapDrawable) drawable.bitmap.asImageBitmap()
                        else drawable.toBitmap(64, 64).asImageBitmap()
                    }.getOrNull()
                    InstalledApp(
                        label = label,
                        packageName = pkg,
                        icon = icon,
                        isSystem = isSystem(pkg),
                    )
                }
                .sortedBy { it.label.lowercase() }
        }
        _apps.value = list
    }

    fun setQuery(q: String) {
        _searchQuery.value = q
    }

    fun saveProfile(pkg: String, density: Int?, width: Int?, height: Int?, rotation: Int? = null) {
        viewModelScope.launch {
            store.set(AppProfile(pkg, density, width, height, rotation))
            refreshUsageState()
            if (_usageAccess.value && !_serviceRunning.value) startService()
            val foreground = withContext(Dispatchers.IO) { isForeground(pkg) }
            if (foreground) {
                val wm = AppShell.controller()
                if (lastGoodDensity == null) lastGoodDensity = wm?.density()
                if (lastGoodSize == null) lastGoodSize = wm?.size()
                if (applyProfileNow(pkg)) startFailsafe(pkg)
            }
        }
    }

    private suspend fun applyProfileNow(pkg: String): Boolean {
        val profile = store.get(pkg) ?: return false
        if (profile.isEmpty()) return false
        val wm = AppShell.controller() ?: return false
        var ok = true
        var changed = false
        profile.density?.let { d ->
            if (wm.density() != d) {
                if (wm.setDensity(d).isOk) changed = true else ok = false
            }
        }
        if (profile.width != null && profile.height != null) {
            val target = Size(profile.width, profile.height)
            if (wm.size() != target) {
                if (wm.setSize(target).isOk) changed = true else ok = false
            }
        }
        profile.rotation?.let { target ->
            if (wm.lockedRotation() != target) {
                if (wm.setRotation(target).isOk) changed = true else ok = false
            }
        }
        if (!ok) {
            _message.value = UiMessage(R.string.msg_apply_failed)
            return false
        }
        return changed
    }

    fun confirmPending() {
        _failsafe.value = null
        lastGoodDensity = null
        lastGoodSize = null
        _message.value = UiMessage(R.string.msg_applied)
    }

    fun cancelPending() {
        _failsafe.value = null
        viewModelScope.launch { restoreLastGood() }
        _message.value = UiMessage(R.string.msg_reverted)
    }

    private fun startFailsafe(pkg: String) {
        val label = _apps.value.find { it.packageName == pkg }?.label ?: pkg
        _failsafe.value = Failsafe(label)
    }

    private suspend fun restoreLastGood() {
        val wm = AppShell.controller() ?: return
        lastGoodDensity?.let { wm.setDensity(it) }
        lastGoodSize?.let { wm.setSize(it) }
        lastGoodDensity = null
        lastGoodSize = null
    }

    private fun isForeground(pkg: String): Boolean {
        return runCatching {
            val usm = getApplication<Application>()
                .getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(now - 300_000, now)
            val e = UsageEvents.Event()
            var top: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                ) {
                    top = e.packageName
                }
            }
            top == pkg
        }.getOrDefault(false)
    }

    fun removeProfile(pkg: String) {
        viewModelScope.launch { store.remove(pkg) }
    }

    fun clearAllProfiles() {
        viewModelScope.launch {
            store.clearAll()
            _message.value = UiMessage(R.string.msg_profiles_cleared)
        }
    }

    fun openApp(pkg: String) {
        if (!AppActions.open(getApplication(), pkg)) {
            _message.value = UiMessage(R.string.msg_open_failed, pkg)
        }
    }

    fun forceStop(pkg: String) {
        viewModelScope.launch {
            if (AppActions.forceStop(pkg)) _message.value = UiMessage(R.string.msg_force_stopped, pkg)
            else _message.value = UiMessage(R.string.msg_force_stop_need_backend)
        }
    }

    fun restartApp(pkg: String) {
        viewModelScope.launch {
            val stopped = AppActions.forceStop(pkg)
            AppActions.open(getApplication(), pkg)
            _message.value = if (stopped) UiMessage(R.string.msg_restarted, pkg)
            else UiMessage(R.string.msg_restart_need_backend)
        }
    }

    fun refreshUsageState() {
        _usageAccess.value = ProfileService.hasUsageAccess(getApplication())
        _serviceRunning.value = isServiceRunning(getApplication())
    }

    fun startService() {
        val ctx = getApplication<Application>()
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            ctx.startForegroundService(Intent(ctx, ProfileService::class.java))
        } else {
            ctx.startService(Intent(ctx, ProfileService::class.java))
        }
        _serviceRunning.value = true
        viewModelScope.launch { settingsStore.update { it.copy(monitoringEnabled = true) } }
    }

    fun stopService() {
        getApplication<Application>().stopService(
            Intent(getApplication(), ProfileService::class.java),
        )
        _serviceRunning.value = false
        viewModelScope.launch { settingsStore.update { it.copy(monitoringEnabled = false) } }
    }

    fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { getApplication<Application>().startActivity(intent) }
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun isServiceRunning(ctx: Context): Boolean {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        return am.getRunningServices(100).any { it.service.className == ProfileService::class.java.name }
    }

    private fun isSystem(pkg: String): Boolean {
        return runCatching {
            (pm.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_SYSTEM) != 0
        }.getOrDefault(false)
    }
}

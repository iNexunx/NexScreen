package nx.screen.ds.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import nx.screen.ds.R
import nx.screen.ds.core.AppShell
import nx.screen.ds.core.Size
import nx.screen.ds.core.WmController
import nx.screen.ds.data.AppSettings
import nx.screen.ds.data.BackendMode
import nx.screen.ds.data.SettingsStore
import nx.screen.ds.data.WallpaperStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class Failsafe(val label: String, val labelRes: Int? = null)

enum class ShizukuStatus { NOT_RUNNING, NEEDS_PERMISSION, READY }

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)

    private val _density = MutableStateFlow<Int?>(null)
    val density: StateFlow<Int?> = _density
    private val _physicalDensity = MutableStateFlow<Int?>(null)
    val physicalDensity: StateFlow<Int?> = _physicalDensity
    private val _size = MutableStateFlow<Size?>(null)
    val size: StateFlow<Size?> = _size
    private val _physicalSize = MutableStateFlow<Size?>(null)
    val physicalSize: StateFlow<Size?> = _physicalSize

    private val _backend = MutableStateFlow<String?>(null)
    val backend: StateFlow<String?> = _backend
    private val _backendReady = MutableStateFlow(false)
    val backendReady: StateFlow<Boolean> = _backendReady
    private val _shizukuStatus = MutableStateFlow(ShizukuStatus.NOT_RUNNING)
    val shizukuStatus: StateFlow<ShizukuStatus> = _shizukuStatus
    private val _rootAvailable = MutableStateFlow(false)
    val rootAvailable: StateFlow<Boolean> = _rootAvailable

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled
    private val _backendMode = MutableStateFlow(BackendMode.AUTO)
    val backendMode: StateFlow<BackendMode> = _backendMode
    private val _shizukuInstalled = MutableStateFlow(false)
    val shizukuInstalled: StateFlow<Boolean> = _shizukuInstalled
    private val _message = MutableStateFlow<Int?>(null)
    val message: StateFlow<Int?> = _message
    private val _failsafe = MutableStateFlow<Failsafe?>(null)
    val failsafe: StateFlow<Failsafe?> = _failsafe

    private var lastGoodDensity: Int? = null
    private var lastGoodSize: Size? = null
    private var lastGoodSettings: AppSettings? = null
    private var pendingResetAll = false

    private val backendListener: () -> Unit = { refresh() }

    private var initialized = false

    init {
        AppShell.addRefreshListener(backendListener)
        _shizukuInstalled.value = isShizukuInstalled()
        refresh()
    }

    override fun onCleared() {
        AppShell.removeRefreshListener(backendListener)
        super.onCleared()
    }

    /** Primer refresco al arrancar la app: se ejecuta una sola vez y devuelve el estado real. */
    suspend fun refreshInitial() {
        if (initialized) return
        initialized = true
        doRefresh(force = false)
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            doRefresh(force)
        }
    }

    private suspend fun doRefresh(force: Boolean) {
        _busy.value = true
        try {
            val prefs = store.settings.first()
            _enabled.value = prefs.backendEnabled
            _backendMode.value = prefs.backendMode
            _shizukuInstalled.value = isShizukuInstalled()
            AppShell.setPreferMode(prefs.backendMode.name)
            if (!prefs.backendEnabled) {
                _backend.value = null
                _backendReady.value = false
                _shizukuStatus.value = ShizukuStatus.NOT_RUNNING
                _rootAvailable.value = false
                _density.value = null
                _physicalDensity.value = null
                _size.value = null
                _physicalSize.value = null
                return
            }
            AppShell.refresh(force = force)
            val active = AppShell.active
            val effectiveActive = when (prefs.backendMode) {
                BackendMode.SHIZUKU -> if (active?.name == "Shevery") active else null
                BackendMode.ROOT -> if (active?.name == "Root (su)") active else null
                BackendMode.AUTO -> active
            }
            _backend.value = effectiveActive?.name
            _backendReady.value = effectiveActive != null
            _shizukuStatus.value = when {
                !AppShell.shizuku.running -> ShizukuStatus.NOT_RUNNING
                AppShell.shizuku.granted -> ShizukuStatus.READY
                else -> ShizukuStatus.NEEDS_PERMISSION
            }
            _rootAvailable.value = AppShell.rootAvailableCached()

            val wm = AppShell.controller()
            if (wm != null) {
                val info = wm.info()
                _density.value = info.density
                _physicalDensity.value = info.physicalDensity
                _size.value = info.size
                _physicalSize.value = info.physicalSize
                if (lastGoodDensity == null) lastGoodDensity = _density.value
                if (lastGoodSize == null) lastGoodSize = _size.value
            }
        } finally {
            _busy.value = false
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            store.update { it.copy(backendEnabled = enabled) }
            refresh()
        }
    }

    fun setBackendMode(mode: BackendMode) {
        viewModelScope.launch {
            store.update { it.copy(backendMode = mode) }
            refresh()
        }
    }

    fun activateMode(mode: BackendMode) {
        viewModelScope.launch {
            store.update {
                it.copy(
                    backendMode = mode,
                    backendEnabled = true,
                )
            }
            refresh(force = true)
        }
    }

    fun detectNow() {
        viewModelScope.launch {
            refresh(force = true)
        }
    }

    fun requestShizukuPermission() {
        runCatching { AppShell.shizuku.requestPermission(PERMISSION_REQUEST_CODE) }
    }

    private fun isShizukuInstalled(): Boolean = runCatching {
        val pm = getApplication<Application>().packageManager
        pm.getPackageInfo("com.hamondev.shevery", 0)
        true
    }.getOrElse {
        runCatching {
            getApplication<Application>().packageManager
                .getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        }.getOrDefault(false)
    }

    fun applyDensity(d: Int) {
        viewModelScope.launch {
            val wm = activeController()
            if (wm == null) {
                _message.value = R.string.backend_unavailable
                return@launch
            }
            val target = d.coerceIn(safeDensityRange(_physicalDensity.value, _density.value))
            if (lastGoodDensity == null) lastGoodDensity = _density.value
            _busy.value = true
            val res = wm.setDensity(target)
            _busy.value = false
            if (!res.isOk) {
                _message.value = R.string.msg_apply_failed
                return@launch
            }
            _density.value = target
            startFailsafe(R.string.label_density)
        }
    }

    fun applySize(w: Int, h: Int) {
        viewModelScope.launch {
            val wm = activeController()
            if (wm == null) {
                _message.value = R.string.backend_unavailable
                return@launch
            }
            val safe = safeSizeRange(_physicalSize.value)
            val target = Size(w.coerceIn(safe.first), h.coerceIn(safe.second))
            if (lastGoodSize == null) lastGoodSize = _size.value
            _busy.value = true
            val res = wm.setSize(target)
            _busy.value = false
            if (!res.isOk) {
                _message.value = R.string.msg_apply_failed
                return@launch
            }
            _size.value = target
            startFailsafe(R.string.label_resolution)
        }
    }

    fun applyDensityAndSize(d: Int, w: Int?, h: Int?) {
        viewModelScope.launch {
            val wm = activeController()
            if (wm == null) {
                _message.value = R.string.backend_unavailable
                return@launch
            }
            val applyDensity = d > 0
            val applySize = w != null && h != null
            if (!applyDensity && !applySize) return@launch
            if (lastGoodDensity == null && applyDensity) lastGoodDensity = _density.value
            if (lastGoodSize == null && applySize) lastGoodSize = _size.value
            _busy.value = true
            var ok = true
            var newDensity = _density.value
            var newSize = _size.value
            if (applyDensity) {
                val target = d.coerceIn(safeDensityRange(_physicalDensity.value, _density.value))
                ok = wm.setDensity(target).isOk && ok
                newDensity = target
            }
            if (applySize) {
                val safeRange = safeSizeRange(_physicalSize.value)
                val safe = Size(w!!.coerceIn(safeRange.first), h!!.coerceIn(safeRange.second))
                ok = wm.setSize(safe).isOk && ok
                newSize = safe
            }
            _busy.value = false
            if (!ok) {
                lastGoodDensity?.let { wm.setDensity(it) }
                lastGoodSize?.let { wm.setSize(it) }
                _density.value = wm.density()
                _size.value = wm.size()
                lastGoodDensity = null
                lastGoodSize = null
                _message.value = R.string.msg_apply_failed
                return@launch
            }
            if (applyDensity) _density.value = newDensity
            if (applySize) _size.value = newSize
            startFailsafe(R.string.label_density_resolution)
        }
    }

    fun confirmPending() {
        _failsafe.value = null
        lastGoodDensity = _density.value
        lastGoodSize = _size.value
        lastGoodSettings = null
        if (pendingResetAll) {
            pendingResetAll = false
            viewModelScope.launch { WallpaperStore.clear(getApplication()) }
        }
        _message.value = R.string.msg_applied
    }

    fun cancelPending() {
        _failsafe.value = null
        viewModelScope.launch { restoreLastGood() }
        _message.value = R.string.msg_reverted
    }

    fun resetSystem() {
        viewModelScope.launch {
            val wm = activeController()
            if (wm == null) {
                _message.value = R.string.backend_unavailable
                return@launch
            }
            _failsafe.value = null
            _busy.value = true
            if (lastGoodDensity == null) lastGoodDensity = _density.value
            if (lastGoodSize == null) lastGoodSize = _size.value
            if (lastGoodSettings == null) lastGoodSettings = store.settings.first()
            pendingResetAll = true
            wm.resetAll()
            store.update {
                it.copy(
                    backendMode = BackendMode.AUTO,
                    backendEnabled = true,
                )
            }
            _backendMode.value = BackendMode.AUTO
            _enabled.value = true
            _density.value = wm.density()
            _size.value = wm.size()
            _physicalDensity.value = wm.physicalDensity()
            _physicalSize.value = wm.physicalSize()
            _busy.value = false
            startFailsafe(R.string.label_reset_system)
            refresh()
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun startFailsafe(labelRes: Int) {
        val label = getApplication<Application>().getString(labelRes)
        _failsafe.value = Failsafe(label, labelRes)
    }

    private suspend fun activeController(): WmController? {
        val cached = AppShell.controller()
        if (cached != null) return cached
        AppShell.refresh()
        return AppShell.controller()
    }

    private suspend fun restoreLastGood() {
        pendingResetAll = false
        lastGoodSettings?.let { saved ->
            store.update { saved }
            if (saved.customWallpaper) {
                SettingsViewModel.sharedWallpaper.value = WallpaperStore.load(getApplication())
            } else {
                SettingsViewModel.sharedWallpaper.value = null
            }
        }
        lastGoodSettings = null
        val wm = AppShell.controller() ?: return
        lastGoodDensity?.let { wm.setDensity(it) }
        lastGoodSize?.let { wm.setSize(it) }
        _density.value = wm.density()
        _size.value = wm.size()
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
        const val MIN_DENSITY = 160
        const val MAX_DENSITY = 640
        const val MIN_SIZE = 240
        const val MAX_SIZE = 7680

        /** Rango de densidad seguro por dispositivo, basado en la densidad física del panel. */
        private fun safeDensityRange(physicalDensity: Int?, currentDensity: Int?): IntRange {
            val physical = physicalDensity ?: currentDensity ?: 420
            val min = (physical * 0.5).toInt().coerceIn(160, 320)
            val max = (physical * 1.6).toInt().coerceIn(320, MAX_DENSITY)
            return min..max
        }

        /** Rango de resolución seguro: ni más pequeña que la mitad ni más grande que 2x la física. */
        private fun safeSizeRange(physical: Size?): Pair<IntRange, IntRange> {
            val refW = physical?.w ?: 1080
            val refH = physical?.h ?: 2400
            return (refW / 2).coerceAtLeast(MIN_SIZE)..(refW * 2).coerceAtMost(MAX_SIZE) to
                (refH / 2).coerceAtLeast(MIN_SIZE)..(refH * 2).coerceAtMost(MAX_SIZE)
        }
    }
}

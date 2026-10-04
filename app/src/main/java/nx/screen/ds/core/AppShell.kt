package nx.screen.ds.core

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nx.screen.ds.data.BackendMode

object AppShell {
    private lateinit var appContext: Context
    private var activeBackend: Shell? = null
    private val refreshListeners = mutableListOf<() -> Unit>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val detectMutex = Mutex()

    @Volatile
    private var preferMode: String = BackendMode.AUTO.name

    @Volatile
    private var lastRefreshMs = 0L

    @Volatile
    private var lastRootCheckMs = 0L

    @Volatile
    private var lastRootOk = false

    private const val DETECT_CACHE_MS = 2000L
    private const val ROOT_CACHE_MS = 30000L

    val shizuku = ShizukuShell()
    val root = RootShell()

    fun init(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
        }
        shizuku.init(appContext) { onBinderChanged() }
        scope.launch { refresh(force = true) }
    }

    fun setPreferMode(mode: String) {
        if (preferMode != mode) lastRefreshMs = 0
        preferMode = mode
    }

    private fun onBinderChanged() {
        scope.launch {
            refresh(force = true)
            notifyBackendChanged()
        }
    }

    fun addRefreshListener(listener: () -> Unit) {
        synchronized(refreshListeners) {
            if (!refreshListeners.contains(listener)) refreshListeners.add(listener)
        }
    }

    fun removeRefreshListener(listener: () -> Unit) {
        synchronized(refreshListeners) {
            refreshListeners.remove(listener)
        }
    }

    fun notifyBackendChanged() {
        val copy = synchronized(refreshListeners) { refreshListeners.toList() }
        copy.forEach { it.invoke() }
    }

    fun backends(): List<Shell> = listOf(root, shizuku)

    val active: Shell?
        get() = activeBackend

    /**
     * Contexto de la aplicacion, disponible tras [init].
     *
     * Lo consultan partes que no son el servicio (por ejemplo la UI al aplicar
     * la rotacion) y necesitan leer las mismas SharedPreferences que este.
     */
    val appContextOrNull: Context?
        get() = if (::appContext.isInitialized) appContext else null

    /** Each activation method is a separate, reusable detection function. */
    suspend fun detectRoot(): Boolean = root.available()

    suspend fun detectShizuku(): Boolean = shizuku.available()

    /**
     * AUTO = Shizuku primero, root solo como respaldo.
     *
     * Shizuku corre sobre adb y no deja el dispositivo con root, así que es
     * el camino de menor privilegio. Antes se probaba root primero, y eso
     * significa que bastaba con tener root en el móvil —aunque fuera por
     * motivos ajenos a esta app— para que todos los cambios se aplicaran
     * por root sin haberlo elegido. Root queda disponible, pero hay que
     * pedirlo explícitamente con [BackendMode.ROOT].
     */
    fun preferredOrder(): List<Shell> = when (preferMode) {
        BackendMode.ROOT.name -> listOf(root)
        BackendMode.SHIZUKU.name -> listOf(shizuku)
        else -> listOf(shizuku, root)
    }

    /**
     * Re-evaluates which backend is usable. Results are cached for a short window
     * so repeated calls (e.g. on every screen resume) don't run expensive probes
     * over and over. Use [force] after activation or on explicit "Detectar de nuevo".
     */
    suspend fun refresh(force: Boolean = false) {
        var ran = false
        val detected = detectMutex.withLock {
            val now = System.currentTimeMillis()
            if (!force && now - lastRefreshMs < DETECT_CACHE_MS) {
                null
            } else {
                ran = true
                lastRefreshMs = System.currentTimeMillis()
                preferredOrder().firstOrNull { shell ->
                    when (shell) {
                        is RootShell -> rootAvailableCached()
                        else -> shell.available()
                    }
                }
            }
        }
        if (ran) {
            activeBackend = detected
            if (activeBackend != null) ensureBatteryWhitelist()
        }
    }

    fun controller(): WmController? = active?.let { WmController(it) }

    suspend fun rootAvailableCached(): Boolean {
        val now = System.currentTimeMillis()
        // Cache largo: evita disparar el diálogo de superuser de Magisk repetidamente.
        if (now - lastRootCheckMs > ROOT_CACHE_MS) {
            lastRootCheckMs = now
            lastRootOk = root.available()
        }
        return lastRootOk
    }

    private suspend fun ensureBatteryWhitelist() {
        val backend = activeBackend ?: return
        val pkg = appContext.packageName
        runCatching {
            backend.run("dumpsys deviceidle whitelist +$pkg")
        }
    }
}

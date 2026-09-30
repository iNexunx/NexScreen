package nx.screen.ds.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import nx.screen.ds.R
import nx.screen.ds.data.AppProfile
import nx.screen.ds.data.AppProfileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ProfileService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null
    private lateinit var store: AppProfileStore

    private var originalsApplied = false
    private var originalDensity: Int? = null
    private var originalSize: Size? = null
    private var originalRotation: Int? = null
    private var originalsRotationCaptured = false

    /**
     * Sondeos consecutivos que la app objetivo lleva sin estar en primer plano
     * antes de restaurar. Dos a POLL_MS = 1 s bastan para absorbing un fallo
     * puntual de la consulta de uso sin meter un parpadeo al restaurar.
     */
    private var awayPolls = 0
    private var lastPidofMs = 0L
    private var appliedApp: String? = null
    private var lastShellRefresh = 0L

    private val prefs by lazy { getSharedPreferences("profile_service", Context.MODE_PRIVATE) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        store = AppProfileStore(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()
        if (pollJob == null) {
            pollJob = scope.launch {
                restorePersistedIfNeeded()
                applyForForeground()
                monitor()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        pollJob?.cancel()
        scope.cancel()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { restoreOriginals() }
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // specialUse no tiene el timeout de 6 h que sí matan los dataSync
            // desde Android 15, y es el tipo correcto para un monitor perpetuo.
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_profiles),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private suspend fun monitor() {
        while (scope.isActive) {
            applyForForeground()
            delay(POLL_MS)
        }
    }

    /**
     * Aplica el perfil de la app en primer plano y restaura los originales en
     * cuanto deja de estarlo.
     *
     * La clave es que la restauracion NO depende de que el proceso muera: se
     * dispara porque la app objetivo ya no es la de primer plano. Depender de
     * la muerte del proceso dejaba la densidad y la rotacion puestas
     * indefinidamente (pulsar Inicio no restaura nada, porque el proceso sigue
     * vivo en segundo plano).
     */
    private suspend fun applyForForeground() {
        if (AppShell.active == null && System.currentTimeMillis() - lastShellRefresh > 5000) {
            lastShellRefresh = System.currentTimeMillis()
            AppShell.refresh()
        }
        val shell = AppShell.active ?: return
        val wm = WmController(shell)
        val top = getForegroundPackage()

        val target = appliedApp
        if (target != null) {
            // top == null significa que no sabemos quien esta delante. Ante la
            // duda, tratamos la app objetivo como no-activa: es preferible
            // devolver el móvil a su estado normal (molesto, recuperable) que
            // dejarlo con la densidad o la pantalla girada (inservible).
            val notInForeground = top == null || top != target
            if (notInForeground || processGoneThrottled(target)) {
                awayPolls++
                if (awayPolls >= AWAY_POLLS_BEFORE_RESTORE) {
                    restoreOriginals(wm)
                    appliedApp = null
                    awayPolls = 0
                }
            } else {
                awayPolls = 0
                // Algo externo puede haber tocado la densidad (el usuario la
                // cambio en Ajustes, o el sistema la reseto). Reafirmamos.
                val profile = store.get(target)
                if (!profile.isEmpty() && originalsApplied) applyProfile(wm, profile)
            }
            return
        }

        if (top == null || top == packageName) return

        val profile = store.get(top)
        if (!profile.isEmpty()) {
            if (!originalsApplied) {
                originalDensity = wm.density()
                originalSize = wm.size()
                originalRotation = wm.lockedRotation()
                originalsRotationCaptured = true
                originalsApplied = true
                saveOriginals()
            }
            applyProfile(wm, profile)
            appliedApp = top
            awayPolls = 0
        }
    }

    /**
     * `pidof` lanza un proceso shell, asi que no puede ir en cada sondeo.
     * Es solo una via de escape para cuando la consulta de uso dice que la app
     * sigue delante pero en realidad esta muerta; el caso normal lo cubre
     * `top != target`.
     */
    private suspend fun processGoneThrottled(pkg: String): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastPidofMs < PIDOF_INTERVAL_MS) return false
        lastPidofMs = now
        return processGone(pkg)
    }

    /**
     * Comprobacion rapida para el caso de "forzar cierre" desde Ajustes, que no
     * genera ningun evento de primer plano. Si `pidof` no existe (code 127 en
     * algunas ROM) devolvemos false en vez de asumir: antes ese caso caia en un
     * heuristico basado en lastTop que podia no restaurar nunca.
     */
    private suspend fun processGone(pkg: String): Boolean {
        val shell = AppShell.active ?: return false
        val result = runCatching { shell.run("pidof $pkg") }.getOrNull() ?: return false
        return when (result.code) {
            0 -> result.stdout.isBlank()
            1 -> true
            else -> false
        }
    }

    private suspend fun applyProfile(wm: WmController, profile: AppProfile) {
        profile.density?.let { d ->
            val current = wm.density()
            if (current != d) wm.setDensity(d)
        }
        if (profile.width != null && profile.height != null) {
            val current = wm.size()
            if (current != Size(profile.width, profile.height)) {
                wm.setSize(Size(profile.width, profile.height))
            }
        }
        profile.rotation?.let { target ->
            if (wm.lockedRotation() != target) wm.setRotation(target)
        }
    }

    private suspend fun restoreOriginals(wm: WmController? = null) {
        if (AppShell.active == null) AppShell.refresh()
        val controller = wm ?: run {
            val shell = AppShell.active ?: return
            WmController(shell)
        }
        originalDensity?.let { controller.setDensity(it) }
        originalSize?.let { controller.setSize(it) }
        // Se restaura siempre que se capturo el original, incluso si era null
        // (giro libre): si no, la pantalla se queda bloqueada en la rotacion
        // que pedia el perfil.
        if (originalsRotationCaptured) controller.setRotation(originalRotation)
        originalsApplied = false
        originalsRotationCaptured = false
        clearPersistedOriginals()
    }

    private fun saveOriginals() {
        prefs.edit().apply {
            originalDensity?.let { putInt(KEY_DENSITY, it) }
            originalSize?.let {
                putInt(KEY_W, it.w)
                putInt(KEY_H, it.h)
            }
            // -1 = el usuario tenia el giro libre. Hay que persistirlo aunque
            // sea null, o al reiniciar el servicio no sabriamos si restaurarlo.
            if (originalsRotationCaptured) {
                putInt(KEY_ROTATION, originalRotation ?: ROTATION_FREE)
            }
        }.apply()
    }

    private fun clearPersistedOriginals() {
        prefs.edit().clear().apply()
    }

    private suspend fun restorePersistedIfNeeded() {
        if (!prefs.contains(KEY_DENSITY) && !prefs.contains(KEY_W) &&
            !prefs.contains(KEY_ROTATION)
        ) return
        if (AppShell.active == null) AppShell.refresh()
        val shell = AppShell.active ?: return
        val controller = WmController(shell)
        prefs.getInt(KEY_DENSITY, 0).takeIf { it > 0 }?.let { controller.setDensity(it) }
        if (prefs.contains(KEY_W) && prefs.contains(KEY_H)) {
            controller.setSize(Size(prefs.getInt(KEY_W, 0), prefs.getInt(KEY_H, 0)))
        }
        if (prefs.contains(KEY_ROTATION)) {
            val stored = prefs.getInt(KEY_ROTATION, ROTATION_FREE)
            controller.setRotation(if (stored == ROTATION_FREE) null else stored)
        }
        clearPersistedOriginals()
    }

    private fun getForegroundPackage(): String? {
        return runCatching {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(now - EVENT_WINDOW_MS, now)
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
            if (top != null) return@runCatching top
            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - EVENT_WINDOW_MS,
                now,
            )
            return@runCatching stats.maxByOrNull { it.lastTimeUsed }?.packageName
        }.getOrNull()
    }

    companion object {
        private const val CHANNEL_ID = "profile_service"
        private const val NOTIFICATION_ID = 1
        private const val POLL_MS = 1000L
        private const val AWAY_POLLS_BEFORE_RESTORE = 2
        private const val PIDOF_INTERVAL_MS = 3000L
        private const val EVENT_WINDOW_MS = 10_000L
        private const val KEY_DENSITY = "orig_density"
        private const val KEY_W = "orig_w"
        private const val KEY_H = "orig_h"
        private const val KEY_ROTATION = "orig_rotation"

        /** Centinela para "el usuario no tenia el giro bloqueado". */
        private const val ROTATION_FREE = -1

        fun hasUsageAccess(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= 29) {
                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName,
                )
            }
            if (mode == android.app.AppOpsManager.MODE_ALLOWED) return true
            return try {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                usm.queryEvents(System.currentTimeMillis() - 1000, System.currentTimeMillis())
                true
            } catch (_: SecurityException) {
                false
            }
        }
    }
}

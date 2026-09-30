package nx.screen.ds

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nx.screen.ds.core.AppShell
import nx.screen.ds.core.ProfileService
import nx.screen.ds.data.SettingsStore

class App : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AppShell.init(this)
        applyStoredLocale()
        restoreMonitoring()
    }

    private fun applyStoredLocale() {
        appScope.launch {
            val language = SettingsStore(this@App).settings.first().language
            applyAppLocale(language)
        }
    }

    private fun restoreMonitoring() {
        appScope.launch {
            val settings = SettingsStore(this@App).settings.first()
            if (settings.monitoringEnabled && ProfileService.hasUsageAccess(this@App)) {
                val intent = Intent(this@App, ProfileService::class.java)
                runCatching {
                    if (android.os.Build.VERSION.SDK_INT >= 26) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                }
            }
        }
    }
}

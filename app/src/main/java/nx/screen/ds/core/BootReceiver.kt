package nx.screen.ds.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nx.screen.ds.data.SettingsStore

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val settings = SettingsStore(appContext).settings.first()
            if (settings.monitoringEnabled && ProfileService.hasUsageAccess(appContext)) {
                val service = Intent(appContext, ProfileService::class.java)
                runCatching {
                    if (android.os.Build.VERSION.SDK_INT >= 26) {
                        appContext.startForegroundService(service)
                    } else {
                        appContext.startService(service)
                    }
                }
            }
        }
    }
}

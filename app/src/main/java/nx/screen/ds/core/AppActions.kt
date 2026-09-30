package nx.screen.ds.core

import android.content.Context
import android.content.Intent

object AppActions {

    fun open(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    suspend fun forceStop(packageName: String): Boolean {
        val shell = AppShell.active ?: return false
        return shell.run("am force-stop $packageName").isOk
    }
}

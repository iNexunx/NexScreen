package nx.screen.ds.core

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

class ShizukuShell : Shell {
    override val name: String = "Shevery"

    private var context: Context? = null
    private var binderListener: (() -> Unit)? = null

    fun init(context: Context, onBinderChanged: () -> Unit) {
        if (this.context != null) return
        this.context = context.applicationContext
        binderListener = onBinderChanged
        Shizuku.addBinderReceivedListener(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        onBinderChanged()
    }

    private val binderReceived = Shizuku.OnBinderReceivedListener {
        binderListener?.invoke()
    }

    private val binderDead = Shizuku.OnBinderDeadListener {
        binderListener?.invoke()
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        binderListener?.invoke()
    }

    val running: Boolean
        get() = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    val granted: Boolean
        get() = running && runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

    val served: Boolean
        get() = running && runCatching { Shizuku.getVersion() > 0 }.getOrDefault(false)

    override suspend fun available(): Boolean = granted

    fun requestPermission(requestCode: Int) {
        Shizuku.requestPermission(requestCode)
    }

    override suspend fun run(command: String): CommandResult {
        if (!granted) return CommandResult(-1, "", "Shizuku sin permiso")
        return try {
            val p = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            readProcessStreams(p.inputStream, p.errorStream) {
                val deadline = System.currentTimeMillis() + TIMEOUT_MS
                while (System.currentTimeMillis() < deadline) {
                    try {
                        return@readProcessStreams p.exitValue()
                    } catch (_: IllegalThreadStateException) {
                        Thread.sleep(50)
                    }
                }
                p.destroy()
                waitForBoundedShizuku(p)
                -1
            }
        } catch (e: Exception) {
            CommandResult(-1, "", e.message ?: "error")
        }
    }

    companion object {
        private const val TIMEOUT_MS = 5000L

        private fun waitForBoundedShizuku(p: Process) {
            val waiter = Thread { p.waitFor() }
            waiter.isDaemon = true
            waiter.start()
            try {
                waiter.join(1500)
            } catch (_: InterruptedException) {
            }
            if (waiter.isAlive) runCatching { p.destroy() }
        }
    }
}

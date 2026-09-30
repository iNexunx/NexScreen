package nx.screen.ds.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CommandResult(
    val code: Int,
    val stdout: String,
    val stderr: String,
) {
    val output: String get() = if (stderr.isBlank()) stdout else "$stdout\n$stderr"
    val isOk: Boolean get() = code == 0
}

/**
 * En API < 26 `waitFor()` sin límite puede bloquear para siempre si el proceso
 * ignora SIGTERM. Espera acotada para no fugar hilos del pool de IO.
 */
private fun waitForBounded(p: Process, ms: Long) {
    val waiter = Thread { p.waitFor() }
    waiter.isDaemon = true
    waiter.start()
    try {
        waiter.join(ms)
    } catch (_: InterruptedException) {
    }
    if (waiter.isAlive) runCatching { p.destroy() }
}

suspend fun runProcess(vararg cmd: String, timeoutMs: Long = 5000): CommandResult =
    withContext(Dispatchers.IO) {
        try {
            val p = ProcessBuilder(*cmd).redirectErrorStream(false).start()
            val out = StringBuilder()
            val err = StringBuilder()
            val t1 = Thread { p.inputStream.bufferedReader().use { out.append(it.readText()) } }
            val t2 = Thread { p.errorStream.bufferedReader().use { err.append(it.readText()) } }
            t1.isDaemon = true
            t2.isDaemon = true
            t1.start()
            t2.start()
            val deadline = System.currentTimeMillis() + timeoutMs
            var code: Int? = null
            while (System.currentTimeMillis() < deadline) {
                code = try {
                    p.exitValue()
                } catch (_: IllegalThreadStateException) {
                    Thread.sleep(50)
                    null
                }
                if (code != null) break
            }
            val exit = if (code == null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    p.destroyForcibly()
                    p.waitFor()
                } else {
                    p.destroy()
                    waitForBounded(p, 1500)
                }
                -1
            } else {
                code
            }
            t1.join(1000)
            t2.join(1000)
            CommandResult(exit, out.toString().trim(), err.toString().trim())
        } catch (e: Exception) {
            CommandResult(-1, "", e.message ?: "error")
        }
    }

suspend fun readProcessStreams(
    input: java.io.InputStream,
    error: java.io.InputStream,
    wait: () -> Int,
): CommandResult = withContext(Dispatchers.IO) {
    val out = StringBuilder()
    val err = StringBuilder()
    val t1 = Thread { input.bufferedReader().use { out.append(it.readText()) } }
    val t2 = Thread { error.bufferedReader().use { err.append(it.readText()) } }
    t1.isDaemon = true
    t2.isDaemon = true
    t1.start()
    t2.start()
    val code = try {
        wait()
    } catch (e: InterruptedException) {
        -1
    }
    t1.join(1000)
    t2.join(1000)
    CommandResult(code, out.toString().trim(), err.toString().trim())
}

interface Shell {
    val name: String
    suspend fun available(): Boolean
    suspend fun run(command: String): CommandResult
}

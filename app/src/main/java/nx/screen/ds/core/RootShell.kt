package nx.screen.ds.core

class RootShell : Shell {
    override val name: String = "Root (su)"

    override suspend fun available(): Boolean {
        val result = runProcess("su", "-c", "id", timeoutMs = 1500)
        return result.code == 0 && result.stdout.contains("uid=0")
    }

    override suspend fun run(command: String): CommandResult =
        runProcess("su", "-c", command)
}

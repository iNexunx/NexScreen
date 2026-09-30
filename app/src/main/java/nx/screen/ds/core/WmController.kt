package nx.screen.ds.core

data class Size(val w: Int, val h: Int) {
    override fun toString(): String = "${w}x$h"
}

data class DisplayInfo(
    val density: Int?,
    val physicalDensity: Int?,
    val size: Size?,
    val physicalSize: Size?,
)

class WmController(private val shell: Shell) {

    suspend fun info(): DisplayInfo {
        val out = shell.run("wm density; wm size").stdout
        return DisplayInfo(
            density = parseLine(out, "Override density:")
                ?: parseLine(out, "Physical density:")
                ?: runCatching {
                    shell.run("getprop ro.sf.lcd_density").stdout.trim().toIntOrNull()
                }.getOrNull(),
            physicalDensity = parseLine(out, "Physical density:"),
            size = parseSize(out, "Override size:") ?: parseSize(out, "Physical size:"),
            physicalSize = parseSize(out, "Physical size:"),
        )
    }

    suspend fun density(): Int? {
        val out = shell.run("wm density").stdout
        return parseLine(out, "Override density:")
            ?: parseLine(out, "Physical density:")
            ?: shell.run("getprop ro.sf.lcd_density").stdout.trim().toIntOrNull()
    }

    suspend fun physicalDensity(): Int? {
        val out = shell.run("wm density").stdout
        return parseLine(out, "Physical density:")
    }

    suspend fun size(): Size? {
        val out = shell.run("wm size").stdout
        return parseSize(out, "Override size:")
            ?: parseSize(out, "Physical size:")
    }

    suspend fun physicalSize(): Size? {
        val out = shell.run("wm size").stdout
        return parseSize(out, "Physical size:")
    }

    suspend fun setDensity(density: Int): CommandResult =
        shell.run("wm density $density")

    suspend fun setSize(size: Size): CommandResult =
        shell.run("wm size ${size.w}x${size.h}")

    suspend fun resetDensity(): CommandResult = shell.run("wm density reset")

    suspend fun resetSize(): CommandResult = shell.run("wm size reset")

    suspend fun resetAll(): CommandResult = shell.run("wm size reset; wm density reset")

    private fun parseLine(out: String, key: String): Int? {
        out.lineSequence().forEach { line ->
            val idx = line.indexOf(key)
            if (idx >= 0) {
                val v = line.substring(idx + key.length).trim()
                val m = Regex("\\d+").find(v)
                if (m != null) return m.value.toInt()
            }
        }
        return null
    }

    private fun parseSize(out: String, key: String): Size? {
        out.lineSequence().forEach { line ->
            val idx = line.indexOf(key)
            if (idx >= 0) {
                val v = line.substring(idx + key.length).trim()
                val m = Regex("(\\d+)x(\\d+)").find(v)
                if (m != null) return Size(m.groupValues[1].toInt(), m.groupValues[2].toInt())
            }
        }
        return null
    }
}

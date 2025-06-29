package com.example.adbagent.domain.usecase

object RootShell {
    fun exec(cmd: String): Boolean = try {
        val p = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
        p.waitFor() == 0
    } catch (_: Exception) {
        false
    }
}
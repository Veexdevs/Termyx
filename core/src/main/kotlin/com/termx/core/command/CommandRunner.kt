package com.termx.core.command

import com.termx.core.util.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Real, cross-platform command runner backed by [ProcessBuilder].
 *
 * It executes a command through the host/device shell, captures stdout/stderr,
 * enforces a timeout and reports the exit code. This is genuine process execution,
 * not a simulation.
 */
object CommandRunner {

    /**
     * Run [command] and capture its output.
     *
     * @param shell shell prefix, e.g. `["sh","-c"]` or `["cmd","/c"]`. Defaults to the platform shell.
     * @param workingDir working directory (null = inherited).
     * @param env extra environment variables merged into the process environment.
     * @param timeoutMs hard timeout; on expiry the process is destroyed and [CommandResult.timedOut] is true.
     */
    suspend fun run(
        command: String,
        shell: List<String> = Platform.defaultShell(),
        workingDir: String? = null,
        env: Map<String, String> = emptyMap(),
        timeoutMs: Long = 120_000
    ): CommandResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val fullCommand = if (shell.size >= 2) {
            shell + command
        } else {
            listOf(shell.firstOrNull() ?: "sh", "-c", command)
        }

        val process = try {
            ProcessBuilder(fullCommand).apply {
                redirectErrorStream(false)
                workingDir?.let { directory(File(it)) }
                environment().putAll(env)
            }.start()
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - start
            return@withContext CommandResult(
                command = command,
                exitCode = -1,
                stdout = "",
                stderr = "Failed to start process: ${e.message}",
                durationMs = elapsed
            )
        }

        val out = StringBuilder()
        val err = StringBuilder()

        val readOut = Thread { process.inputStream.bufferedReader().use { r -> r.forEachLine { out.appendLine(it) } } }
        val readErr = Thread { process.errorStream.bufferedReader().use { r -> r.forEachLine { err.appendLine(it) } } }
        readOut.start()
        readErr.start()

        val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        val exitCode: Int
        val timedOut: Boolean
        if (!finished) {
            process.destroyForcibly()
            exitCode = -1
            timedOut = true
        } else {
            exitCode = process.exitValue()
            timedOut = false
        }
        readOut.join(2000)
        readErr.join(2000)

        CommandResult(
            command = command,
            exitCode = exitCode,
            stdout = out.toString().trimEnd(),
            stderr = err.toString().trimEnd(),
            timedOut = timedOut,
            durationMs = System.currentTimeMillis() - start
        )
    }
}

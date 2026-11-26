package com.termx.core.terminal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.io.File
import java.util.UUID

/**
 * Real terminal session backed by a spawned shell process.
 *
 * Output is streamed as it arrives (no buffering until EOF), input is forwarded to
 * the process stdin, and the exit code is surfaced through [exited]. This is the
 * engine used by both the Android and Desktop terminals; on Android the platform
 * supplies `/system/bin/sh` (or a PTY-wrapped shell) as [shellCommand].
 */
class LocalShellSession(
    private val shellCommand: List<String>,
    private val workingDir: String? = null,
    private val env: Map<String, String> = emptyMap(),
    override val id: String = UUID.randomUUID().toString()
) : TerminalSession {

    private val _output = MutableSharedFlow<String>(extraBufferCapacity = 2048)
    private val _exited = MutableSharedFlow<Int?>(extraBufferCapacity = 1)
    override val output: Flow<String> = _output.asSharedFlow()
    override val exited: Flow<Int?> = _exited.asSharedFlow()

    private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var process: Process? = null
    private var writer: java.io.BufferedWriter? = null
    private var job: Job? = null

    @Synchronized
    override fun start() {
        if (process != null) return
        // Fresh scope so the session can be restarted after stop().
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val pb = ProcessBuilder(shellCommand).apply {
            redirectErrorStream(true) // merge stderr into the terminal stream
            workingDir?.let { directory(File(it)) }
            environment().putAll(env)
        }
        val p = try {
            pb.start()
        } catch (e: Exception) {
            scope.launch { _output.emit("\r\n[termx] failed to start shell: ${e.message}\r\n") }
            scope.launch { _exited.emit(-1) }
            return
        }
        process = p
        writer = p.outputStream.bufferedWriter()

        job = scope.launch {
            val reader = p.inputStream.bufferedReader()
            val buf = CharArray(1024)
            var n: Int
            while (reader.read(buf).also { n = it } != -1) {
                _output.emit(String(buf, 0, n))
            }
        }
        scope.launch {
            val code = p.waitFor()
            _exited.emit(code)
        }
    }

    override fun write(data: String) {
        val w = writer ?: return
        scope.launch {
            try {
                w.write(data)
                w.flush()
            } catch (_: Exception) {
                // session may have ended
            }
        }
    }

    override fun resize(columns: Int, rows: Int) {
        // No-op: resizing a non-PTY process is a no-operation. Real PTY backends override this.
    }

    @Synchronized
    override fun stop() {
        try { process?.destroyForcibly() } catch (_: Exception) {}
        try { writer?.close() } catch (_: Exception) {}
        job?.cancel()
        scope.cancel()
        process = null
    }
}

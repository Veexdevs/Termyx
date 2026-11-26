package com.termx.terminal

import android.util.Log
import com.termx.core.terminal.TerminalEmulator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real terminal session for Android: a shell running inside a PTY, whose output is fed
 * into a [TerminalEmulator] and whose input is written to the PTY master fd.
 *
 * The [emulator] is the single source of truth the UI renders. [version] bumps on every
 * batch of received output so a Compose `Canvas` can re-draw efficiently.
 */
class PtyTerminalSession(
    private val workingDir: String,
    private val shell: List<String> = listOf("/system/bin/sh"),
    cols: Int = 80,
    rows: Int = 24,
    maxScrollback: Int = 5000
) {
    private val _emulator = TerminalEmulator(cols, rows, maxScrollback)
    val emulator: TerminalEmulator get() = _emulator

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    private val _running = MutableStateFlow(true)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _exitCode = MutableStateFlow<Int?>(null)
    val exitCode: StateFlow<Int?> = _exitCode.asStateFlow()

    private var pty: PtyProcess? = null
    private var reader: Thread? = null
    private val alive = AtomicBoolean(false)

    init {
        start(cols, rows)
    }

    private fun buildEnv(): Map<String, String> = mapOf(
        "TERM" to "xterm-256color",
        "HOME" to workingDir,
        "PATH" to "/system/bin:/system/xbin:/sbin:$workingDir",
        "LANG" to "C.UTF-8",
        "ANDROID_ROOT" to "/system",
        "ANDROID_DATA" to "/data",
        "PWD" to workingDir
    )

    fun start(initialCols: Int, initialRows: Int) {
        val cmd = shell.first()
        val args = shell.drop(1)
        val process = try {
            Pty.create(
                command = cmd,
                args = args,
                cwd = workingDir,
                env = buildEnv(),
                rows = initialRows,
                cols = initialCols
            )
        } catch (e: Throwable) {
            Log.e("TermyxSession", "PTY spawn failed", e)
            null
        }
        if (process == null) {
            _emulator.input("\r\n[termx] failed to start ${shell.joinToString(" ")} (PTY unavailable)\r\n")
            _running.value = false
            return
        }
        pty = process
        alive.set(true)
        reader = Thread({
            try {
                while (alive.get()) {
                    val bytes = process.read() ?: break
                    _emulator.input(bytes)
                    _version.value += 1
                }
                val code = process.waitFor()
                _exitCode.value = code
            } catch (e: Throwable) {
                Log.w("TermyxSession", "reader ended", e)
            } finally {
                _running.value = false
            }
        }, "termx-pty-reader").also { it.isDaemon = true; it.start() }
    }

    /** Forward raw keystrokes (including escape sequences) to the shell. */
    fun send(text: String) {
        val p = pty ?: return
        if (text.isEmpty()) return
        try {
            p.write(text.toByteArray(Charsets.UTF_8))
        } catch (_: Throwable) {
        }
    }

    fun sendBytes(bytes: ByteArray) {
        val p = pty ?: return
        try {
            p.write(bytes)
        } catch (_: Throwable) {
        }
    }

    fun resize(cols: Int, rows: Int) {
        _emulator.resize(cols, rows)
        try {
            pty?.resize(rows, cols)
        } catch (_: Throwable) {
        }
    }

    fun stop() {
        alive.set(false)
        try {
            pty?.close()
        } catch (_: Throwable) {
        }
        reader = null
    }
}

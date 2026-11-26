package com.termx.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.termx.TermyxApplication
import com.termx.core.terminal.TerminalEmulator
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Drives a real PTY-backed terminal session (default Android shell). UI reads the live
 * [emulator] model and re-draws whenever [version] changes; keystrokes are forwarded to
 * the shell via [send].
 */
class TerminalViewModel(app: Application) : AndroidViewModel(app) {
    private val container = getApplication<TermyxApplication>().container

    private val session = PtyTerminalSession(
        workingDir = container.fileRoot,
        shell = container.terminalShell
    )

    val emulator: TerminalEmulator get() = session.emulator
    val version: StateFlow<Int> get() = session.version
    val running: StateFlow<Boolean> get() = session.running
    val exitCode: StateFlow<Int?> get() = session.exitCode

    fun send(text: String) = session.send(text)
    fun sendControl(code: String) = session.send(code)

    fun resize(cols: Int, rows: Int) = session.resize(cols, rows)

    fun clear() {
        viewModelScope.launch {
            session.emulator.clearScreen()
        }
    }

    fun restart() {
        session.stop()
        session.start(emulator.colCount, emulator.rowCount)
    }

    override fun onCleared() {
        session.stop()
    }
}

package com.termx.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.termx.TermyxApplication
import com.termx.core.terminal.LocalShellSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives a real local shell session (currently /system/bin/sh on Android).
 * Output is streamed from the session and buffered for display.
 */
class TerminalViewModel(app: Application) : AndroidViewModel(app) {
    private val container = getApplication<TermyxApplication>().container
    private val session = LocalShellSession(container.terminalShell, workingDir = container.fileRoot)

    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _running = MutableStateFlow(true)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _exitCode = MutableStateFlow<Int?>(null)
    val exitCode: StateFlow<Int?> = _exitCode.asStateFlow()

    init { attach() }

    private fun attach() {
        viewModelScope.launch {
            session.output.collect { chunk ->
                _output.value = (_output.value + chunk).takeLast(200_000)
            }
        }
        viewModelScope.launch {
            session.exited.collect { code ->
                _exitCode.value = code
                _running.value = false
            }
        }
        session.start()
    }

    fun send(text: String) = session.write(text)
    fun sendControl(code: String) = session.write(code)
    fun clear() { _output.value = "" }

    fun restart() {
        // Collectors are attached once in init; the same session object re-emits to the
        // same flow after stop()/start(), so we must not re-attach (would double-append).
        session.stop()
        _exitCode.value = null
        _running.value = true
        _output.value = ""
        session.start()
    }

    override fun onCleared() {
        session.stop()
    }
}

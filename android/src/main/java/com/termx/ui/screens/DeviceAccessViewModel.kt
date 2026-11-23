package com.termx.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.termx.core.command.CommandRunner
import com.termx.core.model.DeviceAccessStatus
import com.termx.device.DeviceAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceAccessViewModel(app: Application) : AndroidViewModel(app) {
    private val _status = MutableStateFlow<DeviceAccessStatus?>(null)
    val status: StateFlow<DeviceAccessStatus?> = _status.asStateFlow()

    private val _output = MutableStateFlow<String?>(null)
    val output: StateFlow<String?> = _output.asStateFlow()

    fun check() {
        viewModelScope.launch { _status.value = DeviceAccess.status(getApplication()) }
    }

    fun runAuthorized(command: String) {
        viewModelScope.launch {
            val r = CommandRunner.run("su -c '${command.replace("'", "'\\''")}'")
            _output.value = if (r.success) r.stdout else "Failed (no root?):\n${r.stderr}"
        }
    }

    fun clearOutput() { _output.value = null }
}

package com.termx.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.termx.TermyxApplication
import com.termx.core.model.DeviceAccessStatus
import com.termx.core.model.SystemMetrics
import com.termx.device.DeviceAccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val container = getApplication<TermyxApplication>().container

    private val _metrics = MutableStateFlow<SystemMetrics?>(null)
    val metrics: StateFlow<SystemMetrics?> = _metrics.asStateFlow()

    private val _device = MutableStateFlow<DeviceAccessStatus?>(null)
    val device: StateFlow<DeviceAccessStatus?> = _device.asStateFlow()

    init {
        viewModelScope.launch {
            _device.value = DeviceAccess.status(getApplication())
            while (true) {
                _metrics.value = container.metrics.snapshot()
                delay(2000)
            }
        }
    }

    fun refreshDevice() {
        viewModelScope.launch { _device.value = DeviceAccess.status(getApplication()) }
    }

    val installedSystems get() = container.linuxManager.list().filter { it.installed }
    val servers get() = container.serverManager.list()
}

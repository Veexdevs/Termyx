package com.termx.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.termx.TermyxApplication
import com.termx.core.linux.LinuxSystemManager
import com.termx.core.model.LinuxDistribution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SystemsViewModel(app: Application) : AndroidViewModel(app) {
    private val manager: LinuxSystemManager = getApplication<TermyxApplication>().container.linuxManager

    private val _systems = MutableStateFlow(manager.list())
    val systems: StateFlow<List<com.termx.core.model.LinuxSystem>> = _systems.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun refresh() { _systems.value = manager.list() }

    fun install(distro: LinuxDistribution) {
        viewModelScope.launch {
            _busy.value = true
            _status.value = "Installing ${distro.displayName} (PRoot)…"
            val res = manager.install(distro)
            _status.value = res.fold(
                onSuccess = { "Installed ${it.name} at ${it.installPath}" },
                onFailure = { it.message ?: "Install failed" }
            )
            refresh()
            _busy.value = false
        }
    }

    fun isInstalled(distro: LinuxDistribution): Boolean =
        systems.value.any { it.distribution == distro && it.installed }
}

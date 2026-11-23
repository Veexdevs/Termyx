package com.termx.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.termx.TermyxApplication
import com.termx.ai.ProviderRepository
import com.termx.core.agent.Agent
import com.termx.core.agent.AgentEvent
import com.termx.core.agent.ToolContext
import com.termx.core.ai.AiClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Runs the autonomous agent against the real tool set. Progress is surfaced as
 * [AgentEvent]s; destructive tools raise a UI confirmation via [confirmation].
 */
class AgentViewModel(app: Application) : AndroidViewModel(app) {
    private val container = getApplication<TermyxApplication>().container
    private val repo = ProviderRepository(container.keyStore)

    private val _events = MutableSharedFlow<AgentEvent>(extraBufferCapacity = 128)
    val events = _events.asSharedFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _log = MutableStateFlow("")
    val log: StateFlow<String> = _log.asStateFlow()

    private val _confirmation = MutableStateFlow<String?>(null)
    val confirmation: StateFlow<String?> = _confirmation.asStateFlow()
    private var confirmDeferred: CompletableDeferred<Boolean>? = null

    fun run(request: String) {
        if (_running.value) return
        viewModelScope.launch {
            _running.value = true
            _log.value = ""
            val providers = repo.load()
            val def = providers.firstOrNull { it.isDefault } ?: providers.firstOrNull()
            if (def == null || def.apiKey.isBlank()) {
                _events.emit(AgentEvent.Error("Add an AI provider with a valid API key in AI Providers first."))
                _running.value = false
                return@launch
            }

            val ctx = ToolContext(
                workingDirectory = container.fileRoot,
                fileRoot = container.fileRoot,
                providers = providers,
                defaultProvider = def,
                aiClient = AiClient,
                serverManager = container.serverManager,
                linuxManager = container.linuxManager,
                onLog = { line -> _log.value = (_log.value + line + "\n").takeLast(20000) },
                confirm = { message ->
                    confirmDeferred = CompletableDeferred()
                    _confirmation.value = message
                    confirmDeferred!!.await()
                }
            )

            Agent(ctx).run(request) { event -> _events.emit(event) }
            _running.value = false
        }
    }

    fun confirm(allowed: Boolean) {
        confirmDeferred?.complete(allowed)
        confirmDeferred = null
        _confirmation.value = null
    }
}

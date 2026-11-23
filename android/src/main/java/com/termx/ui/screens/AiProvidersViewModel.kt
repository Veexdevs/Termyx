package com.termx.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.termx.TermyxApplication
import com.termx.ai.ProviderRepository
import com.termx.core.ai.AiClient
import com.termx.core.model.AiProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiProvidersViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ProviderRepository(getApplication<TermyxApplication>().container.keyStore)

    private val _providers = MutableStateFlow<List<AiProvider>>(emptyList())
    val providers: StateFlow<List<AiProvider>> = _providers.asStateFlow()

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

    init { viewModelScope.launch { _providers.value = repo.load() } }

    fun upsert(provider: AiProvider) {
        viewModelScope.launch {
            val list = _providers.value.toMutableList()
            val idx = list.indexOfFirst { it.id == provider.id }
            if (idx >= 0) list[idx] = provider else list.add(provider)
            repo.save(list)
            _providers.value = list
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            val list = _providers.value.filter { it.id != id }
            repo.save(list)
            _providers.value = list
        }
    }

    fun setDefault(id: String) {
        viewModelScope.launch {
            val list = _providers.value.map { it.copy(isDefault = it.id == id) }
            repo.save(list)
            repo.setDefault(id)
            _providers.value = list
        }
    }

    fun test(provider: AiProvider) {
        viewModelScope.launch {
            _testResult.value = runCatching { AiClient.testConnection(provider) }
                .fold(onSuccess = { "Connected ✓" }, onFailure = { "Failed: ${it.message}" })
        }
    }

    fun clearTest() { _testResult.value = null }
}

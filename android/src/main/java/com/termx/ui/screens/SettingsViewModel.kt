package com.termx.ui.screens

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Application.dataStore by preferencesDataStore("termx_settings")

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val ds = getApplication<Application>().dataStore
    private val DARK = booleanPreferencesKey("dark_theme")
    private val CONFIRM = booleanPreferencesKey("confirm_destructive")
    private val AUTO_START_SERVERS = booleanPreferencesKey("auto_start_servers")

    val darkTheme: StateFlow<Boolean> = ds.data.map { it[DARK] ?: false }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)
    val confirmDestructive: StateFlow<Boolean> = ds.data.map { it[CONFIRM] ?: true }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), true)
    val autoStartServers: StateFlow<Boolean> = ds.data.map { it[AUTO_START_SERVERS] ?: false }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)

    fun setDark(v: Boolean) = write(DARK, v)
    fun setConfirm(v: Boolean) = write(CONFIRM, v)
    fun setAutoStartServers(v: Boolean) = write(AUTO_START_SERVERS, v)

    private fun write(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { ds.edit { it[key] = value } }
    }
}

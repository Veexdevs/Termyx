package com.termx.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.termx.TermyxApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class DownloadsViewModel(app: Application) : AndroidViewModel(app) {
    private val dir = getApplication<TermyxApplication>().container.downloadsDir

    private val _files = MutableStateFlow<List<FileEntry>>(emptyList())
    val files: StateFlow<List<FileEntry>> = _files.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = File(dir).listFiles()?.map { FileEntry(it.name, it.isDirectory, it.length()) }
                ?.sortedBy { it.name.lowercase() } ?: emptyList()
            _files.value = list
        }
    }

    fun delete(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            File(dir, name).deleteRecursively()
            reload()
        }
    }
}

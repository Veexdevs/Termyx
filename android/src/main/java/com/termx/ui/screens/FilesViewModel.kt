package com.termx.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.termx.TermyxApplication
import com.termx.core.command.CommandRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class FileEntry(val name: String, val isDirectory: Boolean, val size: Long)

class FilesViewModel(app: Application) : AndroidViewModel(app) {
    private val root = getApplication<TermyxApplication>().container.fileRoot

    private val _path = MutableStateFlow(root)
    val path: StateFlow<String> = _path.asStateFlow()

    private val _entries = MutableStateFlow<List<FileEntry>>(emptyList())
    val entries: StateFlow<List<FileEntry>> = _entries.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(_path.value)
            val list = dir.listFiles()?.map { FileEntry(it.name, it.isDirectory, it.length()) }
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?: emptyList()
            _entries.value = list
        }
    }

    fun open(name: String) {
        val next = File(_path.value, name)
        if (next.isDirectory) { _path.value = next.absolutePath; reload() }
    }

    fun up() {
        val parent = File(_path.value).parent ?: root
        if (parent.startsWith(root)) { _path.value = parent; reload() }
    }

    fun createFile(name: String, content: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            File(_path.value, name).writeText(content)
            reload()
        }
    }

    fun delete(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            File(_path.value, name).deleteRecursively()
            reload()
        }
    }

    fun read(name: String): String = runCatching { File(_path.value, name).readText() }.getOrDefault("")
    fun write(name: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) { File(_path.value, name).writeText(content) }
    }

    private val _runOutput = MutableStateFlow<String?>(null)
    val runOutput: StateFlow<String?> = _runOutput.asStateFlow()

    /** Execute a source file with a suitable interpreter (python3/node/bash/kotlin). */
    fun runFile(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(_path.value, name)
            val ext = name.substringAfterLast('.', "")
            val interpreter = when (ext) {
                "py", "pyw" -> "python3"
                "js", "mjs", "cjs" -> "node"
                "sh", "bash" -> "bash"
                "kt" -> "kotlin"
                else -> null
            }
            val cmd = if (interpreter != null) "$interpreter \"${file.absolutePath}\"" else "\"${file.absolutePath}\""
            val r = CommandRunner.run(cmd, workingDir = _path.value)
            _runOutput.value = "${r.stdout}${if (r.stderr.isNotBlank()) "\n" + r.stderr else ""}\n[exit ${r.exitCode}]"
        }
    }

    fun clearRun() { _runOutput.value = null }
}

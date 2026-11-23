package com.termx.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.termx.TermyxApplication
import com.termx.core.model.ServerType

class ServersViewModel(app: Application) : AndroidViewModel(app) {
    private val manager = getApplication<TermyxApplication>().container.serverManager

    fun list() = manager.list()
    fun create(name: String, type: ServerType, command: String, dir: String, port: Int, autoRestart: Boolean, autoStart: Boolean) =
        manager.create(name, type, command, dir, port, autoRestart, autoStart)

    fun start(id: String) = manager.start(id)
    fun stop(id: String) = manager.stop(id)
    fun restart(id: String) = manager.restart(id)
    fun logs(id: String) = manager.logs(id, 120)
    fun state(id: String) = manager.state(id)
}

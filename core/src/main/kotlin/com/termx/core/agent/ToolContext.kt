package com.termx.core.agent

import com.termx.core.ai.AiClient
import com.termx.core.linux.LinuxSystemManager
import com.termx.core.model.AiProvider
import com.termx.core.server.ServerManager

/**
 * Execution environment handed to every [Tool]. Centralises the real engines the
 * agent may use: command runner, file root, AI client, server and system managers,
 * plus the host callbacks for logging and confirmation of dangerous operations.
 */
data class ToolContext(
    val workingDirectory: String,
    val fileRoot: String,
    val providers: List<AiProvider>,
    val defaultProvider: AiProvider?,
    val aiClient: AiClient,
    val serverManager: ServerManager,
    val linuxManager: LinuxSystemManager,
    val onLog: (String) -> Unit,
    val confirm: suspend (String) -> Boolean
)

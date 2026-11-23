package com.termx.core.agent

import com.termx.core.agent.tools.*

/**
 * Registry of agent tools. Adding a new capability is a matter of implementing
 * [Tool] and registering it here; the agent discovers it automatically through
 * the tool descriptions included in its system prompt.
 */
object ToolRegistry {
    val tools: List<Tool> = listOf(
        TerminalExecuteTool(),
        FilesCreateTool(),
        FilesReadTool(),
        FilesEditTool(),
        FilesDeleteTool(),
        PackagesInstallTool(),
        LinuxInstallTool(),
        LinuxStartTool(),
        LinuxStopTool(),
        ServerCreateTool(),
        ServerStartTool(),
        ServerStopTool(),
        ServerRestartTool(),
        ServerLogsTool(),
        GitCloneTool(),
        GitPullTool(),
        EditorCreateTool(),
        EditorModifyTool(),
        DeviceInspectTool(),
        DeviceExecuteAuthorizedTool()
    )

    fun byName(name: String): Tool? = tools.firstOrNull { it.name == name }

    /** Human-readable catalog injected into the planner system prompt. */
    fun catalog(): String = tools.joinToString("\n") { "- ${it.name}: ${it.description}" + if (it.dangerous) " (REQUIRES CONFIRMATION)" else "" }
}

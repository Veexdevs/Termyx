package com.termx.core.agent.tools

import com.termx.core.agent.Tool
import com.termx.core.agent.ToolContext
import com.termx.core.agent.ToolResult
import com.termx.core.model.ServerType

/** Create a managed server configuration. */
class ServerCreateTool : Tool {
    override val name = "server.create"
    override val description = "Register a managed server. Args: name, type, command, directory, port (optional)."

    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult = runCatching {
        val name = args.str("name")
        if (name.isBlank()) return ToolResult.fail("Missing 'name'.")
        val type = runCatching { ServerType.valueOf(args.str("type", "CUSTOM").uppercase()) }.getOrDefault(ServerType.CUSTOM)
        val cfg = ctx.serverManager.create(
            name = name,
            type = type,
            startCommand = args.str("command", "echo hello"),
            workingDirectory = args.str("directory", ctx.workingDirectory),
            port = args.str("port", "0").toIntOrNull() ?: 0
        )
        ToolResult.ok("Created server '${cfg.name}' (id=${cfg.id})")
    }.getOrElse { ToolResult.fail(it.message ?: it.toString()) }
}

private fun ToolContext.findServerId(args: Map<String, String>): String? {
    val id = args.str("id")
    if (id.isNotBlank()) return id
    val name = args.str("name")
    return serverManager.list().firstOrNull { it.name.equals(name, true) }?.id
}

/** Start a managed server. */
class ServerStartTool : Tool {
    override val name = "server.start"
    override val description = "Start a managed server. Args: id (or name)."
    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult {
        val id = ctx.findServerId(args) ?: return ToolResult.fail("Missing 'id' or 'name'.")
        val ok = ctx.serverManager.start(id)
        return ToolResult(ok, if (ok) "Server $id started" else "Failed to start $id")
    }
}

/** Stop a managed server. */
class ServerStopTool : Tool {
    override val name = "server.stop"
    override val description = "Stop a managed server. Args: id (or name)."
    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult {
        val id = ctx.findServerId(args) ?: return ToolResult.fail("Missing 'id' or 'name'.")
        ctx.serverManager.stop(id)
        return ToolResult.ok("Server $id stopped")
    }
}

/** Restart a managed server. */
class ServerRestartTool : Tool {
    override val name = "server.restart"
    override val description = "Restart a managed server. Args: id (or name)."
    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult {
        val id = ctx.findServerId(args) ?: return ToolResult.fail("Missing 'id' or name'.")
        val ok = ctx.serverManager.restart(id)
        return ToolResult(ok, if (ok) "Server $id restarted" else "Failed to restart $id")
    }
}

/** Read recent logs of a managed server. */
class ServerLogsTool : Tool {
    override val name = "server.logs"
    override val description = "Return recent logs of a managed server. Args: id (or name)."
    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult {
        val id = ctx.findServerId(args) ?: return ToolResult.fail("Missing 'id' or 'name'.")
        return ToolResult.ok(ctx.serverManager.logs(id, 200))
    }
}

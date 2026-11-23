package com.termx.core.agent.tools

import com.termx.core.agent.Tool
import com.termx.core.agent.ToolContext
import com.termx.core.agent.ToolResult
import com.termx.core.command.CommandRunner

/** Inspect device information. */
class DeviceInspectTool : Tool {
    override val name = "device.inspect"
    override val description = "Collect device information (OS, memory, storage). Args: none."

    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult {
        val sb = StringBuilder()
        sb.appendLine("== uname ==")
        sb.appendLine(CommandRunner.run("uname -a").stdout)
        sb.appendLine("== model ==")
        val model = CommandRunner.run("getprop ro.product.model 2>/dev/null || echo n/a").stdout
        sb.appendLine(model)
        sb.appendLine("== memory ==")
        sb.appendLine(CommandRunner.run("free -h 2>/dev/null || cat /proc/meminfo | head -3").stdout)
        sb.appendLine("== storage ==")
        sb.appendLine(CommandRunner.run("df -h 2>/dev/null | head -10").stdout)
        return ToolResult.ok(sb.toString().take(6000))
    }
}

/** Execute an authorized (root) command. Requires confirmation and root availability. */
class DeviceExecuteAuthorizedTool : Tool {
    override val name = "device.execute_authorized"
    override val description = "Run a command with root via 'su'. Args: command. Requires confirmation and root."
    override val dangerous = true

    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult {
        val command = args.str("command")
        if (command.isBlank()) return ToolResult.fail("Missing 'command'.")
        val res = CommandRunner.run("su -c '${command.replace("'", "'\\''")}'")
        return if (res.exitCode == 0 || res.stdout.isNotBlank()) {
            ToolResult.ok(res.stdout + if (res.stderr.isNotBlank()) "\n${res.stderr}" else "")
        } else {
            ToolResult.fail("Authorized execution failed (no root?): ${res.stderr.take(1500)}")
        }
    }
}

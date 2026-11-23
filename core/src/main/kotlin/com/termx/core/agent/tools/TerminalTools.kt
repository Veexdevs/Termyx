package com.termx.core.agent.tools

import com.termx.core.agent.Tool
import com.termx.core.agent.ToolContext
import com.termx.core.agent.ToolResult
import com.termx.core.command.CommandRunner

/** Execute a shell command in the agent's working directory. The only path to a shell. */
class TerminalExecuteTool : Tool {
    override val name = "terminal.execute"
    override val description = "Run a shell command and return its output. Args: command."

    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult {
        val command = args.str("command")
        if (command.isBlank()) return ToolResult.fail("Missing 'command' argument.")
        ctx.onLog("$ $command")
        val res = CommandRunner.run(command, workingDir = ctx.workingDirectory)
        val out = buildString {
            if (res.stdout.isNotBlank()) append(res.stdout)
            if (res.stderr.isNotBlank()) append("\n[stderr]\n").append(res.stderr)
            append("\n[exit ${res.exitCode}${if (res.timedOut) " timed-out" else ""}]")
        }
        return ToolResult(res.success, out)
    }
}

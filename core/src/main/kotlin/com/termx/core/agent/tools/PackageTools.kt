package com.termx.core.agent.tools

import com.termx.core.agent.Tool
import com.termx.core.agent.ToolContext
import com.termx.core.agent.ToolResult
import com.termx.core.command.CommandRunner

/** Install system packages using the platform package manager. */
class PackagesInstallTool : Tool {
    override val name = "packages.install"
    override val description = "Install packages. Args: packages (space separated), manager (optional, default 'pkg')."

    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult {
        val packages = args.str("packages").trim()
        if (packages.isBlank()) return ToolResult.fail("Missing 'packages'.")
        val manager = args.str("manager", "pkg")
        val res = CommandRunner.run("$manager install -y $packages", workingDir = ctx.workingDirectory)
        return ToolResult(
            res.success,
            if (res.success) "Installed: $packages" else "Install failed:\n${res.stderr.take(2000)}"
        )
    }
}

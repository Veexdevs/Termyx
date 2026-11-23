package com.termx.core.agent.tools

import com.termx.core.agent.Tool
import com.termx.core.agent.ToolContext
import com.termx.core.agent.ToolResult
import com.termx.core.model.LinuxDistribution

/** Install a Linux distribution via PRoot. */
class LinuxInstallTool : Tool {
    override val name = "linux.install"
    override val description = "Install a Linux distribution. Args: distro (Ubuntu|Debian|Arch|Alpine|Fedora|Kali|Void|openSUSE)."

    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult {
        val name = args.str("distro")
        val distro = LinuxDistribution.entries.firstOrNull {
            it.name.equals(name, true) || it.displayName.equals(name, true) || it.prootName.equals(name, true)
        } ?: return ToolResult.fail("Unknown distribution: $name")
        return ctx.linuxManager.install(distro).fold(
            onSuccess = { ToolResult.ok("Installed ${it.name} at ${it.installPath}") },
            onFailure = { ToolResult.fail(it.message ?: "install failed") }
        )
    }
}

/** Run a command inside an installed distribution. */
class LinuxStartTool : Tool {
    override val name = "linux.start"
    override val description = "Run a command in an installed distribution. Args: distro, command (optional)."

    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult {
        val name = args.str("distro")
        val distro = LinuxDistribution.entries.firstOrNull {
            it.name.equals(name, true) || it.displayName.equals(name, true) || it.prootName.equals(name, true)
        } ?: return ToolResult.fail("Unknown distribution: $name")
        val res = ctx.linuxManager.start(distro, args.str("command", "echo started"))
        return ToolResult(res.success, res.stdout + if (res.stderr.isNotBlank()) "\n${res.stderr}" else "")
    }
}

/** Stop a distribution's processes. */
class LinuxStopTool : Tool {
    override val name = "linux.stop"
    override val description = "Stop a distribution's processes. Args: distro."

    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult {
        val name = args.str("distro")
        val distro = LinuxDistribution.entries.firstOrNull {
            it.name.equals(name, true) || it.displayName.equals(name, true) || it.prootName.equals(name, true)
        } ?: return ToolResult.fail("Unknown distribution: $name")
        val ok = ctx.linuxManager.stop(distro)
        return ToolResult(ok, if (ok) "Stopped $name" else "Nothing to stop for $name")
    }
}

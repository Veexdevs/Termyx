package com.termx.core.agent.tools

import com.termx.core.agent.Tool
import com.termx.core.agent.ToolContext
import com.termx.core.agent.ToolResult
import com.termx.core.command.CommandRunner

/** Clone a git repository. */
class GitCloneTool : Tool {
    override val name = "git.clone"
    override val description = "Clone a git repository. Args: url, dir (optional)."

    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult {
        val url = args.str("url")
        if (url.isBlank()) return ToolResult.fail("Missing 'url'.")
        val dir = args.str("dir")
        val res = CommandRunner.run("git clone ${url.take(4000)} ${dir.take(200)}".trim(), workingDir = ctx.workingDirectory)
        return ToolResult(res.success, if (res.success) "Cloned $url" else res.stderr.take(2000))
    }
}

/** Pull updates in a git repository. */
class GitPullTool : Tool {
    override val name = "git.pull"
    override val description = "Pull updates in a git repository. Args: dir."

    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult {
        val dir = args.str("dir")
        if (dir.isBlank()) return ToolResult.fail("Missing 'dir'.")
        val res = CommandRunner.run("git -C ${dir.take(200)} pull", workingDir = ctx.workingDirectory)
        return ToolResult(res.success, if (res.success) "Pulled $dir" else res.stderr.take(2000))
    }
}

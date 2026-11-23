package com.termx.core.agent.tools

import com.termx.core.agent.Tool
import com.termx.core.agent.ToolContext
import com.termx.core.agent.ToolResult

/** Create a source file through the code editor (same backing store as files.create). */
class EditorCreateTool : Tool {
    override val name = "editor.create"
    override val description = "Create a source file in the editor. Args: path, content."

    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult = runCatching {
        val path = args.str("path")
        if (path.isBlank()) return ToolResult.fail("Missing 'path'.")
        val file = resolveWithin(ctx.fileRoot, path)
        file.parentFile?.mkdirs()
        file.writeText(args.str("content"))
        ToolResult.ok("Editor file created: ${file.absolutePath}")
    }.getOrElse { ToolResult.fail(it.message ?: it.toString()) }
}

/** Modify a source file through the code editor (same backing store as files.edit). */
class EditorModifyTool : Tool {
    override val name = "editor.modify"
    override val description = "Modify a source file in the editor. Args: path, oldText, newText."

    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult = runCatching {
        val path = args.str("path")
        val oldText = args.str("oldText")
        val newText = args.str("newText")
        if (path.isBlank() || oldText.isBlank()) return ToolResult.fail("Missing 'path' or 'oldText'.")
        val file = resolveWithin(ctx.fileRoot, path)
        if (!file.exists()) return ToolResult.fail("File not found: $path")
        val text = file.readText()
        if (!text.contains(oldText)) return ToolResult.fail("oldText not found in $path")
        file.writeText(text.replaceFirst(oldText, newText))
        ToolResult.ok("Modified $path")
    }.getOrElse { ToolResult.fail(it.message ?: it.toString()) }
}

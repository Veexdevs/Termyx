package com.termx.core.agent.tools

import com.termx.core.agent.Tool
import com.termx.core.agent.ToolContext
import com.termx.core.agent.ToolResult

/** Create a file with given content. */
class FilesCreateTool : Tool {
    override val name = "files.create"
    override val description = "Create or overwrite a file. Args: path, content."

    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult = runCatching {
        val path = args.str("path")
        if (path.isBlank()) return ToolResult.fail("Missing 'path'.")
        val file = resolveWithin(ctx.fileRoot, path)
        file.parentFile?.mkdirs()
        file.writeText(args.str("content"))
        ToolResult.ok("Created ${file.absolutePath} (${file.length()} bytes)")
    }.getOrElse { ToolResult.fail(it.message ?: it.toString()) }
}

/** Read a file's content. */
class FilesReadTool : Tool {
    override val name = "files.read"
    override val description = "Read a file's text content. Args: path."

    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult = runCatching {
        val path = args.str("path")
        if (path.isBlank()) return ToolResult.fail("Missing 'path'.")
        val file = resolveWithin(ctx.fileRoot, path)
        if (!file.exists()) return ToolResult.fail("File not found: $path")
        if (file.length() > 2_000_000) return ToolResult.fail("File too large to read (${file.length()} bytes).")
        ToolResult.ok(file.readText().take(20000))
    }.getOrElse { ToolResult.fail(it.message ?: it.toString()) }
}

/** Replace the first occurrence of oldText with newText in a file. */
class FilesEditTool : Tool {
    override val name = "files.edit"
    override val description = "Edit a file by replacing oldText with newText. Args: path, oldText, newText."

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
        ToolResult.ok("Edited $path")
    }.getOrElse { ToolResult.fail(it.message ?: it.toString()) }
}

/** Delete a file or directory (requires confirmation). */
class FilesDeleteTool : Tool {
    override val name = "files.delete"
    override val description = "Delete a file or directory (destructive). Args: path."
    override val dangerous = true

    override suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult = runCatching {
        val path = args.str("path")
        if (path.isBlank()) return ToolResult.fail("Missing 'path'.")
        val file = resolveWithin(ctx.fileRoot, path)
        if (!file.exists()) return ToolResult.fail("Not found: $path")
        val deleted = file.deleteRecursively()
        return if (deleted) ToolResult.ok("Deleted $path") else ToolResult.fail("Failed to delete $path")
    }.getOrElse { ToolResult.fail(it.message ?: it.toString()) }
}

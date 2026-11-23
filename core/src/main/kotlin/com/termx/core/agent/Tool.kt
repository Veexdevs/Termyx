package com.termx.core.agent

/**
 * Result returned by a [Tool]. Never fake a success — [success] reflects the real
 * outcome of the operation.
 */
data class ToolResult(
    val success: Boolean,
    val output: String,
    val data: Map<String, String> = emptyMap()
) {
    companion object {
        fun ok(output: String, data: Map<String, String> = emptyMap()) = ToolResult(true, output, data)
        fun fail(output: String) = ToolResult(false, output)
    }
}

/**
 * A capability the autonomous agent can invoke. Tools are the only way the agent
 * touches the environment — it cannot run arbitrary commands except through
 * [com.termx.core.agent.tools.TerminalExecuteTool].
 *
 * @property name unique id, e.g. `terminal.execute`.
 * @property description shown to the AI so it knows when to use the tool.
 * @property dangerous when true the host must confirm before execution.
 */
interface Tool {
    val name: String
    val description: String
    val dangerous: Boolean get() = false

    /** Execute the tool. Implementations must be side-effect real and report truthfully. */
    suspend fun execute(args: Map<String, String>, ctx: ToolContext): ToolResult
}

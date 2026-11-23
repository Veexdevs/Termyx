package com.termx.core.command

/**
 * Result of a one-shot command execution.
 *
 * @property command the exact command string that was executed
 * @property exitCode process exit code (0 == success)
 * @property stdout captured standard output
 * @property stderr captured standard error
 * @property timedOut true when the command exceeded [CommandRunner.run] timeout
 * @property durationMs wall-clock duration of execution
 */
data class CommandResult(
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean = false,
    val durationMs: Long = 0
) {
    val success: Boolean get() = !timedOut && exitCode == 0
}

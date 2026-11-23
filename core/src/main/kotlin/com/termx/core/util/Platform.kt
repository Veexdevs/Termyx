package com.termx.core.util

/**
 * Minimal, dependency-free platform detection used by the cross-platform core.
 * The Android module overrides the shell/working directory through its own
 * [com.termx.core.terminal.TerminalSession] configuration.
 */
enum class OS { LINUX, WINDOWS, MACOS, UNKNOWN }

object Platform {
    val rawName: String = System.getProperty("os.name") ?: "unknown"

    val os: OS
        get() = when {
            rawName.contains("windows", ignoreCase = true) -> OS.WINDOWS
            rawName.contains("mac", ignoreCase = true) -> OS.MACOS
            rawName.contains("linux", ignoreCase = true) -> OS.LINUX
            else -> OS.UNKNOWN
        }

    fun isWindows(): Boolean = os == OS.WINDOWS

    /** Default one-shot shell prefix used by [com.termx.core.command.CommandRunner]. */
    fun defaultShell(): List<String> = if (isWindows()) listOf("cmd", "/c") else listOf("sh", "-c")

    /** Used by the desktop terminal to launch an interactive host shell. */
    fun interactiveShell(): List<String> = when {
        isWindows() -> listOf("cmd")
        else -> listOf("bash", "-i")
    }
}

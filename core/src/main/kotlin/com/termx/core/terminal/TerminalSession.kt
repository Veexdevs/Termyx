package com.termx.core.terminal

import kotlinx.coroutines.flow.Flow

/**
 * A live terminal session that streams output and accepts input.
 *
 * The default implementation [LocalShellSession] spawns a real host/device shell
 * process. A full PTY (pseudo-terminal) with job control requires native code on
 * Android; [resize] is therefore a no-op in the portable implementation and is
 * overridden where a real PTY is available.
 */
interface TerminalSession {
    val id: String

    /** Emits output chunks (stdout + stderr merged, terminal-style). */
    val output: Flow<String>

    /** Emits the exit code once the underlying process terminates. */
    val exited: Flow<Int?>

    /** Begin the session. */
    fun start()

    /** Write raw input (keystrokes, including control sequences) to the shell. */
    fun write(data: String)

    /** Notify the PTY of a new size. No-op without native PTY support. */
    fun resize(columns: Int, rows: Int)

    /** Terminate the session. */
    fun stop()
}

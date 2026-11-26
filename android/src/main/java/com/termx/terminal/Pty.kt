package com.termx.terminal

import android.util.Log

/**
 * JNI bridge to the native PTY helper (see src/main/cpp/pty.c).
 *
 * Opens a real pseudo-terminal, forks and execs the requested command inside it, and
 * exposes read/write/resize/wait/close primitives over the master file descriptor.
 */
object Pty {
    private const val TAG = "TermyxPty"

    init {
        try {
            System.loadLibrary("termixpty")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native PTY library", e)
        }
    }

    /** [masterFd, pid] for the spawned subprocess, or null on failure. */
    external fun nativeCreateSubprocess(
        cmd: String,
        cwd: String,
        args: Array<String>,
        env: Array<String>,
        rows: Int,
        cols: Int,
        cellWidth: Int,
        cellHeight: Int
    ): IntArray?

    external fun nativeSetWindowSize(fd: Int, rows: Int, cols: Int, cellWidth: Int, cellHeight: Int)
    external fun nativeWaitFor(pid: Int): Int
    external fun nativeRead(fd: Int, buf: ByteArray, len: Int): Int
    external fun nativeWrite(fd: Int, buf: ByteArray, len: Int): Int
    external fun nativeClose(fd: Int)

    /**
     * Spawn [command]+[args] inside a PTY rooted at [cwd] with the given [env] and
     * initial [rows]x[cols] size. Returns a [PtyProcess] or null if it could not start.
     */
    fun create(
        command: String,
        args: List<String>,
        cwd: String,
        env: Map<String, String>,
        rows: Int,
        cols: Int,
        cellWidth: Int = 8,
        cellHeight: Int = 16
    ): PtyProcess? {
        val envArray = env.map { "${it.key}=${it.value}" }.toTypedArray()
        val argArray = (listOf(command) + args).toTypedArray()
        val result = nativeCreateSubprocess(
            command, cwd, argArray, envArray, rows, cols, cellWidth, cellHeight
        ) ?: return null
        return PtyProcess(result[0], result[1])
    }
}

/** Handle to a running PTY-backed process. */
class PtyProcess(val masterFd: Int, val pid: Int) {
    private val readBuf = ByteArray(16 * 1024)

    /** Read up to [max] bytes from the PTY master; returns bytes read or -1 on EOF/error. */
    fun read(max: Int = readBuf.size): ByteArray? {
        val len = Pty.nativeRead(masterFd, readBuf, max.coerceAtMost(readBuf.size))
        if (len <= 0) return null
        return readBuf.copyOf(len)
    }

    fun write(data: ByteArray): Int = Pty.nativeWrite(masterFd, data, data.size)

    fun resize(rows: Int, cols: Int, cellWidth: Int = 8, cellHeight: Int = 16) {
        Pty.nativeSetWindowSize(masterFd, rows, cols, cellWidth, cellHeight)
    }

    /** Wait for the process to exit; returns its exit code (negative = signal). */
    fun waitFor(): Int = Pty.nativeWaitFor(pid)

    fun close() = Pty.nativeClose(masterFd)
}

package com.termx.core.terminal

/**
 * Standard xterm 256-color palette (16 system colors + 6x6x6 cube + 24 grays).
 * Used to resolve SGR indexed colors to packed RGB ints for rendering.
 */
object TerminalPalette {
    private val BASE = intArrayOf(
        0x000000, 0x800000, 0x008000, 0x808000, 0x000080, 0x800080, 0x008080, 0xc0c0c0,
        0x808080, 0xff0000, 0x00ff00, 0xffff00, 0x0000ff, 0xff00ff, 0x00ffff, 0xffffff
    )
    private val cube = IntArray(216)
    private val gray = IntArray(24)

    init {
        var i = 0
        for (r in 0..5) for (g in 0..5) for (b in 0..5) {
            val rv = r * 255 / 5
            val gv = g * 255 / 5
            val bv = b * 255 / 5
            cube[i++] = (rv shl 16) or (gv shl 8) or bv
        }
        for (n in 0..23) {
            val v = n * 10 + 8
            gray[n] = (v shl 16) or (v shl 8) or v
        }
    }

    /** Resolve an 8-bit color index (0..255) to a packed RGB int. */
    fun color(index: Int): Int = when {
        index < 16 -> BASE[index]
        index < 232 -> cube[index - 16]
        else -> gray[index - 232]
    }
}

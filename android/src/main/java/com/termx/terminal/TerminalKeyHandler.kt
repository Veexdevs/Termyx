package com.termx.terminal

import android.view.KeyEvent

/**
 * Maps Android key events to the byte sequences a real terminal expects, mirroring the
 * behaviour of Termux's [com.termux.terminal.KeyHandler]: arrows, Home/End, function keys,
 * editing keys, Tab/Esc/Enter and Ctrl/Alt-modified sequences (e.g. Ctrl+C => 0x03,
 * Alt+Arrow => ESC + arrow, Ctrl+Arrow => CSI 1;5C).
 *
 * Printable text from the soft keyboard is handled separately via [printableSequence] so
 * that sticky on-screen Ctrl/Alt modifiers can be applied.
 */
object TerminalKeyHandler {
    private const val ESC = "\u001b"
    private const val CSI = "\u001b["

    /** Returns the sequence to send for a hardware key, or null if not handled here. */
    fun hardwareKeySequence(
        keyCode: Int,
        ctrl: Boolean,
        alt: Boolean,
        shift: Boolean,
        meta: Boolean,
        cursorApp: Boolean
    ): String? {
        val mod = (if (shift) 1 else 0) or
            (if (alt) 2 else 0) or
            (if (ctrl) 4 else 0) or
            (if (meta) 8 else 0)

        return when (keyCode) {
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> "\r"
            KeyEvent.KEYCODE_ESCAPE -> ESC
            KeyEvent.KEYCODE_TAB -> if (shift) "${CSI}Z" else "\t"
            KeyEvent.KEYCODE_DEL -> "\u007f" // backspace
            KeyEvent.KEYCODE_FORWARD_DEL -> csiTilde("3", mod)
            KeyEvent.KEYCODE_INSERT -> csiTilde("2", mod)
            KeyEvent.KEYCODE_MOVE_HOME, KeyEvent.KEYCODE_HOME -> csiLetter('H', mod, cursorApp)
            KeyEvent.KEYCODE_MOVE_END, KeyEvent.KEYCODE_END -> csiLetter('F', mod, cursorApp)
            KeyEvent.KEYCODE_DPAD_UP -> csiLetter('A', mod, cursorApp)
            KeyEvent.KEYCODE_DPAD_DOWN -> csiLetter('B', mod, cursorApp)
            KeyEvent.KEYCODE_DPAD_RIGHT -> csiLetter('C', mod, cursorApp)
            KeyEvent.KEYCODE_DPAD_LEFT -> csiLetter('D', mod, cursorApp)
            KeyEvent.KEYCODE_PAGE_UP -> csiTilde("5", mod)
            KeyEvent.KEYCODE_PAGE_DOWN -> csiTilde("6", mod)
            KeyEvent.KEYCODE_SPACE -> if (ctrl) "\u0000" else " "
            KeyEvent.KEYCODE_F1 -> csiLetter('P', mod, true)
            KeyEvent.KEYCODE_F2 -> csiLetter('Q', mod, true)
            KeyEvent.KEYCODE_F3 -> csiLetter('R', mod, true)
            KeyEvent.KEYCODE_F4 -> csiLetter('S', mod, true)
            KeyEvent.KEYCODE_F5 -> csiTilde("15", mod)
            KeyEvent.KEYCODE_F6 -> csiTilde("17", mod)
            KeyEvent.KEYCODE_F7 -> csiTilde("18", mod)
            KeyEvent.KEYCODE_F8 -> csiTilde("19", mod)
            KeyEvent.KEYCODE_F9 -> csiTilde("20", mod)
            KeyEvent.KEYCODE_F10 -> csiTilde("21", mod)
            KeyEvent.KEYCODE_F11 -> csiTilde("23", mod)
            KeyEvent.KEYCODE_F12 -> csiTilde("24", mod)
            else -> numpad(keyCode)?.let { if (ctrl) controlChar(it).toString() else it.toString() }
        }
    }

    /** Apply sticky on-screen Ctrl/Alt to typed text. */
    fun printableSequence(text: String, ctrl: Boolean, alt: Boolean): String {
        if (alt) return ESC + text
        if (ctrl) {
            val ch = text.firstOrNull() ?: return text
            return controlChar(ch).toString()
        }
        return text
    }

    /** Control-character for a Ctrl+<key> combination (e.g. Ctrl+C => 0x03). */
    fun controlChar(ch: Char): Char = when (ch) {
        ' ', '@' -> '\u0000'
        in 'a'..'z' -> (ch.code - 'a'.code + 1).toChar()
        in 'A'..'Z' -> (ch.code - 'A'.code + 1).toChar()
        '[' -> '\u001b'
        '\\' -> '\u001c'
        ']' -> '\u001d'
        '^' -> '\u001e'
        '_' -> '\u001f'
        else -> ch
    }

    private fun csiLetter(letter: Char, mod: Int, vt100: Boolean): String =
        if (mod == 0) (if (vt100) "$ESC O$letter" else "$CSI$letter")
        else "$CSI 1;${mod + 1}$letter"

    private fun csiTilde(num: String, mod: Int): String {
        val base = "$CSI${num}~"
        return if (mod == 0) base else base.substring(0, base.length - 1) + ";" + (mod + 1) + "~"
    }

    private fun numpad(keyCode: Int): Char? = when (keyCode) {
        KeyEvent.KEYCODE_NUMPAD_0 -> '0'
        KeyEvent.KEYCODE_NUMPAD_1 -> '1'
        KeyEvent.KEYCODE_NUMPAD_2 -> '2'
        KeyEvent.KEYCODE_NUMPAD_3 -> '3'
        KeyEvent.KEYCODE_NUMPAD_4 -> '4'
        KeyEvent.KEYCODE_NUMPAD_5 -> '5'
        KeyEvent.KEYCODE_NUMPAD_6 -> '6'
        KeyEvent.KEYCODE_NUMPAD_7 -> '7'
        KeyEvent.KEYCODE_NUMPAD_8 -> '8'
        KeyEvent.KEYCODE_NUMPAD_9 -> '9'
        KeyEvent.KEYCODE_NUMPAD_DOT -> '.'
        KeyEvent.KEYCODE_NUMPAD_DIVIDE -> '/'
        KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> '*'
        KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> '-'
        KeyEvent.KEYCODE_NUMPAD_ADD -> '+'
        KeyEvent.KEYCODE_NUMPAD_EQUALS -> '='
        else -> null
    }
}

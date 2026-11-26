package com.termx.core.terminal

import kotlin.math.min

/**
 * A real, self-contained VT100/xterm-compatible terminal emulator.
 *
 * It maintains a screen grid plus a scrollback ring buffer, parses the control
 * sequences a real shell/program emits (cursor movement, erase, scroll regions,
 * SGR colors/styles, save/restore cursor, alternate screen, DEC private modes) and
 * exposes the rendered rows and cursor for a UI to draw.
 *
 * This is the engine that makes the TERMYX terminal behave like Termux: programs
 * such as `vim`, `htop`, `less` or `ls --color` render correctly instead of as raw
 * text. It is pure Kotlin with no platform dependencies, so it is reused by both
 * the Android and Desktop terminals.
 */
class TerminalEmulator(
    var cols: Int,
    var rows: Int,
    private val maxScrollback: Int = 2000
) {
    private var screen = Array(rows) { Row(cols) }
    val scrollback = ArrayDeque<Row>(maxScrollback)

    var cursorX = 0
        private set
    var cursorY = 0
        private set
    var cursorVisible = true
        private set

    private var scrollTop = 0
    private var scrollBottom = rows - 1
    private var originMode = false
    private var autoWrap = true
    private var wrapPending = false
    private var altScreen = false
    private var altBuffer: Array<Row>? = null
    private var savedCursorX = 0
    private var savedCursorY = 0
    private val savedStyle = Cell()

    // Current SGR style applied to newly printed characters.
    private var curFg = COLOR_DEFAULT
    private var curBg = COLOR_DEFAULT
    private var curBold = false
    private var curItalic = false
    private var curUnderline = false
    private var curInverse = false
    private var curDim = false
    private var curStrike = false

    private val curStyle: Cell
        get() = Cell(
            fg = curFg, bg = curBg, bold = curBold, italic = curItalic,
            underline = curUnderline, inverse = curInverse, dim = curDim, strikethrough = curStrike
        )

    // --- Escape sequence parse state ---
    private val STATE_NORMAL = 0
    private val STATE_ESC = 1
    private val STATE_CSI = 2
    private val STATE_OSC = 3
    private val STATE_CHARSET = 4
    private var state = STATE_NORMAL
    private val csiParams = StringBuilder()
    private var csiPriv = 0.toChar() // '?' / '>' / '=' or 0
    private var csiIntermediate = StringBuilder()
    private val oscBuf = StringBuilder()
    private val charsetBuf = StringBuilder()

    // UTF-8 accumulator.
    private var utf8 = ByteArray(4)
    private var utf8Len = 0
    private var utf8Got = 0

    /** Total rows available for rendering: scrollback + screen. */
    val totalRows: Int get() = scrollback.size + rows

    /** Get a row by combined index (scrollback first, then screen). */
    fun getRow(index: Int): Row =
        if (index < scrollback.size) scrollback[index] else screen[index - scrollback.size]

    // ----------------------------------------------------------------- parsing

    fun input(data: ByteArray) {
        for (b in data) {
            val ub = b.toInt() and 0xFF
            if (utf8Len > 0) {
                if (ub in 0x80..0xbf) {
                    utf8[utf8Got++] = b
                    if (utf8Got == utf8Len) {
                        val cp = decodeUtf8(utf8, utf8Len)
                        utf8Len = 0
                        if (cp >= 0) handle(cp)
                    }
                    continue
                }
                // Invalid continuation; drop partial and reprocess this byte.
                utf8Len = 0
            }
            if (ub < 0x80) {
                handle(ub) // ASCII / C0 control
            } else {
                utf8Len = utf8SeqLen(ub)
                if (utf8Len <= 1) {
                    utf8Len = 0
                    handle(ub)
                } else {
                    utf8[0] = b
                    utf8Got = 1
                }
            }
        }
    }

    fun input(text: String) = input(text.toByteArray(Charsets.UTF_8))

    private fun utf8SeqLen(b: Int): Int = when {
        b and 0xe0 == 0xc0 -> 2
        b and 0xf0 == 0xe0 -> 3
        b and 0xf8 == 0xf0 -> 4
        else -> 1
    }

    private fun decodeUtf8(buf: ByteArray, len: Int): Int {
        return when (len) {
            1 -> buf[0].toInt() and 0xff
            2 -> ((buf[0].toInt() and 0x1f) shl 6) or (buf[1].toInt() and 0x3f)
            3 -> ((buf[0].toInt() and 0x0f) shl 12) or ((buf[1].toInt() and 0x3f) shl 6) or (buf[2].toInt() and 0x3f)
            4 -> ((buf[0].toInt() and 0x07) shl 18) or ((buf[1].toInt() and 0x3f) shl 12) or
                ((buf[2].toInt() and 0x3f) shl 6) or (buf[3].toInt() and 0x3f)
            else -> -1
        }
    }

    private fun handle(c: Int) {
        when (state) {
            STATE_ESC -> handleEsc(c)
            STATE_CSI -> handleCsi(c)
            STATE_OSC -> handleOsc(c)
            STATE_CHARSET -> { charsetBuf.clear(); state = STATE_NORMAL }
            else -> handleNormal(c)
        }
    }

    private fun handleNormal(c: Int) {
        when (c) {
            0x00, 0x07, 0x0e, 0x0f -> { /* NUL, BEL, SO, SI */ }
            0x08 -> { // BS
                wrapPending = false
                if (cursorX > 0) {
                    cursorX--
                    screen[cursorY].cells[cursorX].reset()
                }
            }
            0x09 -> { // HT
                wrapPending = false
                val next = ((cursorX / 8) + 1) * 8
                cursorX = if (next < cols) next else cols - 1
            }
            0x0a, 0x0b, 0x0c -> { wrapPending = false; lineFeed() } // LF, VT, FF
            0x0d -> { wrapPending = false; cursorX = 0 } // CR
            0x1b -> { wrapPending = false; state = STATE_ESC }
            else -> {
                if (c >= 0x20) putChar(c)
            }
        }
    }

    private fun handleEsc(c: Int) {
        when (c) {
            '['.code -> {
                csiParams.clear(); csiPriv = 0.toChar(); csiIntermediate.clear()
                state = STATE_CSI
            }
            ']'.code -> { oscBuf.clear(); state = STATE_OSC }
            '('.code, ')'.code, '*'.code, '+'.code -> state = STATE_CHARSET
            '='.code -> state = STATE_NORMAL // keypad application (input side)
            '>'.code -> state = STATE_NORMAL // keypad numeric
            'M'.code -> reverseIndex()
            'D'.code -> index()
            'E'.code -> { cursorX = 0; index() }
            '7'.code -> saveCursor()
            '8'.code -> restoreCursor()
            'c'.code -> reset()
            '#'.code -> state = STATE_NORMAL // could be '#8' alignment test
            '\\'.code -> state = STATE_NORMAL // ST
            else -> state = STATE_NORMAL
        }
    }

    private fun handleOsc(c: Int) {
        if (c == 0x07 || (c == 0x1b)) {
            // BEL or ESC terminates OSC; if ESC, it is the start of an ST (ESC \) — keep simple.
            state = STATE_NORMAL
            if (c == 0x1b) state = STATE_ESC
        } else {
            if (c >= 0x20) oscBuf.append(c.toChar())
        }
    }

    private fun handleCsi(c: Int) {
        when {
            c in 0x30..0x39 || c == ';'.code || c == ':'.code -> csiParams.append(c.toChar())
            c == '?'.code || c == '>'.code || c == '='.code -> csiPriv = c.toChar()
            c in 0x20..0x2f -> csiIntermediate.append(c.toChar())
            c >= 0x40 -> {
                val final = c.toChar()
                val params = parseParams(csiParams.toString())
                when (csiPriv) {
                    '?' -> handlePrivate(final, params)
                    else -> handleCsiFinal(final, params, csiIntermediate.toString())
                }
                state = STATE_NORMAL
            }
            else -> state = STATE_NORMAL
        }
    }

    private fun parseParams(s: String): List<Int> {
        if (s.isEmpty()) return emptyList()
        return s.split(';').map { it.toIntOrNull() ?: 0 }
    }

    // -------------------------------------------------------------- CSI finals

    private fun handleCsiFinal(final: Char, p: List<Int>, inter: String) {
        fun d(i: Int, def: Int) = if (i < p.size) (p[i].takeIf { it != 0 } ?: def) else def
        when (final) {
            'A' -> moveCursor(-d(0, 1), 0)
            'B' -> moveCursor(d(0, 1), 0)
            'C' -> moveCursor(0, d(0, 1))
            'D' -> moveCursor(0, -d(0, 1))
            'E' -> { cursorX = 0; moveCursor(d(0, 1), 0) }
            'F' -> { cursorX = 0; moveCursor(-d(0, 1), 0) }
            'G', '`' -> cursorX = clampX(d(0, 1) - 1)
            'd' -> cursorY = clampY(d(0, 1) - 1)
            'H', 'f' -> {
                cursorY = clampY(d(0, 1) - 1)
                cursorX = clampX(d(1, 1) - 1)
            }
            'J' -> eraseInDisplay(p.getOrElse(0) { 0 })
            'K' -> eraseInLine(p.getOrElse(0) { 0 })
            'L' -> insertLines(d(0, 1))
            'M' -> deleteLines(d(0, 1))
            'P' -> deleteChars(d(0, 1))
            '@' -> insertChars(d(0, 1))
            'S' -> scrollUp(d(0, 1))
            'T' -> scrollDown(d(0, 1))
            'm' -> sgr(p)
            'r' -> {
                val top = d(0, 1)
                val bot = if (p.size > 1 && p[1] != 0) p[1] else rows
                if (top in 1..bot && bot <= rows) {
                    scrollTop = top - 1
                    scrollBottom = bot - 1
                    cursorX = 0
                    cursorY = if (originMode) scrollTop else 0
                }
            }
            'h' -> if (inter.contains('h')) { /* ignore */ }
            'l' -> { /* ignore */ }
            's' -> saveCursor()
            'u' -> restoreCursor()
            'n' -> { /* DSR: no reply */ }
            'c' -> { /* DA: no reply */ }
            'g' -> { /* tab clear: ignore */ }
            'X' -> eraseChars(d(0, 1))
            else -> { /* unsupported */ }
        }
    }

    private fun handlePrivate(final: Char, p: List<Int>) {
        when (final) {
            'h' -> for (m in p) setMode(m, true)
            'l' -> for (m in p) setMode(m, false)
            'r' -> { /* DECSTBM with ? — ignore */ }
            'h', 'l' -> { /* nothing */ }
            else -> {}
        }
    }

    private fun setMode(mode: Int, on: Boolean) {
        when (mode) {
            6 -> originMode = on
            7 -> autoWrap = on
            25 -> cursorVisible = on // ?25h shows, ?25l hides
            47, 1047, 1049 -> if (on) enterAltScreen() else exitAltScreen()
            1000, 1002, 1006 -> { /* mouse reporting: not implemented */ }
            2004 -> { /* bracketed paste: not implemented */ }
        }
    }

    // -------------------------------------------------------------- operations

    private fun moveCursor(dy: Int, dx: Int) {
        cursorY = clampY(cursorY + dy)
        cursorX = clampX(cursorX + dx)
    }

    private fun clampX(x: Int) = x.coerceIn(0, cols - 1)
    private fun clampY(y: Int) = y.coerceIn(0, rows - 1)

    private fun lineFeed() {
        if (cursorY == scrollBottom) {
            scrollUp(1)
        } else if (cursorY < rows - 1) {
            cursorY++
        }
    }

    private fun index() {
        if (cursorY == scrollBottom) scrollUp(1) else if (cursorY < rows - 1) cursorY++
    }

    private fun reverseIndex() {
        if (cursorY == scrollTop) scrollDown(1) else if (cursorY > 0) cursorY--
    }

    private fun scrollUp(n: Int) {
        repeat(n) {
            if (scrollback.size >= maxScrollback) scrollback.removeFirstOrNull()
            scrollback.addLast(screen[scrollTop].copy())
            for (r in scrollTop until scrollBottom) screen[r] = screen[r + 1]
            screen[scrollBottom] = Row(cols).also { it.blank(curStyle) }
        }
    }

    private fun scrollDown(n: Int) {
        repeat(n) {
            for (r in scrollBottom downTo scrollTop + 1) screen[r] = screen[r - 1]
            screen[scrollTop] = Row(cols).also { it.blank(curStyle) }
        }
    }

    private fun insertLines(n: Int) {
        repeat(n) {
            for (r in scrollBottom downTo cursorY + 1) screen[r] = screen[r - 1]
            screen[cursorY] = Row(cols).also { it.blank(curStyle) }
        }
        cursorX = 0
    }

    private fun deleteLines(n: Int) {
        repeat(n) {
            for (r in cursorY until scrollBottom) screen[r] = screen[r + 1]
            screen[scrollBottom] = Row(cols).also { it.blank(curStyle) }
        }
        cursorX = 0
    }

    private fun insertChars(n: Int) {
        val row = screen[cursorY]
        for (i in cols - 1 downTo cursorX + n) row.cells[i].copyFrom(row.cells[i - n])
        for (i in cursorX until min(cursorX + n, cols)) {
            row.cells[i].ch = ' '.code
            row.cells[i].fg = curFg; row.cells[i].bg = curBg
            row.cells[i].bold = curBold; row.cells[i].italic = curItalic
            row.cells[i].underline = curUnderline; row.cells[i].inverse = curInverse
            row.cells[i].dim = curDim; row.cells[i].strikethrough = curStrike
        }
    }

    private fun deleteChars(n: Int) {
        val row = screen[cursorY]
        for (i in cursorX until cols - n) row.cells[i].copyFrom(row.cells[i + n])
        for (i in cols - n until cols) {
            row.cells[i].ch = ' '.code
            row.cells[i].fg = curFg; row.cells[i].bg = curBg
            row.cells[i].bold = curBold; row.cells[i].italic = curItalic
            row.cells[i].underline = curUnderline; row.cells[i].inverse = curInverse
            row.cells[i].dim = curDim; row.cells[i].strikethrough = curStrike
        }
    }

    private fun eraseChars(n: Int) {
        val row = screen[cursorY]
        for (i in cursorX until min(cursorX + n, cols)) {
            row.cells[i].ch = ' '.code
            row.cells[i].fg = curFg; row.cells[i].bg = curBg
            row.cells[i].bold = curBold; row.cells[i].italic = curItalic
            row.cells[i].underline = curUnderline; row.cells[i].inverse = curInverse
            row.cells[i].dim = curDim; row.cells[i].strikethrough = curStrike
        }
    }

    private fun eraseInDisplay(mode: Int) {
        when (mode) {
            0 -> { eraseInLine(0); for (r in cursorY + 1 until rows) blankRow(r) }
            1 -> { eraseInLine(1); for (r in 0 until cursorY) blankRow(r) }
            2, 3 -> { for (r in 0 until rows) blankRow(r); if (mode == 3) scrollback.clear() }
        }
    }

    private fun eraseInLine(mode: Int) {
        val row = screen[cursorY]
        when (mode) {
            0 -> for (i in cursorX until cols) resetCell(row, i)
            1 -> for (i in 0..cursorX) resetCell(row, i)
            2 -> for (i in 0 until cols) resetCell(row, i)
        }
    }

    private fun blankRow(r: Int) {
        screen[r] = Row(cols).also { it.blank(curStyle) }
    }

    private fun resetCell(row: Row, i: Int) {
        val c = row.cells[i]
        c.ch = ' '.code
        c.fg = curFg; c.bg = curBg
        c.bold = curBold; c.italic = curItalic
        c.underline = curUnderline; c.inverse = curInverse
        c.dim = curDim; c.strikethrough = curStrike
    }

    private fun sgr(p: List<Int>) {
        if (p.isEmpty()) { resetStyle(); return }
        var i = 0
        while (i < p.size) {
            val code = p[i]
            when {
                code == 0 -> resetStyle()
                code == 1 -> curBold = true
                code == 2 -> curDim = true
                code == 3 -> curItalic = true
                code == 4 -> curUnderline = true
                code == 5 || code == 6 -> { /* blink: ignore */ }
                code == 7 -> curInverse = true
                code == 8 -> { /* conceal: ignore */ }
                code == 9 -> curStrike = true
                code == 21 -> curBold = false
                code == 22 -> { curBold = false; curDim = false }
                code == 23 -> curItalic = false
                code == 24 -> curUnderline = false
                code == 27 -> curInverse = false
                code == 29 -> curStrike = false
                code in 30..37 -> curFg = TerminalPalette.color(code - 30)
                code == 38 -> { val r = readExtendedColor(p, i); curFg = r.first; i = r.second }
                code == 39 -> curFg = COLOR_DEFAULT
                code in 40..47 -> curBg = TerminalPalette.color(code - 40)
                code == 48 -> { val r = readExtendedColor(p, i); curBg = r.first; i = r.second }
                code == 49 -> curBg = COLOR_DEFAULT
                code in 90..97 -> curFg = TerminalPalette.color(code - 90 + 8)
                code in 100..107 -> curBg = TerminalPalette.color(code - 100 + 8)
            }
            i++
        }
    }

    /** Reads 38/48 extended color (`:5:n` or `:2:r:g:b`, optional leading `:`), returns (color, newIndex). */
    private fun readExtendedColor(p: List<Int>, start: Int): Pair<Int, Int> {
        // p[start] is 38 or 48. Look at p[start+1].
        if (start + 1 >= p.size) return COLOR_DEFAULT to start
        return when (p[start + 1]) {
            2 -> { // :2:r:g:b (some terminals use ';' separators)
                val r = p.getOrNull(start + 2) ?: 0
                val g = p.getOrNull(start + 3) ?: 0
                val b = p.getOrNull(start + 4) ?: 0
                (r shl 16 or (g shl 8) or b) to (start + 4)
            }
            5 -> {
                val idx = p.getOrNull(start + 2) ?: 0
                TerminalPalette.color(idx) to (start + 2)
            }
            else -> COLOR_DEFAULT to start
        }
    }

    private fun resetStyle() {
        curFg = COLOR_DEFAULT
        curBg = COLOR_DEFAULT
        curBold = false; curItalic = false; curUnderline = false
        curInverse = false; curDim = false; curStrike = false
    }

    private fun saveCursor() {
        savedCursorX = cursorX
        savedCursorY = cursorY
        savedStyle.copyFrom(curStyle)
    }

    private fun restoreCursor() {
        cursorX = clampX(savedCursorX)
        cursorY = clampY(savedCursorY)
        curFg = savedStyle.fg; curBg = savedStyle.bg
        curBold = savedStyle.bold; curItalic = savedStyle.italic
        curUnderline = savedStyle.underline; curInverse = savedStyle.inverse
        curDim = savedStyle.dim; curStrike = savedStyle.strikethrough
    }

    private fun enterAltScreen() {
        if (altScreen) return
        altScreen = true
        altBuffer = screen
        screen = Array(rows) { Row(cols).also { it.blank(curStyle) } }
        cursorX = 0; cursorY = 0
    }

    private fun exitAltScreen() {
        if (!altScreen) return
        altScreen = false
        altBuffer?.let { screen = it }
        altBuffer = null
    }

    private fun reset() {
        screen = Array(rows) { Row(cols) }
        scrollback.clear()
        cursorX = 0; cursorY = 0; cursorVisible = true
        scrollTop = 0; scrollBottom = rows - 1
        originMode = false; autoWrap = true; wrapPending = false
        exitAltScreen()
        resetStyle()
    }

    /** Print a single code point at the cursor, applying autowrap and wide-char handling. */
    private fun putChar(cp: Int) {
        if (wrapPending) {
            wrapPending = false
            cursorX = 0
            lineFeed()
        }
        val w = wcwidth(cp)
        if (w < 0) return // control
        if (cursorX >= cols) {
            if (!autoWrap) {
                cursorX = cols - 1
            } else {
                cursorX = 0
                lineFeed()
            }
        }
        val row = screen[cursorY]
        val cell = row.cells[cursorX]
        cell.ch = cp
        cell.fg = curFg; cell.bg = curBg
        cell.bold = curBold; cell.italic = curItalic
        cell.underline = curUnderline; cell.inverse = curInverse
        cell.dim = curDim; cell.strikethrough = curStrike

        if (w == 2) {
            if (cursorX + 1 < cols) {
                val nxt = row.cells[cursorX + 1]
                nxt.ch = WIDE_CONT; nxt.fg = curFg; nxt.bg = curBg
                nxt.bold = curBold; nxt.italic = curItalic; nxt.underline = curUnderline
                nxt.inverse = curInverse; nxt.dim = curDim; nxt.strikethrough = curStrike
            }
        }
        if (autoWrap && cursorX == cols - 1) {
            wrapPending = true
        } else {
            cursorX = min(cursorX + w, cols - 1)
        }
    }

    val colCount: Int get() = cols
    val rowCount: Int get() = rows

    /** Erase the visible screen and the scrollback buffer. */
    fun clearScreen() {
        for (r in 0 until rows) screen[r] = Row(cols).also { it.blank(curStyle) }
        scrollback.clear()
        cursorX = 0
        cursorY = 0
    }

    /** Resize the screen, preserving existing content where possible. */
    fun resize(newCols: Int, newRows: Int) {
        if (newCols <= 0 || newRows <= 0) return
        if (newCols == cols && newRows == rows) return
        val newScreen = Array(newRows) { Row(newCols) }
        for (r in 0 until min(newRows, rows)) {
            val src = screen[r]
            for (c in 0 until min(newCols, cols)) newScreen[r].cells[c].copyFrom(src.cells[c])
        }
        screen = newScreen
        cols = newCols
        rows = newRows
        scrollTop = 0
        scrollBottom = rows - 1
        cursorX = clampX(cursorX)
        cursorY = clampY(cursorY)
    }
}

/** Approximate East-Asian width: returns 2 for wide characters, 1 otherwise, -1 for controls. */
fun wcwidth(cp: Int): Int {
    if (cp == 0) return 0
    if (cp < 32 || (cp >= 0x7f && cp < 0xa0)) return -1
    if (cp == 0x00ad || cp == 0x200b || (cp >= 0x2028 && cp <= 0x202e) ||
        cp == 0x2060 || cp == 0xfeff || cp == 0x180e
    ) return 0
    if (cp >= 0x1100 &&
        (cp <= 0x115f || cp in 0x2e80..0x303e || cp == 0x303f || cp in 0x3041..0x33ff ||
            cp in 0x3400..0x4dbf || cp in 0x4e00..0x9fff || cp in 0xa000..0xa4cf ||
            cp in 0xac00..0xd7a3 || cp in 0xf900..0xfaff || cp in 0xfe10..0xfe19 ||
            cp in 0xfe30..0xfe6f || cp in 0xff00..0xff60 || cp in 0xffe0..0xffe6 ||
            cp in 0x1f300..0x1faff || cp in 0x20000..0x3fffd || cp in 0x30000..0x3fffd)
    ) {
        return 2
    }
    return 1
}

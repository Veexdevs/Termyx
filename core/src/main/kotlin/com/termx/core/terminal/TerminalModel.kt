package com.termx.core.terminal

/** Sentinel for the default foreground/background color (resolved by the UI theme). */
const val COLOR_DEFAULT: Int = -1

/** Marker for a cell that is the trailing half of a wide (CJK) character. */
const val WIDE_CONT: Int = -1

/**
 * A single terminal cell. Holds a Unicode code point and its visual attributes.
 * Cells are mutated in place by the emulator for performance.
 */
class Cell(
    var ch: Int = ' '.code,
    var fg: Int = COLOR_DEFAULT,
    var bg: Int = COLOR_DEFAULT,
    var bold: Boolean = false,
    var italic: Boolean = false,
    var underline: Boolean = false,
    var inverse: Boolean = false,
    var dim: Boolean = false,
    var strikethrough: Boolean = false
) {
    fun reset() {
        ch = ' '.code
        fg = COLOR_DEFAULT
        bg = COLOR_DEFAULT
        bold = false
        italic = false
        underline = false
        inverse = false
        dim = false
        strikethrough = false
    }

    fun copyFrom(o: Cell) {
        ch = o.ch
        fg = o.fg
        bg = o.bg
        bold = o.bold
        italic = o.italic
        underline = o.underline
        inverse = o.inverse
        dim = o.dim
        strikethrough = o.strikethrough
    }

    fun sameStyleAs(o: Cell): Boolean =
        fg == o.fg && bg == o.bg && bold == o.bold && italic == o.italic &&
            underline == o.underline && inverse == o.inverse && dim == o.dim &&
            strikethrough == o.strikethrough
}

/** A terminal row: a fixed-width array of cells. */
class Row(val cols: Int) {
    val cells = Array(cols) { Cell() }

    fun blank(style: Cell) {
        for (c in cells) {
            c.ch = ' '.code
            c.fg = style.fg
            c.bg = style.bg
            c.bold = style.bold
            c.italic = style.italic
            c.underline = style.underline
            c.inverse = style.inverse
            c.dim = style.dim
            c.strikethrough = style.strikethrough
        }
    }

    fun copy(): Row {
        val r = Row(cols)
        for (i in cells.indices) r.cells[i].copyFrom(cells[i])
        return r
    }
}

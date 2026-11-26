package com.termx.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onSizeChanged
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.runtime.collectAsState
import com.termx.core.terminal.Cell
import com.termx.core.terminal.COLOR_DEFAULT
import com.termx.core.terminal.TerminalEmulator
import com.termx.core.terminal.WIDE_CONT
import com.termx.core.terminal.wcwidth
import com.termx.terminal.TerminalKeyHandler
import com.termx.terminal.TerminalViewModel

private val DEFAULT_FG = 0xFFE8E8E8.toInt()
private val DEFAULT_BG = 0xFF0A0A0A.toInt()

@Composable
fun TerminalScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: TerminalViewModel = viewModel()
    val emulator = vm.emulator
    val version by vm.version.collectAsState()
    val running by vm.running.collectAsState()

    var ctrl by remember { mutableStateOf(false) }
    var alt by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val fontSize = 13.sp
    val fontSizePx = with(density) { fontSize.toPx() }
    val textPaint = remember(fontSizePx) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSizePx
            typeface = Typeface.MONOSPACE
        }
    }
    val cellW = remember(textPaint) { textPaint.measureText("M") }
    val cellH = fontSizePx * 1.25f
    val bgPaint = remember { Paint() }

    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var tfv by remember { mutableStateOf(TextFieldValue("")) }

    val send = { s: String ->
        if (s.isNotEmpty()) vm.send(s)
        ctrl = false
        alt = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(8.dp)
            .background(Color(DEFAULT_BG))
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(DEFAULT_BG))
                .onPreviewKeyEvent { ev ->
                    if (ev.type != KeyEventType.KeyDown) false
                    else {
                        val seq = TerminalKeyHandler.hardwareKeySequence(
                            keyCode = ev.nativeKeyCode,
                            ctrl = ctrl || ev.isCtrlPressed,
                            alt = alt || ev.isAltPressed,
                            shift = ev.isShiftPressed,
                            meta = ev.isMetaPressed,
                            cursorApp = false
                        )
                        if (seq != null) {
                            send(seq)
                            true
                        } else false
                    }
                }
        ) {
            TerminalCanvas(
                emulator = emulator,
                version = version,
                cellW = cellW,
                cellH = cellH,
                textPaint = textPaint,
                bgPaint = bgPaint
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeyChip("CTRL", ctrl) { ctrl = !ctrl }
            KeyChip("ALT", alt) { alt = !alt }
            KeyChip("ESC") { send("\u001b") }
            KeyChip("TAB") { send("\t") }
            KeyChip("←") { send("\u001b[D") }
            KeyChip("→") { send("\u001b[C") }
            KeyChip("↑") { send("\u001b[A") }
            KeyChip("↓") { send("\u001b[B") }
            KeyChip("⌨") {
                focusRequester.requestFocus()
                keyboard?.show()
            }
            KeyChip("Clear") { vm.clear() }
            KeyChip("Restart") { vm.restart() }
        }

        if (!running) {
            Text(
                "Session ended (exit ${vm.exitCode.value ?: "?"}). Tap Restart for a new shell.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        BasicTextField(
            value = tfv,
            onValueChange = { nv ->
                val added = nv.text.substring(tfv.text.length)
                if (added.isNotEmpty()) {
                    send(TerminalKeyHandler.printableSequence(added.replace("\n", "\r"), ctrl, alt))
                }
                tfv = TextFieldValue("")
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send("\r") }),
            modifier = Modifier
                .width(0.dp)
                .height(0.dp)
                .focusRequester(focusRequester)
        )

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
}

@Composable
private fun KeyChip(label: String, active: Boolean = false, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        colors = if (active) {
            ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1B5E20))
        } else {
            ButtonDefaults.outlinedButtonColors()
        }
    ) {
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun TerminalCanvas(
    emulator: TerminalEmulator,
    version: Int,
    cellW: Float,
    cellH: Float,
    textPaint: Paint,
    bgPaint: Paint
) {
    val density = LocalDensity.current
    val toDp = { px: Float -> with(density) { px.toDp() } }
    val scroll = rememberScrollState()
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(Modifier.fillMaxSize().onSizeChanged { size = it }) {
        if (size != IntSize.Zero) {
            val widthPx = size.width.toFloat()
            val heightPx = size.height.toFloat()
            val cols = maxOf(1, (widthPx / cellW).toInt())
            val rows = maxOf(1, (heightPx / cellH).toInt())
            emulator.resize(cols, rows)

            LaunchedEffect(version, emulator.totalRows) {
                val target = (emulator.totalRows * cellH - heightPx).toInt().coerceAtLeast(0)
                scroll.scrollTo(target)
            }

            val totalH = (emulator.totalRows * cellH).coerceAtLeast(heightPx)
            val topPad = (totalH - heightPx).coerceAtLeast(0f)
            val scrollPos = scroll.value

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
            ) {
                androidx.compose.foundation.layout.Spacer(Modifier.height(toDp(topPad)))
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(toDp(totalH))
                        .background(Color(DEFAULT_BG))
                ) {
                    val first = (scrollPos / cellH).toInt() - 1
                    val last = ((scrollPos + heightPx) / cellH).toInt() + 1
                    val total = emulator.totalRows
                    val canvas = drawContext.canvas.nativeCanvas
                    for (row in maxOf(0, first)..minOf(total - 1, last)) {
                        drawRow(canvas, emulator, row, cellW, cellH, textPaint, bgPaint)
                    }
                    val cursorRow = total - emulator.rowCount + emulator.cursorY
                    if (emulator.cursorVisible &&
                        cursorRow in maxOf(0, first)..minOf(total - 1, last)
                    ) {
                        val cx = emulator.cursorX * cellW
                        val cy = cursorRow * cellH
                        bgPaint.color = 0xFF66CCFF.toInt()
                        canvas.drawRect(cx, cy, cx + cellW, cy + cellH, bgPaint)
                    }
                }
            }
        }
    }
}

private fun drawRow(
    canvas: android.graphics.Canvas,
    emulator: TerminalEmulator,
    row: Int,
    cellW: Float,
    cellH: Float,
    textPaint: Paint,
    bgPaint: Paint
) {
    val r = emulator.getRow(row)
    val yTop = row * cellH
    val baseline = yTop + cellH * 0.5f + textPaint.textSize * 0.35f
    val cols = emulator.colCount
    var col = 0
    while (col < cols) {
        val cell = r.cells[col]
        if (cell.ch == WIDE_CONT) {
            col++
            continue
        }
        var end = col
        val sb = StringBuilder()
        var runPx = 0f
        while (end < cols) {
            val c = r.cells[end]
            if (c.ch == WIDE_CONT) break
            if (!c.sameStyleAs(cell)) break
            sb.append(if (c.ch == 0) ' ' else c.ch.toChar())
            runPx += (wcwidth(c.ch).coerceAtLeast(1)) * cellW
            end++
        }
        val x = col * cellW
        val fg = resolveColor(cell.fg, DEFAULT_FG)
        val bg = resolveColor(cell.bg, DEFAULT_BG)
        if (cell.inverse) {
            if (bg != DEFAULT_BG) {
                bgPaint.color = fg
                canvas.drawRect(x, yTop, x + runPx, yTop + cellH, bgPaint)
            }
            textPaint.color = bg
        } else {
            if (bg != DEFAULT_BG) {
                bgPaint.color = bg
                canvas.drawRect(x, yTop, x + runPx, yTop + cellH, bgPaint)
            }
            textPaint.color = fg
        }
        val wasBold = textPaint.isFakeBoldText
        val wasSkew = textPaint.textSkewX
        textPaint.isFakeBoldText = cell.bold
        textPaint.textSkewX = if (cell.italic) -0.25f else 0f
        canvas.drawText(sb.toString(), x, baseline, textPaint)
        if (cell.underline) {
            canvas.drawLine(x, baseline + 2f, x + runPx, baseline + 2f, textPaint)
        }
        textPaint.isFakeBoldText = wasBold
        textPaint.textSkewX = wasSkew
        col = end
    }
}

private fun resolveColor(c: Int, def: Int): Int =
    if (c == COLOR_DEFAULT) def else (0xFF000000.toInt() or c)

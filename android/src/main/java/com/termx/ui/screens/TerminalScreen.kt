package com.termx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.PaddingValues
import com.termx.terminal.TerminalViewModel

@Composable
fun TerminalScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: TerminalViewModel = viewModel()
    val output by vm.output
    val running by vm.running

    var input by remember { mutableStateOf("") }
    val scroll = rememberScrollState()

    LaunchedEffect(output) {
        scroll.animateScrollTo(scroll.maxValue)
    }

    Column(
        Modifier.fillMaxSize().padding(padding).padding(8.dp).imePadding()
    ) {
        // Control bar
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { vm.sendControl("\u0003") }) { Text("Ctrl+C") }
            TextButton(onClick = { vm.sendControl("\u0004") }) { Text("Ctrl+D") }
            TextButton(onClick = { vm.sendControl("\u001b[A") }) { Text("↑") }
            TextButton(onClick = { vm.sendControl("\u001b[B") }) { Text("↓") }
            TextButton(onClick = { vm.sendControl("\u0009") }) { Text("Tab") }
            TextButton(onClick = { vm.clear() }) { Text("Clear") }
            TextButton(onClick = { vm.restart() }) { Text("Restart") }
        }

        // Output
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .background(Color(0xFF0A0A0A))
                .padding(8.dp)
        ) {
            Text(
                text = if (output.isEmpty()) "Starting shell…" else output,
                color = Color(0xFFE8E8E8),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.verticalScroll(scroll)
            )
        }

        // Input
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).padding(8.dp),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                decorationBox = { inner -> if (input.isEmpty()) Text("$ ", color = Color.Gray) else inner() }
            )
            OutlinedButton(onClick = {
                vm.send(input + "\n")
                input = ""
            }) { Text("Send") }
        }

        if (!running) {
            Text("Session ended (exit ${vm.exitCode.value}). Tap Restart to start a new shell.", color = MaterialTheme.colorScheme.error)
        }
    }
}

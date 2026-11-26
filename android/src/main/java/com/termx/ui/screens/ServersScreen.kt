package com.termx.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.PaddingValues
import com.termx.core.model.ServerState
import com.termx.core.model.ServerType
import com.termx.servers.ServerService
import com.termx.ui.components.ActionButton
import com.termx.ui.components.CardSection
import com.termx.ui.components.InfoRow
import com.termx.ui.components.ScreenSurface
import com.termx.ui.components.SecondaryButton

@Composable
fun ServersScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: ServersViewModel = viewModel()
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    var logsFor by remember { mutableStateOf<String?>(null) }

    ScreenSurface("SERVERS", padding) {
        ActionButton("Add server") { showAdd = true }
        Text(
            "Servers run as real OS processes. 'Run in background' starts a foreground " +
                "service (best-effort 24/7). Local servers work without Wi-Fi or internet.",
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp)
        )

        vm.list().forEach { server ->
            val state = vm.state(server.id)
            CardSection(server.name) {
                InfoRow("Type", server.typeLabel)
                InfoRow("State", state.name)
                InfoRow("Port", if (server.port > 0) server.port.toString() else "—")
                InfoRow("Auto-restart", if (server.autoRestart) "on" else "off")
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SecondaryButton(if (state == ServerState.RUNNING) "Stop" else "Start") {
                        if (state == ServerState.RUNNING) vm.stop(server.id) else vm.start(server.id)
                    }
                    SecondaryButton("Restart") { vm.restart(server.id) }
                    SecondaryButton("Logs") { logsFor = server.id }
                }
            }
        }

        if (logsFor != null) {
            val id = logsFor!!
            AlertDialog(
                onDismissRequest = { logsFor = null },
                confirmButton = { TextButton({ logsFor = null }) { Text("Close") } },
                title = { Text("Logs") },
                text = {
                    Text(vm.logs(id).ifBlank { "(no output yet)" }, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                }
            )
        }
    }

    if (showAdd) {
        AddServerDialog(
            onDismiss = { showAdd = false },
            onAdd = { name, type, cmd, dir, port, ar, as_ ->
                vm.create(name, type, cmd, dir, port, ar, as_)
                // Start the foreground service so the process can persist.
                context.startForegroundService(Intent(context, ServerService::class.java))
                showAdd = false
            }
        )
    }
}

@Composable
private fun AddServerDialog(
    onDismiss: () -> Unit,
    onAdd: (String, ServerType, String, String, Int, Boolean, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var dir by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("0") }
    var type by remember { mutableStateOf(ServerType.CUSTOM) }
    var autoRestart by remember { mutableStateOf(false) }
    var autoStart by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onAdd(name.ifBlank { "server" }, type, command.ifBlank { "echo hi" }, dir.ifBlank { "." }, port.toIntOrNull() ?: 0, autoRestart, autoStart)
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
        title = { Text("New server") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") })
                OutlinedTextField(command, { command = it }, Modifier.fillMaxWidth(), label = { Text("Start command") }, singleLine = false)
                OutlinedTextField(dir, { dir = it }, Modifier.fillMaxWidth(), label = { Text("Working dir") })
                OutlinedTextField(port, { port = it }, Modifier.fillMaxWidth(), label = { Text("Port") })
                Text("Type: ${type.name}", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(if (autoRestart) "Auto-restart: ON" else "Auto-restart: OFF") { autoRestart = !autoRestart }
                    SecondaryButton(if (autoStart) "Auto-start: ON" else "Auto-start: OFF") { autoStart = !autoStart }
                }
            }
        }
    )
}

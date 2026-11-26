package com.termx.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.PaddingValues
import com.termx.ui.components.ActionButton
import com.termx.ui.components.CardSection
import com.termx.ui.components.InfoRow
import com.termx.ui.components.ScreenSurface
import com.termx.ui.components.SecondaryButton

@Composable
fun DeviceAccessScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: DeviceAccessViewModel = viewModel()
    val context = LocalContext.current
    val status by vm.status.collectAsState()
    val output by vm.output.collectAsState()

    var cmd by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.check() }

    ScreenSurface("Device Access", padding) {
        Text(
            "TERMYX never assumes root and never tries to gain it. Operations run only when the " +
                "privilege is actually available and the user approves.",
            style = MaterialTheme.typography.bodySmall
        )

        CardSection("Availability") {
            val s = status
            if (s == null) Text("Checking…")
            else {
                InfoRow("Root", if (s.rootGranted) "Granted" else "Unavailable")
                InfoRow("Shizuku", if (s.shizukuAvailable) "Installed (API pending)" else "Not installed")
                InfoRow("KernelSU", if (s.kernelSuAvailable) "Detected" else "No")
                InfoRow("APatch", if (s.apatchAvailable) "Detected" else "No")
                SecondaryButton("Re-check") { vm.check() }
            }
        }

        CardSection("Authorized command (root)") {
            OutlinedTextField(cmd, { cmd = it }, Modifier.fillMaxWidth(), label = { Text("Command (runs via su)") })
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("Run authorized") {
                    if (cmd.isNotBlank()) confirm = true
                }
            }
        }

        output?.let {
            CardSection("Output") {
                Text(it, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                SecondaryButton("Clear") { vm.clearOutput() }
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            confirmButton = { TextButton({ confirm = false; vm.runAuthorized(cmd) }) { Text("Allow") } },
            dismissButton = { TextButton({ confirm = false }) { Text("Cancel") } },
            title = { Text("Confirm root command") },
            text = { Text("This will run as root:\n$cmd") }
        )
    }
}

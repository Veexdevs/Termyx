package com.termx.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.PaddingValues
import com.termx.ui.components.CardSection
import com.termx.ui.components.ScreenSurface
import com.termx.ui.components.ToggleRow

@Composable
fun SettingsScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: SettingsViewModel = viewModel()
    val dark by vm.darkTheme.collectAsState()
    val confirm by vm.confirmDestructive.collectAsState()
    val autoStart by vm.autoStartServers.collectAsState()

    ScreenSurface("Settings", padding) {
        CardSection("Appearance") {
            ToggleRow("Dark theme", dark) { vm.setDark(it) }
        }
        CardSection("Safety") {
            ToggleRow("Confirm destructive operations", confirm) { vm.setConfirm(it) }
            ToggleRow("Auto-start servers after reboot", autoStart) { vm.setAutoStartServers(it) }
        }
        CardSection("Security") {
            androidx.compose.material3.Text(
                "API keys are stored encrypted with EncryptedSharedPreferences and never written to " +
                    "the repository or plain preferences. Agent tools enforce permission and confirmation " +
                    "before destructive or privileged actions.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )
        }
    }
}

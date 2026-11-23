package com.termx.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.PaddingValues
import com.termx.core.model.ServerState
import com.termx.ui.components.CardSection
import com.termx.ui.components.InfoRow
import com.termx.ui.components.ScreenSurface
import com.termx.ui.components.formatBytes
import com.termx.ui.components.formatPercent

@Composable
fun HomeScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: HomeViewModel = viewModel()
    val metrics by vm.metrics
    val device by vm.device

    ScreenSurface("HOME", padding) {
        CardSection("Device") {
            val m = metrics
            InfoRow("CPU", formatPercent(m?.cpuUsagePercent ?: -1.0))
            InfoRow("Memory", m?.let { "${formatPercent(it.memoryUsedPercent)} (${formatBytes(it.usedMemoryBytes)}/${formatBytes(it.totalMemoryBytes)})" } ?: "N/A")
            InfoRow("Storage", m?.let { "${formatPercent(it.storageUsedPercent)} (${formatBytes(it.usedStorageBytes)}/${formatBytes(it.totalStorageBytes)})" } ?: "N/A")
            InfoRow("Temperature", m?.let { if (it.temperatureCelsius < 0) "N/A" else "%.1f °C".format(it.temperatureCelsius) } ?: "N/A")
            InfoRow("Root", device?.rootGranted?.let { if (it) "Granted" else "Unavailable" } ?: "Checking…")
            InfoRow("Shizuku", device?.shizukuAvailable?.let { if (it) "Installed" else "Not installed" } ?: "Checking…")
        }

        CardSection("Installed Systems (${vm.installedSystems.size})") {
            if (vm.installedSystems.isEmpty()) {
                Text("No Linux systems installed yet. Open SYSTEMS to install one.", style = MaterialTheme.typography.bodyMedium)
            } else {
                vm.installedSystems.forEach {
                    InfoRow(it.name, it.state)
                }
            }
        }

        CardSection("Active Servers (${vm.servers.count { it.id.isNotBlank() }})") {
            if (vm.servers.isEmpty()) {
                Text("No servers configured. Open SERVERS to add one.", style = MaterialTheme.typography.bodyMedium)
            } else {
                vm.servers.forEach {
                    InfoRow(it.name, it.typeLabel)
                }
            }
        }

        CardSection("Recent Activity") {
            Text("Terminal sessions, agent runs and server events appear here.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

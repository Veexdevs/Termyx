package com.termx.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.termx.core.model.LinuxDistribution
import com.termx.ui.components.ActionButton
import com.termx.ui.components.CardSection
import com.termx.ui.components.ScreenSurface
import com.termx.ui.components.SecondaryButton

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystemsScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: SystemsViewModel = viewModel()
    val status by vm.status
    val busy by vm.busy

    ScreenSurface("SYSTEMS", padding) {
        Text(
            "Install Linux distributions via PRoot (no root required). Real installation " +
                "needs the proot-distro tooling bundled in the environment; if it is missing the " +
                "operation reports the requirement instead of faking success.",
            style = MaterialTheme.typography.bodySmall
        )

        if (status != null) {
            CardSection {
                Text(status ?: "", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text("Available distributions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LinuxDistribution.entries.forEach { distro ->
                Column(Modifier.weight(1f, fill = false).padding(4.dp)) {
                    CardSection(distro.displayName) {
                        Text("Version: ${distro.defaultVersion}", style = MaterialTheme.typography.bodySmall)
                        Text("Arch: ${distro.architectures.joinToString()}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (vm.isInstalled(distro)) "Installed" else "Not installed",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (vm.isInstalled(distro)) {
                            SecondaryButton("Reinstall") { }
                        } else {
                            ActionButton(if (busy) "Working…" else "Install") { vm.install(distro) }
                        }
                    }
                }
            }
        }
    }
}

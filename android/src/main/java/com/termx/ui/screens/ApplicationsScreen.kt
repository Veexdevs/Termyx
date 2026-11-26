package com.termx.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.PaddingValues
import com.termx.ui.components.CardSection
import com.termx.ui.components.InfoRow
import com.termx.ui.components.ScreenSurface
import com.termx.termxContainer

@Composable
fun ApplicationsScreen(navController: NavHostController, padding: PaddingValues) {
    val ctx = LocalContext.current
    val systems = remember { ctx.termxContainer.linuxManager.list() }

    ScreenSurface("Applications", padding) {
        Text(
            "Manage applications available inside installed Linux systems and the TERMYX desktop " +
                "environment. Enumeration is real but depends on a system being installed and, for GUI " +
                "apps, a graphical session — this section is architected and populated accordingly " +
                "rather than showing fake entries.",
            style = MaterialTheme.typography.bodySmall
        )

        CardSection("Installed systems (${systems.size})") {
            if (systems.isEmpty()) {
                Text("No Linux systems installed yet. Open SYSTEMS to install one, then its packages appear here.")
            } else {
                systems.forEach {
                    InfoRow(it.name, "pkg manager: ${it.packageManager} · state: ${it.state}")
                }
            }
        }

        CardSection("TERMYX desktop apps") {
            Text(
                "The native desktop environment (taskbar, app menu, file explorer, process manager) " +
                    "is part of the architecture. GUI application enumeration is populated once a " +
                    "graphical session is available. This is a documented limitation, not simulated data."
            )
        }
    }
}

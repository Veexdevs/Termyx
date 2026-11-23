package com.termx.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.PaddingValues
import com.termx.ui.components.CardSection
import com.termx.ui.components.InfoRow
import com.termx.ui.components.ScreenSurface

@Composable
fun AboutScreen(navController: NavHostController, padding: PaddingValues) {
    ScreenSurface("About", padding) {
        CardSection("TERMYX") {
            Text("Beyond the Terminal.", style = MaterialTheme.typography.titleLarge)
            Text("An advanced Linux environment for Android and Windows: real terminal, installable " +
                "operating systems, a self-rendered desktop, 24/7 servers, Root/Shizuku integration, " +
                "a code editor, a file manager and an autonomous AI agent.")
        }
        CardSection("Build") {
            InfoRow("Version", "0.1.0")
            InfoRow("Platform", "Android")
            InfoRow("License", "MIT")
        }
        CardSection("Open source references") {
            Text(
                "termux-app, termux-packages, termux-tools, proot-distro, termux-services, " +
                    "termux-gui, termux-api, termux-boot, Shizuku, Magisk, QEMU. Their licenses and " +
                    "attributions are respected (see NOTICE.md). TERMYX is an independent project.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

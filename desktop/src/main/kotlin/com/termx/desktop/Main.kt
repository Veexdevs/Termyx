package com.termx.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.termx.desktop.ui.DesktopScreens

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "TERMYX — Beyond the Terminal") {
        MaterialTheme { App() }
    }
}

private val SCREENS = listOf(
    "HOME", "TERMINAL", "SYSTEMS", "SERVERS", "IA", "FILES", "EDITOR", "PROVIDERS"
)

@Composable
fun App() {
    val container = remember { DesktopContainer() }
    var screen by remember { mutableStateOf("HOME") }

    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier.width(180.dp).fillMaxHeight().padding(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text("TERMYX", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(8.dp))
            Text("Beyond the Terminal.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            Spacer(Modifier.height(8.dp))
            SCREENS.forEach { name ->
                val selected = name == screen
                Text(
                    name,
                    Modifier.fillMaxWidth().clickable { screen = name }
                        .padding(10.dp)
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .padding(6.dp),
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            DesktopScreens(screen, container)
        }
    }
}

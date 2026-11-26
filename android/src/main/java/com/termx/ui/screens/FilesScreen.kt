package com.termx.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.PaddingValues
import com.termx.ui.components.ActionButton
import com.termx.ui.components.CardSection
import com.termx.ui.components.ScreenSurface
import com.termx.ui.components.SecondaryButton
import com.termx.ui.components.formatBytes

@Composable
fun FilesScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: FilesViewModel = viewModel()
    val entries by vm.entries.collectAsState()
    val path by vm.path.collectAsState()
    var newName by remember { mutableStateOf("") }
    var showNew by remember { mutableStateOf(false) }

    ScreenSurface("Files", padding) {
        CardSection {
            Text(path, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Up") { vm.up() }
                ActionButton("New file") { showNew = true }
            }
        }

        CardSection("Entries (${entries.size})") {
            LazyColumn {
                items(entries) { entry ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.open(entry.name) }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text((if (entry.isDirectory) "📁 " else "📄 ") + entry.name)
                        Text(if (entry.isDirectory) "dir" else formatBytes(entry.size), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showNew) {
        AlertDialog(
            onDismissRequest = { showNew = false },
            confirmButton = { TextButton({ showNew = false; if (newName.isNotBlank()) vm.createFile(newName) }) { Text("Create") } },
            dismissButton = { TextButton({ showNew = false }) { Text("Cancel") } },
            title = { Text("New file") },
            text = {
                Column {
                    OutlinedTextField(newName, { newName = it }, Modifier.fillMaxWidth(), label = { Text("Name") })
                }
            }
        )
    }
}

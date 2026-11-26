package com.termx.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.PaddingValues
import com.termx.ui.components.ActionButton
import com.termx.ui.components.CardSection
import com.termx.ui.components.ScreenSurface
import com.termx.ui.components.SecondaryButton

@Composable
fun CodeEditorScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: FilesViewModel = viewModel()
    val entries by vm.entries
    val runOutput by vm.runOutput

    var selected by remember { mutableStateOf<String?>(null) }
    var content by remember { mutableStateOf("") }
    var dirty by remember { mutableStateOf(false) }

    fun load(name: String) {
        selected = name
        content = vm.read(name)
        dirty = false
    }

    ScreenSurface("Code Editor", padding) {
        Text(
            "Integrated editor with project files. Syntax highlighting is planned (documented " +
                "limitation); editing and run are functional.",
            style = MaterialTheme.typography.bodySmall
        )

        CardSection("Open") {
            LazyColumn(Modifier.height(160.dp)) {
                items(entries.filter { !it.isDirectory }) { entry ->
                    Text(
                        entry.name,
                        Modifier.fillMaxWidth().clickable { load(entry.name) }.padding(6.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        selected?.let { name ->
            if (runOutput != null) {
                AlertDialog(
                    onDismissRequest = { vm.clearRun() },
                    confirmButton = { TextButton({ vm.clearRun() }) { Text("Close") } },
                    title = { Text("Run output") },
                    text = { Text(runOutput ?: "", fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                )
            }

            CardSection("Editing: $name${if (dirty) " *" else ""}") {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it; dirty = true },
                    Modifier.fillMaxWidth().height(280.dp).verticalScroll(rememberScrollState()),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    singleLine = false
                )
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton("Save") {
                        vm.write(name, content)
                        dirty = false
                    }
                    SecondaryButton("Run") {
                        vm.write(name, content)
                        vm.runFile(name)
                    }
                }
            }
        }
    }
}

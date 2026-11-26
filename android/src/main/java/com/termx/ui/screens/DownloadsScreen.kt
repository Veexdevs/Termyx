package com.termx.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.PaddingValues
import com.termx.ui.components.CardSection
import com.termx.ui.components.ScreenSurface
import com.termx.ui.components.SecondaryButton
import com.termx.ui.components.formatBytes

@Composable
fun DownloadsScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: DownloadsViewModel = viewModel()
    val files by vm.files.collectAsState()

    ScreenSurface("Downloads", padding) {
        Text(
            "Files saved in the TERMYX downloads directory. Use the terminal or agent to fetch " +
                "files here (e.g. curl/wget). Validation/integrity checks run before anything is " +
                "treated as installed.",
            style = MaterialTheme.typography.bodySmall
        )
        CardSection("Files (${files.size})") {
            if (files.isEmpty()) {
                Text("No downloads yet.", style = MaterialTheme.typography.bodyMedium)
            }
            LazyColumn {
                items(files) { entry ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(entry.name, Modifier.clickable { })
                        Row {
                            Text(formatBytes(entry.size), style = MaterialTheme.typography.bodySmall)
                            SecondaryButton("Delete") { vm.delete(entry.name) }
                        }
                    }
                }
            }
        }
    }
}

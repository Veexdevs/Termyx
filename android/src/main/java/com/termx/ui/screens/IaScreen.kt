package com.termx.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.foundation.layout.PaddingValues
import com.termx.core.agent.AgentEvent
import com.termx.ui.components.ActionButton
import com.termx.ui.components.ScreenSurface

@Composable
fun IaScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: AgentViewModel = viewModel()
    val running by vm.running
    val log by vm.log
    val confirmation by vm.confirmation

    var request by remember { mutableStateOf("") }
    val events = remember { androidx.compose.runtime.mutableStateListOf<AgentEvent>() }

    LaunchedEffect(Unit) {
        vm.events.collect { events.add(it) }
    }

    ScreenSurface("IA — Autonomous Agent", padding) {
        Text(
            "Describe a goal in plain language. The agent plans it, then executes real steps " +
                "(run commands, create files, install systems, manage servers). Example: " +
                "\"Create a Python script that prints hello and run it.\"",
            style = MaterialTheme.typography.bodySmall
        )

        OutlinedTextField(
            value = request,
            onValueChange = { request = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(96.dp),
            label = { Text("Request") },
            singleLine = false
        )
        ActionButton(if (running) "Running…" else "Run agent") {
            if (request.isNotBlank() && !running) vm.run(request)
        }

        if (confirmation != null) {
            AlertDialog(
                onDismissRequest = { vm.confirm(false) },
                title = { Text("Confirm operation") },
                text = { Text(confirmation ?: "") },
                confirmButton = { TextButton({ vm.confirm(true) }) { Text("Allow") } },
                dismissButton = { TextButton({ vm.confirm(false) }) { Text("Deny") } }
            )
        }

        if (events.isNotEmpty()) {
            Text("Progress", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            LazyColumn(Modifier.fillMaxWidth().height(220.dp)) {
                items(events) { event ->
                    Text(
                        when (event) {
                            is AgentEvent.Log -> "· ${event.message}"
                            is AgentEvent.Planning -> "Plan: ${event.thinking.take(140)}"
                            is AgentEvent.StepStart -> "▶ ${event.step.tool}"
                            is AgentEvent.StepDone -> "✓ ${event.step.tool}"
                            is AgentEvent.StepFailed -> "✗ ${event.step.tool} — ${event.result.output.take(120)}"
                            is AgentEvent.Replanning -> "↻ replanning…"
                            is AgentEvent.Done -> "Done: ${event.summary.take(200)}"
                            is AgentEvent.Error -> "Error: ${event.message.take(200)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        Text("Agent log", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text(
            log.ifBlank { "(no activity yet)" },
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        )
    }
}

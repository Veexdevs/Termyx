package com.termx.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.termx.core.model.ApiFormat
import com.termx.core.model.AiProvider
import com.termx.core.security.Validation
import com.termx.ui.components.ActionButton
import com.termx.ui.components.CardSection
import com.termx.ui.components.ScreenSurface
import com.termx.ui.components.SecondaryButton
import java.util.UUID

@Composable
fun AiProvidersScreen(navController: NavHostController, padding: PaddingValues) {
    val vm: AiProvidersViewModel = viewModel()
    val providers by vm.providers.collectAsState()
    val testResult by vm.testResult.collectAsState()

    var editing by remember { mutableStateOf<AiProvider?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    ScreenSurface("AI Providers", padding) {
        Text(
            "Register any OpenAI-compatible (or Anthropic / Gemini) endpoint. Keys are encrypted at " +
                "rest. The selected default provider powers the autonomous agent.",
            style = MaterialTheme.typography.bodySmall
        )

        ActionButton("Add provider") { showAdd = true }

        providers.forEach { p ->
            CardSection(p.name + if (p.isDefault) " (default)" else "") {
                Text("URL: ${p.trimmedBaseUrl}", style = MaterialTheme.typography.bodySmall)
                Text("Model: ${p.model} · Format: ${p.format}", style = MaterialTheme.typography.bodySmall)
                Text("Key: ${if (p.apiKey.isNotBlank()) Validation.redact(p.apiKey) else "(empty)"}", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SecondaryButton("Edit") { editing = p }
                    SecondaryButton("Default") { vm.setDefault(p.id) }
                    SecondaryButton("Test") { vm.test(p) }
                    SecondaryButton("Delete") { vm.remove(p.id) }
                }
            }
        }

        testResult?.let {
            CardSection("Test result") {
                Text(it)
                SecondaryButton("Dismiss") { vm.clearTest() }
            }
        }
    }

    if (showAdd) {
        ProviderDialog(onDismiss = { showAdd = false }, onSave = { vm.upsert(it); showAdd = false })
    }
    editing?.let { p ->
        ProviderDialog(initial = p, onDismiss = { editing = null }, onSave = { vm.upsert(it); editing = null })
    }
}

@Composable
private fun ProviderDialog(
    initial: AiProvider? = null,
    onDismiss: () -> Unit,
    onSave: (AiProvider) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var baseUrl by remember { mutableStateOf(initial?.baseUrl ?: "https://openrouter.ai/api/v1") }
    var apiKey by remember { mutableStateOf(initial?.apiKey ?: "") }
    var model by remember { mutableStateOf(initial?.model ?: "") }
    var format by remember { mutableStateOf(initial?.format ?: ApiFormat.OPENAI) }
    var temperature by remember { mutableStateOf(initial?.temperature?.toString() ?: "0.7") }
    var maxTokens by remember { mutableStateOf(initial?.maxTokens?.toString() ?: "2048") }
    var isDefault by remember { mutableStateOf(initial?.isDefault ?: false) }
    var menuOpen by remember { mutableStateOf(false) }

    val candidate = AiProvider(
        id = initial?.id ?: UUID.randomUUID().toString().take(8),
        name = name, baseUrl = baseUrl, apiKey = apiKey, model = model,
        format = format, temperature = temperature.toDoubleOrNull() ?: 0.7,
        maxTokens = maxTokens.toIntOrNull() ?: 2048, isDefault = isDefault
    )
    val errors = Validation.validateProvider(candidate)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = errors.isEmpty(), onClick = { onSave(candidate) }) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
        title = { Text(if (initial == null) "Add provider" else "Edit provider") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Provider name") })
                OutlinedTextField(baseUrl, { baseUrl = it }, Modifier.fillMaxWidth(), label = { Text("API URL") })
                OutlinedTextField(apiKey, { apiKey = it }, Modifier.fillMaxWidth(), label = { Text("API key") })
                OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), label = { Text("Model ID") })
                Column {
                    OutlinedButton({ menuOpen = true }, Modifier.fillMaxWidth()) { Text("Format: ${format.name}") }
                    DropdownMenu(menuOpen, { menuOpen = false }) {
                        ApiFormat.entries.forEach {
                            DropdownMenuItem(text = { Text(it.name) }, onClick = { format = it; menuOpen = false })
                        }
                    }
                }
                OutlinedTextField(temperature, { temperature = it }, Modifier.fillMaxWidth(), label = { Text("Temperature") })
                OutlinedTextField(maxTokens, { maxTokens = it }, Modifier.fillMaxWidth(), label = { Text("Max tokens") })
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Default provider", Modifier.weight(1f))
                    Switch(checked = isDefault, onCheckedChange = { isDefault = it })
                }
                if (errors.isNotEmpty()) {
                    Text(errors.joinToString("\n"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    )
}

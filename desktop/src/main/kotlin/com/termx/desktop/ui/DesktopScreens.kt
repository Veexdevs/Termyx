package com.termx.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termx.core.ai.AiClient
import com.termx.core.ai.ProviderRepository
import com.termx.core.agent.Agent
import com.termx.core.agent.AgentEvent
import com.termx.core.agent.ToolContext
import com.termx.core.model.ApiFormat
import com.termx.core.model.AiProvider
import com.termx.core.model.LinuxDistribution
import com.termx.core.model.ServerType
import com.termx.core.security.Validation
import com.termx.core.terminal.LocalShellSession
import com.termx.desktop.DesktopContainer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.cancel
import java.io.File
import java.util.UUID

@Composable
fun DesktopScreens(screen: String, container: DesktopContainer) {
    when (screen) {
        "HOME" -> HomeScreenD(container)
        "TERMINAL" -> TerminalScreenD(container)
        "SYSTEMS" -> SystemsScreenD(container)
        "SERVERS" -> ServersScreenD(container)
        "IA" -> IaScreenD(container)
        "FILES" -> FilesScreenD(container)
        "EDITOR" -> EditorScreenD(container)
        "PROVIDERS" -> ProvidersScreenD(container)
    }
}

// ---------------------------------------------------------------- HOME
@Composable
fun HomeScreenD(container: DesktopContainer) {
    val systems = container.linuxManager.list().count { it.installed }
    val servers = container.serverManager.list().size
    val os = System.getProperty("os.name") ?: "unknown"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("HOME", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("OS: $os")
                Text("Java: ${System.getProperty("java.version")}")
                Text("Installed systems: $systems")
                Text("Configured servers: $servers")
                Text("Project root: ${container.fileRoot}")
            }
        }
    }
}

// ---------------------------------------------------------------- TERMINAL
@Composable
fun rememberTerminalSession(shell: List<String>, workingDir: String): TerminalHandle {
    val handle = remember {
        TerminalHandle(LocalShellSession(shell, workingDir = workingDir))
    }
    DisposableEffect(handle) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch { handle.session.output.collect { handle.append(it) } }
        handle.session.start()
        onDispose { handle.session.stop(); scope.cancel() }
    }
    return handle
}

class TerminalHandle(private val s: LocalShellSession) {
    private val _output = mutableStateOf("Starting shell…\r\n")
    val output: androidx.compose.runtime.State<String> get() = _output
    val session: LocalShellSession get() = s
    fun append(chunk: String) { _output.value = (_output.value + chunk).takeLast(200_000) }
    fun send(text: String) = s.write(text)
    fun clear() { _output.value = "" }
}

@Composable
fun TerminalScreenD(container: DesktopContainer) {
    var input by remember { mutableStateOf("") }
    val term = rememberTerminalSession(container.terminalShell, container.fileRoot)
    val output by term.output
    val scroll = rememberScrollState()
    LaunchedEffect(output) { scroll.animateScrollTo(scroll.maxValue) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ term.send("\u0003") }) { Text("Ctrl+C") }
            OutlinedButton({ term.clear() }) { Text("Clear") }
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF0A0A0A)).padding(8.dp)) {
            Text(output, color = Color(0xFFE8E8E8), fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp,
                modifier = Modifier.verticalScroll(scroll))
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            BasicTextField(input, { input = it }, Modifier.weight(1f).padding(8.dp),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp))
            OutlinedButton({
                term.send(input + "\n")
                input = ""
            }) { Text("Send") }
        }
    }
}

// ---------------------------------------------------------------- SYSTEMS
@Composable
fun SystemsScreenD(container: DesktopContainer) {
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("SYSTEMS", style = MaterialTheme.typography.headlineSmall)
        Text("Install Linux distributions via PRoot. Requires proot-distro in the environment; " +
            "otherwise the operation reports the requirement honestly.", style = MaterialTheme.typography.bodySmall)
        status?.let { Text(it) }
        Spacer(Modifier.height(8.dp))
        LinuxDistribution.entries.forEach { distro ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(distro.displayName)
                        Text("${distro.defaultVersion} · ${distro.architectures.joinToString()}", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(enabled = !busy, onClick = {
                        busy = true
                        scope.launch {
                            val r = container.linuxManager.install(distro)
                            status = r.fold(onSuccess = { "Installed ${it.name}" }, onFailure = { it.message })
                            busy = false
                        }
                    }) { Text(if (busy) "…" else "Install") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- SERVERS
@Composable
fun ServersScreenD(container: DesktopContainer) {
    var name by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("python3 -m http.server 8000") }
    var servers by remember { mutableStateOf(container.serverManager.list()) }

    fun refresh() { servers = container.serverManager.list() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("SERVERS", style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, Modifier.weight(1f), label = { Text("Name") })
            OutlinedTextField(command, { command = it }, Modifier.weight(2f), label = { Text("Start command") })
            Button(onClick = {
                if (name.isNotBlank()) {
                    container.serverManager.create(name, ServerType.CUSTOM, command, container.fileRoot)
                    name = ""; refresh()
                }
            }) { Text("Add") }
        }
        Spacer(Modifier.height(8.dp))
        servers.forEach { s ->
            val state = container.serverManager.state(s.id)
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.name)
                        Text("${s.typeLabel} · $state · port ${if (s.port > 0) s.port else "—"}", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = { container.serverManager.start(s.id); refresh() }) { Text("Start") }
                    OutlinedButton(onClick = { container.serverManager.stop(s.id); refresh() }) { Text("Stop") }
                    OutlinedButton(onClick = { container.serverManager.restart(s.id); refresh() }) { Text("Restart") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- IA
@Composable
fun IaScreenD(container: DesktopContainer) {
    var request by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf("") }
    var confirmMsg by remember { mutableStateOf<String?>(null) }
    var confirmDeferred by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    val events = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("/IA — Autonomous Agent", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(request, { request = it }, Modifier.fillMaxWidth().height(90.dp), label = { Text("Request") }, singleLine = false)
        Button(enabled = !running && request.isNotBlank(), onClick = {
            running = true; log = ""; events.clear()
            scope.launch {
                val repo = ProviderRepository(container.keyStore)
                val providers = repo.load()
                val def = providers.firstOrNull { it.isDefault } ?: providers.firstOrNull()
                if (def == null || def.apiKey.isBlank()) {
                    events.add("Error: configure an AI provider with a key first.")
                    running = false; return@launch
                }
                val ctx = ToolContext(
                    workingDirectory = container.fileRoot,
                    fileRoot = container.fileRoot,
                    providers = providers,
                    defaultProvider = def,
                    aiClient = AiClient,
                    serverManager = container.serverManager,
                    linuxManager = container.linuxManager,
                    onLog = { log = (log + it + "\n").takeLast(20000) },
                    confirm = { msg ->
                        confirmDeferred = CompletableDeferred(); confirmMsg = msg
                        confirmDeferred!!.await()
                    }
                )
                Agent(ctx).run(request) { e ->
                    events.add(when (e) {
                        is AgentEvent.StepDone -> "✓ ${e.step.tool}"
                        is AgentEvent.StepFailed -> "✗ ${e.step.tool} — ${e.result.output.take(120)}"
                        is AgentEvent.Done -> "Done: ${e.summary.take(160)}"
                        is AgentEvent.Error -> "Error: ${e.message}"
                        else -> e.toString()
                    })
                }
                running = false
            }
        }) { Text(if (running) "Running…" else "Run agent") }

        Spacer(Modifier.height(8.dp))
        Text("Progress", style = MaterialTheme.typography.titleMedium)
        events.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        Text("Log", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text(log.ifBlank { "(no activity)" }, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }

    if (confirmMsg != null) {
        AlertDialog(
            onDismissRequest = { confirmDeferred?.complete(false); confirmDeferred = null; confirmMsg = null },
            confirmButton = { TextButton({ confirmDeferred?.complete(true); confirmDeferred = null; confirmMsg = null }) { Text("Allow") } },
            dismissButton = { TextButton({ confirmDeferred?.complete(false); confirmDeferred = null; confirmMsg = null }) { Text("Deny") } },
            title = { Text("Confirm operation") }, text = { Text(confirmMsg ?: "") }
        )
    }
}

// ---------------------------------------------------------------- FILES
@Composable
fun FilesScreenD(container: DesktopContainer) {
    var path by remember { mutableStateOf(container.fileRoot) }
    var entries by remember { mutableStateOf(listFiles(path)) }
    var newName by remember { mutableStateOf("") }

    fun reload() { entries = listFiles(path) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("FILES", style = MaterialTheme.typography.headlineSmall)
        Text(path, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ val p = File(path).parent; if (p != null) { path = p; reload() } }) { Text("Up") }
            OutlinedTextField(newName, { newName = it }, Modifier.weight(1f), label = { Text("New file name") })
            Button(onClick = { if (newName.isNotBlank()) { File(path, newName).writeText(""); newName = ""; reload() } }) { Text("New") }
        }
        Spacer(Modifier.height(8.dp))
        entries.forEach { (n, isDir) ->
            Row(Modifier.fillMaxWidth().clickable { if (isDir) { path = File(path, n).absolutePath; reload() } }.padding(6.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text((if (isDir) "📁 " else "📄 ") + n, Modifier.weight(1f))
                OutlinedButton(onClick = { File(path, n).deleteRecursively(); reload() }) { Text("Delete") }
            }
        }
    }
}

private fun listFiles(path: String): List<Pair<String, Boolean>> =
    File(path).listFiles()?.map { it.name to it.isDirectory }
        ?.sortedWith(compareBy({ !it.second }, { it.first.lowercase() })) ?: emptyList()

// ---------------------------------------------------------------- EDITOR
@Composable
fun EditorScreenD(container: DesktopContainer) {
    var path by remember { mutableStateOf(container.fileRoot) }
    var entries by remember { mutableStateOf(listFiles(path)) }
    var selected by remember { mutableStateOf<String?>(null) }
    var content by remember { mutableStateOf("") }
    var runOut by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Text("CODE EDITOR", style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(Modifier.weight(1f).padding(4.dp)) {
                items(entries.filter { !it.second }) { (n, _) ->
                    Text(n, Modifier.fillMaxWidth().clickable {
                        selected = n; content = File(path, n).readText()
                    }.padding(6.dp))
                }
            }
            Column(Modifier.weight(2f).padding(4.dp)) {
                selected?.let { Text("Editing: $it") }
                BasicTextField(
                    content, { content = it }, Modifier.fillMaxSize().padding(8.dp).verticalScroll(rememberScrollState()),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { selected?.let { File(path, it).writeText(content) } }) { Text("Save") }
            Button(onClick = {
                selected?.let { name ->
                    scope.launch {
                        val ext = name.substringAfterLast('.', "")
                        val interp = when (ext) { "py" -> "python3"; "js" -> "node"; "sh","bash" -> "bash"; "kt" -> "kotlin"; else -> null }
                        val cmd = if (interp != null) "$interp \"${File(path, name).absolutePath}\"" else "\"${File(path, name).absolutePath}\""
                        val r = com.termx.core.command.CommandRunner.run(cmd, workingDir = path)
                        runOut = "${r.stdout}${if (r.stderr.isNotBlank()) "\n" + r.stderr else ""}\n[exit ${r.exitCode}]"
                    }
                }
            }) { Text("Run") }
        }
        runOut?.let { Text(it, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
    }
}

// ---------------------------------------------------------------- PROVIDERS
@Composable
fun ProvidersScreenD(container: DesktopContainer) {
    var providers by remember { mutableStateOf(emptyList<AiProvider>()) }
    var testResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { scope.launch { providers = ProviderRepository(container.keyStore).load() } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("AI PROVIDERS", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { showAdd = true }) { Text("Add provider") }
        Spacer(Modifier.height(8.dp))
        providers.forEach { p ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("${p.name}${if (p.isDefault) " (default)" else ""}")
                    Text("${p.trimmedBaseUrl} · ${p.model} · ${p.format}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { scope.launch { ProviderRepository(container.keyStore).setDefault(p.id); providers = ProviderRepository(container.keyStore).load() } }) { Text("Default") }
                        OutlinedButton(onClick = { scope.launch { testResult = runCatching { AiClient.testConnection(p) }.fold(onSuccess = { "Connected ✓" }, onFailure = { "Failed: ${it.message}" }) } }) { Text("Test") }
                        OutlinedButton(onClick = { scope.launch { val list = providers.filter { it.id != p.id }; ProviderRepository(container.keyStore).save(list); providers = list } }) { Text("Delete") }
                    }
                }
            }
        }
        testResult?.let { Text(it) }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var baseUrl by remember { mutableStateOf("https://openrouter.ai/api/v1") }
        var apiKey by remember { mutableStateOf("") }
        var model by remember { mutableStateOf("") }
        var format by remember { mutableStateOf(ApiFormat.OPENAI) }
        var menu by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val np = AiProvider(UUID.randomUUID().toString().take(8), name, baseUrl, apiKey, model, format)
                        val list = providers + np
                        ProviderRepository(container.keyStore).save(list); providers = list; showAdd = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton({ showAdd = false }) { Text("Cancel") } },
            title = { Text("Add provider") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") })
                    OutlinedTextField(baseUrl, { baseUrl = it }, Modifier.fillMaxWidth(), label = { Text("API URL") })
                    OutlinedTextField(apiKey, { apiKey = it }, Modifier.fillMaxWidth(), label = { Text("API key") })
                    OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), label = { Text("Model ID") })
                    OutlinedButton({ menu = true }, Modifier.fillMaxWidth()) { Text("Format: ${format.name}") }
                    DropdownMenu(menu, { menu = false }) {
                        ApiFormat.entries.forEach { DropdownMenuItem(text = { Text(it.name) }, onClick = { format = it; menu = false }) }
                    }
                    val errors = Validation.validateProvider(AiProvider(UUID.randomUUID().toString().take(8), name, baseUrl, apiKey, model, format))
                    if (errors.isNotEmpty()) Text(errors.joinToString("\n"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        )
    }
}

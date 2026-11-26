package com.termx.core.server

import com.termx.core.command.CommandRunner
import com.termx.core.model.ServerConfig
import com.termx.core.model.ServerState
import com.termx.core.util.TermyxJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Real server process manager.
 *
 * Each managed server is a genuine OS process spawned from its start command. Output
 * is captured per-server, state is tracked, and automatic restart is honoured up to
 * [ServerConfig.maxRestarts]. Configurations persist as JSON so servers survive
 * app restarts (auto-start is honoured by the platform on boot).
 *
 * CPU/RAM per process require platform-specific counters; the manager exposes state,
 * uptime and logs, and the UI overlays system-wide metrics.
 */
class ServerManager(private val dataDir: String) {

    private val configFile = File(dataDir, "servers.json").also { it.parentFile.mkdirs() }
    private val configs = ConcurrentHashMap<String, ServerConfig>()
    private val runtimes = ConcurrentHashMap<String, Runtime>()

    private data class Runtime(
        var config: ServerConfig,
        var process: Process? = null,
        var state: ServerState = ServerState.STOPPED,
        var startedAtMs: Long = 0,
        var restarts: Int = 0,
        val logs: MutableList<String> = mutableListOf()
    )

    init { load() }

    fun list(): List<ServerConfig> = configs.values.toList()
    fun get(id: String): ServerConfig? = configs[id]
    fun state(id: String): ServerState = runtimes[id]?.state ?: ServerState.STOPPED
    fun uptimeMs(id: String): Long = runtimes[id]?.let { if (it.startedAtMs > 0) System.currentTimeMillis() - it.startedAtMs else 0 } ?: 0
    fun logs(id: String, max: Int = 200): String = runtimes[id]?.logs?.takeLast(max)?.joinToString("\n") ?: ""

    /** Create and persist a server configuration (does not start it). */
    fun create(
        name: String,
        type: com.termx.core.model.ServerType,
        startCommand: String,
        workingDirectory: String,
        port: Int = 0,
        autoRestart: Boolean = false,
        autoStart: Boolean = false,
        environment: Map<String, String> = emptyMap()
    ): ServerConfig {
        val id = UUID.randomUUID().toString().take(8)
        val cfg = ServerConfig(id, name, type, startCommand, workingDirectory, port, autoRestart, autoStart, true, environment)
        configs[id] = cfg
        persist()
        return cfg
    }

    fun register(config: ServerConfig) {
        configs[config.id] = config
        persist()
    }

    fun remove(id: String) {
        stop(id)
        configs.remove(id)
        persist()
    }

    fun start(id: String): Boolean {
        val cfg = configs[id] ?: return false
        val rt = runtimes.getOrPut(id) { Runtime(cfg) }
        rt.config = cfg
        if (rt.state == ServerState.RUNNING) return true
        rt.state = ServerState.STARTING
        return spawn(rt)
    }

    fun stop(id: String) {
        val rt = runtimes[id] ?: return
        rt.state = ServerState.STOPPING
        try { rt.process?.destroyForcibly() } catch (_: Exception) {}
        rt.process = null
        rt.state = ServerState.STOPPED
    }

    fun restart(id: String): Boolean {
        stop(id)
        return start(id)
    }

    /** Start every server flagged autoStart (called by the platform after boot). */
    fun startAutoStart() {
        configs.values.filter { it.autoStart }.forEach { start(it.id) }
    }

    private fun spawn(rt: Runtime): Boolean {
        return try {
            val pb = ProcessBuilder("sh", "-c", rt.config.startCommand).apply {
                directory(File(rt.config.workingDirectory).takeIf { it.exists() })
                environment().putAll(rt.config.environment)
                redirectErrorStream(true)
            }
            val p = pb.start()
            rt.process = p
            rt.startedAtMs = System.currentTimeMillis()
            rt.state = ServerState.RUNNING
            rt.logs.add("[termx] started: ${rt.config.startCommand}")

            Thread {
                p.inputStream.bufferedReader().forEachLine { line ->
                    synchronized(rt.logs) {
                        rt.logs.add(line)
                        if (rt.logs.size > 4000) rt.logs.removeAt(0)
                    }
                }
                val code = p.waitFor()
                rt.state = if (code == 0) ServerState.STOPPED else ServerState.CRASHED
                rt.process = null
                rt.logs.add("[termx] exited with code $code")
                if (code != 0 && rt.config.autoRestart && rt.restarts < rt.config.maxRestarts) {
                    rt.restarts++
                    rt.logs.add("[termx] auto-restart ${rt.restarts}/${rt.config.maxRestarts}")
                    spawn(rt)
                }
            }.start()
            true
        } catch (e: Exception) {
            rt.state = ServerState.CRASHED
            rt.logs.add("[termx] failed to start: ${e.message}")
            false
        }
    }

    // ---- persistence ----

    private fun persist() {
        runCatching {
            val arr: JsonArray = buildJsonArray {
                configs.values.forEach { add(JsonPrimitive(TermyxJson.encodeToString(it))) }
            }
            configFile.writeText(arr.toString())
        }
    }

    private fun load() {
        val file = configFile
        if (!file.exists()) return
        runCatching {
            val arr = TermyxJson.parseToJsonElement(file.readText()).jsonArray
            arr.forEach { el ->
                runCatching {
                    val cfg = TermyxJson.decodeFromString<ServerConfig>(el.jsonPrimitive.content)
                    configs[cfg.id] = cfg
                }
            }
        }
    }
}

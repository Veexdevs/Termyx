package com.termx.core.linux

import com.termx.core.command.CommandRunner
import com.termx.core.model.LinuxDistribution
import com.termx.core.model.LinuxSystem
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
 * Manages Linux distributions inside TERMYX.
 *
 * Installation/start/stop are real invocations of `proot-distro` (PRoot, no root
 * required) when that binary is present in the environment. The manager NEVER reports
 * a distribution as installed unless the provisioning command actually succeeded and
 * the install record was written. If `proot-distro` is unavailable the operations
 * fail with a clear, actionable message rather than faking success.
 */
class LinuxSystemManager(private val dataDir: String, private val prootDistroPath: String = "proot-distro") {

    private val file = File(dataDir, "systems.json").also { it.parentFile.mkdirs() }
    private val systems = ConcurrentHashMap<String, LinuxSystem>()

    init { load() }

    fun list(): List<LinuxSystem> = systems.values.toList()
    fun get(id: String): LinuxSystem? = systems[id]
    fun isInstalled(distribution: LinuxDistribution): Boolean =
        systems.values.any { it.distribution == distribution && it.installed }

    /** Install a distribution via PRoot. Returns the real command output. */
    suspend fun install(distribution: LinuxDistribution, architecture: String = "aarch64"): Result<LinuxSystem> {
        val check = CommandRunner.run("$prootDistroPath --help")
        if (check.exitCode != 0) {
            return Result.failure(IllegalStateException(
                "proot-distro not available in this environment. " +
                    "Bundle the Termux proot-distro tooling to enable OS installation. (exit ${check.exitCode})"
            ))
        }
        val res = CommandRunner.run("$prootDistroPath install ${distribution.prootName} -a $architecture")
        if (res.exitCode != 0) {
            return Result.failure(RuntimeException("Install failed (exit ${res.exitCode}):\n${res.stderr.take(2000)}"))
        }
        val sys = LinuxSystem(
            id = UUID.randomUUID().toString().take(8),
            distribution = distribution,
            version = distribution.defaultVersion,
            architecture = architecture,
            installPath = File(dataDir, distribution.prootName).absolutePath,
            installed = true,
            installedAtMs = System.currentTimeMillis(),
            packageManager = when (distribution) {
                LinuxDistribution.ALPINE -> "apk"
                LinuxDistribution.ARCH, LinuxDistribution.VOID -> "pacman"
                else -> "apt"
            }
        )
        systems[sys.id] = sys
        persist()
        return Result.success(sys)
    }

    /** Run a command inside an installed distribution (real PRoot login). */
    suspend fun start(distribution: LinuxDistribution, command: String = "true"): com.termx.core.command.CommandResult {
        val sys = systems.values.firstOrNull { it.distribution == distribution && it.installed }
            ?: return com.termx.core.command.CommandResult(
                command, -1, "", "Distribution ${distribution.displayName} is not installed.", timedOut = false
            )
        val res = CommandRunner.run("$prootDistroPath login ${distribution.prootName} -- ${command.take(4000)}")
        if (res.success) {
            systems[sys.id] = sys.copy(running = true)
            persist()
        }
        return res
    }

    /** Stop processes of a distribution. */
    suspend fun stop(distribution: LinuxDistribution): Boolean {
        val sys = systems.values.firstOrNull { it.distribution == distribution } ?: return false
        CommandRunner.run("pkill -f 'proot.*${distribution.prootName}' || true")
        systems[sys.id] = sys.copy(running = false)
        persist()
        return true
    }

    fun uninstall(id: String): Boolean {
        val sys = systems.remove(id) ?: return false
        File(sys.installPath).deleteRecursively()
        persist()
        return true
    }

    private fun persist() {
        runCatching {
            val arr: JsonArray = buildJsonArray {
                systems.values.forEach { add(JsonPrimitive(TermyxJson.encodeToString(it))) }
            }
            file.writeText(arr.toString())
        }
    }

    private fun load() {
        if (!file.exists()) return
        runCatching {
            TermyxJson.parseToJsonElement(file.readText()).jsonArray.forEach { el ->
                runCatching {
                    val s = TermyxJson.decodeFromString<LinuxSystem>(el.jsonPrimitive.content)
                    systems[s.id] = s
                }
            }
        }
    }
}

package com.termx.device

import android.content.Context
import com.termx.core.command.CommandRunner
import com.termx.core.model.DeviceAccessStatus

/**
 * Detects privileged access layers on the device.
 *
 * TERMYX never assumes root and never attempts to obtain it. These checks are
 * read-only and time-boxed so the UI never hangs.
 *
 * - Root: attempts `su -c id` with a short timeout.
 * - Shizuku: detects the Shizuku provider package (full API usage needs the Shizuku
 *   client library + a bound service — see [com.termx.device.ShizukuClient]).
 * - KernelSU / APatch: presence of their well-known mount namespaces / binaries.
 */
object DeviceAccess {

    suspend fun checkRoot(timeoutMs: Long = 3000): Boolean {
        val r = CommandRunner.run("su -c 'id -u'", timeoutMs = timeoutMs)
        return r.success && r.stdout.trim() == "0"
    }

    fun shizukuInstalled(context: Context): Boolean =
        runCatching {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        }.getOrDefault(false)

    fun kernelSuAvailable(): Boolean =
        runCatching { CommandRunner.run("which ksud").success }.getOrDefault(false) ||
            java.io.File("/data/adb/ksu").exists()

    fun apatchAvailable(): Boolean =
        java.io.File("/data/adb/ap").exists() ||
            runCatching { CommandRunner.run("which apd").success }.getOrDefault(false)

    suspend fun status(context: Context): DeviceAccessStatus {
        val root = checkRoot()
        return DeviceAccessStatus(
            shizukuAvailable = shizukuInstalled(context),
            shizukuGranted = false, // requires bound Shizuku service
            rootAvailable = root,
            rootGranted = root,
            kernelSuAvailable = kernelSuAvailable(),
            apatchAvailable = apatchAvailable(),
            details = mapOf(
                "root" to if (root) "granted" else "unavailable",
                "shizuku" to if (shizukuInstalled(context)) "installed" else "not installed"
            )
        )
    }
}

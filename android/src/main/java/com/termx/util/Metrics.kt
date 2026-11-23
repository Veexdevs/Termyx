package com.termx.util

import android.app.Application
import android.os.StatFs
import com.termx.core.model.SystemMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reads real device metrics from /proc and the filesystem.
 * Values that cannot be obtained return -1 so the UI shows "N/A" instead of faking data.
 */
class Metrics(private val app: Application) {
    private var lastCpu: Pair<Long, Long>? = null

    suspend fun snapshot(): SystemMetrics = withContext(Dispatchers.IO) {
        val cpu = cpuUsage()
        val (totalMem, usedMem) = memory()
        val (totalStore, usedStore) = storage()
        val temp = temperature()
        SystemMetrics(
            cpuUsagePercent = cpu,
            totalMemoryBytes = totalMem,
            usedMemoryBytes = usedMem,
            totalStorageBytes = totalStore,
            usedStorageBytes = usedStore,
            temperatureCelsius = temp
        )
    }

    private fun cpuUsage(): Double {
        return try {
            val lines = File("/proc/stat").readLines()
            val parts = lines.firstOrNull { it.startsWith("cpu ") }?.split("\\s+".toRegex()) ?: return -1.0
            val idle = parts.getOrNull(4)?.toLongOrNull() ?: return -1.0
            val total = parts.drop(1).take(7).sumOf { it.toLongOrNull() ?: 0L }
            val prev = lastCpu
            lastCpu = total to idle
            if (prev == null) return -1.0
            val dTotal = (total - prev.first).coerceAtLeast(1)
            val dIdle = (idle - prev.second).coerceAtLeast(0)
            ((dTotal - dIdle).toDouble() / dTotal) * 100.0
        } catch (_: Exception) {
            -1.0
        }
    }

    private fun memory(): Pair<Long, Long> {
        return try {
            val text = File("/proc/meminfo").readText()
            val total = Regex("MemTotal:\\s+(\\d+)").find(text)?.groupValues?.get(1)?.toLongOrNull()?.times(1024) ?: return -1L to -1L
            val avail = Regex("MemAvailable:\\s+(\\d+)").find(text)?.groupValues?.get(1)?.toLongOrNull()?.times(1024) ?: total
            total to (total - avail)
        } catch (_: Exception) {
            -1L to -1L
        }
    }

    private fun storage(): Pair<Long, Long> {
        return try {
            val stat = StatFs(app.filesDir.absolutePath)
            val block = stat.blockSizeLong
            val total = stat.totalBytes
            val free = stat.availableBytes
            total to (total - free)
        } catch (_: Exception) {
            -1L to -1L
        }
    }

    private fun temperature(): Double {
        return try {
            val zoneDir = File("/sys/class/thermal")
            zoneDir.listFiles()?.firstNotNullOfOrNull { f ->
                runCatching {
                    val raw = File(f, "temp").readText().trim().toDoubleOrNull()
                    raw?.div(1000.0)
                }.getOrNull()
            } ?: -1.0
        } catch (_: Exception) {
            -1.0
        }
    }
}

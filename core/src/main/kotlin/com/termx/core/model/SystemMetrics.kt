package com.termx.core.model

/**
 * Snapshot of device/process resource usage for the HOME dashboard and server cards.
 *
 * Fields may be -1 when the platform cannot provide a value (e.g. temperature on
 * devices without a readable thermal sensor). The UI shows "N/A" instead of faking
 * a number.
 */
data class SystemMetrics(
    val timestampMs: Long = System.currentTimeMillis(),
    val cpuUsagePercent: Double = -1.0,
    val totalMemoryBytes: Long = -1,
    val usedMemoryBytes: Long = -1,
    val totalStorageBytes: Long = -1,
    val usedStorageBytes: Long = -1,
    val temperatureCelsius: Double = -1.0,
    val loadAverage: List<Double> = emptyList()
) {
    val memoryUsedPercent: Double
        get() = if (totalMemoryBytes > 0) (usedMemoryBytes.toDouble() / totalMemoryBytes) * 100.0 else -1.0
    val storageUsedPercent: Double
        get() = if (totalStorageBytes > 0) (usedStorageBytes.toDouble() / totalStorageBytes) * 100.0 else -1.0
}

package com.termx.core.model

/**
 * Status of privileged device access layers. Availability is discovered at runtime;
 * TERMYX never assumes a device is rooted and never attempts to gain root on its own.
 */
data class DeviceAccessStatus(
    val shizukuAvailable: Boolean = false,
    val shizukuGranted: Boolean = false,
    val rootAvailable: Boolean = false,
    val rootGranted: Boolean = false,
    val kernelSuAvailable: Boolean = false,
    val apatchAvailable: Boolean = false,
    val details: Map<String, String> = emptyMap()
) {
    val hasAnyPrivilege: Boolean get() = rootGranted || shizukuGranted
    val summary: String
        get() = buildList {
            if (rootGranted) add("Root")
            if (shizukuGranted) add("Shizuku")
            if (kernelSuAvailable) add("KernelSU")
            if (apatchAvailable) add("APatch")
            if (isEmpty()) add("No privileged access")
        }.joinToString(", ")
}

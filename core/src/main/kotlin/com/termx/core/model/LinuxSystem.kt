package com.termx.core.model

import kotlinx.serialization.Serializable

/**
 * A Linux system managed by TERMYX.
 *
 * [installed] is strictly derived from a real provisioning result: a system is
 * reported as installed only after [LinuxSystemManager] has actually created it.
 */
@Serializable
data class LinuxSystem(
    val id: String,
    val distribution: LinuxDistribution,
    val version: String,
    val architecture: String,
    val installPath: String,
    val installed: Boolean = false,
    val running: Boolean = false,
    val installedAtMs: Long = 0,
    val packageManager: String = "apt",
    val occupiedBytes: Long = 0,
    val usedMemoryBytes: Long = 0
) {
    val name: String get() = "${distribution.displayName} $version"
    val state: String
        get() = when {
            !installed -> "Not installed"
            running -> "Running"
            else -> "Installed"
        }
}

package com.termx.core.model

/**
 * Linux distributions installable through TERMYX.
 *
 * The [prootName] is the identifier understood by `proot-distro` when available;
 * [defaultVersion] is shown in the UI before install. Installation is only marked
 * complete after a real, successful provisioning run (see [LinuxSystemManager]).
 */
enum class LinuxDistribution(
    val displayName: String,
    val prootName: String,
    val defaultVersion: String,
    val architectures: List<String> = listOf("aarch64", "arm", "x86_64")
) {
    UBUNTU("Ubuntu", "ubuntu", "24.04"),
    DEBIAN("Debian", "debian", "12"),
    ARCH("Arch Linux", "archlinux", "rolling"),
    ALPINE("Alpine Linux", "alpine", "3.20", listOf("aarch64", "arm", "x86_64")),
    FEDORA("Fedora", "fedora", "40"),
    KALI("Kali Linux", "kali", "2024.3"),
    VOID("Void Linux", "void", "rolling"),
    OPENSUSE("openSUSE", "opensuse", "15.6");

    companion object {
        fun fromProotName(name: String): LinuxDistribution? =
            entries.firstOrNull { it.prootName.equals(name, ignoreCase = true) }
    }
}

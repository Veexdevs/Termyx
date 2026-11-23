package com.termx.desktop

import com.termx.core.linux.LinuxSystemManager
import com.termx.core.server.ServerManager
import java.io.File

/**
 * Desktop counterpart of the Android [com.termx.AppContainer]. Owns the same core engines
 * so behaviour is shared across platforms.
 */
class DesktopContainer {
    private val home = System.getProperty("user.home")
    private val dataDir = File(home, ".termx").also { it.mkdirs() }

    val fileRoot: String = File(dataDir, "projects").also { it.mkdirs() }.absolutePath
    val downloadsDir: String = File(dataDir, "downloads").also { it.mkdirs() }.absolutePath

    val serverManager = ServerManager(File(dataDir, "servers").absolutePath)
    val linuxManager = LinuxSystemManager(File(dataDir, "systems").absolutePath)
    val keyStore = DesktopKeyStore(File(dataDir, "secure.json"))

    /** Host shell for the desktop terminal. */
    val terminalShell: List<String> = com.termx.core.util.Platform.interactiveShell()
}

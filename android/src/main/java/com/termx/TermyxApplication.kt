package com.termx

import android.app.Application
import android.content.Context
import com.termx.core.linux.LinuxSystemManager
import com.termx.core.server.ServerManager
import com.termx.security.EncryptedKeyStore
import java.io.File

/** Convenience accessor for the shared [AppContainer]. */
val Context.termxContainer: AppContainer
    get() = (this.applicationContext as TermyxApplication).container

/**
 * Application entry point. Owns the shared, real engines from the [core] module
 * (server/process manager, Linux system manager, encrypted key store) so every
 * screen works against the same instances.
 */
class TermyxApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Dependency container shared across the app. */
class AppContainer(app: Application) {
    val dataDir: String = app.filesDir.absolutePath
    val fileRoot: String = File(app.filesDir, "projects").also { it.mkdirs() }.absolutePath
    val downloadsDir: String = File(app.filesDir, "downloads").also { it.mkdirs() }.absolutePath

    val serverManager: ServerManager = ServerManager(File(app.filesDir, "servers").absolutePath)
    val linuxManager: LinuxSystemManager = LinuxSystemManager(File(app.filesDir, "systems").absolutePath)
    val keyStore: EncryptedKeyStore = EncryptedKeyStore(app)
    val metrics: Metrics = Metrics(app)

    /** Shell used by the terminal. Android devices expose /system/bin/sh. */
    val terminalShell: List<String> = listOf("/system/bin/sh")
}

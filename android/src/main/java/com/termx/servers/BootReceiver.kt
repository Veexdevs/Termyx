package com.termx.servers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.termx.TermyxApplication

/**
 * Re-launches the server foreground service after a reboot, but only when the user
 * has at least one server with auto-start enabled.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? TermyxApplication ?: return
        val hasAuto = app.container.serverManager.list().any { it.autoStart }
        if (hasAuto) {
            val serviceIntent = Intent(context, ServerService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}

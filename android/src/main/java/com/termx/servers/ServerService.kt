package com.termx.servers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.termx.TermyxApplication

/**
 * Foreground service that keeps managed servers alive (24/7 best-effort).
 *
 * Honours Android limits: it requests a data-sync foreground service type, shows a
 * persistent notification, and starts only the servers the user flagged auto-start.
 * It does NOT promise uninterrupted execution — Doze, OEM killers and battery limits
 * may still stop processes; the manager restarts them when it can.
 */
class ServerService : Service() {

    companion object {
        const val CHANNEL_ID = "termx_servers"
        const val NOTIF_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        val container = (application as TermyxApplication).container
        container.serverManager.startAutoStart()
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "TERMYX Servers",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Keeps background servers alive" }
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TERMYX Servers")
            .setContentText("Background servers are running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
}

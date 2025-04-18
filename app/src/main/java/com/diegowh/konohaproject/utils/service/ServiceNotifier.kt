package com.diegowh.konohaproject.utils.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

class ServiceNotifier(private val context: Context) {

    fun buildNotification(): Notification {
        createNotificationChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Contador activo")
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Contador en curso",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Notificación para el contador en segundo plano"
            setShowBadge(false)
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "countdown_channel"
        const val NOTIFICATION_ID = 101
    }
}
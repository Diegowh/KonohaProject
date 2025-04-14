package com.example.konohaproject.controller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import androidx.core.app.NotificationCompat
import java.util.Locale

class ServiceNotifier(private val context: Context) {

    private fun buildNotification(): Notification {
        createNotificationChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Contador activo")
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }


    fun updateNotification() {
        val notification = buildNotification()
        (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Contador regresivo",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Canal para contador regresivo" }

        (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000 % 60).toInt()
        val minutes = (millis / (1000 * 60) % 60).toInt()
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    companion object {
        const val CHANNEL_ID = "countdown_channel"
        const val NOTIFICATION_ID = 101
    }
}
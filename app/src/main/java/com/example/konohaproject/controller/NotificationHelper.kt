package com.example.konohaproject.controller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    fun buildDefaultNotification(): Notification {
        createNotificationChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Contador en ejecución")
            .setContentText("El contador está activo en segundo plano")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    fun updateNotification(context: Context, remainingTime: Long) {
        val notification = buildNotificationWithTime(context, remainingTime)
        (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotificationWithTime(context: Context, millis: Long): Notification {
        val formattedTime = formatTime(millis)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Tiempo restante")
            .setContentText(formattedTime)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000 % 60).toInt()
        val minutes = (millis / (1000 * 60) % 60).toInt()
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Contador regresivo",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Canal para contador regresivo" }

            (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "countdown_channel"
        const val NOTIFICATION_ID = 101
    }
}
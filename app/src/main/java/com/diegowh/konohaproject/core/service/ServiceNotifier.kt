package com.diegowh.konohaproject.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.core.timer.IntervalType
import com.diegowh.konohaproject.ui.main.MainActivity

class ServiceNotifier(private val context: Context) {

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun buildNotification(): Notification {
        createNotificationChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Contador activo")
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    fun sendIntervalFinishedNotification(intervalType: IntervalType) {
        createIntervalNotificationChannel()
        
        val title = when (intervalType) {
            IntervalType.FOCUS -> "Focus time finished. Take a break!"
            else -> "Break finished. Back to focus!"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = "OPEN_FROM_INTERVAL_NOTIFICATION"
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, INTERVAL_CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_play)
            .setColor(Color.GREEN)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
            .build()
            
        notificationManager.notify(INTERVAL_NOTIFICATION_ID, notification)
    }
    
    fun sendSessionFinishedNotification() {
        createIntervalNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = "OPEN_FROM_SESSION_NOTIFICATION"
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 2, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, INTERVAL_CHANNEL_ID)
            .setContentTitle("Session Completed!")
            .setContentText("Great job! Your timer session is complete.")
            .setSmallIcon(R.drawable.ic_stop)
            .setColor(Color.RED)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
            .build()
            
        notificationManager.notify(INTERVAL_NOTIFICATION_ID, notification)
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
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createIntervalNotificationChannel() {
        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.interval_finished}")

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
            
        val channel = NotificationChannel(
            INTERVAL_CHANNEL_ID,
            "Interval Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for completed intervals"
            setShowBadge(true)
            enableVibration(true)
            enableLights(true)
            vibrationPattern = longArrayOf(0, 250, 250, 250)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC

            setSound(soundUri, audioAttributes)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "countdown_channel"
        const val NOTIFICATION_ID = 101
        
        const val INTERVAL_CHANNEL_ID = "interval_notification_channel"
        const val INTERVAL_NOTIFICATION_ID = 102
    }
}
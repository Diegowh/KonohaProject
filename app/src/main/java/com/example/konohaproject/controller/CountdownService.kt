package com.example.konohaproject.controller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CountdownService : Service() {


    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var countDownJob: Job? = null
    private var endTime: Long = 0L

    companion object {
        const val TAG = "CountdownService"
        const val CHANNEL_ID = "countdown_channel"
        const val NOTIFICATION_ID = 101
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startCountdown(intent?.getLongExtra("duration", 0L) ?: 0L)
        return START_STICKY
    }

    private fun startCountdown(durationMillis: Long) {

        endTime = SystemClock.elapsedRealtime() + durationMillis
        countDownJob?.cancel();

        countDownJob = serviceScope.launch {
            while (SystemClock.elapsedRealtime() < endTime) {
                val remaining = endTime - SystemClock.elapsedRealtime()
                logRemainingTime(remaining)
                delay(1000)
            }
            stopSelf()
        }
    }

    private fun logRemainingTime(remaining: Long) {
        val minutes = remaining / 1000 / 60
        val seconds = remaining / 1000 % 60
        Log.d(TAG, "Tiempo restante:  ${minutes}m ${seconds}s");
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Contador regresivo",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel);
        }
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Contador en ejecucion")
            .setContentText("El contador esta activo en segundo plano")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        countDownJob?.cancel()
        super.onDestroy()
    }

}
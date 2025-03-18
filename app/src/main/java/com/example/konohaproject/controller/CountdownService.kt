package com.example.konohaproject.controller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
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

    interface TimeUpdateListener {
        fun onTimeUpdate(remainingTime: Long)
    }

    var timeListener: TimeUpdateListener? = null

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var countDownJob: Job? = null
    private var endTime: Long = 0L

    inner class CountdownBinder : Binder() {
        fun getService(): CountdownService = this@CountdownService
    }

    companion object {
        @Volatile
        private var isRunning: Boolean = false

        @Volatile
        private var isPaused: Boolean = false

        @Volatile
        private var remainingTime: Long = 0L

        fun isCountDownActive() = isRunning && !isPaused
        fun isPaused() = isPaused

        internal fun setRunning(running: Boolean) {
            isRunning = running
            if (!running) {
                isPaused = false
                remainingTime = 0L
            }
        }

        internal fun setPaused(paused: Boolean, time: Long = 0L) {
            isPaused = paused
            if (paused) remainingTime = time
        }

        const val TAG = "CountdownService"
        const val CHANNEL_ID = "countdown_channel"
        const val NOTIFICATION_ID = 101
    }

    override fun onBind(intent: Intent?): IBinder? = CountdownBinder()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Contador en ejecución")
            .setContentText("El contador está activo en segundo plano")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)  // Notificación persistente
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!isRunning) {
            setRunning(true)
            val duration = intent?.getLongExtra("duration", 0L) ?: 0L
            startCountdown(duration)

        }
        return START_STICKY
    }

    private fun startCountdown(durationMillis: Long) {
        countDownJob?.cancel();
        endTime = SystemClock.elapsedRealtime() + durationMillis

        countDownJob = serviceScope.launch {
            setPaused(false)
            while (SystemClock.elapsedRealtime() < endTime) {
                if (isPaused){
                    setPaused(true, endTime - SystemClock.elapsedRealtime())
                    delay(100)
                    continue
                }
                logRemainingTime(endTime - SystemClock.elapsedRealtime())
                delay(1000)

            }
            if (!isPaused) {
                stopSelf()
            }
        }
    }

    fun pauseCountdown() {
        if (isRunning && !isPaused) {
            setPaused(true, endTime - SystemClock.elapsedRealtime())
            countDownJob?.cancel()
        }
    }

    fun resumeCountdown() {
        if (isRunning && isPaused) {
            setPaused(false)
            endTime = SystemClock.elapsedRealtime() + remainingTime
            startCountdown(remainingTime)
        }
    }

    private fun logRemainingTime(remaining: Long) {
        val minutes = remaining / 1000 / 60
        val seconds = remaining / 1000 % 60
        timeListener?.onTimeUpdate(remaining)
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
        super.onDestroy()
        setRunning(false)
        countDownJob?.cancel()
        endTime = 0L
        remainingTime = 0L
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    public fun isPaused(): Boolean {
        return isPaused
    }

    fun fullReset() {
        hardReset()
        remainingTime = 25 * 60 * 1000L
    }

    fun hardReset() {
        countDownJob?.cancel()
        endTime = 0L
        remainingTime = 0L
        setRunning(false)
        setPaused(false)
    }

    fun getCurrentRemainingTime(): Long {
        return if (isRunning) {
            if (isPaused) remainingTime else endTime - SystemClock.elapsedRealtime()
        } else {
            0L
        }
    }
}
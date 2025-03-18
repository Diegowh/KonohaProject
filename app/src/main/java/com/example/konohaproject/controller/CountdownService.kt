package com.example.konohaproject.controller

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class CountdownService : Service(), CountdownController, CountdownTimer.Listener {


    private val binder = CountdownBinder()
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var countdownTimer: CountdownTimer
    private var timeListener: TimeUpdateListener? = null

    private val serviceScope = CoroutineScope(Dispatchers.Default)

    inner class CountdownBinder: Binder() {
        fun getController(): CountdownController = this@CountdownService
    }



    interface TimeUpdateListener {
        fun onTimeUpdate(remainingTime: Long)
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        countdownTimer = CountdownTimer(serviceScope, this)
        startForegroundService()
    }

    private fun startForegroundService() {
        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            notificationHelper.buildDefaultNotification()
        )
    }
    override fun onBind(intent: Intent?): IBinder? = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            else -> intent?.getLongExtra(EXTRA_DURATION, 0L)?.let {
                if (!countdownTimer.isRunning()) countdownTimer.start(it)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        countdownTimer.reset()
        super.onDestroy()
    }

    override fun onTimeUpdate(remainingTime: Long) {
        timeListener?.onTimeUpdate(remainingTime)
        notificationHelper.updateNotification(this, remainingTime)
    }

    override fun onCountdownFinished() {
        stopSelf()
    }

    override fun start(durationMillis: Long) = countdownTimer.start(durationMillis)
    override fun pause() = countdownTimer.pause()
    override fun resume() = countdownTimer.resume()
    override fun reset() = countdownTimer.reset()
    override fun getRemainingTime(): Long = countdownTimer.getRemainingTime()
    override fun isPaused(): Boolean = countdownTimer.isPaused()
    override fun isRunning(): Boolean = countdownTimer.isRunning()
    override fun setTimeUpdateListener(listener: TimeUpdateListener?) {
        timeListener = listener
    }

    companion object {
        const val ACTION_STOP = "STOP"
        const val EXTRA_DURATION = "duration"
    }
}
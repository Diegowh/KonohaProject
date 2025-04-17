package com.diegowh.konohaproject.domain.timer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.diegowh.konohaproject.utils.ServiceNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class TimerService : Service(), TimerController {

    private val binder = TimerBinder()
    private lateinit var serviceNotifier: ServiceNotifier
    private lateinit var intervalManager: IntervalManager

    private val serviceScope = CoroutineScope(Dispatchers.Default)

    inner class TimerBinder: Binder() {
        fun getController(): TimerController = this@TimerService
    }

    override fun onCreate() {
        super.onCreate()
        serviceNotifier = ServiceNotifier(this)
        intervalManager = IntervalManager(this, serviceScope)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(ServiceNotifier.NOTIFICATION_ID, serviceNotifier.buildNotification())
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            else -> intent?.getLongExtra(EXTRA_DURATION, 0L)?.let {
                if (!intervalManager.isRunning()) intervalManager.start(it)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        intervalManager.reset()
        super.onDestroy()
    }

    override fun start(durationMillis: Long) = intervalManager.start(durationMillis)
    override fun pause() = intervalManager.pause()
    override fun resume() = intervalManager.resume()
    override fun reset() = intervalManager.reset()
    override fun getRemainingTime(): Long = intervalManager.getRemainingTime()
    override fun isPaused(): Boolean = intervalManager.isPaused()
    override fun isRunning(): Boolean = intervalManager.isRunning()
    override fun getCurrentRound(): Int = intervalManager.getCurrentRound()
    override fun isFocusInterval(): Boolean = intervalManager.isFocusInterval()

    fun getTimerEvents() = intervalManager.eventFlow

    companion object {
        const val ACTION_STOP = "STOP"
        const val EXTRA_DURATION = "duration"
    }
}
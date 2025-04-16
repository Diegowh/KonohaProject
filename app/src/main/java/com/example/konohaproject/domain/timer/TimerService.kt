package com.example.konohaproject.domain.timer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.example.konohaproject.utils.ServiceNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class TimerService : Service(), TimerController {

    private val binder = TimerBinder()
    private lateinit var serviceNotifier: ServiceNotifier
    private lateinit var timerManager: TimerManager
    private var timeListener: TimeUpdateListener? = null

    private val serviceScope = CoroutineScope(Dispatchers.Default)

    inner class TimerBinder: Binder() {
        fun getController(): TimerController = this@TimerService
    }

    override fun onCreate() {
        super.onCreate()
        serviceNotifier = ServiceNotifier(this)
        timerManager = TimerManager(this, serviceScope, object : TimeUpdateListener {
            override fun onTimeUpdate(remainingTime: Long) {
                timeListener?.onTimeUpdate(remainingTime)
            }

            override fun onTimerFinished(currentRound: Int, isFocus: Boolean) {
                timeListener?.onTimerFinished(currentRound, isFocus)
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(ServiceNotifier.NOTIFICATION_ID, serviceNotifier.buildNotification())
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            else -> intent?.getLongExtra(EXTRA_DURATION, 0L)?.let {
                if (!timerManager.isRunning()) timerManager.start(it)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        timerManager.reset()
        super.onDestroy()
    }

    override fun start(durationMillis: Long) = timerManager.start(durationMillis)
    override fun pause() = timerManager.pause()
    override fun resume() = timerManager.resume()
    override fun reset() = timerManager.reset()
    override fun getRemainingTime(): Long = timerManager.getRemainingTime()
    override fun isPaused(): Boolean = timerManager.isPaused()
    override fun isRunning(): Boolean = timerManager.isRunning()
    override fun setTimeUpdateListener(listener: TimeUpdateListener?) {
        timeListener = listener
    }

    override fun getCurrentCycle(): Int = timerManager.getCurrentCycle()
    override fun isFocusSession(): Boolean = timerManager.isFocusSession()

    companion object {
        const val ACTION_STOP = "STOP"
        const val EXTRA_DURATION = "duration"
    }
}
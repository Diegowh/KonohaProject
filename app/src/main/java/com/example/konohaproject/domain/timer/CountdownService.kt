package com.example.konohaproject.domain.timer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.example.konohaproject.utils.ServiceNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class CountdownService : Service(), CountdownController {

    private val binder = CountdownBinder()
    private lateinit var serviceNotifier: ServiceNotifier
    private lateinit var countdownManager: CountdownManager
    private var timeListener: CountdownController.TimeUpdateListener? = null

    private val serviceScope = CoroutineScope(Dispatchers.Default)

    inner class CountdownBinder: Binder() {
        fun getController(): CountdownController = this@CountdownService
    }

    override fun onCreate() {
        super.onCreate()
        serviceNotifier = ServiceNotifier(this)
        countdownManager = CountdownManager(this, serviceScope, object : CountdownController.TimeUpdateListener {
            override fun onTimeUpdate(remainingTime: Long) {
                timeListener?.onTimeUpdate(remainingTime)
            }

            override fun onCountdownFinished(currentRound: Int, isFocus: Boolean) {
                timeListener?.onCountdownFinished(currentRound, isFocus)
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(ServiceNotifier.NOTIFICATION_ID, serviceNotifier.buildNotification())
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            else -> intent?.getLongExtra(EXTRA_DURATION, 0L)?.let {
                if (!countdownManager.isRunning()) countdownManager.start(it)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        countdownManager.reset()
        super.onDestroy()
    }

    override fun start(durationMillis: Long) = countdownManager.start(durationMillis)
    override fun pause() = countdownManager.pause()
    override fun resume() = countdownManager.resume()
    override fun reset() = countdownManager.reset()
    override fun getRemainingTime(): Long = countdownManager.getRemainingTime()
    override fun isPaused(): Boolean = countdownManager.isPaused()
    override fun isRunning(): Boolean = countdownManager.isRunning()
    override fun setTimeUpdateListener(listener: CountdownController.TimeUpdateListener?) {
        timeListener = listener
    }

    override fun getCurrentCycle(): Int = countdownManager.getCurrentCycle()
    override fun isFocusSession(): Boolean = countdownManager.isFocusSession()

    companion object {
        const val ACTION_STOP = "STOP"
        const val EXTRA_DURATION = "duration"
    }
}
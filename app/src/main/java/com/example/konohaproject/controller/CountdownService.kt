package com.example.konohaproject.controller

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class CountdownService : Service(), CountdownController, CountdownTimer.Listener {


    private var currentCycle = 0
    private var isFocusSession = true
    private var totalCycles = TimeConfig.getTotalCycles()

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
        fun onCountdownFinished(currentCycle: Int, isFocus: Boolean)
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

    override fun onTimeUpdate(remaining: Long) {
        timeListener?.onTimeUpdate(remaining)
        notificationHelper.updateNotification(this, remaining)
    }

    override fun start(durationMillis: Long) {
//        countdownTimer.pause()
        if (currentCycle == 0) {
            currentCycle++
            timeListener?.onCountdownFinished(currentCycle, true)
        }
        countdownTimer.reset()
        countdownTimer.start(durationMillis)
    }

    override fun pause() = countdownTimer.pause()
    override fun resume() = countdownTimer.resume()
    override fun reset() {
        currentCycle = 0
        countdownTimer.reset()
    }
    override fun getRemainingTime(): Long = countdownTimer.getRemainingTime()
    override fun isPaused(): Boolean = countdownTimer.isPaused()
    override fun isRunning(): Boolean = countdownTimer.isRunning()
    override fun setTimeUpdateListener(listener: TimeUpdateListener?) {
        timeListener = listener
    }
    override fun getCurrentCycle() = currentCycle
    override fun isFocusSession() = isFocusSession

    override fun onCountdownFinished() {

        if (isFocusSession) {
            // Estamos en sesion de Focus, por lo que hay que pasar a sesión de Break independientemente del ciclo.
            isFocusSession = false
            start(TimeConfig.breakTimeMillis())

        } else if (currentCycle < totalCycles) {
            // Es sesion de Break pero quedan ciclos antes del tope.
            isFocusSession = true
            currentCycle++
            start(TimeConfig.focusTimeMillis())
        } else {
            if (!TimeConfig.isAutoRestartEnabled()) {
                // Es sesion de Break pero estamos en el ultimo ciclo, por lo que hay que resetear ciclo y parar timer
                isFocusSession = true
                currentCycle = 0
                reset()
            } else {
                isFocusSession = true
                currentCycle = 1
                start(TimeConfig.focusTimeMillis())
            }

        }
        timeListener?.onCountdownFinished(currentCycle, isFocusSession)

    }

//    override fun onCountdownFinished() {
//        moveToNextSession()
//    }

    companion object {
        const val ACTION_STOP = "STOP"
        const val EXTRA_DURATION = "duration"
    }
}
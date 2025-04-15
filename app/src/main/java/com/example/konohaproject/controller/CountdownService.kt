package com.example.konohaproject.controller

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.example.konohaproject.model.TimeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class CountdownService : Service(), CountdownController, CountdownTimer.Listener {


    private var currentCycle = 0
    private var isFocusSession = true

    private val binder = CountdownBinder()
    private lateinit var serviceNotifier: ServiceNotifier
    private lateinit var countdownTimer: CountdownTimer
    private var timeListener: TimeUpdateListener? = null

    private val serviceScope = CoroutineScope(Dispatchers.Default)

    inner class CountdownBinder: Binder() {
        fun getController(): CountdownController = this@CountdownService
    }

    interface TimeUpdateListener {
        fun onTimeUpdate(remainingTime: Long)
        fun onCountdownFinished(currentRound: Int, isFocus: Boolean)
    }

    override fun onCreate() {
        super.onCreate()
        serviceNotifier = ServiceNotifier(this)
        countdownTimer = CountdownTimer(serviceScope, this)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(ServiceNotifier.NOTIFICATION_ID, serviceNotifier.buildNotification())
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            else -> intent?.getLongExtra(EXTRA_DURATION, 0L)?.let {
                if (!countdownTimer.isRunning()) countdownTimer.start(it)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        countdownTimer.reset()
        super.onDestroy()
    }

    override fun onTimeUpdate(remaining: Long) {
        timeListener?.onTimeUpdate(remaining)
    }

    override fun start(durationMillis: Long) {
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
         val totalCycles = TimeConfig.getTotalRounds(applicationContext)

        if (isFocusSession) {
            // Estamos en sesion de Focus, por lo que hay que pasar a sesión de Break independientemente del ciclo.
            isFocusSession = false
            val breakDuration = if (currentCycle == totalCycles) {
                TimeConfig.longBreakTimeMillis(applicationContext)
            } else {
                TimeConfig.shortBreakTimeMillis(applicationContext)
            }
            start(breakDuration)

        } else {
            // Transicion de Break a Focus
            isFocusSession = true
            if (currentCycle < totalCycles) {
                currentCycle++
            } else {
                currentCycle = if (TimeConfig.isAutoRestartEnabled(applicationContext)) 1 else 0
            }
            start(TimeConfig.focusTimeMillis(applicationContext))
        }

        // Notifica al MaiNActivity para actualizar la UI
        timeListener?.onCountdownFinished(currentCycle, isFocusSession)

    }

    companion object {
        const val ACTION_STOP = "STOP"
        const val EXTRA_DURATION = "duration"
    }
}
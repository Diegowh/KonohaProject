package com.diegowh.konohaproject.feature.timer.data.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.diegowh.konohaproject.core.app.App
import com.diegowh.konohaproject.feature.timer.domain.repository.TimerSettingsRepository
import com.diegowh.konohaproject.core.notification.ServiceNotifier
import com.diegowh.konohaproject.feature.timer.domain.repository.TimerController
import com.diegowh.konohaproject.feature.timer.domain.service.SessionManager
import com.diegowh.konohaproject.feature.timer.domain.service.TimerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class TimerService : Service(), TimerController {

    private val binder = TimerBinder()
    private lateinit var serviceNotifier: ServiceNotifier
    private lateinit var sessionManager: SessionManager

    private val settingsProvider: TimerSettingsRepository
        get() = (application as App).timerSettings

    private val serviceScope = CoroutineScope(Dispatchers.Default)

    inner class TimerBinder : Binder() {
        fun getController(): TimerController = this@TimerService
    }

    override fun onCreate() {
        super.onCreate()
        serviceNotifier = ServiceNotifier(this)
        val engine = TimerEngine(serviceScope)

        sessionManager = SessionManager(engine, settingsProvider, serviceScope)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(ServiceNotifier.NOTIFICATION_ID, serviceNotifier.buildNotification())
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            else -> intent?.getLongExtra(EXTRA_DURATION, 0L)?.let {
                if (!sessionManager.isRunning()) sessionManager.start(it)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        sessionManager.reset()
        super.onDestroy()
    }

    override fun start(durationMillis: Long) = sessionManager.start(durationMillis)
    override fun pause() = sessionManager.pause()
    override fun resume() = sessionManager.resume()
    override fun reset() = sessionManager.reset()
    override fun getRemainingTime(): Long = sessionManager.getRemainingTime()
    override fun isPaused(): Boolean = sessionManager.isPaused()
    override fun isRunning(): Boolean = sessionManager.isRunning()
    override fun getCurrentRound(): Int = sessionManager.getCurrentRound()
    override fun skip() = sessionManager.skipInterval()

    fun getTimerEvents() = sessionManager.eventFlow

    companion object {
        const val ACTION_STOP = "STOP"
        const val EXTRA_DURATION = "duration"
    }
}
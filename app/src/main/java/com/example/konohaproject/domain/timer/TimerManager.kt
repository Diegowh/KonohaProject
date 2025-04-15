package com.example.konohaproject.domain.timer

import android.content.Context
import kotlinx.coroutines.CoroutineScope

class TimerManager (
    private val context: Context,
    scope: CoroutineScope,
    private val listener: TimeUpdateListener
) : TimerEngine.Listener {

    private val timer = TimerEngine(scope, this)
    private var currentCycle = 0
    private var isFocusSession = false

    override fun onTimeUpdate(remaining: Long) {
        listener.onTimeUpdate(remaining)
    }

    override fun onCountdownFinished() {
        val totalRounds = TimerSettings.getTotalRounds(context)
        if (isFocusSession) {
            isFocusSession = false
            val breakDuration = if (currentCycle == totalRounds) {
                TimerSettings.longBreakTimeMillis(context)
            } else {
                TimerSettings.shortBreakTimeMillis(context)
            }
            start(breakDuration)
        } else {
            isFocusSession = true
            val focusDuration = TimerSettings.focusTimeMillis(context)
            val isAutorun = TimerSettings.isAutorunEnabled(context)
            val isLastRound = currentCycle >= totalRounds

            when {
                !isLastRound -> {
                    currentCycle++
                    start(focusDuration)
                }
                isAutorun -> {
                    currentCycle = 1
                    start(focusDuration)
                }
                else -> {
                    reset()
                }
            }
        }
        listener.onTimerFinished(currentCycle, isFocusSession)
    }

    fun start(durationMillis: Long) {
        if (currentCycle == 0) {
            currentCycle++
            listener.onTimerFinished(currentCycle, true)
        }
        timer.reset()
        timer.start(durationMillis)
    }

    fun pause() = timer.pause()
    fun resume() = timer.resume()
    fun reset() {
        currentCycle = 0
        timer.reset()
    }

    fun getRemainingTime(): Long = timer.getRemainingTime()
    fun isPaused(): Boolean = timer.isPaused()
    fun isRunning(): Boolean = timer.isRunning()
    fun getCurrentCycle(): Int = currentCycle
    fun isFocusSession(): Boolean = isFocusSession
}
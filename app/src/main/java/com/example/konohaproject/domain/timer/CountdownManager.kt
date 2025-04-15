package com.example.konohaproject.domain.timer

import android.content.Context
import kotlinx.coroutines.CoroutineScope

class CountdownManager (
    private val context: Context,
    private val scope: CoroutineScope,
    private val listener: CountdownController.TimeUpdateListener
) : CountdownTimer.Listener {

    private val timer = CountdownTimer(scope, this)
    private var currentCycle = 0
    private var isFocusSession = false

    override fun onTimeUpdate(remaining: Long) {
        listener.onTimeUpdate(remaining)
    }

    override fun onCountdownFinished() {
        val totalRounds = TimeConfig.getTotalRounds(context)
        if (isFocusSession) {
            isFocusSession = false
            val breakDuration = if (currentCycle == totalRounds) {
                TimeConfig.longBreakTimeMillis(context)
            } else {
                TimeConfig.shortBreakTimeMillis(context)
            }
            start(breakDuration)
        } else {
            isFocusSession = true
            val focusDuration = TimeConfig.focusTimeMillis(context)
            val isAutorun = TimeConfig.isAutorunEnabled(context)
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
        listener.onCountdownFinished(currentCycle, isFocusSession)
    }

    fun start(durationMillis: Long) {
        if (currentCycle == 0) {
            currentCycle++
            listener.onCountdownFinished(currentCycle, true)
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
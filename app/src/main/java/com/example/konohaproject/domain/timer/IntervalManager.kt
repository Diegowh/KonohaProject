package com.example.konohaproject.domain.timer

import android.content.Context
import kotlinx.coroutines.CoroutineScope

class IntervalManager (
    private val context: Context,
    scope: CoroutineScope,
    private val listener: TimeUpdateListener
) : TimerEngine.Listener {

    private val timer = TimerEngine(scope, this)
    private var currentRound = 0
    private var isFocusInterval = false

    override fun onTimeUpdate(remaining: Long) {
        listener.onTimeUpdate(remaining)
    }

    override fun onIntervalFinished() {
        val intervalDuration: Long
        val totalRounds = TimerSettings.getTotalRounds(context)
        if (isFocusInterval) {
            isFocusInterval = false
            intervalDuration = if (currentRound == totalRounds) {
                TimerSettings.longBreakTimeMillis(context)
            } else {
                TimerSettings.shortBreakTimeMillis(context)
            }
            start(intervalDuration)
        } else {
            isFocusInterval = true
            intervalDuration = TimerSettings.focusTimeMillis(context)
            val isAutorunEnabled = TimerSettings.isAutorunEnabled(context)
            val isLastRound = currentRound >= totalRounds

            when {
                !isLastRound -> {
                    currentRound++
                    start(intervalDuration)
                }
                isAutorunEnabled -> {
                    currentRound = 1
                    start(intervalDuration)
                }
                else -> {
                    reset()
                }
            }
        }
        listener.onIntervalFinished(currentRound, isFocusInterval)
    }

    fun start(durationMillis: Long) {
        if (currentRound == 0) {
            currentRound++
            listener.onIntervalFinished(currentRound, true)
        }
        timer.reset()
        timer.start(durationMillis)
    }

    fun pause() = timer.pause()
    fun resume() = timer.resume()
    fun reset() {
        currentRound = 0
        timer.reset()
    }

    fun getRemainingTime(): Long = timer.getRemainingTime()
    fun isPaused(): Boolean = timer.isPaused()
    fun isRunning(): Boolean = timer.isRunning()
    fun getCurrentRound(): Int = currentRound
    fun isFocusInterval(): Boolean = isFocusInterval
}
package com.diegowh.konohaproject.domain.timer

import android.content.Context
import com.diegowh.konohaproject.domain.settings.TimerSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class IntervalManager (
    private val context: Context,
    private val scope: CoroutineScope
) {

    private val timer = TimerEngine(scope)

    private var currentRound = 0
    private var isFocusInterval = false

    private val _eventFlow = MutableSharedFlow<TimerUIEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        scope.launch {
            timer.timeFlow.collect { remaining ->
                _eventFlow.emit(TimerUIEvent.TimeUpdate(remaining))
            }
        }

        scope.launch {
            timer.finishFlow.collect {
                handleIntervalFinished()
            }
        }
    }

    private suspend fun handleIntervalFinished() {

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
        _eventFlow.emit(TimerUIEvent.IntervalFinished(currentRound, isFocusInterval))
    }

    fun start(durationMillis: Long) {
        if (currentRound == 0) {
            currentRound++
            isFocusInterval = true
            scope.launch {
                _eventFlow.emit(TimerUIEvent.IntervalFinished(currentRound, true))
            }
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
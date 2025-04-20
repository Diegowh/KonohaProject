package com.diegowh.konohaproject.domain.timer


import com.diegowh.konohaproject.domain.settings.SettingsProvider
import com.diegowh.konohaproject.utils.timer.IntervalType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SessionManager (
    private val engine: TimerEngine,
    private val settings: SettingsProvider,
    private val scope: CoroutineScope
) {

    private var currentRound = 0
    private var isFocusInterval = false
    private var actualInterval: IntervalType = IntervalType.FOCUS

    private val _eventFlow = MutableSharedFlow<TimerUIEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        scope.launch {
            engine.timeFlow.collect { remaining ->
                _eventFlow.emit(TimerUIEvent.TimeUpdate(remaining))
            }
        }

        scope.launch {
            engine.finishFlow.collect {
                handleIntervalFinished()
            }
        }
    }

    private suspend fun handleIntervalFinished() {

        val nextInterval: IntervalType
        val totalRounds = settings.totalRounds()
        val isAutorun = settings.isAutorunEnabled()
        val isLastRound = currentRound == totalRounds

        if (actualInterval == IntervalType.FOCUS) {
            val breakTime: Long
            if (isLastRound) {
                actualInterval = IntervalType.LONG_BREAK
                nextInterval = IntervalType.LONG_BREAK
                breakTime = settings.longBreakTimeMillis()
            } else {
                actualInterval = IntervalType.SHORT_BREAK
                nextInterval = IntervalType.SHORT_BREAK
                breakTime = settings.shortBreakTimeMillis()
            }
            start(breakTime)
            _eventFlow.emit(TimerUIEvent.IntervalFinished(currentRound, nextInterval))
        } else {
            actualInterval = IntervalType.FOCUS
            nextInterval = IntervalType.FOCUS
            val focusTime = settings.focusTimeMillis()
            when {
                !isLastRound -> {
                    currentRound++
                    start(focusTime)
                    _eventFlow.emit(TimerUIEvent.IntervalFinished(currentRound, nextInterval))
                }
                isAutorun -> {
                    currentRound = 1
                    start(focusTime)
                    _eventFlow.emit(TimerUIEvent.IntervalFinished(currentRound, nextInterval))
                }
                else -> {
                    currentRound = 0
                    reset()
                    _eventFlow.emit(TimerUIEvent.SessionFinished)
                    return
                }
            }
        }
    }

    fun start(durationMillis: Long) {
        if (currentRound == 0) {
            currentRound++
            isFocusInterval = true
            scope.launch {
                _eventFlow.emit(TimerUIEvent.IntervalFinished(currentRound, actualInterval))
            }
        }
        engine.reset()
        engine.start(durationMillis)
    }

    fun pause() = engine.pause()
    fun resume() = engine.resume()
    fun reset() = engine.reset()

    fun getRemainingTime(): Long = engine.getRemainingTime()
    fun isPaused(): Boolean = engine.isPaused()
    fun isRunning(): Boolean = engine.isRunning()
    fun getCurrentRound(): Int = currentRound
    fun isFocusInterval(): Boolean = isFocusInterval
}
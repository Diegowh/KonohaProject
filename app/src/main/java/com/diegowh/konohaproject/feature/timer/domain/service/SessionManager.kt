package com.diegowh.konohaproject.feature.timer.domain.service


import com.diegowh.konohaproject.feature.timer.domain.model.IntervalType
import com.diegowh.konohaproject.feature.timer.domain.repository.TimerSettingsRepository
import com.diegowh.konohaproject.feature.timer.domain.model.SessionState
import com.diegowh.konohaproject.feature.timer.domain.model.TimerUIEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SessionManager(
    private val engine: TimerEngine,
    private val settings: TimerSettingsRepository,
    private val scope: CoroutineScope
) {

    private var currentSession = SessionState()
    private val _eventFlow = MutableSharedFlow<TimerUIEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        scope.launch {
            engine.state.collect { state ->
                when (state) {
                    is TimerState.Running -> {
                        _eventFlow.emit(TimerUIEvent.TimeUpdate(state.remaining))
                    }
                    is TimerState.Finished -> handleIntervalFinished()
                    else -> Unit
                }
            }
        }
    }

    private suspend fun handleIntervalFinished() {
        val finishedInterval = currentSession.intervalType
        val totalRounds = settings.totalRounds()
        val isLastRound = currentSession.round == totalRounds

        val nextInterval: IntervalType
        if (finishedInterval == IntervalType.FOCUS) {
            val breakTime: Long
            if (isLastRound) {
                currentSession.intervalType = IntervalType.LONG_BREAK
                nextInterval = IntervalType.LONG_BREAK
                breakTime = settings.longBreakTimeMillis()
            } else {
                currentSession.intervalType = IntervalType.SHORT_BREAK
                nextInterval = IntervalType.SHORT_BREAK
                breakTime = settings.shortBreakTimeMillis()
            }
            start(breakTime)
            _eventFlow.emit(
                TimerUIEvent.IntervalFinished(
                    currentSession.round,
                    finishedInterval,
                    nextInterval
                )
            )
        } else {
            currentSession.intervalType = IntervalType.FOCUS
            nextInterval = IntervalType.FOCUS
            val focusTime = settings.focusTimeMillis()
            when {
                !isLastRound -> {
                    currentSession.round++
                    start(focusTime)
                    _eventFlow.emit(
                        TimerUIEvent.IntervalFinished(
                            currentSession.round,
                            finishedInterval,
                            nextInterval
                        )
                    )
                }

                else -> {
                    currentSession.round = 0
                    reset()
                    _eventFlow.emit(TimerUIEvent.SessionFinished)
                    return
                }
            }
        }
    }

    fun start(durationMillis: Long) {
        if (currentSession.round == 0) {
            currentSession.round++
            currentSession.intervalType = IntervalType.FOCUS
            scope.launch {
                _eventFlow.emit(
                    TimerUIEvent.IntervalFinished(
                        currentSession.round,
                        currentSession.intervalType,
                        currentSession.intervalType
                    )
                )
            }
        }
        engine.reset()
        engine.start(durationMillis)
    }

    fun pause() = engine.pause()
    fun resume() = engine.resume()
    fun reset() {
        engine.reset()
        currentSession.round = 0
    }
    fun skipInterval() {
        engine.reset()
        scope.launch {
            handleIntervalFinished()
        }
    }

    fun getRemainingTime(): Long = engine.getRemainingTime()
    fun isPaused(): Boolean = engine.isPaused()
    fun isRunning(): Boolean = engine.isRunning()
}
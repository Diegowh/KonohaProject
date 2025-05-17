package com.diegowh.konohaproject.feature.timer.domain.service


import com.diegowh.konohaproject.feature.timer.domain.model.IntervalType
import com.diegowh.konohaproject.feature.timer.domain.model.SessionState
import com.diegowh.konohaproject.feature.timer.domain.model.TimerUIEvent
import com.diegowh.konohaproject.feature.timer.domain.repository.TimerSettingsRepository
import com.diegowh.konohaproject.feature.xp.domain.XpManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SessionManager(
    private val engine: TimerEngine,
    private val settings: TimerSettingsRepository,
    private val scope: CoroutineScope,
    private val xpManager: XpManager
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
        val finished = currentSession.intervalType
        val totalRounds = settings.totalRounds()
        val isLastRound = currentSession.round == totalRounds

        // calcula el siguiente intervalo
        val next = when (finished) {
            IntervalType.FOCUS ->
                if (isLastRound) IntervalType.LONG_BREAK else IntervalType.SHORT_BREAK

            else -> IntervalType.FOCUS
        }

        val finishedDuration = calculateIntervalDuration(finished)
        xpManager.addXpForIntervalCompleted(finished, finishedDuration)

        // comprueba si es fin de sesion
        if (finished == IntervalType.LONG_BREAK) {

            xpManager.addXpForSession()

            currentSession.round = 0
            reset()
            _eventFlow.emit(TimerUIEvent.SessionFinished)
            return
        }

        // aumenta ronda si corresponde
        if (finished != IntervalType.FOCUS) {
            currentSession.round++
        }

        // obtiene la duracion del siguiente intervalo
        currentSession.intervalType = next
        val nextDuration = calculateIntervalDuration(next)

        // inicia el temporizador
        start(nextDuration)
        _eventFlow.emit(
            TimerUIEvent.IntervalFinished(
                currentSession.round,
                finished,
                next
            )
        )
    }

    private fun calculateIntervalDuration(interval: IntervalType): Long {
        val duration = when (interval) {
            IntervalType.FOCUS -> settings.focusTimeMillis()
            IntervalType.SHORT_BREAK -> settings.shortBreakTimeMillis()
            IntervalType.LONG_BREAK -> settings.longBreakTimeMillis()
        }
        return duration
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
        println("xD!")
        engine.reset()
        currentSession.round = 0
        xpManager.resetSessionXp()
    }

    fun skipInterval() {
        val skippedInterval = currentSession.intervalType
        val skippedIntervalDuration = calculateIntervalDuration(skippedInterval)
        engine.reset()
        scope.launch {
            handleIntervalFinished()
            xpManager.applySkipPenalty(skippedInterval, skippedIntervalDuration)
        }
    }

    fun getRemainingTime(): Long = engine.getRemainingTime()
    fun isPaused(): Boolean = engine.isPaused()
    fun isRunning(): Boolean = engine.isRunning()
}
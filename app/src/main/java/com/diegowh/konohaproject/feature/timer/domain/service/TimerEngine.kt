package com.diegowh.konohaproject.feature.timer.domain.service

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

fun interface Clock {
    fun nowMillis(): Long
}

object SystemClockProvider : Clock {
    override fun nowMillis(): Long = SystemClock.elapsedRealtime()
}

sealed interface TimerState {
    data object Idle : TimerState
    data class Running(val remaining: Long) : TimerState
    data class Paused(val remaining: Long) : TimerState
    data object Finished : TimerState
}

class TimerEngine(
    private val scope: CoroutineScope,
    private val clock: Clock = SystemClockProvider,
    private val logicalTickMillis: Long = 1_000L,
    private val timeScale: Float = 1f
) {

    init {
        // por si aca
        require(timeScale > 0f)
    }

    private val _state = MutableStateFlow<TimerState>(TimerState.Idle)
    val state: StateFlow<TimerState> = _state

    private var job: Job? = null

    private var durationLogical: Long = 0L // milisegundos lógicos totales
    private var startReal: Long = 0L // instante real de arranque
    private var remainingAtPause: Long = 0L

    fun start(durationMillis: Long) {
        durationLogical = durationMillis
        startReal = clock.nowMillis()
        launchTimer()
    }

    fun pause() {
        val remaining = currentRemaining()
        if (_state.value is TimerState.Running) {
            job?.cancel()
            remainingAtPause = remaining
            _state.value = TimerState.Paused(remaining)
        }
    }

    fun resume() {
        if (_state.value is TimerState.Paused) {
            durationLogical = remainingAtPause
            startReal = clock.nowMillis()
            launchTimer()
        }
    }

    fun reset() {
        job?.cancel()
        _state.value = TimerState.Idle
    }

    fun getRemainingTime(): Long = currentRemaining()
    fun isPaused(): Boolean = _state.value is TimerState.Paused
    fun isRunning(): Boolean = _state.value is TimerState.Running

    private fun currentRemaining(): Long {
        return when (val s = _state.value) {
            is TimerState.Running -> s.remaining
            is TimerState.Paused -> s.remaining
            else -> 0L
        }
    }

    private fun launchTimer() {
        job?.cancel()

        // intervalo real al que se emite
        val realDelay = (logicalTickMillis / timeScale).toLong().coerceAtLeast(1L)

        job = scope.launch {
            while (true) {
                val elapsedReal = clock.nowMillis() - startReal // ms reales
                val elapsedLogical = (elapsedReal * timeScale).toLong() // ms “en app”
                val remaining = durationLogical - elapsedLogical

                if (remaining <= 0) {
                    _state.value = TimerState.Finished
                    break
                }

                _state.value = TimerState.Running(remaining)
                delay(realDelay)
            }
        }
    }
}
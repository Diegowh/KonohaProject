package com.diegowh.konohaproject.domain.timer

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.math.max

class TimerEngine(private val scope: CoroutineScope) {

    private val _timeFlow = MutableSharedFlow<Long>(replay = 1)
    val timeFlow = _timeFlow.asSharedFlow()

    private val _finishFlow = MutableSharedFlow<Unit>()
    val finishFlow = _finishFlow.asSharedFlow()

    private var endTime: Long = 0L
    private var job: Job? = null
    private var isPaused: Boolean = false
    private var remainingWhenPaused: Long = 0L

    fun start(durationMillis: Long) {
        endTime = SystemClock.elapsedRealtime() + durationMillis
        startTimerJob()
    }

    fun pause() {
        if (isRunning() && !isPaused) {
            isPaused = true
            remainingWhenPaused = endTime - SystemClock.elapsedRealtime()
            job?.cancel()
        }
    }

    fun resume() {
        if (isRunning() && isPaused) {
            isPaused = false
            endTime = SystemClock.elapsedRealtime() + remainingWhenPaused
            startTimerJob()
        }
    }

    fun reset() {
        job?.cancel()
        endTime = 0L
        remainingWhenPaused = 0L
        isPaused = false
    }

    fun getRemainingTime(): Long = when {
        !isRunning() -> 0L
        isPaused -> remainingWhenPaused
        else -> endTime - SystemClock.elapsedRealtime()
    }

    fun isPaused(): Boolean = isPaused
    fun isRunning(): Boolean = endTime > 0L

    private fun startTimerJob() {
        job?.cancel()
        job = scope.launch {
            var nextEmit = SystemClock.elapsedRealtime()
            while (isRunning() && !isPaused) {
                val remaining = endTime - SystemClock.elapsedRealtime()
                if (remaining <= 0) {
                    _finishFlow.emit(Unit)
                    break
                }
                _timeFlow.emit(remaining)
                nextEmit += 1000L
                val wait = nextEmit - SystemClock.elapsedRealtime()
                delay(max(0, wait))
            }
        }
    }
}
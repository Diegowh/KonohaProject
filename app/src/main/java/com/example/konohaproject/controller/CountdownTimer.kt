package com.example.konohaproject.controller

import android.location.GnssAntennaInfo.Listener
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CountdownTimer(
    private val scope: CoroutineScope,
    private val listener: Listener
) {
    interface Listener {
        fun onTimeUpdate(remaining: Long)
        fun onCountdownFinished()
    }

    private var endTime: Long = 0L
    private var job: Job? = null
    private var isPaused: Boolean = false
    private var remainingWhenPaused: Long = 0L

    fun start(durationMillis: Long) {
        reset()
        endTime = SystemClock.elapsedRealtime() + durationMillis
        startCountdownLoop()
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
            startCountdownLoop()
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

    private fun startCountdownLoop() {
        job = scope.launch {
            while (isRunning() && !isPaused) {
                val remaining = endTime - SystemClock.elapsedRealtime()
                if (remaining <= 0) {
                    listener.onCountdownFinished()
                    reset()
                    break
                }
                listener.onTimeUpdate(remaining)
                delay(1000)
            }
        }
    }
}
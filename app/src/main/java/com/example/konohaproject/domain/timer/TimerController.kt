package com.example.konohaproject.domain.timer

interface TimerController {

    fun start(durationMillis: Long)
    fun pause()
    fun resume()
    fun reset()
    fun getRemainingTime(): Long
    fun isPaused(): Boolean
    fun isRunning(): Boolean
    fun setTimeUpdateListener(listener: TimeUpdateListener?)
    fun getCurrentCycle(): Int
    fun isFocusSession(): Boolean


}
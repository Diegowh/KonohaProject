package com.example.konohaproject.model

import com.example.konohaproject.controller.CountdownService.TimeUpdateListener

interface CountdownController {
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
//    fun moveToNextSession()

}
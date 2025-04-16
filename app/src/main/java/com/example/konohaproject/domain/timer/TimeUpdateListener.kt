package com.example.konohaproject.domain.timer

interface TimeUpdateListener {
    fun onTimeUpdate(remainingTime: Long)
    fun onIntervalFinished(currentRound: Int, isFocus: Boolean)
}
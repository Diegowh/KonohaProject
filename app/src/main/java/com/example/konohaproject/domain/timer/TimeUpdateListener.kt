package com.example.konohaproject.domain.timer

interface TimeUpdateListener {
    fun onTimeUpdate(remainingTime: Long)
    fun onTimerFinished(currentRound: Int, isFocus: Boolean)
}
package com.diegowh.konohaproject.domain.timer

sealed class TimerUIEvent {
    data class TimeUpdate(val remainingMillis: Long) : TimerUIEvent()
    data class IntervalFinished(val currentRound: Int, val isFocusInterval: Boolean) :TimerUIEvent()
}
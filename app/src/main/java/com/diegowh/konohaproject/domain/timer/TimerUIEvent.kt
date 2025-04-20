package com.diegowh.konohaproject.domain.timer

import com.diegowh.konohaproject.utils.timer.IntervalType

sealed class TimerUIEvent {
    data class TimeUpdate(val remainingMillis: Long) : TimerUIEvent()
    data class IntervalFinished(
        val currentRound: Int,
        val nextInterval: IntervalType
    ) : TimerUIEvent()

    data object SessionFinished : TimerUIEvent()
}
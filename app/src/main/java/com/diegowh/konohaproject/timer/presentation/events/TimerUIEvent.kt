package com.diegowh.konohaproject.timer.presentation.events

import com.diegowh.konohaproject.timer.domain.model.IntervalType

sealed class TimerUIEvent {
    data class TimeUpdate(val remainingMillis: Long) : TimerUIEvent()
    data class IntervalFinished(
        val currentRound: Int,
        val nextInterval: IntervalType
    ) : TimerUIEvent()

    data object SessionFinished : TimerUIEvent()
}


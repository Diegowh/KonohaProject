package com.diegowh.konohaproject.feature.timer.domain.model

sealed class TimerUIEvent {
    data class TimeUpdate(val remainingMillis: Long) : TimerUIEvent()
    data class IntervalFinished(
        val currentRound: Int,
        val finishedInterval: IntervalType,
        val nextInterval: IntervalType
    ) : TimerUIEvent()

    data object SessionFinished : TimerUIEvent()
}


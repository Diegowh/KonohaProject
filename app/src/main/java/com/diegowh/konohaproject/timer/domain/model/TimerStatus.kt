package com.diegowh.konohaproject.timer.domain.model

sealed class TimerStatus {
    data object Running : TimerStatus()
    data object Paused : TimerStatus()
    data object Stopped : TimerStatus()
}
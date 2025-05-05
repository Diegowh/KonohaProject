package com.diegowh.konohaproject.timer.domain.models

sealed class TimerStatus {
    data object Running : TimerStatus()
    data object Paused : TimerStatus()
    data object Stopped : TimerStatus()
}
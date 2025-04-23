package com.diegowh.konohaproject.domain.timer

sealed class TimerStatus {
    data object Running : TimerStatus()
    data object Paused : TimerStatus()
    data object Stopped : TimerStatus()
}
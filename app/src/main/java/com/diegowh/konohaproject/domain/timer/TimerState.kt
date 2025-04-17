package com.diegowh.konohaproject.domain.timer

sealed class TimerState {
    data object Running : TimerState()
    data object Paused : TimerState()
    data object Stopped : TimerState()
}
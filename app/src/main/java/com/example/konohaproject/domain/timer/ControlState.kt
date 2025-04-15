package com.example.konohaproject.domain.timer

sealed class ControlState {
    data object Running : ControlState()
    data object Paused : ControlState()
    data object Stopped : ControlState()
}
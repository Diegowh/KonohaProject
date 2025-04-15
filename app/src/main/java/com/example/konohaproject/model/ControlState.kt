package com.example.konohaproject.model

sealed class ControlState {
    data object Running : ControlState()
    data object Paused : ControlState()
    data object Stopped : ControlState()
}
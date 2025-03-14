package com.example.konohaproject.controller

sealed class ControlState {
    data object Running : ControlState()
    data object Paused : ControlState()
    data object Stopped : ControlState()
}
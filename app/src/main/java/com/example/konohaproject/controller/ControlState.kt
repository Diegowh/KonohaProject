package com.example.konohaproject.controller

sealed class ControlState {
    data object Playing : ControlState()
    data object Paused : ControlState()

}
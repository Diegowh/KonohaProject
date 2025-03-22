package com.example.konohaproject.controller

import java.util.Locale

object TimeConfig {
    private const val FOCUS_TIME_MINUTES: Long = 25L
    const val BREAK_TIME_MINUTES = 5L

    fun focusTimeMillis() = FOCUS_TIME_MINUTES * 60 * 1000
    fun breakTimeMillis() = BREAK_TIME_MINUTES * 60 * 1000

    fun initialFocusDisplayTime() = initialDisplayTime(focusTimeMillis())
    fun initialBreakDisplayTime() = initialDisplayTime(breakTimeMillis())

    private fun initialDisplayTime(displayTime: Long): String {
        val minutes = displayTime / 1000 / 60
        val seconds = displayTime / 1000 % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
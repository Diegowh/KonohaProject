package com.example.konohaproject.controller

import java.util.Locale

object TimeConfig {
    private const val FOCUS_TIME_MINUTES: Long = 25L
    private const val BREAK_TIME_MINUTES: Long = 5L
    private const val LONG_BREAK_TIME_MINUTES: Long = 15L

    private const val TOTAL_CYCLES: Int = 4

    fun getTotalCycles() = TOTAL_CYCLES

    fun focusTimeMinutes() = FOCUS_TIME_MINUTES
    fun breakTimeMinutes() = BREAK_TIME_MINUTES
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
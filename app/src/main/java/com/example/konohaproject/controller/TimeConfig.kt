package com.example.konohaproject.controller

import java.util.Locale

object TimeConfig {
    private const val PREFS_NAME = "timer_settings"
    private const val KEY_FOCUS = "focus_time"
    private const val KEY_SHORT_BREAK = "short_break"
    private const val KEY_LONG_BREAK = "long_break"
    private const val KEY_CYCLES = "total_cycles"
    private const val KEY_AUTO_RESTART = "auto_restart"

    private const val DEFAULT_FOCUS = 25L
    private const val DEFAULT_SHORT_BREAK = 5L
    private const val DEFAULT_LONG_BREAK = 15L
    private const val DEFAULT_CYCLES = 4
    private const val DEFAULT_AUTO_RESTART = false

    private const val FOCUS_TIME_MINUTES: Long = 25L
    private const val BREAK_TIME_MINUTES: Long = 5L
    private const val LONG_BREAK_TIME_MINUTES: Long = 15L

    private const val TOTAL_CYCLES: Int = 4

    private const val AUTO_RESTART: Boolean = false

    fun getTotalCycles() = TOTAL_CYCLES
    fun isAutoRestartEnabled() = AUTO_RESTART
    fun focusTimeMinutes() = FOCUS_TIME_MINUTES
    fun breakTimeMinutes() = BREAK_TIME_MINUTES
    fun longBreakTimeMinutes() = LONG_BREAK_TIME_MINUTES
    fun focusTimeMillis() = minutesToMilliseconds(FOCUS_TIME_MINUTES)
    fun breakTimeMillis() = minutesToMilliseconds(BREAK_TIME_MINUTES)
    fun longBreakTimeMillis() = minutesToMilliseconds(LONG_BREAK_TIME_MINUTES)

    fun initialFocusDisplayTime() = initialDisplayTime(focusTimeMillis())
    fun initialBreakDisplayTime() = initialDisplayTime(breakTimeMillis())

    private fun initialDisplayTime(displayTime: Long): String {
        val minutes = displayTime / 1000 / 60
        val seconds = displayTime / 1000 % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    private fun minutesToMilliseconds(minutes: Long): Long {
        return minutes * 60 * 1000
    }
}
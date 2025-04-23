package com.diegowh.konohaproject.domain.settings

interface TimerSettingsRepository {
    fun totalRounds(): Int
    fun focusTimeMillis(): Long
    fun shortBreakTimeMillis(): Long
    fun longBreakTimeMillis(): Long
    fun isAutorunEnabled(): Boolean
    fun isMuteEnabled(): Boolean
    fun initialDisplayTime(): String
    fun updateSettings(
        focus: Long,
        shortBreak: Long,
        longBreak: Long,
        rounds: Int,
        autorun: Boolean,
        mute: Boolean
    )

    fun resetToDefaults()
    fun focusMinutes(): Long
    fun shortBreakMinutes(): Long
    fun longBreakMinutes(): Long
}

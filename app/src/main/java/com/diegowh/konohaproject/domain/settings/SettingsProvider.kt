package com.diegowh.konohaproject.domain.settings

interface SettingsProvider {
    fun totalRounds(): Int
    fun focusTimeMillis(): Long
    fun shortBreakTimeMillis(): Long
    fun longBreakTimeMillis(): Long
    fun isAutorunEnabled(): Boolean
    fun isMuteEnabled(): Boolean
    fun initialDisplayTime(isFocus: Boolean = true): String
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

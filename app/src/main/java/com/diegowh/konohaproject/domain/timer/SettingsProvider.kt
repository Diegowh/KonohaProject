package com.diegowh.konohaproject.domain.timer

interface SettingsProvider {
    fun totalRounds(): Int
    fun focusTimeMillis(): Long
    fun shortBreakTimeMillis(): Long
    fun longBreakTimeMillis(): Long
    fun isAutorunEnabled(): Boolean
}
package com.diegowh.konohaproject.domain.timer

import android.content.Context
import com.diegowh.konohaproject.domain.settings.TimerSettings

class TimerSettingsProvider(private val context: Context) : SettingsProvider {
    override fun totalRounds() = TimerSettings.getTotalRounds(context)
    override fun focusTimeMillis() = TimerSettings.focusTimeMillis(context)
    override fun shortBreakTimeMillis() = TimerSettings.shortBreakTimeMillis(context)
    override fun longBreakTimeMillis() = TimerSettings.longBreakTimeMillis(context)
    override fun isAutorunEnabled() = TimerSettings.isAutorunEnabled(context)
}
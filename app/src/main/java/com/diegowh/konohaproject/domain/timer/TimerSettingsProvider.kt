package com.diegowh.konohaproject.domain.timer

import android.content.Context
import com.diegowh.konohaproject.domain.settings.TimerSettings

class TimerSettingsProvider(private val context: Context) : SettingsProvider {
    override fun totalRounds(): Int = TimerSettings.getTotalRounds(context)
    override fun focusTimeMillis(): Long = TimerSettings.focusTimeMillis(context)
    override fun shortBreakTimeMillis(): Long = TimerSettings.shortBreakTimeMillis(context)
    override fun longBreakTimeMillis(): Long = TimerSettings.longBreakTimeMillis(context)
    override fun isAutorunEnabled(): Boolean = TimerSettings.isAutorunEnabled(context)
    override fun isMuteEnabled(): Boolean = TimerSettings.isMuteEnabled(context)
    override fun initialDisplayTime(isFocus: Boolean): String =
        TimerSettings.initialDisplayTime(context, isFocus)
    override fun updateSettings(
        focus: Long,
        shortBreak: Long,
        longBreak: Long,
        rounds: Int,
        autorun: Boolean,
        mute: Boolean
    ) {
        TimerSettings.updateSettings(
            context,
            focus,
            shortBreak,
            longBreak,
            rounds,
            autorun,
            mute
        )
    }
    override fun resetToDefaults() { TimerSettings.resetToDefaults(context) }
    override fun focusMinutes(): Long = TimerSettings.getFocusMinutes(context)
    override fun shortBreakMinutes(): Long = TimerSettings.getShortBreakMinutes(context)
    override fun longBreakMinutes(): Long = TimerSettings.getLongBreakMinutes(context)
}
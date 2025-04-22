package com.diegowh.konohaproject.domain.settings

import android.content.Context
import java.util.Locale

class TimerPrefsRepository(
    private val context: Context
) : TimerSettingsRepository {

    private val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)

    private companion object Keys {
        const val FOCUS_TIME = "focus_time"
        const val SHORT_BREAK = "short_break"
        const val LONG_BREAK = "long_break"
        const val ROUNDS = "rounds"
        const val AUTORUN = "autorun"
        const val MUTE = "mute"

        const val DEFAULT_FOCUS_MINUTES = 25L
        const val DEFAULT_SHORT_BREAK_MINUTES = 5L
        const val DEFAULT_LONG_BREAK_MINUTES = 15L
        const val DEFAULT_ROUNDS = 4
        const val DEFAULT_AUTORUN = true
        const val DEFAULT_MUTE = false
    }

    override fun totalRounds(): Int =
        prefs.getInt(ROUNDS, DEFAULT_ROUNDS)

    override fun focusTimeMillis(): Long =
        prefs.getLong(FOCUS_TIME, DEFAULT_FOCUS_MINUTES) * 60 * 1000

    override fun shortBreakTimeMillis(): Long =
        prefs.getLong(SHORT_BREAK, DEFAULT_SHORT_BREAK_MINUTES) * 60 * 1000

    override fun longBreakTimeMillis(): Long =
        prefs.getLong(LONG_BREAK, DEFAULT_LONG_BREAK_MINUTES) * 60 * 1000

    override fun isAutorunEnabled(): Boolean =
        prefs.getBoolean(AUTORUN, DEFAULT_AUTORUN)

    override fun isMuteEnabled(): Boolean =
        prefs.getBoolean(MUTE, DEFAULT_MUTE)

    override fun initialDisplayTime(): String {
        val millis = focusTimeMillis()
        val minutes = millis / 1000 / 60
        val seconds = millis / 1000 % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    override fun updateSettings(
        focus: Long,
        shortBreak: Long,
        longBreak: Long,
        rounds: Int,
        autorun: Boolean,
        mute: Boolean
    ) {
        prefs.edit().apply {
            putLong(FOCUS_TIME, focus)
            putLong(SHORT_BREAK, shortBreak)
            putLong(LONG_BREAK, longBreak)
            putInt(ROUNDS, rounds)
            putBoolean(AUTORUN, autorun)
            putBoolean(MUTE, mute)
        }.apply()
    }

    override fun resetToDefaults() {
        updateSettings(
            focus = DEFAULT_FOCUS_MINUTES,
            shortBreak = DEFAULT_SHORT_BREAK_MINUTES,
            longBreak = DEFAULT_LONG_BREAK_MINUTES,
            rounds = DEFAULT_ROUNDS,
            autorun = DEFAULT_AUTORUN,
            mute = DEFAULT_MUTE
        )
    }

    override fun focusMinutes(): Long =
        prefs.getLong(FOCUS_TIME, DEFAULT_FOCUS_MINUTES)

    override fun shortBreakMinutes(): Long =
        prefs.getLong(SHORT_BREAK, DEFAULT_SHORT_BREAK_MINUTES)

    override fun longBreakMinutes(): Long =
        prefs.getLong(LONG_BREAK, DEFAULT_LONG_BREAK_MINUTES)
}
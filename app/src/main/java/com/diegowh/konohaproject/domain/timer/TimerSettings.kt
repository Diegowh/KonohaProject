package com.diegowh.konohaproject.domain.timer

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

object TimerSettings {
    private const val PREFS_NAME = "timer_settings"
    private const val KEY_FOCUS = "focus_time"
    private const val KEY_SHORT_BREAK = "short_break"
    private const val KEY_LONG_BREAK = "long_break"
    private const val KEY_ROUNDS = "total_rounds"
    private const val KEY_AUTO_RUN = "auto_run"


    private const val DEFAULT_FOCUS = 25L
    private const val DEFAULT_SHORT_BREAK = 5L
    private const val DEFAULT_LONG_BREAK = 15L
    private const val DEFAULT_ROUNDS = 4
    private const val DEFAULT_AUTO_RUN = true

    fun getDefaultFocus(): Long { return DEFAULT_FOCUS }
    fun getDefaultShortBreak(): Long { return DEFAULT_SHORT_BREAK }
    fun getDefaultLongBreak(): Long { return DEFAULT_LONG_BREAK }
    fun getDefaultRounds(): Int { return DEFAULT_ROUNDS }
    fun getDefaultAutorun(): Boolean { return DEFAULT_AUTO_RUN }

    fun getFocusMinutes(context: Context): Long {
        val prefs = getPrefs(context)
        return try {
            prefs.getLong(KEY_FOCUS, DEFAULT_FOCUS)
        } catch (e: ClassCastException) {

            val intValue = prefs.getInt(KEY_FOCUS, DEFAULT_FOCUS.toInt())
            val longValue = intValue.toLong()

            prefs.edit().putLong(KEY_FOCUS, longValue).apply()
            longValue
        }
    }

    fun getShortBreakMinutes(context: Context): Long {
        val prefs = getPrefs(context)
        return try {
            prefs.getLong(KEY_SHORT_BREAK, DEFAULT_SHORT_BREAK)
        } catch (e: ClassCastException) {
            val intValue = prefs.getInt(KEY_SHORT_BREAK, DEFAULT_SHORT_BREAK.toInt())
            val longValue = intValue.toLong()
            prefs.edit().putLong(KEY_SHORT_BREAK, longValue).apply()
            longValue
        }
    }

    fun getLongBreakMinutes(context: Context): Long {
        val prefs = getPrefs(context)
        return try {
            prefs.getLong(KEY_LONG_BREAK, DEFAULT_LONG_BREAK)
        } catch (e: ClassCastException) {
            val intValue = prefs.getInt(KEY_LONG_BREAK, DEFAULT_LONG_BREAK.toInt())
            val longValue = intValue.toLong()
            prefs.edit().putLong(KEY_LONG_BREAK, longValue).apply()
            longValue
        }
    }
    fun getTotalRounds(context: Context): Int = getPrefs(context).getInt(KEY_ROUNDS, DEFAULT_ROUNDS)
    fun isAutorunEnabled(context: Context): Boolean = getPrefs(context).getBoolean(
        KEY_AUTO_RUN, DEFAULT_AUTO_RUN
    )



    fun focusTimeMillis(context: Context) = minutesToMilliseconds(getFocusMinutes(context))
    fun shortBreakTimeMillis(context: Context) = minutesToMilliseconds(getShortBreakMinutes(context))
    fun longBreakTimeMillis(context: Context) = minutesToMilliseconds(getLongBreakMinutes(context))


    fun initialDisplayTime(context: Context, isFocus: Boolean = true): String {
        val millis = if (isFocus) focusTimeMillis(context) else shortBreakTimeMillis(context)
        val minutes = millis / 1000 / 60
        val seconds = millis / 1000 % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun minutesToMilliseconds(minutes: Long): Long = minutes * 60 * 1000

    fun updateSettings(
        context: Context,
        focus: Long,
        shortBreak: Long,
        longBreak: Long,
        rounds: Int,
        autorun: Boolean
    ) {
        getPrefs(context).edit().apply {
            putLong(KEY_FOCUS, focus)
            putLong(KEY_SHORT_BREAK, shortBreak)
            putLong(KEY_LONG_BREAK, longBreak)
            putInt(KEY_ROUNDS, rounds)
            putBoolean(KEY_AUTO_RUN, autorun)
            apply()
        }
    }
}
package com.diegowh.konohaproject.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.diegowh.konohaproject.domain.timer.TimerSettings

class SettingsViewModel : ViewModel() {

    val focusValues = mutableListOf<Int>()
    val shortBreakValues = mutableListOf<Int>()
    val longBreakValues = mutableListOf<Int>()
    val roundsValues = mutableListOf<Int>()

    var focusProgress: Int = 0
    var shortBreakProgress: Int = 0
    var longBreakProgress: Int = 0
    var roundsProgress: Int = 0
    var autorun: Boolean = true
    var mute: Boolean = false

    init {
        setupFocusValues()
        setupShortBreakValues()
        setupLongBreakValues()
        setupRoundsValues()
    }

    private fun setupFocusValues() {
        var current = 1
        while (current < 5) {
            focusValues.add(current)
            current += 1
        }
        while (current < 60) {
            focusValues.add(current)
            current += 5
        }
        while (current <= 120) {
            focusValues.add(current)
            current += 15
        }
    }

    private fun setupShortBreakValues() {
        var current = 1
        while (current < 5) {
            shortBreakValues.add(current)
            current += 1
        }
        while (current <= 30) {
            shortBreakValues.add(current)
            current += 5
        }
    }

    private fun setupLongBreakValues() {
        var current = 1
        while (current < 5) {
            longBreakValues.add(current)
            current += 1
        }
        while (current < 30) {
            longBreakValues.add(current)
            current += 5
        }
        while (current <= 60) {
            longBreakValues.add(current)
            current += 15
        }
    }

    private fun setupRoundsValues() {
        var current = 2
        while (current <= 6) {
            roundsValues.add(current)
            current += 1
        }
    }

    fun getDefaultIndices(): Map<String, Int> {
        return mapOf(
            "focus" to focusValues.indexOf(TimerSettings.getDefaultFocus().toInt()),
            "shortBreak" to shortBreakValues.indexOf(TimerSettings.getDefaultShortBreak().toInt()),
            "longBreak" to longBreakValues.indexOf(TimerSettings.getDefaultLongBreak().toInt()),
            "rounds" to roundsValues.indexOf(TimerSettings.getDefaultRounds()),
            "autorun" to if (TimerSettings.getDefaultAutorun()) 1 else 0, // quiero mantener el tipo del Map en String, Int
            "mute" to if(TimerSettings.getDefaultMute()) 1 else 0
        )
    }

    fun loadSavedPreferences(context: Context) {
        val savedFocus = TimerSettings.getFocusMinutes(context).toInt()
        focusProgress = focusValues.indexOf(savedFocus).coerceAtLeast(0)

        val savedShort = TimerSettings.getShortBreakMinutes(context).toInt()
        shortBreakProgress = shortBreakValues.indexOf(savedShort).coerceAtLeast(0)

        val savedLong = TimerSettings.getLongBreakMinutes(context).toInt()
        longBreakProgress = longBreakValues.indexOf(savedLong).coerceAtLeast(0)

        val savedRounds = TimerSettings.getTotalRounds(context)
        roundsProgress = roundsValues.indexOf(savedRounds).coerceAtLeast(0)

        autorun = TimerSettings.isAutorunEnabled(context)
        mute = TimerSettings.isMuteEnabled(context)

    }

    fun savePreferences(context: Context) {
        TimerSettings.updateSettings(
            context,
            focus = focusValues[focusProgress].toLong(),
            shortBreak = shortBreakValues[shortBreakProgress].toLong(),
            longBreak = longBreakValues[longBreakProgress].toLong(),
            rounds = roundsValues[roundsProgress],
            autorun = autorun,
            mute = mute
        )
    }
}

package com.example.konohaproject.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.konohaproject.model.TimeConfig

class SettingsViewModel : ViewModel() {

    val focusValues = mutableListOf<Int>()
    val shortBreakValues = mutableListOf<Int>()
    val longBreakValues = mutableListOf<Int>()
    val roundsValues = mutableListOf<Int>()

    var focusProgress: Int = 0
    var shortBreakProgress: Int = 0
    var longBreakProgress: Int = 0
    var roundsProgress: Int = 0

    init {
        setupFocusValues()
        setupShortBreakValues()
        setupLongBreakValues()
        setupRoundsValues()
    }

    private fun setupFocusValues() {
        var current = 1
        while (current <= 60) {
            focusValues.add(current)
            current += 5
        }
        current = 75
        while (current <= 90) {
            focusValues.add(current)
            current += 15
        }
    }

    private fun setupShortBreakValues() {
        var current = 1
        while (current <= 5) {
            shortBreakValues.add(current)
            current += 1
        }
        current = 10
        while (current <= 15) {
            shortBreakValues.add(current)
            current += 5
        }
    }

    private fun setupLongBreakValues() {
        var current = 2
        while (current <= 40) {
            longBreakValues.add(current)
            current += 5
        }
    }

    private fun setupRoundsValues() {
        var current = 2
        while (current <= 8) {
            roundsValues.add(current)
            current += 1
        }
    }

    fun getDefaultIndices(): Map<String, Int> {
        return mapOf(
            "focus" to focusValues.indexOf(TimeConfig.getDefaultFocus().toInt()),
            "shortBreak" to shortBreakValues.indexOf(TimeConfig.getDefaultShortBreak().toInt()),
            "longBreak" to longBreakValues.indexOf(TimeConfig.getDefaultLongBreak().toInt()),
            "rounds" to roundsValues.indexOf(TimeConfig.getDefaultRounds())
        )
    }

    fun loadSavedPreferences(context: Context) {
        focusProgress = focusValues.indexOf(TimeConfig.getFocusMinutes(context).toInt())
        shortBreakProgress = shortBreakValues.indexOf(TimeConfig.getShortBreakMinutes(context).toInt())
        longBreakProgress = longBreakValues.indexOf(TimeConfig.getLongBreakMinutes(context).toInt())
        roundsProgress = roundsValues.indexOf(TimeConfig.getTotalRounds(context))
    }

    fun savePreferences(context: Context) {
        TimeConfig.updateSettings(
            context,
            focus = focusValues[focusProgress].toLong(),
            shortBreak = shortBreakValues[shortBreakProgress].toLong(),
            longBreak = longBreakValues[longBreakProgress].toLong(),
            rounds = roundsValues[roundsProgress],
            autoRestart = true
        )
    }
}

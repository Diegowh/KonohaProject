package com.diegowh.konohaproject.timer.application.usecases

import com.diegowh.konohaproject.timer.domain.models.IntervalType
import com.diegowh.konohaproject.character.domain.models.Character

sealed class TimerUIEvent {
    data class TimeUpdate(val remainingMillis: Long) : TimerUIEvent()
    data class IntervalFinished(
        val currentRound: Int,
        val nextInterval: IntervalType
    ) : TimerUIEvent()

    data object SessionFinished : TimerUIEvent()
}

sealed class TimerScreenEvent {
    sealed class TimerEvent : TimerScreenEvent() {
        data object Play : TimerEvent()
        data object Pause : TimerEvent()
        data object Reset : TimerEvent()
    }
    
    sealed class CharacterEvent : TimerScreenEvent() {
        data class Select(val character: Character) : CharacterEvent()
    }
    
    sealed class SettingsEvent : TimerScreenEvent() {
        data class UpdateSettings(
            val focusMinutes: Long,
            val shortBreakMinutes: Long,
            val longBreakMinutes: Long,
            val totalRounds: Int,
            val isAutorunEnabled: Boolean,
            val isMuteEnabled: Boolean
        ) : SettingsEvent()
        data object Reset : SettingsEvent()
    }
}
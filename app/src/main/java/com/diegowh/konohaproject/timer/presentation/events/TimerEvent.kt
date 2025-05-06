package com.diegowh.konohaproject.timer.presentation.events

import com.diegowh.konohaproject.character.domain.model.Character

sealed class TimerEvent {
    sealed class TimerAction : TimerEvent() {
        data object Play : TimerAction()
        data object Pause : TimerAction()
        data object Reset : TimerAction()
    }

    sealed class CharacterAction : TimerEvent() {
        data class Select(val character: Character) : CharacterAction()
    }

    sealed class SettingsAction : TimerEvent() {
        data class UpdateSettings(
            val focusMinutes: Long,
            val shortBreakMinutes: Long,
            val longBreakMinutes: Long,
            val totalRounds: Int,
            val isAutorunEnabled: Boolean,
            val isMuteEnabled: Boolean
        ) : SettingsAction()
        data object Reset : SettingsAction()
    }
}
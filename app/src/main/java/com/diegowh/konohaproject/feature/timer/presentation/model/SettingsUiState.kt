package com.diegowh.konohaproject.feature.timer.presentation.model

data class SettingsUiState(
    val isMuteEnabled: Boolean = false,
    val isAutorunEnabled: Boolean = true,
    val focusMinutes: Long = 25,
    val shortBreakMinutes: Long = 5,
    val longBreakMinutes: Long = 15,
    val totalRounds: Int = 4
)
package com.diegowh.konohaproject.feature.timer.presentation.model

data class CombinedUiState(
    val timer: TimerUiState,
    val character: CharacterUiState,
    val animation: AnimationUiState,
    val settings: SettingsUiState,
    val screen: ScreenUiState
)
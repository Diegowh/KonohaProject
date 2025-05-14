package com.diegowh.konohaproject.feature.timer.presentation.model

data class ScreenUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val intervalDialog: IntervalDialogState = IntervalDialogState(),
    val sessionDialogVisible: Boolean = false
)
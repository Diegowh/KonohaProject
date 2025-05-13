package com.diegowh.konohaproject.feature.timer.presentation.model

import com.diegowh.konohaproject.feature.timer.domain.model.Interval
import com.diegowh.konohaproject.feature.timer.domain.model.TimerStatus

data class TimerUiState(
    val timerText: String = "",
    val status: TimerStatus = TimerStatus.Stopped,
    val currentRound: Int = 0,
    val totalRounds: Int = 0,
    val interval: Interval? = null,
    val resumedTime: Long = 0L
)
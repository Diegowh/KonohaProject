package com.diegowh.konohaproject.feature.timer.presentation.model

import com.diegowh.konohaproject.feature.timer.domain.model.TimerStatus
import com.diegowh.konohaproject.feature.timer.domain.model.Interval


data class TimerState(
    val timerText: String = "",
    val status: TimerStatus = TimerStatus.Stopped,
    val currentRound: Int = 0,
    val totalRounds: Int = 0,
    val interval: Interval? = null,
    val resumedTime: Long = 0L
)


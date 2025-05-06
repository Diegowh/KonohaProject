package com.diegowh.konohaproject.timer.presentation.state

import com.diegowh.konohaproject.timer.domain.model.TimerStatus
import com.diegowh.konohaproject.timer.domain.model.Interval


data class TimerState(
    val timerText: String = "",
    val status: TimerStatus = TimerStatus.Stopped,
    val currentRound: Int = 0,
    val totalRounds: Int = 0,
    val interval: Interval? = null,
    val resumedTime: Long = 0L
)


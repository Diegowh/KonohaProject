package com.diegowh.konohaproject.ui.timer

import com.diegowh.konohaproject.domain.timer.TimerState
import com.diegowh.konohaproject.utils.animation.AnimationAction
import com.diegowh.konohaproject.utils.animation.AnimationState


import com.diegowh.konohaproject.utils.timer.Interval

data class TimerUIState(
    val timerText: String = "",
    val state: TimerState = TimerState.Stopped,
    val currentRound: Int = 0,
    val interval: Interval? = null,
    val resumedTime: Long = 0L,
    val totalRounds: Int = 0,
    val animationAction: AnimationAction? = null,
    val animationState: AnimationState = AnimationState(0, isPaused = false)
)

package com.diegowh.konohaproject.ui.timer

import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.domain.character.Character
import com.diegowh.konohaproject.domain.timer.TimerState
import com.diegowh.konohaproject.core.animation.AnimationAction
import com.diegowh.konohaproject.core.animation.AnimationState


import com.diegowh.konohaproject.core.timer.Interval

data class TimerUIState(
    val timerText: String = "",
    val state: TimerState = TimerState.Stopped,
    val currentRound: Int = 0,
    val interval: Interval? = null,
    val resumedTime: Long = 0L,
    val totalRounds: Int = 0,
    val animationAction: AnimationAction? = null,
    val animationState: AnimationState = AnimationState(0, isPaused = false),
    val selectedCharacter: Character = Character(
        1,
        "Sakura",
        R.drawable.sakura_miniatura,
        R.array.test_sakura_focus_frames,
        R.array.test_sakura_break_frames,
        R.array.test_sakura_focus_palette,
        R.array.test_sakura_break_palette
    )
)

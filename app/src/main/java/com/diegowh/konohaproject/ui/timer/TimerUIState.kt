package com.diegowh.konohaproject.ui.timer

import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.domain.character.Character
import com.diegowh.konohaproject.domain.timer.TimerStatus
import com.diegowh.konohaproject.core.animation.AnimationAction
import com.diegowh.konohaproject.core.timer.Interval


data class TimerState(
    val timerText: String = "",
    val status: TimerStatus = TimerStatus.Stopped,
    val currentRound: Int = 0,
    val totalRounds: Int = 0,
    val interval: Interval? = null,
    val resumedTime: Long = 0L
)

data class AnimationState(
    val action: AnimationAction? = null,
    val currentFrame: Int = 0,
    val isPaused: Boolean = false
)

data class TimerUIState(
    val timerState: TimerState = TimerState(),
    val animationState: AnimationState = AnimationState(),
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
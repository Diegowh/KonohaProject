package com.diegowh.konohaproject.ui.timer

import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.domain.character.Character
import com.diegowh.konohaproject.domain.timer.TimerStatus
import com.diegowh.konohaproject.core.animation.AnimationAction
import com.diegowh.konohaproject.core.sound.SoundType
import com.diegowh.konohaproject.core.timer.Interval
import com.diegowh.konohaproject.core.timer.IntervalType


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
    val isPaused: Boolean = false,
    val shouldUpdateFrames: Boolean = false
)

data class SettingsState(
    val isMuteEnabled: Boolean = false,
    val isAutorunEnabled: Boolean = true
)

data class IntervalDialogState(
    val showDialog: Boolean = false,
    val intervalType: IntervalType? = null,
    val continueNext: Boolean? = null
)

data class TimerScreenState(
    val timer: TimerState = TimerState(),
    val animation: AnimationState = AnimationState(),
    val character: Character = Character(
        1,
        "Sakura",
        R.drawable.sakura_miniatura,
        R.array.test_sakura_focus_frames,
        R.array.test_sakura_break_frames,
        R.array.test_sakura_focus_palette,
        R.array.test_sakura_break_palette
    ),
    val settings: SettingsState = SettingsState(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val intervalDialog: IntervalDialogState = IntervalDialogState()
)
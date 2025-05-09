package com.diegowh.konohaproject.feature.timer.presentation.model

import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.feature.character.domain.model.Character
import com.diegowh.konohaproject.feature.settings.presentation.model.SettingsState

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
    val intervalDialog: IntervalDialogState = IntervalDialogState(),
    val sessionDialogVisible: Boolean = false
)
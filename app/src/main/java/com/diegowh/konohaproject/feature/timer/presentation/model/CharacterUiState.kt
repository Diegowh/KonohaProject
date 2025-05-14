package com.diegowh.konohaproject.feature.timer.presentation.model

import com.diegowh.konohaproject.feature.character.domain.model.Character
import com.diegowh.konohaproject.feature.timer.domain.model.IntervalType

data class CharacterUiState(
    val character: Character,
    val currentIntervalType: IntervalType = IntervalType.FOCUS
)
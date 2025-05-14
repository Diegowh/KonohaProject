package com.diegowh.konohaproject.feature.character.presentation.model

import com.diegowh.konohaproject.core.animation.AnimationAction
import com.diegowh.konohaproject.feature.timer.domain.model.IntervalType

data class AnimationUiState(
    val action: AnimationAction? = null,
    val shouldUpdateFrames: Boolean = false,
    val currentIntervalType: IntervalType = IntervalType.FOCUS
)
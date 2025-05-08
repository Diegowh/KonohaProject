package com.diegowh.konohaproject.feature.timer.presentation.model

import com.diegowh.konohaproject.core.animation.AnimationAction

data class AnimationState(
    val action: AnimationAction? = null,
    val currentFrame: Int = 0,
    val isPaused: Boolean = false,
    val shouldUpdateFrames: Boolean = false
)
package com.diegowh.konohaproject.utils.animation

data class AnimationState(
    val currentFrame: Int,
    val isPaused: Boolean
)


sealed class AnimationAction {
    data class Start(val fromFrame: Int? = null) : AnimationAction()
    data object Pause : AnimationAction()
    data object Stop  : AnimationAction()
}
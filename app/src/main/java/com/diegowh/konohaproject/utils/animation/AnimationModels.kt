package com.diegowh.konohaproject.utils.animation

data class AnimationState(
    val currentFrame: Int,
    val isPaused: Boolean
)


sealed class AnimationAction {
    data object Start : AnimationAction()
    data object Pause : AnimationAction()
    data object Stop : AnimationAction()
}
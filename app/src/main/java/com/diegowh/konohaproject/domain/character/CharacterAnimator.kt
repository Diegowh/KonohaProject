package com.diegowh.konohaproject.domain.character

import android.widget.ImageView
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AnimationState(val currentFrame: Int, val isPaused: Boolean)

class CharacterAnimator(
    private val imageView: ImageView,
    private val frameResources: List<Int>,
    private val scope: LifecycleCoroutineScope,
    private var fps: Float = 1f
) {

    private var animationJob: Job? = null
    var currentFrame = 0
        private set
    private var isPaused = false

    fun start(startFrame: Int = 0) {
        stop()
        currentFrame = startFrame
        animationJob = scope.launch {
            while (isActive) {
                if (!isPaused) {
                    updateFrame()
                }
                delay((1000 / fps).toLong())
            }
        }
    }

    fun stop() {
        animationJob?.cancel()
        animationJob = null
        isPaused = false
        currentFrame = 0
        imageView.setImageResource(frameResources.first())
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun setFps(newFps: Float) {
        fps = newFps
        if (animationJob?.isActive == true) {
            start()
        }
    }

    private fun updateFrame() {
        imageView.apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = false
            setImageResource(frameResources[currentFrame])
        }
        currentFrame = (currentFrame + 1) % frameResources.size
    }
}
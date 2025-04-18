package com.diegowh.konohaproject.domain.character

import android.os.SystemClock
import android.widget.ImageView
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

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
        val framePeriod = (1000f / fps).toLong()
        animationJob = scope.launch {
            while (isActive) {
                val frameStart = SystemClock.elapsedRealtime()
                if (!isPaused) updateFrame()

                val workTime = SystemClock.elapsedRealtime() - frameStart
                delay(max(0, framePeriod - workTime))
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
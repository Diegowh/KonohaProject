package com.diegowh.konohaproject.character.presentation

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.diegowh.konohaproject.core.animation.AnimationAction
import com.diegowh.konohaproject.timer.domain.model.IntervalType
import com.diegowh.konohaproject.core.ui.CharacterTheme
import com.diegowh.konohaproject.character.domain.model.Character
import com.diegowh.konohaproject.timer.domain.model.TimerStatus
import com.diegowh.konohaproject.settings.application.usecases.ThemeManager


class CharacterAnimationManager(
    private val context: Context,
    private val imageView: ImageView
) {
    private lateinit var currentAnimation: AnimationDrawable
    private var focusAnimation: AnimationDrawable? = null
    private var breakAnimation: AnimationDrawable? = null
    
    private lateinit var currentTheme: CharacterTheme
    private var currentCharacterId: Int = -1
    
    fun initialize(character: Character) {
        if (!this::currentTheme.isInitialized || character.id != currentCharacterId) {
            currentCharacterId = character.id
            currentTheme = ThemeManager.loadTheme(context, character)
            resetAnimations()
            updateAnimationFrames(IntervalType.FOCUS)
        }
    }
    
    fun updateAnimationFrames(intervalType: IntervalType) {
        if (!this::currentTheme.isInitialized) return

        if (focusAnimation == null) {
            focusAnimation = createAnimationDrawable(currentTheme.focusFrames, currentTheme.frameDuration)
        }
        
        if (breakAnimation == null) {
            breakAnimation = createAnimationDrawable(currentTheme.breakFrames, currentTheme.frameDuration)
        }

        val newAnimation = if (intervalType == IntervalType.FOCUS) {
            focusAnimation!!
        } else {
            breakAnimation!!
        }

        if (!this::currentAnimation.isInitialized || currentAnimation != newAnimation) {
            currentAnimation = newAnimation
            imageView.setImageDrawable(currentAnimation)
        }
    }
    
    fun performAnimationAction(action: AnimationAction) {
        if (this::currentAnimation.isInitialized) {
            when (action) {
                AnimationAction.Start -> startAnimation()
                AnimationAction.Pause -> pauseAnimation()
                AnimationAction.Stop -> stopAnimation()
            }
        }
    }
    
    fun updateFromTimerStatus(status: TimerStatus) {
        if (this::currentAnimation.isInitialized) {
            when (status) {
                TimerStatus.Running -> if (!currentAnimation.isRunning) currentAnimation.start()
                TimerStatus.Paused, TimerStatus.Stopped -> currentAnimation.stop()
            }
        }
    }
    
    fun resetAnimations() {
        focusAnimation = null
        breakAnimation = null
    }
    
    fun release() {
        if (this::currentAnimation.isInitialized) {
            currentAnimation.stop()
        }
        focusAnimation = null
        breakAnimation = null
    }
    
    private fun createAnimationDrawable(frames: List<Drawable>, duration: Int): AnimationDrawable {
        return AnimationDrawable().apply {
            isOneShot = false
            frames.forEach { frame ->
                addFrame(frame, duration)
            }
        }
    }
    
    private fun startAnimation() {
        currentAnimation.stop() 
        currentAnimation.start()
    }
    
    private fun pauseAnimation() {
        currentAnimation.stop()
    }
    
    private fun stopAnimation() {
        currentAnimation.stop()
        currentAnimation.selectDrawable(0)
    }
    
    fun getCurrentTheme(): CharacterTheme {
        return if (this::currentTheme.isInitialized) {
            currentTheme
        } else {
            CharacterTheme(
                focusPalette = intArrayOf(),
                breakPalette = intArrayOf(),
                focusFrames = listOf(),
                breakFrames = listOf(),
                frameDuration = 0,
                character = null
            )
        }
    }
    
    fun getCurrentCharacterId(): Int = currentCharacterId
}
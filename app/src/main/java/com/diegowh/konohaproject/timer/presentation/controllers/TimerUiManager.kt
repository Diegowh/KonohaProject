package com.diegowh.konohaproject.timer.presentation.controllers

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.timer.domain.model.IntervalType
import com.diegowh.konohaproject.databinding.FragmentTimerBinding
import com.diegowh.konohaproject.timer.domain.model.TimerStatus
import com.diegowh.konohaproject.character.presentation.CharacterAnimationManager
import com.google.android.material.button.MaterialButton



class TimerUiManager(
    private val binding: FragmentTimerBinding,
    private val animationManager: CharacterAnimationManager
) {
    private val roundViews = mutableListOf<View>()
    
    fun updateTimerText(text: String) {
        binding.txtTimer.text = text
    }
    
    fun updateButtonVisibility(timerStatus: TimerStatus) {
        with(binding) {
            when (timerStatus) {
                TimerStatus.Running -> {
                    btnPlay.visibility = View.GONE
                    btnReset.visibility = View.GONE
                    btnPause.visibility = View.VISIBLE
                }
                TimerStatus.Paused, TimerStatus.Stopped -> {
                    btnPlay.visibility = View.VISIBLE
                    btnReset.visibility = View.VISIBLE
                    btnPause.visibility = View.GONE
                }
            }
        }
    }
    
    fun applyIntervalStyle(intervalType: IntervalType?) {
        val theme = animationManager.getCurrentTheme()
        val palette = when (intervalType) {
            IntervalType.FOCUS -> theme.focusPalette
            IntervalType.SHORT_BREAK, IntervalType.LONG_BREAK -> theme.breakPalette
            null -> theme.focusPalette
        }
        
        binding.main.setBackgroundColor(palette.first())
        updateButtonColors(palette)
    }
    
    fun updateRoundCounters(totalRounds: Int, currentRound: Int) {
        initRoundCounterViews(totalRounds)
        updateRoundUI(currentRound)
    }
    
    fun resetBackgroundColor() {
        val theme = animationManager.getCurrentTheme()
        binding.main.setBackgroundColor(theme.focusPalette[0])
        updateButtonColors(theme.focusPalette)
    }
    
    private fun updateButtonColors(palette: IntArray) {
        listOf(
            binding.btnPlay,
            binding.btnPause,
            binding.btnReset,
            binding.btnSettings,
            binding.btnCharacterSelect
        ).forEach { btn ->
            (btn as? MaterialButton)?.apply {
                backgroundTintList = ColorStateList.valueOf(palette[1])
            }
        }
    }
    
    private fun initRoundCounterViews(total: Int) {
        binding.roundCounterContainer.removeAllViews()
        roundViews.clear()
        
        val context = binding.root.context
        val size = context.resources.getDimensionPixelSize(R.dimen.round_indicator_size)
        val margin = context.resources.getDimensionPixelSize(R.dimen.round_indicator_margin)
        val theme = animationManager.getCurrentTheme()
        
        repeat(total) { idx ->
            View(context).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    if (idx < total - 1) marginEnd = margin
                }
                setBackgroundResource(R.drawable.round_button)
                
                ((background as? GradientDrawable)?.mutate() as? GradientDrawable)?.apply {
                    setColor(theme.focusPalette[1])
                }
                binding.roundCounterContainer.addView(this)
                roundViews.add(this)
            }
        }
    }
    
    private fun updateRoundUI(currentRound: Int) {
        val theme = animationManager.getCurrentTheme()
        val activeColor = theme.focusPalette[2]
        val inactiveColor = theme.focusPalette[1]
        
        roundViews.forEachIndexed { i, v ->
            ((v.background as? GradientDrawable)?.mutate() as GradientDrawable).apply {
                setColor(if (i < currentRound) activeColor else inactiveColor)
            }
        }
    }
}
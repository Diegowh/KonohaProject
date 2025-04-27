package com.diegowh.konohaproject.ui.timer

import android.content.res.ColorStateList
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.core.animation.AnimationAction
import com.diegowh.konohaproject.core.timer.IntervalType
import com.diegowh.konohaproject.core.ui.CharacterTheme
import com.diegowh.konohaproject.databinding.FragmentTimerBinding
import com.diegowh.konohaproject.domain.character.Character
import com.diegowh.konohaproject.domain.timer.TimerScreenEvent
import com.diegowh.konohaproject.domain.timer.TimerStatus
import com.diegowh.konohaproject.ui.character.CharacterSelectionFragment
import com.diegowh.konohaproject.ui.settings.SettingsFragment
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class TimerFragment : Fragment(R.layout.fragment_timer), SettingsFragment.Listener {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!
    val viewModel: TimerViewModel by viewModels({ requireActivity() })
    private val roundViews = mutableListOf<View>()
    private var currentCharacterId: Int = -1

    private lateinit var currentTheme: CharacterTheme
    private lateinit var charAnim: AnimationDrawable

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTimerBinding.bind(view)

        currentCharacterId = viewModel.state.value.character.id
        currentTheme = ThemeManager.loadTheme(requireContext(), viewModel.state.value.character)
        binding.main.setBackgroundColor(currentTheme.focusPalette.first())

        initComponents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun initAnimator() {
        updateAnimationFrames()
        charAnim.isOneShot = false
    }

    private fun initComponents() {
        initAnimator()
        observeViewModel()
        setupListeners()
        updateTimerUI(viewModel.state.value.timer)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { screenState ->
                        updateTimerUI(screenState.timer)
                        handleAnimations(screenState.animation)
                        handleCharacterChange(screenState.character)
                    }
                }
            }
        }
    }

    private fun updateTimerUI(state: TimerState) {
        binding.txtTimer.text = state.timerText
        updateButtonVisibility(state.status)

        if (::currentTheme.isInitialized) {
            updateRoundCounters(state.totalRounds, state.currentRound)
            applyIntervalStyle(state)
        } else {
            handleCharacterChange(viewModel.state.value.character)
            updateRoundCounters(state.totalRounds, state.currentRound)
            applyIntervalStyle(state)
        }
    }

    private fun handleCharacterChange(newCharacter: Character) {
        if (newCharacter.id != currentCharacterId) {
            currentCharacterId = newCharacter.id
            currentTheme = ThemeManager.loadTheme(requireContext(), newCharacter)

            updateAnimationFrames()

            val palette = currentTheme.focusPalette
            binding.main.setBackgroundColor(palette.first())
            updateButtonColors(palette)

            println("Personaje cambiado a: ${newCharacter.name}")

            // fuerza el cambio a la UI independientemente del estado del timer
            updateTimerUI(viewModel.state.value.timer)

            if (viewModel.state.value.timer.status == TimerStatus.Running) {
                startAnimation()
            }
        }
    }

    private fun handleAnimations(state: AnimationState) {
        state.action?.let { action ->
            when (action) {
                AnimationAction.Start -> startAnimation()
                AnimationAction.Pause -> pauseAnimation()
                AnimationAction.Stop -> stopAnimation()
            }
            viewModel.clearAnimationAction()
        }
        
        if (state.shouldUpdateFrames) {
            updateAnimationFrames()
            viewModel.clearAnimationAction()
        }
        
        manageAnimationState(state)
    }

    private fun updateAnimationFrames() {
        val currentIntervalType = viewModel.state.value.timer.interval?.type ?: IntervalType.FOCUS
        val frames = if (currentIntervalType == IntervalType.FOCUS) {
            currentTheme.focusFrames
        } else {
            currentTheme.breakFrames
        }
        val newAnim = AnimationDrawable().apply {
            isOneShot = false
            frames.forEach { frame ->
                addFrame(frame, currentTheme.frameDuration)
            }
        }
        binding.imgCharacter.setImageDrawable(newAnim)
        charAnim = newAnim
    }

    private fun manageAnimationState(state: AnimationState) {
        when {
            state.isPaused -> charAnim.stop()
            state.currentFrame > 0 -> charAnim.selectDrawable(state.currentFrame)
        }

        // sincroniza el estado de la animación con el servicio
        when (viewModel.state.value.timer.status) {
            TimerStatus.Running -> if (!charAnim.isRunning) charAnim.start()
            TimerStatus.Paused, TimerStatus.Stopped -> charAnim.stop()
        }
    }

    private fun startAnimation() {
        // para asegurarme de que la animacion se pare antes de iniciar otra
        charAnim.stop()
        charAnim.start()
    }

    private fun pauseAnimation() {
        charAnim.stop()
    }

    private fun stopAnimation() {
        charAnim.stop()
        charAnim.selectDrawable(0)
    }

    private fun updateButtonVisibility(timerStatus: TimerStatus) {
        with(binding) {
            when (timerStatus) {
                TimerStatus.Running -> {
                    btnPlay.visibility = View.GONE
                    btnReset.visibility = View.GONE
                    btnPause.visibility = View.VISIBLE
                }

                TimerStatus.Paused -> {
                    btnPlay.visibility = View.VISIBLE
                    btnReset.visibility = View.VISIBLE
                    btnPause.visibility = View.GONE
                }

                TimerStatus.Stopped -> {
                    btnPlay.visibility = View.VISIBLE
                    btnReset.visibility = View.VISIBLE
                    btnPause.visibility = View.GONE
                }
            }
        }
    }

    private fun applyIntervalStyle(state: TimerState) {

        val palette = when {

            state.interval != null -> {
                if (state.interval.type == IntervalType.FOCUS) currentTheme.focusPalette
                else currentTheme.breakPalette
            }

            else -> currentTheme.focusPalette
        }

        binding.main.setBackgroundColor(palette.first())

        updateButtonColors(palette)
    }

    private fun updateRoundCounters(totalRounds: Int, currentRound: Int) {
        initRoundCounterViews(totalRounds)
        updateRoundUI(currentRound)
    }


    private fun setupListeners() {
        binding.btnPlay.setOnClickListener {
            viewModel.onEvent(TimerScreenEvent.TimerEvent.Play)
        }
        binding.btnPause.setOnClickListener {
            viewModel.onEvent(TimerScreenEvent.TimerEvent.Pause)
        }
        binding.btnReset.setOnClickListener {
            viewModel.onEvent(TimerScreenEvent.TimerEvent.Reset)
        }
        binding.btnSettings.setOnClickListener {
            binding.btnSettings.isEnabled = false
            viewModel.onEvent(TimerScreenEvent.TimerEvent.Pause)
            SettingsFragment().show(childFragmentManager, "SettingsDialog")
        }
        binding.btnCharacterSelect.setOnClickListener {
            CharacterSelectionFragment().show(childFragmentManager, "CharacterSelector")
        }
    }

    private fun resetBackgroundColor() {
        binding.main.setBackgroundColor(currentTheme.focusPalette[0])
        updateButtonColors(currentTheme.focusPalette)
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
//                iconTint = ColorStateList.valueOf(palette[2])
            }
        }
    }

    private fun initRoundCounterViews(total: Int) {
        binding.roundCounterContainer.removeAllViews()
        roundViews.clear()

        val size = resources.getDimensionPixelSize(R.dimen.round_indicator_size)
        val margin = resources.getDimensionPixelSize(R.dimen.round_indicator_margin)

        repeat(total) { idx ->
            View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    if (idx < total - 1) marginEnd = margin
                }
                setBackgroundResource(R.drawable.round_button)

                ((background as? GradientDrawable)?.mutate() as? GradientDrawable)?.apply {
                    setColor(currentTheme.focusPalette[1])
                }
                binding.roundCounterContainer.addView(this)
                roundViews.add(this)
            }
        }
    }

    private fun updateRoundUI(cycle: Int) {
        val activeColor = currentTheme.focusPalette[2]
        val inactiveColor = currentTheme.focusPalette[1]

        roundViews.forEachIndexed { i, v ->
            ((v.background as? GradientDrawable)?.mutate() as GradientDrawable).apply {
                setColor(if (i < cycle) activeColor else inactiveColor)
            }
        }
    }

    override fun onSettingsChanged() {
        viewModel.onEvent(TimerScreenEvent.TimerEvent.Reset)
        resetBackgroundColor()
    }

    override fun onDismiss() {
        binding.btnSettings.isEnabled = true
    }
}
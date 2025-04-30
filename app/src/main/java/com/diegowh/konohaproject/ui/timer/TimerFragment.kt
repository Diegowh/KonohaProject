package com.diegowh.konohaproject.ui.timer

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.databinding.FragmentTimerBinding
import com.diegowh.konohaproject.domain.character.Character
import com.diegowh.konohaproject.domain.timer.TimerScreenEvent
import com.diegowh.konohaproject.domain.timer.TimerStatus
import com.diegowh.konohaproject.ui.character.CharacterSelectionFragment
import com.diegowh.konohaproject.ui.settings.SettingsFragment
import kotlinx.coroutines.launch


class TimerFragment : Fragment(R.layout.fragment_timer) {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    private var lastClickTime: Long = 0
    val viewModel: TimerViewModel by viewModels({ requireActivity() })

    private lateinit var animationManager: CharacterAnimationManager
    private lateinit var uiManager: TimerUiManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTimerBinding.bind(view)

        animationManager = CharacterAnimationManager(requireContext(), binding.imgCharacter)
        animationManager.initialize(viewModel.state.value.character)

        uiManager = TimerUiManager(binding, animationManager)

        initComponents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        animationManager.release()
        _binding = null
    }

    private fun initComponents() {
        observeViewModel()
        setupListeners()
        updateUiFromState(viewModel.state.value)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        updateUiFromState(state)
                    }
                }
            }
        }
    }

    private fun updateUiFromState(state: TimerScreenState) {

        processAnimationState(state.animation)

        if (state.character.id != animationManager.getCurrentCharacterId()) {
            handleCharacterChange(state.character)
        }
        updateTimerUI(state.timer)
    }

    private fun processAnimationState(state: AnimationState) {
        state.action?.let { action ->
            animationManager.performAnimationAction(action)
            viewModel.clearAnimationAction()
        }

        if (state.shouldUpdateFrames) {
            val currentIntervalType = viewModel.state.value.timer.interval?.type
            if (currentIntervalType != null) {
                animationManager.updateAnimationFrames(currentIntervalType)
            }
            viewModel.clearAnimationAction()
        }
    }

    private fun handleCharacterChange(newCharacter: Character) {
        animationManager.initialize(newCharacter)

        updateTimerUI(viewModel.state.value.timer)

        if (viewModel.state.value.timer.status == TimerStatus.Running) {
            animationManager.performAnimationAction(com.diegowh.konohaproject.core.animation.AnimationAction.Start)
        }
    }

    private fun updateTimerUI(state: TimerState) {
        uiManager.updateTimerText(state.timerText)
        uiManager.updateButtonVisibility(state.status)
        uiManager.updateRoundCounters(state.totalRounds, state.currentRound)

        uiManager.applyIntervalStyle(state.interval?.type)

        animationManager.updateFromTimerStatus(state.status)
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
            if (SystemClock.elapsedRealtime() - lastClickTime > 1000) {
                viewModel.onEvent(TimerScreenEvent.TimerEvent.Pause)
                SettingsFragment().show(childFragmentManager, "SettingsDialog")
                lastClickTime = SystemClock.elapsedRealtime()
            }
        }
        binding.btnCharacterSelect.setOnClickListener {
            if (SystemClock.elapsedRealtime() - lastClickTime > 1000) {
                CharacterSelectionFragment().show(childFragmentManager, "CharacterSelector")
                lastClickTime = SystemClock.elapsedRealtime()
            }
        }
    }

}
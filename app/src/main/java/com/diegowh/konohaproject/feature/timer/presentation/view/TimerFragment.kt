package com.diegowh.konohaproject.feature.timer.presentation.view

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.feature.timer.domain.model.IntervalType
import com.diegowh.konohaproject.databinding.FragmentTimerBinding
import com.diegowh.konohaproject.feature.character.domain.model.Character
import com.diegowh.konohaproject.feature.timer.domain.model.TimerStatus
import com.diegowh.konohaproject.feature.settings.presentation.view.SettingsFragment
import com.diegowh.konohaproject.feature.character.presentation.controller.CharacterAnimationManager
import com.diegowh.konohaproject.feature.character.presentation.view.CharacterSelectionFragment
import com.diegowh.konohaproject.feature.timer.presentation.model.AnimationState
import com.diegowh.konohaproject.feature.timer.presentation.viewmodel.TimerEvent
import com.diegowh.konohaproject.feature.timer.presentation.model.TimerScreenState
import com.diegowh.konohaproject.feature.timer.presentation.model.TimerState
import com.diegowh.konohaproject.feature.timer.presentation.controller.TimerUiManager
import com.diegowh.konohaproject.feature.timer.presentation.viewmodel.TimerViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        state.intervalDialog.intervalType?.let { intervalType ->
            if (state.intervalDialog.showDialog) {
                showIntervalFinishedDialog(
                    intervalType = intervalType,
                    onTakeBreak = {
                        viewModel.onDialogContinueClicked()
                    },
                    onSkipBreak = {
                        viewModel.onDialogSkipClicked()
                    }
                )
            }
        }
        if (state.sessionDialogVisible) {
            showSessionFinishedDialog()
            viewModel.onSessionDialogDismissed()
        }
    }

    private fun showIntervalFinishedDialog(
        intervalType: IntervalType,
        onTakeBreak: () -> Unit,
        onSkipBreak: () -> Unit
    ) {

        if (intervalType == IntervalType.SHORT_BREAK || intervalType == IntervalType.LONG_BREAK) {
            val options = arrayOf("Take a break", "Skip break")
            var selectedOption = 0


            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Well done! What's next?")
                .setSingleChoiceItems(options, selectedOption) { _, which ->
                    selectedOption = which
                }
                .setPositiveButton("Confirm") { dialog, _ ->
                    if (selectedOption == 0) {
                        onTakeBreak()
                    } else {
                        onSkipBreak()
                    }
                    dialog.dismiss()
                }
                .setCancelable(false)
                .create()
                .show()

            viewModel.onDialogShown()
        }
    }

    private fun showSessionFinishedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Congratulations!")
            .setMessage("You finished a session!")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
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
            viewModel.onEvent(TimerEvent.TimerAction.Play)
        }
        binding.btnPause.setOnClickListener {
            viewModel.onEvent(TimerEvent.TimerAction.Pause)
        }
        binding.btnReset.setOnClickListener {
            viewModel.onEvent(TimerEvent.TimerAction.Reset)
        }
        binding.btnSettings.setOnClickListener {
            if (SystemClock.elapsedRealtime() - lastClickTime > 1000) {
                viewModel.onEvent(TimerEvent.TimerAction.Pause)
                SettingsFragment().show(childFragmentManager, "SettingsFragment")
                lastClickTime = SystemClock.elapsedRealtime()
            }
        }
        binding.btnCharacterSelect.setOnClickListener {
            if (SystemClock.elapsedRealtime() - lastClickTime > 1000) {
                CharacterSelectionFragment().show(childFragmentManager, "CharacterSelectionFragment")
                lastClickTime = SystemClock.elapsedRealtime()
            }
        }
    }

}
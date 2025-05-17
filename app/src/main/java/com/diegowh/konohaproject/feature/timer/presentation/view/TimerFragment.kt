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
import com.diegowh.konohaproject.core.animation.AnimationAction
import com.diegowh.konohaproject.databinding.FragmentTimerBinding
import com.diegowh.konohaproject.feature.character.presentation.controller.CharacterAnimationManager
import com.diegowh.konohaproject.feature.character.presentation.model.AnimationUiState
import com.diegowh.konohaproject.feature.character.presentation.model.CharacterUiState
import com.diegowh.konohaproject.feature.character.presentation.view.CharacterSelectionFragment
import com.diegowh.konohaproject.feature.settings.presentation.view.SettingsFragment
import com.diegowh.konohaproject.feature.timer.domain.model.IntervalType
import com.diegowh.konohaproject.feature.timer.domain.model.TimerStatus
import com.diegowh.konohaproject.feature.timer.presentation.controller.TimerUiManager
import com.diegowh.konohaproject.feature.timer.presentation.model.ScreenUiState
import com.diegowh.konohaproject.feature.timer.presentation.model.TimerUiState
import com.diegowh.konohaproject.feature.timer.presentation.viewmodel.TimerEvent
import com.diegowh.konohaproject.feature.timer.presentation.viewmodel.TimerViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class TimerFragment : Fragment(R.layout.fragment_timer) {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    private var lastClickTime = 0L

    private var debugClicks: Int = 0
    private var lastDebugClickTime = 0L
    private val DEBUG_CLICK_INTERVAL = 500L

    val viewModel: TimerViewModel by viewModels({ requireActivity() })

    private lateinit var animationManager: CharacterAnimationManager
    private lateinit var uiManager: TimerUiManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTimerBinding.bind(view)

        val initialCharacter = viewModel.characterState.value.character

        animationManager = CharacterAnimationManager(requireContext(), binding.imgCharacter)
        animationManager.initialize(initialCharacter)

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

        // Inicializa la UI basandose en el estado actual
        updateUiFromSpecializedState(
            viewModel.timerState.value,
            viewModel.characterState.value,
            viewModel.animationState.value,
            viewModel.screenState.value
        )
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Timer
                launch {
                    viewModel.timerState.collect { state ->
                        updateTimerUI(state)
                    }
                }

                // Estado del personaje (Character e IntervalType)
                launch {
                    viewModel.characterState.collect { state ->
                        handleCharacterStateChange(state)
                    }
                }

                // Animacion
                launch {
                    viewModel.animationState.collect { state ->
                        processAnimationState(state)
                    }
                }

                // State de la pantalla
                launch {
                    viewModel.screenState.collect { state ->
                        processScreenState(state)
                    }
                }
            }
        }
    }

    private fun processScreenState(state: ScreenUiState) {
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

    private fun processAnimationState(state: AnimationUiState) {
        state.action?.let { action ->
            animationManager.performAnimationAction(action)
            viewModel.clearAnimationAction()
        }

        if (state.shouldUpdateFrames) {
            val currentIntervalType = state.currentIntervalType
            animationManager.updateAnimationFrames(currentIntervalType)
            viewModel.clearAnimationAction()
        }
    }

    private fun handleCharacterStateChange(state: CharacterUiState) {
        val currentCharacterId = animationManager.getCurrentCharacterId()
        if (state.character.id != currentCharacterId) {
            animationManager.initialize(state.character)

            val intervalType = state.currentIntervalType
            animationManager.updateAnimationFrames(intervalType)
            uiManager.applyIntervalStyle(intervalType)

            val currentRound = viewModel.timerState.value.currentRound
            uiManager.updateRoundUI(currentRound)

            // inicia animacion si esta Running unicamente
            val timerStatus = viewModel.timerState.value.status
            if (timerStatus == TimerStatus.Running) {
                animationManager.performAnimationAction(AnimationAction.Start)
            }
        }
    }

    private fun updateUiFromSpecializedState(
        timerState: TimerUiState,
        characterState: CharacterUiState,
        animationState: AnimationUiState,
        screenState: ScreenUiState
    ) {
        processAnimationState(animationState)
        handleCharacterStateChange(characterState)
        updateTimerUI(timerState)
        processScreenState(screenState)
    }

    private fun updateTimerUI(state: TimerUiState) {
        uiManager.updateTimerText(state.timerText)
        uiManager.updateButtonVisibility(state.status)
        uiManager.updateRoundCounters(state.totalRounds, state.currentRound)

        uiManager.applyIntervalStyle(state.interval?.type)

        animationManager.updateFromTimerStatus(state.status)
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
                CharacterSelectionFragment().show(
                    childFragmentManager,
                    "CharacterSelectionFragment"
                )
                lastClickTime = SystemClock.elapsedRealtime()
            }
        }

        binding.debugTriggerArea.setOnClickListener {
            val now = SystemClock.elapsedRealtime()

            if (now - lastDebugClickTime > DEBUG_CLICK_INTERVAL) {
                debugClicks = 1
            } else {
                debugClicks += 1
            }

            lastDebugClickTime = now

            if (debugClicks == 5) {
                debugClicks = 0
                viewModel.onEvent(TimerEvent.SettingsAction.ToggleDebug)
            }
        }
    }
}
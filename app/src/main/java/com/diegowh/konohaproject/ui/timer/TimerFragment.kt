package com.diegowh.konohaproject.ui.timer

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.databinding.FragmentTimerBinding
import com.diegowh.konohaproject.domain.sound.SoundPlayer
import com.diegowh.konohaproject.domain.timer.TimerState
import com.diegowh.konohaproject.ui.character.CharacterSelectionFragment
import com.diegowh.konohaproject.ui.settings.SettingsFragment
import com.diegowh.konohaproject.utils.animation.AnimationAction
import com.diegowh.konohaproject.utils.sound.SoundType
import com.diegowh.konohaproject.utils.timer.IntervalType
import kotlinx.coroutines.launch

class TimerFragment : Fragment(R.layout.fragment_timer), SettingsFragment.Listener {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TimerViewModel by viewModels({ requireActivity() })
    private lateinit var soundPlayer: SoundPlayer

    private val roundViews = mutableListOf<View>()

    private lateinit var charAnim: AnimationDrawable

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTimerBinding.bind(view)

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner) { /* nada */ }

        initSoundPlayer()
        initAnimator()
        initComponents()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        soundPlayer.release()
        _binding = null
    }

    private fun initSoundPlayer() {
        soundPlayer = SoundPlayer(requireContext()).apply {
            loadSound(SoundType.INTERVAL_CHANGE, R.raw.bubble_tiny)
        }
    }

    private fun initAnimator() {

        charAnim = binding.imgCharacter.drawable as AnimationDrawable
        charAnim.isOneShot = false

    }

    private fun initComponents() {
        observeViewModel()
        setupListeners()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.uiState.collect { state ->
                    updateMainUI(state)
                    handleAnimationLogic(state)
                }
            }
        }
        observeSoundEvents()
    }

    private fun updateMainUI(state: TimerUIState) {
        binding.txtTimer.text = state.timerText
        updateButtonVisibility(state.state)
        updateBackgroundAndRounds(state)
        updateRoundCounters(state.totalRounds, state.currentRound)
    }

    private fun updateButtonVisibility(timerState: TimerState) {
        with(binding) {
            when (timerState) {
                TimerState.Running -> {
                    btnPlay.visibility = View.GONE
                    btnReset.visibility = View.GONE
                    btnPause.visibility = View.VISIBLE
                }

                TimerState.Paused -> {
                    btnPlay.visibility = View.VISIBLE
                    btnReset.visibility = View.VISIBLE
                    btnPause.visibility = View.GONE
                }

                TimerState.Stopped -> {
                    btnPlay.visibility = View.VISIBLE
                    btnReset.visibility = View.VISIBLE
                    btnPause.visibility = View.GONE
                    resetBackgroundColor()
                }
            }
        }
    }

    private fun updateBackgroundAndRounds(state: TimerUIState) {
        state.interval?.let { interval ->
            val isFocus = interval.type == IntervalType.FOCUS
            binding.main.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isFocus) R.color.sakura_focus_primary
                    else R.color.sakura_break_primary
                )
            )
        }
    }

    private fun updateRoundCounters(totalRounds: Int, currentRound: Int) {
        initRoundCounterViews(totalRounds)
        updateRoundUI(currentRound)
    }

    private fun handleAnimationLogic(state: TimerUIState) {

        state.animationAction?.let { action ->
            when (action) {
                is AnimationAction.Start -> {
                    charAnim.start()
                }

                AnimationAction.Pause -> {
                    charAnim.stop()
                }

                AnimationAction.Stop -> {
                    charAnim.stop()
                    charAnim.selectDrawable(0)
                }
            }
            viewModel.clearAnimationAction()
        }
    }

    private fun observeSoundEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.intervalSoundEvent.collect { type ->
                    if (type == SoundType.INTERVAL_CHANGE) soundPlayer.play(type)
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnPlay.setOnClickListener { viewModel.onPlayClicked() }
        binding.btnPause.setOnClickListener { viewModel.onPauseClicked() }
        binding.btnReset.setOnClickListener { viewModel.onResetClicked() }
        binding.btnSettings.setOnClickListener {
            binding.btnSettings.isEnabled = false
            viewModel.onPauseClicked()
            SettingsFragment().show(childFragmentManager, "SettingsDialog")
        }
        binding.btnCharacterSelect.setOnClickListener {
            CharacterSelectionFragment().show(childFragmentManager, "CharacterSelector")
        }
    }

    private fun resetBackgroundColor() {
        binding.main.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.sakura_focus_primary)
        )
    }

    private fun initRoundCounterViews(total: Int) {
        binding.roundCounterContainer.removeAllViews()
        roundViews.clear()

        val size = 31 //31px son 12dp pero asi me ahorraba tener una funcion para transformar a px
        val margin = 31

        repeat(total) { idx ->
            View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    if (idx < total - 1) marginEnd = margin
                }
                setBackgroundResource(R.drawable.round_button)
                backgroundTintList =
                    ContextCompat.getColorStateList(context, R.color.sakura_focus_secondary)

                binding.roundCounterContainer.addView(this)
                roundViews.add(this)
            }
        }
    }

    private fun updateRoundUI(cycle: Int) {
        val active = ContextCompat.getColorStateList(requireContext(), R.color.sakura_focus_tertiary)
        val inactive = ContextCompat.getColorStateList(requireContext(), R.color.sakura_focus_secondary)
        roundViews.forEachIndexed { i, v ->
            v.backgroundTintList = if (i < cycle) active else inactive
        }
    }

    override fun onSettingsChanged() {
        viewModel.onResetClicked()
        resetBackgroundColor()
    }

    override fun onDismiss() {
        binding.btnSettings.isEnabled = true
    }
}
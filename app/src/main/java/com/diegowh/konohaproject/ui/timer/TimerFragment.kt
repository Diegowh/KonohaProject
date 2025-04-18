package com.diegowh.konohaproject.ui.timer

import android.animation.ValueAnimator
import android.content.res.Resources
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.databinding.FragmentTimerBinding
import com.diegowh.konohaproject.domain.character.CharacterAnimator
import com.diegowh.konohaproject.domain.sound.SoundPlayer
import com.diegowh.konohaproject.domain.timer.TimerState
import com.diegowh.konohaproject.ui.components.ArcProgressDrawable
import com.diegowh.konohaproject.ui.settings.SettingsFragment
import com.diegowh.konohaproject.utils.SoundType
import kotlinx.coroutines.launch

class TimerFragment : Fragment(R.layout.fragment_timer), SettingsFragment.Listener {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TimerViewModel by viewModels({ requireActivity() })
    private lateinit var soundPlayer: SoundPlayer

    private var progressAnimator: ValueAnimator? = null
    private var currentProgress: Int = 0
    private val roundViews = mutableListOf<View>()

    private var animator: CharacterAnimator? = null
    private val animationFrames = listOf(
        R.drawable.sakura_crop,
        R.drawable.losiento1,
        R.drawable.losiento2,
        R.drawable.losiento3,
        R.drawable.losiento4
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTimerBinding.bind(view)

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner) { /* nada */ }


        soundPlayer = SoundPlayer(requireContext()).apply {
            loadSound(SoundType.INTERVAL_CHANGE, R.raw.bubble_tiny)
        }

        animator = CharacterAnimator(
            binding.imgCharacter,
            animationFrames,
            viewLifecycleOwner.lifecycleScope,
        )

        initProgressArc()
        observeViewModel()
        setupListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        soundPlayer.release()
        animator?.stop()
        animator = null
        _binding = null
    }

    private fun observeViewModel() {
        viewModel.timerText.observe(viewLifecycleOwner) { binding.txtTimer.text = it }

        viewModel.timerState.observe(viewLifecycleOwner) { state ->
            with(binding) {
                when (state) {
                    TimerState.Running -> {
                        btnPlay.visibility = View.GONE
                        btnReset.visibility = View.GONE
                        btnPause.visibility = View.VISIBLE
                    }
                    TimerState.Paused -> {
                        btnPlay.visibility = View.VISIBLE
                        btnReset.visibility = View.VISIBLE
                        btnPause.visibility = View.GONE
                        pauseProgressAnimation()

                    }
                    TimerState.Stopped -> {
                        btnPlay.visibility = View.VISIBLE
                        btnReset.visibility = View.VISIBLE
                        btnPause.visibility = View.GONE
                        resetProgressAnimation()
                        resetBackgroundColor()
                    }
                }
            }
        }

        viewModel.interval.observe(viewLifecycleOwner) { interval ->
            resetProgressAnimation()
            startProgressAnimation(interval.nextDuration)
            binding.main.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (interval.isFocus) R.color.background_app_focus
                    else R.color.background_app_break
                )
            )
            if (interval.isFocus) updateRoundUI(interval.currentRound)
        }

        viewModel.totalRounds.observe(viewLifecycleOwner) { initRoundCounterViews(it) }
        viewModel.currentRound.observe(viewLifecycleOwner) { updateRoundUI(it) }

        viewModel.resumedTime.observe(viewLifecycleOwner) { startProgressAnimation(it) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.intervalSoundEvent.collect { type ->
                    if (type == SoundType.INTERVAL_CHANGE) soundPlayer.play(type)
                }
            }
        }

        viewModel.animationAction.observe(viewLifecycleOwner) { action ->
            when (action) {
                is AnimationAction.Start -> {
                    action.fromFrame?.let { animator?.start(it) } ?: animator?.start()
                }
                AnimationAction.Pause -> {
                    animator?.pause()

                    val cf = animator?.currentFrame ?: 0
                    viewModel.updateAnimationState(cf, isPaused = true)
                }
                AnimationAction.Stop -> {
                    animator?.stop()
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
    }


    private fun resetBackgroundColor() {
        binding.main.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.background_app_focus)
        )
    }

    private fun initRoundCounterViews(total: Int) {
        binding.roundCounterContainer.removeAllViews()
        roundViews.clear()

        val size = 12.dpToPx()
        val margin = 12.dpToPx()

        repeat(total) { idx ->
            View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    if (idx < total - 1) marginEnd = margin
                }
                setBackgroundResource(R.drawable.round_button)
                backgroundTintList =
                    ContextCompat.getColorStateList(context, R.color.button_secondary)

                binding.roundCounterContainer.addView(this)
                roundViews.add(this)
            }
        }
    }

    private fun initProgressArc() {
        binding.progressBar.apply {
            progressDrawable = ArcProgressDrawable(context = requireContext())
            max = 10_000
            progress = 0
        }
    }

    private fun startProgressAnimation(duration: Long) {
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofInt(currentProgress, 10_000).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            addUpdateListener { anim -> binding.progressBar.progress = anim.animatedValue as Int }
            start()
        }
    }

    private fun pauseProgressAnimation() = progressAnimator?.run {
        currentProgress = animatedValue as Int
        cancel()
    }

    private fun resetProgressAnimation() {
        progressAnimator?.cancel()
        currentProgress = 0
        binding.progressBar.progress = 0
    }

    private fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    private fun updateRoundUI(cycle: Int) {
        val active = ContextCompat.getColorStateList(requireContext(), R.color.button_primary)
        val inactive = ContextCompat.getColorStateList(requireContext(), R.color.button_secondary)
        roundViews.forEachIndexed { i, v -> v.backgroundTintList = if (i < cycle) active else inactive }
    }

    override fun onSettingsChanged() {
        viewModel.onResetClicked()
        resetBackgroundColor()
    }

    override fun onDismiss() {
        binding.btnSettings.isEnabled = true
    }
}
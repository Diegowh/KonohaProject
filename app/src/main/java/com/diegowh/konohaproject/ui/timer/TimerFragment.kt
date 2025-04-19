package com.diegowh.konohaproject.ui.timer

import android.animation.ValueAnimator
import android.content.res.Resources
import android.graphics.drawable.AnimationDrawable
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
import com.diegowh.konohaproject.domain.sound.SoundPlayer
import com.diegowh.konohaproject.domain.timer.TimerState
import com.diegowh.konohaproject.ui.components.ArcProgressDrawable
import com.diegowh.konohaproject.ui.settings.SettingsFragment
import com.diegowh.konohaproject.utils.sound.SoundType
import com.diegowh.konohaproject.utils.animation.AnimationAction
import com.diegowh.konohaproject.utils.timer.IntervalType
import kotlinx.coroutines.launch

class TimerFragment : Fragment(R.layout.fragment_timer), SettingsFragment.Listener {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TimerViewModel by viewModels({ requireActivity() })
    private lateinit var soundPlayer: SoundPlayer

    private var progressAnimator: ValueAnimator? = null
    private var currentProgress: Int = 0
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
        binding.imgCharacter.post {
            charAnim = binding.imgCharacter.drawable as AnimationDrawable
            charAnim.isOneShot = false
        }
    }

    private fun initComponents() {
//        initProgressArc()
        observeViewModel()
        setupListeners()
    }

    private fun observeViewModel() {
        observeTimerText()
        observeTimerState()
        observeIntervalChange()
        observeRoundCounters()
        observeResumedTime()
        observeSoundEvents()
        observeAnimationActions()
    }

    private fun observeAnimationActions() {
        viewModel.animationAction.observe(viewLifecycleOwner) { action ->
            when (action) {
                is AnimationAction.Start -> { handleAnimationStart(action) }
                AnimationAction.Pause -> { handleAnimationPause() }
                AnimationAction.Stop -> { handleAnimationStop() }
            }
        }
    }

    private fun handleAnimationStop() {
        charAnim.stop()
        charAnim.selectDrawable(0)
    }

    private fun handleAnimationPause() {
        val cf = getCurrentFrameIndex(charAnim)
        charAnim.stop()
        viewModel.updateAnimationState(cf, isPaused = true)
    }

    private fun handleAnimationStart(action: AnimationAction.Start) {
        action.fromFrame?.let { charAnim.selectDrawable(it) }
        charAnim.start()
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

    private fun observeResumedTime() {
//        viewModel.resumedTime.observe(viewLifecycleOwner) { startProgressAnimation(it) }
    }

    private fun observeRoundCounters() {
        viewModel.totalRounds.observe(viewLifecycleOwner) { initRoundCounterViews(it) }
        viewModel.currentRound.observe(viewLifecycleOwner) { updateRoundUI(it) }
    }

    private fun observeIntervalChange() {
        viewModel.interval.observe(viewLifecycleOwner) { interval ->
//            resetProgressAnimation()
//            startProgressAnimation(interval.nextDuration)
            val isFocus = (interval.type == IntervalType.FOCUS)
            binding.main.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isFocus) R.color.background_app_focus
                    else R.color.background_app_break
                )
            )
            if (isFocus) updateRoundUI(interval.currentRound)
        }
    }

    private fun observeTimerText() {
        viewModel.timerText.observe(viewLifecycleOwner) { binding.txtTimer.text = it }
    }

    private fun observeTimerState() {
        viewModel.timerState.observe(viewLifecycleOwner) { state ->
            when (state) {
                TimerState.Running -> showRunningState()
                TimerState.Paused -> showPausedState()
                TimerState.Stopped -> showStoppedState()
            }
        }
    }

    private fun showRunningState() = with(binding) {
        btnPlay.visibility = View.GONE
        btnReset.visibility = View.GONE
        btnPause.visibility = View.VISIBLE
    }

    private fun showPausedState() = with(binding) {
        btnPlay.visibility = View.VISIBLE
        btnReset.visibility = View.VISIBLE
        btnPause.visibility = View.GONE
//        pauseProgressAnimation()
    }

    private fun showStoppedState() = with(binding) {
        btnPlay.visibility = View.VISIBLE
        btnReset.visibility = View.VISIBLE
        btnPause.visibility = View.GONE
//        resetProgressAnimation()
        resetBackgroundColor()
    }

    private fun getCurrentFrameIndex(anim: AnimationDrawable): Int {
        val current = anim.current
        for (i in 0 until anim.numberOfFrames) {
            if (anim.getFrame(i) == current) {
                return i
            }
        }
        return 0
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

        val size = 31 //31px son 12dp pero asi me ahorraba tener una funcion para transformar a px
        val margin = 31

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

//    private fun initProgressArc() {
//        binding.progressBar.apply {
//            progressDrawable = ArcProgressDrawable(context = requireContext())
//            max = 10_000
//            progress = 0
//        }
//    }

//    private fun startProgressAnimation(duration: Long) {
//        progressAnimator?.cancel()
//        progressAnimator = ValueAnimator.ofInt(currentProgress, 10_000).apply {
//            this.duration = duration
//            interpolator = LinearInterpolator()
//            addUpdateListener { anim -> binding.progressBar.progress = anim.animatedValue as Int }
//            start()
//        }
//    }

//    private fun pauseProgressAnimation() = progressAnimator?.run {
//        currentProgress = animatedValue as Int
//        cancel()
//    }

//    private fun resetProgressAnimation() {
//        progressAnimator?.cancel()
//        currentProgress = 0
//        binding.progressBar.progress = 0
//    }

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
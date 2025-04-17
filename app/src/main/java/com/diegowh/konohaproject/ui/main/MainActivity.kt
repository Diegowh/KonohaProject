package com.diegowh.konohaproject.ui.main

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.domain.timer.TimerState
import com.diegowh.konohaproject.domain.timer.TimerSettings
import com.diegowh.konohaproject.databinding.ActivityMainBinding
import com.diegowh.konohaproject.ui.components.ArcProgressDrawable
import com.diegowh.konohaproject.ui.settings.SettingsFragment
import com.diegowh.konohaproject.utils.SoundType
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), SettingsFragment.SettingsListener {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    private var progressAnimator: ValueAnimator? = null
    private var currentProgress: Int = 0
    private val roundViews = mutableListOf<View>()

    private var soundPool: SoundPool? = null
    private var soundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        soundPool = SoundPool.Builder().setMaxStreams(2).build()
        soundId = soundPool?.load(this, R.raw.bubble_tiny, 1) ?: 0

        initProgressArc()
        initRoundCounterViews()

        viewModel.timerText.observe(this) { timeText ->
            binding.txtTimer.text = timeText
        }

        viewModel.timerState.observe(this) { state ->
            when (state) {
                TimerState.Running -> {
                    binding.btnPlay.visibility = View.GONE
                    binding.btnReset.visibility = View.GONE
                    binding.btnPause.visibility = View.VISIBLE
                }
                TimerState.Paused -> {
                    binding.btnPlay.visibility = View.VISIBLE
                    binding.btnReset.visibility = View.VISIBLE
                    binding.btnPause.visibility = View.GONE
                    pauseProgressAnimation()
                }
                TimerState.Stopped -> {
                    binding.btnPlay.visibility = View.VISIBLE
                    binding.btnReset.visibility = View.VISIBLE
                    binding.btnPause.visibility = View.GONE
                    binding.txtTimer.text = TimerSettings.initialDisplayTime(this, true)
                    resetProgressAnimation()
                }
            }
        }

        viewModel.interval.observe(this) { interval ->

            resetProgressAnimation()
            startProgressAnimation(interval.nextDuration)
            val backgroundColor = if (interval.isFocus) {
                ContextCompat.getColor(this, R.color.background_app_focus)
            } else {
                ContextCompat.getColor(this, R.color.background_app_break)
            }
            binding.main.setBackgroundColor(backgroundColor)

            if (interval.isFocus) {
                updateRoundUI(interval.currentRound)

                if (interval.currentRound == 0) {
                    viewModel.onResetClicked()
                }
            }
        }
        viewModel.currentRound.observe(this) { currentRound ->
            updateRoundUI(currentRound)
        }

        viewModel.resumedTime.observe(this) { remaining ->
            startProgressAnimation(remaining)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.intervalSoundEvent.collect() { soundType ->
                    when (soundType) {
                        SoundType.INTERVAL_CHANGE -> playIntervalChangeSound()
                        SoundType.BUTTON_CLICK -> TODO()
                    }
                }
            }
        }

        binding.btnPlay.setOnClickListener {
            viewModel.onPlayClicked()
        }
        binding.btnPause.setOnClickListener {
            viewModel.onPauseClicked()
        }
        binding.btnReset.setOnClickListener {
            resetBackgroundColor()
            viewModel.onResetClicked()
        }
        binding.btnSettings.setOnClickListener {
            binding.btnSettings.isEnabled = false // Lo desactivo para evitar dobles pulsaciones
            // Al abrir los settings se pausa el temporizador
            viewModel.onPauseClicked()
            SettingsFragment.newInstance().show(supportFragmentManager, "SettingsDialog")
        }
    }

    private fun playIntervalChangeSound() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()

        soundPool?.play(soundId, volume, volume, 1, 0, 1.0f)
    }

    private fun resetBackgroundColor() {
        binding.main.setBackgroundColor(ContextCompat.getColor(this, R.color.background_app_focus))
    }

    private fun initRoundCounterViews() {
        val totalRounds = TimerSettings.getTotalRounds(this)
        binding.roundCounterContainer.removeAllViews()
        roundViews.clear()

        val sizeInPx = 12.dpToPx()
        val marginInPx = 12.dpToPx()

        repeat(totalRounds) { index ->
            View(this).apply {
                layoutParams = LinearLayout.LayoutParams(sizeInPx, sizeInPx).apply {
                    if (index < totalRounds - 1) marginEnd = marginInPx
                }
                setBackgroundResource(R.drawable.round_button)
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.button_secondary)

                binding.roundCounterContainer.addView(this)
                roundViews.add(this)
            }
        }
    }

    private fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    private fun initProgressArc() {
        binding.progressBar.apply {
            progressDrawable = ArcProgressDrawable(context = this@MainActivity)
            max = 10000
            progress = 0
        }
    }

    private fun startProgressAnimation(totalDurationMillis: Long) {
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofInt(currentProgress, 10000).apply {
            this.duration = totalDurationMillis
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Int
                binding.progressBar.progress = progress
            }
            start()
        }
    }

    private fun pauseProgressAnimation() {
        progressAnimator?.let {
            currentProgress = it.animatedValue as Int
            it.cancel()
        }
    }

    private fun resetProgressAnimation() {
        progressAnimator?.cancel()
        currentProgress = 0
        binding.progressBar.progress = 0
    }

    private fun updateRoundUI(currentCycle: Int) {
        val activeColor = ContextCompat.getColorStateList(this, R.color.button_primary)
        val inactiveColor = ContextCompat.getColorStateList(this, R.color.button_secondary)

        roundViews.forEachIndexed { index, view ->
            view.backgroundTintList = if (index < currentCycle) activeColor else inactiveColor
        }
    }


    override fun onSettingsChanged(
        focusTime: Int,
        shortBreak: Int,
        longBreak: Int,
        rounds: Int
    ) {
        resetBackgroundColor()
        viewModel.onResetClicked()
        initRoundCounterViews()
    }

    override fun onDismiss() {
        binding.btnSettings.isEnabled = true
    }

    override fun onDestroy() {
        soundPool?.release()
        soundPool = null
        _binding = null
        super.onDestroy()
    }
}
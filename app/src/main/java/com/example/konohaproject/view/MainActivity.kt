package com.example.konohaproject.view

import android.animation.ValueAnimator
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.konohaproject.R
import com.example.konohaproject.model.ControlState
import com.example.konohaproject.model.TimeConfig
import com.example.konohaproject.databinding.ActivityMainBinding
import com.example.konohaproject.viewmodel.MainViewModel

class MainActivity : AppCompatActivity(), SettingsFragment.SettingsListener {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    private var progressAnimator: ValueAnimator? = null
    private var currentProgress: Int = 0
    private val roundViews = mutableListOf<View>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        initProgressArc()
        initRoundCounterViews()

        viewModel.timerText.observe(this) { timeText ->
            binding.txtTimer.text = timeText
        }

        viewModel.controlState.observe(this) { state ->
            when (state) {
                ControlState.Running -> {
                    binding.btnPlay.visibility = View.GONE
                    binding.btnReset.visibility = View.GONE
                    binding.btnPause.visibility = View.VISIBLE
                    // Se inicia la animación con el tiempo de Focus.
                    startProgressAnimation(TimeConfig.focusTimeMillis(this))
                }
                ControlState.Paused -> {
                    binding.btnPlay.visibility = View.VISIBLE
                    binding.btnReset.visibility = View.VISIBLE
                    binding.btnPause.visibility = View.GONE
                    pauseProgressAnimation()
                }
                ControlState.Stopped -> {
                    binding.btnPlay.visibility = View.VISIBLE
                    binding.btnReset.visibility = View.VISIBLE
                    binding.btnPause.visibility = View.GONE
                    binding.txtTimer.text = TimeConfig.initialDisplayTime(this, true)
                    resetProgressAnimation()
                }
            }
        }

        viewModel.cycleInfo.observe(this) { cycleInfo ->

            resetProgressAnimation()
            startProgressAnimation(cycleInfo.nextDuration)
            val backgroundColor = if (cycleInfo.isFocus) {
                ContextCompat.getColor(this, R.color.background_app_focus)
            } else {
                ContextCompat.getColor(this, R.color.background_app_break)
            }
            binding.main.setBackgroundColor(backgroundColor)

            if (cycleInfo.isFocus) {
                updateRoundUI(cycleInfo.currentRound)

                if (cycleInfo.currentRound == 0) {
                    viewModel.onResetClicked()
                }
            }
        }
        viewModel.currentRound.observe(this) { currentRound ->
            updateRoundUI(currentRound)
        }


        binding.btnPlay.setOnClickListener {
            viewModel.onPlayClicked()
        }
        binding.btnPause.setOnClickListener {
            viewModel.onPauseClicked()
        }
        binding.btnReset.setOnClickListener {
            viewModel.onResetClicked()
        }
        binding.btnSettings.setOnClickListener {
            binding.btnSettings.isEnabled = false // Lo desactivo para evitar dobles pulsaciones
            // Al abrir los settings se pausa el temporizador
            viewModel.onPauseClicked()
            SettingsFragment.newInstance().show(supportFragmentManager, "SettingsDialog")
        }
    }


    private fun initRoundCounterViews() {
        val totalRounds = TimeConfig.getTotalRounds(this)
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

        val remainingDuration = if (currentProgress > 0) {
            totalDurationMillis * (10000 - currentProgress) / 10000
        } else {
            totalDurationMillis
        }

        progressAnimator = ValueAnimator.ofInt(currentProgress, 10000).apply {
            this.duration = remainingDuration
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
        viewModel.onResetClicked()
        initRoundCounterViews()
    }

    override fun onDismiss() {
        binding.btnSettings.isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
package com.example.konohaproject.view

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Resources
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.konohaproject.R
import com.example.konohaproject.controller.ControlState
import com.example.konohaproject.controller.CountdownController
import com.example.konohaproject.controller.CountdownService
import com.example.konohaproject.controller.TimeConfig
import com.example.konohaproject.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity(), CountdownService.TimeUpdateListener, SettingsFragment.SettingsListener {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var currentTotalDuration: Long = 0

    private val roundViews = mutableListOf<View>()

    private var countdownController: CountdownController? = null
    private var isBound = false

    private var progressAnimator: ValueAnimator? = null
    private var currentProgress: Int = 0

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CountdownService.CountdownBinder
            countdownController = binder.getController().apply {
                setTimeUpdateListener(this@MainActivity)
            }
            isBound = true
            updateUIWithCurrentState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            countdownController = null
        }
    }

    private fun updateUIWithCurrentState() {
        countdownController?.let { controller ->
            val isRunning = controller.isRunning()
            val isPaused = controller.isPaused()

            when {
                isRunning && !isPaused -> updateControlState(ControlState.Running)
                isPaused -> updateControlState(ControlState.Paused)
                else -> updateControlState(ControlState.Stopped)
            }

            if (isRunning) {
                val remaining = controller.getRemainingTime()
                startProgressAnimation(remaining)
            } else {
                resetProgressAnimation()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, CountdownService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            countdownController?.setTimeUpdateListener(null)
            unbindService(serviceConnection)
            isBound = false
            countdownController = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initProgressArc()
        initRoundCounterViews()
        initListeners()
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

    private fun initListeners() {
        binding.btnPlay.setOnClickListener {
            countdownController?.let { controller ->
                when {
                    controller.isPaused() -> {
                        resumeCountdown()
                        updateControlState(ControlState.Running)
                    }

                    !controller.isRunning() -> {
                        startCountdown()
                        updateControlState(ControlState.Running)
                    }
                }
            }
        }

        binding.btnPause.setOnClickListener {
            countdownController?.pause()
            pauseProgressAnimation()
            updateControlState(ControlState.Paused)
        }

        binding.btnReset.setOnClickListener {
            countdownController?.reset()
            resetCountdown()
        }

        binding.btnSettings.setOnClickListener {

            binding.btnSettings.setEnabled(false)
            countdownController?.pause()
            pauseProgressAnimation()
            updateControlState(ControlState.Paused)

            SettingsFragment.newInstance()
                .show(supportFragmentManager, "SettingsDialog")

        }
    }


    private fun updateControlState(state: ControlState) {
        when(state) {
            ControlState.Running -> {
                binding.btnPlay.visibility = View.GONE
                binding.btnReset.visibility = View.GONE
                binding.btnPause.visibility = View.VISIBLE
            }
            ControlState.Paused -> {
                binding.btnPlay.visibility = View.VISIBLE
                binding.btnReset.visibility = View.VISIBLE
                binding.btnPause.visibility = View.GONE
            }
            ControlState.Stopped -> {
                binding.btnPlay.visibility = View.VISIBLE
                binding.btnReset.visibility = View.VISIBLE
                binding.btnPause.visibility = View.GONE
                binding.txtTimer.text = TimeConfig.initialDisplayTime(this, true)
            }
        }
    }

    private fun startCountdown() {
        countdownController?.let { controller ->

            val duration = TimeConfig.focusTimeMillis(this)
            currentTotalDuration = duration

            controller.start(duration)
            startProgressAnimation(duration)

            startService(Intent(this, CountdownService::class.java))
        }
    }

    private fun resumeCountdown() {
        countdownController?.let { controller ->

            val remaining = controller.getRemainingTime()


            controller.resume()
            startProgressAnimation(remaining)
        }
    }

    private fun resetCountdown() {
        countdownController?.reset()
        resetProgressAnimation()
        stopService(Intent(this, CountdownService::class.java))
        binding.txtTimer.text = TimeConfig.initialDisplayTime(this, true)
        currentTotalDuration = TimeConfig.focusTimeMillis(this)
        binding.progressBar.progress = 0

        updateRoundUI(0)
        binding.main.setBackgroundColor(ContextCompat.getColor(this, R.color.background_app_focus))
        updateControlState(ControlState.Stopped)
    }

    override fun onTimeUpdate(remainingTime: Long) {
        runOnUiThread {
            val totalSeconds = (remainingTime + 500) / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            binding.txtTimer.text = String.format(Locale.US,"%02d:%02d", minutes, seconds)

        }

    }

    private fun startProgressAnimation(durationMillis: Long) {
        progressAnimator?.cancel()

        progressAnimator = ValueAnimator.ofInt(currentProgress, 10000).apply {
            this.duration = durationMillis
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

        roundViews.forEachIndexed {
            index, view ->
            view.backgroundTintList = if (index < currentCycle) activeColor else inactiveColor
        }
    }

    override fun onCountdownFinished(currentRound: Int, isFocus: Boolean) {

        val totalRounds = TimeConfig.getTotalRounds(this)

        currentTotalDuration = if (isFocus) {
            TimeConfig.focusTimeMillis(this)
        } else {
            if (currentRound == totalRounds) {
                TimeConfig.longBreakTimeMillis(this)
            } else {
                TimeConfig.shortBreakTimeMillis(this)
            }
        }


        runOnUiThread {

            resetProgressAnimation()
            startProgressAnimation(currentTotalDuration)

            if (!isFocus) {
                val breakColor = ContextCompat.getColor(this, R.color.background_app_break)
                binding.main.setBackgroundColor(breakColor)
            } else {

                // Cambiar el diseño a la pantalla de Focus
                val focusColor = ContextCompat.getColor(this, R.color.background_app_focus)
                binding.main.setBackgroundColor(focusColor)

                // Va solo, con el round el tio se apaña
                updateRoundUI(currentRound)

                // Aqui habra que comprobar si es el round 1 (autorestart on) o el 0 (autorestart off)
                if (currentRound == 0) {
                    updateControlState(ControlState.Stopped)
                }
            }
        }
    }

    override fun onSettingsChanged(focusTime: Int, shortBreak: Int, longBreak: Int, rounds: Int) {
        initRoundCounterViews()
        resetCountdown()
    }

    override fun onDismiss() {
        binding.btnSettings.setEnabled(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
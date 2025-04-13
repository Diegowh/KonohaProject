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
import java.util.Locale

class MainActivity : AppCompatActivity(), CountdownService.TimeUpdateListener, SettingsFragment.SettingsListener {


    private lateinit var pnlMain: ConstraintLayout

    private lateinit var txtTimer: TextView
    private lateinit var btnPlay: ImageButton
    private lateinit var btnPause: ImageButton
    private lateinit var btnReset: ImageButton
    private lateinit var btnSettings: ImageButton

    private lateinit var roundCounterContainer: LinearLayout

    private lateinit var progressBar: ProgressBar
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
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        progressBar = initProgressArc()

        pnlMain = findViewById(R.id.main)

        txtTimer = findViewById(R.id.txtTimer)
        btnPlay = findViewById(R.id.btnPlay)
        btnPause = findViewById(R.id.btnPause)
        btnReset = findViewById(R.id.btnReset)
        btnSettings = findViewById(R.id.btnSettings)
        roundCounterContainer = findViewById(R.id.roundCounterContainer)

        initRoundCounterViews()
        initListeners()
    }


    private fun initRoundCounterViews() {
        val totalRounds = TimeConfig.getTotalRounds(this)
        roundCounterContainer.removeAllViews()
        roundViews.clear()

        val sizeInPx = 8.dpToPx()
        val marginInPx = 8.dpToPx()

        repeat(totalRounds) { index ->
            View(this).apply {
                layoutParams = LinearLayout.LayoutParams(sizeInPx, sizeInPx).apply {
                    if (index < totalRounds - 1) marginEnd = marginInPx
                }
                setBackgroundResource(R.drawable.round_button)
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.button_secondary)

                roundCounterContainer.addView(this)
                roundViews.add(this)
            }
        }
    }

    private fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    private fun initProgressArc(): ProgressBar {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar).apply {
            progressDrawable = ArcProgressDrawable(
                context = this@MainActivity
            )
            max = 10000 // Nivel máximo (requerido para usar level)
            progress = 0
        }
        return progressBar
    }

    private fun initListeners() {
        btnPlay.setOnClickListener {
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

        btnPause.setOnClickListener {
            countdownController?.pause()
            pauseProgressAnimation()
            updateControlState(ControlState.Paused)
        }

        btnReset.setOnClickListener {
            countdownController?.reset()
            resetCountdown()
        }

        btnSettings.setOnClickListener {

            btnSettings.setEnabled(false)
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
                btnPlay.visibility = View.GONE
                btnReset.visibility = View.GONE
                btnPause.visibility = View.VISIBLE
            }
            ControlState.Paused -> {
                btnPlay.visibility = View.VISIBLE
                btnReset.visibility = View.VISIBLE
                btnPause.visibility = View.GONE
            }
            ControlState.Stopped -> {
                btnPlay.visibility = View.VISIBLE
                btnReset.visibility = View.VISIBLE
                btnPause.visibility = View.GONE
                txtTimer.text = TimeConfig.initialDisplayTime(this, true)
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
        txtTimer.text = TimeConfig.initialDisplayTime(this, true)
        currentTotalDuration = TimeConfig.focusTimeMillis(this)
        progressBar.progress = 0

        updateRoundUI(0)
        pnlMain.setBackgroundColor(ContextCompat.getColor(this, R.color.background_app_focus))
        updateControlState(ControlState.Stopped)
    }

    override fun onTimeUpdate(remainingTime: Long) {
        runOnUiThread {
            val totalSeconds = (remainingTime + 500) / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            txtTimer.text = String.format(Locale.US,"%02d:%02d", minutes, seconds)

        }

    }

    private fun startProgressAnimation(durationMillis: Long) {
        progressAnimator?.cancel()

        progressAnimator = ValueAnimator.ofInt(currentProgress, 10000).apply {
            this.duration = durationMillis
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Int
                progressBar.progress = progress
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
        progressBar.progress = 0
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
                pnlMain.setBackgroundColor(breakColor)
            } else {

                // Cambiar el diseño a la pantalla de Focus
                val focusColor = ContextCompat.getColor(this, R.color.background_app_focus)
                pnlMain.setBackgroundColor(focusColor)

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
        btnSettings.setEnabled(true)
    }
}
package com.example.konohaproject.view

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
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

class MainActivity : AppCompatActivity(), CountdownService.TimeUpdateListener {


    private lateinit var pnlMain: ConstraintLayout

    private lateinit var txtTimer: TextView
    private lateinit var btnPlay: ImageButton
    private lateinit var btnPause: ImageButton
    private lateinit var btnReset: ImageButton

    private lateinit var viewRound1: View
    private lateinit var viewRound2: View
    private lateinit var viewRound3: View
    private lateinit var viewRound4: View

    private lateinit var progressBar: ProgressBar
    private var currentTotalDuration: Long = TimeConfig.focusTimeMillis();

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

        viewRound1 = findViewById(R.id.viewRound1)
        viewRound2 = findViewById(R.id.viewRound2)
        viewRound3 = findViewById(R.id.viewRound3)
        viewRound4 = findViewById(R.id.viewRound4)
        initListeners()
    }

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
                txtTimer.text = TimeConfig.initialFocusDisplayTime()
            }
        }
    }

    private fun startCountdown() {
        countdownController?.let { controller ->

            val duration = TimeConfig.focusTimeMillis()
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
        txtTimer.text = TimeConfig.initialFocusDisplayTime()
        currentTotalDuration = TimeConfig.focusTimeMillis()
        progressBar.progress = 0

        updateCycleUI(0)
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

    private fun updateCycleUI(currentCycle: Int) {
        val activeColor = ContextCompat.getColorStateList(this, R.color.button_primary)
        val inactiveColor = ContextCompat.getColorStateList(this, R.color.button_secondary)

        listOf(viewRound1, viewRound2, viewRound3, viewRound4).forEachIndexed {
            index, view ->
            view.backgroundTintList = if (index < currentCycle) activeColor else inactiveColor
        }
    }

    override fun onCountdownFinished(currentCycle: Int, isFocus: Boolean) {

        val totalCycles = TimeConfig.getTotalCycles()

        currentTotalDuration = if (isFocus) {
            TimeConfig.focusTimeMillis()
        } else {
            if (currentCycle == totalCycles) {
                TimeConfig.longBreakTimeMillis()
            } else {
                TimeConfig.breakTimeMillis()
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

                // Va solo, con el ciclo el tio se apaña
                updateCycleUI(currentCycle)

                // Aqui habra que comprobar si es el ciclo 1 (autorestart on) o el 0 (autorestart off)
                if (currentCycle == 0) {
                    updateControlState(ControlState.Stopped)
                }
            }
        }
    }
}
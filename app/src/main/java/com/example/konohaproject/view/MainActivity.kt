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
import com.example.konohaproject.ArcProgressDrawable
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

    companion object {
        private const val SEGUNDOS_POR_MINUTO = 60
        private const val MILESIMAS_POR_SEGUNDO = 1000L
    }


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
                onTimeUpdate(controller.getRemainingTime())
            } else {
                txtTimer.text = TimeConfig.initialFocusDisplayTime()
                updateCycleUI(0)

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

//        ValueAnimator.ofInt(0, 10000).apply {
//            duration = timerDuration
//            interpolator = LinearInterpolator()
//            addUpdateListener {
//                progressBar?.progress = it.animatedValue as Int
//            }
//            start()
//        }


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
                        controller.resume()
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
        countdownController?.start(TimeConfig.focusTimeMillis())
        startService(Intent(this, CountdownService::class.java))
    }

    private fun resetCountdown() {
        countdownController?.reset()
        stopService(Intent(this, CountdownService::class.java))
        txtTimer.text = TimeConfig.initialFocusDisplayTime()
        currentTotalDuration = TimeConfig.focusTimeMillis()
        progressBar.progress = 0
//        val currentCycle = countdownController?.getCurrentCycle() ?: 0
        updateCycleUI(0)
        pnlMain.setBackgroundColor(ContextCompat.getColor(this, R.color.background_app_focus))
        updateControlState(ControlState.Stopped)
    }

    override fun onTimeUpdate(remainingTime: Long) {
        runOnUiThread {
            val minutes = remainingTime / 1000 / 60
            val seconds = remainingTime / 1000 % 60
            txtTimer.text = String.format(Locale.US,"%02d:%02d", minutes, seconds)

            val progress = ((currentTotalDuration - remainingTime).toFloat() / currentTotalDuration * 10000).toInt()
            println(progress)
            progressBar.progress = progress
        }

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
package com.example.konohaproject.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.konohaproject.R
import com.example.konohaproject.controller.ControlState
import com.example.konohaproject.controller.CountdownController
import com.example.konohaproject.controller.CountdownService

class MainActivity : AppCompatActivity(), CountdownService.TimeUpdateListener {

    private lateinit var txtTimer: TextView
    private lateinit var btnPlay: ImageButton
    private lateinit var btnPause: ImageButton
    private lateinit var btnStop: ImageButton

    private var countdownController: CountdownController? = null
    private var isBound = false

    private var focusTime: Long = 25L

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

        txtTimer = findViewById<TextView>(R.id.txtTimer)
        btnPlay = findViewById<ImageButton>(R.id.btnPlay)
        btnPause = findViewById<ImageButton>(R.id.btnPause)
        btnStop = findViewById<ImageButton>(R.id.btnStop)

        initListeners()
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
                        startCountdown(focusTime)
                        updateControlState(ControlState.Running)
                    }
                }
            }
        }

        btnPause.setOnClickListener {
            countdownController?.pause()
            updateControlState(ControlState.Paused)
        }

        btnStop.setOnClickListener {
            countdownController?.reset()
            stopCountdown()
        }
    }


    private fun updateControlState(state: ControlState) {
        when(state) {
            ControlState.Running -> {
                btnPlay.visibility = View.GONE
                btnStop.visibility = View.GONE
                btnPause.visibility = View.VISIBLE
            }
            ControlState.Paused -> {
                btnPlay.visibility = View.VISIBLE
                btnStop.visibility = View.VISIBLE
                btnPause.visibility = View.GONE
            }
            ControlState.Stopped -> {
                btnPlay.visibility = View.VISIBLE
                btnStop.visibility = View.VISIBLE
                btnPause.visibility = View.GONE
            }
        }
    }

    private fun startCountdown(durationMinutes: Long) {
        val durationMillis = durationMinutes * 60 * 1000;
        countdownController?.start(durationMillis)
        startService(Intent(this, CountdownService::class.java))
    }

    private fun stopCountdown() {
        countdownController?.reset()
        stopService(Intent(this, CountdownService::class.java))
        txtTimer.text = "25:00"
        updateControlState(ControlState.Stopped)
    }

    override fun onTimeUpdate(remainingTime: Long) {
        runOnUiThread {
            val minutes = remainingTime / 1000 / 60
            val seconds = remainingTime / 1000 % 60
            txtTimer.text = String.format("%02d:%02d", minutes, seconds)
        }
    }
}
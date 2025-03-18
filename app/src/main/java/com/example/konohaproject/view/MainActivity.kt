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
import com.example.konohaproject.controller.CountdownService

class MainActivity : AppCompatActivity(), CountdownService.TimeUpdateListener {

    private lateinit var txtTimer: TextView
    private lateinit var btnPlay: ImageButton
    private lateinit var btnPause: ImageButton
    private lateinit var btnStop: ImageButton

    private var countdownService: CountdownService? = null
    private var isBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CountdownService.CountdownBinder
            countdownService = binder.getService()
            countdownService?.timeListener = this@MainActivity
            isBound = true

            countdownService?.let {
                if (CountdownService.isCountDownActive() || CountdownService.isPaused()) {
                    val currentTime = it.getCurrentRemainingTime()
                    onTimeUpdate(currentTime)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
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

            countdownService?.timeListener = null
            unbindService(serviceConnection)
            isBound = false

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
            when {
                CountdownService.isPaused() -> {
                    countdownService?.resumeCountdown()
                    updateControlState(ControlState.Running)
                }
                !CountdownService.isCountDownActive() -> {
                    startCountdown(25)
                    updateControlState(ControlState.Running)
                }
            }
        }

        btnPause.setOnClickListener {
            countdownService?.pauseCountdown()
            updateControlState(ControlState.Paused)
        }

        btnStop.setOnClickListener {
            if (CountdownService.isPaused()) {
                Intent(this, CountdownService::class.java).apply {
                    action = "STOP"
                    startService(this)
                }
                stopCountdown()
                updateControlState(ControlState.Stopped)

            }

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

        stopCountdown()

        val durationMillis = durationMinutes * 60 * 1000;
        val serviceIntent = Intent(this, CountdownService::class.java).apply {
            putExtra("duration", durationMillis);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private fun stopCountdown() {
        countdownService?.hardReset()
        Intent(this, CountdownService::class.java).apply {
            action = "STOP"
            startService(this)
            stopService(this)
        }
        updateControlState(ControlState.Stopped)
        txtTimer.text = "25:00"
    }

    override fun onTimeUpdate(remainingTime: Long) {
        runOnUiThread {
            val minutes = remainingTime / 1000 / 60
            val seconds = remainingTime / 1000 % 60
            txtTimer.text = String.format("%02d:%02d", minutes, seconds)
        }
    }
}
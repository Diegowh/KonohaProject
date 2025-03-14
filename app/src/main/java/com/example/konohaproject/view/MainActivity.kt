package com.example.konohaproject.view

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.konohaproject.R
import com.example.konohaproject.controller.ControlState
import com.example.konohaproject.controller.CountdownService

class MainActivity : AppCompatActivity() {

    private lateinit var btnPlay: ImageButton
    private lateinit var btnPause: ImageButton
    private lateinit var btnStop: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnPlay = findViewById<ImageButton>(R.id.btnPlay)
        btnPause = findViewById<ImageButton>(R.id.btnPause)
        btnStop = findViewById<ImageButton>(R.id.btnStop)

        initListeners()
    }

    private fun initListeners() {
        btnPlay.setOnClickListener {
            updateControlState(ControlState.Playing)
            startCountdown(25)
        }

        btnPause.setOnClickListener{
            updateControlState(ControlState.Paused)
        }

        btnStop.setOnClickListener {
            stopCountdown()
        }
    }

    private fun updateControlState(state: ControlState) {
        when(state) {
            is ControlState.Playing -> {
                btnPlay.visibility = View.GONE
                btnStop.visibility = View.GONE
                btnPause.visibility = View.VISIBLE
            }
            is ControlState.Paused -> {
                btnPlay.visibility = View.VISIBLE
                btnStop.visibility = View.VISIBLE
                btnPause.visibility = View.GONE
            }
        }
    }

    private fun startCountdown(durationMinutes: Long) {
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
        stopService(Intent(this, CountdownService::class.java));
    }
}
package com.example.konohaproject.view

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.konohaproject.R
import com.example.konohaproject.controller.CountdownService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnPlay = findViewById<ImageButton>(R.id.btnPlay)
            .setOnClickListener {startCountdown(25)}

        val btnStop = findViewById<ImageButton>(R.id.btnStop)
            .setOnClickListener {stopCountdown()}
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
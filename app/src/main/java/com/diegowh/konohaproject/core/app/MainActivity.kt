package com.diegowh.konohaproject.core.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.diegowh.konohaproject.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        processNotificationIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processNotificationIntent(intent)
    }
    
    private fun processNotificationIntent(intent: Intent?) {
        when (intent?.action) {
            "OPEN_FROM_INTERVAL_NOTIFICATION", "OPEN_FROM_SESSION_NOTIFICATION" -> {
                Log.d("MainActivity", "Opening from notification: ${intent.action}")
            }
        }
    }
}
package com.diegowh.konohaproject.domain.main

import android.app.Application
import com.diegowh.konohaproject.domain.settings.TimerPrefsRepository
import com.diegowh.konohaproject.domain.settings.TimerSettingsRepository

class App : Application() {

    val timerSettings: TimerSettingsRepository by lazy {
        TimerPrefsRepository(this)
    }
}
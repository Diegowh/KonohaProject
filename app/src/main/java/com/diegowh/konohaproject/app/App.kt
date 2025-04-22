package com.diegowh.konohaproject.app

import android.app.Application
import com.diegowh.konohaproject.domain.settings.CharacterPrefsRepository
import com.diegowh.konohaproject.domain.settings.CharacterSettingsRepository
import com.diegowh.konohaproject.domain.settings.TimerPrefsRepository
import com.diegowh.konohaproject.domain.settings.TimerSettingsRepository

class App : Application() {

    val timerSettings: TimerSettingsRepository by lazy {
        TimerPrefsRepository(this)
    }

    val characterSettings: CharacterSettingsRepository by lazy {
        CharacterPrefsRepository(this)
    }
}
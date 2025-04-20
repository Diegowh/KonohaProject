package com.diegowh.konohaproject.domain.main

import android.app.Application
import com.diegowh.konohaproject.domain.settings.SettingsProvider
import com.diegowh.konohaproject.domain.settings.TimerSettingsProvider

class App : Application() {

    val settingsProvider: SettingsProvider by lazy {
        TimerSettingsProvider(this)
    }
}
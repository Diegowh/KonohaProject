package com.diegowh.konohaproject.domain.main

import android.app.Application
import com.diegowh.konohaproject.domain.timer.SettingsProvider
import com.diegowh.konohaproject.domain.timer.TimerSettingsProvider

class App : Application() {

    val settingsProvider: SettingsProvider by lazy {
        TimerSettingsProvider(this)
    }
}
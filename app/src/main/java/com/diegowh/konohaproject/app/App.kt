package com.diegowh.konohaproject.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.diegowh.konohaproject.domain.settings.CharacterPrefsRepository
import com.diegowh.konohaproject.domain.settings.CharacterSettingsRepository
import com.diegowh.konohaproject.domain.settings.TimerPrefsRepository
import com.diegowh.konohaproject.domain.settings.TimerSettingsRepository
import java.util.concurrent.atomic.AtomicBoolean

class App : Application(), Application.ActivityLifecycleCallbacks {

    val timerSettings: TimerSettingsRepository by lazy {
        TimerPrefsRepository(this)
    }

    val characterSettings: CharacterSettingsRepository by lazy {
        CharacterPrefsRepository(this)
    }
    
    private val isInForeground = AtomicBoolean(false)
    private var activeActivities = 0
    
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (activeActivities == 0) {
            isInForeground.set(true)
        }
        activeActivities++
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        activeActivities--
        if (activeActivities == 0) {
            isInForeground.set(false)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
package com.diegowh.konohaproject.core.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.diegowh.konohaproject.feature.character.data.repository.CharacterPrefsRepository
import com.diegowh.konohaproject.feature.character.domain.repository.CharacterSettingsRepository
import com.diegowh.konohaproject.feature.timer.data.repository.TimerPrefsRepository
import com.diegowh.konohaproject.feature.timer.domain.repository.TimerSettingsRepository
import com.diegowh.konohaproject.feature.xp.data.XpPrefsRepository
import com.diegowh.konohaproject.feature.xp.domain.XpManager
import com.diegowh.konohaproject.feature.xp.domain.XpRepository
import java.util.concurrent.atomic.AtomicBoolean

class App : Application(), Application.ActivityLifecycleCallbacks {

    val timerSettings: TimerSettingsRepository by lazy {
        TimerPrefsRepository(this)
    }

    val characterSettings: CharacterSettingsRepository by lazy {
        CharacterPrefsRepository(this)
    }

    private val xpRepository: XpRepository by lazy { XpPrefsRepository(this) }
    val xpManager: XpManager by lazy { XpManager(xpRepository) }
    
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
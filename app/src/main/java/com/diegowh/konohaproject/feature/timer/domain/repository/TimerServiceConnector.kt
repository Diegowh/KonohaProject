package com.diegowh.konohaproject.feature.timer.domain.repository

import android.app.Application
import com.diegowh.konohaproject.feature.timer.domain.model.TimerUIEvent
import com.diegowh.konohaproject.feature.timer.data.service.TimerService


interface TimerServiceConnector {
    fun connect(application: Application, onTimerEvent: (TimerUIEvent) -> Unit)
    fun disconnect(application: Application)
    fun getService(): TimerService?
}


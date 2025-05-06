package com.diegowh.konohaproject.timer.domain.repository

import android.app.Application
import com.diegowh.konohaproject.timer.presentation.events.TimerUIEvent
import com.diegowh.konohaproject.timer.data.service.TimerService


interface TimerServiceConnector {
    fun connect(application: Application, onTimerEvent: (TimerUIEvent) -> Unit)
    fun disconnect(application: Application)
    fun getService(): TimerService?
}


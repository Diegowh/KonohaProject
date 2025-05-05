package com.diegowh.konohaproject.timer.infrastructure.service

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.diegowh.konohaproject.timer.application.usecases.TimerUIEvent
import com.diegowh.konohaproject.timer.domain.repository.TimerServiceConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class TimerServiceConnectorImpl(private val coroutineScope: CoroutineScope) :
    TimerServiceConnector {
    private var serviceRef: WeakReference<TimerService>? = null
    private lateinit var serviceConnection: ServiceConnection

    override fun connect(application: Application, onTimerEvent: (TimerUIEvent) -> Unit) {
        serviceConnection = createServiceConnection(onTimerEvent)

        Intent(application, TimerService::class.java).also { intent ->
            application.bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun disconnect(application: Application) {
        application.unbindService(serviceConnection)
        serviceRef = null
    }

    override fun getService(): TimerService? = serviceRef?.get()

    private fun createServiceConnection(onTimerEvent: (TimerUIEvent) -> Unit): ServiceConnection {
        return object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as TimerService.TimerBinder
                val controller = binder.getController() as TimerService
                serviceRef = WeakReference(controller)

                coroutineScope.launch {
                    controller.getTimerEvents().collect { event ->
                        onTimerEvent(event)
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                serviceRef = null
            }
        }
    }
}
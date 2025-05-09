package com.diegowh.konohaproject.feature.timer.data.service

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.diegowh.konohaproject.feature.timer.domain.model.TimerUIEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class TimerServiceConnection(
    private val scope: CoroutineScope,
    private val onTimerEvent: (TimerUIEvent) -> Unit
) : ServiceConnection {

    var service: TimerService? = null
        private set

    fun connect(application: Application) {
        val intent = Intent(application, TimerService::class.java)
        application.bindService(intent, /* conn = */ this, Context.BIND_AUTO_CREATE)
    }

    fun disconnect(application: Application) {
        application.unbindService(this)
        service = null
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        service = (binder as TimerService.TimerBinder).getController()

        // Reenviamos el Flow de eventos hacia la UI
        scope.launch {
            service!!.getTimerEvents().collect(onTimerEvent)
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        service = null            // el sistema mató el servicio
    }
}
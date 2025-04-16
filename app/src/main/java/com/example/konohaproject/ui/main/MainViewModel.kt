package com.example.konohaproject.ui.main

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.konohaproject.domain.timer.TimeUpdateListener
import com.example.konohaproject.domain.timer.TimerState
import com.example.konohaproject.domain.timer.TimerService
import com.example.konohaproject.domain.timer.TimerController
import com.example.konohaproject.domain.timer.TimerSettings
import java.lang.ref.WeakReference
import java.util.Locale

data class CycleInfo(
    val currentRound: Int,
    val isFocus: Boolean,
    val nextDuration: Long
)

class MainViewModel(application: Application) : AndroidViewModel(application), TimeUpdateListener {

    private val _timerText = MutableLiveData<String>()
    val timerText: LiveData<String> get() = _timerText

    private val _timerState = MutableLiveData<TimerState>()
    val timerState: LiveData<TimerState> get() = _timerState

    private val _currentRound = MutableLiveData<Int>()
    val currentRound: LiveData<Int> get() = _currentRound

    private val _cycleInfo = MutableLiveData<CycleInfo>()
    val cycleInfo: LiveData<CycleInfo> get() = _cycleInfo

    /* Utilizo WeakReference para evitar que el TimerService mantenga una referencia fuerte
    * al context. Ya que previamente se referenciaba de manera directa, lo que podia causar leaks de
    * memoria (cosa que tampoco llegue a comprobar, pero me avisaba el IDE)
    * De esta manera el colector de basura no tendra problemas para liberarlo si fuese necesario */
    private var countdownController: WeakReference<TimerService>? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            val controller = binder.getController() as TimerService
            countdownController = WeakReference(controller)
            controller.setTimeUpdateListener(this@MainViewModel)
            updateUIWithCurrentState()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            countdownController = null
        }
    }

    init {
        val intent = Intent(getApplication(), TimerService::class.java)
        getApplication<Application>().bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )

        _timerText.value = TimerSettings.initialDisplayTime(getApplication(), true)
        _timerState.value = TimerState.Stopped
        _currentRound.value = 0
    }

    private fun updateUIWithCurrentState() {
        countdownController?.get()?.let { controller ->
            when {
                controller.isRunning() && !controller.isPaused() ->
                    _timerState.postValue(TimerState.Running)
                controller.isPaused() ->
                    _timerState.postValue(TimerState.Paused)
                else ->
                    _timerState.postValue(TimerState.Stopped)
            }
        }
    }

    override fun onTimeUpdate(remainingTime: Long) {
        val totalSeconds = (remainingTime + 500) / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val text = String.format(Locale.US, "%02d:%02d", minutes, seconds)
        _timerText.postValue(text)
    }

    fun onPlayClicked() {
        countdownController?.get()?.let { controller ->
            when {
                controller.isPaused() -> {
                    controller.resume()
                    _timerState.postValue(TimerState.Running)
                }
                !controller.isRunning() -> {
                    val duration = TimerSettings.focusTimeMillis(getApplication())
                    controller.start(duration)
                    _timerState.postValue(TimerState.Running)
                }
            }
        }
    }

    fun onPauseClicked() {
        countdownController?.get()?.pause()
        _timerState.postValue(TimerState.Paused)
    }

    fun onResetClicked() {
        countdownController?.get()?.reset()
        _timerState.postValue(TimerState.Stopped)
        _timerText.postValue(TimerSettings.initialDisplayTime(getApplication(), true))
        _currentRound.postValue(0)
    }

    override fun onIntervalFinished(currentRound: Int, isFocus: Boolean) {

        // se calcula la duracion del siguiente ciclo en funcion del estado
        val totalRounds = TimerSettings.getTotalRounds(getApplication())
        val nextDuration = if (isFocus) {
            TimerSettings.focusTimeMillis(getApplication())
        } else {
            if (currentRound == totalRounds) {
                TimerSettings.longBreakTimeMillis(getApplication())
            } else {
                TimerSettings.shortBreakTimeMillis(getApplication())
            }
        }

        _cycleInfo.postValue(CycleInfo(currentRound, isFocus, nextDuration))
        _currentRound.postValue(currentRound)
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unbindService(serviceConnection)
    }
}
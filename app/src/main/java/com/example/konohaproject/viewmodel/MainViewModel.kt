package com.example.konohaproject.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.konohaproject.controller.ControlState
import com.example.konohaproject.controller.CountdownService
import com.example.konohaproject.model.TimeConfig
import java.lang.ref.WeakReference
import java.util.Locale

data class CycleInfo(
    val currentRound: Int,
    val isFocus: Boolean,
    val nextDuration: Long
)

class MainViewModel(application: Application) : AndroidViewModel(application), CountdownService.TimeUpdateListener {

    private val _timerText = MutableLiveData<String>()
    val timerText: LiveData<String> get() = _timerText

    private val _controlState = MutableLiveData<ControlState>()
    val controlState: LiveData<ControlState> get() = _controlState

    private val _currentRound = MutableLiveData<Int>()
    val currentRound: LiveData<Int> get() = _currentRound

    private val _cycleInfo = MutableLiveData<CycleInfo>()
    val cycleInfo: LiveData<CycleInfo> get() = _cycleInfo

    /* Utilizo WeakReference para evitar que el CountdownService mantenga una referencia fuerte
    * al context. Ya que previamente se referenciaba de manera directa, lo que podia causar leaks de
    * memoria (cosa que tampoco llegue a comprobar, pero me avisaba el IDE)
    * De esta manera el colector de basura no tendra problemas para liberarlo si fuese necesario */
    private var countdownController: WeakReference<CountdownService>? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CountdownService.CountdownBinder
            val controller = binder.getController() as CountdownService
            countdownController = WeakReference(controller)
            controller.setTimeUpdateListener(this@MainViewModel)
            updateUIWithCurrentState()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            countdownController = null
        }
    }

    init {
        val intent = Intent(getApplication(), CountdownService::class.java)
        getApplication<Application>().bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )

        _timerText.value = TimeConfig.initialDisplayTime(getApplication(), true)
        _controlState.value = ControlState.Stopped
        _currentRound.value = 0
    }

    private fun updateUIWithCurrentState() {
        countdownController?.get()?.let { controller ->
            when {
                controller.isRunning() && !controller.isPaused() ->
                    _controlState.postValue(ControlState.Running)
                controller.isPaused() ->
                    _controlState.postValue(ControlState.Paused)
                else ->
                    _controlState.postValue(ControlState.Stopped)
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
                    _controlState.postValue(ControlState.Running)
                }
                !controller.isRunning() -> {
                    val duration = TimeConfig.focusTimeMillis(getApplication())
                    controller.start(duration)
                    _controlState.postValue(ControlState.Running)
                }
            }
        }
    }

    fun onPauseClicked() {
        countdownController?.get()?.pause()
        _controlState.postValue(ControlState.Paused)
    }

    fun onResetClicked() {
        countdownController?.get()?.reset()
        _controlState.postValue(ControlState.Stopped)
        _timerText.postValue(TimeConfig.initialDisplayTime(getApplication(), true))
        _currentRound.postValue(0)
    }

    override fun onCountdownFinished(currentRound: Int, isFocus: Boolean) {

        // se calcula la duracion del siguiente ciclo en funcion del estado
        val totalRounds = TimeConfig.getTotalRounds(getApplication())
        val nextDuration = if (isFocus) {
            TimeConfig.focusTimeMillis(getApplication())
        } else {
            if (currentRound == totalRounds) {
                TimeConfig.longBreakTimeMillis(getApplication())
            } else {
                TimeConfig.shortBreakTimeMillis(getApplication())
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
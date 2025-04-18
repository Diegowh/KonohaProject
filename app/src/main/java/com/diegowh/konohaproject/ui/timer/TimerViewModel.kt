package com.diegowh.konohaproject.ui.timer

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.diegowh.konohaproject.domain.timer.TimerState
import com.diegowh.konohaproject.domain.timer.TimerService
import com.diegowh.konohaproject.domain.settings.TimerSettings
import com.diegowh.konohaproject.domain.timer.TimerUIEvent
import com.diegowh.konohaproject.utils.SoundType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.Locale

data class Interval(
    val currentRound: Int,
    val isFocus: Boolean,
    val nextDuration: Long
)

class TimerViewModel(app: Application) : AndroidViewModel(app) {

    private val _timerText = MutableLiveData<String>()
    private val _timerState = MutableLiveData<TimerState>()
    private val _currentRound = MutableLiveData<Int>()
    private val _interval = MutableLiveData<Interval>()
    private val _resumedTime = MutableLiveData<Long>()
    private val _intervalSoundEvent = MutableSharedFlow<SoundType>()
    private val _totalRounds = MutableLiveData<Int>()

    val timerText: LiveData<String> get() = _timerText
    val timerState: LiveData<TimerState> get() = _timerState
    val currentRound: LiveData<Int> get() = _currentRound
    val interval: LiveData<Interval> get() = _interval
    val resumedTime: LiveData<Long> get() = _resumedTime
    val intervalSoundEvent: SharedFlow<SoundType> = _intervalSoundEvent.asSharedFlow()
    val totalRounds: LiveData<Int> get() = _totalRounds


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

            viewModelScope.launch {
                controller.getTimerEvents().collect { event ->
                    when (event) {
                        is TimerUIEvent.TimeUpdate -> {
                            _timerText.postValue(formatTime(event.remainingMillis))
                        }
                        is TimerUIEvent.IntervalFinished -> {
                            val nextDuration = calculateNextDuration(event.currentRound, event.isFocusInterval)
                            _interval.postValue(Interval(event.currentRound, event.isFocusInterval, nextDuration))
                            _currentRound.postValue(event.currentRound)

                            if (!TimerSettings.isMuteEnabled(getApplication())) {
                                _intervalSoundEvent.emit(SoundType.INTERVAL_CHANGE)
                            }
                        }
                    }
                }
            }
            updateUIWithCurrentState(controller)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            countdownController = null
        }
    }

    init {
        Intent(getApplication(), TimerService::class.java).also { intent ->
            getApplication<Application>().bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        }

        _timerText.value = TimerSettings.initialDisplayTime(getApplication(), true)
        _timerState.value = TimerState.Stopped
        _currentRound.value = 0
        _totalRounds.value = TimerSettings.getTotalRounds(getApplication())
    }

    private fun formatTime(remainingMillis: Long): String {
        val totalSeconds = (remainingMillis + 500) / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    private fun calculateNextDuration(currentRound: Int, wasFocus: Boolean): Long {
        return if (wasFocus) {
            TimerSettings.focusTimeMillis(getApplication())
        } else {
            if (currentRound == (_totalRounds.value ?: 0)) {
                TimerSettings.longBreakTimeMillis(getApplication())
            } else {
                TimerSettings.shortBreakTimeMillis(getApplication())
            }
        }
    }

    private fun updateUIWithCurrentState(controller: TimerService) {
        _timerState.postValue(
            when {
                controller.isRunning() && !controller.isPaused() -> TimerState.Running
                controller.isPaused() -> TimerState.Paused
                else -> TimerState.Stopped
            }
        )
    }

    fun onPlayClicked() {
        countdownController?.get()?.let { controller ->
            when {
                controller.isPaused() -> {
                    val remaining = controller.getRemainingTime()
                    controller.resume()
                    _resumedTime.postValue(remaining)
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
        countdownController?.get()?.let { controller ->
            controller.pause()
            _timerState.postValue(TimerState.Paused)
        }
    }

    fun onResetClicked() {
        countdownController?.get()?.let { controller ->
            controller.reset()
            _timerState.postValue(TimerState.Stopped)
            _timerText.postValue(TimerSettings.initialDisplayTime(getApplication(), true))
            _currentRound.postValue(0)
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unbindService(serviceConnection)
    }
}
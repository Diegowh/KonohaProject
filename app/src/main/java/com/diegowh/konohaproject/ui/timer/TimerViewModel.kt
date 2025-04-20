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
import com.diegowh.konohaproject.domain.main.App
import com.diegowh.konohaproject.domain.timer.TimerState
import com.diegowh.konohaproject.domain.timer.TimerService
import com.diegowh.konohaproject.domain.settings.SettingsProvider
import com.diegowh.konohaproject.domain.timer.TimerUIEvent
import com.diegowh.konohaproject.utils.sound.SoundType
import com.diegowh.konohaproject.utils.animation.AnimationAction
import com.diegowh.konohaproject.utils.animation.AnimationState
import com.diegowh.konohaproject.utils.timer.Interval
import com.diegowh.konohaproject.utils.timer.IntervalType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.Locale

class TimerViewModel(app: Application) : AndroidViewModel(app) {

    private val _timerText = MutableLiveData<String>()
    private val _timerState = MutableLiveData<TimerState>()
    private val _currentRound = MutableLiveData<Int>()
    private val _interval = MutableLiveData<Interval>()
    private val _resumedTime = MutableLiveData<Long>()
    private val _intervalSoundEvent = MutableSharedFlow<SoundType>()
    private val _totalRounds = MutableLiveData<Int>()
    private val _animationState = MutableLiveData<AnimationState>()
    private val _animationAction = MutableLiveData<AnimationAction>()

    val timerText: LiveData<String> get() = _timerText
    val timerState: LiveData<TimerState> get() = _timerState
    val currentRound: LiveData<Int> get() = _currentRound
    val interval: LiveData<Interval> get() = _interval
    val resumedTime: LiveData<Long> get() = _resumedTime
    val intervalSoundEvent: SharedFlow<SoundType> = _intervalSoundEvent.asSharedFlow()
    val totalRounds: LiveData<Int> get() = _totalRounds
//    val animationState: LiveData<AnimationState> = _animationState
    val animationAction: LiveData<AnimationAction> = _animationAction

    private val settings: SettingsProvider =
        (getApplication() as App).settingsProvider

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
                            val nextDuration = calculateNextDuration(event.nextInterval)
                            _interval.postValue(Interval(event.currentRound, event.nextInterval, nextDuration))
                            _currentRound.postValue(event.currentRound)

                            if (!settings.isMuteEnabled()) {
                                _intervalSoundEvent.emit(SoundType.INTERVAL_CHANGE)
                            }
                        }
                        TimerUIEvent.SessionFinished -> {
                            _animationAction.postValue(AnimationAction.Stop)
                            handleReset(controller)
                        }
                    }
                }
            }
            updateUIWithCurrentState(controller)
            _animationAction.postValue(AnimationAction.Stop)
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

        _timerText.value = settings.initialDisplayTime(true)
        _timerState.value = TimerState.Stopped
        _currentRound.value = 0
        _totalRounds.value = settings.totalRounds()
    }

    private fun formatTime(remainingMillis: Long): String {
        val totalSeconds = (remainingMillis + 500) / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    private fun calculateNextDuration(intervalType: IntervalType): Long {
        val duration = when (intervalType) {
            IntervalType.FOCUS -> settings.focusTimeMillis()
            IntervalType.SHORT_BREAK -> settings.shortBreakTimeMillis()
            IntervalType.LONG_BREAK -> settings.longBreakTimeMillis()
        }
        return duration
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

    fun updateAnimationState(frame: Int, isPaused: Boolean) {
        _animationState.postValue(AnimationState(frame, isPaused))
    }

    fun onPlayClicked() {
        countdownController?.get()?.let { controller ->
            when {
                controller.isPaused() -> {
                    controller.resume()
                    _resumedTime.postValue(controller.getRemainingTime())
                    val lastFrame = _animationState.value?.currentFrame
                    _animationAction.postValue(AnimationAction.Start(fromFrame = lastFrame))
                    _timerState.postValue(TimerState.Running)
                }
                !controller.isRunning() -> {
                    controller.start(settings.focusTimeMillis())
                    _animationAction.postValue(AnimationAction.Start())
                    _timerState.postValue(TimerState.Running)
                    _currentRound.postValue(1)
                }
            }
        }
    }

    fun onPauseClicked() {
        countdownController?.get()?.let { controller ->
            controller.pause()
            _animationAction.postValue(AnimationAction.Pause)
            _timerState.postValue(TimerState.Paused)
        }
    }

    fun onResetClicked() {
        countdownController?.get()?.let { controller ->
            handleReset(controller)
        }
    }

    private fun handleReset(controller: TimerService) {
        controller.reset()
        _timerState.postValue(TimerState.Stopped)
        _timerText.postValue(settings.initialDisplayTime(true))
        _currentRound.postValue(0)
        _totalRounds.postValue(settings.totalRounds())
        _animationAction.postValue(AnimationAction.Stop)
        _animationState.postValue(AnimationState(0, isPaused = false))
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unbindService(serviceConnection)
    }

}
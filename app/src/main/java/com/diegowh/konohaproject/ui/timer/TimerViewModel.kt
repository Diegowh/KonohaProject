package com.diegowh.konohaproject.ui.timer

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diegowh.konohaproject.app.App
import com.diegowh.konohaproject.core.animation.AnimationAction
import com.diegowh.konohaproject.core.sound.SoundType
import com.diegowh.konohaproject.core.timer.Interval
import com.diegowh.konohaproject.core.timer.IntervalType
import com.diegowh.konohaproject.domain.character.Character
import com.diegowh.konohaproject.domain.character.CharacterSelectionEvent
import com.diegowh.konohaproject.domain.settings.CharacterSettingsRepository
import com.diegowh.konohaproject.domain.settings.TimerSettingsRepository
import com.diegowh.konohaproject.domain.timer.TimerService
import com.diegowh.konohaproject.domain.timer.TimerStatus
import com.diegowh.konohaproject.domain.timer.TimerUIEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.Locale

class TimerViewModel(app: Application) : AndroidViewModel(app) {

    private val characterSettings: CharacterSettingsRepository =
        (getApplication() as App).characterSettings

    private val _timerState = MutableStateFlow(TimerState())
    private val _animationState = MutableStateFlow(AnimationState())
    private val _selectedCharacter = MutableStateFlow(
        characterSettings.getById(
            characterSettings.getSelectedCharacterId()
        )
    )
    private val _intervalSoundEvent = MutableSharedFlow<SoundType>()
    val intervalSoundEvent: SharedFlow<SoundType> = _intervalSoundEvent.asSharedFlow()
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    val animationState: StateFlow<AnimationState> = _animationState.asStateFlow()


    val selectedCharacter: StateFlow<Character> = _selectedCharacter

    private val settings: TimerSettingsRepository =
        (getApplication() as App).timerSettings

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
                        is TimerUIEvent.TimeUpdate -> handleTimeUpdate(event)
                        is TimerUIEvent.IntervalFinished -> handleIntervalFinished(event)
                        TimerUIEvent.SessionFinished -> handleSessionFinished()
                    }
                }
            }
            updateUIWithCurrentState(controller)
            _animationState.update { it.copy(action = AnimationAction.Stop) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            countdownController = null
        }
    }

    init {
        bindService()
        initState()

        // carga del personaje guardado
        val saved = characterSettings.getSelectedCharacterId()
        _selectedCharacter.value = characterSettings.getById(saved)
    }

    private fun bindService() {
        Intent(getApplication(), TimerService::class.java).also { intent ->
            getApplication<Application>().bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    private fun initState() {
        _timerState.value = TimerState(
            timerText = settings.initialDisplayTime(),
            status = TimerStatus.Stopped,
            currentRound = 0,
            totalRounds = settings.totalRounds()
        )
        _animationState.value = AnimationState()
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
        _timerState.update {
            it.copy(
                status = when {
                    controller.isRunning() && !controller.isPaused() -> TimerStatus.Running
                    controller.isPaused() -> TimerStatus.Paused
                    else -> TimerStatus.Stopped
                }
            )
        }
    }

    fun clearAnimationAction() {
        _animationState.update { it.copy(action = null) }
    }

    fun onPlayClicked() {
        countdownController?.get()?.let { controller ->
            when {
                controller.isPaused() -> resumeTimer(controller)
                !controller.isRunning() -> startNewSession(controller)
            }
        }

    }

    private fun resumeTimer(controller: TimerService) {
        controller.resume()
        _timerState.update {
            it.copy(
                resumedTime = controller.getRemainingTime(),
                status = TimerStatus.Running
            )
        }
        _animationState.update { it.copy(action = AnimationAction.Start) }
    }

    private fun startNewSession(controller: TimerService) {
        controller.start(settings.focusTimeMillis())
        _timerState.update {
            it.copy(
                status = TimerStatus.Running,
                currentRound = 1
            )
        }
        _animationState.update { it.copy(action = AnimationAction.Start) }
    }

    fun onPauseClicked() {
        countdownController?.get()?.let { controller ->
            controller.pause()
            _timerState.update { it.copy(status = TimerStatus.Paused) }
            _animationState.update { it.copy(action = AnimationAction.Pause) }
        }
    }

    fun onResetClicked() {
        countdownController?.get()?.let { controller ->
            handleReset(controller)
        }
    }

    fun onCharSelectEvent(event: CharacterSelectionEvent) {
        when (event) {
            is CharacterSelectionEvent.CharacterSelected -> {
                _selectedCharacter.value = event.character
                characterSettings.setSelectedCharacterId(event.character.id)
            }
        }
    }

    private fun handleReset(controller: TimerService) {
        controller.reset()
        _timerState.update {
            TimerState(
                timerText = settings.initialDisplayTime(),
                status = TimerStatus.Stopped,
                currentRound = 0,
                totalRounds = settings.totalRounds()
            )
        }
        _animationState.update {
            AnimationState(
                action = AnimationAction.Stop,
                currentFrame = 0,
                isPaused = false
            )
        }
    }

    private fun handleTimeUpdate(event: TimerUIEvent.TimeUpdate) {
        _timerState.update { it.copy(timerText = formatTime(event.remainingMillis)) }
    }

    private fun handleIntervalFinished(event: TimerUIEvent.IntervalFinished) {
        val nextDuration = calculateNextDuration(event.nextInterval)
        _timerState.update {
            it.copy(
                interval = Interval(event.currentRound, event.nextInterval, nextDuration),
                currentRound = event.currentRound
            )
        }
        if (!settings.isMuteEnabled()) {
            viewModelScope.launch { _intervalSoundEvent.emit(SoundType.INTERVAL_CHANGE) }
        }
    }

    private fun handleSessionFinished() {
        _animationState.update { it.copy(action = AnimationAction.Stop) }
        countdownController?.get()?.let { handleReset(it) }
        resetTimerState()
    }

    private fun resetTimerState() {
        _timerState.update {
            TimerState(
                timerText = settings.initialDisplayTime(),
                status = TimerStatus.Stopped,
                currentRound = 0,
                totalRounds = settings.totalRounds()
            )
        }
        _animationState.update {
            AnimationState(
                action = AnimationAction.Stop,
                currentFrame = 0,
                isPaused = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unbindService(serviceConnection)
    }

}
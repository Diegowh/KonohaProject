package com.diegowh.konohaproject.ui.timer

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.domain.character.Character
import com.diegowh.konohaproject.domain.character.CharacterSelectionEvent
import com.diegowh.konohaproject.domain.main.App
import com.diegowh.konohaproject.domain.settings.TimerSettingsRepository
import com.diegowh.konohaproject.domain.timer.TimerService
import com.diegowh.konohaproject.domain.timer.TimerState
import com.diegowh.konohaproject.domain.timer.TimerUIEvent
import com.diegowh.konohaproject.utils.animation.AnimationAction
import com.diegowh.konohaproject.utils.animation.AnimationState
import com.diegowh.konohaproject.utils.sound.SoundType
import com.diegowh.konohaproject.utils.timer.Interval
import com.diegowh.konohaproject.utils.timer.IntervalType
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

    private val _uiState = MutableStateFlow(TimerUIState())
    private val _intervalSoundEvent = MutableSharedFlow<SoundType>()

    val uiState: StateFlow<TimerUIState> = _uiState.asStateFlow()
    val intervalSoundEvent: SharedFlow<SoundType> = _intervalSoundEvent.asSharedFlow()

    // TODO: Borrar esto de aqui cuando implemente la carga de las prefs y el default
    private val defaultCharacter = Character(
        1,
        "Sakura",
        R.drawable.sakura_miniatura,
        R.array.test_sakura_focus_frames,
        R.array.test_sakura_break_frames,
        R.array.test_sakura_focus_palette,
        R.array.test_sakura_break_palette
    )

    private val _selectedCharacter = MutableStateFlow(defaultCharacter)
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
                        is TimerUIEvent.TimeUpdate -> {
                            _uiState.update {
                                it.copy(
                                    timerText = formatTime(event.remainingMillis)
                                )
                            }
                        }

                        is TimerUIEvent.IntervalFinished -> {
                            val nextDuration = calculateNextDuration(event.nextInterval)
                            _uiState.update {
                                it.copy(
                                    interval = Interval(
                                        event.currentRound,
                                        event.nextInterval,
                                        nextDuration
                                    ),
                                    currentRound = event.currentRound
                                )
                            }

                            if (!settings.isMuteEnabled()) {
                                _intervalSoundEvent.emit(SoundType.INTERVAL_CHANGE)
                            }
                        }

                        TimerUIEvent.SessionFinished -> {
                            _uiState.update {
                                it.copy(
                                    animationAction = AnimationAction.Stop
                                )
                            }
                            handleReset(controller)
                        }
                    }
                }
            }
            updateUIWithCurrentState(controller)
            _uiState.update { it.copy(animationAction = AnimationAction.Stop) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            countdownController = null
        }
    }

    init {
        bindService()
        initState()
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
        _uiState.value = TimerUIState(
            timerText = settings.initialDisplayTime(),
            state = TimerState.Stopped,
            currentRound = 0,
            totalRounds = settings.totalRounds()
        )
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
        _uiState.update {
            it.copy(
                state = when {
                    controller.isRunning() && !controller.isPaused() -> TimerState.Running
                    controller.isPaused() -> TimerState.Paused
                    else -> TimerState.Stopped
                }
            )
        }
    }

    fun clearAnimationAction() {
        _uiState.update { it.copy(animationAction = null) }
    }

    fun onPlayClicked() {
        countdownController?.get()?.let { controller ->
            when {
                controller.isPaused() -> {
                    controller.resume()
                    _uiState.update {
                        it.copy(
                            resumedTime = controller.getRemainingTime(),
                            animationAction = AnimationAction.Start,
                            state = TimerState.Running
                        )
                    }
                }

                !controller.isRunning() -> {
                    controller.start(settings.focusTimeMillis())
                    _uiState.update {
                        it.copy(
                            animationAction = AnimationAction.Start,
                            state = TimerState.Running,
                            currentRound = 1
                        )
                    }
                }
            }
        }
    }

    fun onPauseClicked() {
        countdownController?.get()?.let { controller ->
            controller.pause()
            _uiState.update {
                it.copy(
                    animationAction = AnimationAction.Pause,
                    state = TimerState.Paused
                )
            }
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
                println("Personaje seleccionado: ${event.character.name}")
                _selectedCharacter.value = event.character
                _uiState.update { old ->
                    old.copy(selectedCharacter = event.character)
                }
            }
        }
    }

    private fun handleReset(controller: TimerService) {
        controller.reset()
        _uiState.value = TimerUIState(
            timerText = settings.initialDisplayTime(),
            state = TimerState.Stopped,
            currentRound = 0,
            totalRounds = settings.totalRounds(),
            animationAction = AnimationAction.Stop,
            animationState = AnimationState(0, isPaused = false)
        )
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unbindService(serviceConnection)
    }

}
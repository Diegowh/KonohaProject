package com.diegowh.konohaproject.feature.timer.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.core.animation.AnimationAction
import com.diegowh.konohaproject.core.app.App
import com.diegowh.konohaproject.core.notification.ServiceNotifier
import com.diegowh.konohaproject.core.sound.SoundPlayer
import com.diegowh.konohaproject.core.sound.SoundType
import com.diegowh.konohaproject.feature.character.domain.repository.CharacterSettingsRepository
import com.diegowh.konohaproject.feature.timer.data.service.TimerService
import com.diegowh.konohaproject.feature.timer.data.service.TimerServiceConnection
import com.diegowh.konohaproject.feature.timer.domain.model.Interval
import com.diegowh.konohaproject.feature.timer.domain.model.IntervalType
import com.diegowh.konohaproject.feature.timer.domain.model.TimerStatus
import com.diegowh.konohaproject.feature.timer.domain.model.TimerUIEvent
import com.diegowh.konohaproject.feature.timer.domain.repository.TimerSettingsRepository
import com.diegowh.konohaproject.feature.character.presentation.model.AnimationUiState
import com.diegowh.konohaproject.feature.character.presentation.model.CharacterUiState
import com.diegowh.konohaproject.feature.timer.presentation.model.IntervalDialogState
import com.diegowh.konohaproject.feature.timer.presentation.model.ScreenUiState
import com.diegowh.konohaproject.feature.timer.presentation.model.TimerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

class TimerViewModel(app: Application) : AndroidViewModel(app) {

    // Settings
    private val characterSettings: CharacterSettingsRepository =
        (getApplication() as App).characterSettings
    private val timerSettings: TimerSettingsRepository =
        (getApplication() as App).timerSettings

    // State flows
    private val _timerState = MutableStateFlow(
        TimerUiState(
            timerText = timerSettings.initialDisplayTime(),
            status = TimerStatus.Stopped,
            currentRound = 0,
            totalRounds = timerSettings.totalRounds(),
            interval = Interval(0, IntervalType.FOCUS, timerSettings.focusTimeMillis())
        )
    )
    private val _characterState = MutableStateFlow(
        CharacterUiState(
            character = characterSettings.getById(characterSettings.getSelectedCharacterId())
        )
    )
    private val _animationState = MutableStateFlow(AnimationUiState())
    private val _screenState = MutableStateFlow(ScreenUiState())

    val timerState: StateFlow<TimerUiState> = _timerState.asStateFlow()
    val characterState: StateFlow<CharacterUiState> = _characterState.asStateFlow()
    val animationState: StateFlow<AnimationUiState> = _animationState.asStateFlow()
    val screenState: StateFlow<ScreenUiState> = _screenState.asStateFlow()

    // flag para contemplar primer intervalo
    private var hasStarted = false

    private val soundPlayer: SoundPlayer = SoundPlayer(getApplication()).apply {
        loadSound(SoundType.INTERVAL_CHANGE, R.raw.interval_finished)
    }

    private val serviceNotifier = ServiceNotifier(getApplication())
    private val serviceConnection = TimerServiceConnection(viewModelScope) { event ->
        when (event) {
            is TimerUIEvent.TimeUpdate       -> handleTimeUpdate(event)
            is TimerUIEvent.IntervalFinished -> handleIntervalFinished(event)
            TimerUIEvent.SessionFinished     -> handleSessionFinished()
        }
    }

    init {
        serviceConnection.connect(getApplication())
    }

    fun onEvent(event: TimerEvent) {
        when (event) {
            is TimerEvent.TimerAction.Play    -> onPlayClicked()
            is TimerEvent.TimerAction.Pause   -> onPauseClicked()
            is TimerEvent.TimerAction.Reset   -> onResetClicked()
            is TimerEvent.CharacterAction.Select    -> onCharacterSelected(event)
            is TimerEvent.SettingsAction.UpdateSettings -> onSettingsUpdated(event)
            is TimerEvent.SettingsAction.Reset   -> onSettingsReset()
        }
    }

    private fun onCharacterSelected(event: TimerEvent.CharacterAction.Select) {
        if (event.character.id != _characterState.value.character.id) {
            val isRunning = _timerState.value.status == TimerStatus.Running
            val currentIntervalType = _timerState.value.interval?.type ?: IntervalType.FOCUS

            // Actualiza el state del Character
            _characterState.update { currentState ->
                currentState.copy(
                    character = event.character,
                    currentIntervalType = currentIntervalType
                )
            }
            
            // Actualiza el estado de la animacion
            _animationState.update { currentState ->
                currentState.copy(
                    // Si está running, arranca la animación
                    action = if (isRunning) AnimationAction.Start else null,
                    // Siempre actualiza los frames y el theme
                    shouldUpdateFrames = true,
                    currentIntervalType = currentIntervalType
                )
            }

            // Guarda el character en el repository
            characterSettings.setSelectedCharacterId(event.character.id)
        }
    }

    private fun onSettingsUpdated(event: TimerEvent.SettingsAction.UpdateSettings) {
        // Actualiza el repository
        timerSettings.updateSettings(
            focus = event.focusMinutes,
            shortBreak = event.shortBreakMinutes,
            longBreak = event.longBreakMinutes,
            rounds = event.totalRounds,
            autorun = event.isAutorunEnabled,
            mute = event.isMuteEnabled
        )

        // Resetea el timer
        serviceConnection.service?.let(::handleReset)
    }

    private fun onSettingsReset() {
        timerSettings.resetToDefaults()
        serviceConnection.service?.let(::handleReset)
    }

    private fun onPlayClicked() {
        serviceConnection.service?.let { controller ->
            when {
                controller.isPaused() -> resumeTimer(controller)
                !controller.isRunning() -> startNewSession(controller)
            }
        }
    }

    private fun resumeTimer(controller: TimerService) {
        controller.resume()

        _timerState.update { currentState ->
            currentState.copy(
                resumedTime = controller.getRemainingTime(),
                status = TimerStatus.Running
            )
        }
        _animationState.update { currentState ->
            currentState.copy(action = AnimationAction.Start)
        }
    }

    private fun startNewSession(controller: TimerService) {
        controller.start(timerSettings.focusTimeMillis())

        _timerState.update { currentState ->
            currentState.copy(
                status = TimerStatus.Running,
                currentRound = 1
            )
        }

        _animationState.update { currentState ->
            currentState.copy(action = AnimationAction.Start)
        }
    }

    private fun onPauseClicked() {
        serviceConnection.service?.let { controller ->
            controller.pause()

            _timerState.update { currentState ->
                currentState.copy(status = TimerStatus.Paused)
            }

            _animationState.update { currentState ->
                currentState.copy(action = AnimationAction.Pause)
            }
        }
    }

    private fun onResetClicked() {
        serviceConnection.service?.let(::handleReset)
    }

    private fun handleReset(controller: TimerService) {
        hasStarted = false
        controller.reset()

        _timerState.update { currentState ->
            currentState.copy(
                timerText = timerSettings.initialDisplayTime(),
                status = TimerStatus.Stopped,
                currentRound = 0,
                totalRounds = timerSettings.totalRounds(),
                interval = Interval(0, IntervalType.FOCUS, timerSettings.focusTimeMillis())
            )
        }

        _animationState.update { currentState ->
            currentState.copy(
                action = AnimationAction.Stop,
                shouldUpdateFrames = true
            )
        }

        _characterState.update { currentState ->
            currentState.copy(currentIntervalType = IntervalType.FOCUS)
        }
    }

    private fun handleTimeUpdate(event: TimerUIEvent.TimeUpdate) {
        // actualiza solo el texto
        _timerState.update { currentState ->
            currentState.copy(timerText = formatTime(event.remainingMillis))
        }
    }

    private fun handleIntervalFinished(event: TimerUIEvent.IntervalFinished) {
        val notify = hasStarted
        val shouldPlaySound = notify && !timerSettings.isMuteEnabled()

        if (shouldPlaySound) {
            soundPlayer.play(SoundType.INTERVAL_CHANGE)
        }

        if (notify) {
            serviceNotifier.sendIntervalFinishedNotification(event.nextInterval)
            
            // Si el autorun esta desactivado
            if (!timerSettings.isAutorunEnabled()) {
                val next = calculateNextDuration(event.nextInterval)

                _timerState.update { state ->
                    state.copy(
                        interval = Interval(event.currentRound, event.nextInterval, next),
                        currentRound = event.currentRound,
                        status = TimerStatus.Stopped,
                        timerText = formatTime(next)
                    )
                }

                _animationState.update { state ->
                    state.copy(
                        shouldUpdateFrames = true,
                        currentIntervalType = event.nextInterval
                    )
                }

                _characterState.update { state ->
                    state.copy(currentIntervalType = event.nextInterval)
                }
                
                // Muestra el dialog de fin de intervalo
                _screenState.update { state ->
                    state.copy(
                        intervalDialog = IntervalDialogState(
                            showDialog = true,
                            intervalType = event.nextInterval
                        )
                    )
                }
                
                serviceConnection.service?.pause()
                return
            }
        }
        
        hasStarted = true
        val next = calculateNextDuration(event.nextInterval)

        _timerState.update { state ->
            state.copy(
                interval = Interval(event.currentRound, event.nextInterval, next),
                currentRound = event.currentRound
            )
        }

        _animationState.update { state ->
            state.copy(
                shouldUpdateFrames = true,
                currentIntervalType = event.nextInterval
            )
        }

        _characterState.update { state ->
            state.copy(currentIntervalType = event.nextInterval)
        }
    }

    private fun handleSessionFinished() {
        serviceNotifier.sendSessionFinishedNotification()

        _animationState.update { state ->
            state.copy(
                action = AnimationAction.Stop,
                shouldUpdateFrames = true
            )
        }

        // Muestra el dialog de final de sesión
        _screenState.update { state ->
            state.copy(sessionDialogVisible = true)
        }
        
        serviceConnection.service?.let(::handleReset)
    }

    private fun formatTime(remainingMillis: Long): String {
        val secs = (remainingMillis + 500) / 1000
        val min = secs / 60
        val sec = secs % 60
        return String.format(Locale.US, "%02d:%02d", min, sec)
    }

    private fun calculateNextDuration(type: IntervalType): Long {
        return when (type) {
            IntervalType.FOCUS       -> timerSettings.focusTimeMillis()
            IntervalType.SHORT_BREAK -> timerSettings.shortBreakTimeMillis()
            IntervalType.LONG_BREAK  -> timerSettings.longBreakTimeMillis()
        }
    }

    fun clearAnimationAction() {
        _animationState.update { state ->
            state.copy(
                action = null,
                shouldUpdateFrames = false
            )
        }
    }

    fun onDialogContinueClicked() {
        serviceConnection.service?.resume()
        
        // Actualiza el timer, animacion y screen
        _timerState.update { state ->
            state.copy(status = TimerStatus.Running)
        }
        _animationState.update { state ->
            state.copy(
                action = AnimationAction.Start,
                shouldUpdateFrames = true
            )
        }
        _screenState.update { state ->
            state.copy(intervalDialog = IntervalDialogState())
        }
    }

    fun onDialogSkipClicked() {
        // Limpia el dialog y skipea el intervalo
        _screenState.update { state ->
            state.copy(intervalDialog = IntervalDialogState())
        }
        serviceConnection.service?.skip()
    }

    fun onSessionDialogDismissed() {
        _screenState.update { state ->
            state.copy(sessionDialogVisible = false)
        }
    }

    fun onDialogShown() {
        _screenState.update { state ->
            state.copy(
                intervalDialog = IntervalDialogState()
            )
        }
    }

    override fun onCleared() {
        soundPlayer.release()
        serviceConnection.disconnect(getApplication())
        super.onCleared()
    }
}
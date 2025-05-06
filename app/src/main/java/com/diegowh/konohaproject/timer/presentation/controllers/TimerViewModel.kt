package com.diegowh.konohaproject.timer.presentation.controllers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.app.App
import com.diegowh.konohaproject.core.animation.AnimationAction
import com.diegowh.konohaproject.core.notification.ServiceNotifier
import com.diegowh.konohaproject.timer.data.sound.SoundType
import com.diegowh.konohaproject.timer.domain.model.Interval
import com.diegowh.konohaproject.timer.domain.model.IntervalType
import com.diegowh.konohaproject.settings.domain.repository.CharacterSettingsRepository
import com.diegowh.konohaproject.settings.domain.repository.TimerSettingsRepository
import com.diegowh.konohaproject.timer.data.sound.SoundPlayer
import com.diegowh.konohaproject.timer.data.service.TimerService
import com.diegowh.konohaproject.timer.domain.model.TimerStatus
import com.diegowh.konohaproject.timer.data.service.TimerServiceConnectorImpl
import com.diegowh.konohaproject.timer.presentation.state.AnimationState
import com.diegowh.konohaproject.timer.presentation.state.IntervalDialogState
import com.diegowh.konohaproject.timer.presentation.state.SettingsState
import com.diegowh.konohaproject.timer.presentation.events.TimerEvent
import com.diegowh.konohaproject.timer.presentation.state.TimerScreenState
import com.diegowh.konohaproject.timer.presentation.state.TimerState
import com.diegowh.konohaproject.timer.presentation.events.TimerUIEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale


class TimerViewModel(app: Application) : AndroidViewModel(app) {

    private val characterSettings: CharacterSettingsRepository =
        (getApplication() as App).characterSettings

    private val timerSettings: TimerSettingsRepository =
        (getApplication() as App).timerSettings

    private val _state = MutableStateFlow(
        TimerScreenState(
            character = characterSettings.getById(
                characterSettings.getSelectedCharacterId()
            ),
            settings = SettingsState(
                isMuteEnabled = timerSettings.isMuteEnabled(),
                isAutorunEnabled = timerSettings.isAutorunEnabled()
            )
        )
    )
    val state: StateFlow<TimerScreenState> = _state.asStateFlow()

    private var hasStarted = false
    private val soundPlayer: SoundPlayer = SoundPlayer(getApplication()).apply {
        loadSound(SoundType.INTERVAL_CHANGE, R.raw.interval_finished)
    }

    private val serviceNotifier = ServiceNotifier(getApplication())
    private val serviceConnector: com.diegowh.konohaproject.timer.domain.repository.TimerServiceConnector =
        TimerServiceConnectorImpl(viewModelScope)

    init {
        connectToTimerService()
        initState()
    }

    private fun connectToTimerService() {
        serviceConnector.connect(getApplication()) { event ->
            when (event) {
                is TimerUIEvent.TimeUpdate -> handleTimeUpdate(event)
                is TimerUIEvent.IntervalFinished -> handleIntervalFinished(event)
                TimerUIEvent.SessionFinished -> handleSessionFinished()
            }
        }
    }

    private fun initState() {
        _state.update { currentState ->
            currentState.copy(
                timer = TimerState(
                    timerText = timerSettings.initialDisplayTime(),
                    status = TimerStatus.Stopped,
                    currentRound = 0,
                    totalRounds = timerSettings.totalRounds()
                ),
                animation = AnimationState(),
            )
        }
    }

    fun onEvent(event: TimerEvent) {
        when (event) {
            is TimerEvent.TimerAction.Play -> onPlayClicked()
            is TimerEvent.TimerAction.Pause -> onPauseClicked()
            is TimerEvent.TimerAction.Reset -> onResetClicked()
            is TimerEvent.CharacterAction.Select -> onCharacterSelected(event)
            is TimerEvent.SettingsAction.UpdateSettings -> onSettingsUpdated(event)
            is TimerEvent.SettingsAction.Reset -> onSettingsReset()
        }
    }

    private fun onCharacterSelected(event: TimerEvent.CharacterAction.Select) {
        if (event.character.id != _state.value.character.id) {
            val isTimerRunning = _state.value.timer.status == TimerStatus.Running

            _state.update { currentState ->
                currentState.copy(
                    character = event.character,
                    animation = if (isTimerRunning) {
                        currentState.animation.copy(action = AnimationAction.Start)
                    } else {
                        currentState.animation
                    }
                )
            }
            characterSettings.setSelectedCharacterId(event.character.id)
        }
    }

    private fun onSettingsUpdated(event: TimerEvent.SettingsAction.UpdateSettings) {
        timerSettings.updateSettings(
            focus = event.focusMinutes,
            shortBreak = event.shortBreakMinutes,
            longBreak = event.longBreakMinutes,
            rounds = event.totalRounds,
            autorun = event.isAutorunEnabled,
            mute = event.isMuteEnabled
        )
        _state.update { currentState ->
            currentState.copy(
                settings = SettingsState(
                    isMuteEnabled = event.isMuteEnabled,
                    isAutorunEnabled = event.isAutorunEnabled
                )
            )
        }
        serviceConnector.getService()?.let { handleReset(it) }
    }

    private fun onSettingsReset() {
        timerSettings.resetToDefaults()
        _state.update { currentState ->
            currentState.copy(
                settings = SettingsState(
                    isMuteEnabled = timerSettings.isMuteEnabled(),
                    isAutorunEnabled = timerSettings.isAutorunEnabled()
                )
            )
        }
        serviceConnector.getService()?.let { handleReset(it) }
    }

    private fun onPlayClicked() {
        serviceConnector.getService()?.let { controller ->
            when {
                controller.isPaused() -> resumeTimer(controller)
                !controller.isRunning() -> startNewSession(controller)
            }
        }
    }

    private fun resumeTimer(controller: TimerService) {
        controller.resume()
        _state.update { currentState ->
            currentState.copy(
                timer = currentState.timer.copy(
                    resumedTime = controller.getRemainingTime(),
                    status = TimerStatus.Running
                ),
                animation = currentState.animation.copy(action = AnimationAction.Start)
            )
        }
    }

    private fun startNewSession(controller: TimerService) {
        controller.start(timerSettings.focusTimeMillis())
        _state.update { currentState ->
            currentState.copy(
                timer = currentState.timer.copy(
                    status = TimerStatus.Running,
                    currentRound = 1
                ),
                animation = currentState.animation.copy(action = AnimationAction.Start)
            )
        }
    }

    private fun onPauseClicked() {
        serviceConnector.getService()?.let { controller ->
            controller.pause()
            _state.update { currentState ->
                currentState.copy(
                    timer = currentState.timer.copy(status = TimerStatus.Paused),
                    animation = currentState.animation.copy(action = AnimationAction.Pause)
                )
            }
        }
    }

    private fun onResetClicked() {
        serviceConnector.getService()?.let { controller ->
            handleReset(controller)
        }
    }

    private fun handleReset(controller: TimerService) {
        hasStarted = false
        controller.reset()
        _state.update { currentState ->
            currentState.copy(
                timer = TimerState(
                    timerText = timerSettings.initialDisplayTime(),
                    status = TimerStatus.Stopped,
                    currentRound = 0,
                    totalRounds = timerSettings.totalRounds()
                ),
                animation = AnimationState(
                    action = AnimationAction.Stop,
                    currentFrame = 0,
                    isPaused = false
                )
            )
        }
    }

    private fun handleTimeUpdate(event: TimerUIEvent.TimeUpdate) {
        _state.update { currentState ->
            currentState.copy(
                timer = currentState.timer.copy(
                    timerText = formatTime(event.remainingMillis)
                )
            )
        }
    }

    private fun handleIntervalFinished(event: TimerUIEvent.IntervalFinished) {
        val shouldNotify = hasStarted

        if (shouldNotify && !timerSettings.isMuteEnabled()) {
            soundPlayer.play(SoundType.INTERVAL_CHANGE)
        }

        // evita notificar al iniciar el timer por primera vez
        if (shouldNotify) {
            serviceNotifier.sendIntervalFinishedNotification(event.nextInterval)

            if (!timerSettings.isAutorunEnabled()) {
                val nextDuration = calculateNextDuration(event.nextInterval)

                _state.update { state ->
                    state.copy(
                        timer = state.timer.copy(
                            interval = Interval(
                                event.currentRound,
                                event.nextInterval,
                                nextDuration
                            ),
                            currentRound = event.currentRound,
                            status = TimerStatus.Stopped,
                            timerText = formatTime(nextDuration)
                        ),
                        animation = state.animation.copy(
                            shouldUpdateFrames = true
                        ),
                        intervalDialog = IntervalDialogState(
                            showDialog = true,
                            intervalType = event.nextInterval
                        )
                    )
                }
                serviceConnector.getService()?.pause()
                return
            }
        }

        hasStarted = true
        val nextDuration = calculateNextDuration(event.nextInterval)

        _state.update { currentState ->
            currentState.copy(
                timer = currentState.timer.copy(
                    interval = Interval(event.currentRound, event.nextInterval, nextDuration),
                    currentRound = event.currentRound
                ),
                animation = currentState.animation.copy(
                    shouldUpdateFrames = true
                )
            )
        }
    }

    private fun handleSessionFinished() {
        serviceNotifier.sendSessionFinishedNotification()

        _state.update { currentState ->
            currentState.copy(
                animation = currentState.animation.copy(action = AnimationAction.Stop),
                sessionDialogVisible = true
            )
        }
        serviceConnector.getService()?.let { handleReset(it) }
    }

    private fun formatTime(remainingMillis: Long): String {
        val totalSeconds = (remainingMillis + 500) / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    private fun calculateNextDuration(intervalType: IntervalType): Long {
        return when (intervalType) {
            IntervalType.FOCUS -> timerSettings.focusTimeMillis()
            IntervalType.SHORT_BREAK -> timerSettings.shortBreakTimeMillis()
            IntervalType.LONG_BREAK -> timerSettings.longBreakTimeMillis()
        }
    }

    fun clearAnimationAction() {
        _state.update { currentState ->
            currentState.copy(
                animation = currentState.animation.copy(
                    action = null,
                    shouldUpdateFrames = false
                )
            )
        }
    }

    fun onDialogContinueClicked() {
        serviceConnector.getService()?.resume()
        _state.update { currentState ->
            currentState.copy(
                timer = currentState.timer.copy(
                    status = TimerStatus.Running
                ),
                animation = currentState.animation.copy(
                    action = AnimationAction.Start,
                    shouldUpdateFrames = true
                ),
                intervalDialog = IntervalDialogState()  // showDialog = false
            )
        }
    }

    fun onDialogSkipClicked() {
        _state.update { state ->
            state.copy(intervalDialog = IntervalDialogState())
        }
        serviceConnector.getService()?.skip()
    }

    fun onSessionDialogDismissed() {
        _state.update { currentState ->
            currentState.copy(
                sessionDialogVisible = false
            )
        }
    }

    fun onDialogShown() {
        _state.update { current ->
            current.copy(
                intervalDialog = current.intervalDialog.copy(
                    showDialog = false
                )
            )
        }
    }

    override fun onCleared() {
        soundPlayer.release()
        serviceConnector.disconnect(getApplication())
        super.onCleared()
    }
}
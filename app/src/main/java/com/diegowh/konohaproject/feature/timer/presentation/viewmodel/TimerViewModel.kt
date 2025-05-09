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
import com.diegowh.konohaproject.feature.settings.presentation.model.SettingsState
import com.diegowh.konohaproject.feature.timer.data.service.TimerService
import com.diegowh.konohaproject.feature.timer.data.service.TimerServiceConnection
import com.diegowh.konohaproject.feature.timer.domain.model.Interval
import com.diegowh.konohaproject.feature.timer.domain.model.IntervalType
import com.diegowh.konohaproject.feature.timer.domain.model.TimerStatus
import com.diegowh.konohaproject.feature.timer.domain.model.TimerUIEvent
import com.diegowh.konohaproject.feature.timer.domain.repository.TimerSettingsRepository
import com.diegowh.konohaproject.feature.timer.presentation.model.AnimationState
import com.diegowh.konohaproject.feature.timer.presentation.model.IntervalDialogState
import com.diegowh.konohaproject.feature.timer.presentation.model.TimerScreenState
import com.diegowh.konohaproject.feature.timer.presentation.model.TimerState
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
    private val serviceConnection = TimerServiceConnection(viewModelScope) { event ->
        when (event) {
            is TimerUIEvent.TimeUpdate       -> handleTimeUpdate(event)
            is TimerUIEvent.IntervalFinished -> handleIntervalFinished(event)
            TimerUIEvent.SessionFinished     -> handleSessionFinished()
        }
    }

    init {
        serviceConnection.connect(getApplication())
        initState()
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
            is TimerEvent.TimerAction.Play    -> onPlayClicked()
            is TimerEvent.TimerAction.Pause   -> onPauseClicked()
            is TimerEvent.TimerAction.Reset   -> onResetClicked()
            is TimerEvent.CharacterAction.Select    -> onCharacterSelected(event)
            is TimerEvent.SettingsAction.UpdateSettings -> onSettingsUpdated(event)
            is TimerEvent.SettingsAction.Reset   -> onSettingsReset()
        }
    }

    private fun onCharacterSelected(event: TimerEvent.CharacterAction.Select) {
        if (event.character.id != _state.value.character.id) {
            val isRunning = _state.value.timer.status == TimerStatus.Running
            _state.update { currentState ->
                currentState.copy(
                    character = event.character,
                    animation = if (isRunning) currentState.animation.copy(action = AnimationAction.Start)
                    else currentState.animation
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
        serviceConnection.service?.let(::handleReset)
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
        serviceConnection.service?.let(::handleReset)
    }

    private fun onPlayClicked() {
        serviceConnection.service?.let { controller ->
            when {
                controller.isPaused()                 -> resumeTimer(controller)
                !controller.isRunning()               -> startNewSession(controller)
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
        serviceConnection.service?.let { controller ->
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
        serviceConnection.service?.let(::handleReset)
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
        val notify = hasStarted
        if (notify && !timerSettings.isMuteEnabled()) {
            soundPlayer.play(SoundType.INTERVAL_CHANGE)
        }
        if (notify) {
            serviceNotifier.sendIntervalFinishedNotification(event.nextInterval)
            if (!timerSettings.isAutorunEnabled()) {
                val next = calculateNextDuration(event.nextInterval)
                _state.update { st ->
                    st.copy(
                        timer = st.timer.copy(
                            interval = Interval(event.currentRound, event.nextInterval, next),
                            currentRound = event.currentRound,
                            status = TimerStatus.Stopped,
                            timerText = formatTime(next)
                        ),
                        animation = st.animation.copy(shouldUpdateFrames = true),
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
        _state.update { cs ->
            cs.copy(
                timer = cs.timer.copy(
                    interval = Interval(event.currentRound, event.nextInterval, next),
                    currentRound = event.currentRound
                ),
                animation = cs.animation.copy(shouldUpdateFrames = true)
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
        _state.update { cs ->
            cs.copy(
                animation = cs.animation.copy(
                    action = null,
                    shouldUpdateFrames = false
                )
            )
        }
    }

    fun onDialogContinueClicked() {
        serviceConnection.service?.resume()
        _state.update { cs ->
            cs.copy(
                timer = cs.timer.copy(status = TimerStatus.Running),
                animation = cs.animation.copy(action = AnimationAction.Start, shouldUpdateFrames = true),
                intervalDialog = IntervalDialogState()
            )
        }
    }

    fun onDialogSkipClicked() {
        _state.update { cs -> cs.copy(intervalDialog = IntervalDialogState()) }
        serviceConnection.service?.skip()
    }

    fun onSessionDialogDismissed() {
        _state.update { cs -> cs.copy(sessionDialogVisible = false) }
    }

    fun onDialogShown() {
        _state.update { cs -> cs.copy(intervalDialog = cs.intervalDialog.copy(showDialog = false)) }
    }

    override fun onCleared() {
        soundPlayer.release()
        serviceConnection.disconnect(getApplication())
        super.onCleared()
    }
}
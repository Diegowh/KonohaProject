package com.diegowh.konohaproject.ui.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.app.App
import com.diegowh.konohaproject.core.animation.AnimationAction
import com.diegowh.konohaproject.core.service.ServiceNotifier
import com.diegowh.konohaproject.core.sound.SoundType
import com.diegowh.konohaproject.core.timer.Interval
import com.diegowh.konohaproject.core.timer.IntervalType
import com.diegowh.konohaproject.domain.settings.CharacterSettingsRepository
import com.diegowh.konohaproject.domain.settings.TimerSettingsRepository
import com.diegowh.konohaproject.domain.sound.SoundPlayer
import com.diegowh.konohaproject.domain.timer.TimerScreenEvent
import com.diegowh.konohaproject.domain.timer.TimerService
import com.diegowh.konohaproject.domain.timer.TimerServiceConnector
import com.diegowh.konohaproject.domain.timer.TimerServiceConnectorImpl
import com.diegowh.konohaproject.domain.timer.TimerStatus
import com.diegowh.konohaproject.domain.timer.TimerUIEvent
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
    private val serviceConnector: TimerServiceConnector = TimerServiceConnectorImpl(viewModelScope)
    
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
    
    fun onEvent(event: TimerScreenEvent) {
        when (event) {
            is TimerScreenEvent.TimerEvent.Play -> onPlayClicked()
            is TimerScreenEvent.TimerEvent.Pause -> onPauseClicked()
            is TimerScreenEvent.TimerEvent.Reset -> onResetClicked()
            is TimerScreenEvent.CharacterEvent.Select -> onCharacterSelected(event)
            is TimerScreenEvent.SettingsEvent.UpdateSettings -> onSettingsUpdated(event)
            is TimerScreenEvent.SettingsEvent.Reset -> onSettingsReset()
        }
    }
    
    private fun onCharacterSelected(event: TimerScreenEvent.CharacterEvent.Select) {
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
    
    private fun onSettingsUpdated(event: TimerScreenEvent.SettingsEvent.UpdateSettings) {
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

        if (shouldNotify) {
            serviceNotifier.sendIntervalFinishedNotification(event.nextInterval)
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
    
    override fun onCleared() {
        soundPlayer.release()
        serviceConnector.disconnect(getApplication())
        super.onCleared()
    }
}
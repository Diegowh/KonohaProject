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
import com.diegowh.konohaproject.app.App
import com.diegowh.konohaproject.core.animation.AnimationAction
import com.diegowh.konohaproject.core.sound.SoundType
import com.diegowh.konohaproject.core.timer.Interval
import com.diegowh.konohaproject.core.timer.IntervalType
import com.diegowh.konohaproject.domain.settings.CharacterSettingsRepository
import com.diegowh.konohaproject.domain.settings.TimerSettingsRepository
import com.diegowh.konohaproject.domain.sound.SoundPlayer
import com.diegowh.konohaproject.domain.timer.TimerScreenEvent
import com.diegowh.konohaproject.domain.timer.TimerService
import com.diegowh.konohaproject.domain.timer.TimerStatus
import com.diegowh.konohaproject.domain.timer.TimerUIEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.Locale

class TimerViewModel(app: Application) : AndroidViewModel(app) {

    private val characterSettings: CharacterSettingsRepository =
        (getApplication() as App).characterSettings

    private val _state = MutableStateFlow(
        TimerScreenState(
            character = characterSettings.getById(
                characterSettings.getSelectedCharacterId()
            ),
            settings = SettingsState(
                isMuteEnabled = (getApplication() as App).timerSettings.isMuteEnabled(),
                isAutorunEnabled = (getApplication() as App).timerSettings.isAutorunEnabled()
            )
        )
    )
    val state: StateFlow<TimerScreenState> = _state.asStateFlow()
    private val settings: TimerSettingsRepository =
        (getApplication() as App).timerSettings

    private var hasStarted = false
    private val soundPlayer: SoundPlayer = SoundPlayer(getApplication()).apply {
        loadSound(SoundType.INTERVAL_CHANGE, R.raw.bubble_tiny)
    }

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
            _state.update { currentState ->
                currentState.copy(
                    animation = currentState.animation.copy(action = AnimationAction.Stop)
                )
            }
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
        _state.update { currentState ->
            currentState.copy(
                timer = TimerState(
                    timerText = settings.initialDisplayTime(),
                    status = TimerStatus.Stopped,
                    currentRound = 0,
                    totalRounds = settings.totalRounds()
                ),
                animation = AnimationState(),
            )
        }
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
        _state.update { currentState ->
            currentState.copy(
                timer = currentState.timer.copy(
                    status = when {
                        controller.isRunning() && !controller.isPaused() -> TimerStatus.Running
                        controller.isPaused() -> TimerStatus.Paused
                        else -> TimerStatus.Stopped
                    }
                )
            )
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

    fun onEvent(event: TimerScreenEvent) {
        when (event) {
            is TimerScreenEvent.TimerEvent.Play -> onPlayClicked()
            is TimerScreenEvent.TimerEvent.Pause -> onPauseClicked()
            is TimerScreenEvent.TimerEvent.Reset -> onResetClicked()
            is TimerScreenEvent.CharacterEvent.Select -> {
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

            is TimerScreenEvent.SettingsEvent.UpdateSettings -> {
                settings.updateSettings(
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
                countdownController?.get()?.let { handleReset(it) }
            }

            is TimerScreenEvent.SettingsEvent.Reset -> {
                settings.resetToDefaults()
                _state.update { currentState ->
                    currentState.copy(
                        settings = SettingsState(
                            isMuteEnabled = settings.isMuteEnabled(),
                            isAutorunEnabled = settings.isAutorunEnabled()
                        )
                    )
                }
                countdownController?.get()?.let { handleReset(it) }
            }
        }
    }

    private fun onPlayClicked() {
        countdownController?.get()?.let { controller ->
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
        controller.start(settings.focusTimeMillis())
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
        countdownController?.get()?.let { controller ->
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
        countdownController?.get()?.let { controller ->
            handleReset(controller)
        }
    }

    private fun handleReset(controller: TimerService) {
        hasStarted = false
        controller.reset()
        _state.update { currentState ->
            currentState.copy(
                timer = TimerState(
                    timerText = settings.initialDisplayTime(),
                    status = TimerStatus.Stopped,
                    currentRound = 0,
                    totalRounds = settings.totalRounds()
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
        val shouldPlaySound = !settings.isMuteEnabled() && hasStarted

        if (shouldPlaySound ) {
            soundPlayer.play(SoundType.INTERVAL_CHANGE)
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
        _state.update { currentState ->
            currentState.copy(
                animation = currentState.animation.copy(action = AnimationAction.Stop),
            )
        }
        countdownController?.get()?.let { handleReset(it) }
    }

    override fun onCleared() {
        soundPlayer.release()
        super.onCleared()
        getApplication<Application>().unbindService(serviceConnection)
    }
}
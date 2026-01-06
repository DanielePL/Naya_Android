// app/src/main/java/com/example/myapplicationtest/viewmodels/RestTimerViewModel.kt

package com.example.menotracker.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class TimerData(
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val state: TimerState,
    val progress: Float
) {
    fun formatTime(): String {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}

class RestTimerViewModel : ViewModel() {

    private val _timerData = MutableStateFlow(
        TimerData(
            totalSeconds = 0,
            remainingSeconds = 0,
            state = TimerState.IDLE,
            progress = 0f
        )
    )
    val timerData: StateFlow<TimerData> = _timerData.asStateFlow()

    private var timerJob: Job? = null

    // Callback for vibration (will be set from Activity/Fragment)
    var onTimerComplete: (() -> Unit)? = null
    var onTick: ((Int) -> Unit)? = null  // Called every second with remaining time

    fun startTimer(durationSeconds: Int) {
        // Cancel any existing timer
        timerJob?.cancel()

        _timerData.value = TimerData(
            totalSeconds = durationSeconds,
            remainingSeconds = durationSeconds,
            state = TimerState.RUNNING,
            progress = 1f
        )

        timerJob = viewModelScope.launch {
            var remaining = durationSeconds

            while (remaining > 0 && _timerData.value.state == TimerState.RUNNING) {
                delay(1000L)
                remaining--

                val progress = if (durationSeconds > 0) {
                    remaining.toFloat() / durationSeconds.toFloat()
                } else {
                    0f
                }

                _timerData.value = _timerData.value.copy(
                    remainingSeconds = remaining,
                    progress = progress
                )

                // Callback for each tick
                onTick?.invoke(remaining)

                // Vibrate at 3, 2, 1 seconds
                if (remaining in 1..3) {
                    // Will trigger vibration in UI layer
                }
            }

            // Timer completed
            if (remaining == 0) {
                _timerData.value = _timerData.value.copy(
                    state = TimerState.COMPLETED,
                    remainingSeconds = 0,
                    progress = 0f
                )
                onTimerComplete?.invoke()
            }
        }
    }

    fun pauseTimer() {
        _timerData.value = _timerData.value.copy(state = TimerState.PAUSED)
        timerJob?.cancel()
    }

    fun resumeTimer() {
        if (_timerData.value.state == TimerState.PAUSED) {
            val currentRemaining = _timerData.value.remainingSeconds
            startTimer(currentRemaining)
        }
    }

    fun skipTimer() {
        timerJob?.cancel()
        _timerData.value = TimerData(
            totalSeconds = 0,
            remainingSeconds = 0,
            state = TimerState.IDLE,
            progress = 0f
        )
    }

    fun addTime(seconds: Int) {
        val currentData = _timerData.value
        if (currentData.state == TimerState.RUNNING || currentData.state == TimerState.PAUSED) {
            val newRemaining = currentData.remainingSeconds + seconds
            val newTotal = currentData.totalSeconds + seconds

            // Cancel current timer and restart with new time
            timerJob?.cancel()

            _timerData.value = TimerData(
                totalSeconds = newTotal,
                remainingSeconds = newRemaining,
                state = TimerState.RUNNING,
                progress = if (newTotal > 0) newRemaining.toFloat() / newTotal.toFloat() else 0f
            )

            // Restart timer with new remaining time
            timerJob = viewModelScope.launch {
                var remaining = newRemaining

                while (remaining > 0 && _timerData.value.state == TimerState.RUNNING) {
                    delay(1000L)
                    remaining--

                    val progress = if (newTotal > 0) {
                        remaining.toFloat() / newTotal.toFloat()
                    } else {
                        0f
                    }

                    _timerData.value = _timerData.value.copy(
                        remainingSeconds = remaining,
                        progress = progress
                    )

                    onTick?.invoke(remaining)
                }

                // Timer completed
                if (remaining == 0) {
                    _timerData.value = _timerData.value.copy(
                        state = TimerState.COMPLETED,
                        remainingSeconds = 0,
                        progress = 0f
                    )
                    onTimerComplete?.invoke()
                }
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerData.value = TimerData(
            totalSeconds = 0,
            remainingSeconds = 0,
            state = TimerState.IDLE,
            progress = 0f
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
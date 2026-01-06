// File: android/app/src/main/java/com/example/myapplicationtest/viewmodels/WorkoutTimerViewModel.kt

package com.example.menotracker.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WorkoutTimerData(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val totalSeconds: Int = 0,
    val startTimeMillis: Long = 0L,
    val targetDurationMinutes: Int? = null
) {
    fun formatTime(): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun isExceedingTarget(): Boolean {
        val targetMinutes = targetDurationMinutes ?: return false
        val currentMinutes = totalSeconds / 60
        return currentMinutes > targetMinutes
    }
}

class WorkoutTimerViewModel : ViewModel() {

    private val _workoutTimer = MutableStateFlow(WorkoutTimerData())
    val workoutTimer: StateFlow<WorkoutTimerData> = _workoutTimer.asStateFlow()

    private var timerJob: Job? = null

    /**
     * Start workout timer with optional target duration
     */
    fun startWorkoutTimer(targetDurationMinutes: Int? = null) {
        if (_workoutTimer.value.isRunning) return

        val startTime = System.currentTimeMillis()

        _workoutTimer.value = WorkoutTimerData(
            isRunning = true,
            isPaused = false,
            totalSeconds = 0,
            startTimeMillis = startTime,
            targetDurationMinutes = targetDurationMinutes
        )

        timerJob = viewModelScope.launch {
            while (_workoutTimer.value.isRunning && !_workoutTimer.value.isPaused) {
                delay(1000)

                val elapsed = (System.currentTimeMillis() - _workoutTimer.value.startTimeMillis) / 1000

                _workoutTimer.value = _workoutTimer.value.copy(
                    totalSeconds = elapsed.toInt()
                )
            }
        }
    }

    /**
     * Pause workout timer
     */
    fun pauseWorkoutTimer() {
        _workoutTimer.value = _workoutTimer.value.copy(
            isPaused = true
        )
    }

    /**
     * Resume workout timer
     */
    fun resumeWorkoutTimer() {
        if (!_workoutTimer.value.isPaused) return

        // Adjust start time to account for pause duration
        val pausedDuration = _workoutTimer.value.totalSeconds
        val newStartTime = System.currentTimeMillis() - (pausedDuration * 1000)

        _workoutTimer.value = _workoutTimer.value.copy(
            isPaused = false,
            startTimeMillis = newStartTime
        )

        // Restart timer job
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_workoutTimer.value.isRunning && !_workoutTimer.value.isPaused) {
                delay(1000)

                val elapsed = (System.currentTimeMillis() - _workoutTimer.value.startTimeMillis) / 1000

                _workoutTimer.value = _workoutTimer.value.copy(
                    totalSeconds = elapsed.toInt()
                )
            }
        }
    }

    /**
     * Stop workout timer and return final time
     */
    fun stopWorkoutTimer(): WorkoutTimerData {
        val finalData = _workoutTimer.value
        timerJob?.cancel()

        _workoutTimer.value = WorkoutTimerData()

        return finalData
    }

    /**
     * Reset workout timer
     */
    fun resetWorkoutTimer() {
        timerJob?.cancel()
        _workoutTimer.value = WorkoutTimerData()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
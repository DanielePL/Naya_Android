// app/src/main/java/com/example/myapplicationtest/viewmodels/CalendarViewModel.kt

package com.example.menotracker.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import java.time.LocalDate

// Data classes
data class WorkoutDay(
    val date: LocalDate,
    val workouts: List<ScheduledWorkout>,
    val isCompleted: Boolean = false,
    val isPlanned: Boolean = false
)

data class ScheduledWorkout(
    val id: String,
    val name: String,
    val time: String?,
    val duration: String,
    val isCompleted: Boolean
)

// Alias for compatibility
typealias Workout = ScheduledWorkout

class CalendarViewModel : ViewModel() {

    // State for workout days
    private val _workoutDays = mutableStateListOf<WorkoutDay>()
    val workoutDays: List<WorkoutDay> = _workoutDays

    init {
        // Initialize with mock data
        loadMockData()
    }

    private fun loadMockData() {
        val today = LocalDate.now()
        _workoutDays.addAll(
            listOf(
                WorkoutDay(
                    date = today.minusDays(6),
                    workouts = listOf(ScheduledWorkout("1", "Upper Body", "6:00 AM", "75 min", true)),
                    isCompleted = true
                ),
                WorkoutDay(
                    date = today.minusDays(4),
                    workouts = listOf(ScheduledWorkout("2", "Lower Body", "6:00 AM", "60 min", true)),
                    isCompleted = true
                ),
                WorkoutDay(
                    date = today.minusDays(2),
                    workouts = listOf(ScheduledWorkout("3", "Full Body", null, "90 min", true)),
                    isCompleted = true
                ),
                WorkoutDay(
                    date = today,
                    workouts = listOf(
                        ScheduledWorkout("4", "Upper Body Power", "6:00 PM", "75 min", false),
                        ScheduledWorkout("5", "Cardio", "7:30 PM", "30 min", false)
                    ),
                    isPlanned = true
                ),
                WorkoutDay(
                    date = today.plusDays(2),
                    workouts = listOf(ScheduledWorkout("6", "Leg Day", null, "60 min", false)),
                    isPlanned = true
                )
            )
        )
    }

    fun addWorkoutToDate(date: LocalDate, workoutName: String, duration: String = "60 min") {
        val existingDay = _workoutDays.find { it.date == date }

        if (existingDay != null) {
            // Update existing day
            val newWorkout = ScheduledWorkout(
                id = System.currentTimeMillis().toString(),
                name = workoutName,
                time = null,
                duration = duration,
                isCompleted = false
            )

            val updatedWorkouts = existingDay.workouts + newWorkout
            val updatedDay = existingDay.copy(
                workouts = updatedWorkouts,
                isPlanned = true
            )

            _workoutDays.removeAll { it.date == date }
            _workoutDays.add(updatedDay)
        } else {
            // Create new day
            val newWorkout = ScheduledWorkout(
                id = System.currentTimeMillis().toString(),
                name = workoutName,
                time = null,
                duration = duration,
                isCompleted = false
            )

            _workoutDays.add(
                WorkoutDay(
                    date = date,
                    workouts = listOf(newWorkout),
                    isPlanned = true
                )
            )
        }

        // Sort by date
        _workoutDays.sortBy { it.date }
    }

    fun removeWorkoutFromDate(date: LocalDate, workoutId: String) {
        val existingDay = _workoutDays.find { it.date == date } ?: return

        val updatedWorkouts = existingDay.workouts.filter { it.id != workoutId }

        if (updatedWorkouts.isEmpty()) {
            _workoutDays.removeAll { it.date == date }
        } else {
            val updatedDay = existingDay.copy(
                workouts = updatedWorkouts,
                isPlanned = updatedWorkouts.isNotEmpty(),
                isCompleted = updatedWorkouts.all { it.isCompleted }
            )

            _workoutDays.removeAll { it.date == date }
            _workoutDays.add(updatedDay)
        }
    }

    fun toggleWorkoutCompletion(date: LocalDate, workoutId: String) {
        val existingDay = _workoutDays.find { it.date == date } ?: return

        val updatedWorkouts = existingDay.workouts.map { workout ->
            if (workout.id == workoutId) {
                workout.copy(isCompleted = !workout.isCompleted)
            } else {
                workout
            }
        }

        val updatedDay = existingDay.copy(
            workouts = updatedWorkouts,
            isCompleted = updatedWorkouts.all { it.isCompleted }
        )

        _workoutDays.removeAll { it.date == date }
        _workoutDays.add(updatedDay)
    }
}
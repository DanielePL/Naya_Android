package com.example.menotracker.data.models

import kotlinx.serialization.Serializable

// Represents a single exercise within a user-created workout
@Serializable
data class WorkoutExercise(
    val exercise: Exercise, // The base exercise from the library
    var sets: Int = 3,
    var reps: Int = 10,
    var weight: Float = 0f,
    var restTimeInSeconds: Int = 60
)

// Represents a user-created workout plan
@Serializable
data class Workout(
    val id: String,
    val name: String,
    val exercises: List<WorkoutExercise>
)
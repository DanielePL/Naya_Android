package com.example.menotracker.data

import androidx.compose.runtime.mutableStateListOf
import com.example.menotracker.data.models.Workout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.from

object WorkoutRepository {
    private val _createdWorkouts = mutableStateListOf<Workout>()
    val createdWorkouts: List<Workout> = _createdWorkouts

    private var _isInitialized = false

    suspend fun initialize() {
        if (_isInitialized) return

        try {
            withContext(Dispatchers.IO) {
                println("--- FETCHING WORKOUTS FROM SUPABASE ---")
                val workouts = SupabaseClient.client
                    .from("workouts")
                    .select()
                    .decodeList<Workout>()

                withContext(Dispatchers.Main) {
                    _createdWorkouts.clear()
                    _createdWorkouts.addAll(workouts)
                    _isInitialized = true
                    println("✅ Loaded ${workouts.size} workouts from Supabase")
                }
            }
        } catch (e: Exception) {
            println("❌ ERROR loading workouts: ${e.message}")
            e.printStackTrace()
        }
    }

    fun addWorkout(workout: Workout) {
        if (_createdWorkouts.none { it.id == workout.id }) {
            _createdWorkouts.add(workout)
        }
    }

    suspend fun deleteWorkout(workoutId: String) {
        try {
            withContext(Dispatchers.IO) {
                println("--- DELETING WORKOUT $workoutId FROM SUPABASE ---")
                SupabaseClient.client
                    .from("workouts")
                    .delete {
                        filter {
                            eq("id", workoutId)
                        }
                    }
            }
            withContext(Dispatchers.Main) {
                _createdWorkouts.removeAll { it.id == workoutId }
                println("✅ Successfully removed workout $workoutId")
            }
        } catch(e: Exception) {
            println("❌ ERROR deleting workout $workoutId: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun refresh() {
        _isInitialized = false
        initialize()
    }
}
package com.example.menotracker.data

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.example.menotracker.data.models.Exercise
import com.example.menotracker.data.models.DbExerciseRecommendation
import com.example.menotracker.screens.library.LibrarySearchEngine
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExerciseRepository {
    private const val TAG = "ExerciseRepository"

    // ═══════════════════════════════════════════════════════════════
    // ENV FLAG: Quick rollback between old and new schema
    // ═══════════════════════════════════════════════════════════════
    private const val USE_NEW_SCHEMA = false  // Set to true for exercises_new table
    private const val USE_EXERCISE_VIEW = false  // exercise_view doesn't exist yet

    // Table name - automatically switches based on flags
    private val EXERCISES_TABLE = when {
        USE_EXERCISE_VIEW -> "exercise_view"  // View with technique_sections joined (not yet created)
        USE_NEW_SCHEMA -> "exercises_new"
        else -> "exercises"  // Default: use main exercises table
    }
    private const val RECOMMENDATIONS_TABLE = "exercise_recommendations"

    private val _createdExercises = mutableStateListOf<Exercise>()
    val createdExercises: List<Exercise> = _createdExercises

    private var _isInitialized = false

    suspend fun initialize() {
        if (_isInitialized) return

        try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "--- FETCHING ALL EXERCISES FROM SUPABASE ($EXERCISES_TABLE) ---")

                val allExercises = SupabaseClient.client
                    .from(EXERCISES_TABLE)
                    .select()
                    .decodeList<Exercise>()

                // Sort exercises by importance (compounds first, then accessories, then isolation)
                val sortedExercises = LibrarySearchEngine.sortByImportance(allExercises)

                withContext(Dispatchers.Main) {
                    _createdExercises.clear()
                    _createdExercises.addAll(sortedExercises)
                    _isInitialized = true

                    // Debug: Count exercises with technique guides
                    val withTechniques = allExercises.count { !it.techniqueSections.isNullOrEmpty() }
                    Log.d(TAG, "✅ Loaded ${allExercises.size} exercises from Supabase ($withTechniques have technique guides)")
                    Log.d(TAG, "📊 Sorted by importance - Top 10 exercises:")
                    sortedExercises.take(10).forEachIndexed { index, ex ->
                        Log.d(TAG, "  ${index + 1}. ${ex.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR loading exercises: ${e.message}", e)
            e.printStackTrace()
        }
    }

    fun addExercise(exercise: Exercise) {
        if (_createdExercises.none { it.id == exercise.id }) {
            _createdExercises.add(exercise)
        }
    }

    /**
     * Add an AI-created exercise to the local cache.
     * This is called after the backend creates a new exercise via AI.
     * The exercise is already saved in Supabase, this just updates the local cache.
     */
    fun addExerciseToCache(exercise: Exercise) {
        Log.d(TAG, "📥 Adding AI-created exercise to cache: ${exercise.name}")
        if (_createdExercises.none { it.id == exercise.id }) {
            _createdExercises.add(0, exercise)  // Add to beginning so it's easy to find
            Log.d(TAG, "✅ Added ${exercise.name} to cache. Total: ${_createdExercises.size}")
        } else {
            Log.d(TAG, "ℹ️ Exercise ${exercise.name} already in cache")
        }
    }

    suspend fun getExerciseById(exerciseId: String): Result<Exercise> {
        return try {
            // First check local cache
            val localExercise = _createdExercises.firstOrNull { it.id == exerciseId }
            if (localExercise != null) {
                return Result.success(localExercise)
            }

            // If not in cache, fetch from Supabase
            withContext(Dispatchers.IO) {
                Log.d(TAG, "🔍 Fetching exercise $exerciseId from Supabase")

                val exercise = SupabaseClient.client
                    .from(EXERCISES_TABLE)
                    .select {
                        filter {
                            eq("id", exerciseId)
                        }
                    }
                    .decodeSingle<Exercise>()

                // Add to local cache
                withContext(Dispatchers.Main) {
                    addExercise(exercise)
                }

                Result.success(exercise)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR loading exercise $exerciseId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun saveExercise(exercise: Exercise): Result<String> {
        return try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "💾 Saving exercise to Supabase: ${exercise.name} (owner_id: ${exercise.ownerId})")

                // Always write to exercises_new table (not the view!)
                SupabaseClient.client
                    .from("exercises_new")
                    .insert(exercise) {
                        select()
                    }

                Log.d(TAG, "✅ Exercise saved successfully to exercises_new")
            }

            // Add to local list
            withContext(Dispatchers.Main) {
                addExercise(exercise)
            }

            Result.success(exercise.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR saving exercise: ${e.message}", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun deleteExercise(exerciseId: String) {
        try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "🗑️ Deleting exercise $exerciseId from Supabase")
                // Always delete from exercises_new table (not the view!)
                SupabaseClient.client
                    .from("exercises_new")
                    .delete {
                        filter {
                            eq("id", exerciseId)
                        }
                    }
            }
            withContext(Dispatchers.Main) {
                _createdExercises.removeAll { it.id == exerciseId }
                Log.d(TAG, "✅ Successfully removed exercise $exerciseId")
            }
        } catch(e: Exception) {
            Log.e(TAG, "❌ ERROR deleting exercise $exerciseId: ${e.message}", e)
            e.printStackTrace()
        }
    }

    /**
     * Get exercise recommendations for a specific exercise, sport, and level
     */
    suspend fun getRecommendations(
        exerciseId: String,
        sport: com.example.menotracker.data.models.SportType? = null,
        level: com.example.menotracker.data.models.ExperienceLevel? = null
    ): Result<List<DbExerciseRecommendation>> {
        return try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "📊 Fetching recommendations for exercise: $exerciseId")

                val recommendations = SupabaseClient.client
                    .from(RECOMMENDATIONS_TABLE)
                    .select {
                        filter {
                            eq("exercise_id", exerciseId)
                            sport?.let { eq("sport", it.value) }
                            level?.let { eq("level", it.value) }
                        }
                    }
                    .decodeList<DbExerciseRecommendation>()

                Log.d(TAG, "✅ Loaded ${recommendations.size} recommendations")
                Result.success(recommendations)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR loading recommendations: ${e.message}", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun refresh() {
        _isInitialized = false
        initialize()
    }

    /**
     * Check if an exercise is VBT-capable based on database settings.
     * VBT capability is controlled by superadmin only via supports_power_score or vbt_enabled flags.
     * This ensures only validated barbell exercises can use velocity-based tracking.
     */
    fun isVbtCapable(exerciseName: String): Boolean {
        return createdExercises.any {
            it.name.equals(exerciseName, ignoreCase = true) && it.supportsVBT
        }
    }

    /**
     * Get the exercise ID for a given exercise name if it exists and is VBT-capable.
     * Returns null if not found or not VBT-capable.
     */
    fun getVbtExerciseId(exerciseName: String): String? {
        return createdExercises.firstOrNull {
            it.name.equals(exerciseName, ignoreCase = true) && it.supportsVBT
        }?.id
    }
}
// app/src/main/java/com/example/myapplicationtest/data/WorkoutTemplateRepository.kt

package com.example.menotracker.data

import android.content.Context
import android.util.Log
import com.example.menotracker.data.models.*
import com.example.menotracker.viewmodels.WorkoutTemplate
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.menotracker.data.ExerciseWithSets
import com.example.menotracker.data.ExerciseSet as LocalExerciseSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object WorkoutTemplateRepository {
    private const val TAG = "WorkoutTemplateRepo"
    private const val PREFS_NAME = "workout_template_cache"
    private const val KEY_PUBLIC_TEMPLATES = "public_templates_json"
    private const val KEY_CACHE_TIMESTAMP = "cache_timestamp"
    private const val KEY_USER_TEMPLATES = "user_templates_json"
    private const val KEY_USER_CACHE_TIMESTAMP = "user_cache_timestamp"
    private const val KEY_USER_ID = "cached_user_id"
    private const val KEY_LAST_USED_TEMPLATE = "last_used_template_json"
    private const val KEY_LAST_USED_TIMESTAMP = "last_used_timestamp"

    // In-memory cache for public templates
    private var cachedPublicTemplates: List<WorkoutTemplate>? = null
    private var publicTemplatesLastLoaded: Long = 0
    private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes for memory cache
    private const val DISK_CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours for disk cache

    // In-memory cache for user templates (their own workouts)
    private var cachedUserTemplates: List<WorkoutTemplate>? = null
    private var userTemplatesLastLoaded: Long = 0
    private var cachedUserId: String? = null

    // App context for SharedPreferences (set once on app start)
    private var appContext: Context? = null

    /**
     * Initialize with app context for persistent caching
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        // Pre-load from disk cache into memory
        loadFromDiskCache()
    }

    /**
     * Load cached templates from SharedPreferences into memory
     */
    private fun loadFromDiskCache() {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load public templates
        try {
            val json = prefs.getString(KEY_PUBLIC_TEMPLATES, null)
            val timestamp = prefs.getLong(KEY_CACHE_TIMESTAMP, 0)

            if (json != null && System.currentTimeMillis() - timestamp <= DISK_CACHE_TTL_MS) {
                val type = object : TypeToken<List<WorkoutTemplate>>() {}.type
                val templates: List<WorkoutTemplate> = Gson().fromJson(json, type)
                // Filter out templates with null exercises (Gson doesn't respect Kotlin non-null types)
                val validTemplates = templates.filter { it.exercises != null }
                cachedPublicTemplates = validTemplates
                publicTemplatesLastLoaded = System.currentTimeMillis()
                Log.d(TAG, "📦 Loaded ${validTemplates.size} public templates from disk cache (instant!)")
            } else {
                Log.d(TAG, "📦 Public templates disk cache expired or empty")
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to load public templates disk cache: ${e.message}")
        }

        // Load user templates (no TTL - always show cached, refresh in background)
        try {
            val userJson = prefs.getString(KEY_USER_TEMPLATES, null)
            val userId = prefs.getString(KEY_USER_ID, null)

            if (userJson != null && userId != null) {
                val type = object : TypeToken<List<WorkoutTemplate>>() {}.type
                val templates: List<WorkoutTemplate> = Gson().fromJson(userJson, type)
                // Filter out templates with null exercises (Gson doesn't respect Kotlin non-null types)
                val validTemplates = templates.filter { it.exercises != null }
                cachedUserTemplates = validTemplates
                cachedUserId = userId
                userTemplatesLastLoaded = System.currentTimeMillis()
                Log.d(TAG, "📦 Loaded ${validTemplates.size} user templates from disk cache (instant!)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to load user templates disk cache: ${e.message}")
        }
    }

    /**
     * Save public templates to SharedPreferences for persistence across app restarts
     */
    private fun saveToDiskCache(templates: List<WorkoutTemplate>) {
        val context = appContext ?: return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = Gson().toJson(templates)
            prefs.edit()
                .putString(KEY_PUBLIC_TEMPLATES, json)
                .putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis())
                .apply()
            Log.d(TAG, "💾 Saved ${templates.size} public templates to disk cache")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to save public templates disk cache: ${e.message}")
        }
    }

    /**
     * Save user templates to SharedPreferences for persistence across app restarts
     */
    private fun saveUserTemplatesToDiskCache(templates: List<WorkoutTemplate>, userId: String) {
        val context = appContext ?: return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = Gson().toJson(templates)
            prefs.edit()
                .putString(KEY_USER_TEMPLATES, json)
                .putString(KEY_USER_ID, userId)
                .putLong(KEY_USER_CACHE_TIMESTAMP, System.currentTimeMillis())
                .apply()
            Log.d(TAG, "💾 Saved ${templates.size} user templates to disk cache")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to save user templates disk cache: ${e.message}")
        }
    }

    /**
     * Get cached user templates immediately (for instant UI display)
     * Returns null if no cache exists for this user
     */
    fun getCachedUserTemplates(userId: String): List<WorkoutTemplate>? {
        if (cachedUserId == userId && cachedUserTemplates != null) {
            Log.d(TAG, "📦 Returning ${cachedUserTemplates!!.size} cached user templates (instant!)")
            return cachedUserTemplates
        }
        return null
    }

    /**
     * Invalidate user templates cache (call after save/delete)
     */
    fun invalidateUserTemplatesCache() {
        cachedUserTemplates = null
        userTemplatesLastLoaded = 0
        Log.d(TAG, "🗑️ User templates cache invalidated")
    }

    /**
     * Save a workout template to Supabase with all exercises and sets
     */
    suspend fun saveWorkoutTemplate(
        workoutTemplate: WorkoutTemplate,
        userId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "💾 Saving workout template: ${workoutTemplate.name}")

            // 1. Create the workout template
            val templateRequest = CreateWorkoutTemplateRequest(
                name = workoutTemplate.name,
                userId = userId,
                intensity = workoutTemplate.intensity
            )

            val createdTemplate = SupabaseClient.client
                .from("workout_templates")
                .insert(templateRequest) {
                    select()
                }
                .decodeSingle<WorkoutTemplateDto>()

            Log.d(TAG, "✅ Created template with ID: ${createdTemplate.id}")

            // 2. Create exercises for this template
            workoutTemplate.exercises.forEachIndexed { index, exerciseWithSets ->
                Log.d(TAG, "🔗 Inserting exercise: ${exerciseWithSets.exerciseName} (ID: ${exerciseWithSets.exerciseId})")

                val exerciseRequest = CreateWorkoutExerciseRequest(
                    workoutTemplateId = createdTemplate.id,
                    exerciseId = exerciseWithSets.exerciseId,
                    orderIndex = index
                )

                val createdExercise = SupabaseClient.client
                    .from("workout_template_exercises")
                    .insert(exerciseRequest) {
                        select()
                    }
                    .decodeSingle<WorkoutTemplateExerciseDto>()

                Log.d(TAG, "✅ Created exercise: ${exerciseWithSets.exerciseName}")

                // 3. Create sets for this exercise
                exerciseWithSets.sets.forEach { set ->
                    val setRequest = CreateExerciseSetRequest(
                        workoutExerciseId = createdExercise.id,
                        setNumber = set.setNumber,
                        targetReps = set.targetReps,
                        targetWeight = set.targetWeight.toDouble(),
                        restSeconds = set.restSeconds
                    )

                    SupabaseClient.client
                        .from("exercise_sets")
                        .insert(setRequest)

                    Log.d(TAG, "  ✅ Created set ${set.setNumber}: ${set.targetReps} reps @ ${set.targetWeight}kg")
                }
            }

            Log.d(TAG, "🎉 Successfully saved workout template: ${workoutTemplate.name}")
            Result.success(createdTemplate.id)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving workout template: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Load all workout templates for a user
     * Uses cache-first strategy: returns cached immediately, updates in background
     */
    suspend fun loadWorkoutTemplates(userId: String): Result<List<WorkoutTemplate>> =
        withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                Log.d(TAG, "📥 Loading workout templates for user: $userId")

                // 1. Load all templates for this user
                val templates = SupabaseClient.client
                    .from("workout_templates")
                    .select() {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<WorkoutTemplateDto>()

                Log.d(TAG, "Found ${templates.size} templates")

                // 2. Load exercises and sets for each template
                val workoutTemplates = templates.map { template ->
                    loadCompleteWorkoutTemplate(template.id)
                }.mapNotNull { it.getOrNull() }

                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ Loaded ${workoutTemplates.size} user templates in ${elapsed}ms")

                // Update memory cache
                cachedUserTemplates = workoutTemplates
                cachedUserId = userId
                userTemplatesLastLoaded = System.currentTimeMillis()

                // Save to disk cache for next app start
                saveUserTemplatesToDiskCache(workoutTemplates, userId)

                Result.success(workoutTemplates)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading workout templates: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Load a single workout template by ID (public method for direct access)
     */
    suspend fun loadWorkoutTemplateById(templateId: String): Result<WorkoutTemplate> =
        loadCompleteWorkoutTemplate(templateId)

    /**
     * Load a single complete workout template with all exercises and sets
     * Uses live data from exercises_new table via exercise_id reference
     */
    private suspend fun loadCompleteWorkoutTemplate(templateId: String): Result<WorkoutTemplate> =
        withContext(Dispatchers.IO) {
            try {
                // Load template metadata
                val template = SupabaseClient.client
                    .from("workout_templates")
                    .select() {
                        filter {
                            eq("id", templateId)
                        }
                    }
                    .decodeSingle<WorkoutTemplateDto>()

                // Load exercises for this template
                val exercises = SupabaseClient.client
                    .from("workout_template_exercises")
                    .select() {
                        filter {
                            eq("workout_template_id", templateId)
                        }
                    }
                    .decodeList<WorkoutTemplateExerciseDto>()
                    .sortedBy { it.orderIndex }

                // Load exercise details for each exercise
                // Try exercises_new first, then fallback to exercise_view (which includes old exercises)
                val exercisesWithSets = exercises.mapNotNull { exercise ->
                    // Try to fetch exercise data - first from exercises_new, then fallback
                    val exerciseDetails = try {
                        SupabaseClient.client
                            .from("exercises_new")
                            .select() {
                                filter {
                                    eq("id", exercise.exerciseId)
                                }
                            }
                            .decodeList<Exercise>()
                            .firstOrNull()
                    } catch (e: Exception) {
                        null
                    } ?: try {
                        // Fallback to exercise_view for legacy exercises
                        Log.d(TAG, "⚠️ Exercise ${exercise.exerciseId} not in exercises_new, trying exercise_view")
                        SupabaseClient.client
                            .from("exercise_view")
                            .select() {
                                filter {
                                    eq("id", exercise.exerciseId)
                                }
                            }
                            .decodeList<Exercise>()
                            .firstOrNull()
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Could not find exercise ${exercise.exerciseId} in any table")
                        null
                    }

                    if (exerciseDetails == null) {
                        Log.w(TAG, "⚠️ Skipping exercise ${exercise.exerciseId} - not found")
                        return@mapNotNull null
                    }

                    // Load sets for this exercise
                    val sets = SupabaseClient.client
                        .from("exercise_sets")
                        .select() {
                            filter {
                                eq("workout_exercise_id", exercise.id)
                            }
                        }
                        .decodeList<ExerciseSetDto>()
                        .sortedBy { it.setNumber }

                    ExerciseWithSets(
                        exerciseId = exercise.exerciseId,
                        exerciseName = exerciseDetails.name,
                        muscleGroup = exerciseDetails.muscleCategory ?: "Unknown",
                        equipment = exerciseDetails.equipment?.joinToString(", ") ?: "None",
                        order = exercise.orderIndex,
                        sets = sets.map { set ->
                            LocalExerciseSet(
                                id = set.id,
                                setNumber = set.setNumber,
                                targetReps = set.targetReps,
                                targetWeight = set.targetWeight,
                                restSeconds = set.restSeconds
                            )
                        }
                    )
                }

                val workoutTemplate = WorkoutTemplate(
                    id = template.id,
                    name = template.name,
                    exercises = exercisesWithSets,
                    createdAt = parseTimestamp(template.createdAt),
                    sports = template.sports,
                    intensity = template.intensity ?: "AKTIV"
                )

                Result.success(workoutTemplate)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading complete workout template: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Delete a workout template and all its exercises/sets (cascading delete)
     */
    suspend fun deleteWorkoutTemplate(templateId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🗑️ Deleting workout template: $templateId")

                SupabaseClient.client
                    .from("workout_templates")
                    .delete {
                        filter {
                            eq("id", templateId)
                        }
                    }

                Log.d(TAG, "✅ Successfully deleted workout template")
                Result.success(Unit)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error deleting workout template: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Update a workout template
     */
    suspend fun updateWorkoutTemplate(
        workoutTemplate: WorkoutTemplate,
        userId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Updating workout template: ${workoutTemplate.name}")

            // Delete existing template and create new one
            // (Simpler than complex update logic)
            deleteWorkoutTemplate(workoutTemplate.id).getOrThrow()
            saveWorkoutTemplate(workoutTemplate, userId).getOrThrow()

            Log.d(TAG, "✅ Successfully updated workout template")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating workout template: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Load public workout templates (user_id IS NULL)
     * These are the 16 example templates created via SQL
     *
     * OPTIMIZED: Uses bulk loading (3 queries instead of N*3 queries)
     * Also implements caching with TTL
     */
    suspend fun loadPublicWorkoutTemplates(forceRefresh: Boolean = false): Result<List<WorkoutTemplate>> =
        withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val now = System.currentTimeMillis()
                if (!forceRefresh && cachedPublicTemplates != null &&
                    (now - publicTemplatesLastLoaded) < CACHE_TTL_MS) {
                    Log.d(TAG, "📦 Returning ${cachedPublicTemplates!!.size} cached public templates")
                    return@withContext Result.success(cachedPublicTemplates!!)
                }

                val startTime = System.currentTimeMillis()
                Log.d(TAG, "📥 Loading public workout templates (optimized bulk load)")

                // STEP 1: Load all public templates in ONE query
                val allTemplates = SupabaseClient.client
                    .from("workout_templates")
                    .select()
                    .decodeList<WorkoutTemplateDto>()
                val templates = allTemplates.filter { it.userId == null }
                val templateIds = templates.map { it.id }

                if (templates.isEmpty()) {
                    Log.d(TAG, "No public templates found")
                    cachedPublicTemplates = emptyList()
                    publicTemplatesLastLoaded = now
                    return@withContext Result.success(emptyList())
                }
                Log.d(TAG, "Step 1: Found ${templates.size} public templates")

                // STEP 2: Load ALL exercises for ALL templates in ONE query
                val allExercises = SupabaseClient.client
                    .from("workout_template_exercises")
                    .select()
                    .decodeList<WorkoutTemplateExerciseDto>()
                    .filter { it.workoutTemplateId in templateIds }
                val exercisesByTemplate = allExercises.groupBy { it.workoutTemplateId }
                val exerciseIds = allExercises.map { it.id }
                val uniqueExerciseRefIds = allExercises.map { it.exerciseId }.distinct()
                Log.d(TAG, "Step 2: Loaded ${allExercises.size} exercises")

                // STEP 3: Load ALL sets for ALL exercises in ONE query
                val allSets = SupabaseClient.client
                    .from("exercise_sets")
                    .select()
                    .decodeList<ExerciseSetDto>()
                    .filter { it.workoutExerciseId in exerciseIds }
                val setsByExercise = allSets.groupBy { it.workoutExerciseId }
                Log.d(TAG, "Step 3: Loaded ${allSets.size} sets")

                // STEP 4: Load exercise details (use local cache from ExerciseRepository if available)
                val exerciseDetailsMap = mutableMapOf<String, Exercise>()

                // First try local cache
                uniqueExerciseRefIds.forEach { exId ->
                    ExerciseRepository.createdExercises.find { it.id == exId }?.let {
                        exerciseDetailsMap[exId] = it
                    }
                }

                // Fetch missing from database
                val missingIds = uniqueExerciseRefIds.filter { it !in exerciseDetailsMap }
                if (missingIds.isNotEmpty()) {
                    Log.d(TAG, "Step 4: Fetching ${missingIds.size} exercise details from DB")
                    val fetchedExercises = SupabaseClient.client
                        .from("exercise_view")
                        .select()
                        .decodeList<Exercise>()
                        .filter { it.id in missingIds }
                    fetchedExercises.forEach { exerciseDetailsMap[it.id] = it }
                } else {
                    Log.d(TAG, "Step 4: All ${uniqueExerciseRefIds.size} exercises found in cache")
                }

                // STEP 5: Assemble templates
                val workoutTemplates = templates.mapNotNull { template ->
                    val templateExercises = exercisesByTemplate[template.id] ?: emptyList()

                    val exercisesWithSets = templateExercises.sortedBy { it.orderIndex }.mapNotNull { exercise ->
                        val details = exerciseDetailsMap[exercise.exerciseId]
                        if (details == null) {
                            Log.w(TAG, "⚠️ Exercise ${exercise.exerciseId} not found, skipping")
                            return@mapNotNull null
                        }

                        val sets = setsByExercise[exercise.id] ?: emptyList()

                        ExerciseWithSets(
                            exerciseId = exercise.exerciseId,
                            exerciseName = details.name,
                            muscleGroup = details.muscleCategory ?: "Unknown",
                            equipment = details.equipment?.joinToString(", ") ?: "None",
                            order = exercise.orderIndex,
                            sets = sets.sortedBy { it.setNumber }.map { set ->
                                LocalExerciseSet(
                                    id = set.id,
                                    setNumber = set.setNumber,
                                    targetReps = set.targetReps,
                                    targetWeight = set.targetWeight,
                                    restSeconds = set.restSeconds
                                )
                            }
                        )
                    }

                    WorkoutTemplate(
                        id = template.id,
                        name = template.name,
                        exercises = exercisesWithSets,
                        createdAt = parseTimestamp(template.createdAt),
                        sports = template.sports,
                        intensity = template.intensity ?: "AKTIV"
                    )
                }

                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ Loaded ${workoutTemplates.size} public templates in ${elapsed}ms (optimized)")

                // Update memory cache
                cachedPublicTemplates = workoutTemplates
                publicTemplatesLastLoaded = now

                // Save to disk cache for persistence across app restarts
                saveToDiskCache(workoutTemplates)

                Result.success(workoutTemplates)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading public templates: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Clear the public templates cache (call after changes)
     */
    fun clearPublicTemplatesCache() {
        cachedPublicTemplates = null
        publicTemplatesLastLoaded = 0
    }

    /**
     * Update sets for a workout template exercise
     * Replaces all existing sets with new ones
     */
    suspend fun updateExerciseSets(
        workoutExerciseId: String,
        newSets: List<LocalExerciseSet>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Updating sets for exercise: $workoutExerciseId")

            // 1. Delete existing sets
            SupabaseClient.client
                .from("exercise_sets")
                .delete {
                    filter {
                        eq("workout_exercise_id", workoutExerciseId)
                    }
                }

            // 2. Insert new sets
            newSets.forEach { set ->
                val setRequest = CreateExerciseSetRequest(
                    workoutExerciseId = workoutExerciseId,
                    setNumber = set.setNumber,
                    targetReps = set.targetReps,
                    targetWeight = set.targetWeight,
                    restSeconds = set.restSeconds
                )

                SupabaseClient.client
                    .from("exercise_sets")
                    .insert(setRequest)

                Log.d(TAG, "  ✅ Created set ${set.setNumber}: ${set.targetReps} reps @ ${set.targetWeight}kg")
            }

            Log.d(TAG, "✅ Successfully updated exercise sets")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating exercise sets: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Parse ISO timestamp to milliseconds
     */
    private fun parseTimestamp(timestamp: String): Long {
        return try {
            // Simple parsing - you might want to use a proper date library
            java.time.Instant.parse(timestamp).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    // ==================== LAST USED WORKOUT PERSISTENCE ====================

    /**
     * Save the last used workout template for persistence across app restarts
     * Called when user starts a workout session
     */
    fun saveLastUsedWorkout(template: WorkoutTemplate) {
        val context = appContext ?: return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = Gson().toJson(template)
            prefs.edit()
                .putString(KEY_LAST_USED_TEMPLATE, json)
                .putLong(KEY_LAST_USED_TIMESTAMP, System.currentTimeMillis())
                .apply()
            Log.d(TAG, "💾 Saved last used workout: ${template.name}")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to save last used workout: ${e.message}")
        }
    }

    /**
     * Get the last used workout template (if any)
     * Returns null if no workout was saved
     * Note: No TTL - workout stays until user explicitly removes it or selects a new one
     */
    fun getLastUsedWorkout(): WorkoutTemplate? {
        val context = appContext ?: return null
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_LAST_USED_TEMPLATE, null) ?: return null

            val template: WorkoutTemplate = Gson().fromJson(json, WorkoutTemplate::class.java)
            Log.d(TAG, "📦 Loaded last used workout: ${template.name}")
            template
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to load last used workout: ${e.message}")
            null
        }
    }

    /**
     * Clear the last used workout (e.g., after workout completion)
     */
    fun clearLastUsedWorkout() {
        val context = appContext ?: return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove(KEY_LAST_USED_TEMPLATE)
                .remove(KEY_LAST_USED_TIMESTAMP)
                .apply()
            Log.d(TAG, "🗑️ Cleared last used workout")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to clear last used workout: ${e.message}")
        }
    }
}
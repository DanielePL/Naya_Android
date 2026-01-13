// app/src/main/java/com/example/myapplicationtest/viewmodels/WorkoutBuilderViewModel.kt

package com.example.menotracker.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.data.ExerciseSet
import com.example.menotracker.data.ExerciseWithSets
import com.example.menotracker.data.WorkoutTemplateRepository
import com.example.menotracker.data.models.Exercise
import com.example.menotracker.billing.SubscriptionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * WOD Timer configuration for CrossFit workouts
 */
data class WodTimerConfig(
    val wodType: String, // amrap, emom, for_time, tabata, etc.
    val timeCapSeconds: Int? = null, // Time cap for AMRAP/For Time
    val targetRounds: Int? = null, // Target rounds for RFT
    val emomIntervalSeconds: Int = 60, // EMOM interval (default 1 min)
    val tabataWorkSeconds: Int = 20, // Tabata work interval
    val tabataRestSeconds: Int = 10, // Tabata rest interval
    val tabataRounds: Int = 8 // Tabata rounds
) {
    fun getDisplayName(): String = when (wodType) {
        "amrap" -> "AMRAP"
        "emom" -> "EMOM"
        "for_time" -> "For Time"
        "rft" -> "Rounds For Time"
        "tabata" -> "Tabata"
        "chipper" -> "Chipper"
        else -> wodType.uppercase()
    }
}

data class WorkoutTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val exercises: List<ExerciseWithSets>,
    val createdAt: Long = System.currentTimeMillis(),
    val sports: List<String>? = null,
    // Intensity level for simplified workout selection (Sanft/Aktiv/Power)
    val intensity: String = "AKTIV",
    // Video URL for workout preview (uploaded by admin)
    val videoUrl: String? = null,
    // WOD-specific fields
    val wodTimerConfig: WodTimerConfig? = null,
    val isWod: Boolean = false,
    // ILB (Individuelles Leistungsbild) - Periodized strength testing
    val isILBTestWeek: Boolean = false  // True when this workout is part of an ILB test week
)

class WorkoutBuilderViewModel : ViewModel() {

    // User ID - set from AccountViewModel when available
    private var _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    // Fallback dummy user ID (only used when not logged in)
    private val FALLBACK_USER_ID = "00000000-0000-0000-0000-000000000000"

    /**
     * Set the current user ID from AccountViewModel
     * This should be called when the user logs in or when the profile loads
     */
    fun setUserId(userId: String?) {
        val oldUserId = _currentUserId.value
        _currentUserId.value = userId
        Log.d("WorkoutBuilder", "👤 User ID set: $userId (was: $oldUserId)")

        // Reload workouts if user changed and is not null
        if (userId != null && userId != oldUserId) {
            loadWorkouts()
        }
    }

    /**
     * Get the effective user ID (real user or fallback)
     */
    private fun getEffectiveUserId(): String {
        return _currentUserId.value ?: FALLBACK_USER_ID
    }

    // Current workout being built
    private val _currentWorkoutName = MutableStateFlow("")
    val currentWorkoutName: StateFlow<String> = _currentWorkoutName.asStateFlow()

    private val _selectedExercises = MutableStateFlow<List<ExerciseWithSets>>(emptyList())
    val selectedExercises: StateFlow<List<ExerciseWithSets>> = _selectedExercises.asStateFlow()

    // Saved workout templates (user's own workouts)
    private val _savedWorkouts = MutableStateFlow<List<WorkoutTemplate>>(emptyList())
    val savedWorkouts: StateFlow<List<WorkoutTemplate>> = _savedWorkouts.asStateFlow()

    // Public workout templates (16 example templates from SQL)
    private val _publicWorkouts = MutableStateFlow<List<WorkoutTemplate>>(emptyList())
    val publicWorkouts: StateFlow<List<WorkoutTemplate>> = _publicWorkouts.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Edit mode
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _editingWorkoutId = MutableStateFlow<String?>(null)

    // Template customization mode (when loading a template to customize before starting)
    private val _isTemplateCustomizationMode = MutableStateFlow(false)
    val isTemplateCustomizationMode: StateFlow<Boolean> = _isTemplateCustomizationMode.asStateFlow()

    // Workout limit reached state (for showing upgrade prompt)
    private val _workoutLimitReached = MutableStateFlow(false)
    val workoutLimitReached: StateFlow<Boolean> = _workoutLimitReached.asStateFlow()

    // ILB (Individuelles Leistungsbild) Test Mode - for AMRAP strength testing
    private val _isILBTestMode = MutableStateFlow(false)
    val isILBTestMode: StateFlow<Boolean> = _isILBTestMode.asStateFlow()

    /**
     * Set ILB test mode for the current workout
     */
    fun setILBTestMode(enabled: Boolean) {
        _isILBTestMode.value = enabled
        Log.d("WorkoutBuilder", "📊 ILB Test Mode: $enabled")
    }

    /**
     * Check if user can create more workouts based on subscription tier
     * Free users are limited to 3 workouts
     */
    fun canCreateWorkout(): Boolean {
        val currentCount = _savedWorkouts.value.size
        return SubscriptionManager.canCreateWorkout(currentCount)
    }

    /**
     * Get remaining workout slots for free users
     */
    fun getRemainingWorkoutSlots(): Int {
        return SubscriptionManager.getRemainingWorkoutSlots(_savedWorkouts.value.size)
    }

    /**
     * Clear the workout limit reached flag
     */
    fun clearWorkoutLimitReached() {
        _workoutLimitReached.value = false
    }

    init {
        // Only load public workouts on init (they don't need user ID)
        // User workouts are loaded when setUserId() is called with a real user ID
        loadPublicWorkouts()
    }

    fun updateWorkoutName(name: String) {
        _currentWorkoutName.value = name
    }

    fun addExercise(exercise: Exercise) {
        Log.d("WorkoutBuilder", "📥 addExercise(Exercise) called: ${exercise.name} (id=${exercise.id})")
        val currentList = _selectedExercises.value.toMutableList()

        // Check if exercise already exists
        if (currentList.none { it.exerciseId == exercise.id }) {
            // Create default sets (3 sets × 0 reps @ 0kg with 90s rest)
            // Reps = 0 means user must enter their own target reps
            val defaultSets = List(3) { index ->
                ExerciseSet(
                    setNumber = index + 1,
                    targetReps = 0,  // Empty - user must set their own reps
                    targetWeight = 0.0,
                    restSeconds = 90
                )
            }

            val exerciseWithSets = ExerciseWithSets(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                muscleGroup = exercise.mainMuscle ?: "Unknown",
                equipment = exercise.equipment?.joinToString(", ") ?: "None",
                order = currentList.size,
                sets = defaultSets
            )
            currentList.add(exerciseWithSets)
            _selectedExercises.value = currentList
            Log.d("WorkoutBuilder", "✅ Exercise added. Total exercises: ${_selectedExercises.value.size}")
        } else {
            Log.d("WorkoutBuilder", "⚠️ Exercise already exists, skipping")
        }
    }

    // Overload for String parameter (from MainActivity)
    // Looks up the exercise by name in ExerciseRepository to get the real DB ID
    fun addExercise(exerciseName: String) {
        Log.d("WorkoutBuilder", "📥 addExercise(String) called: '$exerciseName'")
        Log.d("WorkoutBuilder", "📊 ExerciseRepository has ${com.example.menotracker.data.ExerciseRepository.createdExercises.size} exercises")

        // Find the exercise in the repository by name to get its real ID
        val exercise = com.example.menotracker.data.ExerciseRepository.createdExercises
            .firstOrNull { it.name.equals(exerciseName, ignoreCase = true) }

        if (exercise != null) {
            // Use the proper addExercise function with the full Exercise object
            addExercise(exercise)
            Log.d("WorkoutBuilder", "✅ Added exercise with DB ID: ${exercise.id} (${exercise.name})")
        } else {
            // Fallback: Create with generated UUID (shouldn't happen if exercises are loaded)
            Log.w("WorkoutBuilder", "⚠️ Exercise '$exerciseName' not found in repository, using generated ID")

            val currentList = _selectedExercises.value.toMutableList()

            // Create default sets (3 sets × 0 reps @ 0kg with 90s rest)
            // Reps = 0 means user must enter their own target reps
            val defaultSets = List(3) { index ->
                ExerciseSet(
                    setNumber = index + 1,
                    targetReps = 0,  // Empty - user must set their own reps
                    targetWeight = 0.0,
                    restSeconds = 90
                )
            }

            val exerciseWithSets = ExerciseWithSets(
                exerciseId = UUID.randomUUID().toString(),
                exerciseName = exerciseName,
                muscleGroup = "Unknown",
                equipment = "None",
                order = currentList.size,
                sets = defaultSets
            )
            currentList.add(exerciseWithSets)
            _selectedExercises.value = currentList
            Log.d("WorkoutBuilder", "✅ Fallback exercise added. Total exercises: ${_selectedExercises.value.size}")
        }
    }

    fun updateExerciseSets(exerciseId: String, sets: List<ExerciseSet>) {
        val currentList = _selectedExercises.value.toMutableList()
        val index = currentList.indexOfFirst { it.exerciseId == exerciseId }

        if (index != -1) {
            val updatedExercise = currentList[index].copy(sets = sets)
            currentList[index] = updatedExercise
            _selectedExercises.value = currentList
        }
    }

    fun addSetToExercise(exerciseId: String) {
        val currentList = _selectedExercises.value.toMutableList()
        val index = currentList.indexOfFirst { it.exerciseId == exerciseId }

        if (index != -1) {
            val exercise = currentList[index]
            val lastSet = exercise.sets.lastOrNull()

            val newSet = ExerciseSet(
                setNumber = exercise.sets.size + 1,
                targetReps = lastSet?.targetReps ?: 0,  // Empty - user must set reps
                targetWeight = lastSet?.targetWeight ?: 0.0,
                restSeconds = lastSet?.restSeconds ?: 90
            )

            val updatedSets = exercise.sets + newSet
            currentList[index] = exercise.copy(sets = updatedSets)
            _selectedExercises.value = currentList
        }
    }

    fun removeSetFromExercise(exerciseId: String, setId: String) {
        val currentList = _selectedExercises.value.toMutableList()
        val index = currentList.indexOfFirst { it.exerciseId == exerciseId }

        if (index != -1) {
            val exercise = currentList[index]
            val updatedSets = exercise.sets.filter { it.id != setId }
                .mapIndexed { idx, set -> set.copy(setNumber = idx + 1) }

            currentList[index] = exercise.copy(sets = updatedSets)
            _selectedExercises.value = currentList
        }
    }

    fun updateSet(exerciseId: String, setId: String, updatedSet: ExerciseSet) {
        val currentList = _selectedExercises.value.toMutableList()
        val exerciseIndex = currentList.indexOfFirst { it.exerciseId == exerciseId }

        if (exerciseIndex != -1) {
            val exercise = currentList[exerciseIndex]
            val setIndex = exercise.sets.indexOfFirst { it.id == setId }

            if (setIndex != -1) {
                val updatedSets = exercise.sets.toMutableList()
                updatedSets[setIndex] = updatedSet
                currentList[exerciseIndex] = exercise.copy(sets = updatedSets)
                _selectedExercises.value = currentList
            }
        }
    }

    fun removeExercise(exerciseId: String) {
        val currentList = _selectedExercises.value.toMutableList()
        currentList.removeAll { it.exerciseId == exerciseId }

        // Reorder remaining exercises
        _selectedExercises.value = currentList.mapIndexed { index, exercise ->
            exercise.copy(order = index)
        }
    }

    fun moveExercise(fromIndex: Int, toIndex: Int) {
        val currentList = _selectedExercises.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)

            // Update order
            _selectedExercises.value = currentList.mapIndexed { index, exercise ->
                exercise.copy(order = index)
            }
        }
    }

    /**
     * Save workout to Supabase (async)
     * Returns true immediately, actual save happens in background
     * Returns false if workout limit reached for Free tier
     */
    fun saveWorkout(): Boolean {
        Log.d("WorkoutBuilder", "saveWorkout() called")
        Log.d("WorkoutBuilder", "Workout name: '${_currentWorkoutName.value}'")
        Log.d("WorkoutBuilder", "Selected exercises: ${_selectedExercises.value.size}")

        if (_currentWorkoutName.value.isBlank()) {
            Log.e("WorkoutBuilder", "Save failed: Workout name is blank")
            return false
        }

        if (_selectedExercises.value.isEmpty()) {
            Log.e("WorkoutBuilder", "Save failed: No exercises selected")
            return false
        }

        // Validate that all exercises have at least one set
        val exercisesWithoutSets = _selectedExercises.value.filter { it.sets.isEmpty() }
        if (exercisesWithoutSets.isNotEmpty()) {
            Log.e("WorkoutBuilder", "Save failed: ${exercisesWithoutSets.size} exercises have no sets")
            return false
        }

        // Check workout limit for new workouts (editing existing is always allowed)
        if (!_isEditMode.value && !canCreateWorkout()) {
            Log.e("WorkoutBuilder", "Save failed: Free tier workout limit reached (${SubscriptionManager.FREE_WORKOUT_LIMIT})")
            _workoutLimitReached.value = true
            return false
        }

        val workout = WorkoutTemplate(
            id = _editingWorkoutId.value ?: UUID.randomUUID().toString(),
            name = _currentWorkoutName.value,
            exercises = _selectedExercises.value,
            isILBTestWeek = _isILBTestMode.value
        )

        // Save to Supabase in background
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (_isEditMode.value) {
                    // Update existing workout
                    WorkoutTemplateRepository.updateWorkoutTemplate(workout, getEffectiveUserId())
                        .onSuccess {
                            Log.d("WorkoutBuilder", "✅ Updated workout in Supabase: ${workout.name}")
                        }
                        .onFailure { e ->
                            Log.e("WorkoutBuilder", "❌ Failed to update workout: ${e.message}")
                        }
                } else {
                    // Create new workout
                    WorkoutTemplateRepository.saveWorkoutTemplate(workout, getEffectiveUserId())
                        .onSuccess { savedId ->
                            Log.d("WorkoutBuilder", "✅ Saved workout to Supabase with ID: $savedId")
                        }
                        .onFailure { e ->
                            Log.e("WorkoutBuilder", "❌ Failed to save workout: ${e.message}")
                        }
                }

                // Reload workouts from Supabase
                loadWorkouts()
            } finally {
                _isLoading.value = false
            }
        }

        clearCurrentWorkout()
        return true
    }

    /**
     * Load all workouts from Supabase (user's own workouts)
     * Uses cache-first strategy:
     * 1. Show cached workouts immediately (instant!)
     * 2. Refresh from server in background
     * 3. Update UI when fresh data arrives
     */
    fun loadWorkouts() {
        viewModelScope.launch {
            val userId = getEffectiveUserId()

            // STEP 1: Show cached workouts IMMEDIATELY (no network delay)
            val cachedWorkouts = WorkoutTemplateRepository.getCachedUserTemplates(userId)
            if (cachedWorkouts != null) {
                _savedWorkouts.value = cachedWorkouts
                Log.d("WorkoutBuilder", "📦 Showing ${cachedWorkouts.size} cached workouts (instant!)")
            }

            // STEP 2: Refresh from server in background
            try {
                WorkoutTemplateRepository.loadWorkoutTemplates(userId)
                    .onSuccess { workouts ->
                        _savedWorkouts.value = workouts
                        Log.d("WorkoutBuilder", "✅ Refreshed ${workouts.size} user workouts from server")
                    }
                    .onFailure { e ->
                        Log.e("WorkoutBuilder", "❌ Failed to refresh workouts: ${e.message}")
                        // Keep showing cached data if refresh fails
                    }
            } catch (e: Exception) {
                Log.e("WorkoutBuilder", "❌ Exception refreshing workouts: ${e.message}")
                // Keep showing cached data if refresh fails
            }
        }
    }

    /**
     * Load public workout templates (the 16 example templates)
     */
    fun loadPublicWorkouts() {
        viewModelScope.launch {
            try {
                WorkoutTemplateRepository.loadPublicWorkoutTemplates()
                    .onSuccess { templates ->
                        _publicWorkouts.value = templates
                        Log.d("WorkoutBuilder", "✅ Loaded ${templates.size} public templates from Supabase")
                    }
                    .onFailure { e ->
                        Log.e("WorkoutBuilder", "❌ Failed to load public templates: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e("WorkoutBuilder", "❌ Exception loading public templates: ${e.message}")
            }
        }
    }

    /**
     * Clone a public template to user's own workouts
     * This allows users to customize templates
     */
    fun cloneTemplateToMyWorkouts(template: WorkoutTemplate) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("WorkoutBuilder", "📋 Cloning template: ${template.name}")

                // Save template as new workout with user_id
                WorkoutTemplateRepository.saveWorkoutTemplate(template, getEffectiveUserId())
                    .onSuccess { newWorkoutId ->
                        Log.d("WorkoutBuilder", "✅ Template cloned successfully with ID: $newWorkoutId")

                        // Reload workouts to include the new one
                        loadWorkouts()

                        // Set up for editing the cloned workout
                        _isEditMode.value = true
                        _editingWorkoutId.value = newWorkoutId
                        _currentWorkoutName.value = template.name
                        _selectedExercises.value = template.exercises
                    }
                    .onFailure { e ->
                        Log.e("WorkoutBuilder", "❌ Failed to clone template: ${e.message}")
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadWorkoutForEditing(workoutId: String) {
        val workout = _savedWorkouts.value.find { it.id == workoutId }
        if (workout != null) {
            _isEditMode.value = true
            _editingWorkoutId.value = workoutId
            _currentWorkoutName.value = workout.name
            _selectedExercises.value = workout.exercises
        }
    }

    /**
     * Load a template for customization (from Quick Start Box)
     * This loads the template into the builder for editing before starting the session
     */
    fun loadTemplateForCustomization(templateId: String) {
        Log.d("WorkoutBuilder", "📋 Loading template for customization: $templateId")

        // Try public workouts first, then saved workouts
        val template = _publicWorkouts.value.find { it.id == templateId }
            ?: _savedWorkouts.value.find { it.id == templateId }

        if (template != null) {
            Log.d("WorkoutBuilder", "✅ Found template: ${template.name}")
            _isTemplateCustomizationMode.value = true
            _isEditMode.value = false
            _editingWorkoutId.value = null
            _currentWorkoutName.value = template.name
            _selectedExercises.value = template.exercises
        } else {
            Log.e("WorkoutBuilder", "❌ Template not found: $templateId")
        }
    }

    /**
     * Load a template for direct session start (from AI Coach recommendation)
     * This loads the template and invokes a callback with the loaded workout
     */
    fun loadTemplateForSession(templateId: String, onLoaded: (WorkoutTemplate?) -> Unit) {
        Log.d("WorkoutBuilder", "🏋️ Loading template for session: $templateId")

        viewModelScope.launch {
            // First check in memory
            var template = _publicWorkouts.value.find { it.id == templateId }
                ?: _savedWorkouts.value.find { it.id == templateId }

            // If not found in memory, try to load from database
            if (template == null) {
                Log.d("WorkoutBuilder", "📥 Template not in memory, loading from database...")
                val result = WorkoutTemplateRepository.loadWorkoutTemplateById(templateId)
                result.onSuccess { loadedTemplate ->
                    template = loadedTemplate
                }.onFailure { e ->
                    Log.e("WorkoutBuilder", "❌ Failed to load template: ${e.message}")
                }
            }

            if (template != null) {
                Log.d("WorkoutBuilder", "✅ Found template for session: ${template!!.name}")
                onLoaded(template)
            } else {
                Log.e("WorkoutBuilder", "❌ Template not found: $templateId")
                onLoaded(null)
            }
        }
    }

    /**
     * Save workout and return the WorkoutTemplate for starting a session
     * Used when clicking "Save & Start Session"
     * Returns null if workout limit reached for Free tier
     */
    fun saveWorkoutAndGetTemplate(): WorkoutTemplate? {
        Log.d("WorkoutBuilder", "saveWorkoutAndGetTemplate() called")

        if (_currentWorkoutName.value.isBlank() || _selectedExercises.value.isEmpty()) {
            Log.e("WorkoutBuilder", "Save failed: Invalid workout")
            return null
        }

        // Check workout limit for new workouts
        if (!canCreateWorkout()) {
            Log.e("WorkoutBuilder", "Save failed: Free tier workout limit reached (${SubscriptionManager.FREE_WORKOUT_LIMIT})")
            _workoutLimitReached.value = true
            return null
        }

        val workout = WorkoutTemplate(
            id = UUID.randomUUID().toString(),
            name = _currentWorkoutName.value,
            exercises = _selectedExercises.value,
            isILBTestWeek = _isILBTestMode.value
        )

        // Save to Supabase in background
        viewModelScope.launch {
            _isLoading.value = true
            try {
                WorkoutTemplateRepository.saveWorkoutTemplate(workout, getEffectiveUserId())
                    .onSuccess { savedId ->
                        Log.d("WorkoutBuilder", "✅ Saved workout to Supabase with ID: $savedId")
                    }
                    .onFailure { e ->
                        Log.e("WorkoutBuilder", "❌ Failed to save workout: ${e.message}")
                    }

                // Reload workouts from Supabase
                loadWorkouts()
            } finally {
                _isLoading.value = false
            }
        }

        clearCurrentWorkout()
        return workout
    }

    /**
     * Delete workout from Supabase
     */
    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                WorkoutTemplateRepository.deleteWorkoutTemplate(workoutId)
                    .onSuccess {
                        Log.d("WorkoutBuilder", "✅ Deleted workout from Supabase")
                        // Reload workouts
                        loadWorkouts()
                    }
                    .onFailure { e ->
                        Log.e("WorkoutBuilder", "❌ Failed to delete workout: ${e.message}")
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearCurrentWorkout() {
        _currentWorkoutName.value = ""
        _selectedExercises.value = emptyList()
        _isEditMode.value = false
        _editingWorkoutId.value = null
        _isTemplateCustomizationMode.value = false
        _isILBTestMode.value = false
    }

    fun startNewWorkout() {
        clearCurrentWorkout()
    }

    /**
     * Update sets for a specific exercise in a saved template
     * This directly updates the database and reloads the template
     */
    fun updateTemplateSets(
        templateId: String,
        exerciseId: String,
        newSets: List<ExerciseSet>,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // First, we need to find the workout_exercise_id from the loaded template
                val template = _publicWorkouts.value.find { it.id == templateId }
                    ?: _savedWorkouts.value.find { it.id == templateId }

                if (template == null) {
                    Log.e("WorkoutBuilder", "Template not found: $templateId")
                    onFailure("Template not found")
                    return@launch
                }

                val exercise = template.exercises.find { it.exerciseId == exerciseId }
                if (exercise == null) {
                    Log.e("WorkoutBuilder", "Exercise not found in template: $exerciseId")
                    onFailure("Exercise not found")
                    return@launch
                }

                // We need the workout_exercise_id from the database
                // For now, load the template again to get the workout_exercise_id
                // TODO: This could be optimized by storing the workout_exercise_id in ExerciseWithSets

                Log.d("WorkoutBuilder", "Updating sets for exercise: ${exercise.exerciseName}")
                Log.d("WorkoutBuilder", "New sets: ${newSets.size} sets")

                // For now, we reload the template to edit it properly
                // In a future optimization, we could store the workout_exercise_id
                loadWorkoutForEditing(templateId)
                updateExerciseSets(exerciseId, newSets)
                saveWorkout()

                onSuccess()
                Log.d("WorkoutBuilder", "✅ Updated template sets")
            } catch (e: Exception) {
                Log.e("WorkoutBuilder", "❌ Failed to update template sets: ${e.message}")
                onFailure(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
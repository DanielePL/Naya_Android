package com.example.menotracker.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.data.AuthRepository
import com.example.menotracker.data.BodyMeasurementRepository
import com.example.menotracker.data.PartnerInfo
import com.example.menotracker.data.ReferralRepository
import com.example.menotracker.data.SettingsDataStore
import com.example.menotracker.data.StrengthProfileRepository
import com.example.menotracker.data.UserProfileRepository
import com.example.menotracker.screens.account.ReferralCodeStatus
import com.example.menotracker.data.models.BodyMeasurement
import com.example.menotracker.data.models.PersonalRecord
import com.example.menotracker.data.models.StrengthExperienceLevel
import com.example.menotracker.data.models.StrengthGender
import com.example.menotracker.data.models.UserProfile
import com.example.menotracker.data.models.UserStrengthProfile
import com.example.menotracker.onboarding.data.BenchmarkDefinition
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AccountViewModel"
    }

    private val authRepository = AuthRepository(application)
    private val settingsDataStore = SettingsDataStore(application)

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _preferredSports = MutableStateFlow<List<String>>(emptyList())
    val preferredSports: StateFlow<List<String>> = _preferredSports.asStateFlow()

    private val _uploadStatus = MutableStateFlow<com.example.menotracker.screens.account.UploadStatus?>(null)
    val uploadStatus: StateFlow<com.example.menotracker.screens.account.UploadStatus?> = _uploadStatus.asStateFlow()

    // Body Measurements
    private val _latestMeasurement = MutableStateFlow<BodyMeasurement?>(null)
    val latestMeasurement: StateFlow<BodyMeasurement?> = _latestMeasurement.asStateFlow()

    private val _measurementHistory = MutableStateFlow<List<BodyMeasurement>>(emptyList())
    val measurementHistory: StateFlow<List<BodyMeasurement>> = _measurementHistory.asStateFlow()

    // Strength Profile (PR-based system)
    private val _strengthProfile = MutableStateFlow<UserStrengthProfile?>(null)
    val strengthProfile: StateFlow<UserStrengthProfile?> = _strengthProfile.asStateFlow()

    // Settings
    val isMetric: StateFlow<Boolean> = settingsDataStore.isMetric
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val notificationsEnabled: StateFlow<Boolean> = settingsDataStore.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val language: StateFlow<String> = settingsDataStore.language
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    // ═══════════════════════════════════════════════════════════════
    // FORM ANALYSIS SETTINGS
    // ═══════════════════════════════════════════════════════════════
    val frameSkipRate: StateFlow<Int> = settingsDataStore.frameSkipRate
        .stateIn(viewModelScope, SharingStarted.Eagerly, 2)

    val gridOverlayEnabled: StateFlow<Boolean> = settingsDataStore.gridOverlayEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val gridSpacingCm: StateFlow<Int> = settingsDataStore.gridSpacingCm
        .stateIn(viewModelScope, SharingStarted.Eagerly, 15)

    val gridLineThickness: StateFlow<Float> = settingsDataStore.gridLineThickness
        .stateIn(viewModelScope, SharingStarted.Eagerly, 2f)

    val skeletonLineThickness: StateFlow<Float> = settingsDataStore.skeletonLineThickness
        .stateIn(viewModelScope, SharingStarted.Eagerly, 8f)

    val showJointAngles: StateFlow<Boolean> = settingsDataStore.showJointAngles
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    init {
        // ✅ FIX: DON'T load profile immediately - wait for auth state to be determined
        // The profile will be loaded when isLoggedIn becomes true
        // This prevents loading profile with guest ID 00000000... when not logged in

        // Watch for auth state changes and reload profile or clear on logout
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { isLoggedIn ->
                Log.d(TAG, "🔐 Auth state changed: isLoggedIn=$isLoggedIn")
                if (isLoggedIn) {
                    Log.d(TAG, "🔄 User is logged in - loading profile with FORCE REFRESH")
                    // CRITICAL: Force refresh to load the NEW user's profile from Supabase
                    // Without forceRefresh, it would load the cached old user's profile
                    loadUserProfile(forceRefresh = true)
                } else {
                    // Clear the in-memory profile when user logs out
                    Log.d(TAG, "🚪 User not logged in - clearing in-memory profile")
                    _userProfile.value = null
                    _preferredSports.value = emptyList()
                    _strengthProfile.value = null
                    _latestMeasurement.value = null
                    _measurementHistory.value = emptyList()
                }
            }
        }
    }

    fun loadUserProfile(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = authRepository.getCurrentUserId()
                Log.d(TAG, "🔑🔑🔑 LOADING PROFILE - User ID from AuthRepository: $userId")
                Log.d(TAG, "🔑🔑🔑 forceRefresh=$forceRefresh")

                // Force fetch from Supabase if user is logged in
                UserProfileRepository.getCurrentProfile(userId, forceRefresh)
                    .onSuccess { profile ->
                        _userProfile.value = profile
                        _preferredSports.value = profile.preferredSports
                        Log.d(TAG, "✅ Loaded user profile: ${profile.name}")
                        Log.d(TAG, "🖼️ Profile image URL: ${profile.profileImageUrl}")
                        Log.d(TAG, "📊 Personal Records: ${profile.personalRecords.size}")
                        Log.d(TAG, "🏋️ Preferred Sports: ${profile.preferredSports}")
                    }
                    .onFailure { error ->
                        Log.e(TAG, "❌ Failed to load user profile: ${error.message}")
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateBodyStats(
        name: String,
        weight: Double?,
        height: Double?,
        age: Int? = null,
        activityLevel: com.example.menotracker.data.models.ActivityLevel? = null,
        years: Int?
    ) {
        Log.d(TAG, "🔵 updateBodyStats called: name=$name, weight=$weight, height=$height, age=$age, activityLevel=$activityLevel, years=$years")
        viewModelScope.launch {
            val currentProfile = _userProfile.value
            if (currentProfile == null) {
                Log.e(TAG, "❌ Cannot update: currentProfile is null")
                return@launch
            }

            Log.d(TAG, "📝 Current profile before update: name=${currentProfile.name}, weight=${currentProfile.weight}, height=${currentProfile.height}, years=${currentProfile.trainingExperience}")

            val updatedProfile = currentProfile.copy(
                name = name,
                weight = weight,
                height = height,
                age = age ?: currentProfile.age,
                gender = com.example.menotracker.data.models.Gender.FEMALE,  // Always female for menopause app
                activityLevel = activityLevel ?: currentProfile.activityLevel,
                trainingExperience = years
            )

            Log.d(TAG, "📝 Updated profile to save: name=${updatedProfile.name}, weight=${updatedProfile.weight}, height=${updatedProfile.height}, age=${updatedProfile.age}, years=${updatedProfile.trainingExperience}")

            UserProfileRepository.updateProfile(updatedProfile)
                .onSuccess {
                    _userProfile.value = updatedProfile
                    Log.d(TAG, "✅ Body stats updated successfully")
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to update body stats: ${error.message}")
                }
        }
    }

    fun savePRs(sport: String, prs: Map<String, String>) {
        viewModelScope.launch {
            val currentProfile = _userProfile.value ?: return@launch

            val updatedPRs = currentProfile.personalRecords.toMutableMap()

            // Add/update PRs for this sport
            prs.forEach { (exerciseName, valueString) ->
                if (valueString.isNotBlank()) {
                    val value = valueString.toDoubleOrNull() ?: return@forEach

                    val unit = getUnitForPR(exerciseName)
                    val pr = PersonalRecord(
                        exerciseName = exerciseName,
                        value = value,
                        unit = unit,
                        achievedAt = System.currentTimeMillis(),
                        sport = sport
                    )

                    updatedPRs[exerciseName] = pr
                }
            }

            val updatedProfile = currentProfile.copy(personalRecords = updatedPRs)

            UserProfileRepository.updateProfile(updatedProfile)
                .onSuccess {
                    _userProfile.value = updatedProfile
                    Log.d(TAG, "✅ Saved ${prs.size} PRs for $sport")

                    // Sync Powerlifting/General Strength PRs to UserStrengthProfile
                    if (sport == "Powerlifting" || sport == "General Strength") {
                        syncPRsToStrengthProfile(prs)
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to save PRs: ${error.message}")
                }
        }
    }

    /**
     * Sync Powerlifting PRs to UserStrengthProfile for weight recommendations
     */
    private fun syncPRsToStrengthProfile(prs: Map<String, String>) {
        val squatKg = prs["Squat"]?.toFloatOrNull()
        val benchKg = prs["Bench Press"]?.toFloatOrNull()
        val deadliftKg = prs["Deadlift"]?.toFloatOrNull()

        // Only update if at least one value is present
        if (squatKg != null || benchKg != null || deadliftKg != null) {
            Log.d(TAG, "🔄 Syncing PRs to StrengthProfile: Squat=$squatKg, Bench=$benchKg, Deadlift=$deadliftKg")
            updateStrengthPRs(squatKg, benchKg, deadliftKg)
        }
    }

    fun getPRsForSport(sport: String): Map<String, String> {
        val profile = _userProfile.value ?: return emptyMap()

        return profile.personalRecords
            .filter { it.value.sport == sport }
            .mapValues { it.value.value.toString() }
    }

    /**
     * Save a PR for a specific benchmark from SportBenchmarks
     */
    fun saveBenchmarkPR(benchmark: BenchmarkDefinition, value: Double) {
        viewModelScope.launch {
            val currentProfile = _userProfile.value ?: return@launch

            val updatedPRs = currentProfile.personalRecords.toMutableMap()

            val pr = PersonalRecord(
                exerciseName = benchmark.name,
                value = value,
                unit = benchmark.unit,
                achievedAt = System.currentTimeMillis(),
                sport = currentProfile.primarySport ?: "General"
            )

            // Use benchmark.id as key for consistent lookup
            updatedPRs[benchmark.id] = pr

            val updatedProfile = currentProfile.copy(personalRecords = updatedPRs)

            UserProfileRepository.updateProfile(updatedProfile)
                .onSuccess {
                    _userProfile.value = updatedProfile
                    Log.d(TAG, "✅ Saved benchmark PR: ${benchmark.name} = $value ${benchmark.unit}")
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to save benchmark PR: ${error.message}")
                }
        }
    }

    /**
     * Save a custom PR with user-defined exercise name
     */
    fun saveCustomPR(exerciseName: String, value: Double, unit: String) {
        viewModelScope.launch {
            val currentProfile = _userProfile.value ?: return@launch

            val updatedPRs = currentProfile.personalRecords.toMutableMap()

            // Create a unique key from the exercise name
            val key = "custom_${exerciseName.lowercase().replace(" ", "_")}"

            val pr = PersonalRecord(
                exerciseName = exerciseName,
                value = value,
                unit = unit,
                achievedAt = System.currentTimeMillis(),
                sport = "Custom"
            )

            updatedPRs[key] = pr

            val updatedProfile = currentProfile.copy(personalRecords = updatedPRs)

            UserProfileRepository.updateProfile(updatedProfile)
                .onSuccess {
                    _userProfile.value = updatedProfile
                    Log.d(TAG, "✅ Saved custom PR: $exerciseName = $value $unit")
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to save custom PR: ${error.message}")
                }
        }
    }

    fun togglePreferredSport(sport: String) {
        viewModelScope.launch {
            val currentProfile = _userProfile.value ?: return@launch

            val updatedSports = if (sport in currentProfile.preferredSports) {
                currentProfile.preferredSports - sport
            } else {
                currentProfile.preferredSports + sport
            }

            val updatedProfile = currentProfile.copy(preferredSports = updatedSports)

            UserProfileRepository.updateProfile(updatedProfile)
                .onSuccess {
                    _userProfile.value = updatedProfile
                    _preferredSports.value = updatedSports
                    Log.d(TAG, "✅ Updated preferred sports: $updatedSports")
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to update preferred sports: ${error.message}")
                }
        }
    }

    fun updateWorkoutDurationTarget(durationMinutes: Int?) {
        viewModelScope.launch {
            val currentProfile = _userProfile.value ?: return@launch

            val updatedProfile = currentProfile.copy(targetWorkoutDuration = durationMinutes)

            UserProfileRepository.updateProfile(updatedProfile)
                .onSuccess {
                    _userProfile.value = updatedProfile
                    Log.d(TAG, "✅ Updated target workout duration: $durationMinutes minutes")
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to update workout duration: ${error.message}")
                }
        }
    }

    fun setIsMetric(isMetric: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setIsMetric(isMetric)
            Log.d(TAG, "✅ Updated units: ${if (isMetric) "Metric" else "Imperial"}")
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            settingsDataStore.setLanguage(languageCode)
            Log.d(TAG, "✅ Updated language: $languageCode")
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setNotificationsEnabled(enabled)
            Log.d(TAG, "✅ Updated notifications: ${if (enabled) "Enabled" else "Disabled"}")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FORM ANALYSIS SETTINGS SETTERS
    // ═══════════════════════════════════════════════════════════════

    fun setFrameSkipRate(rate: Int) {
        viewModelScope.launch {
            settingsDataStore.setFrameSkipRate(rate)
            Log.d(TAG, "✅ Updated frame skip rate: $rate (${30/rate} FPS)")
        }
    }

    fun setGridOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setGridOverlayEnabled(enabled)
            Log.d(TAG, "✅ Updated grid overlay: ${if (enabled) "Enabled" else "Disabled"}")
        }
    }

    fun setGridSpacingCm(spacingCm: Int) {
        viewModelScope.launch {
            settingsDataStore.setGridSpacingCm(spacingCm)
            Log.d(TAG, "✅ Updated grid spacing: ${spacingCm}cm")
        }
    }

    fun setGridLineThickness(thickness: Float) {
        viewModelScope.launch {
            settingsDataStore.setGridLineThickness(thickness)
            Log.d(TAG, "✅ Updated grid line thickness: $thickness")
        }
    }

    fun setSkeletonLineThickness(thickness: Float) {
        viewModelScope.launch {
            settingsDataStore.setSkeletonLineThickness(thickness)
            Log.d(TAG, "✅ Updated skeleton thickness: $thickness")
        }
    }

    fun setShowJointAngles(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setShowJointAngles(enabled)
            Log.d(TAG, "✅ Updated show joint angles: ${if (enabled) "Enabled" else "Disabled"}")
        }
    }

    /**
     * Upload profile image
     */
    fun uploadProfileImage(imageFile: File) {
        viewModelScope.launch {
            _isLoading.value = true
            _uploadStatus.value = com.example.menotracker.screens.account.UploadStatus.Uploading

            try {
                val userId = authRepository.getCurrentUserId()
                Log.d(TAG, "📸 Uploading profile image for user: $userId")

                // Prevent guest users from uploading
                if (userId == AuthRepository.GUEST_USER_ID) {
                    Log.e(TAG, "❌ Guest users cannot upload profile pictures")
                    _uploadStatus.value = com.example.menotracker.screens.account.UploadStatus.Error("Please log in to upload a profile picture")
                    return@launch
                }

                UserProfileRepository.uploadProfileImage(userId, imageFile)
                    .onSuccess { imageUrl ->
                        Log.d(TAG, "✅ Profile image uploaded: $imageUrl")
                        _uploadStatus.value = com.example.menotracker.screens.account.UploadStatus.Success
                        // Force reload profile from Supabase to get updated image URL
                        loadUserProfile(forceRefresh = true)
                    }
                    .onFailure { error ->
                        Log.e(TAG, "❌ Failed to upload profile image: ${error.message}")
                        _uploadStatus.value = com.example.menotracker.screens.account.UploadStatus.Error(
                            error.message ?: "Unknown error"
                        )
                    }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during upload: ${e.message}")
                _uploadStatus.value = com.example.menotracker.screens.account.UploadStatus.Error(
                    e.message ?: "Unknown error"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearUploadStatus() {
        _uploadStatus.value = null
    }

    private fun getUnitForPR(pr: String): String {
        return when {
            // Reps (CrossFit gymnastics - bodyweight only)
            pr in listOf("Cindy", "Strict Pull-ups") -> "reps"
            pr.contains("Wall Balls") -> "reps"

            // Wilks (Powerlifting)
            pr == "Wilks Score" -> "pts"

            // Time-based (CrossFit WODs)
            pr in listOf("Fran", "Grace", "Isabel", "Helen", "Murph", "Total Race Time") -> "min:sec"

            // Time-based (Hyrox - specific to cardio/sled/carries)
            pr.contains("SkiErg") || pr.contains("1km Row") || pr == "1km Row" -> "min:sec"
            pr.contains("Sled Push") || pr.contains("Sled Pull") -> "sec"
            pr.contains("Farmers Carry") || pr.contains("Sandbag Lunges") || pr.contains("Burpee Broad Jump") -> "sec"

            // Everything else is weight (including Weighted Pull-ups, Weighted Dips, Barbell Row)
            else -> "kg"
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // BODY MEASUREMENTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Load the latest body measurement for the current user
     */
    fun loadBodyMeasurements() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            Log.d(TAG, "📏 Loading body measurements for user: $userId")

            BodyMeasurementRepository.getLatestMeasurement(userId)
                .onSuccess { measurement ->
                    _latestMeasurement.value = measurement
                    Log.d(TAG, "✅ Loaded latest measurement: ${measurement?.date}")
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to load measurements: ${error.message}")
                }
        }
    }

    /**
     * Load all body measurements for history view
     */
    fun loadMeasurementHistory() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            Log.d(TAG, "📏 Loading measurement history for user: $userId")

            BodyMeasurementRepository.getAllMeasurements(userId)
                .onSuccess { measurements ->
                    _measurementHistory.value = measurements
                    Log.d(TAG, "✅ Loaded ${measurements.size} measurements")
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to load measurement history: ${error.message}")
                }
        }
    }

    /**
     * Save or update a body measurement
     */
    fun saveBodyMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch {
            Log.d(TAG, "📏 Saving body measurement for date: ${measurement.date}")

            BodyMeasurementRepository.saveMeasurement(measurement)
                .onSuccess { saved ->
                    _latestMeasurement.value = saved
                    Log.d(TAG, "✅ Body measurement saved successfully")
                    // Reload history if it was loaded
                    if (_measurementHistory.value.isNotEmpty()) {
                        loadMeasurementHistory()
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to save body measurement: ${error.message}")
                }
        }
    }

    /**
     * Delete a body measurement
     */
    fun deleteBodyMeasurement(measurementId: String) {
        viewModelScope.launch {
            Log.d(TAG, "🗑️ Deleting body measurement: $measurementId")

            BodyMeasurementRepository.deleteMeasurement(measurementId)
                .onSuccess {
                    Log.d(TAG, "✅ Body measurement deleted successfully")
                    // Reload measurements
                    loadBodyMeasurements()
                    if (_measurementHistory.value.isNotEmpty()) {
                        loadMeasurementHistory()
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to delete body measurement: ${error.message}")
                }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // STRENGTH PROFILE (PR-based system)
    // ═══════════════════════════════════════════════════════════════

    private var strengthProfileRepository: StrengthProfileRepository? = null

    private fun getStrengthProfileRepository(): StrengthProfileRepository {
        if (strengthProfileRepository == null) {
            val supabase = createSupabaseClient(
                supabaseUrl = "https://lxkpaswqfvrwlcgtgjas.supabase.co",
                supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx4a3Bhc3dxZnZyd2xjZ3RnamFzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MjkxNTkwMTgsImV4cCI6MjA0NDczNTAxOH0.VBBjSjJ7CKcMd7R6rj2ZI-SHw15dv_wvhb6ysX2MtPE"
            ) {
                install(Postgrest)
            }
            strengthProfileRepository = StrengthProfileRepository(supabase)
        }
        return strengthProfileRepository!!
    }

    /**
     * Load user's strength profile from PersonalRecords
     * This builds the StrengthProfile directly from the existing PRs in the account
     * to avoid duplicate data entry - single source of truth!
     */
    fun loadStrengthProfile() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            val userPRs = _userProfile.value?.personalRecords ?: return@launch
            val profile = _userProfile.value ?: return@launch

            Log.d(TAG, "🏋️ Building strength profile from PersonalRecords")
            Log.d(TAG, "📋 Available PRs: ${userPRs.keys}")
            userPRs.forEach { (key, pr) ->
                Log.d(TAG, "   PR[$key] = ${pr.value} ${pr.unit} (${pr.exerciseName})")
            }

            // Look for Squat, Bench Press, Deadlift in existing PRs (from any sport)
            val squatPR = userPRs["Squat"]?.value?.toFloat() ?: 0f
            val benchPR = userPRs["Bench Press"]?.value?.toFloat() ?: 0f
            val deadliftPR = userPRs["Deadlift"]?.value?.toFloat() ?: 0f

            Log.d(TAG, "🎯 Extracted PRs: Squat=$squatPR, Bench=$benchPR, Deadlift=$deadliftPR")

            // Only create profile if at least one PR exists
            if (squatPR > 0 || benchPR > 0 || deadliftPR > 0) {
                val strengthProfile = UserStrengthProfile(
                    id = "local_${userId}",
                    userId = userId,
                    gender = StrengthGender.FEMALE,  // Always female for menopause app
                    bodyweightKg = profile.weight?.toFloat() ?: 80f,
                    experienceLevel = when (profile.trainingExperience) {
                        in 0..1 -> StrengthExperienceLevel.BEGINNER
                        in 2..4 -> StrengthExperienceLevel.INTERMEDIATE
                        in 5..10 -> StrengthExperienceLevel.EXPERIENCED
                        else -> StrengthExperienceLevel.ELITE
                    },
                    currentSquatKg = squatPR,
                    currentBenchKg = benchPR,
                    currentDeadliftKg = deadliftPR,
                    // Goals default to +10% of current
                    goalSquatKg = squatPR * 1.1f,
                    goalBenchKg = benchPR * 1.1f,
                    goalDeadliftKg = deadliftPR * 1.1f
                )
                _strengthProfile.value = strengthProfile
                Log.d(TAG, "✅ Built strength profile from PRs: Squat=$squatPR, Bench=$benchPR, Deadlift=$deadliftPR (Total: ${strengthProfile.currentTotal}kg)")
            } else {
                Log.d(TAG, "📊 No PRs found - add them via Powerlifting or General Strength in Personal Records")
                _strengthProfile.value = null
            }
        }
    }

    /**
     * Update current PRs in strength profile
     */
    fun updateStrengthPRs(
        squatKg: Float? = null,
        benchKg: Float? = null,
        deadliftKg: Float? = null
    ) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            Log.d(TAG, "🔄 Updating strength PRs for user: $userId")

            getStrengthProfileRepository().updateCurrentPRs(
                userId = userId,
                squatKg = squatKg,
                benchKg = benchKg,
                deadliftKg = deadliftKg
            )
                .onSuccess {
                    Log.d(TAG, "✅ Strength PRs updated")
                    loadStrengthProfile() // Reload to get updated data
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to update strength PRs: ${error.message}")
                }
        }
    }

    /**
     * Update goal PRs in strength profile
     */
    fun updateStrengthGoals(
        squatKg: Float? = null,
        benchKg: Float? = null,
        deadliftKg: Float? = null,
        targetDate: String? = null
    ) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            Log.d(TAG, "🎯 Updating strength goals for user: $userId")

            getStrengthProfileRepository().updateGoalPRs(
                userId = userId,
                squatKg = squatKg,
                benchKg = benchKg,
                deadliftKg = deadliftKg,
                targetDate = targetDate
            )
                .onSuccess {
                    Log.d(TAG, "✅ Strength goals updated")
                    loadStrengthProfile() // Reload to get updated data
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to update strength goals: ${error.message}")
                }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PARTNER REFERRAL CODE
    // ═══════════════════════════════════════════════════════════════

    private val _referralCodeStatus = MutableStateFlow<ReferralCodeStatus>(ReferralCodeStatus.Idle)
    val referralCodeStatus: StateFlow<ReferralCodeStatus> = _referralCodeStatus.asStateFlow()

    private val _currentPartnerName = MutableStateFlow<String?>(null)
    val currentPartnerName: StateFlow<String?> = _currentPartnerName.asStateFlow()

    // Temporarily store validated partner info for applying
    private var validatedPartner: PartnerInfo? = null

    /**
     * Check if user has already used a referral code
     */
    fun checkReferralCodeStatus() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()

            // Guest users cannot use referral codes
            if (userId == AuthRepository.GUEST_USER_ID) {
                Log.d(TAG, "👤 Guest user - referral codes not available")
                return@launch
            }

            Log.d(TAG, "🎁 Checking referral code status for user: $userId")

            ReferralRepository.hasUserUsedReferralCode(userId)
                .onSuccess { hasUsed ->
                    if (hasUsed) {
                        Log.d(TAG, "✅ User has already used a referral code")
                        // For now, just set a generic "used" state
                        // In a more complete implementation, we'd fetch the partner name
                        _currentPartnerName.value = "Partner Code Applied"
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to check referral status: ${error.message}")
                }
        }
    }

    /**
     * Validate a referral code without applying it
     */
    fun validateReferralCode(code: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()

            // Guest users cannot use referral codes
            if (userId == AuthRepository.GUEST_USER_ID) {
                _referralCodeStatus.value = ReferralCodeStatus.Error("Please log in to use referral codes")
                return@launch
            }

            // Check if user already has a referral code
            if (_currentPartnerName.value != null) {
                _referralCodeStatus.value = ReferralCodeStatus.AlreadyUsed
                return@launch
            }

            _referralCodeStatus.value = ReferralCodeStatus.Validating
            Log.d(TAG, "🔍 Validating referral code: $code")

            ReferralRepository.validateReferralCode(code)
                .onSuccess { partner ->
                    if (partner != null) {
                        validatedPartner = partner
                        _referralCodeStatus.value = ReferralCodeStatus.Valid(
                            partnerName = partner.name,
                            partnerType = partner.partnerType
                        )
                        Log.d(TAG, "✅ Valid code found: ${partner.name} (${partner.partnerType})")
                    } else {
                        validatedPartner = null
                        _referralCodeStatus.value = ReferralCodeStatus.Invalid
                        Log.d(TAG, "❌ Invalid or expired code")
                    }
                }
                .onFailure { error ->
                    validatedPartner = null
                    _referralCodeStatus.value = ReferralCodeStatus.Error(
                        error.message ?: "Failed to validate code"
                    )
                    Log.e(TAG, "❌ Error validating code: ${error.message}")
                }
        }
    }

    /**
     * Apply a validated referral code
     */
    fun applyReferralCode(code: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            val partner = validatedPartner

            if (partner == null) {
                _referralCodeStatus.value = ReferralCodeStatus.Error("Please validate the code first")
                return@launch
            }

            _referralCodeStatus.value = ReferralCodeStatus.Validating
            Log.d(TAG, "📝 Applying referral code: $code for partner: ${partner.name}")

            // Record the referral code entry
            ReferralRepository.recordReferralEntry(
                userId = userId,
                referralCode = code,
                partnerId = partner.id
            )
                .onSuccess {
                    _referralCodeStatus.value = ReferralCodeStatus.Success
                    _currentPartnerName.value = partner.name
                    validatedPartner = null
                    Log.d(TAG, "✅ Referral code applied successfully")
                }
                .onFailure { error ->
                    _referralCodeStatus.value = ReferralCodeStatus.Error(
                        error.message ?: "Failed to apply code"
                    )
                    Log.e(TAG, "❌ Error applying code: ${error.message}")
                }
        }
    }

    /**
     * Reset referral code status (for dialog dismissal)
     */
    fun resetReferralCodeStatus() {
        _referralCodeStatus.value = ReferralCodeStatus.Idle
        validatedPartner = null
    }
}

package com.example.menotracker.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.menotracker.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.math.sqrt

// ═══════════════════════════════════════════════════════════════
// WORKOUT PATTERN REPOSITORY
// Learns user's workout habits for smart nutrition timing
// ═══════════════════════════════════════════════════════════════

class WorkoutPatternRepository(context: Context) {

    companion object {
        private const val TAG = "WorkoutPatternRepo"
        private const val PREFS_NAME = "workout_patterns"
        private const val KEY_WORKOUT_RECORDS = "workout_records"
        private const val KEY_WORKOUT_PATTERN = "workout_pattern"
        private const val KEY_CURRENT_WORKOUT = "current_workout"

        // Pattern detection settings
        private const val MIN_WORKOUTS_FOR_PATTERN = 3
        private const val MAX_RECORDS_TO_KEEP = 100
        private const val RECENCY_WEIGHT_FACTOR = 0.1f  // More recent = more weight

        @Volatile
        private var INSTANCE: WorkoutPatternRepository? = null

        fun getInstance(context: Context): WorkoutPatternRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WorkoutPatternRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private val _workoutPattern = MutableStateFlow<WorkoutPattern?>(null)
    val workoutPattern: Flow<WorkoutPattern?> = _workoutPattern.asStateFlow()

    private val _currentWorkout = MutableStateFlow<WorkoutRecord?>(null)
    val currentWorkout: Flow<WorkoutRecord?> = _currentWorkout.asStateFlow()

    init {
        loadPattern()
        loadCurrentWorkout()
    }

    // ═══════════════════════════════════════════════════════════════
    // WORKOUT TRACKING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Start tracking a new workout session
     */
    suspend fun startWorkout(
        workoutType: String? = null,
        wasFasted: Boolean = false,
        preWorkoutMealTimeMillis: Long? = null
    ): WorkoutRecord = withContext(Dispatchers.IO) {
        val now = LocalDateTime.now()

        val record = WorkoutRecord(
            id = UUID.randomUUID().toString(),
            startTimeMillis = System.currentTimeMillis(),
            dayOfWeek = now.dayOfWeek.value,
            hourOfDay = now.hour,
            minuteOfHour = now.minute,
            workoutType = workoutType,
            wasFasted = wasFasted,
            preWorkoutMealTimeMillis = preWorkoutMealTimeMillis
        )

        _currentWorkout.value = record
        saveCurrentWorkout(record)

        Log.d(TAG, "Started workout: ${record.id} at ${now.hour}:${now.minute}")
        record
    }

    /**
     * End the current workout and record it
     */
    suspend fun endWorkout(): WorkoutRecord? = withContext(Dispatchers.IO) {
        val current = _currentWorkout.value ?: return@withContext null

        val endTime = System.currentTimeMillis()
        val durationMinutes = ((endTime - current.startTimeMillis) / 60000).toInt()

        val completedRecord = current.copy(
            endTimeMillis = endTime,
            durationMinutes = durationMinutes
        )

        // Save to history
        addWorkoutRecord(completedRecord)

        // Clear current workout
        _currentWorkout.value = null
        prefs.edit().remove(KEY_CURRENT_WORKOUT).apply()

        // Recalculate pattern
        recalculatePattern()

        Log.d(TAG, "Ended workout: ${completedRecord.id}, duration: ${durationMinutes}min")
        completedRecord
    }

    /**
     * Cancel current workout without recording
     */
    fun cancelWorkout() {
        _currentWorkout.value = null
        prefs.edit().remove(KEY_CURRENT_WORKOUT).apply()
        Log.d(TAG, "Workout cancelled")
    }

    /**
     * Check if a workout is currently in progress
     */
    fun isWorkoutInProgress(): Boolean = _currentWorkout.value != null

    // ═══════════════════════════════════════════════════════════════
    // PATTERN DETECTION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get all recorded workouts
     */
    private fun getWorkoutRecords(): List<WorkoutRecord> {
        val jsonString = prefs.getString(KEY_WORKOUT_RECORDS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<WorkoutRecord>>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse workout records", e)
            emptyList()
        }
    }

    /**
     * Add a workout record to history
     */
    private fun addWorkoutRecord(record: WorkoutRecord) {
        val records = getWorkoutRecords().toMutableList()
        records.add(record)

        // Keep only the most recent records
        val trimmedRecords = records
            .sortedByDescending { it.startTimeMillis }
            .take(MAX_RECORDS_TO_KEEP)

        val jsonString = json.encodeToString(trimmedRecords)
        prefs.edit().putString(KEY_WORKOUT_RECORDS, jsonString).apply()
    }

    /**
     * Recalculate the workout pattern from history
     */
    private fun recalculatePattern() {
        val records = getWorkoutRecords()

        if (records.size < MIN_WORKOUTS_FOR_PATTERN) {
            Log.d(TAG, "Not enough workouts for pattern: ${records.size}/$MIN_WORKOUTS_FOR_PATTERN")
            _workoutPattern.value = null
            return
        }

        // Group by day of week
        val byDay = records.groupBy { it.dayOfWeek }

        val dayPatterns = mutableListOf<DayPattern>()

        for (dayOfWeek in 1..7) {
            val dayRecords = byDay[dayOfWeek] ?: continue

            if (dayRecords.size >= 2) {
                // Calculate weighted average (more recent workouts have more weight)
                val sortedRecords = dayRecords.sortedByDescending { it.startTimeMillis }
                var weightedSum = 0f
                var totalWeight = 0f

                sortedRecords.forEachIndexed { index, record ->
                    val weight = 1f / (1f + index * RECENCY_WEIGHT_FACTOR)
                    weightedSum += record.getHourAsFloat() * weight
                    totalWeight += weight
                }

                val averageHour = weightedSum / totalWeight

                // Calculate standard deviation
                val variance = dayRecords.map { record ->
                    val diff = record.getHourAsFloat() - averageHour
                    diff * diff
                }.average()

                val stdDev = sqrt(variance).toFloat()

                // Calculate confidence based on:
                // - Number of occurrences
                // - Consistency (low std dev)
                // - Recency of data
                val occurrenceScore = (dayRecords.size.toFloat() / records.size).coerceAtMost(0.5f) * 2
                val consistencyScore = (1f - (stdDev / 12f)).coerceIn(0f, 1f)  // 12h = max variation
                val recencyScore = calculateRecencyScore(dayRecords)

                val confidence = (occurrenceScore * 0.3f + consistencyScore * 0.4f + recencyScore * 0.3f)
                    .coerceIn(0f, 1f)

                dayPatterns.add(
                    DayPattern(
                        dayOfWeek = dayOfWeek,
                        occurrenceCount = dayRecords.size,
                        averageHour = averageHour,
                        stdDeviation = stdDev,
                        confidence = confidence
                    )
                )
            }
        }

        // Calculate overall confidence
        val overallConfidence = if (dayPatterns.isNotEmpty()) {
            dayPatterns.map { it.confidence }.average().toFloat()
        } else 0f

        // Find most common workout type
        val mostCommonType = records
            .mapNotNull { it.workoutType }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        // Calculate average duration
        val avgDuration = records
            .mapNotNull { it.durationMinutes }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toInt()

        val pattern = WorkoutPattern(
            userId = "default",
            dayPatterns = dayPatterns,
            totalWorkoutsTracked = records.size,
            overallConfidence = overallConfidence,
            mostCommonType = mostCommonType,
            averageDurationMinutes = avgDuration,
            lastUpdatedMillis = System.currentTimeMillis()
        )

        _workoutPattern.value = pattern
        savePattern(pattern)

        Log.d(TAG, "Pattern updated: ${dayPatterns.size} days detected, confidence: $overallConfidence")
    }

    /**
     * Calculate how recent the workout data is
     */
    private fun calculateRecencyScore(records: List<WorkoutRecord>): Float {
        if (records.isEmpty()) return 0f

        val now = System.currentTimeMillis()
        val twoWeeksAgo = now - (14 * 24 * 60 * 60 * 1000L)

        val recentCount = records.count { it.startTimeMillis > twoWeeksAgo }
        return (recentCount.toFloat() / records.size).coerceIn(0f, 1f)
    }

    // ═══════════════════════════════════════════════════════════════
    // PREDICTIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Check if workout is likely today
     */
    fun isWorkoutLikelyToday(): Boolean {
        val pattern = _workoutPattern.value ?: return false
        val todayDayOfWeek = LocalDateTime.now().dayOfWeek.value
        return pattern.isWorkoutLikelyToday(todayDayOfWeek)
    }

    /**
     * Get predicted workout time for today (as Instant)
     */
    fun getPredictedWorkoutTimeToday(): Instant? {
        val pattern = _workoutPattern.value ?: return null
        val today = LocalDateTime.now()
        val todayDayOfWeek = today.dayOfWeek.value

        val predictedHour = pattern.getPredictedTimeToday(todayDayOfWeek) ?: return null

        val hour = predictedHour.toInt()
        val minute = ((predictedHour - hour) * 60).toInt()

        val predictedDateTime = today
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)

        return predictedDateTime.atZone(ZoneId.systemDefault()).toInstant()
    }

    /**
     * Get hours until predicted workout
     */
    fun getHoursUntilPredictedWorkout(): Float? {
        val predictedTime = getPredictedWorkoutTimeToday() ?: return null
        val now = Instant.now()

        if (predictedTime.isBefore(now)) return null  // Already passed

        val millisUntil = predictedTime.toEpochMilli() - now.toEpochMilli()
        return millisUntil / (1000f * 60f * 60f)
    }

    /**
     * Get confidence for today's prediction
     */
    fun getTodayPredictionConfidence(): Float {
        val pattern = _workoutPattern.value ?: return 0f
        val todayDayOfWeek = LocalDateTime.now().dayOfWeek.value
        return pattern.getPatternForDay(todayDayOfWeek)?.confidence ?: 0f
    }

    /**
     * Get the day pattern for today
     */
    fun getTodayPattern(): DayPattern? {
        val pattern = _workoutPattern.value ?: return null
        val todayDayOfWeek = LocalDateTime.now().dayOfWeek.value
        return pattern.getPatternForDay(todayDayOfWeek)
    }

    // ═══════════════════════════════════════════════════════════════
    // PERSISTENCE
    // ═══════════════════════════════════════════════════════════════

    private fun loadPattern() {
        val jsonString = prefs.getString(KEY_WORKOUT_PATTERN, null)
        if (jsonString != null) {
            try {
                _workoutPattern.value = json.decodeFromString<WorkoutPattern>(jsonString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load pattern", e)
            }
        }
    }

    private fun savePattern(pattern: WorkoutPattern) {
        val jsonString = json.encodeToString(pattern)
        prefs.edit().putString(KEY_WORKOUT_PATTERN, jsonString).apply()
    }

    private fun loadCurrentWorkout() {
        val jsonString = prefs.getString(KEY_CURRENT_WORKOUT, null)
        if (jsonString != null) {
            try {
                _currentWorkout.value = json.decodeFromString<WorkoutRecord>(jsonString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load current workout", e)
            }
        }
    }

    private fun saveCurrentWorkout(record: WorkoutRecord) {
        val jsonString = json.encodeToString(record)
        prefs.edit().putString(KEY_CURRENT_WORKOUT, jsonString).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // DEBUG / TESTING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Clear all workout data (for testing)
     */
    fun clearAllData() {
        prefs.edit().clear().apply()
        _workoutPattern.value = null
        _currentWorkout.value = null
        Log.d(TAG, "All workout data cleared")
    }

    /**
     * Add fake workout records for testing pattern detection
     */
    suspend fun addTestWorkouts() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L

        // Simulate 4 weeks of workouts (Mon, Wed, Fri around 18:00)
        val testRecords = listOf(
            // Week 1
            createTestRecord(now - 28 * dayMillis, DayOfWeek.MONDAY, 18, 15),
            createTestRecord(now - 26 * dayMillis, DayOfWeek.WEDNESDAY, 18, 30),
            createTestRecord(now - 24 * dayMillis, DayOfWeek.FRIDAY, 17, 45),
            // Week 2
            createTestRecord(now - 21 * dayMillis, DayOfWeek.MONDAY, 18, 0),
            createTestRecord(now - 19 * dayMillis, DayOfWeek.WEDNESDAY, 18, 20),
            createTestRecord(now - 17 * dayMillis, DayOfWeek.FRIDAY, 18, 10),
            // Week 3
            createTestRecord(now - 14 * dayMillis, DayOfWeek.MONDAY, 17, 50),
            createTestRecord(now - 12 * dayMillis, DayOfWeek.WEDNESDAY, 18, 15),
            createTestRecord(now - 10 * dayMillis, DayOfWeek.FRIDAY, 18, 0),
            // Week 4
            createTestRecord(now - 7 * dayMillis, DayOfWeek.MONDAY, 18, 5),
            createTestRecord(now - 5 * dayMillis, DayOfWeek.WEDNESDAY, 18, 10),
            createTestRecord(now - 3 * dayMillis, DayOfWeek.FRIDAY, 17, 55)
        )

        testRecords.forEach { addWorkoutRecord(it) }
        recalculatePattern()

        Log.d(TAG, "Added ${testRecords.size} test workouts")
    }

    private fun createTestRecord(
        baseTimeMillis: Long,
        dayOfWeek: DayOfWeek,
        hour: Int,
        minute: Int
    ): WorkoutRecord {
        return WorkoutRecord(
            id = UUID.randomUUID().toString(),
            startTimeMillis = baseTimeMillis,
            endTimeMillis = baseTimeMillis + (60 * 60 * 1000),  // 1 hour workout
            dayOfWeek = dayOfWeek.value,
            hourOfDay = hour,
            minuteOfHour = minute,
            durationMinutes = 60,
            workoutType = "STRENGTH"
        )
    }
}

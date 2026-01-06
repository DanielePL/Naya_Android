package com.example.menotracker.data.repository

import android.util.Log
import com.example.menotracker.billing.SubscriptionManager
import com.example.menotracker.data.SupabaseClient
import com.example.menotracker.data.models.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Repository for Mood Journaling
 * Integrates with Supabase mood_entries table
 * Handles tier-based limits (FREE: 3/week, PREMIUM: unlimited)
 */
object MoodRepository {
    private const val TAG = "MoodRepository"
    private const val TABLE_NAME = "mood_entries"

    private val supabase get() = SupabaseClient.client

    // ═══════════════════════════════════════════════════════════════
    // LOCAL STATE
    // ═══════════════════════════════════════════════════════════════

    private val _todayMoods = MutableStateFlow<List<MoodEntry>>(emptyList())
    val todayMoods: StateFlow<List<MoodEntry>> = _todayMoods.asStateFlow()

    private val _weeklyMoods = MutableStateFlow<List<MoodEntry>>(emptyList())
    val weeklyMoods: StateFlow<List<MoodEntry>> = _weeklyMoods.asStateFlow()

    private val _weeklyStats = MutableStateFlow<WeeklyMoodSummary?>(null)
    val weeklyStats: StateFlow<WeeklyMoodSummary?> = _weeklyStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // TIER LIMIT HELPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get count of mood entries this week
     */
    suspend fun getWeeklyEntryCount(userId: String): Int {
        val weekStart = getWeekStart()
        return _weeklyMoods.value.count {
            it.loggedAt >= weekStart
        }
    }

    /**
     * Check if user can add a new mood entry
     */
    suspend fun canAddMoodEntry(userId: String): Boolean {
        val weeklyCount = getWeeklyEntryCount(userId)
        return SubscriptionManager.canAddMoodEntry(weeklyCount)
    }

    /**
     * Get remaining mood slots this week
     */
    suspend fun getRemainingSlots(userId: String): Int {
        val weeklyCount = getWeeklyEntryCount(userId)
        return SubscriptionManager.getRemainingMoodSlots(weeklyCount)
    }

    private fun getWeekStart(): String {
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return "${weekStart}T00:00:00"
    }

    // ═══════════════════════════════════════════════════════════════
    // LOG MOOD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Log a new mood entry
     */
    suspend fun logMood(
        userId: String,
        moodType: MoodType,
        intensity: Int,
        timeOfDay: TimeOfDay? = null,
        triggers: List<MoodTrigger>? = null,
        journalText: String? = null,
        linkedSymptomIds: List<String>? = null
    ): Result<MoodEntry> = withContext(Dispatchers.IO) {
        try {
            // Check tier limit
            if (!canAddMoodEntry(userId)) {
                return@withContext Result.failure(
                    Exception("Weekly limit reached. Upgrade to Premium for unlimited mood entries.")
                )
            }

            _isLoading.value = true
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            Log.d(TAG, "Logging mood: ${moodType.displayName}, intensity: $intensity")

            val newEntry = MoodEntryInsert(
                user_id = userId,
                mood_type = moodType.name,
                intensity = intensity.coerceIn(1, 5),
                time_of_day = timeOfDay?.name,
                triggers = triggers?.map { it.name },
                journal_text = journalText?.takeIf { it.isNotBlank() },
                linked_symptom_ids = linkedSymptomIds,
                logged_at = now
            )

            val createdEntry = supabase.postgrest[TABLE_NAME]
                .insert(newEntry)
                .decodeSingle<MoodEntry>()

            // Update local state
            _todayMoods.value = _todayMoods.value + createdEntry
            _weeklyMoods.value = _weeklyMoods.value + createdEntry

            Log.d(TAG, "Mood logged successfully: ${createdEntry.id}")
            Result.success(createdEntry)

        } catch (e: Exception) {
            Log.e(TAG, "Error logging mood", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Quick mood log without details
     */
    suspend fun quickLogMood(
        userId: String,
        moodType: MoodType,
        intensity: Int = 3
    ): Result<MoodEntry> {
        val timeOfDay = getCurrentTimeOfDay()
        return logMood(userId, moodType, intensity, timeOfDay)
    }

    private fun getCurrentTimeOfDay(): TimeOfDay {
        val hour = LocalDateTime.now().hour
        return when {
            hour < 12 -> TimeOfDay.MORNING
            hour < 17 -> TimeOfDay.AFTERNOON
            hour < 21 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET MOODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Load today's moods
     */
    suspend fun getTodayMoods(userId: String): Result<List<MoodEntry>> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

            Log.d(TAG, "Fetching today's moods for $today")

            val moods = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("logged_at", "${today}T00:00:00")
                        lte("logged_at", "${today}T23:59:59")
                    }
                }
                .decodeList<MoodEntry>()

            _todayMoods.value = moods.sortedByDescending { it.loggedAt }

            Log.d(TAG, "Found ${moods.size} moods today")
            Result.success(moods)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching today's moods", e)
            Result.failure(e)
        }
    }

    /**
     * Load this week's moods
     */
    suspend fun getWeeklyMoods(userId: String): Result<List<MoodEntry>> = withContext(Dispatchers.IO) {
        try {
            val weekStart = getWeekStart()

            Log.d(TAG, "Fetching weekly moods since $weekStart")

            val moods = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("logged_at", weekStart)
                    }
                }
                .decodeList<MoodEntry>()

            _weeklyMoods.value = moods.sortedByDescending { it.loggedAt }

            // Update weekly stats
            updateWeeklyStats(userId, moods)

            Log.d(TAG, "Found ${moods.size} moods this week")
            Result.success(moods)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weekly moods", e)
            Result.failure(e)
        }
    }

    /**
     * Load mood history
     */
    suspend fun getMoodHistory(
        userId: String,
        days: Int = 30
    ): Result<List<MoodEntry>> = withContext(Dispatchers.IO) {
        try {
            val startDate = LocalDate.now().minusDays(days.toLong())
                .format(DateTimeFormatter.ISO_LOCAL_DATE)

            Log.d(TAG, "Fetching mood history since $startDate")

            val moods = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("logged_at", "${startDate}T00:00:00")
                    }
                }
                .decodeList<MoodEntry>()

            Log.d(TAG, "Found ${moods.size} moods in last $days days")
            Result.success(moods.sortedByDescending { it.loggedAt })

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching mood history", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════

    private suspend fun updateWeeklyStats(userId: String, moods: List<MoodEntry>) {
        val weeklyCount = moods.size
        val remainingSlots = SubscriptionManager.getRemainingMoodSlots(weeklyCount)
        val canAddMore = SubscriptionManager.canAddMoodEntry(weeklyCount)

        // Calculate mood distribution
        val moodCounts = moods
            .mapNotNull { it.moodEnum }
            .groupingBy { it }
            .eachCount()

        val totalEntries = moods.size
        val moodDistribution = moodCounts.mapValues { (moodType, count) ->
            MoodStats(
                moodType = moodType,
                occurrenceCount = count,
                avgIntensity = moods
                    .filter { it.moodEnum == moodType }
                    .map { it.intensity }
                    .average()
                    .toFloat(),
                percentage = if (totalEntries > 0) count.toFloat() / totalEntries else 0f
            )
        }

        // Find dominant mood
        val dominantMood = moodCounts.maxByOrNull { it.value }?.key

        // Calculate average intensity
        val avgIntensity = if (moods.isNotEmpty()) {
            moods.map { it.intensity }.average().toFloat()
        } else 0f

        // Find most common triggers
        val triggerCounts = moods
            .flatMap { it.triggerEnums }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        _weeklyStats.value = WeeklyMoodSummary(
            totalEntries = totalEntries,
            dominantMood = dominantMood,
            averageIntensity = avgIntensity,
            moodDistribution = moodDistribution,
            mostCommonTriggers = triggerCounts,
            entriesRemaining = remainingSlots,
            canAddMore = canAddMore
        )
    }

    /**
     * Get mood trends over time
     */
    suspend fun getMoodTrends(
        userId: String,
        days: Int = 14
    ): Result<List<MoodTrendPoint>> = withContext(Dispatchers.IO) {
        try {
            val historyResult = getMoodHistory(userId, days)
            if (historyResult.isFailure) {
                return@withContext Result.failure(historyResult.exceptionOrNull()!!)
            }

            val moods = historyResult.getOrThrow()

            // Group by date and calculate average mood score
            val trends = moods
                .groupBy { it.loggedAt.substring(0, 10) } // Group by date
                .map { (date, entries) ->
                    val avgScore = entries.mapNotNull { it.moodEnum?.toScore() }
                        .average()
                        .toFloat()
                    val dominantMood = entries
                        .mapNotNull { it.moodEnum }
                        .groupingBy { it }
                        .eachCount()
                        .maxByOrNull { it.value }
                        ?.key

                    MoodTrendPoint(
                        date = date,
                        averageMoodScore = avgScore,
                        dominantMood = dominantMood
                    )
                }
                .sortedBy { it.date }

            Result.success(trends)

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating mood trends", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE / DELETE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Delete a mood entry
     */
    suspend fun deleteMood(userId: String, moodId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting mood: $moodId")

            supabase.postgrest[TABLE_NAME]
                .delete {
                    filter {
                        eq("id", moodId)
                        eq("user_id", userId)
                    }
                }

            // Update local state
            _todayMoods.value = _todayMoods.value.filter { it.id != moodId }
            _weeklyMoods.value = _weeklyMoods.value.filter { it.id != moodId }

            Log.d(TAG, "Mood deleted")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error deleting mood", e)
            Result.failure(e)
        }
    }

    /**
     * Update mood journal text
     */
    suspend fun updateJournalText(
        userId: String,
        moodId: String,
        newText: String
    ): Result<MoodEntry> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating journal text for mood: $moodId")

            supabase.postgrest[TABLE_NAME]
                .update({
                    set("journal_text", newText)
                }) {
                    filter {
                        eq("id", moodId)
                        eq("user_id", userId)
                    }
                }

            // Fetch updated entry
            val updatedEntries = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("id", moodId)
                    }
                }
                .decodeList<MoodEntry>()

            val updatedEntry = updatedEntries.firstOrNull()
                ?: return@withContext Result.failure(Exception("Mood not found after update"))

            // Update local state
            _todayMoods.value = _todayMoods.value.map {
                if (it.id == moodId) updatedEntry else it
            }
            _weeklyMoods.value = _weeklyMoods.value.map {
                if (it.id == moodId) updatedEntry else it
            }

            Log.d(TAG, "Journal text updated")
            Result.success(updatedEntry)

        } catch (e: Exception) {
            Log.e(TAG, "Error updating journal text", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════

    /**
     * Clear state (e.g., on logout)
     */
    fun clearState() {
        _todayMoods.value = emptyList()
        _weeklyMoods.value = emptyList()
        _weeklyStats.value = null
        _isLoading.value = false
    }

    /**
     * Refresh all data
     */
    suspend fun refreshAll(userId: String) {
        getTodayMoods(userId)
        getWeeklyMoods(userId)
    }
}

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
 * Repository for Breathing Exercises
 * Integrates with Supabase breathing_sessions table
 * Handles tier-based access (FREE: 1 exercise, PREMIUM: full library)
 */
object BreathingRepository {
    private const val TAG = "BreathingRepository"
    private const val TABLE_NAME = "breathing_sessions"

    private val supabase get() = SupabaseClient.client

    // ═══════════════════════════════════════════════════════════════
    // LOCAL STATE
    // ═══════════════════════════════════════════════════════════════

    private val _recentSessions = MutableStateFlow<List<BreathingSession>>(emptyList())
    val recentSessions: StateFlow<List<BreathingSession>> = _recentSessions.asStateFlow()

    private val _weeklyStats = MutableStateFlow<WeeklyBreathingSummary?>(null)
    val weeklyStats: StateFlow<WeeklyBreathingSummary?> = _weeklyStats.asStateFlow()

    private val _totalStats = MutableStateFlow<BreathingStats?>(null)
    val totalStats: StateFlow<BreathingStats?> = _totalStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // TIER ACCESS HELPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get available exercises based on tier
     */
    fun getAvailableExercises(): List<BreathingExerciseType> {
        val hasFullAccess = SubscriptionManager.hasAccess(
            com.example.menotracker.billing.Feature.BREATHING_LIBRARY
        )

        return if (hasFullAccess) {
            BreathingExerciseType.entries
        } else {
            BreathingExerciseType.entries.filter { it.isFree }
        }
    }

    /**
     * Check if a specific exercise is accessible
     */
    fun canAccessExercise(exerciseType: BreathingExerciseType): Boolean {
        return SubscriptionManager.canAccessBreathingExercise(exerciseType.isFree)
    }

    /**
     * Get locked exercises (for showing upgrade prompt)
     */
    fun getLockedExercises(): List<BreathingExerciseType> {
        val hasFullAccess = SubscriptionManager.hasAccess(
            com.example.menotracker.billing.Feature.BREATHING_LIBRARY
        )

        return if (hasFullAccess) {
            emptyList()
        } else {
            BreathingExerciseType.entries.filter { !it.isFree }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // LOG SESSION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Log a completed breathing session
     */
    suspend fun logSession(
        userId: String,
        exerciseType: BreathingExerciseType,
        durationSeconds: Int,
        cyclesCompleted: Int,
        moodBefore: Int? = null,
        moodAfter: Int? = null
    ): Result<BreathingSession> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            Log.d(TAG, "Logging breathing session: ${exerciseType.displayName}, duration: ${durationSeconds}s")

            val newSession = BreathingSessionInsert(
                user_id = userId,
                exercise_type = exerciseType.name,
                duration_seconds = durationSeconds,
                cycles_completed = cyclesCompleted,
                mood_before = moodBefore,
                mood_after = moodAfter,
                completed_at = now
            )

            val createdSession = supabase.postgrest[TABLE_NAME]
                .insert(newSession)
                .decodeSingle<BreathingSession>()

            // Update local state
            _recentSessions.value = listOf(createdSession) + _recentSessions.value

            Log.d(TAG, "Breathing session logged: ${createdSession.id}")
            Result.success(createdSession)

        } catch (e: Exception) {
            Log.e(TAG, "Error logging breathing session", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET SESSIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get recent breathing sessions
     */
    suspend fun getRecentSessions(
        userId: String,
        limit: Int = 10
    ): Result<List<BreathingSession>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching recent breathing sessions")

            val sessions = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<BreathingSession>()
                .sortedByDescending { it.completedAt }
                .take(limit)

            _recentSessions.value = sessions

            Log.d(TAG, "Found ${sessions.size} recent sessions")
            Result.success(sessions)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching recent sessions", e)
            Result.failure(e)
        }
    }

    /**
     * Get sessions for this week
     */
    suspend fun getWeeklySessions(userId: String): Result<List<BreathingSession>> = withContext(Dispatchers.IO) {
        try {
            val weekStart = getWeekStart()

            Log.d(TAG, "Fetching weekly sessions since $weekStart")

            val sessions = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("completed_at", weekStart)
                    }
                }
                .decodeList<BreathingSession>()

            // Update weekly stats
            updateWeeklyStats(sessions)

            Log.d(TAG, "Found ${sessions.size} sessions this week")
            Result.success(sessions.sortedByDescending { it.completedAt })

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weekly sessions", e)
            Result.failure(e)
        }
    }

    private fun getWeekStart(): String {
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return "${weekStart}T00:00:00"
    }

    // ═══════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════

    private fun updateWeeklyStats(sessions: List<BreathingSession>) {
        if (sessions.isEmpty()) {
            _weeklyStats.value = WeeklyBreathingSummary(
                sessionsCompleted = 0,
                totalMinutes = 0,
                exerciseDistribution = emptyMap(),
                averageMoodBefore = 0f,
                averageMoodAfter = 0f,
                bestDay = null,
                streak = 0
            )
            return
        }

        val totalMinutes = sessions.sumOf { it.durationSeconds } / 60

        // Exercise distribution
        val distribution = sessions
            .mapNotNull { it.exerciseEnum }
            .groupingBy { it }
            .eachCount()

        // Average mood before/after
        val moodsBefore = sessions.mapNotNull { it.moodBefore }
        val moodsAfter = sessions.mapNotNull { it.moodAfter }
        val avgMoodBefore = if (moodsBefore.isNotEmpty()) moodsBefore.average().toFloat() else 0f
        val avgMoodAfter = if (moodsAfter.isNotEmpty()) moodsAfter.average().toFloat() else 0f

        // Best day (most sessions)
        val sessionsByDay = sessions.groupBy { it.completedAt.substring(0, 10) }
        val bestDay = sessionsByDay.maxByOrNull { it.value.size }?.key

        _weeklyStats.value = WeeklyBreathingSummary(
            sessionsCompleted = sessions.size,
            totalMinutes = totalMinutes,
            exerciseDistribution = distribution,
            averageMoodBefore = avgMoodBefore,
            averageMoodAfter = avgMoodAfter,
            bestDay = bestDay,
            streak = calculateStreak(sessions)
        )
    }

    private fun calculateStreak(sessions: List<BreathingSession>): Int {
        if (sessions.isEmpty()) return 0

        val today = LocalDate.now()
        val sessionDates = sessions
            .map { it.completedAt.substring(0, 10) }
            .distinct()
            .sorted()
            .reversed()

        var streak = 0
        var currentDate = today

        for (dateStr in sessionDates) {
            val sessionDate = LocalDate.parse(dateStr)
            if (sessionDate == currentDate || sessionDate == currentDate.minusDays(1)) {
                streak++
                currentDate = sessionDate
            } else {
                break
            }
        }

        return streak
    }

    /**
     * Get all-time statistics
     */
    suspend fun getTotalStats(userId: String): Result<BreathingStats> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Calculating total breathing stats")

            val sessions = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<BreathingSession>()

            if (sessions.isEmpty()) {
                val emptyStats = BreathingStats(
                    totalSessions = 0,
                    totalMinutes = 0,
                    favoriteExercise = null,
                    averageMoodImprovement = 0f,
                    weeklySessionCount = 0,
                    currentStreak = 0
                )
                _totalStats.value = emptyStats
                return@withContext Result.success(emptyStats)
            }

            val totalMinutes = sessions.sumOf { it.durationSeconds } / 60

            // Favorite exercise
            val favorite = sessions
                .mapNotNull { it.exerciseEnum }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key

            // Average mood improvement
            val improvements = sessions.mapNotNull { it.moodImprovement }
            val avgImprovement = if (improvements.isNotEmpty()) improvements.average().toFloat() else 0f

            // Weekly count
            val weekStart = getWeekStart()
            val weeklyCount = sessions.count { it.completedAt >= weekStart }

            val stats = BreathingStats(
                totalSessions = sessions.size,
                totalMinutes = totalMinutes,
                favoriteExercise = favorite,
                averageMoodImprovement = avgImprovement,
                weeklySessionCount = weeklyCount,
                currentStreak = calculateStreak(sessions.sortedByDescending { it.completedAt })
            )

            _totalStats.value = stats

            Log.d(TAG, "Calculated total stats: ${sessions.size} sessions, $totalMinutes minutes")
            Result.success(stats)

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating total stats", e)
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
        _recentSessions.value = emptyList()
        _weeklyStats.value = null
        _totalStats.value = null
        _isLoading.value = false
    }

    /**
     * Refresh all data
     */
    suspend fun refreshAll(userId: String) {
        getRecentSessions(userId)
        getWeeklySessions(userId)
        getTotalStats(userId)
    }
}

package com.example.menotracker.data.repository

import android.util.Log
import com.example.menotracker.billing.SubscriptionManager
import com.example.menotracker.billing.Feature
import com.example.menotracker.data.SupabaseClient
import com.example.menotracker.data.models.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Repository for Meditation Sessions
 * Integrates with Supabase meditation_sessions table
 * Handles tier-based access (FREE: 2 meditations, PREMIUM: full library)
 */
object MeditationRepository {
    private const val TAG = "MeditationRepository"
    private const val TABLE_NAME = "meditation_sessions"

    private val supabase get() = SupabaseClient.client

    // ═══════════════════════════════════════════════════════════════
    // LOCAL STATE
    // ═══════════════════════════════════════════════════════════════

    private val _recentSessions = MutableStateFlow<List<MeditationSession>>(emptyList())
    val recentSessions: StateFlow<List<MeditationSession>> = _recentSessions.asStateFlow()

    private val _totalStats = MutableStateFlow<MeditationStats?>(null)
    val totalStats: StateFlow<MeditationStats?> = _totalStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // TIER ACCESS HELPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get available meditations based on tier
     */
    fun getAvailableMeditations(): List<MeditationType> {
        val hasFullAccess = SubscriptionManager.hasAccess(Feature.MEDITATION_LIBRARY)

        return if (hasFullAccess) {
            MeditationType.entries
        } else {
            MeditationType.entries.filter { it.isFree }
        }
    }

    /**
     * Check if a specific meditation is accessible
     */
    fun canAccessMeditation(meditationType: MeditationType): Boolean {
        return SubscriptionManager.canAccessMeditation(meditationType.isFree)
    }

    /**
     * Get locked meditations (for showing upgrade prompt)
     */
    fun getLockedMeditations(): List<MeditationType> {
        val hasFullAccess = SubscriptionManager.hasAccess(Feature.MEDITATION_LIBRARY)

        return if (hasFullAccess) {
            emptyList()
        } else {
            MeditationType.entries.filter { !it.isFree }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // LOG SESSION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Log a completed meditation session
     */
    suspend fun logSession(
        userId: String,
        meditationType: MeditationType,
        durationSeconds: Int,
        soundsUsed: List<AmbientSound> = emptyList(),
        musicUsed: BackgroundMusic? = null,
        moodBefore: Int? = null,
        moodAfter: Int? = null
    ): Result<MeditationSession> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            Log.d(TAG, "Logging meditation session: ${meditationType.displayName}, duration: ${durationSeconds}s")

            // Serialize sounds list to JSON
            val soundsJson = if (soundsUsed.isNotEmpty()) {
                Json.encodeToString(ListSerializer(String.serializer()), soundsUsed.map { it.name })
            } else null

            val newSession = MeditationSessionInsert(
                user_id = userId,
                meditation_type = meditationType.name,
                duration_seconds = durationSeconds,
                sounds_used = soundsJson,
                music_used = musicUsed?.name,
                mood_before = moodBefore,
                mood_after = moodAfter,
                completed_at = now
            )

            supabase.postgrest[TABLE_NAME].insert(newSession)

            // Create local session object
            val createdSession = MeditationSession(
                userId = userId,
                meditationType = meditationType,
                durationSeconds = durationSeconds,
                soundsUsed = soundsUsed,
                musicUsed = musicUsed,
                moodBefore = moodBefore,
                moodAfter = moodAfter
            )

            // Update local state
            _recentSessions.value = listOf(createdSession) + _recentSessions.value

            Log.d(TAG, "Meditation session logged: ${createdSession.id}")
            Result.success(createdSession)

        } catch (e: Exception) {
            Log.e(TAG, "Error logging meditation session", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GET SESSIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get recent meditation sessions
     */
    suspend fun getRecentSessions(
        userId: String,
        limit: Int = 10
    ): Result<List<MeditationSession>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching recent meditation sessions")

            // Raw database response type
            @kotlinx.serialization.Serializable
            data class MeditationSessionRow(
                val id: String,
                val user_id: String,
                val meditation_type: String,
                val duration_seconds: Int,
                val sounds_used: String? = null,
                val music_used: String? = null,
                val mood_before: Int? = null,
                val mood_after: Int? = null,
                val completed_at: String,
                val created_at: String? = null
            )

            val rows = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<MeditationSessionRow>()

            val sessions = rows.mapNotNull { row ->
                try {
                    val meditationType = MeditationType.valueOf(row.meditation_type)

                    // Parse sounds from JSON
                    val sounds = row.sounds_used?.let { json ->
                        try {
                            val names = Json.decodeFromString<List<String>>(json)
                            names.mapNotNull { name ->
                                try { AmbientSound.valueOf(name) } catch (e: Exception) { null }
                            }
                        } catch (e: Exception) { emptyList() }
                    } ?: emptyList()

                    // Parse music
                    val music = row.music_used?.let { name ->
                        try { BackgroundMusic.valueOf(name) } catch (e: Exception) { null }
                    }

                    MeditationSession(
                        id = row.id,
                        userId = row.user_id,
                        meditationType = meditationType,
                        durationSeconds = row.duration_seconds,
                        soundsUsed = sounds,
                        musicUsed = music,
                        moodBefore = row.mood_before,
                        moodAfter = row.mood_after
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse session row: ${e.message}")
                    null
                }
            }
                .sortedByDescending { it.completedAt }
                .take(limit)

            _recentSessions.value = sessions

            Log.d(TAG, "Found ${sessions.size} recent meditation sessions")
            Result.success(sessions)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching recent meditation sessions", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get all-time meditation statistics
     */
    suspend fun getTotalStats(userId: String): Result<MeditationStats> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Calculating total meditation stats")

            @kotlinx.serialization.Serializable
            data class MeditationSessionRow(
                val id: String,
                val user_id: String,
                val meditation_type: String,
                val duration_seconds: Int,
                val mood_before: Int? = null,
                val mood_after: Int? = null,
                val completed_at: String
            )

            val rows = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<MeditationSessionRow>()

            if (rows.isEmpty()) {
                val emptyStats = MeditationStats()
                _totalStats.value = emptyStats
                return@withContext Result.success(emptyStats)
            }

            val totalMinutes = rows.sumOf { it.duration_seconds } / 60

            // Favorite meditation type
            val favorite = rows
                .mapNotNull { try { MeditationType.valueOf(it.meditation_type) } catch (e: Exception) { null } }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key

            // Average mood improvement
            val improvements = rows.mapNotNull { row ->
                val before = row.mood_before
                val after = row.mood_after
                if (before != null && after != null) after - before else null
            }
            val avgImprovement = if (improvements.isNotEmpty()) improvements.average().toFloat() else 0f

            // Weekly/monthly counts
            val weekStart = getWeekStart()
            val monthStart = getMonthStart()
            val weeklyCount = rows.count { it.completed_at >= weekStart }
            val monthlyCount = rows.count { it.completed_at >= monthStart }

            // Streak calculation
            val streak = calculateStreak(rows.map { it.completed_at })

            val stats = MeditationStats(
                totalSessions = rows.size,
                totalMinutes = totalMinutes,
                currentStreak = streak,
                longestStreak = streak, // Simplified for now
                averageSessionMinutes = if (rows.isNotEmpty()) totalMinutes.toFloat() / rows.size else 0f,
                favoriteType = favorite,
                averageMoodImprovement = avgImprovement,
                sessionsThisWeek = weeklyCount,
                sessionsThisMonth = monthlyCount
            )

            _totalStats.value = stats

            Log.d(TAG, "Calculated meditation stats: ${rows.size} sessions, $totalMinutes minutes")
            Result.success(stats)

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating meditation stats", e)
            Result.failure(e)
        }
    }

    private fun getWeekStart(): String {
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return "${weekStart}T00:00:00"
    }

    private fun getMonthStart(): String {
        val today = LocalDate.now()
        val monthStart = today.with(TemporalAdjusters.firstDayOfMonth())
        return "${monthStart}T00:00:00"
    }

    private fun calculateStreak(completedDates: List<String>): Int {
        if (completedDates.isEmpty()) return 0

        val today = LocalDate.now()
        val sessionDates = completedDates
            .map { it.substring(0, 10) }
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

    // ═══════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════

    /**
     * Clear state (e.g., on logout)
     */
    fun clearState() {
        _recentSessions.value = emptyList()
        _totalStats.value = null
        _isLoading.value = false
    }

    /**
     * Refresh all data
     */
    suspend fun refreshAll(userId: String) {
        getRecentSessions(userId)
        getTotalStats(userId)
    }
}

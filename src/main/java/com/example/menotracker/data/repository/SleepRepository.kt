package com.example.menotracker.data.repository

import android.util.Log
import com.example.menotracker.data.SupabaseClient
import com.example.menotracker.data.models.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Repository für Schlaf-Tracking
 * Integriert mit Supabase sleep_logs Tabelle
 */
object SleepRepository {
    private const val TAG = "SleepRepository"
    private const val TABLE_NAME = "sleep_logs"

    private val supabase get() = SupabaseClient.client

    // ═══════════════════════════════════════════════════════════════
    // LOCAL STATE
    // ═══════════════════════════════════════════════════════════════

    private val _todaySleep = MutableStateFlow<SleepLog?>(null)
    val todaySleep: StateFlow<SleepLog?> = _todaySleep.asStateFlow()

    private val _weeklyLogs = MutableStateFlow<List<SleepLog>>(emptyList())
    val weeklyLogs: StateFlow<List<SleepLog>> = _weeklyLogs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Empfohlene Schlafstunden für Frauen 40+
    const val RECOMMENDED_SLEEP_HOURS = 7.5f

    // ═══════════════════════════════════════════════════════════════
    // LOG SLEEP
    // ═══════════════════════════════════════════════════════════════

    /**
     * Schlaf für ein bestimmtes Datum loggen
     */
    suspend fun logSleep(
        userId: String,
        date: LocalDate = LocalDate.now().minusDays(1), // Default: letzte Nacht
        bedTime: LocalTime? = null,
        wakeTime: LocalTime? = null,
        qualityRating: Int? = null,
        interruptions: Int = 0,
        interruptionReasons: List<SleepInterruptionReason>? = null,
        notes: String? = null
    ): Result<SleepLog> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

            Log.d(TAG, "📝 Logging sleep for $dateStr")

            // Calculate total hours if times provided
            val totalHours = if (bedTime != null && wakeTime != null) {
                calculateSleepHours(bedTime, wakeTime)
            } else null

            val newLog = SleepLogInsert(
                user_id = userId,
                date = dateStr,
                bed_time = bedTime?.format(DateTimeFormatter.ISO_LOCAL_TIME),
                wake_time = wakeTime?.format(DateTimeFormatter.ISO_LOCAL_TIME),
                total_hours = totalHours,
                quality_rating = qualityRating?.coerceIn(1, 5),
                interruptions = interruptions,
                interruption_reasons = interruptionReasons?.map { it.name },
                notes = notes
            )

            // Upsert: Update if exists, insert if not
            val existingLog = getLogForDate(userId, date).getOrNull()

            val createdLog = if (existingLog != null) {
                // Update existing
                supabase.postgrest[TABLE_NAME]
                    .update({
                        bedTime?.let { set("bed_time", it.format(DateTimeFormatter.ISO_LOCAL_TIME)) }
                        wakeTime?.let { set("wake_time", it.format(DateTimeFormatter.ISO_LOCAL_TIME)) }
                        totalHours?.let { set("total_hours", it) }
                        qualityRating?.let { set("quality_rating", it.coerceIn(1, 5)) }
                        set("interruptions", interruptions)
                        interruptionReasons?.let { set("interruption_reasons", it.map { r -> r.name }) }
                        notes?.let { set("notes", it) }
                    }) {
                        filter {
                            eq("id", existingLog.id)
                        }
                    }

                getLogForDate(userId, date).getOrThrow()!!
            } else {
                // Insert new
                supabase.postgrest[TABLE_NAME]
                    .insert(newLog)
                    .decodeSingle<SleepLog>()
            }

            // Update local state
            if (date == LocalDate.now().minusDays(1) || date == LocalDate.now()) {
                _todaySleep.value = createdLog
            }

            Log.d(TAG, "✅ Sleep logged successfully")
            Result.success(createdLog)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error logging sleep", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Schnelles Schlaf-Logging nur mit Qualität
     */
    suspend fun quickLogQuality(
        userId: String,
        qualityRating: Int,
        date: LocalDate = LocalDate.now().minusDays(1)
    ): Result<SleepLog> {
        return logSleep(userId, date, qualityRating = qualityRating)
    }

    // ═══════════════════════════════════════════════════════════════
    // GET SLEEP LOGS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Schlaf-Log für ein bestimmtes Datum
     */
    suspend fun getLogForDate(
        userId: String,
        date: LocalDate
    ): Result<SleepLog?> = withContext(Dispatchers.IO) {
        try {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

            val logs = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("date", dateStr)
                    }
                }
                .decodeList<SleepLog>()

            Result.success(logs.firstOrNull())

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching sleep log for date", e)
            Result.failure(e)
        }
    }

    /**
     * Letzte Nacht's Schlaf (gestern)
     */
    suspend fun getLastNightSleep(userId: String): Result<SleepLog?> {
        val lastNight = LocalDate.now().minusDays(1)
        return getLogForDate(userId, lastNight).also { result ->
            result.getOrNull()?.let { _todaySleep.value = it }
        }
    }

    /**
     * Schlaf-Historie der letzten N Tage
     */
    suspend fun getSleepHistory(
        userId: String,
        days: Int = 7
    ): Result<List<SleepLog>> = withContext(Dispatchers.IO) {
        try {
            val startDate = LocalDate.now().minusDays(days.toLong())
                .format(DateTimeFormatter.ISO_LOCAL_DATE)

            Log.d(TAG, "📅 Fetching sleep history since $startDate")

            val logs = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("date", startDate)
                    }
                }
                .decodeList<SleepLog>()

            _weeklyLogs.value = logs.sortedByDescending { it.date }

            Log.d(TAG, "✅ Found ${logs.size} sleep logs")
            Result.success(logs.sortedByDescending { it.date })

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching sleep history", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Wöchentliche Schlaf-Statistiken
     */
    suspend fun getWeeklyStats(userId: String): Result<WeeklySleepSummary> = withContext(Dispatchers.IO) {
        try {
            val historyResult = getSleepHistory(userId, 7)
            if (historyResult.isFailure) {
                return@withContext Result.failure(historyResult.exceptionOrNull()!!)
            }

            val logs = historyResult.getOrThrow()

            if (logs.isEmpty()) {
                return@withContext Result.success(
                    WeeklySleepSummary(
                        averageHours = 0f,
                        averageQuality = 0f,
                        totalInterruptions = 0,
                        daysLogged = 0,
                        mostCommonInterruption = null
                    )
                )
            }

            // Calculate averages
            val avgHours = logs.mapNotNull { it.totalHours }.average().toFloat()
            val avgQuality = logs.mapNotNull { it.qualityRating }.average().toFloat()
            val totalInterruptions = logs.sumOf { it.interruptions }

            // Find most common interruption
            val allReasons = logs.flatMap { it.interruptionReasons ?: emptyList() }
            val mostCommon = allReasons
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
                ?.let { reason ->
                    try {
                        SleepInterruptionReason.valueOf(reason)
                    } catch (e: Exception) {
                        null
                    }
                }

            val summary = WeeklySleepSummary(
                averageHours = avgHours,
                averageQuality = avgQuality,
                totalInterruptions = totalInterruptions,
                daysLogged = logs.size,
                mostCommonInterruption = mostCommon
            )

            Log.d(TAG, "✅ Weekly stats: ${avgHours}h avg, ${avgQuality} quality")
            Result.success(summary)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error calculating weekly stats", e)
            Result.failure(e)
        }
    }

    /**
     * Schlaf-Score berechnen (0-100)
     */
    fun calculateSleepScore(log: SleepLog): Int {
        var score = 50 // Base score

        // Hours component (max 30 points)
        log.totalHours?.let { hours ->
            score += when {
                hours >= 7f && hours <= 9f -> 30 // Optimal range
                hours >= 6f && hours < 7f -> 20
                hours > 9f && hours <= 10f -> 20
                hours >= 5f && hours < 6f -> 10
                else -> 0
            }
        }

        // Quality component (max 25 points)
        log.qualityRating?.let { quality ->
            score += (quality - 1) * 6 // 0, 6, 12, 18, 24
        }

        // Interruptions penalty (max -20 points)
        score -= (log.interruptions * 5).coerceAtMost(20)

        return score.coerceIn(0, 100)
    }

    // ═══════════════════════════════════════════════════════════════
    // LOG INTERRUPTION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Nächtliche Unterbrechung loggen
     */
    suspend fun logInterruption(
        userId: String,
        reason: SleepInterruptionReason,
        date: LocalDate = LocalDate.now()
    ): Result<SleepLog> = withContext(Dispatchers.IO) {
        try {
            val existingLog = getLogForDate(userId, date).getOrNull()

            if (existingLog != null) {
                // Update existing log
                val currentReasons = existingLog.interruptionReasons?.toMutableList() ?: mutableListOf()
                currentReasons.add(reason.name)

                supabase.postgrest[TABLE_NAME]
                    .update({
                        set("interruptions", existingLog.interruptions + 1)
                        set("interruption_reasons", currentReasons)
                    }) {
                        filter {
                            eq("id", existingLog.id)
                        }
                    }

                val updated = getLogForDate(userId, date).getOrThrow()!!
                _todaySleep.value = updated
                Result.success(updated)

            } else {
                // Create new log with interruption
                logSleep(
                    userId = userId,
                    date = date,
                    interruptions = 1,
                    interruptionReasons = listOf(reason)
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error logging interruption", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Schlaf-Eintrag löschen
     */
    suspend fun deleteSleepLog(
        userId: String,
        logId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.postgrest[TABLE_NAME]
                .delete {
                    filter {
                        eq("id", logId)
                        eq("user_id", userId)
                    }
                }

            _weeklyLogs.value = _weeklyLogs.value.filter { it.id != logId }
            if (_todaySleep.value?.id == logId) {
                _todaySleep.value = null
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting sleep log", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════

    /**
     * Schlafstunden berechnen (berücksichtigt Mitternacht)
     */
    private fun calculateSleepHours(bedTime: LocalTime, wakeTime: LocalTime): Float {
        val bedMinutes = bedTime.hour * 60 + bedTime.minute
        var wakeMinutes = wakeTime.hour * 60 + wakeTime.minute

        // If wake time is before bed time, assume next day
        if (wakeMinutes < bedMinutes) {
            wakeMinutes += 24 * 60
        }

        return (wakeMinutes - bedMinutes) / 60f
    }

    /**
     * State zurücksetzen
     */
    fun clearState() {
        _todaySleep.value = null
        _weeklyLogs.value = emptyList()
        _isLoading.value = false
    }

    /**
     * Alle Daten neu laden
     */
    suspend fun refreshAll(userId: String) {
        getLastNightSleep(userId)
        getSleepHistory(userId, 7)
    }
}

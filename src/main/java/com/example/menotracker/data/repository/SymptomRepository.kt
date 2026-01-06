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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Repository für Menopause-Symptom-Tracking
 * Integriert mit Supabase symptom_logs Tabelle
 */
object SymptomRepository {
    private const val TAG = "SymptomRepository"
    private const val TABLE_NAME = "symptom_logs"

    private val supabase get() = SupabaseClient.client

    // ═══════════════════════════════════════════════════════════════
    // LOCAL STATE
    // ═══════════════════════════════════════════════════════════════

    private val _todaySymptoms = MutableStateFlow<List<SymptomLog>>(emptyList())
    val todaySymptoms: StateFlow<List<SymptomLog>> = _todaySymptoms.asStateFlow()

    private val _weeklyStats = MutableStateFlow<Map<MenopauseSymptomType, SymptomStats>>(emptyMap())
    val weeklyStats: StateFlow<Map<MenopauseSymptomType, SymptomStats>> = _weeklyStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Häufige Trigger für Quick-Select
    val commonTriggers = listOf(
        "Stress", "Koffein", "Alkohol", "Scharfes Essen",
        "Warme Umgebung", "Sport", "Schlafmangel", "Hormonelle Veränderung"
    )

    // ═══════════════════════════════════════════════════════════════
    // LOG SYMPTOM
    // ═══════════════════════════════════════════════════════════════

    /**
     * Neues Symptom loggen
     */
    suspend fun logSymptom(
        userId: String,
        symptomType: MenopauseSymptomType,
        intensity: Int,
        durationMinutes: Int? = null,
        triggers: List<String>? = null,
        notes: String? = null
    ): Result<SymptomLog> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            Log.d(TAG, "📝 Logging symptom: ${symptomType.name}, intensity: $intensity")

            val newLog = SymptomLogInsert(
                user_id = userId,
                symptom_type = symptomType.name,
                intensity = intensity.coerceIn(1, 10),
                duration_minutes = durationMinutes,
                triggers = triggers,
                notes = notes,
                logged_at = now
            )

            val createdLog = supabase.postgrest[TABLE_NAME]
                .insert(newLog)
                .decodeSingle<SymptomLog>()

            // Update local state
            _todaySymptoms.value = _todaySymptoms.value + createdLog

            Log.d(TAG, "✅ Symptom logged successfully: ${createdLog.id}")
            Result.success(createdLog)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error logging symptom", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Schnelles Loggen ohne Details
     */
    suspend fun quickLogSymptom(
        userId: String,
        symptomType: MenopauseSymptomType,
        intensity: Int
    ): Result<SymptomLog> {
        return logSymptom(userId, symptomType, intensity)
    }

    // ═══════════════════════════════════════════════════════════════
    // GET SYMPTOMS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Heutige Symptome laden
     */
    suspend fun getTodaySymptoms(userId: String): Result<List<SymptomLog>> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

            Log.d(TAG, "📊 Fetching today's symptoms for $today")

            val symptoms = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("logged_at", "${today}T00:00:00")
                        lte("logged_at", "${today}T23:59:59")
                    }
                }
                .decodeList<SymptomLog>()

            _todaySymptoms.value = symptoms.sortedByDescending { it.loggedAt }

            Log.d(TAG, "✅ Found ${symptoms.size} symptoms today")
            Result.success(symptoms)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching today's symptoms", e)
            Result.failure(e)
        }
    }

    /**
     * Symptome der letzten N Tage laden
     */
    suspend fun getSymptomHistory(
        userId: String,
        days: Int = 7
    ): Result<List<SymptomLog>> = withContext(Dispatchers.IO) {
        try {
            val startDate = LocalDate.now().minusDays(days.toLong())
                .format(DateTimeFormatter.ISO_LOCAL_DATE)

            Log.d(TAG, "📅 Fetching symptom history since $startDate")

            val symptoms = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("logged_at", "${startDate}T00:00:00")
                    }
                }
                .decodeList<SymptomLog>()

            Log.d(TAG, "✅ Found ${symptoms.size} symptoms in last $days days")
            Result.success(symptoms.sortedByDescending { it.loggedAt })

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching symptom history", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Wöchentliche Symptom-Statistiken berechnen
     */
    suspend fun getWeeklyStats(userId: String): Result<Map<MenopauseSymptomType, SymptomStats>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📈 Calculating weekly symptom stats")

            val historyResult = getSymptomHistory(userId, 7)
            if (historyResult.isFailure) {
                return@withContext Result.failure(historyResult.exceptionOrNull()!!)
            }

            val symptoms = historyResult.getOrThrow()

            val stats = symptoms
                .mapNotNull { log ->
                    log.symptomEnum?.let { type -> type to log }
                }
                .groupBy { it.first }
                .mapValues { (type, entries) ->
                    val logs = entries.map { it.second }
                    SymptomStats(
                        symptomType = type,
                        occurrenceCount = logs.size,
                        avgIntensity = logs.map { it.intensity }.average().toFloat(),
                        maxIntensity = logs.maxOfOrNull { it.intensity } ?: 0,
                        trend = SymptomTrend.STABLE // TODO: Compare with previous week
                    )
                }

            _weeklyStats.value = stats

            Log.d(TAG, "✅ Calculated stats for ${stats.size} symptom types")
            Result.success(stats)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error calculating weekly stats", e)
            Result.failure(e)
        }
    }

    /**
     * Häufigstes Symptom ermitteln
     */
    suspend fun getMostFrequentSymptom(userId: String): Result<MenopauseSymptomType?> {
        return getWeeklyStats(userId).map { stats ->
            stats.maxByOrNull { it.value.occurrenceCount }?.key
        }
    }

    /**
     * Durchschnittliche Intensität der Woche
     */
    suspend fun getAverageIntensity(userId: String): Result<Float> {
        return getSymptomHistory(userId, 7).map { symptoms ->
            if (symptoms.isEmpty()) 0f
            else symptoms.map { it.intensity }.average().toFloat()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE / DELETE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Symptom-Eintrag löschen
     */
    suspend fun deleteSymptom(userId: String, symptomId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🗑️ Deleting symptom: $symptomId")

            supabase.postgrest[TABLE_NAME]
                .delete {
                    filter {
                        eq("id", symptomId)
                        eq("user_id", userId)
                    }
                }

            // Update local state
            _todaySymptoms.value = _todaySymptoms.value.filter { it.id != symptomId }

            Log.d(TAG, "✅ Symptom deleted")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting symptom", e)
            Result.failure(e)
        }
    }

    /**
     * Symptom-Intensität aktualisieren
     */
    suspend fun updateIntensity(
        userId: String,
        symptomId: String,
        newIntensity: Int
    ): Result<SymptomLog> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "✏️ Updating symptom intensity to $newIntensity")

            supabase.postgrest[TABLE_NAME]
                .update({
                    set("intensity", newIntensity.coerceIn(1, 10))
                }) {
                    filter {
                        eq("id", symptomId)
                        eq("user_id", userId)
                    }
                }

            // Fetch updated log
            val updatedLogs = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("id", symptomId)
                    }
                }
                .decodeList<SymptomLog>()

            val updatedLog = updatedLogs.firstOrNull()
                ?: return@withContext Result.failure(Exception("Symptom not found after update"))

            // Update local state
            _todaySymptoms.value = _todaySymptoms.value.map {
                if (it.id == symptomId) updatedLog else it
            }

            Log.d(TAG, "✅ Symptom intensity updated")
            Result.success(updatedLog)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating symptom intensity", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TRIGGER ANALYSIS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Häufigste Trigger für ein Symptom ermitteln
     */
    suspend fun getTopTriggers(
        userId: String,
        symptomType: MenopauseSymptomType,
        limit: Int = 5
    ): Result<List<Pair<String, Int>>> = withContext(Dispatchers.IO) {
        try {
            val history = getSymptomHistory(userId, 30).getOrThrow()

            val triggerCounts = history
                .filter { it.symptomType == symptomType.name }
                .flatMap { it.triggers ?: emptyList() }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
                .take(limit)

            Result.success(triggerCounts)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error analyzing triggers", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════

    /**
     * State zurücksetzen (z.B. bei Logout)
     */
    fun clearState() {
        _todaySymptoms.value = emptyList()
        _weeklyStats.value = emptyMap()
        _isLoading.value = false
    }

    /**
     * Alle Daten neu laden
     */
    suspend fun refreshAll(userId: String) {
        getTodaySymptoms(userId)
        getWeeklyStats(userId)
    }
}

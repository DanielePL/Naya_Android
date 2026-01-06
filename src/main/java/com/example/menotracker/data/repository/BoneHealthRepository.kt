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
import java.time.format.DateTimeFormatter

/**
 * Repository für Knochengesundheits-Tracking
 * Trackt Calcium, Vitamin D, Omega-3 und Krafttraining
 */
object BoneHealthRepository {
    private const val TAG = "BoneHealthRepository"
    private const val TABLE_NAME = "bone_health_logs"

    private val supabase get() = SupabaseClient.client

    // ═══════════════════════════════════════════════════════════════
    // LOCAL STATE
    // ═══════════════════════════════════════════════════════════════

    private val _todayLog = MutableStateFlow<BoneHealthLog?>(null)
    val todayLog: StateFlow<BoneHealthLog?> = _todayLog.asStateFlow()

    private val _weeklyLogs = MutableStateFlow<List<BoneHealthLog>>(emptyList())
    val weeklyLogs: StateFlow<List<BoneHealthLog>> = _weeklyLogs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // DAILY GOALS (for women 40+)
    // ═══════════════════════════════════════════════════════════════

    object DailyGoals {
        const val CALCIUM_MG = 1200f
        const val VITAMIN_D_IU = 800f
        const val OMEGA3_MG = 1000f
        const val MAGNESIUM_MG = 320f
        const val STRENGTH_TRAINING_DAYS_PER_WEEK = 2
    }

    // Lebensmittel mit hohem Calcium-Gehalt (für Empfehlungen)
    val highCalciumFoods = listOf(
        "Milch (250ml)" to 300f,
        "Joghurt (150g)" to 180f,
        "Käse (30g)" to 220f,
        "Sardinen (100g)" to 382f,
        "Brokkoli (100g)" to 47f,
        "Mandeln (30g)" to 75f,
        "Tofu (100g)" to 350f,
        "Grünkohl (100g)" to 150f
    )

    // ═══════════════════════════════════════════════════════════════
    // LOG NUTRIENTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Heutigen Knochengesundheits-Log erstellen oder aktualisieren
     */
    suspend fun logNutrients(
        userId: String,
        date: LocalDate = LocalDate.now(),
        calciumMg: Float? = null,
        vitaminDIu: Float? = null,
        omega3Mg: Float? = null,
        magnesiumMg: Float? = null,
        strengthTrainingDone: Boolean? = null,
        weightBearingMinutes: Int? = null,
        notes: String? = null
    ): Result<BoneHealthLog> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

            Log.d(TAG, "📝 Logging bone health for $dateStr")

            val existingLog = getLogForDate(userId, date).getOrNull()

            val log = if (existingLog != null) {
                // Update existing log
                supabase.postgrest[TABLE_NAME]
                    .update({
                        calciumMg?.let { set("calcium_mg", existingLog.calciumMg + it) }
                        vitaminDIu?.let { set("vitamin_d_iu", existingLog.vitaminDIu + it) }
                        omega3Mg?.let { set("omega3_mg", existingLog.omega3Mg + it) }
                        magnesiumMg?.let { set("magnesium_mg", existingLog.magnesiumMg + it) }
                        strengthTrainingDone?.let { set("strength_training_done", it) }
                        weightBearingMinutes?.let {
                            set("weight_bearing_minutes", existingLog.weightBearingMinutes + it)
                        }
                        notes?.let { set("notes", it) }
                    }) {
                        filter {
                            eq("id", existingLog.id)
                        }
                    }

                getLogForDate(userId, date).getOrThrow()!!
            } else {
                // Create new log
                val newLog = BoneHealthLogInsert(
                    user_id = userId,
                    date = dateStr,
                    calcium_mg = calciumMg ?: 0f,
                    vitamin_d_iu = vitaminDIu ?: 0f,
                    omega3_mg = omega3Mg ?: 0f,
                    magnesium_mg = magnesiumMg ?: 0f,
                    strength_training_done = strengthTrainingDone ?: false,
                    weight_bearing_minutes = weightBearingMinutes ?: 0,
                    notes = notes
                )

                supabase.postgrest[TABLE_NAME]
                    .insert(newLog)
                    .decodeSingle<BoneHealthLog>()
            }

            if (date == LocalDate.now()) {
                _todayLog.value = log
            }

            Log.d(TAG, "✅ Bone health logged: ${log.calciumMg}mg Ca, ${log.vitaminDIu} IU VitD")
            Result.success(log)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error logging bone health", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Calcium hinzufügen
     */
    suspend fun addCalcium(userId: String, amountMg: Float): Result<BoneHealthLog> {
        return logNutrients(userId, calciumMg = amountMg)
    }

    /**
     * Vitamin D hinzufügen
     */
    suspend fun addVitaminD(userId: String, amountIu: Float): Result<BoneHealthLog> {
        return logNutrients(userId, vitaminDIu = amountIu)
    }

    /**
     * Krafttraining markieren
     */
    suspend fun logStrengthTraining(
        userId: String,
        durationMinutes: Int = 0
    ): Result<BoneHealthLog> {
        return logNutrients(
            userId,
            strengthTrainingDone = true,
            weightBearingMinutes = durationMinutes
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // GET LOGS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Log für ein bestimmtes Datum
     */
    suspend fun getLogForDate(
        userId: String,
        date: LocalDate
    ): Result<BoneHealthLog?> = withContext(Dispatchers.IO) {
        try {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

            val logs = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("date", dateStr)
                    }
                }
                .decodeList<BoneHealthLog>()

            Result.success(logs.firstOrNull())

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching bone health log", e)
            Result.failure(e)
        }
    }

    /**
     * Heutigen Log laden
     */
    suspend fun getTodayLog(userId: String): Result<BoneHealthLog?> {
        return getLogForDate(userId, LocalDate.now()).also { result ->
            result.getOrNull()?.let { _todayLog.value = it }
        }
    }

    /**
     * Historie der letzten N Tage
     */
    suspend fun getHistory(
        userId: String,
        days: Int = 7
    ): Result<List<BoneHealthLog>> = withContext(Dispatchers.IO) {
        try {
            val startDate = LocalDate.now().minusDays(days.toLong())
                .format(DateTimeFormatter.ISO_LOCAL_DATE)

            Log.d(TAG, "📅 Fetching bone health history since $startDate")

            val logs = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("date", startDate)
                    }
                }
                .decodeList<BoneHealthLog>()

            _weeklyLogs.value = logs.sortedByDescending { it.date }

            Log.d(TAG, "✅ Found ${logs.size} bone health logs")
            Result.success(logs.sortedByDescending { it.date })

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching bone health history", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Wöchentliche Statistiken
     */
    suspend fun getWeeklyStats(userId: String): Result<WeeklyBoneHealthSummary> = withContext(Dispatchers.IO) {
        try {
            val historyResult = getHistory(userId, 7)
            if (historyResult.isFailure) {
                return@withContext Result.failure(historyResult.exceptionOrNull()!!)
            }

            val logs = historyResult.getOrThrow()

            if (logs.isEmpty()) {
                return@withContext Result.success(
                    WeeklyBoneHealthSummary(
                        averageCalcium = 0f,
                        averageVitaminD = 0f,
                        averageOmega3 = 0f,
                        strengthTrainingDays = 0,
                        daysLogged = 0,
                        calciumGoalMet = 0,
                        vitaminDGoalMet = 0
                    )
                )
            }

            val summary = WeeklyBoneHealthSummary(
                averageCalcium = logs.map { it.calciumMg }.average().toFloat(),
                averageVitaminD = logs.map { it.vitaminDIu }.average().toFloat(),
                averageOmega3 = logs.map { it.omega3Mg }.average().toFloat(),
                strengthTrainingDays = logs.count { it.strengthTrainingDone },
                daysLogged = logs.size,
                calciumGoalMet = logs.count { it.calciumMg >= DailyGoals.CALCIUM_MG },
                vitaminDGoalMet = logs.count { it.vitaminDIu >= DailyGoals.VITAMIN_D_IU }
            )

            Log.d(TAG, "✅ Weekly stats: ${summary.averageCalcium}mg Ca avg")
            Result.success(summary)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error calculating weekly stats", e)
            Result.failure(e)
        }
    }

    /**
     * Knochengesundheits-Score berechnen (0-100)
     */
    fun calculateBoneHealthScore(log: BoneHealthLog): Int {
        var score = 0

        // Calcium (max 30 points)
        score += ((log.calciumMg / DailyGoals.CALCIUM_MG) * 30).toInt().coerceAtMost(30)

        // Vitamin D (max 25 points)
        score += ((log.vitaminDIu / DailyGoals.VITAMIN_D_IU) * 25).toInt().coerceAtMost(25)

        // Omega-3 (max 15 points)
        score += ((log.omega3Mg / DailyGoals.OMEGA3_MG) * 15).toInt().coerceAtMost(15)

        // Strength training (max 20 points)
        if (log.strengthTrainingDone) {
            score += 20
        }

        // Weight-bearing exercise (max 10 points)
        score += (log.weightBearingMinutes / 3).coerceAtMost(10)

        return score.coerceIn(0, 100)
    }

    /**
     * Empfehlungen basierend auf dem heutigen Log
     */
    fun getRecommendations(log: BoneHealthLog?): List<String> {
        val recommendations = mutableListOf<String>()

        if (log == null) {
            recommendations.add("Starte heute mit dem Tracking deiner Knochengesundheit!")
            return recommendations
        }

        // Calcium
        val calciumRemaining = DailyGoals.CALCIUM_MG - log.calciumMg
        if (calciumRemaining > 200) {
            recommendations.add(
                "Du brauchst noch ${calciumRemaining.toInt()}mg Calcium. " +
                "Ein Glas Milch (300mg) oder Joghurt (180mg) würde helfen."
            )
        }

        // Vitamin D
        val vitDRemaining = DailyGoals.VITAMIN_D_IU - log.vitaminDIu
        if (vitDRemaining > 200) {
            recommendations.add(
                "Noch ${vitDRemaining.toInt()} IE Vitamin D empfohlen. " +
                "15 Minuten Sonnenlicht oder ein Supplement helfen."
            )
        }

        // Strength training
        if (!log.strengthTrainingDone) {
            recommendations.add(
                "Krafttraining ist wichtig für die Knochengesundheit. " +
                "Schon 20 Minuten mit Gewichten machen einen Unterschied!"
            )
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Super! Du hast heute deine Knochengesundheits-Ziele erreicht!")
        }

        return recommendations
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════

    /**
     * State zurücksetzen
     */
    fun clearState() {
        _todayLog.value = null
        _weeklyLogs.value = emptyList()
        _isLoading.value = false
    }

    /**
     * Alle Daten neu laden
     */
    suspend fun refreshAll(userId: String) {
        getTodayLog(userId)
        getHistory(userId, 7)
    }
}

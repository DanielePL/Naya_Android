package com.example.menotracker.data

import android.content.Context
import android.util.Log
import com.example.menotracker.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * ILB (Individuelles Leistungsbild) Service
 *
 * Verwaltet periodisierte Krafttests und 1RM-Updates.
 * Ermöglicht automatische Gewichtsanpassung basierend auf AMRAP-Tests.
 *
 * Note: VBT database storage has been removed. This service now operates
 * in-memory only for the current session.
 *
 * @author Naya Team
 */
class ILBService(context: Context) {

    private val TAG = "ILBService"

    // In-memory cache for 1RM values (session-only)
    private val oneRMCache = mutableMapOf<String, Float>()

    // Cache für aktuelle Test-Session
    private val _currentTestResults = MutableStateFlow<List<ILBTestResult>>(emptyList())
    val currentTestResults = _currentTestResults.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // 1RM RETRIEVAL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Holt den aktuellen 1RM für eine Übung (from in-memory cache)
     */
    suspend fun getExercise1RM(
        userId: String,
        exerciseId: String
    ): Float? = withContext(Dispatchers.IO) {
        oneRMCache[exerciseId]
    }

    /**
     * Holt alle 1RM PRs für einen User (from in-memory cache)
     */
    suspend fun getAllExercise1RMs(userId: String): Map<String, Float> = withContext(Dispatchers.IO) {
        oneRMCache.toMap()
    }

    /**
     * Holt die letzten ILB Test-Ergebnisse für eine Übung
     * Note: Returns empty list as history is no longer persisted
     */
    suspend fun getExerciseTestHistory(
        userId: String,
        exerciseId: String
    ): List<ILBTestResult> = withContext(Dispatchers.IO) {
        _currentTestResults.value.filter { it.exerciseId == exerciseId }
    }

    // ═══════════════════════════════════════════════════════════════
    // AMRAP PROCESSING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verarbeitet ein AMRAP-Ergebnis und speichert den neuen 1RM
     *
     * @param userId User ID
     * @param exerciseId Exercise ID
     * @param exerciseName Exercise name for display
     * @param testWeight Gewicht beim AMRAP Test
     * @param amrapReps Anzahl geschaffter Wiederholungen
     * @return ILBTestResult mit neuem 1RM und Vergleich
     */
    suspend fun processAMRAPResult(
        userId: String,
        exerciseId: String,
        exerciseName: String,
        testWeight: Float,
        amrapReps: Int
    ): ILBTestResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "📊 Processing AMRAP: $exerciseName - ${testWeight}kg × $amrapReps reps")

        // Hole vorherigen 1RM (falls vorhanden)
        val previous1RM = getExercise1RM(userId, exerciseId)

        // Berechne neuen 1RM
        val result = ILBCalculator.processAMRAPResult(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            testWeight = testWeight,
            amrapReps = amrapReps,
            previous1RM = previous1RM
        )

        // Speichere neuen 1RM in Datenbank
        saveExercise1RM(
            userId = userId,
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            new1RM = result.new1RM,
            previous1RM = previous1RM,
            testWeight = testWeight,
            testReps = amrapReps
        )

        // Update Cache
        val currentResults = _currentTestResults.value.toMutableList()
        currentResults.add(result)
        _currentTestResults.value = currentResults

        Log.d(TAG, "✅ New 1RM saved: ${result.new1RM}kg (${result.getDisplayMessage()})")
        result
    }

    /**
     * Speichert einen neuen 1RM in der in-memory cache
     */
    private suspend fun saveExercise1RM(
        userId: String,
        exerciseId: String,
        exerciseName: String,
        new1RM: Float,
        previous1RM: Float?,
        testWeight: Float,
        testReps: Int
    ) = withContext(Dispatchers.IO) {
        // Store in memory cache
        oneRMCache[exerciseId] = new1RM
        Log.d(TAG, "💾 Cached 1RM for $exerciseName: ${new1RM}kg")
    }

    /**
     * Wählt das beste AMRAP-Ergebnis aus mehreren Versuchen
     */
    fun selectBestResult(results: List<ILBTestResult>): ILBTestResult? {
        return ILBCalculator.selectBestAMRAP(results)
    }

    // ═══════════════════════════════════════════════════════════════
    // TEST SESSION MANAGEMENT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Startet eine neue ILB Test-Session
     */
    fun startTestSession() {
        _currentTestResults.value = emptyList()
        Log.d(TAG, "🚀 ILB Test session started")
    }

    /**
     * Beendet die aktuelle Test-Session
     * @return Alle Testergebnisse dieser Session
     */
    fun endTestSession(): List<ILBTestResult> {
        val results = _currentTestResults.value
        Log.d(TAG, "🏁 ILB Test session ended with ${results.size} results")
        return results
    }

    /**
     * Holt die Ergebnisse der aktuellen Test-Session
     */
    fun getCurrentTestResults(): List<ILBTestResult> {
        return _currentTestResults.value
    }

    // ═══════════════════════════════════════════════════════════════
    // WEIGHT RECOMMENDATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Berechnet das empfohlene Arbeitsgewicht basierend auf gespeichertem 1RM
     */
    suspend fun getRecommendedWorkingWeight(
        userId: String,
        exerciseId: String,
        targetReps: Int,
        roundTo: Float = 2.5f
    ): Float? = withContext(Dispatchers.IO) {
        val oneRM = getExercise1RM(userId, exerciseId) ?: return@withContext null
        ILBCalculator.calculateNewWorkingWeight(oneRM, targetReps, roundTo)
    }

    /**
     * Erstellt eine Vorschau aller Gewichtsänderungen nach ILB Test
     */
    suspend fun getWeightChangePreview(
        userId: String,
        exerciseId: String,
        newOneRM: Float,
        repRanges: List<Int> = listOf(3, 5, 8, 10, 12)
    ): List<WeightChangePreview> = withContext(Dispatchers.IO) {
        val oldOneRM = getExercise1RM(userId, exerciseId)
        ILBCalculator.getWeightChangePreview(oldOneRM, newOneRM, repRanges)
    }

    // ═══════════════════════════════════════════════════════════════
    // TEST CONFIGURATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Generiert die ILB Test-Konfiguration für eine Übung
     */
    suspend fun generateTestConfig(
        userId: String,
        exerciseId: String,
        exerciseName: String,
        currentWorkingWeight: Float? = null
    ): ILBTestConfig = withContext(Dispatchers.IO) {
        // Versuche aktuelles Arbeitsgewicht zu ermitteln
        val testWeight = currentWorkingWeight
            ?: getRecommendedWorkingWeight(userId, exerciseId, 8) // Default: 8-Rep-Max
            ?: 20f // Fallback

        ILBCalculator.generateTestConfig(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            currentWorkingWeight = testWeight
        )
    }

    /**
     * Prüft ob eine Übung für ILB-Testing geeignet ist
     */
    fun isEligibleForILBTest(exerciseName: String): Boolean {
        return ILBCalculator.isCompoundExercise(exerciseName)
    }

    // ═══════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Holt Statistiken über ILB-Fortschritt
     * Note: Returns current session stats only (no persistence)
     */
    suspend fun getILBStatistics(userId: String): ILBStatistics = withContext(Dispatchers.IO) {
        val results = _currentTestResults.value

        val totalTests = results.size
        val exercisesTested = results.map { it.exerciseId }.distinct().size
        val improvements = results.count { (it.changePercent ?: 0f) > 0 }
        val avgImprovement = results
            .mapNotNull { it.changeKg }
            .filter { it > 0 }
            .average()
            .takeIf { !it.isNaN() }?.toFloat() ?: 0f

        ILBStatistics(
            totalTests = totalTests,
            exercisesTested = exercisesTested,
            improvements = improvements,
            avgImprovementKg = avgImprovement,
            lastTestDate = null
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: ILBService? = null

        fun getInstance(context: Context): ILBService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ILBService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

/**
 * ILB Statistiken
 */
data class ILBStatistics(
    val totalTests: Int,
    val exercisesTested: Int,
    val improvements: Int,
    val avgImprovementKg: Float,
    val lastTestDate: String?
) {
    val improvementRate: Float
        get() = if (totalTests > 0) improvements.toFloat() / totalTests else 0f
}

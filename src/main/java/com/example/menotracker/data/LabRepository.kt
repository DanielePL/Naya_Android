package com.example.menotracker.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lab Repository
 *
 * Stub implementation - VBT database storage has been removed.
 * Provides neutral score values for UI compatibility.
 *
 * @author Naya Team
 */
class LabRepository(context: Context) {

    private val TAG = "LabRepository"

    /**
     * Sport Profile Types - affects scoring weights
     */
    enum class SportProfile {
        POWERLIFTING,
        WEIGHTLIFTING,
        HYBRID
    }

    /**
     * Live VBT Scores Result
     */
    data class VBTScores(
        val powerScore: Int,
        val techniqueScore: Int,
        val readinessScore: Int?,
        val velocityVsBaseline: Float?,
        val isNewPR: Boolean,
        val fatigueIndex: Float?,
        val hasBaseline: Boolean
    )

    /**
     * Get complete VBT scores for live display
     * Returns neutral scores (no historical data available)
     */
    suspend fun getLiveVBTScores(
        sessionId: String?,
        exerciseId: String,
        weight: Float,
        currentAvgVelocity: Float,
        currentPeakVelocity: Float,
        pathAccuracy: Float?,
        pathDrift: Float?,
        velocityVariation: Float?,
        sportProfile: SportProfile = SportProfile.HYBRID
    ): VBTScores = withContext(Dispatchers.IO) {
        // Calculate technique score based on path accuracy and drift
        val techniqueScore = calculateTechniqueScore(pathAccuracy, pathDrift, velocityVariation)

        Log.d(TAG, "📊 VBT Scores (no baseline): technique=$techniqueScore")

        VBTScores(
            powerScore = 50, // Neutral score - no baseline data
            techniqueScore = techniqueScore,
            readinessScore = null,
            velocityVsBaseline = null,
            isNewPR = false,
            fatigueIndex = null,
            hasBaseline = false
        )
    }

    /**
     * Calculate technique score based on path metrics
     */
    private fun calculateTechniqueScore(
        pathAccuracy: Float?,
        pathDrift: Float?,
        velocityVariation: Float?
    ): Int {
        val pathScore = (pathAccuracy ?: 70f).coerceIn(0f, 100f)

        val driftScore = when {
            pathDrift == null -> 70f
            pathDrift <= 2f -> 100f
            pathDrift <= 5f -> 85f
            pathDrift <= 10f -> 70f
            pathDrift <= 20f -> 50f
            else -> 30f
        }

        val consistencyScore = when {
            velocityVariation == null -> 70f
            velocityVariation <= 0.05f -> 100f
            velocityVariation <= 0.10f -> 85f
            velocityVariation <= 0.15f -> 70f
            velocityVariation <= 0.25f -> 50f
            else -> 30f
        }

        return (pathScore * 0.4f + driftScore * 0.3f + consistencyScore * 0.3f)
            .toInt()
            .coerceIn(0, 100)
    }

    companion object {
        @Volatile
        private var instance: LabRepository? = null

        fun getInstance(context: Context): LabRepository {
            return instance ?: synchronized(this) {
                instance ?: LabRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}

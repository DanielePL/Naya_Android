// File: android/app/src/main/java/com/example/myapplicationtest/screens/calibration/CalibrationModels.kt

package com.example.menotracker.screens.calibration

import android.graphics.Bitmap

/**
 * 📏 Calibration Data Models
 *
 * Used to store calibration information for accurate VBT measurements
 */

/**
 * Complete calibration data
 */
data class CalibrationData(
    val calibrationId: String,
    val method: CalibrationMethod,
    val referenceDistanceMeters: Float,
    val referencePixels: Float,
    val pixelsPerMeter: Float,
    val accuracyEstimateCm: Float,
    val timestamp: Long,
    val imageUri: String? = null
)

/**
 * Calibration method types
 */
enum class CalibrationMethod(val displayName: String, val tier: String) {
    LIDAR("LiDAR Scanner", "tier1"),
    REFERENCE_OBJECT("Reference Object", "tier2"),
    ARCORE("ARCore Depth", "tier2"),
    NONE("Uncalibrated", "tier3");

    fun getAccuracyEstimate(): Float {
        return when (this) {
            LIDAR -> 0.5f      // ±0.5cm (excellent)
            ARCORE -> 1.0f     // ±1.0cm (very good)
            REFERENCE_OBJECT -> 2.0f  // ±2.0cm (good)
            NONE -> Float.MAX_VALUE   // Unknown
        }
    }
}

/**
 * Reference objects for calibration
 */
enum class ReferenceObject(
    val displayName: String,
    val sizeMeters: Float,
    val description: String,
    val icon: String = "📏"
) {
    RULER_1M(
        "1m Ruler",
        1.0f,
        "Standard measuring ruler (best option)",
        "📏"
    ),
    RULER_50CM(
        "50cm Ruler",
        0.5f,
        "Half meter ruler",
        "📏"
    ),
    PLATE_45CM(
        "45cm Plate",
        0.45f,
        "Standard Olympic plate diameter",
        "🏋️"
    ),
    PLATE_20KG(
        "20kg Plate",
        0.45f,
        "20kg Olympic plate (45cm diameter)",
        "🏋️"
    ),
    BARBELL_220CM(
        "Olympic Barbell",
        2.20f,
        "Standard Olympic barbell length",
        "💪"
    ),
    CUSTOM(
        "Custom Object",
        0f,
        "Enter your own measurement",
        "✏️"
    );

    fun isValid(): Boolean = sizeMeters > 0f
}

/**
 * Calibration flow steps
 */
enum class CalibrationStep {
    INTRO,              // Explain calibration
    SELECT_OBJECT,      // Choose reference object
    INPUT_CUSTOM_SIZE,  // For custom objects
    POSITION_OBJECT,    // Instructions to place object
    CAPTURE,            // Camera view + capture
    PROCESSING,         // Calculate calibration
    SUCCESS,            // Show results
    ERROR               // Handle errors
}

/**
 * Calibration UI State
 */
sealed class CalibrationUiState {
    object Idle : CalibrationUiState()
    data class Step(val step: CalibrationStep) : CalibrationUiState()
    data class Processing(val progress: Int) : CalibrationUiState()
    data class Success(val calibration: CalibrationData) : CalibrationUiState()
    data class Error(val message: String) : CalibrationUiState()
}

/**
 * Captured calibration image with metadata
 */
data class CalibrationCapture(
    val bitmap: Bitmap,
    val referenceObject: ReferenceObject,
    val customSize: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getReferenceSize(): Float {
        return customSize ?: referenceObject.sizeMeters
    }
}

/**
 * Calibration result from processing
 */
data class CalibrationResult(
    val success: Boolean,
    val pixelsPerMeter: Float,
    val referencePixels: Float,
    val accuracyEstimate: Float,
    val message: String
)

/**
 * Stored calibration preferences
 */
data class CalibrationPreferences(
    val isCalibrated: Boolean = false,
    val calibrationData: CalibrationData? = null,
    val expiresAt: Long? = null  // Optional: calibration can expire after X days
) {
    fun isValid(): Boolean {
        if (!isCalibrated || calibrationData == null) return false
        if (expiresAt != null && System.currentTimeMillis() > expiresAt) return false
        return true
    }

    fun getDaysUntilExpiry(): Int? {
        if (expiresAt == null) return null
        val daysRemaining = ((expiresAt - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
        return daysRemaining.coerceAtLeast(0)
    }
}

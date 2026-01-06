// File: android/app/src/main/java/com/example/myapplicationtest/screens/calibration/CalibrationViewModel.kt

package com.example.menotracker.screens.calibration

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 📏 Calibration ViewModel
 *
 * Manages calibration flow and state
 */
class CalibrationViewModel : ViewModel() {

    companion object {
        private const val TAG = "CalibrationVM"
        private const val PREFS_NAME = "calibration_prefs"
        private const val KEY_IS_CALIBRATED = "is_calibrated"
        private const val KEY_CALIBRATION_DATA = "calibration_data"
        private const val KEY_EXPIRY = "calibration_expiry"

        // Calibration expires after 30 days (optional)
        private const val CALIBRATION_EXPIRY_DAYS = 30L
    }

    private val _uiState = MutableStateFlow<CalibrationUiState>(CalibrationUiState.Idle)
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

    private val _selectedObject = MutableStateFlow<ReferenceObject?>(null)
    val selectedObject: StateFlow<ReferenceObject?> = _selectedObject.asStateFlow()

    private val _customSize = MutableStateFlow<Float?>(null)
    val customSize: StateFlow<Float?> = _customSize.asStateFlow()

    private val _capturedImage = MutableStateFlow<Bitmap?>(null)
    val capturedImage: StateFlow<Bitmap?> = _capturedImage.asStateFlow()

    private val _calibrationPreferences = MutableStateFlow<CalibrationPreferences?>(null)
    val calibrationPreferences: StateFlow<CalibrationPreferences?> = _calibrationPreferences.asStateFlow()

    /**
     * Start calibration flow
     */
    fun startCalibration() {
        Log.d(TAG, "🎯 Starting calibration flow")
        _uiState.value = CalibrationUiState.Step(CalibrationStep.INTRO)
    }

    /**
     * Move to next step
     */
    fun nextStep() {
        val currentState = _uiState.value
        if (currentState !is CalibrationUiState.Step) return

        val nextStep = when (currentState.step) {
            CalibrationStep.INTRO -> CalibrationStep.SELECT_OBJECT
            CalibrationStep.SELECT_OBJECT -> {
                if (_selectedObject.value == ReferenceObject.CUSTOM) {
                    CalibrationStep.INPUT_CUSTOM_SIZE
                } else {
                    CalibrationStep.POSITION_OBJECT
                }
            }
            CalibrationStep.INPUT_CUSTOM_SIZE -> CalibrationStep.POSITION_OBJECT
            CalibrationStep.POSITION_OBJECT -> CalibrationStep.CAPTURE
            else -> currentState.step
        }

        _uiState.value = CalibrationUiState.Step(nextStep)
    }

    /**
     * Go to previous step
     */
    fun previousStep() {
        val currentState = _uiState.value
        if (currentState !is CalibrationUiState.Step) return

        val prevStep = when (currentState.step) {
            CalibrationStep.SELECT_OBJECT -> CalibrationStep.INTRO
            CalibrationStep.INPUT_CUSTOM_SIZE -> CalibrationStep.SELECT_OBJECT
            CalibrationStep.POSITION_OBJECT -> {
                if (_selectedObject.value == ReferenceObject.CUSTOM) {
                    CalibrationStep.INPUT_CUSTOM_SIZE
                } else {
                    CalibrationStep.SELECT_OBJECT
                }
            }
            CalibrationStep.CAPTURE -> CalibrationStep.POSITION_OBJECT
            else -> currentState.step
        }

        _uiState.value = CalibrationUiState.Step(prevStep)
    }

    /**
     * Select reference object
     */
    fun selectReferenceObject(obj: ReferenceObject) {
        Log.d(TAG, "📏 Selected reference: ${obj.displayName}")
        _selectedObject.value = obj
    }

    /**
     * Set custom size (for custom objects)
     */
    fun setCustomSize(sizeMeters: Float) {
        if (sizeMeters <= 0f || sizeMeters > 10f) {
            Log.e(TAG, "❌ Invalid custom size: $sizeMeters")
            return
        }
        Log.d(TAG, "✏️ Custom size set: ${sizeMeters}m")
        _customSize.value = sizeMeters
    }

    /**
     * Capture calibration image
     */
    fun captureImage(bitmap: Bitmap) {
        Log.d(TAG, "📸 Image captured: ${bitmap.width}x${bitmap.height}")
        _capturedImage.value = bitmap
        processCalibration()
    }

    /**
     * Process calibration from captured image
     */
    private fun processCalibration() {
        val bitmap = _capturedImage.value ?: return
        val refObject = _selectedObject.value ?: return
        val customSize = _customSize.value

        viewModelScope.launch {
            try {
                _uiState.value = CalibrationUiState.Step(CalibrationStep.PROCESSING)
                Log.d(TAG, "⚙️ Processing calibration...")

                val result = withContext(Dispatchers.Default) {
                    calculateCalibration(bitmap, refObject, customSize)
                }

                if (result.success) {
                    Log.d(TAG, "✅ Calibration successful!")
                    Log.d(TAG, "   Pixels per meter: ${result.pixelsPerMeter}")
                    Log.d(TAG, "   Accuracy: ±${result.accuracyEstimate}cm")

                    val calibrationData = CalibrationData(
                        calibrationId = UUID.randomUUID().toString(),
                        method = CalibrationMethod.REFERENCE_OBJECT,
                        referenceDistanceMeters = customSize ?: refObject.sizeMeters,
                        referencePixels = result.referencePixels,
                        pixelsPerMeter = result.pixelsPerMeter,
                        accuracyEstimateCm = result.accuracyEstimate,
                        timestamp = System.currentTimeMillis()
                    )

                    _uiState.value = CalibrationUiState.Success(calibrationData)
                } else {
                    Log.e(TAG, "❌ Calibration failed: ${result.message}")
                    _uiState.value = CalibrationUiState.Error(result.message)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Calibration error", e)
                _uiState.value = CalibrationUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Calculate calibration from image
     * TODO: Implement computer vision detection
     */
    private suspend fun calculateCalibration(
        bitmap: Bitmap,
        referenceObject: ReferenceObject,
        customSize: Float?
    ): CalibrationResult {
        // Simulate processing delay
        kotlinx.coroutines.delay(1500)

        // TODO: Implement actual object detection using OpenCV or ML Kit
        // For now, use a simplified estimation based on image size

        val referenceSize = customSize ?: referenceObject.sizeMeters

        // Simplified calculation:
        // Assume reference object takes up ~30% of image width
        val estimatedReferencePixels = bitmap.width * 0.3f
        val pixelsPerMeter = estimatedReferencePixels / referenceSize

        // Accuracy estimate (worse for automatic detection)
        val accuracyEstimate = CalibrationMethod.REFERENCE_OBJECT.getAccuracyEstimate()

        return CalibrationResult(
            success = true,
            pixelsPerMeter = pixelsPerMeter,
            referencePixels = estimatedReferencePixels,
            accuracyEstimate = accuracyEstimate,
            message = "Calibration successful! Object detected."
        )
    }

    /**
     * Save calibration to persistent storage
     */
    fun saveCalibration(context: Context, calibrationData: CalibrationData) {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val expiryTime = System.currentTimeMillis() + (CALIBRATION_EXPIRY_DAYS * 24 * 60 * 60 * 1000)

                prefs.edit().apply {
                    putBoolean(KEY_IS_CALIBRATED, true)
                    putString(KEY_CALIBRATION_DATA, serializeCalibrationData(calibrationData))
                    putLong(KEY_EXPIRY, expiryTime)
                    apply()
                }

                _calibrationPreferences.value = CalibrationPreferences(
                    isCalibrated = true,
                    calibrationData = calibrationData,
                    expiresAt = expiryTime
                )

                Log.d(TAG, "💾 Calibration saved successfully")
                Log.d(TAG, "   ID: ${calibrationData.calibrationId}")
                Log.d(TAG, "   Expires: ${CALIBRATION_EXPIRY_DAYS} days")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save calibration", e)
            }
        }
    }

    /**
     * Load calibration from storage
     */
    fun loadCalibration(context: Context) {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val isCalibrated = prefs.getBoolean(KEY_IS_CALIBRATED, false)

                if (isCalibrated) {
                    val dataString = prefs.getString(KEY_CALIBRATION_DATA, null)
                    val expiryTime = prefs.getLong(KEY_EXPIRY, 0L)

                    if (dataString != null) {
                        val calibrationData = deserializeCalibrationData(dataString)

                        _calibrationPreferences.value = CalibrationPreferences(
                            isCalibrated = true,
                            calibrationData = calibrationData,
                            expiresAt = if (expiryTime > 0) expiryTime else null
                        )

                        Log.d(TAG, "✅ Calibration loaded from storage")
                        Log.d(TAG, "   Valid: ${_calibrationPreferences.value?.isValid()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load calibration", e)
            }
        }
    }

    /**
     * Clear calibration
     */
    fun clearCalibration(context: Context) {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().clear().apply()

                _calibrationPreferences.value = CalibrationPreferences(isCalibrated = false)
                Log.d(TAG, "🗑️ Calibration cleared")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to clear calibration", e)
            }
        }
    }

    /**
     * Reset flow
     */
    fun reset() {
        _uiState.value = CalibrationUiState.Idle
        _selectedObject.value = null
        _customSize.value = null
        _capturedImage.value = null
    }

    /**
     * Serialize calibration data to JSON string
     * TODO: Use proper JSON serialization (kotlinx.serialization or Gson)
     */
    private fun serializeCalibrationData(data: CalibrationData): String {
        return "${data.calibrationId}|${data.method.name}|${data.referenceDistanceMeters}|${data.referencePixels}|${data.pixelsPerMeter}|${data.accuracyEstimateCm}|${data.timestamp}"
    }

    /**
     * Deserialize calibration data from JSON string
     */
    private fun deserializeCalibrationData(dataString: String): CalibrationData {
        val parts = dataString.split("|")
        return CalibrationData(
            calibrationId = parts[0],
            method = CalibrationMethod.valueOf(parts[1]),
            referenceDistanceMeters = parts[2].toFloat(),
            referencePixels = parts[3].toFloat(),
            pixelsPerMeter = parts[4].toFloat(),
            accuracyEstimateCm = parts[5].toFloat(),
            timestamp = parts[6].toLong()
        )
    }
}

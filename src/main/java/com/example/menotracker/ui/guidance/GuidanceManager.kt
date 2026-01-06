package com.example.menotracker.ui.guidance

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * GUIDANCE MANAGER
 * ══════════════════════════════════════════════════════════════════════════════
 *
 * Verwaltet welche Hints/Spotlights/Tours bereits gezeigt wurden.
 * Speichert den Status persistent in SharedPreferences.
 *
 * WICHTIG: Ein Hint wird nur EINMAL gezeigt (es sei denn User resettet in Settings)
 *
 * ══════════════════════════════════════════════════════════════════════════════
 */

class GuidanceManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _shownHints = MutableStateFlow<Set<String>>(loadShownHints())
    val shownHints: StateFlow<Set<String>> = _shownHints.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Prüft ob ein Hint bereits gezeigt wurde
     */
    fun hasSeenHint(hintId: String): Boolean {
        return _shownHints.value.contains(hintId)
    }

    /**
     * Markiert einen Hint als gesehen
     */
    fun markHintAsSeen(hintId: String) {
        val updated = _shownHints.value + hintId
        _shownHints.value = updated
        saveShownHints(updated)
    }

    /**
     * Prüft ob ein Spotlight bereits gezeigt wurde
     */
    fun hasSeenSpotlight(spotlightId: String): Boolean {
        return prefs.getBoolean("${SPOTLIGHT_PREFIX}$spotlightId", false)
    }

    /**
     * Markiert ein Spotlight als gesehen
     */
    fun markSpotlightAsSeen(spotlightId: String) {
        prefs.edit().putBoolean("${SPOTLIGHT_PREFIX}$spotlightId", true).apply()
    }

    /**
     * Prüft ob eine Tour bereits abgeschlossen wurde
     */
    fun hasCompletedTour(tourId: String): Boolean {
        return prefs.getBoolean("${TOUR_PREFIX}$tourId", false)
    }

    /**
     * Markiert eine Tour als abgeschlossen
     */
    fun markTourAsCompleted(tourId: String) {
        prefs.edit().putBoolean("${TOUR_PREFIX}$tourId", true).apply()
    }

    /**
     * Gibt den aktuellen Schritt einer Tour zurück (falls unterbrochen)
     */
    fun getTourProgress(tourId: String): Int {
        return prefs.getInt("${TOUR_PROGRESS_PREFIX}$tourId", 0)
    }

    /**
     * Speichert den Fortschritt einer Tour
     */
    fun saveTourProgress(tourId: String, stepIndex: Int) {
        prefs.edit().putInt("${TOUR_PROGRESS_PREFIX}$tourId", stepIndex).apply()
    }

    /**
     * Resettet alle Guidance (für Settings "Tutorial zurücksetzen")
     */
    fun resetAllGuidance() {
        prefs.edit().clear().apply()
        _shownHints.value = emptySet()
    }

    /**
     * Resettet nur Hints (nicht Spotlights/Tours)
     */
    fun resetHints() {
        prefs.edit().remove(SHOWN_HINTS_KEY).apply()
        _shownHints.value = emptySet()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private fun loadShownHints(): Set<String> {
        return prefs.getStringSet(SHOWN_HINTS_KEY, emptySet()) ?: emptySet()
    }

    private fun saveShownHints(hints: Set<String>) {
        prefs.edit().putStringSet(SHOWN_HINTS_KEY, hints).apply()
    }

    companion object {
        private const val PREFS_NAME = "naya_guidance"
        private const val SHOWN_HINTS_KEY = "shown_hints"
        private const val SPOTLIGHT_PREFIX = "spotlight_"
        private const val TOUR_PREFIX = "tour_"
        private const val TOUR_PROGRESS_PREFIX = "tour_progress_"

        @Volatile
        private var instance: GuidanceManager? = null

        fun getInstance(context: Context): GuidanceManager {
            return instance ?: synchronized(this) {
                instance ?: GuidanceManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════════
// COMPOSABLE HELPER - Einfache Integration in Screens
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * rememberGuidanceState - Composable helper für einfache Hint-Integration
 *
 * Usage:
 * ```
 * val (showHint, dismissHint) = rememberGuidanceState(GuidanceHints.VBT_CAMERA_POSITION)
 *
 * PulseHint(
 *     hint = "Handy seitlich für beste Barpath-Erkennung",
 *     isVisible = showHint,
 *     onDismiss = dismissHint
 * )
 * ```
 */
@Composable
fun rememberGuidanceState(
    hintId: String,
    guidanceManager: GuidanceManager
): Pair<Boolean, () -> Unit> {
    var showHint by remember { mutableStateOf(!guidanceManager.hasSeenHint(hintId)) }

    val dismissHint: () -> Unit = {
        showHint = false
        guidanceManager.markHintAsSeen(hintId)
    }

    return Pair(showHint, dismissHint)
}

/**
 * rememberSpotlightState - Für FeatureSpotlight
 */
@Composable
fun rememberSpotlightState(
    spotlightId: String,
    guidanceManager: GuidanceManager
): Pair<Boolean, () -> Unit> {
    var showSpotlight by remember { mutableStateOf(!guidanceManager.hasSeenSpotlight(spotlightId)) }

    val dismissSpotlight: () -> Unit = {
        showSpotlight = false
        guidanceManager.markSpotlightAsSeen(spotlightId)
    }

    return Pair(showSpotlight, dismissSpotlight)
}

/**
 * rememberTourState - Für InteractiveTour
 */
@Composable
fun rememberTourState(
    tourId: String,
    steps: List<TourStep>,
    guidanceManager: GuidanceManager
): TourState {
    val hasCompleted = guidanceManager.hasCompletedTour(tourId)
    var currentIndex by remember { mutableStateOf(guidanceManager.getTourProgress(tourId)) }
    var isActive by remember { mutableStateOf(!hasCompleted) }

    return TourState(
        isActive = isActive,
        currentStep = if (isActive && currentIndex < steps.size) steps[currentIndex] else null,
        currentIndex = currentIndex,
        totalSteps = steps.size,
        onNext = {
            if (currentIndex < steps.size - 1) {
                currentIndex++
                guidanceManager.saveTourProgress(tourId, currentIndex)
            } else {
                isActive = false
                guidanceManager.markTourAsCompleted(tourId)
            }
        },
        onSkip = {
            isActive = false
            guidanceManager.markTourAsCompleted(tourId)
        }
    )
}

data class TourState(
    val isActive: Boolean,
    val currentStep: TourStep?,
    val currentIndex: Int,
    val totalSteps: Int,
    val onNext: () -> Unit,
    val onSkip: () -> Unit
)
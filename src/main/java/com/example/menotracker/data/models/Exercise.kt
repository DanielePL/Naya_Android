// app/src/main/java/com/example/myapplicationtest/data/models/Exercise.kt

package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    @SerialName("id")
    val id: String,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("name")
    val name: String,

    // ═══════════════════════════════════════════════════════════════
    // NEW SCHEMA: category (replaces main_muscle_group)
    // ═══════════════════════════════════════════════════════════════
    @SerialName("category")
    val category: String? = null,

    // Backward compatibility: old schema uses main_muscle_group
    @SerialName("main_muscle_group")
    val mainMuscle: String? = null,

    @SerialName("secondary_muscle_groups")
    val secondaryMuscles: List<String>? = null,

    val equipment: List<String>? = null,

    // ═══════════════════════════════════════════════════════════════
    // NEW SCHEMA: level, visibility, owner_id
    // ═══════════════════════════════════════════════════════════════
    @SerialName("level")
    val level: String? = null,  // "beginner", "intermediate", "advanced"

    @SerialName("visibility")
    val visibility: String? = null,  // "admin", "coach", "user"

    @SerialName("owner_id")
    val ownerId: String? = null,

    val tempo: String? = null,

    @SerialName("rest_time_seconds")
    val restTimeInSeconds: Int? = null,

    @SerialName("track_reps")
    val trackReps: Boolean = true,

    @SerialName("track_sets")
    val trackSets: Boolean = true,

    @SerialName("track_weight")
    val trackWeight: Boolean = true,

    @SerialName("track_rpe")
    val trackRpe: Boolean = false,

    @SerialName("track_duration")
    val trackDuration: Boolean = false,

    @SerialName("track_distance")
    val trackDistance: Boolean = false,

    @SerialName("video_url")
    val videoUrl: String? = null,

    val tutorial: String? = null,

    val notes: String? = null,

    // ═══════════════════════════════════════════════════════════════
    // VBT (VELOCITY-BASED TRAINING) PROPERTIES
    // ═══════════════════════════════════════════════════════════════

    // NEW SCHEMA: vbt_enabled, bartracker_enabled, advanced_metrics
    @SerialName("vbt_enabled")
    val vbtEnabled: Boolean = false,

    @SerialName("bartracker_enabled")
    val barTrackerEnabled: Boolean = false,

    @SerialName("advanced_metrics")
    val advancedMetrics: Map<String, String>? = null,  // JSONB field

    // LEGACY: Keep for backward compatibility
    @SerialName("supports_power_score")
    val supportsPowerScore: Boolean = false,  // ⚡ Measures bar velocity/power

    @SerialName("supports_technique_score")
    val supportsTechniqueScore: Boolean = false,  // ✓ Analyzes bar path

    @SerialName("vbt_measurement_type")
    val vbtMeasurementType: String? = null,  // "average" or "peak"

    @SerialName("vbt_category")
    val vbtCategory: String? = null,  // "Squat", "Deadlift", "Olympic", "Press", "Row", "Pull"

    // ═══════════════════════════════════════════════════════════════
    // SPORT CATEGORIES (can belong to multiple sports)
    // ═══════════════════════════════════════════════════════════════

    @SerialName("sports")
    val sports: List<String>? = null,  // ["Weightlifting", "Powerlifting", "General Strength", "CrossFit", "Hyrox"]

    // ═══════════════════════════════════════════════════════════════
    // TECHNIQUE GUIDES (from exercise_view join with technique_guides)
    // ═══════════════════════════════════════════════════════════════

    @SerialName("technique_sections")
    val techniqueSections: List<TechniqueSection>? = null
) {
    // Convenience getter for category that falls back to mainMuscle for backward compatibility
    val muscleCategory: String?
        get() = category ?: mainMuscle

    // Convenience getter for VBT support (new schema or legacy)
    val supportsVBT: Boolean
        get() = vbtEnabled || supportsPowerScore

    // Convenience getter for BarTracker support (new schema or legacy)
    val supportsBarTracker: Boolean
        get() = barTrackerEnabled || supportsTechniqueScore
}

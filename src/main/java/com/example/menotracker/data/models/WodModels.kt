package com.example.menotracker.data.models

import com.google.gson.annotations.SerializedName

/**
 * CrossFit WOD (Workout of the Day) Data Models
 * Used for WOD scanning, storage, and tracking
 */

// ============================================================
// WOD TEMPLATE - Main WOD definition
// ============================================================

data class WodTemplate(
    val id: String = "",

    val name: String,

    val description: String? = null,

    @SerializedName("wod_type")
    val wodType: String, // amrap, emom, for_time, rft, chipper, etc.

    @SerializedName("time_cap_seconds")
    val timeCapSeconds: Int? = null,

    @SerializedName("target_rounds")
    val targetRounds: Int? = null,

    @SerializedName("rep_scheme")
    val repScheme: List<Int>? = null, // e.g., [21, 15, 9]

    @SerializedName("rep_scheme_type")
    val repSchemeType: String? = null, // fixed, descending, ascending, pyramid

    @SerializedName("scoring_type")
    val scoringType: String = "rounds_reps",

    val source: String? = null, // whiteboard_scan, manual, etc.

    @SerializedName("source_box_name")
    val sourceBoxName: String? = null,

    @SerializedName("source_image_url")
    val sourceImageUrl: String? = null,

    @SerializedName("user_id")
    val userId: String? = null,

    @SerializedName("is_public")
    val isPublic: Boolean = false,

    val difficulty: String? = null, // beginner, intermediate, advanced, elite

    @SerializedName("estimated_duration_minutes")
    val estimatedDurationMinutes: Int? = null,

    @SerializedName("primary_focus")
    val primaryFocus: List<String>? = null, // cardio, strength, gymnastics, weightlifting

    @SerializedName("equipment_needed")
    val equipmentNeeded: List<String>? = null,

    val tags: List<String>? = null,

    @SerializedName("likes_count")
    val likesCount: Int = 0,

    @SerializedName("completions_count")
    val completionsCount: Int = 0,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null
) {
    /**
     * Get display string for WOD type
     */
    fun getWodTypeDisplay(): String = when (wodType) {
        "amrap" -> "AMRAP"
        "emom" -> "EMOM"
        "for_time" -> "For Time"
        "rft" -> "Rounds For Time"
        "chipper" -> "Chipper"
        "ladder" -> "Ladder"
        "tabata" -> "Tabata"
        "death_by" -> "Death By"
        "max_effort" -> "Max Effort"
        else -> wodType.replaceFirstChar { it.uppercase() }
    }

    /**
     * Get formatted time cap string
     */
    fun getTimeCapDisplay(): String? {
        val seconds = timeCapSeconds ?: return null
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        return if (remainingSeconds > 0) {
            "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
        } else {
            "$minutes min"
        }
    }

    /**
     * Get rep scheme display (e.g., "21-15-9")
     */
    fun getRepSchemeDisplay(): String? {
        return repScheme?.joinToString("-")
    }

    /**
     * Get difficulty badge color
     */
    fun getDifficultyColor(): Long = when (difficulty) {
        "beginner" -> 0xFF4CAF50 // Green
        "intermediate" -> 0xFFFF9800 // Orange
        "advanced" -> 0xFFF44336 // Red
        "elite" -> 0xFF9C27B0 // Purple
        else -> 0xFF9E9E9E // Grey
    }
}

// ============================================================
// WOD MOVEMENT - Individual exercise in a WOD
// ============================================================

data class WodMovement(
    val id: String = "",

    @SerializedName("wod_template_id")
    val wodTemplateId: String = "",

    @SerializedName("exercise_id")
    val exerciseId: String? = null,

    @SerializedName("movement_name")
    val movementName: String,

    @SerializedName("movement_description")
    val movementDescription: String? = null,

    @SerializedName("order_index")
    val orderIndex: Int = 0,

    val segment: Int = 1,

    @SerializedName("rep_type")
    val repType: String = "fixed", // fixed, calories, distance, time, max

    val reps: Int? = null,

    @SerializedName("reps_male")
    val repsMale: Int? = null,

    @SerializedName("reps_female")
    val repsFemale: Int? = null,

    @SerializedName("distance_meters")
    val distanceMeters: Int? = null,

    val calories: Int? = null,

    @SerializedName("time_seconds")
    val timeSeconds: Int? = null,

    @SerializedName("weight_type")
    val weightType: String = "bodyweight", // bodyweight, fixed, percentage

    @SerializedName("weight_kg_male")
    val weightKgMale: Double? = null,

    @SerializedName("weight_kg_female")
    val weightKgFemale: Double? = null,

    @SerializedName("emom_minutes")
    val emomMinutes: List<Int>? = null,

    val notes: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
) {
    /**
     * Get display string for reps/work
     */
    fun getWorkDisplay(): String {
        return when (repType) {
            "calories" -> "${calories ?: 0} cal"
            "distance" -> "${distanceMeters ?: 0}m"
            "time" -> {
                val secs = timeSeconds ?: 0
                if (secs >= 60) "${secs / 60} min" else "$secs sec"
            }
            "max" -> "Max reps"
            else -> "${reps ?: 0} reps"
        }
    }

    /**
     * Get weight display (if applicable)
     */
    fun getWeightDisplay(isMale: Boolean = true): String? {
        if (weightType == "bodyweight") return null

        val weight = if (isMale) weightKgMale else weightKgFemale
        return weight?.let { "${it}kg" }
    }

    /**
     * Get full movement display string
     */
    fun getFullDisplay(isMale: Boolean = true): String {
        val work = getWorkDisplay()
        val weight = getWeightDisplay(isMale)

        return if (weight != null) {
            "$work $movementName @ $weight"
        } else {
            "$work $movementName"
        }
    }
}

// ============================================================
// WOD SCALING - Rx/Scaled/Foundations options
// ============================================================

data class WodScaling(
    val id: String = "",

    @SerializedName("wod_template_id")
    val wodTemplateId: String = "",

    @SerializedName("wod_movement_id")
    val wodMovementId: String? = null,

    @SerializedName("scaling_level")
    val scalingLevel: String, // rx, scaled, foundations, masters

    val reps: Int? = null,

    @SerializedName("reps_male")
    val repsMale: Int? = null,

    @SerializedName("reps_female")
    val repsFemale: Int? = null,

    @SerializedName("weight_kg_male")
    val weightKgMale: Double? = null,

    @SerializedName("weight_kg_female")
    val weightKgFemale: Double? = null,

    @SerializedName("alternative_exercise_id")
    val alternativeExerciseId: String? = null,

    @SerializedName("alternative_movement_name")
    val alternativeMovementName: String? = null,

    @SerializedName("alternative_description")
    val alternativeDescription: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
)

// ============================================================
// WOD RESULT - User's score/performance
// ============================================================

data class WodResult(
    val id: String = "",

    @SerializedName("user_id")
    val userId: String,

    @SerializedName("wod_template_id")
    val wodTemplateId: String,

    @SerializedName("completed_at")
    val completedAt: String? = null,

    @SerializedName("score_type")
    val scoreType: String, // rounds_reps, time, weight, reps, pass_fail

    @SerializedName("rounds_completed")
    val roundsCompleted: Int? = null,

    @SerializedName("reps_completed")
    val repsCompleted: Int? = null,

    @SerializedName("time_seconds")
    val timeSeconds: Int? = null,

    @SerializedName("weight_kg")
    val weightKg: Double? = null,

    @SerializedName("total_reps")
    val totalReps: Int? = null,

    @SerializedName("scaling_level")
    val scalingLevel: String = "rx",

    @SerializedName("completed_within_cap")
    val completedWithinCap: Boolean = true,

    val notes: String? = null,

    @SerializedName("video_url")
    val videoUrl: String? = null,

    @SerializedName("is_verified")
    val isVerified: Boolean = false,

    @SerializedName("created_at")
    val createdAt: String? = null
) {
    /**
     * Get display string for score
     */
    fun getScoreDisplay(): String {
        return when (scoreType) {
            "rounds_reps" -> {
                val rounds = roundsCompleted ?: 0
                val reps = repsCompleted ?: 0
                if (reps > 0) "$rounds rounds + $reps reps" else "$rounds rounds"
            }
            "time" -> {
                val totalSecs = timeSeconds ?: 0
                val mins = totalSecs / 60
                val secs = totalSecs % 60
                "$mins:${secs.toString().padStart(2, '0')}"
            }
            "weight" -> "${weightKg ?: 0}kg"
            "reps" -> "${totalReps ?: 0} reps"
            "pass_fail" -> if (completedWithinCap) "Completed" else "DNF"
            else -> "N/A"
        }
    }

    /**
     * Get scaling level display
     */
    fun getScalingDisplay(): String = when (scalingLevel) {
        "rx" -> "Rx"
        "scaled" -> "Scaled"
        "foundations" -> "Foundations"
        "masters" -> "Masters"
        else -> scalingLevel.replaceFirstChar { it.uppercase() }
    }
}

// ============================================================
// COMBINED WOD - Template with all movements
// ============================================================

data class WodWithMovements(
    val wod: WodTemplate,
    val movements: List<WodMovement> = emptyList(),
    val scaling: Map<String, List<WodScaling>> = emptyMap() // keyed by scaling level
)

// ============================================================
// WOD SCAN RESPONSE - From Vision API
// ============================================================

data class WodScanResponse(
    val success: Boolean,
    val wod: ParsedWod? = null,
    @SerializedName("wod_template_id")
    val wodTemplateId: String? = null,
    @SerializedName("movements_saved")
    val movementsSaved: Int? = null,
    val confidence: Double? = null,
    @SerializedName("save_error")
    val saveError: String? = null,
    val error: String? = null
)

data class ParsedWod(
    val name: String,
    val description: String? = null,
    @SerializedName("wod_type")
    val wodType: String,
    @SerializedName("time_cap_seconds")
    val timeCapSeconds: Int? = null,
    @SerializedName("target_rounds")
    val targetRounds: Int? = null,
    @SerializedName("rep_scheme")
    val repScheme: List<Int>? = null,
    @SerializedName("rep_scheme_type")
    val repSchemeType: String? = null,
    val difficulty: String? = null,
    @SerializedName("estimated_duration_minutes")
    val estimatedDurationMinutes: Int? = null,
    @SerializedName("primary_focus")
    val primaryFocus: List<String>? = null,
    @SerializedName("equipment_needed")
    val equipmentNeeded: List<String>? = null,
    val movements: List<ParsedMovement> = emptyList(),
    val scaling: Map<String, List<ParsedScaling>>? = null,
    val notes: String? = null,
    val confidence: Double? = null
)

data class ParsedMovement(
    @SerializedName("order_index")
    val orderIndex: Int = 0,
    @SerializedName("movement_name")
    val movementName: String,
    @SerializedName("rep_type")
    val repType: String = "fixed",
    val reps: Int? = null,
    @SerializedName("reps_male")
    val repsMale: Int? = null,
    @SerializedName("reps_female")
    val repsFemale: Int? = null,
    @SerializedName("distance_meters")
    val distanceMeters: Int? = null,
    val calories: Int? = null,
    @SerializedName("time_seconds")
    val timeSeconds: Int? = null,
    @SerializedName("weight_type")
    val weightType: String = "bodyweight",
    @SerializedName("weight_kg_male")
    val weightKgMale: Double? = null,
    @SerializedName("weight_kg_female")
    val weightKgFemale: Double? = null,
    val notes: String? = null
)

data class ParsedScaling(
    @SerializedName("movement_name")
    val movementName: String,
    @SerializedName("alternative_name")
    val alternativeName: String? = null,
    val reps: Int? = null,
    @SerializedName("weight_kg_male")
    val weightKgMale: Double? = null,
    @SerializedName("weight_kg_female")
    val weightKgFemale: Double? = null,
    val notes: String? = null
)

// ============================================================
// WOD LIST RESPONSE
// ============================================================

data class WodListResponse(
    val wods: List<WodTemplate> = emptyList(),
    val count: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0
)

// ============================================================
// WOD TYPE DEFINITIONS
// ============================================================

enum class WodType(val id: String, val displayName: String, val scoringFormat: String) {
    AMRAP("amrap", "AMRAP", "rounds_reps"),
    EMOM("emom", "EMOM", "pass_fail"),
    FOR_TIME("for_time", "For Time", "time"),
    RFT("rft", "Rounds For Time", "time"),
    CHIPPER("chipper", "Chipper", "time"),
    LADDER("ladder", "Ladder", "time"),
    TABATA("tabata", "Tabata", "reps"),
    DEATH_BY("death_by", "Death By", "reps"),
    MAX_EFFORT("max_effort", "Max Effort", "weight"),
    CUSTOM("custom", "Custom", "custom");

    companion object {
        fun fromId(id: String): WodType = values().find { it.id == id } ?: CUSTOM
    }
}

enum class ScalingLevel(val id: String, val displayName: String) {
    RX("rx", "Rx"),
    SCALED("scaled", "Scaled"),
    FOUNDATIONS("foundations", "Foundations"),
    MASTERS("masters", "Masters");

    companion object {
        fun fromId(id: String): ScalingLevel = values().find { it.id == id } ?: SCALED
    }
}
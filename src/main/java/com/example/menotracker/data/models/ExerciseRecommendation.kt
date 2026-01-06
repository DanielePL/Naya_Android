// app/src/main/java/com/example/myapplicationtest/data/models/ExerciseRecommendation.kt

package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sport-specific and level-specific exercise recommendations from database
 * Maps to public.exercise_recommendations table
 * Note: Different from WorkoutExerciseRecommendation used in AI chat responses
 */
@Serializable
data class DbExerciseRecommendation(
    @SerialName("id")
    val id: String,

    @SerialName("exercise_id")
    val exerciseId: String,

    @SerialName("sport")
    val sport: SportType,

    @SerialName("level")
    val level: ExperienceLevel,

    @SerialName("suggested_sets")
    val suggestedSets: Int? = null,

    @SerialName("suggested_reps")
    val suggestedReps: Int? = null,

    @SerialName("suggested_percent_1rm")
    val suggestedPercent1RM: Double? = null,  // e.g., 80.00 for 80%

    @SerialName("suggested_rir")
    val suggestedRIR: Int? = null,  // Reps in Reserve

    @SerialName("suggested_rest_seconds")
    val suggestedRestSeconds: Int? = null,

    @SerialName("suggested_tempo")
    val suggestedTempo: String? = null,  // e.g., "3-1-X-1"

    @SerialName("notes")
    val notes: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Sport types matching Supabase ENUM: public.sport_type
 */
@Serializable
enum class SportType(val value: String) {
    @SerialName("bodybuilding")
    BODYBUILDING("bodybuilding"),

    @SerialName("powerlifting")
    POWERLIFTING("powerlifting"),

    @SerialName("crossfit")
    CROSSFIT("crossfit"),

    @SerialName("weightlifting")
    WEIGHTLIFTING("weightlifting"),

    @SerialName("hyrox")
    HYROX("hyrox"),

    @SerialName("general_strength")
    GENERAL_STRENGTH("general_strength")
}

/**
 * Experience levels matching Supabase ENUM: public.experience_level
 */
@Serializable
enum class ExperienceLevel(val value: String) {
    @SerialName("beginner")
    BEGINNER("beginner"),

    @SerialName("intermediate")
    INTERMEDIATE("intermediate"),

    @SerialName("advanced")
    ADVANCED("advanced")
}

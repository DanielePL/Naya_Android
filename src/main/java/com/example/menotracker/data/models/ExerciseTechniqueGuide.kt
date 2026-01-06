// app/src/main/java/com/example/myapplicationtest/data/models/ExerciseTechniqueGuide.kt

package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Technique guide section from exercise_technique_guides table
 * Matches the JSONB structure: {"title": "...", "bullets": ["...", "..."]}
 */
@Serializable
data class TechniqueSection(
    @SerialName("title")
    val title: String = "",

    @SerialName("bullets")
    val bullets: List<String> = emptyList()
)

/**
 * Full technique guide for an exercise
 * Maps to public.exercise_technique_guides table
 */
@Serializable
data class ExerciseTechniqueGuide(
    @SerialName("id")
    val id: String,

    @SerialName("exercise_id")
    val exerciseId: String,

    @SerialName("exercise_name")
    val exerciseName: String,

    @SerialName("language")
    val language: String,  // "en" or "de"

    @SerialName("version")
    val version: Int = 1,

    @SerialName("sections")
    val sections: List<TechniqueSection> = emptyList(),

    @SerialName("source")
    val source: String? = null,

    @SerialName("created_by")
    val createdBy: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)

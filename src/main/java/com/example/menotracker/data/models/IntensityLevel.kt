package com.example.menotracker.data.models

/**
 * Intensity levels for workout templates.
 * Used to categorize workouts by difficulty/effort level for the 40+ target audience.
 *
 * Note: Named WorkoutIntensityLevel to avoid conflict with
 * IntensityLevel in MenopauseModels.kt (used for symptom tracking)
 */
enum class WorkoutIntensityLevel(
    val displayName: String,
    val description: String
) {
    SANFT(
        displayName = "Sanft",
        description = "Niedrige Intensität, ideal für Einsteiger oder Erholungstage"
    ),
    AKTIV(
        displayName = "Aktiv",
        description = "Moderate Intensität, ausgewogenes Training"
    ),
    POWER(
        displayName = "Power",
        description = "Hohe Intensität, für ambitionierte Trainierende"
    );

    companion object {
        fun fromString(value: String?): WorkoutIntensityLevel {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: AKTIV
        }

        fun allLevels(): List<WorkoutIntensityLevel> = entries.toList()
    }
}

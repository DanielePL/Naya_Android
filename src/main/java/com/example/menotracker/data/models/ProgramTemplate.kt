package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Strength Program Template - Pre-fabricated training programs for powerlifters
 *
 * Templates define the structure of a strength training program with:
 * - Phases (Accumulation, Intensification, Peaking)
 * - Week templates with day templates
 * - Percentage-based loading (calculated from user's PRs)
 * - VBT velocity targets
 */
@Serializable
data class StrengthProgramTemplate(
    val id: String,
    val name: String,
    val description: String,

    // Duration and structure
    @SerialName("duration_weeks") val durationWeeks: Int,
    @SerialName("sessions_per_week") val sessionsPerWeek: Int,

    // Target audience
    @SerialName("target_levels") val targetLevels: List<StrengthExperienceLevel>,
    val focus: ProgramFocus,

    // Program content
    val phases: List<StrengthProgramPhase>,
    @SerialName("progression_scheme") val progressionScheme: ProgressionScheme,

    // Metadata
    val author: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("is_premium") val isPremium: Boolean = false,
    @SerialName("image_url") val imageUrl: String? = null,
    val rating: Float? = null,
    @SerialName("times_used") val timesUsed: Int = 0
)

@Serializable
enum class ProgramFocus {
    @SerialName("strength") STRENGTH,
    @SerialName("hypertrophy") HYPERTROPHY,
    @SerialName("peaking") PEAKING,
    @SerialName("general") GENERAL;

    val displayName: String
        get() = when (this) {
            STRENGTH -> "Strength"
            HYPERTROPHY -> "Hypertrophy"
            PEAKING -> "Peaking"
            GENERAL -> "General Fitness"
        }
}

/**
 * Strength Program Phase - e.g., Accumulation, Intensification, Peaking
 */
@Serializable
data class StrengthProgramPhase(
    val name: String,
    val description: String? = null,
    val weeks: Int,
    @SerialName("week_templates") val weekTemplates: List<WeekTemplate>,

    // Phase characteristics
    @SerialName("min_intensity") val minIntensity: Float, // e.g., 0.65f
    @SerialName("max_intensity") val maxIntensity: Float, // e.g., 0.75f
    @SerialName("volume_modifier") val volumeModifier: Float = 1.0f // Relative volume vs baseline
) {
    // Helper property for intensity range
    val intensityRange: ClosedFloatingPointRange<Float> get() = minIntensity..maxIntensity
}

/**
 * Week Type - Klassifiziert die Art der Trainingswoche
 */
@Serializable
enum class WeekType {
    @SerialName("normal") NORMAL,         // Reguläres Training
    @SerialName("deload") DELOAD,         // Regenerationswoche (reduzierte Last)
    @SerialName("ilb_test") ILB_TEST;     // ILB Testwoche (AMRAP für alle Compound-Übungen)

    val displayName: String
        get() = when (this) {
            NORMAL -> "Training"
            DELOAD -> "Deload"
            ILB_TEST -> "ILB Test"
        }

    val description: String
        get() = when (this) {
            NORMAL -> "Reguläres Training"
            DELOAD -> "Regeneration - reduzierte Intensität"
            ILB_TEST -> "Leistungstest - AMRAP für neue 1RM-Werte"
        }
}

/**
 * Week Template - Structure for a training week
 */
@Serializable
data class WeekTemplate(
    @SerialName("week_number") val weekNumber: Int, // Within the phase
    val days: List<DayTemplate>,
    @SerialName("is_deload") val isDeload: Boolean = false,
    @SerialName("week_type") val weekType: WeekType = WeekType.NORMAL,  // ILB: Typ der Woche
    val notes: String? = null
) {
    // Computed: Ist das eine Test-Woche?
    val isTestWeek: Boolean get() = weekType == WeekType.ILB_TEST
}

/**
 * Day Template - Structure for a training day
 */
@Serializable
data class DayTemplate(
    val name: String, // "Squat Day", "Bench Day", "Deadlift Day"
    @SerialName("day_number") val dayNumber: Int,
    @SerialName("primary_lift") val primaryLift: LiftType,
    val exercises: List<ExerciseTemplate>
)

/**
 * Exercise Template - Defines sets, reps, and percentage for an exercise
 */
@Serializable
data class ExerciseTemplate(
    @SerialName("exercise_id") val exerciseId: String? = null, // Reference to exercise library
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("is_main_lift") val isMainLift: Boolean = false,
    @SerialName("lift_type") val liftType: LiftType? = null, // For percentage calculation

    // Set/rep scheme
    val sets: Int,
    @SerialName("min_reps") val minReps: Int,
    @SerialName("max_reps") val maxReps: Int,
    @SerialName("min_percentage") val minPercentage: Float? = null, // e.g., 0.75f
    @SerialName("max_percentage") val maxPercentage: Float? = null, // e.g., 0.80f

    // Alternative intensity methods
    @SerialName("rpe_target") val rpeTarget: Float? = null, // e.g., 8.0
    @SerialName("velocity_target") val velocityTarget: Float? = null, // m/s

    // Rest and tempo
    @SerialName("rest_seconds") val restSeconds: Int = 180,
    val tempo: String? = null, // e.g., "3-1-X-0"

    // Notes
    val notes: String? = null,
    @SerialName("superset_with") val supersetWith: String? = null // Exercise ID for supersets
) {
    // Helper property for rep range
    val repRange: IntRange get() = minReps..maxReps

    // Helper property for percentage range
    val percentageRange: ClosedFloatingPointRange<Float>?
        get() = if (minPercentage != null && maxPercentage != null) minPercentage..maxPercentage else null

    companion object {
        // Helper factory for easier creation with ranges
        fun create(
            exerciseName: String,
            sets: Int,
            repRange: IntRange,
            percentageRange: ClosedFloatingPointRange<Float>? = null,
            isMainLift: Boolean = false,
            liftType: LiftType? = null,
            rpeTarget: Float? = null,
            velocityTarget: Float? = null,
            restSeconds: Int = 180,
            tempo: String? = null,
            notes: String? = null
        ) = ExerciseTemplate(
            exerciseName = exerciseName,
            isMainLift = isMainLift,
            liftType = liftType,
            sets = sets,
            minReps = repRange.first,
            maxReps = repRange.last,
            minPercentage = percentageRange?.start,
            maxPercentage = percentageRange?.endInclusive,
            rpeTarget = rpeTarget,
            velocityTarget = velocityTarget,
            restSeconds = restSeconds,
            tempo = tempo,
            notes = notes
        )
    }
}

/**
 * Progression Scheme - How the program progresses over time
 */
@Serializable
data class ProgressionScheme(
    val type: ProgressionType,

    // Linear progression
    @SerialName("weekly_increment_kg") val weeklyIncrementKg: Float? = null,

    // Percentage-based progression
    @SerialName("weekly_percentage_increase") val weeklyPercentageIncrease: Float? = null,

    // Auto-regulation
    @SerialName("use_vbt") val useVBT: Boolean = false,
    @SerialName("use_rpe") val useRPE: Boolean = false,

    // Deload
    @SerialName("deload_frequency_weeks") val deloadFrequencyWeeks: Int = 4,
    @SerialName("deload_intensity_reduction") val deloadIntensityReduction: Float = 0.4f
)

@Serializable
enum class ProgressionType {
    @SerialName("linear") LINEAR,
    @SerialName("percentage_wave") PERCENTAGE_WAVE,
    @SerialName("undulating") UNDULATING,
    @SerialName("block") BLOCK,
    @SerialName("autoregulated") AUTOREGULATED
}

// =====================================================
// PERSONALIZED PROGRAM (User's instance of a template)
// =====================================================

/**
 * Personalized Program - User's active program with calculated weights
 */
@Serializable
data class PersonalizedProgram(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("template_id") val templateId: String,
    @SerialName("template_name") val templateName: String,

    // Start configuration (user's PRs at program start)
    @SerialName("starting_squat_kg") val startingSquatKg: Float,
    @SerialName("starting_bench_kg") val startingBenchKg: Float,
    @SerialName("starting_deadlift_kg") val startingDeadliftKg: Float,

    // Progress tracking
    @SerialName("current_week") val currentWeek: Int = 1,
    @SerialName("current_day") val currentDay: Int = 1,
    @SerialName("current_phase") val currentPhase: String,

    // Status
    val status: PersonalizedProgramStatus = PersonalizedProgramStatus.ACTIVE,
    @SerialName("started_at") val startedAt: String,
    @SerialName("completed_at") val completedAt: String? = null,

    // Workouts (generated from template)
    val workouts: List<PersonalizedWorkout> = emptyList(),

    // Milestones
    val milestones: List<ProgramMilestone> = emptyList()
)

@Serializable
enum class PersonalizedProgramStatus {
    @SerialName("active") ACTIVE,
    @SerialName("paused") PAUSED,
    @SerialName("completed") COMPLETED,
    @SerialName("abandoned") ABANDONED
}

/**
 * Personalized Workout - A single workout with calculated weights
 */
@Serializable
data class PersonalizedWorkout(
    val id: String,
    @SerialName("program_id") val programId: String,

    // Scheduling
    @SerialName("week_number") val weekNumber: Int,
    @SerialName("day_number") val dayNumber: Int,
    val name: String,
    @SerialName("phase_name") val phaseName: String,

    // Exercises with calculated weights
    val exercises: List<PersonalizedExercise>,

    // Status
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("session_id") val sessionId: String? = null // Link to workout_sessions
)

/**
 * Personalized Exercise - Exercise with weight calculated from user's PRs
 */
@Serializable
data class PersonalizedExercise(
    val id: String,
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("is_main_lift") val isMainLift: Boolean = false,
    @SerialName("lift_type") val liftType: LiftType? = null,

    // Prescribed values
    val sets: Int,
    @SerialName("target_reps") val targetReps: Int,
    val percentage: Float? = null, // e.g., 0.75
    @SerialName("calculated_weight_kg") val calculatedWeightKg: Float? = null,
    @SerialName("rpe_target") val rpeTarget: Float? = null,
    @SerialName("velocity_target_ms") val velocityTargetMs: Float? = null,

    // Actual performance (filled after workout)
    @SerialName("actual_sets") val actualSets: List<PerformedSet>? = null,

    // Notes
    val notes: String? = null
)

/**
 * Performed Set - Actual set data from workout
 */
@Serializable
data class PerformedSet(
    @SerialName("set_number") val setNumber: Int,
    val reps: Int,
    @SerialName("weight_kg") val weightKg: Float,
    @SerialName("velocity_ms") val velocityMs: Float? = null,
    val rpe: Float? = null
)

/**
 * Program Milestone - Progress markers within a program
 */
@Serializable
data class ProgramMilestone(
    val week: Int,
    val type: MilestoneType,
    val description: String,
    @SerialName("target_value") val targetValue: Float? = null,
    @SerialName("is_reached") val isReached: Boolean = false
)

@Serializable
enum class MilestoneType {
    @SerialName("phase_complete") PHASE_COMPLETE,
    @SerialName("volume_pr") VOLUME_PR,
    @SerialName("intensity_pr") INTENSITY_PR,
    @SerialName("program_complete") PROGRAM_COMPLETE,
    @SerialName("deload") DELOAD
}

// =====================================================
// PROGRAM GENERATION
// =====================================================

/**
 * Program Generator - Creates personalized programs from strength templates
 */
object StrengthProgramGenerator {

    /**
     * Generate a personalized program from a template using user's PRs
     */
    fun generateProgram(
        template: StrengthProgramTemplate,
        profile: UserStrengthProfile,
        startDate: String = java.time.LocalDate.now().toString()
    ): PersonalizedProgram {
        val workouts = mutableListOf<PersonalizedWorkout>()
        var workoutIdCounter = 0

        val phases: List<StrengthProgramPhase> = template.phases
        phases.forEachIndexed { phaseIndex, phase ->
            val weekTemplates: List<WeekTemplate> = phase.weekTemplates
            weekTemplates.forEach { weekTemplate ->
                val previousPhases: List<StrengthProgramPhase> = phases.take(phaseIndex)
                val absoluteWeek: Int = previousPhases.sumOf { p: StrengthProgramPhase -> p.weeks } + weekTemplate.weekNumber

                val days: List<DayTemplate> = weekTemplate.days
                days.forEach { dayTemplate ->
                    val exercises: List<ExerciseTemplate> = dayTemplate.exercises
                    val personalizedExercises: List<PersonalizedExercise> = exercises.map { exerciseTemplate: ExerciseTemplate ->
                        generatePersonalizedExercise(
                            template = exerciseTemplate,
                            profile = profile,
                            weekNumber = absoluteWeek,
                            progressionScheme = template.progressionScheme
                        )
                    }

                    workouts.add(
                        PersonalizedWorkout(
                            id = "workout_${++workoutIdCounter}",
                            programId = "", // Will be set when saved
                            weekNumber = absoluteWeek,
                            dayNumber = dayTemplate.dayNumber,
                            name = dayTemplate.name,
                            phaseName = phase.name,
                            exercises = personalizedExercises
                        )
                    )
                }
            }
        }

        return PersonalizedProgram(
            id = java.util.UUID.randomUUID().toString(),
            userId = profile.userId,
            templateId = template.id,
            templateName = template.name,
            startingSquatKg = profile.currentSquatKg,
            startingBenchKg = profile.currentBenchKg,
            startingDeadliftKg = profile.currentDeadliftKg,
            currentPhase = phases.firstOrNull()?.name ?: "Week 1",
            startedAt = startDate,
            workouts = workouts,
            milestones = generateMilestones(template)
        )
    }

    private fun generatePersonalizedExercise(
        template: ExerciseTemplate,
        profile: UserStrengthProfile,
        weekNumber: Int,
        progressionScheme: ProgressionScheme
    ): PersonalizedExercise {
        val targetReps = template.repRange.first + (template.repRange.last - template.repRange.first) / 2

        // Calculate percentage and weight for main lifts
        val (percentage, calculatedWeight) = if (template.isMainLift && template.liftType != null) {
            val basePercentage = template.percentageRange?.let {
                it.start + (it.endInclusive - it.start) / 2
            } ?: 0.75f

            // Apply weekly progression
            val progressedPercentage = when (progressionScheme.type) {
                ProgressionType.LINEAR -> basePercentage
                ProgressionType.PERCENTAGE_WAVE -> basePercentage + ((weekNumber - 1) * (progressionScheme.weeklyPercentageIncrease ?: 0.02f))
                else -> basePercentage
            }

            val weight = profile.calculateWorkingWeight(template.liftType, progressedPercentage)
            Pair(progressedPercentage, weight)
        } else {
            Pair(null, null)
        }

        // Get velocity target if VBT enabled
        val velocityTarget = if (progressionScheme.useVBT && percentage != null) {
            profile.getExpectedVelocity(percentage)
        } else {
            template.velocityTarget
        }

        return PersonalizedExercise(
            id = java.util.UUID.randomUUID().toString(),
            exerciseName = template.exerciseName,
            isMainLift = template.isMainLift,
            liftType = template.liftType,
            sets = template.sets,
            targetReps = targetReps,
            percentage = percentage,
            calculatedWeightKg = calculatedWeight,
            rpeTarget = template.rpeTarget,
            velocityTargetMs = velocityTarget,
            notes = template.notes
        )
    }

    private fun generateMilestones(template: StrengthProgramTemplate): List<ProgramMilestone> {
        val milestones = mutableListOf<ProgramMilestone>()
        var currentWeek = 0

        template.phases.forEachIndexed { index, phase ->
            currentWeek += phase.weeks
            milestones.add(
                ProgramMilestone(
                    week = currentWeek,
                    type = if (index == template.phases.lastIndex) MilestoneType.PROGRAM_COMPLETE else MilestoneType.PHASE_COMPLETE,
                    description = if (index == template.phases.lastIndex) "Program Complete!" else "${phase.name} Complete"
                )
            )
        }

        return milestones
    }
}

// =====================================================
// SAMPLE TEMPLATES
// =====================================================

/**
 * Pre-built strength program templates
 *
 * Templates will be loaded from JSON/Supabase in production.
 * This object provides helper methods for creating and managing templates.
 */
object StrengthProgramTemplates {

    /**
     * Get all available templates (will be loaded from database in production)
     */
    fun getAllTemplates(): List<StrengthProgramTemplate> = emptyList()

    /**
     * Get templates recommended for a user's experience level
     */
    fun getRecommendedTemplates(experienceLevel: StrengthExperienceLevel): List<StrengthProgramTemplate> {
        return getAllTemplates().filter { template ->
            experienceLevel in template.targetLevels
        }
    }
}
// screens/wod/WodToWorkoutConverter.kt
package com.example.menotracker.screens.wod

import android.util.Log
import com.example.menotracker.data.ExerciseRepository
import com.example.menotracker.data.ExerciseSet
import com.example.menotracker.data.ExerciseWithSets
import com.example.menotracker.data.models.WodMovement
import com.example.menotracker.data.models.WodTemplate
import com.example.menotracker.data.models.WodWithMovements
import com.example.menotracker.viewmodels.WorkoutTemplate
import com.example.menotracker.viewmodels.WodTimerConfig
import java.util.UUID

/**
 * Converts CrossFit WODs to executable WorkoutTemplates
 *
 * This enables the "Scan & Start" workflow:
 * 1. User scans whiteboard → WOD is parsed
 * 2. WOD movements are mapped to exercises in the database
 * 3. Sets are generated based on WOD type (AMRAP, EMOM, For Time, etc.)
 * 4. User can immediately start the workout
 */
object WodToWorkoutConverter {

    private const val TAG = "WodConverter"

    /**
     * CrossFit movement name aliases → canonical exercise names in our database
     * This handles the many variations in how movements are written on whiteboards
     */
    private val MOVEMENT_ALIASES = mapOf(
        // Olympic Lifts
        "clean" to "Power Clean",
        "power clean" to "Power Clean",
        "squat clean" to "Squat Clean",
        "clean and jerk" to "Clean and Jerk",
        "c&j" to "Clean and Jerk",
        "snatch" to "Snatch",
        "power snatch" to "Power Snatch",
        "squat snatch" to "Squat Snatch",
        "hang clean" to "Hang Power Clean",
        "hang snatch" to "Hang Snatch",

        // Squats
        "back squat" to "Back Squat",
        "bs" to "Back Squat",
        "front squat" to "Front Squat",
        "fs" to "Front Squat",
        "air squat" to "Air Squat",
        "squat" to "Air Squat",
        "goblet squat" to "Goblet Squat",
        "overhead squat" to "Overhead Squat",
        "ohs" to "Overhead Squat",
        "pistol" to "Pistol Squat",
        "pistol squat" to "Pistol Squat",
        "pistols" to "Pistol Squat",

        // Deadlifts
        "deadlift" to "Deadlift",
        "dl" to "Deadlift",
        "sumo deadlift" to "Sumo Deadlift",
        "romanian deadlift" to "Romanian Deadlift",
        "rdl" to "Romanian Deadlift",
        "sumo deadlift high pull" to "Sumo Deadlift High Pull",
        "sdhp" to "Sumo Deadlift High Pull",

        // Pressing
        "shoulder press" to "Shoulder Press",
        "strict press" to "Shoulder Press",
        "press" to "Shoulder Press",
        "push press" to "Push Press",
        "pp" to "Push Press",
        "push jerk" to "Push Jerk",
        "pj" to "Push Jerk",
        "split jerk" to "Split Jerk",
        "jerk" to "Push Jerk",
        "thruster" to "Thruster",
        "thrusters" to "Thruster",
        "dumbbell thruster" to "Dumbbell Thruster",
        "db thruster" to "Dumbbell Thruster",
        "bench press" to "Bench Press",
        "bp" to "Bench Press",

        // Pull Movements
        "pull-up" to "Pull Up",
        "pull up" to "Pull Up",
        "pullup" to "Pull Up",
        "pull-ups" to "Pull Up",
        "pullups" to "Pull Up",
        "strict pull-up" to "Strict Pull Up",
        "strict pullup" to "Strict Pull Up",
        "kipping pull-up" to "Kipping Pull Up",
        "butterfly pull-up" to "Butterfly Pull Up",
        "chest to bar" to "Chest to Bar Pull Up",
        "c2b" to "Chest to Bar Pull Up",
        "ctb" to "Chest to Bar Pull Up",
        "chin-up" to "Chin Up",
        "chin up" to "Chin Up",
        "muscle-up" to "Muscle Up",
        "muscle up" to "Muscle Up",
        "bar muscle-up" to "Bar Muscle Up",
        "bar mu" to "Bar Muscle Up",
        "ring muscle-up" to "Ring Muscle Up",
        "ring mu" to "Ring Muscle Up",
        "mu" to "Muscle Up",

        // Push Movements
        "push-up" to "Push Up",
        "push up" to "Push Up",
        "pushup" to "Push Up",
        "push-ups" to "Push Up",
        "pushups" to "Push Up",
        "hspu" to "Handstand Push Up",
        "handstand push-up" to "Handstand Push Up",
        "handstand push up" to "Handstand Push Up",
        "strict hspu" to "Strict Handstand Push Up",
        "kipping hspu" to "Kipping Handstand Push Up",
        "dip" to "Ring Dip",
        "dips" to "Ring Dip",
        "ring dip" to "Ring Dip",
        "ring dips" to "Ring Dip",

        // Core
        "sit-up" to "Sit Up",
        "sit up" to "Sit Up",
        "situp" to "Sit Up",
        "sit-ups" to "Sit Up",
        "ghd sit-up" to "GHD Sit Up",
        "ghd sit up" to "GHD Sit Up",
        "ghd" to "GHD Sit Up",
        "toes to bar" to "Toes to Bar",
        "t2b" to "Toes to Bar",
        "ttb" to "Toes to Bar",
        "knees to elbow" to "Knees to Elbow",
        "k2e" to "Knees to Elbow",
        "v-up" to "V Up",
        "v up" to "V Up",
        "plank" to "Plank",
        "l-sit" to "L-Sit",
        "l sit" to "L-Sit",

        // Rowing
        "row" to "Rowing",
        "rowing" to "Rowing",
        "cal row" to "Rowing",
        "calorie row" to "Rowing",
        "row calories" to "Rowing",

        // Bike
        "bike" to "Assault Bike",
        "assault bike" to "Assault Bike",
        "echo bike" to "Echo Bike",
        "air bike" to "Assault Bike",
        "cal bike" to "Assault Bike",
        "calorie bike" to "Assault Bike",
        "bike calories" to "Assault Bike",
        "airdyne" to "Assault Bike",

        // Running/Cardio
        "run" to "Running",
        "running" to "Running",
        "sprint" to "Sprint",
        "400m run" to "Running",
        "800m run" to "Running",
        "200m run" to "Running",
        "ski" to "Ski Erg",
        "ski erg" to "Ski Erg",
        "skierg" to "Ski Erg",

        // Jumping
        "box jump" to "Box Jump",
        "box jumps" to "Box Jump",
        "bj" to "Box Jump",
        "box jump over" to "Box Jump Over",
        "bjo" to "Box Jump Over",
        "step up" to "Box Step Up",
        "step-up" to "Box Step Up",
        "box step up" to "Box Step Up",
        "burpee" to "Burpee",
        "burpees" to "Burpee",
        "burpee box jump" to "Burpee Box Jump Over",
        "burpee box jump over" to "Burpee Box Jump Over",
        "double under" to "Double Under",
        "double unders" to "Double Under",
        "du" to "Double Under",
        "dus" to "Double Under",
        "single under" to "Single Under",
        "single unders" to "Single Under",
        "su" to "Single Under",
        "jump rope" to "Single Under",

        // Kettlebell
        "kettlebell swing" to "Kettlebell Swing",
        "kb swing" to "Kettlebell Swing",
        "russian swing" to "Russian Kettlebell Swing",
        "american swing" to "American Kettlebell Swing",
        "kettlebell snatch" to "Kettlebell Snatch",
        "kb snatch" to "Kettlebell Snatch",
        "turkish get up" to "Turkish Get Up",
        "tgu" to "Turkish Get Up",
        "goblet squat" to "Goblet Squat",

        // Dumbbell
        "dumbbell snatch" to "Dumbbell Snatch",
        "db snatch" to "Dumbbell Snatch",
        "dumbbell clean" to "Dumbbell Clean",
        "db clean" to "Dumbbell Clean",
        "devil press" to "Devil Press",
        "man maker" to "Man Maker",
        "manmaker" to "Man Maker",

        // Barbell Movements
        "ground to overhead" to "Ground to Overhead",
        "g2o" to "Ground to Overhead",
        "gto" to "Ground to Overhead",
        "shoulder to overhead" to "Shoulder to Overhead",
        "s2o" to "Shoulder to Overhead",
        "sto" to "Shoulder to Overhead",

        // Lunges
        "lunge" to "Walking Lunge",
        "lunges" to "Walking Lunge",
        "walking lunge" to "Walking Lunge",
        "walking lunges" to "Walking Lunge",
        "overhead lunge" to "Overhead Lunge",
        "oh lunge" to "Overhead Lunge",
        "front rack lunge" to "Front Rack Lunge",

        // Wall Ball
        "wall ball" to "Wall Ball",
        "wall balls" to "Wall Ball",
        "wb" to "Wall Ball",
        "wall ball shot" to "Wall Ball",

        // Rope Climb
        "rope climb" to "Rope Climb",
        "rope climbs" to "Rope Climb",
        "rc" to "Rope Climb",
        "legless rope climb" to "Legless Rope Climb",

        // Carries
        "farmer carry" to "Farmer Carry",
        "farmers carry" to "Farmer Carry",
        "farmer walk" to "Farmer Carry",
        "sled push" to "Sled Push",
        "sled pull" to "Sled Pull",
        "yoke carry" to "Yoke Carry",
        "sandbag carry" to "Sandbag Carry",

        // Gymnastics
        "handstand walk" to "Handstand Walk",
        "hs walk" to "Handstand Walk",
        "handstand hold" to "Handstand Hold"
    )

    /**
     * Default muscle groups for CrossFit movements when exercise not found in DB
     */
    private val MOVEMENT_MUSCLE_GROUPS = mapOf(
        "clean" to "Legs",
        "snatch" to "Full Body",
        "squat" to "Legs",
        "deadlift" to "Back",
        "press" to "Shoulders",
        "thruster" to "Full Body",
        "pull" to "Back",
        "push" to "Chest",
        "row" to "Back",
        "run" to "Cardio",
        "bike" to "Cardio",
        "box" to "Legs",
        "burpee" to "Full Body",
        "kettlebell" to "Full Body",
        "wall ball" to "Full Body",
        "lunge" to "Legs",
        "core" to "Core",
        "sit" to "Core",
        "toes" to "Core"
    )

    /**
     * Convert a WOD with movements to a WorkoutTemplate
     */
    fun convertToWorkout(
        wodWithMovements: WodWithMovements,
        scalingLevel: String = "rx",
        isMale: Boolean = true
    ): WorkoutTemplate {
        val wod = wodWithMovements.wod
        val movements = wodWithMovements.movements

        Log.d(TAG, "Converting WOD '${wod.name}' (${wod.wodType}) with ${movements.size} movements")

        val exercises = movements.mapIndexed { index, movement ->
            convertMovementToExercise(
                movement = movement,
                order = index,
                wodType = wod.wodType,
                scalingLevel = scalingLevel,
                isMale = isMale,
                repScheme = wod.repScheme,
                targetRounds = wod.targetRounds
            )
        }

        // Create timer configuration based on WOD type
        val timerConfig = createTimerConfig(wod)

        return WorkoutTemplate(
            id = UUID.randomUUID().toString(),
            name = "${wod.name} (${wod.getWodTypeDisplay()})",
            exercises = exercises,
            sports = listOf("CrossFit"),
            wodTimerConfig = timerConfig,
            isWod = true
        )
    }

    /**
     * Convert a single WOD movement to ExerciseWithSets
     */
    private fun convertMovementToExercise(
        movement: WodMovement,
        order: Int,
        wodType: String,
        scalingLevel: String,
        isMale: Boolean,
        repScheme: List<Int>?,
        targetRounds: Int?
    ): ExerciseWithSets {
        // Try to find exercise in database
        val (exerciseId, exerciseName, muscleGroup, equipment) = findExerciseInDatabase(movement)

        // Generate sets based on WOD type
        val sets = generateSetsForWodType(
            movement = movement,
            wodType = wodType,
            isMale = isMale,
            repScheme = repScheme,
            targetRounds = targetRounds
        )

        // Build notes from movement details
        val notes = buildMovementNotes(movement, isMale)

        return ExerciseWithSets(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            muscleGroup = muscleGroup,
            equipment = equipment,
            order = order,
            sets = sets,
            notes = notes
        )
    }

    /**
     * Search exercises in the local cache
     */
    private fun searchExercisesLocal(query: String): List<com.example.menotracker.data.models.Exercise> {
        val lowerQuery = query.lowercase()
        return ExerciseRepository.createdExercises.filter { exercise ->
            exercise.name.lowercase().contains(lowerQuery)
        }
    }

    /**
     * Find the best matching exercise in our database
     */
    private fun findExerciseInDatabase(movement: WodMovement): ExerciseMatch {
        val movementName = movement.movementName.lowercase().trim()

        // 1. Check aliases first
        val canonicalName = MOVEMENT_ALIASES[movementName]
        if (canonicalName != null) {
            val exercise = searchExercisesLocal(canonicalName).firstOrNull()
            if (exercise != null) {
                Log.d(TAG, "Found exercise via alias: '$movementName' → '${exercise.name}'")
                return ExerciseMatch(
                    id = exercise.id,
                    name = exercise.name,
                    muscleGroup = exercise.mainMuscle ?: "Full Body",
                    equipment = exercise.equipment?.firstOrNull() ?: "Bodyweight"
                )
            }
        }

        // 2. Direct search in database
        val directMatch = searchExercisesLocal(movementName).firstOrNull()
        if (directMatch != null) {
            Log.d(TAG, "Found exercise via direct search: '${directMatch.name}'")
            return ExerciseMatch(
                id = directMatch.id,
                name = directMatch.name,
                muscleGroup = directMatch.mainMuscle ?: "Full Body",
                equipment = directMatch.equipment?.firstOrNull() ?: "Bodyweight"
            )
        }

        // 3. Fuzzy search - try partial matches
        val words = movementName.split(" ", "-", "_")
        for (word in words) {
            if (word.length >= 3) {
                val fuzzyMatch = searchExercisesLocal(word).firstOrNull { it.name.lowercase().contains(word) }
                if (fuzzyMatch != null) {
                    Log.d(TAG, "Found exercise via fuzzy search: '$word' → '${fuzzyMatch.name}'")
                    return ExerciseMatch(
                        id = fuzzyMatch.id,
                        name = fuzzyMatch.name,
                        muscleGroup = fuzzyMatch.mainMuscle ?: "Full Body",
                        equipment = fuzzyMatch.equipment?.firstOrNull() ?: "Bodyweight"
                    )
                }
            }
        }

        // 4. Fallback - create placeholder with guessed muscle group
        val guessedMuscleGroup = guessMuscleGroup(movementName)
        val guessedEquipment = guessEquipment(movement)

        Log.d(TAG, "No exercise found for '$movementName', using placeholder")
        return ExerciseMatch(
            id = "wod_${UUID.randomUUID().toString().take(8)}",
            name = movement.movementName, // Keep original name
            muscleGroup = guessedMuscleGroup,
            equipment = guessedEquipment
        )
    }

    /**
     * Generate sets based on WOD type
     */
    private fun generateSetsForWodType(
        movement: WodMovement,
        wodType: String,
        isMale: Boolean,
        repScheme: List<Int>?,
        targetRounds: Int?
    ): List<ExerciseSet> {
        val reps = getRepsForMovement(movement, isMale)
        val weight = getWeightForMovement(movement, isMale)

        return when (wodType) {
            "amrap" -> {
                // AMRAP: Create a single set representing the movement
                // User will do as many rounds as possible
                listOf(
                    ExerciseSet(
                        setNumber = 1,
                        targetReps = reps,
                        targetWeight = weight,
                        restSeconds = 0 // No rest in AMRAP
                    )
                )
            }

            "emom" -> {
                // EMOM: Each movement is done once per minute
                // Create sets based on EMOM duration (default 10 minutes if not specified)
                val minutes = movement.emomMinutes?.size ?: 10
                (1..minutes).map { minute ->
                    ExerciseSet(
                        setNumber = minute,
                        targetReps = reps,
                        targetWeight = weight,
                        restSeconds = 60 // Rest is "remaining time in minute"
                    )
                }
            }

            "for_time", "rft" -> {
                // For Time / Rounds For Time
                if (repScheme != null && repScheme.isNotEmpty()) {
                    // 21-15-9 style - create a set for each rep scheme
                    repScheme.mapIndexed { index, schemeReps ->
                        ExerciseSet(
                            setNumber = index + 1,
                            targetReps = schemeReps,
                            targetWeight = weight,
                            restSeconds = 0 // No programmed rest
                        )
                    }
                } else if (targetRounds != null && targetRounds > 0) {
                    // Fixed rounds - create a set for each round
                    (1..targetRounds).map { round ->
                        ExerciseSet(
                            setNumber = round,
                            targetReps = reps,
                            targetWeight = weight,
                            restSeconds = 0
                        )
                    }
                } else {
                    // Single through - one set
                    listOf(
                        ExerciseSet(
                            setNumber = 1,
                            targetReps = reps,
                            targetWeight = weight,
                            restSeconds = 0
                        )
                    )
                }
            }

            "chipper" -> {
                // Chipper: One set per movement, go through once
                listOf(
                    ExerciseSet(
                        setNumber = 1,
                        targetReps = reps,
                        targetWeight = weight,
                        restSeconds = 0
                    )
                )
            }

            "tabata" -> {
                // Tabata: 8 rounds of 20 sec work / 10 sec rest
                (1..8).map { round ->
                    ExerciseSet(
                        setNumber = round,
                        targetReps = reps, // Max reps in 20 sec
                        targetWeight = weight,
                        restSeconds = 10
                    )
                }
            }

            "ladder", "death_by" -> {
                // Ladder/Death By: Increasing reps each round
                // Start at 1, increment by 1 (or movement reps) each minute
                (1..15).map { round -> // Default 15 rounds max
                    ExerciseSet(
                        setNumber = round,
                        targetReps = round * (reps.coerceAtLeast(1)),
                        targetWeight = weight,
                        restSeconds = 60 // Rest is remaining time in minute
                    )
                }
            }

            else -> {
                // Default: 3 sets
                (1..3).map { setNum ->
                    ExerciseSet(
                        setNumber = setNum,
                        targetReps = reps,
                        targetWeight = weight,
                        restSeconds = 60
                    )
                }
            }
        }
    }

    /**
     * Get reps for a movement based on rep type and gender
     */
    private fun getRepsForMovement(movement: WodMovement, isMale: Boolean): Int {
        return when (movement.repType) {
            "fixed" -> {
                val genderReps = if (isMale) movement.repsMale else movement.repsFemale
                genderReps ?: movement.reps ?: 10
            }
            "calories" -> movement.calories ?: 15
            "distance" -> {
                // Convert distance to "reps" (e.g., 400m = 400)
                movement.distanceMeters ?: 400
            }
            "time" -> {
                // Convert seconds to a display value
                movement.timeSeconds ?: 60
            }
            "max" -> 0 // Max reps = no target
            else -> movement.reps ?: 10
        }
    }

    /**
     * Get weight for a movement based on weight type and gender
     */
    private fun getWeightForMovement(movement: WodMovement, isMale: Boolean): Double {
        return when (movement.weightType) {
            "fixed" -> {
                val genderWeight = if (isMale) movement.weightKgMale else movement.weightKgFemale
                genderWeight ?: 0.0
            }
            "percentage" -> {
                // Would need user's 1RM - default to 0 for now
                0.0
            }
            else -> 0.0 // Bodyweight
        }
    }

    /**
     * Build notes string from movement details
     */
    private fun buildMovementNotes(movement: WodMovement, isMale: Boolean): String {
        val parts = mutableListOf<String>()

        // Add rep type info
        when (movement.repType) {
            "calories" -> parts.add("${movement.calories ?: 0} cal")
            "distance" -> parts.add("${movement.distanceMeters ?: 0}m")
            "time" -> {
                val secs = movement.timeSeconds ?: 0
                if (secs >= 60) parts.add("${secs / 60} min")
                else parts.add("$secs sec")
            }
        }

        // Add weight info
        if (movement.weightType != "bodyweight") {
            val weight = if (isMale) movement.weightKgMale else movement.weightKgFemale
            weight?.let { parts.add("@ ${it}kg") }
        }

        // Add any notes
        movement.notes?.let { parts.add(it) }
        movement.movementDescription?.let { parts.add(it) }

        return parts.joinToString(" • ")
    }

    /**
     * Guess muscle group from movement name
     */
    private fun guessMuscleGroup(movementName: String): String {
        val lower = movementName.lowercase()

        for ((keyword, muscleGroup) in MOVEMENT_MUSCLE_GROUPS) {
            if (lower.contains(keyword)) {
                return muscleGroup
            }
        }

        return "Full Body"
    }

    /**
     * Guess equipment from movement
     */
    private fun guessEquipment(movement: WodMovement): String {
        val name = movement.movementName.lowercase()

        return when {
            name.contains("barbell") || name.contains("clean") || name.contains("snatch") ||
            name.contains("deadlift") || name.contains("squat") && movement.weightKgMale != null -> "Barbell"

            name.contains("dumbbell") || name.contains("db ") -> "Dumbbell"
            name.contains("kettlebell") || name.contains("kb ") -> "Kettlebell"
            name.contains("row") && !name.contains("ring") -> "Rowing Machine"
            name.contains("bike") -> "Assault Bike"
            name.contains("ski") -> "Ski Erg"
            name.contains("ring") -> "Rings"
            name.contains("rope") && name.contains("climb") -> "Rope"
            name.contains("box") -> "Box"
            name.contains("wall ball") -> "Medicine Ball"
            name.contains("pull-up") || name.contains("pullup") -> "Pull Up Bar"
            else -> "Bodyweight"
        }
    }

    /**
     * Create timer configuration based on WOD type
     */
    private fun createTimerConfig(wod: WodTemplate): WodTimerConfig {
        return when (wod.wodType) {
            "amrap" -> WodTimerConfig(
                wodType = "amrap",
                timeCapSeconds = wod.timeCapSeconds ?: (12 * 60) // Default 12 min AMRAP
            )

            "emom" -> WodTimerConfig(
                wodType = "emom",
                timeCapSeconds = wod.timeCapSeconds ?: (10 * 60), // Default 10 min EMOM
                emomIntervalSeconds = 60 // 1 min intervals
            )

            "for_time", "rft", "chipper" -> WodTimerConfig(
                wodType = "for_time",
                timeCapSeconds = wod.timeCapSeconds ?: (20 * 60), // Default 20 min cap
                targetRounds = wod.targetRounds
            )

            "tabata" -> WodTimerConfig(
                wodType = "tabata",
                tabataWorkSeconds = 20,
                tabataRestSeconds = 10,
                tabataRounds = 8,
                timeCapSeconds = (8 * 30) // 8 rounds × 30 sec = 4 min
            )

            "ladder", "death_by" -> WodTimerConfig(
                wodType = "death_by",
                emomIntervalSeconds = 60, // Each round is 1 minute
                timeCapSeconds = wod.timeCapSeconds ?: (15 * 60) // Default 15 min
            )

            else -> WodTimerConfig(
                wodType = wod.wodType,
                timeCapSeconds = wod.timeCapSeconds
            )
        }
    }

    /**
     * Data class for exercise match result
     */
    private data class ExerciseMatch(
        val id: String,
        val name: String,
        val muscleGroup: String,
        val equipment: String
    )
}
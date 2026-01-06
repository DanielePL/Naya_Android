// app/src/main/java/com/example/myapplicationtest/screens/library/LibrarySearchEngine.kt

package com.example.menotracker.screens.library

import com.example.menotracker.data.models.Exercise
import com.example.menotracker.data.models.ProgramTemplate
import com.example.menotracker.onboarding.data.UnifiedSport
import com.example.menotracker.viewmodels.WorkoutTemplate

/**
 * Intelligent Search Engine for the Library
 *
 * Features:
 * - Multi-field search (name, muscles, equipment, category)
 * - Relevance ranking with compound movements prioritized
 * - Fuzzy matching for typos
 * - Exercise popularity/importance weighting
 * - SPORT-AWARE: Prioritizes exercises based on user's selected sport
 */
object LibrarySearchEngine {

    // ═══════════════════════════════════════════════════════════════
    // SPORT-AWARE CONTEXT
    // User's primary sport for personalized search ranking
    // ═══════════════════════════════════════════════════════════════

    private var userSport: UnifiedSport? = null
    private var userPrimaryExercises: Set<String> = emptySet()

    /**
     * Set the user's sport for personalized search results
     * Call this when user profile loads or changes
     */
    fun setUserSport(sport: UnifiedSport?) {
        userSport = sport
        userPrimaryExercises = sport?.primaryExercises
            ?.map { it.lowercase() }
            ?.toSet() ?: emptySet()
    }

    /**
     * Check if an exercise matches user's sport primary exercises
     */
    private fun isUserPrimaryExercise(exerciseName: String): Boolean {
        if (userPrimaryExercises.isEmpty()) return false
        val nameLower = exerciseName.lowercase()
        return userPrimaryExercises.any { primary ->
            nameLower.contains(primary) || primary.contains(nameLower.split(" ").first())
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // COMPOUND MOVEMENTS - Highest priority exercises
    // These are the "main" exercises users search for most often
    // ═══════════════════════════════════════════════════════════════

    private val compoundMovements = setOf(
        // Squat family
        "squat", "back squat", "front squat", "high bar squat", "low bar squat",
        // Bench family
        "bench press", "bench", "barbell bench press", "flat bench press",
        // Deadlift family
        "deadlift", "conventional deadlift", "sumo deadlift",
        // Press family
        "overhead press", "ohp", "military press", "shoulder press", "strict press",
        // Row family
        "barbell row", "bent over row", "pendlay row",
        // Pull family
        "pull-up", "pull up", "pullup", "chin-up", "chin up", "chinup",
        // Olympic lifts
        "clean", "snatch", "clean and jerk", "power clean", "hang clean",
        // Other compounds
        "dip", "dips", "weighted dip"
    )

    // ═══════════════════════════════════════════════════════════════
    // TRANSITION/ACCESSORY MOVEMENTS - Medium priority
    // Variations that are commonly used but not "main" exercises
    // ═══════════════════════════════════════════════════════════════

    private val transitionMovements = setOf(
        // Squat variations
        "pause squat", "box squat", "pin squat", "tempo squat", "goblet squat",
        // Bench variations
        "close grip bench", "incline bench", "decline bench", "pause bench", "spoto press",
        // Deadlift variations
        "romanian deadlift", "rdl", "stiff leg deadlift", "deficit deadlift", "block pull",
        // Press variations
        "push press", "seated press", "db shoulder press", "arnold press",
        // Row variations
        "cable row", "seated row", "t-bar row", "dumbbell row", "one arm row",
        // Pull variations
        "lat pulldown", "assisted pull-up",
        // Leg accessories
        "leg press", "hack squat", "lunge", "split squat", "bulgarian split squat",
        // Common accessories
        "face pull", "lateral raise", "bicep curl", "tricep extension", "skull crusher"
    )

    // ═══════════════════════════════════════════════════════════════
    // SPECIALTY MOVEMENTS - Lower priority
    // Advanced/specialized exercises
    // ═══════════════════════════════════════════════════════════════

    private val specialtyMovements = setOf(
        // Specialty squats
        "zercher squat", "ssb squat", "safety bar squat", "belt squat", "hatfield squat",
        "anderson squat", "breathing squat",
        // Specialty bench
        "floor press", "board press", "slingshot bench", "reverse grip bench",
        // Specialty deadlift
        "jefferson deadlift", "trap bar deadlift", "snatch grip deadlift", "rack pull",
        // Olympic variations
        "muscle snatch", "snatch pull", "clean pull", "hang snatch", "block clean"
    )

    // ═══════════════════════════════════════════════════════════════
    // COMMON TYPOS & SYNONYMS
    // Maps common misspellings/alternatives to correct terms
    // ═══════════════════════════════════════════════════════════════

    private val synonyms = mapOf(
        // German -> English
        "kniebeuge" to listOf("squat"),
        "bankdrücken" to listOf("bench press"),
        "kreuzheben" to listOf("deadlift"),
        "schulterdrücken" to listOf("overhead press", "shoulder press"),
        "rudern" to listOf("row"),
        "klimmzug" to listOf("pull-up"),
        "klimmzüge" to listOf("pull-up"),
        "liegestütze" to listOf("push-up"),
        "ausfallschritt" to listOf("lunge"),
        "beinpresse" to listOf("leg press"),
        "bizeps" to listOf("bicep", "curl"),
        "trizeps" to listOf("tricep", "extension"),
        "brust" to listOf("chest", "bench", "fly"),
        "rücken" to listOf("back", "row", "pull"),
        "schulter" to listOf("shoulder", "press", "raise"),
        "beine" to listOf("leg", "squat", "lunge"),

        // Common abbreviations
        "rdl" to listOf("romanian deadlift"),
        "ohp" to listOf("overhead press"),
        "cgbp" to listOf("close grip bench press"),
        "sldl" to listOf("stiff leg deadlift"),
        "bss" to listOf("bulgarian split squat"),
        "db" to listOf("dumbbell"),
        "bb" to listOf("barbell"),
        "ssb" to listOf("safety squat bar", "safety bar squat"),

        // Common typos
        "benchpress" to listOf("bench press"),
        "deadlifts" to listOf("deadlift"),
        "squats" to listOf("squat"),
        "pullups" to listOf("pull-up"),
        "chinups" to listOf("chin-up"),
        "pushups" to listOf("push-up"),
        "dumbell" to listOf("dumbbell"),
        "barbell" to listOf("barbell")
    )

    // ═══════════════════════════════════════════════════════════════
    // MUSCLE GROUP MAPPINGS
    // Maps muscle names to exercises that target them
    // ═══════════════════════════════════════════════════════════════

    private val muscleKeywords = mapOf(
        "chest" to listOf("bench", "fly", "press", "push-up", "dip"),
        "back" to listOf("row", "pull", "deadlift", "lat"),
        "legs" to listOf("squat", "leg", "lunge", "calf"),
        "shoulders" to listOf("press", "raise", "shrug", "face pull"),
        "biceps" to listOf("curl", "chin-up", "row"),
        "triceps" to listOf("extension", "dip", "press", "skull crusher"),
        "core" to listOf("plank", "crunch", "ab", "deadlift", "squat"),
        "glutes" to listOf("hip thrust", "deadlift", "squat", "lunge"),
        "hamstrings" to listOf("deadlift", "rdl", "leg curl", "good morning"),
        "quads" to listOf("squat", "leg press", "lunge", "leg extension")
    )

    // ═══════════════════════════════════════════════════════════════
    // MAIN SEARCH FUNCTION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sort exercises by importance (compound > transition > specialty > isolation)
     * Use this when displaying exercises WITHOUT a search query
     *
     * @param exercises List of exercises to sort
     * @return Sorted list with most important exercises first
     */
    fun sortByImportance(exercises: List<Exercise>): List<Exercise> {
        return exercises.sortedByDescending { exercise ->
            calculateImportanceScore(exercise)
        }
    }

    /**
     * Calculate importance score for an exercise (used for default sorting)
     * Higher score = more important/popular exercise
     */
    private fun calculateImportanceScore(exercise: Exercise): Int {
        var score = 0
        val nameLower = exercise.name.lowercase()
        val nameWords = nameLower.split(" ", "-", "_", "(", ")").filter { it.isNotBlank() }

        // Check if this is a PRIMARY compound movement
        // e.g., "Back Squat", "Squat (Barbell)", "Front Squat", "Bench Press"
        // NOT: "Squat Jump to Tuck Jump" (contains squat but is a jump exercise)
        val isCompound = isCompoundMovement(nameLower, nameWords)
        val isTransition = isTransitionMovement(nameLower, nameWords)
        val isSpecialty = isSpecialtyMovement(nameLower)

        when {
            isCompound -> score += 100
            isTransition -> score += 50
            isSpecialty -> score += 25
            else -> score += 10
        }

        // Bonus for exercises with video tutorials
        if (!exercise.videoUrl.isNullOrBlank()) {
            score += 5
        }

        // Bonus for exercises with technique guides
        if (!exercise.techniqueSections.isNullOrEmpty()) {
            score += 5
        }

        // Bonus for VBT-enabled exercises
        if (exercise.vbtEnabled == true) {
            score += 3
        }

        return score
    }

    /**
     * Check if exercise is a PRIMARY compound movement
     * Must be the main exercise, not a variation or accessory that contains the word
     */
    private fun isCompoundMovement(nameLower: String, nameWords: List<String>): Boolean {
        // Exclude exercises that contain compound words but are actually different exercises
        val excludePatterns = listOf(
            "jump", "tuck", "hop", "throw", "walk", "carry", "hold",
            "plank", "crunch", "raise", "curl", "extension", "fly",
            "kickback", "pushdown", "pullover"
        )

        // If name contains exclusion patterns, it's probably not a compound
        if (excludePatterns.any { nameLower.contains(it) }) {
            return false
        }

        // Check if the exercise name primarily matches a compound movement
        return compoundMovements.any { compound ->
            val compoundWords = compound.split(" ").filter { it.isNotBlank() }

            // Exact match
            nameLower == compound ||
            // Name starts with compound: "Squat (Barbell)", "Bench Press Close Grip"
            nameLower.startsWith("$compound ") ||
            nameLower.startsWith("$compound(") ||
            // Name ends with compound: "Back Squat", "Incline Bench Press"
            nameLower.endsWith(" $compound") ||
            // All compound words appear AND it's a short name (not "Squat Jump to Tuck Jump")
            (compoundWords.all { word -> nameWords.contains(word) } && nameWords.size <= 4) ||
            // Common patterns: "X Squat", "Squat X", "X Bench Press"
            (nameWords.size <= 3 && nameWords.any { it in listOf("squat", "bench", "deadlift", "press", "row", "pull-up", "pullup", "chin-up", "chinup", "dip", "clean", "snatch") })
        }
    }

    /**
     * Check if exercise is a transition/accessory movement
     */
    private fun isTransitionMovement(nameLower: String, nameWords: List<String>): Boolean {
        return transitionMovements.any { transition ->
            val transitionWords = transition.split(" ").filter { it.isNotBlank() }

            nameLower.contains(transition) ||
            transitionWords.all { word -> nameWords.contains(word) }
        }
    }

    /**
     * Check if exercise is a specialty movement
     */
    private fun isSpecialtyMovement(nameLower: String): Boolean {
        return specialtyMovements.any { specialty ->
            nameLower.contains(specialty)
        }
    }

    /**
     * Search exercises with intelligent ranking
     *
     * @param exercises List of all exercises
     * @param query Search query
     * @return Sorted list with most relevant exercises first
     */
    fun searchExercises(exercises: List<Exercise>, query: String): List<Exercise> {
        if (query.isBlank()) return sortByImportance(exercises)

        val normalizedQuery = query.trim().lowercase()
        val queryWords = normalizedQuery.split(" ").filter { it.isNotBlank() }

        // Expand query with synonyms
        val expandedQueries = expandQueryWithSynonyms(normalizedQuery)

        return exercises
            .map { exercise ->
                exercise to calculateExerciseScore(exercise, normalizedQuery, queryWords, expandedQueries)
            }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (exercise, _) -> exercise }
    }

    /**
     * Search workouts with intelligent ranking
     */
    fun searchWorkouts(workouts: List<WorkoutTemplate>, query: String): List<WorkoutTemplate> {
        if (query.isBlank()) return workouts

        val normalizedQuery = query.trim().lowercase()
        val queryWords = normalizedQuery.split(" ").filter { it.isNotBlank() }

        return workouts
            .map { workout ->
                workout to calculateWorkoutScore(workout, normalizedQuery, queryWords)
            }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (workout, _) -> workout }
    }

    /**
     * Search programs with intelligent ranking
     */
    fun searchPrograms(programs: List<ProgramTemplate>, query: String): List<ProgramTemplate> {
        if (query.isBlank()) return programs

        val normalizedQuery = query.trim().lowercase()
        val queryWords = normalizedQuery.split(" ").filter { it.isNotBlank() }

        return programs
            .map { program ->
                program to calculateProgramScore(program, normalizedQuery, queryWords)
            }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (program, _) -> program }
    }

    // ═══════════════════════════════════════════════════════════════
    // SCORING FUNCTIONS
    // ═══════════════════════════════════════════════════════════════

    private fun calculateExerciseScore(
        exercise: Exercise,
        query: String,
        queryWords: List<String>,
        expandedQueries: List<String>
    ): Int {
        var score = 0
        val nameLower = exercise.name.lowercase()
        val nameWords = nameLower.split(" ", "-", "_").filter { it.isNotBlank() }

        // ═══════════════════════════════════════════════════════════
        // NAME MATCHING (highest weight)
        // ═══════════════════════════════════════════════════════════

        // Exact match (100 points)
        if (nameLower == query) {
            score += 100
        }
        // Name starts with query (80 points)
        else if (nameLower.startsWith(query)) {
            score += 80
        }
        // First word matches (70 points) - e.g., "squat" matches "Squat (Back)"
        else if (nameWords.firstOrNull() == query || nameWords.firstOrNull()?.startsWith(query) == true) {
            score += 70
        }
        // Any word starts with query (50 points)
        else if (nameWords.any { it.startsWith(query) }) {
            score += 50
        }
        // Contains query (30 points)
        else if (nameLower.contains(query)) {
            score += 30
        }
        // Match expanded queries (synonyms) (25 points)
        else if (expandedQueries.any { nameLower.contains(it) }) {
            score += 25
        }
        // Multi-word matching
        else {
            val matchedWords = queryWords.count { word ->
                nameLower.contains(word) || expandedQueries.any { it.contains(word) }
            }
            if (matchedWords > 0) {
                score += matchedWords * 15
            }
        }

        // If no name match, check other fields
        if (score == 0) {
            // Check muscle groups
            val muscles = listOfNotNull(
                exercise.muscleCategory,
                exercise.mainMuscle
            ) + (exercise.secondaryMuscles ?: emptyList())

            if (muscles.any { it.lowercase().contains(query) }) {
                score += 20
            }

            // Check equipment
            if (exercise.equipment?.any { it.lowercase().contains(query) } == true) {
                score += 15
            }

            // Check sports
            if (exercise.sports?.any { it.lowercase().contains(query) } == true) {
                score += 10
            }

            // Check muscle keywords
            muscleKeywords.forEach { (muscle, keywords) ->
                if (query.contains(muscle) || muscle.contains(query)) {
                    if (keywords.any { keyword -> nameLower.contains(keyword) }) {
                        score += 15
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════
        // SPORT-SPECIFIC BONUS (user's primary exercises get priority)
        // ═══════════════════════════════════════════════════════════

        if (score > 0 && isUserPrimaryExercise(exercise.name)) {
            score += 75  // Big bonus for exercises matching user's sport
        }

        // ═══════════════════════════════════════════════════════════
        // IMPORTANCE BONUS (compound movements get priority)
        // ═══════════════════════════════════════════════════════════

        if (score > 0) {
            // Use the same logic as calculateImportanceScore for consistency
            val isCompound = isCompoundMovement(nameLower, nameWords)
            val isTransition = isTransitionMovement(nameLower, nameWords)
            val isSpecialty = isSpecialtyMovement(nameLower)

            when {
                isCompound -> score += 50  // Big compound bonus
                isTransition -> score += 25 // Transition bonus
                isSpecialty -> score += 10  // Small specialty bonus
                // else: no bonus for isolation exercises
            }

            // Bonus for exercises with video tutorials
            if (!exercise.videoUrl.isNullOrBlank()) {
                score += 5
            }

            // Bonus for exercises with technique guides
            if (!exercise.techniqueSections.isNullOrEmpty()) {
                score += 5
            }
        }

        return score
    }

    private fun calculateWorkoutScore(
        workout: WorkoutTemplate,
        query: String,
        queryWords: List<String>
    ): Int {
        var score = 0
        val nameLower = workout.name.lowercase()

        // Exact match
        if (nameLower == query) {
            score += 100
        }
        // Starts with query
        else if (nameLower.startsWith(query)) {
            score += 80
        }
        // Contains query
        else if (nameLower.contains(query)) {
            score += 50
        }
        // Multi-word matching
        else {
            val matchedWords = queryWords.count { word -> nameLower.contains(word) }
            if (matchedWords > 0) {
                score += matchedWords * 20
            }
        }

        // Check exercise names in workout
        val exerciseMatch = workout.exercises.any { ex ->
            ex.exerciseName.lowercase().contains(query)
        }
        if (exerciseMatch && score == 0) {
            score += 25
        }

        // Check muscle groups in exercises
        val muscleMatch = workout.exercises.any { ex ->
            ex.muscleGroup.lowercase().contains(query)
        }
        if (muscleMatch && score == 0) {
            score += 15
        }

        return score
    }

    private fun calculateProgramScore(
        program: ProgramTemplate,
        query: String,
        queryWords: List<String>
    ): Int {
        var score = 0
        val nameLower = program.name.lowercase()

        // Exact match
        if (nameLower == query) {
            score += 100
        }
        // Starts with query
        else if (nameLower.startsWith(query)) {
            score += 80
        }
        // Contains query
        else if (nameLower.contains(query)) {
            score += 50
        }
        // Multi-word matching
        else {
            val matchedWords = queryWords.count { word -> nameLower.contains(word) }
            if (matchedWords > 0) {
                score += matchedWords * 20
            }
        }

        // Check description
        if (program.description?.lowercase()?.contains(query) == true) {
            score += 15
        }

        // Check sport type (using displaySport computed property)
        if (program.displaySport.lowercase().contains(query)) {
            score += 20
        }

        // Check difficulty
        if (program.displayDifficulty.lowercase().contains(query)) {
            score += 10
        }

        // Check goal tags
        if (program.goalTags?.any { it.lowercase().contains(query) } == true) {
            score += 15
        }

        return score
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Expand query with synonyms and common alternatives
     */
    private fun expandQueryWithSynonyms(query: String): List<String> {
        val expanded = mutableListOf(query)

        // Check direct synonym match
        synonyms[query]?.let { expanded.addAll(it) }

        // Check if query is part of a synonym
        synonyms.forEach { (key, values) ->
            if (query.contains(key) || key.contains(query)) {
                expanded.addAll(values)
            }
            if (values.any { it.contains(query) }) {
                expanded.add(key)
                expanded.addAll(values)
            }
        }

        return expanded.distinct()
    }

    /**
     * Simple fuzzy match using Levenshtein distance
     * Returns true if strings are similar enough
     */
    fun fuzzyMatch(s1: String, s2: String, threshold: Int = 2): Boolean {
        if (s1 == s2) return true
        if (kotlin.math.abs(s1.length - s2.length) > threshold) return false

        val distance = levenshteinDistance(s1, s2)
        return distance <= threshold
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length

        if (m == 0) return n
        if (n == 0) return m

        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[m][n]
    }
}
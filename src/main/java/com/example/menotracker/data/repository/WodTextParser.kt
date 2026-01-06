// data/repository/WodTextParser.kt
package com.example.menotracker.data.repository

import android.util.Log
import com.example.menotracker.data.models.ParsedMovement
import com.example.menotracker.data.models.ParsedWod

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * WOD TEXT PARSER - LOCAL PATTERN MATCHING
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Parses WOD text locally using regex patterns and heuristics.
 * No AI required for clearly formatted WODs.
 *
 * Supports:
 * - AMRAP (As Many Rounds As Possible)
 * - For Time
 * - EMOM (Every Minute On the Minute)
 * - Tabata
 * - Chipper
 * - Ladder (ascending/descending)
 *
 * Multi-language: English, German
 */
object WodTextParser {

    private const val TAG = "WodTextParser"

    /**
     * Parse result with confidence score
     */
    data class ParseResult(
        val wod: ParsedWod?,
        val confidence: Float,
        val warnings: List<String> = emptyList(),
        val requiresAIReview: Boolean = false
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // REGEX PATTERNS
    // ═══════════════════════════════════════════════════════════════════════════

    // WOD Type patterns
    private val amrapPattern = Regex(
        """(?i)amrap\s*(\d+)?\s*(min(?:utes?)?)?|(\d+)\s*min(?:ute)?\s*amrap""",
        RegexOption.IGNORE_CASE
    )
    private val forTimePattern = Regex(
        """(?i)for\s*time|auf\s*zeit|so\s*schnell\s*wie\s*möglich""",
        RegexOption.IGNORE_CASE
    )
    private val emomPattern = Regex(
        """(?i)e\.?m\.?o\.?m\.?\s*(\d+)?|every\s*min(?:ute)?\s*(?:on\s*the\s*min(?:ute)?)?""",
        RegexOption.IGNORE_CASE
    )
    private val tabataPattern = Regex(
        """(?i)tabata""",
        RegexOption.IGNORE_CASE
    )
    private val timeCapPattern = Regex(
        """(?i)(?:time\s*cap|cap|limit)[:\s]*(\d+)\s*(?:min(?:utes?)?)?|(\d+)\s*min(?:ute)?\s*(?:time\s*)?cap""",
        RegexOption.IGNORE_CASE
    )

    // Rep scheme patterns
    private val repSchemePattern = Regex(
        """(\d+)[-–](\d+)[-–](\d+)(?:[-–](\d+))?(?:[-–](\d+))?"""
    )
    private val roundsPattern = Regex(
        """(\d+)\s*(?:rounds?|rds?|runden?)""",
        RegexOption.IGNORE_CASE
    )

    // Movement patterns
    private val repsMovementPattern = Regex(
        """(\d+)\s+([A-Za-zÄÖÜäöüß\s\-]+?)(?:\s*[@\(]?\s*(\d+(?:[,\.]\d+)?)\s*(?:kg|lbs?|lb))?""",
        RegexOption.IGNORE_CASE
    )
    private val movementRepsPattern = Regex(
        """([A-Za-zÄÖÜäöüß\s\-]+?)\s*[:\-–]\s*(\d+)(?:\s*(?:reps?|x))?""",
        RegexOption.IGNORE_CASE
    )
    private val caloriePattern = Regex(
        """(\d+)\s*(?:cal(?:ories?)?|kcal)(?:\s+(\w+))?""",
        RegexOption.IGNORE_CASE
    )
    private val distancePattern = Regex(
        """(\d+)\s*(?:m(?:eter)?|km|mi(?:les?)?)\s+([A-Za-z\s]+)""",
        RegexOption.IGNORE_CASE
    )
    private val weightPattern = Regex(
        """(\d+(?:[,\.]\d+)?)\s*/\s*(\d+(?:[,\.]\d+)?)\s*(?:kg|lbs?)|(\d+(?:[,\.]\d+)?)\s*(?:kg|lbs?)(?:\s*/\s*(\d+(?:[,\.]\d+)?)\s*(?:kg|lbs?))?""",
        RegexOption.IGNORE_CASE
    )

    // Common CrossFit movements (for better matching)
    private val knownMovements = listOf(
        // Barbell
        "thrusters", "thruster", "clean", "cleans", "clean and jerk", "c&j",
        "deadlift", "deadlifts", "dl", "squat", "squats", "back squat", "front squat",
        "overhead squat", "ohs", "snatch", "snatches", "power clean", "power snatch",
        "hang clean", "hang snatch", "push press", "push jerk", "split jerk",
        "sumo deadlift high pull", "sdhp", "cluster", "clusters",
        // Gymnastics
        "pull-up", "pull-ups", "pullup", "pullups", "pull up", "pull ups",
        "chest to bar", "c2b", "ctb", "muscle-up", "muscle-ups", "muscle up", "mu",
        "bar muscle-up", "bmu", "ring muscle-up", "rmu",
        "toes to bar", "toes-to-bar", "t2b", "ttb", "knees to elbow", "k2e",
        "handstand push-up", "handstand push-ups", "hspu", "handstand pushup",
        "handstand walk", "hsw", "handstand hold",
        "ring dip", "ring dips", "dip", "dips", "strict dip",
        "rope climb", "rope climbs", "legless rope climb",
        "pistol", "pistols", "pistol squat", "pistol squats",
        // Monostructural
        "run", "running", "row", "rowing", "bike", "biking", "assault bike",
        "ski", "ski erg", "echo bike", "air bike",
        "double under", "double unders", "du", "dus", "double-unders",
        "single under", "single unders", "singles",
        // Weighted
        "wall ball", "wall balls", "wb", "wallball",
        "kettlebell swing", "kb swing", "kettlebell swings", "kb swings",
        "goblet squat", "goblet squats",
        "turkish get-up", "tgu", "turkish getup",
        "farmers carry", "farmers walk", "farmer carry",
        // Bodyweight
        "burpee", "burpees", "burpee over bar", "burpee box jump over",
        "box jump", "box jumps", "bj", "box jump over", "bjo",
        "step up", "step ups", "step-up", "step-ups",
        "lunge", "lunges", "walking lunge", "walking lunges",
        "air squat", "air squats", "squat", "squats",
        "push-up", "push-ups", "pushup", "pushups", "push up", "push ups",
        "sit-up", "sit-ups", "situp", "situps", "sit up", "sit ups",
        "ghd sit-up", "ghd sit-ups", "ghd situp",
        "v-up", "v-ups", "v up", "v ups",
        // Dumbbell
        "dumbbell snatch", "db snatch", "dumbbell clean", "db clean",
        "dumbbell thruster", "db thruster", "dumbbell deadlift", "db deadlift",
        "devil press", "devil's press", "man maker", "manmaker"
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Parse WOD text and return structured data
     */
    fun parse(text: String): ParseResult {
        Log.d(TAG, "📝 Parsing WOD text (${text.length} chars)")

        val warnings = mutableListOf<String>()
        val normalizedText = normalizeText(text)

        // 1. Detect WOD type
        val (wodType, timeCapSeconds, targetRounds) = detectWodType(normalizedText)
        Log.d(TAG, "  WOD Type: $wodType, TimeCap: $timeCapSeconds, Rounds: $targetRounds")

        // 2. Detect rep scheme
        val repScheme = detectRepScheme(normalizedText)
        Log.d(TAG, "  Rep Scheme: $repScheme")

        // 3. Extract movements
        val movements = extractMovements(normalizedText)
        Log.d(TAG, "  Movements found: ${movements.size}")

        if (movements.isEmpty()) {
            warnings.add("No movements detected")
        }

        // 4. Calculate confidence
        val confidence = calculateConfidence(wodType, movements, repScheme)
        val requiresAIReview = confidence < 0.5f || movements.isEmpty()

        // 5. Try to generate a name
        val wodName = generateWodName(wodType, movements, repScheme, targetRounds)

        // 6. Detect equipment
        val equipment = detectEquipment(normalizedText)

        // 7. Estimate difficulty
        val difficulty = estimateDifficulty(movements)

        val parsedWod = if (movements.isNotEmpty() || wodType != "unknown") {
            ParsedWod(
                name = wodName,
                wodType = wodType,
                timeCapSeconds = timeCapSeconds,
                targetRounds = targetRounds,
                repScheme = repScheme,
                movements = movements,
                difficulty = difficulty,
                primaryFocus = detectPrimaryFocus(movements),
                equipmentNeeded = equipment,
                notes = null
            )
        } else {
            null
        }

        Log.d(TAG, "  Confidence: ${(confidence * 100).toInt()}%, AI Review: $requiresAIReview")

        return ParseResult(
            wod = parsedWod,
            confidence = confidence,
            warnings = warnings,
            requiresAIReview = requiresAIReview
        )
    }

    /**
     * Check if text looks like a WOD (quick check)
     */
    fun looksLikeWod(text: String): Boolean {
        val normalizedText = text.lowercase()
        val wodIndicators = listOf(
            "amrap", "for time", "emom", "tabata", "rounds",
            "21-15-9", "15-12-9", "reps", "wod", "metcon",
            "thrusters", "burpees", "pull-ups", "box jumps"
        )
        return wodIndicators.count { normalizedText.contains(it) } >= 2
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    private fun normalizeText(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("–", "-")
            .replace("—", "-")
            .replace("'", "'")
            .replace(""", "\"")
            .replace(""", "\"")
            .trim()
    }

    private fun detectWodType(text: String): Triple<String, Int?, Int?> {
        val lowerText = text.lowercase()

        // Check for AMRAP
        amrapPattern.find(lowerText)?.let { match ->
            val minutes = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() && it.all { c -> c.isDigit() } }?.toIntOrNull()
            return Triple("amrap", minutes?.times(60), null)
        }

        // Check for EMOM
        emomPattern.find(lowerText)?.let { match ->
            val minutes = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() && it.all { c -> c.isDigit() } }?.toIntOrNull()
            return Triple("emom", minutes?.times(60), null)
        }

        // Check for Tabata
        if (tabataPattern.containsMatchIn(lowerText)) {
            return Triple("tabata", 4 * 60, 8) // 4 min, 8 rounds standard
        }

        // Check for For Time
        if (forTimePattern.containsMatchIn(lowerText)) {
            val timeCap = timeCapPattern.find(lowerText)?.let { match ->
                match.groupValues.drop(1).firstOrNull { it.isNotEmpty() && it.all { c -> c.isDigit() } }?.toIntOrNull()?.times(60)
            }
            val rounds = roundsPattern.find(lowerText)?.groupValues?.get(1)?.toIntOrNull()
            return Triple("for_time", timeCap, rounds)
        }

        // Check for rounds pattern
        roundsPattern.find(lowerText)?.let { match ->
            val rounds = match.groupValues[1].toIntOrNull()
            return Triple("rounds", null, rounds)
        }

        return Triple("unknown", null, null)
    }

    private fun detectRepScheme(text: String): List<Int>? {
        repSchemePattern.find(text)?.let { match ->
            val reps = match.groupValues.drop(1)
                .filter { it.isNotEmpty() }
                .mapNotNull { it.toIntOrNull() }
            if (reps.size >= 2) {
                return reps
            }
        }
        return null
    }

    private fun extractMovements(text: String): List<ParsedMovement> {
        val movements = mutableListOf<ParsedMovement>()
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        for (line in lines) {
            val movement = parseMovementLine(line)
            if (movement != null) {
                movements.add(movement)
            }
        }

        // Deduplicate by name
        return movements.distinctBy { it.movementName.lowercase() }
    }

    private fun parseMovementLine(line: String): ParsedMovement? {
        val lowerLine = line.lowercase()

        // Skip non-movement lines
        if (lowerLine.startsWith("amrap") || lowerLine.startsWith("for time") ||
            lowerLine.startsWith("emom") || lowerLine.contains("time cap") ||
            lowerLine.startsWith("rest") || lowerLine.length < 3) {
            return null
        }

        // Try calorie pattern first
        caloriePattern.find(line)?.let { match ->
            val calories = match.groupValues[1].toIntOrNull() ?: return@let
            val equipment = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
            val movementName = equipment?.let { "$it" } ?: "Calories"
            return ParsedMovement(
                movementName = movementName.capitalize(),
                reps = null,
                repType = "calories",
                calories = calories,
                weightType = "bodyweight"
            )
        }

        // Try distance pattern
        distancePattern.find(line)?.let { match ->
            val distance = match.groupValues[1].toIntOrNull() ?: return@let
            val unit = if (line.lowercase().contains("km")) "km" else "m"
            val actualDistance = if (unit == "km") distance * 1000 else distance
            val movementName = match.groupValues[2].trim()
            return ParsedMovement(
                movementName = movementName.capitalize(),
                reps = null,
                repType = "distance",
                distanceMeters = actualDistance,
                weightType = "bodyweight"
            )
        }

        // Try reps + movement pattern (e.g., "21 Thrusters")
        repsMovementPattern.find(line)?.let { match ->
            val reps = match.groupValues[1].toIntOrNull() ?: return@let
            var movementName = match.groupValues[2].trim()

            // Check if it's a known movement
            val knownMatch = knownMovements.find { known ->
                movementName.lowercase().contains(known.lowercase())
            }
            if (knownMatch != null) {
                movementName = knownMatch
            }

            // Try to extract weight
            val weight = match.groupValues.getOrNull(3)?.replace(",", ".")?.toDoubleOrNull()
            val (maleWeight, femaleWeight) = parseWeight(line)

            return ParsedMovement(
                movementName = movementName.capitalize(),
                reps = reps,
                repType = "reps",
                weightKgMale = maleWeight ?: weight,
                weightKgFemale = femaleWeight,
                weightType = if (weight != null || maleWeight != null) "barbell" else "bodyweight"
            )
        }

        // Try movement: reps pattern (e.g., "Burpees: 10")
        movementRepsPattern.find(line)?.let { match ->
            val movementName = match.groupValues[1].trim()
            val reps = match.groupValues[2].toIntOrNull() ?: return@let

            // Check if it's a known movement
            val isKnown = knownMovements.any { known ->
                movementName.lowercase().contains(known.lowercase())
            }
            if (!isKnown && movementName.length < 3) return@let

            return ParsedMovement(
                movementName = movementName.capitalize(),
                reps = reps,
                repType = "reps",
                weightType = "bodyweight"
            )
        }

        // Check if line contains a known movement without reps
        for (known in knownMovements) {
            if (lowerLine.contains(known.lowercase())) {
                val (maleWeight, femaleWeight) = parseWeight(line)
                return ParsedMovement(
                    movementName = known.capitalize(),
                    reps = null,
                    repType = "reps",
                    weightKgMale = maleWeight,
                    weightKgFemale = femaleWeight,
                    weightType = if (maleWeight != null) "barbell" else "bodyweight"
                )
            }
        }

        return null
    }

    private fun parseWeight(line: String): Pair<Double?, Double?> {
        weightPattern.find(line)?.let { match ->
            val groups = match.groupValues.drop(1).filter { it.isNotEmpty() }
            return when (groups.size) {
                2 -> {
                    val first = groups[0].replace(",", ".").toDoubleOrNull()
                    val second = groups[1].replace(",", ".").toDoubleOrNull()
                    Pair(first, second)
                }
                1 -> {
                    val weight = groups[0].replace(",", ".").toDoubleOrNull()
                    Pair(weight, null)
                }
                else -> Pair(null, null)
            }
        }
        return Pair(null, null)
    }

    private fun calculateConfidence(
        wodType: String,
        movements: List<ParsedMovement>,
        repScheme: List<Int>?
    ): Float {
        var score = 0f

        // WOD type detected
        if (wodType != "unknown") score += 0.3f

        // Movements found
        when {
            movements.size >= 4 -> score += 0.4f
            movements.size >= 2 -> score += 0.3f
            movements.size >= 1 -> score += 0.2f
        }

        // Rep scheme detected
        if (repScheme != null) score += 0.15f

        // Known movements found
        val knownCount = movements.count { m ->
            knownMovements.any { known ->
                m.movementName.lowercase().contains(known.lowercase())
            }
        }
        if (knownCount > 0) score += 0.15f

        return score.coerceAtMost(1f)
    }

    private fun generateWodName(
        wodType: String,
        movements: List<ParsedMovement>,
        repScheme: List<Int>?,
        rounds: Int?
    ): String {
        val parts = mutableListOf<String>()

        // Add rep scheme if present
        repScheme?.let {
            parts.add(it.joinToString("-"))
        }

        // Add WOD type
        when (wodType) {
            "amrap" -> parts.add("AMRAP")
            "for_time" -> parts.add("For Time")
            "emom" -> parts.add("EMOM")
            "tabata" -> parts.add("Tabata")
        }

        // Add rounds if present
        rounds?.let {
            if ("rounds" !in parts.joinToString(" ").lowercase()) {
                parts.add("$it Rounds")
            }
        }

        // If we have a rep scheme and movements, create a descriptive name
        if (repScheme != null && movements.isNotEmpty()) {
            return repScheme.joinToString("-") + " " + movements.take(2).joinToString(" + ") { it.movementName }
        }

        // Fallback to movement names
        if (movements.isNotEmpty()) {
            val movementPart = movements.take(2).joinToString(" & ") { it.movementName }
            parts.add(movementPart)
        }

        return if (parts.isNotEmpty()) {
            parts.joinToString(" - ")
        } else {
            "Scanned WOD"
        }
    }

    private fun detectEquipment(text: String): List<String> {
        val equipment = mutableSetOf<String>()
        val lowerText = text.lowercase()

        val equipmentKeywords = mapOf(
            "barbell" to listOf("barbell", "bar", "clean", "snatch", "deadlift", "squat", "thruster"),
            "kettlebell" to listOf("kettlebell", "kb", "swing"),
            "dumbbell" to listOf("dumbbell", "db"),
            "pull-up bar" to listOf("pull-up", "pullup", "toes to bar", "muscle-up", "t2b"),
            "box" to listOf("box jump", "step up", "box"),
            "rower" to listOf("row", "rowing", "rower"),
            "bike" to listOf("bike", "assault", "echo", "air bike"),
            "jump rope" to listOf("double under", "du", "single under", "jump rope"),
            "wall ball" to listOf("wall ball", "wallball", "wb"),
            "rings" to listOf("ring", "ring dip", "muscle-up"),
            "rope" to listOf("rope climb"),
            "ghd" to listOf("ghd"),
            "ski erg" to listOf("ski erg", "ski")
        )

        for ((equip, keywords) in equipmentKeywords) {
            if (keywords.any { lowerText.contains(it) }) {
                equipment.add(equip)
            }
        }

        return equipment.toList()
    }

    private fun estimateDifficulty(movements: List<ParsedMovement>): String {
        val advancedMovements = listOf(
            "muscle-up", "muscle up", "handstand", "hspu", "pistol",
            "snatch", "clean and jerk", "double under"
        )
        val intermediateMovements = listOf(
            "pull-up", "toes to bar", "box jump", "thruster",
            "kettlebell swing", "deadlift", "clean"
        )

        val advancedCount = movements.count { m ->
            advancedMovements.any { m.movementName.lowercase().contains(it) }
        }
        val intermediateCount = movements.count { m ->
            intermediateMovements.any { m.movementName.lowercase().contains(it) }
        }

        return when {
            advancedCount >= 2 -> "advanced"
            advancedCount >= 1 || intermediateCount >= 3 -> "intermediate"
            else -> "beginner"
        }
    }

    private fun detectPrimaryFocus(movements: List<ParsedMovement>): List<String> {
        val focus = mutableSetOf<String>()

        val cardioKeywords = listOf("run", "row", "bike", "ski", "double under")
        val strengthKeywords = listOf("deadlift", "squat", "press", "clean", "snatch", "thruster")
        val gymnasticsKeywords = listOf("pull-up", "muscle-up", "handstand", "toes to bar", "ring")

        for (movement in movements) {
            val name = movement.movementName.lowercase()
            when {
                cardioKeywords.any { name.contains(it) } -> focus.add("cardio")
                strengthKeywords.any { name.contains(it) } -> focus.add("strength")
                gymnasticsKeywords.any { name.contains(it) } -> focus.add("gymnastics")
            }
        }

        return focus.toList().ifEmpty { listOf("mixed") }
    }

    private fun String.capitalize(): String {
        return this.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}
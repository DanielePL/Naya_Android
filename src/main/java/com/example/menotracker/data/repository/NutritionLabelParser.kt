// data/repository/NutritionLabelParser.kt
package com.example.menotracker.data.repository

import android.util.Log

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * NUTRITION LABEL PARSER - ROBUST OCR EXTRACTION
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Intelligent parser for extracting nutritional data from OCR text.
 * Handles various label formats from different countries:
 * - German: "Brennwert", "Eiweiß", "Kohlenhydrate", "Fett"
 * - English/US: "Calories", "Protein", "Total Carbohydrate", "Total Fat"
 * - International: kJ/kcal conversions
 *
 * Uses multiple extraction strategies for maximum reliability.
 */
object NutritionLabelParser {

    private const val TAG = "NutritionLabelParser"

    data class ParsedNutrition(
        val calories: Float?,
        val protein: Float?,
        val carbs: Float?,
        val fat: Float?,
        val fiber: Float? = null,
        val sugar: Float? = null,
        val saturatedFat: Float? = null,
        val sodium: Float? = null,
        val rawText: String,
        val confidence: Float
    ) {
        val isValid: Boolean
            get() = calories != null && protein != null && carbs != null && fat != null

        val missingFields: List<String>
            get() = buildList {
                if (calories == null) add("Calories")
                if (protein == null) add("Protein")
                if (carbs == null) add("Carbs")
                if (fat == null) add("Fat")
            }
    }

    /**
     * Parse nutrition information from OCR text
     */
    fun parse(ocrText: String): ParsedNutrition {
        Log.d(TAG, "═══ RAW OCR TEXT ═══\n$ocrText\n═══ END RAW ═══")

        // Create multiple text variants for matching
        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val flatText = normalizeToFlat(ocrText)
        val lineByLine = lines.joinToString(" | ")

        Log.d(TAG, "═══ FLAT TEXT ═══\n$flatText\n═══ END FLAT ═══")

        // Extract using multiple strategies
        val calories = extractCalories(flatText, lines)
        val protein = extractProtein(flatText, lines)
        val carbs = extractCarbs(flatText, lines)
        val fat = extractFat(flatText, lines)
        val fiber = extractFiber(flatText, lines)
        val sugar = extractSugar(flatText, lines)
        val saturatedFat = extractSaturatedFat(flatText, lines)
        val sodium = extractSodium(flatText, lines)

        val foundCount = listOf(calories, protein, carbs, fat).count { it != null }
        val extendedCount = listOf(fiber, sugar, saturatedFat, sodium).count { it != null }
        val confidence = (foundCount * 0.2f) + (extendedCount * 0.05f)

        Log.d(TAG, "═══ RESULTS ═══")
        Log.d(TAG, "Calories: $calories, Protein: $protein, Carbs: $carbs, Fat: $fat")
        Log.d(TAG, "Fiber: $fiber, Sugar: $sugar, SatFat: $saturatedFat, Sodium: $sodium")
        Log.d(TAG, "Confidence: ${(confidence * 100).toInt()}%")

        return ParsedNutrition(
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            fiber = fiber,
            sugar = sugar,
            saturatedFat = saturatedFat,
            sodium = sodium,
            rawText = ocrText,
            confidence = confidence.coerceIn(0f, 1f)
        )
    }

    /**
     * Normalize text to a flat single-line string for pattern matching
     */
    private fun normalizeToFlat(text: String): String {
        return text
            .lowercase()
            // Replace newlines with spaces first
            .replace(Regex("[\r\n]+"), " ")
            // German umlauts
            .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
            // Common OCR substitutions
            .replace("é", "e").replace("è", "e")
            // Normalize units
            .replace("gramm", "g").replace("gram", "g")
            // German/European decimal comma to dot
            .replace(",", ".")
            // Fix common OCR digit errors (only between digits)
            .replace(Regex("(?<=[0-9])[oO](?=[0-9])"), "0")
            .replace(Regex("(?<=[0-9])[lI](?=[0-9])"), "1")
            .replace(Regex("(?<=[0-9])S(?=[0-9])"), "5")
            .replace(Regex("(?<=[0-9])B(?=[0-9])"), "8")
            // Separate numbers from units
            .replace(Regex("(\\d)(kcal|kj)"), "$1 $2")
            .replace(Regex("(\\d)g\\b"), "$1 g")
            .replace(Regex("(\\d)mg\\b"), "$1 mg")
            // Collapse whitespace
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Find a number near a keyword, checking both flat text and line-by-line
     */
    private fun findNumberNearKeyword(
        flatText: String,
        lines: List<String>,
        keywords: List<String>,
        requireUnit: String? = null,
        excludeKeywords: List<String> = emptyList()
    ): Float? {
        // Strategy 1: Search in flat text with flexible patterns
        for (keyword in keywords) {
            val escaped = Regex.escape(keyword)

            // Skip if an exclude keyword is too close
            if (excludeKeywords.isNotEmpty()) {
                val excludePattern = excludeKeywords.joinToString("|") { Regex.escape(it) }
                if (Regex("($excludePattern).{0,15}$escaped", RegexOption.IGNORE_CASE).containsMatchIn(flatText)) {
                    continue
                }
            }

            val unitPart = requireUnit?.let { "\\s*$it" } ?: ""

            // Pattern: keyword followed by number (with optional stuff in between)
            val patterns = listOf(
                // Tight: "keyword 123" or "keyword: 123"
                Regex("$escaped[:\\s]{0,5}([0-9]+\\.?[0-9]*)$unitPart", RegexOption.IGNORE_CASE),
                // Medium: up to 15 chars between keyword and number
                Regex("$escaped.{0,15}?([0-9]+\\.?[0-9]*)$unitPart", RegexOption.IGNORE_CASE),
                // Loose: up to 25 chars
                Regex("$escaped.{0,25}?([0-9]+\\.?[0-9]*)$unitPart", RegexOption.IGNORE_CASE)
            )

            for (pattern in patterns) {
                val match = pattern.find(flatText)
                if (match != null) {
                    val value = match.groupValues[1].toFloatOrNull()
                    if (value != null && value >= 0) {
                        Log.d(TAG, "Found '$keyword' = $value (flat text)")
                        return value
                    }
                }
            }
        }

        // Strategy 2: Search line by line
        for ((index, line) in lines.withIndex()) {
            val normalizedLine = line.lowercase()
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
                .replace(",", ".")

            for (keyword in keywords) {
                if (normalizedLine.contains(keyword, ignoreCase = true)) {
                    // Check for exclude keywords on same line
                    if (excludeKeywords.any { normalizedLine.contains(it, ignoreCase = true) }) {
                        continue
                    }

                    // Try to find number on same line
                    val unitPart = requireUnit?.let { "\\s*$it" } ?: ""
                    val numberPattern = Regex("([0-9]+\\.?[0-9]*)$unitPart")
                    val match = numberPattern.find(normalizedLine)
                    if (match != null) {
                        val value = match.groupValues[1].toFloatOrNull()
                        if (value != null && value >= 0) {
                            Log.d(TAG, "Found '$keyword' = $value (line $index)")
                            return value
                        }
                    }

                    // Check next line for number
                    if (index + 1 < lines.size) {
                        val nextLine = lines[index + 1].lowercase().replace(",", ".")
                        val nextMatch = numberPattern.find(nextLine)
                        if (nextMatch != null) {
                            val value = nextMatch.groupValues[1].toFloatOrNull()
                            if (value != null && value >= 0) {
                                Log.d(TAG, "Found '$keyword' = $value (next line)")
                                return value
                            }
                        }
                    }
                }
            }
        }

        return null
    }

    /**
     * Extract calories - handles kcal, kJ, and American "Calories" format
     */
    private fun extractCalories(flatText: String, lines: List<String>): Float? {
        // Strategy 1: Look for explicit kcal
        val kcalPatterns = listOf(
            Regex("([0-9]+\\.?[0-9]*)\\s*kcal", RegexOption.IGNORE_CASE),
            Regex("([0-9]+)\\s*kj\\s*[/|(]\\s*([0-9]+)\\s*kcal", RegexOption.IGNORE_CASE),
            Regex("\\(\\s*([0-9]+\\.?[0-9]*)\\s*kcal\\s*\\)", RegexOption.IGNORE_CASE)
        )

        for (pattern in kcalPatterns) {
            val matches = pattern.findAll(flatText)
            for (match in matches) {
                // For kJ/kcal pattern, kcal is in group 2
                val value = if (match.groupValues.size > 2 && match.groupValues[2].isNotEmpty()) {
                    match.groupValues[2].toFloatOrNull()
                } else {
                    match.groupValues[1].toFloatOrNull()
                }
                if (value != null && value in 10f..900f) {
                    Log.d(TAG, "Found kcal: $value")
                    return value
                }
            }
        }

        // Strategy 2: American format "Calories" without unit
        val calorieKeywords = listOf("calories", "calorie", "cal ")
        for (keyword in calorieKeywords) {
            val patterns = listOf(
                // "Calories 230"
                Regex("$keyword\\s*:?\\s*([0-9]+)", RegexOption.IGNORE_CASE),
                // With some text between
                Regex("$keyword.{0,20}?([0-9]{2,3})(?!\\s*[gm%])", RegexOption.IGNORE_CASE)
            )
            for (pattern in patterns) {
                val match = pattern.find(flatText)
                if (match != null) {
                    val value = match.groupValues[1].toFloatOrNull()
                    if (value != null && value in 10f..900f) {
                        Log.d(TAG, "Found American calories: $value")
                        return value
                    }
                }
            }
        }

        // Strategy 3: Line-by-line for "Calories"
        for ((index, line) in lines.withIndex()) {
            val normalizedLine = line.lowercase().trim()
            if (normalizedLine.contains("calorie") || normalizedLine.contains("calories")) {
                // Check same line for number
                val numberMatch = Regex("([0-9]{2,3})").find(normalizedLine)
                if (numberMatch != null) {
                    val value = numberMatch.value.toFloatOrNull()
                    if (value != null && value in 10f..900f) {
                        Log.d(TAG, "Found calories on line: $value")
                        return value
                    }
                }
                // Check next line
                if (index + 1 < lines.size) {
                    val nextLine = lines[index + 1].trim()
                    val nextMatch = Regex("^([0-9]{2,3})").find(nextLine)
                    if (nextMatch != null) {
                        val value = nextMatch.value.toFloatOrNull()
                        if (value != null && value in 10f..900f) {
                            Log.d(TAG, "Found calories on next line: $value")
                            return value
                        }
                    }
                }
            }
        }

        // Strategy 4: Convert from kJ
        val kjPattern = Regex("([0-9]+\\.?[0-9]*)\\s*kj", RegexOption.IGNORE_CASE)
        val kjMatch = kjPattern.find(flatText)
        if (kjMatch != null) {
            val kjValue = kjMatch.groupValues[1].toFloatOrNull()
            if (kjValue != null && kjValue in 100f..4000f) {
                val kcalValue = kjValue / 4.184f
                Log.d(TAG, "Converted kJ to kcal: $kjValue -> $kcalValue")
                return kcalValue
            }
        }

        // Strategy 5: Look for brennwert/energie (German)
        val germanResult = findNumberNearKeyword(
            flatText, lines,
            listOf("brennwert", "energie", "energy"),
            requireUnit = null
        )
        if (germanResult != null && germanResult in 10f..900f) {
            return germanResult
        }

        return null
    }

    private fun extractProtein(flatText: String, lines: List<String>): Float? {
        return findNumberNearKeyword(
            flatText, lines,
            listOf("protein", "eiweiss", "eiweiß", "proteine"),
            requireUnit = "g"
        )?.takeIf { it in 0f..100f }
    }

    private fun extractCarbs(flatText: String, lines: List<String>): Float? {
        return findNumberNearKeyword(
            flatText, lines,
            listOf(
                "total carbohydrate", "total carb",
                "kohlenhydrate", "kohlenhydrat",
                "carbohydrate", "carbohydrates", "carbs"
            ),
            requireUnit = "g",
            excludeKeywords = listOf("sugar", "zucker", "fiber", "fibre", "davon")
        )?.takeIf { it in 0f..100f }
    }

    private fun extractFat(flatText: String, lines: List<String>): Float? {
        // Try "Total Fat" first (US format)
        var result = findNumberNearKeyword(
            flatText, lines,
            listOf("total fat", "fett"),
            requireUnit = "g",
            excludeKeywords = listOf("saturated", "gesaettigt", "davon", "trans")
        )

        if (result == null) {
            // Fallback to just "fat" but be careful
            val fatPattern = Regex("(?<!saturated\\s)(?<!trans\\s)(?<!gesaettigt.{0,5})fat\\s*:?\\s*([0-9]+\\.?[0-9]*)\\s*g", RegexOption.IGNORE_CASE)
            val match = fatPattern.find(flatText)
            if (match != null) {
                result = match.groupValues[1].toFloatOrNull()
            }
        }

        return result?.takeIf { it in 0f..100f }
    }

    private fun extractFiber(flatText: String, lines: List<String>): Float? {
        return findNumberNearKeyword(
            flatText, lines,
            listOf("dietary fiber", "fibre", "fiber", "ballaststoff", "ballaststoffe"),
            requireUnit = "g"
        )?.takeIf { it in 0f..50f }
    }

    private fun extractSugar(flatText: String, lines: List<String>): Float? {
        return findNumberNearKeyword(
            flatText, lines,
            listOf("total sugars", "sugars", "sugar", "davon zucker", "zucker"),
            requireUnit = "g"
        )?.takeIf { it in 0f..100f }
    }

    private fun extractSaturatedFat(flatText: String, lines: List<String>): Float? {
        return findNumberNearKeyword(
            flatText, lines,
            listOf(
                "saturated fat", "saturated", "saturates",
                "gesaettigte fettsaeuren", "gesaettigte fett", "davon gesaettigte"
            ),
            requireUnit = "g"
        )?.takeIf { it in 0f..50f }
    }

    private fun extractSodium(flatText: String, lines: List<String>): Float? {
        // Try sodium in mg
        var result = findNumberNearKeyword(
            flatText, lines,
            listOf("sodium", "natrium"),
            requireUnit = "mg"
        )

        if (result != null) {
            return result.takeIf { it in 0f..5000f }
        }

        // Try sodium in g (convert to mg)
        result = findNumberNearKeyword(
            flatText, lines,
            listOf("sodium", "natrium"),
            requireUnit = "g"
        )
        if (result != null && result < 10) {
            return (result * 1000).takeIf { it in 0f..5000f }
        }

        // Try salt and convert
        val salt = findNumberNearKeyword(
            flatText, lines,
            listOf("salt", "salz"),
            requireUnit = "g"
        )
        if (salt != null) {
            return ((salt / 2.5f) * 1000).takeIf { it in 0f..5000f }
        }

        return null
    }

    /**
     * Validate parsed nutrition values
     */
    fun validateNutrition(parsed: ParsedNutrition): List<String> {
        val warnings = mutableListOf<String>()

        parsed.calories?.let { cal ->
            if (cal < 0) warnings.add("Calories cannot be negative")
            if (cal > 900) warnings.add("Calories seem too high (>900 kcal/100g)")
        }

        parsed.protein?.let { prot ->
            if (prot < 0) warnings.add("Protein cannot be negative")
            if (prot > 100) warnings.add("Protein cannot exceed 100g per 100g")
        }

        parsed.carbs?.let { carb ->
            if (carb < 0) warnings.add("Carbs cannot be negative")
            if (carb > 100) warnings.add("Carbs cannot exceed 100g per 100g")
        }

        parsed.fat?.let { fat ->
            if (fat < 0) warnings.add("Fat cannot be negative")
            if (fat > 100) warnings.add("Fat cannot exceed 100g per 100g")
        }

        val macroSum = (parsed.protein ?: 0f) + (parsed.carbs ?: 0f) + (parsed.fat ?: 0f)
        if (macroSum > 110) {
            warnings.add("Macro sum (${"%.1f".format(macroSum)}g) exceeds 100g - please verify")
        }

        if (parsed.calories != null && parsed.protein != null && parsed.carbs != null && parsed.fat != null) {
            val calculatedCal = (parsed.protein * 4) + (parsed.carbs * 4) + (parsed.fat * 9)
            val diff = kotlin.math.abs(calculatedCal - parsed.calories)
            if (diff > 100) {
                warnings.add("Calculated calories (${"%.0f".format(calculatedCal)}) differ from stated (${parsed.calories.toInt()})")
            }
        }

        return warnings
    }
}
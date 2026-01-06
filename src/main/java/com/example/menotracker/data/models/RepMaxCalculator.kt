package com.example.menotracker.data.models

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Rep Max Calculator - Wissenschaftlich fundierte Prozent/Rep-Berechnungen
 *
 * Basiert auf etablierten Formeln:
 * - Epley: weight × (1 + reps/30)
 * - Brzycki: weight × (36 / (37 - reps))
 * - Lombardi: weight × reps^0.10
 * - O'Conner: weight × (1 + 0.025 × reps)
 *
 * Die App verwendet primär Epley (genauer für moderate Reps) mit
 * Brzycki-Korrektur für höhere Rep-Ranges.
 */
object RepMaxCalculator {

    // ==========================================
    // CORE FORMULAS
    // ==========================================

    /**
     * Berechnet e1RM (estimated 1 Rep Max) aus Gewicht und Reps
     * Verwendet Hybrid-Formel für beste Genauigkeit über alle Rep-Ranges
     */
    fun calculate1RM(weight: Float, reps: Int): Float {
        if (reps <= 0) return 0f
        if (reps == 1) return weight
        if (weight <= 0) return 0f

        return when {
            reps <= 5 -> epley1RM(weight, reps)
            reps <= 10 -> (epley1RM(weight, reps) + brzycki1RM(weight, reps)) / 2f
            else -> brzycki1RM(weight, reps)
        }
    }

    /**
     * Epley Formula: weight × (1 + reps/30)
     * Gut für 1-10 Reps
     */
    private fun epley1RM(weight: Float, reps: Int): Float {
        return weight * (1 + reps / 30f)
    }

    /**
     * Brzycki Formula: weight × (36 / (37 - reps))
     * Genauer für höhere Rep-Ranges (6-15+)
     */
    private fun brzycki1RM(weight: Float, reps: Int): Float {
        if (reps >= 37) return weight * 2f // Prevent division by zero/negative
        return weight * (36f / (37f - reps))
    }

    // ==========================================
    // REP MAX TABLES
    // ==========================================

    /**
     * Standard Rep-Max Prozentsätze (% of 1RM)
     * Wissenschaftlich validiert über jahrzehntelange Forschung
     */
    val STANDARD_REP_PERCENTAGES = mapOf(
        1 to 1.00f,
        2 to 0.97f,
        3 to 0.94f,
        4 to 0.92f,
        5 to 0.89f,
        6 to 0.86f,
        7 to 0.83f,
        8 to 0.81f,
        9 to 0.78f,
        10 to 0.75f,
        11 to 0.73f,
        12 to 0.71f,
        13 to 0.69f,
        14 to 0.67f,
        15 to 0.65f,
        20 to 0.58f,
        25 to 0.52f,
        30 to 0.47f
    )

    /**
     * Berechnet das Gewicht für eine bestimmte Rep-Anzahl
     * @param oneRM Der 1RM des Lifts
     * @param targetReps Ziel-Wiederholungen
     * @param roundTo Auf welchen Wert runden (default 2.5kg)
     */
    fun calculateWeightForReps(
        oneRM: Float,
        targetReps: Int,
        roundTo: Float = 2.5f
    ): Float {
        val percentage = getPercentageForReps(targetReps)
        val raw = oneRM * percentage
        return roundToPlate(raw, roundTo)
    }

    /**
     * Gibt die Prozentsätze für alle Rep-Ranges zurück
     * Für UI-Tabellen wie in der Rep Max App
     */
    fun getRepMaxTable(oneRM: Float, roundTo: Float = 2.5f): List<RepMaxEntry> {
        return (1..15).map { reps ->
            RepMaxEntry(
                reps = reps,
                weight = calculateWeightForReps(oneRM, reps, roundTo),
                percentage = getPercentageForReps(reps)
            )
        }
    }

    /**
     * Gibt Prozentsatz-Tabelle zurück (wie Screenshot 2)
     * Von 40% bis 105% in 5%-Schritten
     */
    fun getPercentageTable(oneRM: Float, roundTo: Float = 2.5f): List<PercentageEntry> {
        val percentages = listOf(105, 102, 100, 95, 90, 85, 80, 75, 70, 65, 60, 55, 50, 45, 40)
        return percentages.map { pct ->
            PercentageEntry(
                percentage = pct,
                weight = roundToPlate(oneRM * (pct / 100f), roundTo)
            )
        }
    }

    // ==========================================
    // PERCENTAGE HELPERS
    // ==========================================

    /**
     * Holt den Prozentsatz für eine Rep-Anzahl
     * Interpoliert für Werte zwischen den Standard-Werten
     */
    fun getPercentageForReps(reps: Int): Float {
        if (reps <= 0) return 1.0f
        if (reps >= 30) return 0.47f

        // Direkte Lookup für Standard-Werte
        STANDARD_REP_PERCENTAGES[reps]?.let { return it }

        // Interpolation für Zwischenwerte
        val lowerReps = STANDARD_REP_PERCENTAGES.keys.filter { it < reps }.maxOrNull() ?: 1
        val upperReps = STANDARD_REP_PERCENTAGES.keys.filter { it > reps }.minOrNull() ?: 30

        val lowerPct = STANDARD_REP_PERCENTAGES[lowerReps] ?: 1.0f
        val upperPct = STANDARD_REP_PERCENTAGES[upperReps] ?: 0.47f

        val ratio = (reps - lowerReps).toFloat() / (upperReps - lowerReps)
        return lowerPct - (lowerPct - upperPct) * ratio
    }

    /**
     * Berechnet die Reps die mit einem bestimmten Prozentsatz möglich sind
     */
    fun getRepsForPercentage(percentage: Float): Int {
        val pct = percentage.coerceIn(0.40f, 1.0f)

        // Reverse lookup - finde nächste Rep-Anzahl
        for ((reps, repPct) in STANDARD_REP_PERCENTAGES.entries.sortedBy { it.key }) {
            if (repPct <= pct) return reps
        }
        return 1
    }

    // ==========================================
    // ROUNDING HELPERS
    // ==========================================

    /**
     * Rundet auf verfügbare Hantelscheiben
     */
    fun roundToPlate(weight: Float, roundTo: Float = 2.5f): Float {
        if (roundTo <= 0) return weight
        return (weight / roundTo).roundToInt() * roundTo
    }

    /**
     * Rundet intelligent nach unten für konservative Schätzungen
     */
    fun roundDownToPlate(weight: Float, roundTo: Float = 2.5f): Float {
        if (roundTo <= 0) return weight
        return kotlin.math.floor(weight / roundTo) * roundTo
    }

    // ==========================================
    // WILKS COEFFICIENT
    // ==========================================

    /**
     * Berechnet Wilks Coefficient für Vergleichbarkeit über Gewichtsklassen
     * Formel: Total × Wilks-Multiplier basierend auf Körpergewicht
     */
    fun calculateWilks(
        total: Float,
        bodyweightKg: Float,
        gender: StrengthGender
    ): Float {
        if (bodyweightKg <= 0 || total <= 0) return 0f

        val coefficients = if (gender == StrengthGender.MALE) {
            // Male coefficients
            doubleArrayOf(
                -216.0475144,
                16.2606339,
                -0.002388645,
                -0.00113732,
                7.01863E-06,
                -1.291E-08
            )
        } else {
            // Female coefficients
            doubleArrayOf(
                594.31747775582,
                -27.23842536447,
                0.82112226871,
                -0.00930733913,
                4.731582E-05,
                -9.054E-08
            )
        }

        val x = bodyweightKg.toDouble()
        var denominator = coefficients[0]
        for (i in 1 until coefficients.size) {
            denominator += coefficients[i] * x.pow(i)
        }

        return if (denominator > 0) {
            (total * 500 / denominator).toFloat()
        } else 0f
    }
}

/**
 * Entry für Rep-Max Tabelle
 */
data class RepMaxEntry(
    val reps: Int,
    val weight: Float,
    val percentage: Float
)

/**
 * Entry für Prozentsatz-Tabelle
 */
data class PercentageEntry(
    val percentage: Int,
    val weight: Float
)

// ==========================================
// EXERCISE-SPECIFIC PERCENTAGES
// ==========================================

/**
 * Prozent-Verhältnisse von Übungsvarianten zum Haupt-Lift
 * Basiert auf Forschung und Erfahrungswerten von Elite-Coaches
 */
object ExercisePercentages {

    // Squat Varianten (% des Back Squat 1RM)
    // WICHTIG: Übungen mit NO_CORRELATION (null) bekommen KEINE Empfehlung
    val SQUAT_VARIANTS = mapOf(
        "Back Squat" to 1.00f,
        "Front Squat" to 0.85f,            // ~85% des Back Squat
        "Pause Squat" to 0.85f,            // 3-Sekunden Pause
        "Tempo Squat" to 0.75f,            // 3-1-3-0 Tempo
        "Pin Squat" to 0.80f,              // Aus dem Loch
        "Box Squat" to 0.90f,              // Parallelbox
        "Safety Bar Squat" to 0.90f,       // SSB
        "High Bar Squat" to 0.95f,         // vs Low Bar
        "Low Bar Squat" to 1.00f,          // Competition Style
        "Zercher Squat" to 0.70f,          // Front-loaded, limitiert durch Arms
        "Anderson Squat" to 0.85f          // Aus Pins
    )

    // Squat-Varianten die KEINE Korrelation zum Back Squat haben
    // Diese bekommen keine automatische Empfehlung - User muss selbst tracken
    val SQUAT_NO_CORRELATION = setOf(
        "Belt Squat",                      // Maschine, völlig andere Belastung
        "Pendulum Squat",                  // Maschine
        "Overhead Squat",                  // OLY-Lift, limitiert durch Mobilität/Schulter
        "Sissy Squat",                     // Isolation, kein Vergleich
        "Goblet Squat",                    // KB/DB limitiert, nicht vergleichbar
        "Bulgarian Split Squat",           // Einbeinig, andere Mechanik
        "Split Squat",                     // Einbeinig
        "Pistol Squat",                    // Einbeinig, Skill-basiert
        "Leg Press",                       // Maschine, kein Transfer
        "Hack Squat",                      // Maschine
        "Smith Machine Squat",             // Geführt, nicht vergleichbar
        "V Squat",                         // Maschine
        "Lunge",                           // Einbeinig, dynamisch
        "Walking Lunge",
        "Reverse Lunge",
        "Step Up"
    )

    // Bench Varianten (% des Competition Bench 1RM)
    val BENCH_VARIANTS = mapOf(
        "Bench Press" to 1.00f,
        "Pause Bench" to 0.95f,            // Competition-Style
        "Touch and Go Bench" to 1.02f,     // Leicht mehr
        "Close Grip Bench" to 0.85f,       // Enggriff
        "Wide Grip Bench" to 0.95f,        // Weitgriff
        "Incline Bench" to 0.75f,          // 30-45°
        "Decline Bench" to 1.05f,          // Stärker
        "Floor Press" to 0.85f,            // Reduzierter ROM
        "Spoto Press" to 0.90f,            // 2" über Brust
        "Larsen Press" to 0.90f            // Beine hoch
    )

    // Bench-Varianten ohne Korrelation
    val BENCH_NO_CORRELATION = setOf(
        "DB Bench Press",                  // Kurzhantel, nicht vergleichbar
        "Dumbbell Bench Press",
        "Incline DB Press",
        "DB Incline Press",
        "Machine Chest Press",
        "Smith Machine Bench",
        "Cable Fly",
        "Pec Deck",
        "Chest Fly",
        "Push Up",
        "Dips"                             // Bodyweight + limitiert
    )

    // Deadlift Varianten (% des Conventional Deadlift 1RM)
    val DEADLIFT_VARIANTS = mapOf(
        "Deadlift" to 1.00f,
        "Conventional Deadlift" to 1.00f,
        "Sumo Deadlift" to 1.00f,          // Individuell
        "Pause Deadlift" to 0.85f,         // Kniehöhe
        "Deficit Deadlift" to 0.90f,       // 2-4" Erhöhung
        "Block Pull" to 1.10f,             // Unter Knie
        "Rack Pull" to 1.20f,              // Über Knie
        "Romanian Deadlift" to 0.65f,      // RDL
        "Stiff Leg Deadlift" to 0.60f,     // SLDL
        "Trap Bar Deadlift" to 1.10f,      // Hex Bar
        "Snatch Grip Deadlift" to 0.80f,   // Weiter Griff
        "Good Morning" to 0.45f            // Hip Hinge
    )

    // Deadlift-Varianten ohne Korrelation
    val DEADLIFT_NO_CORRELATION = setOf(
        "DB Romanian Deadlift",            // Kurzhantel, limitiert
        "Single Leg RDL",                  // Einbeinig
        "Kettlebell Swing",                // Explosive, andere Mechanik
        "Hip Thrust",                      // Glute-fokussiert
        "Glute Bridge",
        "Hyperextension",                  // Isolation
        "Back Extension",
        "Reverse Hyper",
        "Cable Pull Through",
        "Leg Curl",                        // Isolation
        "Nordic Curl"
    )

    // Overhead Press Varianten (% des Strict OHP 1RM)
    val OVERHEAD_VARIANTS = mapOf(
        "Overhead Press" to 1.00f,
        "Strict Press" to 1.00f,
        "Military Press" to 1.00f,
        "Push Press" to 1.20f,             // Mit Beinstreckung
        "Push Jerk" to 1.35f,              // Mit Dip
        "Split Jerk" to 1.40f,             // OLY-Jerk
        "Seated OHP" to 0.90f,             // Kein Core
        "Behind Neck Press" to 0.85f,      // BTN
        "Z Press" to 0.75f                 // Sitzend ohne Stütze
    )

    // Overhead-Varianten ohne Korrelation
    val OVERHEAD_NO_CORRELATION = setOf(
        "DB Shoulder Press",               // Kurzhantel
        "Dumbbell Press",
        "Arnold Press",
        "Landmine Press",
        "Machine Shoulder Press",
        "Smith Machine OHP",
        "Lateral Raise",                   // Isolation
        "Front Raise",
        "Face Pull",
        "Upright Row",
        "Shrug"
    )

    // ==========================================
    // VOLLSTÄNDIGE LISTE ALLER ISOLATION/MACHINE ÜBUNGEN
    // Diese bekommen NIEMALS eine 1RM-basierte Empfehlung
    // ==========================================
    val ALL_NO_CORRELATION = SQUAT_NO_CORRELATION +
            BENCH_NO_CORRELATION +
            DEADLIFT_NO_CORRELATION +
            OVERHEAD_NO_CORRELATION +
            setOf(
                // ARM EXERCISES
                "Bicep Curl", "Barbell Curl", "DB Curl", "Hammer Curl",
                "Preacher Curl", "Cable Curl", "Concentration Curl",
                "Tricep Extension", "Tricep Pushdown", "Skull Crusher",
                "Overhead Tricep Extension", "Tricep Kickback",
                "Cable Tricep Extension",

                // BACK EXERCISES (Rows haben keine Korrelation zu Deadlift)
                "Lat Pulldown", "Pull Up", "Chin Up",
                "Barbell Row", "DB Row", "Cable Row",
                "Seated Row", "T-Bar Row", "Pendlay Row",
                "Face Pull", "Rear Delt Fly",

                // LEG ISOLATION
                "Leg Extension", "Leg Curl", "Calf Raise",
                "Seated Calf Raise", "Adductor Machine", "Abductor Machine",
                "Glute Kickback",

                // CORE
                "Plank", "Ab Crunch", "Sit Up", "Leg Raise",
                "Cable Crunch", "Ab Wheel", "Russian Twist",
                "Hanging Leg Raise", "Dead Bug",

                // OLYMPIC LIFTS (eigene Kategorie, nicht von Powerlifts ableitbar)
                "Clean", "Clean and Jerk", "Snatch",
                "Power Clean", "Power Snatch", "Hang Clean",
                "Clean Pull", "Snatch Pull"
            )

    /**
     * Findet den Prozentsatz für eine Übung relativ zum Haupt-Lift
     */
    fun getExercisePercentage(exerciseName: String): ExerciseRelation? {
        // Check each category
        SQUAT_VARIANTS[exerciseName]?.let {
            return ExerciseRelation(LiftType.SQUAT, it)
        }
        BENCH_VARIANTS[exerciseName]?.let {
            return ExerciseRelation(LiftType.BENCH, it)
        }
        DEADLIFT_VARIANTS[exerciseName]?.let {
            return ExerciseRelation(LiftType.DEADLIFT, it)
        }
        OVERHEAD_VARIANTS[exerciseName]?.let {
            return ExerciseRelation(LiftType.OVERHEAD, it)
        }

        // Fuzzy matching für ähnliche Namen
        return findFuzzyMatch(exerciseName)
    }

    /**
     * Prüft ob eine Übung KEINE Korrelation zu den Big 4 hat
     * Diese Übungen bekommen keine automatische Empfehlung
     */
    fun hasNoCorrelation(exerciseName: String): Boolean {
        val lowerName = exerciseName.lowercase()

        // Exakte Matches prüfen
        if (ALL_NO_CORRELATION.any { it.lowercase() == lowerName }) {
            return true
        }

        // Fuzzy matching für NO_CORRELATION Übungen
        return when {
            // SQUAT-artige ohne Korrelation
            lowerName.contains("belt squat") -> true
            lowerName.contains("pendulum squat") -> true
            lowerName.contains("overhead squat") -> true
            lowerName.contains("sissy squat") -> true
            lowerName.contains("goblet squat") -> true
            lowerName.contains("bulgarian") -> true
            lowerName.contains("split squat") -> true
            lowerName.contains("pistol") -> true
            lowerName.contains("leg press") -> true
            lowerName.contains("hack squat") -> true
            lowerName.contains("v squat") -> true
            lowerName.contains("smith") && lowerName.contains("squat") -> true
            lowerName.contains("lunge") -> true
            lowerName.contains("step up") -> true

            // BENCH-artige ohne Korrelation
            lowerName.contains("dumbbell") && lowerName.contains("bench") -> true
            lowerName.contains("db") && lowerName.contains("bench") -> true
            lowerName.contains("db") && lowerName.contains("press") -> true
            lowerName.contains("machine") && lowerName.contains("chest") -> true
            lowerName.contains("cable fly") -> true
            lowerName.contains("pec deck") -> true
            lowerName.contains("push up") -> true
            lowerName.contains("pushup") -> true
            lowerName.contains("dip") -> true

            // DEADLIFT-artige ohne Korrelation
            lowerName.contains("single leg") -> true
            lowerName.contains("kettlebell swing") -> true
            lowerName.contains("hip thrust") -> true
            lowerName.contains("glute bridge") -> true
            lowerName.contains("hyperextension") -> true
            lowerName.contains("back extension") -> true
            lowerName.contains("reverse hyper") -> true
            lowerName.contains("leg curl") -> true
            lowerName.contains("nordic") -> true

            // OHP-artige ohne Korrelation
            lowerName.contains("dumbbell") && lowerName.contains("shoulder") -> true
            lowerName.contains("db") && lowerName.contains("shoulder") -> true
            lowerName.contains("arnold") -> true
            lowerName.contains("landmine") -> true
            lowerName.contains("lateral raise") -> true
            lowerName.contains("front raise") -> true
            lowerName.contains("face pull") -> true
            lowerName.contains("upright row") -> true
            lowerName.contains("shrug") -> true

            // ISOLATION/MACHINE Übungen
            lowerName.contains("curl") -> true  // Alle Curls
            lowerName.contains("extension") && !lowerName.contains("back") -> true  // Tricep Extensions, Leg Extensions
            lowerName.contains("pushdown") -> true
            lowerName.contains("kickback") -> true
            lowerName.contains("pulldown") -> true
            lowerName.contains("pull up") || lowerName.contains("pullup") -> true
            lowerName.contains("chin up") || lowerName.contains("chinup") -> true
            lowerName.contains("row") -> true  // Alle Rows
            lowerName.contains("rudern") -> true
            lowerName.contains("calf") -> true
            lowerName.contains("adductor") -> true
            lowerName.contains("abductor") -> true
            lowerName.contains("plank") -> true
            lowerName.contains("crunch") -> true
            lowerName.contains("ab wheel") -> true
            lowerName.contains("russian twist") -> true
            lowerName.contains("leg raise") -> true

            // OLYMPIC LIFTS
            lowerName.contains("clean") -> true
            lowerName.contains("snatch") && !lowerName.contains("deadlift") -> true
            lowerName.contains("jerk") && !lowerName.contains("push") -> true

            else -> false
        }
    }

    /**
     * Fuzzy matching für Übungsnamen die nicht exakt matchen
     * Gibt null zurück wenn die Übung keine Korrelation hat
     */
    private fun findFuzzyMatch(exerciseName: String): ExerciseRelation? {
        val lowerName = exerciseName.lowercase()

        // ⚠️ ZUERST: Prüfen ob die Übung KEINE Korrelation hat
        if (hasNoCorrelation(exerciseName)) {
            return null  // Keine Empfehlung für diese Übung
        }

        // Squat detection (nur für Barbell Squats mit Korrelation)
        if (lowerName.contains("squat") || lowerName.contains("kniebeuge")) {
            return when {
                lowerName.contains("front") -> ExerciseRelation(LiftType.SQUAT, 0.85f)
                lowerName.contains("pause") -> ExerciseRelation(LiftType.SQUAT, 0.85f)
                lowerName.contains("tempo") -> ExerciseRelation(LiftType.SQUAT, 0.75f)
                lowerName.contains("pin") || lowerName.contains("bottom") -> ExerciseRelation(LiftType.SQUAT, 0.80f)
                lowerName.contains("box") -> ExerciseRelation(LiftType.SQUAT, 0.90f)
                lowerName.contains("safety") || lowerName.contains("ssb") -> ExerciseRelation(LiftType.SQUAT, 0.90f)
                lowerName.contains("zercher") -> ExerciseRelation(LiftType.SQUAT, 0.70f)
                lowerName.contains("anderson") -> ExerciseRelation(LiftType.SQUAT, 0.85f)
                lowerName.contains("high bar") -> ExerciseRelation(LiftType.SQUAT, 0.95f)
                lowerName.contains("low bar") -> ExerciseRelation(LiftType.SQUAT, 1.00f)
                else -> ExerciseRelation(LiftType.SQUAT, 1.00f) // Default Back Squat
            }
        }

        // Bench detection (nur für Barbell Bench mit Korrelation)
        if (lowerName.contains("bench") || lowerName.contains("bankdrücken")) {
            return when {
                lowerName.contains("incline") || lowerName.contains("schräg") -> ExerciseRelation(LiftType.BENCH, 0.75f)
                lowerName.contains("decline") -> ExerciseRelation(LiftType.BENCH, 1.05f)
                lowerName.contains("close") || lowerName.contains("eng") -> ExerciseRelation(LiftType.BENCH, 0.85f)
                lowerName.contains("pause") -> ExerciseRelation(LiftType.BENCH, 0.95f)
                lowerName.contains("floor") -> ExerciseRelation(LiftType.BENCH, 0.85f)
                lowerName.contains("spoto") -> ExerciseRelation(LiftType.BENCH, 0.90f)
                lowerName.contains("larsen") -> ExerciseRelation(LiftType.BENCH, 0.90f)
                lowerName.contains("wide") -> ExerciseRelation(LiftType.BENCH, 0.95f)
                lowerName.contains("touch and go") || lowerName.contains("tng") -> ExerciseRelation(LiftType.BENCH, 1.02f)
                else -> ExerciseRelation(LiftType.BENCH, 1.00f)
            }
        }

        // Deadlift detection (nur für Barbell Deadlifts mit Korrelation)
        if (lowerName.contains("deadlift") || lowerName.contains("kreuzheben")) {
            return when {
                lowerName.contains("sumo") -> ExerciseRelation(LiftType.DEADLIFT, 1.00f)
                lowerName.contains("pause") -> ExerciseRelation(LiftType.DEADLIFT, 0.85f)
                lowerName.contains("deficit") -> ExerciseRelation(LiftType.DEADLIFT, 0.90f)
                lowerName.contains("block") -> ExerciseRelation(LiftType.DEADLIFT, 1.10f)
                lowerName.contains("rack") -> ExerciseRelation(LiftType.DEADLIFT, 1.20f)
                lowerName.contains("romanian") || lowerName.contains("rdl") -> ExerciseRelation(LiftType.DEADLIFT, 0.65f)
                lowerName.contains("stiff") -> ExerciseRelation(LiftType.DEADLIFT, 0.60f)
                lowerName.contains("trap") || lowerName.contains("hex") -> ExerciseRelation(LiftType.DEADLIFT, 1.10f)
                lowerName.contains("snatch grip") -> ExerciseRelation(LiftType.DEADLIFT, 0.80f)
                else -> ExerciseRelation(LiftType.DEADLIFT, 1.00f)
            }
        }

        // Press detection (nur für Barbell OHP mit Korrelation)
        if (lowerName.contains("press") || lowerName.contains("drücken")) {
            // Exclude bench press (already handled)
            if (!lowerName.contains("bench") && !lowerName.contains("bank")) {
                return when {
                    lowerName.contains("push") && lowerName.contains("jerk") -> ExerciseRelation(LiftType.OVERHEAD, 1.35f)
                    lowerName.contains("push") -> ExerciseRelation(LiftType.OVERHEAD, 1.20f)
                    lowerName.contains("split jerk") -> ExerciseRelation(LiftType.OVERHEAD, 1.40f)
                    lowerName.contains("seated") || lowerName.contains("sitzend") -> ExerciseRelation(LiftType.OVERHEAD, 0.90f)
                    lowerName.contains("behind") || lowerName.contains("btn") -> ExerciseRelation(LiftType.OVERHEAD, 0.85f)
                    lowerName.contains("z press") || lowerName.contains("z-press") -> ExerciseRelation(LiftType.OVERHEAD, 0.75f)
                    lowerName.contains("military") -> ExerciseRelation(LiftType.OVERHEAD, 1.00f)
                    lowerName.contains("overhead") || lowerName.contains("ohp") -> ExerciseRelation(LiftType.OVERHEAD, 1.00f)
                    lowerName.contains("strict") -> ExerciseRelation(LiftType.OVERHEAD, 1.00f)
                    else -> null  // Unbekannte Press-Variante → keine Empfehlung
                }
            }
        }

        // Good morning (Barbell → hat Korrelation zu Deadlift)
        if (lowerName.contains("good morning") || (lowerName == "gm")) {
            return ExerciseRelation(LiftType.DEADLIFT, 0.45f)
        }

        // Keine Korrelation gefunden
        return null
    }
}

/**
 * Relation einer Übung zum Haupt-Lift
 */
data class ExerciseRelation(
    val baseLift: LiftType,
    val percentage: Float
)

// ==========================================
// EXTENSION FUNCTIONS
// ==========================================

/**
 * Berechnet das empfohlene Gewicht für eine Übung basierend auf User's PRs
 */
fun UserStrengthProfile.calculateRecommendedWeight(
    exerciseName: String,
    targetReps: Int,
    roundTo: Float = 2.5f
): Float? {
    val relation = ExercisePercentages.getExercisePercentage(exerciseName) ?: return null

    // Hole 1RM des Basis-Lifts
    val base1RM = when (relation.baseLift) {
        LiftType.SQUAT -> currentSquatKg
        LiftType.BENCH -> currentBenchKg
        LiftType.DEADLIFT -> currentDeadliftKg
        LiftType.OVERHEAD -> currentOverheadKg ?: (currentBenchKg * 0.65f)
    }

    // Berechne 1RM der Variante
    val variant1RM = base1RM * relation.percentage

    // Berechne Gewicht für Ziel-Reps
    return RepMaxCalculator.calculateWeightForReps(variant1RM, targetReps, roundTo)
}

/**
 * Gibt eine vollständige Gewichtsempfehlung mit Kontext zurück
 */
fun UserStrengthProfile.getWeightRecommendation(
    exerciseName: String,
    targetReps: Int,
    roundTo: Float = 2.5f
): WeightRecommendation? {
    val relation = ExercisePercentages.getExercisePercentage(exerciseName) ?: return null

    val base1RM = when (relation.baseLift) {
        LiftType.SQUAT -> currentSquatKg
        LiftType.BENCH -> currentBenchKg
        LiftType.DEADLIFT -> currentDeadliftKg
        LiftType.OVERHEAD -> currentOverheadKg ?: (currentBenchKg * 0.65f)
    }

    val variant1RM = base1RM * relation.percentage
    val repPercentage = RepMaxCalculator.getPercentageForReps(targetReps)
    val recommendedWeight = RepMaxCalculator.roundToPlate(variant1RM * repPercentage, roundTo)

    return WeightRecommendation(
        exerciseName = exerciseName,
        baseLift = relation.baseLift,
        base1RM = base1RM,
        variantPercentage = relation.percentage,
        variant1RM = variant1RM,
        targetReps = targetReps,
        repPercentage = repPercentage,
        recommendedWeight = recommendedWeight,
        lowerBound = RepMaxCalculator.roundDownToPlate(recommendedWeight * 0.95f, roundTo),
        upperBound = RepMaxCalculator.roundToPlate(recommendedWeight * 1.05f, roundTo)
    )
}

// ==========================================
// ILB (INDIVIDUELLES LEISTUNGSBILD) SYSTEM
// ==========================================

/**
 * ILB Test Result - Ergebnis eines AMRAP-Tests
 */
data class ILBTestResult(
    val exerciseId: String,
    val exerciseName: String,
    val testWeight: Float,        // Gewicht beim Test
    val testReps: Int,            // AMRAP Wiederholungen
    val previous1RM: Float?,      // Vorheriger 1RM (falls bekannt)
    val new1RM: Float,            // Neu berechneter 1RM
    val changePercent: Float?,    // % Veränderung
    val changeKg: Float?,         // kg Veränderung
    val testDate: String = java.time.LocalDate.now().toString()
) {
    val isImproved: Boolean get() = (changePercent ?: 0f) > 0
    val isDeclined: Boolean get() = (changePercent ?: 0f) < -2f // 2% Toleranz

    fun getDisplayMessage(): String {
        return when {
            changePercent == null -> "Erster Test: ${new1RM.toInt()}kg 1RM"
            changePercent > 0 -> "+${changeKg?.toInt()}kg (+${changePercent.toInt()}%)"
            changePercent < -2f -> "${changeKg?.toInt()}kg (${changePercent.toInt()}%)"
            else -> "Stabil (${new1RM.toInt()}kg)"
        }
    }
}

/**
 * ILB Warmup Set - Generierter Aufwärmsatz für ILB Test
 */
data class ILBWarmupSet(
    val setNumber: Int,
    val percentage: Float,        // % des Arbeitsgewichts
    val weight: Float,            // Tatsächliches Gewicht
    val targetReps: Int           // Ziel-Wiederholungen
)

/**
 * ILB Test Configuration für eine Übung
 */
data class ILBTestConfig(
    val exerciseId: String,
    val exerciseName: String,
    val testWeight: Float,        // Gewicht für AMRAP
    val warmupSets: List<ILBWarmupSet>,
    val amrapSets: Int = 3,       // Anzahl AMRAP-Versuche
    val restBetweenAmrap: Int = 120 // Sekunden Pause zwischen AMRAPs
)

/**
 * ILB Calculator - Berechnet Tests und verarbeitet Ergebnisse
 */
object ILBCalculator {

    /**
     * Compound-Übungen die automatisch für ILB getestet werden
     */
    val COMPOUND_EXERCISES = setOf(
        // Squat Familie
        "Back Squat", "Front Squat", "Box Squat", "Pause Squat",
        "High Bar Squat", "Low Bar Squat", "Safety Bar Squat",
        // Bench Familie
        "Bench Press", "Incline Bench", "Close Grip Bench",
        "Pause Bench", "Floor Press",
        // Deadlift Familie
        "Deadlift", "Sumo Deadlift", "Romanian Deadlift", "Trap Bar Deadlift",
        "Conventional Deadlift", "Deficit Deadlift", "Block Pull",
        // Press Familie
        "Overhead Press", "Push Press", "Seated Press", "Military Press",
        "Strict Press",
        // Rows
        "Barbell Row", "Pendlay Row", "T-Bar Row",
        // Weitere Compounds
        "Pull Up", "Chin Up", "Dip", "Hip Thrust", "Leg Press"
    )

    /**
     * Prüft ob eine Übung automatisch ILB-getestet werden soll
     */
    fun isCompoundExercise(exerciseName: String): Boolean {
        val lowerName = exerciseName.lowercase()
        return COMPOUND_EXERCISES.any { it.lowercase() == lowerName } ||
               // Fuzzy matching für Varianten
               lowerName.contains("squat") && !ExercisePercentages.hasNoCorrelation(exerciseName) ||
               lowerName.contains("bench") && !ExercisePercentages.hasNoCorrelation(exerciseName) ||
               lowerName.contains("deadlift") && !ExercisePercentages.hasNoCorrelation(exerciseName) ||
               lowerName.contains("press") && !lowerName.contains("leg") && !ExercisePercentages.hasNoCorrelation(exerciseName)
    }

    /**
     * Generiert Warmup-Sets für einen ILB Test
     *
     * Struktur:
     * - Set 1: 50% × 8 reps (leicht)
     * - Set 2: 60% × 5 reps
     * - Set 3: 70% × 3 reps
     * - Set 4: 80% × 2 reps
     * Dann: AMRAP @ 100%
     */
    fun generateWarmupSets(
        testWeight: Float,
        roundTo: Float = 2.5f
    ): List<ILBWarmupSet> {
        return listOf(
            ILBWarmupSet(1, 0.50f, RepMaxCalculator.roundToPlate(testWeight * 0.50f, roundTo), 8),
            ILBWarmupSet(2, 0.60f, RepMaxCalculator.roundToPlate(testWeight * 0.60f, roundTo), 5),
            ILBWarmupSet(3, 0.70f, RepMaxCalculator.roundToPlate(testWeight * 0.70f, roundTo), 3),
            ILBWarmupSet(4, 0.80f, RepMaxCalculator.roundToPlate(testWeight * 0.80f, roundTo), 2)
        )
    }

    /**
     * Generiert komplette ILB Test-Konfiguration für eine Übung
     */
    fun generateTestConfig(
        exerciseId: String,
        exerciseName: String,
        currentWorkingWeight: Float,
        roundTo: Float = 2.5f
    ): ILBTestConfig {
        val testWeight = RepMaxCalculator.roundToPlate(currentWorkingWeight, roundTo)
        return ILBTestConfig(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            testWeight = testWeight,
            warmupSets = generateWarmupSets(testWeight, roundTo)
        )
    }

    /**
     * Verarbeitet ein AMRAP-Ergebnis und berechnet neuen 1RM
     *
     * @param exerciseId ID der Übung
     * @param exerciseName Name der Übung
     * @param testWeight Gewicht beim AMRAP
     * @param amrapReps Anzahl geschaffter Wiederholungen
     * @param previous1RM Vorheriger 1RM (optional)
     * @return ILBTestResult mit neuem 1RM und Vergleich
     */
    fun processAMRAPResult(
        exerciseId: String,
        exerciseName: String,
        testWeight: Float,
        amrapReps: Int,
        previous1RM: Float? = null
    ): ILBTestResult {
        // Berechne neuen 1RM mit Hybrid-Formel
        val new1RM = RepMaxCalculator.calculate1RM(testWeight, amrapReps)

        // Berechne Veränderung falls vorheriger Wert bekannt
        val changeKg = previous1RM?.let { new1RM - it }
        val changePercent = previous1RM?.let {
            if (it > 0) ((new1RM - it) / it * 100) else null
        }

        return ILBTestResult(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            testWeight = testWeight,
            testReps = amrapReps,
            previous1RM = previous1RM,
            new1RM = new1RM,
            changePercent = changePercent,
            changeKg = changeKg
        )
    }

    /**
     * Wählt das beste Ergebnis aus mehreren AMRAP-Versuchen
     * (User macht 2-3 AMRAPs, bester Wert zählt)
     */
    fun selectBestAMRAP(results: List<ILBTestResult>): ILBTestResult? {
        return results.maxByOrNull { it.new1RM }
    }

    /**
     * Berechnet neue Arbeitsgewichte basierend auf neuem 1RM
     *
     * @param new1RM Neuer 1RM nach ILB Test
     * @param targetReps Ziel-Wiederholungen für Arbeitssets
     * @param roundTo Rundung (default 2.5kg)
     * @return Neues empfohlenes Arbeitsgewicht
     */
    fun calculateNewWorkingWeight(
        new1RM: Float,
        targetReps: Int,
        roundTo: Float = 2.5f
    ): Float {
        return RepMaxCalculator.calculateWeightForReps(new1RM, targetReps, roundTo)
    }

    /**
     * Erstellt Übersicht der Gewichtsänderungen nach ILB Test
     */
    fun getWeightChangePreview(
        oldOneRM: Float?,
        newOneRM: Float,
        repRanges: List<Int> = listOf(3, 5, 8, 10, 12),
        roundTo: Float = 2.5f
    ): List<WeightChangePreview> {
        return repRanges.map { reps ->
            val oldWeight = oldOneRM?.let {
                RepMaxCalculator.calculateWeightForReps(it, reps, roundTo)
            }
            val newWeight = RepMaxCalculator.calculateWeightForReps(newOneRM, reps, roundTo)

            WeightChangePreview(
                reps = reps,
                oldWeight = oldWeight,
                newWeight = newWeight,
                change = oldWeight?.let { newWeight - it }
            )
        }
    }
}

/**
 * Vorschau der Gewichtsänderung für verschiedene Rep-Ranges
 */
data class WeightChangePreview(
    val reps: Int,
    val oldWeight: Float?,
    val newWeight: Float,
    val change: Float?
) {
    fun getDisplayString(): String {
        return if (change != null && change != 0f) {
            "${reps} Reps: ${newWeight.toInt()}kg (${if (change > 0) "+" else ""}${change.toInt()}kg)"
        } else {
            "${reps} Reps: ${newWeight.toInt()}kg"
        }
    }
}

/**
 * Vollständige Gewichtsempfehlung mit allen Berechnungsdetails
 */
data class WeightRecommendation(
    val exerciseName: String,
    val baseLift: LiftType,
    val base1RM: Float,           // User's 1RM des Haupt-Lifts
    val variantPercentage: Float, // % dieser Variante vom Haupt-Lift
    val variant1RM: Float,        // Geschätzter 1RM dieser Variante
    val targetReps: Int,
    val repPercentage: Float,     // % für diese Rep-Anzahl
    val recommendedWeight: Float, // Finale Empfehlung
    val lowerBound: Float,        // -5% für leichtere Tage
    val upperBound: Float         // +5% für stärkere Tage
) {
    /**
     * Formatierte Darstellung für UI
     */
    fun toDisplayString(): String {
        return "${recommendedWeight.toInt()}kg (${lowerBound.toInt()}-${upperBound.toInt()}kg)"
    }

    /**
     * Erklärung der Berechnung
     */
    fun getExplanation(): String {
        return buildString {
            append("Based on your ${baseLift.name.lowercase().replaceFirstChar { it.uppercase() }} PR of ${base1RM.toInt()}kg:\n")
            append("• $exerciseName ≈ ${(variantPercentage * 100).toInt()}% = ${variant1RM.toInt()}kg 1RM\n")
            append("• For $targetReps reps @ ${(repPercentage * 100).toInt()}%\n")
            append("• Recommended: ${recommendedWeight.toInt()}kg")
        }
    }
}
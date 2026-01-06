package com.example.menotracker.onboarding.data

/**
 * PROMETHEUS SPORT DATABASE
 *
 * Comprehensive database of 135+ sports organized by category.
 * Each sport has:
 * - Training focus weights (6 factors: Kraft, Schnelligkeit, Ausdauer, Beweglichkeit, Geschicklichkeit, Mindset)
 * - VBT relevance score (how useful is velocity-based training)
 * - Primary exercises that are most beneficial
 *
 * The "Traktor-Prinzip": One powerful engine (Naya Core) with
 * interchangeable attachments (sport-specific training focus)
 */

// ═══════════════════════════════════════════════════════════════
// SPORT CATEGORY - Top level groupings with icons
// ═══════════════════════════════════════════════════════════════

enum class SportCategory(
    val displayName: String,
    val iconName: String,  // Material icon name for reference
    val description: String
) {
    STRENGTH("Strength", "fitness_center", "Weightlifting, Powerlifting, Strongman"),
    BALL("Ball Sports", "sports_soccer", "Soccer, Basketball, Tennis, Golf"),
    COMBAT("Combat", "sports_mma", "Boxing, MMA, Judo, Wrestling"),
    ENDURANCE("Endurance", "directions_run", "Running, Triathlon, Cycling"),
    RACKET("Racket", "sports_tennis", "Tennis, Badminton, Squash"),
    WATER("Water", "pool", "Swimming, Rowing, Surfing"),
    WINTER("Winter", "ac_unit", "Ski, Snowboard, Ice Hockey"),
    ATHLETICS("Athletics", "emoji_events", "Sprint, Throw, Jump"),
    GYMNASTICS("Gymnastics", "accessibility_new", "Apparatus, Acrobatics, Calisthenics"),
    OUTDOOR("Outdoor", "terrain", "Climbing, Hiking, Mountain Biking"),
    PRECISION("Precision", "gps_fixed", "Golf, Archery, Shooting"),
    TEAM("Team", "groups", "Team Sports"),
    OTHER("Other", "sports", "Other Sports")
}

// ═══════════════════════════════════════════════════════════════
// BENCHMARK TYPES - Sport-specific performance metrics
// ═══════════════════════════════════════════════════════════════

/**
 * Different types of benchmarks based on sport category
 * This replaces the old LiftType enum with a universal system
 */
enum class BenchmarkType {
    WEIGHT,      // kg - Lift PRs (Squat: 140kg)
    TIME,        // seconds/minutes - Race times (5k: 22:30)
    DISTANCE,    // meters - Throw/Jump distances (Shot Put: 15m)
    REPS,        // count - Max reps (Pull-ups: 15)
    HEIGHT,      // cm - Jump height (Vertical: 60cm)
    SPEED,       // km/h or m/s - Sprint speed
    SCORE,       // points - Skill scores (Gymnastics: 9.5)
    LEVEL        // tier - Skill level (Climbing: V6)
}

/**
 * A single benchmark definition
 * Used in onboarding to collect sport-specific performance data
 */
data class BenchmarkDefinition(
    val id: String,                    // Unique identifier
    val name: String,                  // Display name (e.g., "Back Squat")
    val nameDE: String,                // German name
    val type: BenchmarkType,           // What unit to use
    val unit: String,                  // Display unit (kg, min, m, reps)
    val placeholder: String,           // Example value
    val isOptional: Boolean = true,    // Can be skipped
    val description: String? = null,   // Help text
    @Deprecated("VBT capability is now checked from database via ExerciseRepository.isVbtCapable()")
    val vbtCapable: Boolean = false,   // DEPRECATED: Use ExerciseRepository.isVbtCapable() instead
    val exerciseId: String? = null     // Links to Exercise in database for VBT tracking
)

/**
 * Category-specific benchmark collections
 * Maps SportCategory to relevant benchmarks
 */
object SportBenchmarks {

    // ═══════════════════════════════════════════════════════════════
    // STRENGTH BENCHMARKS - For Powerlifting, Weightlifting, etc.
    // VBT capability is controlled via database (superadmin only)
    // ═══════════════════════════════════════════════════════════════
    val strengthBenchmarks = listOf(
        BenchmarkDefinition("squat", "Back Squat", "Kniebeuge", BenchmarkType.WEIGHT, "kg", "140"),
        BenchmarkDefinition("bench", "Bench Press", "Bankdrücken", BenchmarkType.WEIGHT, "kg", "100"),
        BenchmarkDefinition("deadlift", "Deadlift", "Kreuzheben", BenchmarkType.WEIGHT, "kg", "180"),
        BenchmarkDefinition("ohp", "Overhead Press", "Schulterdrücken", BenchmarkType.WEIGHT, "kg", "60"),
        BenchmarkDefinition("snatch", "Snatch", "Reißen", BenchmarkType.WEIGHT, "kg", "80"),
        BenchmarkDefinition("clean_jerk", "Clean & Jerk", "Stoßen", BenchmarkType.WEIGHT, "kg", "100"),
        BenchmarkDefinition("front_squat", "Front Squat", "Frontkniebeuge", BenchmarkType.WEIGHT, "kg", "110"),
        BenchmarkDefinition("power_clean", "Power Clean", "Umsetzen", BenchmarkType.WEIGHT, "kg", "90")
    )

    // ═══════════════════════════════════════════════════════════════
    // ENDURANCE BENCHMARKS - For Running, Triathlon, Cycling
    // ═══════════════════════════════════════════════════════════════
    val enduranceBenchmarks = listOf(
        BenchmarkDefinition("5k", "5K Run", "5 km Lauf", BenchmarkType.TIME, "min", "22:00"),
        BenchmarkDefinition("10k", "10K Run", "10 km Lauf", BenchmarkType.TIME, "min", "45:00"),
        BenchmarkDefinition("half_marathon", "Half Marathon", "Halbmarathon", BenchmarkType.TIME, "h:min", "1:45"),
        BenchmarkDefinition("marathon", "Marathon", "Marathon", BenchmarkType.TIME, "h:min", "3:30"),
        BenchmarkDefinition("ftp", "FTP (Cycling)", "FTP (Rad)", BenchmarkType.WEIGHT, "W", "250"),
        BenchmarkDefinition("vo2max", "VO2max", "VO2max", BenchmarkType.SCORE, "ml/kg/min", "50")
    )

    // ═══════════════════════════════════════════════════════════════
    // ATHLETIC BENCHMARKS - For Ball Sports, Athletics, Combat
    // ═══════════════════════════════════════════════════════════════
    val athleticBenchmarks = listOf(
        BenchmarkDefinition("vertical_jump", "Vertical Jump", "Vertikalsprung", BenchmarkType.HEIGHT, "cm", "55"),
        BenchmarkDefinition("broad_jump", "Broad Jump", "Weitsprung (Stand)", BenchmarkType.DISTANCE, "m", "2.50"),
        BenchmarkDefinition("40m_sprint", "40m Sprint", "40m Sprint", BenchmarkType.TIME, "sec", "5.2"),
        BenchmarkDefinition("100m_sprint", "100m Sprint", "100m Sprint", BenchmarkType.TIME, "sec", "12.5"),
        BenchmarkDefinition("agility_5_10_5", "5-10-5 Shuttle", "5-10-5 Shuttle", BenchmarkType.TIME, "sec", "4.8")
    )

    // ═══════════════════════════════════════════════════════════════
    // BODYWEIGHT BENCHMARKS - For Gymnastics, Calisthenics, Combat
    // ═══════════════════════════════════════════════════════════════
    val bodyweightBenchmarks = listOf(
        BenchmarkDefinition("pullups", "Pull-ups", "Klimmzüge", BenchmarkType.REPS, "reps", "12"),
        BenchmarkDefinition("pushups", "Push-ups", "Liegestütze", BenchmarkType.REPS, "reps", "40"),
        BenchmarkDefinition("dips", "Dips", "Dips", BenchmarkType.REPS, "reps", "20"),
        BenchmarkDefinition("handstand_hold", "Handstand Hold", "Handstand halten", BenchmarkType.TIME, "sec", "30"),
        BenchmarkDefinition("l_sit_hold", "L-Sit Hold", "L-Sitz halten", BenchmarkType.TIME, "sec", "20"),
        BenchmarkDefinition("muscle_ups", "Muscle-ups", "Muscle-ups", BenchmarkType.REPS, "reps", "5")
    )

    // ═══════════════════════════════════════════════════════════════
    // WATER BENCHMARKS - For Swimming, Rowing
    // ═══════════════════════════════════════════════════════════════
    val waterBenchmarks = listOf(
        BenchmarkDefinition("50m_swim", "50m Freestyle", "50m Freistil", BenchmarkType.TIME, "sec", "30"),
        BenchmarkDefinition("100m_swim", "100m Freestyle", "100m Freistil", BenchmarkType.TIME, "min", "1:05"),
        BenchmarkDefinition("400m_swim", "400m Freestyle", "400m Freistil", BenchmarkType.TIME, "min", "5:00"),
        BenchmarkDefinition("2k_row", "2K Row (Erg)", "2km Rudern (Erg)", BenchmarkType.TIME, "min", "7:30"),
        BenchmarkDefinition("500m_row", "500m Row (Erg)", "500m Rudern (Erg)", BenchmarkType.TIME, "min", "1:40")
    )

    // ═══════════════════════════════════════════════════════════════
    // COMBAT BENCHMARKS - For MMA, Boxing, Wrestling
    // ═══════════════════════════════════════════════════════════════
    val combatBenchmarks = listOf(
        BenchmarkDefinition("pullups", "Pull-ups", "Klimmzüge", BenchmarkType.REPS, "reps", "15"),
        BenchmarkDefinition("conditioning_rounds", "Sparring Rounds", "Sparring Runden", BenchmarkType.REPS, "rounds", "5"),
        BenchmarkDefinition("grip_hang", "Dead Hang", "Aushängen", BenchmarkType.TIME, "sec", "60"),
        BenchmarkDefinition("burpees_1min", "Burpees (1 min)", "Burpees (1 min)", BenchmarkType.REPS, "reps", "20")
    )

    // ═══════════════════════════════════════════════════════════════
    // CLIMBING BENCHMARKS - For Rock Climbing, Bouldering
    // ═══════════════════════════════════════════════════════════════
    val climbingBenchmarks = listOf(
        BenchmarkDefinition("boulder_grade", "Boulder Grade", "Boulder Grad", BenchmarkType.LEVEL, "V-Grade", "V5"),
        BenchmarkDefinition("lead_grade", "Lead Grade", "Vorstieg Grad", BenchmarkType.LEVEL, "Grade", "7a"),
        BenchmarkDefinition("pullups", "Pull-ups", "Klimmzüge", BenchmarkType.REPS, "reps", "15"),
        BenchmarkDefinition("hang_20mm", "20mm Edge Hang", "20mm Leiste hängen", BenchmarkType.TIME, "sec", "30")
    )

    // ═══════════════════════════════════════════════════════════════
    // FLEXIBILITY BENCHMARKS - For Yoga, Dance, Gymnastics
    // ═══════════════════════════════════════════════════════════════
    val flexibilityBenchmarks = listOf(
        BenchmarkDefinition("toe_touch", "Toe Touch", "Zehenberührung", BenchmarkType.DISTANCE, "cm", "+10"),
        BenchmarkDefinition("split_front", "Front Split", "Spagat frontal", BenchmarkType.LEVEL, "level", "Full"),
        BenchmarkDefinition("split_side", "Side Split", "Seitspagat", BenchmarkType.LEVEL, "level", "Full"),
        BenchmarkDefinition("bridge", "Bridge Hold", "Brücke halten", BenchmarkType.TIME, "sec", "30")
    )

    // ═══════════════════════════════════════════════════════════════
    // GENERAL FITNESS - Baseline for all athletes
    // ═══════════════════════════════════════════════════════════════
    val generalBenchmarks = listOf(
        BenchmarkDefinition("pushups", "Push-ups", "Liegestütze", BenchmarkType.REPS, "reps", "30"),
        BenchmarkDefinition("pullups", "Pull-ups", "Klimmzüge", BenchmarkType.REPS, "reps", "10"),
        BenchmarkDefinition("plank", "Plank Hold", "Unterarmstütz", BenchmarkType.TIME, "sec", "60"),
        BenchmarkDefinition("run_1_5mi", "1.5 Mile Run", "2.4 km Lauf", BenchmarkType.TIME, "min", "12:00")
    )

    /**
     * Get relevant benchmarks for a sport based on its category and primary exercises
     */
    fun getBenchmarksForSport(sport: UnifiedSport): List<BenchmarkDefinition> {
        val benchmarks = mutableListOf<BenchmarkDefinition>()

        when (sport.category) {
            SportCategory.STRENGTH -> {
                // Filter strength benchmarks based on primary exercises
                val primaryLower = sport.primaryExercises.map { it.lowercase() }
                strengthBenchmarks.forEach { benchmark ->
                    if (primaryLower.any { it.contains(benchmark.name.lowercase().split(" ")[0]) } ||
                        benchmark.id in listOf("squat", "bench", "deadlift")) {
                        benchmarks.add(benchmark)
                    }
                }
                // Special handling for Olympic weightlifting
                if (sport.id == "weightlifting") {
                    benchmarks.clear()
                    benchmarks.addAll(strengthBenchmarks.filter {
                        it.id in listOf("snatch", "clean_jerk", "front_squat", "squat")
                    })
                }
            }
            SportCategory.ENDURANCE -> {
                benchmarks.addAll(enduranceBenchmarks.take(4)) // Common running times
            }
            SportCategory.BALL, SportCategory.TEAM -> {
                benchmarks.addAll(athleticBenchmarks)
                benchmarks.addAll(bodyweightBenchmarks.take(2)) // Pull-ups, Push-ups
            }
            SportCategory.COMBAT -> {
                benchmarks.addAll(combatBenchmarks)
                benchmarks.add(strengthBenchmarks.first { it.id == "deadlift" })
            }
            SportCategory.WATER -> {
                benchmarks.addAll(waterBenchmarks)
            }
            SportCategory.GYMNASTICS -> {
                benchmarks.addAll(bodyweightBenchmarks)
                benchmarks.addAll(flexibilityBenchmarks.take(2))
            }
            SportCategory.OUTDOOR -> {
                if (sport.id.contains("climb") || sport.id.contains("boulder")) {
                    benchmarks.addAll(climbingBenchmarks)
                } else {
                    benchmarks.addAll(enduranceBenchmarks.take(2))
                    benchmarks.addAll(generalBenchmarks.take(2))
                }
            }
            SportCategory.ATHLETICS -> {
                // Track & Field - depends on event type
                if (sport.primaryExercises.any { it.contains("Sprint") }) {
                    benchmarks.addAll(athleticBenchmarks.filter { it.id.contains("sprint") })
                }
                if (sport.primaryExercises.any { it.contains("Jump") }) {
                    benchmarks.addAll(athleticBenchmarks.filter { it.id.contains("jump") })
                }
                if (sport.primaryExercises.any { it.contains("Throw") }) {
                    benchmarks.add(strengthBenchmarks.first { it.id == "squat" })
                    benchmarks.add(strengthBenchmarks.first { it.id == "bench" })
                }
                if (benchmarks.isEmpty()) {
                    benchmarks.addAll(athleticBenchmarks)
                }
            }
            SportCategory.RACKET -> {
                benchmarks.addAll(athleticBenchmarks.filter {
                    it.id in listOf("40m_sprint", "agility_5_10_5", "vertical_jump")
                })
            }
            SportCategory.WINTER -> {
                benchmarks.addAll(athleticBenchmarks.take(3))
                benchmarks.addAll(bodyweightBenchmarks.take(2))
            }
            SportCategory.PRECISION -> {
                // Golf, Archery, etc. - minimal physical benchmarks
                benchmarks.addAll(generalBenchmarks.take(2))
            }
            SportCategory.OTHER -> {
                benchmarks.addAll(generalBenchmarks)
            }
        }

        return benchmarks.distinctBy { it.id }.take(6) // Max 6 benchmarks
    }

    /**
     * Check if a sport category needs benchmarks at all
     * Some sports (e.g., Precision sports) may skip this step
     */
    fun shouldShowBenchmarks(sport: UnifiedSport): Boolean {
        return when (sport.category) {
            SportCategory.PRECISION -> false  // Golf, Archery - skip benchmarks
            SportCategory.OTHER -> sport.vbtRelevance > 0.3f
            else -> true
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TRAINING FOCUS - The 6 conditioning factors
// ═══════════════════════════════════════════════════════════════

data class TrainingFocus(
    val kraft: Float = 0f,           // Strength / Power
    val schnelligkeit: Float = 0f,   // Speed / Explosiveness
    val ausdauer: Float = 0f,        // Endurance / Stamina
    val beweglichkeit: Float = 0f,   // Flexibility / Mobility
    val geschicklichkeit: Float = 0f, // Skill / Coordination
    val mindset: Float = 0f          // Mental / Focus
) {
    // Normalize all values to ensure they sum to reasonable weights
    fun normalized(): TrainingFocus {
        val sum = kraft + schnelligkeit + ausdauer + beweglichkeit + geschicklichkeit + mindset
        if (sum == 0f) return this
        return TrainingFocus(
            kraft / sum,
            schnelligkeit / sum,
            ausdauer / sum,
            beweglichkeit / sum,
            geschicklichkeit / sum,
            mindset / sum
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// UNIFIED SPORT - Single sport entry with all metadata
// ═══════════════════════════════════════════════════════════════

data class UnifiedSport(
    val id: String,
    val name: String,
    val nameDE: String,              // German name for search
    val category: SportCategory,
    val trainingFocus: TrainingFocus,
    val vbtRelevance: Float,         // 0.0 - 1.0: How useful is VBT
    val nutritionRelevance: Float,   // 0.0 - 1.0: How important is nutrition tracking
    val keywords: List<String> = emptyList(),  // Additional search terms
    val primaryExercises: List<String> = emptyList() // Exercise names to prioritize
)

// ═══════════════════════════════════════════════════════════════
// SPORT DATABASE - All 135+ sports
// ═══════════════════════════════════════════════════════════════

object SportDatabase {

    val allSports: List<UnifiedSport> = listOf(
        // ═══════════════════════════════════════════════════════════════
        // STRENGTH SPORTS (💪)
        // ═══════════════════════════════════════════════════════════════
        UnifiedSport(
            id = "powerlifting",
            name = "Powerlifting",
            nameDE = "Kraftdreikampf",
            category = SportCategory.STRENGTH,
            trainingFocus = TrainingFocus(kraft = 1.0f, schnelligkeit = 0.3f, mindset = 0.7f),
            vbtRelevance = 1.0f,
            nutritionRelevance = 0.6f,
            keywords = listOf("squat", "bench", "deadlift", "sbd", "ipf"),
            primaryExercises = listOf("Back Squat", "Bench Press", "Deadlift")
        ),
        UnifiedSport(
            id = "weightlifting",
            name = "Olympic Weightlifting",
            nameDE = "Gewichtheben",
            category = SportCategory.STRENGTH,
            trainingFocus = TrainingFocus(kraft = 0.9f, schnelligkeit = 0.9f, beweglichkeit = 0.7f, geschicklichkeit = 0.8f),
            vbtRelevance = 1.0f,
            nutritionRelevance = 0.5f,
            keywords = listOf("snatch", "clean", "jerk", "olympic", "iwf"),
            primaryExercises = listOf("Snatch", "Clean and Jerk", "Front Squat", "Overhead Squat")
        ),
        UnifiedSport(
            id = "strongman",
            name = "Strongman",
            nameDE = "Strongman",
            category = SportCategory.STRENGTH,
            trainingFocus = TrainingFocus(kraft = 1.0f, schnelligkeit = 0.5f, ausdauer = 0.6f, geschicklichkeit = 0.4f),
            vbtRelevance = 0.7f,
            nutritionRelevance = 0.7f,
            keywords = listOf("atlas", "stones", "yoke", "log", "deadlift"),
            primaryExercises = listOf("Deadlift", "Log Press", "Farmer's Walk", "Atlas Stones")
        ),
        UnifiedSport(
            id = "bodybuilding",
            name = "Bodybuilding",
            nameDE = "Bodybuilding",
            category = SportCategory.STRENGTH,
            trainingFocus = TrainingFocus(kraft = 0.7f, beweglichkeit = 0.3f, mindset = 0.6f),
            vbtRelevance = 0.3f,
            nutritionRelevance = 1.0f,
            keywords = listOf("physique", "muscle", "hypertrophy", "ifbb"),
            primaryExercises = listOf("Bench Press", "Squat", "Lat Pulldown", "Shoulder Press")
        ),
        UnifiedSport(
            id = "crossfit",
            name = "CrossFit",
            nameDE = "CrossFit",
            category = SportCategory.STRENGTH,
            trainingFocus = TrainingFocus(kraft = 0.7f, schnelligkeit = 0.7f, ausdauer = 0.8f, beweglichkeit = 0.5f, geschicklichkeit = 0.6f),
            vbtRelevance = 0.7f,
            nutritionRelevance = 0.7f,
            keywords = listOf("wod", "amrap", "emom", "functional", "fitness"),
            primaryExercises = listOf("Snatch", "Clean", "Thruster", "Pull-up", "Box Jump")
        ),
        UnifiedSport(
            id = "hyrox",
            name = "HYROX",
            nameDE = "HYROX",
            category = SportCategory.STRENGTH,
            trainingFocus = TrainingFocus(kraft = 0.6f, schnelligkeit = 0.5f, ausdauer = 0.9f, geschicklichkeit = 0.4f),
            vbtRelevance = 0.5f,
            nutritionRelevance = 0.8f,
            keywords = listOf("functional", "fitness", "race", "running", "sled"),
            primaryExercises = listOf("Sled Push", "Sled Pull", "Rowing", "Lunges", "Wall Balls")
        ),
        UnifiedSport(
            id = "calisthenics",
            name = "Calisthenics",
            nameDE = "Calisthenics",
            category = SportCategory.GYMNASTICS,
            trainingFocus = TrainingFocus(kraft = 0.8f, beweglichkeit = 0.6f, geschicklichkeit = 0.7f),
            vbtRelevance = 0.4f,
            nutritionRelevance = 0.5f,
            keywords = listOf("bodyweight", "street workout", "bar", "planche", "muscle up"),
            primaryExercises = listOf("Pull-up", "Dip", "Push-up", "Muscle Up", "Handstand")
        ),
        UnifiedSport(
            id = "general_fitness",
            name = "General Fitness",
            nameDE = "Allgemeine Fitness",
            category = SportCategory.STRENGTH,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.3f, ausdauer = 0.5f, beweglichkeit = 0.5f, geschicklichkeit = 0.3f, mindset = 0.4f),
            vbtRelevance = 0.4f,
            nutritionRelevance = 0.6f,
            keywords = listOf("gym", "workout", "health", "wellness"),
            primaryExercises = listOf("Squat", "Bench Press", "Deadlift", "Row", "Shoulder Press")
        ),

        // ═══════════════════════════════════════════════════════════════
        // BALL SPORTS (⚽)
        // ═══════════════════════════════════════════════════════════════
        UnifiedSport(
            id = "football_soccer",
            name = "Soccer",
            nameDE = "Fußball",
            category = SportCategory.BALL,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.8f, ausdauer = 0.9f, beweglichkeit = 0.6f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.6f,
            nutritionRelevance = 0.7f,
            keywords = listOf("football", "fussball", "kicker"),
            primaryExercises = listOf("Squat", "Nordic Curl", "Box Jump", "Single Leg RDL")
        ),
        UnifiedSport(
            id = "basketball",
            name = "Basketball",
            nameDE = "Basketball",
            category = SportCategory.BALL,
            trainingFocus = TrainingFocus(kraft = 0.6f, schnelligkeit = 0.9f, ausdauer = 0.7f, beweglichkeit = 0.5f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.7f,
            nutritionRelevance = 0.6f,
            keywords = listOf("nba", "hoops", "dunk"),
            primaryExercises = listOf("Squat", "Deadlift", "Box Jump", "Lateral Lunge")
        ),
        UnifiedSport(
            id = "volleyball",
            name = "Volleyball",
            nameDE = "Volleyball",
            category = SportCategory.BALL,
            trainingFocus = TrainingFocus(kraft = 0.6f, schnelligkeit = 0.8f, ausdauer = 0.5f, beweglichkeit = 0.6f, geschicklichkeit = 0.7f),
            vbtRelevance = 0.7f,
            nutritionRelevance = 0.5f,
            keywords = listOf("beach", "indoor"),
            primaryExercises = listOf("Squat", "Box Jump", "Shoulder Press", "Core")
        ),
        UnifiedSport(
            id = "handball",
            name = "Handball",
            nameDE = "Handball",
            category = SportCategory.BALL,
            trainingFocus = TrainingFocus(kraft = 0.7f, schnelligkeit = 0.8f, ausdauer = 0.7f, beweglichkeit = 0.5f, geschicklichkeit = 0.7f),
            vbtRelevance = 0.7f,
            nutritionRelevance = 0.6f,
            keywords = listOf("team", "throw"),
            primaryExercises = listOf("Bench Press", "Rotational Throws", "Squat", "Lateral Movement")
        ),
        UnifiedSport(
            id = "rugby",
            name = "Rugby",
            nameDE = "Rugby",
            category = SportCategory.BALL,
            trainingFocus = TrainingFocus(kraft = 0.9f, schnelligkeit = 0.7f, ausdauer = 0.8f, beweglichkeit = 0.4f, geschicklichkeit = 0.5f),
            vbtRelevance = 0.8f,
            nutritionRelevance = 0.8f,
            keywords = listOf("union", "league", "tackle"),
            primaryExercises = listOf("Squat", "Deadlift", "Bench Press", "Power Clean")
        ),
        UnifiedSport(
            id = "american_football",
            name = "American Football",
            nameDE = "American Football",
            category = SportCategory.BALL,
            trainingFocus = TrainingFocus(kraft = 0.9f, schnelligkeit = 0.9f, ausdauer = 0.5f, beweglichkeit = 0.4f, geschicklichkeit = 0.6f),
            vbtRelevance = 0.9f,
            nutritionRelevance = 0.8f,
            keywords = listOf("nfl", "gridiron", "tackle"),
            primaryExercises = listOf("Squat", "Bench Press", "Power Clean", "Deadlift", "40 Yard Dash")
        ),
        UnifiedSport(
            id = "baseball",
            name = "Baseball",
            nameDE = "Baseball",
            category = SportCategory.BALL,
            trainingFocus = TrainingFocus(kraft = 0.6f, schnelligkeit = 0.8f, ausdauer = 0.3f, beweglichkeit = 0.5f, geschicklichkeit = 0.9f),
            vbtRelevance = 0.7f,
            nutritionRelevance = 0.5f,
            keywords = listOf("mlb", "pitching", "batting"),
            primaryExercises = listOf("Rotational Medicine Ball", "Hip Hinge", "Shoulder Stability")
        ),
        UnifiedSport(
            id = "softball",
            name = "Softball",
            nameDE = "Softball",
            category = SportCategory.BALL,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.7f, ausdauer = 0.3f, beweglichkeit = 0.5f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.6f,
            nutritionRelevance = 0.5f,
            keywords = listOf("fastpitch"),
            primaryExercises = listOf("Rotational Core", "Hip Hinge", "Lateral Movement")
        ),
        UnifiedSport(
            id = "hockey_field",
            name = "Field Hockey",
            nameDE = "Feldhockey",
            category = SportCategory.BALL,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.7f, ausdauer = 0.8f, beweglichkeit = 0.6f, geschicklichkeit = 0.7f),
            vbtRelevance = 0.5f,
            nutritionRelevance = 0.6f,
            keywords = listOf("stick"),
            primaryExercises = listOf("Squat", "Lateral Lunge", "Core Rotation")
        ),
        UnifiedSport(
            id = "lacrosse",
            name = "Lacrosse",
            nameDE = "Lacrosse",
            category = SportCategory.BALL,
            trainingFocus = TrainingFocus(kraft = 0.6f, schnelligkeit = 0.8f, ausdauer = 0.7f, beweglichkeit = 0.5f, geschicklichkeit = 0.7f),
            vbtRelevance = 0.6f,
            nutritionRelevance = 0.6f,
            keywords = listOf("lax", "stick"),
            primaryExercises = listOf("Squat", "Shoulder Press", "Rotational Core")
        ),
        UnifiedSport(
            id = "cricket",
            name = "Cricket",
            nameDE = "Cricket",
            category = SportCategory.BALL,
            trainingFocus = TrainingFocus(kraft = 0.4f, schnelligkeit = 0.7f, ausdauer = 0.5f, beweglichkeit = 0.5f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.5f,
            nutritionRelevance = 0.5f,
            keywords = listOf("batting", "bowling", "wicket"),
            primaryExercises = listOf("Rotational Power", "Shoulder Stability", "Core")
        ),

        // ═══════════════════════════════════════════════════════════════
        // RACKET SPORTS (🎾)
        // ═══════════════════════════════════════════════════════════════
        UnifiedSport(
            id = "tennis",
            name = "Tennis",
            nameDE = "Tennis",
            category = SportCategory.RACKET,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.8f, ausdauer = 0.7f, beweglichkeit = 0.7f, geschicklichkeit = 0.9f),
            vbtRelevance = 0.6f,
            nutritionRelevance = 0.6f,
            keywords = listOf("atp", "wta", "serve", "forehand", "backhand"),
            primaryExercises = listOf("Lateral Lunge", "Rotational Medicine Ball", "Shoulder Stability", "Core")
        ),
        UnifiedSport(
            id = "badminton",
            name = "Badminton",
            nameDE = "Badminton",
            category = SportCategory.RACKET,
            trainingFocus = TrainingFocus(kraft = 0.4f, schnelligkeit = 0.9f, ausdauer = 0.6f, beweglichkeit = 0.8f, geschicklichkeit = 0.9f),
            vbtRelevance = 0.5f,
            nutritionRelevance = 0.5f,
            keywords = listOf("shuttle", "smash"),
            primaryExercises = listOf("Lateral Movement", "Jump Training", "Shoulder Mobility")
        ),
        UnifiedSport(
            id = "squash",
            name = "Squash",
            nameDE = "Squash",
            category = SportCategory.RACKET,
            trainingFocus = TrainingFocus(kraft = 0.4f, schnelligkeit = 0.8f, ausdauer = 0.8f, beweglichkeit = 0.7f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.5f,
            nutritionRelevance = 0.6f,
            keywords = listOf("racquet"),
            primaryExercises = listOf("Lateral Lunge", "Rotational Core", "Conditioning")
        ),
        UnifiedSport(
            id = "padel",
            name = "Padel",
            nameDE = "Padel",
            category = SportCategory.RACKET,
            trainingFocus = TrainingFocus(kraft = 0.4f, schnelligkeit = 0.7f, ausdauer = 0.6f, beweglichkeit = 0.6f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.4f,
            nutritionRelevance = 0.5f,
            keywords = listOf("paddle", "tennis"),
            primaryExercises = listOf("Lateral Movement", "Shoulder Stability", "Core")
        ),
        UnifiedSport(
            id = "table_tennis",
            name = "Table Tennis",
            nameDE = "Tischtennis",
            category = SportCategory.RACKET,
            trainingFocus = TrainingFocus(kraft = 0.2f, schnelligkeit = 0.9f, ausdauer = 0.4f, beweglichkeit = 0.6f, geschicklichkeit = 1.0f),
            vbtRelevance = 0.3f,
            nutritionRelevance = 0.4f,
            keywords = listOf("ping pong"),
            primaryExercises = listOf("Footwork Drills", "Core Rotation", "Reaction Training")
        ),
        UnifiedSport(
            id = "racquetball",
            name = "Racquetball",
            nameDE = "Racquetball",
            category = SportCategory.RACKET,
            trainingFocus = TrainingFocus(kraft = 0.4f, schnelligkeit = 0.8f, ausdauer = 0.7f, beweglichkeit = 0.6f, geschicklichkeit = 0.7f),
            vbtRelevance = 0.4f,
            nutritionRelevance = 0.5f,
            keywords = listOf("court"),
            primaryExercises = listOf("Lateral Movement", "Rotational Power", "Conditioning")
        ),

        // ═══════════════════════════════════════════════════════════════
        // COMBAT SPORTS (🥊)
        // ═══════════════════════════════════════════════════════════════
        UnifiedSport(
            id = "boxing",
            name = "Boxing",
            nameDE = "Boxen",
            category = SportCategory.COMBAT,
            trainingFocus = TrainingFocus(kraft = 0.6f, schnelligkeit = 0.9f, ausdauer = 0.9f, beweglichkeit = 0.5f, geschicklichkeit = 0.8f, mindset = 0.8f),
            vbtRelevance = 0.6f,
            nutritionRelevance = 0.8f,
            keywords = listOf("punch", "jab", "hook", "uppercut"),
            primaryExercises = listOf("Medicine Ball Throws", "Pull-up", "Core", "Conditioning")
        ),
        UnifiedSport(
            id = "mma",
            name = "MMA",
            nameDE = "MMA",
            category = SportCategory.COMBAT,
            trainingFocus = TrainingFocus(kraft = 0.8f, schnelligkeit = 0.8f, ausdauer = 0.9f, beweglichkeit = 0.7f, geschicklichkeit = 0.8f, mindset = 0.9f),
            vbtRelevance = 0.7f,
            nutritionRelevance = 0.9f,
            keywords = listOf("mixed martial arts", "ufc", "cage", "fighting"),
            primaryExercises = listOf("Deadlift", "Pull-up", "Hip Escape Drills", "Conditioning")
        ),
        UnifiedSport(
            id = "kickboxing",
            name = "Kickboxing",
            nameDE = "Kickboxen",
            category = SportCategory.COMBAT,
            trainingFocus = TrainingFocus(kraft = 0.6f, schnelligkeit = 0.9f, ausdauer = 0.8f, beweglichkeit = 0.7f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.6f,
            nutritionRelevance = 0.7f,
            keywords = listOf("muay thai", "k1"),
            primaryExercises = listOf("Hip Flexor Strength", "Core", "Conditioning", "Leg Kicks")
        ),
        UnifiedSport(
            id = "muay_thai",
            name = "Muay Thai",
            nameDE = "Muay Thai",
            category = SportCategory.COMBAT,
            trainingFocus = TrainingFocus(kraft = 0.7f, schnelligkeit = 0.8f, ausdauer = 0.8f, beweglichkeit = 0.6f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.6f,
            nutritionRelevance = 0.7f,
            keywords = listOf("thai boxing", "clinch", "elbow", "knee"),
            primaryExercises = listOf("Hip Strength", "Core", "Neck Strength", "Conditioning")
        ),
        UnifiedSport(
            id = "judo",
            name = "Judo",
            nameDE = "Judo",
            category = SportCategory.COMBAT,
            trainingFocus = TrainingFocus(kraft = 0.8f, schnelligkeit = 0.7f, ausdauer = 0.7f, beweglichkeit = 0.6f, geschicklichkeit = 0.9f),
            vbtRelevance = 0.6f,
            nutritionRelevance = 0.7f,
            keywords = listOf("throw", "grappling", "olympic"),
            primaryExercises = listOf("Deadlift", "Pull-up", "Grip Strength", "Core")
        ),
        UnifiedSport(
            id = "wrestling",
            name = "Wrestling",
            nameDE = "Ringen",
            category = SportCategory.COMBAT,
            trainingFocus = TrainingFocus(kraft = 0.9f, schnelligkeit = 0.8f, ausdauer = 0.9f, beweglichkeit = 0.6f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.7f,
            nutritionRelevance = 0.9f,
            keywords = listOf("grappling", "takedown", "pin"),
            primaryExercises = listOf("Deadlift", "Pull-up", "Sprawl", "Conditioning")
        ),
        UnifiedSport(
            id = "bjj",
            name = "Brazilian Jiu-Jitsu",
            nameDE = "Brasilianisches Jiu-Jitsu",
            category = SportCategory.COMBAT,
            trainingFocus = TrainingFocus(kraft = 0.6f, schnelligkeit = 0.5f, ausdauer = 0.8f, beweglichkeit = 0.9f, geschicklichkeit = 1.0f),
            vbtRelevance = 0.4f,
            nutritionRelevance = 0.7f,
            keywords = listOf("grappling", "submission", "guard", "mount"),
            primaryExercises = listOf("Hip Mobility", "Grip Strength", "Core", "Pull-up")
        ),
        UnifiedSport(
            id = "taekwondo",
            name = "Taekwondo",
            nameDE = "Taekwondo",
            category = SportCategory.COMBAT,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.9f, ausdauer = 0.6f, beweglichkeit = 0.9f, geschicklichkeit = 0.9f),
            vbtRelevance = 0.5f,
            nutritionRelevance = 0.6f,
            keywords = listOf("kick", "olympic", "martial arts"),
            primaryExercises = listOf("Hip Flexor", "Flexibility", "Jump Training", "Core")
        ),
        UnifiedSport(
            id = "karate",
            name = "Karate",
            nameDE = "Karate",
            category = SportCategory.COMBAT,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.8f, ausdauer = 0.5f, beweglichkeit = 0.7f, geschicklichkeit = 0.9f, mindset = 0.7f),
            vbtRelevance = 0.5f,
            nutritionRelevance = 0.5f,
            keywords = listOf("kata", "kumite", "martial arts"),
            primaryExercises = listOf("Hip Rotation", "Core", "Stance Training")
        ),
        UnifiedSport(
            id = "fencing",
            name = "Fencing",
            nameDE = "Fechten",
            category = SportCategory.COMBAT,
            trainingFocus = TrainingFocus(kraft = 0.4f, schnelligkeit = 0.9f, ausdauer = 0.5f, beweglichkeit = 0.6f, geschicklichkeit = 1.0f, mindset = 0.8f),
            vbtRelevance = 0.4f,
            nutritionRelevance = 0.5f,
            keywords = listOf("epee", "foil", "sabre", "olympic"),
            primaryExercises = listOf("Lunge", "Footwork", "Shoulder Stability", "Reaction")
        ),

        // ═══════════════════════════════════════════════════════════════
        // ENDURANCE SPORTS (🏃)
        // ═══════════════════════════════════════════════════════════════
        UnifiedSport(
            id = "running_marathon",
            name = "Marathon Running",
            nameDE = "Marathonlauf",
            category = SportCategory.ENDURANCE,
            trainingFocus = TrainingFocus(kraft = 0.3f, schnelligkeit = 0.4f, ausdauer = 1.0f, beweglichkeit = 0.4f, mindset = 0.8f),
            vbtRelevance = 0.2f,
            nutritionRelevance = 0.9f,
            keywords = listOf("42k", "long distance", "running"),
            primaryExercises = listOf("Single Leg Squat", "Hip Stability", "Core", "Calf Raises")
        ),
        UnifiedSport(
            id = "running_half",
            name = "Half Marathon",
            nameDE = "Halbmarathon",
            category = SportCategory.ENDURANCE,
            trainingFocus = TrainingFocus(kraft = 0.3f, schnelligkeit = 0.5f, ausdauer = 0.9f, beweglichkeit = 0.4f),
            vbtRelevance = 0.2f,
            nutritionRelevance = 0.8f,
            keywords = listOf("21k", "running"),
            primaryExercises = listOf("Single Leg Work", "Hip Stability", "Core")
        ),
        UnifiedSport(
            id = "running_5k10k",
            name = "5K/10K Running",
            nameDE = "5K/10K Lauf",
            category = SportCategory.ENDURANCE,
            trainingFocus = TrainingFocus(kraft = 0.4f, schnelligkeit = 0.6f, ausdauer = 0.8f, beweglichkeit = 0.4f),
            vbtRelevance = 0.3f,
            nutritionRelevance = 0.7f,
            keywords = listOf("running", "road race"),
            primaryExercises = listOf("Single Leg Squat", "Hip Stability", "Plyometrics")
        ),
        UnifiedSport(
            id = "trail_running",
            name = "Trail Running",
            nameDE = "Trailrunning",
            category = SportCategory.ENDURANCE,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.5f, ausdauer = 0.9f, beweglichkeit = 0.5f, geschicklichkeit = 0.6f),
            vbtRelevance = 0.3f,
            nutritionRelevance = 0.8f,
            keywords = listOf("ultra", "mountain", "trail"),
            primaryExercises = listOf("Single Leg Stability", "Hip Strength", "Ankle Stability")
        ),
        UnifiedSport(
            id = "triathlon",
            name = "Triathlon",
            nameDE = "Triathlon",
            category = SportCategory.ENDURANCE,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.5f, ausdauer = 1.0f, beweglichkeit = 0.5f, mindset = 0.7f),
            vbtRelevance = 0.3f,
            nutritionRelevance = 0.9f,
            keywords = listOf("ironman", "swim bike run", "multisport"),
            primaryExercises = listOf("Core", "Hip Stability", "Shoulder Mobility", "Single Leg Work")
        ),
        UnifiedSport(
            id = "cycling_road",
            name = "Road Cycling",
            nameDE = "Rennradfahren",
            category = SportCategory.ENDURANCE,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.5f, ausdauer = 1.0f, beweglichkeit = 0.4f),
            vbtRelevance = 0.3f,
            nutritionRelevance = 0.8f,
            keywords = listOf("bike", "tour", "racing"),
            primaryExercises = listOf("Single Leg Press", "Core", "Hip Flexor")
        ),
        UnifiedSport(
            id = "cycling_mtb",
            name = "Mountain Biking",
            nameDE = "Mountainbiking",
            category = SportCategory.OUTDOOR,
            trainingFocus = TrainingFocus(kraft = 0.6f, schnelligkeit = 0.5f, ausdauer = 0.8f, beweglichkeit = 0.5f, geschicklichkeit = 0.7f),
            vbtRelevance = 0.4f,
            nutritionRelevance = 0.7f,
            keywords = listOf("mtb", "downhill", "cross country"),
            primaryExercises = listOf("Single Leg Squat", "Core", "Grip Strength")
        ),

        // ═══════════════════════════════════════════════════════════════
        // WATER SPORTS (🏊)
        // ═══════════════════════════════════════════════════════════════
        UnifiedSport(
            id = "swimming",
            name = "Swimming",
            nameDE = "Schwimmen",
            category = SportCategory.WATER,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.6f, ausdauer = 0.9f, beweglichkeit = 0.7f, geschicklichkeit = 0.7f),
            vbtRelevance = 0.3f,
            nutritionRelevance = 0.7f,
            keywords = listOf("freestyle", "backstroke", "breaststroke", "butterfly", "pool"),
            primaryExercises = listOf("Lat Pulldown", "Shoulder Stability", "Core", "Hip Mobility")
        ),
        UnifiedSport(
            id = "rowing",
            name = "Rowing",
            nameDE = "Rudern",
            category = SportCategory.WATER,
            trainingFocus = TrainingFocus(kraft = 0.8f, schnelligkeit = 0.5f, ausdauer = 0.9f, beweglichkeit = 0.5f),
            vbtRelevance = 0.6f,
            nutritionRelevance = 0.8f,
            keywords = listOf("crew", "sculling", "ergometer", "erg"),
            primaryExercises = listOf("Deadlift", "Row", "Leg Press", "Core")
        ),
        UnifiedSport(
            id = "kayak_canoe",
            name = "Kayaking/Canoeing",
            nameDE = "Kanu/Kajak",
            category = SportCategory.WATER,
            trainingFocus = TrainingFocus(kraft = 0.7f, schnelligkeit = 0.5f, ausdauer = 0.8f, beweglichkeit = 0.5f, geschicklichkeit = 0.6f),
            vbtRelevance = 0.5f,
            nutritionRelevance = 0.7f,
            keywords = listOf("paddle", "sprint", "slalom"),
            primaryExercises = listOf("Rotational Core", "Lat Pulldown", "Shoulder Stability")
        ),
        UnifiedSport(
            id = "surfing",
            name = "Surfing",
            nameDE = "Surfen",
            category = SportCategory.WATER,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.6f, ausdauer = 0.6f, beweglichkeit = 0.7f, geschicklichkeit = 0.9f),
            vbtRelevance = 0.4f,
            nutritionRelevance = 0.5f,
            keywords = listOf("wave", "board", "ocean"),
            primaryExercises = listOf("Pop-up", "Shoulder Stability", "Core", "Hip Mobility")
        ),
        UnifiedSport(
            id = "water_polo",
            name = "Water Polo",
            nameDE = "Wasserball",
            category = SportCategory.WATER,
            trainingFocus = TrainingFocus(kraft = 0.7f, schnelligkeit = 0.7f, ausdauer = 0.9f, beweglichkeit = 0.5f, geschicklichkeit = 0.7f),
            vbtRelevance = 0.5f,
            nutritionRelevance = 0.7f,
            keywords = listOf("pool", "team"),
            primaryExercises = listOf("Shoulder Press", "Core", "Leg Strength", "Treading")
        ),
        UnifiedSport(
            id = "diving",
            name = "Diving",
            nameDE = "Turmspringen",
            category = SportCategory.WATER,
            trainingFocus = TrainingFocus(kraft = 0.6f, schnelligkeit = 0.6f, ausdauer = 0.3f, beweglichkeit = 0.9f, geschicklichkeit = 1.0f, mindset = 0.8f),
            vbtRelevance = 0.5f,
            nutritionRelevance = 0.5f,
            keywords = listOf("springboard", "platform", "olympic"),
            primaryExercises = listOf("Jump Training", "Core", "Flexibility", "Acrobatics")
        ),

        // ═══════════════════════════════════════════════════════════════
        // WINTER SPORTS (⛷️)
        // ═══════════════════════════════════════════════════════════════
        UnifiedSport(
            id = "skiing_alpine",
            name = "Alpine Skiing",
            nameDE = "Ski Alpin",
            category = SportCategory.WINTER,
            trainingFocus = TrainingFocus(kraft = 0.7f, schnelligkeit = 0.6f, ausdauer = 0.6f, beweglichkeit = 0.6f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.6f,
            nutritionRelevance = 0.6f,
            keywords = listOf("downhill", "slalom", "giant slalom", "super g"),
            primaryExercises = listOf("Squat", "Single Leg Work", "Core", "Lateral Movement")
        ),
        UnifiedSport(
            id = "skiing_cross_country",
            name = "Cross-Country Skiing",
            nameDE = "Skilanglauf",
            category = SportCategory.WINTER,
            trainingFocus = TrainingFocus(kraft = 0.6f, schnelligkeit = 0.5f, ausdauer = 1.0f, beweglichkeit = 0.5f),
            vbtRelevance = 0.4f,
            nutritionRelevance = 0.9f,
            keywords = listOf("nordic", "xc", "classic", "skate"),
            primaryExercises = listOf("Single Leg Squat", "Core", "Upper Body Pull", "Conditioning")
        ),
        UnifiedSport(
            id = "snowboarding",
            name = "Snowboarding",
            nameDE = "Snowboarden",
            category = SportCategory.WINTER,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.5f, ausdauer = 0.5f, beweglichkeit = 0.7f, geschicklichkeit = 0.9f),
            vbtRelevance = 0.4f,
            nutritionRelevance = 0.5f,
            keywords = listOf("freestyle", "halfpipe", "slopestyle"),
            primaryExercises = listOf("Single Leg Squat", "Core Rotation", "Jump Training")
        ),
        UnifiedSport(
            id = "ice_hockey",
            name = "Ice Hockey",
            nameDE = "Eishockey",
            category = SportCategory.WINTER,
            trainingFocus = TrainingFocus(kraft = 0.8f, schnelligkeit = 0.9f, ausdauer = 0.7f, beweglichkeit = 0.5f, geschicklichkeit = 0.7f),
            vbtRelevance = 0.7f,
            nutritionRelevance = 0.7f,
            keywords = listOf("nhl", "puck", "skating"),
            primaryExercises = listOf("Squat", "Hip Adductor", "Core Rotation", "Conditioning")
        ),
        UnifiedSport(
            id = "figure_skating",
            name = "Figure Skating",
            nameDE = "Eiskunstlauf",
            category = SportCategory.WINTER,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.6f, ausdauer = 0.5f, beweglichkeit = 0.9f, geschicklichkeit = 1.0f),
            vbtRelevance = 0.4f,
            nutritionRelevance = 0.6f,
            keywords = listOf("skating", "jumps", "spins"),
            primaryExercises = listOf("Jump Training", "Single Leg Strength", "Core", "Flexibility")
        ),
        UnifiedSport(
            id = "speed_skating",
            name = "Speed Skating",
            nameDE = "Eisschnelllauf",
            category = SportCategory.WINTER,
            trainingFocus = TrainingFocus(kraft = 0.8f, schnelligkeit = 0.9f, ausdauer = 0.7f, beweglichkeit = 0.5f),
            vbtRelevance = 0.7f,
            nutritionRelevance = 0.7f,
            keywords = listOf("short track", "long track"),
            primaryExercises = listOf("Squat", "Single Leg Press", "Hip Adductor", "Core")
        ),
        UnifiedSport(
            id = "biathlon",
            name = "Biathlon",
            nameDE = "Biathlon",
            category = SportCategory.WINTER,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.5f, ausdauer = 0.9f, beweglichkeit = 0.4f, geschicklichkeit = 0.8f, mindset = 0.8f),
            vbtRelevance = 0.3f,
            nutritionRelevance = 0.8f,
            keywords = listOf("skiing", "shooting", "nordic"),
            primaryExercises = listOf("Core Stability", "Single Leg Work", "Shoulder Stability")
        ),
        UnifiedSport(
            id = "bobsled",
            name = "Bobsled",
            nameDE = "Bob",
            category = SportCategory.WINTER,
            trainingFocus = TrainingFocus(kraft = 0.9f, schnelligkeit = 1.0f, ausdauer = 0.3f),
            vbtRelevance = 0.9f,
            nutritionRelevance = 0.6f,
            keywords = listOf("bobsleigh", "push start"),
            primaryExercises = listOf("Squat", "Deadlift", "Sprint", "Power Clean")
        ),
        UnifiedSport(
            id = "luge_skeleton",
            name = "Luge/Skeleton",
            nameDE = "Rodeln/Skeleton",
            category = SportCategory.WINTER,
            trainingFocus = TrainingFocus(kraft = 0.6f, schnelligkeit = 0.8f, ausdauer = 0.3f, geschicklichkeit = 0.7f, mindset = 0.8f),
            vbtRelevance = 0.6f,
            nutritionRelevance = 0.5f,
            keywords = listOf("sled", "start"),
            primaryExercises = listOf("Core", "Neck Strength", "Sprint Start")
        ),
        UnifiedSport(
            id = "curling",
            name = "Curling",
            nameDE = "Curling",
            category = SportCategory.WINTER,
            trainingFocus = TrainingFocus(kraft = 0.3f, schnelligkeit = 0.2f, ausdauer = 0.4f, beweglichkeit = 0.5f, geschicklichkeit = 0.8f, mindset = 0.7f),
            vbtRelevance = 0.2f,
            nutritionRelevance = 0.4f,
            keywords = listOf("stone", "sweeping"),
            primaryExercises = listOf("Lunge", "Core Stability", "Shoulder Endurance")
        ),

        // ═══════════════════════════════════════════════════════════════
        // ATHLETICS / TRACK & FIELD (🏅)
        // ═══════════════════════════════════════════════════════════════
        UnifiedSport(
            id = "sprinting",
            name = "Sprinting",
            nameDE = "Sprint",
            category = SportCategory.ATHLETICS,
            trainingFocus = TrainingFocus(kraft = 0.8f, schnelligkeit = 1.0f, ausdauer = 0.3f, beweglichkeit = 0.6f),
            vbtRelevance = 0.9f,
            nutritionRelevance = 0.6f,
            keywords = listOf("100m", "200m", "400m", "track"),
            primaryExercises = listOf("Squat", "Deadlift", "Power Clean", "Hip Thrust", "Plyometrics")
        ),
        UnifiedSport(
            id = "hurdles",
            name = "Hurdles",
            nameDE = "Hürdenlauf",
            category = SportCategory.ATHLETICS,
            trainingFocus = TrainingFocus(kraft = 0.7f, schnelligkeit = 0.9f, ausdauer = 0.4f, beweglichkeit = 0.8f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.8f,
            nutritionRelevance = 0.6f,
            keywords = listOf("110m", "400m", "track"),
            primaryExercises = listOf("Hip Mobility", "Single Leg Power", "Plyometrics")
        ),
        UnifiedSport(
            id = "long_jump",
            name = "Long Jump",
            nameDE = "Weitsprung",
            category = SportCategory.ATHLETICS,
            trainingFocus = TrainingFocus(kraft = 0.8f, schnelligkeit = 1.0f, ausdauer = 0.2f, beweglichkeit = 0.5f, geschicklichkeit = 0.7f),
            vbtRelevance = 0.9f,
            nutritionRelevance = 0.5f,
            keywords = listOf("jump", "field", "triple jump"),
            primaryExercises = listOf("Squat", "Plyometrics", "Single Leg Power", "Sprint")
        ),
        UnifiedSport(
            id = "high_jump",
            name = "High Jump",
            nameDE = "Hochsprung",
            category = SportCategory.ATHLETICS,
            trainingFocus = TrainingFocus(kraft = 0.7f, schnelligkeit = 0.8f, ausdauer = 0.2f, beweglichkeit = 0.7f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.8f,
            nutritionRelevance = 0.5f,
            keywords = listOf("jump", "field", "fosbury"),
            primaryExercises = listOf("Single Leg Squat", "Plyometrics", "Core", "Approach Run")
        ),
        UnifiedSport(
            id = "pole_vault",
            name = "Pole Vault",
            nameDE = "Stabhochsprung",
            category = SportCategory.ATHLETICS,
            trainingFocus = TrainingFocus(kraft = 0.8f, schnelligkeit = 0.9f, ausdauer = 0.2f, beweglichkeit = 0.7f, geschicklichkeit = 0.9f),
            vbtRelevance = 0.8f,
            nutritionRelevance = 0.5f,
            keywords = listOf("vault", "field"),
            primaryExercises = listOf("Pull-up", "Core", "Sprint", "Gymnastic Strength")
        ),
        UnifiedSport(
            id = "shot_put",
            name = "Shot Put",
            nameDE = "Kugelstoßen",
            category = SportCategory.ATHLETICS,
            trainingFocus = TrainingFocus(kraft = 1.0f, schnelligkeit = 0.8f, ausdauer = 0.2f, geschicklichkeit = 0.7f),
            vbtRelevance = 0.9f,
            nutritionRelevance = 0.7f,
            keywords = listOf("throw", "field", "glide", "spin"),
            primaryExercises = listOf("Bench Press", "Squat", "Medicine Ball Throws", "Rotational Power")
        ),
        UnifiedSport(
            id = "discus",
            name = "Discus",
            nameDE = "Diskuswerfen",
            category = SportCategory.ATHLETICS,
            trainingFocus = TrainingFocus(kraft = 0.9f, schnelligkeit = 0.8f, ausdauer = 0.2f, beweglichkeit = 0.5f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.8f,
            nutritionRelevance = 0.6f,
            keywords = listOf("throw", "field", "spin"),
            primaryExercises = listOf("Rotational Core", "Squat", "Medicine Ball Throws")
        ),
        UnifiedSport(
            id = "javelin",
            name = "Javelin",
            nameDE = "Speerwerfen",
            category = SportCategory.ATHLETICS,
            trainingFocus = TrainingFocus(kraft = 0.8f, schnelligkeit = 0.9f, ausdauer = 0.2f, beweglichkeit = 0.6f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.8f,
            nutritionRelevance = 0.5f,
            keywords = listOf("throw", "field"),
            primaryExercises = listOf("Shoulder Stability", "Core", "Medicine Ball Throws", "Sprint")
        ),
        UnifiedSport(
            id = "hammer_throw",
            name = "Hammer Throw",
            nameDE = "Hammerwurf",
            category = SportCategory.ATHLETICS,
            trainingFocus = TrainingFocus(kraft = 1.0f, schnelligkeit = 0.7f, ausdauer = 0.2f, beweglichkeit = 0.4f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.7f,
            nutritionRelevance = 0.6f,
            keywords = listOf("throw", "field", "spin"),
            primaryExercises = listOf("Squat", "Core Rotation", "Grip Strength")
        ),
        UnifiedSport(
            id = "decathlon_heptathlon",
            name = "Decathlon/Heptathlon",
            nameDE = "Zehnkampf/Siebenkampf",
            category = SportCategory.ATHLETICS,
            trainingFocus = TrainingFocus(kraft = 0.8f, schnelligkeit = 0.8f, ausdauer = 0.7f, beweglichkeit = 0.6f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.7f,
            nutritionRelevance = 0.7f,
            keywords = listOf("combined", "multi-event"),
            primaryExercises = listOf("Squat", "Power Clean", "Plyometrics", "All-around")
        ),

        // ═══════════════════════════════════════════════════════════════
        // GYMNASTICS (🤸)
        // ═══════════════════════════════════════════════════════════════
        UnifiedSport(
            id = "artistic_gymnastics",
            name = "Artistic Gymnastics",
            nameDE = "Geräteturnen",
            category = SportCategory.GYMNASTICS,
            trainingFocus = TrainingFocus(kraft = 0.9f, schnelligkeit = 0.7f, ausdauer = 0.5f, beweglichkeit = 1.0f, geschicklichkeit = 1.0f),
            vbtRelevance = 0.5f,
            nutritionRelevance = 0.6f,
            keywords = listOf("vault", "bars", "beam", "floor", "rings", "pommel"),
            primaryExercises = listOf("Handstand", "Pull-up", "Core", "Flexibility", "Plyometrics")
        ),
        UnifiedSport(
            id = "rhythmic_gymnastics",
            name = "Rhythmic Gymnastics",
            nameDE = "Rhythmische Sportgymnastik",
            category = SportCategory.GYMNASTICS,
            trainingFocus = TrainingFocus(kraft = 0.4f, schnelligkeit = 0.5f, ausdauer = 0.5f, beweglichkeit = 1.0f, geschicklichkeit = 1.0f),
            vbtRelevance = 0.2f,
            nutritionRelevance = 0.6f,
            keywords = listOf("ribbon", "ball", "hoop", "clubs"),
            primaryExercises = listOf("Flexibility", "Balance", "Core", "Dance")
        ),
        UnifiedSport(
            id = "trampoline",
            name = "Trampoline",
            nameDE = "Trampolinturnen",
            category = SportCategory.GYMNASTICS,
            trainingFocus = TrainingFocus(kraft = 0.6f, schnelligkeit = 0.7f, ausdauer = 0.4f, beweglichkeit = 0.8f, geschicklichkeit = 1.0f),
            vbtRelevance = 0.5f,
            nutritionRelevance = 0.5f,
            keywords = listOf("jump", "acrobatics"),
            primaryExercises = listOf("Core", "Jump Training", "Body Control")
        ),
        UnifiedSport(
            id = "acrobatics",
            name = "Acrobatics",
            nameDE = "Akrobatik",
            category = SportCategory.GYMNASTICS,
            trainingFocus = TrainingFocus(kraft = 0.7f, schnelligkeit = 0.6f, ausdauer = 0.5f, beweglichkeit = 0.9f, geschicklichkeit = 1.0f),
            vbtRelevance = 0.4f,
            nutritionRelevance = 0.5f,
            keywords = listOf("partner", "tumbling", "circus"),
            primaryExercises = listOf("Core", "Flexibility", "Balance", "Handstand")
        ),
        UnifiedSport(
            id = "cheerleading",
            name = "Cheerleading",
            nameDE = "Cheerleading",
            category = SportCategory.GYMNASTICS,
            trainingFocus = TrainingFocus(kraft = 0.7f, schnelligkeit = 0.6f, ausdauer = 0.6f, beweglichkeit = 0.8f, geschicklichkeit = 0.9f),
            vbtRelevance = 0.4f,
            nutritionRelevance = 0.5f,
            keywords = listOf("stunts", "tumbling", "dance"),
            primaryExercises = listOf("Squat", "Core", "Flexibility", "Jump Training")
        ),
        UnifiedSport(
            id = "parkour",
            name = "Parkour",
            nameDE = "Parkour",
            category = SportCategory.GYMNASTICS,
            trainingFocus = TrainingFocus(kraft = 0.7f, schnelligkeit = 0.8f, ausdauer = 0.6f, beweglichkeit = 0.7f, geschicklichkeit = 0.9f),
            vbtRelevance = 0.5f,
            nutritionRelevance = 0.5f,
            keywords = listOf("freerunning", "movement", "urban"),
            primaryExercises = listOf("Pull-up", "Jump Training", "Core", "Conditioning")
        ),

        // ═══════════════════════════════════════════════════════════════
        // OUTDOOR / ADVENTURE (🧗)
        // ═══════════════════════════════════════════════════════════════
        UnifiedSport(
            id = "rock_climbing",
            name = "Rock Climbing",
            nameDE = "Klettern",
            category = SportCategory.OUTDOOR,
            trainingFocus = TrainingFocus(kraft = 0.8f, schnelligkeit = 0.3f, ausdauer = 0.6f, beweglichkeit = 0.7f, geschicklichkeit = 0.8f, mindset = 0.7f),
            vbtRelevance = 0.3f,
            nutritionRelevance = 0.5f,
            keywords = listOf("bouldering", "sport climbing", "trad"),
            primaryExercises = listOf("Pull-up", "Grip Strength", "Core", "Finger Training")
        ),
        UnifiedSport(
            id = "bouldering",
            name = "Bouldering",
            nameDE = "Bouldern",
            category = SportCategory.OUTDOOR,
            trainingFocus = TrainingFocus(kraft = 0.9f, schnelligkeit = 0.5f, ausdauer = 0.4f, beweglichkeit = 0.7f, geschicklichkeit = 0.9f),
            vbtRelevance = 0.4f,
            nutritionRelevance = 0.5f,
            keywords = listOf("climbing", "problems"),
            primaryExercises = listOf("Pull-up", "Core", "Grip Strength", "Campus Board")
        ),
        UnifiedSport(
            id = "hiking",
            name = "Hiking",
            nameDE = "Wandern",
            category = SportCategory.OUTDOOR,
            trainingFocus = TrainingFocus(kraft = 0.4f, schnelligkeit = 0.2f, ausdauer = 0.8f, beweglichkeit = 0.4f),
            vbtRelevance = 0.2f,
            nutritionRelevance = 0.6f,
            keywords = listOf("trekking", "backpacking", "mountain"),
            primaryExercises = listOf("Single Leg Squat", "Step-up", "Core", "Conditioning")
        ),
        UnifiedSport(
            id = "mountaineering",
            name = "Mountaineering",
            nameDE = "Bergsteigen",
            category = SportCategory.OUTDOOR,
            trainingFocus = TrainingFocus(kraft = 0.6f, schnelligkeit = 0.3f, ausdauer = 0.9f, beweglichkeit = 0.5f, geschicklichkeit = 0.6f, mindset = 0.8f),
            vbtRelevance = 0.3f,
            nutritionRelevance = 0.8f,
            keywords = listOf("alpine", "climbing", "high altitude"),
            primaryExercises = listOf("Step-up", "Core", "Grip Strength", "Conditioning")
        ),
        UnifiedSport(
            id = "obstacle_course",
            name = "Obstacle Course Racing",
            nameDE = "Hindernislauf",
            category = SportCategory.OUTDOOR,
            trainingFocus = TrainingFocus(kraft = 0.7f, schnelligkeit = 0.6f, ausdauer = 0.9f, beweglichkeit = 0.5f, geschicklichkeit = 0.7f),
            vbtRelevance = 0.5f,
            nutritionRelevance = 0.7f,
            keywords = listOf("spartan", "tough mudder", "ocr"),
            primaryExercises = listOf("Pull-up", "Grip Strength", "Running", "Core")
        ),

        // ═══════════════════════════════════════════════════════════════
        // PRECISION SPORTS (🎯)
        // ═══════════════════════════════════════════════════════════════
        UnifiedSport(
            id = "golf",
            name = "Golf",
            nameDE = "Golf",
            category = SportCategory.PRECISION,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.6f, ausdauer = 0.4f, beweglichkeit = 0.7f, geschicklichkeit = 0.9f, mindset = 0.8f),
            vbtRelevance = 0.5f,
            nutritionRelevance = 0.4f,
            keywords = listOf("swing", "drive", "putt", "pga"),
            primaryExercises = listOf("Hip Rotation", "Core", "Shoulder Mobility", "Single Leg Stability")
        ),
        UnifiedSport(
            id = "archery",
            name = "Archery",
            nameDE = "Bogenschießen",
            category = SportCategory.PRECISION,
            trainingFocus = TrainingFocus(kraft = 0.4f, schnelligkeit = 0.1f, ausdauer = 0.4f, beweglichkeit = 0.4f, geschicklichkeit = 0.8f, mindset = 0.9f),
            vbtRelevance = 0.2f,
            nutritionRelevance = 0.4f,
            keywords = listOf("bow", "target", "olympic"),
            primaryExercises = listOf("Back Strength", "Shoulder Stability", "Core", "Grip Endurance")
        ),
        UnifiedSport(
            id = "shooting",
            name = "Shooting Sports",
            nameDE = "Schießsport",
            category = SportCategory.PRECISION,
            trainingFocus = TrainingFocus(kraft = 0.2f, schnelligkeit = 0.2f, ausdauer = 0.3f, beweglichkeit = 0.3f, geschicklichkeit = 0.7f, mindset = 1.0f),
            vbtRelevance = 0.1f,
            nutritionRelevance = 0.3f,
            keywords = listOf("rifle", "pistol", "target"),
            primaryExercises = listOf("Core Stability", "Breathing", "Grip Endurance")
        ),
        UnifiedSport(
            id = "darts",
            name = "Darts",
            nameDE = "Darts",
            category = SportCategory.PRECISION,
            trainingFocus = TrainingFocus(kraft = 0.1f, schnelligkeit = 0.1f, ausdauer = 0.2f, beweglichkeit = 0.2f, geschicklichkeit = 0.9f, mindset = 0.9f),
            vbtRelevance = 0.1f,
            nutritionRelevance = 0.3f,
            keywords = listOf("180", "checkout"),
            primaryExercises = listOf("Shoulder Stability", "Core", "Focus Training")
        ),
        UnifiedSport(
            id = "bowling",
            name = "Bowling",
            nameDE = "Bowling",
            category = SportCategory.PRECISION,
            trainingFocus = TrainingFocus(kraft = 0.3f, schnelligkeit = 0.2f, ausdauer = 0.3f, beweglichkeit = 0.4f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.2f,
            nutritionRelevance = 0.3f,
            keywords = listOf("strike", "spare", "pins"),
            primaryExercises = listOf("Wrist Strength", "Core", "Single Leg Balance")
        ),
        UnifiedSport(
            id = "billiards",
            name = "Billiards/Pool",
            nameDE = "Billard",
            category = SportCategory.PRECISION,
            trainingFocus = TrainingFocus(kraft = 0.1f, schnelligkeit = 0.1f, ausdauer = 0.2f, beweglichkeit = 0.3f, geschicklichkeit = 0.9f, mindset = 0.8f),
            vbtRelevance = 0.1f,
            nutritionRelevance = 0.2f,
            keywords = listOf("snooker", "pool", "cue"),
            primaryExercises = listOf("Core Stability", "Stance", "Focus")
        ),

        // ═══════════════════════════════════════════════════════════════
        // OTHER SPORTS
        // ═══════════════════════════════════════════════════════════════
        UnifiedSport(
            id = "equestrian",
            name = "Equestrian",
            nameDE = "Reitsport",
            category = SportCategory.OTHER,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.3f, ausdauer = 0.5f, beweglichkeit = 0.6f, geschicklichkeit = 0.8f),
            vbtRelevance = 0.3f,
            nutritionRelevance = 0.4f,
            keywords = listOf("horse", "dressage", "jumping", "eventing"),
            primaryExercises = listOf("Core", "Hip Mobility", "Inner Thigh", "Balance")
        ),
        UnifiedSport(
            id = "skateboarding",
            name = "Skateboarding",
            nameDE = "Skateboarden",
            category = SportCategory.OTHER,
            trainingFocus = TrainingFocus(kraft = 0.4f, schnelligkeit = 0.5f, ausdauer = 0.4f, beweglichkeit = 0.6f, geschicklichkeit = 1.0f),
            vbtRelevance = 0.3f,
            nutritionRelevance = 0.4f,
            keywords = listOf("skate", "street", "park", "vert"),
            primaryExercises = listOf("Single Leg Balance", "Core", "Ankle Stability")
        ),
        UnifiedSport(
            id = "dance",
            name = "Dance",
            nameDE = "Tanzen",
            category = SportCategory.OTHER,
            trainingFocus = TrainingFocus(kraft = 0.4f, schnelligkeit = 0.5f, ausdauer = 0.7f, beweglichkeit = 0.9f, geschicklichkeit = 0.9f),
            vbtRelevance = 0.2f,
            nutritionRelevance = 0.6f,
            keywords = listOf("ballet", "contemporary", "hip hop", "ballroom"),
            primaryExercises = listOf("Core", "Flexibility", "Single Leg Strength", "Jump Training")
        ),
        UnifiedSport(
            id = "yoga",
            name = "Yoga",
            nameDE = "Yoga",
            category = SportCategory.OTHER,
            trainingFocus = TrainingFocus(kraft = 0.3f, schnelligkeit = 0.1f, ausdauer = 0.4f, beweglichkeit = 1.0f, geschicklichkeit = 0.5f, mindset = 0.9f),
            vbtRelevance = 0.1f,
            nutritionRelevance = 0.5f,
            keywords = listOf("flexibility", "mindfulness", "asana"),
            primaryExercises = listOf("Flexibility", "Core", "Balance", "Breathing")
        ),
        UnifiedSport(
            id = "pilates",
            name = "Pilates",
            nameDE = "Pilates",
            category = SportCategory.OTHER,
            trainingFocus = TrainingFocus(kraft = 0.4f, schnelligkeit = 0.1f, ausdauer = 0.4f, beweglichkeit = 0.8f, geschicklichkeit = 0.5f),
            vbtRelevance = 0.1f,
            nutritionRelevance = 0.5f,
            keywords = listOf("core", "reformer", "mat"),
            primaryExercises = listOf("Core", "Flexibility", "Control", "Breathing")
        ),
        UnifiedSport(
            id = "motor_racing",
            name = "Motor Racing",
            nameDE = "Motorsport",
            category = SportCategory.OTHER,
            trainingFocus = TrainingFocus(kraft = 0.5f, schnelligkeit = 0.6f, ausdauer = 0.6f, beweglichkeit = 0.4f, geschicklichkeit = 0.7f, mindset = 0.9f),
            vbtRelevance = 0.4f,
            nutritionRelevance = 0.6f,
            keywords = listOf("f1", "nascar", "motogp", "racing"),
            primaryExercises = listOf("Neck Strength", "Core", "Reaction Training", "Conditioning")
        ),
        UnifiedSport(
            id = "esports",
            name = "Esports",
            nameDE = "E-Sport",
            category = SportCategory.OTHER,
            trainingFocus = TrainingFocus(kraft = 0.1f, schnelligkeit = 0.5f, ausdauer = 0.3f, beweglichkeit = 0.2f, geschicklichkeit = 0.8f, mindset = 0.9f),
            vbtRelevance = 0.1f,
            nutritionRelevance = 0.5f,
            keywords = listOf("gaming", "competitive", "league", "fps"),
            primaryExercises = listOf("Wrist Health", "Posture", "Eye Health", "General Fitness")
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // SEARCH & FILTER FUNCTIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * PERFECT PREFIX SEARCH
     *
     * The user should feel like the app knows EXACTLY their sport.
     *
     * Rules:
     * 1. Name prefix match gets HIGHEST priority (bod → Bodybuilding)
     * 2. Shorter names win when both match (ten → Tennis, NOT Table Tennis)
     * 3. Keywords only match if NO name matches exist
     * 4. Results are limited to feel precise, not overwhelming
     */
    fun search(query: String): List<UnifiedSport> {
        if (query.isBlank()) return emptyList()

        val normalizedQuery = query.trim().lowercase()

        // First: Find all sports with name prefix matches
        val nameMatches = allSports
            .map { sport -> sport to calculateNameScore(sport, normalizedQuery) }
            .filter { (_, score) -> score > 0 }
            .sortedWith(compareByDescending<Pair<UnifiedSport, Int>> { it.second }
                .thenBy { it.first.name.length }) // Shorter names first for same score
            .map { it.first }

        // If we have name matches, return only those (no keyword pollution)
        if (nameMatches.isNotEmpty()) {
            return nameMatches.take(5) // Max 5 results for focused feel
        }

        // No name matches - try keyword matches as fallback
        val keywordMatches = allSports
            .filter { sport ->
                sport.keywords.any { it.lowercase().startsWith(normalizedQuery) }
            }
            .sortedBy { it.name.length }
            .take(3) // Even fewer for keyword matches

        return keywordMatches
    }

    /**
     * Score based ONLY on name matching - no keywords here
     *
     * Scoring:
     * - 1000: Exact match
     * - 900: Name starts with query
     * - 800: German name starts with query
     * - 700: First word of multi-word name starts with query
     * - 600: Second/third word starts with query
     *
     * BONUS: Subtract name length to prefer shorter matches
     * "ten" → Tennis (6 chars) beats Table Tennis (12 chars)
     */
    private fun calculateNameScore(sport: UnifiedSport, query: String): Int {
        val nameLower = sport.name.lowercase()
        val nameDELower = sport.nameDE.lowercase()

        // Exact match - perfect
        if (nameLower == query || nameDELower == query) {
            return 1000
        }

        // Primary name starts with query
        if (nameLower.startsWith(query)) {
            return 900
        }

        // German name starts with query
        if (nameDELower.startsWith(query)) {
            return 850
        }

        // For multi-word names, check each word
        val nameWords = nameLower.split(" ", "-", "/")
        val nameDEWords = nameDELower.split(" ", "-", "/")

        // First word match (after splitting) - still high priority
        // "Olympic Weightlifting" - "wei" matches "Weightlifting"
        if (nameWords.size > 1 && nameWords.drop(1).any { it.startsWith(query) }) {
            return 700
        }

        if (nameDEWords.size > 1 && nameDEWords.drop(1).any { it.startsWith(query) }) {
            return 650
        }

        // No match
        return 0
    }

    /**
     * Get sports by category
     */
    fun getByCategory(category: SportCategory): List<UnifiedSport> {
        return allSports.filter { it.category == category }
    }

    /**
     * Get sport by ID
     */
    fun getById(id: String): UnifiedSport? {
        return allSports.find { it.id == id }
    }

    /**
     * Get all categories with their sport counts
     */
    fun getCategoriesWithCounts(): List<Pair<SportCategory, Int>> {
        return SportCategory.entries.map { category ->
            category to allSports.count { it.category == category }
        }.filter { (_, count) -> count > 0 }
    }

    /**
     * Get popular/suggested sports for quick selection
     */
    fun getPopularSports(): List<UnifiedSport> {
        val popularIds = listOf(
            "powerlifting", "weightlifting", "crossfit", "bodybuilding",
            "football_soccer", "basketball", "tennis", "running_marathon",
            "swimming", "boxing", "mma", "golf"
        )
        return popularIds.mapNotNull { getById(it) }
    }
}
package com.example.menotracker.ui.guidance

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * GUIDANCE CONTENT - All Hints, Spotlights and Tours
 * ══════════════════════════════════════════════════════════════════════════════
 *
 * RULES FOR GOOD CONTENT:
 * - PulseHint: MAX 8 WORDS
 * - SpotlightPoint: MAX 10 WORDS per point
 * - TourStep description: MAX 15 WORDS
 *
 * LANGUAGE: English, direct, active voice
 *
 * ══════════════════════════════════════════════════════════════════════════════
 */

// ═══════════════════════════════════════════════════════════════════════════════
// HINT IDs - For GuidanceManager tracking
// ═══════════════════════════════════════════════════════════════════════════════

object GuidanceIds {
    // Home
    const val HOME_COACH_SUGGESTIONS = "home_coach_suggestions"
    const val HOME_QUICK_START = "home_quick_start"
    const val HOME_LAB_INTRO = "home_lab_intro"

    // VBT & Form Analysis
    const val VBT_CAMERA_POSITION = "vbt_camera_position"
    const val VBT_CALIBRATION_NEEDED = "vbt_calibration_needed"
    const val VBT_METRICS_MEANING = "vbt_metrics_meaning"
    const val FORM_VIEW_PROFILE = "form_view_profile"
    const val FORM_RECORD_VS_VBT = "form_record_vs_vbt"
    const val FORM_LEVELING = "form_leveling"
    const val FORM_TRACKER_BOX = "form_tracker_box"

    // Nutrition
    const val NUTRITION_FAB_BUTTONS = "nutrition_fab_buttons"
    const val NUTRITION_MACRO_COLORS = "nutrition_macro_colors"
    const val NUTRITION_QUICK_ADD = "nutrition_quick_add"
    const val NUTRITION_STREAK = "nutrition_streak"
    const val NUTRITION_PHOTO_AI = "nutrition_photo_ai"

    // Training & Workouts
    const val TRAINING_PROGRAM_ACTIVE = "training_program_active"
    const val WORKOUT_SESSION_START = "workout_session_start"
    const val WORKOUT_SET_LOGGING = "workout_set_logging"
    const val WORKOUT_REST_TIMER = "workout_rest_timer"
    const val WORKOUT_VBT_RECORDING = "workout_vbt_recording"

    // Lab
    const val LAB_VBT_TAB = "lab_vbt_tab"
    const val LAB_LOAD_TAB = "lab_load_tab"
    const val LAB_TRENDS_TAB = "lab_trends_tab"
    const val LAB_INSIGHTS_TAB = "lab_insights_tab"

    // AI Coach
    const val COACH_ATTACHMENTS = "coach_attachments"
    const val COACH_SAVE_WORKOUT = "coach_save_workout"
    const val COACH_CONTEXT = "coach_context"

    // Library
    const val LIBRARY_TABS = "library_tabs"
    const val LIBRARY_TEMPLATES = "library_templates"
    const val LIBRARY_CREATE = "library_create"

    // Spotlights
    const val SPOTLIGHT_VBT_INTRO = "spotlight_vbt_intro"
    const val SPOTLIGHT_NUTRITION_INTRO = "spotlight_nutrition_intro"
    const val SPOTLIGHT_LAB_INTRO = "spotlight_lab_intro"
    const val SPOTLIGHT_TRACKER_BOX = "spotlight_tracker_box"
    const val SPOTLIGHT_AI_COACH = "spotlight_ai_coach"
    const val SPOTLIGHT_WORKOUT_SESSION = "spotlight_workout_session"
    const val SPOTLIGHT_LIBRARY = "spotlight_library"
    const val SPOTLIGHT_TRAINING = "spotlight_training"
    const val SPOTLIGHT_HOME = "spotlight_home"
    const val SPOTLIGHT_CALIBRATION = "spotlight_calibration"
    const val SPOTLIGHT_PHYSICAL_COACH = "spotlight_physical_coach"
    const val SPOTLIGHT_ACCOUNT = "spotlight_account"
    const val SPOTLIGHT_INSIGHTS = "spotlight_insights"

    // Tours
    const val TOUR_FIRST_WORKOUT = "tour_first_workout"
    const val TOUR_FIRST_VBT = "tour_first_vbt"
    const val TOUR_NUTRITION_SETUP = "tour_nutrition_setup"
}


// ═══════════════════════════════════════════════════════════════════════════════
// PULSE HINTS - Max 8 words!
// ═══════════════════════════════════════════════════════════════════════════════

object PulseHints {
    // ─────────────────────────────────────────────────────────────────────────
    // HOME SCREEN
    // ─────────────────────────────────────────────────────────────────────────
    val HOME_COACH = "Your coach knows all your PRs"
    val HOME_QUICK_START = "Tap here to start directly"
    val HOME_LAB = "Your training data visualized"

    // ─────────────────────────────────────────────────────────────────────────
    // VBT & FORM ANALYSIS
    // ─────────────────────────────────────────────────────────────────────────
    val VBT_CAMERA = "Film sideways for best bar path detection"
    val VBT_CALIBRATE = "Calibrate for real m/s values"
    val VBT_LEVEL = "Green = phone is level"
    val FORM_PROFILE = "Profile view shows your bar path"
    val FORM_RECORD_VS_VBT = "VBT = with velocity analysis"

    // ─────────────────────────────────────────────────────────────────────────
    // NUTRITION
    // ─────────────────────────────────────────────────────────────────────────
    val NUTRITION_SNAP = "Snap photo, AI detects your food"
    val NUTRITION_QUICK = "Log your frequent meals quickly"
    val NUTRITION_SCAN = "Scan barcode for exact nutrition values"
    val NUTRITION_COLORS = "Blue=Protein, Purple=Carbs, Yellow=Fat"
    val NUTRITION_STREAK = "Log daily to keep your streak"

    // ─────────────────────────────────────────────────────────────────────────
    // TRAINING & WORKOUTS
    // ─────────────────────────────────────────────────────────────────────────
    val WORKOUT_START = "START begins timer and tracking"
    val WORKOUT_LOG_SET = "Enter your actual reps"
    val WORKOUT_REST = "Rest timer with vibration at end"
    val WORKOUT_CAMERA = "Camera icon for form check this set"
    val TRAINING_PROGRAM = "Your active program for this week"

    // ─────────────────────────────────────────────────────────────────────────
    // LAB
    // ─────────────────────────────────────────────────────────────────────────
    val LAB_VBT = "Your velocity data over time"
    val LAB_LOAD = "Training volume and load"
    val LAB_TRENDS = "Spot progress and plateaus"
    val LAB_INSIGHTS = "AI recommendations based on data"

    // ─────────────────────────────────────────────────────────────────────────
    // AI COACH
    // ─────────────────────────────────────────────────────────────────────────
    val COACH_ATTACH = "Attach DEXA, bloodwork or physio report"
    val COACH_SAVE = "Save workout directly to your library"
    val COACH_KNOWS = "Knows your workouts, PRs and goals"

    // ─────────────────────────────────────────────────────────────────────────
    // LIBRARY
    // ─────────────────────────────────────────────────────────────────────────
    val LIBRARY_BROWSE = "Browse all exercises and templates"
    val LIBRARY_MY = "Your custom created exercises"
    val LIBRARY_CREATE = "Create custom exercise or workout"
}


// ═══════════════════════════════════════════════════════════════════════════════
// FEATURE SPOTLIGHTS - For complex features (max 3 points à 10 words)
// ═══════════════════════════════════════════════════════════════════════════════

object Spotlights {

    // ─────────────────────────────────────────────────────────────────────────
    // VBT INTRODUCTION
    // ─────────────────────────────────────────────────────────────────────────
    val VBT_INTRO = SpotlightContent(
        id = GuidanceIds.SPOTLIGHT_VBT_INTRO,
        title = "Velocity Based Training",
        icon = Icons.Default.Speed,
        points = listOf(
            SpotlightPoint(
                icon = Icons.Default.Videocam,
                text = "Film your sets from the side for precise bar path analysis"
            ),
            SpotlightPoint(
                icon = Icons.Default.Speed,
                text = "Velocity shows your true daily readiness and fatigue"
            ),
            SpotlightPoint(
                icon = Icons.Default.TrendingUp,
                text = "Velocity drop over 20%? Time to end the set"
            )
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // TRACKER BOX EXPLANATION - VBT tracking behavior
    // ─────────────────────────────────────────────────────────────────────────
    val TRACKER_BOX = SpotlightContent(
        id = GuidanceIds.SPOTLIGHT_TRACKER_BOX,
        title = "Barbell Tracker",
        icon = Icons.Default.CropFree,
        points = listOf(
            SpotlightPoint(
                icon = Icons.Default.Visibility,
                text = "Green box shows barbell detected - confirms tracking is active"
            ),
            SpotlightPoint(
                icon = Icons.Default.Speed,
                text = "Box stays visible during VBT recording for real-time feedback"
            ),
            SpotlightPoint(
                icon = Icons.Default.TrendingUp,
                text = "Watch velocity in real-time to optimize every rep"
            )
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // NUTRITION
    // ─────────────────────────────────────────────────────────────────────────
    val NUTRITION_INTRO = SpotlightContent(
        id = GuidanceIds.SPOTLIGHT_NUTRITION_INTRO,
        title = "Nutrition Tracking",
        icon = Icons.Default.Restaurant,
        points = listOf(
            SpotlightPoint(
                icon = Icons.Default.PhotoCamera,
                text = "Take a photo - AI automatically detects meal and macros"
            ),
            SpotlightPoint(
                icon = Icons.Default.QrCodeScanner,
                text = "Scan barcode for exact nutrition values from database"
            ),
            SpotlightPoint(
                icon = Icons.Default.Bolt,
                text = "Quick-Add saves your frequent meals for fast logging"
            )
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // MENO LAB
    // ─────────────────────────────────────────────────────────────────────────
    val LAB_INTRO = SpotlightContent(
        id = GuidanceIds.SPOTLIGHT_LAB_INTRO,
        title = "Naya Lab",
        icon = Icons.Default.Science,
        points = listOf(
            SpotlightPoint(
                icon = Icons.Default.Speed,
                text = "VBT Tab: Your velocity progression and power trends"
            ),
            SpotlightPoint(
                icon = Icons.Default.FitnessCenter,
                text = "Load Tab: Training volume, ACWR and overtraining warning"
            ),
            SpotlightPoint(
                icon = Icons.Default.Lightbulb,
                text = "Insights: AI analyzes your data and gives recommendations"
            )
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // AI COACH
    // ─────────────────────────────────────────────────────────────────────────
    val AI_COACH_INTRO = SpotlightContent(
        id = GuidanceIds.SPOTLIGHT_AI_COACH,
        title = "Your AI Coach",
        icon = Icons.Default.Psychology,
        points = listOf(
            SpotlightPoint(
                icon = Icons.Default.Memory,
                text = "Knows your PRs, workout history and training goals"
            ),
            SpotlightPoint(
                icon = Icons.Default.AttachFile,
                text = "Attach DEXA scans, bloodwork or physio reports for context"
            ),
            SpotlightPoint(
                icon = Icons.Default.SaveAlt,
                text = "Save generated workouts directly to your library"
            )
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // WORKOUT SESSION
    // ─────────────────────────────────────────────────────────────────────────
    val WORKOUT_SESSION_INTRO = SpotlightContent(
        id = GuidanceIds.SPOTLIGHT_WORKOUT_SESSION,
        title = "Active Workout",
        icon = Icons.Default.FitnessCenter,
        points = listOf(
            SpotlightPoint(
                icon = Icons.Default.PlayArrow,
                text = "Tap START to begin - timer and tracking activate automatically"
            ),
            SpotlightPoint(
                icon = Icons.Default.Timer,
                text = "Rest timer starts after each set with vibration alert"
            ),
            SpotlightPoint(
                icon = Icons.Default.Videocam,
                text = "Camera icon records your set for form analysis"
            )
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // LIBRARY
    // ─────────────────────────────────────────────────────────────────────────
    val LIBRARY_INTRO = SpotlightContent(
        id = GuidanceIds.SPOTLIGHT_LIBRARY,
        title = "Exercise Library",
        icon = Icons.Default.LibraryBooks,
        points = listOf(
            SpotlightPoint(
                icon = Icons.Default.Search,
                text = "Browse 500+ exercises with video demos and cues"
            ),
            SpotlightPoint(
                icon = Icons.Default.Bookmark,
                text = "Save templates and create custom workout programs"
            ),
            SpotlightPoint(
                icon = Icons.Default.Add,
                text = "Create your own exercises with custom tracking"
            )
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // TRAINING / PROGRAMS
    // ─────────────────────────────────────────────────────────────────────────
    val TRAINING_INTRO = SpotlightContent(
        id = GuidanceIds.SPOTLIGHT_TRAINING,
        title = "Training Programs",
        icon = Icons.Default.CalendarMonth,
        points = listOf(
            SpotlightPoint(
                icon = Icons.Default.EventNote,
                text = "Your active program shows this week's scheduled workouts"
            ),
            SpotlightPoint(
                icon = Icons.Default.SwapHoriz,
                text = "Drag to reorder or swap workouts between days"
            ),
            SpotlightPoint(
                icon = Icons.Default.AutoGraph,
                text = "Progressive overload is auto-calculated each week"
            )
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // HOME SCREEN
    // ─────────────────────────────────────────────────────────────────────────
    val HOME_INTRO = SpotlightContent(
        id = GuidanceIds.SPOTLIGHT_HOME,
        title = "Welcome to Naya",
        icon = Icons.Default.Home,
        points = listOf(
            SpotlightPoint(
                icon = Icons.Default.TrendingUp,
                text = "Your dashboard shows today's workout and weekly progress"
            ),
            SpotlightPoint(
                icon = Icons.Default.Psychology,
                text = "AI Coach gives personalized suggestions based on your data"
            ),
            SpotlightPoint(
                icon = Icons.Default.Science,
                text = "Lab analyzes your training with pro-level metrics"
            )
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // CALIBRATION
    // ─────────────────────────────────────────────────────────────────────────
    val CALIBRATION_INTRO = SpotlightContent(
        id = GuidanceIds.SPOTLIGHT_CALIBRATION,
        title = "VBT Calibration",
        icon = Icons.Default.Tune,
        points = listOf(
            SpotlightPoint(
                icon = Icons.Default.Straighten,
                text = "Calibrate once to convert pixels to real m/s values"
            ),
            SpotlightPoint(
                icon = Icons.Default.CropFree,
                text = "Place a known object (plate) in frame and mark its size"
            ),
            SpotlightPoint(
                icon = Icons.Default.Check,
                text = "After calibration, all velocity readings are accurate"
            )
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // PHYSICAL COACH
    // ─────────────────────────────────────────────────────────────────────────
    val PHYSICAL_COACH_INTRO = SpotlightContent(
        id = GuidanceIds.SPOTLIGHT_PHYSICAL_COACH,
        title = "Find a Coach",
        icon = Icons.Default.Person,
        points = listOf(
            SpotlightPoint(
                icon = Icons.Default.LocationOn,
                text = "Browse certified coaches near you or online"
            ),
            SpotlightPoint(
                icon = Icons.Default.Star,
                text = "Filter by specialty: Powerlifting, Olympic, Bodybuilding"
            ),
            SpotlightPoint(
                icon = Icons.Default.Share,
                text = "Share your Naya data directly with your coach"
            )
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // ACCOUNT / PROFILE
    // ─────────────────────────────────────────────────────────────────────────
    val ACCOUNT_INTRO = SpotlightContent(
        id = GuidanceIds.SPOTLIGHT_ACCOUNT,
        title = "Your Profile",
        icon = Icons.Default.AccountCircle,
        points = listOf(
            SpotlightPoint(
                icon = Icons.Default.Edit,
                text = "Update weight, height and goals for accurate calculations"
            ),
            SpotlightPoint(
                icon = Icons.Default.BarChart,
                text = "Track body measurements and progress photos over time"
            ),
            SpotlightPoint(
                icon = Icons.Default.Settings,
                text = "Customize units, notifications and app preferences"
            )
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // INSIGHTS / ANALYTICS
    // ─────────────────────────────────────────────────────────────────────────
    val INSIGHTS_INTRO = SpotlightContent(
        id = GuidanceIds.SPOTLIGHT_INSIGHTS,
        title = "Training Insights",
        icon = Icons.Default.Insights,
        points = listOf(
            SpotlightPoint(
                icon = Icons.Default.TrendingUp,
                text = "See strength progression and estimated 1RM over time"
            ),
            SpotlightPoint(
                icon = Icons.Default.PieChart,
                text = "Volume distribution shows muscle group balance"
            ),
            SpotlightPoint(
                icon = Icons.Default.Warning,
                text = "Fatigue alerts warn when you're at overtraining risk"
            )
        )
    )
}

data class SpotlightContent(
    val id: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val points: List<SpotlightPoint>
)


// ═══════════════════════════════════════════════════════════════════════════════
// INTERACTIVE TOURS - For critical first-time workflows
// ═══════════════════════════════════════════════════════════════════════════════

object Tours {

    // ─────────────────────────────────────────────────────────────────────────
    // FIRST WORKOUT SESSION
    // ─────────────────────────────────────────────────────────────────────────
    val FIRST_WORKOUT = listOf(
        TourStep(
            id = "workout_1",
            title = "Start Workout",
            description = "Tap START to begin your workout. The timer starts automatically.",
            targetId = "start_button",
            position = TourPosition.BOTTOM
        ),
        TourStep(
            id = "workout_2",
            title = "Log Set",
            description = "Enter your actual reps and weight. Swipe for next set.",
            targetId = "set_input",
            position = TourPosition.TOP
        ),
        TourStep(
            id = "workout_3",
            title = "Rest Timer",
            description = "After each set your rest timer starts automatically. Vibration when done.",
            targetId = "rest_timer",
            position = TourPosition.TOP
        ),
        TourStep(
            id = "workout_4",
            title = "Record Form",
            description = "Tap the camera icon to film this set. Perfect for heavy sets!",
            targetId = "camera_button",
            position = TourPosition.BOTTOM
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // FIRST VBT RECORDING
    // ─────────────────────────────────────────────────────────────────────────
    val FIRST_VBT = listOf(
        TourStep(
            id = "vbt_1",
            title = "Position Camera",
            description = "Place your phone sideways. The level bubble helps you align it.",
            targetId = "level_indicator",
            position = TourPosition.BOTTOM
        ),
        TourStep(
            id = "vbt_2",
            title = "Choose Profile View",
            description = "Select 'Profile' for best bar path detection on squats and deadlifts.",
            targetId = "view_profile",
            position = TourPosition.TOP
        ),
        TourStep(
            id = "vbt_3",
            title = "Record VBT",
            description = "Tap the VBT button to record with velocity tracking.",
            targetId = "vbt_record_button",
            position = TourPosition.TOP
        ),
        TourStep(
            id = "vbt_4",
            title = "Understand Results",
            description = "After the set you'll see velocity, power score and technique rating.",
            targetId = "results_panel",
            position = TourPosition.BOTTOM
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // NUTRITION SETUP
    // ─────────────────────────────────────────────────────────────────────────
    val NUTRITION_SETUP = listOf(
        TourStep(
            id = "nutrition_1",
            title = "Daily Goals",
            description = "Your macro goals are calculated from your profile. Tap to adjust.",
            targetId = "macro_goals",
            position = TourPosition.BOTTOM
        ),
        TourStep(
            id = "nutrition_2",
            title = "Add Meal",
            description = "4 ways: Photo, Barcode, Search or Quick-Add for frequent meals.",
            targetId = "fab_buttons",
            position = TourPosition.TOP
        ),
        TourStep(
            id = "nutrition_3",
            title = "Track Progress",
            description = "The bars show your daily progress. Blue=Protein, Purple=Carbs, Yellow=Fat.",
            targetId = "progress_bars",
            position = TourPosition.BOTTOM
        )
    )
}


// ═══════════════════════════════════════════════════════════════════════════════
// METRIC EXPLANATIONS - For Lab and VBT Metrics
// ═══════════════════════════════════════════════════════════════════════════════

object MetricExplanations {
    // VBT Metrics
    val PEAK_VELOCITY = "Highest speed during the movement (m/s)"
    val AVG_VELOCITY = "Average speed over the entire set"
    val VELOCITY_DROP = "How much slower you got. >20% = fatigue"
    val POWER_SCORE = "Force × Speed. Shows your explosive output"
    val TECH_SCORE = "How clean and consistent your technique was (0-100)"
    val BAR_PATH_DRIFT = "Deviation from ideal line. Less = better"
    val ROM = "Range of Motion - your movement range in cm"
    val FATIGUE_INDEX = "Fatigue indicator. Red = time to stop"

    // Lab Metrics
    val ACWR = "Acute:Chronic Workload Ratio - current vs usual load"
    val TONNAGE = "Total weight moved (Sets × Reps × Weight)"
    val FREQUENCY = "How often you train per week"
    val VOLUME = "Number of working sets per muscle group"

    // Nutrition Metrics
    val TDEE = "Total Daily Energy Expenditure - your daily calorie needs"
    val PROTEIN_TARGET = "Recommended: 1.6-2.2g per kg bodyweight"
    val DEFICIT_SURPLUS = "Difference between eaten and burned"
}


// ═══════════════════════════════════════════════════════════════════════════════
// CONTEXTUAL BADGES - Status explanations
// ═══════════════════════════════════════════════════════════════════════════════

object BadgeExplanations {
    val CALIBRATED = "Camera calibrated for real velocity values in m/s"
    val UNCALIBRATED = "Relative values. Calibrate for precise m/s measurements"
    val STREAK_ACTIVE = "You're logging every day! Keep going for habit building"
    val PR_NEW = "New personal record! Your best performance yet"
    val FATIGUE_WARNING = "Velocity dropping fast. Consider ending the set"
    val OVERTRAINING_RISK = "ACWR over 1.5 - increased injury risk"
}
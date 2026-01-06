package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Menopause-spezifische Datenmodelle
 */

// ============================================================
// SYMPTOM TRACKING
// ============================================================

/**
 * Menopause-Symptom-Typen
 */
enum class MenopauseSymptomType(
    val displayName: String,
    val description: String,
    val category: SymptomCategory
) {
    HOT_FLASH("Hot Flash", "Sudden feeling of warmth", SymptomCategory.VASOMOTOR),
    NIGHT_SWEAT("Night Sweat", "Sweating during sleep", SymptomCategory.VASOMOTOR),
    MOOD_SWING("Mood Swing", "Emotional changes", SymptomCategory.PSYCHOLOGICAL),
    ANXIETY("Anxiety", "Nervousness or worry", SymptomCategory.PSYCHOLOGICAL),
    FATIGUE("Fatigue", "Exhaustion and low energy", SymptomCategory.GENERAL),
    BRAIN_FOG("Brain Fog", "Concentration problems", SymptomCategory.COGNITIVE),
    SLEEP_ISSUE("Sleep Issues", "Difficulty falling or staying asleep", SymptomCategory.SLEEP),
    JOINT_PAIN("Joint Pain", "Pain in joints", SymptomCategory.PHYSICAL),
    HEADACHE("Headache", "Migraine or tension headache", SymptomCategory.PHYSICAL),
    WEIGHT_GAIN("Weight Gain", "Unwanted weight gain", SymptomCategory.METABOLIC),
    LOW_LIBIDO("Low Libido", "Reduced sexual interest", SymptomCategory.SEXUAL),
    VAGINAL_DRYNESS("Vaginal Dryness", "Mucosal dryness", SymptomCategory.UROGENITAL),
    HEART_PALPITATIONS("Heart Palpitations", "Irregular heartbeat", SymptomCategory.VASOMOTOR),
    IRRITABILITY("Irritability", "Increased irritability", SymptomCategory.PSYCHOLOGICAL)
}

enum class SymptomCategory(val displayName: String) {
    VASOMOTOR("Vasomotor"),
    PSYCHOLOGICAL("Psychological"),
    COGNITIVE("Cognitive"),
    PHYSICAL("Physical"),
    SLEEP("Sleep"),
    METABOLIC("Metabolic"),
    SEXUAL("Sexual"),
    UROGENITAL("Urogenital"),
    GENERAL("General")
}

/**
 * Symptom-Log Eintrag
 */
@Serializable
data class SymptomLog(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("symptom_type") val symptomType: String,
    val intensity: Int, // 1-10
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    val triggers: List<String>? = null,
    val notes: String? = null,
    @SerialName("logged_at") val loggedAt: String,
    @SerialName("created_at") val createdAt: String? = null
) {
    val symptomEnum: MenopauseSymptomType?
        get() = try {
            MenopauseSymptomType.valueOf(symptomType)
        } catch (e: Exception) {
            null
        }

    val intensityLevel: IntensityLevel
        get() = when (intensity) {
            in 1..3 -> IntensityLevel.MILD
            in 4..6 -> IntensityLevel.MODERATE
            in 7..10 -> IntensityLevel.SEVERE
            else -> IntensityLevel.MILD
        }
}

enum class IntensityLevel(val displayName: String, val color: Long) {
    MILD("Mild", 0xFF10B981), // Green
    MODERATE("Moderate", 0xFFFBBF24), // Yellow
    SEVERE("Severe", 0xFFEF4444) // Red
}

@Serializable
data class SymptomLogInsert(
    val user_id: String,
    val symptom_type: String,
    val intensity: Int,
    val duration_minutes: Int? = null,
    val triggers: List<String>? = null,
    val notes: String? = null,
    val logged_at: String
)

/**
 * Symptom-Statistik für Dashboard
 */
data class SymptomStats(
    val symptomType: MenopauseSymptomType,
    val occurrenceCount: Int,
    val avgIntensity: Float,
    val maxIntensity: Int,
    val trend: SymptomTrend = SymptomTrend.STABLE
)

enum class SymptomTrend { IMPROVING, STABLE, WORSENING }

// ============================================================
// MENOPAUSE PROFILE
// ============================================================

/**
 * Menopause-Stadium
 */
enum class MenopauseStage(
    val displayName: String,
    val description: String,
    val order: Int
) {
    PREMENOPAUSE(
        "Premenopause",
        "Regular cycles, no symptoms",
        0
    ),
    EARLY_PERIMENOPAUSE(
        "Early Perimenopause",
        "Cycles becoming irregular",
        1
    ),
    LATE_PERIMENOPAUSE(
        "Late Perimenopause",
        "60+ days without period",
        2
    ),
    MENOPAUSE(
        "Menopause",
        "12 months without period",
        3
    ),
    POSTMENOPAUSE(
        "Postmenopause",
        "More than 12 months without period",
        4
    )
}

/**
 * HRT (Hormonersatztherapie) Status
 */
enum class HRTStatus(val displayName: String) {
    NONE("None"),
    CONSIDERING("Considering"),
    CURRENT("Current"),
    PAST("Past")
}

@Serializable
data class MenopauseProfile(
    val id: String,
    @SerialName("user_id") val userId: String,
    val stage: String = "premenopause",
    @SerialName("last_period_date") val lastPeriodDate: String? = null,
    @SerialName("average_cycle_length") val averageCycleLength: Int? = null,
    @SerialName("hrt_status") val hrtStatus: String = "none",
    @SerialName("primary_symptoms") val primarySymptoms: List<String>? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val stageEnum: MenopauseStage
        get() = try {
            MenopauseStage.valueOf(stage.uppercase())
        } catch (e: Exception) {
            MenopauseStage.PREMENOPAUSE
        }

    val hrtStatusEnum: HRTStatus
        get() = try {
            HRTStatus.valueOf(hrtStatus.uppercase())
        } catch (e: Exception) {
            HRTStatus.NONE
        }
}

// ============================================================
// SLEEP TRACKING
// ============================================================

/**
 * Schlaf-Unterbrechungsgründe
 */
enum class SleepInterruptionReason(val displayName: String) {
    NIGHT_SWEAT("Night Sweat"),
    HOT_FLASH("Hot Flash"),
    BATHROOM("Bathroom"),
    ANXIETY("Anxiety"),
    PAIN("Pain"),
    NOISE("Noise"),
    OTHER("Other")
}

@Serializable
data class SleepLog(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    @SerialName("bed_time") val bedTime: String? = null,
    @SerialName("wake_time") val wakeTime: String? = null,
    @SerialName("total_hours") val totalHours: Float? = null,
    @SerialName("quality_rating") val qualityRating: Int? = null, // 1-5
    val interruptions: Int = 0,
    @SerialName("interruption_reasons") val interruptionReasons: List<String>? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    val qualityLevel: SleepQuality
        get() = when (qualityRating) {
            1 -> SleepQuality.POOR
            2 -> SleepQuality.FAIR
            3 -> SleepQuality.GOOD
            4 -> SleepQuality.VERY_GOOD
            5 -> SleepQuality.EXCELLENT
            else -> SleepQuality.GOOD
        }
}

enum class SleepQuality(val displayName: String, val color: Long) {
    POOR("Poor", 0xFFEF4444),
    FAIR("Fair", 0xFFF97316),
    GOOD("Good", 0xFFFBBF24),
    VERY_GOOD("Very Good", 0xFF84CC16),
    EXCELLENT("Excellent", 0xFF10B981)
}

@Serializable
data class SleepLogInsert(
    val user_id: String,
    val date: String,
    val bed_time: String? = null,
    val wake_time: String? = null,
    val total_hours: Float? = null,
    val quality_rating: Int? = null,
    val interruptions: Int = 0,
    val interruption_reasons: List<String>? = null,
    val notes: String? = null
)

// ============================================================
// BONE HEALTH TRACKING
// ============================================================

@Serializable
data class BoneHealthLog(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    @SerialName("calcium_mg") val calciumMg: Float = 0f,
    @SerialName("vitamin_d_iu") val vitaminDIu: Float = 0f,
    @SerialName("omega3_mg") val omega3Mg: Float = 0f,
    @SerialName("magnesium_mg") val magnesiumMg: Float = 0f,
    @SerialName("strength_training_done") val strengthTrainingDone: Boolean = false,
    @SerialName("weight_bearing_minutes") val weightBearingMinutes: Int = 0,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    // Ziele für Frauen 40+
    companion object {
        const val CALCIUM_GOAL = 1200f // mg
        const val VITAMIN_D_GOAL = 800f // IU
        const val OMEGA3_GOAL = 1000f // mg
        const val MAGNESIUM_GOAL = 320f // mg
    }

    val calciumProgress: Float get() = (calciumMg / CALCIUM_GOAL).coerceIn(0f, 1f)
    val vitaminDProgress: Float get() = (vitaminDIu / VITAMIN_D_GOAL).coerceIn(0f, 1f)
    val omega3Progress: Float get() = (omega3Mg / OMEGA3_GOAL).coerceIn(0f, 1f)
    val magnesiumProgress: Float get() = (magnesiumMg / MAGNESIUM_GOAL).coerceIn(0f, 1f)
}

@Serializable
data class BoneHealthLogInsert(
    val user_id: String,
    val date: String,
    val calcium_mg: Float = 0f,
    val vitamin_d_iu: Float = 0f,
    val omega3_mg: Float = 0f,
    val magnesium_mg: Float = 0f,
    val strength_training_done: Boolean = false,
    val weight_bearing_minutes: Int = 0,
    val notes: String? = null
)

// ============================================================
// WEEKLY STATS
// ============================================================

data class WeeklySymptomSummary(
    val totalSymptoms: Int,
    val mostFrequent: MenopauseSymptomType?,
    val averageIntensity: Float,
    val symptomsByType: Map<MenopauseSymptomType, SymptomStats>,
    val trendComparedToLastWeek: SymptomTrend
)

data class WeeklySleepSummary(
    val averageHours: Float,
    val averageQuality: Float,
    val totalInterruptions: Int,
    val daysLogged: Int,
    val mostCommonInterruption: SleepInterruptionReason?
)

data class WeeklyBoneHealthSummary(
    val averageCalcium: Float,
    val averageVitaminD: Float,
    val averageOmega3: Float,
    val strengthTrainingDays: Int,
    val daysLogged: Int,
    val calciumGoalMet: Int, // Anzahl Tage
    val vitaminDGoalMet: Int
)

// ============================================================
// HEALTH DOCUMENTS
// ============================================================

/**
 * Dokumenttypen für Gesundheitsdokumente
 */
enum class HealthDocumentType(val displayName: String, val icon: String) {
    HORMONE_TEST("Hormone Test", "science"),
    DEXA_SCAN("Bone Density Scan", "skeleton"),
    MEDICAL_REPORT("Medical Report", "medical_report"),
    LAB_RESULT("Lab Result", "lab"),
    ULTRASOUND("Ultrasound", "ultrasound"),
    MAMMOGRAM("Mammogram", "mammogram"),
    OTHER("Other", "document")
}

/**
 * Gesundheitsdokument (PDF, Bild von Laborbericht etc.)
 */
@Serializable
data class HealthDocument(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("document_type") val documentType: String,
    val title: String,
    @SerialName("file_url") val fileUrl: String,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_size_bytes") val fileSizeBytes: Long? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("document_date") val documentDate: String? = null, // Datum des Tests/Berichts
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val documentTypeEnum: HealthDocumentType
        get() = try {
            HealthDocumentType.valueOf(documentType.uppercase())
        } catch (e: Exception) {
            HealthDocumentType.OTHER
        }

    val isPdf: Boolean
        get() = mimeType?.contains("pdf") == true || fileName?.endsWith(".pdf") == true

    val isImage: Boolean
        get() = mimeType?.startsWith("image/") == true ||
                fileName?.let { it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") } == true

    val formattedFileSize: String
        get() {
            val bytes = fileSizeBytes ?: return ""
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            }
        }
}

@Serializable
data class HealthDocumentInsert(
    val user_id: String,
    val document_type: String,
    val title: String,
    val file_url: String,
    val file_name: String? = null,
    val file_size_bytes: Long? = null,
    val mime_type: String? = null,
    val document_date: String? = null,
    val notes: String? = null
)

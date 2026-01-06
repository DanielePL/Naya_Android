package com.example.menotracker.data.repository

import android.util.Log
import com.example.menotracker.data.SupabaseClient
import com.example.menotracker.data.models.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Repository für Menopause-Profil und Hormon-Phasen
 */
object MenopauseProfileRepository {
    private const val TAG = "MenopauseProfileRepo"
    private const val TABLE_NAME = "menopause_profiles"

    private val supabase get() = SupabaseClient.client

    // ═══════════════════════════════════════════════════════════════
    // LOCAL STATE
    // ═══════════════════════════════════════════════════════════════

    private val _profile = MutableStateFlow<MenopauseProfile?>(null)
    val profile: StateFlow<MenopauseProfile?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // DATA CLASSES FOR INSERT/UPDATE
    // ═══════════════════════════════════════════════════════════════

    @Serializable
    private data class MenopauseProfileInsert(
        val user_id: String,
        val stage: String = "premenopause",
        val last_period_date: String? = null,
        val average_cycle_length: Int? = null,
        val hrt_status: String = "none",
        val primary_symptoms: List<String>? = null
    )

    // ═══════════════════════════════════════════════════════════════
    // GET PROFILE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Menopause-Profil laden
     */
    suspend fun getProfile(userId: String): Result<MenopauseProfile?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📊 Fetching menopause profile")

            val profiles = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<MenopauseProfile>()

            val profile = profiles.firstOrNull()
            _profile.value = profile

            Log.d(TAG, "✅ Profile loaded: ${profile?.stage ?: "none"}")
            Result.success(profile)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching profile", e)
            Result.failure(e)
        }
    }

    /**
     * Profil erstellen oder aktualisieren
     */
    suspend fun saveProfile(
        userId: String,
        stage: MenopauseStage,
        lastPeriodDate: LocalDate? = null,
        averageCycleLength: Int? = null,
        hrtStatus: HRTStatus = HRTStatus.NONE,
        primarySymptoms: List<MenopauseSymptomType>? = null
    ): Result<MenopauseProfile> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            Log.d(TAG, "📝 Saving menopause profile: ${stage.name}")

            val existingProfile = getProfile(userId).getOrNull()

            val profile = if (existingProfile != null) {
                // Update existing
                supabase.postgrest[TABLE_NAME]
                    .update({
                        set("stage", stage.name.lowercase())
                        lastPeriodDate?.let {
                            set("last_period_date", it.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        }
                        averageCycleLength?.let { set("average_cycle_length", it) }
                        set("hrt_status", hrtStatus.name.lowercase())
                        primarySymptoms?.let { set("primary_symptoms", it.map { s -> s.name }) }
                    }) {
                        filter {
                            eq("user_id", userId)
                        }
                    }

                getProfile(userId).getOrThrow()!!
            } else {
                // Create new
                val newProfile = MenopauseProfileInsert(
                    user_id = userId,
                    stage = stage.name.lowercase(),
                    last_period_date = lastPeriodDate?.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    average_cycle_length = averageCycleLength,
                    hrt_status = hrtStatus.name.lowercase(),
                    primary_symptoms = primarySymptoms?.map { it.name }
                )

                supabase.postgrest[TABLE_NAME]
                    .insert(newProfile)
                    .decodeSingle<MenopauseProfile>()
            }

            _profile.value = profile
            Log.d(TAG, "✅ Profile saved")
            Result.success(profile)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving profile", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE SPECIFIC FIELDS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Menopause-Stadium aktualisieren
     */
    suspend fun updateStage(userId: String, stage: MenopauseStage): Result<MenopauseProfile> {
        val current = _profile.value
        return saveProfile(
            userId = userId,
            stage = stage,
            lastPeriodDate = current?.lastPeriodDate?.let { LocalDate.parse(it) },
            averageCycleLength = current?.averageCycleLength,
            hrtStatus = current?.hrtStatusEnum ?: HRTStatus.NONE,
            primarySymptoms = current?.primarySymptoms?.mapNotNull {
                try { MenopauseSymptomType.valueOf(it) } catch (e: Exception) { null }
            }
        )
    }

    /**
     * Letzte Periode aktualisieren
     */
    suspend fun updateLastPeriod(userId: String, date: LocalDate): Result<MenopauseProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📝 Updating last period date: $date")

            supabase.postgrest[TABLE_NAME]
                .update({
                    set("last_period_date", date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                }) {
                    filter {
                        eq("user_id", userId)
                    }
                }

            val updated = getProfile(userId).getOrThrow()!!

            // Auto-update stage based on time since last period
            val daysSinceLastPeriod = ChronoUnit.DAYS.between(date, LocalDate.now())
            val suggestedStage = suggestStageBasedOnPeriod(daysSinceLastPeriod)

            if (suggestedStage != updated.stageEnum) {
                Log.d(TAG, "🔄 Suggesting stage change: ${suggestedStage.name}")
            }

            Result.success(updated)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating last period", e)
            Result.failure(e)
        }
    }

    /**
     * HRT-Status aktualisieren
     */
    suspend fun updateHRTStatus(userId: String, status: HRTStatus): Result<MenopauseProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📝 Updating HRT status: ${status.name}")

            supabase.postgrest[TABLE_NAME]
                .update({
                    set("hrt_status", status.name.lowercase())
                }) {
                    filter {
                        eq("user_id", userId)
                    }
                }

            val updated = getProfile(userId).getOrThrow()!!
            Result.success(updated)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating HRT status", e)
            Result.failure(e)
        }
    }

    /**
     * Primäre Symptome aktualisieren
     */
    suspend fun updatePrimarySymptoms(
        userId: String,
        symptoms: List<MenopauseSymptomType>
    ): Result<MenopauseProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📝 Updating primary symptoms: ${symptoms.size} items")

            supabase.postgrest[TABLE_NAME]
                .update({
                    set("primary_symptoms", symptoms.map { it.name })
                }) {
                    filter {
                        eq("user_id", userId)
                    }
                }

            val updated = getProfile(userId).getOrThrow()!!
            Result.success(updated)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating symptoms", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // STAGE CALCULATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Stadium basierend auf letzter Periode vorschlagen
     */
    fun suggestStageBasedOnPeriod(daysSinceLastPeriod: Long): MenopauseStage {
        return when {
            daysSinceLastPeriod < 35 -> MenopauseStage.PREMENOPAUSE
            daysSinceLastPeriod < 60 -> MenopauseStage.EARLY_PERIMENOPAUSE
            daysSinceLastPeriod < 365 -> MenopauseStage.LATE_PERIMENOPAUSE
            daysSinceLastPeriod < 400 -> MenopauseStage.MENOPAUSE
            else -> MenopauseStage.POSTMENOPAUSE
        }
    }

    /**
     * Tage seit letzter Periode berechnen
     */
    fun getDaysSinceLastPeriod(profile: MenopauseProfile?): Long? {
        val lastPeriod = profile?.lastPeriodDate ?: return null
        return try {
            val date = LocalDate.parse(lastPeriod)
            ChronoUnit.DAYS.between(date, LocalDate.now())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Stadium-spezifische Empfehlungen
     */
    fun getStageRecommendations(stage: MenopauseStage): List<String> {
        return when (stage) {
            MenopauseStage.PREMENOPAUSE -> listOf(
                "Regelmäßige Zyklusbeobachtung empfohlen",
                "Calcium und Vitamin D zur Vorbeugung",
                "Krafttraining für Knochengesundheit beginnen"
            )
            MenopauseStage.EARLY_PERIMENOPAUSE -> listOf(
                "Symptom-Tracking hilft Muster zu erkennen",
                "Stressmanagement wird wichtiger",
                "Schlafhygiene optimieren",
                "Mit Arzt über Veränderungen sprechen"
            )
            MenopauseStage.LATE_PERIMENOPAUSE -> listOf(
                "Symptome können intensiver werden",
                "Regelmäßige Bewegung hilft bei Stimmung",
                "Phytoöstrogen-reiche Ernährung erwägen",
                "HRT-Optionen mit Arzt besprechen"
            )
            MenopauseStage.MENOPAUSE -> listOf(
                "12 Monate ohne Periode = offiziell Menopause",
                "Knochengesundheit wird besonders wichtig",
                "Herz-Kreislauf-Gesundheit beachten",
                "Weiterhin aktiv bleiben"
            )
            MenopauseStage.POSTMENOPAUSE -> listOf(
                "Regelmäßige Vorsorgeuntersuchungen",
                "Knochengesundheit weiter priorisieren",
                "Krafttraining für Muskelerhalt",
                "Herzgesundheit im Fokus behalten"
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════

    /**
     * State zurücksetzen
     */
    fun clearState() {
        _profile.value = null
        _isLoading.value = false
    }
}

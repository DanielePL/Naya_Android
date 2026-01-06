package com.example.menotracker.data.repository

import android.util.Log
import com.example.menotracker.data.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Repository for tracking daily water/hydration intake
 * Integrates with Supabase hydration_logs table
 */
object HydrationRepository {
    private const val TAG = "HydrationRepository"
    private const val TABLE_NAME = "hydration_logs"

    private val supabase get() = SupabaseClient.client

    // ═══════════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════════

    @Serializable
    data class HydrationLog(
        val id: String,
        @SerialName("user_id") val userId: String,
        val date: String,
        @SerialName("water_intake_ml") val waterIntakeMl: Int,
        @SerialName("target_ml") val targetMl: Int,
        val entries: String? = null, // JSON array of entries
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
    ) {
        val progressPercent: Float
            get() = if (targetMl > 0) (waterIntakeMl.toFloat() / targetMl).coerceIn(0f, 1.5f) else 0f

        val remainingMl: Int
            get() = (targetMl - waterIntakeMl).coerceAtLeast(0)

        val isGoalReached: Boolean
            get() = waterIntakeMl >= targetMl
    }

    @Serializable
    private data class HydrationLogInsert(
        val user_id: String,
        val date: String,
        val water_intake_ml: Int = 0,
        val target_ml: Int = 2500
    )

    // ═══════════════════════════════════════════════════════════════
    // LOCAL STATE (for immediate UI updates)
    // ═══════════════════════════════════════════════════════════════

    private val _todayLog = MutableStateFlow<HydrationLog?>(null)
    val todayLog: StateFlow<HydrationLog?> = _todayLog.asStateFlow()

    // Quick-add amounts in ml
    val quickAddAmounts = listOf(250, 500, 750, 1000)

    // ═══════════════════════════════════════════════════════════════
    // GET TODAY'S LOG
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get or create today's hydration log
     */
    suspend fun getTodayLog(userId: String): Result<HydrationLog> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

            Log.d(TAG, "📊 Fetching hydration log for $today")

            // Try to get existing log
            val existingLogs = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("date", today)
                    }
                }
                .decodeList<HydrationLog>()

            if (existingLogs.isNotEmpty()) {
                val log = existingLogs.first()
                _todayLog.value = log
                Log.d(TAG, "✅ Found existing log: ${log.waterIntakeMl}ml / ${log.targetMl}ml")
                return@withContext Result.success(log)
            }

            // Create new log for today
            Log.d(TAG, "📝 Creating new hydration log for today")
            val newLog = HydrationLogInsert(
                user_id = userId,
                date = today,
                water_intake_ml = 0,
                target_ml = 2500 // Default, can be customized
            )

            val createdLog = supabase.postgrest[TABLE_NAME]
                .insert(newLog)
                .decodeSingle<HydrationLog>()

            _todayLog.value = createdLog
            Log.d(TAG, "✅ Created new hydration log")
            Result.success(createdLog)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting hydration log", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ADD WATER
    // ═══════════════════════════════════════════════════════════════

    /**
     * Add water intake (in milliliters)
     */
    suspend fun addWater(userId: String, amountMl: Int): Result<HydrationLog> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

            Log.d(TAG, "💧 Adding ${amountMl}ml water")

            // Optimistic UI update
            _todayLog.value?.let { current ->
                _todayLog.value = current.copy(
                    waterIntakeMl = current.waterIntakeMl + amountMl
                )
            }

            // Try RPC function first (handles upsert)
            try {
                val result = supabase.postgrest.rpc(
                    function = "add_water_intake",
                    parameters = buildJsonObject {
                        put("p_user_id", userId)
                        put("p_amount_ml", amountMl)
                        put("p_date", today)
                    }
                ).decodeSingle<HydrationLog>()

                _todayLog.value = result
                Log.d(TAG, "✅ Water added via RPC: ${result.waterIntakeMl}ml total")
                return@withContext Result.success(result)

            } catch (rpcError: Exception) {
                Log.w(TAG, "RPC not available, using direct update", rpcError)
            }

            // Fallback: Direct update
            // First ensure log exists
            val currentLog = getTodayLog(userId).getOrThrow()

            // Update the log
            supabase.postgrest[TABLE_NAME]
                .update({
                    set("water_intake_ml", currentLog.waterIntakeMl + amountMl)
                }) {
                    filter {
                        eq("user_id", userId)
                        eq("date", today)
                    }
                }

            // Fetch updated log
            val updatedLog = getTodayLog(userId).getOrThrow()
            Log.d(TAG, "✅ Water added: ${updatedLog.waterIntakeMl}ml total")
            Result.success(updatedLog)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding water", e)
            // Revert optimistic update
            getTodayLog(userId)
            Result.failure(e)
        }
    }

    /**
     * Quick add preset amount
     */
    suspend fun quickAdd(userId: String, presetIndex: Int): Result<HydrationLog> {
        val amount = quickAddAmounts.getOrElse(presetIndex) { 250 }
        return addWater(userId, amount)
    }

    // ═══════════════════════════════════════════════════════════════
    // SET TARGET
    // ═══════════════════════════════════════════════════════════════

    /**
     * Set daily water target (in milliliters)
     */
    suspend fun setDailyTarget(userId: String, targetMl: Int): Result<HydrationLog> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

            Log.d(TAG, "🎯 Setting water target to ${targetMl}ml")

            // Ensure log exists
            getTodayLog(userId)

            // Update target
            supabase.postgrest[TABLE_NAME]
                .update({
                    set("target_ml", targetMl)
                }) {
                    filter {
                        eq("user_id", userId)
                        eq("date", today)
                    }
                }

            // Fetch updated log
            val updatedLog = getTodayLog(userId).getOrThrow()
            Log.d(TAG, "✅ Target updated to ${updatedLog.targetMl}ml")
            Result.success(updatedLog)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting target", e)
            Result.failure(e)
        }
    }

    /**
     * Calculate recommended target based on body weight
     * Athletes: 40ml per kg body weight
     */
    fun calculateRecommendedTarget(bodyWeightKg: Float): Int {
        return (bodyWeightKg * 40).toInt().coerceIn(1500, 5000)
    }

    // ═══════════════════════════════════════════════════════════════
    // RESET / UNDO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reset today's water intake to zero
     */
    suspend fun resetToday(userId: String): Result<HydrationLog> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

            Log.d(TAG, "🔄 Resetting today's water intake")

            supabase.postgrest[TABLE_NAME]
                .update({
                    set("water_intake_ml", 0)
                    set("entries", "[]")
                }) {
                    filter {
                        eq("user_id", userId)
                        eq("date", today)
                    }
                }

            val updatedLog = getTodayLog(userId).getOrThrow()
            Log.d(TAG, "✅ Reset complete")
            Result.success(updatedLog)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error resetting", e)
            Result.failure(e)
        }
    }

    /**
     * Subtract water (for undo functionality)
     */
    suspend fun subtractWater(userId: String, amountMl: Int): Result<HydrationLog> = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

            Log.d(TAG, "➖ Subtracting ${amountMl}ml water")

            val currentLog = getTodayLog(userId).getOrThrow()
            val newAmount = (currentLog.waterIntakeMl - amountMl).coerceAtLeast(0)

            supabase.postgrest[TABLE_NAME]
                .update({
                    set("water_intake_ml", newAmount)
                }) {
                    filter {
                        eq("user_id", userId)
                        eq("date", today)
                    }
                }

            val updatedLog = getTodayLog(userId).getOrThrow()
            Log.d(TAG, "✅ Subtracted: ${updatedLog.waterIntakeMl}ml total")
            Result.success(updatedLog)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error subtracting water", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HISTORY
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get hydration logs for past N days
     */
    suspend fun getHistory(userId: String, days: Int = 7): Result<List<HydrationLog>> = withContext(Dispatchers.IO) {
        try {
            val startDate = LocalDate.now().minusDays(days.toLong())
                .format(DateTimeFormatter.ISO_LOCAL_DATE)

            Log.d(TAG, "📅 Fetching hydration history since $startDate")

            val logs = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("date", startDate)
                    }
                }
                .decodeList<HydrationLog>()

            Log.d(TAG, "✅ Found ${logs.size} hydration logs")
            Result.success(logs.sortedByDescending { it.date })

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching history", e)
            Result.failure(e)
        }
    }

    /**
     * Get weekly average intake
     */
    suspend fun getWeeklyAverage(userId: String): Result<Int> {
        return getHistory(userId, 7).map { logs ->
            if (logs.isEmpty()) 0
            else logs.sumOf { it.waterIntakeMl } / logs.size
        }
    }

    /**
     * Clear local state (e.g., on logout)
     */
    fun clearState() {
        _todayLog.value = null
    }
}
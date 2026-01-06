package com.example.menotracker.data

import android.util.Log
import com.example.menotracker.data.models.BodyMeasurement
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object BodyMeasurementRepository {
    private const val TAG = "BodyMeasurementRepo"
    private const val TABLE_NAME = "body_measurements"

    private val supabase get() = SupabaseClient.client

    // DTO for database operations
    @Serializable
    private data class BodyMeasurementDTO(
        val id: String? = null,
        @SerialName("client_id")
        val clientId: String,
        val date: String,
        val neck: Double? = null,
        val shoulders: Double? = null,
        val chest: Double? = null,
        val arms: Double? = null,
        val forearms: Double? = null,
        val waist: Double? = null,
        val hips: Double? = null,
        val glutes: Double? = null,
        val legs: Double? = null,
        val calves: Double? = null,
        @SerialName("created_at")
        val createdAt: String? = null
    )

    /**
     * Get the latest body measurement for a user
     */
    suspend fun getLatestMeasurement(userId: String): Result<BodyMeasurement?> {
        return try {
            Log.d(TAG, "Fetching latest measurement for user: $userId")

            val result = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("client_id", userId)
                    }
                    order("date", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<BodyMeasurementDTO>()

            val measurement = result.firstOrNull()?.toModel()
            Log.d(TAG, "Found measurement: ${measurement != null}")
            Result.success(measurement)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest measurement: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all measurements for a user (for history/progress tracking)
     */
    suspend fun getAllMeasurements(userId: String): Result<List<BodyMeasurement>> {
        return try {
            Log.d(TAG, "Fetching all measurements for user: $userId")

            val result = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("client_id", userId)
                    }
                    order("date", Order.DESCENDING)
                }
                .decodeList<BodyMeasurementDTO>()

            val measurements = result.map { it.toModel() }
            Log.d(TAG, "Found ${measurements.size} measurements")
            Result.success(measurements)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching measurements: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Save or update a body measurement
     */
    suspend fun saveMeasurement(measurement: BodyMeasurement): Result<BodyMeasurement> {
        return try {
            Log.d(TAG, "Saving measurement for date: ${measurement.date}")

            val dto = measurement.toDTO()

            // Check if measurement for this date already exists
            val existing = supabase.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("client_id", measurement.clientId)
                        eq("date", measurement.date)
                    }
                }
                .decodeList<BodyMeasurementDTO>()

            val savedDto = if (existing.isNotEmpty()) {
                // Update existing
                Log.d(TAG, "Updating existing measurement")
                supabase.postgrest[TABLE_NAME]
                    .update(dto) {
                        filter {
                            eq("client_id", measurement.clientId)
                            eq("date", measurement.date)
                        }
                    }
                    .decodeSingle<BodyMeasurementDTO>()
            } else {
                // Insert new
                Log.d(TAG, "Inserting new measurement")
                supabase.postgrest[TABLE_NAME]
                    .insert(dto)
                    .decodeSingle<BodyMeasurementDTO>()
            }

            Log.d(TAG, "Measurement saved successfully")
            Result.success(savedDto.toModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error saving measurement: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a measurement
     */
    suspend fun deleteMeasurement(measurementId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Deleting measurement: $measurementId")

            supabase.postgrest[TABLE_NAME]
                .delete {
                    filter {
                        eq("id", measurementId)
                    }
                }

            Log.d(TAG, "Measurement deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting measurement: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Extension functions for conversion
    private fun BodyMeasurementDTO.toModel(): BodyMeasurement {
        return BodyMeasurement(
            id = id ?: java.util.UUID.randomUUID().toString(),
            clientId = clientId,
            date = date,
            neck = neck,
            shoulders = shoulders,
            chest = chest,
            arms = arms,
            forearms = forearms,
            waist = waist,
            hips = hips,
            glutes = glutes,
            legs = legs,
            calves = calves,
            createdAt = createdAt
        )
    }

    private fun BodyMeasurement.toDTO(): BodyMeasurementDTO {
        return BodyMeasurementDTO(
            id = id,
            clientId = clientId,
            date = date,
            neck = neck,
            shoulders = shoulders,
            chest = chest,
            arms = arms,
            forearms = forearms,
            waist = waist,
            hips = hips,
            glutes = glutes,
            legs = legs,
            calves = calves,
            createdAt = createdAt
        )
    }
}
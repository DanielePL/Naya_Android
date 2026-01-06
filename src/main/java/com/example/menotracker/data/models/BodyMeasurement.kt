package com.example.menotracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class BodyMeasurement(
    val id: String = java.util.UUID.randomUUID().toString(),

    @SerialName("client_id")
    val clientId: String,

    val date: String, // ISO date format "YYYY-MM-DD"

    // All measurements in cm
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
) {
    companion object {
        val MEASUREMENT_LABELS = mapOf(
            "neck" to "Neck",
            "shoulders" to "Shoulders",
            "chest" to "Chest",
            "arms" to "Arms",
            "forearms" to "Forearms",
            "waist" to "Waist",
            "hips" to "Hips",
            "glutes" to "Glutes",
            "legs" to "Legs (Thighs)",
            "calves" to "Calves"
        )

        fun createNew(clientId: String): BodyMeasurement {
            return BodyMeasurement(
                clientId = clientId,
                date = java.time.LocalDate.now().toString()
            )
        }
    }

    /**
     * Get all non-null measurements as a map
     */
    fun toMeasurementMap(): Map<String, Double> {
        return buildMap {
            neck?.let { put("neck", it) }
            shoulders?.let { put("shoulders", it) }
            chest?.let { put("chest", it) }
            arms?.let { put("arms", it) }
            forearms?.let { put("forearms", it) }
            waist?.let { put("waist", it) }
            hips?.let { put("hips", it) }
            glutes?.let { put("glutes", it) }
            legs?.let { put("legs", it) }
            calves?.let { put("calves", it) }
        }
    }

    /**
     * Check if any measurement is set
     */
    fun hasMeasurements(): Boolean {
        return neck != null || shoulders != null || chest != null ||
               arms != null || forearms != null || waist != null ||
               hips != null || glutes != null || legs != null || calves != null
    }

    /**
     * Copy with updated measurement by key
     */
    fun withMeasurement(key: String, value: Double?): BodyMeasurement {
        return when (key) {
            "neck" -> copy(neck = value)
            "shoulders" -> copy(shoulders = value)
            "chest" -> copy(chest = value)
            "arms" -> copy(arms = value)
            "forearms" -> copy(forearms = value)
            "waist" -> copy(waist = value)
            "hips" -> copy(hips = value)
            "glutes" -> copy(glutes = value)
            "legs" -> copy(legs = value)
            "calves" -> copy(calves = value)
            else -> this
        }
    }

    /**
     * Get measurement by key
     */
    fun getMeasurement(key: String): Double? {
        return when (key) {
            "neck" -> neck
            "shoulders" -> shoulders
            "chest" -> chest
            "arms" -> arms
            "forearms" -> forearms
            "waist" -> waist
            "hips" -> hips
            "glutes" -> glutes
            "legs" -> legs
            "calves" -> calves
            else -> null
        }
    }
}
package com.example.menotracker.data.local

import androidx.room.TypeConverter
import com.example.menotracker.data.models.ActivityLevel
import com.example.menotracker.data.models.Gender
import com.example.menotracker.data.models.Injury
import com.example.menotracker.data.models.MedicalCondition
import com.example.menotracker.data.models.PersonalRecord
import com.example.menotracker.data.models.GoalRecord
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {

    private val json = Json { ignoreUnknownKeys = true }
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // ========== LocalDateTime ==========
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(dateTimeFormatter)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, dateTimeFormatter) }
    }

    // ========== LocalDate ==========
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.format(dateFormatter)
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, dateFormatter) }
    }

    // ========== Gender ==========
    @TypeConverter
    fun fromGender(value: Gender?): String? {
        return value?.name
    }

    @TypeConverter
    fun toGender(value: String?): Gender? {
        return value?.let {
            try { Gender.valueOf(it) } catch (e: Exception) { null }
        }
    }

    // ========== ActivityLevel ==========
    @TypeConverter
    fun fromActivityLevel(value: ActivityLevel?): String? {
        return value?.name
    }

    @TypeConverter
    fun toActivityLevel(value: String?): ActivityLevel? {
        return value?.let {
            try { ActivityLevel.valueOf(it) } catch (e: Exception) { null }
        }
    }

    // ========== PersonalRecords Map ==========
    @TypeConverter
    fun fromPersonalRecordsMap(value: Map<String, PersonalRecord>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toPersonalRecordsMap(value: String): Map<String, PersonalRecord> {
        return if (value.isEmpty()) {
            emptyMap()
        } else {
            json.decodeFromString(value)
        }
    }

    // ========== MedicalConditions List ==========
    @TypeConverter
    fun fromMedicalConditionsList(value: List<MedicalCondition>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toMedicalConditionsList(value: String): List<MedicalCondition> {
        return if (value.isEmpty()) {
            emptyList()
        } else {
            json.decodeFromString(value)
        }
    }

    // ========== Injuries List ==========
    @TypeConverter
    fun fromInjuriesList(value: List<Injury>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toInjuriesList(value: String): List<Injury> {
        return if (value.isEmpty()) {
            emptyList()
        } else {
            json.decodeFromString(value)
        }
    }

    // ========== String List (for goals, preferredSports) ==========
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) {
            emptyList()
        } else {
            json.decodeFromString(value)
        }
    }

    // ========== GoalRecords Map ==========
    @TypeConverter
    fun fromGoalRecordsMap(value: Map<String, GoalRecord>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toGoalRecordsMap(value: String): Map<String, GoalRecord> {
        return if (value.isEmpty()) {
            emptyMap()
        } else {
            json.decodeFromString(value)
        }
    }
}
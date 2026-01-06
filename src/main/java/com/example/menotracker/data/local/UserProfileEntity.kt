package com.example.menotracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.menotracker.data.models.ActivityLevel
import com.example.menotracker.data.models.Gender
import com.example.menotracker.data.models.Injury
import com.example.menotracker.data.models.MedicalCondition
import com.example.menotracker.data.models.PersonalRecord
import com.example.menotracker.data.models.GoalRecord
import com.example.menotracker.data.models.UserProfile

@Entity(tableName = "user_profiles")
@TypeConverters(Converters::class)
data class UserProfileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val weight: Double? = null,
    val height: Double? = null,
    val age: Int? = null,
    val gender: Gender? = null,
    val activityLevel: ActivityLevel? = null,
    val trainingExperience: Int? = null,
    val personalRecords: Map<String, PersonalRecord> = emptyMap(),
    val medicalConditions: List<MedicalCondition> = emptyList(),
    val injuries: List<Injury> = emptyList(),
    val goals: List<String> = emptyList(),
    val preferredSports: List<String> = emptyList(),
    val targetWorkoutDuration: Int? = null,
    val profileImageUrl: String? = null,
    val hasCoach: Boolean = false,
    val lastSeen: Long? = null,
    // Onboarding data
    val goalRecords: Map<String, GoalRecord> = emptyMap(),
    val sessionsPerWeek: Int? = null,
    val effortLevel: Int? = null,
    val experienceLevel: String? = null,
    val primaryGoals: List<String> = emptyList(),
    val featureInterests: List<String> = emptyList(),
    val coachSituation: String? = null,
    val onboardingCompletedAt: Long? = null,
    // Dietary preferences (can be multiple, e.g. Vegan + Halal)
    val dietaryPreferences: List<String> = emptyList(),
    val foodAllergies: List<String> = emptyList(),
    val foodDislikes: List<String> = emptyList(),
    val customAllergyNote: String? = null
)

// Extension functions to convert between UserProfile and UserProfileEntity
fun UserProfile.toEntity(): UserProfileEntity {
    return UserProfileEntity(
        id = id,
        name = name,
        weight = weight,
        height = height,
        age = age,
        gender = gender,
        activityLevel = activityLevel,
        trainingExperience = trainingExperience,
        personalRecords = personalRecords,
        medicalConditions = medicalConditions,
        injuries = injuries,
        goals = goals,
        preferredSports = preferredSports,
        targetWorkoutDuration = targetWorkoutDuration,
        profileImageUrl = profileImageUrl,
        hasCoach = hasCoach,
        lastSeen = lastSeen,
        // Onboarding data
        goalRecords = goalRecords,
        sessionsPerWeek = sessionsPerWeek,
        effortLevel = effortLevel,
        experienceLevel = experienceLevel,
        primaryGoals = primaryGoals,
        featureInterests = featureInterests,
        coachSituation = coachSituation,
        onboardingCompletedAt = onboardingCompletedAt,
        // Dietary preferences (can be multiple)
        dietaryPreferences = dietaryPreferences,
        foodAllergies = foodAllergies,
        foodDislikes = foodDislikes,
        customAllergyNote = customAllergyNote
    )
}

fun UserProfileEntity.toDomain(): UserProfile {
    return UserProfile(
        id = id,
        name = name,
        weight = weight,
        height = height,
        age = age,
        gender = gender,
        activityLevel = activityLevel,
        trainingExperience = trainingExperience,
        personalRecords = personalRecords,
        medicalConditions = medicalConditions,
        injuries = injuries,
        goals = goals,
        preferredSports = preferredSports,
        targetWorkoutDuration = targetWorkoutDuration,
        profileImageUrl = profileImageUrl,
        hasCoach = hasCoach,
        lastSeen = lastSeen,
        // Onboarding data
        goalRecords = goalRecords,
        sessionsPerWeek = sessionsPerWeek,
        effortLevel = effortLevel,
        experienceLevel = experienceLevel,
        primaryGoals = primaryGoals,
        featureInterests = featureInterests,
        coachSituation = coachSituation,
        onboardingCompletedAt = onboardingCompletedAt,
        // Dietary preferences (can be multiple)
        dietaryPreferences = dietaryPreferences,
        foodAllergies = foodAllergies,
        foodDislikes = foodDislikes,
        customAllergyNote = customAllergyNote
    )
}
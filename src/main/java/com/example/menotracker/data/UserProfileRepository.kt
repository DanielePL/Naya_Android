package com.example.menotracker.data

import android.content.Context
import com.example.menotracker.data.local.AppDatabase
import com.example.menotracker.data.local.toDomain
import com.example.menotracker.data.local.toEntity
import com.example.menotracker.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import java.io.File
import java.util.UUID

object UserProfileRepository {

    private var _currentProfile: UserProfile? = null
    private lateinit var database: AppDatabase

    fun initialize(context: Context) {
        database = AppDatabase.getInstance(context)
    }

    suspend fun getCurrentProfile(userId: String, forceRefresh: Boolean = false): Result<UserProfile> {
        return try {
            withContext(Dispatchers.IO) {
                println("🔑🔑🔑 FETCHING PROFILE FOR USER ID: $userId (forceRefresh=$forceRefresh)")

                // If force refresh, skip local and go directly to Supabase
                if (forceRefresh) {
                    println("🔄 Force refresh requested - fetching from Supabase directly for user: $userId")
                    return@withContext fetchFromSupabaseAndSave(userId)
                }

                // 1. First, try to load from local database
                val localProfile = database.userProfileDao().getProfile(userId)?.toDomain()

                if (localProfile != null) {
                    println("✅ Loaded profile from LOCAL database: ${localProfile.name}")
                    _currentProfile = localProfile

                    // Try to sync with Supabase in the background (don't wait for it)
                    try {
                        syncWithSupabase(userId)
                    } catch (e: Exception) {
                        println("⚠️ Background sync failed (using local data): ${e.message}")
                    }

                    return@withContext Result.success(localProfile)
                }

                // 2. If not in local DB, try to fetch from Supabase
                println("📡 No local data found. Fetching from Supabase...")
                val profiles = SupabaseClient.client
                    .from("user_profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeList<UserProfile>()

                val profile: UserProfile
                if (profiles.isNotEmpty()) {
                    profile = profiles.first()
                    println("✅ Loaded user profile from Supabase: ${profile.name}")
                } else {
                    // Create default profile and save to BOTH Supabase AND local
                    profile = createDefaultProfile(userId)
                    println("🆕 Creating new user profile in Supabase for: $userId")
                    try {
                        SupabaseClient.client
                            .from("user_profiles")
                            .upsert(profile)
                        println("✅ New user profile created in Supabase")
                    } catch (e: Exception) {
                        println("⚠️ Failed to create profile in Supabase: ${e.message}")
                        // Continue - local will still work
                    }
                }

                // Save to local database
                database.userProfileDao().upsertProfile(profile.toEntity())
                _currentProfile = profile

                println("🖼️ Profile image URL: ${profile.profileImageUrl}")
                Result.success(profile)
            }
        } catch (e: Exception) {
            println("❌ ERROR loading user profile: ${e.message}")
            e.printStackTrace()

            // Last resort: Check local DB one more time
            val localProfile = database.userProfileDao().getProfile(userId)?.toDomain()
            if (localProfile != null) {
                println("✅ Using cached local profile after error")
                _currentProfile = localProfile
                return Result.success(localProfile)
            }

            // If everything fails, return default profile
            val defaultProfile = createDefaultProfile(userId)
            _currentProfile = defaultProfile
            Result.success(defaultProfile)
        }
    }

    private suspend fun syncWithSupabase(userId: String) {
        val profiles = SupabaseClient.client
            .from("user_profiles")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeList<UserProfile>()

        val remoteProfile = profiles.firstOrNull()
        if (remoteProfile != null) {
            database.userProfileDao().upsertProfile(remoteProfile.toEntity())
            _currentProfile = remoteProfile
            println("🔄 Synced with Supabase")
        } else {
            // Profile exists locally but NOT in Supabase - create it
            val localProfile = _currentProfile
            if (localProfile != null) {
                println("🆕 Local profile found but not in Supabase - creating...")
                try {
                    SupabaseClient.client
                        .from("user_profiles")
                        .upsert(localProfile)
                    println("✅ Local profile synced to Supabase")
                } catch (e: Exception) {
                    println("⚠️ Failed to sync local profile to Supabase: ${e.message}")
                }
            }
        }
    }

    private suspend fun fetchFromSupabaseAndSave(userId: String): Result<UserProfile> {
        val profiles = SupabaseClient.client
            .from("user_profiles")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeList<UserProfile>()

        val profile: UserProfile
        if (profiles.isNotEmpty()) {
            profile = profiles.first()
            println("✅ Force-fetched profile from Supabase: ${profile.name}")
        } else {
            // Create default profile and save to Supabase
            profile = createDefaultProfile(userId)
            println("🆕 Creating new user profile in Supabase for: $userId")
            try {
                SupabaseClient.client
                    .from("user_profiles")
                    .upsert(profile)
                println("✅ New user profile created in Supabase")
            } catch (e: Exception) {
                println("⚠️ Failed to create profile in Supabase: ${e.message}")
            }
        }

        // Save to local database
        database.userProfileDao().upsertProfile(profile.toEntity())
        _currentProfile = profile

        println("🖼️ Profile image URL: ${profile.profileImageUrl}")

        return Result.success(profile)
    }

    /**
     * Get profile image URL for a user (from Supabase)
     * Used by Community to fetch profile images from user_profiles table
     */
    suspend fun getProfileImageUrl(userId: String): String? {
        return try {
            withContext(Dispatchers.IO) {
                val profiles = SupabaseClient.client
                    .from("user_profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeList<UserProfile>()

                profiles.firstOrNull()?.profileImageUrl
            }
        } catch (e: Exception) {
            println("❌ Error fetching profile image URL: ${e.message}")
            null
        }
    }

    suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                println("💾 Updating user profile")

                // 1. ALWAYS save to local database first (OFFLINE FIRST)
                database.userProfileDao().upsertProfile(profile.toEntity())
                _currentProfile = profile
                println("✅ Profile saved to LOCAL database")

                // 2. Try to sync to Supabase (non-blocking - don't fail if this fails)
                try {
                    SupabaseClient.client
                        .from("user_profiles")
                        .upsert(profile)
                    println("✅ Profile synced to Supabase")
                } catch (e: Exception) {
                    println("⚠️ Failed to sync to Supabase (saved locally): ${e.message}")
                    // Don't throw - local save succeeded, which is what matters
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ ERROR updating profile: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun updateLastSeen(userId: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                SupabaseClient.client
                    .from("user_profiles")
                    .update({
                        set("last_seen", System.currentTimeMillis())
                    }) {
                        filter {
                            eq("id", userId)
                        }
                    }
                println("✅ Updated last_seen for user $userId")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ ERROR updating last_seen: ${e.message}")
            Result.failure(e)
        }
    }


    suspend fun addPersonalRecord(pr: PersonalRecord): Result<Unit> {
        val profile = _currentProfile ?: return Result.failure(Exception("No profile loaded"))

        val updatedPRs = profile.personalRecords.toMutableMap()
        updatedPRs[pr.exerciseName] = pr

        val updatedProfile = profile.copy(personalRecords = updatedPRs)
        return updateProfile(updatedProfile)
    }

    suspend fun addMedicalCondition(condition: MedicalCondition): Result<Unit> {
        val profile = _currentProfile ?: return Result.failure(Exception("No profile loaded"))

        val updatedConditions = profile.medicalConditions.toMutableList()
        updatedConditions.add(condition)

        val updatedProfile = profile.copy(medicalConditions = updatedConditions)
        return updateProfile(updatedProfile)
    }

    suspend fun addInjury(injury: Injury): Result<Unit> {
        val profile = _currentProfile ?: return Result.failure(Exception("No profile loaded"))

        val updatedInjuries = profile.injuries.toMutableList()
        updatedInjuries.add(injury)

        val updatedProfile = profile.copy(injuries = updatedInjuries)
        return updateProfile(updatedProfile)
    }

    suspend fun clearLocalProfile() {
        withContext(Dispatchers.IO) {
            try {
                database.userProfileDao().deleteAllProfiles()
                _currentProfile = null
                println("🗑️ Cleared all local profiles")
            } catch (e: Exception) {
                println("❌ Failed to clear local profiles: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Upload profile image to Supabase Storage and update profile
     */
    suspend fun uploadProfileImage(userId: String, imageFile: File): Result<String> {
        return try {
            withContext(Dispatchers.IO) {
                println("📸 Uploading profile image for user: $userId")
                println("📁 Image file: ${imageFile.absolutePath}")
                println("📊 File size: ${imageFile.length()} bytes")

                // Validate file
                if (!imageFile.exists()) {
                    return@withContext Result.failure(Exception("Image file does not exist"))
                }

                if (imageFile.length() == 0L) {
                    return@withContext Result.failure(Exception("Image file is empty"))
                }

                // Generate unique filename
                val extension = imageFile.extension.ifEmpty { "jpg" }
                val fileName = "profile_$userId.$extension"
                val bucketName = "profile-images"

                println("📤 Uploading to bucket: $bucketName, filename: $fileName")

                // Read file bytes
                val imageBytes = imageFile.readBytes()
                println("✅ Read ${imageBytes.size} bytes from file")

                try {
                    // Upload to Supabase Storage
                    val storage = SupabaseClient.client.storage
                    println("🔄 Uploading to Supabase Storage...")

                    storage.from(bucketName).upload(
                        path = fileName,
                        data = imageBytes,
                        upsert = true
                    )

                    println("✅ Upload successful!")

                    // Get public URL (bucket must be set to public in Supabase)
                    // This URL never expires, unlike signed URLs
                    val publicUrl = storage.from(bucketName).publicUrl(fileName)
                    val imageUrl = "$publicUrl?t=${System.currentTimeMillis()}"
                    println("🔗 Public URL (with cache-buster): $imageUrl")

                    // Update profile with new image URL
                    val profile = _currentProfile
                    if (profile == null) {
                        println("⚠️ No profile loaded, but image uploaded successfully")
                        return@withContext Result.success(imageUrl)
                    }

                    val updatedProfile = profile.copy(profileImageUrl = imageUrl)

                    // WICHTIG: Warten bis das Profil aktualisiert wurde!
                    val updateResult = updateProfile(updatedProfile)
                    if (updateResult.isFailure) {
                        println("⚠️ Failed to update profile with new image URL: ${updateResult.exceptionOrNull()?.message}")
                    } else {
                        println("✅ Profile updated with new image URL")
                    }
                    Result.success(imageUrl)
                } catch (storageError: Exception) {
                    println("❌ STORAGE ERROR: ${storageError.message}")
                    storageError.printStackTrace()
                    return@withContext Result.failure(Exception("Failed to upload to Supabase Storage: ${storageError.message}", storageError))
                }
            }
        } catch (e: Exception) {
            println("❌ ERROR uploading profile image: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun createDefaultProfile(userId: String): UserProfile {
        return UserProfile(
            id = userId,
            name = "Champion",
            weight = null,
            height = null,
            age = null,
            trainingExperience = null,
            personalRecords = emptyMap(),
            medicalConditions = emptyList(),
            injuries = emptyList(),
            goals = emptyList(),
            preferredSports = emptyList()
        )
    }
}

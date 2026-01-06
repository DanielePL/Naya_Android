package com.example.menotracker.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profiles WHERE id = :userId")
    suspend fun getProfile(userId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE id = :userId")
    fun getProfileFlow(userId: String): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    @Upsert
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profiles WHERE id = :userId")
    suspend fun deleteProfile(userId: String)

    @Query("DELETE FROM user_profiles")
    suspend fun deleteAllProfiles()

    @Query("SELECT COUNT(*) FROM user_profiles WHERE id = :userId")
    suspend fun profileExists(userId: String): Int
}
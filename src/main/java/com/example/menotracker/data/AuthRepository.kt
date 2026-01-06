package com.example.menotracker.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

class AuthRepository(private val context: Context) {

    companion object {
        private const val TAG = "AuthRepository"
        private val GUEST_MODE_KEY = stringPreferencesKey("guest_mode")
        const val GUEST_USER_ID = "00000000-0000-0000-0000-000000000000"
    }

    private val auth: Auth = SupabaseClient.client.auth

    // ========== AUTH STATE ==========

    val isLoggedIn: Flow<Boolean> = kotlinx.coroutines.flow.combine(
        context.authDataStore.data,
        auth.sessionStatus
    ) { preferences, sessionStatus ->
        val isGuest = preferences[GUEST_MODE_KEY] == "true"
        val hasSession = sessionStatus is io.github.jan.supabase.gotrue.SessionStatus.Authenticated
        // ✅ FIX: Also verify we have actual user data, not just session status
        // This prevents race condition where session is "Authenticated" but currentUserOrNull() is null
        val hasRealUser = auth.currentUserOrNull() != null
        Log.d(TAG, "🔍 isLoggedIn check: isGuest=$isGuest, hasSession=$hasSession, hasRealUser=$hasRealUser")
        isGuest || (hasSession && hasRealUser)
    }

    val currentUserId: Flow<String?> = context.authDataStore.data.map { preferences ->
        // ✅ PRIORITY: Real authenticated session takes precedence over guest mode
        val realUserId = auth.currentUserOrNull()?.id
        val isGuest = preferences[GUEST_MODE_KEY] == "true"
        when {
            realUserId != null -> realUserId  // Real session has priority
            isGuest -> GUEST_USER_ID
            else -> null
        }
    }

    suspend fun getCurrentUserId(): String {
        // ✅ PRIORITY: Real authenticated session takes precedence over guest mode
        val realUserId = auth.currentUserOrNull()?.id
        val userEmail = auth.currentUserOrNull()?.email
        Log.d(TAG, "🔑🔑🔑 getCurrentUserId - realUserId=$realUserId, email=$userEmail")
        if (realUserId != null) return realUserId

        val isGuest = context.authDataStore.data.map { it[GUEST_MODE_KEY] == "true" }.first()
        Log.d(TAG, "🔑🔑🔑 No real user found, isGuest=$isGuest")
        return when {
            isGuest -> GUEST_USER_ID
            else -> GUEST_USER_ID  // Fallback
        }
    }

    val currentUserEmail: Flow<String?> = context.authDataStore.data.map { preferences ->
        // ✅ PRIORITY: Real authenticated session takes precedence over guest mode
        val realEmail = auth.currentUserOrNull()?.email
        val isGuest = preferences[GUEST_MODE_KEY] == "true"
        when {
            realEmail != null -> realEmail  // Real session has priority
            isGuest -> "Guest"
            else -> null
        }
    }

    fun getCurrentUser(): UserInfo? = auth.currentUserOrNull()

    // ========== EMAIL/PASSWORD SIGN UP ==========

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        return try {
            Log.d(TAG, "📧 Signing up with email: $email")

            // Sign up with explicit redirect URL for the app's deep link
            // This overrides the default Site URL in Supabase
            auth.signUpWith(Email, redirectUrl = "naya://auth/callback") {
                this.email = email
                this.password = password
            }

            Log.d(TAG, "✅ Sign up successful - email confirmation sent with app redirect")

            // Always require email confirmation - no auto-login
            Result.failure(Exception("Account created! Please check your email to confirm your account, then log in."))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sign up failed: ${e.message}")

            // Better error messages
            val friendlyMessage = when {
                e.message?.contains("already registered") == true -> "This email is already registered. Try logging in instead."
                e.message?.contains("Invalid email") == true -> "Please enter a valid email address."
                e.message?.contains("Password") == true -> "Password must be at least 6 characters."
                else -> e.message ?: "Sign up failed. Please try again."
            }

            Result.failure(Exception(friendlyMessage))
        }
    }

    // ========== EMAIL/PASSWORD LOGIN ==========

    suspend fun loginWithEmail(email: String, password: String): Result<Unit> {
        return try {
            Log.d(TAG, "📧 Logging in with email: $email")

            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            // Disable guest mode
            context.authDataStore.edit { it[GUEST_MODE_KEY] = "false" }

            Log.d(TAG, "✅ Login successful. User ID: ${auth.currentUserOrNull()?.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Login failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ========== GOOGLE SIGN-IN ==========
    // TODO: Implement Google Sign-In with proper OAuth flow
    suspend fun loginWithGoogle(idToken: String): Result<Unit> {
        return try {
            Log.d(TAG, "🔵 Google Sign-In not yet implemented")
            Result.failure(Exception("Google Sign-In not yet implemented. Please use Email/Password or Guest Mode."))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Google login failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ========== GUEST MODE ==========

    suspend fun continueAsGuest(): Result<Unit> {
        return try {
            Log.d(TAG, "👤 Continuing as guest")

            // Set guest mode flag
            context.authDataStore.edit { it[GUEST_MODE_KEY] = "true" }

            Log.d(TAG, "✅ Guest mode enabled")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Guest mode failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ========== LOGOUT ==========

    suspend fun logout(): Result<Unit> {
        return try {
            Log.d(TAG, "🚪 Logging out")

            // Sign out from Supabase
            auth.signOut()

            // Clear guest mode
            context.authDataStore.edit { it[GUEST_MODE_KEY] = "false" }

            // Clear local user profile data
            try {
                UserProfileRepository.clearLocalProfile()
                Log.d(TAG, "🗑️ Cleared local profile data")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Failed to clear local profile: ${e.message}")
            }

            Log.d(TAG, "✅ Logout successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Logout failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ========== PASSWORD RESET ==========

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            Log.d(TAG, "🔑 Sending password reset email to: $email")

            auth.resetPasswordForEmail(email)

            Log.d(TAG, "✅ Password reset email sent")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Password reset failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ========== SET SESSION FROM TOKENS (Email Verification Deep Link) ==========

    suspend fun setSessionFromTokens(accessToken: String, refreshToken: String): Result<Unit> {
        return try {
            Log.d(TAG, "🔐 Setting session from tokens (email verification)")

            // Import session using tokens
            auth.importSession(
                io.github.jan.supabase.gotrue.user.UserSession(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresIn = 3600,
                    tokenType = "bearer",
                    user = null
                )
            )

            // IMPORTANT: After importing session, we must retrieve user info
            // importSession with user=null doesn't automatically fetch user details
            Log.d(TAG, "📥 Retrieving user info after session import...")
            val userInfo = auth.retrieveUserForCurrentSession(updateSession = true)
            Log.d(TAG, "✅ User info retrieved: ${userInfo.id}, email: ${userInfo.email}")

            // Disable guest mode
            context.authDataStore.edit { it[GUEST_MODE_KEY] = "false" }

            Log.d(TAG, "✅ Session restored successfully. User ID: ${userInfo.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to restore session: ${e.message}")
            Result.failure(e)
        }
    }
}
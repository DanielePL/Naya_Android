package com.example.menotracker.data

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AdminManager - Manages admin authentication for app owners
 *
 * Admin users have special privileges:
 * - Upload workout videos
 * - Create/edit public workout templates
 * - Manage content visible to all users
 *
 * The login UI is identical for all users - admin status is determined
 * automatically based on the authenticated user's credentials.
 */
object AdminManager {

    private const val TAG = "AdminManager"

    // ========== ADMIN CREDENTIALS ==========
    // Only these users have admin access
    private val ADMIN_USERS = mapOf(
        "5f4afbeb-0b0e-46c7-9caf-0d95c57fbd93" to "kloe.borge18@gmail.com"  // Chloe - App Owner
    )

    // ========== STATE ==========
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _adminEmail = MutableStateFlow<String?>(null)
    val adminEmail: StateFlow<String?> = _adminEmail.asStateFlow()

    // ========== ADMIN CHECK ==========

    /**
     * Check if the given user credentials match an admin user.
     * Called automatically after successful login.
     */
    fun checkAdminStatus(userId: String?, userEmail: String?) {
        val isAdminUser = when {
            // Check by user ID first (more reliable)
            userId != null && ADMIN_USERS.containsKey(userId) -> {
                Log.d(TAG, "👑 Admin detected by user ID: $userId")
                true
            }
            // Fallback to email check
            userEmail != null && ADMIN_USERS.containsValue(userEmail.lowercase()) -> {
                Log.d(TAG, "👑 Admin detected by email: $userEmail")
                true
            }
            else -> {
                Log.d(TAG, "👤 Regular user: $userEmail")
                false
            }
        }

        _isAdmin.value = isAdminUser
        _adminEmail.value = if (isAdminUser) userEmail else null

        if (isAdminUser) {
            Log.i(TAG, "✅ Admin privileges granted to: $userEmail")
        }
    }

    /**
     * Clear admin status on logout
     */
    fun clearAdminStatus() {
        Log.d(TAG, "🚪 Clearing admin status")
        _isAdmin.value = false
        _adminEmail.value = null
    }

    /**
     * Check if a specific email is an admin (for UI hints)
     */
    fun isAdminEmail(email: String): Boolean {
        return ADMIN_USERS.containsValue(email.lowercase())
    }

    /**
     * Check if a specific user ID is an admin
     */
    fun isAdminUserId(userId: String): Boolean {
        return ADMIN_USERS.containsKey(userId)
    }
}

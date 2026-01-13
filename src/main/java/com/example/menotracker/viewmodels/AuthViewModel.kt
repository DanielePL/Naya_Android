package com.example.menotracker.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.data.AdminManager
import com.example.menotracker.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val authRepository = AuthRepository(application)

    // Auth State
    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val currentUserId: StateFlow<String?> = authRepository.currentUserId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val currentUserEmail: StateFlow<String?> = authRepository.currentUserEmail
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Admin State (exposed from AdminManager)
    val isAdmin: StateFlow<Boolean> = AdminManager.isAdmin

    init {
        // Check admin status for existing session on app startup
        authRepository.checkAdminStatusForCurrentUser()
    }

    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ========== EMAIL/PASSWORD SIGN UP ==========

    fun signUpWithEmail(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onNeedsEmailConfirmation: () -> Unit = {}
    ) {
        viewModelScope.launch {
            // Guard against multiple simultaneous calls
            if (_isLoading.value) {
                Log.w(TAG, "⚠️ Sign up already in progress, ignoring duplicate call")
                return@launch
            }

            _isLoading.value = true
            _errorMessage.value = null

            Log.d(TAG, "🔵 signUpWithEmail called for: $email")

            authRepository.signUpWithEmail(email, password)
                .onSuccess {
                    Log.d(TAG, "✅ Sign up successful - auto-login worked")
                    _isLoading.value = false
                    onSuccess()
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Sign up/Login failed: ${error.message}")

                    // Check if this is a successful account creation (needs email confirmation)
                    if (error.message?.contains("Account created") == true) {
                        Log.d(TAG, "📧 Account created - email confirmation required")
                        _isLoading.value = false
                        onNeedsEmailConfirmation()
                    } else {
                        _errorMessage.value = error.message ?: "Sign up failed"
                        _isLoading.value = false
                    }
                }
        }
    }

    // ========== EMAIL/PASSWORD LOGIN ==========

    fun loginWithEmail(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Guard against multiple simultaneous calls
            if (_isLoading.value) {
                Log.w(TAG, "⚠️ Login already in progress, ignoring duplicate call")
                return@launch
            }

            _isLoading.value = true
            _errorMessage.value = null

            Log.d(TAG, "🔵 loginWithEmail called for: $email")

            authRepository.loginWithEmail(email, password)
                .onSuccess {
                    Log.d(TAG, "✅ Login successful")
                    _isLoading.value = false
                    onSuccess()
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Login failed: ${error.message}")
                    _errorMessage.value = error.message ?: "Login failed"
                    _isLoading.value = false
                }
        }
    }

    // ========== GOOGLE SIGN-IN ==========

    fun loginWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            authRepository.loginWithGoogle(idToken)
                .onSuccess {
                    Log.d(TAG, "✅ Google login successful")
                    _isLoading.value = false
                    onSuccess()
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Google login failed: ${error.message}")
                    _errorMessage.value = error.message ?: "Google sign-in failed"
                    _isLoading.value = false
                }
        }
    }

    // ========== GUEST MODE ==========

    fun continueAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            authRepository.continueAsGuest()
                .onSuccess {
                    Log.d(TAG, "✅ Guest mode enabled")
                    _isLoading.value = false
                    onSuccess()
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Guest mode failed: ${error.message}")
                    _errorMessage.value = error.message ?: "Failed to continue as guest"
                    _isLoading.value = false
                }
        }
    }

    // ========== LOGOUT ==========

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            authRepository.logout()
                .onSuccess {
                    Log.d(TAG, "✅ Logout successful")
                    _isLoading.value = false
                    onSuccess()
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Logout failed: ${error.message}")
                    _errorMessage.value = error.message ?: "Logout failed"
                    _isLoading.value = false
                }
        }
    }

    // ========== PASSWORD RESET ==========

    fun resetPassword(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            authRepository.resetPassword(email)
                .onSuccess {
                    Log.d(TAG, "✅ Password reset email sent")
                    _isLoading.value = false
                    onSuccess()
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Password reset failed: ${error.message}")
                    _errorMessage.value = error.message ?: "Password reset failed"
                    _isLoading.value = false
                }
        }
    }

    // ========== CLEAR ERROR ==========

    fun clearError() {
        _errorMessage.value = null
    }
}

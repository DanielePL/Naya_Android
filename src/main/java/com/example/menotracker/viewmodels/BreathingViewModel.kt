package com.example.menotracker.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.audio.AudioManager
import com.example.menotracker.data.models.*
import com.example.menotracker.data.repository.BreathingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for Breathing Exercises
 * Manages the breathing timer state machine, audio, and session logging
 */
class BreathingViewModel : ViewModel() {
    private val TAG = "BreathingViewModel"

    // ═══════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════

    private val _uiState = MutableStateFlow(BreathingUiState())
    val uiState: StateFlow<BreathingUiState> = _uiState.asStateFlow()

    private val _sessionState = MutableStateFlow<BreathingSessionState?>(null)
    val sessionState: StateFlow<BreathingSessionState?> = _sessionState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showUpgradePrompt = MutableStateFlow(false)
    val showUpgradePrompt: StateFlow<Boolean> = _showUpgradePrompt.asStateFlow()

    // ═══════════════════════════════════════════════════════════════
    // AUDIO STATE
    // ═══════════════════════════════════════════════════════════════

    private val _selectedSound = MutableStateFlow<AmbientSound?>(null)
    val selectedSound: StateFlow<AmbientSound?> = _selectedSound.asStateFlow()

    private val _soundVolume = MutableStateFlow(0.6f)
    val soundVolume: StateFlow<Float> = _soundVolume.asStateFlow()

    private val _chimeEnabled = MutableStateFlow(true)
    val chimeEnabled: StateFlow<Boolean> = _chimeEnabled.asStateFlow()

    private val _selectedChime = MutableStateFlow(ChimeSound.BOWL)
    val selectedChime: StateFlow<ChimeSound> = _selectedChime.asStateFlow()

    // Timer job
    private var timerJob: Job? = null

    // Session tracking
    private var sessionStartTime: Long = 0
    private var moodBefore: Int? = null
    private var currentUserId: String? = null

    // ═══════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════

    fun initialize(userId: String) {
        currentUserId = userId
        loadData()
    }

    private fun loadData() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                BreathingRepository.getRecentSessions(userId)
                BreathingRepository.getTotalStats(userId)
                updateUiState()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading data", e)
                _error.value = "Failed to load breathing data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateUiState() {
        val availableExercises = BreathingRepository.getAvailableExercises()
        val lockedExercises = BreathingRepository.getLockedExercises()
        val stats = BreathingRepository.totalStats.value
        val recentSessions = BreathingRepository.recentSessions.value

        _uiState.value = BreathingUiState(
            availableExercises = availableExercises,
            lockedExercises = lockedExercises,
            totalStats = stats,
            recentSessions = recentSessions.take(5),
            hasFullAccess = lockedExercises.isEmpty()
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // EXERCISE SELECTION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Start a breathing exercise
     */
    fun selectExercise(exerciseType: BreathingExerciseType) {
        if (!BreathingRepository.canAccessExercise(exerciseType)) {
            _showUpgradePrompt.value = true
            return
        }

        // Create initial session state
        _sessionState.value = exerciseType.createInitialState()
        sessionStartTime = 0
        moodBefore = null
    }

    fun dismissUpgradePrompt() {
        _showUpgradePrompt.value = false
    }

    // ═══════════════════════════════════════════════════════════════
    // SESSION CONTROL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Set mood before starting
     */
    fun setMoodBefore(mood: Int) {
        moodBefore = mood.coerceIn(1, 5)
    }

    // ═══════════════════════════════════════════════════════════════
    // AUDIO CONTROL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Set background sound for session
     */
    fun setSelectedSound(sound: AmbientSound?) {
        _selectedSound.value = sound
    }

    /**
     * Set sound volume
     */
    fun setSoundVolume(volume: Float) {
        _soundVolume.value = volume.coerceIn(0f, 1f)
        AudioManager.setAmbientVolume(_soundVolume.value)
    }

    /**
     * Enable/disable session chimes
     */
    fun setChimeEnabled(enabled: Boolean) {
        _chimeEnabled.value = enabled
    }

    /**
     * Set chime sound type
     */
    fun setSelectedChime(chime: ChimeSound) {
        _selectedChime.value = chime
    }

    // ═══════════════════════════════════════════════════════════════
    // SESSION CONTROL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Start or resume the breathing session
     */
    fun startSession() {
        val state = _sessionState.value ?: return

        if (sessionStartTime == 0L) {
            sessionStartTime = System.currentTimeMillis()

            // Play start chime if enabled
            if (_chimeEnabled.value) {
                AudioManager.playChime(_selectedChime.value)
            }

            // Start background sound if selected
            _selectedSound.value?.let { sound ->
                AudioManager.playAmbient(sound, _soundVolume.value)
            }
        } else {
            // Resuming - just resume audio
            AudioManager.resumeAll()
        }

        _sessionState.value = state.copy(
            isRunning = true,
            isPaused = false
        )

        startTimer()
    }

    /**
     * Pause the session
     */
    fun pauseSession() {
        timerJob?.cancel()
        AudioManager.pauseAll()

        val state = _sessionState.value ?: return

        _sessionState.value = state.copy(
            isRunning = false,
            isPaused = true
        )
    }

    /**
     * Resume paused session
     */
    fun resumeSession() {
        startSession()
    }

    /**
     * Stop and cancel the session
     */
    fun cancelSession() {
        timerJob?.cancel()
        AudioManager.stopAll()
        _sessionState.value = null
        sessionStartTime = 0
        moodBefore = null
    }

    /**
     * Complete the session and log it
     */
    fun completeSession(moodAfter: Int? = null) {
        val state = _sessionState.value ?: return
        val userId = currentUserId ?: return

        timerJob?.cancel()

        // Play end chime if enabled
        if (_chimeEnabled.value) {
            AudioManager.playChime(_selectedChime.value)
        }

        // Fade out background audio
        AudioManager.fadeOutAll(durationMs = 2000L)

        val durationSeconds = if (sessionStartTime > 0) {
            ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
        } else {
            state.exerciseType.totalDurationSeconds
        }

        viewModelScope.launch {
            _isLoading.value = true

            BreathingRepository.logSession(
                userId = userId,
                exerciseType = state.exerciseType,
                durationSeconds = durationSeconds,
                cyclesCompleted = state.currentCycle,
                moodBefore = moodBefore,
                moodAfter = moodAfter?.coerceIn(1, 5)
            ).onSuccess { session ->
                Log.d(TAG, "Session logged: ${session.id}")
                _sessionState.value = null
                sessionStartTime = 0
                moodBefore = null
                loadData() // Refresh stats
            }.onFailure { e ->
                Log.e(TAG, "Failed to log session", e)
                _error.value = "Failed to save session"
            }

            _isLoading.value = false
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TIMER LOGIC
    // ═══════════════════════════════════════════════════════════════

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(100) // 100ms tick for smooth animation
                tick()
            }
        }
    }

    private fun tick() {
        val state = _sessionState.value ?: return
        if (!state.isRunning) return

        val pattern = state.exerciseType.pattern

        // Calculate progress within current phase
        val phaseDuration = when (state.currentPhase) {
            BreathingPhase.INHALE -> pattern.inhaleSeconds
            BreathingPhase.HOLD_IN -> pattern.holdInSeconds
            BreathingPhase.EXHALE -> pattern.exhaleSeconds
            BreathingPhase.HOLD_OUT -> pattern.holdOutSeconds
            BreathingPhase.REST -> 1
        }

        val newSecondsRemaining = state.phaseSecondsRemaining - 0.1f
        val newProgress = calculateProgress(state.currentPhase, phaseDuration, newSecondsRemaining)

        if (newSecondsRemaining <= 0) {
            // Move to next phase
            moveToNextPhase()
        } else {
            // Update countdown
            _sessionState.value = state.copy(
                phaseSecondsRemaining = newSecondsRemaining.toInt().coerceAtLeast(0),
                totalSecondsRemaining = state.totalSecondsRemaining - 1,
                progress = newProgress
            )
        }
    }

    private fun calculateProgress(
        phase: BreathingPhase,
        phaseDuration: Int,
        remaining: Float
    ): Float {
        if (phaseDuration == 0) return 0.5f

        val elapsed = phaseDuration - remaining
        val phaseProgress = (elapsed / phaseDuration).coerceIn(0f, 1f)

        return when (phase) {
            BreathingPhase.INHALE -> 0.6f + (0.4f * phaseProgress) // 0.6 -> 1.0
            BreathingPhase.HOLD_IN -> 1.0f // Stay at max
            BreathingPhase.EXHALE -> 1.0f - (0.4f * phaseProgress) // 1.0 -> 0.6
            BreathingPhase.HOLD_OUT -> 0.6f // Stay at min
            BreathingPhase.REST -> 0.6f
        }
    }

    private fun moveToNextPhase() {
        val state = _sessionState.value ?: return
        val pattern = state.exerciseType.pattern

        val (nextPhase, nextCycle, isComplete) = when (state.currentPhase) {
            BreathingPhase.INHALE -> {
                if (pattern.holdInSeconds > 0) {
                    Triple(BreathingPhase.HOLD_IN, state.currentCycle, false)
                } else {
                    Triple(BreathingPhase.EXHALE, state.currentCycle, false)
                }
            }
            BreathingPhase.HOLD_IN -> {
                Triple(BreathingPhase.EXHALE, state.currentCycle, false)
            }
            BreathingPhase.EXHALE -> {
                if (pattern.holdOutSeconds > 0) {
                    Triple(BreathingPhase.HOLD_OUT, state.currentCycle, false)
                } else {
                    // End of cycle
                    if (state.currentCycle >= state.totalCycles) {
                        Triple(BreathingPhase.REST, state.currentCycle, true)
                    } else {
                        Triple(BreathingPhase.INHALE, state.currentCycle + 1, false)
                    }
                }
            }
            BreathingPhase.HOLD_OUT -> {
                // End of cycle
                if (state.currentCycle >= state.totalCycles) {
                    Triple(BreathingPhase.REST, state.currentCycle, true)
                } else {
                    Triple(BreathingPhase.INHALE, state.currentCycle + 1, false)
                }
            }
            BreathingPhase.REST -> {
                Triple(BreathingPhase.REST, state.currentCycle, true)
            }
        }

        if (isComplete) {
            // Session complete - auto-complete
            timerJob?.cancel()
            _sessionState.value = state.copy(
                currentPhase = BreathingPhase.REST,
                isRunning = false,
                phaseSecondsRemaining = 0,
                totalSecondsRemaining = 0,
                progress = 0.6f
            )
            return
        }

        // Move to next phase
        val nextPhaseDuration = when (nextPhase) {
            BreathingPhase.INHALE -> pattern.inhaleSeconds
            BreathingPhase.HOLD_IN -> pattern.holdInSeconds
            BreathingPhase.EXHALE -> pattern.exhaleSeconds
            BreathingPhase.HOLD_OUT -> pattern.holdOutSeconds
            BreathingPhase.REST -> 0
        }

        _sessionState.value = state.copy(
            currentPhase = nextPhase,
            currentCycle = nextCycle,
            phaseSecondsRemaining = nextPhaseDuration,
            progress = when (nextPhase) {
                BreathingPhase.INHALE -> 0.6f
                BreathingPhase.HOLD_IN -> 1.0f
                BreathingPhase.EXHALE -> 1.0f
                BreathingPhase.HOLD_OUT -> 0.6f
                BreathingPhase.REST -> 0.6f
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════

    fun clearError() {
        _error.value = null
    }

    fun refresh() {
        loadData()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        AudioManager.stopAll()
        BreathingRepository.clearState()
    }
}

/**
 * UI State for Breathing Exercise list
 */
data class BreathingUiState(
    val availableExercises: List<BreathingExerciseType> = emptyList(),
    val lockedExercises: List<BreathingExerciseType> = emptyList(),
    val totalStats: BreathingStats? = null,
    val recentSessions: List<BreathingSession> = emptyList(),
    val hasFullAccess: Boolean = false
)

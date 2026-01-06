package com.example.menotracker.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.audio.AudioManager
import com.example.menotracker.data.models.*
import com.example.menotracker.data.repository.MeditationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for Meditation Sessions
 * Manages meditation timer, audio, and session logging
 */
class MeditationViewModel : ViewModel() {
    private val TAG = "MeditationViewModel"

    // ═══════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════

    private val _uiState = MutableStateFlow(MeditationUiState())
    val uiState: StateFlow<MeditationUiState> = _uiState.asStateFlow()

    private val _sessionState = MutableStateFlow<MeditationSessionState?>(null)
    val sessionState: StateFlow<MeditationSessionState?> = _sessionState.asStateFlow()

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

    private val _selectedMusic = MutableStateFlow<BackgroundMusic?>(null)
    val selectedMusic: StateFlow<BackgroundMusic?> = _selectedMusic.asStateFlow()

    private val _soundVolume = MutableStateFlow(0.6f)
    val soundVolume: StateFlow<Float> = _soundVolume.asStateFlow()

    private val _musicVolume = MutableStateFlow(0.4f)
    val musicVolume: StateFlow<Float> = _musicVolume.asStateFlow()

    private val _chimeEnabled = MutableStateFlow(true)
    val chimeEnabled: StateFlow<Boolean> = _chimeEnabled.asStateFlow()

    private val _selectedChime = MutableStateFlow(ChimeSound.BOWL)
    val selectedChime: StateFlow<ChimeSound> = _selectedChime.asStateFlow()

    // Soundscape layers (for mixer)
    private val _soundscapeLayers = MutableStateFlow<List<SoundLayer>>(emptyList())
    val soundscapeLayers: StateFlow<List<SoundLayer>> = _soundscapeLayers.asStateFlow()

    // Timer job
    private var timerJob: Job? = null

    // Session tracking
    private var sessionStartTime: Long = 0
    private var moodBefore: Int? = null
    private var currentUserId: String? = null

    // Selected duration
    private val _selectedDuration = MutableStateFlow(10)
    val selectedDuration: StateFlow<Int> = _selectedDuration.asStateFlow()

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
                MeditationRepository.getRecentSessions(userId)
                MeditationRepository.getTotalStats(userId)
                updateUiState()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading data", e)
                _error.value = "Failed to load meditation data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateUiState() {
        val availableMeditations = MeditationRepository.getAvailableMeditations()
        val lockedMeditations = MeditationRepository.getLockedMeditations()
        val stats = MeditationRepository.totalStats.value
        val recentSessions = MeditationRepository.recentSessions.value

        _uiState.value = MeditationUiState(
            availableMeditations = availableMeditations,
            lockedMeditations = lockedMeditations,
            totalStats = stats,
            recentSessions = recentSessions.take(5),
            hasFullAccess = lockedMeditations.isEmpty()
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // MEDITATION SELECTION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Select a meditation to start
     */
    fun selectMeditation(meditationType: MeditationType) {
        if (!MeditationRepository.canAccessMeditation(meditationType)) {
            _showUpgradePrompt.value = true
            return
        }

        // Set default duration
        _selectedDuration.value = meditationType.defaultDurationMinutes

        // Create initial session state
        _sessionState.value = meditationType.createSessionState(_selectedDuration.value)
        sessionStartTime = 0
        moodBefore = null
    }

    /**
     * Set session duration (in minutes)
     */
    fun setDuration(minutes: Int) {
        val state = _sessionState.value ?: return
        if (state.meditationType.availableDurations.contains(minutes)) {
            _selectedDuration.value = minutes
            _sessionState.value = state.meditationType.createSessionState(minutes)
        }
    }

    fun dismissUpgradePrompt() {
        _showUpgradePrompt.value = false
    }

    // ═══════════════════════════════════════════════════════════════
    // AUDIO CONTROL
    // ═══════════════════════════════════════════════════════════════

    fun setSelectedSound(sound: AmbientSound?) {
        _selectedSound.value = sound
    }

    fun setSelectedMusic(music: BackgroundMusic?) {
        _selectedMusic.value = music
    }

    fun setSoundVolume(volume: Float) {
        _soundVolume.value = volume.coerceIn(0f, 1f)
        AudioManager.setAmbientVolume(_soundVolume.value)
    }

    fun setMusicVolume(volume: Float) {
        _musicVolume.value = volume.coerceIn(0f, 1f)
        AudioManager.setMusicVolume(_musicVolume.value)
    }

    fun setChimeEnabled(enabled: Boolean) {
        _chimeEnabled.value = enabled
    }

    fun setSelectedChime(chime: ChimeSound) {
        _selectedChime.value = chime
    }

    // Soundscape methods
    fun addSoundscapeLayer(sound: AmbientSound, volume: Float = 0.5f) {
        if (AudioManager.addSoundscapeLayer(sound, volume)) {
            _soundscapeLayers.value = AudioManager.soundscapeLayers.value
        }
    }

    fun removeSoundscapeLayer(sound: AmbientSound) {
        AudioManager.removeSoundscapeLayer(sound)
        _soundscapeLayers.value = AudioManager.soundscapeLayers.value
    }

    fun setSoundscapeLayerVolume(sound: AmbientSound, volume: Float) {
        AudioManager.setSoundscapeLayerVolume(sound, volume)
        _soundscapeLayers.value = AudioManager.soundscapeLayers.value
    }

    fun clearSoundscape() {
        AudioManager.clearSoundscape()
        _soundscapeLayers.value = emptyList()
    }

    fun loadSoundscapePreset(preset: SoundscapePreset) {
        AudioManager.loadSoundscapePreset(preset.layers)
        _soundscapeLayers.value = AudioManager.soundscapeLayers.value
    }

    // ═══════════════════════════════════════════════════════════════
    // SESSION CONTROL
    // ═══════════════════════════════════════════════════════════════

    fun setMoodBefore(mood: Int) {
        moodBefore = mood.coerceIn(1, 5)
    }

    /**
     * Start or resume the meditation session
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

            // Start background music if selected
            _selectedMusic.value?.let { music ->
                AudioManager.playMusic(music, _musicVolume.value)
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
            state.totalDurationSeconds
        }

        viewModelScope.launch {
            _isLoading.value = true

            // Collect sounds used
            val soundsUsed = listOfNotNull(_selectedSound.value) + _soundscapeLayers.value.map { it.sound }

            MeditationRepository.logSession(
                userId = userId,
                meditationType = state.meditationType,
                durationSeconds = durationSeconds,
                soundsUsed = soundsUsed.distinct(),
                musicUsed = _selectedMusic.value,
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
                delay(1000) // 1 second tick
                tick()
            }
        }
    }

    private fun tick() {
        val state = _sessionState.value ?: return
        if (!state.isRunning) return

        val newRemaining = state.remainingSeconds - 1
        val newProgress = 1f - (newRemaining.toFloat() / state.totalDurationSeconds)

        // Determine phase based on progress
        val newPhase = when {
            newProgress < 0.1f -> MeditationPhase.INTRO
            newProgress > 0.9f -> MeditationPhase.CLOSING
            newRemaining <= 0 -> MeditationPhase.COMPLETE
            else -> MeditationPhase.MAIN
        }

        if (newRemaining <= 0) {
            // Session complete
            timerJob?.cancel()
            _sessionState.value = state.copy(
                remainingSeconds = 0,
                progress = 1f,
                isRunning = false,
                currentPhase = MeditationPhase.COMPLETE
            )
        } else {
            _sessionState.value = state.copy(
                remainingSeconds = newRemaining,
                progress = newProgress,
                currentPhase = newPhase
            )
        }
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
        MeditationRepository.clearState()
    }
}

/**
 * UI State for Meditation list
 */
data class MeditationUiState(
    val availableMeditations: List<MeditationType> = emptyList(),
    val lockedMeditations: List<MeditationType> = emptyList(),
    val totalStats: MeditationStats? = null,
    val recentSessions: List<MeditationSession> = emptyList(),
    val hasFullAccess: Boolean = false
)

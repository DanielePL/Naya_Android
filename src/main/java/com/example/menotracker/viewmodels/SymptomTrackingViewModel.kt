package com.example.menotracker.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.data.models.*
import com.example.menotracker.data.repository.SymptomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel für Symptom-Tracking
 */
class SymptomTrackingViewModel : ViewModel() {
    private val TAG = "SymptomTrackingVM"

    // ═══════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════

    private val _uiState = MutableStateFlow(SymptomUiState())
    val uiState: StateFlow<SymptomUiState> = _uiState.asStateFlow()

    private val _todaySymptoms = MutableStateFlow<List<SymptomLog>>(emptyList())
    val todaySymptoms: StateFlow<List<SymptomLog>> = _todaySymptoms.asStateFlow()

    private val _weeklyStats = MutableStateFlow<Map<MenopauseSymptomType, SymptomStats>>(emptyMap())
    val weeklyStats: StateFlow<Map<MenopauseSymptomType, SymptomStats>> = _weeklyStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Current user ID (set from AuthViewModel)
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
                // Load today's symptoms
                SymptomRepository.getTodaySymptoms(userId).onSuccess { symptoms ->
                    _todaySymptoms.value = symptoms
                    updateUiState()
                }

                // Load weekly stats
                SymptomRepository.getWeeklyStats(userId).onSuccess { stats ->
                    _weeklyStats.value = stats
                    updateUiState()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading data", e)
                _error.value = "Fehler beim Laden der Daten"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateUiState() {
        val symptoms = _todaySymptoms.value
        val stats = _weeklyStats.value

        _uiState.value = SymptomUiState(
            todaySymptomCount = symptoms.size,
            todayAverageIntensity = if (symptoms.isEmpty()) 0f
                else symptoms.map { it.intensity }.average().toFloat(),
            mostFrequentSymptom = stats.maxByOrNull { it.value.occurrenceCount }?.key,
            weeklySymptomCount = stats.values.sumOf { it.occurrenceCount },
            symptomsByCategory = groupSymptomsByCategory(stats)
        )
    }

    private fun groupSymptomsByCategory(
        stats: Map<MenopauseSymptomType, SymptomStats>
    ): Map<SymptomCategory, List<SymptomStats>> {
        return stats.values
            .groupBy { it.symptomType.category }
            .mapValues { it.value.sortedByDescending { s -> s.occurrenceCount } }
    }

    // ═══════════════════════════════════════════════════════════════
    // LOG SYMPTOMS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Neues Symptom loggen
     */
    fun logSymptom(
        symptomType: MenopauseSymptomType,
        intensity: Int,
        durationMinutes: Int? = null,
        triggers: List<String>? = null,
        notes: String? = null
    ) {
        val userId = currentUserId ?: run {
            _error.value = "Nicht angemeldet"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            SymptomRepository.logSymptom(
                userId = userId,
                symptomType = symptomType,
                intensity = intensity,
                durationMinutes = durationMinutes,
                triggers = triggers,
                notes = notes
            ).onSuccess { log ->
                Log.d(TAG, "Symptom logged: ${log.symptomType}")
                _todaySymptoms.value = _todaySymptoms.value + log
                updateUiState()
            }.onFailure { e ->
                Log.e(TAG, "Failed to log symptom", e)
                _error.value = "Symptom konnte nicht gespeichert werden"
            }

            _isLoading.value = false
        }
    }

    /**
     * Schnelles Loggen (nur Typ und Intensität)
     */
    fun quickLog(symptomType: MenopauseSymptomType, intensity: Int) {
        logSymptom(symptomType, intensity)
    }

    // ═══════════════════════════════════════════════════════════════
    // DELETE / UPDATE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Symptom löschen
     */
    fun deleteSymptom(symptomId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            SymptomRepository.deleteSymptom(userId, symptomId).onSuccess {
                _todaySymptoms.value = _todaySymptoms.value.filter { it.id != symptomId }
                updateUiState()
            }.onFailure { e ->
                _error.value = "Konnte nicht gelöscht werden"
            }
        }
    }

    /**
     * Intensität aktualisieren
     */
    fun updateIntensity(symptomId: String, newIntensity: Int) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            SymptomRepository.updateIntensity(userId, symptomId, newIntensity).onSuccess { updated ->
                _todaySymptoms.value = _todaySymptoms.value.map {
                    if (it.id == symptomId) updated else it
                }
                updateUiState()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // REFRESH
    // ═══════════════════════════════════════════════════════════════

    fun refresh() {
        loadData()
    }

    fun clearError() {
        _error.value = null
    }

    // ═══════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════

    override fun onCleared() {
        super.onCleared()
        SymptomRepository.clearState()
    }
}

/**
 * UI State für Symptom-Tracking Screen
 */
data class SymptomUiState(
    val todaySymptomCount: Int = 0,
    val todayAverageIntensity: Float = 0f,
    val mostFrequentSymptom: MenopauseSymptomType? = null,
    val weeklySymptomCount: Int = 0,
    val symptomsByCategory: Map<SymptomCategory, List<SymptomStats>> = emptyMap()
)

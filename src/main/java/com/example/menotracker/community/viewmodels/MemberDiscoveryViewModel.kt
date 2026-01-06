package com.example.menotracker.community.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menotracker.community.data.MemberDiscoveryRepository
import com.example.menotracker.community.data.models.*
import com.example.menotracker.community.util.CommunityFeatureFlag
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Member Discovery functionality.
 * Handles member search, filtering, and suggestions.
 */
class MemberDiscoveryViewModel : ViewModel() {
    companion object {
        private const val TAG = "MemberDiscoveryVM"
        private const val PAGE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 300L
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════

    private val _state = MutableStateFlow(MemberDiscoveryState())
    val state: StateFlow<MemberDiscoveryState> = _state.asStateFlow()

    private var currentUserId: String? = null
    private var currentOffset = 0
    private var searchJob: Job? = null

    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════

    fun initialize(userId: String) {
        if (currentUserId == userId) return
        currentUserId = userId
        loadSuggestedMembers()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SUGGESTED MEMBERS (for HomeScreen)
    // ═══════════════════════════════════════════════════════════════════════

    fun loadSuggestedMembers() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            MemberDiscoveryRepository.getSuggestedMembers(userId)
                .onSuccess { suggestions ->
                    _state.value = _state.value.copy(
                        suggestedMembers = suggestions,
                        isLoading = false,
                        error = null
                    )
                    if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                        Log.d(TAG, "Loaded ${suggestions.size} suggested members")
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to load suggestions: ${error.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SEARCH
    // ═══════════════════════════════════════════════════════════════════════

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(
            filters = _state.value.filters.copy(searchQuery = query)
        )

        // Debounce search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            performSearch(refresh = true)
        }
    }

    fun updateFilters(
        sportFilter: String? = _state.value.filters.sportFilter,
        experienceLevel: ExperienceLevelFilter? = _state.value.filters.experienceLevel,
        genderFilter: GenderFilter? = _state.value.filters.genderFilter
    ) {
        _state.value = _state.value.copy(
            filters = _state.value.filters.copy(
                sportFilter = sportFilter,
                experienceLevel = experienceLevel,
                genderFilter = genderFilter
            )
        )
        performSearch(refresh = true)
    }

    fun clearFilters() {
        _state.value = _state.value.copy(
            filters = MemberSearchFilters(),
            searchResults = emptyList()
        )
    }

    private fun performSearch(refresh: Boolean = false) {
        val userId = currentUserId ?: return

        // Only search if there's a query or filters
        if (_state.value.filters.searchQuery.isBlank() && !_state.value.filters.hasActiveFilters()) {
            _state.value = _state.value.copy(
                searchResults = emptyList(),
                isSearching = false
            )
            return
        }

        if (refresh) {
            currentOffset = 0
            _state.value = _state.value.copy(isSearching = true, error = null)
        } else {
            if (_state.value.isLoadingMore || !_state.value.hasMore) return
            _state.value = _state.value.copy(isLoadingMore = true)
        }

        viewModelScope.launch {
            MemberDiscoveryRepository.searchMembers(
                currentUserId = userId,
                filters = _state.value.filters,
                limit = PAGE_SIZE,
                offset = currentOffset
            ).onSuccess { results ->
                val allResults = if (refresh) results
                else _state.value.searchResults + results
                currentOffset = allResults.size

                _state.value = _state.value.copy(
                    searchResults = allResults,
                    isSearching = false,
                    isLoadingMore = false,
                    hasMore = results.size >= PAGE_SIZE,
                    error = null
                )

                if (CommunityFeatureFlag.VERBOSE_LOGGING) {
                    Log.d(TAG, "Search returned ${results.size} members, total: ${allResults.size}")
                }
            }.onFailure { error ->
                Log.e(TAG, "Search failed: ${error.message}")
                _state.value = _state.value.copy(
                    isSearching = false,
                    isLoadingMore = false,
                    error = error.message
                )
            }
        }
    }

    fun loadMore() {
        performSearch(refresh = false)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FOLLOW ACTIONS
    // ═══════════════════════════════════════════════════════════════════════

    fun toggleFollow(targetUserId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            // Find current state
            val searchResult = _state.value.searchResults.find { it.userId == targetUserId }
            val suggestion = _state.value.suggestedMembers.find { it.userId == targetUserId }
            val isCurrentlyFollowing = searchResult?.isFollowing ?: suggestion?.isFollowing ?: false

            // Optimistic update
            updateFollowState(targetUserId, !isCurrentlyFollowing)

            // API call
            val result = if (isCurrentlyFollowing) {
                MemberDiscoveryRepository.quickUnfollow(userId, targetUserId)
            } else {
                MemberDiscoveryRepository.quickFollow(userId, targetUserId)
            }

            result.onFailure {
                // Revert on failure
                updateFollowState(targetUserId, isCurrentlyFollowing)
                Log.e(TAG, "Follow toggle failed: ${it.message}")
            }
        }
    }

    private fun updateFollowState(targetUserId: String, isFollowing: Boolean) {
        _state.value = _state.value.copy(
            searchResults = _state.value.searchResults.map { result ->
                if (result.userId == targetUserId)
                    result.copy(isFollowing = isFollowing)
                else result
            },
            suggestedMembers = _state.value.suggestedMembers.map { suggestion ->
                if (suggestion.userId == targetUserId)
                    suggestion.copy(isFollowing = isFollowing)
                else suggestion
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REFRESH
    // ═══════════════════════════════════════════════════════════════════════

    fun refresh() {
        loadSuggestedMembers()
        if (_state.value.filters.searchQuery.isNotBlank() || _state.value.filters.hasActiveFilters()) {
            performSearch(refresh = true)
        }
    }
}

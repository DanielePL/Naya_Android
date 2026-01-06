package com.example.menotracker.community.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.community.components.MemberResultCard
import com.example.menotracker.community.data.models.*
import com.example.menotracker.community.viewmodels.MemberDiscoveryViewModel
import com.example.menotracker.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════
// MEMBER DISCOVERY SCREEN
// Full search screen with filters
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDiscoveryScreen(
    currentUserId: String,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: MemberDiscoveryViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    var showFiltersSheet by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        viewModel.initialize(currentUserId)
    }

    // Infinite scroll detection
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastIndex ->
            if (lastIndex != null &&
                lastIndex >= state.searchResults.size - 3 &&
                state.hasMore &&
                !state.isLoadingMore &&
                state.searchResults.isNotEmpty()) {
                viewModel.loadMore()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NayaBackground)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Find Members",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFiltersSheet = true }) {
                            BadgedBox(
                                badge = {
                                    if (state.filters.hasActiveFilters()) {
                                        Badge(
                                            containerColor = NayaPrimary
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filters"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = state.filters.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = {
                        Text(
                            text = "Search by name...",
                            fontFamily = Poppins,
                            color = NayaTextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NayaTextSecondary
                        )
                    },
                    trailingIcon = {
                        if (state.filters.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = NayaTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NayaPrimary,
                        unfocusedBorderColor = NayaGlass,
                        focusedContainerColor = NayaGlass.copy(alpha = 0.3f),
                        unfocusedContainerColor = NayaGlass.copy(alpha = 0.3f)
                    )
                )

                // Active filters chips
                if (state.filters.hasActiveFilters()) {
                    ActiveFiltersRow(
                        filters = state.filters,
                        onClearAll = { viewModel.clearFilters() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Results
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Suggestions section (when not searching)
                    if (state.filters.searchQuery.isBlank() &&
                        !state.filters.hasActiveFilters() &&
                        state.suggestedMembers.isNotEmpty()) {

                        item {
                            Text(
                                text = "Suggested for you",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = NayaTextPrimary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(
                            items = state.suggestedMembers,
                            key = { "suggestion_${it.userId}" }
                        ) { member ->
                            MemberResultCard(
                                name = member.name,
                                avatarUrl = member.profileImageUrl,
                                primarySport = member.primarySport,
                                experienceLevel = member.experienceLevel,
                                followersCount = member.followersCount,
                                isFollowing = member.isFollowing,
                                suggestionReason = member.suggestionReason,
                                onClick = { onNavigateToProfile(member.userId) },
                                onFollowClick = { viewModel.toggleFollow(member.userId) }
                            )
                        }
                    }

                    // Search results
                    if (state.searchResults.isNotEmpty()) {
                        item {
                            Text(
                                text = "Search Results",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = NayaTextPrimary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(
                            items = state.searchResults,
                            key = { "result_${it.userId}" }
                        ) { member ->
                            MemberResultCard(
                                name = member.name,
                                avatarUrl = member.profileImageUrl,
                                primarySport = member.primarySport,
                                experienceLevel = member.experienceLevel,
                                followersCount = member.followersCount,
                                isFollowing = member.isFollowing,
                                commonSportsCount = member.commonSportsCount,
                                onClick = { onNavigateToProfile(member.userId) },
                                onFollowClick = { viewModel.toggleFollow(member.userId) }
                            )
                        }
                    }

                    // Loading indicator
                    if (state.isSearching || state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = NayaPrimary
                                )
                            }
                        }
                    }

                    // Empty state
                    if (!state.isSearching &&
                        state.searchResults.isEmpty() &&
                        (state.filters.searchQuery.isNotBlank() || state.filters.hasActiveFilters())) {
                        item {
                            EmptySearchState(
                                query = state.filters.searchQuery,
                                onClearFilters = { viewModel.clearFilters() }
                            )
                        }
                    }

                    // Initial empty state (no suggestions, no search)
                    if (!state.isLoading &&
                        !state.isSearching &&
                        state.suggestedMembers.isEmpty() &&
                        state.searchResults.isEmpty() &&
                        state.filters.searchQuery.isBlank()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = NayaTextSecondary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Search for members",
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = NayaTextPrimary
                                    )
                                    Text(
                                        text = "Find members by name or experience level",
                                        fontFamily = Poppins,
                                        fontSize = 14.sp,
                                        color = NayaTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Filters bottom sheet
    if (showFiltersSheet) {
        MemberFiltersSheet(
            currentFilters = state.filters,
            onApplyFilters = { sport, level, gender ->
                viewModel.updateFilters(
                    sportFilter = sport,
                    experienceLevel = level,
                    genderFilter = gender
                )
                showFiltersSheet = false
            },
            onDismiss = { showFiltersSheet = false }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ACTIVE FILTERS ROW
// Shows currently applied filters as chips
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ActiveFiltersRow(
    filters: MemberSearchFilters,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.sportFilter?.let { sport ->
            FilterChip(
                selected = true,
                onClick = { },
                label = { Text(sport, fontFamily = Poppins, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NayaPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = NayaPrimary
                )
            )
        }

        filters.experienceLevel?.takeIf { it != ExperienceLevelFilter.ALL }?.let { level ->
            FilterChip(
                selected = true,
                onClick = { },
                label = { Text(level.displayName, fontFamily = Poppins, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NayaPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = NayaPrimary
                )
            )
        }

        filters.genderFilter?.takeIf { it != GenderFilter.ALL }?.let { gender ->
            FilterChip(
                selected = true,
                onClick = { },
                label = { Text(gender.displayName, fontFamily = Poppins, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NayaPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = NayaPrimary
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = onClearAll) {
            Text(
                text = "Clear all",
                fontFamily = Poppins,
                fontSize = 12.sp,
                color = NayaTextSecondary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// EMPTY SEARCH STATE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun EmptySearchState(
    query: String,
    onClearFilters: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                tint = NayaTextSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No members found",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = NayaTextPrimary
            )
            if (query.isNotBlank()) {
                Text(
                    text = "No results for \"$query\"",
                    fontFamily = Poppins,
                    fontSize = 14.sp,
                    color = NayaTextSecondary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onClearFilters) {
                Text(
                    text = "Clear filters",
                    fontFamily = Poppins,
                    color = NayaPrimary
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FILTERS BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberFiltersSheet(
    currentFilters: MemberSearchFilters,
    onApplyFilters: (String?, ExperienceLevelFilter?, GenderFilter?) -> Unit,
    onDismiss: () -> Unit
) {
    var sportFilter by remember { mutableStateOf(currentFilters.sportFilter) }
    var experienceLevel by remember { mutableStateOf(currentFilters.experienceLevel ?: ExperienceLevelFilter.ALL) }
    var genderFilter by remember { mutableStateOf(currentFilters.genderFilter ?: GenderFilter.ALL) }

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NayaSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Filter Members",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = NayaTextPrimary
            )

            // Sport filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Activity",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = NayaTextSecondary
                )
                OutlinedTextField(
                    value = sportFilter ?: "",
                    onValueChange = { sportFilter = it.ifBlank { null } },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Enter activity name", fontFamily = Poppins)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NayaPrimary,
                        unfocusedBorderColor = NayaGlass
                    )
                )
            }

            // Experience level filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Experience Level",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = NayaTextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExperienceLevelFilter.entries.take(3).forEach { level ->
                        FilterChip(
                            selected = experienceLevel == level,
                            onClick = { experienceLevel = level },
                            label = {
                                Text(
                                    text = level.displayName,
                                    fontFamily = Poppins,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NayaPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExperienceLevelFilter.entries.drop(3).forEach { level ->
                        FilterChip(
                            selected = experienceLevel == level,
                            onClick = { experienceLevel = level },
                            label = {
                                Text(
                                    text = level.displayName,
                                    fontFamily = Poppins,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NayaPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Gender filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Gender",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = NayaTextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GenderFilter.entries.forEach { gender ->
                        FilterChip(
                            selected = genderFilter == gender,
                            onClick = { genderFilter = gender },
                            label = {
                                Text(
                                    text = gender.displayName,
                                    fontFamily = Poppins,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NayaPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Apply button
            Button(
                onClick = {
                    onApplyFilters(
                        sportFilter,
                        experienceLevel,
                        genderFilter
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NayaPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Apply Filters",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

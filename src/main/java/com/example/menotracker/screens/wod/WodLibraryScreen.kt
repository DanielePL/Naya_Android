// screens/wod/WodLibraryScreen.kt
package com.example.menotracker.screens.wod

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.NayaBackendRepository
import com.example.menotracker.data.models.*
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaSurface
import kotlinx.coroutines.launch

/**
 * WOD Library Screen - Browse and manage saved CrossFit WODs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WodLibraryScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onWodClick: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var wods by remember { mutableStateOf<List<WodTemplate>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Filters
    var selectedWodType by remember { mutableStateOf<String?>(null) }
    var selectedDifficulty by remember { mutableStateOf<String?>(null) }
    var showOnlyMine by remember { mutableStateOf(false) }

    val wodTypes = listOf("amrap", "emom", "for_time", "rft", "chipper", "tabata")
    val difficulties = listOf("beginner", "intermediate", "advanced", "elite")

    // Load WODs
    LaunchedEffect(selectedWodType, selectedDifficulty, showOnlyMine) {
        isLoading = true
        error = null

        val result = NayaBackendRepository.getWods(
            userId = if (showOnlyMine) userId else null,
            wodType = selectedWodType,
            difficulty = selectedDifficulty,
            limit = 50
        )

        result.onSuccess { response ->
            wods = response.wods
            isLoading = false
        }.onFailure { e ->
            error = e.message
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WOD Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(Icons.Default.CameraAlt, "Scan WOD")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToScanner,
                containerColor = NayaPrimary
            ) {
                Icon(Icons.Default.Add, "Scan New WOD", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // My WODs filter
                item {
                    FilterChip(
                        selected = showOnlyMine,
                        onClick = { showOnlyMine = !showOnlyMine },
                        label = { Text("My WODs") },
                        leadingIcon = if (showOnlyMine) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        } else null
                    )
                }

                // WOD Type filters
                items(wodTypes) { type ->
                    FilterChip(
                        selected = selectedWodType == type,
                        onClick = {
                            selectedWodType = if (selectedWodType == type) null else type
                        },
                        label = { Text(type.uppercase()) }
                    )
                }
            }

            // Difficulty filters
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(difficulties) { difficulty ->
                    val color = when (difficulty) {
                        "beginner" -> Color(0xFF4CAF50)
                        "intermediate" -> Color(0xFFFF9800)
                        "advanced" -> Color(0xFFF44336)
                        "elite" -> Color(0xFF9C27B0)
                        else -> Color.Gray
                    }
                    FilterChip(
                        selected = selectedDifficulty == difficulty,
                        onClick = {
                            selectedDifficulty = if (selectedDifficulty == difficulty) null else difficulty
                        },
                        label = { Text(difficulty.replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.2f)
                        )
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NayaPrimary)
                    }
                }

                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                null,
                                tint = Color(0xFFF44336),
                                modifier = Modifier.size(48.dp)
                            )
                            Text("Failed to load WODs")
                            Text(
                                error ?: "Unknown error",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        error = null
                                        val result = NayaBackendRepository.getWods(
                                            userId = if (showOnlyMine) userId else null,
                                            wodType = selectedWodType,
                                            difficulty = selectedDifficulty,
                                            limit = 50
                                        )
                                        result.onSuccess { response ->
                                            wods = response.wods
                                            isLoading = false
                                        }.onFailure { e ->
                                            error = e.message
                                            isLoading = false
                                        }
                                    }
                                }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                wods.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                "No WODs Found",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Scan your first WOD from the whiteboard!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Button(
                                onClick = onNavigateToScanner,
                                colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary)
                            ) {
                                Icon(Icons.Default.CameraAlt, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Scan WOD")
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Stats header
                        item {
                            WodStatsCard(wods = wods)
                        }

                        items(wods) { wod ->
                            WodCard(
                                wod = wod,
                                onClick = { onWodClick(wod.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WodStatsCard(wods: List<WodTemplate>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = NayaPrimary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = wods.size.toString(),
                label = "WODs"
            )
            StatItem(
                value = wods.count { it.wodType == "amrap" }.toString(),
                label = "AMRAPs"
            )
            StatItem(
                value = wods.count { it.wodType == "for_time" || it.wodType == "rft" }.toString(),
                label = "For Time"
            )
            StatItem(
                value = wods.count { it.completionsCount > 0 }.toString(),
                label = "Completed"
            )
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = NayaPrimary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun WodCard(
    wod: WodTemplate,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = NayaSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WOD Type badge
                Surface(
                    color = NayaPrimary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        wod.getWodTypeDisplay(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Time cap
                wod.getTimeCapDisplay()?.let { timeCap ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                        Text(
                            timeCap,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // WOD name
            Text(
                wod.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Description
            wod.description?.let { desc ->
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Rep scheme if present
            wod.getRepSchemeDisplay()?.let { scheme ->
                Text(
                    "Rep scheme: $scheme",
                    style = MaterialTheme.typography.bodySmall,
                    color = NayaPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Footer row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Difficulty badge
                wod.difficulty?.let { difficulty ->
                    val color = Color(wod.getDifficultyColor())
                    Surface(
                        color = color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            difficulty.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = color
                        )
                    }
                }

                // Stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (wod.completionsCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Text(
                                "${wod.completionsCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    if (wod.likesCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFF44336)
                            )
                            Text(
                                "${wod.likesCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Source box name
            wod.sourceBoxName?.let { boxName ->
                Text(
                    boxName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
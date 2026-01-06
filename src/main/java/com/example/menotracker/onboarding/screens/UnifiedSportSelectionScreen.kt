package com.example.menotracker.onboarding.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.menotracker.onboarding.components.*
import com.example.menotracker.onboarding.data.SportCategory
import com.example.menotracker.onboarding.data.SportDatabase
import com.example.menotracker.onboarding.data.UnifiedSport
import com.example.menotracker.ui.theme.*

/**
 * Unified Sport Selection Screen
 *
 * Clean UI with 2 search fields for multi-sport athletes.
 * PREFIX SEARCH with MINIMUM 3 CHARACTERS
 *
 * Uses shared OnboardingComponents for consistent design.
 */
@Composable
fun UnifiedSportSelectionScreen(
    currentStep: Int,
    totalSteps: Int,
    selectedSport: UnifiedSport?,
    selectedSecondarySport: UnifiedSport? = null,
    selectedTertiarySport: UnifiedSport? = null,
    onSelectSport: (UnifiedSport) -> Unit,
    onSelectSecondarySport: (UnifiedSport?) -> Unit = {},
    onSelectTertiarySport: (UnifiedSport?) -> Unit = {},
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    // State for up to 2 sport selections
    var primarySport by remember { mutableStateOf(selectedSport) }
    var secondarySport by remember { mutableStateOf(selectedSecondarySport) }

    // Search queries
    var primaryQuery by remember { mutableStateOf(selectedSport?.name ?: "") }
    var secondaryQuery by remember { mutableStateOf("") }

    // Active field (showing dropdown)
    var activeField by remember { mutableStateOf<Int?>(null) }

    val focusManager = LocalFocusManager.current

    OnboardingScaffold(
        currentStep = currentStep,
        totalSteps = totalSteps,
        title = "What sport do you train for?",
        subtitle = "Type at least 3 letters to search",
        onBack = onBack,
        footer = {
            OnboardingPrimaryButton(
                text = "Continue",
                onClick = onContinue,
                enabled = primarySport != null
            )
        }
    ) {
        // Main content - scrollable
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(OnboardingTokens.spacingMd)
        ) {
            // PRIMARY SPORT (required)
            item {
                SportSearchField(
                    label = "Primary Sport",
                    query = primaryQuery,
                    onQueryChange = { primaryQuery = it },
                    selectedSport = primarySport,
                    onSelectSport = {
                        primarySport = it
                        primaryQuery = it.name
                        onSelectSport(it)
                        activeField = null
                        focusManager.clearFocus()
                    },
                    onClearSport = {
                        primarySport = null
                        primaryQuery = ""
                    },
                    isActive = activeField == 0,
                    onFocusChange = { focused ->
                        activeField = if (focused) 0 else activeField?.takeIf { it != 0 }
                    },
                    placeholder = "e.g. Powerlifting, Tennis, Soccer...",
                    isRequired = true
                )
            }

            // SECONDARY SPORT (optional)
            item {
                SportSearchField(
                    label = "Second Sport (optional)",
                    query = secondaryQuery,
                    onQueryChange = { secondaryQuery = it },
                    selectedSport = secondarySport,
                    onSelectSport = {
                        secondarySport = it
                        secondaryQuery = it.name
                        onSelectSecondarySport(it)
                        activeField = null
                        focusManager.clearFocus()
                    },
                    onClearSport = {
                        secondarySport = null
                        secondaryQuery = ""
                        onSelectSecondarySport(null)
                    },
                    isActive = activeField == 1,
                    onFocusChange = { focused ->
                        activeField = if (focused) 1 else activeField?.takeIf { it != 1 }
                    },
                    placeholder = "e.g. Olympic Weightlifting, CrossFit...",
                    isRequired = false
                )
            }

            // Selected sports summary
            if (primarySport != null) {
                item {
                    Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))
                    SelectedSportsSummary(
                        primary = primarySport,
                        secondary = secondarySport
                    )
                }
            }
        }
    }
}

/**
 * Get Material Icon for sport category
 */
@Composable
private fun getCategoryIcon(category: SportCategory): ImageVector {
    return when (category) {
        SportCategory.STRENGTH -> Icons.Default.FitnessCenter
        SportCategory.BALL -> Icons.Default.SportsSoccer
        SportCategory.COMBAT -> Icons.Default.SportsMartialArts
        SportCategory.ENDURANCE -> Icons.Default.DirectionsRun
        SportCategory.RACKET -> Icons.Default.SportsTennis
        SportCategory.WATER -> Icons.Default.Pool
        SportCategory.WINTER -> Icons.Default.AcUnit
        SportCategory.ATHLETICS -> Icons.Default.EmojiEvents
        SportCategory.GYMNASTICS -> Icons.Default.AccessibilityNew
        SportCategory.OUTDOOR -> Icons.Default.Terrain
        SportCategory.PRECISION -> Icons.Default.GpsFixed
        SportCategory.TEAM -> Icons.Default.Groups
        SportCategory.OTHER -> Icons.Default.Sports
    }
}

/**
 * Sport search field with autocomplete dropdown
 * Uses standardized glassmorphism from BRANDING.md
 */
@Composable
private fun SportSearchField(
    label: String,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedSport: UnifiedSport?,
    onSelectSport: (UnifiedSport) -> Unit,
    onClearSport: () -> Unit,
    isActive: Boolean,
    onFocusChange: (Boolean) -> Unit,
    placeholder: String,
    isRequired: Boolean
) {
    val focusRequester = remember { FocusRequester() }

    // Search results - MINIMUM 3 CHARACTERS
    val searchResults = remember(query) {
        if (query.length >= 3 && selectedSport == null) {
            SportDatabase.search(query).take(5)
        } else {
            emptyList()
        }
    }

    Column {
        // Label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = OnboardingTokens.spacingSm)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = NayaTextPrimary,
                fontFamily = Poppins,
                fontWeight = FontWeight.Medium
            )
            if (isRequired) {
                Text(
                    text = " *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = NayaPrimary,
                    fontFamily = Poppins
                )
            }
        }

        // Search field with glass effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(OnboardingTokens.radiusSmall))
                .background(
                    brush = Brush.linearGradient(
                        colors = when {
                            selectedSport != null -> listOf(
                                NayaPrimary.copy(alpha = 0.15f),
                                Color(0xFF1C1C1C).copy(alpha = 0.9f)
                            )
                            isActive -> listOf(
                                NayaPrimary.copy(alpha = 0.1f),
                                Color(0xFF1C1C1C).copy(alpha = 0.85f)
                            )
                            else -> listOf(
                                Color(0xFF252525).copy(alpha = 0.85f),
                                Color(0xFF1C1C1C).copy(alpha = 0.75f)
                            )
                        }
                    )
                )
                .border(
                    width = if (isActive || selectedSport != null) 2.dp else 1.dp,
                    brush = Brush.linearGradient(
                        colors = when {
                            selectedSport != null -> listOf(
                                NayaPrimary.copy(alpha = 0.6f),
                                NayaPrimary.copy(alpha = 0.3f)
                            )
                            isActive -> listOf(
                                NayaPrimary.copy(alpha = 0.8f),
                                NayaPrimary.copy(alpha = 0.4f)
                            )
                            else -> listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        }
                    ),
                    shape = RoundedCornerShape(OnboardingTokens.radiusSmall)
                )
                .padding(horizontal = OnboardingTokens.spacingMd, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Sport icon if selected, otherwise search icon
                Icon(
                    imageVector = if (selectedSport != null) {
                        getCategoryIcon(selectedSport.category)
                    } else {
                        Icons.Default.Search
                    },
                    contentDescription = null,
                    tint = if (selectedSport != null) NayaPrimary else NayaTextTertiary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(OnboardingTokens.spacingSm + 4.dp))

                // Text input
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { onFocusChange(it.isFocused) },
                    textStyle = TextStyle(
                        color = NayaTextPrimary,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        fontFamily = Poppins
                    ),
                    cursorBrush = SolidColor(NayaPrimary),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = NayaTextTertiary,
                                    fontFamily = Poppins
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // Clear button
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = onClearSport,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = NayaTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Hint text when typing but not enough chars
        if (query.isNotEmpty() && query.length < 3 && selectedSport == null && isActive) {
            Text(
                text = "Type ${3 - query.length} more character${if (3 - query.length > 1) "s" else ""}...",
                style = MaterialTheme.typography.bodySmall,
                color = NayaTextTertiary,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        // Dropdown results
        AnimatedVisibility(
            visible = searchResults.isNotEmpty() && isActive,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(OnboardingTokens.radiusSmall))
                    .background(NayaSurface)
                    .border(
                        width = 1.dp,
                        color = NayaTextTertiary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(OnboardingTokens.radiusSmall)
                    )
            ) {
                searchResults.forEachIndexed { index, sport ->
                    SearchResultItem(
                        sport = sport,
                        onClick = { onSelectSport(sport) }
                    )
                    if (index < searchResults.lastIndex) {
                        HorizontalDivider(
                            color = NayaTextTertiary.copy(alpha = 0.2f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single search result item in dropdown
 */
@Composable
private fun SearchResultItem(
    sport: UnifiedSport,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = OnboardingTokens.spacingMd, vertical = OnboardingTokens.spacingSm + 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category icon
        Icon(
            imageVector = getCategoryIcon(sport.category),
            contentDescription = null,
            tint = NayaPrimary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(OnboardingTokens.spacingSm + 4.dp))

        // Sport name
        Text(
            text = sport.name,
            style = MaterialTheme.typography.bodyLarge,
            color = NayaTextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // VBT indicator for strength-focused sports
        if (sport.vbtRelevance >= 0.7f) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NayaPrimary.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "VBT",
                    style = MaterialTheme.typography.labelSmall,
                    color = NayaPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Summary of selected sports with training focus visualization
 * Uses glassmorphism from BRANDING.md
 */
@Composable
private fun SelectedSportsSummary(
    primary: UnifiedSport?,
    secondary: UnifiedSport?
) {
    val selectedSports = listOfNotNull(primary, secondary)
    if (selectedSports.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .nayaGlow(
                glowColor = NayaPrimary,
                glowRadius = 20.dp,
                glowAlpha = 0.15f,
                cornerRadius = OnboardingTokens.radiusMedium
            )
            .clip(RoundedCornerShape(OnboardingTokens.radiusMedium))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        NayaPrimary.copy(alpha = 0.15f),
                        Color(0xFF1C1C1C).copy(alpha = 0.9f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        NayaPrimary.copy(alpha = 0.5f),
                        NayaPrimary.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(OnboardingTokens.radiusMedium)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OnboardingTokens.spacingMd)
        ) {
            Text(
                text = "Your Training Profile",
                style = MaterialTheme.typography.titleMedium,
                color = NayaTextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm + 4.dp))

            // Selected sports chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(OnboardingTokens.spacingSm),
                modifier = Modifier.fillMaxWidth()
            ) {
                selectedSports.forEach { sport ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(NayaPrimary.copy(alpha = 0.15f))
                            .border(
                                width = 1.dp,
                                color = NayaPrimary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = OnboardingTokens.spacingSm + 4.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = getCategoryIcon(sport.category),
                                contentDescription = null,
                                tint = NayaPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sport.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = NayaTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))

            // Combined training focus
            val combinedFocus = remember(selectedSports) {
                if (selectedSports.isEmpty()) return@remember null
                val avgKraft = selectedSports.map { it.trainingFocus.kraft }.average().toFloat()
                val avgSchnell = selectedSports.map { it.trainingFocus.schnelligkeit }.average().toFloat()
                val avgAusdauer = selectedSports.map { it.trainingFocus.ausdauer }.average().toFloat()
                val avgBeweg = selectedSports.map { it.trainingFocus.beweglichkeit }.average().toFloat()
                val avgGeschick = selectedSports.map { it.trainingFocus.geschicklichkeit }.average().toFloat()
                val avgMindset = selectedSports.map { it.trainingFocus.mindset }.average().toFloat()

                listOf(
                    "Strength" to avgKraft,
                    "Speed" to avgSchnell,
                    "Endurance" to avgAusdauer,
                    "Mobility" to avgBeweg,
                    "Skill" to avgGeschick,
                    "Mindset" to avgMindset
                ).sortedByDescending { it.second }.take(3)
            }

            combinedFocus?.let { topFactors ->
                Text(
                    text = "Training Focus",
                    style = MaterialTheme.typography.labelMedium,
                    color = NayaTextSecondary
                )

                Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm))

                topFactors.forEach { (name, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = NayaTextSecondary,
                            modifier = Modifier.width(70.dp)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(NayaTextTertiary.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(value.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(NayaPrimary)
                            )
                        }

                        Text(
                            text = "${(value * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = NayaTextTertiary,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

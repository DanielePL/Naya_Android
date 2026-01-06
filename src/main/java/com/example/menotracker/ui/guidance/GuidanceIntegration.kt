package com.example.menotracker.ui.guidance

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * GUIDANCE INTEGRATION EXAMPLES
 * ══════════════════════════════════════════════════════════════════════════════
 *
 * Copy-paste Beispiele für jede Screen-Integration
 *
 * ══════════════════════════════════════════════════════════════════════════════
 */

// ═══════════════════════════════════════════════════════════════════════════════
// BEISPIEL 1: Einfacher PulseHint in einem Screen
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Beispiel: PulseHint im NutritionScreen
 *
 * ```kotlin
 * @Composable
 * fun NutritionScreen() {
 *     val context = LocalContext.current
 *     val guidanceManager = remember { GuidanceManager.getInstance(context) }
 *     val (showFabHint, dismissFabHint) = rememberGuidanceState(
 *         hintId = GuidanceIds.NUTRITION_FAB_BUTTONS,
 *         guidanceManager = guidanceManager
 *     )
 *
 *     Box(modifier = Modifier.fillMaxSize()) {
 *         // ... dein Screen content ...
 *
 *         // FAB Buttons
 *         Column(
 *             modifier = Modifier
 *                 .align(Alignment.BottomEnd)
 *                 .padding(16.dp)
 *         ) {
 *             // Hint erscheint NUR beim ersten Mal
 *             PulseHint(
 *                 hint = PulseHints.NUTRITION_SNAP,
 *                 isVisible = showFabHint,
 *                 onDismiss = dismissFabHint
 *             )
 *
 *             Spacer(modifier = Modifier.height(8.dp))
 *
 *             // Deine FABs
 *             FloatingActionButton(onClick = { /* snap */ }) {
 *                 Icon(Icons.Default.PhotoCamera, null)
 *             }
 *         }
 *     }
 * }
 * ```
 */


// ═══════════════════════════════════════════════════════════════════════════════
// BEISPIEL 2: FeatureSpotlight für komplexes Feature
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Beispiel: Spotlight beim ersten Öffnen eines Feature-Screens
 *
 * ```kotlin
 * @Composable
 * fun FeatureScreen() {
 *     val context = LocalContext.current
 *     val guidanceManager = remember { GuidanceManager.getInstance(context) }
 *     val (showSpotlight, dismissSpotlight) = rememberSpotlightState(
 *         spotlightId = GuidanceIds.SPOTLIGHT_FEATURE_INTRO,
 *         guidanceManager = guidanceManager
 *     )
 *
 *     Box(modifier = Modifier.fillMaxSize()) {
 *         // ... Screen Content ...
 *
 *         // Spotlight overlay - blockiert UI bis User "Verstanden" tippt
 *         FeatureSpotlight(
 *             title = Spotlights.FEATURE_INTRO.title,
 *             points = Spotlights.FEATURE_INTRO.points,
 *             isVisible = showSpotlight,
 *             onConfirm = dismissSpotlight,
 *             icon = Spotlights.FEATURE_INTRO.icon
 *         )
 *     }
 * }
 * ```
 */


// ═══════════════════════════════════════════════════════════════════════════════
// BEISPIEL 3: Interactive Tour für First-Time Workflow
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Beispiel: Erste Workout Session Tour
 *
 * ```kotlin
 * @Composable
 * fun ActiveWorkoutSessionScreen() {
 *     val context = LocalContext.current
 *     val guidanceManager = remember { GuidanceManager.getInstance(context) }
 *     val tourState = rememberTourState(
 *         tourId = GuidanceIds.TOUR_FIRST_WORKOUT,
 *         steps = Tours.FIRST_WORKOUT,
 *         guidanceManager = guidanceManager
 *     )
 *
 *     Box(modifier = Modifier.fillMaxSize()) {
 *         // ... Workout Session UI ...
 *
 *         // Tour overlay
 *         if (tourState.isActive) {
 *             TourOverlay(
 *                 currentStep = tourState.currentStep,
 *                 totalSteps = tourState.totalSteps,
 *                 currentIndex = tourState.currentIndex,
 *                 onNext = tourState.onNext,
 *                 onSkip = tourState.onSkip
 *             )
 *         }
 *     }
 * }
 * ```
 */


// ═══════════════════════════════════════════════════════════════════════════════
// BEISPIEL 4: ContextualBadge mit Erklärung
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Beispiel: Calibration Badge in VBT Metrics
 *
 * ```kotlin
 * @Composable
 * fun VBTMetricsDisplay(isCalibrated: Boolean) {
 *     Row {
 *         Text("Peak Velocity: 0.85 m/s")
 *
 *         Spacer(modifier = Modifier.width(8.dp))
 *
 *         // Badge erklärt sich bei Tap
 *         if (isCalibrated) {
 *             ContextualBadge(
 *                 label = "Calibrated ✓",
 *                 explanation = BadgeExplanations.CALIBRATED,
 *                 backgroundColor = NayaSuccess.copy(alpha = 0.15f),
 *                 textColor = NayaSuccess
 *             )
 *         } else {
 *             ContextualBadge(
 *                 label = "Relative",
 *                 explanation = BadgeExplanations.UNCALIBRATED,
 *                 backgroundColor = NayaWarning.copy(alpha = 0.15f),
 *                 textColor = NayaWarning
 *             )
 *         }
 *     }
 * }
 * ```
 */


// ═══════════════════════════════════════════════════════════════════════════════
// BEISPIEL 5: PulseHintDot für kompakte UI
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Beispiel: Info-Dot neben einem Icon-Button
 *
 * ```kotlin
 * @Composable
 * fun SettingsButton(onInfoClick: () -> Unit) {
 *     Row(verticalAlignment = Alignment.CenterVertically) {
 *         IconButton(onClick = { /* settings */ }) {
 *             Icon(Icons.Default.Settings, "Settings")
 *         }
 *
 *         // Pulsierender Punkt zeigt "hier gibt's was zu entdecken"
 *         PulseHintDot(
 *             onClick = onInfoClick,
 *             color = NayaPrimary
 *         )
 *     }
 * }
 * ```
 */


// ═══════════════════════════════════════════════════════════════════════════════
// BEISPIEL 6: MiniTooltip für Metriken
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Beispiel: Erklärung für VBT Metrik
 *
 * ```kotlin
 * @Composable
 * fun MetricCard(
 *     label: String,
 *     value: String,
 *     explanation: String
 * ) {
 *     var showTooltip by remember { mutableStateOf(false) }
 *
 *     Column {
 *         Row(
 *             verticalAlignment = Alignment.CenterVertically,
 *             modifier = Modifier.clickable { showTooltip = !showTooltip }
 *         ) {
 *             Text(label, style = MaterialTheme.typography.labelSmall)
 *             Icon(
 *                 Icons.Default.Info,
 *                 contentDescription = "Info",
 *                 modifier = Modifier.size(14.dp)
 *             )
 *         }
 *
 *         Text(value, style = MaterialTheme.typography.headlineMedium)
 *
 *         MiniTooltip(
 *             text = explanation,
 *             isVisible = showTooltip,
 *             onDismiss = { showTooltip = false }
 *         )
 *     }
 * }
 *
 * // Usage:
 * MetricCard(
 *     label = "Velocity Drop",
 *     value = "18%",
 *     explanation = MetricExplanations.VELOCITY_DROP
 * )
 * ```
 */


// ═══════════════════════════════════════════════════════════════════════════════
// HELPER: Guidance in Settings zurücksetzen
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Beispiel: "Tutorials zurücksetzen" Button in Settings
 *
 * ```kotlin
 * @Composable
 * fun SettingsScreen() {
 *     val context = LocalContext.current
 *     val guidanceManager = remember { GuidanceManager.getInstance(context) }
 *
 *     // ... andere Settings ...
 *
 *     SettingsItem(
 *         icon = Icons.Default.Refresh,
 *         title = "Tutorials zurücksetzen",
 *         subtitle = "Zeige alle Tipps und Anleitungen erneut",
 *         onClick = {
 *             guidanceManager.resetAllGuidance()
 *             // Optional: Toast/Snackbar zeigen
 *         }
 *     )
 * }
 * ```
 */


// ═══════════════════════════════════════════════════════════════════════════════
// QUICK REFERENCE: Welche Guidance für welchen Screen?
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * SCREEN → GUIDANCE MAPPING
 *
 * ┌─────────────────────────────┬─────────────────────────────────────────────┐
 * │ Screen                      │ Guidance                                    │
 * ├─────────────────────────────┼─────────────────────────────────────────────┤
 * │ HomeScreen                  │ PulseHint: Coach, Quick Start, Lab          │
 * │ NutritionScreen             │ Spotlight: Nutrition Intro                  │
 * │                             │ PulseHint: FAB Buttons, Macro Colors        │
 * │                             │ Tour: Nutrition Setup (first time)          │
 * │ ActiveWorkoutSessionScreen  │ Tour: First Workout                         │
 * │                             │ PulseHint: Start, Set Logging, Rest Timer   │
 * │ NayaLabScreen         │ Spotlight: Lab Intro                        │
 * │                             │ PulseHint: VBT Tab, Load Tab, Insights      │
 * │ AICoachScreen               │ PulseHint: Attachments, Save Workout        │
 * │ LibraryScreen               │ PulseHint: Tabs, Templates, Create          │
 * │ TrainingScreen              │ PulseHint: Active Program                   │
 * │ CalibrationScreen           │ Eigene inline Guidance (bereits vorhanden)  │
 * └─────────────────────────────┴─────────────────────────────────────────────┘
 */
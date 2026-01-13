// app/src/main/java/com/example/myapplicationtest/MainActivity.kt

package com.example.menotracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.menotracker.data.AuthRepository
import com.example.menotracker.data.ThemePreferences
import com.example.menotracker.data.UserProfileRepository
import com.example.menotracker.ui.guidance.GuidanceManager
import com.example.menotracker.screens.account.AccountScreen
import com.example.menotracker.screens.auth.LoginScreen
import com.example.menotracker.viewmodels.AuthViewModel
import com.example.menotracker.onboarding.OnboardingFlow
import com.example.menotracker.onboarding.OnboardingViewModel
import com.example.menotracker.screens.campus.CampusScreen
import com.example.menotracker.screens.create.CreateExerciseScreen
import com.example.menotracker.screens.create.CreateProgramScreen
import com.example.menotracker.screens.detail.ExerciseDetailScreen
import com.example.menotracker.screens.workout.WorkoutDetailScreen
import com.example.menotracker.screens.home.SmartHomeScreen
import com.example.menotracker.screens.home.SmartHomeViewModel
import com.example.menotracker.screens.library.LibraryScreen
import com.example.menotracker.screens.training.TrainingScreen
import com.example.menotracker.screens.nutrition.NutritionScreen
// Form Analysis removed - feature not included in NAYA
import com.example.menotracker.screens.workoutbuilder.WorkoutBuilderScreen
import com.example.menotracker.screens.session.ActiveWorkoutSessionScreen
import com.example.menotracker.screens.ai_coach.AICoachScreen
import com.example.menotracker.screens.ai_coach.AICoachViewModel
import com.example.menotracker.screens.coach.CoachScreen
import com.example.menotracker.screens.coach.PhysicalCoachViewModel
import com.example.menotracker.screens.lab.MenoLabScreen
import com.example.menotracker.screens.lab.NayaLabScreen
import com.example.menotracker.screens.statistics.TrainingStatisticsScreen
import com.example.menotracker.data.ExerciseRepository
import com.example.menotracker.data.WorkoutRepository
import com.example.menotracker.data.ProgramRepository
import com.example.menotracker.data.WorkoutTemplateRepository
import com.example.menotracker.data.WorkoutSessionRepository
import com.example.menotracker.data.SupabaseClient
import com.example.menotracker.data.models.Exercise
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaTheme
import com.example.menotracker.ui.theme.glassBackground
import com.example.menotracker.ui.theme.NayaSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import com.example.menotracker.viewmodels.WorkoutBuilderViewModel
import com.example.menotracker.viewmodels.WorkoutTemplate
import com.example.menotracker.viewmodels.WorkoutTimerViewModel
import com.example.menotracker.viewmodels.RestTimerViewModel
import com.example.menotracker.viewmodels.WorkoutSessionViewModel
import com.example.menotracker.debug.DebugLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import java.util.UUID
import com.example.menotracker.data.ExerciseWithSets
import com.example.menotracker.community.screens.*
import com.example.menotracker.billing.ProStatusProvider
import com.example.menotracker.billing.BillingManager
import com.example.menotracker.screens.wod.WodScannerScreen
import com.example.menotracker.screens.wod.WodLibraryScreen
import com.example.menotracker.screens.wod.WodDetailScreen
import com.example.menotracker.screens.breathing.BreathingScreen
import com.example.menotracker.screens.breathing.BreathingSessionScreen
import com.example.menotracker.screens.mindfulness.MindfulnessScreen
import com.example.menotracker.screens.meditation.MeditationScreen
import com.example.menotracker.screens.meditation.MeditationSessionScreen
import com.example.menotracker.screens.meditation.SoundscapeScreen
import com.example.menotracker.screens.admin.AdminContentScreen
import com.example.menotracker.data.AdminManager
import com.example.menotracker.data.models.BreathingExerciseType
import com.example.menotracker.data.models.MeditationType
import com.example.menotracker.util.LocaleHelper
import com.example.menotracker.data.SettingsDataStore

// Data class for tracking exercise swap context
data class SwapExerciseContext(
    val workoutId: String,
    val exerciseIdToReplace: String
)

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val workoutBuilderViewModel: WorkoutBuilderViewModel by viewModels()
    private lateinit var themePreferences: ThemePreferences

    // Track when deep link auth is processed - triggers re-initialization of onboarding
    private var _deepLinkAuthProcessed = mutableStateOf(0)
    // Track when deep link is being processed - shows loading spinner to prevent flicker
    private var _isProcessingDeepLink = mutableStateOf(false)

    // Track current language to detect changes
    private var currentLanguage: String = "system"

    /**
     * Apply saved locale before the activity is created.
     * This ensures the correct language is used from the start.
     */
    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            val languageCode = SettingsDataStore.getLanguageSync(newBase)
            currentLanguage = languageCode
            val localizedContext = LocaleHelper.setLocale(newBase, languageCode)
            super.attachBaseContext(localizedContext)
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        themePreferences = ThemePreferences(this)

        // Initialize SubscriptionManager to restore trial/dev mode state
        com.example.menotracker.billing.SubscriptionManager.initialize(this)

        // Initialize WorkoutTemplateRepository with context for disk caching
        // This loads cached templates from SharedPreferences INSTANTLY (no network)
        com.example.menotracker.data.WorkoutTemplateRepository.initialize(this)

        // Handle deep link from email verification
        handleDeepLink(intent)

        setContent {
            val isDarkMode by themePreferences.isDarkMode.collectAsState(initial = true)
            val scope = rememberCoroutineScope()
            val authViewModel: AuthViewModel = viewModel()
            val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

            // ═══════════════════════════════════════════════════════════════
            // LANGUAGE CHANGE OBSERVER - Recreate activity when language changes
            // ═══════════════════════════════════════════════════════════════
            val settingsDataStore = remember { SettingsDataStore(this@MainActivity) }
            val selectedLanguage by settingsDataStore.language.collectAsState(initial = currentLanguage)

            LaunchedEffect(selectedLanguage) {
                // Only recreate if language actually changed (not on initial load)
                if (selectedLanguage != currentLanguage) {
                    Log.d(TAG, "🌐 Language changed from $currentLanguage to $selectedLanguage - recreating activity")
                    currentLanguage = selectedLanguage
                    recreate()
                }
            }

            // ✅ FIX: Debounced login state to prevent flickering during camera/background transitions
            // This prevents the app from showing LoginScreen when returning from camera
            // Supabase can take up to 10+ seconds to restore session after returning from camera activity
            //
            // IMPORTANT: Start with null (unknown) state to show loading until we know the real auth state
            var stableLoggedIn by remember { mutableStateOf<Boolean?>(null) }  // null = loading, checking auth
            var hasInitialized by remember { mutableStateOf(false) }
            var wasLoggedInBefore by remember { mutableStateOf(false) }  // Track if user was ever logged in
            var isExplicitLogout by remember { mutableStateOf(false) }  // Track explicit logout to skip recovery
            var showLoginAfterLogout by remember { mutableStateOf(false) }  // Show login screen after explicit logout

            LaunchedEffect(isLoggedIn, isExplicitLogout) {
                // If user explicitly logged out, immediately show login screen
                if (isExplicitLogout) {
                    Log.d(TAG, "🔐 Explicit logout - skipping session recovery")
                    stableLoggedIn = false
                    wasLoggedInBefore = false
                    hasInitialized = false  // Reset so next login initializes properly
                    isExplicitLogout = false  // Reset flag for next login cycle
                    return@LaunchedEffect
                }

                if (!hasInitialized) {
                    // First check - use actual value immediately
                    stableLoggedIn = isLoggedIn
                    wasLoggedInBefore = isLoggedIn
                    hasInitialized = true
                    Log.d(TAG, "🔐 Initial auth state: isLoggedIn=$isLoggedIn, setting stableLoggedIn=$isLoggedIn")
                } else if (!isLoggedIn && wasLoggedInBefore) {
                    // User WAS logged in, now appears logged out
                    // This is likely a false positive from camera/background - wait longer!
                    Log.d(TAG, "🔐 Auth state changed to false - waiting 15 seconds to confirm (camera/background recovery)...")

                    // Wait up to 15 seconds, checking periodically
                    var confirmed = false
                    repeat(15) { second ->
                        delay(1000)  // Wait 1 second
                        // Check if user explicitly logged out during wait
                        if (isExplicitLogout) {
                            Log.d(TAG, "🔐 Explicit logout during wait - aborting recovery")
                            stableLoggedIn = false
                            wasLoggedInBefore = false
                            return@LaunchedEffect
                        }
                        if (authViewModel.isLoggedIn.value) {
                            Log.d(TAG, "🔐 Session restored after ${second + 1} seconds - staying logged in")
                            confirmed = true
                            return@repeat
                        }
                    }

                    if (!confirmed) {
                        // After 15 seconds, actually log out
                        Log.d(TAG, "🔐 Confirmed after 15s: User is logged out")
                        stableLoggedIn = false
                        wasLoggedInBefore = false
                    }
                } else if (!isLoggedIn) {
                    // User was never logged in - respect immediately
                    Log.d(TAG, "🔐 User not logged in (first time)")
                    stableLoggedIn = false
                } else {
                    // isLoggedIn is true - immediately update
                    Log.d(TAG, "🔐 Auth state: logged in")
                    stableLoggedIn = true
                    wasLoggedInBefore = true
                }
            }

            NayaTheme(darkTheme = isDarkMode) {
                // Provide Pro status throughout the app
                val context = androidx.compose.ui.platform.LocalContext.current

                // ✅ Track deep link auth processing to re-trigger initialization
                val deepLinkAuthProcessed by remember { _deepLinkAuthProcessed }
                // ✅ Track if deep link is being processed (prevents flicker)
                val isProcessingDeepLink by remember { _isProcessingDeepLink }

                // When deep link auth is processed, immediately update login state
                LaunchedEffect(deepLinkAuthProcessed) {
                    if (deepLinkAuthProcessed > 0) {
                        Log.d(TAG, "🔐 Deep link auth detected - setting stableLoggedIn = true")
                        stableLoggedIn = true
                        wasLoggedInBefore = true
                        // ✅ FIX: Only hide loading spinner AFTER stableLoggedIn is true
                        // This prevents CreateAccountScreen flicker
                        _isProcessingDeepLink.value = false
                    }
                }

                ProStatusProvider(context = context) {
                    // ✅ ANTI-FLICKER: Dark background prevents white flash during state transitions
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF121212))  // Match app's dark theme
                    ) {
                    // ✅ Use stableLoggedIn instead of isLoggedIn
                    // null = still loading/checking auth, show nothing (or loading spinner)
                    Log.d(TAG, "🔐🔐🔐 RENDERING: stableLoggedIn=$stableLoggedIn, isLoggedIn=$isLoggedIn, isProcessingDeepLink=$isProcessingDeepLink, deepLinkAuthProcessed=$deepLinkAuthProcessed")

                    // ✅ Show loading spinner while processing deep link (prevents flicker)
                    // Also show spinner if we just processed a deep link but stableLoggedIn hasn't updated yet
                    // Use != true to catch both null and false cases
                    val shouldShowDeepLinkLoading = isProcessingDeepLink ||
                        (deepLinkAuthProcessed > 0 && stableLoggedIn != true)

                    if (shouldShowDeepLinkLoading) {
                        Log.d(TAG, "🔐🔐🔐 RENDERING: Deep link in progress - showing LOADING spinner (isProcessing=$isProcessingDeepLink, processed=$deepLinkAuthProcessed, stableLoggedIn=$stableLoggedIn)")
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NayaPrimary)
                        }
                    } else when (stableLoggedIn) {
                        null -> {
                            // Auth state unknown - show loading
                            Log.d(TAG, "🔐🔐🔐 RENDERING: Showing LOADING spinner")
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = NayaPrimary)
                            }
                        }
                        true -> {
                    // User is logged in → Check onboarding status
                    val onboardingViewModel: OnboardingViewModel = viewModel()
                    val isOnboardingComplete by onboardingViewModel.isOnboardingComplete.collectAsState()

                    // ✅ DEFINITIVE FIX: Key-based rendering to prevent flicker
                    // Problem: When deepLinkAuthProcessed changes, LaunchedEffect runs AFTER composition
                    // So the first frame after deep link still shows old content → flicker!
                    //
                    // Solution: Use two keys - targetKey updates immediately, renderKey updates
                    // ONLY after LaunchedEffect completes. Show loading when keys don't match.
                    //
                    // ✅ FIX: Start renderKey at -1 so first composition shows loading
                    // This ensures isOnboardingComplete is loaded BEFORE we check it
                    var renderKey by remember { mutableStateOf(-1) }
                    val targetKey = deepLinkAuthProcessed

                    // Call initialize() and only allow rendering when it completes
                    LaunchedEffect(deepLinkAuthProcessed) {
                        Log.d(TAG, "🔄 LaunchedEffect START: deepLinkAuthProcessed=$deepLinkAuthProcessed, renderKey=$renderKey")
                        onboardingViewModel.initialize(context)
                        // Wait for initialization to actually complete
                        // Use StateFlow.first() directly (not snapshotFlow - that's for Compose State)
                        onboardingViewModel.isInitialized
                            .filter { it }
                            .first()
                        // NOW it's safe to render - update the render key
                        renderKey = deepLinkAuthProcessed
                        Log.d(TAG, "🔄 LaunchedEffect DONE: renderKey updated to $renderKey")
                    }

                    // DEBUG: Track if we want to force show onboarding
                    var forceShowOnboarding by remember { mutableStateOf(false) }

                    Log.d(TAG, "🔐🔐🔐 RENDERING true branch: targetKey=$targetKey, renderKey=$renderKey, isComplete=$isOnboardingComplete")

                    // ✅ Show loading if keys don't match (LaunchedEffect hasn't completed yet)
                    if (renderKey != targetKey) {
                        Log.d(TAG, "🔐🔐🔐 RENDERING: Keys don't match ($renderKey != $targetKey) - showing LOADING")
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NayaPrimary)
                        }
                    } else if ((!isOnboardingComplete && !wasLoggedInBefore) || forceShowOnboarding) {
                        // ✅ FIX: If user explicitly logged in (wasLoggedInBefore=true), skip onboarding
                        // and go directly to MainScreen. This prevents the login→welcome loop!
                        // Onboarding not complete → Show onboarding flow
                        OnboardingFlow(
                            onComplete = {
                                // Onboarding complete - state will automatically update
                                forceShowOnboarding = false
                                android.util.Log.d("MainActivity", "✅ Onboarding completed")
                            },
                            onLogin = {
                                // User wants to log in with existing account
                                // Log out current session and show LoginScreen
                                android.util.Log.d("MainActivity", "🔐 User clicked login - showing login screen")
                                wasLoggedInBefore = false
                                stableLoggedIn = false
                                authViewModel.logout {
                                    android.util.Log.d("MainActivity", "✅ Logged out - showing login screen")
                                }
                            }
                        )
                    } else {
                        // Onboarding complete → Show main app
                        val aiCoachViewModel = remember {
                            AICoachViewModel(workoutBuilderViewModel)
                        }
                        val physicalCoachViewModel = remember {
                            PhysicalCoachViewModel()
                        }

                        // DEBUG: Reset onboarding button (only in debug builds)
                        if (BuildConfig.DEBUG) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                MainScreen(
                                    workoutBuilderViewModel = workoutBuilderViewModel,
                                    aiCoachViewModel = aiCoachViewModel,
                                    physicalCoachViewModel = physicalCoachViewModel,
                                    isDarkMode = isDarkMode,
                                    onThemeChange = { isDark ->
                                        scope.launch {
                                            themePreferences.setDarkMode(isDark)
                                        }
                                    },
                                    onLogout = {
                                        // Mark as explicit logout to skip session recovery
                                        isExplicitLogout = true
                                        showLoginAfterLogout = true  // Show login screen directly after logout
                                        wasLoggedInBefore = false
                                        stableLoggedIn = false
                                    }
                                )

                                // DEBUG buttons - small dots top right
                                // Red = Toggle Dev Mode (tier bypass + onboarding), Blue = Toggle Guidance
                                val guidanceManager = remember { GuidanceManager.getInstance(context) }
                                var guidanceEnabled by remember { mutableStateOf(true) }
                                val devModeEnabled by com.example.menotracker.billing.SubscriptionManager.devModeEnabled.collectAsState()

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 52.dp, end = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Blue dot - Toggle Guidance (tap to show/hide all hints)
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                color = if (guidanceEnabled) Color(0xFF2196F3) else Color(0xFF2196F3).copy(alpha = 0.3f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                if (guidanceEnabled) {
                                                    // Disable - mark all as seen
                                                    guidanceManager.resetAllGuidance()
                                                    // Mark a special flag so hints don't show
                                                    guidanceManager.markHintAsSeen("__guidance_disabled__")
                                                } else {
                                                    // Enable - reset all guidance to show again
                                                    guidanceManager.resetAllGuidance()
                                                }
                                                guidanceEnabled = !guidanceEnabled
                                                Log.d("MainActivity", "Guidance ${if (guidanceEnabled) "ENABLED" else "DISABLED"}")
                                            }
                                    )

                                    // Red dot - Toggle Dev Mode (bypasses tier restrictions + resets onboarding)
                                    // Bright red = DEV MODE ON (all features unlocked)
                                    // Dim red = DEV MODE OFF (real tier restrictions)
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                color = if (devModeEnabled) Color.Red else Color.Red.copy(alpha = 0.3f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                // Toggle dev mode (tier restrictions bypass)
                                                com.example.menotracker.billing.SubscriptionManager.toggleDevMode()
                                                Log.d("MainActivity", "Dev Mode ${if (!devModeEnabled) "ENABLED - All features unlocked" else "DISABLED - Tier restrictions active"}")

                                                // Also toggle onboarding for testing
                                                if (!devModeEnabled) {
                                                    // If turning ON dev mode, reset onboarding to test flow
                                                    onboardingViewModel.resetOnboarding()
                                                    forceShowOnboarding = true
                                                }
                                            }
                                    )
                                }
                            }
                        } else {
                            MainScreen(
                                workoutBuilderViewModel = workoutBuilderViewModel,
                                aiCoachViewModel = aiCoachViewModel,
                                physicalCoachViewModel = physicalCoachViewModel,
                                isDarkMode = isDarkMode,
                                onThemeChange = { isDark ->
                                    scope.launch {
                                        themePreferences.setDarkMode(isDark)
                                    }
                                },
                                onLogout = {
                                    // Mark as explicit logout to skip session recovery
                                    isExplicitLogout = true
                                    showLoginAfterLogout = true  // Show login screen directly after logout
                                    wasLoggedInBefore = false
                                    stableLoggedIn = false
                                }
                            )
                        }
                    }
                }
                false -> {
                    // User is not logged in → Show Onboarding flow
                    // NEW: First-time users see onboarding, then register at the end
                    // Existing users can click "Already have an account? Log in" on WelcomeScreen

                    // Track if user wants to see login screen instead
                    // Also check if we should show login after explicit logout
                    var showLoginScreen by remember { mutableStateOf(showLoginAfterLogout) }

                    // Reset showLoginAfterLogout after we've consumed it
                    LaunchedEffect(showLoginAfterLogout) {
                        if (showLoginAfterLogout) {
                            showLoginScreen = true
                            showLoginAfterLogout = false  // Reset flag
                        }
                    }

                    if (showLoginScreen) {
                        // User clicked "Log in" on WelcomeScreen
                        LoginScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = {
                                // After login, immediately show home screen
                                android.util.Log.d("MainActivity", "✅ Login successful - navigating to home")
                                showLoginScreen = false  // Exit LoginScreen
                                stableLoggedIn = true    // Navigate to HomeScreen
                                wasLoggedInBefore = true // Prevent session recovery issues
                            }
                        )
                    } else {
                        // Show onboarding flow for new users
                        val onboardingViewModel: OnboardingViewModel = viewModel()

                        LaunchedEffect(Unit) {
                            onboardingViewModel.initialize(context)
                        }

                        OnboardingFlow(
                            onComplete = {
                                // After registration in onboarding, user will be logged in
                                // DEV SKIP also triggers this - fake login state to show MainScreen
                                android.util.Log.d("MainActivity", "✅ Onboarding + Registration completed")
                                stableLoggedIn = true
                            },
                            onLogin = {
                                // User clicked "Already have an account? Log in"
                                android.util.Log.d("MainActivity", "🔐 User wants to log in - showing login screen")
                                showLoginScreen = true
                            }
                        )
                    }
                }
                } // End of when (stableLoggedIn)
            } // End of anti-flicker Box
            } // End of ProStatusProvider
        } // End of NayaTheme
        } // End of setContent
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data: Uri? = intent?.data

        if (data != null && data.scheme == "naya" && data.host == "auth") {
            Log.d(TAG, "🔗 Deep link received: $data")

            // Supabase sends tokens as FRAGMENT (after #), not query params
            // Example: naya://auth/callback#access_token=xxx&refresh_token=yyy
            val fragment = data.fragment
            val fragmentParams = fragment?.split("&")?.associate {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
            } ?: emptyMap()

            // Try fragment first, then query params as fallback
            val accessToken = fragmentParams["access_token"] ?: data.getQueryParameter("access_token")
            val refreshToken = fragmentParams["refresh_token"] ?: data.getQueryParameter("refresh_token")

            Log.d(TAG, "🔐 Fragment params: ${fragmentParams.keys}")
            Log.d(TAG, "🔐 Access token found: ${accessToken != null}")
            Log.d(TAG, "🔐 Refresh token found: ${refreshToken != null}")

            if (accessToken != null && refreshToken != null) {
                Log.d(TAG, "✅ Tokens found in deep link - setting session")

                // ✅ Show loading spinner immediately to prevent flicker
                _isProcessingDeepLink.value = true

                // Clear the intent data to prevent reprocessing on activity recreation
                intent?.data = null
                setIntent(Intent())

                // Use AuthRepository to set the session
                lifecycleScope.launch {
                    try {
                        val authRepository = AuthRepository(applicationContext)
                        authRepository.setSessionFromTokens(accessToken, refreshToken)
                        Log.d(TAG, "✅ Session restored from email verification - user is now logged in!")

                        // Trigger UI refresh by incrementing the counter
                        _deepLinkAuthProcessed.value++
                        Log.d(TAG, "🔄 Triggering UI refresh for deep link auth")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to set session from tokens: ${e.message}")
                        // Only hide spinner on error - success case is handled in LaunchedEffect
                        _isProcessingDeepLink.value = false
                    }
                }
            } else {
                Log.w(TAG, "⚠️ Deep link received but no tokens found. Fragment: $fragment")
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Library : Screen("library", "Library", Icons.Default.FitnessCenter)
    object Training : Screen("training", "Training", Icons.Default.DirectionsRun)
    object Nutrition : Screen("nutrition", "Nutrition", Icons.Default.Restaurant)
    object MealPhotoCapture : Screen("meal_photo_capture", "Snap Meal", Icons.Default.CameraAlt)
    object MealAnalysisResult : Screen("meal_analysis_result", "Analysis Result", Icons.Default.Restaurant)
    object NutritionInsights : Screen("nutrition_insights", "Nutrition Insights", Icons.Default.Insights)
    object FoodSearch : Screen("food_search/{mealType}/{mealId}", "Search Food", Icons.Default.Search) {
        fun createRoute(mealType: String, mealId: String) = "food_search/$mealType/$mealId"
    }
    object BarcodeScanner : Screen("barcode_scanner/{mealType}", "Scan Barcode", Icons.Default.QrCodeScanner) {
        fun createRoute(mealType: String) = "barcode_scanner/$mealType"
    }
    object SmartFoodCamera : Screen("smart_food_camera/{mealType}", "Smart Scanner", Icons.Default.CameraAlt) {
        fun createRoute(mealType: String) = "smart_food_camera/$mealType"
    }
    object Coach : Screen("coach", "Coach", Icons.Default.Psychology)
    object Account : Screen("account", "Account", Icons.Default.Person)
    object AICoach : Screen("ai_coach", "Naya Coach", Icons.Default.Psychology) // Keep for direct navigation
    object Campus : Screen("campus", "Naya Campus", Icons.Default.School)
    object Lab : Screen("lab", "Naya Lab", Icons.Default.Science)
    object LibrarySelection : Screen("library_selection/{selectionMode}/{initialTab}", "Select", Icons.Default.FitnessCenter) {
        fun createRoute(selectionMode: String, initialTab: Int) = "library_selection/$selectionMode/$initialTab"
    }
    object WorkoutBuilder : Screen("workout_builder", "Create Workout", Icons.Default.Add)
    object CreateExercise : Screen("create_exercise", "Create Exercise", Icons.Default.FitnessCenter)
    object CreateProgram : Screen("create_program", "Create Program", Icons.Default.CalendarViewWeek)
    object ActiveSession : Screen("active_session", "Workout Session", Icons.Default.FitnessCenter)
    object VideoPlayer : Screen("video_player/{videoUri}", "Video Player", Icons.Default.PlayCircle) {
        fun createRoute(videoUri: String) = "video_player/$videoUri"
    }
    object ExerciseDetail : Screen("exercise_detail/{exerciseId}", "Exercise Detail", Icons.Default.FitnessCenter) {
        fun createRoute(exerciseId: String) = "exercise_detail/$exerciseId"
    }
    object WorkoutDetail : Screen("workout_detail/{workoutId}", "Workout Detail", Icons.Default.FitnessCenter) {
        fun createRoute(workoutId: String) = "workout_detail/$workoutId"
    }
    // WOD (CrossFit Workout of the Day) Screens
    object WodScanner : Screen("wod_scanner", "Scan WOD", Icons.Default.CameraAlt)
    object WodLibrary : Screen("wod_library", "WOD Library", Icons.Default.FitnessCenter)
    object WodDetail : Screen("wod_detail/{wodId}", "WOD Detail", Icons.Default.FitnessCenter) {
        fun createRoute(wodId: String) = "wod_detail/$wodId"
    }

    // Breathing Exercises Screens
    object Breathing : Screen("breathing", "Breathing", Icons.Default.Air)
    object BreathingSession : Screen("breathing_session/{exerciseType}", "Breathing Session", Icons.Default.Air) {
        fun createRoute(exerciseType: BreathingExerciseType) = "breathing_session/${exerciseType.name}"
    }

    // Mindfulness Hub (combines Breathing + Meditation)
    object Mindfulness : Screen("mindfulness", "Mindfulness", Icons.Default.Spa)

    // Meditation Screens
    object Meditation : Screen("meditation", "Meditation", Icons.Default.SelfImprovement)
    object MeditationSession : Screen("meditation_session/{meditationType}", "Meditation Session", Icons.Default.SelfImprovement) {
        fun createRoute(meditationType: MeditationType) = "meditation_session/${meditationType.name}"
    }
    object Soundscape : Screen("soundscape", "Soundscape", Icons.Default.Tune)

    // Admin Screen (only accessible to admins)
    object AdminContent : Screen("admin_content", "Admin", Icons.Default.AdminPanelSettings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    workoutBuilderViewModel: WorkoutBuilderViewModel,
    aiCoachViewModel: AICoachViewModel,
    physicalCoachViewModel: PhysicalCoachViewModel,
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    // ✅ Update user's last seen timestamp on app start
    LaunchedEffect(Unit) {
        // In a real app, you would get the current user's ID after they log in.
        // For now, we use a placeholder ID.
        val dummyUserId = "00000000-0000-0000-0000-000000000000"
        UserProfileRepository.updateLastSeen(dummyUserId)
    }

    // ✅ PRE-LOAD all repositories in PARALLEL at app startup for faster perceived performance
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        Log.d("MainActivity", "🚀 Pre-loading repositories in parallel...")

        // Launch all initializations in parallel
        val exercisesDeferred = async { ExerciseRepository.initialize() }
        val workoutsDeferred = async { WorkoutRepository.initialize() }
        val programsDeferred = async { ProgramRepository.initialize() }
        val templatesDeferred = async { ProgramRepository.initializeTemplates() }
        val publicTemplatesDeferred = async { WorkoutTemplateRepository.loadPublicWorkoutTemplates() }

        // Wait for all to complete
        awaitAll(exercisesDeferred, workoutsDeferred, programsDeferred, templatesDeferred, publicTemplatesDeferred)

        val elapsed = System.currentTimeMillis() - startTime
        Log.d("MainActivity", "✅ All repositories pre-loaded in ${elapsed}ms")

        // ✅ IMPORTANT: Trigger ViewModel to reload from cache after repositories are ready
        workoutBuilderViewModel.loadWorkouts()
        workoutBuilderViewModel.loadPublicWorkouts()
        Log.d("MainActivity", "✅ Triggered ViewModel to reload workouts from cache")
    }

    // State for exercise swap functionality
    var swapContext by remember { mutableStateOf<SwapExerciseContext?>(null) }

    // Shared ViewModel for workout session communication
    val workoutSessionViewModel: WorkoutSessionViewModel = viewModel()

    // ✅ Use WorkoutSessionViewModel to persist active workout across camera/background transitions
    val activeWorkout by workoutSessionViewModel.activeWorkout.collectAsState()
    val activeWorkoutSessionId by workoutSessionViewModel.activeSessionId.collectAsState()
    val workoutTimerViewModel: WorkoutTimerViewModel = viewModel()
    val restTimerViewModel: RestTimerViewModel = viewModel()
    val accountViewModel: com.example.menotracker.viewmodels.AccountViewModel = viewModel()

    // Shared NutritionViewModel - must be at MainScreen level to survive navigation
    val nutritionViewModel: com.example.menotracker.viewmodels.NutritionViewModel = viewModel()

    // Shared AuthViewModel for logout functionality
    val authViewModel: com.example.menotracker.viewmodels.AuthViewModel = viewModel()

    // ✅ Sync real user ID to WorkoutBuilderViewModel when user profile loads
    val userProfile by accountViewModel.userProfile.collectAsState()
    LaunchedEffect(userProfile?.id) {
        val userId = userProfile?.id
        Log.d("MainActivity", "👤 Syncing user ID to WorkoutBuilder: $userId")
        workoutBuilderViewModel.setUserId(userId)
    }

    // ✅ Restore last used workout on app start (survives app restarts!)
    LaunchedEffect(Unit) {
        val lastWorkout = WorkoutTemplateRepository.getLastUsedWorkout()
        if (lastWorkout != null && activeWorkout == null) {
            Log.d("MainActivity", "📦 Restoring last used workout: ${lastWorkout.name}")
            workoutSessionViewModel.setActiveWorkout(lastWorkout)
            workoutSessionViewModel.setActiveSessionId(java.util.UUID.randomUUID().toString())
        }
    }

    // Bottom Nav Layout: Home | Nutrition | Training | Coach | Profile (Prometheus style)
    // Show bottom bar on main screens
    val mainRoutes = listOf("home", "nutrition", "training", "coach", "account", "library")
    val showBottomBar = currentRoute in mainRoutes

    // Glassmorphism background for footer - semi-transparent with premium border
    val glassBackground = Color(0xFF1A1A1A).copy(alpha = 0.85f)
    val topBorderAccent = Brush.horizontalGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.12f),
            NayaPrimary.copy(alpha = 0.4f),
            Color.White.copy(alpha = 0.12f),
            Color.Transparent
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Content layer - NO bottom padding so content scrolls BEHIND the glass footer
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            // Home Screen - Smart Hero Card System
            composable(Screen.Home.route) {
                val smartHomeViewModel: SmartHomeViewModel = viewModel()

                SmartHomeScreen(
                    accountViewModel = accountViewModel,
                    smartHomeViewModel = smartHomeViewModel,
                    onStartWorkout = {
                        navController.navigate(Screen.Training.route)
                    },
                    onNavigateToNutrition = { mealType ->
                        if (mealType != null) {
                            navController.navigate("meal_photo_capture?mealType=${mealType.name.lowercase()}")
                        } else {
                            navController.navigate(Screen.Nutrition.route)
                        }
                    },
                    onNavigateToAccount = {
                        navController.navigate(Screen.Account.route)
                    },
                    onNavigateToCommunity = {
                        navController.navigate("community")
                    },
                    onNavigateToMaxOutFriday = {
                        navController.navigate("max_out_friday")
                    },
                    onNavigateToPost = { postId ->
                        navController.navigate("community_post/$postId")
                    },
                    onNavigateToUserProfile = { userId ->
                        navController.navigate("community_profile/$userId")
                    },
                    onNavigateToCreatePost = {
                        navController.navigate("create_post")
                    },
                    onNavigateToPaywall = {
                        // TODO: Navigate to paywall/subscription screen
                        navController.navigate(Screen.Account.route)
                    },
                    onNavigateToMemberDiscovery = {
                        navController.navigate("member_discovery")
                    }
                )
            }

            // Library Screen
            composable(Screen.Library.route) {
                LibraryScreen(
                    paddingValues = PaddingValues(),
                    initialTab = 0,
                    selectionMode = null,
                    workoutBuilderViewModel = workoutBuilderViewModel,
                    onNavigateToCreateExercise = {
                        navController.navigate(Screen.CreateExercise.route)
                    },
                    onNavigateToCreateWorkout = {
                        navController.navigate(Screen.WorkoutBuilder.route)
                    },
                    onNavigateToCreateProgram = {
                        navController.navigate(Screen.CreateProgram.route)
                    },
                    onNavigateToExerciseDetail = { exercise ->
                        navController.navigate(Screen.ExerciseDetail.createRoute(exercise.id))
                    },
                    onNavigateToTraining = {
                        navController.navigate(Screen.Training.route)
                    },
                    onStartWorkout = { workout ->
                        android.util.Log.d("MainActivity", "🚀 Starting workout from Library: ${workout.name}")
                        workoutSessionViewModel.setActiveWorkout(workout)
                        workoutSessionViewModel.setActiveSessionId(java.util.UUID.randomUUID().toString())
                        WorkoutTemplateRepository.saveLastUsedWorkout(workout)
                        navController.navigate(Screen.ActiveSession.route)
                    },
                    onItemSelected = null,
                    onCancelSelection = null
                )
            }

            // Library Selection Screen
            composable(
                route = Screen.LibrarySelection.route,
                arguments = listOf(
                    navArgument("selectionMode") { type = NavType.StringType },
                    navArgument("initialTab") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val selectionMode = backStackEntry.arguments?.getString("selectionMode") ?: "exercises"
                val initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0

                LibraryScreen(
                    paddingValues = PaddingValues(),
                    initialTab = initialTab,
                    selectionMode = selectionMode,
                    workoutBuilderViewModel = workoutBuilderViewModel,
                    onNavigateToCreateExercise = { },
                    onNavigateToCreateWorkout = { },
                    onNavigateToCreateProgram = { },
                    onNavigateToExerciseDetail = { },
                    onItemSelected = { itemName ->
                        workoutBuilderViewModel.addExercise(itemName)
                        navController.popBackStack()
                    },
                    onCancelSelection = {
                        navController.popBackStack()
                    }
                )
            }

            // Workout Builder Screen
            composable(Screen.WorkoutBuilder.route) {
                WorkoutBuilderScreen(
                    viewModel = workoutBuilderViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToExerciseSelection = {
                        navController.navigate(Screen.LibrarySelection.createRoute("exercises", 0))
                    },
                    onSaveAndStartSession = { workout ->
                        // Save workout and start session immediately
                        workoutSessionViewModel.setActiveWorkout(workout)
                        workoutSessionViewModel.setActiveSessionId(java.util.UUID.randomUUID().toString())
                        WorkoutTemplateRepository.saveLastUsedWorkout(workout)
                        navController.navigate(Screen.ActiveSession.route) {
                            popUpTo(Screen.Training.route)
                        }
                    }
                )
            }

            // Training Screen
            composable(Screen.Training.route) {
                TrainingScreen(
                    workoutBuilderViewModel = workoutBuilderViewModel,
                    onWorkoutClick = { workoutId ->
                        android.util.Log.d("MainActivity", "🎯 onWorkoutClick called with workoutId=$workoutId")
                        // Simple flow: clicking workout always goes to ActiveSession
                        // Find the workout and set it as active
                        val clickedWorkout = workoutBuilderViewModel.savedWorkouts.value.find { it.id == workoutId }
                            ?: workoutBuilderViewModel.publicWorkouts.value.find { it.id == workoutId }

                        android.util.Log.d("MainActivity", "🎯 Found workout: ${clickedWorkout?.name ?: "NULL"}")

                        if (clickedWorkout != null) {
                            // Only set active workout if it's different from current
                            if (activeWorkout?.id != workoutId) {
                                workoutSessionViewModel.setActiveWorkout(clickedWorkout)
                                workoutSessionViewModel.setActiveSessionId(java.util.UUID.randomUUID().toString())
                                WorkoutTemplateRepository.saveLastUsedWorkout(clickedWorkout)
                            }
                            android.util.Log.d("MainActivity", "🎯 Navigating to ActiveSession")
                            navController.navigate(Screen.ActiveSession.route)
                        }
                    },
                    onVbtClick = {
                        // Form Check feature removed in NAYA relaunch
                    },
                    onNavigateToPrograms = {
                        navController.navigate(Screen.LibrarySelection.createRoute("programs", 1))
                    },
                    onNavigateToWorkouts = {
                        navController.navigate(Screen.LibrarySelection.createRoute("workouts", 2))
                    },
                    onNavigateToLibrary = { selectionMode ->
                        val initialTab = when (selectionMode) {
                            "exercises" -> 0
                            "programs" -> 1
                            "workouts" -> 2
                            else -> 0
                        }
                        navController.navigate(Screen.LibrarySelection.createRoute(selectionMode, initialTab))
                    },
                    onNavigateToWorkoutBuilder = {
                        navController.navigate(Screen.WorkoutBuilder.route)
                    },
                    onStartSession = { workout ->
                        android.util.Log.d("MainActivity", "🚀 onStartSession called with workout=${workout.name}")
                        workoutSessionViewModel.setActiveWorkout(workout)
                        // Generate session ID - will be created in DB when first set is saved
                        workoutSessionViewModel.setActiveSessionId(java.util.UUID.randomUUID().toString())
                        // 💾 Persist workout for app restart recovery
                        WorkoutTemplateRepository.saveLastUsedWorkout(workout)
                        android.util.Log.d("MainActivity", "🚀 Navigating to ActiveSession")
                        navController.navigate(Screen.ActiveSession.route)
                    },
                    onLoadTemplateAndNavigate = { templateId ->
                        // Load template for customization and navigate to WorkoutBuilder
                        workoutBuilderViewModel.loadTemplateForCustomization(templateId)
                        navController.navigate(Screen.WorkoutBuilder.route)
                    },
                    onNavigateToStatistics = {
                        navController.navigate("training_statistics")
                    },
                    onScanWorkout = {
                        navController.navigate(Screen.WodScanner.route)
                    },
                    onNavigateToMindfulness = {
                        navController.navigate(Screen.Mindfulness.route)
                    }
                )
            }

            // Nutrition Screen
            composable(Screen.Nutrition.route) {
                // Use shared nutritionViewModel from MainScreen level
                com.example.menotracker.screens.nutrition.NutritionScreen(
                    nutritionViewModel = nutritionViewModel,
                    accountViewModel = accountViewModel,
                    onNavigateToMealCapture = { mealType ->
                        navController.navigate("meal_photo_capture?mealType=${mealType.name.lowercase()}")
                    },
                    onNavigateToInsights = {
                        navController.navigate(Screen.NutritionInsights.route)
                    },
                    onNavigateToFoodSearch = { mealType ->
                        // Generate a new mealId for the search session
                        val mealId = java.util.UUID.randomUUID().toString()
                        navController.navigate(Screen.FoodSearch.createRoute(mealType.name.lowercase(), mealId))
                    },
                    onNavigateToFoodSnap = { mealType ->
                        // Direct to MealPhotoCaptureScreen for AI analysis (simple, no detection)
                        navController.navigate("meal_photo_capture?mealType=${mealType.name.lowercase()}")
                    },
                    onNavigateToBarcodeScanner = {
                        // Use dedicated Barcode Scanner (faster, focused)
                        navController.navigate(Screen.BarcodeScanner.createRoute("snack"))
                    }
                )
            }

            // Meal Photo Capture Screen - with mealType navigation argument
            composable(
                route = "meal_photo_capture?mealType={mealType}",
                arguments = listOf(
                    navArgument("mealType") {
                        type = NavType.StringType
                        defaultValue = "lunch"
                    }
                )
            ) { backStackEntry ->
                val mealTypeString = backStackEntry.arguments?.getString("mealType") ?: "lunch"
                val mealType = com.example.menotracker.data.models.MealType.entries.find {
                    it.name.lowercase() == mealTypeString
                } ?: com.example.menotracker.data.models.MealType.LUNCH

                // Use shared nutritionViewModel from MainScreen level
                com.example.menotracker.screens.nutrition.MealPhotoCaptureScreen(
                    onNavigateBack = {
                        // Navigate directly to Nutrition instead of popBackStack
                        navController.navigate(Screen.Nutrition.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onPhotoAnalyzed = {
                        navController.navigate(Screen.MealAnalysisResult.route) {
                            popUpTo("meal_photo_capture?mealType={mealType}") { inclusive = true }
                        }
                    },
                    initialMealType = mealType,
                    nutritionViewModel = nutritionViewModel
                )
            }

            // Meal Analysis Result Screen
            composable(Screen.MealAnalysisResult.route) {
                // Use shared nutritionViewModel from MainScreen level
                val userId = accountViewModel.userProfile.value?.id ?: "00000000-0000-0000-0000-000000000000"

                com.example.menotracker.screens.nutrition.MealAnalysisResultScreen(
                    userId = userId,
                    onNavigateBack = {
                        // Navigate directly to Nutrition instead of popBackStack
                        navController.navigate(Screen.Nutrition.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onSaveComplete = {
                        // Navigate directly to Nutrition instead of popBackStack
                        navController.navigate(Screen.Nutrition.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    nutritionViewModel = nutritionViewModel
                )
            }

            // Nutrition Insights Screen
            composable(Screen.NutritionInsights.route) {
                val log by nutritionViewModel.nutritionLog.collectAsState()
                val goal by nutritionViewModel.nutritionGoal.collectAsState()

                com.example.menotracker.screens.nutrition.NutritionInsightsScreen(
                    logs = listOfNotNull(log),  // Convert single log to list
                    goal = goal,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Food Search Screen
            composable(
                route = Screen.FoodSearch.route,
                arguments = listOf(
                    navArgument("mealType") { type = NavType.StringType },
                    navArgument("mealId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val mealTypeString = backStackEntry.arguments?.getString("mealType") ?: "lunch"
                val mealId = backStackEntry.arguments?.getString("mealId") ?: ""
                val mealType = com.example.menotracker.data.models.MealType.entries.find {
                    it.name.lowercase() == mealTypeString
                } ?: com.example.menotracker.data.models.MealType.LUNCH

                val userId = accountViewModel.userProfile.value?.id ?: ""

                com.example.menotracker.screens.nutrition.FoodSearchScreen(
                    mealType = mealType,
                    mealId = mealId,
                    onFoodSelected = { mealItem ->
                        // Add the selected food to the nutrition log
                        nutritionViewModel.addMealItemToLog(userId, mealType, mealItem)
                        // Navigate back to nutrition
                        navController.navigate(Screen.Nutrition.route) {
                            popUpTo(Screen.Nutrition.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onBarcodeScan = {
                        // Use Smart Food Camera - auto-detects barcode, label, or prepared food
                        navController.navigate(Screen.SmartFoodCamera.createRoute(mealType.name.lowercase()))
                    },
                    userId = userId  // Pass userId for personal food library
                )
            }

            // Barcode Scanner Screen
            composable(
                route = Screen.BarcodeScanner.route,
                arguments = listOf(
                    navArgument("mealType") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val mealTypeString = backStackEntry.arguments?.getString("mealType") ?: "snack"
                val mealType = com.example.menotracker.data.models.MealType.entries.find {
                    it.name.lowercase() == mealTypeString
                } ?: com.example.menotracker.data.models.MealType.SNACK

                val userId = accountViewModel.userProfile.value?.id ?: ""

                com.example.menotracker.screens.nutrition.BarcodeScannerScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onProductFound = { food ->
                        // Product found callback (optional logging)
                        android.util.Log.d("MainActivity", "Product found: ${food.name}")
                    },
                    onAddToMeal = { food, portionGrams, selectedMealType ->
                        // Convert NayaFood to MealItem and add to log
                        val multiplier = portionGrams / 100f
                        val mealItem = com.example.menotracker.data.models.MealItem(
                            id = java.util.UUID.randomUUID().toString(),
                            mealId = "", // Will be set by repository
                            itemName = food.name,
                            quantity = portionGrams,
                            quantityUnit = "g",
                            calories = (food.calories * multiplier),
                            protein = food.protein * multiplier,
                            carbs = food.carbs * multiplier,
                            fat = food.fat * multiplier,
                            fiber = food.fiber * multiplier,
                            sugar = food.sugar * multiplier,
                            sodium = food.sodium * multiplier
                        )
                        nutritionViewModel.addMealItemToLog(userId, selectedMealType, mealItem)
                        // Navigate back to nutrition
                        navController.navigate(Screen.Nutrition.route) {
                            popUpTo(Screen.Nutrition.route) { inclusive = true }
                        }
                    },
                    onAddAndContinue = { food, portionGrams, selectedMealType ->
                        // Add item but stay on scanner for more ingredients (shake building)
                        val multiplier = portionGrams / 100f
                        val mealItem = com.example.menotracker.data.models.MealItem(
                            id = java.util.UUID.randomUUID().toString(),
                            mealId = "",
                            itemName = food.name,
                            quantity = portionGrams,
                            quantityUnit = "g",
                            calories = (food.calories * multiplier),
                            protein = food.protein * multiplier,
                            carbs = food.carbs * multiplier,
                            fat = food.fat * multiplier,
                            fiber = food.fiber * multiplier,
                            sugar = food.sugar * multiplier,
                            sodium = food.sodium * multiplier
                        )
                        nutritionViewModel.addMealItemToLog(userId, selectedMealType, mealItem)
                        // Stay on scanner screen for next ingredient
                    },
                    initialMealType = mealType
                )
            }

            // Smart Food Camera Screen - Unified barcode/label/AI detection
            composable(
                route = Screen.SmartFoodCamera.route,
                arguments = listOf(
                    navArgument("mealType") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val mealTypeString = backStackEntry.arguments?.getString("mealType") ?: "snack"
                val mealType = com.example.menotracker.data.models.MealType.entries.find {
                    it.name.lowercase() == mealTypeString
                } ?: com.example.menotracker.data.models.MealType.SNACK

                val userId = accountViewModel.userProfile.value?.id ?: ""

                com.example.menotracker.screens.nutrition.SmartFoodCameraScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onProductFound = { food ->
                        // Product found via barcode - add to meal
                        val mealItem = com.example.menotracker.data.models.MealItem(
                            id = java.util.UUID.randomUUID().toString(),
                            mealId = "",
                            itemName = food.name,
                            quantity = 100f,
                            quantityUnit = "g",
                            calories = food.calories,
                            protein = food.protein,
                            carbs = food.carbs,
                            fat = food.fat,
                            fiber = food.fiber,
                            sugar = food.sugar,
                            sodium = food.sodium
                        )
                        nutritionViewModel.addMealItemToLog(userId, mealType, mealItem)
                        navController.navigate(Screen.Nutrition.route) {
                            popUpTo(Screen.Nutrition.route) { inclusive = true }
                        }
                    },
                    onNeedAIAnalysis = { photoFile ->
                        // Route to AI analysis flow via MealPhotoCaptureScreen
                        // Navigate to photo capture which handles the AI analysis
                        navController.navigate("meal_photo_capture?mealType=${mealType.name.lowercase()}") {
                            popUpTo(Screen.SmartFoodCamera.route) { inclusive = true }
                        }
                    },
                    onLabelNeedsReview = { bitmap, parsedNutrition ->
                        // Label parsed - create food item from parsed data
                        val mealItem = com.example.menotracker.data.models.MealItem(
                            id = java.util.UUID.randomUUID().toString(),
                            mealId = "",
                            itemName = "Scanned Product",
                            quantity = 100f,
                            quantityUnit = "g",
                            calories = parsedNutrition.calories?.toFloat() ?: 0f,
                            protein = parsedNutrition.protein ?: 0f,
                            carbs = parsedNutrition.carbs ?: 0f,
                            fat = parsedNutrition.fat ?: 0f,
                            fiber = parsedNutrition.fiber ?: 0f,
                            sugar = parsedNutrition.sugar ?: 0f,
                            sodium = parsedNutrition.sodium ?: 0f
                        )
                        nutritionViewModel.addMealItemToLog(userId, mealType, mealItem)
                        navController.navigate(Screen.Nutrition.route) {
                            popUpTo(Screen.Nutrition.route) { inclusive = true }
                        }
                    },
                    initialMealType = mealType
                )
            }

            // Coach Screen (Unified with AI Coach + Physical Coach tabs)
            composable(Screen.Coach.route) {
                CoachScreen(
                    aiCoachViewModel = aiCoachViewModel,
                    physicalCoachViewModel = physicalCoachViewModel,
                    onNavigateBack = {
                        navController.navigate(Screen.Home.route) { // Go to Home from Coach
                            popUpTo(navController.graph.startDestinationId)
                        }
                    },
                    onStartWorkout = { templateId ->
                        // Navigate to workout detail where user can review and start it
                        navController.navigate(Screen.WorkoutDetail.createRoute(templateId))
                    }
                )
            }

            // Account Screen
            composable(Screen.Account.route) {
                AccountScreen(
                    viewModel = accountViewModel,
                    authViewModel = authViewModel,
                    nutritionViewModel = nutritionViewModel,
                    isDarkMode = isDarkMode,
                    onThemeChange = onThemeChange,
                    onLogout = onLogout,
                    onNavigateToAdmin = {
                        navController.navigate(Screen.AdminContent.route)
                    }
                )
            }

            // Campus Screen
            composable(Screen.Campus.route) {
                CampusScreen()
            }

            // Admin Content Screen (only for admins)
            composable(Screen.AdminContent.route) {
                AdminContentScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToWorkoutBuilder = {
                        navController.navigate(Screen.WorkoutBuilder.route)
                    }
                )
            }

            // Training Statistics Screen
            composable("training_statistics") {
                TrainingStatisticsScreen(
                    accountViewModel = accountViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Naya Lab Screen
            composable(Screen.Lab.route) {
                NayaLabScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // ═══════════════════════════════════════════════════════════════
            // BREATHING EXERCISES SCREENS
            // ═══════════════════════════════════════════════════════════════

            // Breathing Exercises List
            composable(Screen.Breathing.route) {
                val userId = accountViewModel.userProfile.value?.id ?: ""
                BreathingScreen(
                    userId = userId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToSession = { exerciseType ->
                        navController.navigate(Screen.BreathingSession.createRoute(exerciseType))
                    },
                    onNavigateToPaywall = {
                        navController.navigate("paywall")
                    }
                )
            }

            // Active Breathing Session
            composable(
                route = Screen.BreathingSession.route,
                arguments = listOf(
                    navArgument("exerciseType") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = accountViewModel.userProfile.value?.id ?: ""
                val exerciseTypeName = backStackEntry.arguments?.getString("exerciseType") ?: ""
                val exerciseType = try {
                    BreathingExerciseType.valueOf(exerciseTypeName)
                } catch (e: IllegalArgumentException) {
                    BreathingExerciseType.RELAXATION_478 // Default fallback
                }

                BreathingSessionScreen(
                    exerciseType = exerciseType,
                    userId = userId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSessionComplete = {
                        navController.popBackStack()
                    }
                )
            }

            // ═══════════════════════════════════════════════════════════════
            // MINDFULNESS HUB & MEDITATION SCREENS
            // ═══════════════════════════════════════════════════════════════

            // Mindfulness Hub (Breathing + Meditation combined entry)
            composable(Screen.Mindfulness.route) {
                val userId = accountViewModel.userProfile.value?.id ?: ""
                MindfulnessScreen(
                    userId = userId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToBreathing = {
                        navController.navigate(Screen.Breathing.route)
                    },
                    onNavigateToBreathingSession = { exerciseType ->
                        navController.navigate(Screen.BreathingSession.createRoute(exerciseType))
                    },
                    onNavigateToMeditation = {
                        navController.navigate(Screen.Meditation.route)
                    },
                    onNavigateToMeditationSession = { meditationType ->
                        navController.navigate(Screen.MeditationSession.createRoute(meditationType))
                    },
                    onNavigateToSoundscape = {
                        navController.navigate(Screen.Soundscape.route)
                    },
                    onNavigateToPaywall = {
                        navController.navigate("paywall")
                    }
                )
            }

            // Meditation List Screen
            composable(Screen.Meditation.route) {
                val userId = accountViewModel.userProfile.value?.id ?: ""
                MeditationScreen(
                    userId = userId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToSession = { meditationType ->
                        navController.navigate(Screen.MeditationSession.createRoute(meditationType))
                    },
                    onNavigateToSoundscape = {
                        navController.navigate(Screen.Soundscape.route)
                    },
                    onNavigateToPaywall = {
                        navController.navigate("paywall")
                    }
                )
            }

            // Active Meditation Session
            composable(
                route = Screen.MeditationSession.route,
                arguments = listOf(
                    navArgument("meditationType") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = accountViewModel.userProfile.value?.id ?: ""
                val meditationTypeName = backStackEntry.arguments?.getString("meditationType") ?: ""
                val meditationType = try {
                    MeditationType.valueOf(meditationTypeName)
                } catch (e: IllegalArgumentException) {
                    MeditationType.BODY_SCAN // Default fallback
                }

                MeditationSessionScreen(
                    meditationType = meditationType,
                    userId = userId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onSessionComplete = {
                        navController.popBackStack()
                    }
                )
            }

            // Soundscape Mixer
            composable(Screen.Soundscape.route) {
                val userId = accountViewModel.userProfile.value?.id ?: ""
                SoundscapeScreen(
                    userId = userId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // ═══════════════════════════════════════════════════════════════
            // COMMUNITY SCREENS
            // ═══════════════════════════════════════════════════════════════

            // Community Main Screen
            composable("community") {
                val currentUserId = accountViewModel.userProfile.value?.id ?: ""
                CommunityScreen(
                    userId = currentUserId,
                    onNavigateToProfile = { userId ->
                        navController.navigate("community_profile/$userId")
                    },
                    onNavigateToPost = { postId ->
                        navController.navigate("community_post/$postId")
                    },
                    onNavigateToChallenge = { challengeId ->
                        navController.navigate("community_challenge/$challengeId")
                    },
                    onNavigateToMaxOutFriday = {
                        navController.navigate("max_out_friday")
                    },
                    onNavigateToCreateChallenge = {
                        navController.navigate("create_challenge")
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Member Discovery Screen
            composable("member_discovery") {
                val currentUserId = accountViewModel.userProfile.value?.id ?: ""
                MemberDiscoveryScreen(
                    currentUserId = currentUserId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProfile = { userId ->
                        navController.navigate("community_profile/$userId")
                    }
                )
            }

            // Create Post Screen
            composable("create_post") {
                val currentUserId = accountViewModel.userProfile.value?.id ?: ""
                val userProfileImageUrl = accountViewModel.userProfile.value?.profileImageUrl
                CreatePostScreen(
                    currentUserId = currentUserId,
                    userProfileImageUrl = userProfileImageUrl,
                    onNavigateBack = { navController.popBackStack() },
                    onPostCreated = {
                        // Navigate back to home after post is created
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            // Max Out Friday Screen
            composable("max_out_friday") {
                MaxOutFridayScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onUserClick = { userId ->
                        navController.navigate("community_profile/$userId")
                    },
                    onSubmitEntry = {
                        // Navigate to workout for submitting an entry
                        navController.navigate("training")
                    }
                )
            }

            // Create Challenge Screen
            composable("create_challenge") {
                CreateChallengeScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onChallengeCreated = { challengeId ->
                        navController.navigate("community_challenge/$challengeId") {
                            popUpTo("community") { inclusive = false }
                        }
                    },
                    onInviteUsers = {
                        // TODO: Navigate to user picker screen
                    }
                )
            }

            // Community Profile Screen
            composable(
                route = "community_profile/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val currentUserId = accountViewModel.userProfile.value?.id ?: ""
                UserProfileScreen(
                    userId = userId,
                    currentUserId = currentUserId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToFollowers = { uid ->
                        navController.navigate("community_followers/$uid/true")
                    },
                    onNavigateToFollowing = { uid ->
                        navController.navigate("community_followers/$uid/false")
                    },
                    onNavigateToPost = { postId ->
                        navController.navigate("community_post/$postId")
                    }
                )
            }

            // Community Post Detail Screen
            composable(
                route = "community_post/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: ""
                val currentUserId = accountViewModel.userProfile.value?.id ?: ""
                PostDetailScreen(
                    postId = postId,
                    currentUserId = currentUserId,
                    onNavigateBack = { navController.popBackStack() },
                    onUserClick = { userId ->
                        navController.navigate("community_profile/$userId")
                    }
                )
            }

            // Community Challenge Detail Screen
            composable(
                route = "community_challenge/{challengeId}",
                arguments = listOf(navArgument("challengeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val challengeId = backStackEntry.arguments?.getString("challengeId") ?: ""
                ChallengeDetailScreen(
                    challengeId = challengeId,
                    onNavigateBack = { navController.popBackStack() },
                    onUserClick = { userId ->
                        navController.navigate("community_profile/$userId")
                    }
                )
            }

            // Community Followers/Following Screen
            composable(
                route = "community_followers/{userId}/{showFollowers}",
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("showFollowers") { type = NavType.BoolType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val showFollowers = backStackEntry.arguments?.getBoolean("showFollowers") ?: true
                val currentUserId = accountViewModel.userProfile.value?.id ?: ""
                FollowersScreen(
                    userId = userId,
                    currentUserId = currentUserId,
                    showFollowers = showFollowers,
                    onNavigateBack = { navController.popBackStack() },
                    onUserClick = { uid ->
                        navController.navigate("community_profile/$uid")
                    }
                )
            }

            // ═══════════════════════════════════════════════════════════════
            // WOD (CROSSFIT WORKOUT OF THE DAY) SCREENS
            // ═══════════════════════════════════════════════════════════════

            // WOD Scanner Screen - Camera capture for whiteboard photos
            composable(Screen.WodScanner.route) {
                val userId = accountViewModel.userProfile.value?.id ?: ""
                WodScannerScreen(
                    userId = userId,
                    boxName = null, // Could be passed from user profile if stored
                    onNavigateBack = { navController.popBackStack() },
                    onWodSaved = { wodTemplateId ->
                        // Navigate to WOD detail after saving
                        navController.navigate(Screen.WodDetail.createRoute(wodTemplateId)) {
                            popUpTo(Screen.WodScanner.route) { inclusive = true }
                        }
                    }
                )
            }

            // WOD Library Screen - Browse and manage saved WODs
            composable(Screen.WodLibrary.route) {
                val userId = accountViewModel.userProfile.value?.id ?: ""
                WodLibraryScreen(
                    userId = userId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToScanner = {
                        navController.navigate(Screen.WodScanner.route)
                    },
                    onWodClick = { wodId ->
                        navController.navigate(Screen.WodDetail.createRoute(wodId))
                    }
                )
            }

            // WOD Detail Screen - View WOD details and log scores
            composable(
                route = Screen.WodDetail.route,
                arguments = listOf(
                    navArgument("wodId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val wodId = backStackEntry.arguments?.getString("wodId") ?: ""
                val userId = accountViewModel.userProfile.value?.id ?: ""
                WodDetailScreen(
                    wodId = wodId,
                    userId = userId,
                    onNavigateBack = { navController.popBackStack() },
                    onStartWorkout = { workoutTemplate ->
                        // WOD has been converted to WorkoutTemplate - start the session
                        workoutSessionViewModel.setActiveWorkout(workoutTemplate)
                        workoutSessionViewModel.setActiveSessionId(java.util.UUID.randomUUID().toString())
                        // 💾 Persist WOD workout for app restart recovery
                        WorkoutTemplateRepository.saveLastUsedWorkout(workoutTemplate)
                        android.util.Log.d("MainActivity", "Starting WOD workout: ${workoutTemplate.name} with ${workoutTemplate.exercises.size} exercises")
                        navController.navigate(Screen.ActiveSession.route) {
                            // Don't pop the WOD detail, so user can come back to log score
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Form Check feature removed in NAYA relaunch

            // Video Player feature removed in NAYA relaunch

            // Create Exercise Screen
            composable(Screen.CreateExercise.route) {
                CreateExerciseScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onExerciseSaved = { exerciseTemplate ->
                        scope.launch {
                            // Get current user's ID
                            val userId = accountViewModel.userProfile.value?.id

                            val exercise = Exercise(
                                id = UUID.randomUUID().toString(),
                                name = exerciseTemplate.name,
                                mainMuscle = exerciseTemplate.primaryMuscle,
                                secondaryMuscles = exerciseTemplate.secondaryMuscles,
                                equipment = listOf(exerciseTemplate.equipment),
                                tempo = exerciseTemplate.tempo,
                                notes = exerciseTemplate.notes,
                                trackReps = exerciseTemplate.trackingType.name.contains("REPS"),
                                trackWeight = exerciseTemplate.trackingType.name.contains("WEIGHT"),
                                trackDuration = exerciseTemplate.trackingType.name.contains("DURATION"),
                                trackDistance = exerciseTemplate.trackingType.name.contains("DISTANCE"),
                                supportsPowerScore = false, // User exercises don't support VBT
                                supportsTechniqueScore = false, // User exercises don't support VBT
                                ownerId = userId // ✅ Set owner_id to current user
                            )

                            ExerciseRepository.saveExercise(exercise)
                                .onSuccess {
                                    android.util.Log.d("MainActivity", "✅ Exercise saved: ${exercise.name} (owner: $userId)")
                                }
                                .onFailure { error ->
                                    android.util.Log.e("MainActivity", "❌ Failed to save exercise: ${error.message}")
                                }
                        }
                        navController.popBackStack()
                    }
                )
            }

            // Create Program Screen
            composable(Screen.CreateProgram.route) {
                CreateProgramScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Active Workout Session Screen
            composable(Screen.ActiveSession.route) {
                android.util.Log.d("MainActivity", "📍 ActiveSession composable - activeWorkout=${activeWorkout?.name ?: "NULL"}")

                // Safety check: if activeWorkout is null, navigate back to Training
                // This prevents the screen from being empty if ViewModel state was lost
                if (activeWorkout == null) {
                    LaunchedEffect(Unit) {
                        android.util.Log.w("MainActivity", "⚠️ ActiveSession - activeWorkout is NULL, navigating back to Training")
                        navController.navigate(Screen.Training.route) {
                            popUpTo(Screen.Training.route) { inclusive = true }
                        }
                    }
                    return@composable
                }

                val workout = activeWorkout
                if (workout != null) {
                    ActiveWorkoutSessionScreen(
                        workout = workout,
                        workoutSessionViewModel = workoutSessionViewModel,
                        workoutTimerViewModel = workoutTimerViewModel,
                        restTimerViewModel = restTimerViewModel,
                        accountViewModel = accountViewModel,
                        onNavigateBack = { totalTimeSeconds ->
                            // TODO: Save workout session with total time
                            android.util.Log.d("MainActivity", "Workout finished in $totalTimeSeconds seconds")
                            workoutSessionViewModel.setActiveWorkout(null)
                            workoutSessionViewModel.setActiveSessionId(null)
                            // Note: DON'T clear persisted workout - user may want to use it again
                            // Template stays until user explicitly selects a different one
                            navController.popBackStack()
                        },
                        onNavigateToVBT = { exerciseIdParam, setNumber ->
                            // Navigate with workout context
                            val exercise = workout.exercises.find { it.exerciseId == exerciseIdParam }
                            val exerciseName = exercise?.exerciseName ?: "Exercise"
                            val exerciseId = exerciseIdParam
                            val sessionId = activeWorkoutSessionId ?: java.util.UUID.randomUUID().toString()

                            // Store session ID if not set
                            if (activeWorkoutSessionId == null) {
                                workoutSessionViewModel.setActiveSessionId(sessionId)
                            }

                            // setId uses workout.id for local UI state mapping (matches setDataMap keys)
                            // sessionId is passed separately for database operations
                            val setId = "${workout.id}_${exerciseId}_set${setNumber}"

                            // 🆕 VBT Auto-Regulation: Get load and reference weight
                            val setData = workoutSessionViewModel.setDataMap.value[setId]
                            val loadKg = setData?.completedWeight
                                ?: exercise?.sets?.getOrNull(setNumber - 1)?.targetWeight
                                ?: 0.0

                            // Reference weight hierarchy:
                            // 1. Top-set target from template (highest weight in this exercise)
                            // 2. Strength profile estimated working weight (85% of 1RM)
                            // 3. Fallback to loadKg itself
                            val templateTopSet = exercise?.sets?.maxByOrNull { it.targetWeight }?.targetWeight ?: 0.0
                            val strengthProfile = accountViewModel.strengthProfile.value
                            val profile1RM = strengthProfile?.let {
                                com.example.menotracker.data.models.ExercisePercentages
                                    .getExercisePercentage(exerciseName)?.let { relation ->
                                        when (relation.baseLift) {
                                            com.example.menotracker.data.models.LiftType.SQUAT -> it.currentSquatKg
                                            com.example.menotracker.data.models.LiftType.BENCH -> it.currentBenchKg
                                            com.example.menotracker.data.models.LiftType.DEADLIFT -> it.currentDeadliftKg
                                            com.example.menotracker.data.models.LiftType.OVERHEAD -> it.currentOverheadKg ?: (it.currentBenchKg * 0.65f)
                                        } * relation.percentage
                                    }
                            } ?: 0f
                            val profileWorkingWeight = profile1RM * 0.85f // Working sets typically at 85%

                            val referenceKg = when {
                                templateTopSet > 0 -> templateTopSet
                                profileWorkingWeight > 0 -> profileWorkingWeight.toDouble()
                                else -> loadKg
                            }

                            // Form Check feature removed in NAYA relaunch
                        },
                        onNavigateToVideoPlayer = { _, _ ->
                            // Video Player feature removed in NAYA relaunch
                        }
                    )
                }
            }

            // Exercise Detail Screen
            composable(
                route = Screen.ExerciseDetail.route,
                arguments = listOf(
                    navArgument("exerciseId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""

                // Load exercise from repository
                var exercise by remember { mutableStateOf<Exercise?>(null) }

                LaunchedEffect(exerciseId) {
                    ExerciseRepository.getExerciseById(exerciseId)
                        .onSuccess { loadedExercise ->
                            exercise = loadedExercise
                        }
                        .onFailure { error ->
                            android.util.Log.e("MainActivity", "❌ Failed to load exercise: ${error.message}")
                        }
                }

                exercise?.let {
                    ExerciseDetailScreen(
                        exercise = it,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                } ?: run {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NayaPrimary)
                    }
                }
            }

            // Workout Detail Screen
            composable(
                route = Screen.WorkoutDetail.route,
                arguments = listOf(
                    navArgument("workoutId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getString("workoutId") ?: ""
                android.util.Log.d("MainActivity", "🏋️ WorkoutDetailScreen composable - workoutId=$workoutId")

                // Get workout templates from ViewModel
                val savedWorkouts by workoutBuilderViewModel.savedWorkouts.collectAsState()
                val publicWorkouts by workoutBuilderViewModel.publicWorkouts.collectAsState()

                // Find the workout by ID (check both saved and public)
                val workout = remember(workoutId, savedWorkouts, publicWorkouts) {
                    savedWorkouts.find { it.id == workoutId }
                        ?: publicWorkouts.find { it.id == workoutId }
                }

                // State for tracking exercise being swapped
                var swappingExerciseId by remember { mutableStateOf<String?>(null) }

                // Local copy of workout for editing
                var editableWorkout by remember(workout) { mutableStateOf(workout) }

                WorkoutDetailScreen(
                    workout = editableWorkout,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onStartWorkout = { workoutToStart ->
                        workoutSessionViewModel.setActiveWorkout(workoutToStart)
                        workoutSessionViewModel.setActiveSessionId(java.util.UUID.randomUUID().toString())
                        // 💾 Persist workout for app restart recovery
                        WorkoutTemplateRepository.saveLastUsedWorkout(workoutToStart)
                        navController.navigate(Screen.ActiveSession.route) {
                            popUpTo(Screen.Training.route)
                        }
                    },
                    onSwapExercise = { exerciseId ->
                        // Store which exercise we're swapping
                        swappingExerciseId = exerciseId
                        // Navigate to library in selection mode
                        navController.navigate(Screen.LibrarySelection.createRoute("swap_exercise", 0))
                    },
                    onUpdateExercise = { exerciseId, newSets ->
                        // Update the local workout state with new sets
                        editableWorkout = editableWorkout?.copy(
                            exercises = editableWorkout?.exercises?.map { exercise ->
                                if (exercise.exerciseId == exerciseId) {
                                    exercise.copy(sets = newSets)
                                } else {
                                    exercise
                                }
                            } ?: emptyList()
                        )
                        // Also update in ViewModel for persistence
                        workoutBuilderViewModel.updateExerciseSets(exerciseId, newSets)
                    }
                )
            }
        }

        // Bottom Navigation Bar - Prometheus style (5 tabs)
        if (showBottomBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(glassBackground)
                    .border(
                        width = 1.dp,
                        brush = topBorderAccent,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Tab
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        NavBarItem(
                            icon = Icons.Default.Home,
                            label = "Home",
                            selected = currentRoute == Screen.Home.route,
                            onClick = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    // Nutrition Tab
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        NavBarItem(
                            icon = Icons.Default.Restaurant,
                            label = "Nutrition",
                            selected = currentRoute == Screen.Nutrition.route,
                            onClick = {
                                navController.navigate(Screen.Nutrition.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    // Training Tab
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        NavBarItem(
                            icon = Icons.Default.FitnessCenter,
                            label = "Training",
                            selected = currentRoute == Screen.Training.route,
                            onClick = {
                                navController.navigate(Screen.Training.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    // Coach Tab
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        NavBarItem(
                            icon = Icons.Default.Psychology,
                            label = "Coach",
                            selected = currentRoute == Screen.Coach.route,
                            onClick = {
                                navController.navigate(Screen.Coach.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    // Profile Tab
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        NavBarItem(
                            icon = Icons.Default.Person,
                            label = "Profile",
                            selected = currentRoute == Screen.Account.route,
                            onClick = {
                                navController.navigate(Screen.Account.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// Navigation Bar Item Component
@Composable
private fun NavBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) NayaPrimary else Color.Gray

    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = color
        )
    }
}


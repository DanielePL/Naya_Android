package com.example.menotracker.screens.account

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.menotracker.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.draw.rotate
import com.example.menotracker.data.models.BodyMeasurement
import com.example.menotracker.data.models.MenopauseProfile
import com.example.menotracker.data.models.HealthDocument
import com.example.menotracker.data.repository.HealthDocumentRepository
import com.example.menotracker.data.repository.MenopauseProfileRepository
import com.example.menotracker.ui.theme.AppBackground
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.ui.theme.NayaProtein
import com.example.menotracker.ui.theme.NayaCarbs
import com.example.menotracker.ui.theme.NayaFat
import com.example.menotracker.ui.theme.glassCardAccent
import java.io.File

// Design System - Using Theme Colors
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow

// Upload Status
sealed class UploadStatus {
    object Uploading : UploadStatus()
    object Success : UploadStatus()
    data class Error(val message: String) : UploadStatus()
}

@Composable
fun AccountScreen(
    viewModel: com.example.menotracker.viewmodels.AccountViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    authViewModel: com.example.menotracker.viewmodels.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    nutritionViewModel: com.example.menotracker.viewmodels.NutritionViewModel? = null,
    isDarkMode: Boolean = true,
    onThemeChange: (Boolean) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var showBodyStatsDialog by remember { mutableStateOf(false) }
    var showNameEditDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showUnitsDialog by remember { mutableStateOf(false) }
    var showWorkoutDurationDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showGuestUploadDialog by remember { mutableStateOf(false) }
    var showMeasurementsDialog by remember { mutableStateOf(false) }
    var measurementsExpanded by remember { mutableStateOf(false) }
    var showReferralCodeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showMenopauseProfileDialog by remember { mutableStateOf(false) }
    var showDocumentUploadDialog by remember { mutableStateOf(false) }

    // Menopause Profile state
    val menopauseProfile by MenopauseProfileRepository.profile.collectAsState()

    // Health Documents state
    val healthDocuments by HealthDocumentRepository.documents.collectAsState()
    val documentsLoading by HealthDocumentRepository.isLoading.collectAsState()

    // Nutrition state
    val nutritionGoal = nutritionViewModel?.nutritionGoal?.collectAsState()?.value

    // Body Measurements state
    val latestMeasurement by viewModel.latestMeasurement.collectAsState()


    // Referral Code state
    val referralCodeStatus by viewModel.referralCodeStatus.collectAsState()
    val currentPartnerName by viewModel.currentPartnerName.collectAsState()

    val userProfile by viewModel.userProfile.collectAsState()

    // Load body measurements, referral status, menopause profile and health documents on first composition
    LaunchedEffect(userProfile?.id) {
        viewModel.loadBodyMeasurements()
        viewModel.checkReferralCodeStatus()
        // Load menopause profile and health documents
        userProfile?.id?.let { userId ->
            nutritionViewModel?.loadTodayData(userId)
            MenopauseProfileRepository.getProfile(userId)
            HealthDocumentRepository.getDocuments(userId)
        }
    }
    val isLoading by viewModel.isLoading.collectAsState()
    val preferredSports by viewModel.preferredSports.collectAsState()
    val uploadStatus by viewModel.uploadStatus.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // User Data from profile
    val userName = userProfile?.name ?: "Champion"
    val userWeight = userProfile?.weight?.toString() ?: ""
    val userHeight = userProfile?.height?.toString() ?: ""
    val userYears = userProfile?.trainingExperience?.toString() ?: ""

    // Settings - Persisted in DataStore via ViewModel
    val isMetric by viewModel.isMetric.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val currentLanguage by viewModel.language.collectAsState()

    // Image Picker Launcher
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri ->
            try {
                android.util.Log.d("AccountScreen", "📷 Image selected: $imageUri")

                // Create a temporary file from URI
                val inputStream = context.contentResolver.openInputStream(imageUri)
                if (inputStream == null) {
                    android.util.Log.e("AccountScreen", "❌ Failed to open input stream for URI: $imageUri")
                    return@let
                }

                // Get the file extension from the URI
                val extension = context.contentResolver.getType(imageUri)?.let { mimeType ->
                    when (mimeType) {
                        "image/jpeg", "image/jpg" -> "jpg"
                        "image/png" -> "png"
                        "image/webp" -> "webp"
                        else -> "jpg"
                    }
                } ?: "jpg"

                val tempFile = File.createTempFile("profile_image", ".$extension", context.cacheDir)
                android.util.Log.d("AccountScreen", "📁 Temp file created: ${tempFile.absolutePath}")

                tempFile.outputStream().use { outputStream ->
                    val bytes = inputStream.copyTo(outputStream)
                    android.util.Log.d("AccountScreen", "✅ Copied $bytes bytes to temp file")
                }
                inputStream.close()

                // Upload the image
                android.util.Log.d("AccountScreen", "📤 Starting upload...")
                viewModel.uploadProfileImage(tempFile)

                // Clean up temp file after a delay to ensure upload completes
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (tempFile.exists()) {
                        tempFile.delete()
                        android.util.Log.d("AccountScreen", "🗑️ Temp file deleted")
                    }
                }, 2000)
            } catch (e: Exception) {
                android.util.Log.e("AccountScreen", "❌ Error processing image: ${e.message}", e)
            }
        } ?: run {
            android.util.Log.e("AccountScreen", "❌ No image selected")
        }
    }

    // Show snackbar when upload status changes
    val uploadSuccessMessage = stringResource(R.string.upload_success)
    val uploadErrorFormat = stringResource(R.string.upload_error_format)
    LaunchedEffect(uploadStatus) {
        uploadStatus?.let { status ->
            when (status) {
                is UploadStatus.Success -> {
                    snackbarHostState.showSnackbar(
                        message = uploadSuccessMessage,
                        duration = SnackbarDuration.Short
                    )
                    viewModel.clearUploadStatus()
                }
                is UploadStatus.Error -> {
                    snackbarHostState.showSnackbar(
                        message = String.format(uploadErrorFormat, status.message),
                        duration = SnackbarDuration.Long
                    )
                    viewModel.clearUploadStatus()
                }
                UploadStatus.Uploading -> { /* Show loading in UI */ }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        AppBackground {
            LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(Modifier.height(16.dp)) }

            // Profile Header
            item {
                // Check if user is in guest mode
                val isGuest = userProfile?.id == "00000000-0000-0000-0000-000000000000"

                ProfileHeader(
                    userName = userName,
                    profileImageUrl = userProfile?.profileImageUrl,
                    isGuest = isGuest,
                    onEditClick = { showNameEditDialog = true },
                    onUploadImageClick = {
                        if (isGuest) {
                            showGuestUploadDialog = true
                        } else {
                            imagePickerLauncher.launch("image/*")
                        }
                    }
                )
            }

            // Menopause Profile Card
            item {
                MenopauseProfileCard(
                    profile = menopauseProfile,
                    onEditClick = { showMenopauseProfileDialog = true }
                )
            }

            // Health Documents Card
            item {
                HealthDocumentsCard(
                    documents = healthDocuments,
                    isLoading = documentsLoading,
                    onAddClick = { showDocumentUploadDialog = true },
                    onDocumentClick = { document ->
                        // TODO: Open document preview
                        android.util.Log.d("AccountScreen", "Document clicked: ${document.title}")
                    }
                )
            }

            // Body Stats Card
            item {
                BodyStatsCard(
                    weight = userWeight,
                    height = userHeight,
                    years = userYears,
                    latestMeasurement = latestMeasurement,
                    isExpanded = measurementsExpanded,
                    onExpandToggle = { measurementsExpanded = !measurementsExpanded },
                    onEditClick = { showBodyStatsDialog = true },
                    onTrackMeasurementsClick = { showMeasurementsDialog = true }
                )
            }

            // Nutrition Goals Card (read-only display, edit via Nutrition Screen)
            if (nutritionViewModel != null) {
                item {
                    NutritionGoalsCard(
                        nutritionGoal = nutritionGoal,
                        userProfile = userProfile,
                        onAutoCalculate = {
                            // Auto-calculate based on profile and current goal type
                            val goalType = nutritionGoal?.goalType ?: com.example.menotracker.data.models.GoalType.MAINTENANCE
                            val profile = userProfile // Capture to local var for smart cast
                            profile?.calculateSuggestedMacros(goalType)?.let { suggestion ->
                                val newGoal = com.example.menotracker.data.models.NutritionGoal(
                                    id = nutritionGoal?.id ?: java.util.UUID.randomUUID().toString(),
                                    userId = profile.id,
                                    goalType = goalType,
                                    targetCalories = suggestion.calories.toFloat(),
                                    targetProtein = suggestion.protein.toFloat(),
                                    targetCarbs = suggestion.carbs.toFloat(),
                                    targetFat = suggestion.fat.toFloat(),
                                    mealsPerDay = nutritionGoal?.mealsPerDay ?: 3,
                                    isActive = true
                                )
                                nutritionViewModel.saveNutritionGoal(newGoal)
                            }
                        }
                    )
                }
            }

            // Settings Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.section_settings),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            item {
                SettingsCard(
                    isDarkMode = isDarkMode,
                    isMetric = isMetric,
                    notificationsEnabled = notificationsEnabled,
                    targetWorkoutDuration = userProfile?.targetWorkoutDuration,
                    currentPartnerName = currentPartnerName,
                    currentLanguage = currentLanguage,
                    onLanguageClick = { showLanguageDialog = true },
                    onThemeClick = { showThemeDialog = true },
                    onUnitsClick = { showUnitsDialog = true },
                    onWorkoutDurationClick = { showWorkoutDurationDialog = true },
                    onNotificationsChange = { viewModel.setNotificationsEnabled(it) },
                    onReferralCodeClick = { showReferralCodeDialog = true },
                    onAboutClick = { showAboutDialog = true },
                    onHelpClick = { showHelpDialog = true },
                    onInviteClick = {
                        val shareSubject = context.getString(R.string.share_subject)
                        val shareText = context.getString(R.string.share_text)
                        val shareChooserTitle = context.getString(R.string.share_chooser_title)
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, shareSubject)
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, shareChooserTitle))
                    }
                )
            }

            // Logout Button
            item {
                OutlinedButton(
                    onClick = {
                        // First perform the actual logout, then reset debounce state
                        authViewModel.logout {
                            android.util.Log.d("AccountScreen", "✅ Logged out successfully")
                            // Reset debounce state AFTER logout completes to ensure navigation
                            onLogout()
                        }
                    },
                    border = BorderStroke(1.5.dp, Color.Red.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.cd_logout),
                            tint = Color.Red.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.action_logout),
                            color = Color.Red.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    }

    // Dialogs
    if (showBodyStatsDialog) {
        BodyStatsDialog(
            currentName = userName,
            currentWeight = userWeight,
            currentHeight = userHeight,
            currentAge = userProfile?.age?.toString() ?: "",
            currentGender = userProfile?.gender,
            currentActivityLevel = userProfile?.activityLevel,
            currentYears = userYears,
            onDismiss = { showBodyStatsDialog = false },
            onSave = { name, weight, height, age, gender, activityLevel, years ->
                android.util.Log.d("AccountScreen", "🟢 Dialog Save clicked: name='$name', weight='$weight', height='$height', age='$age', gender=$gender, activity=$activityLevel, years='$years'")
                val weightValue = weight.toDoubleOrNull()
                val heightValue = height.toDoubleOrNull()
                val ageValue = age.toIntOrNull()
                val yearsValue = years.toIntOrNull()

                android.util.Log.d("AccountScreen", "🟢 Converted values: name=$name, weight=$weightValue, height=$heightValue, age=$ageValue, years=$yearsValue")
                viewModel.updateBodyStats(
                    name = name,
                    weight = weightValue,
                    height = heightValue,
                    age = ageValue,
                    gender = gender,
                    activityLevel = activityLevel,
                    years = yearsValue
                )
                showBodyStatsDialog = false
            }
        )
    }

    if (showThemeDialog) {
        ThemeDialog(
            currentTheme = if (isDarkMode) "Dark" else "Light",
            onDismiss = { showThemeDialog = false },
            onSelect = { theme ->
                onThemeChange(theme == "Dark")
                showThemeDialog = false
            }
        )
    }

    if (showUnitsDialog) {
        UnitsDialog(
            currentUnit = if (isMetric) "Metric" else "Imperial",
            onDismiss = { showUnitsDialog = false },
            onSelect = { unit ->
                viewModel.setIsMetric(unit == "Metric")
                showUnitsDialog = false
            }
        )
    }

    if (showWorkoutDurationDialog) {
        WorkoutDurationDialog(
            currentDuration = userProfile?.targetWorkoutDuration,
            onDismiss = { showWorkoutDurationDialog = false },
            onSelect = { durationMinutes ->
                viewModel.updateWorkoutDurationTarget(durationMinutes)
                showWorkoutDurationDialog = false
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    if (showNameEditDialog) {
        NameEditDialog(
            currentName = userName,
            onDismiss = { showNameEditDialog = false },
            onSave = { newName ->
                viewModel.updateBodyStats(
                    name = newName,
                    weight = userProfile?.weight,
                    height = userProfile?.height,
                    years = userProfile?.trainingExperience
                )
                showNameEditDialog = false
            }
        )
    }

    if (showGuestUploadDialog) {
        GuestUploadDialog(
            onDismiss = { showGuestUploadDialog = false }
        )
    }

    // Body Measurements Edit Dialog
    if (showMeasurementsDialog) {
        BodyMeasurementsEditDialog(
            currentMeasurement = latestMeasurement,
            userId = userProfile?.id ?: "",
            onDismiss = { showMeasurementsDialog = false },
            onSave = { measurement ->
                viewModel.saveBodyMeasurement(measurement)
                showMeasurementsDialog = false
            }
        )
    }

    // Partner Referral Code Dialog
    if (showReferralCodeDialog) {
        ReferralCodeDialog(
            currentPartnerName = currentPartnerName,
            status = referralCodeStatus,
            onDismiss = {
                showReferralCodeDialog = false
                viewModel.resetReferralCodeStatus()
            },
            onValidateCode = { code ->
                viewModel.validateReferralCode(code)
            },
            onApplyCode = { code ->
                viewModel.applyReferralCode(code)
            }
        )
    }

    // Language Dialog
    if (showLanguageDialog) {
        LanguageDialog(
            currentLanguage = currentLanguage,
            onDismiss = { showLanguageDialog = false },
            onSelect = { languageCode ->
                viewModel.setLanguage(languageCode)
                showLanguageDialog = false
            }
        )
    }

}


@Composable
private fun ProfileHeader(
    userName: String,
    profileImageUrl: String?,
    isGuest: Boolean = false,
    onEditClick: () -> Unit,
    onUploadImageClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardAccent(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with Profile Image or Initials
                    Box(
                        modifier = Modifier.size(80.dp)
                    ) {
                        if (profileImageUrl != null) {
                            // Show profile image - disable cache to always show latest
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(profileImageUrl)
                                    .crossfade(true)
                                    .diskCachePolicy(CachePolicy.DISABLED)
                                    .memoryCachePolicy(CachePolicy.DISABLED)
                                    .build(),
                                contentDescription = "Profile picture",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(android.R.drawable.ic_menu_gallery),
                                error = painterResource(android.R.drawable.ic_menu_report_image)
                            )
                        } else {
                            // Show initials
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(orangeGlow, orangePrimary)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userName.firstOrNull()?.uppercase() ?: "C",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.displaySmall
                                )
                            }
                        }

                        // Camera Button for Upload
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp),
                            shape = CircleShape,
                            color = orangePrimary,
                            shadowElevation = 4.dp,
                            onClick = onUploadImageClick
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Upload photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // User Info
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = userName,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "Naya Member",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = orangeGlow,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Pro",
                                color = orangeGlow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Edit Button
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        tint = orangeGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BodyStatsCard(
    weight: String,
    height: String,
    years: String,
    latestMeasurement: BodyMeasurement? = null,
    isExpanded: Boolean = false,
    onExpandToggle: () -> Unit = {},
    onEditClick: () -> Unit,
    onTrackMeasurementsClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardAccent(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "BODY STATS",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = orangeGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Main Stats (Weight, Height, Experience)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.MonitorWeight,
                    value = if (weight.isNotEmpty()) "${weight}kg" else "--",
                    label = "Weight"
                )

                VerticalDivider(
                    modifier = Modifier.height(60.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )

                StatItem(
                    icon = Icons.Default.Height,
                    value = if (height.isNotEmpty()) "${height}cm" else "--",
                    label = "Height"
                )

                VerticalDivider(
                    modifier = Modifier.height(60.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )

                StatItem(
                    icon = Icons.Default.FitnessCenter,
                    value = if (years.isNotEmpty()) "${years}y" else "--",
                    label = "Experience"
                )
            }

            // Expandable "Track More Measurements" Section
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Expand/Collapse Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = orangeGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Track More Measurements",
                        color = orangeGlow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (latestMeasurement?.hasMeasurements() == true) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = orangePrimary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = latestMeasurement.date,
                                color = orangeGlow,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = orangeGlow,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(if (isExpanded) 180f else 0f)
                )
            }

            // Expanded Measurements Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    if (latestMeasurement?.hasMeasurements() == true) {
                        // Show measurement grid
                        MeasurementsGrid(measurement = latestMeasurement)
                    } else {
                        // Empty state
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No measurements recorded yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Add/Edit Measurements Button
                    Button(
                        onClick = onTrackMeasurementsClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = orangePrimary.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (latestMeasurement?.hasMeasurements() == true)
                                Icons.Default.Edit else Icons.Default.Add,
                            contentDescription = null,
                            tint = orangeGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (latestMeasurement?.hasMeasurements() == true)
                                "Update Measurements" else "Add Measurements",
                            color = orangeGlow,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MeasurementsGrid(measurement: BodyMeasurement) {
    val measurementItems = listOf(
        Triple("Neck", measurement.neck, Icons.Default.Face),
        Triple("Shoulders", measurement.shoulders, Icons.Default.Accessibility),
        Triple("Chest", measurement.chest, Icons.Default.Favorite),
        Triple("Arms", measurement.arms, Icons.Default.FitnessCenter),
        Triple("Forearms", measurement.forearms, Icons.Default.FitnessCenter),
        Triple("Waist", measurement.waist, Icons.Default.Circle),
        Triple("Hips", measurement.hips, Icons.Default.Circle),
        Triple("Glutes", measurement.glutes, Icons.Default.Circle),
        Triple("Legs", measurement.legs, Icons.AutoMirrored.Filled.DirectionsWalk),
        Triple("Calves", measurement.calves, Icons.AutoMirrored.Filled.DirectionsWalk)
    ).filter { it.second != null }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        measurementItems.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (label, value, _) ->
                    MeasurementChip(
                        label = label,
                        value = value!!,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty space if row has less than 3 items
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MeasurementChip(
    label: String,
    value: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${value.toInt()}cm",
                color = orangeGlow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = orangeGlow,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = value,
            color = orangeGlow,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// NUTRITION GOALS CARD
// ═══════════════════════════════════════════════════════════════

@Composable
fun NutritionGoalsCard(
    nutritionGoal: com.example.menotracker.data.models.NutritionGoal?,
    userProfile: com.example.menotracker.data.models.UserProfile?,
    onAutoCalculate: () -> Unit
) {
    val tdee = userProfile?.calculateTDEE()
    val canCalculateTDEE = userProfile?.weight != null &&
                           userProfile.height != null &&
                           userProfile.age != null &&
                           userProfile.gender != null &&
                           userProfile.activityLevel != null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardAccent(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "NUTRITION GOALS",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Goal Type & TDEE Info
            if (nutritionGoal != null || tdee != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Goal Type Badge
                    if (nutritionGoal != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = orangePrimary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (nutritionGoal.goalType) {
                                        com.example.menotracker.data.models.GoalType.CUTTING -> Icons.AutoMirrored.Filled.TrendingDown
                                        com.example.menotracker.data.models.GoalType.BULKING -> Icons.AutoMirrored.Filled.TrendingUp
                                        com.example.menotracker.data.models.GoalType.MAINTENANCE -> Icons.Default.Balance
                                        com.example.menotracker.data.models.GoalType.PERFORMANCE -> Icons.Default.Bolt
                                    },
                                    contentDescription = null,
                                    tint = orangeGlow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = nutritionGoal.goalType.displayName,
                                    color = orangeGlow,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // TDEE Info
                    if (tdee != null) {
                        Text(
                            text = "TDEE: ${tdee} kcal",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Macro Values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroItem(
                    icon = Icons.Default.LocalFireDepartment,
                    value = nutritionGoal?.targetCalories?.toInt()?.toString() ?: "--",
                    label = "kcal",
                    color = orangeGlow
                )

                MacroItem(
                    icon = Icons.Default.FitnessCenter,
                    value = nutritionGoal?.targetProtein?.toInt()?.toString() ?: "--",
                    label = "Protein",
                    color = NayaProtein
                )

                MacroItem(
                    icon = Icons.Default.Grain,
                    value = nutritionGoal?.targetCarbs?.toInt()?.toString() ?: "--",
                    label = "Carbs",
                    color = NayaCarbs
                )

                MacroItem(
                    icon = Icons.Default.WaterDrop,
                    value = nutritionGoal?.targetFat?.toInt()?.toString() ?: "--",
                    label = "Fat",
                    color = NayaFat
                )
            }

            // Auto-Calculate Button (only if profile data allows TDEE calculation)
            if (canCalculateTDEE) {
                OutlinedButton(
                    onClick = onAutoCalculate,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, orangePrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = orangeGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Auto-Calculate from Profile",
                        color = orangeGlow,
                        fontSize = 14.sp
                    )
                }
            } else {
                // Show hint to complete profile
                Text(
                    text = "Complete your profile (age, gender, activity) to auto-calculate goals",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MacroItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun SettingsCard(
    isDarkMode: Boolean,
    isMetric: Boolean,
    notificationsEnabled: Boolean,
    targetWorkoutDuration: Int?,
    currentPartnerName: String?,
    currentLanguage: String = "system",
    onLanguageClick: () -> Unit = {},
    onThemeClick: () -> Unit,
    onUnitsClick: () -> Unit,
    onWorkoutDurationClick: () -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onReferralCodeClick: () -> Unit,
    onAboutClick: () -> Unit,
    onHelpClick: () -> Unit,
    onInviteClick: () -> Unit
) {
    val languageDisplayName = when (currentLanguage) {
        "de" -> stringResource(R.string.language_german)
        "en" -> stringResource(R.string.language_english)
        "fr" -> stringResource(R.string.language_french)
        else -> stringResource(R.string.language_system)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .glassCardAccent(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            SettingItem(
                icon = Icons.Default.Language,
                title = stringResource(R.string.setting_language),
                subtitle = languageDisplayName,
                onClick = onLanguageClick
            )
            SettingItem(
                icon = Icons.Default.Palette,
                title = "Theme",
                subtitle = if (isDarkMode) "Dark Mode" else "Light Mode",
                onClick = onThemeClick
            )
            SettingItem(
                icon = Icons.Default.FitnessCenter,
                title = "Units",
                subtitle = if (isMetric) "Metric (kg, cm)" else "Imperial (lbs, inch)",
                onClick = onUnitsClick
            )
            SettingItem(
                icon = Icons.Default.Timer,
                title = "Workout Duration Target",
                subtitle = if (targetWorkoutDuration != null) "$targetWorkoutDuration minutes" else "Not set",
                onClick = onWorkoutDurationClick
            )
            SettingToggleItem(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                subtitle = if (notificationsEnabled) "Enabled" else "Disabled",
                checked = notificationsEnabled,
                onCheckedChange = onNotificationsChange
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            SettingItem(
                icon = Icons.Default.CardGiftcard,
                title = "Partner Referral Code",
                subtitle = currentPartnerName ?: "Enter a code to support your coach or gym",
                onClick = onReferralCodeClick
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            SettingItem(
                icon = Icons.Default.Info,
                title = "About",
                subtitle = "Version 1.0.0",
                onClick = onAboutClick
            )
            SettingItem(
                icon = Icons.AutoMirrored.Filled.Help,
                title = "Help & Support",
                subtitle = "Get assistance",
                onClick = onHelpClick
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
            SettingItem(
                icon = Icons.Default.Share,
                title = "Invite Friends",
                subtitle = "Share Naya with friends",
                onClick = onInviteClick
            )
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = orangeGlow,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = orangeGlow,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = orangePrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}



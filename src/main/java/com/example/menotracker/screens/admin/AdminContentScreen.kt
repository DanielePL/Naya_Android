package com.example.menotracker.screens.admin

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.AdminManager
import com.example.menotracker.data.WorkoutTemplateRepository
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaSurface
import com.example.menotracker.viewmodels.WorkoutTemplate
import kotlinx.coroutines.launch

private const val TAG = "AdminContentScreen"

/**
 * Admin Content Management Screen
 *
 * Only accessible to admin users (Chloe).
 * Features:
 * - Upload workout videos
 * - Create/edit public workout templates
 * - Manage content visible to all users
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminContentScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWorkoutBuilder: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Admin check
    val isAdmin by AdminManager.isAdmin.collectAsState()
    val adminEmail by AdminManager.adminEmail.collectAsState()

    // Public templates
    var publicTemplates by remember { mutableStateOf<List<WorkoutTemplate>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Video upload state
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var uploadingVideo by remember { mutableStateOf(false) }
    var showVideoUploadDialog by remember { mutableStateOf(false) }
    var selectedWorkoutForVideo by remember { mutableStateOf<WorkoutTemplate?>(null) }

    // Video picker launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedVideoUri = uri
        if (uri != null) {
            Log.d(TAG, "Video selected: $uri")
        }
    }

    // Load public templates
    LaunchedEffect(Unit) {
        isLoading = true
        WorkoutTemplateRepository.loadPublicWorkoutTemplates()
            .onSuccess { templates ->
                publicTemplates = templates
                Log.d(TAG, "Loaded ${templates.size} public templates")
            }
            .onFailure { e ->
                errorMessage = "Failed to load templates: ${e.message}"
                Log.e(TAG, "Error loading templates", e)
            }
        isLoading = false
    }

    // Non-admin access denied screen
    if (!isAdmin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NayaSurface),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Access Denied",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = "Admin privileges required",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary)
                ) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = NayaPrimary
                        )
                        Text("Admin Panel")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NayaSurface,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToWorkoutBuilder,
                containerColor = NayaPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Workout")
            }
        },
        containerColor = NayaSurface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Admin Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2D1B4E)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(NayaPrimary, Color(0xFFA78BFA))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Admin Access",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = adminEmail ?: "Unknown",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Create Public Workout
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FitnessCenter,
                        title = "New Workout",
                        subtitle = "Create public template",
                        onClick = onNavigateToWorkoutBuilder
                    )

                    // Upload Video
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.VideoLibrary,
                        title = "Upload Video",
                        subtitle = "Add workout video",
                        onClick = { showVideoUploadDialog = true }
                    )
                }
            }

            // Public Templates Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Public Workouts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${publicTemplates.size} templates",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NayaPrimary)
                    }
                }
            } else if (errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3D1515))
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else if (publicTemplates.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No public workouts yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                            Text(
                                text = "Create your first workout template",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(publicTemplates) { template ->
                    AdminWorkoutCard(
                        template = template,
                        onAddVideo = {
                            selectedWorkoutForVideo = template
                            showVideoUploadDialog = true
                        },
                        onEdit = {
                            // TODO: Navigate to edit screen
                        }
                    )
                }
            }

            // Bottom spacer for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Video Upload Dialog
    if (showVideoUploadDialog) {
        VideoUploadDialog(
            selectedWorkout = selectedWorkoutForVideo,
            publicTemplates = publicTemplates,
            onWorkoutSelected = { selectedWorkoutForVideo = it },
            onSelectVideo = {
                videoPickerLauncher.launch("video/*")
            },
            selectedVideoUri = selectedVideoUri,
            onUpload = {
                scope.launch {
                    uploadingVideo = true
                    // TODO: Implement actual video upload to Supabase Storage
                    Log.d(TAG, "Uploading video ${selectedVideoUri} for workout ${selectedWorkoutForVideo?.name}")
                    kotlinx.coroutines.delay(2000) // Simulate upload
                    uploadingVideo = false
                    showVideoUploadDialog = false
                    selectedVideoUri = null
                    selectedWorkoutForVideo = null
                }
            },
            uploading = uploadingVideo,
            onDismiss = {
                showVideoUploadDialog = false
                selectedVideoUri = null
                selectedWorkoutForVideo = null
            }
        )
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NayaPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NayaPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AdminWorkoutCard(
    template: WorkoutTemplate,
    onAddVideo: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${template.exercises.size} exercises",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                // Show video status (placeholder - videos not yet supported)
                val hasVideo = false // TODO: Add videoUrl field to WorkoutTemplate
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (hasVideo) Icons.Default.PlayCircle else Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = if (hasVideo) Color.Green else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (hasVideo) "Video attached" else "No video",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasVideo) Color.Green else Color.Gray
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Add/Update Video Button
                IconButton(
                    onClick = onAddVideo,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NayaPrimary.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoCall,
                        contentDescription = "Add Video",
                        tint = NayaPrimary
                    )
                }

                // Edit Button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoUploadDialog(
    selectedWorkout: WorkoutTemplate?,
    publicTemplates: List<WorkoutTemplate>,
    onWorkoutSelected: (WorkoutTemplate) -> Unit,
    onSelectVideo: () -> Unit,
    selectedVideoUri: Uri?,
    onUpload: () -> Unit,
    uploading: Boolean,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!uploading) onDismiss() },
        title = {
            Text("Upload Workout Video", color = Color.White)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Workout Selector
                Text("Select Workout:", color = Color.Gray)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedWorkout?.name ?: "Select a workout...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NayaPrimary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        publicTemplates.forEach { template ->
                            DropdownMenuItem(
                                text = { Text(template.name) },
                                onClick = {
                                    onWorkoutSelected(template)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Video Selection
                Text("Video File:", color = Color.Gray)
                OutlinedButton(
                    onClick = onSelectVideo,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NayaPrimary
                    )
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedVideoUri != null) "Video selected" else "Choose video file"
                    )
                }

                if (selectedVideoUri != null) {
                    Text(
                        text = "Ready to upload",
                        color = Color.Green,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpload,
                enabled = selectedWorkout != null && selectedVideoUri != null && !uploading,
                colors = ButtonDefaults.buttonColors(containerColor = NayaPrimary)
            ) {
                if (uploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uploading...")
                } else {
                    Text("Upload")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !uploading
            ) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1E1E2E)
    )
}

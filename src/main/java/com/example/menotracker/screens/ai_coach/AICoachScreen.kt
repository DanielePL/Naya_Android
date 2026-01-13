package com.example.menotracker.screens.ai_coach

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.ChatMessage
import com.example.menotracker.data.models.AttachmentType
import com.example.menotracker.data.models.MessageAttachment
import kotlinx.coroutines.launch
import java.util.UUID
import com.example.menotracker.ui.theme.*
import com.example.menotracker.billing.Feature
import com.example.menotracker.billing.FeatureGate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.*

// Design System - NAYA Wellness Colors
private val nayaPrimary = NayaPrimary                // Violet #A78BFA (aufgehellt)
private val nayaGlow = Color(0xFFC4B5FD)             // Lighter violet glow
private val nayaPink = Color(0xFFEC4899)             // Accent pink for wellness
private val nayaTeal = Color(0xFF14B8A6)             // Calming teal
private val textWhite = NayaTextWhite                // #FAFAFA
private val textGray = NayaTextGray                  // #9CA3AF
private val cardBackground = NayaSurface             // #1C1C1C
private val cardSurface = NayaSurface                // #1C1C1C

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICoachScreen(
    viewModel: AICoachViewModel,
    onNavigateBack: () -> Unit,
    onStartWorkout: (templateId: String) -> Unit = {},  // Navigate to training with template
    drawerState: DrawerState? = null,  // Optional - if null, creates own
    showHeader: Boolean = true,  // Whether to show the TopAppBar
    onNavigateToPaywall: () -> Unit = {}  // For upgrade prompts
) {
    // Feature gate for AI Coach - Elite only
    FeatureGate(
        feature = Feature.AI_COACH,
        onUpgradeClick = onNavigateToPaywall,
        modifier = Modifier.fillMaxSize()
    ) {
        AICoachScreenContent(
            viewModel = viewModel,
            onNavigateBack = onNavigateBack,
            onStartWorkout = onStartWorkout,
            drawerState = drawerState,
            showHeader = showHeader
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AICoachScreenContent(
    viewModel: AICoachViewModel,
    onNavigateBack: () -> Unit,
    onStartWorkout: (templateId: String) -> Unit = {},
    drawerState: DrawerState? = null,
    showHeader: Boolean = true
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val workoutActions by viewModel.workoutActions.collectAsState()
    val templateRecommendations by viewModel.templateRecommendations.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val currentConversationId by viewModel.currentConversationId.collectAsState()
    val context = LocalContext.current

    // Wellness context and alerts
    val topAlert by viewModel.topAlert.collectAsState()
    val recommendedQuickActions = viewModel.getRecommendedQuickActions()
    val wellnessSummary = viewModel.getWellnessSummary()

    var messageText by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<MessageAttachment>>(emptyList()) }
    var showAttachmentPicker by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    // Use passed drawerState or create own
    val internalDrawerState = rememberDrawerState(DrawerValue.Closed)
    val actualDrawerState = drawerState ?: internalDrawerState
    val coroutineScope = rememberCoroutineScope()

    // File Pickers
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val attachment = MessageAttachment(
                id = UUID.randomUUID().toString(),
                type = AttachmentType.IMAGE,
                fileName = "image_${System.currentTimeMillis()}.jpg",
                fileUri = it.toString()
            )
            attachments = attachments + attachment
        }
    }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = "document_${System.currentTimeMillis()}.pdf"
            val attachment = MessageAttachment(
                id = UUID.randomUUID().toString(),
                type = AttachmentType.PDF,
                fileName = fileName,
                fileUri = it.toString()
            )
            attachments = attachments + attachment
        }
    }

    // Camera capture functionality
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            // Process the captured image with EXIF rotation correction
            try {
                val correctedUri = processCapturedImage(context, tempCameraUri!!)
                val attachment = MessageAttachment(
                    id = UUID.randomUUID().toString(),
                    type = AttachmentType.IMAGE,
                    fileName = "photo_${System.currentTimeMillis()}.jpg",
                    fileUri = correctedUri.toString()
                )
                attachments = attachments + attachment
            } catch (e: Exception) {
                android.util.Log.e("AICoachScreen", "Failed to process camera image", e)
                // Fallback: use original URI
                val attachment = MessageAttachment(
                    id = UUID.randomUUID().toString(),
                    type = AttachmentType.IMAGE,
                    fileName = "photo_${System.currentTimeMillis()}.jpg",
                    fileUri = tempCameraUri.toString()
                )
                attachments = attachments + attachment
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, launch camera
            val photoFile = createImageFile(context)
            tempCameraUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraLauncher.launch(tempCameraUri!!)
        }
    }

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = actualDrawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = cardSurface,
                modifier = Modifier.width(300.dp)
            ) {
                ConversationsDrawerContent(
                    conversations = conversations,
                    currentConversationId = currentConversationId,
                    onConversationClick = { conversationId ->
                        viewModel.loadConversation(conversationId)
                        coroutineScope.launch {
                            actualDrawerState.close()
                        }
                    },
                    onNewChatClick = {
                        viewModel.createNewChat()
                        coroutineScope.launch {
                            actualDrawerState.close()
                        }
                    },
                    onDeleteConversation = { conversationId ->
                        viewModel.deleteConversation(conversationId)
                    },
                    onClose = {
                        coroutineScope.launch {
                            actualDrawerState.close()
                        }
                    }
                )
            }
        }
    ) {
    // Only show TopAppBar when showHeader is true (standalone mode)
    Scaffold(
        topBar = {
            if (showHeader) {
                TopAppBar(
                    title = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(nayaGlow, nayaPrimary)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = textWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Naya Coach",
                                    color = textWhite,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isLoading) "Schreibt..." else "Online",
                                    color = if (isLoading) nayaGlow else nayaTeal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                actualDrawerState.open()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open conversations",
                                tint = textWhite
                            )
                        }
                    },
                    actions = {
                        // New Chat Button
                        IconButton(onClick = {
                            viewModel.createNewChat()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = nayaPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = cardSurface
                    )
                )
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Proactive Alert Banner
            AnimatedVisibility(
                visible = topAlert != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                topAlert?.let { alert ->
                    ProactiveAlertBanner(
                        alert = alert,
                        onDismiss = { viewModel.dismissAlert(alert.id) },
                        onAction = {
                            viewModel.startProactiveChat(alert)
                        }
                    )
                }
            }

            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        workout = workoutActions[message.id],
                        templateRecommendation = templateRecommendations[message.id],
                        onSaveWorkout = { workout ->
                            viewModel.saveWorkout(
                                workout = workout,
                                onSuccess = {
                                    // Could show toast or navigate to library
                                },
                                onError = { error ->
                                    // Error already shown in chat
                                }
                            )
                        },
                        onStartWorkout = onStartWorkout
                    )
                }

                // Suggested topics after welcome message
                if (messages.size == 1 && !isLoading) {
                    item {
                        SuggestedTopicsSection(
                            onTopicClick = { topic ->
                                viewModel.sendMessage(topic, emptyList(), context)
                            }
                        )
                    }
                }

                // Loading indicator
                if (isLoading) {
                    item {
                        LoadingBubble()
                    }
                }
            }

            // Quick Actions Row - shown when few messages
            if (messages.size <= 2 && !isLoading) {
                QuickActionsRow(
                    quickActions = recommendedQuickActions,
                    onActionClick = { action ->
                        viewModel.sendQuickAction(action, context)
                    }
                )
            }

            // Input Area - with padding for footer bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp),  // Space for footer bar
                color = cardSurface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Attachment Preview
                    if (attachments.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(attachments) { attachment ->
                                AttachmentPreviewChip(
                                    attachment = attachment,
                                    onRemove = {
                                        attachments = attachments.filter { it.id != attachment.id }
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Attachment button
                        IconButton(
                            onClick = {
                                showAttachmentPicker = true
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(cardBackground)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach File",
                                tint = nayaGlow
                            )
                        }

                        // Text Input
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    "Ask me anything...",
                                    color = textGray
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textWhite,
                                unfocusedTextColor = textWhite,
                                focusedBorderColor = nayaGlow,
                                unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                                cursorColor = nayaGlow,
                                focusedContainerColor = cardBackground,
                                unfocusedContainerColor = cardBackground
                            ),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4
                        )

                        // Send Button
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank() || attachments.isNotEmpty()) {
                                    viewModel.sendMessage(messageText, attachments, context)
                                    messageText = ""
                                    attachments = emptyList()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (messageText.isNotBlank() || attachments.isNotEmpty()) {
                                        Brush.linearGradient(
                                            colors = listOf(nayaGlow, nayaPrimary)
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            colors = listOf(textGray.copy(alpha = 0.3f), textGray.copy(alpha = 0.3f))
                                        )
                                    }
                                ),
                            enabled = messageText.isNotBlank() || attachments.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (messageText.isNotBlank() || attachments.isNotEmpty()) textWhite else textGray
                            )
                        }
                    }
                }
            }

            // Attachment Picker Dialog
            if (showAttachmentPicker) {
                AlertDialog(
                    onDismissRequest = { showAttachmentPicker = false },
                    title = { Text("Add Attachment") },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Camera Button - Take Photo
                            Button(
                                onClick = {
                                    showAttachmentPicker = false
                                    // Check camera permission
                                    when {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED -> {
                                            // Permission already granted, launch camera
                                            val photoFile = createImageFile(context)
                                            tempCameraUri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                photoFile
                                            )
                                            cameraLauncher.launch(tempCameraUri!!)
                                        }
                                        else -> {
                                            // Request permission
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = nayaPrimary
                                )
                            ) {
                                Icon(Icons.Default.CameraAlt, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Take Photo")
                            }

                            // Gallery Button
                            Button(
                                onClick = {
                                    imagePicker.launch("image/*")
                                    showAttachmentPicker = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = cardBackground
                                )
                            ) {
                                Icon(Icons.Default.Image, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Choose from Gallery")
                            }

                            // PDF Button
                            Button(
                                onClick = {
                                    documentPicker.launch("application/pdf")
                                    showAttachmentPicker = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = cardBackground
                                )
                            ) {
                                Icon(Icons.Default.PictureAsPdf, null)
                                Spacer(Modifier.width(8.dp))
                                Text("PDF Document")
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showAttachmentPicker = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    } // Close Scaffold
    } // Close ModalNavigationDrawer
}


@Composable
private fun MessageBubble(
    message: ChatMessage,
    workout: com.example.menotracker.data.models.WorkoutRecommendation? = null,
    templateRecommendation: com.example.menotracker.data.models.TemplateRecommendation? = null,
    onSaveWorkout: (com.example.menotracker.data.models.WorkoutRecommendation) -> Unit = {},
    onStartWorkout: (templateId: String) -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isFromUser) {
            // AI Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(nayaGlow, nayaPrimary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = textWhite,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(8.dp))
        }

        val userShape = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
        val aiShape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Message Content - using glassmorphism modifiers
            Box(
                modifier = Modifier
                    .then(
                        if (message.isFromUser) {
                            Modifier.glassUserMessage(userShape)
                        } else {
                            Modifier.glassAiMessage(aiShape)
                        }
                    )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = message.content,
                        color = textWhite,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )

                    // Attachments
                    if (!message.attachments.isNullOrEmpty()) {
                        message.attachments.forEach { attachment ->
                            AttachmentChip(attachment = attachment)
                        }
                    }

                    // Timestamp
                    Text(
                        text = formatTimestamp(message.timestamp),
                        color = textGray,
                        fontSize = 11.sp,
                        modifier = Modifier.align(if (message.isFromUser) Alignment.End else Alignment.Start)
                    )
                }
            }

            // Workout Save Button
            if (workout != null && !message.isFromUser) {
                Button(
                    onClick = { onSaveWorkout(workout) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = nayaPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Save to My Workouts",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Template Recommendation Card
            if (templateRecommendation != null && !message.isFromUser) {
                WorkoutTemplateCard(
                    template = templateRecommendation,
                    onStartWorkout = { onStartWorkout(templateRecommendation.templateId) }
                )
            }
        }

        if (message.isFromUser) {
            Spacer(Modifier.width(8.dp))

            // User Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(nayaPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "C",
                    color = textWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LoadingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // AI Avatar
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(nayaGlow, nayaPrimary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = textWhite,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        val aiShape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
        Box(
            modifier = Modifier.glassAiMessage(aiShape)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(textGray)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreviewChip(
    attachment: MessageAttachment,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = nayaPrimary.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (attachment.type) {
                    AttachmentType.IMAGE -> Icons.Default.Image
                    AttachmentType.PDF -> Icons.Default.PictureAsPdf
                    AttachmentType.DEXA_SCAN -> Icons.Default.FitnessCenter
                    AttachmentType.MEDICAL_DIAGNOSIS -> Icons.Default.MedicalServices
                    AttachmentType.PHYSIO_REPORT -> Icons.Default.HealthAndSafety
                    else -> Icons.Default.AttachFile
                },
                contentDescription = null,
                tint = nayaGlow,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = attachment.fileName.take(15) + if (attachment.fileName.length > 15) "..." else "",
                color = textWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = textWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AttachmentChip(attachment: com.example.menotracker.data.models.MessageAttachment) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = nayaPrimary.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (attachment.type) {
                    com.example.menotracker.data.models.AttachmentType.DEXA_SCAN -> Icons.Default.FitnessCenter
                    com.example.menotracker.data.models.AttachmentType.MEDICAL_DIAGNOSIS -> Icons.Default.MedicalServices
                    com.example.menotracker.data.models.AttachmentType.PHYSIO_REPORT -> Icons.Default.HealthAndSafety
                    com.example.menotracker.data.models.AttachmentType.PDF -> Icons.Default.PictureAsPdf
                    else -> Icons.Default.AttachFile
                },
                contentDescription = null,
                tint = nayaGlow,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = attachment.fileName,
                color = textWhite,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ConversationsDrawerContent(
    conversations: List<com.example.menotracker.data.models.Conversation>,
    currentConversationId: String?,
    onConversationClick: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cardSurface)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Conversations",
                color = textWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = textWhite
                )
            }
        }

        HorizontalDivider(color = Color(0xFF333333))

        // New Chat Button
        Surface(
            onClick = onNewChatClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            color = nayaPrimary.copy(alpha = 0.2f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = nayaPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "New Chat",
                    color = nayaPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        HorizontalDivider(
            color = Color(0xFF333333),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Conversations List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(conversations, key = { it.id }) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    isSelected = conversation.id == currentConversationId,
                    onClick = { onConversationClick(conversation.id) },
                    onDelete = { onDeleteConversation(conversation.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationItem(
    conversation: com.example.menotracker.data.models.Conversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelected) nayaPrimary.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = conversation.title,
                    color = if (isSelected) nayaPrimary else textWhite,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
                conversation.lastMessage?.let { lastMsg ->
                    Text(
                        text = lastMsg,
                        color = textGray,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                Text(
                    text = conversation.getFormattedTime(),
                    color = textGray.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = textGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete conversation?") },
            text = { Text("The conversation will be archived, not permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Clickable card displaying a workout template recommendation
 * Shows template name, sports tags, and exercises with a "Start Workout" button
 */
@Composable
private fun WorkoutTemplateCard(
    template: com.example.menotracker.data.models.TemplateRecommendation,
    onStartWorkout: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E3A2F)  // Dark green tint
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with icon and name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(nayaPrimary.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = nayaPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        color = textWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (template.exerciseCount > 0) {
                        Text(
                            text = "${template.exerciseCount} exercises",
                            color = textGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Sports tags
            if (!template.sports.isNullOrEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(template.sports) { sport ->
                        Surface(
                            color = nayaPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = sport,
                                color = nayaPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Exercise preview
            if (template.exerciseNames.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    template.exerciseNames.take(4).forEach { exerciseName ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(nayaPrimary)
                            )
                            Text(
                                text = exerciseName,
                                color = textGray,
                                fontSize = 13.sp
                            )
                        }
                    }
                    if (template.exerciseNames.size > 4) {
                        Text(
                            text = "+${template.exerciseNames.size - 4} more exercises",
                            color = textGray.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            // Start Workout Button
            Button(
                onClick = onStartWorkout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = nayaPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Start Workout",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Suggested menopause topics for quick access
 */
@Composable
private fun SuggestedTopicsSection(
    onTopicClick: (String) -> Unit
) {
    val topics = listOf(
        SuggestedTopic(
            icon = Icons.Default.Thermostat,
            title = "Hot Flashes",
            question = "What can I do about hot flashes? I get them especially at night.",
            color = Color(0xFFEF4444)  // Red
        ),
        SuggestedTopic(
            icon = Icons.Default.Bedtime,
            title = "Sleep Issues",
            question = "I haven't been sleeping well since menopause. Do you have tips for better sleep?",
            color = Color(0xFF3B82F6)  // Blue
        ),
        SuggestedTopic(
            icon = Icons.Default.Mood,
            title = "Mood & Energy",
            question = "I often feel tired and low. How can I improve my energy levels?",
            color = Color(0xFFFBBF24)  // Amber
        ),
        SuggestedTopic(
            icon = Icons.Default.FitnessCenter,
            title = "Exercise",
            question = "What exercises are good for women going through menopause?",
            color = nayaPrimary
        ),
        SuggestedTopic(
            icon = Icons.Default.Restaurant,
            title = "Nutrition",
            question = "What foods should I eat to help manage my symptoms?",
            color = Color(0xFF10B981)  // Green
        ),
        SuggestedTopic(
            icon = Icons.Default.Psychology,
            title = "Understanding Hormones",
            question = "Can you explain what's happening in my body during menopause?",
            color = nayaPink
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Common Topics",
            color = textGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp)
        )

        // Display topics in a 2-column grid
        topics.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { topic ->
                    SuggestedTopicChip(
                        topic = topic,
                        onClick = { onTopicClick(topic.question) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if odd number of items
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class SuggestedTopic(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val question: String,
    val color: Color
)

@Composable
private fun SuggestedTopicChip(
    topic: SuggestedTopic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = topic.color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = topic.icon,
                contentDescription = null,
                tint = topic.color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = topic.title,
                color = textWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} min"
        diff < 86400_000 -> "${diff / 3600_000} hrs"
        else -> "${diff / 86400_000} days"
    }
}

// ============================================================
// PROACTIVE WELLNESS UI COMPONENTS
// ============================================================

/**
 * Proactive Alert Banner - Shows important health alerts at top of chat
 */
@Composable
private fun ProactiveAlertBanner(
    alert: ProactiveAlert,
    onDismiss: () -> Unit,
    onAction: () -> Unit
) {
    val backgroundColor = when (alert.priority) {
        AlertPriority.HIGH -> Color(0xFF7F1D1D).copy(alpha = 0.9f)  // Dark red
        AlertPriority.MEDIUM -> Color(0xFF78350F).copy(alpha = 0.9f)  // Dark amber
        AlertPriority.LOW -> Color(0xFF14532D).copy(alpha = 0.9f)  // Dark green
    }

    val accentColor = when (alert.priority) {
        AlertPriority.HIGH -> Color(0xFFFCA5A5)  // Light red
        AlertPriority.MEDIUM -> Color(0xFFFCD34D)  // Light amber
        AlertPriority.LOW -> Color(0xFF86EFAC)  // Light green
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Emoji indicator
                    Text(
                        text = alert.emoji,
                        fontSize = 28.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = alert.title,
                            color = accentColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = alert.message,
                            color = textWhite.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Dismiss button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = textGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Suggestion text
            Text(
                text = alert.suggestion,
                color = textGray,
                fontSize = 13.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )

            // Action button
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = alert.actionText,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Quick Actions Row - Contextual action chips based on user's wellness data
 */
@Composable
private fun QuickActionsRow(
    quickActions: List<AICoachViewModel.QuickAction>,
    onActionClick: (AICoachViewModel.QuickAction) -> Unit
) {
    if (quickActions.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Quick Actions",
            color = textGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickActions) { action ->
                QuickActionChip(
                    action = action,
                    onClick = { onActionClick(action) }
                )
            }
        }
    }
}

/**
 * Individual Quick Action Chip
 */
@Composable
private fun QuickActionChip(
    action: AICoachViewModel.QuickAction,
    onClick: () -> Unit
) {
    val chipColor = when (action) {
        AICoachViewModel.QuickAction.SYMPTOMS -> Color(0xFFEF4444)  // Red
        AICoachViewModel.QuickAction.SLEEP -> Color(0xFF3B82F6)  // Blue
        AICoachViewModel.QuickAction.NUTRITION -> Color(0xFF10B981)  // Green
        AICoachViewModel.QuickAction.RELAXATION -> nayaTeal
        AICoachViewModel.QuickAction.EXERCISE -> nayaPrimary
        AICoachViewModel.QuickAction.MOOD -> nayaPink
    }

    Surface(
        onClick = onClick,
        color = chipColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, chipColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = action.emoji,
                fontSize = 16.sp
            )
            Text(
                text = action.displayName,
                color = textWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Wellness Context Chips - Shows what data the AI knows about
 */
@Composable
private fun WellnessContextChips(
    summary: String,
    modifier: Modifier = Modifier
) {
    if (summary.isBlank() || summary == "No recent health data") return

    val chips = summary.split(" | ")

    LazyRow(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips) { chip ->
            Surface(
                color = nayaPrimary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = chip,
                    color = nayaGlow,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ============================================================
// CAMERA HELPER FUNCTIONS
// ============================================================

/**
 * Create a temporary image file for camera capture
 */
private fun createImageFile(context: Context): File {
    val timestamp = System.currentTimeMillis()
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
        "COACH_${timestamp}_",
        ".jpg",
        storageDir
    )
}

/**
 * Process captured image with EXIF rotation correction
 * Android cameras often save images in wrong orientation - this fixes it
 */
private fun processCapturedImage(context: Context, uri: Uri): Uri {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: return uri

    // Read the original bitmap
    val originalBitmap = BitmapFactory.decodeStream(inputStream)
    inputStream.close()

    // Get EXIF rotation
    val exifInputStream = context.contentResolver.openInputStream(uri)
    val exif = exifInputStream?.let { ExifInterface(it) }
    exifInputStream?.close()

    val rotation = when (exif?.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    // If no rotation needed, return original
    if (rotation == 0f) {
        return uri
    }

    // Rotate the bitmap
    val matrix = Matrix().apply { postRotate(rotation) }
    val rotatedBitmap = Bitmap.createBitmap(
        originalBitmap,
        0, 0,
        originalBitmap.width,
        originalBitmap.height,
        matrix,
        true
    )

    // Save rotated bitmap to a new file
    val outputFile = createImageFile(context)
    FileOutputStream(outputFile).use { out ->
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }

    // Clean up
    if (rotatedBitmap != originalBitmap) {
        originalBitmap.recycle()
    }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outputFile
    )
}

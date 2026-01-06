package com.example.menotracker.screens.coach

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.menotracker.data.CoachRepository
import com.example.menotracker.data.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaBackground
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.ui.theme.glassUserMessage
import com.example.menotracker.ui.theme.glassAiMessage
import com.example.menotracker.ui.theme.glassInputArea
import com.example.menotracker.billing.Feature
import com.example.menotracker.billing.FeatureGate

// Design System - Naya Premium Dark Theme
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)
private val userMessageBackground = NayaPrimary.copy(alpha = 0.2f)
private val coachMessageBackground = Color(0xFF1E1E1E)
// Premium dark background with subtle warmth
private val darkBackground = Color(0xFF0D0D0D)
private val cardSurface = Color(0xFF1A1A1A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicalCoachScreen(
    viewModel: PhysicalCoachViewModel,
    onNavigateToPaywall: () -> Unit = {}
) {
    // Feature gate for Physical Coach - Elite only
    FeatureGate(
        feature = Feature.PHYSICAL_COACH,
        onUpgradeClick = onNavigateToPaywall,
        modifier = Modifier.fillMaxSize()
    ) {
        PhysicalCoachScreenContent(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhysicalCoachScreenContent(
    viewModel: PhysicalCoachViewModel
) {
    val state by viewModel.state.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    val connectionError by viewModel.connectionError.collectAsState()
    val connectionSuccess by viewModel.connectionSuccess.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var showCodeDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Get current user ID for message alignment
    val currentUserId = remember {
        try {
            SupabaseClient.client.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            null
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

    // Handle connection success
    LaunchedEffect(connectionSuccess) {
        if (connectionSuccess) {
            showCodeDialog = false
            viewModel.clearConnectionSuccess()
        }
    }

    // Transparent background - AppBackground is provided by parent CoachScreen
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (val currentState = state) {
            is PhysicalCoachViewModel.CoachState.Loading -> {
                LoadingContent()
            }

            is PhysicalCoachViewModel.CoachState.NoCoachConnected -> {
                NoCoachContent(
                    onEnterCode = { showCodeDialog = true }
                )
            }

            is PhysicalCoachViewModel.CoachState.PendingConnection -> {
                PendingConnectionContent(
                    coachName = currentState.coachName,
                    onRefresh = { viewModel.retry() }
                )
            }

            is PhysicalCoachViewModel.CoachState.Error -> {
                ErrorContent(
                    message = currentState.message,
                    onRetry = { viewModel.retry() }
                )
            }

            is PhysicalCoachViewModel.CoachState.Ready -> {
                val context = LocalContext.current
                ReadyChatContent(
                    coachName = currentState.coachName,
                    coachAvatarUrl = currentState.coachAvatarUrl,
                    messages = messages,
                    currentUserId = currentUserId,
                    isSending = isSending,
                    isRefreshing = isRefreshing,
                    messageText = messageText,
                    onMessageChange = { messageText = it },
                    onSend = { text ->
                        viewModel.sendMessage(text)
                        messageText = ""
                    },
                    onSendWithFile = { text, fileUri, fileName, fileType ->
                        viewModel.sendMessageWithFile(context, text, fileUri, fileName, fileType)
                        messageText = ""
                    },
                    onRefresh = { viewModel.refreshMessages() },
                    listState = listState
                )
            }
        }

        // Coach Code Entry Dialog
        if (showCodeDialog) {
            CoachCodeDialog(
                isLoading = isConnecting,
                error = connectionError,
                onDismiss = {
                    showCodeDialog = false
                    viewModel.clearConnectionError()
                },
                onSubmit = { code ->
                    viewModel.connectWithCode(code)
                }
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = orangePrimary)
            Text(
                text = "Connecting to your coach...",
                color = textGray,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun NoCoachContent(
    onEnterCode: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PersonOff,
                contentDescription = null,
                tint = textGray,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "No Coach Connected",
                color = textWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "You don't have a physical coach connected yet. Enter your coach's code or contact them to get connected.",
                color = textGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Enter Coach Code Button
            Button(
                onClick = onEnterCode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = orangePrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Enter Coach Code",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PendingConnectionContent(
    coachName: String?,
    onRefresh: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = orangePrimary,
                modifier = Modifier.size(64.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = "Connection Pending",
                color = textWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (coachName != null) {
                    "Waiting for $coachName to accept your connection request."
                } else {
                    "Waiting for your coach to accept the connection request."
                },
                color = textGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "You'll be able to chat once your coach accepts.",
                color = textGray.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onRefresh,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = orangePrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Check Status",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = Color.Red.copy(alpha = 0.7f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Something went wrong",
                color = textWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                color = textGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = orangePrimary)
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyChatContent(coachName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                tint = orangePrimary.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Start a conversation with $coachName",
                color = textGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoachHeader(
    coachName: String,
    coachAvatarUrl: String?,
    onRefresh: () -> Unit,
    isRefreshing: Boolean
) {
    Surface(
        color = cardSurface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Coach Avatar
            if (coachAvatarUrl != null) {
                AsyncImage(
                    model = coachAvatarUrl,
                    contentDescription = "Coach avatar",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(orangePrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = orangePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Coach Name and Status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coachName,
                    color = textWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Text(
                        text = "Your Coach",
                        color = textGray,
                        fontSize = 12.sp
                    )
                }
            }

            // Refresh Button
            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = orangePrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = textGray
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: CoachRepository.CoachMessage,
    isFromCurrentUser: Boolean
) {
    val alignment = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    val userShape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    val coachShape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = alignment
    ) {
        // Use glassmorphism modifiers instead of Surface
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .then(
                    if (isFromCurrentUser) {
                        Modifier.glassUserMessage(userShape)
                    } else {
                        Modifier.glassAiMessage(coachShape)
                    }
                )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Display file attachment if present
                message.fileUrl?.let { fileUrl ->
                    val isImage = message.fileType == "image"

                    if (isImage) {
                        // Show image preview - clickable to open full size
                        AsyncImage(
                            model = fileUrl,
                            contentDescription = "Attached image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
                                    context.startActivity(intent)
                                },
                            contentScale = ContentScale.Fit,
                            onError = { Log.e("PhysicalCoach", "Failed to load image: $fileUrl") }
                        )
                    } else {
                        // Show file download card for PDFs
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = orangePrimary.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            onClick = {
                                // Open file in browser
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
                                context.startActivity(intent)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = orangePrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.fileName ?: "Document",
                                        color = textWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Tap to open",
                                        color = textGray,
                                        fontSize = 10.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = orangePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Text content (only show if not just the filename)
                if (message.content.isNotBlank() && message.content != message.fileName) {
                    Text(
                        text = message.content,
                        color = textWhite,
                        fontSize = 14.sp
                    )
                }

                message.createdAt?.let { timestamp ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatTimestamp(timestamp),
                        color = textGray.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// Colors matching AI Coach
private val cardBackground = Color(0xFF1C1C1C)

// Data class for attachments
data class CoachAttachment(
    val id: String,
    val fileName: String,
    val fileUri: String,
    val type: AttachmentType
)

enum class AttachmentType {
    IMAGE, PDF
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadyChatContent(
    coachName: String,
    coachAvatarUrl: String?,
    messages: List<CoachRepository.CoachMessage>,
    currentUserId: String?,
    isSending: Boolean,
    isRefreshing: Boolean,
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onSendWithFile: (String, Uri, String, String) -> Unit,
    onRefresh: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    var attachments by remember { mutableStateOf<List<CoachAttachment>>(emptyList()) }
    var showAttachmentPicker by remember { mutableStateOf(false) }

    // File Pickers
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val attachment = CoachAttachment(
                id = UUID.randomUUID().toString(),
                fileName = "image_${System.currentTimeMillis()}.jpg",
                fileUri = it.toString(),
                type = AttachmentType.IMAGE
            )
            attachments = attachments + attachment
        }
    }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val attachment = CoachAttachment(
                id = UUID.randomUUID().toString(),
                fileName = "document_${System.currentTimeMillis()}.pdf",
                fileUri = it.toString(),
                type = AttachmentType.PDF
            )
            attachments = attachments + attachment
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Coach Header
            CoachHeader(
                coachName = coachName,
                coachAvatarUrl = coachAvatarUrl,
                onRefresh = onRefresh,
                isRefreshing = isRefreshing
            )

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
                if (messages.isEmpty()) {
                    item {
                        EmptyChatContent(coachName = coachName)
                    }
                } else {
                    items(messages) { message ->
                        MessageBubble(
                            message = message,
                            isFromCurrentUser = message.senderId == currentUserId
                        )
                    }
                }

                if (isSending) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = orangePrimary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }

            // Input Area - with glassmorphism effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp)  // Space for footer bar
                    .glassInputArea()
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
                            onClick = { showAttachmentPicker = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(cardBackground)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach File",
                                tint = orangeGlow
                            )
                        }

                        // Text Input
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = onMessageChange,
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    "Message your coach...",
                                    color = textGray
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textWhite,
                                unfocusedTextColor = textWhite,
                                focusedBorderColor = orangeGlow,
                                unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                                cursorColor = orangeGlow,
                                focusedContainerColor = cardBackground,
                                unfocusedContainerColor = cardBackground
                            ),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4
                        )

                        // Send Button
                        IconButton(
                            onClick = {
                                if (attachments.isNotEmpty()) {
                                    // Send each attachment as a separate message
                                    attachments.forEach { attachment ->
                                        val uri = Uri.parse(attachment.fileUri)
                                        val fileType = when (attachment.type) {
                                            AttachmentType.IMAGE -> "image"
                                            AttachmentType.PDF -> "pdf"
                                        }
                                        onSendWithFile(messageText, uri, attachment.fileName, fileType)
                                    }
                                    attachments = emptyList()
                                } else if (messageText.isNotBlank()) {
                                    onSend(messageText)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (messageText.isNotBlank() || attachments.isNotEmpty()) {
                                        Brush.linearGradient(
                                            colors = listOf(orangeGlow, orangePrimary)
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                textGray.copy(alpha = 0.3f),
                                                textGray.copy(alpha = 0.3f)
                                            )
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
                            Button(
                                onClick = {
                                    imagePicker.launch("image/*")
                                    showAttachmentPicker = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = orangePrimary
                                )
                            ) {
                                Icon(Icons.Default.Image, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Photo / Image")
                            }

                            Button(
                                onClick = {
                                    documentPicker.launch("application/pdf")
                                    showAttachmentPicker = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = orangePrimary
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
    }
}

@Composable
private fun AttachmentPreviewChip(
    attachment: CoachAttachment,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = orangePrimary.copy(alpha = 0.3f)
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
                },
                contentDescription = null,
                tint = orangeGlow,
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

private fun formatTimestamp(isoTimestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(isoTimestamp.substringBefore('.').substringBefore('+'))

        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        ""
    }
}

@Composable
private fun CoachCodeDialog(
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardSurface,
        title = {
            Text(
                text = "Connect with Coach",
                color = textWhite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enter the 6-character code from your coach to connect.",
                    color = textGray,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        if (it.length <= 6 && it.all { c -> c.isLetterOrDigit() }) {
                            code = it.uppercase()
                        }
                    },
                    label = { Text("Coach Code", color = textGray) },
                    placeholder = { Text("e.g., A3B7X9", color = textGray.copy(alpha = 0.5f)) },
                    singleLine = true,
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textWhite,
                        unfocusedTextColor = textWhite,
                        focusedBorderColor = orangePrimary,
                        unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                        cursorColor = orangePrimary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(
                        text = error,
                        color = Color(0xFFFF6B6B),
                        fontSize = 12.sp
                    )
                }

                if (isLoading) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            color = orangePrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(code) },
                enabled = code.length == 6 && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = orangePrimary,
                    disabledContainerColor = orangePrimary.copy(alpha = 0.3f)
                )
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel", color = textGray)
            }
        }
    )
}
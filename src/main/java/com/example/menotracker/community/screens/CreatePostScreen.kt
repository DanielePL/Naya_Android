package com.example.menotracker.community.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.menotracker.community.viewmodels.CreatePostViewModel
import com.example.menotracker.ui.theme.*

// =============================================================================
// CREATE POST SCREEN - Naya Branding
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    currentUserId: String,
    userProfileImageUrl: String? = null,
    onNavigateBack: () -> Unit,
    onPostCreated: () -> Unit,
    viewModel: CreatePostViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var caption by remember { mutableStateOf("") }
    var selectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Media picker
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedMediaUris = (selectedMediaUris + uris).take(5)
        }
    }

    // Video picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedMediaUris = (selectedMediaUris + it).take(5)
        }
    }

    // Handle post success
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onPostCreated()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = NayaOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "CREATE POST",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = NayaTextPrimary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NayaTextPrimary
                            )
                        }
                    },
                    actions = {
                        // Post button
                        IconButton(
                            onClick = {
                                viewModel.createPost(
                                    userId = currentUserId,
                                    content = caption,
                                    postType = "GENERAL",
                                    mediaUris = selectedMediaUris,
                                    context = context
                                )
                            },
                            enabled = caption.isNotBlank() && !state.isLoading
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = NayaOrange
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Post",
                                    tint = if (caption.isNotBlank()) NayaOrange else NayaTextSecondary
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
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // =============================================================
                // CONTENT INPUT CARD
                // =============================================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .glassBackground(cornerRadius = 16.dp, alpha = 0.35f),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header with profile image
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Profile Avatar
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(NayaOrange, NayaOrangeBright)
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!userProfileImageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = userProfileImageUrl,
                                        contentDescription = "Profile",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(NayaOrange, NayaOrangeBright)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Share with the community",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = NayaTextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Text input
                        OutlinedTextField(
                            value = caption,
                            onValueChange = { if (it.length <= 500) caption = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 150.dp),
                            placeholder = {
                                Text(
                                    text = "What's on your mind?",
                                    fontFamily = Poppins,
                                    color = NayaTextSecondary
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NayaOrange,
                                unfocusedBorderColor = NayaGlass,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = NayaOrange
                            ),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = Poppins,
                                color = NayaTextPrimary
                            )
                        )

                        // Character count
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = null,
                                    tint = NayaTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${caption.length}/500",
                                    color = if (caption.length > 450) NayaOrange else NayaTextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = Poppins
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // =============================================================
                // MEDIA SECTION
                // =============================================================
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = NayaOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ADD MEDIA",
                        color = NayaTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        fontFamily = Poppins
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Media picker buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MediaPickerButton(
                        icon = Icons.Default.Image,
                        label = "Photos",
                        onClick = { mediaPickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    )
                    MediaPickerButton(
                        icon = Icons.Default.Videocam,
                        label = "Video",
                        onClick = { videoPickerLauncher.launch("video/*") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Selected media preview
                if (selectedMediaUris.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedMediaUris) { uri ->
                            MediaPreviewItem(
                                uri = uri,
                                onRemove = {
                                    selectedMediaUris = selectedMediaUris.filter { it != uri }
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NayaOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${selectedMediaUris.size}/5 media selected",
                            color = NayaTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = Poppins
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // =============================================================
                // TIPS SECTION
                // =============================================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .glassBackground(cornerRadius = 16.dp, alpha = 0.2f),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = NayaOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Tips for great posts",
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NayaTextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val tips = listOf(
                            "Share your training progress and achievements",
                            "Add photos or videos to increase engagement",
                            "Ask questions to connect with the community",
                            "Keep it authentic and motivating"
                        )

                        tips.forEach { tip ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowRight,
                                    contentDescription = null,
                                    tint = NayaOrange,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tip,
                                    fontFamily = Poppins,
                                    fontSize = 13.sp,
                                    color = NayaTextSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Error message
                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = Color.Red.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = error,
                                color = Color.Red,
                                fontSize = 14.sp,
                                fontFamily = Poppins
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

// =============================================================================
// COMPONENTS
// =============================================================================

@Composable
private fun MediaPickerButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = NayaGlass,
                shape = RoundedCornerShape(12.dp)
            ),
        color = NayaGlass.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NayaOrange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontFamily = Poppins,
                fontWeight = FontWeight.Medium,
                color = NayaTextPrimary
            )
        }
    }
}

@Composable
private fun MediaPreviewItem(
    uri: Uri,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = NayaOrange.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "Selected media",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
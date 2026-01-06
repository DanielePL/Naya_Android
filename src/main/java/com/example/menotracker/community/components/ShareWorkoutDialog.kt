package com.example.menotracker.community.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.core.content.FileProvider
import com.example.menotracker.community.util.CommunityFeatureFlag
import java.io.File

// Colors matching the app theme
private val darkBackground = Color(0xFF1A1A1A)
private val cardBackground = Color(0xFF252525)
private val violetAccent = Color(0xFFA78BFA)
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFFB0B0B0)

/**
 * Data class representing a video available for sharing
 */
data class ShareableVideo(
    val path: String,
    val exerciseName: String,
    val setNumber: Int
)

/**
 * Dialog shown after workout completion to share to community.
 * Only appears when CommunityFeatureFlag.SHARE_DIALOG_ENABLED is true.
 */
@Composable
fun ShareWorkoutDialog(
    workoutName: String,
    totalVolumeKg: Double,
    totalSets: Int,
    totalReps: Int,
    durationMinutes: Int,
    prsAchieved: Int,
    prExercises: List<String>,
    availableVideos: List<ShareableVideo> = emptyList(),
    onShare: (caption: String, selectedVideoPaths: List<String>) -> Unit,
    onSkip: () -> Unit
) {
    // Don't show if feature is disabled
    if (!CommunityFeatureFlag.ENABLED || !CommunityFeatureFlag.SHARE_DIALOG_ENABLED) {
        // Auto-skip when disabled
        LaunchedEffect(Unit) {
            onSkip()
        }
        return
    }

    var caption by remember { mutableStateOf("") }
    // Track selected videos - default to all selected
    var selectedVideos by remember { mutableStateOf(availableVideos.map { it.path }.toSet()) }

    AlertDialog(
        onDismissRequest = onSkip,
        containerColor = darkBackground,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = violetAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Share Workout?",
                    fontWeight = FontWeight.Bold,
                    color = textWhite
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Workout summary card with orange border
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = cardBackground
                    ),
                    border = BorderStroke(1.dp, violetAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = workoutName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textWhite
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatVolume(totalVolumeKg),
                                style = MaterialTheme.typography.bodySmall,
                                color = textGray
                            )
                            Text(
                                text = "$totalSets sets",
                                style = MaterialTheme.typography.bodySmall,
                                color = textGray
                            )
                            Text(
                                text = "${durationMinutes}min",
                                style = MaterialTheme.typography.bodySmall,
                                color = textGray
                            )
                        }

                        if (prsAchieved > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            PrBadge(prsAchieved = prsAchieved)
                        }
                    }
                }

                // Video selection (if videos are available)
                if (availableVideos.isNotEmpty()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = violetAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Include Videos (${selectedVideos.size}/${availableVideos.size})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textWhite
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Video thumbnails with selection
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableVideos) { video ->
                                VideoSelectionItem(
                                    video = video,
                                    isSelected = video.path in selectedVideos,
                                    onToggle = {
                                        selectedVideos = if (video.path in selectedVideos) {
                                            selectedVideos - video.path
                                        } else {
                                            selectedVideos + video.path
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Caption input with dark styling
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = {
                        Text(
                            "You crushed it! Share your thoughts...",
                            color = textGray
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardBackground,
                        unfocusedContainerColor = cardBackground,
                        focusedBorderColor = violetAccent,
                        unfocusedBorderColor = violetAccent.copy(alpha = 0.5f),
                        focusedTextColor = textWhite,
                        unfocusedTextColor = textWhite,
                        cursorColor = violetAccent
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onShare(caption, selectedVideos.toList()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = violetAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (selectedVideos.isNotEmpty()) "Share with ${selectedVideos.size} video${if (selectedVideos.size > 1) "s" else ""}"
                    else "Share",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Skip", color = violetAccent)
            }
        }
    )
}

/**
 * Individual video selection item in the share dialog
 */
@Composable
private fun VideoSelectionItem(
    video: ShareableVideo,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) violetAccent.copy(alpha = 0.3f) else cardBackground)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = if (isSelected) violetAccent else textGray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Set ${video.setNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) textWhite else textGray
            )
        }

        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(violetAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Simplified version that can be called from WorkoutCompletionScreen
 * Returns true if the dialog should be shown
 */
fun shouldShowShareDialog(): Boolean {
    return CommunityFeatureFlag.ENABLED && CommunityFeatureFlag.SHARE_DIALOG_ENABLED
}

// Platform colors
private val instagramPink = Color(0xFFE1306C)
private val instagramGradient = Color(0xFFE4405F) // Instagram brand color
private val facebookBlue = Color(0xFF1877F2)
private val tiktokBlack = Color(0xFF000000)
private val nayaCommunity = violetAccent

/**
 * Share destination enum
 */
enum class ShareDestination {
    INSTAGRAM,
    FACEBOOK,
    TIKTOK,
    COMMUNITY,
    OTHER
}

/**
 * Enhanced share dialog with social media options
 */
@Composable
fun EnhancedShareWorkoutDialog(
    workoutName: String,
    totalVolumeKg: Double,
    totalSets: Int,
    totalReps: Int,
    durationMinutes: Int,
    prsAchieved: Int,
    prExercises: List<String>,
    availableVideos: List<ShareableVideo> = emptyList(),
    isSharing: Boolean = false,
    onShareToCommunity: (caption: String, selectedVideoPaths: List<String>) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var caption by remember { mutableStateOf("") }
    var selectedVideos by remember { mutableStateOf(availableVideos.map { it.path }.toSet()) }

    // Generate share text
    val shareText = remember(workoutName, totalVolumeKg, totalSets, totalReps, durationMinutes, prsAchieved, caption) {
        buildString {
            append("Just crushed my $workoutName workout!\n\n")
            append("${formatVolume(totalVolumeKg)} • $totalSets sets • $totalReps reps • ${durationMinutes}min\n")
            if (prsAchieved > 0) {
                append("🏆 $prsAchieved PR${if (prsAchieved > 1) "s" else ""} achieved!\n")
            }
            if (caption.isNotBlank()) {
                append("\n$caption\n")
            }
            append("\n#Naya #FitnessJourney #Workout")
        }
    }

    AlertDialog(
        onDismissRequest = onSkip,
        containerColor = darkBackground,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = violetAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Share Your Workout",
                    fontWeight = FontWeight.Bold,
                    color = textWhite
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Workout summary card
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    border = BorderStroke(1.dp, violetAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = workoutName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatVolume(totalVolumeKg), style = MaterialTheme.typography.bodySmall, color = textGray)
                            Text("$totalSets sets", style = MaterialTheme.typography.bodySmall, color = textGray)
                            Text("${durationMinutes}min", style = MaterialTheme.typography.bodySmall, color = textGray)
                        }
                        if (prsAchieved > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            PrBadge(prsAchieved = prsAchieved)
                        }
                    }
                }

                // Caption input
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = { Text("Add a caption...", color = textGray) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardBackground,
                        unfocusedContainerColor = cardBackground,
                        focusedBorderColor = violetAccent,
                        unfocusedBorderColor = violetAccent.copy(alpha = 0.5f),
                        focusedTextColor = textWhite,
                        unfocusedTextColor = textWhite,
                        cursorColor = violetAccent
                    )
                )

                // Share destination buttons
                Text(
                    "Share to:",
                    style = MaterialTheme.typography.labelMedium,
                    color = textGray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Instagram
                    SharePlatformButton(
                        label = "Story",
                        color = instagramGradient,
                        icon = { InstagramIcon() },
                        onClick = {
                            shareToInstagramStory(
                                context = context,
                                text = shareText,
                                videoPath = selectedVideos.firstOrNull()
                            )
                        }
                    )

                    // Facebook
                    SharePlatformButton(
                        label = "Facebook",
                        color = facebookBlue,
                        icon = { FacebookIcon() },
                        onClick = {
                            shareToExternalApp(
                                context = context,
                                text = shareText,
                                videoPath = selectedVideos.firstOrNull(),
                                packageName = "com.facebook.katana"
                            )
                        }
                    )

                    // TikTok
                    SharePlatformButton(
                        label = "TikTok",
                        color = tiktokBlack,
                        icon = { TikTokIcon() },
                        onClick = {
                            shareToExternalApp(
                                context = context,
                                text = shareText,
                                videoPath = selectedVideos.firstOrNull(),
                                packageName = "com.zhiliaoapp.musically"
                            )
                        }
                    )

                    // More (generic share)
                    SharePlatformButton(
                        label = "More",
                        color = textGray,
                        icon = { MoreIcon() },
                        onClick = {
                            shareToExternalApp(
                                context = context,
                                text = shareText,
                                videoPath = selectedVideos.firstOrNull(),
                                packageName = null // Opens system share sheet
                            )
                        }
                    )
                }

                // Divider
                HorizontalDivider(color = textGray.copy(alpha = 0.3f))

                // Community share button (full width, prominent)
                Button(
                    onClick = { onShareToCommunity(caption, selectedVideos.toList()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSharing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = violetAccent,
                        contentColor = Color.White,
                        disabledContainerColor = violetAccent.copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uploading...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share to Naya Community", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Skip", color = textGray)
            }
        }
    )
}

/**
 * Platform share button with custom icon
 */
@Composable
private fun SharePlatformButton(
    label: String,
    color: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textWhite,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Instagram icon (camera with gradient look)
 */
@Composable
private fun InstagramIcon() {
    // Instagram camera icon representation
    Canvas(modifier = Modifier.size(28.dp)) {
        val canvasSize = size.minDimension
        val strokeWidth = canvasSize * 0.08f

        // Outer rounded rectangle
        drawRoundRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(strokeWidth, strokeWidth),
            size = androidx.compose.ui.geometry.Size(canvasSize - strokeWidth * 2, canvasSize - strokeWidth * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(canvasSize * 0.28f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )

        // Center circle (lens)
        drawCircle(
            color = Color.White,
            radius = canvasSize * 0.22f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )

        // Small dot (flash)
        drawCircle(
            color = Color.White,
            radius = canvasSize * 0.06f,
            center = androidx.compose.ui.geometry.Offset(canvasSize * 0.72f, canvasSize * 0.28f)
        )
    }
}

/**
 * Facebook icon (f letter)
 */
@Composable
private fun FacebookIcon() {
    Text(
        text = "f",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
    )
}

/**
 * TikTok icon (musical note style)
 */
@Composable
private fun TikTokIcon() {
    // TikTok uses a musical note-like design
    Canvas(modifier = Modifier.size(26.dp)) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Main vertical bar
        drawRoundRect(
            color = Color.White,
            topLeft = androidx.compose.ui.geometry.Offset(canvasWidth * 0.45f, canvasHeight * 0.1f),
            size = androidx.compose.ui.geometry.Size(canvasWidth * 0.15f, canvasHeight * 0.7f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
        )

        // Bottom circle (note head)
        drawCircle(
            color = Color.White,
            radius = canvasWidth * 0.2f,
            center = androidx.compose.ui.geometry.Offset(canvasWidth * 0.35f, canvasHeight * 0.8f)
        )

        // Top curved part
        drawArc(
            color = Color.White,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(canvasWidth * 0.5f, canvasHeight * 0.05f),
            size = androidx.compose.ui.geometry.Size(canvasWidth * 0.4f, canvasHeight * 0.3f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = canvasWidth * 0.12f)
        )
    }
}

/**
 * More/Share icon
 */
@Composable
private fun MoreIcon() {
    Icon(
        imageVector = Icons.Default.MoreHoriz,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(28.dp)
    )
}

/**
 * Share to external app using Android's share intent
 */
private fun shareToExternalApp(
    context: Context,
    text: String,
    videoPath: String?,
    packageName: String?
) {
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        type = if (videoPath != null) "video/*" else "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)

        // Add video if available
        videoPath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } catch (e: Exception) {
                android.util.Log.e("ShareWorkout", "Error sharing video: ${e.message}")
            }
        }

        // Target specific app if provided
        packageName?.let { pkg ->
            setPackage(pkg)
        }
    }

    try {
        if (packageName != null) {
            // Try to open specific app
            context.startActivity(intent)
        } else {
            // Open system share sheet
            context.startActivity(Intent.createChooser(intent, "Share your workout"))
        }
    } catch (e: Exception) {
        // App not installed, fall back to generic share
        android.util.Log.w("ShareWorkout", "App not installed ($packageName), opening share sheet")
        val fallbackIntent = Intent(intent).apply { setPackage(null) }
        context.startActivity(Intent.createChooser(fallbackIntent, "Share your workout"))
    }
}

/**
 * Share to Instagram Story specifically
 * This opens Instagram's story creation screen instead of DMs
 * Note: Instagram Stories REQUIRES media content (video/image) - text only won't work
 */
private fun shareToInstagramStory(
    context: Context,
    text: String,
    videoPath: String?
) {
    android.util.Log.d("ShareWorkout", "shareToInstagramStory called with videoPath: $videoPath")

    // Check if it's a local file or a remote URL
    val isRemoteUrl = videoPath?.startsWith("http") == true
    val videoFile = if (!isRemoteUrl) videoPath?.let { File(it) }?.takeIf { it.exists() } else null

    if (videoFile != null) {
        // We have a LOCAL video file - share to Instagram Story
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                videoFile
            )

            val storyIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                setDataAndType(uri, "video/*")
                setPackage("com.instagram.android")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Add source application identifier
                putExtra("source_application", context.packageName)
            }

            context.startActivity(storyIntent)
            android.util.Log.d("ShareWorkout", "Instagram Story intent launched with local video")
        } catch (e: Exception) {
            android.util.Log.e("ShareWorkout", "Instagram Story failed: ${e.message}")
            // Fallback to regular Instagram share
            shareToInstagramFeed(context, text, videoPath)
        }
    } else {
        // No local video (either remote URL or no video at all)
        // Use regular Instagram share - Instagram will handle the text
        android.util.Log.d("ShareWorkout", "No local video (remote=$isRemoteUrl), using regular Instagram share")
        shareToInstagramFeed(context, text, null) // Don't pass remote URL as it won't work
    }
}

/**
 * Share to Instagram - copies text to clipboard and opens Instagram app
 * Instagram doesn't support text-only shares via Intent, so we use clipboard
 */
private fun shareToInstagramFeed(
    context: Context,
    text: String,
    videoPath: String?
) {
    // Copy text to clipboard
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Workout Stats", text)
    clipboard.setPrimaryClip(clip)

    // Show toast telling user text is copied
    Toast.makeText(context, "Caption copied! Paste in your post", Toast.LENGTH_LONG).show()

    try {
        // Use getLaunchIntentForPackage to get the main launcher intent
        // This opens Instagram's main screen (feed) without any share behavior
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.instagram.android")

        if (launchIntent != null) {
            // Clear any existing task flags and start fresh
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(launchIntent)
            android.util.Log.d("ShareWorkout", "Opened Instagram via getLaunchIntentForPackage")
        } else {
            // Instagram not installed, open in browser
            android.util.Log.w("ShareWorkout", "Instagram not installed, opening web")
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/"))
            context.startActivity(webIntent)
        }
    } catch (e: Exception) {
        android.util.Log.e("ShareWorkout", "Failed to open Instagram: ${e.message}")
        // Fallback to browser
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/"))
            context.startActivity(webIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Could not open Instagram", Toast.LENGTH_SHORT).show()
        }
    }
}

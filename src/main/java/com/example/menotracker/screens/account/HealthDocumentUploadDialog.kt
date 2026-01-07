package com.example.menotracker.screens.account

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.menotracker.data.models.HealthDocumentType
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val primaryColor = NayaPrimary
private val accentColor = NayaOrangeGlow

/**
 * Dialog zum Hochladen von Gesundheitsdokumenten
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDocumentUploadDialog(
    onDismiss: () -> Unit,
    onUpload: (
        file: File,
        documentType: HealthDocumentType,
        title: String,
        documentDate: String?,
        notes: String?
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<Long?>(null) }
    var selectedDocumentType by remember { mutableStateOf(HealthDocumentType.OTHER) }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var documentDateString by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it

            // Get file name and size
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (nameIndex >= 0) {
                        selectedFileName = cursor.getString(nameIndex)
                        // Auto-fill title from filename if empty
                        if (title.isEmpty()) {
                            title = selectedFileName?.substringBeforeLast(".") ?: ""
                        }
                    }
                    if (sizeIndex >= 0) {
                        selectedFileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        }
    }

    // Date picker state
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    Dialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upload Document",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isUploading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                HorizontalDivider()

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // File Selection Area
                    SectionHeader(
                        icon = Icons.Default.UploadFile,
                        title = "Select File"
                    )

                    if (selectedUri != null) {
                        // File Selected
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = primaryColor.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (selectedFileName?.endsWith(".pdf") == true) {
                                        Icons.Default.PictureAsPdf
                                    } else {
                                        Icons.Default.Image
                                    },
                                    contentDescription = null,
                                    tint = if (selectedFileName?.endsWith(".pdf") == true) {
                                        Color(0xFFEF4444)
                                    } else {
                                        Color(0xFF10B981)
                                    },
                                    modifier = Modifier.size(40.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedFileName ?: "Unknown file",
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    selectedFileSize?.let { size ->
                                        Text(
                                            text = formatFileSize(size),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        selectedUri = null
                                        selectedFileName = null
                                        selectedFileSize = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // Change file button
                        TextButton(
                            onClick = { filePickerLauncher.launch("*/*") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Choose different file")
                        }
                    } else {
                        // Upload Area
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = 2.dp,
                                    color = accentColor.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { filePickerLauncher.launch("*/*") },
                            color = primaryColor.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Tap to select a file",
                                    fontWeight = FontWeight.Medium,
                                    color = accentColor
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "PDF, JPG, PNG supported",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Document Type Section
                    SectionHeader(
                        icon = Icons.Default.Category,
                        title = "Document Type"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(HealthDocumentType.entries) { type ->
                            DocumentTypeChip(
                                type = type,
                                isSelected = selectedDocumentType == type,
                                onClick = { selectedDocumentType = type }
                            )
                        }
                    }

                    // Title Field
                    SectionHeader(
                        icon = Icons.Default.Title,
                        title = "Title"
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., Hormone Test March 2024") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Document Date Section
                    SectionHeader(
                        icon = Icons.Default.CalendarToday,
                        title = "Document Date",
                        subtitle = "Optional - date of the test or report"
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (documentDateString.isNotEmpty()) {
                                    formatDisplayDate(documentDateString)
                                } else {
                                    "Select date (optional)"
                                },
                                color = if (documentDateString.isNotEmpty()) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (documentDateString.isNotEmpty()) {
                                    IconButton(
                                        onClick = { documentDateString = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear date",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = null,
                                    tint = accentColor
                                )
                            }
                        }
                    }

                    // Notes Field
                    SectionHeader(
                        icon = Icons.Default.Notes,
                        title = "Notes",
                        subtitle = "Optional"
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        placeholder = { Text("Add any notes about this document...") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Footer with Upload Button
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isUploading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            selectedUri?.let { uri ->
                                isUploading = true
                                try {
                                    // Create temp file from URI
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    if (inputStream != null) {
                                        val extension = selectedFileName?.substringAfterLast(".") ?: "bin"
                                        val tempFile = File.createTempFile("upload_", ".$extension", context.cacheDir)
                                        tempFile.outputStream().use { outputStream ->
                                            inputStream.copyTo(outputStream)
                                        }
                                        inputStream.close()

                                        onUpload(
                                            tempFile,
                                            selectedDocumentType,
                                            title.ifEmpty { selectedFileName ?: "Document" },
                                            documentDateString.ifEmpty { null },
                                            notes.ifEmpty { null }
                                        )
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("DocumentUpload", "Error creating temp file", e)
                                    isUploading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedUri != null && title.isNotEmpty() && !isUploading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Uploading...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Upload")
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                            documentDateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun DocumentTypeChip(
    type: HealthDocumentType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = getDocumentTypeIcon(type),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = type.displayName,
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

private fun getDocumentTypeIcon(type: HealthDocumentType): ImageVector {
    return when (type) {
        HealthDocumentType.HORMONE_TEST -> Icons.Default.Science
        HealthDocumentType.DEXA_SCAN -> Icons.Default.Accessibility
        HealthDocumentType.MEDICAL_REPORT -> Icons.Default.Description
        HealthDocumentType.LAB_RESULT -> Icons.Default.Biotech
        HealthDocumentType.ULTRASOUND -> Icons.Default.GraphicEq
        HealthDocumentType.MAMMOGRAM -> Icons.Default.MedicalServices
        HealthDocumentType.OTHER -> Icons.Default.InsertDriveFile
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

private fun formatDisplayDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    } catch (e: Exception) {
        dateString
    }
}
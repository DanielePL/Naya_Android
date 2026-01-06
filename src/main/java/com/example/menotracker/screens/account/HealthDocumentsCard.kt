package com.example.menotracker.screens.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.data.models.HealthDocument
import com.example.menotracker.data.models.HealthDocumentType
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow
import com.example.menotracker.ui.theme.glassCardAccent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val primaryColor = NayaPrimary
private val accentColor = NayaOrangeGlow

/**
 * Card displaying the user's health documents
 * Allows viewing, adding, and managing medical documents
 */
@Composable
fun HealthDocumentsCard(
    documents: List<HealthDocument>,
    isLoading: Boolean = false,
    onAddClick: () -> Unit,
    onDocumentClick: (HealthDocument) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
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
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "MY DOCUMENTS",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                    if (documents.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = primaryColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${documents.size}",
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add document",
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (isLoading) {
                // Loading State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = accentColor,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else if (documents.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No Documents",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Store hormone tests, lab reports and more",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = onAddClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Upload First Document")
                    }
                }
            } else {
                // Document Type Summary
                val documentsByType = documents.groupBy { it.documentTypeEnum }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    documentsByType.entries.take(4).forEach { (type, docs) ->
                        DocumentTypeChip(
                            type = type,
                            count = docs.size,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                )

                // Expand/Collapse
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Show All Documents",
                            color = accentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = accentColor,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(if (isExpanded) 180f else 0f)
                    )
                }

                // Expanded Document List
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        documents.sortedByDescending { it.documentDate ?: it.createdAt }
                            .take(10)
                            .forEach { document ->
                                DocumentListItem(
                                    document = document,
                                    onClick = { onDocumentClick(document) }
                                )
                            }

                        if (documents.size > 10) {
                            Text(
                                text = "...and ${documents.size - 10} more",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentTypeChip(
    type: HealthDocumentType,
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = primaryColor.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = getDocumentTypeIcon(type),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "$count",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = type.displayName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DocumentListItem(
    document: HealthDocument,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Document Icon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = primaryColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = getDocumentTypeIcon(document.documentTypeEnum),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Document Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = document.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = document.documentTypeEnum.displayName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    document.documentDate?.let { date ->
                        Text(
                            text = formatDate(date),
                            color = accentColor,
                            fontSize = 11.sp
                        )
                    }
                    document.formattedFileSize.takeIf { it.isNotEmpty() }?.let { size ->
                        Text(
                            text = size,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // File type indicator
            Icon(
                imageVector = if (document.isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                contentDescription = null,
                tint = if (document.isPdf) Color(0xFFEF4444) else Color(0xFF10B981),
                modifier = Modifier.size(18.dp)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
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

private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    } catch (e: Exception) {
        dateString
    }
}

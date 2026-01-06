// src/main/java/com/example/myapplicationtest/screens/account/SettingsDialogs.kt

package com.example.menotracker.screens.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow

private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow

// ═══════════════════════════════════════════════════════════════
// GUEST UPLOAD DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
fun GuestUploadDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = orangeGlow,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Login Required",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Profile picture upload is not available in Guest Mode. Please create an account or log in to upload a profile picture.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = orangePrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Got it")
            }
        },
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ═══════════════════════════════════════════════════════════════
// THEME DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
fun ThemeDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "Select Theme",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionOption(
                    label = "Dark Mode",
                    isSelected = currentTheme == "Dark",
                    onClick = { onSelect("Dark") }
                )
                SelectionOption(
                    label = "Light Mode",
                    isSelected = currentTheme == "Light",
                    onClick = { onSelect("Light") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = orangeGlow)
            }
        },
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ═══════════════════════════════════════════════════════════════
// LANGUAGE DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
fun LanguageDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Text(
                "Select Language",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionOption(
                    label = "System",
                    isSelected = currentLanguage == "system",
                    onClick = { onSelect("system") }
                )
                SelectionOption(
                    label = "English",
                    isSelected = currentLanguage == "en",
                    onClick = { onSelect("en") }
                )
                SelectionOption(
                    label = "Deutsch",
                    isSelected = currentLanguage == "de",
                    onClick = { onSelect("de") }
                )
                SelectionOption(
                    label = "Français",
                    isSelected = currentLanguage == "fr",
                    onClick = { onSelect("fr") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = orangeGlow)
            }
        },
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ═══════════════════════════════════════════════════════════════
// UNITS DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
fun UnitsDialog(
    currentUnit: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Text(
                "Select Units",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionOption(
                    label = "Metric (kg, cm)",
                    isSelected = currentUnit == "Metric",
                    onClick = { onSelect("Metric") }
                )
                SelectionOption(
                    label = "Imperial (lbs, inch)",
                    isSelected = currentUnit == "Imperial",
                    onClick = { onSelect("Imperial") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = orangeGlow)
            }
        },
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ═══════════════════════════════════════════════════════════════
// WORKOUT DURATION DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
fun WorkoutDurationDialog(
    currentDuration: Int?,
    onDismiss: () -> Unit,
    onSelect: (Int?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Text(
                "Workout Duration Target",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Set your target workout duration. The timer will turn red when you exceed this time.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SelectionOption(
                    label = "No target",
                    isSelected = currentDuration == null,
                    onClick = { onSelect(null) }
                )
                SelectionOption(
                    label = "60 minutes",
                    isSelected = currentDuration == 60,
                    onClick = { onSelect(60) }
                )
                SelectionOption(
                    label = "75 minutes",
                    isSelected = currentDuration == 75,
                    onClick = { onSelect(75) }
                )
                SelectionOption(
                    label = "90 minutes",
                    isSelected = currentDuration == 90,
                    onClick = { onSelect(90) }
                )
                SelectionOption(
                    label = "120 minutes",
                    isSelected = currentDuration == 120,
                    onClick = { onSelect(120) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = orangeGlow)
            }
        },
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ═══════════════════════════════════════════════════════════════
// ABOUT DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        icon = {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = orangeGlow,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Naya",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Version 1.0.0",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Text(
                    "Your ultimate workout companion for tracking, planning, and crushing your fitness goals.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "© 2024 Naya Wellness",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = orangePrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Got it")
            }
        },
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ═══════════════════════════════════════════════════════════════
// HELP DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        icon = {
            Icon(
                imageVector = Icons.Default.Help,
                contentDescription = null,
                tint = orangeGlow,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Help & Support",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HelpItem(
                    icon = Icons.Default.Email,
                    title = "Email Support",
                    subtitle = "support@naya.app"
                )
                HelpItem(
                    icon = Icons.Default.QuestionAnswer,
                    title = "FAQ",
                    subtitle = "Find answers to common questions"
                )
                HelpItem(
                    icon = Icons.Default.Group,
                    title = "Community",
                    subtitle = "Join our Discord server"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = orangePrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close")
            }
        },
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ═══════════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════════

@Composable
internal fun SelectionOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val unselectedColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) orangePrimary.copy(alpha = 0.2f) else unselectedColor
        ),
        border = if (isSelected) BorderStroke(2.dp, orangePrimary) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (isSelected) orangeGlow else textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = orangeGlow,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun HelpItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = orangeGlow,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// REFERRAL CODE DIALOG
// ═══════════════════════════════════════════════════════════════

sealed class ReferralCodeStatus {
    object Idle : ReferralCodeStatus()
    object Validating : ReferralCodeStatus()
    data class Valid(val partnerName: String, val partnerType: String) : ReferralCodeStatus()
    object Invalid : ReferralCodeStatus()
    data class Error(val message: String) : ReferralCodeStatus()
    object AlreadyUsed : ReferralCodeStatus()
    object Success : ReferralCodeStatus()
}

@Composable
fun ReferralCodeDialog(
    currentPartnerName: String?,
    status: ReferralCodeStatus,
    onDismiss: () -> Unit,
    onValidateCode: (String) -> Unit,
    onApplyCode: (String) -> Unit
) {
    var codeText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = null,
                tint = orangeGlow,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Partner Referral Code",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show current partner if exists
                if (currentPartnerName != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    "Active Partner",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    currentPartnerName,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "Enter a referral code from your coach, gym, or favorite fitness influencer to support them!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                // Code input field
                OutlinedTextField(
                    value = codeText,
                    onValueChange = {
                        codeText = it.uppercase().filter { char -> char.isLetterOrDigit() }
                    },
                    label = { Text("Referral Code") },
                    placeholder = { Text("e.g. COACH2024") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = orangeGlow
                        )
                    },
                    trailingIcon = {
                        if (codeText.isNotEmpty() && status !is ReferralCodeStatus.Validating) {
                            IconButton(onClick = { onValidateCode(codeText) }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Validate",
                                    tint = orangeGlow
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = currentPartnerName == null && status !is ReferralCodeStatus.Validating,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = orangePrimary,
                        focusedLabelColor = orangePrimary,
                        cursorColor = orangePrimary,
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Status feedback
                when (status) {
                    is ReferralCodeStatus.Validating -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = orangeGlow
                            )
                            Text(
                                "Validating code...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                    is ReferralCodeStatus.Valid -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = orangePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = orangeGlow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "Valid Code!",
                                        color = orangeGlow,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    "Partner: ${status.partnerName}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "Type: ${status.partnerType.replaceFirstChar { it.uppercase() }}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    is ReferralCodeStatus.Invalid -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Invalid or expired code",
                                color = Color.Red,
                                fontSize = 13.sp
                            )
                        }
                    }
                    is ReferralCodeStatus.AlreadyUsed -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFFA726),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "You've already applied a referral code",
                                color = Color(0xFFFFA726),
                                fontSize = 13.sp
                            )
                        }
                    }
                    is ReferralCodeStatus.Error -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                status.message,
                                color = Color.Red,
                                fontSize = 13.sp
                            )
                        }
                    }
                    is ReferralCodeStatus.Success -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Celebration,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Referral code applied successfully!",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    is ReferralCodeStatus.Idle -> {
                        // No status message
                    }
                }
            }
        },
        confirmButton = {
            if (status is ReferralCodeStatus.Valid && currentPartnerName == null) {
                Button(
                    onClick = { onApplyCode(codeText) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = orangePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Apply Code")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(
                        if (status is ReferralCodeStatus.Success) "Done" else "Close",
                        color = orangeGlow
                    )
                }
            }
        },
        dismissButton = {
            if (status is ReferralCodeStatus.Valid && currentPartnerName == null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}
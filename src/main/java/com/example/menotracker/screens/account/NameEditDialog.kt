package com.example.menotracker.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow

private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow
private val textWhite = Color(0xFFFFFFFF)
private val textGray = Color(0xFF999999)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameEditDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = orangeGlow,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Edit Name",
                    color = textWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = textGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textWhite,
                        unfocusedTextColor = textWhite,
                        focusedBorderColor = orangeGlow,
                        unfocusedBorderColor = textGray.copy(alpha = 0.3f),
                        cursorColor = orangeGlow
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = orangePrimary
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = name.isNotBlank()
            ) {
                Text("Save", color = textWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textGray)
            }
        }
    )
}
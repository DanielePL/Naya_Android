package com.example.menotracker.onboarding.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.menotracker.onboarding.components.*
import com.example.menotracker.ui.theme.*

/**
 * Registration Screen - shown after Paywall in onboarding
 * Collects: Name, Email, Password (with confirmation)
 *
 * Uses shared OnboardingComponents for consistent design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null,
    showLoginButton: Boolean = false,
    onRegister: (name: String, email: String, password: String) -> Unit,
    onBack: () -> Unit,
    onGoToLogin: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    // Validation
    val isNameValid = name.trim().length >= 2
    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.length >= 6
    val doPasswordsMatch = password == confirmPassword && confirmPassword.isNotEmpty()
    val isFormValid = isNameValid && isEmailValid && isPasswordValid && doPasswordsMatch

    OnboardingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(OnboardingTokens.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NayaTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))

            // Title
            OnboardingTitle(
                title = "Create Your Account",
                subtitle = "One last step to unlock your training"
            )

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingXl))

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; localError = null },
                label = { Text("Your Name") },
                placeholder = { Text("How should we call you?") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isNameValid) NayaPrimary else NayaTextTertiary
                    )
                },
                trailingIcon = {
                    if (name.isNotEmpty() && isNameValid) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Valid",
                            tint = NayaSuccess
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NayaPrimary,
                    unfocusedBorderColor = NayaTextTertiary.copy(alpha = 0.5f),
                    focusedLabelColor = NayaPrimary,
                    unfocusedLabelColor = NayaTextTertiary,
                    cursorColor = NayaPrimary,
                    focusedTextColor = NayaTextPrimary,
                    unfocusedTextColor = NayaTextPrimary
                ),
                shape = RoundedCornerShape(OnboardingTokens.radiusSmall),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim(); localError = null },
                label = { Text("Email") },
                placeholder = { Text("your@email.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = if (isEmailValid) NayaPrimary else NayaTextTertiary
                    )
                },
                trailingIcon = {
                    if (email.isNotEmpty()) {
                        if (isEmailValid) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Valid",
                                tint = NayaSuccess
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Invalid",
                                tint = NayaError.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NayaPrimary,
                    unfocusedBorderColor = NayaTextTertiary.copy(alpha = 0.5f),
                    focusedLabelColor = NayaPrimary,
                    unfocusedLabelColor = NayaTextTertiary,
                    cursorColor = NayaPrimary,
                    focusedTextColor = NayaTextPrimary,
                    unfocusedTextColor = NayaTextPrimary
                ),
                shape = RoundedCornerShape(OnboardingTokens.radiusSmall),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; localError = null },
                label = { Text("Password") },
                placeholder = { Text("Min. 6 characters") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isPasswordValid) NayaPrimary else NayaTextTertiary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = NayaTextTertiary
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NayaPrimary,
                    unfocusedBorderColor = NayaTextTertiary.copy(alpha = 0.5f),
                    focusedLabelColor = NayaPrimary,
                    unfocusedLabelColor = NayaTextTertiary,
                    cursorColor = NayaPrimary,
                    focusedTextColor = NayaTextPrimary,
                    unfocusedTextColor = NayaTextPrimary
                ),
                shape = RoundedCornerShape(OnboardingTokens.radiusSmall),
                modifier = Modifier.fillMaxWidth()
            )

            // Password strength indicator
            if (password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    val strength = when {
                        password.length < 6 -> "Too short" to NayaError
                        password.length < 8 -> "Weak" to NayaWarning
                        password.length < 12 && password.any { it.isDigit() } -> "Good" to Color(0xFF8BC34A)
                        password.length >= 12 && password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() } -> "Strong" to NayaSuccess
                        else -> "Good" to Color(0xFF8BC34A)
                    }
                    Text(
                        text = strength.first,
                        style = MaterialTheme.typography.bodySmall,
                        color = strength.second,
                        fontFamily = Poppins
                    )
                }
            }

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))

            // Confirm Password Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; localError = null },
                label = { Text("Confirm Password") },
                placeholder = { Text("Repeat your password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (doPasswordsMatch) NayaPrimary else NayaTextTertiary
                    )
                },
                trailingIcon = {
                    Row {
                        // Match indicator
                        if (confirmPassword.isNotEmpty()) {
                            if (doPasswordsMatch) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Passwords match",
                                    tint = NayaSuccess,
                                    modifier = Modifier.padding(end = OnboardingTokens.spacingSm)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Passwords don't match",
                                    tint = NayaError.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(end = OnboardingTokens.spacingSm)
                                )
                            }
                        }
                        // Visibility toggle
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                tint = NayaTextTertiary
                            )
                        }
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                isError = confirmPassword.isNotEmpty() && !doPasswordsMatch,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (isFormValid) {
                            onRegister(name.trim(), email.trim(), password)
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (confirmPassword.isNotEmpty() && !doPasswordsMatch) NayaError else NayaPrimary,
                    unfocusedBorderColor = if (confirmPassword.isNotEmpty() && !doPasswordsMatch) NayaError.copy(alpha = 0.5f) else NayaTextTertiary.copy(alpha = 0.5f),
                    focusedLabelColor = NayaPrimary,
                    unfocusedLabelColor = NayaTextTertiary,
                    cursorColor = NayaPrimary,
                    focusedTextColor = NayaTextPrimary,
                    unfocusedTextColor = NayaTextPrimary,
                    errorBorderColor = NayaError
                ),
                shape = RoundedCornerShape(OnboardingTokens.radiusSmall),
                modifier = Modifier.fillMaxWidth()
            )

            // Error message for password mismatch
            if (confirmPassword.isNotEmpty() && !doPasswordsMatch) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Passwords don't match",
                    style = MaterialTheme.typography.bodySmall,
                    color = NayaError,
                    fontFamily = Poppins
                )
            }

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingLg))

            // Success Message (Email confirmation required)
            if (successMessage != null) {
                val context = LocalContext.current

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = NayaSuccess.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(OnboardingTokens.radiusSmall),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(OnboardingTokens.spacingLg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MarkEmailRead,
                            contentDescription = null,
                            tint = NayaSuccess,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))
                        Text(
                            text = "Check Your Email",
                            style = MaterialTheme.typography.titleMedium,
                            color = NayaTextPrimary,
                            textAlign = TextAlign.Center,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(OnboardingTokens.spacingSm))
                        Text(
                            text = "We sent a confirmation link to\n$email",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NayaTextSecondary,
                            textAlign = TextAlign.Center,
                            fontFamily = Poppins
                        )
                        Spacer(modifier = Modifier.height(OnboardingTokens.spacingLg))

                        // Open Email App Button
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_APP_EMAIL)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback: open Gmail directly
                                    val gmailIntent = Intent(Intent.ACTION_MAIN).apply {
                                        setPackage("com.google.android.gm")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(gmailIntent)
                                    } catch (e2: Exception) {
                                        // Ignore if no email app found
                                    }
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = NayaPrimary
                            ),
                            shape = RoundedCornerShape(OnboardingTokens.radiusSmall),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open Email App",
                                fontFamily = Poppins,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))
            }

            // Error Message
            val displayError = localError ?: errorMessage
            if (displayError != null && successMessage == null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = NayaError.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(OnboardingTokens.spacingSm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = displayError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NayaError,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(OnboardingTokens.spacingSm + 4.dp),
                        fontFamily = Poppins
                    )
                }
                Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Show "Go to Login" button after successful registration, or "Create Account" button
            if (showLoginButton && onGoToLogin != null) {
                OnboardingPrimaryButton(
                    text = "Go to Login",
                    onClick = onGoToLogin,
                    showArrow = false
                )
            } else {
                // Create Account Button
                Button(
                    onClick = {
                        localError = null
                        when {
                            !isNameValid -> localError = "Please enter your name (at least 2 characters)"
                            !isEmailValid -> localError = "Please enter a valid email address"
                            !isPasswordValid -> localError = "Password must be at least 6 characters"
                            !doPasswordsMatch -> localError = "Passwords don't match"
                            else -> onRegister(name.trim(), email.trim(), password)
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NayaPrimary,
                        disabledContainerColor = NayaPrimary.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(OnboardingTokens.radiusSmall),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(OnboardingTokens.buttonHeight)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = NayaTextPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Create Account",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = Poppins
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingMd))

            // Terms text with clickable links
            val uriHandler = LocalUriHandler.current
            val termsUrl = "https://naya.app/terms"
            val privacyUrl = "https://naya.app/privacy"

            val annotatedText = buildAnnotatedString {
                append("By creating an account, you agree to our ")

                pushStringAnnotation(tag = "terms", annotation = termsUrl)
                withStyle(style = SpanStyle(
                    color = NayaPrimary,
                    textDecoration = TextDecoration.Underline
                )) {
                    append("Terms of Service")
                }
                pop()

                append(" and ")

                pushStringAnnotation(tag = "privacy", annotation = privacyUrl)
                withStyle(style = SpanStyle(
                    color = NayaPrimary,
                    textDecoration = TextDecoration.Underline
                )) {
                    append("Privacy Policy")
                }
                pop()
            }

            ClickableText(
                text = annotatedText,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    color = NayaTextTertiary,
                    textAlign = TextAlign.Center,
                    fontFamily = Poppins
                ),
                onClick = { offset ->
                    annotatedText.getStringAnnotations(tag = "terms", start = offset, end = offset)
                        .firstOrNull()?.let { uriHandler.openUri(it.item) }
                    annotatedText.getStringAnnotations(tag = "privacy", start = offset, end = offset)
                        .firstOrNull()?.let { uriHandler.openUri(it.item) }
                }
            )

            Spacer(modifier = Modifier.height(OnboardingTokens.spacingLg))
        }
    }
}

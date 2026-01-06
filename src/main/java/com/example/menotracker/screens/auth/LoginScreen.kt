package com.example.menotracker.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.menotracker.R
import com.example.menotracker.ui.theme.AppBackground
import com.example.menotracker.viewmodels.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.example.menotracker.ui.theme.NayaPrimary
import com.example.menotracker.ui.theme.NayaOrangeGlow

// Design System
private val orangePrimary = NayaPrimary
private val orangeGlow = NayaOrangeGlow

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current

    // Google Sign-In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { idToken ->
                    viewModel.loginWithGoogle(idToken, onSuccess = onLoginSuccess)
                }
            } catch (e: ApiException) {
                android.util.Log.e("LoginScreen", "Google sign-in failed: ${e.message}")
            }
        }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Flexible spacer - pushes content to center, shrinks when keyboard appears
            Spacer(Modifier.weight(1f))

            // Logo - NAYA text branding
            Text(
                text = "NAYA",
                color = orangeGlow,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp
            )

            Text(
                text = "YOUR TRAINING COMPANION",
                color = Color.Gray,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Toggle: Login / Sign Up
            Text(
                text = if (isSignUp) "Create Account" else "Welcome Back",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(20.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = orangeGlow,
                    unfocusedBorderColor = orangePrimary.copy(alpha = 0.6f),
                    focusedLabelColor = orangeGlow,
                    unfocusedLabelColor = orangePrimary.copy(alpha = 0.7f),
                    cursorColor = orangeGlow
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = orangeGlow,
                    unfocusedBorderColor = orangePrimary.copy(alpha = 0.6f),
                    focusedLabelColor = orangeGlow,
                    unfocusedLabelColor = orangePrimary.copy(alpha = 0.7f),
                    cursorColor = orangeGlow
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Confirm Password (Sign Up Only)
            if (isSignUp) {
                Spacer(Modifier.height(16.dp))

                val passwordsMatch = password.isNotEmpty() && confirmPassword.isNotEmpty() && password == confirmPassword

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Checkmark when passwords match
                            if (passwordsMatch) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Passwords match",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            // Visibility toggle
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = orangeGlow,
                        unfocusedBorderColor = orangePrimary.copy(alpha = 0.6f),
                        focusedLabelColor = orangeGlow,
                        unfocusedLabelColor = orangePrimary.copy(alpha = 0.7f),
                        cursorColor = orangeGlow
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Forgot Password (Login Only)
            if (!isSignUp) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Forgot Password?",
                    color = orangeGlow,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { showForgotPasswordDialog = true }
                        .padding(4.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // Success Message
            if (successMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E4620)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "✅ Account Created!",
                            color = Color(0xFF4CAF50),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = successMessage!!,
                            color = Color(0xFFB8E6BA),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Error Message (from ViewModel or local)
            val displayError = localErrorMessage ?: errorMessage
            if (displayError != null) {
                Text(
                    text = displayError,
                    color = Color.Red,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Login / Sign Up Button
            val isButtonEnabled = !isLoading && email.isNotEmpty() && password.isNotEmpty()
            Button(
                onClick = {
                    localErrorMessage = null  // Clear local error
                    successMessage = null  // Clear success message

                    if (isSignUp) {
                        if (password == confirmPassword) {
                            if (password.length < 6) {
                                localErrorMessage = "Password must be at least 6 characters"
                            } else {
                                viewModel.signUpWithEmail(
                                    email = email,
                                    password = password,
                                    onSuccess = {
                                        // Auto-login successful
                                        onLoginSuccess()
                                    },
                                    onNeedsEmailConfirmation = {
                                        // Sign-up successful but needs email confirmation
                                        android.util.Log.d("LoginScreen", "✅ Switching to login mode")
                                        successMessage = "Please check your email and click the confirmation link. Then log in here."
                                        isSignUp = false  // Switch to login mode
                                        viewModel.clearError()
                                    }
                                )
                            }
                        } else {
                            localErrorMessage = "Passwords don't match"
                        }
                    } else {
                        viewModel.loginWithEmail(email, password, onSuccess = onLoginSuccess)
                    }
                },
                enabled = isButtonEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = orangePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .then(
                        if (isButtonEnabled) {
                            Modifier.shadow(
                                elevation = 16.dp,
                                spotColor = orangeGlow,
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = if (isSignUp) "Sign Up" else "Log In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
                Text(
                    text = "OR",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
            }

            Spacer(Modifier.height(16.dp))

            // Toggle Sign Up / Login
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isSignUp) "Already have an account? " else "Don't have an account? ",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    text = if (isSignUp) "Log In" else "Sign Up",
                    color = orangeGlow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        isSignUp = !isSignUp
                        localErrorMessage = null
                        successMessage = null
                        viewModel.clearError()
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            // Flexible spacer - balances the top spacer for centering
            Spacer(Modifier.weight(1f))
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            onDismiss = { showForgotPasswordDialog = false },
            onSend = { resetEmail ->
                viewModel.resetPassword(resetEmail) {
                    showForgotPasswordDialog = false
                }
            }
        )
    }
}

@Composable
private fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Text(
                "Reset Password",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Enter your email address and we'll send you a link to reset your password.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = orangePrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = orangePrimary,
                        cursorColor = orangePrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(email) },
                enabled = email.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = orangePrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Send Reset Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
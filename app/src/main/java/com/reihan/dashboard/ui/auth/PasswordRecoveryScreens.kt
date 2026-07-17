package com.reihan.dashboard.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reihan.dashboard.data.model.ResetPasswordRequest
import com.reihan.dashboard.ui.components.StatusPopup
import com.reihan.dashboard.ui.theme.*

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onCodeSent: (String) -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.LockReset, null, tint = AccentBlue, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Forgot Password", style = Typography.displaySmall.copy(fontWeight = FontWeight.Black))
            Text("Enter your email to receive a reset code", color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))
            ModernTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                icon = Icons.Outlined.Email,
                placeholder = "you@example.com",
                keyboardType = KeyboardType.Email
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { viewModel.forgotPassword(email, onCodeSent) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = DarkBg)
            ) {
                if (authState is AuthState.Loading) CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else Text("Send Reset Code", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onBack) { Text("Back to Login", color = AccentBlue) }
        }
    }
    HandleAuthState(authState, viewModel)
}

@Composable
fun ResetPasswordScreen(
    email: String,
    viewModel: AuthViewModel,
    onResetSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val authState by viewModel.authState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Reset Password", style = Typography.displaySmall.copy(fontWeight = FontWeight.Black))
            Text("Enter the 6-digit code and your new password", color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))
            ModernTextField(
                value = code,
                onValueChange = { if (it.length <= 6) code = it },
                label = "Reset Code",
                icon = Icons.Outlined.Key,
                placeholder = "123456",
                keyboardType = KeyboardType.Number
            )
            Spacer(modifier = Modifier.height(16.dp))
            ModernTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "New Password",
                icon = Icons.Outlined.Lock,
                placeholder = "Minimum 8 characters",
                isPassword = true,
                showPassword = showPassword,
                onTogglePassword = { showPassword = !showPassword }
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { 
                    viewModel.resetPassword(
                        ResetPasswordRequest(email, code, newPassword),
                        onResetSuccess
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = DarkBg),
                enabled = code.length == 6 && newPassword.isNotBlank()
            ) {
                if (authState is AuthState.Loading) CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else Text("Update Password", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onBack) { Text("Cancel", color = AccentBlue) }
        }
    }
    HandleAuthState(authState, viewModel)
}

@Composable
private fun HandleAuthState(authState: AuthState, viewModel: AuthViewModel) {
    when (val state = authState) {
        is AuthState.Error -> StatusPopup(false, state.message, { viewModel.resetState() })
        is AuthState.Success -> StatusPopup(true, state.message, { viewModel.resetState() })
        else -> {}
    }
}

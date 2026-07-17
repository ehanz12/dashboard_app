package com.reihan.dashboard.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.outlined.Key
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
import com.reihan.dashboard.ui.components.StatusPopup
import com.reihan.dashboard.ui.theme.*

@Composable
fun VerificationScreen(
    email: String,
    viewModel: AuthViewModel,
    onVerificationSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Subtle background glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = 100.dp, y = (-100).dp)
                .background(Brush.radialGradient(listOf(AccentBlue.copy(alpha = 0.05f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AccentBlue.copy(alpha = 0.1f))
                    .border(1.dp, AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Verify Your Email",
                style = Typography.displaySmall.copy(fontWeight = FontWeight.Black)
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We've sent a 6-digit verification code to\n$email",
                style = Typography.bodyMedium.copy(color = TextSecondary),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            ModernTextField(
                value = code,
                onValueChange = { if (it.length <= 6) code = it },
                label = "Verification Code",
                icon = Icons.Outlined.Key,
                placeholder = "Enter 6-digit code",
                keyboardType = KeyboardType.NumberPassword
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.verifyEmail(email, code, onVerificationSuccess)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    contentColor = DarkBg
                ),
                enabled = code.length == 6 && authState !is AuthState.Loading
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Verify & Continue",
                        style = Typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = DarkBg,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onBack) {
                Text(
                    text = "Back to Login",
                    style = Typography.bodyMedium.copy(color = AccentBlue, fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    // Status Popups
    when (val state = authState) {
        is AuthState.Error -> {
            StatusPopup(
                isSuccess = false,
                message = state.message,
                onDismiss = { viewModel.resetState() }
            )
        }
        is AuthState.Success -> {
            StatusPopup(
                isSuccess = true,
                message = state.message,
                onDismiss = { 
                    viewModel.resetState()
                }
            )
        }
        else -> {}
    }
}

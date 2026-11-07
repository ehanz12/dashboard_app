package com.reihan.dashboard.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reihan.dashboard.ui.components.NeoBadge
import com.reihan.dashboard.ui.components.NeoButton
import com.reihan.dashboard.ui.components.StatusPopup
import com.reihan.dashboard.ui.components.dotGridBackground
import com.reihan.dashboard.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onRegisterSuccess: (String) -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onGoogleSignInClick: () -> Unit = {}
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .dotGridBackground()
            .background(NeoBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Branding
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(NeoYellow, RoundedCornerShape(16.dp))
                        .border(2.5.dp, Color.Black, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wallet,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (isLoginMode) "Welcome Back" else "Join Financia",
                    style = Typography.displaySmall.copy(fontWeight = FontWeight.Black, color = Color.Black)
                )
                Text(
                    text = if (isLoginMode) "Log in to manage your assets" else "Start your journey to financial freedom",
                    style = Typography.bodyMedium.copy(color = NeoTextSecondary),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Form Fields
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!isLoginMode) {
                    ModernTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Full Name",
                        icon = Icons.Outlined.Person,
                        placeholder = "Your name"
                    )

                    ModernTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = "Phone Number",
                        icon = Icons.Outlined.Phone,
                        placeholder = "08123456789",
                        keyboardType = KeyboardType.Phone
                    )

                    ModernTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = "Bio",
                        icon = Icons.Outlined.Info,
                        placeholder = "Tell us about yourself"
                    )
                }

                ModernTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    icon = Icons.Outlined.Email,
                    placeholder = "you@example.com",
                    keyboardType = KeyboardType.Email
                )

                ModernTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    icon = Icons.Outlined.Lock,
                    placeholder = "Minimum 8 characters",
                    isPassword = true,
                    showPassword = showPassword,
                    onTogglePassword = { showPassword = !showPassword }
                )
                
                if (isLoginMode) {
                    Text(
                        text = "Forgot Password?",
                        style = Typography.bodySmall.copy(
                            color = NeoBlue,
                            fontWeight = FontWeight.Black
                        ),
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable { onForgotPasswordClick() }
                            .padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Submit Button (Neobrutal style)
            NeoButton(
                onClick = {
                    if (isLoginMode) {
                        viewModel.login(
                            email = email,
                            password = password,
                            onSuccess = onAuthSuccess,
                            onNotVerified = { targetEmail ->
                                onRegisterSuccess(targetEmail)
                            }
                        )
                    } else {
                        viewModel.register(name, email, password, phoneNumber, bio) { registeredEmail ->
                            onRegisterSuccess(registeredEmail)
                        }
                    }
                },
                containerColor = NeoYellow,
                contentColor = Color.Black,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (isLoginMode) "Log In" else "Create Account",
                        style = Typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.Black,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider with text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.Black.copy(alpha = 0.2f)
                )
                Text(
                    text = "or continue with",
                    style = Typography.bodySmall.copy(color = NeoTextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.Black.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Google Sign-In Button
            GoogleSignInButton(
                onClick = onGoogleSignInClick,
                isLoading = authState is AuthState.Loading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Toggle Mode
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        isLoginMode = !isLoginMode
                        viewModel.resetState()
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLoginMode) "New here? " else "Have an account? ",
                    style = Typography.bodyMedium.copy(color = NeoTextSecondary)
                )
                Text(
                    text = if (isLoginMode) "Sign Up" else "Log In",
                    style = Typography.bodyMedium.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
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

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = Typography.labelSmall.copy(color = Color.Black, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = NeoTextSecondary.copy(alpha = 0.5f), fontSize = 14.sp) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp)) },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                cursorColor = Color.Black,
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Black,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
            singleLine = true
        )
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    NeoButton(
        onClick = onClick,
        containerColor = Color.White,
        contentColor = Color.Black,
        modifier = modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.Black,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(NeoCyan, RoundedCornerShape(6.dp))
                        .border(1.dp, Color.Black, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("G", style = Typography.labelSmall.copy(fontWeight = FontWeight.Black, color = Color.Black))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Continue with Google",
                    style = Typography.labelLarge.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                )
            }
        }
    }
}

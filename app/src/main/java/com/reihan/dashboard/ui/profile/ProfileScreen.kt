package com.reihan.dashboard.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reihan.dashboard.ui.components.StatusPopup
import com.reihan.dashboard.ui.components.dotGridBackground
import com.reihan.dashboard.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onChangePassword: () -> Unit
) {
    val profileState by viewModel.profileState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    LaunchedEffect(profileState) {
        if (profileState is ProfileState.Loaded) {
            val loaded = profileState as ProfileState.Loaded
            name = loaded.user.name
            email = loaded.user.email
            phone = loaded.user.phoneNumber ?: ""
            bio = loaded.user.bio ?: ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .dotGridBackground()
            .background(NeoBg)
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 24.dp)
    ) {
        ProfileTopBar(onBack = onBack)

        Spacer(modifier = Modifier.height(16.dp))

        when (profileState) {
            is ProfileState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue, strokeWidth = 2.dp)
                }
            }
            is ProfileState.Error -> {
                ErrorView((profileState as ProfileState.Error).message) {
                    viewModel.loadProfile()
                }
            }
            is ProfileState.Loaded -> {
                ProfileContent(
                    name = name,
                    email = email,
                    phone = phone,
                    bio = bio,
                    oldPassword = oldPassword,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword,
                    showPassword = showPassword,
                    actionState = actionState,
                    onNameChange = { name = it },
                    onBioChange = { bio = it },
                    onPhoneChange = { phone = it },
                    onEmailChange = { email = it },
                    onOldPasswordChange = { oldPassword = it },
                    onNewPasswordChange = { newPassword = it },
                    onConfirmPasswordChange = { confirmPassword = it },
                    onToggleShowPassword = { showPassword = !showPassword },
                    onSaveProfile = {
                        if (name.isBlank() || email.isBlank()) return@ProfileContent
                        viewModel.editProfile(name.trim(), email.trim(), phone.trim(), bio.trim())
                    },
                    onChangePassword = onChangePassword
                )
            }
        }
    }

    // Status Popups for Actions (Save Profile, etc.)
    when (val state = actionState) {
        is ProfileActionState.Error -> {
            StatusPopup(
                isSuccess = false,
                message = state.message,
                onDismiss = { viewModel.resetActionState() }
            )
        }
        is ProfileActionState.Success -> {
            StatusPopup(
                isSuccess = true,
                message = state.message,
                onDismiss = { viewModel.resetActionState() }
            )
        }
        else -> {}
    }
}

@Composable
fun ProfileTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(DarkSurface)
                .border(1.dp, BorderGlass, CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("Profile", style = Typography.titleLarge.copy(fontWeight = FontWeight.Black))
            Text("Manage your account", style = Typography.bodySmall.copy(color = TextSecondary))
        }
    }
}

@Composable
fun ProfileContent(
    name: String,
    email: String,
    phone: String,
    bio: String,
    oldPassword: String,
    newPassword: String,
    confirmPassword: String,
    showPassword: Boolean,
    actionState: ProfileActionState,
    onNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onOldPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleShowPassword: () -> Unit,
    onSaveProfile: () -> Unit,
    onChangePassword: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(title = "Edit Profile")

        ModernTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Name",
            icon = Icons.Filled.Person,
            placeholder = "Your full name"
        )

        ModernTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = "Phone",
            icon = Icons.Filled.Person,
            placeholder = "+62 812 3456 7890",
            keyboardType = KeyboardType.Phone
        )

        ModernTextField(
            value = bio,
            onValueChange = onBioChange,
            label = "Bio",
            icon = Icons.Filled.Person,
            placeholder = "Short bio about you"
        )

        ModernTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email",
            icon = Icons.Filled.Person,
            placeholder = "name@example.com",
            keyboardType = KeyboardType.Email
        )

        Button(
            onClick = onSaveProfile,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = DarkBg)
        ) {
            if (actionState is ProfileActionState.Loading) {
                CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("Save Profile", style = Typography.labelLarge.copy(fontWeight = FontWeight.Black))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onChangePassword,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple, contentColor = Color.White)
        ) {
            Text("Change Password", style = Typography.labelLarge.copy(fontWeight = FontWeight.Black))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, style = Typography.titleMedium.copy(fontWeight = FontWeight.Black))
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
            text = label,
            style = Typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextSecondary.copy(alpha = 0.6f)) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = TextSecondary) },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (showPassword) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = BorderGlass,
                cursorColor = AccentBlue,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        )
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, style = Typography.bodyLarge.copy(color = ExpenseRed, fontWeight = FontWeight.Black), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = DarkBg)
        ) {
            Text("Retry")
        }
    }
}

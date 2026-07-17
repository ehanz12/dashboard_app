package com.reihan.dashboard

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reihan.dashboard.data.network.SessionManager
import com.reihan.dashboard.ui.auth.AuthScreen
import com.reihan.dashboard.ui.auth.AuthViewModel
import com.reihan.dashboard.ui.auth.GoogleSignInHelper
import com.reihan.dashboard.ui.auth.VerificationScreen
import com.reihan.dashboard.ui.auth.ForgotPasswordScreen
import com.reihan.dashboard.ui.auth.ResetPasswordScreen
import com.reihan.dashboard.ui.analytics.AnalyticsScreen
import com.reihan.dashboard.ui.analytics.AnalyticsViewModel
import com.reihan.dashboard.ui.calendar.CalendarScreen
import com.reihan.dashboard.ui.calendar.CalendarViewModel
import com.reihan.dashboard.ui.habit.HabitScreen
import com.reihan.dashboard.ui.habit.HabitViewModel
import com.reihan.dashboard.ui.home.HomeScreen
import com.reihan.dashboard.ui.home.HomeViewModel
import com.reihan.dashboard.ui.profile.ProfileScreen
import com.reihan.dashboard.ui.profile.ProfileViewModel
import com.reihan.dashboard.ui.profile.ChangePasswordScreen
import com.reihan.dashboard.ui.task.TaskScreen
import com.reihan.dashboard.ui.task.TaskViewModel
import com.reihan.dashboard.ui.theme.*
import com.reihan.dashboard.ui.transaction.CategoryScreen
import com.reihan.dashboard.ui.transaction.TransactionScreen
import com.reihan.dashboard.ui.transaction.TransactionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DashboardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

// Bottom nav destinations
data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
    val accentColor: Color
)

val bottomNavItems = listOf(
    BottomNavItem("home", Icons.Default.Dashboard, "Home", AccentBlue),
    BottomNavItem("analytics", Icons.Default.Analytics, "Analytics", AccentBlue),
    BottomNavItem("calendar", Icons.Default.CalendarMonth, "Calendar", AccentBlue),
    BottomNavItem("tasks", Icons.Default.CheckCircleOutline, "Tasks", AccentTask)
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val startDestination = if (SessionManager.hasToken()) "home" else "auth"
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()

    // Notification Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) { // Android 13 (Tiramisu)
            permissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
        }
    }

    LaunchedEffect(navController) {
        SessionManager.sessionExpiredEvent.collect {
            navController.navigate("auth") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(SessionManager.hasToken()) {
        if (SessionManager.hasToken()) {
            authViewModel.syncFmcToken()
        }
    }

    // Determine if we should show bottom nav
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNav = currentRoute in bottomNavItems.map { it.route }

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val account = GoogleSignInHelper.handleSignInResult(result)
        if (account != null) {
            val idToken = GoogleSignInHelper.getIdToken(account)
            val userInfo = GoogleSignInHelper.getUserInfo(account)
            
            // Pass the idToken to the AuthViewModel through SavedStateHandle
            if (idToken != null) {
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("idToken", idToken)
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("userName", userInfo?.first)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            composable("auth") {
                // Use the authViewModel from AppNavigation scope
                
                // Observe Google Sign-In result from savedStateHandle
                val idTokenState = navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.getStateFlow<String?>("idToken", null)
                    ?.collectAsState()

                LaunchedEffect(idTokenState?.value) {
                    val idToken = idTokenState?.value
                    if (!idToken.isNullOrBlank()) {
                        authViewModel.googleLogin(idToken) {
                            authViewModel.syncFmcToken() // Sync token after Google login
                            navController.navigate("home") {
                                popUpTo("auth") { inclusive = true }
                            }
                            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("idToken")
                        }
                    }
                }
                
                AuthScreen(
                    viewModel = authViewModel,
                    onAuthSuccess = {
                        authViewModel.syncFmcToken() // Sync token after login
                        navController.navigate("home") {
                            popUpTo("auth") { inclusive = true }
                        }
                    },
                    onRegisterSuccess = { email ->
                        val encodedEmail = Uri.encode(email)
                        navController.navigate("verify_email/$encodedEmail")
                    },
                    onForgotPasswordClick = {
                        navController.navigate("forgot_password")
                    },
                    onGoogleSignInClick = {
                        val signInIntent = GoogleSignInHelper.getSignInIntent(context)
                        googleSignInLauncher.launch(signInIntent)
                    }
                )
            }
            composable("forgot_password") {
                val authViewModel: AuthViewModel = viewModel()
                ForgotPasswordScreen(
                    viewModel = authViewModel,
                    onCodeSent = { email ->
                        val encodedEmail = Uri.encode(email)
                        navController.navigate("reset_password/$encodedEmail")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("reset_password/{email}") { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                val authViewModel: AuthViewModel = viewModel()
                ResetPasswordScreen(
                    email = email,
                    viewModel = authViewModel,
                    onResetSuccess = {
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("verify_email/{email}") { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                VerificationScreen(
                    email = email,
                    viewModel = authViewModel,
                    onVerificationSuccess = {
                        if (SessionManager.hasToken()) {
                            authViewModel.syncFmcToken() // Sync token after verification
                            navController.navigate("home") {
                                popUpTo("auth") { inclusive = true }
                            }
                        } else {
                            navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onBack = {
                        navController.popBackStack("auth", inclusive = false)
                    }
                )
            }
            composable("home") {
                val homeViewModel: HomeViewModel = viewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onLogout = {
                        SessionManager.clearToken()
                        navController.navigate("auth") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onNavigateToProfile = { navController.navigate("profile") },
                    onNavigateToTransactions = { navController.navigate("transactions") },
                    onNavigateToCategories = { navController.navigate("categories") },
                    onNavigateToHabits = { navController.navigate("habits") },
                    onNavigateToTasks = { navController.navigate("tasks") },
                    onNavigateToCalendar = { navController.navigate("calendar") }
                )
            }
            composable("analytics") {
                val analyticsViewModel: AnalyticsViewModel = viewModel()
                AnalyticsScreen(
                    viewModel = analyticsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("calendar") {
                val calendarViewModel: CalendarViewModel = viewModel()
                CalendarScreen(
                    viewModel = calendarViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("transactions") {
                val transactionViewModel: TransactionViewModel = viewModel()
                TransactionScreen(
                    viewModel = transactionViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("categories") {
                val transactionViewModel: TransactionViewModel = viewModel()
                CategoryScreen(
                    viewModel = transactionViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("habits") {
                val habitViewModel: HabitViewModel = viewModel()
                HabitScreen(
                    viewModel = habitViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("tasks") {
                val taskViewModel: TaskViewModel = viewModel()
                TaskScreen(
                    viewModel = taskViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("profile") {
                val profileViewModel: ProfileViewModel = viewModel()
                ProfileScreen(
                    viewModel = profileViewModel,
                    onBack = { navController.popBackStack() },
                    onChangePassword = { navController.navigate("change_password") }
                )
            }
            composable("change_password") {
                ChangePasswordScreen(onBack = { navController.popBackStack() })
            }
        }

        // Bottom Navigation Bar
        AnimatedVisibility(
            visible = showBottomNav,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PremiumBottomNav(navController = navController, currentRoute = currentRoute)
        }
    }
}

@Composable
fun PremiumBottomNav(navController: NavController, currentRoute: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurface)
            .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentRoute == item.route
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) item.accentColor.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .clickable {
                            if (!isSelected) {
                                navController.navigate(item.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) item.accentColor else TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.label,
                                style = Typography.labelLarge.copy(
                                    color = item.accentColor,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

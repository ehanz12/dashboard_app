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
import androidx.compose.ui.text.font.FontWeight
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
import com.reihan.dashboard.ui.components.dotGridBackground
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
                    modifier = Modifier
                        .fillMaxSize()
                        .dotGridBackground(),
                    color = NeoBg
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

// Bottom nav destinations (Neobrutalism Style)
data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
    val accentColor: Color
)

val bottomNavItems = listOf(
    BottomNavItem("home", Icons.Default.Dashboard, "Home", NeoYellow),
    BottomNavItem("analytics", Icons.Default.Analytics, "Analytics", NeoCyan),
    BottomNavItem("habits", Icons.Default.Checklist, "Habits", NeoGreen),
    BottomNavItem("tasks", Icons.Default.AssignmentTurnedIn, "Tasks", NeoPink)
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
        if (android.os.Build.VERSION.SDK_INT >= 33) {
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
        val signResult = GoogleSignInHelper.handleSignInResult(result)
        signResult.onSuccess { account ->
            val idToken = GoogleSignInHelper.getIdToken(account)
            val userInfo = GoogleSignInHelper.getUserInfo(account)
            
            if (!idToken.isNullOrBlank()) {
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("idToken", idToken)
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("userName", userInfo?.first)
            } else {
                authViewModel.setError("ID Token Google kosong. Pastikan Web Client ID sudah sesuai di Google Cloud Console.")
            }
        }.onFailure { exception ->
            authViewModel.setError(exception.localizedMessage ?: "Google Sign-In gagal.")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .dotGridBackground()
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            composable("auth") {
                val idTokenState = navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.getStateFlow<String?>("idToken", null)
                    ?.collectAsState()

                LaunchedEffect(idTokenState?.value) {
                    val idToken = idTokenState?.value
                    if (!idToken.isNullOrBlank()) {
                        authViewModel.googleLogin(idToken) {
                            authViewModel.syncFmcToken()
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
                        authViewModel.syncFmcToken()
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
                            authViewModel.syncFmcToken()
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
                    onNavigateToTasks = { navController.navigate("tasks") }
                )
            }
            composable("analytics") {
                val analyticsViewModel: AnalyticsViewModel = viewModel()
                AnalyticsScreen(
                    viewModel = analyticsViewModel,
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

        // Bottom Navigation Bar (Neobrutalism Style with Hard Shadow and Thick Borders)
        AnimatedVisibility(
            visible = showBottomNav,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            NeobrutalBottomNav(navController = navController, currentRoute = currentRoute)
        }
    }
}

@Composable
fun NeobrutalBottomNav(navController: NavController, currentRoute: String?) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Shadow offset
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(Color.Black, shape)
        )
        // Main bottom nav container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NeoCardSurface, shape)
                .border(2.5.dp, Color.Black, shape)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentRoute == item.route
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(item.accentColor, RoundedCornerShape(12.dp))
                                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                            } else Modifier
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
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) Color.Black else NeoTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.label,
                                style = Typography.labelLarge.copy(
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

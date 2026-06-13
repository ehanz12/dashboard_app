package com.reihan.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.reihan.dashboard.ui.habit.HabitScreen
import com.reihan.dashboard.ui.habit.HabitViewModel
import com.reihan.dashboard.ui.home.HomeScreen
import com.reihan.dashboard.ui.home.HomeViewModel
import com.reihan.dashboard.ui.journal.JournalScreen
import com.reihan.dashboard.ui.journal.JournalViewModel
import com.reihan.dashboard.ui.task.TaskScreen
import com.reihan.dashboard.ui.task.TaskViewModel
import com.reihan.dashboard.ui.theme.*
import com.reihan.dashboard.ui.timeblock.TimeblockScreen
import com.reihan.dashboard.ui.timeblock.TimeblockViewModel
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
    BottomNavItem("tasks", Icons.Default.CheckCircleOutline, "Tasks", AccentTask),
    BottomNavItem("journal", Icons.Default.AutoStories, "Journal", AccentJournal),
    BottomNavItem("schedule", Icons.Default.CalendarMonth, "Schedule", AccentSchedule)
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val startDestination = if (SessionManager.hasToken()) "home" else "auth"

    LaunchedEffect(navController) {
        SessionManager.sessionExpiredEvent.collect {
            navController.navigate("auth") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Determine if we should show bottom nav
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNav = currentRoute in bottomNavItems.map { it.route }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            composable("auth") {
                val authViewModel: AuthViewModel = viewModel()
                AuthScreen(
                    viewModel = authViewModel,
                    onAuthSuccess = {
                        navController.navigate("home") {
                            popUpTo("auth") { inclusive = true }
                        }
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
                    onNavigateToTransactions = { navController.navigate("transactions") },
                    onNavigateToCategories = { navController.navigate("categories") },
                    onNavigateToHabits = { navController.navigate("habits") },
                    onNavigateToTasks = { navController.navigate("tasks") },
                    onNavigateToJournal = { navController.navigate("journal") },
                    onNavigateToSchedule = { navController.navigate("schedule") }
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
            composable("journal") {
                val journalViewModel: JournalViewModel = viewModel()
                JournalScreen(
                    viewModel = journalViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("schedule") {
                val timeblockViewModel: TimeblockViewModel = viewModel()
                TimeblockScreen(
                    viewModel = timeblockViewModel,
                    onBack = { navController.popBackStack() }
                )
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

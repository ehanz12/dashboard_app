package com.reihan.dashboard.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reihan.dashboard.data.model.DashboardData
import com.reihan.dashboard.ui.theme.*
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.ui.tooling.preview.Preview
import com.reihan.dashboard.data.model.*

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    DashboardTheme {
        Box(modifier = Modifier.background(DarkBg)) {
            DashboardContent(
                userName = "Reihan",
                data = DashboardData(
                    finance = FinanceSummary(135000.0, 1175128.0, 1040128.0, 135000.0),
                    habits = HabitSummary(5, 12, 7, 18, 84, 71),
                    tasks = TaskSummary(1, 5, 67, 2),
                    schedule = ScheduleSummary(3, "Meeting Tim", "13:00")
                ),
                onLogout = {},
                onProfileClick = {},
                onNavigateToTransactions = {},
                onNavigateToHabits = {},
                onNavigateToCategories = {},
                onNavigateToTasks = {},
                onNavigateToCalendar = {}
            )
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToHabits: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToCalendar: () -> Unit
) {
    val homeState by viewModel.homeState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        when (val state = homeState) {
            is HomeState.Loading -> {
                DashboardSkeleton()
            }
            is HomeState.Error -> {
                ErrorView(state.message) { viewModel.loadDashboardData() }
            }
            is HomeState.Success -> {
                DashboardContent(
                    userName = state.userName,
                    data = state.dashboardData,
                    onLogout = onLogout,
                    onProfileClick = onNavigateToProfile,
                    onNavigateToTransactions = onNavigateToTransactions,
                    onNavigateToHabits = onNavigateToHabits,
                    onNavigateToCategories = onNavigateToCategories,
                    onNavigateToTasks = onNavigateToTasks,
                    onNavigateToCalendar = onNavigateToCalendar
                )
            }
        }
    }
}

@Composable
fun DashboardContent(
    userName: String,
    data: DashboardData,
    onLogout: () -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToHabits: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToCalendar: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // 1. GREETING
        item {
            HomeHeader(userName = userName, onLogout = onLogout, onProfileClick = onProfileClick)
        }

        // 2. FINANCIAL CARD
        item {
            BalanceCard(
                balance = data.finance.balance,
                income = data.finance.income,
                expense = data.finance.expense
            )
        }

        // 3. QUICK MENU
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionItem(
                    title = "Transaction",
                    icon = Icons.Outlined.AddCard,
                    color = AccentBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTransactions
                )
                QuickActionItem(
                    title = "Category",
                    icon = Icons.Outlined.Category,
                    color = AccentPurple,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCategories
                )
            }
        }

        // 4. DAILY FOCUS
        item {
            SectionTitle(title = "Daily Focus", actionText = "Manage", onAction = onNavigateToHabits)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HabitProgressCard(
                    completed = data.habits.completedToday,
                    total = data.habits.totalToday,
                    streak = data.habits.currentStreak,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToHabits
                )
                TaskProgressCard(
                    completed = data.tasks.completedToday,
                    total = data.tasks.totalToday,
                    overdue = data.tasks.overdue,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTasks
                )
            }
        }

        // 5. STATISTICS
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionTitle(title = "Statistics", actionText = "", onAction = {})
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        title = "Current Streak",
                        value = "${data.habits.currentStreak} Days",
                        icon = Icons.Default.Whatshot,
                        color = Color(0xFFF97316), // Orange
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Best Streak",
                        value = "${data.habits.longestStreak} Days",
                        icon = Icons.Default.EmojiEvents,
                        color = Color(0xFFEAB308), // Yellow
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        title = "Habit Rate",
                        value = "${data.habits.completionRate}%",
                        icon = Icons.Default.CheckCircle,
                        color = IncomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Task Rate",
                        value = "${data.tasks.completionRate}%",
                        icon = Icons.Default.Assignment,
                        color = AccentTask,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 6. TODAY'S SCHEDULE
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionTitle(title = "Today's Schedule", actionText = "See All", onAction = onNavigateToCalendar)
            ScheduleCard(
                eventsCount = data.schedule.todayEvents,
                nextEvent = data.schedule.nextEvent,
                nextTime = data.schedule.nextTime,
                onClick = onNavigateToCalendar
            )
        }
    }
}

// --- COMPONENTS ---

@Composable
fun HomeHeader(userName: String, onLogout: () -> Unit, onProfileClick: () -> Unit) {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "$greeting,",
                style = Typography.bodyMedium.copy(color = TextSecondary)
            )
            Text(
                text = userName,
                style = Typography.headlineMedium.copy(fontWeight = FontWeight.Black)
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
                    .border(1.dp, BorderGlass, CircleShape)
            ) {
                Icon(Icons.Outlined.Person, contentDescription = "Profile", tint = AccentBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
                    .border(1.dp, BorderGlass, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = ExpenseRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun BalanceCard(balance: Double, income: Double, expense: Double) {
    val animatedBalance by animateIntAsState(targetValue = balance.toInt(), label = "balance")
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(AccentBlue, Color(0xFFC48B2B))
                )
            )
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Balance", style = Typography.labelLarge.copy(color = DarkBg.copy(alpha = 0.6f), fontWeight = FontWeight.Bold))
                    Text(formatCurrency(animatedBalance.toDouble()), style = Typography.displaySmall.copy(color = DarkBg, fontWeight = FontWeight.Black))
                }
                Icon(Icons.Default.Wallet, contentDescription = null, tint = DarkBg.copy(alpha = 0.2f), modifier = Modifier.size(40.dp))
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkBg.copy(alpha = 0.1f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BalanceStat(label = "Income", amount = income, color = DarkBg)
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(DarkBg.copy(alpha = 0.1f)))
                BalanceStat(label = "Expense", amount = expense, color = DarkBg)
            }
        }
    }
}

@Composable
fun BalanceStat(label: String, amount: Double, color: Color) {
    val animatedAmount by animateIntAsState(targetValue = amount.toInt(), label = "amount")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = Typography.labelSmall.copy(color = color.copy(alpha = 0.6f)))
        Text(formatCurrency(animatedAmount.toDouble()), style = Typography.bodyLarge.copy(color = color, fontWeight = FontWeight.Black))
    }
}

@Composable
fun HabitProgressCard(completed: Int, total: Int, streak: Int, modifier: Modifier, onClick: () -> Unit) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    val percentage = (progress * 100).toInt()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurface)
            .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text("Habits", style = Typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))
        
        if (streak > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Whatshot, null, tint = Color(0xFFF97316), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("$streak Days", style = Typography.labelLarge.copy(color = Color(0xFFF97316), fontWeight = FontWeight.Black))
            }
            Text("Current Streak", style = Typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
        } else {
            Text("💪 Start Today", style = Typography.labelLarge.copy(color = AccentBlue, fontWeight = FontWeight.Black))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Today's Progress", style = Typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
        Text("$completed / $total", style = Typography.titleMedium.copy(fontWeight = FontWeight.Black))
        
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = IncomeGreen,
            trackColor = IncomeGreen.copy(alpha = 0.1f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        Text("$percentage% Completed", style = Typography.labelSmall.copy(color = IncomeGreen, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun TaskProgressCard(completed: Int, total: Int, overdue: Int, modifier: Modifier, onClick: () -> Unit) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    val percentage = (progress * 100).toInt()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurface)
            .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Tasks", style = Typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
            if (overdue > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ExpenseRed.copy(alpha = 0.1f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("$overdue Overdue", style = Typography.labelSmall.copy(color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 8.sp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Today's Progress", style = Typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
        Text("$completed / $total", style = Typography.titleMedium.copy(fontWeight = FontWeight.Black))

        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.height(12.dp)) // Alignment with Habit card
        
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = AccentTask,
            trackColor = AccentTask.copy(alpha = 0.1f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        Text("$percentage% Completed", style = Typography.labelSmall.copy(color = AccentTask, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .padding(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, style = Typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
        Text(value, style = Typography.titleMedium.copy(fontWeight = FontWeight.Black))
    }
}

@Composable
fun ScheduleCard(eventsCount: Int, nextEvent: String?, nextTime: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurface)
            .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AccentSchedule.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CalendarMonth, null, tint = AccentSchedule, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                if (eventsCount == 0) {
                    Text("No Schedule Today", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                } else {
                    Text(
                        text = if (eventsCount == 1) "1 Event Today" else "$eventsCount Events Today",
                        style = Typography.labelSmall.copy(color = AccentSchedule, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (nextEvent != null) {
                        Text(
                            text = "Next: $nextTime",
                            style = Typography.labelSmall.copy(color = TextSecondary)
                        )
                        Text(
                            text = nextEvent,
                            style = Typography.bodyLarge.copy(fontWeight = FontWeight.Black)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionItem(title: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, style = Typography.labelLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun SectionTitle(title: String, actionText: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        if (actionText.isNotEmpty()) {
            Text(
                text = actionText,
                style = Typography.labelLarge.copy(color = AccentBlue, fontWeight = FontWeight.Bold),
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}

@Composable
fun DashboardSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Box(modifier = Modifier.padding(24.dp).fillMaxWidth().height(60.dp).clip(RoundedCornerShape(12.dp)).background(DarkSurface.copy(alpha = alpha)))
        // Finance Card
        Box(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(180.dp).clip(RoundedCornerShape(32.dp)).background(DarkSurface.copy(alpha = alpha)))
        // Quick Menu
        Row(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f).height(80.dp).clip(RoundedCornerShape(20.dp)).background(DarkSurface.copy(alpha = alpha)))
            Box(modifier = Modifier.weight(1f).height(80.dp).clip(RoundedCornerShape(20.dp)).background(DarkSurface.copy(alpha = alpha)))
        }
        // Daily Focus
        Row(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(24.dp)).background(DarkSurface.copy(alpha = alpha)))
            Box(modifier = Modifier.weight(1f).height(160.dp).clip(RoundedCornerShape(24.dp)).background(DarkSurface.copy(alpha = alpha)))
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Oops! Something went wrong", style = Typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(message, style = Typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Try Again", color = DarkBg, fontWeight = FontWeight.Bold)
        }
    }
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    return format.format(amount).replace("Rp", "Rp ").substringBefore(",")
}

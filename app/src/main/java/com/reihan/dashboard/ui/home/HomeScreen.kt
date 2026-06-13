package com.reihan.dashboard.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reihan.dashboard.data.model.HabitLogTodayResponse
import com.reihan.dashboard.data.model.JournalResponse
import com.reihan.dashboard.data.model.TaskResponse
import com.reihan.dashboard.data.model.TransactionResponse
import com.reihan.dashboard.ui.journal.moodEmoji
import com.reihan.dashboard.ui.journal.moodLabel
import com.reihan.dashboard.ui.theme.*
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onLogout: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToHabits: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToSchedule: () -> Unit
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue, strokeWidth = 2.dp)
                }
            }
            is HomeState.Error -> {
                ErrorView(state.message) { viewModel.loadDashboardData() }
            }
            is HomeState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // ── 1. HEADER ──
                    item {
                        HomeHeader(userName = state.userName, onLogout = onLogout)
                    }

                    // ── 2. BALANCE CARD ──
                    item {
                        BalanceCard(
                            balance = state.balance,
                            income = state.totalIncome,
                            expense = state.totalExpense
                        )
                    }

                    // ── 3. QUICK ACTIONS ──
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

                    // ── 4. SUMMARY WIDGETS ──
                    item {
                        SectionTitle(title = "Daily Focus", actionText = "Manage", onAction = onNavigateToHabits)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val habitProgress = if (state.todayHabits.isNotEmpty()) {
                                state.todayCompletedHabits.toFloat() / state.todayHabits.size
                            } else 0f
                            
                            SummaryWidget(
                                title = "Habits",
                                value = "${state.todayCompletedHabits}/${state.todayHabits.size}",
                                progress = habitProgress,
                                color = IncomeGreen,
                                icon = Icons.Outlined.Loop,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToHabits
                            )
                            
                            SummaryWidget(
                                title = "Tasks",
                                value = "${state.todayCompletedTasks}/${state.todayTasks.size + state.todayCompletedTasks}",
                                progress = if (state.todayTasks.size + state.todayCompletedTasks > 0) {
                                    state.todayCompletedTasks.toFloat() / (state.todayTasks.size + state.todayCompletedTasks)
                                } else 0f,
                                color = AccentTask,
                                icon = Icons.Outlined.TaskAlt,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToTasks
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SummaryWidget(
                                title = "Journal",
                                value = if (state.todayJournal != null) moodLabel(state.todayJournal.mood) else "Empty",
                                icon = Icons.Outlined.EditNote,
                                color = AccentJournal,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToJournal
                            )
                            SummaryWidget(
                                title = "Schedule",
                                value = "Weekly",
                                icon = Icons.Outlined.CalendarToday,
                                color = AccentSchedule,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToSchedule
                            )
                        }
                    }

                    // ── 5. RECENT ACTIVITY ──
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        SectionTitle(title = "Recent Transactions", actionText = "See All", onAction = onNavigateToTransactions)
                    }

                    if (state.recentTransactions.isEmpty()) {
                        item {
                            EmptyActivityView()
                        }
                    } else {
                        items(state.recentTransactions) { transaction ->
                            ModernTransactionRow(transaction)
                        }
                    }
                }
            }
        }
    }
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    return format.format(amount).replace("Rp", "Rp ").substringBefore(",")
}

@Composable
fun HomeHeader(userName: String, onLogout: () -> Unit) {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
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

@Composable
fun BalanceCard(balance: Double, income: Double, expense: Double) {
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
                    Text(formatCurrency(balance), style = Typography.displaySmall.copy(color = DarkBg, fontWeight = FontWeight.Black))
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = Typography.labelSmall.copy(color = color.copy(alpha = 0.6f)))
        Text(formatCurrency(amount), style = Typography.bodyLarge.copy(color = color, fontWeight = FontWeight.Black))
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
fun SummaryWidget(
    title: String,
    value: String,
    progress: Float? = null,
    color: Color,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurface)
            .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = Typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(value, style = Typography.titleLarge.copy(fontWeight = FontWeight.Black))
        
        if (progress != null) {
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )
        }
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
        Text(
            text = actionText,
            style = Typography.labelLarge.copy(color = AccentBlue, fontWeight = FontWeight.Bold),
            modifier = Modifier.clickable { onAction() }
        )
    }
}

@Composable
fun ModernTransactionRow(transaction: TransactionResponse) {
    val isIncome = transaction.type.lowercase() == "income"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isIncome) IncomeGreen.copy(alpha = 0.1f) else ExpenseRed.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                contentDescription = null,
                tint = if (isIncome) IncomeGreen else ExpenseRed,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.description.ifEmpty { "Transaction" },
                style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = transaction.category?.name ?: "No Category",
                style = Typography.bodySmall.copy(color = TextSecondary)
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isIncome) "+" else "-"}${formatCurrency(transaction.amount)}",
                style = Typography.bodyLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = if (isIncome) IncomeGreen else ExpenseRed
                )
            )
            Text(transaction.transactionDate, style = Typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp))
        }
    }
}

@Composable
fun EmptyActivityView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, BorderGlass, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("No recent transactions", style = Typography.bodyMedium, color = TextSecondary)
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

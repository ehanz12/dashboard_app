package com.reihan.dashboard.ui.home

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reihan.dashboard.data.model.DashboardData
import com.reihan.dashboard.data.repository.HomeRepository
import com.reihan.dashboard.ui.components.NeoBadge
import com.reihan.dashboard.ui.components.NeoCard
import com.reihan.dashboard.ui.components.StatusPopup
import com.reihan.dashboard.ui.components.VoiceTransactionDialog
import com.reihan.dashboard.ui.components.dotGridBackground
import com.reihan.dashboard.ui.habit.HabitViewModel
import com.reihan.dashboard.ui.task.TaskViewModel
import com.reihan.dashboard.ui.theme.*
import com.reihan.dashboard.ui.transaction.TransactionViewModel
import com.reihan.dashboard.util.ParsedVoiceTransaction
import com.reihan.dashboard.util.VoiceActionProcessor
import com.reihan.dashboard.util.VoiceActionResult
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToHabits: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToTasks: () -> Unit,
    habitViewModel: HabitViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    taskViewModel: TaskViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    transactionViewModel: TransactionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val homeState by viewModel.homeState.collectAsState()
    val habitState by habitViewModel.state.collectAsState()
    val taskState by taskViewModel.state.collectAsState()
    val txState by transactionViewModel.state.collectAsState()

    var statusPopup by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var parsedVoiceTx by remember { mutableStateOf<ParsedVoiceTransaction?>(null) }

    // Load main dashboard data immediately
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                val habitsToday = (habitState as? com.reihan.dashboard.ui.habit.HabitState.Success)?.habitsToday ?: emptyList()
                val allHabits = (habitState as? com.reihan.dashboard.ui.habit.HabitState.Success)?.allHabits ?: emptyList()
                val tState = taskState as? com.reihan.dashboard.ui.task.TaskState.Success
                val allTasks = if (tState != null) (tState.q1 + tState.q2 + tState.q3 + tState.q4) else emptyList()
                val categories = (txState as? com.reihan.dashboard.ui.transaction.TransactionState.Success)?.categories ?: emptyList()

                val actionResult = VoiceActionProcessor.processSpeech(
                    spokenText = spokenText,
                    habitsToday = habitsToday,
                    allHabits = allHabits,
                    allTasks = allTasks,
                    categories = categories
                )

                when (actionResult) {
                    is VoiceActionResult.HabitCompleted -> {
                        habitViewModel.toggleHabitToday(actionResult.habitId)
                        HomeRepository.invalidateCache()
                        viewModel.loadDashboardData(forceRefresh = true)
                        statusPopup = true to "Habit \"${actionResult.habitName}\" berhasil diselesaikan! 💪"
                    }
                    is VoiceActionResult.TaskCompleted -> {
                        taskViewModel.toggleTask(actionResult.taskId)
                        HomeRepository.invalidateCache()
                        viewModel.loadDashboardData(forceRefresh = true)
                        statusPopup = true to "Task \"${actionResult.taskTitle}\" berhasil diselesaikan! ✅"
                    }
                    is VoiceActionResult.TransactionDetected -> {
                        parsedVoiceTx = actionResult.parsedTransaction
                    }
                    is VoiceActionResult.NoMatch -> {
                        statusPopup = false to actionResult.reason
                    }
                }
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Lazy load dependencies for voice processing only when user initiates voice action
            if (habitState !is com.reihan.dashboard.ui.habit.HabitState.Success) {
                habitViewModel.loadHabitData(isRefresh = true)
            }
            if (taskState !is com.reihan.dashboard.ui.task.TaskState.Success) {
                taskViewModel.loadTasks()
            }
            if (txState !is com.reihan.dashboard.ui.transaction.TransactionState.Success) {
                transactionViewModel.loadTransactionsAndCategories()
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara ucapan Anda (contoh: 'aku selesai solat ashar' atau 'jajan 10000')")
            }
            speechLauncher.launch(intent)
        } else {
            statusPopup = false to "Izin mikrofon dibutuhkan untuk fitur asisten suara."
        }
    }

    fun startVoiceAssistant() {
        audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .dotGridBackground()
            .background(NeoBg)
    ) {
        when (val state = homeState) {
            is HomeState.Loading -> {
                DashboardSkeleton()
            }
            is HomeState.Error -> {
                ErrorView(state.message) { viewModel.loadDashboardData(forceRefresh = true) }
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
                    onVoiceAssistant = { startVoiceAssistant() }
                )
            }
        }

        statusPopup?.let { (isSuccess, message) ->
            StatusPopup(isSuccess = isSuccess, message = message, onDismiss = { statusPopup = null })
        }

        if (parsedVoiceTx != null && txState is com.reihan.dashboard.ui.transaction.TransactionState.Success) {
            VoiceTransactionDialog(
                parsed = parsedVoiceTx!!,
                categories = (txState as com.reihan.dashboard.ui.transaction.TransactionState.Success).categories,
                onDismiss = { parsedVoiceTx = null },
                onConfirm = { catId, amt, type, desc, date ->
                    transactionViewModel.addTransaction(
                        categoryId = catId,
                        amount = amt,
                        type = type,
                        description = desc,
                        transactionDate = date,
                        onSuccess = { 
                            parsedVoiceTx = null
                            HomeRepository.invalidateCache()
                            viewModel.loadDashboardData(forceRefresh = true)
                            statusPopup = true to "Transaksi suara berhasil disimpan!"
                        },
                        onFailure = { err ->
                            statusPopup = false to err
                        }
                    )
                }
            )
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
    onVoiceAssistant: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isTablet = screenWidth >= 600.dp
    val contentHorizontalPadding = if (isTablet) 32.dp else 20.dp

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidth = maxWidth
        val isWide = maxWidth > 700.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // 1. PROFESSIONAL RESPONSIVE HEADER
            item {
                HomeHeader(
                    userName = userName,
                    horizontalPadding = contentHorizontalPadding,
                    onLogout = onLogout,
                    onProfileClick = onProfileClick
                )
            }

            // 2. FINANCIAL BALANCE CARD (Neobrutalism Hero)
            item {
                BalanceCard(
                    balance = data.finance.balance,
                    income = data.finance.income,
                    expense = data.finance.expense,
                    horizontalPadding = contentHorizontalPadding,
                    onViewTransactions = onNavigateToTransactions
                )
            }

            // 3. QUICK ACTIONS GRID
            item {
                Spacer(modifier = Modifier.height(18.dp))
                QuickActionsGrid(
                    horizontalPadding = contentHorizontalPadding,
                    onVoiceAssistant = onVoiceAssistant,
                    onNavigateToTransactions = onNavigateToTransactions,
                    onNavigateToCategories = onNavigateToCategories,
                    onNavigateToHabits = onNavigateToHabits
                )
            }

            // 4. DAILY FOCUS (HABITS & TASKS) - RESPONSIVE ROW OR STACK
            item {
                Spacer(modifier = Modifier.height(22.dp))
                SectionTitle(
                    title = "Daily Focus",
                    actionText = "Manage Habits",
                    horizontalPadding = contentHorizontalPadding,
                    onAction = onNavigateToHabits
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = contentHorizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = contentHorizontalPadding),
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
            }

            // 5. STATISTICS & OVERVIEW
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle(
                    title = "Overview & Stats",
                    actionText = "",
                    horizontalPadding = contentHorizontalPadding,
                    onAction = {}
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = contentHorizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            title = "Current Streak",
                            value = "${data.habits.currentStreak} Days",
                            subtitle = "Consecutive active",
                            icon = Icons.Default.Whatshot,
                            color = NeoOrange,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Best Record",
                            value = "${data.habits.longestStreak} Days",
                            subtitle = "All-time personal best",
                            icon = Icons.Default.EmojiEvents,
                            color = NeoYellow,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            title = "Habit Success",
                            value = "${data.habits.completionRate}%",
                            subtitle = "Daily targets met",
                            icon = Icons.Default.CheckCircle,
                            color = NeoGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Task Efficiency",
                            value = "${data.tasks.completionRate}%",
                            subtitle = "Matrix completion",
                            icon = Icons.Default.AssignmentTurnedIn,
                            color = NeoCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// --- NEOBRUTALISM PROFESSIONAL COMPONENTS ---

@Composable
fun HomeHeader(
    userName: String,
    horizontalPadding: Dp,
    onLogout: () -> Unit,
    onProfileClick: () -> Unit
) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
    val currentDateStr = remember {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = horizontalPadding, end = horizontalPadding, top = 28.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeoBadge(text = greeting.uppercase(), containerColor = NeoCyan, contentColor = Color.Black)
                Text(
                    text = currentDateStr,
                    style = Typography.labelSmall.copy(
                        color = NeoTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = userName,
                style = Typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    color = Color.Black
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right side: Avatar profile + Logout button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Profile Neo Button with Initial Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onProfileClick() }
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 3.dp, y = 3.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NeoYellow, RoundedCornerShape(12.dp))
                        .border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = userName.trim().firstOrNull()?.uppercase() ?: "U"
                    Text(
                        text = initial,
                        style = Typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            fontSize = 18.sp
                        )
                    )
                }
            }

            // Logout Neo Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onLogout() }
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = 3.dp, y = 3.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = NeoRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceCard(
    balance: Double,
    income: Double,
    expense: Double,
    horizontalPadding: Dp,
    onViewTransactions: () -> Unit
) {
    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        backgroundColor = NeoYellow,
        borderColor = Color.Black,
        shadowColor = Color.Black,
        shadowOffset = 5.dp,
        cornerRadius = 16.dp,
        contentPadding = 20.dp
    ) {
        // Card Header: Badge + View Details Clickable
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeoBadge(
                text = "TOTAL BALANCE",
                containerColor = Color.Black,
                contentColor = Color.White
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.08f))
                    .clickable { onViewTransactions() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History",
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Black, color = Color.Black)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Big Balance Display
        Text(
            text = formatCurrency(balance),
            style = Typography.displayLarge.copy(
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 32.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Income & Expense Sub-bar (Dark Neobrutal Container)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black, RoundedCornerShape(12.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Income
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(NeoGreen, CircleShape)
                        .border(1.5.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "INCOME",
                        style = Typography.labelSmall.copy(
                            color = NeoGreen,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = formatCurrency(income),
                        style = Typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .height(28.dp)
                    .background(Color.White.copy(alpha = 0.25f))
            )

            // Expense
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.weight(1f)
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "EXPENSE",
                        style = Typography.labelSmall.copy(
                            color = NeoRed,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = formatCurrency(expense),
                        style = Typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(NeoRed, CircleShape)
                        .border(1.5.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsGrid(
    horizontalPadding: Dp,
    onVoiceAssistant: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToHabits: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionItem(
                title = "Voice Mic",
                subtitle = "Fast input",
                icon = Icons.Default.Mic,
                color = NeoCyan,
                modifier = Modifier.weight(1f),
                onClick = onVoiceAssistant
            )
            QuickActionItem(
                title = "Transactions",
                subtitle = "Manage log",
                icon = Icons.Outlined.AddCard,
                color = NeoYellow,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToTransactions
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionItem(
                title = "Categories",
                subtitle = "Custom tags",
                icon = Icons.Outlined.Category,
                color = NeoGreen,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToCategories
            )
            QuickActionItem(
                title = "Habit Log",
                subtitle = "Daily targets",
                icon = Icons.Default.Checklist,
                color = NeoPink,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToHabits
            )
        }
    }
}

@Composable
fun QuickActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    NeoCard(
        modifier = modifier,
        backgroundColor = color,
        borderColor = Color.Black,
        shadowColor = Color.Black,
        shadowOffset = 3.dp,
        cornerRadius = 14.dp,
        contentPadding = 12.dp,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = Typography.titleSmall.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = Typography.labelSmall.copy(
                        color = Color.Black.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun HabitProgressCard(
    completed: Int,
    total: Int,
    streak: Int,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val progress = if (total > 0) (completed.toFloat() / total).coerceIn(0f, 1f) else 0f
    val percentage = (progress * 100).toInt()

    NeoCard(
        modifier = modifier,
        backgroundColor = NeoCardSurface,
        borderColor = Color.Black,
        shadowColor = Color.Black,
        shadowOffset = 4.dp,
        cornerRadius = 14.dp,
        contentPadding = 14.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeoBadge(text = "HABITS", containerColor = NeoYellow, contentColor = Color.Black)
            if (streak > 0) {
                NeoBadge(text = "🔥 $streak D", containerColor = NeoOrange, contentColor = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Today's Target", style = Typography.labelSmall.copy(color = NeoTextSecondary, fontSize = 11.sp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("$completed", style = Typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Color.Black, fontSize = 22.sp))
            Text("/ $total", style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NeoTextSecondary, fontSize = 14.sp))
        }

        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .border(1.5.dp, Color.Black, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp)),
            color = NeoGreen,
            trackColor = Color(0xFFEEEEEE)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$percentage% Done",
            style = Typography.labelLarge.copy(color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
        )
    }
}

@Composable
fun TaskProgressCard(
    completed: Int,
    total: Int,
    overdue: Int,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val progress = if (total > 0) (completed.toFloat() / total).coerceIn(0f, 1f) else 0f
    val percentage = (progress * 100).toInt()

    NeoCard(
        modifier = modifier,
        backgroundColor = NeoCardSurface,
        borderColor = Color.Black,
        shadowColor = Color.Black,
        shadowOffset = 4.dp,
        cornerRadius = 14.dp,
        contentPadding = 14.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeoBadge(text = "TASKS", containerColor = NeoCyan, contentColor = Color.Black)
            if (overdue > 0) {
                NeoBadge(text = "$overdue DUE", containerColor = NeoRed, contentColor = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Eisenhower Focus", style = Typography.labelSmall.copy(color = NeoTextSecondary, fontSize = 11.sp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("$completed", style = Typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Color.Black, fontSize = 22.sp))
            Text("/ $total", style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NeoTextSecondary, fontSize = 14.sp))
        }

        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .border(1.5.dp, Color.Black, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp)),
            color = NeoCyan,
            trackColor = Color(0xFFEEEEEE)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$percentage% Done",
            style = Typography.labelLarge.copy(color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier
) {
    NeoCard(
        modifier = modifier,
        backgroundColor = NeoCardSurface,
        borderColor = Color.Black,
        shadowColor = Color.Black,
        shadowOffset = 3.dp,
        cornerRadius = 14.dp,
        contentPadding = 12.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color, RoundedCornerShape(10.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = Typography.labelSmall.copy(color = NeoTextSecondary, fontSize = 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Black, color = Color.Black, fontSize = 15.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = Typography.labelSmall.copy(color = Color.Gray, fontSize = 9.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    actionText: String,
    horizontalPadding: Dp,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = Typography.titleMedium.copy(fontWeight = FontWeight.Black, color = Color.Black, fontSize = 16.sp)
        )
        if (actionText.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .clickable { onAction() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeoBadge(text = actionText, containerColor = NeoYellow, contentColor = Color.Black)
            }
        }
    }
}

@Composable
fun DashboardSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        // Header skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(width = 80.dp, height = 20.dp).clip(RoundedCornerShape(6.dp)).background(NeoCardSurface.copy(alpha = alpha)))
                Box(modifier = Modifier.size(width = 160.dp, height = 28.dp).clip(RoundedCornerShape(6.dp)).background(NeoCardSurface.copy(alpha = alpha)))
            }
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(NeoCardSurface.copy(alpha = alpha)))
        }

        // Hero Balance card skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(NeoCardSurface.copy(alpha = alpha))
        )

        // Quick actions row skeleton
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(14.dp)).background(NeoCardSurface.copy(alpha = alpha)))
            Box(modifier = Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(14.dp)).background(NeoCardSurface.copy(alpha = alpha)))
        }

        // Daily focus skeleton
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f).height(130.dp).clip(RoundedCornerShape(14.dp)).background(NeoCardSurface.copy(alpha = alpha)))
            Box(modifier = Modifier.weight(1f).height(130.dp).clip(RoundedCornerShape(14.dp)).background(NeoCardSurface.copy(alpha = alpha)))
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = NeoRed, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(20.dp))
        Text("Koneksi Terputus", style = Typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.Black)
        Text(message, style = Typography.bodyMedium, color = NeoTextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = NeoYellow),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp).border(2.dp, Color.Black, RoundedCornerShape(12.dp))
        ) {
            Text("Coba Lagi", color = Color.Black, fontWeight = FontWeight.Black)
        }
    }
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    return format.format(amount).replace("Rp", "Rp ").substringBefore(",")
}

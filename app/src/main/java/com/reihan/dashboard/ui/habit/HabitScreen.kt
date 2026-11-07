package com.reihan.dashboard.ui.habit

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reihan.dashboard.data.model.HabitLogTodayResponse
import com.reihan.dashboard.data.model.HabitMini
import com.reihan.dashboard.data.model.HabitSummary
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.reihan.dashboard.ui.components.DeleteConfirmationDialog
import com.reihan.dashboard.ui.components.NeoBadge
import com.reihan.dashboard.ui.components.NeoButton
import com.reihan.dashboard.ui.components.NeoCard
import com.reihan.dashboard.ui.components.StatusPopup
import com.reihan.dashboard.ui.components.dotGridBackground
import com.reihan.dashboard.ui.theme.*
import com.reihan.dashboard.util.VoiceActionProcessor
import com.reihan.dashboard.util.VoiceActionResult
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    viewModel: HabitViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitMini?>(null) }
    var statusPopup by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }
    
    val listState = rememberLazyListState()

    // Speech Recognizer Activity Launcher for Habits
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()
            if (!spokenText.isNullOrBlank() && state is HabitState.Success) {
                val succ = state as HabitState.Success
                val actionResult = VoiceActionProcessor.processSpeech(
                    spokenText = spokenText,
                    habitsToday = succ.habitsToday,
                    allHabits = succ.allHabits,
                    allTasks = emptyList(),
                    categories = emptyList()
                )
                when (actionResult) {
                    is VoiceActionResult.HabitCompleted -> {
                        viewModel.toggleHabitToday(actionResult.habitId) { err ->
                            statusPopup = false to err
                        }
                        statusPopup = true to "Berhasil menyelesaikan Habit \"${actionResult.habitName}\"! 💪"
                    }
                    is VoiceActionResult.NoMatch -> {
                        statusPopup = false to actionResult.reason
                    }
                    else -> {
                        statusPopup = false to "Tidak menemukan habit yang sesuai dengan \"$spokenText\"."
                    }
                }
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara habit Anda (contoh: 'aku selesai solat ashar')")
            }
            speechLauncher.launch(intent)
        } else {
            statusPopup = false to "Izin mikrofon dibutuhkan untuk menyelesaikan habit lewat suara."
        }
    }

    fun startVoiceInput() {
        audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(Unit) {
        viewModel.loadHabitData(isRefresh = true)
    }

    // Pagination detect
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 2
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && state is HabitState.Success) {
            val successState = state as HabitState.Success
            if (successState.hasMore && !successState.isNextPageLoading) {
                viewModel.loadHabitData(isRefresh = false)
            }
        }
    }

    Scaffold(
        containerColor = NeoBg,
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                FloatingActionButton(
                    onClick = { startVoiceInput() },
                    containerColor = NeoCyan,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(2.5.dp, Color.Black, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice Habit", tint = Color.Black)
                }
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = NeoYellow,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(2.5.dp, Color.Black, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Habit", tint = Color.Black)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .dotGridBackground()
        ) {
            when (val s = state) {
                is HabitState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Black, strokeWidth = 3.dp)
                    }
                }
                is HabitState.Error -> {
                    ErrorStateView(message = s.message)
                }
                is HabitState.Success -> {
                    HabitRedesignedContent(
                        listState = listState,
                        current = s,
                        viewModel = viewModel,
                        onBack = onBack,
                        onEdit = { editingHabit = it },
                        onDeleteHabit = { deleteConfirmId = it },
                        onStatusUpdate = { isSuccess, message ->
                            statusPopup = isSuccess to message
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        HabitFormDialog(
            title = "Create New Habit",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, freq, days, time, enabled ->
                viewModel.addHabit(name, freq, days, time, enabled,
                    onSuccess = { 
                        showAddDialog = false
                        statusPopup = true to "Habit created successfully!"
                    },
                    onFailure = { err -> statusPopup = false to err }
                )
            }
        )
    }

    if (editingHabit != null) {
        HabitFormDialog(
            title = "Edit Habit",
            initialName = editingHabit!!.name,
            initialFrequency = editingHabit!!.frequency,
            initialDays = editingHabit!!.days ?: emptyList(),
            initialReminderTime = editingHabit!!.reminderTime ?: "08:00",
            initialReminderEnabled = editingHabit!!.reminderEnabled,
            onDismiss = { editingHabit = null },
            onConfirm = { name, freq, days, time, enabled ->
                viewModel.editHabit(editingHabit!!.id, name, freq, days, time, enabled,
                    onSuccess = { 
                        editingHabit = null
                        statusPopup = true to "Habit updated successfully!"
                    },
                    onFailure = { err -> statusPopup = false to err }
                )
            }
        )
    }

    statusPopup?.let { (isSuccess, message) ->
        StatusPopup(isSuccess = isSuccess, message = message, onDismiss = { statusPopup = null })
    }

    deleteConfirmId?.let { id ->
        DeleteConfirmationDialog(
            title = "Delete Habit?",
            message = "Are you sure you want to remove this habit?",
            onConfirm = {
                viewModel.removeHabit(id) { err -> statusPopup = false to err }
                deleteConfirmId = null
            },
            onDismiss = { deleteConfirmId = null }
        )
    }
}

@Composable
fun HabitRedesignedContent(
    listState: androidx.compose.foundation.lazy.LazyListState,
    current: HabitState.Success,
    viewModel: HabitViewModel,
    onBack: () -> Unit,
    onEdit: (HabitMini) -> Unit,
    onDeleteHabit: (String) -> Unit,
    onStatusUpdate: (Boolean, String) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // 1. Header
        item {
            HabitPremiumHeader(onBack = onBack)
        }

        // 2. Week Calendar
        item {
            PremiumWeekCalendar()
        }

        // 3. Stats Summary
        item {
            HabitStatsRow(summary = current.summary)
        }

        // 4. Progress Card
        item {
            TodayCompactProgressCard(
                completed = current.habitsToday.count { it.completed },
                total = current.habitsToday.size
            )
        }

        // 5. Section: Today's Journey
        item {
            SectionLabel("TODAY'S JOURNEY")
        }

        if (current.habitsToday.isEmpty()) {
            item { EmptyStateView(Icons.Default.Bedtime, "No habits scheduled for today.") }
        } else {
            items(current.habitsToday) { logItem ->
                HabitPremiumLogItem(
                    item = logItem,
                    allHabits = current.allHabits,
                    onToggle = {
                        viewModel.toggleHabitToday(
                            habitId = logItem.habitId,
                            onFailure = { err -> onStatusUpdate(false, err) }
                        )
                    }
                )
            }
        }

        // 6. Section: Management
        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionLabel("ALL HABITS")
        }

        if (current.allHabits.isEmpty()) {
            item { 
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No habits found. Tap + to add one.", color = NeoTextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            items(current.allHabits) { habit ->
                HabitManagementCard(
                    habit = habit,
                    onClick = { onEdit(habit) },
                    onDelete = { onDeleteHabit(habit.id) }
                )
            }
        }

        if (current.isNextPageLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun HabitPremiumHeader(onBack: () -> Unit) {
    val monthYear = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clickable { onBack() }
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 3.dp, y = 3.dp)
                    .background(Color.Black, RoundedCornerShape(10.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black, modifier = Modifier.size(20.dp))
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text("Habit Tracker", style = Typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = Color.Black))
            Text(monthYear, style = Typography.bodySmall.copy(color = NeoTextSecondary, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun PremiumWeekCalendar() {
    val days = remember {
        val calendar = Calendar.getInstance()
        val list = mutableListOf<Pair<String, Int>>()
        val dayNames = arrayOf("S", "M", "T", "W", "T", "F", "S")
        for (i in -3..3) {
            val cal = calendar.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, i)
            val dayName = dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val dayNum = cal.get(Calendar.DAY_OF_MONTH)
            list.add(dayName to dayNum)
        }
        list
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        itemsIndexed(days) { index, pair ->
            val isToday = index == 3
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isToday) NeoYellow else Color.White)
                    .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(pair.first, style = Typography.labelSmall.copy(color = Color.Black, fontWeight = FontWeight.Black))
                    Text("${pair.second}", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Black, color = Color.Black))
                }
            }
        }
    }
}

@Composable
fun HabitStatsRow(summary: HabitSummary?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatPill(
            icon = Icons.Default.Whatshot,
            label = "STREAK",
            value = "${summary?.currentStreak ?: 0} Days",
            color = NeoOrange,
            modifier = Modifier.weight(1f)
        )
        StatPill(
            icon = Icons.Default.CheckCircle,
            label = "RATE",
            value = "${summary?.completionRate ?: 0}%",
            color = NeoGreen,
            modifier = Modifier.weight(1f)
        )
        StatPill(
            icon = Icons.Default.EmojiEvents,
            label = "LONGEST",
            value = "${summary?.longestStreak ?: 0} Days",
            color = NeoYellow,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatPill(icon: ImageVector, label: String, value: String, color: Color, modifier: Modifier) {
    NeoCard(
        modifier = modifier,
        backgroundColor = Color.White,
        borderColor = Color.Black,
        shadowColor = Color.Black,
        shadowOffset = 3.dp,
        cornerRadius = 10.dp,
        contentPadding = 12.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color, RoundedCornerShape(6.dp))
                    .border(1.dp, Color.Black, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.Black, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, style = Typography.labelSmall.copy(color = NeoTextSecondary, fontSize = 9.sp))
            Text(value, style = Typography.bodyMedium.copy(fontWeight = FontWeight.Black, color = Color.Black))
        }
    }
}

@Composable
fun TodayCompactProgressCard(completed: Int, total: Int) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    val animatedProgress by animateFloatAsState(progress, label = "progress")

    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        backgroundColor = Color.White,
        borderColor = Color.Black,
        shadowColor = Color.Black,
        shadowOffset = 4.dp,
        cornerRadius = 12.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(56.dp),
                    color = Color(0xFFEEEEEE),
                    strokeWidth = 6.dp
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(56.dp),
                    color = NeoGreen,
                    strokeWidth = 6.dp,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    style = Typography.labelMedium.copy(fontWeight = FontWeight.Black, color = Color.Black)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text("Today's Progress", style = Typography.titleMedium.copy(fontWeight = FontWeight.Black, color = Color.Black))
                Text(
                    "$completed of $total Habits Completed",
                    style = Typography.bodySmall.copy(color = NeoTextSecondary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .border(1.dp, Color.Black, RoundedCornerShape(3.dp))
                        .clip(RoundedCornerShape(3.dp)),
                    color = NeoGreen,
                    trackColor = Color(0xFFEEEEEE)
                )
            }
        }
    }
}

@Composable
fun HabitPremiumLogItem(
    item: HabitLogTodayResponse,
    allHabits: List<HabitMini>,
    onToggle: () -> Unit
) {
    val habitDetails = allHabits.find { it.id == item.habitId }
    val isDaily = habitDetails?.frequency?.equals("daily", ignoreCase = true) ?: true

    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        backgroundColor = if (item.completed) Color(0xFFE8F5E9) else Color.White,
        borderColor = Color.Black,
        shadowColor = Color.Black,
        shadowOffset = 3.dp,
        cornerRadius = 10.dp,
        onClick = onToggle,
        contentPadding = 12.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (item.completed) NeoGreen else Color.White)
                    .border(2.dp, Color.Black, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (item.completed) {
                    Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = Typography.bodyLarge.copy(
                        fontWeight = FontWeight.Black,
                        textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
                        color = Color.Black
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isDaily) "Daily" else "Weekly",
                        style = Typography.labelSmall.copy(color = NeoTextSecondary, fontSize = 10.sp)
                    )
                    if (habitDetails?.reminderEnabled == true && habitDetails.reminderTime != null) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.Notifications, null, tint = Color.Black, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(habitDetails.reminderTime, style = Typography.labelSmall.copy(color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }
                }
            }
            
            if (item.completed) {
                NeoBadge(text = "DONE", containerColor = NeoGreen, contentColor = Color.Black)
            } else {
                NeoBadge(text = "TODO", containerColor = NeoYellow, contentColor = Color.Black)
            }
        }
    }
}

@Composable
fun HabitManagementCard(habit: HabitMini, onClick: () -> Unit, onDelete: () -> Unit) {
    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        backgroundColor = Color.White,
        borderColor = Color.Black,
        shadowColor = Color.Black,
        shadowOffset = 3.dp,
        cornerRadius = 10.dp,
        onClick = onClick,
        contentPadding = 12.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(habit.name, style = Typography.bodyMedium.copy(fontWeight = FontWeight.Black, color = Color.Black))
                Text(
                    habit.frequency.replaceFirstChar { it.uppercase() },
                    style = Typography.labelSmall.copy(color = NeoTextSecondary)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, null, tint = NeoRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = Typography.labelSmall.copy(
            color = Color.Black,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp
        ),
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun EmptyStateView(icon: ImageVector, message: String) {
    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        backgroundColor = Color.White,
        borderColor = Color.Black,
        shadowColor = Color.Black,
        shadowOffset = 3.dp,
        cornerRadius = 10.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.Black, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(message, style = Typography.bodySmall.copy(color = NeoTextSecondary))
        }
    }
}

@Composable
fun ErrorStateView(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = NeoRed, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = NeoTextSecondary, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitFormDialog(
    title: String,
    initialName: String = "",
    initialFrequency: String = "Daily",
    initialDays: List<String> = emptyList(),
    initialReminderTime: String = "08:00",
    initialReminderEnabled: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<String>?, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var frequency by remember { mutableStateOf(initialFrequency.replaceFirstChar { it.uppercase() }) }
    var selectedDays by remember { mutableStateOf(initialDays.toSet()) }
    var reminderTime by remember { mutableStateOf(initialReminderTime) }
    var reminderEnabled by remember { mutableStateOf(initialReminderEnabled) }
    
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(2.5.dp, Color.Black, RoundedCornerShape(16.dp)),
        title = {
            Text(title, style = Typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = Color.Black))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("HABIT NAME", style = Typography.labelSmall.copy(color = Color.Black, fontWeight = FontWeight.Black))
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g. Read 10 pages", color = NeoTextSecondary.copy(alpha = 0.5f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, Color.Black, RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("Daily", "Weekly").forEach { freq ->
                        val selected = frequency.equals(freq, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (selected) NeoYellow else Color.White, RoundedCornerShape(10.dp))
                                .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
                                .clickable { frequency = freq }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(freq, style = Typography.labelLarge.copy(color = Color.Black, fontWeight = FontWeight.Black))
                        }
                    }
                }
            }
        },
        confirmButton = {
            NeoButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, frequency.lowercase(), if (frequency.equals("weekly", ignoreCase = true)) selectedDays.toList() else null, reminderTime, reminderEnabled) },
                containerColor = NeoYellow,
                contentColor = Color.Black,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Habit", fontWeight = FontWeight.Black, color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = NeoTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

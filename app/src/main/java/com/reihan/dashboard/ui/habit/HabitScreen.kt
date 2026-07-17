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
import com.reihan.dashboard.ui.components.DeleteConfirmationDialog
import com.reihan.dashboard.ui.components.StatusPopup
import com.reihan.dashboard.ui.theme.*
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
        containerColor = DarkBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentBlue,
                contentColor = DarkBg,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val s = state) {
                is HabitState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentBlue, strokeWidth = 2.dp)
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

    // Dialogs...
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
                    Text("No habits found. Tap + to add one.", color = TextSecondary, fontSize = 14.sp)
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
                    CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(24.dp))
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
            .padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(DarkSurface)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(20.dp))
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text("Habit Tracker", style = Typography.headlineSmall.copy(fontWeight = FontWeight.Black))
            Text(monthYear, style = Typography.bodySmall.copy(color = TextSecondary, letterSpacing = 0.5.sp))
        }
    }
}

@Composable
fun PremiumWeekCalendar() {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_MONTH)
    
    // Find Monday of current week
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val weekDates = mutableListOf<Pair<String, Int>>()
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    
    for (i in 0..6) {
        weekDates.add(dayNames[i] to calendar.get(Calendar.DAY_OF_MONTH))
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items(weekDates) { (name, date) ->
            val isSelected = date == today
            val animatedColor by animateColorAsState(if (isSelected) AccentBlue else Color.Transparent)
            val animatedTextColor by animateColorAsState(if (isSelected) DarkBg else TextSecondary)
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(animatedColor)
                    .clickable { /* Handle date selection if needed */ }
                    .padding(vertical = 12.dp)
            ) {
                Text(name, style = Typography.labelSmall.copy(color = if (isSelected) DarkBg.copy(alpha = 0.6f) else TextSecondary, fontSize = 10.sp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(date.toString(), style = Typography.titleMedium.copy(color = animatedTextColor, fontWeight = FontWeight.Black))
                
                if (isSelected) {
                    Box(Modifier.size(4.dp).clip(CircleShape).background(DarkBg))
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
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatPill(
            icon = Icons.Default.Whatshot,
            label = "Streak",
            value = "${summary?.currentStreak ?: 0} Days",
            color = Color(0xFFF97316),
            modifier = Modifier.weight(1f)
        )
        StatPill(
            icon = Icons.Default.EmojiEvents,
            label = "Best",
            value = "${summary?.longestStreak ?: 0} Days",
            color = Color(0xFFEAB308),
            modifier = Modifier.weight(1f)
        )
        StatPill(
            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
            label = "Total",
            value = "${summary?.completedDays ?: 0}",
            color = AccentTask,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatPill(icon: ImageVector, label: String, value: String, color: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Column {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = Typography.labelSmall.copy(color = TextSecondary, fontSize = 9.sp))
            Text(value, style = Typography.bodyMedium.copy(fontWeight = FontWeight.Black))
        }
    }
}

@Composable
fun TodayCompactProgressCard(completed: Int, total: Int) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    val animatedProgress by animateFloatAsState(progress, label = "progress")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, BorderGlass)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(64.dp),
                    color = BorderGlass,
                    strokeWidth = 6.dp
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(64.dp),
                    color = IncomeGreen,
                    strokeWidth = 6.dp,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    style = Typography.labelMedium.copy(fontWeight = FontWeight.Black)
                )
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column {
                Text("Today's Progress", style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    "$completed of $total Habits Completed",
                    style = Typography.bodySmall.copy(color = TextSecondary)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = IncomeGreen,
                    trackColor = BorderGlass
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
    val scale by animateFloatAsState(if (item.completed) 0.98f else 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable { onToggle() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.completed) IncomeGreen.copy(alpha = 0.05f) else DarkSurface
        ),
        border = BorderStroke(
            1.dp, 
            if (item.completed) IncomeGreen.copy(alpha = 0.2f) else BorderGlass
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Interactive Checkbox
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (item.completed) IncomeGreen else Color.Transparent)
                    .border(2.dp, if (item.completed) IncomeGreen else TextSecondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (item.completed) {
                    Icon(Icons.Default.Check, null, tint = DarkBg, modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = Typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (item.completed) TextSecondary else TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isDaily) "Daily" else "Weekly",
                        style = Typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                    )
                    if (habitDetails?.reminderEnabled == true && habitDetails.reminderTime != null) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.Notifications, null, tint = AccentBlue, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(habitDetails.reminderTime, style = Typography.labelSmall.copy(color = AccentBlue, fontSize = 10.sp))
                    }
                }
            }
            
            // Emoji / Streak Placeholder
            if (!item.completed) {
                Text("🔥", fontSize = 14.sp)
            } else {
                Icon(Icons.Default.CheckCircle, null, tint = IncomeGreen, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun HabitManagementCard(habit: HabitMini, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(habit.name, style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Text(
                habit.frequency.replaceFirstChar { it.uppercase() },
                style = Typography.labelSmall.copy(color = TextSecondary)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.DeleteOutline, null, tint = ExpenseRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = Typography.labelSmall.copy(
            color = AccentBlue,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp
        ),
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp)
    )
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
        containerColor = DarkSurface,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.border(1.dp, BorderGlass, RoundedCornerShape(28.dp)),
        title = {
            Text(title, style = Typography.headlineSmall.copy(fontWeight = FontWeight.Black))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("What's the habit?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderGlass,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    )
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBg)
                        .padding(4.dp)
                ) {
                    listOf("Daily", "Weekly").forEach { freq ->
                        val active = frequency == freq
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) AccentBlue else Color.Transparent)
                                .clickable { frequency = freq }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(freq, color = if (active) DarkBg else TextSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (frequency == "Weekly") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                            val isSelected = selectedDays.contains(day)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDays = if (isSelected) selectedDays - day else selectedDays + day },
                                label = { Text(day) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentBlue,
                                    selectedLabelColor = DarkBg
                                )
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                    Text("Reminder")
                    Spacer(Modifier.weight(1f))
                    if (reminderEnabled) {
                        Text(
                            reminderTime, 
                            Modifier.clickable {
                                val parts = reminderTime.split(":")
                                android.app.TimePickerDialog(context, { _, h, m ->
                                    reminderTime = String.format("%02d:%02d", h, m)
                                }, parts[0].toInt(), parts[1].toInt(), true).show()
                            },
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, frequency, selectedDays.toList(), reminderTime, reminderEnabled) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = DarkBg)
            ) {
                Text("Confirm", fontWeight = FontWeight.Black)
            }
        }
    )
}

@Composable
fun EmptyStateView(icon: ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = BorderGlass, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
fun ErrorStateView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = ExpenseRed)
    }
}

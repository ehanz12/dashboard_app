package com.reihan.dashboard.ui.habit

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.reihan.dashboard.data.model.HabitLogTodayResponse
import com.reihan.dashboard.data.model.HabitResponse
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
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitResponse?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadHabitData()
    }

    Scaffold(
        containerColor = DarkBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentBlue,
                contentColor = DarkBg,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // --- TOP BAR ---
                HabitTopBar(onBack = onBack)

                // --- CONTENT ---
                when (val current = state) {
                    is HabitState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AccentBlue, strokeWidth = 2.dp)
                        }
                    }
                    is HabitState.Error -> {
                        ErrorStateView(message = current.message)
                    }
                    is HabitState.Success -> {
                        HabitContent(
                            current = current,
                            viewModel = viewModel,
                            onEdit = { editingHabit = it }
                        )
                    }
                }
            }
        }
    }

    // --- MODALS ---
    if (showAddDialog) {
        HabitFormDialog(
            title = "Create New Habit",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, freq ->
                viewModel.addHabit(
                    name = name,
                    frequency = freq,
                    onSuccess = { showAddDialog = false },
                    onFailure = { err ->
                        android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    if (editingHabit != null) {
        HabitFormDialog(
            title = "Edit Habit",
            initialName = editingHabit!!.name,
            initialFrequency = editingHabit!!.frequency,
            onDismiss = { editingHabit = null },
            onConfirm = { name, freq ->
                viewModel.editHabit(
                    id = editingHabit!!.id,
                    name = name,
                    frequency = freq,
                    onSuccess = { editingHabit = null },
                    onFailure = { err ->
                        android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
}

@Composable
fun HabitTopBar(onBack: () -> Unit) {
    val date = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }
    
    Column(modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
                    .border(1.dp, BorderGlass, CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Habit Tracker",
                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
                Text(
                    text = date,
                    style = Typography.bodySmall.copy(color = TextSecondary, letterSpacing = 1.sp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Horizontal Day Selector (Visual Only for now)
        WeekDaySelector()
    }
}

@Composable
fun WeekDaySelector() {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val context = LocalContext.current
    
    // Find today's index (0 = Mon, 1 = Tue ... 6 = Sun)
    val todayCalendar = Calendar.getInstance()
    val dayOfWeek = todayCalendar.get(Calendar.DAY_OF_WEEK)
    val todayIndex = when (dayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }
    
    // Calculate calendar dates of the current week (Monday to Sunday)
    val weekDates = remember {
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        val diffToMonday = when (currentDay) {
            Calendar.SUNDAY -> -6
            else -> Calendar.MONDAY - currentDay
        }
        cal.add(Calendar.DAY_OF_MONTH, diffToMonday)
        
        val list = mutableListOf<Int>()
        for (i in 0..6) {
            list.add(cal.get(Calendar.DAY_OF_MONTH))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, day ->
            val isSelected = index == todayIndex
            val dateNum = weekDates.getOrElse(index) { index + 1 }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) AccentBlue else DarkSurface)
                    .width(42.dp)
                    .clickable {
                        if (!isSelected) {
                            android.widget.Toast.makeText(
                                context,
                                "Consistency is key! You can only log habits for today.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = day,
                    style = Typography.labelSmall.copy(
                        color = if (isSelected) DarkBg else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateNum.toString(),
                    style = Typography.bodySmall.copy(
                        color = if (isSelected) DarkBg else TextPrimary,
                        fontWeight = FontWeight.Black
                    )
                )
            }
        }
    }
}

@Composable
fun HabitContent(
    current: HabitState.Success,
    viewModel: HabitViewModel,
    onEdit: (HabitResponse) -> Unit
) {
    val completedCount = current.habitsToday.count { it.completed }
    val totalCount = current.habitsToday.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
    ) {
        // --- SUMMARY CARD ---
        item {
            ProgressSummaryCard(progress, completedCount, totalCount)
        }

        // --- TODAY'S SECTION ---
        item {
            SectionHeader("Today's Journey", "${(progress * 100).toInt()}% Done")
        }

        if (current.habitsToday.isEmpty()) {
            item { EmptyStateView(Icons.Default.Bedtime, "No habits for today") }
        } else {
            items(current.habitsToday) { logItem ->
                HabitChecklistItem(
                    item = logItem,
                    onToggle = {
                        viewModel.toggleHabitToday(
                            habitId = logItem.habitId,
                            onFailure = { err ->
                                android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                )
            }
        }

        // --- ALL HABITS SECTION ---
        item {
            SectionHeader("Management", "${current.allHabits.size} Total")
        }

        if (current.allHabits.isEmpty()) {
            item { EmptyStateView(Icons.Default.AddTask, "Add your first habit!") }
        } else {
            items(current.allHabits) { habit ->
                HabitManagementItem(
                    habit = habit,
                    onClick = { onEdit(habit) },
                    onDelete = {
                        viewModel.removeHabit(
                            id = habit.id,
                            onFailure = { err ->
                                android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ProgressSummaryCard(progress: Float, done: Int, total: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(AccentBlue, AccentBlue.copy(alpha = 0.7f))
                )
            )
            .padding(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Keep it up!",
                    style = Typography.headlineSmall.copy(color = DarkBg, fontWeight = FontWeight.Black)
                )
                Text(
                    text = "You've completed $done out of $total tasks today. Consistency builds mastery.",
                    style = Typography.bodyMedium.copy(color = DarkBg.copy(alpha = 0.7f), lineHeight = 20.sp),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            // Circular Progress
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg.copy(alpha = 0.1f),
                    strokeWidth = 8.dp,
                    trackColor = Color.Transparent
                )
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg,
                    strokeWidth = 8.dp,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = Typography.labelLarge.copy(color = DarkBg, fontWeight = FontWeight.Black)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
        Text(
            text = subtitle,
            style = Typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun HabitChecklistItem(item: HabitLogTodayResponse, onToggle: () -> Unit) {
    val scale by animateFloatAsState(if (item.completed) 0.98f else 1f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.completed) AccentBlue.copy(alpha = 0.05f) else DarkSurface
        ),
        border = BorderStroke(
            1.dp, 
            if (item.completed) AccentBlue.copy(alpha = 0.3f) else BorderGlass
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Custom Checkbox
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (item.completed) AccentBlue else Color.Transparent)
                    .border(
                        2.dp, 
                        if (item.completed) AccentBlue else TextSecondary.copy(alpha = 0.3f), 
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.completed) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = DarkBg, modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = item.name,
                    style = Typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (item.completed) TextSecondary else TextPrimary
                )
                Text(
                    text = if (item.completed) "Completed today" else "Pending action",
                    style = Typography.bodySmall.copy(color = TextSecondary.copy(alpha = 0.7f))
                )
            }
        }
    }
}

@Composable
fun HabitManagementItem(habit: HabitResponse, onClick: () -> Unit, onDelete: () -> Unit) {
    val isDaily = habit.frequency.equals("daily", ignoreCase = true)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isDaily) AccentBlue.copy(alpha = 0.1f) else AccentPurple.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isDaily) Icons.Default.Repeat else Icons.Default.Event,
                contentDescription = null,
                tint = if (isDaily) AccentBlue else AccentPurple,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = habit.name, style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            Text(
                text = habit.frequency.replaceFirstChar { it.uppercase() },
                style = Typography.bodySmall.copy(color = TextSecondary)
            )
        }
        
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ExpenseRed, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun HabitFormDialog(
    title: String,
    initialName: String = "",
    initialFrequency: String = "Daily",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var frequency by remember { mutableStateOf(initialFrequency.replaceFirstChar { it.uppercase() }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.border(1.dp, BorderGlass, RoundedCornerShape(28.dp)),
        title = {
            Text(
                text = title,
                style = Typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("What's the habit?", color = TextSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderGlass,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    )
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkBg)
                        .padding(4.dp)
                ) {
                    listOf("Daily", "Weekly").forEach { freq ->
                        val active = frequency == freq
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) AccentBlue else Color.Transparent)
                                .clickable { frequency = freq }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = freq,
                                color = if (active) DarkBg else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, frequency) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = DarkBg)
            ) {
                Text("Confirm", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun EmptyStateView(icon: ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary.copy(alpha = 0.1f), modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, color = TextSecondary.copy(alpha = 0.5f), style = Typography.bodyMedium)
    }
}

@Composable
fun ErrorStateView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, color = ExpenseRed, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp))
        }
    }
}

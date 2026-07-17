package com.reihan.dashboard.ui.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reihan.dashboard.data.model.HabitLogTodayResponse
import com.reihan.dashboard.data.model.TaskResponse
import com.reihan.dashboard.data.model.TimeblockResponse
import com.reihan.dashboard.ui.components.DeleteConfirmationDialog
import com.reihan.dashboard.ui.components.StatusPopup
import com.reihan.dashboard.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    
    var currentMonth by remember { mutableStateOf(Calendar.getInstance().apply { 
        set(Calendar.DAY_OF_MONTH, 1) 
    }) }
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTimeblock by remember { mutableStateOf<TimeblockResponse?>(null) }
    var statusPopup by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = DarkBg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = AccentSchedule,
                contentColor = DarkBg,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 96.dp, end = 8.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 12.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("", fontWeight = FontWeight.Black) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item { CalendarTopBar(onBack = onBack) }

            item {
                MonthHeader(
                    currentMonth = currentMonth,
                    onMonthChange = { currentMonth = it }
                )
            }

            item {
                CalendarGrid(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.selectDate(it) }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = BorderGlass)
            }

            item {
                Text(
                    text = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(selectedDate.time),
                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            when (val s = state) {
                is CalendarState.Loading -> item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentBlue)
                    }
                }
                is CalendarState.Error -> item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(s.message, color = ExpenseRed, modifier = Modifier.padding(24.dp), textAlign = TextAlign.Center)
                    }
                }
                is CalendarState.Success -> {
                    // Inline Agenda items here for unified scrolling
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
                    val filteredTasks = s.tasks.filter { it.dueDate?.startsWith(dateStr) == true }
                    val mappedDayOfWeek = when (selectedDate.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.SUNDAY -> 7
                        else -> selectedDate.get(Calendar.DAY_OF_WEEK) - 1
                    }
                    val filteredTimeblocks = s.timeblocks.filter { block ->
                        when {
                            !block.date.isNullOrEmpty() -> block.date.startsWith(dateStr)
                            block.dayOfWeek != null -> block.dayOfWeek == mappedDayOfWeek
                            else -> false
                        }
                    }

                    if (filteredTasks.isNotEmpty()) {
                        item { 
                            Box(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                                AgendaSectionTitle("Tasks", AccentTask) 
                            }
                        }
                        items(filteredTasks) { task ->
                            Box(Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                                AgendaItem(task.title, if (task.isCompleted) "Completed" else "Pending", AccentTask, task.isCompleted)
                            }
                        }
                    }

                    if (filteredTimeblocks.isNotEmpty()) {
                        item { 
                            Box(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                                AgendaSectionTitle("Schedule", AccentSchedule) 
                            }
                        }
                        items(filteredTimeblocks) { block ->
                            Box(Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                                AgendaItemWithAction(
                                    title = block.activityName,
                                    subtitle = "${block.startTime} - ${block.endTime}",
                                    color = AccentSchedule,
                                    onClick = { editingTimeblock = block },
                                    onDelete = { deleteConfirmId = block.id }
                                )
                            }
                        }
                    }

                    if (s.habits.isNotEmpty()) {
                        item { 
                            Box(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                                AgendaSectionTitle("Habits", AccentBlue) 
                            }
                        }
                        items(s.habits) { habit ->
                            Box(Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                                AgendaItem(habit.name, if (habit.completed) "Completed" else "Goal", AccentBlue, habit.completed)
                            }
                        }
                    }
                    
                    if (filteredTasks.isEmpty() && filteredTimeblocks.isEmpty() && s.habits.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                Text("No activities for this day", color = TextSecondary, style = Typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    // --- STATUS POPUP ---
    statusPopup?.let { (isSuccess, message) ->
        StatusPopup(
            isSuccess = isSuccess,
            message = message,
            onDismiss = { statusPopup = null }
        )
    }

    // --- DELETE CONFIRMATION ---
    deleteConfirmId?.let { id ->
        DeleteConfirmationDialog(
            title = "Hapus Jadwal?",
            message = "Apakah Anda yakin ingin menghapus jadwal ini?",
            onConfirm = {
                viewModel.deleteTimeblock(id) { err ->
                    statusPopup = false to err
                }
                deleteConfirmId = null
            },
            onDismiss = { deleteConfirmId = null }
        )
    }

    if (showCreateDialog) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
        TimeblockDialog(
            title = "Plan Activity",
            initialDate = dateStr,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, start, end, day, date ->
                viewModel.addTimeblock(
                    activityName = name,
                    startTime = start,
                    endTime = end,
                    dayOfWeek = day,
                    date = date,
                    onSuccess = { 
                        showCreateDialog = false 
                        statusPopup = true to "Jadwal berhasil ditambahkan!"
                    },
                    onFailure = { err ->
                        statusPopup = false to err
                    }
                )
            }
        )
    }

    if (editingTimeblock != null) {
        TimeblockDialog(
            title = "Edit Activity",
            timeblock = editingTimeblock,
            onDismiss = { editingTimeblock = null },
            onConfirm = { name, start, end, day, date ->
                viewModel.updateTimeblock(
                    id = editingTimeblock!!.id,
                    activityName = name,
                    startTime = start,
                    endTime = end,
                    dayOfWeek = day,
                    date = date,
                    onSuccess = { 
                        editingTimeblock = null 
                        statusPopup = true to "Jadwal berhasil diperbarui!"
                    },
                    onFailure = { err ->
                        statusPopup = false to err
                    }
                )
            }
        )
    }
}

@Composable
fun CalendarTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DarkSurface)
                .border(1.dp, BorderGlass, CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text("Calendar View", style = Typography.titleLarge.copy(fontWeight = FontWeight.Black))
    }
}

@Composable
fun MonthHeader(currentMonth: Calendar, onMonthChange: (Calendar) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time),
            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AccentBlue)
        )
        Row {
            IconButton(onClick = {
                val newMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                onMonthChange(newMonth)
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = TextPrimary)
            }
            IconButton(onClick = {
                val newMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                onMonthChange(newMonth)
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextPrimary)
            }
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: Calendar,
    selectedDate: Calendar,
    onDateSelected: (Calendar) -> Unit
) {
    val daysOfWeek = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    
    val days = remember(currentMonth) {
        val list = mutableListOf<Calendar?>()
        val cal = currentMonth.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        
        var firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2 
        if (firstDayOfWeek == -1) firstDayOfWeek = 6
        
        for (i in 0 until firstDayOfWeek) {
            list.add(null)
        }
        
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..maxDay) {
            val d = cal.clone() as Calendar
            d.set(Calendar.DAY_OF_MONTH, i)
            list.add(d)
        }
        list
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = Typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(260.dp),
            userScrollEnabled = false
        ) {
            items(days.size) { index ->
                val date = days[index]
                if (date != null) {
                    val isSelected = date.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                                     date.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
                    val isToday = date.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR) &&
                                  date.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentBlue else if (isToday) AccentBlue.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.get(Calendar.DAY_OF_MONTH).toString(),
                            style = Typography.bodyMedium.copy(
                                fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Normal,
                                color = if (isSelected) DarkBg else if (isToday) AccentBlue else TextPrimary
                            )
                        )
                    }
                } else {
                    Box(modifier = Modifier.aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
fun AgendaSectionTitle(title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp, 4.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = Typography.labelLarge.copy(fontWeight = FontWeight.Black, color = color))
    }
}

@Composable
fun AgendaItem(title: String, subtitle: String, color: Color, isDone: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isDone) IncomeGreen else color)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            Text(subtitle, style = Typography.bodySmall.copy(color = TextSecondary))
        }
    }
}

@Composable
fun AgendaItemWithAction(title: String, subtitle: String, color: Color, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                Text(subtitle, style = Typography.bodySmall.copy(color = TextSecondary))
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeblockDialog(
    title: String,
    timeblock: TimeblockResponse? = null,
    initialDate: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, start: String, end: String, day: Int?, date: String?) -> Unit
) {
    var activityName by remember { mutableStateOf(timeblock?.activityName ?: "") }
    var startTime by remember { mutableStateOf(timeblock?.startTime ?: "08:00") }
    var endTime by remember { mutableStateOf(timeblock?.endTime ?: "09:00") }
    
    var mode by remember { mutableIntStateOf(if (timeblock?.dayOfWeek != null) 1 else 0) }
    var selectedDate by remember { mutableStateOf(timeblock?.date ?: initialDate ?: "") }
    var selectedDay by remember { mutableIntStateOf(timeblock?.dayOfWeek ?: 1) }
    
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
                    value = activityName,
                    onValueChange = { activityName = it },
                    placeholder = { Text("What's the plan?", color = TextSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentSchedule,
                        unfocusedBorderColor = BorderGlass,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Time", style = Typography.labelSmall, color = TextSecondary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBg)
                                .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
                                .clickable {
                                    val parts = startTime.split(":")
                                    TimePickerDialog(context, { _, h, m ->
                                        startTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                    }, parts[0].toInt(), parts[1].toInt(), true).show()
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(startTime, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End Time", style = Typography.labelSmall, color = TextSecondary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBg)
                                .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
                                .clickable {
                                    val parts = endTime.split(":")
                                    TimePickerDialog(context, { _, h, m ->
                                        endTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                    }, parts[0].toInt(), parts[1].toInt(), true).show()
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(endTime, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBg)
                        .padding(4.dp)
                ) {
                    listOf("Specific Date", "Recurring").forEachIndexed { index, label ->
                        val sel = mode == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) AccentSchedule else Color.Transparent)
                                .clickable { 
                                    mode = index 
                                    // Reset other field when switching mode to avoid sending both
                                    if (mode == 0) selectedDay = 1 else selectedDate = initialDate ?: ""
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (sel) DarkBg else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                
                if (mode == 1) {
                    Column {
                        Text("Day of Week", style = Typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(DAY_NAMES) { i, name ->
                                val d = i + 1
                                val sel = selectedDay == d
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (sel) AccentSchedule else DarkBg)
                                        .border(1.dp, if (sel) AccentSchedule else BorderGlass, RoundedCornerShape(8.dp))
                                        .clickable { selectedDay = d }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(name, color = if (sel) DarkBg else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    Column {
                        Text("Date", style = Typography.labelSmall, color = TextSecondary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBg)
                                .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
                                .clickable {
                                    val calendar = Calendar.getInstance()
                                    DatePickerDialog(context, { _, y, m, d ->
                                        selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(if (selectedDate.isEmpty()) "Select Date" else selectedDate, color = if (selectedDate.isEmpty()) TextSecondary else TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (activityName.isNotBlank()) {
                        onConfirm(
                            activityName, 
                            startTime, 
                            endTime, 
                            if (mode == 1) selectedDay else null,
                            if (mode == 0) selectedDate else null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentSchedule, contentColor = DarkBg)
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

package com.reihan.dashboard.ui.timeblock

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import com.reihan.dashboard.data.model.TimeblockResponse
import com.reihan.dashboard.ui.theme.*

val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
val DAY_FULL = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

val PRESET_COLORS = listOf(
    "#4F46E5", "#10B981", "#EF4444", "#F59E0B", "#22D3EE",
    "#A78BFA", "#EC4899", "#2DD4BF", "#FB923C", "#6B7280"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeblockScreen(
    viewModel: TimeblockViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var editingTimeblock by remember { mutableStateOf<TimeblockResponse?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadTimeblocks()
    }

    Scaffold(
        containerColor = DarkBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = AccentSchedule,
                contentColor = DarkBg,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 96.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Block")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // --- TOP BAR ---
            TimeblockTopBar(onBack = onBack)

            // --- DAY SELECTOR ---
            val selectedDay = if (state is TimeblockState.Success) (state as TimeblockState.Success).selectedDay else 1
            DaySelector(selectedDay = selectedDay, onDaySelect = { viewModel.loadTimeblocks(it) })

            Spacer(modifier = Modifier.height(16.dp))

            when (val s = state) {
                is TimeblockState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentSchedule, strokeWidth = 2.dp)
                    }
                }
                is TimeblockState.Error -> {
                    TimeblockErrorView(s.message) { viewModel.loadTimeblocks() }
                }
                is TimeblockState.Success -> {
                    if (s.timeblocks.isEmpty()) {
                        EmptyScheduleView(DAY_FULL.getOrElse(s.selectedDay - 1) { "the day" })
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(s.timeblocks) { tb ->
                                ModernTimeblockCard(
                                    tb = tb,
                                    onClick = { editingTimeblock = tb },
                                    onDelete = {
                                        viewModel.deleteTimeblock(
                                            id = tb.id,
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
            }
        }
    }

    if (showDialog) {
        val currentDay = if (state is TimeblockState.Success) (state as TimeblockState.Success).selectedDay else 1
        TimeblockFormDialog(
            selectedDay = currentDay,
            onDismiss = { showDialog = false },
            onConfirm = { name, start, end, color, day ->
                viewModel.addTimeblock(
                    activityName = name,
                    startTime = start,
                    endTime = end,
                    colorCode = color,
                    dayOfWeek = day,
                    onSuccess = { showDialog = false },
                    onFailure = { err ->
                        android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    if (editingTimeblock != null) {
        val tb = editingTimeblock!!
        TimeblockFormDialog(
            timeblock = tb,
            selectedDay = tb.dayOfWeek,
            onDismiss = { editingTimeblock = null },
            onConfirm = { name, start, end, color, day ->
                viewModel.editTimeblock(
                    id = tb.id,
                    activityName = name,
                    startTime = start,
                    endTime = end,
                    colorCode = color,
                    dayOfWeek = day,
                    onSuccess = { editingTimeblock = null },
                    onFailure = { err ->
                        android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
}

@Composable
fun TimeblockTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
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
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(18.dp))
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text("PLANNER", style = Typography.labelSmall.copy(color = AccentSchedule, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
            Text("Schedule", style = Typography.titleLarge.copy(fontWeight = FontWeight.Black))
        }
    }
}

@Composable
fun DaySelector(selectedDay: Int, onDaySelect: (Int) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(DAY_NAMES) { index, name ->
            val dayNum = index + 1
            val isSelected = selectedDay == dayNum
            
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) AccentSchedule else DarkSurface)
                    .border(1.dp, if (isSelected) Color.Transparent else BorderGlass, RoundedCornerShape(16.dp))
                    .clickable { onDaySelect(dayNum) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = name,
                    style = Typography.labelSmall.copy(
                        color = if (isSelected) DarkBg else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) DarkBg else Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun ModernTimeblockCard(tb: TimeblockResponse, onClick: () -> Unit, onDelete: (String) -> Unit) {
    val accentColor = try { Color(android.graphics.Color.parseColor(tb.colorCode)) } catch (e: Exception) { AccentSchedule }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurface)
            .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(80.dp)
                .background(accentColor)
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(20.dp)
        ) {
            Text(
                text = tb.activityName,
                style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${tb.startTime} – ${tb.endTime}",
                    style = Typography.labelLarge.copy(color = accentColor, fontWeight = FontWeight.Bold)
                )
            }
        }
        
        IconButton(onClick = { onDelete(tb.id) }, modifier = Modifier.padding(end = 8.dp)) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeblockFormDialog(
    timeblock: TimeblockResponse? = null,
    selectedDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (name: String, start: String, end: String, color: String, day: Int) -> Unit
) {
    var activityName by remember { mutableStateOf(timeblock?.activityName ?: "") }
    var startTime by remember { mutableStateOf(timeblock?.startTime ?: "08:00") }
    var endTime by remember { mutableStateOf(timeblock?.endTime ?: "09:00") }
    var selectedColor by remember { mutableStateOf(timeblock?.colorCode ?: PRESET_COLORS[0]) }
    var dayOfWeek by remember { mutableIntStateOf(timeblock?.dayOfWeek ?: selectedDay) }
    
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.border(1.dp, BorderGlass, RoundedCornerShape(28.dp)),
        title = {
            Text(if (timeblock == null) "Plan Activity" else "Edit Activity", style = Typography.headlineSmall.copy(fontWeight = FontWeight.Black))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                OutlinedTextField(
                    value = activityName,
                    onValueChange = { activityName = it },
                    placeholder = { Text("What's the plan?", color = TextSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
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
                    TimeField(value = startTime, label = "Start", modifier = Modifier.weight(1f)) {
                        val parts = startTime.split(":")
                        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
                        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        TimePickerDialog(context, { _, h, m ->
                            startTime = String.format("%02d:%02d", h, m)
                        }, hour, minute, true).show()
                    }
                    TimeField(value = endTime, label = "End", modifier = Modifier.weight(1f)) {
                        val parts = endTime.split(":")
                        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
                        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        TimePickerDialog(context, { _, h, m ->
                            endTime = String.format("%02d:%02d", h, m)
                        }, hour, minute, true).show()
                    }
                }

                // Color Selection
                Column {
                    Text("THEME", style = Typography.labelSmall.copy(color = AccentSchedule, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(PRESET_COLORS) { hex ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { AccentSchedule }
                            val isSelected = selectedColor == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(2.dp, if (isSelected) TextPrimary else Color.Transparent, CircleShape)
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                }
                
                // Day Selection
                Column {
                    Text("DAY", style = Typography.labelSmall.copy(color = AccentSchedule, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(DAY_NAMES) { i, name ->
                            val d = i + 1
                            val sel = dayOfWeek == d
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (sel) AccentSchedule else DarkBg)
                                    .border(1.dp, if (sel) AccentSchedule else BorderGlass, RoundedCornerShape(12.dp))
                                    .clickable { dayOfWeek = d }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    name,
                                    style = Typography.labelSmall.copy(
                                        color = if (sel) DarkBg else TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (activityName.isNotBlank()) onConfirm(activityName, startTime, endTime, selectedColor, dayOfWeek) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
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

@Composable
fun TimeField(value: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, style = Typography.labelSmall, color = TextSecondary) },
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
        Box(modifier = Modifier.matchParentSize().clickable { onClick() })
    }
}

@Composable
fun EmptyScheduleView(dayName: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.EventAvailable, contentDescription = null, tint = TextSecondary.copy(alpha = 0.1f), modifier = Modifier.size(100.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("No plans for $dayName", style = Typography.bodyLarge.copy(color = TextSecondary.copy(alpha = 0.5f)))
    }
}

@Composable
fun TimeblockErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AccentSchedule)) {
            Text("Retry", color = DarkBg)
        }
    }
}

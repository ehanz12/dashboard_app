package com.reihan.dashboard.ui.task

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import com.reihan.dashboard.data.model.TaskResponse
import com.reihan.dashboard.ui.components.DeleteConfirmationDialog
import com.reihan.dashboard.ui.components.StatusPopup
import com.reihan.dashboard.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskResponse?>(null) }
    var statusPopup by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadTasks()
    }

    Scaffold(
        containerColor = DarkBg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                containerColor = AccentTask,
                contentColor = DarkBg,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 96.dp, end = 16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 12.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Task", fontWeight = FontWeight.Black) }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            TaskTopBar(onBack = onBack)

            when (val s = state) {
                is TaskState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentTask, strokeWidth = 2.dp)
                    }
                }
                is TaskState.Error -> {
                    TaskErrorView(s.message) { viewModel.loadTasks() }
                }
                is TaskState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        item {
                            QuadrantSection(
                                title = "Do First",
                                subtitle = "Urgent & Important",
                                color = QuadrantQ1,
                                tasks = s.q1,
                                viewModel = viewModel,
                                onClickTask = { editingTask = it },
                                onDeleteTask = { deleteConfirmId = it },
                                onStatusUpdate = { isSuccess, message ->
                                    statusPopup = isSuccess to message
                                }
                            )
                        }

                        item {
                            QuadrantSection(
                                title = "Schedule",
                                subtitle = "Important but Not Urgent",
                                color = QuadrantQ2,
                                tasks = s.q2,
                                viewModel = viewModel,
                                onClickTask = { editingTask = it },
                                onDeleteTask = { deleteConfirmId = it },
                                onStatusUpdate = { isSuccess, message ->
                                    statusPopup = isSuccess to message
                                }
                            )
                        }

                        item {
                            QuadrantSection(
                                title = "Delegate",
                                subtitle = "Urgent but Not Important",
                                color = QuadrantQ3,
                                tasks = s.q3,
                                viewModel = viewModel,
                                onClickTask = { editingTask = it },
                                onDeleteTask = { deleteConfirmId = it },
                                onStatusUpdate = { isSuccess, message ->
                                    statusPopup = isSuccess to message
                                }
                            )
                        }

                        item {
                            QuadrantSection(
                                title = "Eliminate",
                                subtitle = "Neither Urgent nor Important",
                                color = QuadrantQ4,
                                tasks = s.q4,
                                viewModel = viewModel,
                                onClickTask = { editingTask = it },
                                onDeleteTask = { deleteConfirmId = it },
                                onStatusUpdate = { isSuccess, message ->
                                    statusPopup = isSuccess to message
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        TaskFormDialog(
            onDismiss = { showDialog = false },
            onConfirm = { title, desc, quad, dueDate ->
                viewModel.addTask(
                    title = title,
                    description = desc,
                    quadrant = quad,
                    dueDate = dueDate,
                    onSuccess = { 
                        showDialog = false
                        statusPopup = true to "Task created successfully!"
                    },
                    onFailure = { err ->
                        statusPopup = false to err
                    }
                )
            }
        )
    }

    if (editingTask != null) {
        val task = editingTask!!
        TaskFormDialog(
            task = task,
            onDismiss = { editingTask = null },
            onConfirm = { title, desc, quad, dueDate ->
                viewModel.editTask(
                    id = task.id,
                    title = title,
                    description = desc,
                    quadrant = quad,
                    isCompleted = task.isCompleted,
                    dueDate = dueDate,
                    onSuccess = { 
                        editingTask = null
                        statusPopup = true to "Task updated successfully!"
                    },
                    onFailure = { err ->
                        statusPopup = false to err
                    }
                )
            }
        )
    }

    statusPopup?.let { (isSuccess, message) ->
        StatusPopup(
            isSuccess = isSuccess,
            message = message,
            onDismiss = { statusPopup = null }
        )
    }

    deleteConfirmId?.let { id ->
        DeleteConfirmationDialog(
            title = "Hapus Tugas?",
            message = "Apakah Anda yakin ingin menghapus tugas ini?",
            onConfirm = {
                viewModel.deleteTask(id) { err ->
                    statusPopup = false to err
                }
                deleteConfirmId = null
            },
            onDismiss = { deleteConfirmId = null }
        )
    }
}

@Composable
fun TaskTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 24.dp),
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
            Text("Eisenhower Matrix", style = Typography.labelSmall.copy(color = AccentTask, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
            Text("Focus & Tasks", style = Typography.titleLarge.copy(fontWeight = FontWeight.Black))
        }
    }
}

@Composable
fun QuadrantSection(
    title: String,
    subtitle: String,
    color: Color,
    tasks: List<TaskResponse>,
    viewModel: TaskViewModel,
    onClickTask: (TaskResponse) -> Unit,
    onDeleteTask: (String) -> Unit,
    onStatusUpdate: (Boolean, String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(subtitle, style = Typography.labelSmall.copy(color = TextSecondary))
                }
            }
            Text(
                text = "${tasks.size} tasks",
                style = Typography.labelSmall.copy(color = color.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
            )
        }

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No tasks in this quadrant", style = Typography.bodySmall.copy(color = TextSecondary.copy(alpha = 0.5f)))
            }
        } else {
            tasks.forEach { task ->
                ModernTaskItem(
                    task = task,
                    accentColor = color,
                    onClick = { onClickTask(task) },
                    onToggle = {
                        viewModel.toggleTask(
                            id = task.id,
                            onFailure = { err ->
                                onStatusUpdate(false, err)
                            }
                        )
                    },
                    onDelete = {
                        onDeleteTask(task.id)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ModernTaskItem(
    task: TaskResponse,
    accentColor: Color,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = task.isCompleted
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCompleted) DarkSurface.copy(alpha = 0.5f) else DarkSurface)
            .border(1.dp, if (isCompleted) BorderGlass.copy(alpha = 0.3f) else BorderGlass, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isCompleted) accentColor else Color.Transparent)
                .border(2.dp, if (isCompleted) accentColor else TextSecondary.copy(alpha = 0.3f), CircleShape)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = DarkBg, modifier = Modifier.size(14.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = Typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (isCompleted) TextSecondary else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = Typography.bodySmall.copy(color = TextSecondary.copy(alpha = 0.7f)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ExpenseRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormDialog(
    task: TaskResponse? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, quadrant: Int, dueDate: String?) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var desc by remember { mutableStateOf(task?.description ?: "") }
    var selectedQuadrant by remember { mutableIntStateOf(task?.quadrant ?: 1) }
    var dueDate by remember { mutableStateOf<String?>(task?.dueDate?.take(10)) }

    val quadrants = listOf(
        1 to "Do First" to QuadrantQ1,
        2 to "Schedule" to QuadrantQ2,
        3 to "Delegate" to QuadrantQ3,
        4 to "Eliminate" to QuadrantQ4
    )

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.border(1.dp, BorderGlass, RoundedCornerShape(28.dp)),
        title = {
            Text(if (task == null) "Create Task" else "Edit Task", style = Typography.headlineSmall.copy(fontWeight = FontWeight.Black))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("TITLE", style = Typography.labelSmall.copy(color = AccentTask, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("What needs to be done?", color = TextSecondary.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentTask,
                            unfocusedBorderColor = BorderGlass,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        )
                    )
                }

                Column {
                    Text("DESCRIPTION", style = Typography.labelSmall.copy(color = AccentTask, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        placeholder = { Text("Add details/notes...", color = TextSecondary.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentTask,
                            unfocusedBorderColor = BorderGlass,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        ),
                        maxLines = 3
                    )
                }

                Column {
                    Text("DUE DATE", style = Typography.labelSmall.copy(color = AccentTask, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box {
                        OutlinedTextField(
                            value = dueDate ?: "No due date",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            trailingIcon = {
                                if (dueDate != null) {
                                    IconButton(onClick = { dueDate = null }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear date", tint = ExpenseRed)
                                    }
                                } else {
                                    Icon(Icons.Default.CalendarToday, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (dueDate != null) TextPrimary else TextSecondary,
                                unfocusedTextColor = if (dueDate != null) TextPrimary else TextSecondary,
                                focusedBorderColor = AccentTask,
                                unfocusedBorderColor = BorderGlass,
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    val calendar = Calendar.getInstance()
                                    if (dueDate != null) {
                                        val parts = dueDate!!.split("-")
                                        if (parts.size == 3) {
                                            try {
                                                calendar.set(Calendar.YEAR, parts[0].toInt())
                                                calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                                                calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                                            } catch (e: Exception) {}
                                        }
                                    }
                                    DatePickerDialog(context, { _, y, m, d ->
                                        dueDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                                }
                        )
                    }
                }

                Column {
                    Text("QUADRANT", style = Typography.labelSmall.copy(color = AccentTask, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quadrants.forEach { (pair, color) ->
                            val (q, label) = pair
                            val selected = selectedQuadrant == q
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.2f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) color else DarkBg)
                                    .border(1.dp, if (selected) color else BorderGlass, RoundedCornerShape(12.dp))
                                    .clickable { selectedQuadrant = q }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (selected) DarkBg else color))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label.split(" ")[0],
                                        style = Typography.labelSmall.copy(
                                            color = if (selected) DarkBg else TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title, desc, selectedQuadrant, dueDate) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentTask, contentColor = DarkBg)
            ) {
                Text(if (task == null) "Create Task" else "Save Changes", fontWeight = FontWeight.Black)
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
fun TaskErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AccentTask)) {
            Text("Retry", color = DarkBg)
        }
    }
}

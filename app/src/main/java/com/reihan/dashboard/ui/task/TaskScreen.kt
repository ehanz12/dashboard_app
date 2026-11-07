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
import com.reihan.dashboard.ui.components.NeoBadge
import com.reihan.dashboard.ui.components.NeoButton
import com.reihan.dashboard.ui.components.NeoCard
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.reihan.dashboard.ui.components.StatusPopup
import com.reihan.dashboard.ui.components.dotGridBackground
import com.reihan.dashboard.ui.theme.*
import com.reihan.dashboard.util.VoiceActionProcessor
import com.reihan.dashboard.util.VoiceActionResult
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

    // Speech Recognizer Activity Launcher for Tasks
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()
            if (!spokenText.isNullOrBlank() && state is TaskState.Success) {
                val succ = state as TaskState.Success
                val allTasks = succ.q1 + succ.q2 + succ.q3 + succ.q4
                val actionResult = VoiceActionProcessor.processSpeech(
                    spokenText = spokenText,
                    habitsToday = emptyList(),
                    allHabits = emptyList(),
                    allTasks = allTasks,
                    categories = emptyList()
                )
                when (actionResult) {
                    is VoiceActionResult.TaskCompleted -> {
                        viewModel.toggleTask(actionResult.taskId) { err ->
                            statusPopup = false to err
                        }
                        statusPopup = true to "Berhasil menyelesaikan Task \"${actionResult.taskTitle}\"! ✅"
                    }
                    is VoiceActionResult.NoMatch -> {
                        statusPopup = false to actionResult.reason
                    }
                    else -> {
                        statusPopup = false to "Tidak menemukan task yang sesuai dengan \"$spokenText\"."
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
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara task Anda (contoh: 'sudah kerjakan laporan')")
            }
            speechLauncher.launch(intent)
        } else {
            statusPopup = false to "Izin mikrofon dibutuhkan untuk menyelesaikan task lewat suara."
        }
    }

    fun startVoiceInput() {
        audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(Unit) {
        viewModel.loadTasks()
    }

    Scaffold(
        containerColor = NeoBg,
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 96.dp, end = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = { startVoiceInput() },
                    containerColor = NeoYellow,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(2.5.dp, Color.Black, RoundedCornerShape(12.dp)),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice Task", tint = Color.Black)
                }
                ExtendedFloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = NeoCyan,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(2.5.dp, Color.Black, RoundedCornerShape(12.dp)),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black) },
                    text = { Text("New Task", fontWeight = FontWeight.Black, color = Color.Black) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .dotGridBackground()
        ) {
            TaskTopBar(onBack = onBack)

            when (val s = state) {
                is TaskState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.Black, strokeWidth = 3.dp)
                    }
                }
                is TaskState.Error -> {
                    TaskErrorView(s.message) { viewModel.loadTasks() }
                }
                is TaskState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        item {
                            QuadrantSection(
                                title = "Do First",
                                subtitle = "Urgent & Important",
                                color = NeoRed,
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
                                color = NeoBlue,
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
                                color = NeoOrange,
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
                                color = NeoTextSecondary,
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
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
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
            NeoBadge(text = "Eisenhower Matrix", containerColor = NeoYellow, contentColor = Color.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Focus & Tasks", style = Typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Color.Black))
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, CircleShape)
                        .border(1.5.dp, Color.Black, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(title, style = Typography.titleMedium.copy(fontWeight = FontWeight.Black, color = Color.Black))
                    Text(subtitle, style = Typography.labelSmall.copy(color = NeoTextSecondary))
                }
            }
            NeoBadge(text = "${tasks.size} tasks", containerColor = color, contentColor = if (color == NeoRed) Color.White else Color.Black)
        }

        if (tasks.isEmpty()) {
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.White,
                borderColor = Color.Black,
                shadowColor = Color.Black,
                shadowOffset = 3.dp,
                cornerRadius = 10.dp
            ) {
                Text("No tasks in this quadrant", style = Typography.bodySmall.copy(color = NeoTextSecondary, fontWeight = FontWeight.Bold))
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
    
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (isCompleted) Color(0xFFF0F0F0) else Color.White,
        borderColor = Color.Black,
        shadowColor = Color.Black,
        shadowOffset = 3.dp,
        cornerRadius = 10.dp,
        onClick = onClick,
        contentPadding = 12.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) accentColor else Color.White)
                    .border(2.dp, Color.Black, CircleShape)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = if (accentColor == NeoRed) Color.White else Color.Black, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = Typography.bodyLarge.copy(
                        fontWeight = FontWeight.Black,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = Color.Black
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = Typography.bodySmall.copy(color = NeoTextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = NeoRed, modifier = Modifier.size(20.dp))
            }
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
        1 to "Do First" to NeoRed,
        2 to "Schedule" to NeoBlue,
        3 to "Delegate" to NeoOrange,
        4 to "Eliminate" to NeoTextSecondary
    )

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(2.5.dp, Color.Black, RoundedCornerShape(16.dp)),
        title = {
            Text(if (task == null) "Create Task" else "Edit Task", style = Typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = Color.Black))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("TITLE", style = Typography.labelSmall.copy(color = Color.Black, fontWeight = FontWeight.Black))
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("What needs to be done?", color = NeoTextSecondary.copy(alpha = 0.5f)) },
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

                Column {
                    Text("DESCRIPTION", style = Typography.labelSmall.copy(color = Color.Black, fontWeight = FontWeight.Black))
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        placeholder = { Text("Add details/notes...", color = NeoTextSecondary.copy(alpha = 0.5f)) },
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
                        ),
                        maxLines = 3
                    )
                }

                Column {
                    Text("DUE DATE", style = Typography.labelSmall.copy(color = Color.Black, fontWeight = FontWeight.Black))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box {
                        OutlinedTextField(
                            value = dueDate ?: "No due date",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, Color.Black, RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = {
                                if (dueDate != null) {
                                    IconButton(onClick = { dueDate = null }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear date", tint = NeoRed)
                                    }
                                } else {
                                    Icon(Icons.Default.CalendarToday, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = Color.Black,
                                unfocusedBorderColor = Color.Black,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
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
                    Text("QUADRANT", style = Typography.labelSmall.copy(color = Color.Black, fontWeight = FontWeight.Black))
                    Spacer(modifier = Modifier.height(10.dp))
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) color else Color.White)
                                    .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
                                    .clickable { selectedQuadrant = q }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (selected) Color.White else color)
                                            .border(1.dp, Color.Black, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label.split(" ")[0],
                                        style = Typography.labelSmall.copy(
                                            color = if (selected) (if (color == NeoRed) Color.White else Color.Black) else Color.Black,
                                            fontWeight = FontWeight.Black,
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
            NeoButton(
                onClick = { if (title.isNotBlank()) onConfirm(title, desc, selectedQuadrant, dueDate) },
                containerColor = NeoYellow,
                contentColor = Color.Black,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (task == null) "Create Task" else "Save Changes", fontWeight = FontWeight.Black, color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = NeoTextSecondary, fontWeight = FontWeight.Bold)
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
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = NeoRed, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = NeoTextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        NeoButton(onClick = onRetry, containerColor = NeoYellow) {
            Text("Retry", color = Color.Black, fontWeight = FontWeight.Black)
        }
    }
}

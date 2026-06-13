package com.reihan.dashboard.ui.journal

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reihan.dashboard.data.model.JournalResponse
import com.reihan.dashboard.ui.theme.*

fun moodEmoji(mood: Int): String = when (mood) {
    1 -> "😢"
    2 -> "😔"
    3 -> "😐"
    4 -> "😊"
    5 -> "🤩"
    else -> "😐"
}

fun moodLabel(mood: Int): String = when (mood) {
    1 -> "Awful"
    2 -> "Bad"
    3 -> "Neutral"
    4 -> "Good"
    5 -> "Amazing"
    else -> "Neutral"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showWriteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadJournals()
    }

    Scaffold(
        containerColor = DarkBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showWriteDialog = true },
                containerColor = AccentJournal,
                contentColor = DarkBg,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 96.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Write")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // --- TOP BAR ---
            JournalTopBar(onBack = onBack)

            when (val s = state) {
                is JournalState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentJournal, strokeWidth = 2.dp)
                    }
                }
                is JournalState.Error -> {
                    JournalErrorView(s.message) { viewModel.loadJournals() }
                }
                is JournalState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Today's Entry Highlight
                        item {
                            TodayJournalHeader(
                                journal = s.todayJournal,
                                onWrite = { showWriteDialog = true }
                            )
                        }

                        // Past Entries List
                        val pastEntries = if (s.todayJournal != null) {
                            s.journals.filter { it.id != s.todayJournal.id }
                        } else s.journals

                        if (pastEntries.isNotEmpty()) {
                            item {
                                Text(
                                    "Past Reflections",
                                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            items(pastEntries) { journal ->
                                ModernJournalRow(
                                    journal = journal,
                                    onDelete = { viewModel.deleteJournal(journal.id) }
                                )
                            }
                        } else if (s.todayJournal == null) {
                            item {
                                EmptyJournalView()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWriteDialog) {
        val currentState = state
        val existingMood = if (currentState is JournalState.Success) currentState.todayJournal?.mood ?: 3 else 3
        val existingContent = if (currentState is JournalState.Success) currentState.todayJournal?.content ?: "" else ""
        
        JournalFormDialog(
            initialMood = existingMood,
            initialContent = existingContent,
            onDismiss = { showWriteDialog = false },
            onSave = { mood, content ->
                viewModel.saveJournal(mood, content) { showWriteDialog = false }
            }
        )
    }
}

@Composable
fun JournalTopBar(onBack: () -> Unit) {
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
            Text("REFLECT", style = Typography.labelSmall.copy(color = AccentJournal, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
            Text("Mood Diary", style = Typography.titleLarge.copy(fontWeight = FontWeight.Black))
        }
    }
}

@Composable
fun TodayJournalHeader(journal: JournalResponse?, onWrite: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                if (journal != null) Brush.linearGradient(listOf(AccentJournal, AccentJournal.copy(alpha = 0.7f)))
                else Brush.linearGradient(listOf(DarkSurface, DarkSurfaceGlass))
            )
            .border(1.dp, if (journal != null) Color.Transparent else BorderGlass, RoundedCornerShape(32.dp))
            .clickable(enabled = journal == null) { onWrite() }
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Mood",
                    style = Typography.labelLarge.copy(
                        color = if (journal != null) DarkBg.copy(alpha = 0.6f) else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (journal != null) {
                    Text(moodEmoji(journal.mood), fontSize = 32.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (journal != null) {
                Text(
                    text = moodLabel(journal.mood),
                    style = Typography.headlineSmall.copy(color = DarkBg, fontWeight = FontWeight.Black)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = journal.content,
                    style = Typography.bodyMedium.copy(color = DarkBg.copy(alpha = 0.7f), lineHeight = 22.sp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = AccentJournal, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "How are you feeling today?",
                        style = Typography.bodyLarge.copy(color = TextSecondary, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernJournalRow(journal: JournalResponse, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurface)
            .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AccentJournal.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(moodEmoji(journal.mood), fontSize = 24.sp)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = moodLabel(journal.mood),
                    style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• ${journal.entryDate.take(10)}",
                    style = Typography.labelSmall.copy(color = TextSecondary)
                )
            }
            Text(
                text = journal.content,
                style = Typography.bodySmall.copy(color = TextSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalFormDialog(
    initialMood: Int,
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (mood: Int, content: String) -> Unit
) {
    var mood by remember { mutableIntStateOf(initialMood) }
    var content by remember { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.border(1.dp, BorderGlass, RoundedCornerShape(28.dp)),
        title = {
            Text("Reflect Today", style = Typography.headlineSmall.copy(fontWeight = FontWeight.Black))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // Mood Selection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (1..5).forEach { m ->
                        val selected = mood == m
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (selected) AccentJournal else DarkBg)
                                .border(1.dp, if (selected) AccentJournal else BorderGlass, CircleShape)
                                .clickable { mood = m },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(moodEmoji(m), fontSize = if (selected) 24.sp else 20.sp)
                        }
                    }
                }
                
                Text(
                    text = "Currently feeling: ${moodLabel(mood)}",
                    style = Typography.labelLarge.copy(color = AccentJournal, fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("What's on your mind?", color = TextSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    minLines = 4,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentJournal,
                        unfocusedBorderColor = BorderGlass,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (content.isNotBlank()) onSave(mood, content) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentJournal, contentColor = DarkBg)
            ) {
                Text("Save Entry", fontWeight = FontWeight.Black)
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
fun EmptyJournalView() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = TextSecondary.copy(alpha = 0.1f), modifier = Modifier.size(100.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("No reflections yet", style = Typography.bodyLarge.copy(color = TextSecondary.copy(alpha = 0.5f)))
    }
}

@Composable
fun JournalErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AccentJournal)) {
            Text("Retry", color = DarkBg)
        }
    }
}

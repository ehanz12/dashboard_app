package com.reihan.dashboard.ui.transaction

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reihan.dashboard.data.model.CategoryResponse
import com.reihan.dashboard.data.model.TransactionResponse
import com.reihan.dashboard.ui.components.DeleteConfirmationDialog
import com.reihan.dashboard.ui.components.NeoBadge
import com.reihan.dashboard.ui.components.NeoButton
import com.reihan.dashboard.ui.components.NeoCard
import com.reihan.dashboard.ui.components.StatusPopup
import com.reihan.dashboard.ui.components.VoiceTransactionDialog
import com.reihan.dashboard.ui.components.dotGridBackground
import com.reihan.dashboard.ui.home.formatCurrency
import com.reihan.dashboard.ui.theme.*
import com.reihan.dashboard.util.ParsedVoiceTransaction
import com.reihan.dashboard.util.VoiceTransactionParser
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    var selectedType by remember { mutableStateOf("All") }
    var selectedCategoryId by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionResponse?>(null) }
    var statusPopup by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }
    var parsedVoiceTx by remember { mutableStateOf<ParsedVoiceTransaction?>(null) }

    val context = LocalContext.current

    // Speech Recognizer Activity Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()
            if (!spokenText.isNullOrBlank() && state is TransactionState.Success) {
                val categories = (state as TransactionState.Success).categories
                val parsed = VoiceTransactionParser.parse(spokenText, categories)
                parsedVoiceTx = parsed
            }
        }
    }

    // Audio Permission Launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara transaksi Anda (contoh: 'jajan 10000 kategori jajan harian')")
            }
            speechLauncher.launch(intent)
        } else {
            statusPopup = false to "Izin mikrofon dibutuhkan untuk menginput transaksi lewat suara."
        }
    }

    fun startVoiceInput() {
        audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(selectedType, selectedCategoryId, searchQuery, startDate, endDate) {
        viewModel.loadTransactionsAndCategories(
            type = selectedType,
            categoryId = selectedCategoryId,
            search = searchQuery,
            startDate = startDate,
            endDate = endDate
        )
    }

    Scaffold(
        containerColor = NeoBg,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 96.dp, end = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = { startVoiceInput() },
                    containerColor = NeoCyan,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(2.5.dp, Color.Black, RoundedCornerShape(12.dp)),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = Color.Black)
                }
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = NeoYellow,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(2.5.dp, Color.Black, RoundedCornerShape(12.dp)),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black) },
                    text = { Text("New Record", fontWeight = FontWeight.Black, color = Color.Black) }
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
            TransactionTopBar(onBack = onBack, onVoiceClick = { startVoiceInput() })

            TransactionFilters(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                startDate = startDate,
                onStartDateChange = { startDate = it },
                endDate = endDate,
                onEndDateChange = { endDate = it },
                selectedType = selectedType,
                onTypeChange = { selectedType = it }
            )

            if (state is TransactionState.Success) {
                CategoryScrollRow(
                    categories = (state as TransactionState.Success).categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelect = { selectedCategoryId = it }
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (val current = state) {
                    is TransactionState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.Black, strokeWidth = 3.dp)
                        }
                    }
                    is TransactionState.Error -> {
                        TransactionErrorView(current.message) { viewModel.loadTransactionsAndCategories() }
                    }
                    is TransactionState.Success -> {
                        if (current.transactions.isEmpty()) {
                            EmptyTransactionsView()
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(current.transactions) { transaction ->
                                    ModernTransactionItem(
                                        transaction = transaction,
                                        onClick = { editingTransaction = transaction },
                                        onDelete = {
                                            deleteConfirmId = transaction.id
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (parsedVoiceTx != null && state is TransactionState.Success) {
        VoiceTransactionDialog(
            parsed = parsedVoiceTx!!,
            categories = (state as TransactionState.Success).categories,
            onDismiss = { parsedVoiceTx = null },
            onConfirm = { catId, amt, type, desc, date ->
                viewModel.addTransaction(
                    categoryId = catId,
                    amount = amt,
                    type = type,
                    description = desc,
                    transactionDate = date,
                    onSuccess = {
                        parsedVoiceTx = null
                        statusPopup = true to "Transaksi suara berhasil disimpan!"
                    },
                    onFailure = { err ->
                        statusPopup = false to err
                    }
                )
            }
        )
    }

    if (showAddDialog && state is TransactionState.Success) {
        TransactionFormDialog(
            title = "New Transaction",
            categories = (state as TransactionState.Success).categories,
            onDismiss = { showAddDialog = false },
            onConfirm = { catId, amt, type, desc, date ->
                viewModel.addTransaction(
                    categoryId = catId,
                    amount = amt,
                    type = type,
                    description = desc,
                    transactionDate = date,
                    onSuccess = { 
                        showAddDialog = false
                        statusPopup = true to "Transaction added successfully!"
                    },
                    onFailure = { err ->
                        statusPopup = false to err
                    }
                )
            }
        )
    }

    if (editingTransaction != null && state is TransactionState.Success) {
        TransactionFormDialog(
            title = "Edit Transaction",
            transaction = editingTransaction,
            categories = (state as TransactionState.Success).categories,
            onDismiss = { editingTransaction = null },
            onConfirm = { catId, amt, type, desc, date ->
                viewModel.editTransaction(
                    id = editingTransaction!!.id,
                    categoryId = catId,
                    amount = amt,
                    type = type,
                    description = desc,
                    transactionDate = date,
                    onSuccess = { 
                        editingTransaction = null
                        statusPopup = true to "Transaction updated successfully!"
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
            title = "Hapus Transaksi?",
            message = "Apakah Anda yakin ingin menghapus transaksi ini?",
            onConfirm = {
                viewModel.removeTransaction(id) { err ->
                    statusPopup = false to err
                }
                deleteConfirmId = null
            },
            onDismiss = { deleteConfirmId = null }
        )
    }
}

@Composable
fun TransactionTopBar(onBack: () -> Unit, onVoiceClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clickable { onVoiceClick() }
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
                        .background(NeoCyan, RoundedCornerShape(10.dp))
                        .border(2.dp, Color.Black, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = Color.Black, modifier = Modifier.size(20.dp))
                }
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            NeoBadge(text = "FINANCE", containerColor = NeoYellow, contentColor = Color.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Transactions", style = Typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Color.Black))
        }
    }
}

@Composable
fun TransactionFilters(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    startDate: String,
    onStartDateChange: (String) -> Unit,
    endDate: String,
    onEndDateChange: (String) -> Unit,
    selectedType: String,
    onTypeChange: (String) -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by description...", color = NeoTextSecondary.copy(alpha = 0.5f), fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Black,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1.5f)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
                    .padding(3.dp)
            ) {
                listOf("All", "Income", "Expense").forEach { type ->
                    val active = selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (active) NeoYellow else Color.White, RoundedCornerShape(8.dp))
                            .border(if (active) 1.5.dp else 0.dp, Color.Black, RoundedCornerShape(8.dp))
                            .clickable { onTypeChange(type) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (type == "All") "All" else if (type == "Income") "In" else "Out",
                            style = Typography.labelSmall.copy(
                                color = Color.Black,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
                    .clickable {
                        val calendar = Calendar.getInstance()
                        if (startDate.isNotEmpty()) {
                            val parts = startDate.split("-")
                            if (parts.size == 3) {
                                try {
                                    calendar.set(Calendar.YEAR, parts[0].toInt())
                                    calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                                    calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                                } catch (e: Exception) {}
                            }
                        }
                        DatePickerDialog(context, { _, y, m, d ->
                            onStartDateChange(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d))
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = "Start Date", tint = Color.Black, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (startDate.isEmpty()) "Date" else startDate.takeLast(5),
                    style = Typography.labelSmall.copy(color = Color.Black, fontWeight = FontWeight.Black)
                )
            }
            
            if (startDate.isNotEmpty() || endDate.isNotEmpty() || selectedType != "All") {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(NeoRed, RoundedCornerShape(10.dp))
                        .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
                        .clickable { onStartDateChange(""); onEndDateChange(""); onTypeChange("All") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FilterAltOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun CategoryScrollRow(
    categories: List<CategoryResponse>,
    selectedCategoryId: String,
    onCategorySelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            CategoryChip(name = "All", isSelected = selectedCategoryId == "All", onClick = { onCategorySelect("All") })
        }
        items(categories) { cat ->
            CategoryChip(name = cat.name, isSelected = selectedCategoryId == cat.id, onClick = { onCategorySelect(cat.id) })
        }
    }
}

@Composable
fun CategoryChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (isSelected) NeoCyan else Color.White, RoundedCornerShape(8.dp))
            .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = Typography.labelSmall.copy(
                color = Color.Black,
                fontWeight = FontWeight.Black
            )
        )
    }
}

@Composable
fun ModernTransactionItem(
    transaction: TransactionResponse,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = transaction.type.lowercase() == "income"
    
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color.White,
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
                    .size(40.dp)
                    .background(if (isIncome) NeoGreen else NeoRed, CircleShape)
                    .border(2.dp, Color.Black, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (isIncome) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifEmpty { "Transaction" },
                    style = Typography.bodyLarge.copy(fontWeight = FontWeight.Black, color = Color.Black),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = transaction.category?.name ?: "No Category",
                    style = Typography.bodySmall.copy(color = NeoTextSecondary, fontWeight = FontWeight.Bold)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncome) "+" else "-"}${formatCurrency(transaction.amount)}",
                    style = Typography.bodyLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isIncome) Color(0xFF16A34A) else NeoRed
                    )
                )
                Text(transaction.transactionDate, style = Typography.bodySmall.copy(color = NeoTextSecondary, fontSize = 10.sp))
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = NeoRed, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun EmptyTransactionsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        NeoCard(
            modifier = Modifier.padding(32.dp),
            backgroundColor = Color.White,
            borderColor = Color.Black,
            shadowColor = Color.Black,
            shadowOffset = 4.dp,
            cornerRadius = 12.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = Color.Black, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No Transactions Found", style = Typography.titleMedium.copy(fontWeight = FontWeight.Black, color = Color.Black))
                Text("Try adjusting filters or tap + to add a transaction.", style = Typography.bodySmall.copy(color = NeoTextSecondary), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun TransactionErrorView(message: String, onRetry: () -> Unit) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormDialog(
    title: String,
    transaction: TransactionResponse? = null,
    categories: List<CategoryResponse>,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: String?, amount: Double, type: String, description: String, date: String) -> Unit
) {
    var amountText by remember { mutableStateOf(transaction?.amount?.toInt()?.toString() ?: "") }
    var description by remember { mutableStateOf(transaction?.description ?: "") }
    var type by remember { mutableStateOf(transaction?.type ?: "expense") }
    var selectedCategoryId by remember { mutableStateOf(transaction?.category?.id ?: categories.firstOrNull()?.id) }
    var date by remember { mutableStateOf(transaction?.transactionDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("income" to "Income", "expense" to "Expense").forEach { (t, label) ->
                        val selected = type.equals(t, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (selected) (if (t == "income") NeoGreen else NeoRed) else Color.White, RoundedCornerShape(10.dp))
                                .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
                                .clickable { type = t }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, style = Typography.labelLarge.copy(color = if (selected && t == "expense") Color.White else Color.Black, fontWeight = FontWeight.Black))
                        }
                    }
                }

                Column {
                    Text("AMOUNT (RP)", style = Typography.labelSmall.copy(color = Color.Black, fontWeight = FontWeight.Black))
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        placeholder = { Text("0", color = NeoTextSecondary.copy(alpha = 0.5f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("What was this for?", color = NeoTextSecondary.copy(alpha = 0.5f)) },
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
            }
        },
        confirmButton = {
            NeoButton(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(selectedCategoryId, amt, type, description, date)
                    }
                },
                containerColor = NeoYellow,
                contentColor = Color.Black,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Transaction", fontWeight = FontWeight.Black, color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = NeoTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

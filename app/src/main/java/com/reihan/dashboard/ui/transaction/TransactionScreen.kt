package com.reihan.dashboard.ui.transaction

import android.app.DatePickerDialog
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
import com.reihan.dashboard.ui.components.StatusPopup
import com.reihan.dashboard.ui.home.formatCurrency
import com.reihan.dashboard.ui.theme.*
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
        containerColor = DarkBg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentBlue,
                contentColor = DarkBg,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 96.dp, end = 16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 12.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Record", fontWeight = FontWeight.Black) }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            TransactionTopBar(onBack = onBack)

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
                            CircularProgressIndicator(color = AccentBlue, strokeWidth = 2.dp)
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
                                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 100.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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
fun TransactionTopBar(onBack: () -> Unit) {
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
            Text("FINANCE", style = Typography.labelSmall.copy(color = AccentBlue, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
            Text("Transactions", style = Typography.titleLarge.copy(fontWeight = FontWeight.Black))
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

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by description...", color = TextSecondary.copy(alpha = 0.5f), fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = BorderGlass,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1.5f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                listOf("All", "Income", "Expense").forEach { type ->
                    val active = selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) AccentBlue else Color.Transparent)
                            .clickable { onTypeChange(type) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (type == "All") "All" else if (type == "Income") "In" else "Out",
                            style = Typography.labelSmall.copy(
                                color = if (active) DarkBg else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
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
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = "Start Date", tint = if (startDate.isNotEmpty()) AccentBlue else TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (startDate.isNotEmpty()) startDate.takeLast(5) else "Start",
                    style = Typography.labelSmall.copy(
                        color = if (startDate.isNotEmpty()) AccentBlue else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
                    .clickable {
                        val calendar = Calendar.getInstance()
                        if (endDate.isNotEmpty()) {
                            val parts = endDate.split("-")
                            if (parts.size == 3) {
                                try {
                                    calendar.set(Calendar.YEAR, parts[0].toInt())
                                    calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                                    calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                                } catch (e: Exception) {}
                            }
                        }
                        DatePickerDialog(context, { _, y, m, d ->
                            onEndDateChange(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d))
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = "End Date", tint = if (endDate.isNotEmpty()) AccentBlue else TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (endDate.isNotEmpty()) endDate.takeLast(5) else "End",
                    style = Typography.labelSmall.copy(
                        color = if (endDate.isNotEmpty()) AccentBlue else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            if (startDate.isNotEmpty() || endDate.isNotEmpty() || selectedType != "All") {
                IconButton(
                    onClick = { onStartDateChange(""); onEndDateChange(""); onTypeChange("All") },
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(ExpenseRed.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.FilterAltOff, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
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
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) AccentBlue.copy(alpha = 0.1f) else DarkSurface)
            .border(1.dp, if (isSelected) AccentBlue else BorderGlass, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = Typography.labelSmall.copy(
                color = if (isSelected) AccentBlue else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isIncome) IncomeGreen.copy(alpha = 0.1f) else ExpenseRed.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isIncome) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                contentDescription = null,
                tint = if (isIncome) IncomeGreen else ExpenseRed,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.description.ifEmpty { "Transaction" },
                style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = transaction.category?.name ?: "No Category",
                style = Typography.bodySmall.copy(color = TextSecondary)
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isIncome) "+" else "-"}${formatCurrency(transaction.amount)}",
                style = Typography.bodyLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = if (isIncome) IncomeGreen else ExpenseRed
                )
            )
            Text(transaction.transactionDate, style = Typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp))
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
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
    onConfirm: (categoryId: String, amount: Double, type: String, description: String, date: String) -> Unit
) {
    var amount by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(transaction?.description ?: "") }
    var selectedType by remember { mutableStateOf(transaction?.type?.replaceFirstChar { it.uppercase() } ?: "Expense") }
    var selectedCategoryId by remember { mutableStateOf(transaction?.category?.id ?: "") }
    var date by remember { mutableStateOf(transaction?.transactionDate?.take(10) ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var isExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.border(1.dp, BorderGlass, RoundedCornerShape(28.dp)),
        title = {
            Text(title, style = Typography.headlineSmall.copy(fontWeight = FontWeight.Black))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkBg)
                        .padding(4.dp)
                ) {
                    listOf("Income", "Expense").forEach { type ->
                        val active = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) (if (type == "Income") IncomeGreen else ExpenseRed) else Color.Transparent)
                                .clickable { selectedType = type }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = type, color = if (active) DarkBg else TextSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    placeholder = { Text("0.00", color = TextSecondary.copy(alpha = 0.5f)) },
                    prefix = { Text("Rp ", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("What was it for?", color = TextSecondary.copy(alpha = 0.5f)) },
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

                Box {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCategoryId }?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BorderGlass,
                            unfocusedBorderColor = BorderGlass,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        )
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { isExpanded = true })
                    
                    DropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.7f).background(DarkSurface).border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name, color = TextPrimary) },
                                onClick = { selectedCategoryId = cat.id; isExpanded = false }
                            )
                        }
                    }
                }

                val context = LocalContext.current
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Select Date", color = TextSecondary.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = { Icon(Icons.Default.CalendarToday, null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentBlue,
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
                                val parts = date.split("-")
                                if (parts.size == 3) {
                                    try {
                                        calendar.set(Calendar.YEAR, parts[0].toInt())
                                        calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                                        calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                                    } catch (e: Exception) {}
                                }
                                DatePickerDialog(context, { _, y, m, d ->
                                    date = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                            }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val amtVal = amount.toDoubleOrNull() ?: 0.0
                    onConfirm(selectedCategoryId, amtVal, selectedType, description, date)
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = DarkBg)
            ) {
                Text(if (transaction == null) "Add Transaction" else "Save Changes", fontWeight = FontWeight.Black)
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
fun EmptyTransactionsView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = TextSecondary.copy(alpha = 0.1f), modifier = Modifier.size(100.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Your financial history is empty", style = Typography.titleMedium.copy(color = TextSecondary.copy(alpha = 0.5f)))
    }
}

@Composable
fun TransactionErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) {
            Text("Retry", color = DarkBg)
        }
    }
}

package com.reihan.dashboard.ui.transaction

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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.reihan.dashboard.ui.theme.*
import com.reihan.dashboard.data.model.CategoryResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var newCategoryName by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<CategoryResponse?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadTransactionsAndCategories()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOOLBAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .border(1.dp, BorderGlass, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Categories",
                    style = Typography.headlineMedium.copy(fontWeight = FontWeight.Black, color = TextPrimary)
                )
            }

            // 2. CREATE CATEGORY FIELD (Titanium Card Panel)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "CREATE NEW CATEGORY",
                    style = Typography.labelLarge.copy(color = AccentBlue, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 10.sp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("e.g. Shopping", color = TextSecondary.copy(alpha = 0.5f), fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentBlue,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderGlass,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                viewModel.addCategory(
                                    name = newCategoryName,
                                    onSuccess = { newCategoryName = "" },
                                    onFailure = { err ->
                                        android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = DarkBg),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("ADD", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                    }
                }
            }

            // 3. CATEGORIES LIST
            Text(
                text = "YOUR CATEGORIES",
                style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = TextSecondary, fontSize = 10.sp),
                modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                when (val current = state) {
                    is TransactionState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AccentBlue)
                        }
                    }
                    is TransactionState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(current.message, color = ExpenseRed, style = Typography.bodyMedium)
                        }
                    }
                    is TransactionState.Success -> {
                        if (current.categories.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No categories created yet", color = TextSecondary, style = Typography.bodyMedium)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(current.categories) { cat ->
                                    CategoryItemRow(
                                        name = cat.name,
                                        onClick = { editingCategory = cat },
                                        onDelete = {
                                            viewModel.removeCategory(
                                                id = cat.id,
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

        // EDIT CATEGORY DIALOG
        if (editingCategory != null) {
            val cat = editingCategory!!
            var editName by remember { mutableStateOf(cat.name) }
            AlertDialog(
                onDismissRequest = { editingCategory = null },
                title = { Text("Edit Category", color = TextPrimary) },
                text = {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        placeholder = { Text("Category Name", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentBlue,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = BorderGlass,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editName.isNotBlank()) {
                                viewModel.editCategory(
                                    id = cat.id,
                                    name = editName.trim(),
                                    onSuccess = { editingCategory = null },
                                    onFailure = { err ->
                                        android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = DarkBg)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingCategory = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = DarkSurface,
                modifier = Modifier.border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
            )
        }
    }
}

@Composable
fun CategoryItemRow(
    name: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, BorderGlass, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Category, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = name,
                style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(BorderGlass)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
        }
    }
}

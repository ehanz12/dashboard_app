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
import com.reihan.dashboard.ui.theme.*
import com.reihan.dashboard.data.model.CategoryResponse
import com.reihan.dashboard.ui.components.DeleteConfirmationDialog
import com.reihan.dashboard.ui.components.NeoBadge
import com.reihan.dashboard.ui.components.NeoButton
import com.reihan.dashboard.ui.components.NeoCard
import com.reihan.dashboard.ui.components.StatusPopup
import com.reihan.dashboard.ui.components.dotGridBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var newCategoryName by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<CategoryResponse?>(null) }
    var statusPopup by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadTransactionsAndCategories()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .dotGridBackground()
            .background(NeoBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOOLBAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
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
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Categories",
                    style = Typography.headlineMedium.copy(fontWeight = FontWeight.Black, color = Color.Black)
                )
            }

            // 2. CREATE CATEGORY FIELD
            NeoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                backgroundColor = Color.White,
                borderColor = Color.Black,
                shadowColor = Color.Black,
                shadowOffset = 4.dp,
                cornerRadius = 12.dp
            ) {
                NeoBadge(text = "CREATE NEW CATEGORY", containerColor = NeoCyan, contentColor = Color.Black)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("e.g. Shopping", color = NeoTextSecondary.copy(alpha = 0.5f), fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            cursorColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(2.dp, Color.Black, RoundedCornerShape(10.dp)),
                        singleLine = true
                    )
                    NeoButton(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                viewModel.addCategory(
                                    name = newCategoryName,
                                    onSuccess = { 
                                        newCategoryName = ""
                                        statusPopup = true to "Category added successfully!"
                                    },
                                    onFailure = { err ->
                                        statusPopup = false to err
                                    }
                                )
                            }
                        },
                        containerColor = NeoYellow,
                        contentColor = Color.Black,
                        shadowOffset = 3.dp,
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("ADD", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Black)
                    }
                }
            }

            // 3. CATEGORIES LIST
            Text(
                text = "YOUR CATEGORIES",
                style = Typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = Color.Black),
                modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                when (val current = state) {
                    is TransactionState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.Black)
                        }
                    }
                    is TransactionState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(current.message, color = NeoRed, style = Typography.bodyMedium)
                        }
                    }
                    is TransactionState.Success -> {
                        if (current.categories.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No categories created yet", color = NeoTextSecondary, style = Typography.bodyMedium)
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
                                            deleteConfirmId = cat.id
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Edit Dialog
        if (editingCategory != null) {
            var catName by remember { mutableStateOf(editingCategory!!.name) }
            AlertDialog(
                onDismissRequest = { editingCategory = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(2.5.dp, Color.Black, RoundedCornerShape(16.dp)),
                title = { Text("Edit Category", style = Typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Color.Black)) },
                text = {
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
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
                },
                confirmButton = {
                    NeoButton(
                        onClick = {
                            if (catName.isNotBlank()) {
                                viewModel.editCategory(
                                    id = editingCategory!!.id,
                                    name = catName,
                                    onSuccess = {
                                        editingCategory = null
                                        statusPopup = true to "Category updated successfully!"
                                    },
                                    onFailure = { err ->
                                        statusPopup = false to err
                                    }
                                )
                            }
                        },
                        containerColor = NeoYellow
                    ) {
                        Text("Save", fontWeight = FontWeight.Black, color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingCategory = null }) {
                        Text("Cancel", color = NeoTextSecondary, fontWeight = FontWeight.Bold)
                    }
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
                title = "Hapus Kategori?",
                message = "Apakah Anda yakin ingin menghapus kategori ini?",
                onConfirm = {
                    viewModel.removeCategory(id) { err ->
                        statusPopup = false to err
                    }
                    deleteConfirmId = null
                },
                onDismiss = { deleteConfirmId = null }
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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(NeoCyan, RoundedCornerShape(8.dp))
                        .border(1.5.dp, Color.Black, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = name,
                    style = Typography.bodyLarge.copy(fontWeight = FontWeight.Black, color = Color.Black)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeoRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}

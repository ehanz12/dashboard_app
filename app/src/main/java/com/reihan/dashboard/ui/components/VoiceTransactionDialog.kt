package com.reihan.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reihan.dashboard.data.model.CategoryResponse
import com.reihan.dashboard.ui.home.formatCurrency
import com.reihan.dashboard.ui.theme.*
import com.reihan.dashboard.util.ParsedVoiceTransaction
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTransactionDialog(
    parsed: ParsedVoiceTransaction,
    categories: List<CategoryResponse>,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: String?, amount: Double, type: String, description: String, date: String) -> Unit
) {
    var amountText by remember { mutableStateOf(parsed.amount.toInt().toString()) }
    var description by remember { mutableStateOf(parsed.description) }
    var type by remember { mutableStateOf(parsed.type) }
    var selectedCategoryId by remember { mutableStateOf(parsed.categoryId ?: categories.firstOrNull()?.id) }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(2.5.dp, Color.Black, RoundedCornerShape(16.dp)),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(NeoCyan, RoundedCornerShape(8.dp))
                        .border(1.5.dp, Color.Black, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Voice Transaction", style = Typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Color.Black))
                    Text("Terdeteksi otomatis dari ucapan", style = Typography.labelSmall.copy(color = NeoTextSecondary))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Raw Speech Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoYellow.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color.Black, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "💬 \"${parsed.rawSpeechText}\"",
                        style = Typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Black)
                    )
                }

                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("income" to "Pemasukan", "expense" to "Pengeluaran").forEach { (t, label) ->
                        val selected = type.equals(t, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (selected) (if (t == "income") NeoGreen else NeoRed) else Color.White, RoundedCornerShape(10.dp))
                                .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
                                .clickable { type = t }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, style = Typography.labelLarge.copy(color = if (selected && t == "expense") Color.White else Color.Black, fontWeight = FontWeight.Black))
                        }
                    }
                }

                Column {
                    Text("NOMINAL (RP)", style = Typography.labelSmall.copy(color = Color.Black, fontWeight = FontWeight.Black))
                    Spacer(modifier = Modifier.height(4.dp))
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
                    Text("KATEGORI", style = Typography.labelSmall.copy(color = Color.Black, fontWeight = FontWeight.Black))
                    Spacer(modifier = Modifier.height(4.dp))
                    if (categories.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            categories.chunked(3).forEach { rowCats ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowCats.forEach { cat ->
                                        val isSelected = selectedCategoryId == cat.id
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (isSelected) NeoYellow else Color.White, RoundedCornerShape(8.dp))
                                                .border(1.5.dp, Color.Black, RoundedCornerShape(8.dp))
                                                .clickable { selectedCategoryId = cat.id }
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                cat.name,
                                                style = Typography.labelSmall.copy(
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Column {
                    Text("DESKRIPSI", style = Typography.labelSmall.copy(color = Color.Black, fontWeight = FontWeight.Black))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
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
                Text("Simpan Transaksi", fontWeight = FontWeight.Black, color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Batal", color = NeoTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

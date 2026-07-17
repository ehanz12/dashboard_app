package com.reihan.dashboard.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reihan.dashboard.ui.theme.*

@Composable
fun DeleteConfirmationDialog(
    title: String = "Hapus Data?",
    message: String = "Apakah Anda yakin ingin menghapus data ini? Tindakan ini tidak dapat dibatalkan.",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.border(1.dp, BorderGlass, RoundedCornerShape(28.dp)),
        icon = {
            Icon(
                Icons.Default.DeleteForever,
                contentDescription = null,
                tint = ExpenseRed,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = Typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = message,
                style = Typography.bodyMedium.copy(color = TextSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed, contentColor = DarkBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Ya, Hapus", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Batal", color = TextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

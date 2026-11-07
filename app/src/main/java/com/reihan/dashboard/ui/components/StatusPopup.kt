package com.reihan.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.reihan.dashboard.ui.theme.*

@Composable
fun StatusPopup(
    isSuccess: Boolean,
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        NeoCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            backgroundColor = NeoCardSurface,
            borderColor = Color.Black,
            shadowColor = Color.Black,
            shadowOffset = 6.dp,
            cornerRadius = 14.dp,
            contentPadding = 24.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isSuccess) NeoGreen else NeoRed,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isSuccess) "Berhasil!" else "Gagal",
                    style = Typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = Typography.bodyMedium.copy(color = NeoTextSecondary),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                NeoButton(
                    onClick = onDismiss,
                    containerColor = if (isSuccess) NeoGreen else NeoRed,
                    contentColor = if (isSuccess) Color.Black else Color.White,
                    shadowOffset = 3.dp,
                    cornerRadius = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "OK",
                        style = Typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = if (isSuccess) Color.Black else Color.White
                        )
                    )
                }
            }
        }
    }
}

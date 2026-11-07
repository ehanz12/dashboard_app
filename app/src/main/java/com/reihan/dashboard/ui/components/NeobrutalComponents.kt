package com.reihan.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reihan.dashboard.ui.theme.*

/**
 * Custom modifier that draws a Neobrutalist dotted grid background texture.
 */
fun Modifier.dotGridBackground(
    dotColor: Color = Color.Black.copy(alpha = 0.12f),
    dotRadius: Dp = 1.5.dp,
    spacing: Dp = 20.dp
): Modifier = this.drawBehind {
    val radiusPx = dotRadius.toPx()
    val spacingPx = spacing.toPx()
    val cols = (size.width / spacingPx).toInt() + 1
    val rows = (size.height / spacingPx).toInt() + 1

    for (i in 0..cols) {
        for (j in 0..rows) {
            drawCircle(
                color = dotColor,
                radius = radiusPx,
                center = Offset(i * spacingPx, j * spacingPx)
            )
        }
    }
}

/**
 * Neobrutalism Card with thick black border and 3D offset shadow.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeoCardSurface,
    borderColor: Color = Color.Black,
    shadowColor: Color = Color.Black,
    borderWidth: Dp = 2.5.dp,
    shadowOffset: Dp = 4.dp,
    cornerRadius: Dp = 12.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(modifier = modifier) {
        // Hard 3D offset shadow box
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(color = shadowColor, shape = shape)
        )
        // Card Surface
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = backgroundColor, shape = shape)
                .border(width = borderWidth, color = borderColor, shape = shape)
                .then(
                    if (onClick != null) Modifier.clip(shape).clickable { onClick() }
                    else Modifier
                )
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Neobrutalism Button with bold typography, thick black border, and 3D offset shadow.
 */
@Composable
fun NeoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = NeoYellow,
    contentColor: Color = Color.Black,
    borderColor: Color = Color.Black,
    shadowColor: Color = Color.Black,
    borderWidth: Dp = 2.5.dp,
    shadowOffset: Dp = 4.dp,
    cornerRadius: Dp = 12.dp,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier.then(
            if (enabled) Modifier.clickable { onClick() } else Modifier
        )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(color = shadowColor, shape = shape)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (enabled) containerColor else containerColor.copy(alpha = 0.5f),
                    shape = shape
                )
                .border(width = borderWidth, color = borderColor, shape = shape)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Neobrutalism Badge with high contrast solid fill and bold border.
 */
@Composable
fun NeoBadge(
    text: String,
    containerColor: Color,
    contentColor: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(6.dp))
            .border(1.5.dp, Color.Black, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = Typography.labelSmall.copy(
                color = contentColor,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                fontSize = 10.sp
            )
        )
    }
}

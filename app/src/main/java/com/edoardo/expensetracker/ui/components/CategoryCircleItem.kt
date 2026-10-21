package com.edoardo.expensetracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.ui.theme.PrimaryMint
import com.edoardo.expensetracker.ui.theme.TextMuted
import com.edoardo.expensetracker.ui.theme.TextPrimary
import com.edoardo.expensetracker.ui.theme.TextSecondary
import java.util.Locale

/**
 * Elemento Categoria FinTech circolare (100% geometricamente circolare, invariante e non deformabile).
 * - Icona in cerchio perfetto a dimensione vincolata (requiredSize 46dp)
 * - Nome e importo sempre allineati e leggibili
 * - Modalità Modifica con accento mint e matita
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryCircleItem(
    categoryName: String,
    totalAmount: Double,
    colorHex: String,
    icon: ImageVector = Icons.Default.Category,
    isEditMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val categoryColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        PrimaryMint
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Nome categoria
        Text(
            text = categoryName,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isEditMode) PrimaryMint else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Badge Icona Circolare Geometricamente Vincolato (Cerchio Perfetto 46dp)
        Box(
            modifier = Modifier
                .requiredSize(46.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            categoryColor.copy(alpha = 0.24f),
                            categoryColor.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(
                    width = if (isEditMode) 1.8.dp else 1.2.dp,
                    brush = if (isEditMode) {
                        Brush.linearGradient(listOf(PrimaryMint, PrimaryMint))
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                categoryColor.copy(alpha = 0.50f),
                                categoryColor.copy(alpha = 0.20f)
                            )
                        )
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isEditMode) Icons.Default.Edit else icon,
                contentDescription = categoryName,
                tint = if (isEditMode) PrimaryMint else categoryColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Importo sempre pulito
        Text(
            text = if (totalAmount > 0) "€ ${String.format(Locale.ITALY, "%.0f", totalAmount)}" else "€ 0",
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontWeight = if (totalAmount > 0) FontWeight.Bold else FontWeight.Normal,
            color = if (isEditMode) TextSecondary else if (totalAmount > 0) TextPrimary else TextMuted
        )
    }
}

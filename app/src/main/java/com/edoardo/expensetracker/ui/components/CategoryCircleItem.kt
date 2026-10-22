package com.edoardo.expensetracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.edoardo.expensetracker.ui.theme.*
import java.util.Locale

/**
 * Elemento Categoria FinTech circolare (100% geometricamente circolare, invariante e non deformabile).
 * - Se ha un budget:
 *   - Il badge circolare diventa una barra di progresso che si riempie dal basso verso l'alto
 *   - L'importo mostra sia il totale speso che il tetto massimo del budget (€ speso / budget)
 * - Se non ha budget:
 *   - Mostra il cerchio standard con sfumatura e importo speso
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryCircleItem(
    categoryName: String,
    totalAmount: Double,
    colorHex: String,
    icon: ImageVector = Icons.Default.Category,
    budgetLimit: Double? = null,
    hasBudget: Boolean = budgetLimit != null && budgetLimit > 0,
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

    val limit = budgetLimit ?: 0.0
    val isOverBudget = hasBudget && limit > 0 && totalAmount > limit
    val progress = if (hasBudget && limit > 0) {
        (totalAmount / limit).toFloat().coerceIn(0f, 1f)
    } else 0f

    val progressFillColor = when {
        isOverBudget -> ExpenseRed.copy(alpha = 0.85f)
        progress > 0.85f -> Color(0xFFF59E0B).copy(alpha = 0.85f)
        else -> categoryColor.copy(alpha = 0.65f)
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
        // Se ha budget, diventa una barra di progresso che si riempie dal basso verso l'alto
        val backgroundModifier = if (hasBudget) {
            Modifier.background(SurfaceDark)
        } else {
            Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        categoryColor.copy(alpha = 0.24f),
                        categoryColor.copy(alpha = 0.08f)
                    )
                )
            )
        }

        Box(
            modifier = Modifier
                .requiredSize(46.dp)
                .clip(CircleShape)
                .then(backgroundModifier)
                .border(
                    width = if (isEditMode) 1.8.dp else 1.2.dp,
                    brush = if (isEditMode) {
                        Brush.linearGradient(listOf(PrimaryMint, PrimaryMint))
                    } else if (isOverBudget) {
                        Brush.linearGradient(listOf(ExpenseRed, ExpenseRed))
                    } else if (hasBudget) {
                        Brush.verticalGradient(
                            colors = listOf(
                                categoryColor.copy(alpha = 0.65f),
                                categoryColor.copy(alpha = 0.35f)
                            )
                        )
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
            // Barra di progresso interna dal basso verso l'alto
            if (hasBudget && progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(progress)
                        .align(Alignment.BottomCenter)
                        .background(progressFillColor)
                )
            }

            Icon(
                imageVector = if (isEditMode) Icons.Default.Edit else icon,
                contentDescription = categoryName,
                tint = if (isEditMode) PrimaryMint
                       else if (hasBudget && progress > 0.45f) Color.White
                       else categoryColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Importo: se ha budget mostra "€ speso / budget", altrimenti "€ speso"
        if (hasBudget && limit > 0) {
            Text(
                text = "€ ${String.format(Locale.ITALY, "%.0f", totalAmount)} / ${String.format(Locale.ITALY, "%.0f", limit)}",
                fontSize = 9.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOverBudget) ExpenseRed else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = if (totalAmount > 0) "€ ${String.format(Locale.ITALY, "%.0f", totalAmount)}" else "€ 0",
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontWeight = if (totalAmount > 0) FontWeight.Bold else FontWeight.Normal,
                color = if (isEditMode) TextSecondary else if (totalAmount > 0) TextPrimary else TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

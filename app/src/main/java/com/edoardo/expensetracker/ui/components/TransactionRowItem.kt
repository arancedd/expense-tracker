package com.edoardo.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.data.local.dao.TransactionDetail
import com.edoardo.expensetracker.data.local.entity.TransactionType
import com.edoardo.expensetracker.ui.theme.ExpenseRed
import com.edoardo.expensetracker.ui.theme.IncomeGreen
import com.edoardo.expensetracker.ui.theme.TransferPurple
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionRowItem(
    transaction: TransactionDetail,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val isIncome = transaction.type == TransactionType.INCOME
    val isTransfer = transaction.type == TransactionType.TRANSFER

    val amountColor = when {
        isExpense -> ExpenseRed
        isIncome -> IncomeGreen
        else -> TransferPurple
    }

    val icon = when {
        isExpense -> Icons.AutoMirrored.Filled.TrendingDown
        isIncome -> Icons.AutoMirrored.Filled.TrendingUp
        else -> Icons.Default.SwapHoriz
    }

    val prefix = when {
        isExpense -> "-"
        isIncome -> "+"
        else -> "⇄"
    }

    val categoryColor = transaction.categoryColor?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null }
    } ?: amountColor

    val timeFormat = SimpleDateFormat("HH:mm", Locale.ITALY)
    val timeStr = timeFormat.format(Date(transaction.dateMillis))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = transaction.type.name,
                        tint = categoryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (isTransfer) {
                            "Giroconto: ${transaction.accountName} → ${transaction.destinationAccountName ?: "Altro conto"}"
                        } else {
                            transaction.payee?.takeIf { it.isNotBlank() }
                                ?: transaction.subcategoryName
                                ?: transaction.categoryName
                                ?: "Spesa non categorizzata"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isTransfer) timeStr else "${transaction.categoryName ?: "Varie"} • ${transaction.accountName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (transaction.source == "CARD_NOTIFICATION") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Auto",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix ${String.format(Locale.ITALY, "%.2f", transaction.amount)} €",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Text(
                    text = timeStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }
    }
}

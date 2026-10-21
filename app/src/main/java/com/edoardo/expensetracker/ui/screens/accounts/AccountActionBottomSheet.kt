package com.edoardo.expensetracker.ui.screens.accounts

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.edoardo.expensetracker.data.local.dao.AccountWithBalance
import com.edoardo.expensetracker.data.local.dao.TransactionDetail
import com.edoardo.expensetracker.data.local.entity.TransactionType
import com.edoardo.expensetracker.ui.components.OneMoneyCalculatorKeypad
import com.edoardo.expensetracker.ui.components.TransactionRowItem
import com.edoardo.expensetracker.ui.components.evaluateMathExpression
import com.edoardo.expensetracker.ui.components.getAccountIcon
import com.edoardo.expensetracker.ui.components.getCategoryIconByName
import com.edoardo.expensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AccountSheetMode {
    ACTIONS,
    ADJUST_BALANCE
}

/**
 * Bottom Sheet di azione sul Conto (FinTech Modern Aesthetic).
 * - Card conto in testata con Saldo Disponibile interattivo (stile FinTechHeaderCard neutro)
 * - Modalità Balance: Numpad/Calcolatrice stile transazioni in-place
 * - Transazioni visualizzate direttamente inline nella stessa scheda
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountActionBottomSheet(
    accountWithBalance: AccountWithBalance,
    transactions: List<TransactionDetail>,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onAdjustBalance: (Double) -> Unit,
    onTransferClick: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {}
) {
    val account = accountWithBalance.account
    val balance = accountWithBalance.currentBalance

    val accountColor = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (_: Exception) {
        PrimaryMint
    }

    var sheetMode by remember { mutableStateOf(AccountSheetMode.ACTIONS) }
    var showInlineTransactions by remember { mutableStateOf(false) }
    var expressionInput by remember(accountWithBalance) {
        mutableStateOf(String.format(Locale.US, "%.2f", balance))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        tonalElevation = 0.dp,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(tween(260))
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card Elegante del Conto (stile FinTechHeaderCard coerente)
            val cardCorner = RoundedCornerShape(24.dp)
            val isEditingBalance = sheetMode == AccountSheetMode.ADJUST_BALANCE

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardCorner)
                    .clickable {
                        sheetMode = if (sheetMode == AccountSheetMode.ADJUST_BALANCE) AccountSheetMode.ACTIONS else AccountSheetMode.ADJUST_BALANCE
                    },
                shape = cardCorner,
                color = SurfaceElevated,
                border = BorderStroke(1.dp, if (isEditingBalance) PrimaryMint else SurfaceBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    SurfaceElevated,
                                    SurfaceDark.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(accountColor.copy(alpha = 0.18f))
                                        .border(1.dp, accountColor.copy(alpha = 0.40f), RoundedCornerShape(11.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getAccountIcon(account.accountType),
                                        contentDescription = null,
                                        tint = accountColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = account.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            if (isEditingBalance) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(PrimaryMint.copy(alpha = 0.18f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "MODIFICA SALDO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryMint
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                                        .clickable {
                                            onDismiss()
                                            onTransferClick()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "Giroconto",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Giroconto",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (isEditingBalance) "DIGITA IL NUOVO SALDO" else "SALDO DISPONIBILE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.1.sp,
                                color = if (isEditingBalance) PrimaryMint else TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))

                            val displayBalance = if (isEditingBalance) {
                                val parsed = evaluateMathExpression(expressionInput)
                                if (parsed != null) "€ ${String.format(Locale.ITALY, "%,.2f", parsed)}" else "€ $expressionInput"
                            } else {
                                "€ ${String.format(Locale.ITALY, "%,.2f", balance)}"
                            }

                            Text(
                                text = displayBalance,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEditingBalance) PrimaryMint else TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Contenuto dinamico in base a SheetMode
            AnimatedContent(
                targetState = sheetMode,
                label = "account_sheet_content"
            ) { mode ->
                when (mode) {
                    AccountSheetMode.ACTIONS -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 3 Azioni Rapide Sobrie (Edit, Balance Numpad, Transactions List)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                RoundActionButton(
                                    icon = Icons.Default.Edit,
                                    label = "Modifica Conto",
                                    iconColor = Color(0xFFF59E0B),
                                    onClick = {
                                        onDismiss()
                                        onEditClick()
                                    }
                                )

                                RoundActionButton(
                                    icon = Icons.Default.AccountBalanceWallet,
                                    label = "Aggiorna Saldo",
                                    iconColor = AccentSky,
                                    onClick = {
                                        expressionInput = String.format(Locale.US, "%.2f", balance)
                                        sheetMode = AccountSheetMode.ADJUST_BALANCE
                                    }
                                )

                                RoundActionButton(
                                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                    label = "Transazioni",
                                    iconColor = PrimaryMint,
                                    onClick = {
                                        showInlineTransactions = !showInlineTransactions
                                    }
                                )
                            }

                            // Transazioni inline visibili direttamente sotto i 3 pulsanti
                            AnimatedVisibility(
                                visible = showInlineTransactions,
                                enter = expandVertically(tween(260)) + fadeIn(tween(240)),
                                exit = shrinkVertically(tween(200)) + fadeOut(tween(180))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Cronologia Transazioni",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "${transactions.size} transazioni",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))

                                    if (transactions.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Nessuna transazione per questo conto",
                                                color = TextMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 240.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(transactions) { tx ->
                                                TransactionRowItem(
                                                    transaction = tx,
                                                    onClick = {
                                                        onDismiss()
                                                        onTransactionClick(tx.id)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AccountSheetMode.ADJUST_BALANCE -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { sheetMode = AccountSheetMode.ACTIONS }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Indietro",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Annulla", color = TextSecondary, fontSize = 12.sp)
                                }

                                Text(
                                    text = "Usa ✓ per confermare",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            // Tastierino Calcolatrice Stile Transazioni
                            OneMoneyCalculatorKeypad(
                                currentExpression = expressionInput,
                                onExpressionChange = { expressionInput = it },
                                onConfirm = {
                                    val target = evaluateMathExpression(expressionInput)
                                    if (target != null) {
                                        onAdjustBalance(target)
                                    }
                                    onDismiss()
                                },
                                onDateClick = { /* no op */ },
                                confirmColor = PrimaryMint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundActionButton(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(SurfaceElevated)
                .border(1.dp, iconColor.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}

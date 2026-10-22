package com.edoardo.expensetracker.ui.screens.investments

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.data.local.entity.AccountEntity
import com.edoardo.expensetracker.data.local.entity.InvestmentTransactionType
import com.edoardo.expensetracker.data.local.entity.StockEntity
import com.edoardo.expensetracker.ui.theme.*
import java.util.Locale

private enum class ActiveInputField {
    PRIMARY, // Compra: totale pagato (€). Vendi: quantità quote.
    PRICE    // Prezzo per azione (€).
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockTradeSheet(
    stock: StockEntity,
    accounts: List<AccountEntity>,
    sharesOwnedTotal: Double,
    onDismiss: () -> Unit,
    onConfirmTrade: (
        accountId: Long,
        symbol: String,
        stockName: String,
        type: InvestmentTransactionType,
        shares: Double,
        pricePerShare: Double,
        totalAmount: Double
    ) -> Unit
) {
    var tradeType by remember { mutableStateOf(InvestmentTransactionType.BUY) }
    var selectedAccountId by remember {
        mutableStateOf(accounts.firstOrNull()?.id ?: 0L)
    }

    // Input states
    // Compra: quanto sto pagando (€)
    var buyTotalAmountStr by remember { mutableStateOf("") }
    // Vendi: quante stock sto vendendo
    var sellSharesStr by remember { mutableStateOf("") }

    // Prezzo per azione (precompilato con stock.currentPrice)
    var pricePerShareStr by remember(stock.currentPrice) {
        mutableStateOf(String.format(Locale.US, "%.2f", stock.currentPrice))
    }

    // Quale campo sta digitando il keypad
    var activeField by remember { mutableStateOf(ActiveInputField.PRIMARY) }

    // Calcoli dinamici
    val parsedPrice = pricePerShareStr.toDoubleOrNull() ?: stock.currentPrice.coerceAtLeast(0.01)

    // Se COMPRA: calcola le quote = totale / prezzo
    val parsedBuyTotal = buyTotalAmountStr.toDoubleOrNull() ?: 0.0
    val calculatedBuyShares = if (parsedPrice > 0.0) parsedBuyTotal / parsedPrice else 0.0

    // Se VENDI: calcola il ricavo = quote * prezzo
    val parsedSellShares = sellSharesStr.toDoubleOrNull() ?: 0.0
    val calculatedSellTotal = parsedSellShares * parsedPrice

    val isBuy = tradeType == InvestmentTransactionType.BUY
    val themeColor = if (isBuy) PrimaryMint else ExpenseRed

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceDark,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.20f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 20.dp)
        ) {
            // Header: Simbolo, Nome e Prezzo Live
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stock.symbol,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryMint
                            )
                        }
                    }
                    Text(
                        text = stock.name,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "€ ${String.format(Locale.ITALY, "%,.2f", stock.currentPrice)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Possedute: ${String.format(Locale.US, "%.4f", sharesOwnedTotal)} quote",
                        fontSize = 11.sp,
                        color = if (sharesOwnedTotal > 0) PrimaryMint else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selettore Conto Investimento (se più di 1)
            if (accounts.size > 1) {
                Text(
                    text = "CONTO INVESTIMENTO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        val isSelected = acc.id == selectedAccountId
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedAccountId = acc.id },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFF0EA5E9).copy(alpha = 0.22f) else SurfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF0EA5E9) else SurfaceBorder)
                        ) {
                            Text(
                                text = acc.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF38BDF8) else TextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Tab Toggle: COMPRA (Default) vs VENDI
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                ) {
                    // Tasto COMPRA
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isBuy) PrimaryMint.copy(alpha = 0.25f) else Color.Transparent
                            )
                            .clickable {
                                tradeType = InvestmentTransactionType.BUY
                                activeField = ActiveInputField.PRIMARY
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "COMPRA",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBuy) PrimaryMint else TextSecondary
                        )
                    }

                    // Tasto VENDI
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (!isBuy) ExpenseRed.copy(alpha = 0.25f) else Color.Transparent
                            )
                            .clickable {
                                tradeType = InvestmentTransactionType.SELL
                                activeField = ActiveInputField.PRIMARY
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "VENDI",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isBuy) ExpenseRed else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Box Input Campi (Totale/Quote e Prezzo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Campo Principale
                val isPrimaryActive = activeField == ActiveInputField.PRIMARY
                Surface(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { activeField = ActiveInputField.PRIMARY },
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(
                        1.5.dp,
                        if (isPrimaryActive) themeColor else SurfaceBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isBuy) "QUANTO STAI PAGANDO (€)" else "QUANTE STOCK VENDI",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPrimaryActive) themeColor else TextSecondary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val mainDisplay = if (isBuy) {
                            if (buyTotalAmountStr.isEmpty()) "0.00" else buyTotalAmountStr
                        } else {
                            if (sellSharesStr.isEmpty()) "0" else sellSharesStr
                        }
                        Text(
                            text = mainDisplay,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPrimaryActive) TextPrimary else TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                // Campo Prezzo
                val isPriceActive = activeField == ActiveInputField.PRICE
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { activeField = ActiveInputField.PRICE },
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(
                        1.5.dp,
                        if (isPriceActive) themeColor else SurfaceBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isBuy) "PREZZO COMPRA (€)" else "PREZZO VENDITA (€)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPriceActive) themeColor else TextSecondary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (pricePerShareStr.isEmpty()) "0.00" else pricePerShareStr,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPriceActive) TextPrimary else TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Riepilogo Calcolato Dinamico & Tasto rapido "MAX" per Vendi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isBuy) {
                    Text(
                        text = "Quote acquistate: ≈ ${String.format(Locale.US, "%.4f", calculatedBuyShares)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryMint
                    )
                } else {
                    Text(
                        text = "Ricavo stimato: ≈ € ${String.format(Locale.ITALY, "%,.2f", calculatedSellTotal)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    if (sharesOwnedTotal > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ExpenseRed.copy(alpha = 0.18f))
                                .clickable {
                                    sellSharesStr = String.format(Locale.US, "%.4f", sharesOwnedTotal).trimEnd('0').trimEnd('.')
                                    activeField = ActiveInputField.PRIMARY
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "TUTTE (MAX)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Keypad Numerico Moderno Touch
            StockKeypad(
                onDigitClick = { digit ->
                    when (activeField) {
                        ActiveInputField.PRIMARY -> {
                            if (isBuy) {
                                if (buyTotalAmountStr.length < 10) buyTotalAmountStr += digit
                            } else {
                                if (sellSharesStr.length < 10) sellSharesStr += digit
                            }
                        }
                        ActiveInputField.PRICE -> {
                            if (pricePerShareStr.length < 10) pricePerShareStr += digit
                        }
                    }
                },
                onDotClick = {
                    when (activeField) {
                        ActiveInputField.PRIMARY -> {
                            if (isBuy && !buyTotalAmountStr.contains(".")) {
                                buyTotalAmountStr = if (buyTotalAmountStr.isEmpty()) "0." else "$buyTotalAmountStr."
                            } else if (!isBuy && !sellSharesStr.contains(".")) {
                                sellSharesStr = if (sellSharesStr.isEmpty()) "0." else "$sellSharesStr."
                            }
                        }
                        ActiveInputField.PRICE -> {
                            if (!pricePerShareStr.contains(".")) {
                                pricePerShareStr = if (pricePerShareStr.isEmpty()) "0." else "$pricePerShareStr."
                            }
                        }
                    }
                },
                onBackspaceClick = {
                    when (activeField) {
                        ActiveInputField.PRIMARY -> {
                            if (isBuy) {
                                if (buyTotalAmountStr.isNotEmpty()) {
                                    buyTotalAmountStr = buyTotalAmountStr.dropLast(1)
                                }
                            } else {
                                if (sellSharesStr.isNotEmpty()) {
                                    sellSharesStr = sellSharesStr.dropLast(1)
                                }
                            }
                        }
                        ActiveInputField.PRICE -> {
                            if (pricePerShareStr.isNotEmpty()) {
                                pricePerShareStr = pricePerShareStr.dropLast(1)
                            }
                        }
                    }
                },
                onClearClick = {
                    when (activeField) {
                        ActiveInputField.PRIMARY -> {
                            if (isBuy) buyTotalAmountStr = "" else sellSharesStr = ""
                        }
                        ActiveInputField.PRICE -> pricePerShareStr = ""
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bottone Conferma
            val isValid = if (isBuy) {
                parsedBuyTotal > 0.0 && parsedPrice > 0.0 && selectedAccountId > 0
            } else {
                parsedSellShares > 0.0 && parsedPrice > 0.0 && selectedAccountId > 0 && parsedSellShares <= sharesOwnedTotal + 0.0001
            }

            Button(
                onClick = {
                    if (!isValid) return@Button
                    val finalShares = if (isBuy) calculatedBuyShares else parsedSellShares
                    val finalTotal = if (isBuy) parsedBuyTotal else calculatedSellTotal

                    onConfirmTrade(
                        selectedAccountId,
                        stock.symbol,
                        stock.name,
                        tradeType,
                        finalShares,
                        parsedPrice,
                        finalTotal
                    )
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColor,
                    contentColor = if (isBuy) Color(0xFF0F172A) else Color.White,
                    disabledContainerColor = themeColor.copy(alpha = 0.25f),
                    disabledContentColor = TextMuted
                )
            ) {
                Text(
                    text = if (isBuy) {
                        "Conferma Acquisto (€ ${String.format(Locale.ITALY, "%,.2f", parsedBuyTotal)})"
                    } else {
                        "Conferma Vendita (Ricavo € ${String.format(Locale.ITALY, "%,.2f", calculatedSellTotal)})"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StockKeypad(
    onDigitClick: (String) -> Unit,
    onDotClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                when (key) {
                                    "." -> onDotClick()
                                    "⌫" -> onBackspaceClick()
                                    else -> onDigitClick(key)
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = if (key == "⌫") SurfaceBorder else SurfaceElevated,
                        border = BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "⌫") {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = key,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

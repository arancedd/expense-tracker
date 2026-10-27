package com.edoardo.expensetracker.ui.screens.investments

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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.data.local.dao.InvestmentTransactionWithAccount
import com.edoardo.expensetracker.data.local.entity.AccountType
import com.edoardo.expensetracker.data.local.entity.InvestmentTransactionType
import com.edoardo.expensetracker.ui.components.FinTechHeaderCard
import com.edoardo.expensetracker.ui.screens.accounts.EditAccountSheet
import com.edoardo.expensetracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvestmentsScreen(
    viewModel: InvestmentsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    // Aggiornamento prezzi via WebSocket: ATTIVO SOLO NELLA SEZIONE INVESTIMENTI
    DisposableEffect(Unit) {
        viewModel.startStreaming()
        onDispose {
            viewModel.stopStreaming()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        if (!state.hasInvestmentAccounts) {
            // Stato vuoto: Nessun conto investimento ancora presente
            EmptyInvestmentAccountsState(
                onAddAccountClick = { viewModel.openAddAccount() }
            )
        } else {
            // Sezione con FinTechHeaderCard e contenuti
            InvestmentsContent(
                state = state,
                onTabSelected = { viewModel.setTab(it) },
                onAddStockClick = { viewModel.openFollowSheet() },
                onStockClick = { stockWithHolding ->
                    viewModel.openTradeForStock(stockWithHolding.stock)
                }
            )
        }

        // Bottom Sheet di Creazione Conto Investimento (pre-selezionato su INVESTMENT)
        if (state.isAddAccountOpen) {
            EditAccountSheet(
                initialType = AccountType.INVESTMENT,
                onDismiss = { viewModel.closeAddAccount() },
                onSave = { name, _, colorHex, iconName, _, _, _ ->
                    viewModel.createInvestmentAccount(name, colorHex, iconName)
                }
            )
        }

        // Bottom Sheet "Segui Stock"
        if (state.isFollowSheetOpen) {
            FollowStockSheet(
                allStocks = state.allAvailableStocks,
                onlineResults = state.onlineSearchResults,
                isSearchingOnline = state.isSearchingOnline,
                onSearchOnline = { query -> viewModel.searchOnline(query) },
                onDismiss = { viewModel.closeFollowSheet() },
                onToggleFollow = { sym, followed ->
                    viewModel.toggleFollowStock(sym, followed)
                },
                onAddNewStock = { sym, name, price, assetType ->
                    viewModel.addNewStock(sym, name, price, assetType)
                }
            )
        }

        // Keypad Bottom Sheet Compra / Vendi
        state.selectedStockForTrade?.let { selectedStock ->
            val stockHolding = state.stocksWithHoldings.firstOrNull { it.stock.symbol == selectedStock.symbol }
            StockTradeSheet(
                stock = selectedStock,
                accounts = state.investmentAccounts,
                sharesOwnedTotal = stockHolding?.sharesOwned ?: 0.0,
                onDismiss = { viewModel.closeTrade() },
                onConfirmTrade = { accountId, symbol, stockName, type, shares, price, total ->
                    viewModel.executeTrade(
                        accountId = accountId,
                        symbol = symbol,
                        stockName = stockName,
                        type = type,
                        shares = shares,
                        pricePerShare = price,
                        totalAmount = total
                    )
                }
            )
        }
    }
}

/**
 * Schermata di benvenuto elegante quando non ci sono ancora conti investimento.
 */
@Composable
private fun EmptyInvestmentAccountsState(
    onAddAccountClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icona illustrativa
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0EA5E9).copy(alpha = 0.25f),
                            Color(0xFF0EA5E9).copy(alpha = 0.05f)
                        )
                    )
                )
                .border(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.4f), RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Nessun Conto Investimenti",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Per iniziare a seguire le azioni in tempo reale e registrare i tuoi acquisti o vendite, crea il tuo primo conto investimento.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onAddAccountClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0EA5E9),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(50.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Aggiungi Conto Investimento",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Contenuto principale con FinTechHeaderCard standardizzata Luxury FinTech.
 */
@Composable
private fun InvestmentsContent(
    state: InvestmentsUiState,
    onTabSelected: (InvestmentTab) -> Unit,
    onAddStockClick: () -> Unit,
    onStockClick: (StockWithHolding) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. FinTechHeaderCard
        item {
            FinTechHeaderCard(
                badgeIcon = Icons.AutoMirrored.Filled.TrendingUp,
                badgeTitle = "INVESTIMENTI",
                trailingTopContent = {
                    // Bottone + in alto a destra per seguire nuovi stock
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onAddStockClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Segui Stock",
                            tint = PrimaryMint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                middleContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "VALORE TOTALE INVESTIMENTI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Totale di TUTTI i conti investimento (formula aggiornata al minuto)
                        Text(
                            text = "€ ${String.format(Locale.ITALY, "%,.2f", state.totalPortfolioValue)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Indicatore live al minuto
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (state.isStreamingActive) PrimaryMint else TextMuted)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.isStreamingActive) "Aggiornamento live al minuto" else "Pausa",
                                fontSize = 11.sp,
                                color = if (state.isStreamingActive) PrimaryMint else TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                bottomContent = {
                    // Due chip: Sinistra "Stock", Destra "Transazioni"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Chip Sinistra: Stock
                        val isStockSelected = state.selectedTab == InvestmentTab.STOCKS
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onTabSelected(InvestmentTab.STOCKS) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isStockSelected) PrimaryMint.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(
                                1.dp,
                                if (isStockSelected) PrimaryMint else SurfaceBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                    contentDescription = null,
                                    tint = if (isStockSelected) PrimaryMint else TextSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Stock",
                                    fontSize = 12.sp,
                                    fontWeight = if (isStockSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isStockSelected) PrimaryMint else TextPrimary
                                )
                            }
                        }

                        // Chip Destra: Transazioni
                        val isTxSelected = state.selectedTab == InvestmentTab.TRANSACTIONS
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onTabSelected(InvestmentTab.TRANSACTIONS) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isTxSelected) PrimaryMint.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(
                                1.dp,
                                if (isTxSelected) PrimaryMint else SurfaceBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = if (isTxSelected) PrimaryMint else TextSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Transazioni",
                                    fontSize = 12.sp,
                                    fontWeight = if (isTxSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isTxSelected) PrimaryMint else TextPrimary
                                )
                            }
                        }
                    }
                }
            )
        }

        // 2. Contenuto in base alla Chip selezionata
        when (state.selectedTab) {
            InvestmentTab.STOCKS -> {
                if (state.stocksWithHoldings.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Nessuno stock seguito",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tocca il pulsante \"+\" in alto a destra per iniziare a seguire dei titoli.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(state.stocksWithHoldings, key = { it.stock.symbol }) { stockItem ->
                        StockRowCard(
                            item = stockItem,
                            onClick = { onStockClick(stockItem) }
                        )
                    }
                }
            }
            InvestmentTab.TRANSACTIONS -> {
                if (state.transactions.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Nessuna transazione ancora",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Clicca su uno stock per comprare o vendere quote.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(state.transactions, key = { it.id }) { tx ->
                        InvestmentTransactionRow(transaction = tx)
                    }
                }
            }
        }
    }
}

/**
 * Scheda per singolo stock con quotazione live e quote possedute.
 */
@Composable
private fun StockRowCard(
    item: StockWithHolding,
    onClick: () -> Unit
) {
    val stock = item.stock
    val isPositive = stock.changePercent >= 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Ticker & Nome
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stock.symbol,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    AssetTypeBadge(assetType = stock.assetType)
                    if (item.sharesOwned > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimaryMint.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.3f", item.sharesOwned)} quote",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryMint
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stock.name,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Prezzo e Variazione %
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "€ ${String.format(Locale.ITALY, "%,.2f", stock.currentPrice)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = if (isPositive) PrimaryMint else ExpenseRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.2f", stock.changePercent)}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) PrimaryMint else ExpenseRed
                    )
                }

                if (item.sharesOwned > 0) {
                    Text(
                        text = "Val: € ${String.format(Locale.ITALY, "%,.2f", item.totalValue)}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Riga per transazione di investimento (Compra / Vendi).
 */
@Composable
private fun InvestmentTransactionRow(
    transaction: InvestmentTransactionWithAccount
) {
    val isBuy = transaction.type == InvestmentTransactionType.BUY
    val typeColor = if (isBuy) PrimaryMint else ExpenseRed
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.ITALY) }
    val dateStr = remember(transaction.timestamp) { dateFormat.format(Date(transaction.timestamp)) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Badge Compra/Vendi
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(typeColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBuy) Icons.Default.ShoppingBag else Icons.Default.Sell,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.symbol,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBuy) "ACQUISTO" else "VENDITA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }
                    Text(
                        text = "${String.format(Locale.US, "%.4f", transaction.shares)} quote @ € ${String.format(Locale.ITALY, "%,.2f", transaction.pricePerShare)}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${transaction.accountName} • $dateStr",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            // Totale Pagato o Ricavato
            Text(
                text = "${if (isBuy) "-" else "+"}€ ${String.format(Locale.ITALY, "%,.2f", transaction.totalAmount)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isBuy) TextPrimary else PrimaryMint
            )
        }
    }
}

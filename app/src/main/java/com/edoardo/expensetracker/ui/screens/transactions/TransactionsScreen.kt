package com.edoardo.expensetracker.ui.screens.transactions

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.data.local.dao.TransactionDetail
import com.edoardo.expensetracker.data.local.entity.TransactionType
import com.edoardo.expensetracker.ui.components.CalendarPeriodDialog
import com.edoardo.expensetracker.ui.components.OneMoneyCalculatorKeypad
import com.edoardo.expensetracker.ui.components.TransactionRowItem
import com.edoardo.expensetracker.ui.components.evaluateMathExpression
import com.edoardo.expensetracker.ui.components.getCategoryIconByName
import com.edoardo.expensetracker.ui.theme.*
import java.util.Locale

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onTransactionClick: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // --- FinTech Luxury Top Card per Transazioni ---
        com.edoardo.expensetracker.ui.components.FinTechHeaderCard(
            badgeIcon = Icons.AutoMirrored.Filled.ReceiptLong,
            badgeTitle = "TRANSAZIONI",
            trailingTopContent = {
                // Tasto Calendario in squircle coerente con le altre sezioni
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (state.isDateFilterActive) PrimaryMint.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            if (state.isDateFilterActive) PrimaryMint else SurfaceBorder,
                            RoundedCornerShape(11.dp)
                        )
                        .clickable {
                            if (state.isDateFilterActive) {
                                viewModel.resetDateFilter()
                            } else {
                                viewModel.toggleDatePicker()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isDateFilterActive) Icons.Default.Close else Icons.Default.CalendarToday,
                        contentDescription = if (state.isDateFilterActive) "Azzera data" else "Filtra per data",
                        tint = if (state.isDateFilterActive) PrimaryMint else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            middleContent = {

                    // Centro card: Barra di Ricerca Integrata (altezza e proporzioni coerenti con le altre card di testata)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(
                            1.dp,
                            if (state.searchQuery.isNotBlank() || state.isSearchActive) PrimaryMint.copy(alpha = 0.45f) else SurfaceBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cerca",
                                tint = if (state.searchQuery.isNotBlank()) PrimaryMint else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (state.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Cerca per note, esercente, conto...",
                                        fontSize = 13.sp,
                                        color = TextMuted
                                    )
                                }
                                BasicTextField(
                                    value = state.searchQuery,
                                    onValueChange = { viewModel.onSearchQueryChange(it) },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(PrimaryMint),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                viewModel.setSearchActive(true)
                                            }
                                        }
                                )
                            }

                            if (state.searchQuery.isNotEmpty() || state.selectedAccountIds.isNotEmpty() || state.selectedCategoryIds.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.clearSearchAndFilters() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Azzera Filtri",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { viewModel.toggleSearchActive() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Filtri",
                                        tint = if (state.isSearchActive) PrimaryMint else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
            },
            bottomContent = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Base card: Le 3 chips "Uscite", "Entrate", "Giroconti" con toggle deselezionabile
                    Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Chip 1: Uscite
                        val isExpense = state.selectedTypeFilter == TransactionType.EXPENSE
                        val expBgColor by animateColorAsState(if (isExpense) ExpenseRed.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.04f), label = "exp_bg")
                        val expBorderColor by animateColorAsState(if (isExpense) ExpenseRed.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.06f), label = "exp_border")

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.onTypeFilterChange(if (isExpense) null else TransactionType.EXPENSE)
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = expBgColor,
                            border = BorderStroke(1.dp, expBorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(ExpenseRed.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Uscite",
                                    fontSize = 12.sp,
                                    fontWeight = if (isExpense) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isExpense) ExpenseRed else TextSecondary
                                )
                            }
                        }

                        // Chip 2: Entrate
                        val isIncome = state.selectedTypeFilter == TransactionType.INCOME
                        val incBgColor by animateColorAsState(if (isIncome) PrimaryMint.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.04f), label = "inc_bg")
                        val incBorderColor by animateColorAsState(if (isIncome) PrimaryMint.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.06f), label = "inc_border")

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.onTypeFilterChange(if (isIncome) null else TransactionType.INCOME)
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = incBgColor,
                            border = BorderStroke(1.dp, incBorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryMint.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = PrimaryMint,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Entrate",
                                    fontSize = 12.sp,
                                    fontWeight = if (isIncome) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isIncome) PrimaryMint else TextSecondary
                                )
                            }
                        }

                        // Chip 3: Giroconti
                        val isTransfer = state.selectedTypeFilter == TransactionType.TRANSFER
                        val trBgColor by animateColorAsState(if (isTransfer) SecondaryViolet.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.04f), label = "tr_bg")
                        val trBorderColor by animateColorAsState(if (isTransfer) SecondaryViolet.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.06f), label = "tr_border")

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.onTypeFilterChange(if (isTransfer) null else TransactionType.TRANSFER)
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = trBgColor,
                            border = BorderStroke(1.dp, trBorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(SecondaryViolet.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = SecondaryViolet,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Giroconti",
                                    fontSize = 12.sp,
                                    fontWeight = if (isTransfer) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isTransfer) SecondaryViolet else TextSecondary
                                )
                            }
                        }
                    }

                    // --- Filtri Avanzati Multi-Select: allargano direttamente la card esistente ---
                    AnimatedVisibility(
                        visible = state.isSearchActive,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(SurfaceBorder)
                            )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Intestazione Sezione Conti
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "FILTRA PER CONTO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextSecondary
                        )
                        if (state.selectedAccountIds.isNotEmpty()) {
                            Text(
                                text = "${state.selectedAccountIds.size} selezionati",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryMint
                            )
                        }
                    }

                    // Chip orizzontali Conti (Multi-Select)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.availableAccounts.forEach { accWithBal ->
                            val acc = accWithBal.account
                            val isSelected = state.selectedAccountIds.contains(acc.id)
                            val chipCorner = RoundedCornerShape(10.dp)
                            val accColor = try {
                                Color(android.graphics.Color.parseColor(acc.colorHex))
                            } catch (_: Exception) {
                                PrimaryMint
                            }

                            Surface(
                                modifier = Modifier
                                    .clip(chipCorner)
                                    .clickable { viewModel.toggleAccountFilter(acc.id) },
                                shape = chipCorner,
                                color = if (isSelected) PrimaryMint.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryMint else SurfaceBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(accColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = acc.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) PrimaryMint else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Intestazione Sezione Categorie
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "FILTRA PER CATEGORIA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextSecondary
                        )
                        if (state.selectedCategoryIds.isNotEmpty()) {
                            Text(
                                text = "${state.selectedCategoryIds.size} selezionate",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryMint
                            )
                        }
                    }

                    // Chip orizzontali Categorie (Multi-Select)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.availableCategories.forEach { catWithSubs ->
                            val cat = catWithSubs.category
                            val isSelected = state.selectedCategoryIds.contains(cat.id)
                            val chipCorner = RoundedCornerShape(10.dp)
                            val catColor = try {
                                Color(android.graphics.Color.parseColor(cat.colorHex))
                            } catch (_: Exception) {
                                PrimaryMint
                            }

                            Surface(
                                modifier = Modifier
                                    .clip(chipCorner)
                                    .clickable { viewModel.toggleCategoryFilter(cat.id) },
                                shape = chipCorner,
                                color = if (isSelected) PrimaryMint.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryMint else SurfaceBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) PrimaryMint else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
)

        if (state.isDatePickerOpen) {
            CalendarPeriodDialog(
                currentPeriod = state.selectedPeriod,
                currentDateMillis = state.selectedDateMillis,
                selectedRangeStartMillis = state.selectedRangeStartMillis,
                selectedRangeEndMillis = state.selectedRangeEndMillis,
                onSelectPeriod = { viewModel.onSelectPeriod(it) },
                onSelectDay = { viewModel.onSelectDay(it) },
                onSelectRange = { start, end -> viewModel.onSelectRange(start, end) },
                onResetToDefault = { viewModel.resetDateFilter() },
                onDismissRequest = { viewModel.closeDatePicker() },
                defaultActionLabel = "Tutte le Date (Default)"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- Lista Transazioni Raggruppate per Giorno ---
        if (state.groupedTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (state.searchQuery.isNotBlank() || state.selectedAccountIds.isNotEmpty() || state.selectedCategoryIds.isNotEmpty())
                        "Nessun risultato trovato per i filtri attivi"
                    else "Nessuna transazione registrata",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                state.groupedTransactions.forEach { dayGroup ->
                    item {
                        // Header del Giorno con Spese ed Entrate
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dayGroup.dateHeader,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (dayGroup.dayTotalExpense > 0) {
                                    Text(
                                        text = "-${String.format(Locale.ITALY, "%.2f", dayGroup.dayTotalExpense)} €",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                }
                                if (dayGroup.dayTotalIncome > 0) {
                                    Text(
                                        text = "+${String.format(Locale.ITALY, "%.2f", dayGroup.dayTotalIncome)} €",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryMint
                                    )
                                }
                            }
                        }
                    }

                    items(dayGroup.transactions) { tx ->
                        TransactionRowItem(
                            transaction = tx,
                            onClick = {
                                onTransactionClick(tx.id)
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

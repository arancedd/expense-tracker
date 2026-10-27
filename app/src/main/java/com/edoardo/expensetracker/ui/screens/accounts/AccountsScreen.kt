package com.edoardo.expensetracker.ui.screens.accounts

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edoardo.expensetracker.data.local.dao.AccountWithBalance
import com.edoardo.expensetracker.data.local.entity.AccountEntity
import com.edoardo.expensetracker.data.local.entity.AccountType
import com.edoardo.expensetracker.ui.components.getAccountIcon
import com.edoardo.expensetracker.ui.components.getAccountTypeLabel
import com.edoardo.expensetracker.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onRechargeAccount: (Long) -> Unit = {},
    onWithdrawAccount: (Long) -> Unit = {},
    onTransferAccount: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    var selectedAccountForAction by remember { mutableStateOf<AccountWithBalance?>(null) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var showArchivedSection by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Accounts, 1 = Debts

    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // --- FinTech Luxury Card per Conti (Badge "CONTI" + Saldo Netto + 2 Grandi Chips: Conti e Debiti) ---
        com.edoardo.expensetracker.ui.components.FinTechHeaderCard(
            badgeIcon = Icons.Default.AccountBalanceWallet,
            badgeTitle = "CONTI",
            trailingTopContent = {
                // Tasto '+' per nuovo conto in squircle elevato
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(11.dp))
                        .clickable { viewModel.openAddAccountDialog() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Aggiungi Conto",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            middleContent = {
                // Cifra monumentale patrimonio netto
                Text(
                    text = "€ ${String.format(Locale.ITALY, "%,.2f", state.totalWealth)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = TextPrimary
                )
            },
            bottomContent = {
                // Le due grandi chips: Conti e Debiti
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Chip 1: Conti
                    val isAccountsSelected = selectedTab == 0
                    val accBgColor by animateColorAsState(if (isAccountsSelected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f), label = "acc_bg")
                    val accBorderColor by animateColorAsState(if (isAccountsSelected) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.06f), label = "acc_border")

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedTab = 0 },
                        shape = RoundedCornerShape(14.dp),
                        color = accBgColor,
                        border = BorderStroke(1.dp, accBorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isAccountsSelected) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = if (isAccountsSelected) Color.White else TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Conti",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAccountsSelected) Color.White else TextSecondary
                                )
                                Text(
                                    text = "€ ${String.format(Locale.ITALY, "%,.0f", state.accountsTotal)} • ${state.accountsCount}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Chip 2: Debiti / Crediti (se > 0 diventa "Crediti", se <= 0 resta "Debiti")
                    val isDebtsSelected = selectedTab == 1
                    val debtsTitle = if (state.debtsTotal > 0) "Crediti" else "Debiti"
                    val debtThemeColor = if (state.debtsTotal > 0) PrimaryMint else ExpenseRed
                    val debtBgColor by animateColorAsState(if (isDebtsSelected) debtThemeColor.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f), label = "debt_bg")
                    val debtBorderColor by animateColorAsState(if (isDebtsSelected) debtThemeColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f), label = "debt_border")

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedTab = 1 },
                        shape = RoundedCornerShape(14.dp),
                        color = debtBgColor,
                        border = BorderStroke(1.dp, debtBorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(debtThemeColor.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = debtThemeColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = debtsTitle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDebtsSelected) debtThemeColor else TextSecondary
                                )
                                Text(
                                    text = "€ ${String.format(Locale.ITALY, "%,.0f", state.debtsTotal)} • ${state.debtsCount}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (selectedTab == 0) {
                // Vista Accounts: Conti Correnti, Carte, Risparmi
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Conti",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        val regColor = if (state.regularSubtotal < 0) ExpenseRed else PrimaryMint
                        Text(
                            text = "€ ${String.format(Locale.ITALY, "%,.2f", state.regularSubtotal)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = regColor
                        )
                    }
                }

                if (state.regularAccounts.isEmpty() && state.savingsAccounts.isEmpty() && state.investmentAccounts.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceElevated,
                            border = BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Nessun conto attivo", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tocca '+' in alto a destra per creare un conto.", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    items(state.regularAccounts) { accWithBal ->
                        Account1MoneyRow(
                            accountWithBal = accWithBal,
                            onClick = { selectedAccountForAction = accWithBal }
                        )
                    }
                }

                if (state.savingsAccounts.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Risparmi",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            val savColor = if (state.savingsSubtotal < 0) ExpenseRed else PrimaryMint
                            Text(
                                text = "€ ${String.format(Locale.ITALY, "%,.2f", state.savingsSubtotal)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = savColor
                            )
                        }
                    }

                    items(state.savingsAccounts) { accWithBal ->
                        Account1MoneyRow(
                            accountWithBal = accWithBal,
                            onClick = { selectedAccountForAction = accWithBal }
                        )
                    }
                }

                // --- Categoria Investimenti (Sotto Risparmi) ---
                if (state.investmentAccounts.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Investimenti",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            val invColor = if (state.investmentsSubtotal < 0) ExpenseRed else PrimaryMint
                            Text(
                                text = "€ ${String.format(Locale.ITALY, "%,.2f", state.investmentsSubtotal)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = invColor
                            )
                        }
                    }

                    items(state.investmentAccounts) { accWithBal ->
                        Account1MoneyRow(
                            accountWithBal = accWithBal,
                            onClick = { selectedAccountForAction = accWithBal }
                        )
                    }
                }

                // --- Sezione Conti Archiviati (Solo in "Conti", mai in "Debiti") ---
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showArchivedSection = !showArchivedSection },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.03f),
                        border = BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Conti Archiviati (${state.archivedAccounts.size})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                            }

                            Icon(
                                imageVector = if (showArchivedSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (showArchivedSection) {
                    if (state.archivedAccounts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nessun conto archiviato",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    } else {
                        items(state.archivedAccounts) { archWithBal ->
                            ArchivedAccountRow(
                                accountWithBal = archWithBal,
                                onUnarchive = { viewModel.unarchiveAccount(archWithBal) },
                                onClick = { selectedAccountForAction = archWithBal }
                            )
                        }
                    }
                }
            } else {
                // Vista Debiti / Crediti: Conti di tipo DEBT
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    val debtsSectionTitle = if (state.debtsTotal > 0) "Crediti" else "Debiti"
                    val debtColor = if (state.debtsTotal < 0) ExpenseRed else PrimaryMint
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = debtsSectionTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "€ ${String.format(Locale.ITALY, "%,.2f", state.debtsTotal)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = debtColor
                        )
                    }
                }

                if (state.debtAccounts.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceElevated,
                            border = BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryMint.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = PrimaryMint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Nessun conto debito registrato", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Crea un conto di tipo 'Debiti' con il tasto +.", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    items(state.debtAccounts) { accWithBal ->
                        Account1MoneyRow(
                            accountWithBal = accWithBal,
                            onClick = { selectedAccountForAction = accWithBal }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Bottom Sheet Azioni Conto (Numpad Saldo & Lista Transazioni in-place)
        selectedAccountForAction?.let { accWithBal ->
            val accountTransactions by viewModel.getTransactionsForAccount(accWithBal.account.id)
                .collectAsState(initial = emptyList())

            AccountActionBottomSheet(
                accountWithBalance = accWithBal,
                transactions = accountTransactions,
                onDismiss = { selectedAccountForAction = null },
                onTransferClick = {
                    onTransferAccount(accWithBal.account.id)
                    selectedAccountForAction = null
                },
                onEditClick = {
                    accountToEdit = accWithBal.account
                    selectedAccountForAction = null
                },
                onAdjustBalance = { newBalance ->
                    viewModel.adjustAccountBalance(accWithBal, newBalance)
                    selectedAccountForAction = null
                }
            )
        }

        // Sheet Modifica Conto
        accountToEdit?.let { acc ->
            EditAccountSheet(
                account = acc,
                onDismiss = { accountToEdit = null },
                onSave = { name, type, color, icon, isActive, includeInTotal, _ ->
                    viewModel.updateAccount(acc, name, type, color, icon, isActive, includeInTotal)
                    accountToEdit = null
                },
                onDelete = {
                    viewModel.deleteAccount(acc)
                    accountToEdit = null
                }
            )
        }

        // Sheet Aggiungi Nuovo Conto
        if (state.isAddAccountDialogOpen) {
            EditAccountSheet(
                account = null,
                onDismiss = { viewModel.closeAddAccountDialog() },
                onSave = { name, type, color, icon, isActive, includeInTotal, initialBal ->
                    viewModel.saveNewAccount(name, type, color, icon, isActive, includeInTotal, initialBal)
                }
            )
        }
    }
}

@Composable
fun Account1MoneyRow(
    accountWithBal: AccountWithBalance,
    onClick: () -> Unit
) {
    val account = accountWithBal.account
    val balance = accountWithBal.currentBalance
    val accColor = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (_: Exception) {
        PrimaryMint
    }

    val rowCorner = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowCorner)
            .background(SurfaceElevated)
            .border(1.dp, SurfaceBorder, rowCorner)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icona in badge squircle traslucido Ivy style
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(accColor.copy(alpha = 0.18f))
                .border(1.dp, accColor.copy(alpha = 0.40f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getAccountIcon(account.accountType),
                contentDescription = account.name,
                tint = accColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            val balanceColor = if (balance < 0) ExpenseRed else PrimaryMint
            Text(
                text = "€ ${String.format(Locale.ITALY, "%,.2f", balance)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = balanceColor
            )
        }

        if (!account.includeInTotalBalance) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = "Escluso dal totale patrimoniale",
                    tint = TextMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun ArchivedAccountRow(
    accountWithBal: AccountWithBalance,
    onUnarchive: () -> Unit,
    onClick: () -> Unit
) {
    val account = accountWithBal.account
    val balance = accountWithBal.currentBalance
    val rowCorner = RoundedCornerShape(20.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowCorner)
            .background(SurfaceElevated.copy(alpha = 0.55f))
            .border(1.dp, SurfaceBorder.copy(alpha = 0.5f), rowCorner)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getAccountIcon(account.accountType),
                contentDescription = account.name,
                tint = TextMuted,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = account.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ARCHIVIATO",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "€ ${String.format(Locale.ITALY, "%,.2f", balance)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = TextMuted
            )
        }

        TextButton(
            onClick = onUnarchive,
            colors = ButtonDefaults.textButtonColors(contentColor = PrimaryMint)
        ) {
            Icon(
                Icons.Default.Unarchive,
                contentDescription = "Ripristina",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Ripristina", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

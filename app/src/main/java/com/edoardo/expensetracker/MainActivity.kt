package com.edoardo.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edoardo.expensetracker.data.local.dao.AccountDao
import com.edoardo.expensetracker.data.local.dao.AccountWithBalance
import com.edoardo.expensetracker.data.local.dao.TransactionDao
import com.edoardo.expensetracker.data.local.dao.TransactionDetail
import com.edoardo.expensetracker.data.local.entity.TransactionType
import com.edoardo.expensetracker.ui.navigation.Screen
import com.edoardo.expensetracker.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    accountDao: AccountDao,
    transactionDao: TransactionDao
) : ViewModel() {

    val accounts: StateFlow<List<AccountWithBalance>> = accountDao.observeAccountsWithBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionDetail>> = transactionDao.observeFilteredTransactions(limit = 50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseTrackerTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel = hiltViewModel()) {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    val screens = listOf(Screen.Dashboard, Screen.Accounts, Screen.Transactions, Screen.Budgets)

    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.recentTransactions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedScreen.title,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Apri modal inserimento spesa/entrata/giroconto */ },
                containerColor = PrimaryBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuova Transazione")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedScreen) {
                Screen.Dashboard -> DashboardContent(accounts, transactions)
                Screen.Accounts -> AccountsListContent(accounts)
                Screen.Transactions -> TransactionsListContent(transactions)
                Screen.Budgets -> BudgetsContent()
            }
        }
    }
}

@Composable
fun DashboardContent(accounts: List<AccountWithBalance>, transactions: List<TransactionDetail>) {
    val totalBalance = accounts.sumOf { it.currentBalance }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Patrimonio Totale", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${String.format(Locale.ITALY, "%,.2f", totalBalance)} €",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Text("I Tuoi Conti", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        items(accounts) { accountWithBal ->
            AccountRow(accountWithBal)
        }

        item {
            Text("Ultime Transazioni", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        items(transactions.take(10)) { tx ->
            TransactionRow(tx)
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun AccountsListContent(accounts: List<AccountWithBalance>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(accounts) { acc ->
            AccountRow(acc)
        }
    }
}

@Composable
fun TransactionsListContent(transactions: List<TransactionDetail>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(transactions) { tx ->
            TransactionRow(tx)
        }
    }
}

@Composable
fun BudgetsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(64.dp), tint = SecondaryBlue)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Modulo Budgeting Mensile", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Configura tetti di spesa per categoria con alert automatici all'80% e 100%.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
    }
}

@Composable
fun AccountRow(acc: AccountWithBalance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(acc.account.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(acc.account.accountType.name, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            Text(
                "${String.format(Locale.ITALY, "%,.2f", acc.currentBalance)} €",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (acc.currentBalance >= 0) PrimaryBlue else ExpenseRed
            )
        }
    }
}

@Composable
fun TransactionRow(tx: TransactionDetail) {
    val isExpense = tx.type == TransactionType.EXPENSE
    val amountColor = when (tx.type) {
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.INCOME -> IncomeGreen
        TransactionType.TRANSFER -> TransferPurple
    }
    val sign = if (isExpense) "-" else if (tx.type == TransactionType.INCOME) "+" else ""

    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.ITALY)
    val dateStr = dateFormat.format(Date(tx.dateMillis))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tx.payee ?: tx.categoryName ?: "Transazione",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    "${tx.accountName} • $dateStr",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            Text(
                "$sign${String.format(Locale.ITALY, "%.2f", tx.amount)} €",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = amountColor
            )
        }
    }
}

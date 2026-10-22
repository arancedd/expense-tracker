package com.edoardo.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.edoardo.expensetracker.data.local.entity.TransactionType
import com.edoardo.expensetracker.ui.components.FloatingGlassNavBar
import com.edoardo.expensetracker.ui.navigation.Screen
import com.edoardo.expensetracker.ui.screens.accounts.AccountsScreen
import com.edoardo.expensetracker.ui.screens.accounts.AccountsViewModel
import com.edoardo.expensetracker.ui.screens.budgets.BudgetsScreen
import com.edoardo.expensetracker.ui.screens.budgets.BudgetsViewModel
import com.edoardo.expensetracker.ui.screens.dashboard.DashboardScreen
import com.edoardo.expensetracker.ui.screens.dashboard.DashboardViewModel
import com.edoardo.expensetracker.ui.screens.investments.InvestmentsScreen
import com.edoardo.expensetracker.ui.screens.investments.InvestmentsViewModel
import com.edoardo.expensetracker.ui.screens.quickadd.OneMoneyAddExpenseSheet
import com.edoardo.expensetracker.ui.screens.quickadd.QuickAddViewModel
import com.edoardo.expensetracker.ui.screens.transactions.TransactionsScreen
import com.edoardo.expensetracker.ui.screens.transactions.TransactionsViewModel
import com.edoardo.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

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

@Composable
fun MainAppScreen() {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var isAddExpenseOpen by remember { mutableStateOf(false) }

    val quickAddViewModel: QuickAddViewModel = hiltViewModel()
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val accountsViewModel: AccountsViewModel = hiltViewModel()
    val investmentsViewModel: InvestmentsViewModel = hiltViewModel()
    val transactionsViewModel: TransactionsViewModel = hiltViewModel()
    val budgetsViewModel: BudgetsViewModel = hiltViewModel()

    val screens = listOf(Screen.Dashboard, Screen.Accounts, Screen.Investments, Screen.Transactions, Screen.Budgets)

    Scaffold(
        bottomBar = {
            FloatingGlassNavBar(
                selectedScreen = selectedScreen,
                screens = screens,
                onScreenSelected = { screen ->
                    selectedScreen = screen
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = selectedScreen,
                transitionSpec = {
                    val fromIndex = screens.indexOf(initialState)
                    val toIndex = screens.indexOf(targetState)
                    val direction = if (toIndex >= fromIndex) 1 else -1
                    (slideInHorizontally(animationSpec = tween(220)) { width -> (width * 0.12f * direction).toInt() } + fadeIn(animationSpec = tween(220)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(200)) { width -> -(width * 0.12f * direction).toInt() } + fadeOut(animationSpec = tween(180)))
                },
                label = "screen_content_transition"
            ) { screen ->
                when (screen) {
                    Screen.Dashboard -> DashboardScreen(
                        viewModel = dashboardViewModel,
                        onCategoryClick = { categoryId, type ->
                            quickAddViewModel.onTypeChange(type)
                            quickAddViewModel.onCategorySelected(categoryId)
                            isAddExpenseOpen = true
                        },
                        onAccountsClick = { selectedScreen = Screen.Accounts },
                        onSettingsClick = { dashboardViewModel.toggleEditMode() }
                    )
                    Screen.Accounts -> AccountsScreen(
                        viewModel = accountsViewModel,
                        onRechargeAccount = { accountId ->
                            quickAddViewModel.onTypeChange(TransactionType.INCOME)
                            quickAddViewModel.onAccountSelected(accountId)
                            isAddExpenseOpen = true
                        },
                        onWithdrawAccount = { accountId ->
                            quickAddViewModel.onTypeChange(TransactionType.EXPENSE)
                            quickAddViewModel.onAccountSelected(accountId)
                            isAddExpenseOpen = true
                        },
                        onTransferAccount = { accountId ->
                            quickAddViewModel.onTypeChange(TransactionType.TRANSFER)
                            quickAddViewModel.onAccountSelected(accountId)
                            isAddExpenseOpen = true
                        }
                    )
                    Screen.Investments -> InvestmentsScreen(
                        viewModel = investmentsViewModel
                    )
                    Screen.Transactions -> TransactionsScreen(
                        viewModel = transactionsViewModel,
                        onTransactionClick = { txId ->
                            quickAddViewModel.loadTransactionForEdit(txId)
                            isAddExpenseOpen = true
                        }
                    )
                    Screen.Budgets -> BudgetsScreen(
                        viewModel = budgetsViewModel
                    )
                }
            }

            if (isAddExpenseOpen) {
                OneMoneyAddExpenseSheet(
                    viewModel = quickAddViewModel,
                    onDismiss = { isAddExpenseOpen = false }
                )
            }
        }
    }
}

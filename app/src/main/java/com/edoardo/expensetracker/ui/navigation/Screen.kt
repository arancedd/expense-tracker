package com.edoardo.expensetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Overview", Icons.Default.Dashboard)
    object Accounts : Screen("accounts", "Conti", Icons.Default.AccountBalanceWallet)
    object Transactions : Screen("transactions", "Transazioni", Icons.AutoMirrored.Filled.ReceiptLong)
    object Budgets : Screen("budgets", "Budget", Icons.Default.PieChart)
}

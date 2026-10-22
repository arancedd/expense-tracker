package com.edoardo.expensetracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector
import com.edoardo.expensetracker.data.local.entity.AccountType

/**
 * Utility per icone ed etichette dei conti bancari / finanziari.
 */
fun getAccountIcon(type: AccountType): ImageVector {
    return when (type) {
        AccountType.REGULAR, AccountType.CHECKING -> Icons.Default.AccountBalance
        AccountType.CREDIT_CARD, AccountType.DEBT -> Icons.Default.CreditCard
        AccountType.CASH -> Icons.Default.Payments
        AccountType.SAVINGS -> Icons.Default.Savings
        AccountType.CRYPTO -> Icons.Default.CurrencyBitcoin
        AccountType.INVESTMENT -> Icons.AutoMirrored.Filled.TrendingUp
    }
}

fun getAccountTypeLabel(type: AccountType): String {
    return when (type) {
        AccountType.REGULAR -> "Regolare"
        AccountType.SAVINGS -> "Risparmi"
        AccountType.DEBT -> "Debiti"
        AccountType.CHECKING -> "Conto"
        AccountType.CREDIT_CARD -> "Carta"
        AccountType.CASH -> "Contanti"
        AccountType.CRYPTO -> "Crypto"
        AccountType.INVESTMENT -> "Investimenti"
    }
}

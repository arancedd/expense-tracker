package com.edoardo.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType {
    REGULAR,
    SAVINGS,
    DEBT,
    CHECKING,
    CREDIT_CARD,
    CASH,
    CRYPTO,
    INVESTMENT
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val accountType: AccountType,
    val initialBalance: Double = 0.0,
    val currency: String = "EUR",
    val colorHex: String = "#3B82F6",
    val iconName: String = "account_balance",
    val isActive: Boolean = true,
    val includeInTotalBalance: Boolean = true
)

package com.edoardo.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class InvestmentTransactionType {
    BUY,
    SELL
}

@Entity(
    tableName = "investment_transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("accountId"),
        Index("symbol")
    ]
)
data class InvestmentTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val symbol: String,
    val stockName: String,
    val type: InvestmentTransactionType,
    val shares: Double,
    val pricePerShare: Double,
    val totalAmount: Double, // Se BUY: importo pagato; se SELL: importo ricavato
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)

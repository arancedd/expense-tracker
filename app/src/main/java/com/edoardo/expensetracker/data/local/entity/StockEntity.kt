package com.edoardo.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey
    val symbol: String, // es. "AAPL", "TSLA", "NVDA", "MSFT", "AMZN", "BTC"
    val name: String, // es. "Apple Inc."
    val currentPrice: Double, // Prezzo in tempo reale
    val previousClosePrice: Double = currentPrice, // Per calcolo variazione %
    val currency: String = "EUR",
    val isFollowed: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val changePercent: Double
        get() = if (previousClosePrice > 0.0) {
            ((currentPrice - previousClosePrice) / previousClosePrice) * 100.0
        } else 0.0

    val changeAmount: Double
        get() = currentPrice - previousClosePrice
}

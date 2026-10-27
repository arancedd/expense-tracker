package com.edoardo.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey
    val symbol: String, // es. "AAPL", "VWCE", "SWDA", "^GSPC", "BTC"
    val name: String, // es. "Apple Inc.", "Vanguard FTSE All-World"
    val currentPrice: Double, // Prezzo in tempo reale
    val previousClosePrice: Double = currentPrice, // Per calcolo variazione %
    val currency: String = "EUR",
    val isFollowed: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis(),
    val assetType: String = "STOCK" // "ETF", "INDEX", "STOCK", "CRYPTO", "COMMODITY"
) {
    companion object {
        const val TYPE_STOCK = "STOCK"
        const val TYPE_ETF = "ETF"
        const val TYPE_INDEX = "INDEX"
        const val TYPE_CRYPTO = "CRYPTO"
        const val TYPE_COMMODITY = "COMMODITY"
    }

    val changePercent: Double
        get() = if (previousClosePrice > 0.0) {
            ((currentPrice - previousClosePrice) / previousClosePrice) * 100.0
        } else 0.0

    val changeAmount: Double
        get() = currentPrice - previousClosePrice
}

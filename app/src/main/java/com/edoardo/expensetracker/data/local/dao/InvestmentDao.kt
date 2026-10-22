package com.edoardo.expensetracker.data.local.dao

import androidx.room.*
import com.edoardo.expensetracker.data.local.entity.InvestmentTransactionEntity
import com.edoardo.expensetracker.data.local.entity.StockEntity
import kotlinx.coroutines.flow.Flow

data class StockHolding(
    val symbol: String,
    val totalShares: Double
)

data class InvestmentTransactionWithAccount(
    val id: Long,
    val accountId: Long,
    val accountName: String,
    val symbol: String,
    val stockName: String,
    val type: com.edoardo.expensetracker.data.local.entity.InvestmentTransactionType,
    val shares: Double,
    val pricePerShare: Double,
    val totalAmount: Double,
    val timestamp: Long,
    val notes: String?
)

@Dao
interface InvestmentDao {

    // --- Stocks ---
    @Query("SELECT * FROM stocks WHERE isFollowed = 1 ORDER BY symbol ASC")
    fun observeFollowedStocks(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks ORDER BY symbol ASC")
    fun observeAllStocks(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks WHERE symbol = :symbol LIMIT 1")
    suspend fun getStockBySymbol(symbol: String): StockEntity?

    @Query("SELECT * FROM stocks")
    suspend fun getAllStocksSync(): List<StockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockEntity>)

    @Update
    suspend fun updateStock(stock: StockEntity)

    @Query("UPDATE stocks SET currentPrice = :price, lastUpdated = :timestamp WHERE symbol = :symbol")
    suspend fun updateStockPrice(symbol: String, price: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE stocks SET isFollowed = :isFollowed WHERE symbol = :symbol")
    suspend fun setStockFollowed(symbol: String, isFollowed: Boolean)

    @Query("DELETE FROM stocks WHERE symbol = :symbol")
    suspend fun deleteStock(symbol: String)

    // --- Holdings Calculation ---
    @Query("""
        SELECT symbol,
        COALESCE(SUM(CASE WHEN type = 'BUY' THEN shares ELSE -shares END), 0.0) AS totalShares
        FROM investment_transactions
        GROUP BY symbol
    """)
    fun observeAllHoldings(): Flow<List<StockHolding>>

    @Query("""
        SELECT symbol,
        COALESCE(SUM(CASE WHEN type = 'BUY' THEN shares ELSE -shares END), 0.0) AS totalShares
        FROM investment_transactions
        WHERE accountId = :accountId
        GROUP BY symbol
    """)
    fun observeHoldingsForAccount(accountId: Long): Flow<List<StockHolding>>

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'BUY' THEN shares ELSE -shares END), 0.0)
        FROM investment_transactions
        WHERE symbol = :symbol AND (:accountId IS NULL OR accountId = :accountId)
    """)
    suspend fun getSharesForStock(symbol: String, accountId: Long? = null): Double

    // --- Transactions ---
    @Query("""
        SELECT it.id, it.accountId, a.name AS accountName, it.symbol, it.stockName,
               it.type, it.shares, it.pricePerShare, it.totalAmount, it.timestamp, it.notes
        FROM investment_transactions it
        JOIN accounts a ON it.accountId = a.id
        ORDER BY it.timestamp DESC, it.id DESC
    """)
    fun observeAllInvestmentTransactions(): Flow<List<InvestmentTransactionWithAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: InvestmentTransactionEntity): Long

    @Delete
    suspend fun deleteTransaction(transaction: InvestmentTransactionEntity)
}

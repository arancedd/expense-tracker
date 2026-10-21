package com.edoardo.expensetracker.data.local.dao

import androidx.room.*
import com.edoardo.expensetracker.data.local.entity.TransactionEntity
import com.edoardo.expensetracker.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

data class TransactionDetail(
    val id: Long,
    val accountId: Long,
    val accountName: String,
    val type: TransactionType,
    val amount: Double,
    val categoryId: Long?,
    val categoryName: String?,
    val categoryColor: String?,
    val subcategoryId: Long?,
    val subcategoryName: String?,
    val destinationAccountId: Long?,
    val destinationAccountName: String?,
    val dateMillis: Long,
    val payee: String?,
    val note: String?,
    val source: String
)

data class CategoryStat(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val totalAmount: Double,
    val count: Int
)

data class MonthlyOverview(
    val totalIncome: Double,
    val totalExpense: Double,
    val netCashflow: Double
)

@Dao
interface TransactionDao {

    @Query("""
        SELECT 
            t.id, t.accountId, a.name AS accountName,
            t.type, t.amount,
            t.categoryId, c.name AS categoryName, c.colorHex AS categoryColor,
            t.subcategoryId, s.name AS subcategoryName,
            t.destinationAccountId, da.name AS destinationAccountName,
            t.dateMillis, t.payee, t.note, t.source
        FROM transactions t
        JOIN accounts a ON t.accountId = a.id
        LEFT JOIN categories c ON t.categoryId = c.id
        LEFT JOIN subcategories s ON t.subcategoryId = s.id
        LEFT JOIN accounts da ON t.destinationAccountId = da.id
        WHERE (:accountId IS NULL OR t.accountId = :accountId OR t.destinationAccountId = :accountId)
          AND (:categoryId IS NULL OR t.categoryId = :categoryId)
          AND (:type IS NULL OR t.type = :type)
        ORDER BY t.dateMillis DESC, t.id DESC
        LIMIT :limit
    """)
    fun observeFilteredTransactions(
        accountId: Long? = null,
        categoryId: Long? = null,
        type: TransactionType? = null,
        limit: Int = 100
    ): Flow<List<TransactionDetail>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    // --- Query Statistiche per Leonardo & Dashboard ---
    @Query("""
        SELECT 
            c.id AS categoryId,
            c.name AS categoryName,
            c.colorHex AS colorHex,
            SUM(t.amount) AS totalAmount,
            COUNT(t.id) AS count
        FROM transactions t
        JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'EXPENSE'
          AND t.dateMillis >= :fromMillis
          AND t.dateMillis <= :toMillis
        GROUP BY c.id
        ORDER BY totalAmount DESC
    """)
    fun observeExpensesByCategoryBetween(fromMillis: Long, toMillis: Long): Flow<List<CategoryStat>>

    @Query("""
        SELECT 
            c.id AS categoryId,
            c.name AS categoryName,
            c.colorHex AS colorHex,
            SUM(t.amount) AS totalAmount,
            COUNT(t.id) AS count
        FROM transactions t
        JOIN categories c ON t.categoryId = c.id
        WHERE t.type = :type
          AND t.dateMillis >= :fromMillis
          AND t.dateMillis <= :toMillis
        GROUP BY c.id
        ORDER BY totalAmount DESC
    """)
    fun observeTransactionsByCategoryBetween(type: TransactionType, fromMillis: Long, toMillis: Long): Flow<List<CategoryStat>>

    @Query("""
        SELECT 
            c.id AS categoryId,
            c.name AS categoryName,
            c.colorHex AS colorHex,
            SUM(t.amount) AS totalAmount,
            COUNT(t.id) AS count
        FROM transactions t
        JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'EXPENSE'
          AND t.dateMillis >= :fromMillis
          AND t.dateMillis <= :toMillis
        GROUP BY c.id
        ORDER BY totalAmount DESC
    """)
    suspend fun getExpensesByCategoryBetween(fromMillis: Long, toMillis: Long): List<CategoryStat>

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0.0) AS totalIncome,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0.0) AS totalExpense,
            (
                COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0.0)
                - COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0.0)
            ) AS netCashflow
        FROM transactions
        WHERE dateMillis >= :fromMillis AND dateMillis <= :toMillis
    """)
    fun observeMonthlyOverview(fromMillis: Long, toMillis: Long): Flow<MonthlyOverview>

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0.0) AS totalIncome,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0.0) AS totalExpense,
            (
                COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0.0)
                - COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0.0)
            ) AS netCashflow
        FROM transactions
        WHERE dateMillis >= :fromMillis AND dateMillis <= :toMillis
    """)
    suspend fun getMonthlyOverview(fromMillis: Long, toMillis: Long): MonthlyOverview
}

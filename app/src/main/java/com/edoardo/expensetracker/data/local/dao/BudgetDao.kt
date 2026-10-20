package com.edoardo.expensetracker.data.local.dao

import androidx.room.*
import com.edoardo.expensetracker.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

data class BudgetWithProgress(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String,
    val yearMonth: String,
    val amountLimit: Double,
    val alertThreshold: Double,
    val currentSpent: Double
) {
    val percentageUsed: Double
        get() = if (amountLimit > 0) (currentSpent / amountLimit) * 100 else 0.0

    val isOverBudget: Boolean
        get() = currentSpent > amountLimit

    val isNearBudgetLimit: Boolean
        get() = percentageUsed >= (alertThreshold * 100)
}

@Dao
interface BudgetDao {

    @Query("""
        SELECT 
            b.id, b.categoryId, c.name AS categoryName, c.colorHex AS categoryColor,
            b.yearMonth, b.amountLimit, b.alertThreshold,
            COALESCE((
                SELECT SUM(t.amount) 
                FROM transactions t 
                WHERE t.categoryId = b.categoryId 
                  AND t.type = 'EXPENSE'
                  AND t.dateMillis >= :monthStartMillis 
                  AND t.dateMillis <= :monthEndMillis
            ), 0.0) AS currentSpent
        FROM budgets b
        JOIN categories c ON b.categoryId = c.id
        WHERE b.yearMonth = :yearMonth
        ORDER BY b.amountLimit DESC
    """)
    fun observeBudgetsWithProgress(
        yearMonth: String,
        monthStartMillis: Long,
        monthEndMillis: Long
    ): Flow<List<BudgetWithProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)
}

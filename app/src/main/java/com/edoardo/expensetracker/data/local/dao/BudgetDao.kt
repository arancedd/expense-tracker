package com.edoardo.expensetracker.data.local.dao

import androidx.room.*
import com.edoardo.expensetracker.data.local.entity.BudgetEntity
import com.edoardo.expensetracker.data.local.entity.ExpectedIncomeEntity
import com.edoardo.expensetracker.data.local.entity.SavingsBudgetEntity
import com.edoardo.expensetracker.data.local.entity.SubcategoryBudgetEntity
import kotlinx.coroutines.flow.Flow

data class BudgetWithProgress(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String,
    val categoryIcon: String = "category",
    val amountLimit: Double,
    val isPercentage: Boolean = false,
    val percentageValue: Double = 0.0,
    val alertThreshold: Double = 0.8,
    val hasSubcategoriesBudget: Boolean = false,
    val currentSpent: Double = 0.0
) {
    val percentageUsed: Double
        get() = if (amountLimit > 0) (currentSpent / amountLimit) * 100 else 0.0

    val isOverBudget: Boolean
        get() = currentSpent > amountLimit

    val isNearBudgetLimit: Boolean
        get() = percentageUsed >= (alertThreshold * 100)
}

data class SubcategoryBudgetWithProgress(
    val id: Long,
    val subcategoryId: Long,
    val categoryId: Long,
    val subcategoryName: String,
    val amountLimit: Double,
    val isPercentage: Boolean = false,
    val percentageValue: Double = 0.0,
    val currentSpent: Double = 0.0
) {
    val percentageUsed: Double
        get() = if (amountLimit > 0) (currentSpent / amountLimit) * 100 else 0.0

    val isOverBudget: Boolean
        get() = currentSpent > amountLimit
}

data class SavingsBudgetWithProgress(
    val id: Long,
    val accountId: Long,
    val accountName: String,
    val accountColor: String,
    val accountIcon: String = "savings",
    val amountLimit: Double,
    val isPercentage: Boolean = false,
    val percentageValue: Double = 0.0,
    val currentSaved: Double = 0.0
) {
    val percentageUsed: Double
        get() = if (amountLimit > 0) (currentSaved / amountLimit) * 100 else 0.0

    val isGoalReached: Boolean
        get() = currentSaved >= amountLimit
}

@Dao
interface BudgetDao {

    @Query("""
        SELECT 
            b.id, b.categoryId, c.name AS categoryName, c.colorHex AS categoryColor,
            c.iconName AS categoryIcon,
            b.amountLimit, b.isPercentage, b.percentageValue, b.alertThreshold,
            b.hasSubcategoriesBudget,
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
        WHERE b.amountLimit > 0
        ORDER BY b.amountLimit DESC
    """)
    fun observeBudgetsWithProgress(
        monthStartMillis: Long,
        monthEndMillis: Long
    ): Flow<List<BudgetWithProgress>>

    @Query("SELECT * FROM budgets")
    fun observeAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgets(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudgets(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets")
    suspend fun clearBudgets()

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    suspend fun deleteBudgetByCategoryId(categoryId: Long)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    // --- Subcategory Budgets ---
    @Query("""
        SELECT 
            sb.id, sb.subcategoryId, sb.categoryId, s.name AS subcategoryName,
            sb.amountLimit, sb.isPercentage, sb.percentageValue,
            COALESCE((
                SELECT SUM(t.amount) 
                FROM transactions t 
                WHERE t.subcategoryId = sb.subcategoryId 
                  AND t.type = 'EXPENSE'
                  AND t.dateMillis >= :monthStartMillis 
                  AND t.dateMillis <= :monthEndMillis
            ), 0.0) AS currentSpent
        FROM subcategory_budgets sb
        JOIN subcategories s ON sb.subcategoryId = s.id
        WHERE sb.amountLimit > 0
        ORDER BY sb.amountLimit DESC
    """)
    fun observeSubcategoryBudgetsWithProgress(
        monthStartMillis: Long,
        monthEndMillis: Long
    ): Flow<List<SubcategoryBudgetWithProgress>>

    @Query("SELECT * FROM subcategory_budgets")
    fun observeAllSubcategoryBudgets(): Flow<List<SubcategoryBudgetEntity>>

    @Query("SELECT * FROM subcategory_budgets")
    suspend fun getAllSubcategoryBudgets(): List<SubcategoryBudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSubcategoryBudgets(budgets: List<SubcategoryBudgetEntity>)

    @Query("DELETE FROM subcategory_budgets")
    suspend fun clearSubcategoryBudgets()

    // --- Savings Budgets (Conti Risparmio) ---
    @Query("""
        SELECT 
            sb.id, sb.accountId, a.name AS accountName, a.colorHex AS accountColor,
            a.iconName AS accountIcon,
            sb.amountLimit, sb.isPercentage, sb.percentageValue,
            COALESCE((
                SELECT SUM(t.amount) 
                FROM transactions t 
                WHERE ((t.type = 'TRANSFER' AND t.destinationAccountId = sb.accountId) OR (t.type = 'INCOME' AND t.accountId = sb.accountId))
                  AND t.dateMillis >= :monthStartMillis 
                  AND t.dateMillis <= :monthEndMillis
            ), 0.0) AS currentSaved
        FROM savings_budgets sb
        JOIN accounts a ON sb.accountId = a.id
        WHERE sb.amountLimit > 0
        ORDER BY sb.amountLimit DESC
    """)
    fun observeSavingsBudgetsWithProgress(
        monthStartMillis: Long,
        monthEndMillis: Long
    ): Flow<List<SavingsBudgetWithProgress>>

    @Query("SELECT * FROM savings_budgets")
    fun observeAllSavingsBudgets(): Flow<List<SavingsBudgetEntity>>

    @Query("SELECT * FROM savings_budgets")
    suspend fun getAllSavingsBudgets(): List<SavingsBudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSavingsBudgets(budgets: List<SavingsBudgetEntity>)

    @Query("DELETE FROM savings_budgets")
    suspend fun clearSavingsBudgets()

    // --- Entrate Previste ---
    @Query("SELECT * FROM expected_incomes")
    fun observeAllExpectedIncomes(): Flow<List<ExpectedIncomeEntity>>

    @Query("SELECT * FROM expected_incomes")
    suspend fun getAllExpectedIncomes(): List<ExpectedIncomeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateExpectedIncome(item: ExpectedIncomeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateExpectedIncomes(items: List<ExpectedIncomeEntity>)

    @Query("DELETE FROM expected_incomes")
    suspend fun clearExpectedIncomes()
}

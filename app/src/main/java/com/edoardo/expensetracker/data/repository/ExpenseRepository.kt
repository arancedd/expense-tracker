package com.edoardo.expensetracker.data.repository

import com.edoardo.expensetracker.data.local.dao.*
import com.edoardo.expensetracker.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) {

    // --- Accounts ---
    fun observeAccountsWithBalance(): Flow<List<AccountWithBalance>> =
        accountDao.observeAccountsWithBalance()

    fun observeAllAccountsWithBalance(): Flow<List<AccountWithBalance>> =
        accountDao.observeAllAccountsWithBalance()

    suspend fun getActiveAccounts(): List<AccountEntity> =
        accountDao.getActiveAccounts()

    suspend fun getAccountById(id: Long): AccountEntity? =
        accountDao.getAccountById(id)

    suspend fun insertAccount(account: AccountEntity): Long =
        accountDao.insertAccount(account)

    suspend fun updateAccount(account: AccountEntity) =
        accountDao.updateAccount(account)

    suspend fun deleteAccount(account: AccountEntity) =
        accountDao.deleteAccount(account)

    // --- Categories ---
    fun observeCategoriesWithType(type: TransactionType): Flow<List<CategoryWithSubcategories>> =
        categoryDao.observeCategoriesWithType(type)

    fun observeAllCategories(): Flow<List<CategoryWithSubcategories>> =
        categoryDao.observeAllCategories()

    suspend fun getCategoryById(id: Long): CategoryEntity? =
        categoryDao.getCategoryById(id)

    suspend fun insertCategory(category: CategoryEntity): Long =
        categoryDao.insertCategory(category)

    suspend fun updateCategory(category: CategoryEntity) =
        categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: CategoryEntity) =
        categoryDao.deleteCategory(category)

    suspend fun insertSubcategory(subcategory: SubcategoryEntity): Long =
        categoryDao.insertSubcategory(subcategory)

    suspend fun updateSubcategory(subcategory: SubcategoryEntity) =
        categoryDao.updateSubcategory(subcategory)

    suspend fun deleteSubcategory(subcategory: SubcategoryEntity) =
        categoryDao.deleteSubcategory(subcategory)

    // --- Transactions ---
    fun observeFilteredTransactions(
        accountId: Long? = null,
        categoryId: Long? = null,
        type: TransactionType? = null,
        limit: Int = 100
    ): Flow<List<TransactionDetail>> =
        transactionDao.observeFilteredTransactions(accountId, categoryId, type, limit)

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    suspend fun getTransactionById(id: Long): TransactionEntity? =
        transactionDao.getTransactionById(id)

    // --- Giroconto / Transfer ---
    suspend fun executeTransfer(
        sourceAccountId: Long,
        destinationAccountId: Long,
        amount: Double,
        dateMillis: Long = System.currentTimeMillis(),
        note: String? = null
    ): Long {
        val transferTx = TransactionEntity(
            accountId = sourceAccountId,
            destinationAccountId = destinationAccountId,
            type = TransactionType.TRANSFER,
            amount = amount,
            dateMillis = dateMillis,
            note = note,
            source = TransactionSource.MANUAL
        )
        return transactionDao.insertTransaction(transferTx)
    }

    // --- Statistiche & Overview ---
    fun observeCurrentMonthOverview(): Flow<MonthlyOverview> {
        val (start, end) = getMonthBounds()
        return transactionDao.observeMonthlyOverview(start, end)
    }

    fun observeCurrentMonthExpensesByCategory(): Flow<List<CategoryStat>> {
        val (start, end) = getMonthBounds()
        return transactionDao.observeExpensesByCategoryBetween(start, end)
    }

    fun observeCurrentMonthStatsByCategory(type: TransactionType): Flow<List<CategoryStat>> {
        val (start, end) = getMonthBounds()
        return transactionDao.observeTransactionsByCategoryBetween(type, start, end)
    }

    fun observePeriodOverview(fromMillis: Long, toMillis: Long): Flow<MonthlyOverview> =
        transactionDao.observeMonthlyOverview(fromMillis, toMillis)

    fun observePeriodStatsByCategory(type: TransactionType, fromMillis: Long, toMillis: Long): Flow<List<CategoryStat>> =
        transactionDao.observeTransactionsByCategoryBetween(type, fromMillis, toMillis)

    // --- Budget ---
    fun observeCurrentMonthBudgets(): Flow<List<BudgetWithProgress>> {
        val (start, end) = getMonthBounds()
        val yearMonth = getCurrentYearMonth()
        return budgetDao.observeBudgetsWithProgress(yearMonth, start, end)
    }

    suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long =
        budgetDao.insertOrUpdateBudget(budget)

    suspend fun deleteBudget(budget: BudgetEntity) =
        budgetDao.deleteBudget(budget)

    companion object {
        fun getMonthBounds(calendar: Calendar = Calendar.getInstance()): Pair<Long, Long> {
            val startCal = calendar.clone() as Calendar
            startCal.set(Calendar.DAY_OF_MONTH, 1)
            startCal.set(Calendar.HOUR_OF_DAY, 0)
            startCal.set(Calendar.MINUTE, 0)
            startCal.set(Calendar.SECOND, 0)
            startCal.set(Calendar.MILLISECOND, 0)

            val endCal = calendar.clone() as Calendar
            endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            endCal.set(Calendar.SECOND, 59)
            endCal.set(Calendar.MILLISECOND, 999)

            return Pair(startCal.timeInMillis, endCal.timeInMillis)
        }

        fun getCurrentYearMonth(calendar: Calendar = Calendar.getInstance()): String {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            return String.format("%04d-%02d", year, month)
        }
    }
}

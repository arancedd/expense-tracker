package com.edoardo.expensetracker.service.leonardo

import com.edoardo.expensetracker.data.local.dao.CategoryStat
import com.edoardo.expensetracker.data.local.dao.MonthlyOverview
import com.edoardo.expensetracker.data.local.dao.TransactionDao
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Calendar

@Serializable
data class CategoryExpenseStat(
    val categoryId: Long,
    val categoryName: String,
    val totalSpent: Double,
    val transactionCount: Int
)

@Serializable
data class LeonardoFinancialReport(
    val generatedAt: Long,
    val yearMonth: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netSavings: Double,
    val categoriesBreakdown: List<CategoryExpenseStat>
)

/**
 * Modulo Read-Only per Leonardo Assistant.
 * Leonardo si limita a consultare le statistiche aggregate e lo stato dei flussi di cassa,
 * senza alcun permesso di modifica, scrittura o eliminazione sul database locale.
 */
class LeonardoStatsExporter(
    private val transactionDao: TransactionDao
) {

    suspend fun generateMonthlyReport(year: Int, month: Int): LeonardoFinancialReport {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val fromMillis = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val toMillis = calendar.timeInMillis

        val overview: MonthlyOverview = transactionDao.getMonthlyOverview(fromMillis, toMillis)
        val categoryStats: List<CategoryStat> = transactionDao.getExpensesByCategoryBetween(fromMillis, toMillis)

        val yearMonthStr = String.format("%04d-%02d", year, month)

        return LeonardoFinancialReport(
            generatedAt = System.currentTimeMillis(),
            yearMonth = yearMonthStr,
            totalIncome = overview.totalIncome,
            totalExpense = overview.totalExpense,
            netSavings = overview.netCashflow,
            categoriesBreakdown = categoryStats.map {
                CategoryExpenseStat(
                    categoryId = it.categoryId,
                    categoryName = it.categoryName,
                    totalSpent = it.totalAmount,
                    transactionCount = it.count
                )
            }
        )
    }

    suspend fun exportReportToJsonFile(outputFile: File, year: Int, month: Int) {
        val report = generateMonthlyReport(year, month)
        val jsonString = Json { prettyPrint = true }.encodeToString(report)
        outputFile.writeText(jsonString)
    }
}

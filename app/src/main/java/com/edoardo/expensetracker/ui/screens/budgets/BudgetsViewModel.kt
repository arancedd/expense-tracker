package com.edoardo.expensetracker.ui.screens.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edoardo.expensetracker.data.local.dao.BudgetWithProgress
import com.edoardo.expensetracker.data.local.dao.CategoryStat
import com.edoardo.expensetracker.data.local.dao.CategoryWithSubcategories
import com.edoardo.expensetracker.data.local.entity.BudgetEntity
import com.edoardo.expensetracker.data.local.entity.TransactionType
import com.edoardo.expensetracker.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetsUiState(
    val categoryStats: List<CategoryStat> = emptyList(),
    val totalSpent: Double = 0.0,
    val budgets: List<BudgetWithProgress> = emptyList(),
    val availableCategories: List<CategoryWithSubcategories> = emptyList(),
    val isSetBudgetDialogOpen: Boolean = false,
    val selectedCategoryId: Long? = null,
    val budgetLimitInput: String = ""
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _dialogState = MutableStateFlow(
        Triple<Boolean, Long?, String>(false, null, "")
    )

    val uiState: StateFlow<BudgetsUiState> = combine(
        repository.observeCurrentMonthExpensesByCategory(),
        repository.observeCurrentMonthBudgets(),
        repository.observeCategoriesWithType(TransactionType.EXPENSE),
        _dialogState
    ) { stats, budgets, categories, dialog ->
        val totalSpent = stats.sumOf { it.totalAmount }

        BudgetsUiState(
            categoryStats = stats,
            totalSpent = totalSpent,
            budgets = budgets,
            availableCategories = categories,
            isSetBudgetDialogOpen = dialog.first,
            selectedCategoryId = dialog.second,
            budgetLimitInput = dialog.third
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BudgetsUiState()
    )

    fun openSetBudgetDialog(categoryId: Long? = null, currentLimit: Double = 0.0) {
        val limitStr = if (currentLimit > 0) currentLimit.toInt().toString() else ""
        _dialogState.value = Triple(true, categoryId, limitStr)
    }

    fun closeSetBudgetDialog() {
        _dialogState.value = Triple(false, null, "")
    }

    fun onBudgetLimitInputChange(input: String) {
        _dialogState.update { it.copy(third = input) }
    }

    fun onCategorySelectedForBudget(categoryId: Long) {
        _dialogState.update { it.copy(second = categoryId) }
    }

    fun saveBudget() {
        val (_, catId, limitStr) = _dialogState.value
        val limit = limitStr.toDoubleOrNull() ?: 0.0
        val targetCatId = catId ?: uiState.value.availableCategories.firstOrNull()?.category?.id ?: return

        if (limit > 0) {
            viewModelScope.launch {
                val yearMonth = ExpenseRepository.getCurrentYearMonth()
                repository.insertOrUpdateBudget(
                    BudgetEntity(
                        categoryId = targetCatId,
                        yearMonth = yearMonth,
                        amountLimit = limit
                    )
                )
                closeSetBudgetDialog()
            }
        }
    }
}

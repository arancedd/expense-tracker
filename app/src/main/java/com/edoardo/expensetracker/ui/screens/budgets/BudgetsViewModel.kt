package com.edoardo.expensetracker.ui.screens.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edoardo.expensetracker.data.local.dao.*
import com.edoardo.expensetracker.data.local.entity.*
import com.edoardo.expensetracker.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetsUiState(
    val categoryStats: List<CategoryStat> = emptyList(),
    val totalSpent: Double = 0.0,
    val budgets: List<BudgetWithProgress> = emptyList(),
    val subcategoryBudgets: List<SubcategoryBudgetWithProgress> = emptyList(),
    val savingsBudgets: List<SavingsBudgetWithProgress> = emptyList(),
    val savingsAccounts: List<AccountEntity> = emptyList(),
    val incomeCategories: List<CategoryWithSubcategories> = emptyList(),
    val expenseCategories: List<CategoryWithSubcategories> = emptyList(),
    val savedExpectedIncomes: List<ExpectedIncomeEntity> = emptyList(),
    val savedBudgets: List<BudgetEntity> = emptyList(),
    val savedSubcategoryBudgets: List<SubcategoryBudgetEntity> = emptyList(),
    val savedSavingsBudgets: List<SavingsBudgetEntity> = emptyList(),
    val isBudgetSheetOpen: Boolean = false
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _isBudgetSheetOpen = MutableStateFlow(false)

    private val statsAndBudgetsFlow = combine(
        repository.observeCurrentMonthExpensesByCategory(),
        repository.observeCurrentMonthBudgets(),
        repository.observeCurrentMonthSubcategoryBudgets()
    ) { stats, budgets, subBudgets -> Triple(stats, budgets, subBudgets) }

    private val categoriesFlow = combine(
        repository.observeCategoriesWithType(TransactionType.INCOME),
        repository.observeCategoriesWithType(TransactionType.EXPENSE)
    ) { inCats, exCats -> inCats to exCats }

    private val savingsFlow = combine(
        repository.observeSavingsAccounts(),
        repository.observeCurrentMonthSavingsBudgets()
    ) { accounts, savingsWithProgress -> accounts to savingsWithProgress }

    private val configsFlow = combine(
        repository.observeAllExpectedIncomes(),
        repository.observeAllBudgets(),
        repository.observeAllSubcategoryBudgets(),
        repository.observeAllSavingsBudgets(),
        _isBudgetSheetOpen
    ) { expIncomes, savedBudgets, savedSubBudgets, savedSavingsBudgets, isSheetOpen ->
        SavedConfigsBundle(expIncomes, savedBudgets, savedSubBudgets, savedSavingsBudgets, isSheetOpen)
    }

    val uiState: StateFlow<BudgetsUiState> = combine(
        statsAndBudgetsFlow,
        categoriesFlow,
        savingsFlow,
        configsFlow
    ) { (stats, budgets, subBudgets), (incomeCats, expenseCats), (savingsAccounts, savingsBudgets), configs ->
        val totalSpent = stats.sumOf { it.totalAmount }

        BudgetsUiState(
            categoryStats = stats,
            totalSpent = totalSpent,
            budgets = budgets,
            subcategoryBudgets = subBudgets,
            savingsBudgets = savingsBudgets,
            savingsAccounts = savingsAccounts,
            incomeCategories = incomeCats,
            expenseCategories = expenseCats,
            savedExpectedIncomes = configs.expectedIncomes,
            savedBudgets = configs.savedBudgets,
            savedSubcategoryBudgets = configs.savedSubBudgets,
            savedSavingsBudgets = configs.savedSavingsBudgets,
            isBudgetSheetOpen = configs.isSheetOpen
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BudgetsUiState()
    )

    fun openBudgetSheet() {
        _isBudgetSheetOpen.value = true
    }

    fun closeBudgetSheet() {
        _isBudgetSheetOpen.value = false
    }

    fun saveBudgetConfiguration(
        expectedIncomes: List<ExpectedIncomeEntity>,
        budgets: List<BudgetEntity>,
        subcategoryBudgets: List<SubcategoryBudgetEntity>,
        savingsBudgets: List<SavingsBudgetEntity>
    ) {
        viewModelScope.launch {
            repository.saveBudgetConfiguration(expectedIncomes, budgets, subcategoryBudgets, savingsBudgets)
            closeBudgetSheet()
        }
    }
}

private data class SavedConfigsBundle(
    val expectedIncomes: List<ExpectedIncomeEntity>,
    val savedBudgets: List<BudgetEntity>,
    val savedSubBudgets: List<SubcategoryBudgetEntity>,
    val savedSavingsBudgets: List<SavingsBudgetEntity>,
    val isSheetOpen: Boolean
)

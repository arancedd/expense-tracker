package com.edoardo.expensetracker.ui.screens.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edoardo.expensetracker.data.local.dao.AccountWithBalance
import com.edoardo.expensetracker.data.local.dao.CategoryWithSubcategories
import com.edoardo.expensetracker.data.local.entity.TransactionEntity
import com.edoardo.expensetracker.data.local.entity.TransactionSource
import com.edoardo.expensetracker.data.local.entity.TransactionType
import com.edoardo.expensetracker.data.repository.ExpenseRepository
import com.edoardo.expensetracker.ui.components.evaluateMathExpression
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuickAddUiState(
    val editingTransactionId: Long? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountExpression: String = "",
    val selectedAccountId: Long? = null,
    val selectedDestinationAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val selectedSubcategoryId: Long? = null,
    val payee: String = "",
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val accounts: List<AccountWithBalance> = emptyList(),
    val expenseCategories: List<CategoryWithSubcategories> = emptyList(),
    val incomeCategories: List<CategoryWithSubcategories> = emptyList(),
    val isSavedSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickAddUiState())
    val uiState: StateFlow<QuickAddUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAccountsWithBalance().collect { accounts ->
                val usableAccounts = accounts.filter {
                    it.account.accountType != com.edoardo.expensetracker.data.local.entity.AccountType.INVESTMENT
                }
                _uiState.update { current ->
                    current.copy(
                        accounts = usableAccounts,
                        selectedAccountId = current.selectedAccountId ?: usableAccounts.firstOrNull()?.account?.id,
                        selectedDestinationAccountId = current.selectedDestinationAccountId ?: usableAccounts.getOrNull(1)?.account?.id
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.observeCategoriesWithType(TransactionType.EXPENSE).collect { categories ->
                _uiState.update { current ->
                    current.copy(
                        expenseCategories = categories,
                        selectedCategoryId = if (current.type == TransactionType.EXPENSE && current.selectedCategoryId == null) {
                            categories.firstOrNull()?.category?.id
                        } else current.selectedCategoryId
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.observeCategoriesWithType(TransactionType.INCOME).collect { categories ->
                _uiState.update { current ->
                    current.copy(incomeCategories = categories)
                }
            }
        }
    }

    fun onTypeChange(newType: TransactionType) {
        _uiState.update { current ->
            val defaultCat = when (newType) {
                TransactionType.EXPENSE -> current.expenseCategories.firstOrNull()?.category?.id
                TransactionType.INCOME -> current.incomeCategories.firstOrNull()?.category?.id
                TransactionType.TRANSFER -> null
            }
            current.copy(type = newType, selectedCategoryId = defaultCat, selectedSubcategoryId = null)
        }
    }

    fun onAmountExpressionChange(newExpr: String) {
        _uiState.update { it.copy(amountExpression = newExpr, errorMessage = null) }
    }

    fun onAccountSelected(accountId: Long) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    fun onDestinationAccountSelected(accountId: Long) {
        _uiState.update { it.copy(selectedDestinationAccountId = accountId) }
    }

    fun onCategorySelected(categoryId: Long) {
        _uiState.update { it.copy(selectedCategoryId = categoryId, selectedSubcategoryId = null) }
    }

    fun onSubcategorySelected(subcategoryId: Long?) {
        _uiState.update { it.copy(selectedSubcategoryId = subcategoryId) }
    }

    fun onPayeeChange(payee: String) {
        _uiState.update { it.copy(payee = payee) }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun onDateChange(newMillis: Long) {
        _uiState.update { it.copy(dateMillis = newMillis) }
    }

    fun loadTransactionForEdit(transactionId: Long) {
        viewModelScope.launch {
            val tx = repository.getTransactionById(transactionId) ?: return@launch
            val exprStr = if (tx.amount % 1.0 == 0.0) {
                String.format(java.util.Locale.US, "%.0f", tx.amount)
            } else {
                String.format(java.util.Locale.US, "%.2f", tx.amount)
            }
            _uiState.update { current ->
                current.copy(
                    editingTransactionId = transactionId,
                    type = tx.type,
                    amountExpression = exprStr,
                    selectedAccountId = tx.accountId,
                    selectedDestinationAccountId = tx.destinationAccountId,
                    selectedCategoryId = tx.categoryId,
                    selectedSubcategoryId = tx.subcategoryId,
                    payee = tx.payee ?: "",
                    note = tx.note ?: "",
                    dateMillis = tx.dateMillis,
                    errorMessage = null
                )
            }
        }
    }

    fun deleteCurrentTransaction(onSuccess: () -> Unit) {
        val txId = _uiState.value.editingTransactionId ?: return
        viewModelScope.launch {
            val tx = repository.getTransactionById(txId)
            if (tx != null) {
                repository.deleteTransaction(tx)
            }
            resetState()
            onSuccess()
        }
    }

    fun duplicateCurrentTransaction(onSuccess: () -> Unit) {
        val txId = _uiState.value.editingTransactionId ?: return
        viewModelScope.launch {
            val tx = repository.getTransactionById(txId)
            if (tx != null) {
                val duplicate = tx.copy(id = 0, dateMillis = System.currentTimeMillis())
                repository.insertTransaction(duplicate)
            }
            resetState()
            onSuccess()
        }
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val state = _uiState.value
        val amount = evaluateMathExpression(state.amountExpression) ?: 0.0

        if (amount <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Inserisci un importo valido maggiore di 0") }
            return
        }

        val accountId = state.selectedAccountId
        if (accountId == null) {
            _uiState.update { it.copy(errorMessage = "Seleziona un conto") }
            return
        }

        viewModelScope.launch {
            val editingId = state.editingTransactionId
            if (editingId != null) {
                val existing = repository.getTransactionById(editingId)
                if (existing != null) {
                    val updated = existing.copy(
                        accountId = accountId,
                        type = state.type,
                        amount = amount,
                        categoryId = if (state.type == TransactionType.TRANSFER) null else state.selectedCategoryId,
                        subcategoryId = if (state.type == TransactionType.TRANSFER) null else state.selectedSubcategoryId,
                        destinationAccountId = if (state.type == TransactionType.TRANSFER) state.selectedDestinationAccountId else null,
                        payee = state.payee.takeIf { it.isNotBlank() },
                        note = state.note.takeIf { it.isNotBlank() },
                        dateMillis = state.dateMillis
                    )
                    repository.updateTransaction(updated)
                }
            } else {
                if (state.type == TransactionType.TRANSFER) {
                    val destId = state.selectedDestinationAccountId
                    if (destId == null || destId == accountId) {
                        _uiState.update { it.copy(errorMessage = "Seleziona un conto di destinazione differente") }
                        return@launch
                    }
                    repository.executeTransfer(
                        sourceAccountId = accountId,
                        destinationAccountId = destId,
                        amount = amount,
                        note = state.note.takeIf { it.isNotBlank() }
                    )
                } else {
                    val transaction = TransactionEntity(
                        accountId = accountId,
                        type = state.type,
                        amount = amount,
                        categoryId = state.selectedCategoryId,
                        subcategoryId = state.selectedSubcategoryId,
                        payee = state.payee.takeIf { it.isNotBlank() },
                        note = state.note.takeIf { it.isNotBlank() },
                        dateMillis = state.dateMillis,
                        source = TransactionSource.MANUAL
                    )
                    repository.insertTransaction(transaction)
                }
            }

            _uiState.update { it.copy(isSavedSuccess = true) }
            onSuccess()
        }
    }

    fun resetState() {
        _uiState.update { current ->
            current.copy(
                editingTransactionId = null,
                amountExpression = "",
                payee = "",
                note = "",
                dateMillis = System.currentTimeMillis(),
                isSavedSuccess = false,
                errorMessage = null
            )
        }
    }
}

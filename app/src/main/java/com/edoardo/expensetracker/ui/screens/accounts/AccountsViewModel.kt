package com.edoardo.expensetracker.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edoardo.expensetracker.data.local.dao.AccountWithBalance
import com.edoardo.expensetracker.data.local.entity.AccountEntity
import com.edoardo.expensetracker.data.local.entity.AccountType
import com.edoardo.expensetracker.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val totalWealth: Double = 0.0,
    val accountsTotal: Double = 0.0,
    val debtsTotal: Double = 0.0,
    val accountsCount: Int = 0,
    val debtsCount: Int = 0,
    val regularAccounts: List<AccountWithBalance> = emptyList(),
    val regularSubtotal: Double = 0.0,
    val savingsAccounts: List<AccountWithBalance> = emptyList(),
    val savingsSubtotal: Double = 0.0,
    val investmentAccounts: List<AccountWithBalance> = emptyList(),
    val investmentsSubtotal: Double = 0.0,
    val debtAccounts: List<AccountWithBalance> = emptyList(),
    val archivedAccounts: List<AccountWithBalance> = emptyList(),
    val isAddAccountDialogOpen: Boolean = false,
    val newAccountName: String = "",
    val newAccountType: AccountType = AccountType.REGULAR,
    val newAccountInitialBalance: String = "0.00"
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _dialogState = MutableStateFlow(Triple(false, "", AccountType.CHECKING))
    private val _initialBalanceInput = MutableStateFlow("0.00")

    val uiState: StateFlow<AccountsUiState> = combine(
        repository.observeAllAccountsWithBalance(),
        _dialogState,
        _initialBalanceInput
    ) { allAccounts, dialog, initialBal ->

        // Calcola il patrimonio totale sommando solo i conti attivi che hanno includeInTotalBalance = true
        val totalWealth = allAccounts
            .filter { it.account.isActive && it.account.includeInTotalBalance }
            .sumOf { it.currentBalance }

        val activeAccounts = allAccounts.filter { it.account.isActive }

        val regular = activeAccounts.filter {
            it.account.accountType in listOf(AccountType.REGULAR, AccountType.CHECKING, AccountType.CREDIT_CARD, AccountType.CASH)
        }
        val regularSubtotal = regular.sumOf { it.currentBalance }

        val savings = activeAccounts.filter {
            it.account.accountType in listOf(AccountType.SAVINGS, AccountType.CRYPTO)
        }
        val savingsSubtotal = savings.sumOf { it.currentBalance }

        val investments = activeAccounts.filter {
            it.account.accountType == AccountType.INVESTMENT
        }
        val investmentsSubtotal = investments.sumOf { it.currentBalance }

        val debts = activeAccounts.filter {
            it.account.accountType == AccountType.DEBT
        }
        val debtsTotal = debts.sumOf { it.currentBalance }
        val debtsCount = debts.size

        val accountsCount = regular.size + savings.size + investments.size
        val accountsTotal = regularSubtotal + savingsSubtotal + investmentsSubtotal

        val archived = allAccounts.filter { !it.account.isActive }

        AccountsUiState(
            accounts = allAccounts,
            totalWealth = totalWealth,
            accountsTotal = accountsTotal,
            debtsTotal = debtsTotal,
            accountsCount = accountsCount,
            debtsCount = debtsCount,
            regularAccounts = regular,
            regularSubtotal = regularSubtotal,
            savingsAccounts = savings,
            savingsSubtotal = savingsSubtotal,
            investmentAccounts = investments,
            investmentsSubtotal = investmentsSubtotal,
            debtAccounts = debts,
            archivedAccounts = archived,
            isAddAccountDialogOpen = dialog.first,
            newAccountName = dialog.second,
            newAccountType = dialog.third,
            newAccountInitialBalance = initialBal
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountsUiState())

    fun openAddAccountDialog() {
        _dialogState.value = Triple(true, "", AccountType.REGULAR)
        _initialBalanceInput.value = "0.00"
    }

    fun closeAddAccountDialog() {
        _dialogState.value = Triple(false, "", AccountType.REGULAR)
    }

    fun onAccountNameChange(name: String) {
        _dialogState.update { it.copy(second = name) }
    }

    fun onAccountTypeChange(type: AccountType) {
        _dialogState.update { it.copy(third = type) }
    }

    fun onInitialBalanceChange(balance: String) {
        _initialBalanceInput.value = balance
    }

    fun saveNewAccount(
        name: String,
        type: AccountType,
        colorHex: String = "#10B981",
        iconName: String = "account_balance",
        isActive: Boolean = true,
        includeInTotal: Boolean = true,
        initialBalance: Double = 0.0
    ) {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                repository.insertAccount(
                    AccountEntity(
                        name = name.trim(),
                        accountType = type,
                        initialBalance = initialBalance,
                        colorHex = colorHex,
                        iconName = iconName,
                        isActive = isActive,
                        includeInTotalBalance = includeInTotal
                    )
                )
                closeAddAccountDialog()
            }
        }
    }

    fun updateAccount(
        account: AccountEntity,
        name: String,
        type: AccountType,
        colorHex: String,
        iconName: String,
        isActive: Boolean,
        includeInTotalBalance: Boolean
    ) {
        viewModelScope.launch {
            val updated = account.copy(
                name = name,
                accountType = type,
                colorHex = colorHex,
                iconName = iconName,
                isActive = isActive,
                includeInTotalBalance = includeInTotalBalance
            )
            repository.updateAccount(updated)
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun adjustAccountBalance(accountWithBal: AccountWithBalance, targetBalance: Double) {
        val diff = targetBalance - accountWithBal.currentBalance
        if (diff == 0.0) return

        viewModelScope.launch {
            val acc = accountWithBal.account
            // Aggiorna l'initialBalance per allineare il conto
            val updated = acc.copy(initialBalance = acc.initialBalance + diff)
            repository.updateAccount(updated)
        }
    }

    fun unarchiveAccount(accountWithBal: AccountWithBalance) {
        viewModelScope.launch {
            repository.updateAccount(accountWithBal.account.copy(isActive = true))
        }
    }

    fun getTransactionsForAccount(accountId: Long): Flow<List<com.edoardo.expensetracker.data.local.dao.TransactionDetail>> =
        repository.observeFilteredTransactions(accountId = accountId, limit = 100)
}

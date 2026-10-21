package com.edoardo.expensetracker.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edoardo.expensetracker.data.local.dao.AccountWithBalance
import com.edoardo.expensetracker.data.local.dao.CategoryWithSubcategories
import com.edoardo.expensetracker.data.local.dao.TransactionDetail
import com.edoardo.expensetracker.data.local.entity.TransactionType
import com.edoardo.expensetracker.data.repository.ExpenseRepository
import com.edoardo.expensetracker.ui.screens.dashboard.TimePeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class DailyGroupedTransactions(
    val dateHeader: String,
    val dayTotalExpense: Double,
    val dayTotalIncome: Double,
    val transactions: List<TransactionDetail>
)

data class TransactionsUiState(
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedTypeFilter: TransactionType? = null,
    val selectedPeriod: TimePeriod? = null,
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val selectedRangeStartMillis: Long? = null,
    val selectedRangeEndMillis: Long? = null,
    val dateFilterLabel: String = "TUTTE LE DATE",
    val isDateFilterActive: Boolean = false,
    val selectedAccountIds: Set<Long> = emptySet(),
    val selectedCategoryIds: Set<Long> = emptySet(),
    val availableAccounts: List<AccountWithBalance> = emptyList(),
    val availableCategories: List<CategoryWithSubcategories> = emptyList(),
    val rawTransactions: List<TransactionDetail> = emptyList(),
    val groupedTransactions: List<DailyGroupedTransactions> = emptyList(),
    val isDatePickerOpen: Boolean = false
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    private val _selectedPeriod = MutableStateFlow<TimePeriod?>(null)
    private val _selectedDay = MutableStateFlow<Calendar?>(null)
    private val _selectedDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    private val _selectedAccountIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _selectedCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isDatePickerOpen = MutableStateFlow(false)

    val uiState: StateFlow<TransactionsUiState> = combine(
        combine(
            _searchQuery,
            _isSearchActive,
            _selectedType,
            _selectedPeriod,
            _selectedDay
        ) { query, isSearch, type, period, day ->
            FilterArgs1(query, isSearch, type, period, day)
        },
        combine(
            _selectedDateRange,
            _isDatePickerOpen,
            _selectedAccountIds,
            _selectedCategoryIds
        ) { range, isDateOpen, accIds, catIds ->
            FilterArgs2(range, isDateOpen, accIds, catIds)
        },
        combine(
            repository.observeAccountsWithBalance(),
            repository.observeAllCategories(),
            repository.observeFilteredTransactions(limit = 400)
        ) { accounts, categories, transactions ->
            FilterArgs3(accounts, categories, transactions)
        }
    ) { args1, args2, args3 ->
        val query = args1.query
        val isSearch = args1.isSearch
        val type = args1.type
        val period = args1.period
        val day = args1.day

        val range = args2.range
        val isDateOpen = args2.isDateOpen
        val accIds = args2.accIds
        val catIds = args2.catIds

        val accounts = args3.accounts
        val categories = args3.categories
        val transactions = args3.transactions

        val (fromMillis, toMillis, label, isDateActive) = calculateDateFilterBounds(period, day, range)

        val filtered = transactions.filter { tx ->
            val matchesType = type == null || tx.type == type
            val matchesAccount = accIds.isEmpty() ||
                    tx.accountId in accIds ||
                    (tx.destinationAccountId != null && tx.destinationAccountId in accIds)
            val matchesCategory = catIds.isEmpty() ||
                    (tx.categoryId != null && tx.categoryId in catIds)
            val matchesDate = tx.dateMillis in fromMillis..toMillis
            val matchesQuery = query.isBlank() ||
                    (tx.payee?.contains(query, ignoreCase = true) == true) ||
                    (tx.note?.contains(query, ignoreCase = true) == true) ||
                    (tx.categoryName?.contains(query, ignoreCase = true) == true) ||
                    (tx.accountName.contains(query, ignoreCase = true))

            matchesType && matchesAccount && matchesCategory && matchesDate && matchesQuery
        }

        val grouped = groupTransactionsByDay(filtered)

        TransactionsUiState(
            searchQuery = query,
            isSearchActive = isSearch || query.isNotBlank() || accIds.isNotEmpty() || catIds.isNotEmpty(),
            selectedTypeFilter = type,
            selectedPeriod = period,
            selectedDateMillis = day?.timeInMillis ?: System.currentTimeMillis(),
            selectedRangeStartMillis = range?.first,
            selectedRangeEndMillis = range?.second,
            dateFilterLabel = label,
            isDateFilterActive = isDateActive,
            selectedAccountIds = accIds,
            selectedCategoryIds = catIds,
            availableAccounts = accounts,
            availableCategories = categories,
            rawTransactions = filtered,
            groupedTransactions = grouped,
            isDatePickerOpen = isDateOpen
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TransactionsUiState()
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        if (newQuery.isNotBlank()) {
            _isSearchActive.value = true
        }
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
    }

    fun toggleSearchActive() {
        _isSearchActive.value = !_isSearchActive.value
    }

    fun toggleAccountFilter(accountId: Long) {
        val current = _selectedAccountIds.value.toMutableSet()
        if (current.contains(accountId)) {
            current.remove(accountId)
        } else {
            current.add(accountId)
        }
        _selectedAccountIds.value = current
    }

    fun toggleCategoryFilter(categoryId: Long) {
        val current = _selectedCategoryIds.value.toMutableSet()
        if (current.contains(categoryId)) {
            current.remove(categoryId)
        } else {
            current.add(categoryId)
        }
        _selectedCategoryIds.value = current
    }

    fun clearSearchAndFilters() {
        _searchQuery.value = ""
        _selectedAccountIds.value = emptySet()
        _selectedCategoryIds.value = emptySet()
        _isSearchActive.value = false
    }

    fun onTypeFilterChange(type: TransactionType?) {
        _selectedType.value = type
    }

    fun toggleDatePicker() {
        _isDatePickerOpen.value = !_isDatePickerOpen.value
    }

    fun closeDatePicker() {
        _isDatePickerOpen.value = false
    }

    fun onSelectPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
        _selectedDay.value = null
        _selectedDateRange.value = null
        _isDatePickerOpen.value = false
    }

    fun onSelectDay(calendar: Calendar) {
        _selectedPeriod.value = TimePeriod.DAY
        _selectedDay.value = calendar.clone() as Calendar
        _selectedDateRange.value = null
        _isDatePickerOpen.value = false
    }

    fun onSelectRange(startMillis: Long, endMillis: Long) {
        _selectedPeriod.value = null
        _selectedDay.value = null
        _selectedDateRange.value = Pair(startMillis, endMillis)
        _isDatePickerOpen.value = false
    }

    fun resetDateFilter() {
        _selectedPeriod.value = null
        _selectedDay.value = null
        _selectedDateRange.value = null
        _isDatePickerOpen.value = false
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            val tx = repository.getTransactionById(transactionId)
            if (tx != null) {
                repository.deleteTransaction(tx)
            }
        }
    }

    fun updateTransaction(
        transactionId: Long,
        newAmount: Double,
        newDateMillis: Long? = null,
        newNote: String? = null
    ) {
        viewModelScope.launch {
            val tx = repository.getTransactionById(transactionId) ?: return@launch
            repository.updateTransaction(
                tx.copy(
                    amount = newAmount,
                    dateMillis = newDateMillis ?: tx.dateMillis,
                    note = newNote ?: tx.note
                )
            )
        }
    }

    private fun calculateDateFilterBounds(
        period: TimePeriod?,
        day: Calendar?,
        range: Pair<Long, Long>?
    ): DateFilterResult {
        if (range != null) {
            val startFmt = SimpleDateFormat("d MMM", Locale.ITALY)
            val endFmt = SimpleDateFormat("d MMM yyyy", Locale.ITALY)
            val label = "${startFmt.format(Date(range.first))} - ${endFmt.format(Date(range.second))}".uppercase()
            return DateFilterResult(range.first, range.second, label, true)
        }

        if (day != null) {
            val start = (day.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = (day.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            val fmt = SimpleDateFormat("d MMMM yyyy", Locale.ITALY)
            return DateFilterResult(start, end, fmt.format(day.time).uppercase(), true)
        }

        val cal = Calendar.getInstance(Locale.ITALY).apply { firstDayOfWeek = Calendar.MONDAY }

        return when (period) {
            null -> DateFilterResult(0L, Long.MAX_VALUE, "TUTTE LE DATE", false)
            TimePeriod.DAY -> {
                val start = cal.clone() as Calendar
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)

                val end = cal.clone() as Calendar
                end.set(Calendar.HOUR_OF_DAY, 23)
                end.set(Calendar.MINUTE, 59)
                end.set(Calendar.SECOND, 59)
                end.set(Calendar.MILLISECOND, 999)
                DateFilterResult(start.timeInMillis, end.timeInMillis, "OGGI", true)
            }
            TimePeriod.WEEK -> {
                val start = cal.clone() as Calendar
                start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)

                val end = start.clone() as Calendar
                end.add(Calendar.DAY_OF_WEEK, 6)
                end.set(Calendar.HOUR_OF_DAY, 23)
                end.set(Calendar.MINUTE, 59)
                end.set(Calendar.SECOND, 59)
                end.set(Calendar.MILLISECOND, 999)
                DateFilterResult(start.timeInMillis, end.timeInMillis, "QUESTA SETTIMANA", true)
            }
            TimePeriod.MONTH -> {
                val start = cal.clone() as Calendar
                start.set(Calendar.DAY_OF_MONTH, 1)
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)

                val end = cal.clone() as Calendar
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                end.set(Calendar.HOUR_OF_DAY, 23)
                end.set(Calendar.MINUTE, 59)
                end.set(Calendar.SECOND, 59)
                end.set(Calendar.MILLISECOND, 999)
                val fmt = SimpleDateFormat("MMMM yyyy", Locale.ITALY)
                DateFilterResult(start.timeInMillis, end.timeInMillis, fmt.format(cal.time).uppercase(), true)
            }
            TimePeriod.YEAR -> {
                val start = cal.clone() as Calendar
                start.set(Calendar.DAY_OF_YEAR, 1)
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)

                val end = cal.clone() as Calendar
                end.set(Calendar.DAY_OF_YEAR, end.getActualMaximum(Calendar.DAY_OF_YEAR))
                end.set(Calendar.HOUR_OF_DAY, 23)
                end.set(Calendar.MINUTE, 59)
                end.set(Calendar.SECOND, 59)
                end.set(Calendar.MILLISECOND, 999)
                DateFilterResult(start.timeInMillis, end.timeInMillis, "ANNO ${cal.get(Calendar.YEAR)}", true)
            }
        }
    }

    private fun groupTransactionsByDay(transactions: List<TransactionDetail>): List<DailyGroupedTransactions> {
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ITALY)
        val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.ITALY)

        val groups = transactions.groupBy { tx ->
            dayKeyFormat.format(Date(tx.dateMillis))
        }

        return groups.map { (_, txList) ->
            val firstTxDate = Date(txList.first().dateMillis)
            val headerTitle = dateFormat.format(firstTxDate).replaceFirstChar { it.uppercase() }

            val totalExpense = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val totalIncome = txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

            DailyGroupedTransactions(
                dateHeader = headerTitle,
                dayTotalExpense = totalExpense,
                dayTotalIncome = totalIncome,
                transactions = txList
            )
        }
    }
}

private data class DateFilterResult(
    val fromMillis: Long,
    val toMillis: Long,
    val label: String,
    val isDateActive: Boolean
)

private data class FilterArgs1(
    val query: String,
    val isSearch: Boolean,
    val type: TransactionType?,
    val period: TimePeriod?,
    val day: Calendar?
)

private data class FilterArgs2(
    val range: Pair<Long, Long>?,
    val isDateOpen: Boolean,
    val accIds: Set<Long>,
    val catIds: Set<Long>
)

private data class FilterArgs3(
    val accounts: List<AccountWithBalance>,
    val categories: List<CategoryWithSubcategories>,
    val transactions: List<TransactionDetail>
)

package com.edoardo.expensetracker.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edoardo.expensetracker.data.local.dao.AccountWithBalance
import com.edoardo.expensetracker.data.local.dao.CategoryStat
import com.edoardo.expensetracker.data.local.dao.CategoryWithSubcategories
import com.edoardo.expensetracker.data.local.dao.MonthlyOverview
import com.edoardo.expensetracker.data.local.entity.SubcategoryEntity
import com.edoardo.expensetracker.data.local.entity.TransactionType
import com.edoardo.expensetracker.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class TimePeriod(val title: String) {
    DAY("Giorno"),
    WEEK("Settimana"),
    MONTH("Mese"),
    YEAR("Anno")
}

data class PeriodRange(
    val period: TimePeriod,
    val fromMillis: Long,
    val toMillis: Long,
    val displayLabel: String,
    val subLabel: String,
    val daysCount: Int,
    val targetCalendar: Calendar
)

fun calculatePeriodRangeForDate(period: TimePeriod, baseCalendar: Calendar): PeriodRange {
    val cal = baseCalendar.clone() as Calendar
    cal.firstDayOfWeek = Calendar.MONDAY
    val now = Calendar.getInstance()

    val isCurrentDay = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    val isCurrentMonth = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
    val isCurrentYear = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)

    return when (period) {
        TimePeriod.DAY -> {
            val startCal = cal.clone() as Calendar
            startCal.set(Calendar.HOUR_OF_DAY, 0)
            startCal.set(Calendar.MINUTE, 0)
            startCal.set(Calendar.SECOND, 0)
            startCal.set(Calendar.MILLISECOND, 0)

            val endCal = cal.clone() as Calendar
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            endCal.set(Calendar.SECOND, 59)
            endCal.set(Calendar.MILLISECOND, 999)

            val fmt = SimpleDateFormat("d MMMM yyyy", Locale.ITALY)
            val label = fmt.format(cal.time).uppercase()
            PeriodRange(period, startCal.timeInMillis, endCal.timeInMillis, label, if (isCurrentDay) "Oggi" else "", 1, cal)
        }
        TimePeriod.WEEK -> {
            val startCal = cal.clone() as Calendar
            startCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            startCal.set(Calendar.HOUR_OF_DAY, 0)
            startCal.set(Calendar.MINUTE, 0)
            startCal.set(Calendar.SECOND, 0)
            startCal.set(Calendar.MILLISECOND, 0)

            val endCal = startCal.clone() as Calendar
            endCal.add(Calendar.DAY_OF_WEEK, 6)
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            endCal.set(Calendar.SECOND, 59)
            endCal.set(Calendar.MILLISECOND, 999)

            val startFmt = SimpleDateFormat("d MMM", Locale.ITALY)
            val endFmt = SimpleDateFormat("d MMM yyyy", Locale.ITALY)
            val label = "${startFmt.format(startCal.time)} - ${endFmt.format(endCal.time)}".uppercase()

            val isCurrentWeek = now.timeInMillis in startCal.timeInMillis..endCal.timeInMillis
            PeriodRange(period, startCal.timeInMillis, endCal.timeInMillis, label, if (isCurrentWeek) "Questa settimana" else "", 7, cal)
        }
        TimePeriod.MONTH -> {
            val startCal = cal.clone() as Calendar
            startCal.set(Calendar.DAY_OF_MONTH, 1)
            startCal.set(Calendar.HOUR_OF_DAY, 0)
            startCal.set(Calendar.MINUTE, 0)
            startCal.set(Calendar.SECOND, 0)
            startCal.set(Calendar.MILLISECOND, 0)

            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val endCal = cal.clone() as Calendar
            endCal.set(Calendar.DAY_OF_MONTH, maxDays)
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            endCal.set(Calendar.SECOND, 59)
            endCal.set(Calendar.MILLISECOND, 999)

            val fmt = SimpleDateFormat("MMMM yyyy", Locale.ITALY)
            val label = fmt.format(cal.time).uppercase()
            val elapsedDays = if (isCurrentMonth) now.get(Calendar.DAY_OF_MONTH) else maxDays

            PeriodRange(period, startCal.timeInMillis, endCal.timeInMillis, label, if (isCurrentMonth) "Questo mese" else "", maxOf(1, elapsedDays), cal)
        }
        TimePeriod.YEAR -> {
            val startCal = cal.clone() as Calendar
            startCal.set(Calendar.DAY_OF_YEAR, 1)
            startCal.set(Calendar.HOUR_OF_DAY, 0)
            startCal.set(Calendar.MINUTE, 0)
            startCal.set(Calendar.SECOND, 0)
            startCal.set(Calendar.MILLISECOND, 0)

            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
            val endCal = cal.clone() as Calendar
            endCal.set(Calendar.DAY_OF_YEAR, maxDays)
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            endCal.set(Calendar.SECOND, 59)
            endCal.set(Calendar.MILLISECOND, 999)

            val year = cal.get(Calendar.YEAR)
            val elapsedDays = if (isCurrentYear) now.get(Calendar.DAY_OF_YEAR) else maxDays

            PeriodRange(period, startCal.timeInMillis, endCal.timeInMillis, "ANNO $year", if (isCurrentYear) "Inizio - Fine Anno" else "", maxOf(1, elapsedDays), cal)
        }
    }
}

data class OneMoneyCategoryItem(
    val categoryId: Long,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val totalSpent: Double,
    val subcategoryCount: Int = 0
)

data class DashboardUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val totalWealth: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netCashflow: Double = 0.0,
    val dailyExpenseAverage: Double = 0.0,
    val currentType: TransactionType = TransactionType.EXPENSE,
    val selectedPeriod: TimePeriod = TimePeriod.MONTH,
    val defaultPeriod: TimePeriod = TimePeriod.MONTH,
    val periodLabel: String = "",
    val periodSubLabel: String = "",
    val canGoForward: Boolean = false,
    val currentDateMillis: Long = System.currentTimeMillis(),
    val categoriesWithSpending: List<OneMoneyCategoryItem> = emptyList(),
    val categoryStats: List<CategoryStat> = emptyList(),
    val isEditMode: Boolean = false,
    val isCalendarPickerOpen: Boolean = false,
    val isCustomDayOrRange: Boolean = false,
    val selectedRangeStartMillis: Long? = null,
    val selectedRangeEndMillis: Long? = null,
    val selectedCategoryForEdit: CategoryWithSubcategories? = null,
    val isEditSheetOpen: Boolean = false
)

private data class DashboardCoreData(
    val accounts: List<AccountWithBalance>,
    val overview: MonthlyOverview,
    val categories: List<CategoryWithSubcategories>,
    val stats: List<CategoryStat>,
    val periodRange: PeriodRange
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE)

    private val _currentType = MutableStateFlow(TransactionType.EXPENSE)
    private val _defaultPeriod = MutableStateFlow(TimePeriod.MONTH)
    private val _selectedPeriod = MutableStateFlow(TimePeriod.MONTH)
    private val _targetCalendar = MutableStateFlow<Calendar>(Calendar.getInstance())
    private val _isEditMode = MutableStateFlow(false)
    private val _isCalendarPickerOpen = MutableStateFlow(false)
    private val _isCustomDayOrRange = MutableStateFlow(false)
    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    private val _selectedCategoryIdForEdit = MutableStateFlow<Long?>(null)
    private val _customOrder = MutableStateFlow<List<Long>>(loadSavedOrder())

    private fun loadSavedOrder(): List<Long> {
        val raw = prefs.getString("category_custom_order", null) ?: return emptyList()
        return raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    private fun saveOrder(order: List<Long>) {
        prefs.edit().putString("category_custom_order", order.joinToString(",")).apply()
    }

    private val coreDataFlow: Flow<DashboardCoreData> = combine(
        _selectedPeriod,
        _targetCalendar,
        _currentType,
        _customDateRange
    ) { period, cal, type, customRange ->
        val range = if (customRange != null) {
            val startFmt = SimpleDateFormat("d MMM", Locale.ITALY)
            val endFmt = SimpleDateFormat("d MMM yyyy", Locale.ITALY)
            val label = "${startFmt.format(Date(customRange.first))} - ${endFmt.format(Date(customRange.second))}".uppercase()
            val days = maxOf(1, ((customRange.second - customRange.first) / 86400000L).toInt() + 1)
            PeriodRange(period, customRange.first, customRange.second, label, "", days, cal)
        } else {
            calculatePeriodRangeForDate(period, cal)
        }
        Pair(range, type)
    }.flatMapLatest { (range, type) ->
        combine(
            repository.observeAccountsWithBalance(),
            repository.observePeriodOverview(range.fromMillis, range.toMillis),
            repository.observeCategoriesWithType(type),
            repository.observePeriodStatsByCategory(type, range.fromMillis, range.toMillis)
        ) { accounts, overview, categories, stats ->
            DashboardCoreData(accounts, overview, categories, stats, range)
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        coreDataFlow,
        _currentType,
        _selectedPeriod,
        _targetCalendar,
        _isEditMode,
        _isCalendarPickerOpen,
        _selectedCategoryIdForEdit,
        _customOrder
    ) { args: Array<Any?> ->
        val core = args[0] as DashboardCoreData
        val currentType = args[1] as TransactionType
        val selectedPeriod = args[2] as TimePeriod
        val targetCal = args[3] as Calendar
        val isEdit = args[4] as Boolean
        val isCalOpen = args[5] as Boolean
        val editId = args[6] as Long?
        @Suppress("UNCHECKED_CAST")
        val customOrder = args[7] as List<Long>

        val totalWealth = core.accounts
            .filter { it.account.includeInTotalBalance }
            .sumOf { it.currentBalance }
        val netCashflow = core.overview.netCashflow
        val dailyExpenseAverage = if (core.periodRange.daysCount > 0) {
            core.overview.totalExpense / core.periodRange.daysCount
        } else 0.0

        val statsMap = core.stats.associateBy { it.categoryId }

        val mappedCategories = core.categories.map { catWithSubs ->
            val cat = catWithSubs.category
            val spent = statsMap[cat.id]?.totalAmount ?: 0.0
            OneMoneyCategoryItem(
                categoryId = cat.id,
                name = cat.name,
                colorHex = cat.colorHex,
                iconName = cat.iconName,
                totalSpent = spent,
                subcategoryCount = catWithSubs.subcategories.size
            )
        }

        val sortedCategories = if (customOrder.isNotEmpty()) {
            val orderMap = customOrder.withIndex().associate { it.value to it.index }
            mappedCategories.sortedBy { orderMap[it.categoryId] ?: Int.MAX_VALUE }
        } else {
            mappedCategories
        }

        val selectedCat = if (editId != null) {
            core.categories.firstOrNull { it.category.id == editId }
        } else null

        val now = Calendar.getInstance()
        val canGoForward = targetCal.timeInMillis < now.timeInMillis && _customDateRange.value == null

        val customRange = _customDateRange.value

        DashboardUiState(
            accounts = core.accounts,
            totalWealth = totalWealth,
            totalExpense = core.overview.totalExpense,
            totalIncome = core.overview.totalIncome,
            netCashflow = netCashflow,
            dailyExpenseAverage = dailyExpenseAverage,
            currentType = currentType,
            selectedPeriod = selectedPeriod,
            defaultPeriod = _defaultPeriod.value,
            periodLabel = core.periodRange.displayLabel,
            periodSubLabel = core.periodRange.subLabel,
            canGoForward = canGoForward,
            currentDateMillis = targetCal.timeInMillis,
            categoriesWithSpending = sortedCategories,
            categoryStats = core.stats,
            isEditMode = isEdit,
            isCalendarPickerOpen = isCalOpen,
            isCustomDayOrRange = _isCustomDayOrRange.value,
            selectedRangeStartMillis = customRange?.first,
            selectedRangeEndMillis = customRange?.second,
            selectedCategoryForEdit = selectedCat,
            isEditSheetOpen = selectedCat != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun openCalendarPicker() {
        _isCalendarPickerOpen.value = true
    }

    fun closeCalendarPicker() {
        _isCalendarPickerOpen.value = false
    }

    fun toggleCalendarPicker() {
        _isCalendarPickerOpen.value = !_isCalendarPickerOpen.value
    }

    fun setPeriod(period: TimePeriod) {
        _defaultPeriod.value = period
        _selectedPeriod.value = period
        _targetCalendar.value = Calendar.getInstance()
        _isCustomDayOrRange.value = false
        _customDateRange.value = null
        closeCalendarPicker()
    }

    fun selectDateFromCalendar(calendar: Calendar) {
        _targetCalendar.value = calendar.clone() as Calendar
        _selectedPeriod.value = TimePeriod.DAY
        _isCustomDayOrRange.value = true
        _customDateRange.value = null
        closeCalendarPicker()
    }

    fun selectDateRange(startMillis: Long, endMillis: Long) {
        _customDateRange.value = Pair(startMillis, endMillis)
        _isCustomDayOrRange.value = true
        closeCalendarPicker()
    }

    fun resetToDefaultView() {
        _selectedPeriod.value = _defaultPeriod.value
        _targetCalendar.value = Calendar.getInstance()
        _isCustomDayOrRange.value = false
        _customDateRange.value = null
        closeCalendarPicker()
    }

    fun previousPeriod() {
        if (_customDateRange.value != null) {
            resetToDefaultView()
            return
        }
        val cal = _targetCalendar.value.clone() as Calendar
        when (_selectedPeriod.value) {
            TimePeriod.DAY -> cal.add(Calendar.DAY_OF_YEAR, -1)
            TimePeriod.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, -1)
            TimePeriod.MONTH -> cal.add(Calendar.MONTH, -1)
            TimePeriod.YEAR -> cal.add(Calendar.YEAR, -1)
        }
        _targetCalendar.value = cal
    }

    fun nextPeriod() {
        if (_customDateRange.value != null) {
            resetToDefaultView()
            return
        }
        val cal = _targetCalendar.value.clone() as Calendar
        val now = Calendar.getInstance()
        when (_selectedPeriod.value) {
            TimePeriod.DAY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            TimePeriod.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            TimePeriod.MONTH -> cal.add(Calendar.MONTH, 1)
            TimePeriod.YEAR -> cal.add(Calendar.YEAR, 1)
        }
        if (cal.timeInMillis <= now.timeInMillis + 86400000L) {
            _targetCalendar.value = cal
        }
    }

    fun toggleTransactionType() {
        _currentType.value = if (_currentType.value == TransactionType.EXPENSE) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }
    }

    fun setTransactionType(type: TransactionType) {
        _currentType.value = type
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun setEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    fun exitEditMode() {
        _isEditMode.value = false
    }

    val allCategoriesWithSubs: Flow<List<CategoryWithSubcategories>> = repository.observeAllCategories()

    fun getTransactionsForCategory(categoryId: Long): Flow<List<com.edoardo.expensetracker.data.local.dao.TransactionDetail>> {
        return repository.observeFilteredTransactions(categoryId = categoryId)
    }

    fun setCategoryBudget(categoryId: Long, limitAmount: Double) {
        viewModelScope.launch {
            val ym = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            repository.insertOrUpdateBudget(
                com.edoardo.expensetracker.data.local.entity.BudgetEntity(
                    categoryId = categoryId,
                    amountLimit = limitAmount,
                    yearMonth = ym
                )
            )
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            val existing = repository.getCategoryById(categoryId) ?: return@launch
            repository.deleteCategory(existing)
            closeEditCategory()
        }
    }

    fun openEditCategory(categoryId: Long) {
        _selectedCategoryIdForEdit.value = categoryId
    }

    fun closeEditCategory() {
        _selectedCategoryIdForEdit.value = null
    }

    fun moveCategory(fromIndex: Int, toIndex: Int) {
        val currentList = uiState.value.categoriesWithSpending.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices && fromIndex != toIndex) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            val newOrder = currentList.map { it.categoryId }
            _customOrder.value = newOrder
            saveOrder(newOrder)
        }
    }

    fun createCategory(
        name: String,
        colorHex: String = "#10B981",
        iconName: String = "category",
        type: TransactionType = _currentType.value
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val cat = com.edoardo.expensetracker.data.local.entity.CategoryEntity(
                name = name.trim(),
                type = type,
                colorHex = colorHex,
                iconName = iconName,
                isArchived = false
            )
            repository.insertCategory(cat)
        }
    }

    fun saveCategoryEdits(
        categoryId: Long,
        newName: String,
        newColorHex: String,
        newIconName: String,
        isArchived: Boolean
    ) {
        viewModelScope.launch {
            val existing = repository.getCategoryById(categoryId) ?: return@launch
            val updated = existing.copy(
                name = newName.trim(),
                colorHex = newColorHex,
                iconName = newIconName,
                isArchived = isArchived
            )
            repository.updateCategory(updated)
            closeEditCategory()
        }
    }

    fun addSubcategory(categoryId: Long, name: String, iconName: String = "label") {
        if (name.isBlank()) return
        viewModelScope.launch {
            val sub = SubcategoryEntity(
                categoryId = categoryId,
                name = name.trim(),
                iconName = iconName,
                isArchived = false
            )
            repository.insertSubcategory(sub)
        }
    }

    fun deleteSubcategory(sub: SubcategoryEntity) {
        viewModelScope.launch {
            repository.deleteSubcategory(sub)
        }
    }

    fun toggleSubcategoryArchived(sub: SubcategoryEntity) {
        viewModelScope.launch {
            repository.updateSubcategory(sub.copy(isArchived = !sub.isArchived))
        }
    }

    fun updateSubcategory(sub: SubcategoryEntity) {
        viewModelScope.launch {
            repository.updateSubcategory(sub)
        }
    }
}

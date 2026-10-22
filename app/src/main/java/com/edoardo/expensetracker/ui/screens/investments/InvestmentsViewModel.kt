package com.edoardo.expensetracker.ui.screens.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edoardo.expensetracker.data.local.dao.InvestmentTransactionWithAccount
import com.edoardo.expensetracker.data.local.dao.StockHolding
import com.edoardo.expensetracker.data.local.entity.AccountEntity
import com.edoardo.expensetracker.data.local.entity.AccountType
import com.edoardo.expensetracker.data.local.entity.InvestmentTransactionEntity
import com.edoardo.expensetracker.data.local.entity.InvestmentTransactionType
import com.edoardo.expensetracker.data.local.entity.StockEntity
import com.edoardo.expensetracker.data.repository.ExpenseRepository
import com.edoardo.expensetracker.service.investment.StockPriceService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class InvestmentTab {
    STOCKS,
    TRANSACTIONS
}

data class StockWithHolding(
    val stock: StockEntity,
    val sharesOwned: Double,
    val totalValue: Double
)

data class InvestmentsUiState(
    val hasInvestmentAccounts: Boolean = false,
    val investmentAccounts: List<AccountEntity> = emptyList(),
    val selectedTab: InvestmentTab = InvestmentTab.STOCKS,
    val stocksWithHoldings: List<StockWithHolding> = emptyList(),
    val totalPortfolioValue: Double = 0.0,
    val transactions: List<InvestmentTransactionWithAccount> = emptyList(),
    val allAvailableStocks: List<StockEntity> = emptyList(),
    val selectedStockForTrade: StockEntity? = null,
    val isFollowSheetOpen: Boolean = false,
    val isAddAccountOpen: Boolean = false,
    val isStreamingActive: Boolean = false
)

@HiltViewModel
class InvestmentsViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val stockPriceService: StockPriceService
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvestmentsUiState())
    val uiState: StateFlow<InvestmentsUiState> = _uiState.asStateFlow()

    init {
        // 1. Osserva se esistono conti di tipo INVESTMENT
        viewModelScope.launch {
            repository.observeInvestmentAccounts().collect { accounts ->
                _uiState.update { current ->
                    current.copy(
                        hasInvestmentAccounts = accounts.isNotEmpty(),
                        investmentAccounts = accounts
                    )
                }
            }
        }

        // 2. Osserva stock seguiti e quote possedute (Holdings)
        viewModelScope.launch {
            combine(
                repository.observeFollowedStocks(),
                repository.observeAllHoldings()
            ) { followedStocks, holdings ->
                val holdingMap = holdings.associate { it.symbol to it.totalShares }

                val items = followedStocks.map { stock ->
                    val shares = holdingMap[stock.symbol] ?: 0.0
                    val value = shares * stock.currentPrice
                    StockWithHolding(
                        stock = stock,
                        sharesOwned = shares,
                        totalValue = value
                    )
                }

                // Formula che addiziona tutti i valori dei miei stock
                val totalValue = items.sumOf { it.totalValue }

                Pair(items, totalValue)
            }.collect { (items, totalValue) ->
                _uiState.update { current ->
                    current.copy(
                        stocksWithHoldings = items,
                        totalPortfolioValue = totalValue
                    )
                }
            }
        }

        // 3. Osserva le transazioni di investimento
        viewModelScope.launch {
            repository.observeInvestmentTransactions().collect { txList ->
                _uiState.update { current ->
                    current.copy(transactions = txList)
                }
            }
        }

        // 4. Osserva tutti gli stock per il dialog "Segui"
        viewModelScope.launch {
            repository.observeAllStocks().collect { allStocks ->
                _uiState.update { current ->
                    current.copy(allAvailableStocks = allStocks)
                }
            }
        }

        // 5. Osserva lo stato dello streaming websocket
        viewModelScope.launch {
            stockPriceService.isStreaming.collect { active ->
                _uiState.update { it.copy(isStreamingActive = active) }
            }
        }
    }

    /**
     * Chiamato quando l'utente entra nella schermata Investimenti.
     */
    fun startStreaming() {
        stockPriceService.startStreaming()
    }

    /**
     * Chiamato quando l'utente esce dalla schermata Investimenti.
     */
    fun stopStreaming() {
        stockPriceService.stopStreaming()
    }

    fun setTab(tab: InvestmentTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun openTradeForStock(stock: StockEntity) {
        _uiState.update { it.copy(selectedStockForTrade = stock) }
    }

    fun closeTrade() {
        _uiState.update { it.copy(selectedStockForTrade = null) }
    }

    fun openFollowSheet() {
        _uiState.update { it.copy(isFollowSheetOpen = true) }
    }

    fun closeFollowSheet() {
        _uiState.update { it.copy(isFollowSheetOpen = false) }
    }

    fun openAddAccount() {
        _uiState.update { it.copy(isAddAccountOpen = true) }
    }

    fun closeAddAccount() {
        _uiState.update { it.copy(isAddAccountOpen = false) }
    }

    fun toggleFollowStock(symbol: String, isFollowed: Boolean) {
        viewModelScope.launch {
            repository.setStockFollowed(symbol, isFollowed)
        }
    }

    fun addNewStock(symbol: String, name: String, price: Double) {
        viewModelScope.launch {
            val stock = StockEntity(
                symbol = symbol.uppercase().trim(),
                name = name.trim().ifEmpty { symbol.uppercase().trim() },
                currentPrice = price,
                previousClosePrice = price,
                isFollowed = true
            )
            repository.insertStock(stock)
        }
    }

    fun executeTrade(
        accountId: Long,
        symbol: String,
        stockName: String,
        type: InvestmentTransactionType,
        shares: Double,
        pricePerShare: Double,
        totalAmount: Double,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val tx = InvestmentTransactionEntity(
                accountId = accountId,
                symbol = symbol,
                stockName = stockName,
                type = type,
                shares = shares,
                pricePerShare = pricePerShare,
                totalAmount = totalAmount,
                notes = notes
            )
            repository.insertInvestmentTransaction(tx)
            closeTrade()
        }
    }

    fun createInvestmentAccount(
        name: String,
        colorHex: String,
        iconName: String
    ) {
        viewModelScope.launch {
            val acc = AccountEntity(
                name = name.ifBlank { "Portafoglio Investimenti" },
                accountType = AccountType.INVESTMENT,
                initialBalance = 0.0,
                colorHex = colorHex,
                iconName = iconName,
                isActive = true,
                includeInTotalBalance = true
            )
            repository.insertAccount(acc)
            closeAddAccount()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stockPriceService.stopStreaming()
    }
}

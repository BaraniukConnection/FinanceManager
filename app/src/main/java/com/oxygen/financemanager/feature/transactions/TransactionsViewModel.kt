package com.oxygen.financemanager.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygen.financemanager.data.db.AccountDao
import com.oxygen.financemanager.data.db.CategoryDao
import com.oxygen.financemanager.data.db.TransactionDao
import com.oxygen.financemanager.data.db.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    accounts: AccountDao,
    categoryDao: CategoryDao,
    transactions: TransactionDao,
) : ViewModel() {

    private val filter = MutableStateFlow(TransactionFilter())
    private val categories = categoryDao.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val state: StateFlow<TransactionsState> = combine(
        filter, accounts.observeAll(), categories, transactions.observeAll(),
    ) { f, a, c, t ->
        buildTransactionsState(f, a, c, t, LocalDate.now(), ZoneId.systemDefault())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsState())

    fun setPeriod(period: Period) = filter.update { it.copy(period = period) }
    fun setType(type: TransactionType?) = filter.update { it.withType(type, categories.value) }
    fun setAccount(id: Long?) = filter.update { it.copy(accountId = id) }
    fun setCategory(id: Long?) = filter.update { it.copy(categoryId = id) }

    /** Що не видно — те не діє: згортання додаткових фільтрів їх скидає. */
    fun clearSecondary() = filter.update { it.copy(accountId = null, categoryId = null) }
}

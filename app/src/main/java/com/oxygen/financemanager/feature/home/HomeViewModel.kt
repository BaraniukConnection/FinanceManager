package com.oxygen.financemanager.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygen.financemanager.data.db.AccountDao
import com.oxygen.financemanager.data.db.CategoryDao
import com.oxygen.financemanager.data.db.TransactionDao
import com.oxygen.financemanager.data.db.TransactionType
import com.oxygen.financemanager.data.prefs.AppPreferences
import com.oxygen.financemanager.feature.stats.StatsState
import com.oxygen.financemanager.feature.stats.buildStats
import com.oxygen.financemanager.feature.transactions.Period
import com.oxygen.financemanager.feature.transactions.TransactionFilter
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
class HomeViewModel @Inject constructor(
    accounts: AccountDao,
    categoryDao: CategoryDao,
    transactions: TransactionDao,
    prefs: AppPreferences,
) : ViewModel() {

    private val categories = categoryDao.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val state: StateFlow<HomeState> = combine(
        accounts.observeActive(),
        categories,
        transactions.observeAll(),
        prefs.excludeSavingsFromTotal,
    ) { a, c, t, excludeSavings ->
        buildHomeState(a, c, t, excludeSavingsFromTotal = excludeSavings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    // ---------- статистика ----------

    private val statsFilter = MutableStateFlow(TransactionFilter(Period.THIS_MONTH, TransactionType.EXPENSE))

    val stats: StateFlow<StatsState> = combine(
        statsFilter, accounts.observeAll(), categories, transactions.observeAll(),
    ) { f, a, c, t ->
        buildStats(f, a, c, t, LocalDate.now(), ZoneId.systemDefault())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())

    fun setStatsPeriod(period: Period) = statsFilter.update { it.copy(period = period) }

    /** Зміна типу скидає категорію, що з ним несумісна: «Зарплата» не фільтр для витрат. */
    fun setStatsType(type: TransactionType) = statsFilter.update { it.withType(type, categories.value) }

    fun setStatsAccount(id: Long?) = statsFilter.update { it.copy(accountId = id) }
    fun setStatsCategory(id: Long?) = statsFilter.update { it.copy(categoryId = id) }

    /** Що не видно — те не діє: згортання додаткових опцій скидає рахунок і категорію. */
    fun clearStatsSecondary() = statsFilter.update { it.copy(accountId = null, categoryId = null) }
}

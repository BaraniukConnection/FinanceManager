package com.oxygen.financemanager.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygen.financemanager.core.balanceMinor
import com.oxygen.financemanager.data.db.AccountDao
import com.oxygen.financemanager.data.db.TransactionDao
import com.oxygen.financemanager.feature.home.AccountSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    accounts: AccountDao,
    transactions: TransactionDao,
) : ViewModel() {

    val state: StateFlow<List<AccountSummary>> =
        combine(accounts.observeActive(), transactions.observeAll()) { list, txs ->
            list.map { AccountSummary(it, balanceMinor(it, txs)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

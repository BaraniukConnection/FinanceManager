package com.oxygen.financemanager.feature.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.oxygen.financemanager.core.minorToText
import com.oxygen.financemanager.data.db.AccountDao
import com.oxygen.financemanager.navigation.AccountEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountEditViewModel @Inject constructor(
    private val accounts: AccountDao,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val accountId = savedState.toRoute<AccountEditRoute>().accountId

    private val _form = MutableStateFlow(AccountForm())
    val form: StateFlow<AccountForm> = _form.asStateFlow()

    private val _showErrors = MutableStateFlow(false)
    val showErrors: StateFlow<Boolean> = _showErrors.asStateFlow()

    /** Скільки транзакцій зникне разом з рахунком — підтвердження має це назвати чесно. */
    private val _transactionCount = MutableStateFlow(0)
    val transactionCount: StateFlow<Int> = _transactionCount.asStateFlow()

    val isNew: Boolean get() = accountId == 0L

    init {
        if (!isNew) viewModelScope.launch {
            _transactionCount.value = accounts.transactionCount(accountId)
            accounts.byId(accountId)?.let { existing ->
                _form.value = AccountForm(
                    id = existing.id,
                    name = existing.name,
                    type = existing.type,
                    currency = existing.currency,
                    openingText = existing.openingBalanceMinor
                        .takeIf { it != 0L }?.let { minorToText(it, existing.currency) }.orEmpty(),
                    color = existing.color,
                    targetText = existing.targetMinor?.let { minorToText(it, existing.currency) }.orEmpty(),
                    dueDate = existing.dueDate,
                )
            }
        }
    }

    fun update(transform: (AccountForm) -> AccountForm) = _form.update(transform)

    fun save(onSaved: () -> Unit) {
        val entity = _form.value.toEntity()
        if (entity == null) {
            _showErrors.value = true
            return
        }
        viewModelScope.launch {
            if (isNew) accounts.insert(entity) else accounts.update(entity)
            onSaved()
        }
    }

    /** Видаляє рахунок разом з усіма його транзакціями, включно з переказами на нього. */
    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            accounts.deleteWithTransactions(accountId)
            onDone()
        }
    }

    /** М'яка альтернатива видаленню: рахунок ховається, історія лишається. */
    fun archive(onDone: () -> Unit) {
        viewModelScope.launch {
            accounts.byId(accountId)?.let { accounts.update(it.copy(archived = true)) }
            onDone()
        }
    }
}


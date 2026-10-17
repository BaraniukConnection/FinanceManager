package com.oxygen.financemanager.feature.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.oxygen.financemanager.core.balanceMinor
import com.oxygen.financemanager.data.db.AccountDao
import com.oxygen.financemanager.data.db.AccountType
import com.oxygen.financemanager.data.db.AccountEntity
import com.oxygen.financemanager.data.db.CategoryDao
import com.oxygen.financemanager.data.db.CategoryEntity
import com.oxygen.financemanager.data.db.TransactionDao
import com.oxygen.financemanager.data.db.TransactionEntity
import com.oxygen.financemanager.data.db.TransactionType
import com.oxygen.financemanager.feature.home.AccountSummary
import com.oxygen.financemanager.navigation.TransactionEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Створення і редагування транзакції: [TransactionEditRoute.transactionId] == 0 — нова. */
@HiltViewModel
class TransactionEditViewModel @Inject constructor(
    private val transactions: TransactionDao,
    accountDao: AccountDao,
    categoryDao: CategoryDao,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val transactionId = savedState.toRoute<TransactionEditRoute>().transactionId
    val isNew: Boolean get() = transactionId == 0L
    private var existing: TransactionEntity? = null

    private val _form = MutableStateFlow(TransactionForm())
    val form: StateFlow<TransactionForm> = _form.asStateFlow()

    /**
     * Активні рахунки — плюс ті, на яких ця транзакція вже стоїть, навіть якщо їх архівовано:
     * інакше стару транзакцію неможливо було б зберегти без зміни рахунку.
     */
    val accounts: StateFlow<List<AccountEntity>> = combine(accountDao.observeAll(), _form) { all, f ->
        all.filter { !it.archived || it.id == f.accountId || it.id == f.destinationId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Рахунки з балансами — для карток у шторці вибору, як на екрані рахунків. */
    val summaries: StateFlow<List<AccountSummary>> = combine(accounts, transactions.observeAll()) { list, txs ->
        list.map { AccountSummary(it, balanceMinor(it, txs)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _showErrors = MutableStateFlow(false)
    val showErrors: StateFlow<Boolean> = _showErrors.asStateFlow()

    init {
        // нова транзакція одразу стоїть на першому звичайному рахунку — зазвичай його й не треба міняти
        if (isNew) viewModelScope.launch {
            val list = accounts.first { it.isNotEmpty() }
            if (_form.value.accountId == null) {
                val default = list.firstOrNull { it.type != AccountType.SAVINGS } ?: list.first()
                _form.update { it.copy(accountId = default.id) }
            }
        }
        if (!isNew) viewModelScope.launch {
            transactions.byId(transactionId)?.let {
                existing = it
                _form.value = it.toForm()
            }
        }
    }

    /** Валюта транзакції — це валюта обраного рахунку, окремого вибору немає. */
    val currency: String? get() = accounts.value.firstOrNull { it.id == _form.value.accountId }?.currency

    fun update(transform: (TransactionForm) -> TransactionForm) = _form.update(transform)

    fun setType(type: TransactionType) = _form.update { it.withType(type, categories.value) }
    fun setAccount(id: Long) = _form.update { it.withAccount(id) }

    fun delete(onDone: () -> Unit) {
        val original = existing ?: return
        viewModelScope.launch {
            transactions.delete(original)
            onDone()
        }
    }

    fun save(onSaved: () -> Unit) {
        val entity = currency?.let { _form.value.toEntity(it) }
        if (entity == null) {
            _showErrors.value = true
            return
        }
        viewModelScope.launch {
            val original = existing
            // id і час створення — від оригіналу, інакше редагування стало б новою транзакцією
            if (original == null) transactions.insert(entity)
            else transactions.update(entity.copy(id = original.id, createdAt = original.createdAt))
            onSaved()
        }
    }
}

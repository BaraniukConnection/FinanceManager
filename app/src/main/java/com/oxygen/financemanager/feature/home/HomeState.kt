package com.oxygen.financemanager.feature.home

import com.oxygen.financemanager.core.balanceMinor
import com.oxygen.financemanager.core.signedMinorFor
import com.oxygen.financemanager.data.db.AccountEntity
import com.oxygen.financemanager.data.db.AccountType
import com.oxygen.financemanager.data.db.CategoryEntity
import com.oxygen.financemanager.data.db.TransactionEntity

data class AccountSummary(val account: AccountEntity, val balanceMinor: Long)

/** Транзакція разом з назвами, яких немає в самому рядку. */
data class TransactionRow(
    val transaction: TransactionEntity,
    val categoryName: String?,
    val categoryColor: Int?,
    val categoryIcon: String?,
    val accountName: String,
    val counterpartyName: String?,
    /** Скільки ця транзакція додала або відняла з погляду [accountName]. */
    val signedMinor: Long,
)

data class HomeState(
    val totals: List<CurrencyTotal> = emptyList(),
    val accounts: List<AccountSummary> = emptyList(),
    val recent: List<TransactionRow> = emptyList(),
    val loading: Boolean = true,
) {
    val isEmpty: Boolean get() = !loading && accounts.isEmpty()

    /** Картки в каруселі — усе, крім скарбничок: ті живуть у власній згортній панелі. */
    val spending: List<AccountSummary>
        get() = accounts.filter { it.account.type != AccountType.SAVINGS }

    val savings: List<AccountSummary>
        get() = accounts.filter { it.account.type == AccountType.SAVINGS }
}

data class CurrencyTotal(val currency: String, val amountMinor: Long)

/** Скільки останніх тримає Home: показує [RECENT_COLLAPSED], решту — після «Показати більше». */
const val RECENT_LIMIT = 20
const val RECENT_COLLAPSED = 5

/**
 * Зводить сирі таблиці в те, що показує Home.
 *
 * Підсумки групуються **за валютою**, а не конвертуються в базову: курсів у
 * базі ще немає, і вигадувати їх, аби показати одне число, — гірше, ніж
 * показати два чесних.
 */
fun buildHomeState(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    recentLimit: Int = RECENT_LIMIT,
    excludeSavingsFromTotal: Boolean = false,
): HomeState {
    val summaries = accounts.map { AccountSummary(it, balanceMinor(it, transactions)) }

    val totals = summaries
        .filterNot { excludeSavingsFromTotal && it.account.type == AccountType.SAVINGS }
        .groupBy { it.account.currency }
        .map { (currency, group) -> CurrencyTotal(currency, group.sumOf { it.balanceMinor }) }
        .sortedByDescending { it.amountMinor }

    val recent = buildRows(transactions.sortedByDescending { it.occurredAt }.take(recentLimit), accounts, categories)

    return HomeState(totals = totals, accounts = summaries, recent = recent, loading = false)
}

/** Доклеює до транзакцій назви й вигляд рахунків та категорій. Порядок вхідного списку зберігається. */
fun buildRows(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
): List<TransactionRow> {
    val accountNames = accounts.associate { it.id to it.name }
    val categoryById = categories.associateBy { it.id }
    return transactions.map { tx ->
        val category = tx.categoryId?.let(categoryById::get)
        TransactionRow(
            transaction = tx,
            categoryName = category?.name,
            categoryColor = category?.color,
            categoryIcon = category?.icon,
            accountName = accountNames[tx.accountId].orEmpty(),
            counterpartyName = tx.transferAccountId?.let(accountNames::get),
            signedMinor = signedMinorFor(tx.accountId, tx),
        )
    }
}

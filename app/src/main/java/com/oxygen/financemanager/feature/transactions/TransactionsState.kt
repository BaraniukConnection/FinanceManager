package com.oxygen.financemanager.feature.transactions

import com.oxygen.financemanager.data.db.AccountEntity
import com.oxygen.financemanager.data.db.CategoryEntity
import com.oxygen.financemanager.data.db.CategoryKind
import com.oxygen.financemanager.data.db.TransactionEntity
import com.oxygen.financemanager.data.db.TransactionType
import com.oxygen.financemanager.feature.home.CurrencyTotal
import com.oxygen.financemanager.feature.home.TransactionRow
import com.oxygen.financemanager.feature.home.buildRows
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class Period { THIS_MONTH, LAST_MONTH, THIS_YEAR, ALL }

/** Напіввідкритий інтервал [from, until): початок включно, кінець — ні, щоб межі місяців не перетинались. */
data class TimeRange(val from: Instant, val until: Instant) {
    operator fun contains(instant: Instant) = instant >= from && instant < until
}

/** `null` — «увесь час», без меж. Межі — опівночі в поясі користувача, а не в UTC. */
fun Period.range(today: LocalDate, zone: ZoneId): TimeRange? {
    val (start, end) = when (this) {
        Period.THIS_MONTH -> today.withDayOfMonth(1).let { it to it.plusMonths(1) }
        Period.LAST_MONTH -> today.withDayOfMonth(1).minusMonths(1).let { it to it.plusMonths(1) }
        Period.THIS_YEAR -> today.withDayOfYear(1).let { it to it.plusYears(1) }
        Period.ALL -> return null
    }
    return TimeRange(start.atStartOfDay(zone).toInstant(), end.atStartOfDay(zone).toInstant())
}

data class TransactionFilter(
    val period: Period = Period.THIS_MONTH,
    /** `null` — усі типи. */
    val type: TransactionType? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
) {
    val hasSecondary: Boolean get() = accountId != null || categoryId != null

    /**
     * Зміна типу скидає категорію, яка з ним більше не сумісна: у переказу категорій немає,
     * а «Зарплата» не може лишитись фільтром для витрат.
     */
    fun withType(newType: TransactionType?, categories: List<CategoryEntity>): TransactionFilter {
        val kind = categories.firstOrNull { it.id == categoryId }?.kind
        val keepCategory = when (newType) {
            null -> true
            TransactionType.TRANSFER -> false
            TransactionType.EXPENSE -> kind == CategoryKind.EXPENSE
            TransactionType.INCOME -> kind == CategoryKind.INCOME
        }
        return copy(type = newType, categoryId = categoryId.takeIf { keepCategory })
    }
}

/**
 * Чи проходить транзакція фільтр.
 *
 * Рахунок збігається і тоді, коли він отримувач переказу — інакше історія скарбнички
 * була б порожньою. [categoryScope] — обрана категорія разом з її підкатегоріями.
 */
fun TransactionEntity.matches(filter: TransactionFilter, range: TimeRange?, categoryScope: Set<Long>?): Boolean {
    if (range != null && occurredAt !in range) return false
    if (filter.type != null && type != filter.type) return false
    if (filter.accountId != null && accountId != filter.accountId && transferAccountId != filter.accountId) return false
    if (categoryScope != null && categoryId !in categoryScope) return false
    return true
}

data class DayGroup(val date: LocalDate, val rows: List<TransactionRow>)

data class TransactionsState(
    val filter: TransactionFilter = TransactionFilter(),
    val days: List<DayGroup> = emptyList(),
    /** Лише витрати й доходи: переказ — рух між своїми рахунками, а не витрата. */
    val spent: List<CurrencyTotal> = emptyList(),
    val received: List<CurrencyTotal> = emptyList(),
    /** Для чіпів — лише активні; архівні лишаються в історії, але не пропонуються як фільтр. */
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val loading: Boolean = true,
)

fun buildTransactionsState(
    filter: TransactionFilter,
    allAccounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    today: LocalDate,
    zone: ZoneId,
): TransactionsState {
    val range = filter.period.range(today, zone)
    val scope = filter.categoryId?.let { id -> setOf(id) + categories.filter { it.parentId == id }.map { it.id } }

    val matching = transactions
        .filter { it.matches(filter, range, scope) }
        .sortedByDescending { it.occurredAt }

    // назви беремо з усіх рахунків, включно з архівними — історія не має втрачати підписи
    val days = buildRows(matching, allAccounts, categories)
        .groupBy { it.transaction.occurredAt.atZone(zone).toLocalDate() }
        .map { (date, rows) -> DayGroup(date, rows) }

    fun totalsOf(type: TransactionType) = matching
        .filter { it.type == type }
        .groupBy { it.currency }
        .map { (currency, group) -> CurrencyTotal(currency, group.sumOf { it.amountMinor }) }

    val chipCategories = categories.filter { category ->
        category.parentId == null && when (filter.type) {
            null -> true
            TransactionType.EXPENSE -> category.kind == CategoryKind.EXPENSE
            TransactionType.INCOME -> category.kind == CategoryKind.INCOME
            TransactionType.TRANSFER -> false
        }
    }

    return TransactionsState(
        filter = filter,
        days = days,
        spent = totalsOf(TransactionType.EXPENSE),
        received = totalsOf(TransactionType.INCOME),
        accounts = allAccounts.filterNot { it.archived },
        categories = chipCategories,
        loading = false,
    )
}

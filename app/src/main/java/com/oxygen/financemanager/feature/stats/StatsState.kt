package com.oxygen.financemanager.feature.stats

import com.oxygen.financemanager.data.db.AccountEntity
import com.oxygen.financemanager.data.db.CategoryEntity
import com.oxygen.financemanager.data.db.CategoryKind
import com.oxygen.financemanager.data.db.TransactionEntity
import com.oxygen.financemanager.data.db.TransactionType
import com.oxygen.financemanager.feature.transactions.TransactionFilter
import com.oxygen.financemanager.feature.transactions.matches
import com.oxygen.financemanager.feature.transactions.range
import java.time.LocalDate
import java.time.ZoneId

/**
 * Частка діаграми. [categoryId] == null і не [isOther] — «без категорії».
 * [isOther] — хвіст дрібних категорій, зібраний в одну частку.
 */
data class Slice(
    val categoryId: Long?,
    val name: String?,
    val color: Int?,
    val icon: String?,
    val amountMinor: Long,
    val isOther: Boolean = false,
)

/** Більше часток діаграма не показує — решта йде в «Інше», інакше кільце перетворюється на смужки. */
const val TOP_SLICES = 6

data class StatsState(
    val filter: TransactionFilter = TransactionFilter(type = TransactionType.EXPENSE),
    val slices: List<Slice> = emptyList(),
    val totalMinor: Long = 0,
    /** Валюта, в якій рахується діаграма. Різні валюти не складаються без курсу. */
    val currency: String? = null,
    /** Які ще валюти є за фільтром, але не показані — щоб про них не мовчати. */
    val otherCurrencies: List<String> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val loading: Boolean = true,
)

/**
 * Розподіл витрат або доходів за категоріями.
 *
 * Без фільтра категорії підкатегорії складаються у свою батьківську — на верхньому рівні
 * «Кафе» входить у «Продукти». З фільтром категорії — навпаки, розбивка вже по її
 * підкатегоріях, а власні транзакції батьківської лишаються окремою часткою.
 *
 * Перекази не рахуються взагалі: це рух між своїми рахунками, а не витрата й не дохід.
 */
fun buildStats(
    filter: TransactionFilter,
    allAccounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    today: LocalDate,
    zone: ZoneId,
    top: Int = TOP_SLICES,
): StatsState {
    // статистика буває лише витрат або доходів
    val type = filter.type?.takeIf { it != TransactionType.TRANSFER } ?: TransactionType.EXPENSE
    val f = filter.copy(type = type)
    val range = f.period.range(today, zone)
    val byId = categories.associateBy { it.id }
    val scope = f.categoryId?.let { id -> setOf(id) + categories.filter { it.parentId == id }.map { it.id } }

    val matching = transactions.filter { it.matches(f, range, scope) }

    // суми в різних валютах без курсу не складаються — беремо ту, де грошей найбільше
    val byCurrency = matching.groupBy { it.currency }
    val currency = byCurrency.maxByOrNull { (_, txs) -> txs.sumOf { it.amountMinor } }?.key
    val inCurrency = byCurrency[currency].orEmpty()

    fun sliceKey(tx: TransactionEntity): Long? {
        val category = tx.categoryId?.let(byId::get) ?: return null
        return if (f.categoryId != null) category.id else category.parentId ?: category.id
    }

    val grouped = inCurrency
        .groupBy(::sliceKey)
        .map { (key, txs) ->
            val category = key?.let(byId::get)
            Slice(key, category?.name, category?.color, category?.icon, txs.sumOf { it.amountMinor })
        }
        .sortedByDescending { it.amountMinor }

    val slices = if (grouped.size <= top) grouped else {
        val rest = grouped.drop(top - 1)
        grouped.take(top - 1) + Slice(null, null, null, null, rest.sumOf { it.amountMinor }, isOther = true)
    }

    val kind = if (type == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
    return StatsState(
        filter = f,
        slices = slices,
        totalMinor = slices.sumOf { it.amountMinor },
        currency = currency,
        otherCurrencies = byCurrency.keys.filter { it != currency }.sorted(),
        accounts = allAccounts.filterNot { it.archived },
        categories = categories.filter { it.parentId == null && it.kind == kind },
        loading = false,
    )
}

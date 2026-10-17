package com.oxygen.financemanager.core

import com.oxygen.financemanager.data.db.CategoryEntity
import com.oxygen.financemanager.data.db.CategoryKind
import com.oxygen.financemanager.data.db.TransactionEntity
import com.oxygen.financemanager.data.db.TransactionType
import com.oxygen.financemanager.feature.stats.buildStats
import com.oxygen.financemanager.feature.transactions.Period
import com.oxygen.financemanager.feature.transactions.TransactionFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StatsTest {

    private val zone = ZoneId.of("Europe/Berlin")
    private val today = LocalDate.of(2026, 9, 25)

    private fun cat(id: Long, parent: Long? = null, kind: CategoryKind = CategoryKind.EXPENSE) =
        CategoryEntity(id = id, name = "c$id", parentId = parent, kind = kind, color = 0, icon = "")

    private val food = cat(1)
    private val snacks = cat(2, parent = 1)
    private val drinks = cat(3, parent = 1)
    private val transport = cat(4)
    private val salary = cat(5, kind = CategoryKind.INCOME)
    private val categories = listOf(food, snacks, drinks, transport, salary)

    private var nextId = 1L
    private fun tx(
        amount: Long,
        category: Long?,
        type: TransactionType = TransactionType.EXPENSE,
        currency: String = "EUR",
        day: Int = 10,
        month: Int = 9,
    ) = TransactionEntity(
        id = nextId++, accountId = 1, transferAccountId = if (type == TransactionType.TRANSFER) 2 else null,
        amountMinor = amount, currency = currency, type = type, categoryId = category,
        occurredAt = LocalDateTime.of(2026, month, day, 12, 0).atZone(zone).toInstant(),
    )

    private fun stats(txs: List<TransactionEntity>, filter: TransactionFilter = TransactionFilter(Period.THIS_MONTH, TransactionType.EXPENSE), top: Int = 6) =
        buildStats(filter, emptyList(), categories, txs, today, zone, top)

    @Test
    fun `subcategories fold into their parent at the top level`() {
        val s = stats(listOf(tx(1000, food.id), tx(300, snacks.id), tx(200, drinks.id), tx(500, transport.id)))
        assertEquals(listOf(food.id to 1500L, transport.id to 500L), s.slices.map { it.categoryId to it.amountMinor })
        assertEquals(2000L, s.totalMinor)
    }

    @Test
    fun `filtering by a category breaks it down into its subcategories`() {
        val s = stats(
            listOf(tx(1000, food.id), tx(300, snacks.id), tx(200, drinks.id), tx(500, transport.id)),
            TransactionFilter(Period.THIS_MONTH, TransactionType.EXPENSE, categoryId = food.id),
        )
        // власні транзакції батьківської — окремою часткою, транспорт поза фільтром
        assertEquals(listOf(food.id to 1000L, snacks.id to 300L, drinks.id to 200L), s.slices.map { it.categoryId to it.amountMinor })
    }

    @Test
    fun `uncategorised spending is its own slice, not hidden`() {
        val s = stats(listOf(tx(700, null), tx(100, transport.id)))
        val none = s.slices.first()
        assertNull(none.categoryId)
        assertEquals(700L, none.amountMinor)
        assertTrue(!none.isOther)
    }

    @Test
    fun `transfers never count, and income is its own mode`() {
        val txs = listOf(
            tx(500, food.id),
            tx(99_999, null, TransactionType.TRANSFER),
            tx(250_000, salary.id, TransactionType.INCOME),
        )
        assertEquals(500L, stats(txs).totalMinor)
        val income = stats(txs, TransactionFilter(Period.THIS_MONTH, TransactionType.INCOME))
        assertEquals(listOf(salary.id to 250_000L), income.slices.map { it.categoryId to it.amountMinor })
    }

    @Test
    fun `a transfer type in the filter falls back to expenses`() {
        val s = stats(listOf(tx(500, food.id)), TransactionFilter(Period.THIS_MONTH, TransactionType.TRANSFER))
        assertEquals(TransactionType.EXPENSE, s.filter.type)
        assertEquals(500L, s.totalMinor)
    }

    @Test
    fun `the long tail is merged into Other without losing money`() {
        val many = (1..8L).map { id -> cat(100 + id) }
        val txs = many.mapIndexed { i, c -> tx((i + 1) * 100L, c.id) }
        val s = buildStats(
            TransactionFilter(Period.THIS_MONTH, TransactionType.EXPENSE), emptyList(), many, txs, today, zone, top = 4,
        )
        assertEquals(4, s.slices.size)
        assertTrue(s.slices.last().isOther)
        // три найбільші — 800, 700, 600; решта 500+400+300+200+100 = 1500
        assertEquals(listOf(800L, 700L, 600L, 1500L), s.slices.map { it.amountMinor })
        assertEquals(txs.sumOf { it.amountMinor }, s.totalMinor)
    }

    @Test
    fun `currencies are not mixed, the biggest one is shown and the rest are named`() {
        val s = stats(listOf(tx(1000, food.id), tx(50_000, food.id, currency = "UAH"), tx(200, transport.id, currency = "USD")))
        assertEquals("UAH", s.currency)
        assertEquals(50_000L, s.totalMinor)
        assertEquals(listOf("EUR", "USD"), s.otherCurrencies)
    }

    @Test
    fun `the period filter applies`() {
        val s = stats(listOf(tx(1000, food.id, month = 9), tx(9999, food.id, month = 8)))
        assertEquals(1000L, s.totalMinor)
    }

    @Test
    fun `category chips follow the chosen kind and show only top level`() {
        assertEquals(listOf(food.id, transport.id), stats(emptyList()).categories.map { it.id })
        val income = stats(emptyList(), TransactionFilter(type = TransactionType.INCOME))
        assertEquals(listOf(salary.id), income.categories.map { it.id })
    }
}

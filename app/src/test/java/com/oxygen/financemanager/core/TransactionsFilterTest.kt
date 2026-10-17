package com.oxygen.financemanager.core

import com.oxygen.financemanager.data.db.AccountEntity
import com.oxygen.financemanager.data.db.AccountType
import com.oxygen.financemanager.data.db.CategoryEntity
import com.oxygen.financemanager.data.db.CategoryKind
import com.oxygen.financemanager.data.db.TransactionEntity
import com.oxygen.financemanager.data.db.TransactionType
import com.oxygen.financemanager.feature.transactions.Period
import com.oxygen.financemanager.feature.transactions.TransactionFilter
import com.oxygen.financemanager.feature.transactions.buildTransactionsState
import com.oxygen.financemanager.feature.transactions.range
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class TransactionsFilterTest {

    private val zone: ZoneId = ZoneId.of("Europe/Berlin")
    private val today = LocalDate.of(2026, 9, 24)

    private fun at(y: Int, m: Int, d: Int, h: Int = 12) = LocalDateTime.of(y, m, d, h, 0).atZone(zone).toInstant()

    private fun account(id: Long, archived: Boolean = false) = AccountEntity(
        id = id, name = "acc$id", type = AccountType.BANK, currency = "EUR",
        color = 0, icon = "", archived = archived,
    )

    private val food = CategoryEntity(id = 1, name = "Food", kind = CategoryKind.EXPENSE, color = 0, icon = "")
    private val cafe = CategoryEntity(id = 2, name = "Cafe", parentId = 1, kind = CategoryKind.EXPENSE, color = 0, icon = "")
    private val salary = CategoryEntity(id = 3, name = "Salary", kind = CategoryKind.INCOME, color = 0, icon = "")
    private val categories = listOf(food, cafe, salary)

    private var nextId = 1L
    private fun tx(
        type: TransactionType,
        amount: Long,
        at: java.time.Instant,
        from: Long = 1,
        to: Long? = null,
        category: Long? = null,
    ) = TransactionEntity(
        id = nextId++, accountId = from, transferAccountId = to, amountMinor = amount,
        currency = "EUR", type = type, categoryId = category, occurredAt = at,
    )

    private fun state(filter: TransactionFilter, txs: List<TransactionEntity>, accounts: List<AccountEntity> = listOf(account(1), account(2))) =
        buildTransactionsState(filter, accounts, categories, txs, today, zone)

    private fun ids(filter: TransactionFilter, txs: List<TransactionEntity>) =
        state(filter, txs).days.flatMap { day -> day.rows.map { it.transaction.id } }.toSet()

    @Test
    fun `this month starts at local midnight of the 1st, not at UTC midnight`() {
        val r = Period.THIS_MONTH.range(today, zone)!!
        assertEquals(LocalDate.of(2026, 9, 1).atStartOfDay(zone).toInstant(), r.from)
        assertEquals(LocalDate.of(2026, 10, 1).atStartOfDay(zone).toInstant(), r.until)
        // у Берліні північ 1 вересня — це ще 31 серпня за UTC
        assertEquals(31, r.from.atOffset(ZoneOffset.UTC).dayOfMonth)
    }

    @Test
    fun `last month in January reaches back into the previous year`() {
        val r = Period.LAST_MONTH.range(LocalDate.of(2027, 1, 15), zone)!!
        assertEquals(LocalDate.of(2026, 12, 1).atStartOfDay(zone).toInstant(), r.from)
        assertEquals(LocalDate.of(2027, 1, 1).atStartOfDay(zone).toInstant(), r.until)
    }

    @Test
    fun `all time has no bounds`() {
        assertNull(Period.ALL.range(today, zone))
    }

    @Test
    fun `month boundaries are half-open, so a transaction lands in exactly one month`() {
        val firstMoment = tx(TransactionType.EXPENSE, 100, LocalDate.of(2026, 9, 1).atStartOfDay(zone).toInstant())
        val lastMomentOfAugust = tx(TransactionType.EXPENSE, 100, firstMoment.occurredAt.minusMillis(1))
        val txs = listOf(firstMoment, lastMomentOfAugust)

        assertEquals(setOf(firstMoment.id), ids(TransactionFilter(Period.THIS_MONTH), txs))
        assertEquals(setOf(lastMomentOfAugust.id), ids(TransactionFilter(Period.LAST_MONTH), txs))
    }

    @Test
    fun `an account filter includes transfers into that account`() {
        val expense = tx(TransactionType.EXPENSE, 100, at(2026, 9, 10), from = 1)
        val transferIn = tx(TransactionType.TRANSFER, 500, at(2026, 9, 11), from = 1, to = 2)
        val other = tx(TransactionType.EXPENSE, 100, at(2026, 9, 12), from = 3)

        val onlyAccount2 = TransactionFilter(Period.ALL, accountId = 2)
        assertEquals(setOf(transferIn.id), ids(onlyAccount2, listOf(expense, transferIn, other)))
    }

    @Test
    fun `a category filter includes its subcategories`() {
        val groceries = tx(TransactionType.EXPENSE, 100, at(2026, 9, 10), category = food.id)
        val coffee = tx(TransactionType.EXPENSE, 100, at(2026, 9, 10), category = cafe.id)
        val pay = tx(TransactionType.INCOME, 100, at(2026, 9, 10), category = salary.id)

        assertEquals(
            setOf(groceries.id, coffee.id),
            ids(TransactionFilter(Period.ALL, categoryId = food.id), listOf(groceries, coffee, pay)),
        )
        // а підкатегорія окремо — лише себе
        assertEquals(setOf(coffee.id), ids(TransactionFilter(Period.ALL, categoryId = cafe.id), listOf(groceries, coffee, pay)))
    }

    @Test
    fun `totals count expenses and income but never transfers`() {
        val txs = listOf(
            tx(TransactionType.EXPENSE, 1_234, at(2026, 9, 10)),
            tx(TransactionType.EXPENSE, 766, at(2026, 9, 11)),
            tx(TransactionType.INCOME, 250_000, at(2026, 9, 12)),
            tx(TransactionType.TRANSFER, 99_999, at(2026, 9, 13), to = 2),
        )
        val s = state(TransactionFilter(Period.THIS_MONTH), txs)
        assertEquals(2_000L, s.spent.single().amountMinor)
        assertEquals(250_000L, s.received.single().amountMinor)
    }

    @Test
    fun `days are grouped by local date and ordered newest first`() {
        // північ 11 вересня в Берліні — це ще 10-те за UTC; групування за UTC злило б ці два дні в один
        val lateEvening = tx(TransactionType.EXPENSE, 100, at(2026, 9, 10, 23))
        val nextMorning = tx(TransactionType.EXPENSE, 100, at(2026, 9, 11, 0))
        val earlier = tx(TransactionType.EXPENSE, 100, at(2026, 9, 3))

        val days = state(TransactionFilter(Period.THIS_MONTH), listOf(earlier, lateEvening, nextMorning)).days
        assertEquals(
            listOf(LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 3)),
            days.map { it.date },
        )
    }

    @Test
    fun `switching type drops a category that no longer fits`() {
        val onFood = TransactionFilter(type = TransactionType.EXPENSE, categoryId = food.id)
        assertNull(onFood.withType(TransactionType.TRANSFER, categories).categoryId)
        assertNull(onFood.withType(TransactionType.INCOME, categories).categoryId)
        assertEquals(food.id, onFood.withType(null, categories).categoryId)
        assertEquals(food.id, onFood.withType(TransactionType.EXPENSE, categories).categoryId)
    }

    @Test
    fun `archived accounts keep their names in history but are not offered as a filter`() {
        val txs = listOf(tx(TransactionType.EXPENSE, 100, at(2026, 9, 10), from = 9))
        val s = state(TransactionFilter(Period.ALL), txs, accounts = listOf(account(1), account(9, archived = true)))
        assertEquals("acc9", s.days.single().rows.single().accountName)
        assertTrue(s.accounts.none { it.id == 9L })
    }

    @Test
    fun `category chips follow the chosen type`() {
        assertEquals(listOf(food.id), state(TransactionFilter(type = TransactionType.EXPENSE), emptyList()).categories.map { it.id })
        assertEquals(listOf(salary.id), state(TransactionFilter(type = TransactionType.INCOME), emptyList()).categories.map { it.id })
        assertTrue(state(TransactionFilter(type = TransactionType.TRANSFER), emptyList()).categories.isEmpty())
    }
}

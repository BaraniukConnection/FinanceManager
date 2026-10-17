package com.oxygen.financemanager.core

import com.oxygen.financemanager.data.db.AccountEntity
import com.oxygen.financemanager.data.db.AccountType
import com.oxygen.financemanager.data.db.CategoryEntity
import com.oxygen.financemanager.data.db.CategoryKind
import com.oxygen.financemanager.data.db.TransactionEntity
import com.oxygen.financemanager.data.db.TransactionType
import com.oxygen.financemanager.feature.home.buildHomeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class HomeStateTest {

    private fun account(id: Long, currency: String = "EUR", opening: Long = 0) =
        AccountEntity(id = id, name = "acc$id", type = AccountType.BANK, currency = currency,
            openingBalanceMinor = opening, color = 0, icon = "ms:wallet")

    private fun expense(id: Long, from: Long, amount: Long, at: Instant, category: Long? = null) =
        TransactionEntity(id = id, accountId = from, amountMinor = amount, currency = "EUR",
            type = TransactionType.EXPENSE, categoryId = category, occurredAt = at)

    @Test
    fun `totals are grouped per currency, never merged`() {
        val state = buildHomeState(
            accounts = listOf(account(1, "EUR", 10_000), account(2, "USD", 5_000)),
            categories = emptyList(),
            transactions = emptyList(),
        )
        assertEquals(2, state.totals.size)
        assertEquals(10_000, state.totals.single { it.currency == "EUR" }.amountMinor)
        assertEquals(5_000, state.totals.single { it.currency == "USD" }.amountMinor)
    }

    @Test
    fun `same currency accounts are summed into one total`() {
        val state = buildHomeState(
            accounts = listOf(account(1, "EUR", 10_000), account(2, "EUR", 2_500)),
            categories = emptyList(),
            transactions = emptyList(),
        )
        assertEquals(listOf(12_500L), state.totals.map { it.amountMinor })
    }

    @Test
    fun `recent list is newest first and capped`() {
        val base = Instant.parse("2026-01-01T00:00:00Z")
        val txs = (1..15L).map { expense(it, from = 1, amount = it * 100, at = base.plusSeconds(it * 3600)) }
        val state = buildHomeState(listOf(account(1)), emptyList(), txs, recentLimit = 10)

        assertEquals(10, state.recent.size)
        assertEquals(15L, state.recent.first().transaction.id)
        val times = state.recent.map { it.transaction.occurredAt }
        assertEquals(times.sortedDescending(), times)
    }

    @Test
    fun `rows carry resolved names and a missing category stays null`() {
        val food = CategoryEntity(id = 7, name = "Продукти", kind = CategoryKind.EXPENSE, color = 0, icon = "")
        val state = buildHomeState(
            accounts = listOf(account(1)),
            categories = listOf(food),
            transactions = listOf(
                expense(1, from = 1, amount = 500, at = Instant.EPOCH, category = 7),
                expense(2, from = 1, amount = 300, at = Instant.EPOCH),
            ),
        )
        val byId = state.recent.associateBy { it.transaction.id }
        assertEquals("Продукти", byId.getValue(1L).categoryName)
        assertNull(byId.getValue(2L).categoryName)
        assertEquals("acc1", byId.getValue(1L).accountName)
    }

    @Test
    fun `account balance on home matches the balance math`() {
        val acc = account(1, opening = 10_000)
        val txs = listOf(expense(1, from = 1, amount = 2_500, at = Instant.EPOCH))
        val state = buildHomeState(listOf(acc), emptyList(), txs)
        assertEquals(balanceMinor(acc, txs), state.accounts.single().balanceMinor)
        assertEquals(7_500, state.accounts.single().balanceMinor)
    }

    @Test
    fun `no accounts means empty state`() {
        assertTrue(buildHomeState(emptyList(), emptyList(), emptyList()).isEmpty)
        assertTrue(!buildHomeState(listOf(account(1)), emptyList(), emptyList()).isEmpty)
    }
}

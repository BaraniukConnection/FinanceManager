package com.oxygen.financemanager.core

import com.oxygen.financemanager.data.db.AccountEntity
import com.oxygen.financemanager.data.db.AccountType
import com.oxygen.financemanager.data.db.CategoryEntity
import com.oxygen.financemanager.data.db.CategoryKind
import com.oxygen.financemanager.data.db.TransactionEntity
import com.oxygen.financemanager.data.db.TransactionType
import com.oxygen.financemanager.feature.add.TransactionForm
import com.oxygen.financemanager.feature.add.toEntity
import com.oxygen.financemanager.feature.add.toForm
import com.oxygen.financemanager.feature.add.withAccount
import com.oxygen.financemanager.feature.add.withType
import com.oxygen.financemanager.feature.home.buildHomeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class EditingTest {

    private val zone = ZoneId.of("Europe/Berlin")

    @Test
    fun `minorToText is the exact inverse of parseToMinor`() {
        for ((minor, currency) in listOf(1234L to "EUR", 5L to "EUR", 100_000L to "EUR", -25_050L to "EUR", 100L to "JPY")) {
            assertEquals(minor, parseToMinor(minorToText(minor, currency), currency))
        }
        assertEquals("12.34", minorToText(1234, "EUR"))
        assertEquals("0.05", minorToText(5, "EUR"))
        assertEquals("100", minorToText(100, "JPY"))
    }

    @Test
    fun `a transaction survives the round trip through the edit form`() {
        val original = TransactionEntity(
            id = 7, accountId = 1, transferAccountId = null, amountMinor = 420, currency = "EUR",
            type = TransactionType.EXPENSE, categoryId = 3,
            occurredAt = LocalDateTime.of(2026, 9, 20, 8, 15, 30).atZone(zone).toInstant(),
            note = "Coffee",
        )
        val back = original.toForm(zone).toEntity("EUR", zone)!!
        assertEquals(original.copy(createdAt = back.createdAt, id = back.id), back)
    }

    @Test
    fun `changing only the date keeps the time of day`() {
        val original = TransactionEntity(
            accountId = 1, amountMinor = 100, currency = "EUR", type = TransactionType.EXPENSE,
            occurredAt = LocalDateTime.of(2026, 9, 20, 18, 45).atZone(zone).toInstant(),
        )
        val moved = original.toForm(zone).copy(date = LocalDate.of(2026, 9, 18)).toEntity("EUR", zone)!!
        assertEquals(LocalDateTime.of(2026, 9, 18, 18, 45), moved.occurredAt.atZone(zone).toLocalDateTime())
    }

    @Test
    fun `savings leave the total only when the setting asks for it`() {
        fun acc(id: Long, type: AccountType, opening: Long) = AccountEntity(
            id = id, name = "a$id", type = type, currency = "EUR", openingBalanceMinor = opening, color = 0, icon = "",
        )
        val accounts = listOf(acc(1, AccountType.BANK, 10_000), acc(2, AccountType.SAVINGS, 5_000))

        assertEquals(15_000L, buildHomeState(accounts, emptyList(), emptyList()).totals.single().amountMinor)
        val excluded = buildHomeState(accounts, emptyList(), emptyList(), excludeSavingsFromTotal = true)
        assertEquals(10_000L, excluded.totals.single().amountMinor)
        // сама скарбничка з екрана не зникає — лише з підсумку
        assertEquals(1, excluded.savings.size)
    }
}

class FormTypeSwitchTest {
    private val food = CategoryEntity(id = 1, name = "Food", kind = CategoryKind.EXPENSE, color = 0, icon = "")
    private val salary = CategoryEntity(id = 2, name = "Salary", kind = CategoryKind.INCOME, color = 0, icon = "")
    private val categories = listOf(food, salary)
    private val onFood = TransactionForm(categoryId = food.id)

    @Test
    fun `switching to income drops an expense category`() {
        assertNull(onFood.withType(TransactionType.INCOME, categories).categoryId)
    }

    @Test
    fun `switching to transfer drops any category`() {
        assertNull(onFood.withType(TransactionType.TRANSFER, categories).categoryId)
    }

    @Test
    fun `staying within the same kind keeps the category`() {
        assertEquals(food.id, onFood.withType(TransactionType.EXPENSE, categories).categoryId)
    }

    @Test
    fun `picking the destination as the source clears the destination`() {
        val form = TransactionForm(accountId = 1, destinationId = 2)
        val swapped = form.withAccount(2)
        assertEquals(2L, swapped.accountId)
        assertNull(swapped.destinationId)
        assertEquals(2L, form.withAccount(3).destinationId)
    }
}

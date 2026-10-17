package com.oxygen.financemanager.core

import com.oxygen.financemanager.data.db.AccountType
import com.oxygen.financemanager.data.db.TransactionType
import com.oxygen.financemanager.feature.accounts.AccountForm
import com.oxygen.financemanager.feature.accounts.AccountFormError
import com.oxygen.financemanager.feature.accounts.errors as accountErrors
import com.oxygen.financemanager.feature.accounts.toEntity as toAccount
import com.oxygen.financemanager.feature.add.FormError
import com.oxygen.financemanager.feature.add.TransactionForm
import com.oxygen.financemanager.feature.add.errors
import com.oxygen.financemanager.feature.add.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MoneyParsingTest {

    @Test
    fun `dot and comma are both accepted`() {
        assertEquals(1234L, parseToMinor("12.34", "EUR"))
        assertEquals(1234L, parseToMinor("12,34", "EUR"))
    }

    @Test
    fun `thousands separators are ignored, including the non-breaking space`() {
        assertEquals(123450L, parseToMinor("1 234,50", "EUR"))
        assertEquals(123450L, parseToMinor("1 234,50", "EUR"))
        assertEquals(123450L, parseToMinor("1 234,50", "EUR"))
    }

    @Test
    fun `extra digits are rounded by the currency's rules`() {
        assertEquals(1235L, parseToMinor("12.345", "EUR"))
        assertEquals(1234L, parseToMinor("12.344", "EUR"))
        assertEquals(100L, parseToMinor("100", "JPY"))   // нуль знаків після коми
        assertEquals(101L, parseToMinor("100.5", "JPY"))
    }

    @Test
    fun `garbage and emptiness give null, not zero`() {
        assertNull(parseToMinor("", "EUR"))
        assertNull(parseToMinor("   ", "EUR"))
        assertNull(parseToMinor("abc", "EUR"))
        assertNull(parseToMinor("12.3.4", "EUR"))
        assertNull(parseToMinor("-", "EUR"))
    }

    @Test
    fun `a negative input parses, the form is what rejects it`() {
        assertEquals(-500L, parseToMinor("-5", "EUR"))
    }
}

class TransactionFormTest {

    private val valid = TransactionForm(amountText = "10.00", accountId = 1L)

    @Test
    fun `a filled expense has no errors and becomes an entity`() {
        assertTrue(valid.errors("EUR").isEmpty())
        val tx = valid.toEntity("EUR")
        assertNotNull(tx)
        assertEquals(1000L, tx!!.amountMinor)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertNull(tx.transferAccountId)
    }

    @Test
    fun `zero and negative amounts are caught before the entity`() {
        assertEquals(setOf(FormError.AMOUNT_NOT_POSITIVE), valid.copy(amountText = "0").errors("EUR"))
        assertEquals(setOf(FormError.AMOUNT_NOT_POSITIVE), valid.copy(amountText = "-5").errors("EUR"))
        assertNull(valid.copy(amountText = "0").toEntity("EUR"))
    }

    @Test
    fun `empty and unparseable amounts are different errors`() {
        assertEquals(setOf(FormError.AMOUNT_EMPTY), valid.copy(amountText = "").errors("EUR"))
        assertEquals(setOf(FormError.AMOUNT_NOT_A_NUMBER), valid.copy(amountText = "abc").errors("EUR"))
    }

    @Test
    fun `a missing account is reported and blocks amount checking`() {
        val form = TransactionForm(amountText = "10", accountId = null)
        assertTrue(FormError.ACCOUNT_REQUIRED in form.errors(null))
    }

    @Test
    fun `a transfer needs a different destination`() {
        val transfer = valid.copy(type = TransactionType.TRANSFER)
        assertEquals(setOf(FormError.DESTINATION_REQUIRED), transfer.errors("EUR"))
        assertEquals(
            setOf(FormError.DESTINATION_SAME_AS_SOURCE),
            transfer.copy(destinationId = 1L).errors("EUR"),
        )
        assertTrue(transfer.copy(destinationId = 2L).errors("EUR").isEmpty())
    }

    @Test
    fun `a transfer drops the category, an expense keeps it`() {
        val transfer = valid.copy(type = TransactionType.TRANSFER, destinationId = 2L, categoryId = 7L)
        assertNull(transfer.toEntity("EUR")!!.categoryId)
        assertEquals(7L, valid.copy(categoryId = 7L).toEntity("EUR")!!.categoryId)
    }

    @Test
    fun `a blank note is stored as null, not as an empty string`() {
        assertNull(valid.copy(note = "   ").toEntity("EUR")!!.note)
        assertEquals("кава", valid.copy(note = "  кава ").toEntity("EUR")!!.note)
    }
}

class AccountFormTest {

    private val valid = AccountForm(name = "Готівка", type = AccountType.CASH)

    @Test
    fun `a named account is enough`() {
        assertTrue(valid.accountErrors().isEmpty())
        assertEquals(0L, valid.toAccount()!!.openingBalanceMinor)
    }

    @Test
    fun `a blank name is rejected`() {
        assertEquals(setOf(AccountFormError.NAME_REQUIRED), valid.copy(name = "  ").accountErrors())
    }

    @Test
    fun `a card may open in the negative`() {
        val card = valid.copy(type = AccountType.CARD, openingText = "-250,50")
        assertTrue(card.accountErrors().isEmpty())
        assertEquals(-25050L, card.toAccount()!!.openingBalanceMinor)
    }

    @Test
    fun `target and due date belong to savings accounts only`() {
        val savings = valid.copy(type = AccountType.SAVINGS, targetText = "1000", dueDate = LocalDate.now())
        assertTrue(savings.accountErrors().isEmpty())
        assertEquals(100_000L, savings.toAccount()!!.targetMinor)

        val card = savings.copy(type = AccountType.CARD)
        assertTrue(AccountFormError.TARGET_ONLY_FOR_SAVINGS in card.accountErrors())
    }

    @Test
    fun `a non-positive target is rejected`() {
        val savings = valid.copy(type = AccountType.SAVINGS, targetText = "0")
        assertEquals(setOf(AccountFormError.TARGET_NOT_POSITIVE), savings.accountErrors())
    }
}

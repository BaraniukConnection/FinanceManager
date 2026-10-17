package com.oxygen.financemanager.core

import com.oxygen.financemanager.data.db.AccountEntity
import com.oxygen.financemanager.data.db.AccountType
import com.oxygen.financemanager.data.db.TransactionEntity
import com.oxygen.financemanager.data.db.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BalanceTest {

    private fun account(id: Long, opening: Long = 0, type: AccountType = AccountType.BANK) =
        AccountEntity(id = id, name = "acc$id", type = type, currency = "EUR",
            openingBalanceMinor = opening, color = 0, icon = "ms:wallet")

    private fun tx(
        type: TransactionType,
        amount: Long,
        from: Long,
        to: Long? = null,
    ) = TransactionEntity(accountId = from, transferAccountId = to, amountMinor = amount,
        currency = "EUR", type = type, occurredAt = Instant.EPOCH)

    @Test
    fun `income adds and expense subtracts`() {
        val acc = account(1, opening = 10_000)
        val txs = listOf(
            tx(TransactionType.INCOME, 5_000, from = 1),
            tx(TransactionType.EXPENSE, 2_500, from = 1),
        )
        assertEquals(12_500, balanceMinor(acc, txs))
    }

    @Test
    fun `one transfer row moves money both ways`() {
        val source = account(1, opening = 10_000)
        val destination = account(2, opening = 0, type = AccountType.SAVINGS)
        val txs = listOf(tx(TransactionType.TRANSFER, 3_000, from = 1, to = 2))

        assertEquals(7_000, balanceMinor(source, txs))
        assertEquals(3_000, balanceMinor(destination, txs))
        // гроші не з'явились і не зникли
        assertEquals(10_000, balanceMinor(source, txs) + balanceMinor(destination, txs))
    }

    @Test
    fun `foreign transactions do not touch the account`() {
        val acc = account(1, opening = 500)
        val txs = listOf(
            tx(TransactionType.EXPENSE, 9_999, from = 2),
            tx(TransactionType.TRANSFER, 9_999, from = 2, to = 3),
        )
        assertEquals(500, balanceMinor(acc, txs))
    }

    @Test
    fun `negative and zero amounts are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { tx(TransactionType.EXPENSE, -1, from = 1) }
        assertThrows(IllegalArgumentException::class.java) { tx(TransactionType.EXPENSE, 0, from = 1) }
    }

    @Test
    fun `transfer to itself is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            tx(TransactionType.TRANSFER, 100, from = 1, to = 1)
        }
    }

    @Test
    fun `non-transfer may not carry a destination account`() {
        assertThrows(IllegalArgumentException::class.java) {
            tx(TransactionType.EXPENSE, 100, from = 1, to = 2)
        }
    }

    @Test
    fun `savings target only on savings accounts`() {
        // скарбничка з ціллю та дедлайном — норм
        account(1, type = AccountType.SAVINGS).copy(targetMinor = 100_000, dueDate = LocalDate.now())
        assertThrows(IllegalArgumentException::class.java) {
            account(2, type = AccountType.CARD).copy(targetMinor = 100_000)
        }
    }

    @Test
    fun `money formatting follows the currency, not a hardcoded 2 digits`() {
        assertEquals(100L, minorUnitsPer("EUR"))
        assertEquals(1L, minorUnitsPer("JPY"))
    }
}

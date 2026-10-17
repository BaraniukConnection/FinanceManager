package com.oxygen.financemanager.core

import com.oxygen.financemanager.data.db.AccountEntity
import com.oxygen.financemanager.data.db.TransactionEntity
import com.oxygen.financemanager.data.db.TransactionType

/**
 * Як транзакція впливає на баланс конкретного рахунку.
 *
 * Переказ живе одним рядком, тому він тут зустрічається двічі: як мінус на
 * рахунку-джерелі і як плюс на рахунку-отримувачі.
 */
fun signedMinorFor(accountId: Long, tx: TransactionEntity): Long = when {
    tx.type == TransactionType.TRANSFER && tx.transferAccountId == accountId -> tx.amountMinor
    tx.accountId != accountId -> 0L
    tx.type == TransactionType.INCOME -> tx.amountMinor
    else -> -tx.amountMinor
}

/**
 * Баланс рахунку. Свідомо рахується в Kotlin, а не в SQL: так лишається одна
 * реалізація правил знаку замість двох, які розходяться.
 *
 * ponytail: O(n) по всіх транзакціях рахунку; переписати на SUM у SQL,
 * якщо на десятках тисяч записів список рахунків почне гальмувати.
 */
fun balanceMinor(account: AccountEntity, transactions: List<TransactionEntity>): Long =
    account.openingBalanceMinor + transactions.sumOf { signedMinorFor(account.id, it) }

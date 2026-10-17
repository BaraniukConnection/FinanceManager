package com.oxygen.financemanager.feature.accounts

import com.oxygen.financemanager.core.parseToMinor
import com.oxygen.financemanager.data.db.AccountEntity
import com.oxygen.financemanager.data.db.AccountType
import com.oxygen.financemanager.ui.theme.Mark
import java.time.LocalDate

enum class AccountFormError {
    NAME_REQUIRED,
    OPENING_NOT_A_NUMBER,
    TARGET_NOT_A_NUMBER,
    TARGET_NOT_POSITIVE,
    TARGET_ONLY_FOR_SAVINGS,
}

data class AccountForm(
    val id: Long = 0,
    val name: String = "",
    val type: AccountType = AccountType.BANK,
    val currency: String = "EUR",
    val openingText: String = "",
    val color: Int = PRESET_COLORS.first(),
    val targetText: String = "",
    val dueDate: LocalDate? = null,
)

/** Початковий баланс може бути від'ємним (картка в мінусі) і порожнім — це нуль. */
fun AccountForm.errors(): Set<AccountFormError> = buildSet {
    if (name.isBlank()) add(AccountFormError.NAME_REQUIRED)
    if (openingText.isNotBlank() && parseToMinor(openingText, currency) == null) {
        add(AccountFormError.OPENING_NOT_A_NUMBER)
    }
    if (targetText.isNotBlank() || dueDate != null) {
        if (type != AccountType.SAVINGS) add(AccountFormError.TARGET_ONLY_FOR_SAVINGS)
    }
    if (targetText.isNotBlank()) {
        when (val target = parseToMinor(targetText, currency)) {
            null -> add(AccountFormError.TARGET_NOT_A_NUMBER)
            else -> if (target <= 0) add(AccountFormError.TARGET_NOT_POSITIVE)
        }
    }
}

fun AccountForm.toEntity(): AccountEntity? {
    if (errors().isNotEmpty()) return null
    return AccountEntity(
        id = id,
        name = name.trim(),
        type = type,
        currency = currency,
        openingBalanceMinor = openingText.takeIf { it.isNotBlank() }?.let { parseToMinor(it, currency) } ?: 0L,
        color = color,
        icon = defaultIconFor(type),
        targetMinor = targetText.takeIf { it.isNotBlank() }?.let { parseToMinor(it, currency) },
        dueDate = dueDate,
    )
}

private fun defaultIconFor(type: AccountType) = when (type) {
    AccountType.CASH -> "ms:payments"
    AccountType.BANK -> "ms:account_balance"
    AccountType.CARD -> "ms:credit_card"
    AccountType.SAVINGS -> "ms:savings"
}

/** Кольори рахунків — `mark-*` з Oxygen: колір у вмісті належить користувачу. */
val PRESET_COLORS: List<Int> = Mark.all

val SUPPORTED_CURRENCIES = listOf("EUR", "USD", "GBP", "PLN", "UAH", "CHF", "CZK", "SEK")

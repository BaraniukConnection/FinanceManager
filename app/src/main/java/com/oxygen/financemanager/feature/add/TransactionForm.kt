package com.oxygen.financemanager.feature.add

import com.oxygen.financemanager.core.minorToText
import com.oxygen.financemanager.core.parseToMinor
import com.oxygen.financemanager.data.db.CategoryEntity
import com.oxygen.financemanager.data.db.CategoryKind
import com.oxygen.financemanager.data.db.TransactionEntity
import com.oxygen.financemanager.data.db.TransactionType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Помилки форми. Існують, щоб показати їх користувачу, а не впасти:
 * `init`-блоки сутностей — остання лінія, форма має не доводити до них.
 */
enum class FormError {
    AMOUNT_EMPTY,
    AMOUNT_NOT_A_NUMBER,
    AMOUNT_NOT_POSITIVE,
    ACCOUNT_REQUIRED,
    DESTINATION_REQUIRED,
    DESTINATION_SAME_AS_SOURCE,
}

data class TransactionForm(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val accountId: Long? = null,
    val destinationId: Long? = null,
    val categoryId: Long? = null,
    val date: LocalDate = LocalDate.now(),
    /** Час доби зберігається окремо, щоб редагування дати не перемішувало порядок транзакцій за день. */
    val time: LocalTime = LocalTime.now(),
    val note: String = "",
)

/**
 * Зміна типу скидає категорію, що з ним несумісна: інакше дохід зберігся б
 * з категорією витрати, яку вибрали до перемикання.
 */
fun TransactionForm.withType(newType: TransactionType, categories: List<CategoryEntity>): TransactionForm {
    val kind = categories.firstOrNull { it.id == categoryId }?.kind
    val keep = when (newType) {
        TransactionType.EXPENSE -> kind == CategoryKind.EXPENSE
        TransactionType.INCOME -> kind == CategoryKind.INCOME
        TransactionType.TRANSFER -> false
    }
    return copy(type = newType, categoryId = categoryId.takeIf { keep })
}

/** Звідки: якщо рахунок збігся з «куди», «куди» скидається — переказ сам на себе неможливий. */
fun TransactionForm.withAccount(id: Long): TransactionForm =
    copy(accountId = id, destinationId = destinationId.takeIf { it != id })

/** Форма з наявної транзакції — для редагування. Дата й час — у поясі користувача. */
fun TransactionEntity.toForm(zone: ZoneId = ZoneId.systemDefault()): TransactionForm {
    val local = occurredAt.atZone(zone)
    return TransactionForm(
        type = type,
        amountText = minorToText(amountMinor, currency),
        accountId = accountId,
        destinationId = transferAccountId,
        categoryId = categoryId,
        date = local.toLocalDate(),
        time = local.toLocalTime(),
        note = note.orEmpty(),
    )
}

/** Категорія навмисно не обов'язкова: «без категорії» — робочий стан, Home його вміє. */
fun TransactionForm.errors(currency: String?): Set<FormError> = buildSet {
    if (accountId == null) add(FormError.ACCOUNT_REQUIRED)

    when {
        amountText.isBlank() -> add(FormError.AMOUNT_EMPTY)
        currency == null -> Unit // без рахунку невідома валюта, сума ще не перевіряється
        else -> when (val minor = parseToMinor(amountText, currency)) {
            null -> add(FormError.AMOUNT_NOT_A_NUMBER)
            else -> if (minor <= 0) add(FormError.AMOUNT_NOT_POSITIVE)
        }
    }

    if (type == TransactionType.TRANSFER) {
        when {
            destinationId == null -> add(FormError.DESTINATION_REQUIRED)
            destinationId == accountId -> add(FormError.DESTINATION_SAME_AS_SOURCE)
        }
    }
}

/**
 * Збирає сутність з форми. Повертає `null`, якщо форма невалідна, —
 * викликати без перевірки [errors] безпечно.
 *
 * Валюта транзакції береться з рахунку: окремий вибір валюти в транзакції
 * з'явиться тоді, коли запрацюють курси, а не раніше.
 */
fun TransactionForm.toEntity(currency: String, zone: ZoneId = ZoneId.systemDefault()): TransactionEntity? {
    if (errors(currency).isNotEmpty()) return null
    val minor = parseToMinor(amountText, currency) ?: return null

    return TransactionEntity(
        accountId = accountId!!,
        transferAccountId = destinationId.takeIf { type == TransactionType.TRANSFER },
        amountMinor = minor,
        currency = currency,
        type = type,
        categoryId = categoryId.takeIf { type != TransactionType.TRANSFER },
        occurredAt = date.atTime(time).atZone(zone).toInstant(),
        note = note.trim().takeIf { it.isNotEmpty() },
    )
}

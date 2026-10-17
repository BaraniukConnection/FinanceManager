package com.oxygen.financemanager.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * Суми скрізь у мінорних одиницях валюти (центах) як [Long] і завжди додатні —
 * знак виводиться з [TransactionEntity.type], а не зберігається.
 * Скільки знаків має валюта, питаємо в java.util.Currency, не хардкодимо.
 */

enum class AccountType { CASH, BANK, CARD, SAVINGS }
enum class CategoryKind { EXPENSE, INCOME }
enum class TransactionType { EXPENSE, INCOME, TRANSFER }
enum class BudgetPeriod { WEEKLY, MONTHLY, YEARLY, CUSTOM }

/**
 * Баланс рахунку тут не зберігається — він обчислюється з [openingBalanceMinor]
 * плюс транзакції (див. `core/Balance.kt`). Одне джерело істини.
 *
 * Скарбничка — це [AccountType.SAVINGS] з опційними [targetMinor] та [dueDate];
 * поповнення йде звичайним переказом, окрема сутність не потрібна.
 *
 * [icon] — рядок з префіксом джерела: "ms:savings" (Material Symbols),
 * "si:revolut" (Simple Icons) або "emoji:🐷".
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val currency: String,
    val openingBalanceMinor: Long = 0,
    val color: Int,
    val icon: String,
    val targetMinor: Long? = null,
    val dueDate: LocalDate? = null,
    val displayOrder: Int = 0,
    val archived: Boolean = false,
    val createdAt: Instant = Instant.now(),
) {
    init {
        require(name.isNotBlank()) { "account name must not be blank" }
        require(targetMinor == null || targetMinor > 0) { "target must be positive" }
        require(targetMinor == null && dueDate == null || type == AccountType.SAVINGS) {
            "target/dueDate only make sense on a SAVINGS account"
        }
    }
}

/** Підкатегорії — self-FK на [parentId], без окремої таблиці. Глибина рівно два рівні. */
@Entity(
    tableName = "categories",
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["parentId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("parentId")],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val kind: CategoryKind,
    val color: Int,
    val icon: String,
    val displayOrder: Int = 0,
    val isSystem: Boolean = false,
    val archived: Boolean = false,
)

/**
 * Переказ — **один** рядок: гроші йдуть з [accountId] на [transferAccountId].
 * Двох дзеркальних транзакцій не створюємо.
 *
 * [fxRate] — курс до базової валюти на момент запису, потрібен лише коли
 * [currency] відрізняється від базової.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(AccountEntity::class, ["id"], ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(AccountEntity::class, ["id"], ["transferAccountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("accountId"), Index("transferAccountId"), Index("categoryId"), Index("occurredAt")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val transferAccountId: Long? = null,
    val amountMinor: Long,
    val currency: String,
    val fxRate: Double? = null,
    val type: TransactionType,
    val categoryId: Long? = null,
    val occurredAt: Instant,
    val note: String? = null,
    val createdAt: Instant = Instant.now(),
) {
    init {
        require(amountMinor > 0) { "amount must be positive; direction comes from type" }
        if (type == TransactionType.TRANSFER) {
            require(transferAccountId != null) { "transfer needs a destination account" }
            require(transferAccountId != accountId) { "cannot transfer to the same account" }
        } else {
            require(transferAccountId == null) { "only a transfer may have a destination account" }
        }
    }
}

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val period: BudgetPeriod,
    val startsOn: LocalDate,
    val endsOn: LocalDate? = null,
    val color: Int,
    val archived: Boolean = false,
) {
    init {
        require(amountMinor > 0) { "budget amount must be positive" }
        require(period != BudgetPeriod.CUSTOM || endsOn != null) { "custom period needs an end date" }
    }
}

@Entity(
    tableName = "budget_categories",
    primaryKeys = ["budgetId", "categoryId"],
    foreignKeys = [
        ForeignKey(BudgetEntity::class, ["id"], ["budgetId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("categoryId")],
)
data class BudgetCategoryCrossRef(val budgetId: Long, val categoryId: Long)

/** Кеш курсів, оновлюється фоново. [rateToBase] — скільки базової валюти коштує одиниця [code]. */
@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val code: String,
    val rateToBase: Double,
    val fetchedAt: Instant,
)

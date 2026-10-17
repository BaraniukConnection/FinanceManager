package com.oxygen.financemanager.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY displayOrder, name")
    fun observeActive(): Flow<List<AccountEntity>>

    /** Включно з архівними — щоб в історії транзакцій не зникали назви рахунків. */
    @Query("SELECT * FROM accounts")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun byId(id: Long): AccountEntity?

    @Insert suspend fun insert(account: AccountEntity): Long
    @Update suspend fun update(account: AccountEntity)

    /** Скільки транзакцій зачепить видалення — включно з переказами, де рахунок отримувач. */
    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :id OR transferAccountId = :id")
    suspend fun transactionCount(id: Long): Int

    @Query("DELETE FROM transactions WHERE accountId = :id OR transferAccountId = :id")
    suspend fun deleteTransactionsOf(id: Long)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * FK з транзакцій на рахунок — RESTRICT, тож спершу транзакції, потім сам рахунок.
     * Одна транзакція бази: або зникає все, або нічого.
     */
    @Transaction
    suspend fun deleteWithTransactions(id: Long) {
        deleteTransactionsOf(id)
        deleteById(id)
    }
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY displayOrder, name")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun byId(id: Long): CategoryEntity?

    /** Нова категорія стає в кінець — порядок, який склав користувач, не ламається. */
    @Query("SELECT COALESCE(MAX(displayOrder), -1) + 1 FROM categories")
    suspend fun nextDisplayOrder(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId IN (:ids)")
    suspend fun transactionCount(ids: List<Long>): Int

    /** Лише системні й лише ті, що досі мають стандартну назву — перейменовані користувачем не чіпаються. */
    @Query("UPDATE categories SET name = :to WHERE isSystem = 1 AND parentId IS NULL AND kind = :kind AND name IN (:from)")
    suspend fun renameSystem(kind: CategoryKind, from: List<String>, to: String)

    @Insert suspend fun insertAll(categories: List<CategoryEntity>)
    @Insert suspend fun insert(category: CategoryEntity): Long
    @Update suspend fun update(category: CategoryEntity)

    @Query("UPDATE transactions SET categoryId = :parentId WHERE categoryId = :id")
    suspend fun moveTransactions(id: Long, parentId: Long)

    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId IN (:ids)")
    suspend fun uncategorise(ids: List<Long>)

    @Query("DELETE FROM budget_categories WHERE categoryId IN (:ids)")
    suspend fun unlinkBudgets(ids: List<Long>)

    @Query("DELETE FROM categories WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /**
     * Підкатегорія зникає, а її транзакції переходять до батьківської: «Кафе» → «Продукти»,
     * облік не губиться. Усе явно й однією транзакцією бази — не покладаємось на каскади FK.
     */
    @Transaction
    suspend fun deleteSubcategory(id: Long, parentId: Long) {
        moveTransactions(id, parentId)
        unlinkBudgets(listOf(id))
        deleteByIds(listOf(id))
    }

    /** Категорія верхнього рівня — разом з підкатегоріями; їхні транзакції лишаються без категорії. */
    @Transaction
    suspend fun deleteWithChildren(ids: List<Long>) {
        uncategorise(ids)
        unlinkBudgets(ids)
        deleteByIds(ids)
    }
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE occurredAt >= :from AND occurredAt < :until
        ORDER BY occurredAt DESC
        """
    )
    fun observeBetween(from: Instant, until: Instant): Flow<List<TransactionEntity>>

    /** Усе, що впливає на баланс рахунку — включно з переказами, де він отримувач. */
    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR transferAccountId = :accountId")
    fun observeAffecting(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun byId(id: Long): TransactionEntity?

    @Insert suspend fun insert(transaction: TransactionEntity): Long
    @Update suspend fun update(transaction: TransactionEntity)
    @Delete suspend fun delete(transaction: TransactionEntity)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE archived = 0 ORDER BY startsOn DESC")
    fun observeActive(): Flow<List<BudgetEntity>>

    @Insert suspend fun insert(budget: BudgetEntity): Long
    @Insert suspend fun link(refs: List<BudgetCategoryCrossRef>)
    @Update suspend fun update(budget: BudgetEntity)
    @Delete suspend fun delete(budget: BudgetEntity)
}

package com.oxygen.financemanager.di

import android.content.Context
import androidx.room.Room
import com.oxygen.financemanager.data.db.AccountDao
import com.oxygen.financemanager.data.db.BudgetDao
import com.oxygen.financemanager.data.db.CategoryDao
import com.oxygen.financemanager.data.db.OxygenDatabase
import com.oxygen.financemanager.data.db.TransactionDao
import com.oxygen.financemanager.data.db.defaultCategories
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
        categories: Provider<CategoryDao>,
    ): OxygenDatabase = Room.databaseBuilder(context, OxygenDatabase::class.java, "oxygen.db")
        .addCallback(SeedCategories(context, categories))
        .build()

    @Provides
    @Singleton
    fun preferences(@ApplicationContext context: Context) =
        com.oxygen.financemanager.data.prefs.AppPreferences(context)

    @Provides fun accounts(db: OxygenDatabase): AccountDao = db.accounts()
    @Provides fun categories(db: OxygenDatabase): CategoryDao = db.categories()
    @Provides fun transactions(db: OxygenDatabase): TransactionDao = db.transactions()
    @Provides fun budgets(db: OxygenDatabase): BudgetDao = db.budgets()
}

/** Наповнює порожню базу набором категорій за замовчуванням при першому запуску. */
private class SeedCategories(
    private val context: Context,
    private val categories: Provider<CategoryDao>,
) : androidx.room.RoomDatabase.Callback() {
    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            categories.get().insertAll(defaultCategories(context))
        }
    }
}

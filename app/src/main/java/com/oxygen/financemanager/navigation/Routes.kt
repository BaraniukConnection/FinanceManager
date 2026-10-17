package com.oxygen.financemanager.navigation

import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object TransactionsRoute
@Serializable object AccountsRoute
@Serializable object CategoriesRoute
@Serializable object SettingsRoute
@Serializable object FontLicenseRoute

/** [accountId] == 0 означає створення нового рахунку. */
@Serializable data class AccountEditRoute(val accountId: Long = 0L)

/** [transactionId] == 0 означає створення нової транзакції. */
@Serializable data class TransactionEditRoute(val transactionId: Long = 0L)

/** [categoryId] == 0 — нова; [parentId] != 0 — нова одразу як підкатегорія цієї категорії. */
@Serializable data class CategoryEditRoute(val categoryId: Long = 0L, val parentId: Long = 0L)

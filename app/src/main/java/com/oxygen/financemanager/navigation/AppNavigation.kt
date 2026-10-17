package com.oxygen.financemanager.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.oxygen.financemanager.R
import com.oxygen.financemanager.feature.accounts.AccountEditScreen
import com.oxygen.financemanager.feature.accounts.AccountsScreen
import com.oxygen.financemanager.feature.add.TransactionEditScreen
import com.oxygen.financemanager.feature.categories.CategoriesScreen
import com.oxygen.financemanager.feature.categories.CategoryEditScreen
import com.oxygen.financemanager.feature.home.HomeScreen
import com.oxygen.financemanager.feature.settings.FontLicenseScreen
import com.oxygen.financemanager.feature.settings.SettingsScreen
import com.oxygen.financemanager.feature.transactions.TransactionsScreen
import com.oxygen.financemanager.ui.BottomNav
import com.oxygen.financemanager.ui.NavTab
import com.oxygen.financemanager.ui.OxIconButton
import com.oxygen.financemanager.ui.OxTopBar
import com.oxygen.financemanager.ui.theme.Space

/** Меню й бульбашки — 160 мс; екрани — трохи довше, у межах 200–300 мс з Oxygen. */
private const val MENU_MS = 160
private const val SCREEN_MS = 280

/** Розділи верхнього рівня, у порядку кнопок унизу. Від порядку залежить напрямок переходу. */
private fun tabOf(destination: NavDestination?): Int? = when {
    destination == null -> null
    destination.hasRoute(HomeRoute::class) -> 0
    destination.hasRoute(TransactionsRoute::class) -> 1
    else -> null
}

/**
 * Куди їде екран. Вглиб — справа наліво; між розділами — за їхнім порядком унизу,
 * тож перехід на лівіший розділ їде зліва, і рух збігається з тим, де кнопка.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.forward(): SlideDirection {
    val from = tabOf(initialState.destination)
    val to = tabOf(targetState.destination)
    return if (from != null && to != null && to < from) SlideDirection.End else SlideDirection.Start
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    // до першого кадру бекстеку ще немає — це стартовий Home
    val tab = if (backStack == null) 0 else tabOf(backStack?.destination)
    val isTopLevel = tab != null
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    fun open(route: Any) {
        menuOpen = false
        navController.navigate(route) { launchSingleTop = true }
    }

    fun openTab(index: Int) {
        menuOpen = false
        navController.navigate(if (index == 0) HomeRoute else TransactionsRoute) {
            popUpTo(HomeRoute) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        topBar = {
            OxTopBar(
                title = stringResource(titleFor(backStack)),
                onBack = if (isTopLevel) null else ({ navController.popBackStack() }),
            ) {
                if (isTopLevel) QuickMenu(
                    expanded = menuOpen,
                    onToggle = { menuOpen = !menuOpen },
                    onAccounts = { open(AccountsRoute) },
                    onCategories = { open(CategoriesRoute) },
                    onSettings = { open(SettingsRoute) },
                )
            }
        },
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { slideIntoContainer(forward(), tween(SCREEN_MS)) + fadeIn(tween(SCREEN_MS)) },
                exitTransition = {
                    slideOutOfContainer(forward(), tween(SCREEN_MS), targetOffset = { it / 4 }) + fadeOut(tween(SCREEN_MS))
                },
                popEnterTransition = {
                    slideIntoContainer(SlideDirection.End, tween(SCREEN_MS), initialOffset = { it / 4 }) + fadeIn(tween(SCREEN_MS))
                },
                popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(SCREEN_MS)) + fadeOut(tween(SCREEN_MS)) },
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        onAddAccount = { navController.navigate(AccountEditRoute()) },
                        onOpenAccount = { id -> navController.navigate(AccountEditRoute(id)) },
                        onOpenTransaction = { id -> navController.navigate(TransactionEditRoute(id)) },
                    )
                }
                composable<TransactionsRoute> {
                    TransactionsScreen(onOpenTransaction = { id -> navController.navigate(TransactionEditRoute(id)) })
                }
                composable<AccountsRoute> {
                    AccountsScreen(
                        onAdd = { navController.navigate(AccountEditRoute()) },
                        onEdit = { id -> navController.navigate(AccountEditRoute(id)) },
                    )
                }
                composable<CategoriesRoute> {
                    CategoriesScreen(
                        onAdd = { navController.navigate(CategoryEditRoute()) },
                        onEdit = { id -> navController.navigate(CategoryEditRoute(categoryId = id)) },
                    )
                }
                composable<CategoryEditRoute> {
                    CategoryEditScreen(
                        onDone = { navController.popBackStack() },
                        onOpenCategory = { id -> navController.navigate(CategoryEditRoute(categoryId = id)) },
                        onAddSubcategory = { parent -> navController.navigate(CategoryEditRoute(parentId = parent)) },
                    )
                }
                composable<SettingsRoute> {
                    SettingsScreen(onOpenFontLicense = { navController.navigate(FontLicenseRoute) })
                }
                composable<FontLicenseRoute> { FontLicenseScreen() }
                composable<AccountEditRoute> { AccountEditScreen(onDone = { navController.popBackStack() }) }
                composable<TransactionEditRoute> { TransactionEditScreen(onDone = { navController.popBackStack() }) }
            }

            // нижня навігація від'їжджає вниз, коли йдемо вглиб, і повертається на розділах
            AnimatedVisibility(
                visible = isTopLevel,
                enter = slideInVertically(tween(SCREEN_MS)) { it } + fadeIn(tween(SCREEN_MS)),
                exit = slideOutVertically(tween(SCREEN_MS)) { it } + fadeOut(tween(SCREEN_MS)),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Space.s6),
            ) {
                BottomNav(
                    tabs = listOf(
                        // вкладка в спокої — контурна, обрана — залита
                        NavTab(Icons.Outlined.Home, Icons.Default.Home, stringResource(R.string.nav_home)),
                        NavTab(
                            Icons.AutoMirrored.Outlined.ReceiptLong,
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            stringResource(R.string.nav_transactions),
                        ),
                    ),
                    selected = tab ?: 0,
                    onSelect = ::openTab,
                    createLabel = stringResource(R.string.add_transaction_title),
                    onCreate = { navController.navigate(TransactionEditRoute()) },
                )
            }
        }
    }
}

/**
 * Рідкісні переходи ховаються за стрілкою, а не займають екран постійно.
 * Кнопки виїжджають з-за правого краю; налаштування лишаються останніми справа, як у всіх застосунках Oxygen.
 */
@Composable
private fun QuickMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    onAccounts: () -> Unit,
    onCategories: () -> Unit,
    onSettings: () -> Unit,
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(MENU_MS), label = "chevron")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        OxIconButton(
            icon = Icons.Default.ChevronLeft,
            label = stringResource(if (expanded) R.string.action_less else R.string.action_more),
            onClick = onToggle,
            modifier = Modifier.rotate(rotation),
        )
        AnimatedVisibility(
            visible = expanded,
            enter = slideInHorizontally(tween(MENU_MS)) { it } + fadeIn(tween(MENU_MS)),
            exit = slideOutHorizontally(tween(MENU_MS)) { it } + fadeOut(tween(MENU_MS)),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                OxIconButton(Icons.Default.Wallet, stringResource(R.string.nav_accounts), onAccounts)
                OxIconButton(Icons.Default.GridView, stringResource(R.string.nav_categories), onCategories)
                OxIconButton(Icons.Default.Settings, stringResource(R.string.nav_settings), onSettings)
            }
        }
    }
}

private fun titleFor(entry: NavBackStackEntry?): Int {
    val destination = entry?.destination ?: return R.string.app_name
    return when {
        destination.hasRoute(TransactionsRoute::class) -> R.string.nav_transactions
        destination.hasRoute(AccountsRoute::class) -> R.string.nav_accounts
        destination.hasRoute(CategoriesRoute::class) -> R.string.nav_categories
        destination.hasRoute(SettingsRoute::class) -> R.string.nav_settings
        destination.hasRoute(FontLicenseRoute::class) -> R.string.settings_font_license
        destination.hasRoute(TransactionEditRoute::class) ->
            if (entry.toRoute<TransactionEditRoute>().transactionId == 0L) R.string.add_transaction_title
            else R.string.tx_edit_title
        destination.hasRoute(CategoryEditRoute::class) -> entry.toRoute<CategoryEditRoute>().let {
            when {
                it.categoryId != 0L -> R.string.category_edit_title
                it.parentId != 0L -> R.string.subcategory_add
                else -> R.string.category_add
            }
        }
        destination.hasRoute(AccountEditRoute::class) ->
            if (entry.toRoute<AccountEditRoute>().accountId == 0L) R.string.account_add else R.string.account_edit_title
        else -> R.string.app_name
    }
}

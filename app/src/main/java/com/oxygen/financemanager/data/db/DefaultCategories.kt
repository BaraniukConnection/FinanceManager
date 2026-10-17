package com.oxygen.financemanager.data.db

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import com.oxygen.financemanager.R
import com.oxygen.financemanager.ui.theme.Mark
import java.util.Locale

/**
 * Категорії за замовчуванням. Назви — ресурси, тож локалізуються нативно
 * і в БД лягають уже перекладеним текстом, який користувач може перейменувати.
 *
 * Підкатегорій за замовчуванням немає навмисно: вони надто залежать від того,
 * як конкретна людина веде облік.
 */
private data class Seed(@param:StringRes val nameRes: Int, val color: Long, val icon: String)

private val EXPENSES = listOf(
    Seed(R.string.cat_groceries, Mark.EMERALD, "ms:shopping_basket"),
    Seed(R.string.cat_dining, Mark.AMBER, "ms:restaurant"),
    Seed(R.string.cat_transport, Mark.BLUE, "ms:directions_bus"),
    Seed(R.string.cat_housing, Mark.SLATE, "ms:home"),
    Seed(R.string.cat_utilities, Mark.PURPLE, "ms:bolt"),
    Seed(R.string.cat_shopping, Mark.PINK, "ms:shopping_bag"),
    Seed(R.string.cat_health, Mark.RED, "ms:medical_services"),
    Seed(R.string.cat_beauty, Mark.PINK, "ms:spa"),
    Seed(R.string.cat_sport, Mark.EMERALD, "ms:fitness_center"),
    Seed(R.string.cat_entertainment, Mark.PURPLE, "ms:theaters"),
    Seed(R.string.cat_travel, Mark.BLUE, "ms:flight"),
    Seed(R.string.cat_education, Mark.BLUE, "ms:school"),
    Seed(R.string.cat_children, Mark.AMBER, "ms:child_care"),
    Seed(R.string.cat_pets, Mark.AMBER, "ms:pets"),
    Seed(R.string.cat_gifts, Mark.PINK, "ms:card_giftcard"),
    Seed(R.string.cat_charity, Mark.MINT, "ms:volunteer_activism"),
    Seed(R.string.cat_insurance, Mark.SLATE, "ms:shield"),
    Seed(R.string.cat_taxes, Mark.SLATE, "ms:account_balance"),
    Seed(R.string.cat_services, Mark.SLATE, "ms:handyman"),
    Seed(R.string.cat_other_expense, Mark.SLATE, "ms:more_horiz"),
)

private val INCOMES = listOf(
    Seed(R.string.cat_salary, Mark.MINT, "ms:payments"),
    Seed(R.string.cat_freelance, Mark.EMERALD, "ms:work"),
    Seed(R.string.cat_investments, Mark.BLUE, "ms:trending_up"),
    Seed(R.string.cat_gift_received, Mark.PINK, "ms:redeem"),
    Seed(R.string.cat_refund, Mark.PURPLE, "ms:undo"),
    Seed(R.string.cat_other_income, Mark.SLATE, "ms:more_horiz"),
)

fun defaultCategories(context: Context): List<CategoryEntity> =
    (EXPENSES.map { it to CategoryKind.EXPENSE } + INCOMES.map { it to CategoryKind.INCOME })
        .mapIndexed { index, (seed, kind) ->
            CategoryEntity(
                name = context.getString(seed.nameRes),
                kind = kind,
                color = seed.color.toInt(),
                icon = seed.icon,
                displayOrder = index,
                isSystem = true,
            )
        }

/** Мови застосунку, у порядку перемикача. Додати мову — values-xx плюс рядок тут. */
val APP_LANGUAGES = listOf("en", "uk")

private fun Context.stringIn(language: String, @StringRes id: Int): String {
    val config = Configuration(resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
    return createConfigurationContext(config).getString(id)
}

/**
 * Приводить назви системних категорій до мови [context]. Назва в базі — звичайний текст,
 * тож сама вона з мовою не міняється; тут стандартна назва будь-якою з мов замінюється
 * стандартною назвою поточною. Перейменовані користувачем лишаються як є.
 */
suspend fun localizeSystemCategories(context: Context, dao: CategoryDao) {
    (EXPENSES.map { it to CategoryKind.EXPENSE } + INCOMES.map { it to CategoryKind.INCOME }).forEach { (seed, kind) ->
        val target = context.getString(seed.nameRes)
        val others = APP_LANGUAGES.map { context.stringIn(it, seed.nameRes) }.filter { it != target }.distinct()
        if (others.isNotEmpty()) dao.renameSystem(kind, others, target)
    }
}

package com.oxygen.financemanager.feature.categories

import com.oxygen.financemanager.data.db.CategoryEntity
import com.oxygen.financemanager.data.db.CategoryKind
import com.oxygen.financemanager.ui.theme.Mark

enum class CategoryFormError { NAME_REQUIRED }

data class CategoryForm(
    val id: Long = 0,
    val name: String = "",
    val kind: CategoryKind = CategoryKind.EXPENSE,
    val parentId: Long? = null,
    val icon: String = "ms:more_horiz",
    val color: Int = Mark.all.first(),
    val displayOrder: Int = 0,
    val isSystem: Boolean = false,
)

fun CategoryForm.errors(): Set<CategoryFormError> =
    if (name.isBlank()) setOf(CategoryFormError.NAME_REQUIRED) else emptySet()

/**
 * Підкатегорія завжди того ж типу, що батьківська — тип береться з [parent], а не з форми.
 * `null`, якщо форма невалідна.
 */
fun CategoryForm.toEntity(parent: CategoryEntity?): CategoryEntity? {
    if (errors().isNotEmpty()) return null
    return CategoryEntity(
        id = id,
        name = name.trim(),
        parentId = parent?.id,
        kind = parent?.kind ?: kind,
        color = color,
        icon = icon,
        displayOrder = displayOrder,
        isSystem = isSystem,
    )
}

fun CategoryEntity.toForm() = CategoryForm(
    id = id, name = name, kind = kind, parentId = parentId,
    icon = icon, color = color, displayOrder = displayOrder, isSystem = isSystem,
)

/** Вибір батьківської: тип підлаштовується під неї; «без батьківської» тип лишає як був. */
fun CategoryForm.withParent(parent: CategoryEntity?): CategoryForm =
    copy(parentId = parent?.id, kind = parent?.kind ?: kind)

/**
 * Куди можна вкласти категорію. Глибина — рівно два рівні, тому:
 * лише категорії верхнього рівня, того ж типу, не вона сама; а якщо в неї вже є
 * підкатегорії — нікуди, інакше вийшов би третій рівень.
 */
fun parentOptions(all: List<CategoryEntity>, form: CategoryForm, hasChildren: Boolean): List<CategoryEntity> =
    if (hasChildren) emptyList()
    else all.filter { it.parentId == null && it.id != form.id && it.kind == form.kind }

package com.oxygen.financemanager.core

import com.oxygen.financemanager.data.db.CategoryEntity
import com.oxygen.financemanager.data.db.CategoryKind
import com.oxygen.financemanager.feature.categories.CategoryForm
import com.oxygen.financemanager.feature.categories.CategoryFormError
import com.oxygen.financemanager.feature.categories.errors
import com.oxygen.financemanager.feature.categories.parentOptions
import com.oxygen.financemanager.feature.categories.toEntity
import com.oxygen.financemanager.feature.categories.toForm
import com.oxygen.financemanager.feature.categories.withParent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryFormTest {

    private fun cat(id: Long, kind: CategoryKind = CategoryKind.EXPENSE, parent: Long? = null) =
        CategoryEntity(id = id, name = "c$id", parentId = parent, kind = kind, color = 0, icon = "ms:home")

    private val food = cat(1)
    private val cafe = cat(2, parent = 1)
    private val transport = cat(3)
    private val salary = cat(4, CategoryKind.INCOME)
    private val all = listOf(food, cafe, transport, salary)

    @Test
    fun `a blank name is rejected and gives no entity`() {
        assertEquals(setOf(CategoryFormError.NAME_REQUIRED), CategoryForm(name = "  ").errors())
        assertNull(CategoryForm(name = " ").toEntity(null))
    }

    @Test
    fun `a subcategory takes its parent's kind whatever the form says`() {
        val form = CategoryForm(name = "Bonus", kind = CategoryKind.EXPENSE)
        val entity = form.toEntity(parent = salary)!!
        assertEquals(CategoryKind.INCOME, entity.kind)
        assertEquals(salary.id, entity.parentId)
    }

    @Test
    fun `picking a parent aligns the kind, dropping it keeps the kind`() {
        val nested = CategoryForm(name = "x").withParent(salary)
        assertEquals(CategoryKind.INCOME, nested.kind)
        val top = nested.withParent(null)
        assertNull(top.parentId)
        assertEquals(CategoryKind.INCOME, top.kind)
    }

    @Test
    fun `parents are top-level categories of the same kind, never itself`() {
        val editingTransport = transport.toForm()
        assertEquals(listOf(food.id), parentOptions(all, editingTransport, hasChildren = false).map { it.id })
    }

    @Test
    fun `a category with subcategories cannot be nested - no third level`() {
        assertTrue(parentOptions(all, food.toForm(), hasChildren = true).isEmpty())
    }

    @Test
    fun `subcategories are never offered as parents`() {
        val options = parentOptions(all, CategoryForm(name = "new"), hasChildren = false)
        assertTrue(options.none { it.id == cafe.id })
    }

    @Test
    fun `name is trimmed and the rest survives the round trip`() {
        val entity = cafe.toForm().copy(name = "  Coffee ").toEntity(food)!!
        assertEquals("Coffee", entity.name)
        assertEquals(cafe.copy(name = "Coffee"), entity)
    }
}

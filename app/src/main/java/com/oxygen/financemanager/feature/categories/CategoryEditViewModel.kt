package com.oxygen.financemanager.feature.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.oxygen.financemanager.data.db.CategoryDao
import com.oxygen.financemanager.data.db.CategoryEntity
import com.oxygen.financemanager.data.db.CategoryKind
import com.oxygen.financemanager.navigation.CategoryEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Що зникне при видаленні — підтвердження має назвати це чесно. */
data class DeleteImpact(val subcategories: Int, val transactions: Int, val parentName: String?)

@HiltViewModel
class CategoryEditViewModel @Inject constructor(
    private val categories: CategoryDao,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val route = savedState.toRoute<CategoryEditRoute>()
    val isNew: Boolean get() = route.categoryId == 0L

    private val _form = MutableStateFlow(CategoryForm())
    val form: StateFlow<CategoryForm> = _form.asStateFlow()

    private val _showErrors = MutableStateFlow(false)
    val showErrors: StateFlow<Boolean> = _showErrors.asStateFlow()

    private val _deleteImpact = MutableStateFlow<DeleteImpact?>(null)
    val deleteImpact: StateFlow<DeleteImpact?> = _deleteImpact.asStateFlow()

    val all: StateFlow<List<CategoryEntity>> = categories.observeActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val children: StateFlow<List<CategoryEntity>> = combine(all, _form) { list, f ->
        if (f.id == 0L) emptyList() else list.filter { it.parentId == f.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            when {
                !isNew -> categories.byId(route.categoryId)?.let { _form.value = it.toForm() }
                // «Додати підкатегорію» з екрана батьківської — одразу вкладена в неї і стартує
                // з її іконкою й кольором, щоб на вигляд належала до неї; змінити можна
                route.parentId != 0L -> categories.byId(route.parentId)?.let { parent ->
                    _form.update { it.withParent(parent).copy(icon = parent.icon, color = parent.color) }
                }
            }
        }
    }

    fun update(transform: (CategoryForm) -> CategoryForm) = _form.update(transform)
    fun setKind(kind: CategoryKind) = _form.update { it.copy(kind = kind) }
    fun setParent(id: Long?) = _form.update { form -> form.withParent(all.value.firstOrNull { it.id == id }) }

    fun save(onDone: () -> Unit) {
        val form = _form.value
        val entity = form.toEntity(all.value.firstOrNull { it.id == form.parentId })
        if (entity == null) {
            _showErrors.value = true
            return
        }
        viewModelScope.launch {
            if (isNew) categories.insert(entity.copy(displayOrder = categories.nextDisplayOrder()))
            else categories.update(entity)
            onDone()
        }
    }

    /** Рахує наслідки видалення перед тим, як показати підтвердження. */
    fun askDelete() {
        val form = _form.value
        val kids = children.value.map { it.id }
        viewModelScope.launch {
            _deleteImpact.value = DeleteImpact(
                subcategories = kids.size,
                transactions = categories.transactionCount(listOf(form.id) + kids),
                parentName = all.value.firstOrNull { it.id == form.parentId }?.name,
            )
        }
    }

    fun cancelDelete() {
        _deleteImpact.value = null
    }

    fun delete(onDone: () -> Unit) {
        val form = _form.value
        val parentId = form.parentId
        val kids = children.value.map { it.id }
        _deleteImpact.value = null
        viewModelScope.launch {
            if (parentId != null) categories.deleteSubcategory(form.id, parentId)
            else categories.deleteWithChildren(listOf(form.id) + kids)
            onDone()
        }
    }
}

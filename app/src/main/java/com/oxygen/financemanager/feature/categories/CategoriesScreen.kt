package com.oxygen.financemanager.feature.categories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.oxygen.financemanager.R
import com.oxygen.financemanager.data.db.CategoryDao
import com.oxygen.financemanager.data.db.CategoryEntity
import com.oxygen.financemanager.data.db.CategoryKind
import com.oxygen.financemanager.ui.OxPill
import com.oxygen.financemanager.ui.SectionHeader
import com.oxygen.financemanager.ui.theme.Space
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(categories: CategoryDao) : ViewModel() {
    val state: StateFlow<List<CategoryEntity>> = categories.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/**
 * Категорії панелями. Батьківські згорнуті, праворуч «+N кат.» — тап розгортає підкатегорії
 * в тій самій панелі. Порядок — той, у якому їх склав користувач.
 */
@Composable
fun CategoriesScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val categories by viewModel.state.collectAsStateWithLifecycle()
    val children = categories.filter { it.parentId != null }.groupBy { it.parentId }
    val roots = categories.filter { it.parentId == null }.groupBy { it.kind }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = Space.s4, end = Space.s4, bottom = Space.bottomInset),
        ) {
            CategoryKind.entries.forEach { kind ->
                val group = roots[kind].orEmpty()
                if (group.isEmpty()) return@forEach

                item(key = "header-$kind") {
                    SectionHeader(
                        stringResource(if (kind == CategoryKind.EXPENSE) R.string.tx_type_expense else R.string.tx_type_income)
                    )
                }
                items(group, key = { "cat-${it.id}" }) { root ->
                    CategoryGroup(
                        root = root,
                        children = children[root.id].orEmpty(),
                        onClick = { onEdit(it.id) },
                        elevated = true,
                        modifier = Modifier.padding(vertical = Space.s1),
                    )
                }
            }
        }

        OxPill(
            label = stringResource(R.string.category_add),
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Space.s6),
        )
    }
}

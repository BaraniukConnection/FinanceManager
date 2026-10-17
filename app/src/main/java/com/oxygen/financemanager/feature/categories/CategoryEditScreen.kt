package com.oxygen.financemanager.feature.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oxygen.financemanager.R
import com.oxygen.financemanager.data.db.CategoryKind
import com.oxygen.financemanager.feature.add.CategorySheet
import com.oxygen.financemanager.ui.ButtonVariant
import com.oxygen.financemanager.ui.ColorRow
import com.oxygen.financemanager.ui.IconDisc
import com.oxygen.financemanager.ui.OxButton
import com.oxygen.financemanager.ui.OxConfirmDialog
import com.oxygen.financemanager.ui.OxField
import com.oxygen.financemanager.ui.OxListItem
import com.oxygen.financemanager.ui.OxPanel
import com.oxygen.financemanager.ui.OxSegmented
import com.oxygen.financemanager.ui.OxSheet
import com.oxygen.financemanager.ui.PanelDivider
import com.oxygen.financemanager.ui.PanelRow
import com.oxygen.financemanager.ui.SectionHeader
import com.oxygen.financemanager.ui.UiDisc
import com.oxygen.financemanager.ui.pickableIcons
import com.oxygen.financemanager.ui.theme.Mark
import com.oxygen.financemanager.ui.theme.Oxygen
import com.oxygen.financemanager.ui.theme.OxygenType
import com.oxygen.financemanager.ui.theme.Space

private enum class Sheet { None, Parent, Icon }

@Composable
fun CategoryEditScreen(
    onDone: () -> Unit,
    onOpenCategory: (Long) -> Unit,
    onAddSubcategory: (parentId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val all by viewModel.all.collectAsStateWithLifecycle()
    val children by viewModel.children.collectAsStateWithLifecycle()
    val showErrors by viewModel.showErrors.collectAsStateWithLifecycle()
    val impact by viewModel.deleteImpact.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf(Sheet.None) }

    val parent = all.firstOrNull { it.id == form.parentId }
    val parents = parentOptions(all, form, hasChildren = children.isNotEmpty())
    val nameError = if (showErrors && CategoryFormError.NAME_REQUIRED in form.errors()) {
        stringResource(R.string.err_name_required)
    } else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.s4)
            .padding(bottom = Space.s6),
        verticalArrangement = Arrangement.spacedBy(Space.s4),
    ) {
        // прев'ю: так категорія виглядатиме у списках
        Column(
            Modifier.fillMaxWidth().padding(top = Space.s4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            IconDisc(form.icon, form.color, form.name.ifBlank { "?" }, size = 72.dp)
            Text(
                text = form.name.ifBlank { stringResource(R.string.category_name_placeholder) },
                style = OxygenType.screenTitle,
                color = if (form.name.isBlank()) Oxygen.colors.inkMuted else Oxygen.colors.ink,
                textAlign = TextAlign.Center,
            )
        }

        OxField(
            label = stringResource(R.string.account_name),
            value = form.name,
            onValueChange = { name -> viewModel.update { it.copy(name = name) } },
            error = nameError,
        )

        // тип обирається лише при створенні: зміна пізніше лишила б старі витрати з категорією доходу.
        // підкатегорія тип не обирає — він від батьківської
        if (viewModel.isNew && form.parentId == null) OxSegmented(
            options = CategoryKind.entries,
            selected = form.kind,
            onSelect = viewModel::setKind,
            label = { stringResource(if (it == CategoryKind.EXPENSE) R.string.tx_type_expense else R.string.tx_type_income) },
            icon = { if (it == CategoryKind.EXPENSE) Icons.Default.Remove else Icons.Default.Add },
        )

        OxPanel {
            // у категорії з підкатегоріями батьківської бути не може — інакше третій рівень
            if (children.isEmpty()) {
                PanelRow(
                    label = stringResource(R.string.category_parent),
                    value = parent?.name ?: stringResource(R.string.category_parent_none),
                    placeholder = "",
                    leading = { if (parent != null) IconDisc(parent.icon, parent.color, parent.name) else UiDisc(Icons.Default.AccountTree) },
                    onClick = { sheet = Sheet.Parent },
                )
                PanelDivider()
            }
            PanelRow(
                label = stringResource(R.string.category_icon),
                value = stringResource(R.string.category_icon_pick),
                placeholder = "",
                leading = { IconDisc(form.icon, form.color, form.name.ifBlank { "?" }) },
                onClick = { sheet = Sheet.Icon },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.account_color), style = OxygenType.label, color = Oxygen.colors.ink)
            ColorRow(Mark.all, form.color, onSelect = { color -> viewModel.update { it.copy(color = color) } })
        }

        // підкатегорії — лише в наявної категорії верхнього рівня
        if (!viewModel.isNew && form.parentId == null) {
            SectionHeader(stringResource(R.string.category_subcategories))
            children.forEach { child ->
                OxListItem(
                    title = child.name,
                    leading = { IconDisc(child.icon, child.color, child.name, size = 32.dp) },
                    onClick = { onOpenCategory(child.id) },
                )
            }
            OxButton(
                text = stringResource(R.string.category_add_sub),
                onClick = { onAddSubcategory(form.id) },
                variant = ButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        OxButton(
            text = stringResource(if (viewModel.isNew) R.string.action_create else R.string.action_save),
            onClick = { viewModel.save(onDone) },
            modifier = Modifier.fillMaxWidth().padding(top = Space.s2),
        )

        // видалення — у самому низу, окремо від збереження, і лише через підтвердження
        if (!viewModel.isNew) OxButton(
            text = stringResource(R.string.action_delete),
            onClick = viewModel::askDelete,
            variant = ButtonVariant.Danger,
            modifier = Modifier.fillMaxWidth().padding(top = Space.s4),
        )
    }

    when (sheet) {
        Sheet.None -> Unit
        Sheet.Parent -> CategorySheet(
            categories = parents,
            selectedId = form.parentId,
            onPick = viewModel::setParent,
            onDismiss = { sheet = Sheet.None },
            title = stringResource(R.string.category_parent),
            noneLabel = stringResource(R.string.category_parent_none),
        )
        Sheet.Icon -> IconSheet(
            selected = form.icon,
            color = form.color,
            onPick = { icon -> viewModel.update { it.copy(icon = icon) } },
            onDismiss = { sheet = Sheet.None },
        )
    }

    impact?.let { it ->
        val isSub = form.parentId != null
        val text = buildList {
            if (it.subcategories > 0) {
                add(pluralStringResource(R.plurals.delete_category_children, it.subcategories, it.subcategories))
            }
            if (it.transactions > 0) add(
                if (isSub && it.parentName != null) {
                    pluralStringResource(R.plurals.delete_subcategory_tx, it.transactions, it.transactions, it.parentName)
                } else {
                    pluralStringResource(R.plurals.delete_category_tx, it.transactions, it.transactions)
                }
            )
            add(stringResource(R.string.delete_tx_text))
        }.joinToString(" ")

        OxConfirmDialog(
            title = stringResource(if (isSub) R.string.delete_subcategory_title else R.string.delete_category_title),
            text = text,
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { viewModel.delete(onDone) },
            onDismiss = viewModel::cancelDelete,
        )
    }
}

/** Вибір іконки сіткою; вибрана обведена. Колір — уже вибраний колір категорії, щоб одразу видно результат. */
@Composable
private fun IconSheet(selected: String, color: Int, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    OxSheet(title = stringResource(R.string.category_icon), onDismiss = onDismiss) { close ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(56.dp),
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
            modifier = Modifier.heightIn(max = 480.dp),
        ) {
            items(pickableIcons, key = { it }) { icon ->
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .then(
                            if (icon == selected) Modifier.border(BorderStroke(2.dp, Oxygen.colors.accentInk), CircleShape)
                            else Modifier
                        )
                        .clickable {
                            onPick(icon)
                            close()
                        },
                    contentAlignment = Alignment.Center,
                ) { IconDisc(icon, color, "", size = 44.dp) }
            }
        }
    }
}

package com.oxygen.financemanager.feature.accounts

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oxygen.financemanager.R
import com.oxygen.financemanager.data.db.AccountType
import com.oxygen.financemanager.ui.ButtonVariant
import com.oxygen.financemanager.ui.ColorRow
import com.oxygen.financemanager.ui.DatePickerField
import com.oxygen.financemanager.ui.Dropdown
import com.oxygen.financemanager.ui.OxButton
import com.oxygen.financemanager.ui.OxConfirmDialog
import com.oxygen.financemanager.ui.OxField
import com.oxygen.financemanager.ui.theme.Oxygen
import com.oxygen.financemanager.ui.theme.OxygenType
import com.oxygen.financemanager.ui.theme.Space

@Composable
fun AccountEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val showErrors by viewModel.showErrors.collectAsStateWithLifecycle()
    val transactionCount by viewModel.transactionCount.collectAsStateWithLifecycle()
    val errors = if (showErrors) form.errors() else emptySet()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s5),
    ) {
        OxField(
            label = stringResource(R.string.account_name),
            value = form.name,
            onValueChange = { name -> viewModel.update { it.copy(name = name) } },
            error = errorText(errors, AccountFormError.NAME_REQUIRED to R.string.err_name_required),
        )

        Dropdown(
            label = stringResource(R.string.account_type),
            options = AccountType.entries,
            selected = form.type,
            optionLabel = { stringResource(accountTypeLabel(it)) },
            // ціль і дедлайн є лише в скарбнички: при зміні типу вони скидаються, інакше
            // приховане поле блокувало б збереження невидимою помилкою
            onSelect = { type ->
                viewModel.update {
                    if (type == AccountType.SAVINGS) it.copy(type = type)
                    else it.copy(type = type, targetText = "", dueDate = null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Dropdown(
            label = stringResource(R.string.account_currency),
            options = SUPPORTED_CURRENCIES,
            selected = form.currency,
            optionLabel = { it },
            onSelect = { currency -> viewModel.update { it.copy(currency = currency) } },
            modifier = Modifier.fillMaxWidth(),
        )

        OxField(
            label = stringResource(R.string.account_opening_balance),
            value = form.openingText,
            onValueChange = { text -> viewModel.update { it.copy(openingText = text) } },
            helper = stringResource(R.string.account_opening_hint),
            error = errorText(errors, AccountFormError.OPENING_NOT_A_NUMBER to R.string.err_amount_nan),
            keyboardType = KeyboardType.Decimal,
            suffix = form.currency,
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.account_color), style = OxygenType.label, color = Oxygen.colors.ink)
            ColorRow(
                colors = PRESET_COLORS,
                selected = form.color,
                onSelect = { color -> viewModel.update { it.copy(color = color) } },
            )
        }

        if (form.type == AccountType.SAVINGS) {
            OxField(
                label = stringResource(R.string.account_target),
                value = form.targetText,
                onValueChange = { text -> viewModel.update { it.copy(targetText = text) } },
                helper = stringResource(R.string.account_target_hint),
                error = errorText(
                    errors,
                    AccountFormError.TARGET_NOT_A_NUMBER to R.string.err_amount_nan,
                    AccountFormError.TARGET_NOT_POSITIVE to R.string.err_amount_not_positive,
                ),
                keyboardType = KeyboardType.Decimal,
                suffix = form.currency,
            )
            DatePickerField(
                label = stringResource(R.string.account_due_date),
                date = form.dueDate,
                onPick = { date -> viewModel.update { it.copy(dueDate = date) } },
                clearable = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (viewModel.isNew) {
            OxButton(
                text = stringResource(R.string.action_create),
                onClick = { viewModel.save(onDone) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            // відмова/другорядне — зліва, підтвердження — справа
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                OxButton(
                    text = stringResource(R.string.account_archive),
                    onClick = { viewModel.archive(onDone) },
                    variant = ButtonVariant.Quiet,
                )
                OxButton(
                    text = stringResource(R.string.action_save),
                    onClick = { viewModel.save(onDone) },
                    modifier = Modifier.weight(1f),
                )
            }

            // видалення — у самому низу, окремо від збереження, і лише через підтвердження
            OxButton(
                text = stringResource(R.string.action_delete),
                onClick = { confirmDelete = true },
                variant = ButtonVariant.Danger,
                modifier = Modifier.fillMaxWidth().padding(top = Space.s6),
            )
        }
    }

    if (confirmDelete) {
        val name = form.name.trim()
        OxConfirmDialog(
            title = stringResource(
                if (form.type == AccountType.SAVINGS) R.string.delete_savings_title else R.string.delete_account_title
            ),
            text = if (transactionCount == 0) stringResource(R.string.delete_account_text_empty, name)
            else pluralStringResource(R.plurals.delete_account_text, transactionCount, name, transactionCount),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                confirmDelete = false
                viewModel.delete(onDone)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun errorText(errors: Set<AccountFormError>, vararg map: Pair<AccountFormError, Int>): String? =
    map.firstOrNull { it.first in errors }?.let { stringResource(it.second) }

@StringRes
fun accountTypeLabel(type: AccountType) = when (type) {
    AccountType.CASH -> R.string.account_type_cash
    AccountType.BANK -> R.string.account_type_bank
    AccountType.CARD -> R.string.account_type_card
    AccountType.SAVINGS -> R.string.account_type_savings
}

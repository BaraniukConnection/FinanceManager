package com.oxygen.financemanager.feature.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oxygen.financemanager.R
import com.oxygen.financemanager.core.currencySymbol
import com.oxygen.financemanager.core.formatAmountNumber
import com.oxygen.financemanager.data.db.AccountEntity
import com.oxygen.financemanager.data.db.CategoryKind
import com.oxygen.financemanager.data.db.TransactionType
import com.oxygen.financemanager.ui.ButtonVariant
import com.oxygen.financemanager.ui.IconDisc
import com.oxygen.financemanager.ui.OxButton
import com.oxygen.financemanager.ui.OxConfirmDialog
import com.oxygen.financemanager.ui.OxPanel
import com.oxygen.financemanager.ui.OxSegmented
import com.oxygen.financemanager.ui.PanelDivider
import com.oxygen.financemanager.ui.PanelRow
import com.oxygen.financemanager.ui.UiDisc
import com.oxygen.financemanager.ui.theme.Oxygen
import com.oxygen.financemanager.ui.theme.OxygenType
import com.oxygen.financemanager.ui.theme.Space
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Яка шторка відкрита. Одночасно — щонайбільше одна. */
private enum class Sheet { None, Amount, Account, Destination, Category, Date }

private val panelDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
fun TransactionEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val showErrors by viewModel.showErrors.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf(Sheet.None) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val account = accounts.firstOrNull { it.id == form.accountId }
    val destination = accounts.firstOrNull { it.id == form.destinationId }
    val category = categories.firstOrNull { it.id == form.categoryId }
    val errors = if (showErrors) form.errors(account?.currency) else emptySet()
    val isTransfer = form.type == TransactionType.TRANSFER
    // поки рахунку немає, валюта невідома — показуємо базову
    val currency = account?.currency ?: "EUR"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.s4)
            .padding(bottom = Space.s6),
        verticalArrangement = Arrangement.spacedBy(Space.s4),
    ) {
        AmountDisplay(
            amountText = form.amountText,
            currency = currency,
            error = amountError(errors)?.let { stringResource(it) },
            onClick = { sheet = Sheet.Amount },
        )

        OxSegmented(
            options = TransactionType.entries,
            selected = form.type,
            onSelect = viewModel::setType,
            label = { stringResource(typeLabel(it)) },
            icon = ::typeIcon,
        )

        OxPanel {
            PanelRow(
                label = stringResource(if (isTransfer) R.string.tx_account_from else R.string.tx_account),
                value = account?.name,
                placeholder = stringResource(R.string.tx_choose),
                isError = FormError.ACCOUNT_REQUIRED in errors,
                leading = { AccountDisc(account) },
                onClick = { sheet = Sheet.Account },
            )
            PanelDivider()
            if (isTransfer) {
                PanelRow(
                    label = stringResource(R.string.tx_account_to),
                    value = destination?.name,
                    placeholder = stringResource(R.string.tx_choose),
                    isError = errors.any { it.name.startsWith("DESTINATION") },
                    leading = { AccountDisc(destination) },
                    onClick = { sheet = Sheet.Destination },
                )
            } else {
                PanelRow(
                    label = stringResource(R.string.tx_category),
                    value = category?.name,
                    placeholder = stringResource(R.string.home_uncategorised),
                    leading = {
                        if (category != null) IconDisc(category.icon, category.color, category.name)
                        else UiDisc(Icons.Default.GridView)
                    },
                    onClick = { sheet = Sheet.Category },
                )
            }
            PanelDivider()
            PanelRow(
                label = stringResource(R.string.tx_date),
                value = dateLabel(form.date),
                placeholder = "",
                leading = { UiDisc(Icons.Default.CalendarToday) },
                onClick = { sheet = Sheet.Date },
            )
        }

        NotePanel(form.note) { note -> viewModel.update { it.copy(note = note) } }

        OxButton(
            text = stringResource(R.string.action_save),
            onClick = { viewModel.save(onDone) },
            enabled = accounts.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(top = Space.s2),
        )

        if (accounts.isEmpty()) Text(
            text = stringResource(R.string.tx_needs_account),
            style = OxygenType.caption,
            color = Oxygen.colors.inkMuted,
        )

        // видалення — у самому низу, окремо від збереження, і лише через підтвердження
        if (!viewModel.isNew) OxButton(
            text = stringResource(R.string.action_delete),
            onClick = { confirmDelete = true },
            variant = ButtonVariant.Danger,
            modifier = Modifier.fillMaxWidth().padding(top = Space.s4),
        )
    }

    when (sheet) {
        Sheet.None -> Unit
        Sheet.Amount -> CalculatorSheet(
            initial = form.amountText,
            currencySymbol = currencySymbol(currency),
            onConfirm = { result -> result?.let { text -> viewModel.update { it.copy(amountText = text) } } },
            onDismiss = { sheet = Sheet.None },
        )
        Sheet.Account -> AccountSheet(
            title = stringResource(if (isTransfer) R.string.tx_account_from else R.string.tx_account),
            accounts = summaries,
            selectedId = form.accountId,
            onPick = viewModel::setAccount,
            onDismiss = { sheet = Sheet.None },
        )
        Sheet.Destination -> AccountSheet(
            title = stringResource(R.string.tx_account_to),
            accounts = summaries.filter { it.account.id != form.accountId },
            selectedId = form.destinationId,
            onPick = { id -> viewModel.update { it.copy(destinationId = id) } },
            onDismiss = { sheet = Sheet.None },
        )
        Sheet.Category -> CategorySheet(
            // у витраті нема сенсу пропонувати «Зарплату»
            categories = categories.filter {
                it.kind == if (form.type == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
            },
            selectedId = form.categoryId,
            onPick = { id -> viewModel.update { it.copy(categoryId = id) } },
            onDismiss = { sheet = Sheet.None },
        )
        Sheet.Date -> DateSheet(
            date = form.date,
            onPick = { date -> viewModel.update { it.copy(date = date) } },
            onDismiss = { sheet = Sheet.None },
        )
    }

    if (confirmDelete) OxConfirmDialog(
        title = stringResource(R.string.delete_tx_title),
        text = stringResource(R.string.delete_tx_text),
        confirmLabel = stringResource(R.string.action_delete),
        onConfirm = {
            confirmDelete = false
            viewModel.delete(onDone)
        },
        onDismiss = { confirmDelete = false },
    )
}

/** Сума великим посередині; поки порожньо — напівпрозорий «0». Тап відкриває калькулятор. */
@Composable
private fun AmountDisplay(amountText: String, currency: String, error: String?, onClick: () -> Unit) {
    val c = Oxygen.colors
    val shown = formatAmountNumber(amountText, currency)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = Space.s6),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row {
            Text(
                text = shown ?: amountText.ifEmpty { "0" },
                style = OxygenType.display,
                color = if (amountText.isEmpty()) c.ink.copy(alpha = 0.35f) else c.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alignByBaseline().weight(1f, fill = false),
            )
            Text(
                text = currencySymbol(currency),
                style = OxygenType.screenTitle,
                color = c.inkMuted,
                modifier = Modifier.alignByBaseline().padding(start = Space.s2),
            )
        }
        if (error != null) Text(error, style = OxygenType.caption, color = c.dangerInk, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AccountDisc(account: AccountEntity?) {
    if (account != null) IconDisc(account.icon, account.color, account.name)
    else UiDisc(Icons.Default.Wallet)
}

/** Нотатка — окремою панеллю, пише прямо в ній, без рамки поля. */
@Composable
private fun NotePanel(note: String, onChange: (String) -> Unit) {
    val c = Oxygen.colors
    OxPanel {
        Column(Modifier.padding(Space.s4), verticalArrangement = Arrangement.spacedBy(Space.s1)) {
            Text(stringResource(R.string.tx_note), style = OxygenType.caption, color = c.inkMuted)
            BasicTextField(
                value = note,
                onValueChange = onChange,
                textStyle = OxygenType.bodyLg.copy(color = c.ink),
                cursorBrush = SolidColor(c.accentInk),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { field ->
                    if (note.isEmpty()) Text(
                        stringResource(R.string.tx_note_placeholder),
                        style = OxygenType.bodyLg,
                        color = c.inkMuted.copy(alpha = 0.6f),
                    )
                    field()
                },
            )
        }
    }
}

@Composable
private fun dateLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.day_today)
        today.minusDays(1) -> stringResource(R.string.day_yesterday)
        else -> date.format(panelDate)
    }
}

private fun amountError(errors: Set<FormError>) = when {
    FormError.AMOUNT_EMPTY in errors -> R.string.err_amount_empty
    FormError.AMOUNT_NOT_A_NUMBER in errors -> R.string.err_amount_nan
    FormError.AMOUNT_NOT_POSITIVE in errors -> R.string.err_amount_not_positive
    else -> null
}

private fun typeLabel(type: TransactionType) = when (type) {
    TransactionType.EXPENSE -> R.string.tx_type_expense
    TransactionType.INCOME -> R.string.tx_type_income
    TransactionType.TRANSFER -> R.string.tx_type_transfer
}

/** «−» витрата, «+» дохід, стрілки в обидва боки — переказ. */
private fun typeIcon(type: TransactionType) = when (type) {
    TransactionType.EXPENSE -> Icons.Default.Remove
    TransactionType.INCOME -> Icons.Default.Add
    TransactionType.TRANSFER -> Icons.Default.SwapHoriz
}

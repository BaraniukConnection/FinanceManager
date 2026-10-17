package com.oxygen.financemanager.feature.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oxygen.financemanager.R
import com.oxygen.financemanager.core.formatMinor
import com.oxygen.financemanager.data.db.TransactionType
import com.oxygen.financemanager.feature.home.CurrencyTotal
import com.oxygen.financemanager.ui.ChipRow
import com.oxygen.financemanager.ui.IconDisc
import com.oxygen.financemanager.ui.OxIconButton
import com.oxygen.financemanager.ui.SectionHeader
import com.oxygen.financemanager.ui.theme.Oxygen
import com.oxygen.financemanager.ui.theme.OxygenType
import com.oxygen.financemanager.ui.theme.Space
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val margin = Modifier.padding(horizontal = Space.s4)

/** Заголовок дня — як люди пишуть дати: 12.07. понеділок. */
private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM. EEEE")

@Composable
fun TransactionsScreen(
    onOpenTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var moreFilters by rememberSaveable { mutableStateOf(state.filter.hasSecondary) }
    // заголовки днів — лише коли днів щонайменше два; інакше дата йде в сам рядок
    val headed = state.days.size > 1

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Space.bottomInset)) {
        item {
            Filters(
                state = state,
                moreFilters = moreFilters,
                onToggleMore = {
                    if (moreFilters) viewModel.clearSecondary()
                    moreFilters = !moreFilters
                },
                viewModel = viewModel,
            )
        }

        if (!state.loading && state.days.isEmpty()) item {
            Text(
                text = stringResource(R.string.tx_empty_filtered),
                style = OxygenType.body,
                color = Oxygen.colors.inkMuted,
                modifier = margin.padding(top = Space.s6),
            )
        }

        state.days.forEach { day ->
            if (headed) item(key = "day-${day.date}") { SectionHeader(dayLabel(day.date), margin) }
            items(day.rows, key = { "tx-${it.transaction.id}" }) { row ->
                TransactionItem(row, margin, showDate = !headed, onClick = { onOpenTransaction(row.transaction.id) })
            }
        }
    }
}

@Composable
private fun Filters(
    state: TransactionsState,
    moreFilters: Boolean,
    onToggleMore: () -> Unit,
    viewModel: TransactionsViewModel,
) {
    val filter = state.filter
    Column(Modifier.padding(top = Space.s2), verticalArrangement = Arrangement.spacedBy(Space.s2)) {
        ChipRow(
            edgePadding = Space.s4,
            options = Period.entries,
            selected = filter.period,
            optionLabel = { stringResource(periodLabel(it)) },
            onSelect = { picked -> picked?.let(viewModel::setPeriod) },
            required = true,
        )

        Row(
            modifier = Modifier.padding(end = Space.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            ChipRow(
                edgePadding = Space.s4,
                options = listOf(null) + TransactionType.entries,
                selected = filter.type,
                optionLabel = { type -> stringResource(type?.let(::typeLabel) ?: R.string.filter_all_types) },
                onSelect = viewModel::setType,
                required = true,
                modifier = Modifier.weight(1f),
            )
            OxIconButton(
                icon = Icons.Default.Tune,
                label = stringResource(if (moreFilters) R.string.action_fewer_filters else R.string.action_more_filters),
                onClick = onToggleMore,
            )
        }

        AnimatedVisibility(moreFilters) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
                ChipRow(
                    edgePadding = Space.s4,
                    options = listOf(null) + state.accounts,
                    selected = state.accounts.firstOrNull { it.id == filter.accountId },
                    optionLabel = { it?.name ?: stringResource(R.string.filter_all_accounts) },
                    onSelect = { viewModel.setAccount(it?.id) },
                    required = true,
                    leading = { account, selected ->
                        if (account == null) AllDisc(Icons.Default.Wallet)
                        else IconDisc(account.icon, account.color.takeUnless { selected }, account.name, size = ChipDisc)
                    },
                )
                if (state.categories.isNotEmpty()) ChipRow(
                    edgePadding = Space.s4,
                    options = listOf(null) + state.categories,
                    selected = state.categories.firstOrNull { it.id == filter.categoryId },
                    optionLabel = { it?.name ?: stringResource(R.string.filter_all_categories) },
                    onSelect = { viewModel.setCategory(it?.id) },
                    required = true,
                    leading = { category, selected ->
                        if (category == null) AllDisc(Icons.Default.GridView)
                        else IconDisc(category.icon, category.color.takeUnless { selected }, category.name, size = ChipDisc)
                    },
                )
            }
        }

        Summary(state.spent, state.received, margin)
    }
}

/** Іконка в капсулі фільтра. На вибраній (м'ятній) капсулі диск без кольору мітки — інакше м'ята зливається з м'ятою. */
private val ChipDisc = 28.dp

@Composable
private fun AllDisc(icon: ImageVector) {
    Box(
        Modifier.size(ChipDisc).clip(CircleShape).background(Oxygen.colors.accentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Oxygen.colors.accentInk, modifier = Modifier.size(16.dp))
    }
}

/** Скільки витрачено й отримано за фільтром. Перекази не рахуються — це рух між своїми рахунками. */
@Composable
private fun Summary(spent: List<CurrencyTotal>, received: List<CurrencyTotal>, modifier: Modifier) {
    fun List<CurrencyTotal>.text() = joinToString(", ") { formatMinor(it.amountMinor, it.currency) }
    val parts = buildList {
        if (spent.isNotEmpty()) add(stringResource(R.string.tx_summary_spent, spent.text()))
        if (received.isNotEmpty()) add(stringResource(R.string.tx_summary_received, received.text()))
    }
    if (parts.isNotEmpty()) Text(
        text = parts.joinToString(" · "),
        style = OxygenType.body,
        color = Oxygen.colors.inkMuted,
        modifier = modifier.padding(top = Space.s1),
    )
}

@Composable
private fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.day_today)
        today.minusDays(1) -> stringResource(R.string.day_yesterday)
        else -> date.format(dayFormat)
    }
}

private fun periodLabel(period: Period) = when (period) {
    Period.THIS_MONTH -> R.string.period_this_month
    Period.LAST_MONTH -> R.string.period_last_month
    Period.THIS_YEAR -> R.string.period_this_year
    Period.ALL -> R.string.period_all
}

private fun typeLabel(type: TransactionType) = when (type) {
    TransactionType.EXPENSE -> R.string.tx_type_expense
    TransactionType.INCOME -> R.string.tx_type_income
    TransactionType.TRANSFER -> R.string.tx_type_transfer
}

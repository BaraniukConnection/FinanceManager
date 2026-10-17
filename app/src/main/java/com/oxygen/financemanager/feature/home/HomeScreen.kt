package com.oxygen.financemanager.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import com.oxygen.financemanager.feature.accounts.AccountCard
import com.oxygen.financemanager.feature.accounts.AccountCardHeight
import com.oxygen.financemanager.ui.dashedBorder
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oxygen.financemanager.R
import com.oxygen.financemanager.core.formatMinor
import com.oxygen.financemanager.feature.stats.StatsActions
import com.oxygen.financemanager.feature.stats.StatsPanel
import com.oxygen.financemanager.feature.transactions.TransactionItem
import com.oxygen.financemanager.feature.transactions.shortDate
import com.oxygen.financemanager.ui.ButtonVariant
import com.oxygen.financemanager.ui.IconDisc
import com.oxygen.financemanager.ui.OxButton
import com.oxygen.financemanager.ui.OxCard
import com.oxygen.financemanager.ui.OxListItem
import com.oxygen.financemanager.ui.SectionHeader
import com.oxygen.financemanager.ui.theme.Oxygen
import com.oxygen.financemanager.ui.theme.OxygenType
import com.oxygen.financemanager.ui.theme.Space

private val margin = Modifier.padding(horizontal = Space.s4)

@Composable
fun HomeScreen(
    onAddAccount: () -> Unit,
    onOpenAccount: (Long) -> Unit,
    onOpenTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var showAllRecent by rememberSaveable { mutableStateOf(false) }

    if (state.isEmpty) {
        EmptyHome(onAddAccount, modifier)
        return
    }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Space.bottomInset)) {
        item { Balance(state.totals) }

        // карусель є завжди: у її кінці живе «новий рахунок», навіть коли звичайних рахунків ще немає
        item { SectionHeader(stringResource(R.string.home_accounts), margin) }
        item { AccountCarousel(state.spending, onOpenAccount, onAddAccount) }

        if (state.savings.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.home_savings), margin) }
            item { SavingsPanel(state.savings, onOpenAccount) }
        }

        item { SectionHeader(stringResource(R.string.home_recent), margin) }
        if (state.recent.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_no_transactions),
                    style = OxygenType.body,
                    color = Oxygen.colors.inkMuted,
                    modifier = margin.padding(top = Space.s2),
                )
            }
        } else {
            val shown = if (showAllRecent) state.recent else state.recent.take(RECENT_COLLAPSED)
            items(shown, key = { "tx-${it.transaction.id}" }) { row ->
                TransactionItem(row, margin, onClick = { onOpenTransaction(row.transaction.id) })
            }
            if (state.recent.size > RECENT_COLLAPSED) item {
                ShowMore(expanded = showAllRecent, onToggle = { showAllRecent = !showAllRecent })
            }
        }

        item { SectionHeader(stringResource(R.string.home_statistics), margin) }
        item {
            StatsPanel(
                state = stats,
                actions = StatsActions(
                    onPeriod = viewModel::setStatsPeriod,
                    onType = viewModel::setStatsType,
                    onAccount = viewModel::setStatsAccount,
                    onCategory = viewModel::setStatsCategory,
                    onClearSecondary = viewModel::clearStatsSecondary,
                ),
                modifier = margin.padding(vertical = Space.s1),
            )
        }
    }
}

/** «Показати більше / Згорнути» під останніми. Шеврон обертається, як усі шеврони в Oxygen. */
@Composable
private fun ShowMore(expanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(160), label = "more")
    Box(Modifier.fillMaxWidth().padding(top = Space.s1), contentAlignment = Alignment.Center) {
        Surface(
            onClick = onToggle,
            shape = CircleShape,
            color = Oxygen.colors.surfaceVariant,
            contentColor = Oxygen.colors.accentInk,
        ) {
            Row(
                modifier = Modifier.padding(start = Space.s4, end = Space.s3, top = Space.s2, bottom = Space.s2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s1),
            ) {
                Text(
                    text = stringResource(if (expanded) R.string.home_show_less else R.string.home_show_more),
                    style = OxygenType.label,
                )
                Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(20.dp).rotate(rotation))
            }
        }
    }
}

/** Єдиний заголовок екрана — загальний баланс. Кілька валют — кілька рядків, без вигаданої конвертації. */
@Composable
private fun Balance(totals: List<CurrencyTotal>) {
    Column(margin.padding(top = Space.s2)) {
        Text(
            text = stringResource(R.string.home_balance).uppercase(),
            style = OxygenType.overline,
            color = Oxygen.colors.inkMuted,
        )
        totals.forEachIndexed { index, total ->
            Text(
                text = formatMinor(total.amountMinor, total.currency),
                style = if (index == 0) OxygenType.display else OxygenType.screenTitle,
                color = Oxygen.colors.ink,
            )
        }
    }
}

@Composable
private fun AccountCarousel(accounts: List<AccountSummary>, onOpen: (Long) -> Unit, onAdd: () -> Unit) {
    val listState = rememberLazyListState()
    // яка частка «нового рахунку» вже на екрані: 0 — щойно показався край, 1 — видно повністю
    val reveal by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == info.totalItemsCount - 1 } ?: return@derivedStateOf 0f
            val visible = minOf(item.offset + item.size, info.viewportEndOffset) - maxOf(item.offset, info.viewportStartOffset)
            (visible.toFloat() / item.size).coerceIn(0f, 1f)
        }
    }

    // власні відступи, щоб картки прокручувались до краю, а тіні не обрізались
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = Space.s4, vertical = Space.s1),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        items(accounts, key = { it.account.id }) { summary ->
            AccountCard(summary, onClick = { onOpen(summary.account.id) }, modifier = Modifier.width(200.dp))
        }
        // без ключа навмисно: на першому кадрі рахунки ще не завантажені й ця кнопка — єдина;
        // з постійним ключем список тримався б за неї і відкривався прокрученим у кінець
        item { AddAccountBubble(reveal, onAdd) }
    }
}

/**
 * «Новий рахунок» у кінці каруселі. Поки край лише показується, надувається бульбашка з плюсом;
 * коли вона вже повна — навколо проявляється пунктирна панель з підписом.
 * Рух прив'язаний до прокрутки, а не до таймера, тож іде за пальцем.
 */
@Composable
private fun AddAccountBubble(reveal: Float, onAdd: () -> Unit) {
    val c = Oxygen.colors
    val bubble = FastOutSlowInEasing.transform((reveal / 0.6f).coerceIn(0f, 1f))
    val panel = ((reveal - 0.6f) / 0.4f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .width(140.dp)
            .height(AccountCardHeight)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onAdd),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = panel
                    scaleX = 0.92f + 0.08f * panel
                    scaleY = 0.92f + 0.08f * panel
                }
                .dashedBorder(c.inkMuted, radius = 12.dp)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.s2)) {
            Box(
                Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = 0.25f + 0.75f * bubble
                        scaleY = 0.25f + 0.75f * bubble
                        alpha = bubble
                    }
                    .clip(CircleShape)
                    .background(c.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = c.accentInk)
            }
            Text(
                text = stringResource(R.string.account_add),
                style = OxygenType.label,
                color = c.inkMuted,
                modifier = Modifier.graphicsLayer { alpha = panel },
            )
        }
    }
}

/**
 * Скарбнички — на власній панелі з тінню, як і картки рахунків, щоб не зливались зі списком.
 * Згорнута панель одразу каже головне: скільки відкладено і в скількох скарбничках.
 */
@Composable
private fun SavingsPanel(savings: List<AccountSummary>, onOpen: (Long) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    // шеврони повертаються на 90° при розгортанні — як у деревах Oxygen
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, tween(160), label = "savings")
    val saved = savings
        .groupBy { it.account.currency }
        .map { (currency, group) -> formatMinor(group.sumOf { it.balanceMinor }, currency) }
        .joinToString(" · ")

    OxCard(margin.fillMaxWidth().padding(vertical = Space.s1)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(saved, style = OxygenType.screenTitle, color = Oxygen.colors.ink)
                Text(
                    text = pluralStringResource(R.plurals.home_savings_count, savings.size, savings.size),
                    style = OxygenType.caption,
                    color = Oxygen.colors.inkMuted,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Oxygen.colors.inkMuted,
                modifier = Modifier.size(22.dp).rotate(rotation),
            )
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(top = Space.s3)) {
                HorizontalDivider(color = Oxygen.colors.line)
                savings.forEach { SavingsItem(it, onOpen) }
            }
        }
    }
}

@Composable
private fun SavingsItem(summary: AccountSummary, onOpen: (Long) -> Unit) {
    val account = summary.account
    val target = account.targetMinor
    val supporting = buildList {
        if (target != null) {
            val percent = (summary.balanceMinor * 100 / target).coerceIn(0, 100)
            add(stringResource(R.string.home_goal_progress, percent.toInt(), formatMinor(target, account.currency)))
        }
        account.dueDate?.let { add(stringResource(R.string.home_due, it.format(shortDate))) }
    }.joinToString(" · ").ifEmpty { null }

    Column {
        OxListItem(
            title = account.name,
            supporting = supporting,
            leading = { IconDisc(account.icon, account.color, account.name) },
            trailing = {
                Text(
                    text = formatMinor(summary.balanceMinor, account.currency),
                    style = OxygenType.title,
                    color = Oxygen.colors.ink,
                    maxLines = 1,
                )
            },
            onClick = { onOpen(account.id) },
        )
        if (target != null) LinearProgressIndicator(
            progress = { (summary.balanceMinor.toFloat() / target).coerceIn(0f, 1f) },
            color = Oxygen.colors.accentInk,
            trackColor = Oxygen.colors.surfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(start = 56.dp, bottom = Space.s2),
        )
    }
}

@Composable
private fun EmptyHome(onAddAccount: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(Space.s8),
        verticalArrangement = Arrangement.spacedBy(Space.s3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.home_empty_title), style = OxygenType.screenTitle, color = Oxygen.colors.ink)
        Text(stringResource(R.string.home_empty_body), style = OxygenType.body, color = Oxygen.colors.inkMuted)
        // заливка accent на екрані одна — вона у кнопки створення внизу, тож ця кнопка secondary
        OxButton(
            text = stringResource(R.string.account_add),
            onClick = onAddAccount,
            variant = ButtonVariant.Secondary,
            modifier = Modifier.padding(top = Space.s3),
        )
    }
}

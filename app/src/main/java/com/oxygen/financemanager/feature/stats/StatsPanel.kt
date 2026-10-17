package com.oxygen.financemanager.feature.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oxygen.financemanager.R
import com.oxygen.financemanager.core.formatMinor
import com.oxygen.financemanager.data.db.TransactionType
import com.oxygen.financemanager.feature.transactions.Period
import com.oxygen.financemanager.ui.ChipRow
import com.oxygen.financemanager.ui.IconDisc
import com.oxygen.financemanager.ui.OxIconButton
import com.oxygen.financemanager.ui.OxPanel
import com.oxygen.financemanager.ui.OxSegmented
import com.oxygen.financemanager.ui.UiDisc
import com.oxygen.financemanager.ui.theme.Mark
import com.oxygen.financemanager.ui.theme.Oxygen
import com.oxygen.financemanager.ui.theme.OxygenType
import com.oxygen.financemanager.ui.theme.Space

private enum class ChartKind { Donut, Bars }

/** Поведінка панелі — окремо від ViewModel, щоб панель не знала, звідки беруться дані. */
class StatsActions(
    val onPeriod: (Period) -> Unit,
    val onType: (TransactionType) -> Unit,
    val onAccount: (Long?) -> Unit,
    val onCategory: (Long?) -> Unit,
    val onClearSecondary: () -> Unit,
)

/**
 * Панель статистики: розподіл за категоріями кільцем або стовпчиками.
 * Угорі праворуч — перемикач діаграми; внизу — період і кнопка, що розгортає
 * решту фільтрів (тип, рахунок, категорія), як на екрані транзакцій.
 */
@Composable
fun StatsPanel(state: StatsState, actions: StatsActions, modifier: Modifier = Modifier) {
    val c = Oxygen.colors
    var chart by rememberSaveable { mutableStateOf(ChartKind.Donut) }
    var moreOptions by rememberSaveable { mutableStateOf(state.filter.hasSecondary) }
    val filter = state.filter
    val currency = state.currency ?: "EUR"

    OxPanel(modifier) {
        Column(Modifier.padding(Space.s4), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        if (filter.type == TransactionType.INCOME) R.string.stats_title_income else R.string.stats_title_expense
                    ),
                    style = OxygenType.title,
                    color = c.ink,
                    modifier = Modifier.weight(1f),
                )
                ChartToggle(chart, onChange = { chart = it })
            }

            if (state.slices.isEmpty()) {
                Text(
                    text = stringResource(R.string.stats_empty),
                    style = OxygenType.body,
                    color = c.inkMuted,
                    modifier = Modifier.padding(vertical = Space.s6),
                )
            } else {
                val center = formatMinor(state.totalMinor, currency)
                when (chart) {
                    ChartKind.Donut -> Donut(
                        slices = state.slices,
                        total = state.totalMinor,
                        centerLabel = center,
                        centerCaption = stringResource(
                            if (filter.type == TransactionType.INCOME) R.string.stats_received else R.string.stats_spent
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    ChartKind.Bars -> Bars(state.slices)
                }
                Legend(state.slices, state.totalMinor, currency)
                if (state.otherCurrencies.isNotEmpty()) Text(
                    text = stringResource(R.string.stats_only_currency, currency, state.otherCurrencies.joinToString(", ")),
                    style = OxygenType.caption,
                    color = c.inkMuted,
                )
            }

            // внизу: період таблетками зліва, налаштування справа
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                ChipRow(
                    options = Period.entries,
                    selected = filter.period,
                    optionLabel = { stringResource(periodLabel(it)) },
                    onSelect = { picked -> picked?.let(actions.onPeriod) },
                    required = true,
                    modifier = Modifier.weight(1f),
                )
                OxIconButton(
                    icon = Icons.Default.Tune,
                    label = stringResource(if (moreOptions) R.string.action_fewer_filters else R.string.action_more_filters),
                    onClick = {
                        // що не видно — те не діє
                        if (moreOptions) actions.onClearSecondary()
                        moreOptions = !moreOptions
                    },
                )
            }

            AnimatedVisibility(moreOptions) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s3)) {
                    OxSegmented(
                        options = listOf(TransactionType.EXPENSE, TransactionType.INCOME),
                        selected = filter.type ?: TransactionType.EXPENSE,
                        onSelect = actions.onType,
                        label = { stringResource(if (it == TransactionType.INCOME) R.string.tx_type_income else R.string.tx_type_expense) },
                        icon = { if (it == TransactionType.INCOME) Icons.Default.Add else Icons.Default.Remove },
                    )
                    ChipRow(
                        options = listOf(null) + state.accounts,
                        selected = state.accounts.firstOrNull { it.id == filter.accountId },
                        optionLabel = { it?.name ?: stringResource(R.string.filter_all_accounts) },
                        onSelect = { actions.onAccount(it?.id) },
                        required = true,
                        leading = { account, selected ->
                            if (account == null) UiDisc(Icons.Default.Wallet, 28.dp)
                            else IconDisc(account.icon, account.color.takeUnless { selected }, account.name, size = 28.dp)
                        },
                    )
                    if (state.categories.isNotEmpty()) ChipRow(
                        options = listOf(null) + state.categories,
                        selected = state.categories.firstOrNull { it.id == filter.categoryId },
                        optionLabel = { it?.name ?: stringResource(R.string.filter_all_categories) },
                        onSelect = { actions.onCategory(it?.id) },
                        required = true,
                        leading = { category, selected ->
                            if (category == null) UiDisc(Icons.Default.GridView, 28.dp)
                            else IconDisc(category.icon, category.color.takeUnless { selected }, category.name, size = 28.dp)
                        },
                    )
                }
            }
        }
    }
}

/** Колір частки: колір категорії; «без категорії» і «інше» — приглушені, щоб не тягнули увагу. */
@Composable
private fun sliceColor(slice: Slice): Color = when {
    slice.isOther -> Color(Mark.SLATE).copy(alpha = 0.55f)
    slice.color == null -> Oxygen.colors.inkMuted.copy(alpha = 0.45f)
    else -> Color(slice.color)
}

@Composable
private fun sliceName(slice: Slice): String = when {
    slice.isOther -> stringResource(R.string.stats_other)
    slice.name == null -> stringResource(R.string.home_uncategorised)
    else -> slice.name
}

/** Проростання діаграми при зміні даних — тихо й швидко, без відскоків. */
@Composable
private fun growth(key: Any): Float {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { progress.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
    return progress.value
}

@Composable
private fun Donut(
    slices: List<Slice>,
    total: Long,
    centerLabel: String,
    centerCaption: String,
    modifier: Modifier = Modifier,
) {
    val colors = slices.map { sliceColor(it) }
    val progress = growth(slices)
    Box(modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 26.dp.toPx()
            // невеликий проміжок між частками, щоб сусідні однакові кольори не зливались
            val gap = if (slices.size > 1) 1.5f else 0f
            var start = -90f
            slices.forEachIndexed { i, slice ->
                val sweep = 360f * slice.amountMinor / total * progress
                drawArc(
                    color = colors[i],
                    startAngle = start + gap / 2,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke),
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerLabel, style = OxygenType.title, color = Oxygen.colors.ink, maxLines = 1)
            Text(centerCaption, style = OxygenType.caption, color = Oxygen.colors.inkMuted)
        }
    }
}

@Composable
private fun Bars(slices: List<Slice>) {
    val max = slices.maxOf { it.amountMinor }.toFloat()
    val progress = growth(slices)
    Row(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        slices.forEach { slice ->
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.62f)
                            .fillMaxHeight((slice.amountMinor / max * progress).coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(sliceColor(slice))
                    )
                }
                // під стовпчиком — іконка категорії: назви довгі, а легенда нижче і так їх каже
                Box(Modifier.padding(top = Space.s2)) {
                    when {
                        slice.isOther -> UiDisc(Icons.Default.MoreHoriz, 28.dp)
                        else -> IconDisc(slice.icon, slice.color, sliceName(slice), size = 28.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend(slices: List<Slice>, total: Long, currency: String) {
    val c = Oxygen.colors
    Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
        slices.forEach { slice ->
            val share = slice.amountMinor * 100.0 / total
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(sliceColor(slice)))
                Text(
                    text = sliceName(slice),
                    style = OxygenType.body,
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(formatMinor(slice.amountMinor, currency), style = OxygenType.label, color = c.ink, maxLines = 1)
                Text(
                    text = if (share in 0.0..<1.0) "<1%" else "${Math.round(share)}%",
                    style = OxygenType.label,
                    color = c.inkMuted,
                    modifier = Modifier.width(40.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Маленький перемикач «кільце / стовпчики» у правому верхньому куті. */
@Composable
private fun ChartToggle(selected: ChartKind, onChange: (ChartKind) -> Unit) {
    val c = Oxygen.colors
    Surface(shape = CircleShape, color = c.surfaceVariant) {
        Row(Modifier.padding(3.dp)) {
            listOf(
                Triple(ChartKind.Donut, Icons.Default.DonutLarge, R.string.stats_chart_donut),
                Triple(ChartKind.Bars, Icons.Default.BarChart, R.string.stats_chart_bars),
            ).forEach { (kind, icon, label) -> ToggleOption(kind == selected, icon, stringResource(label)) { onChange(kind) } }
        }
    }
}

@Composable
private fun ToggleOption(selected: Boolean, icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = Oxygen.colors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) c.accent else Color.Transparent,
        contentColor = if (selected) c.onAccent else c.inkMuted,
        modifier = Modifier.size(38.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp)) }
    }
}

private fun periodLabel(period: Period) = when (period) {
    Period.THIS_MONTH -> R.string.period_this_month
    Period.LAST_MONTH -> R.string.period_last_month
    Period.THIS_YEAR -> R.string.period_this_year
    Period.ALL -> R.string.period_all
}

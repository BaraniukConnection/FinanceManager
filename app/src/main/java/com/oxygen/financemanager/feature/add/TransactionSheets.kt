package com.oxygen.financemanager.feature.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oxygen.financemanager.R
import com.oxygen.financemanager.core.CalcKey
import com.oxygen.financemanager.core.Calculator
import com.oxygen.financemanager.data.db.AccountType
import com.oxygen.financemanager.data.db.CategoryEntity
import com.oxygen.financemanager.feature.accounts.AccountCard
import com.oxygen.financemanager.feature.categories.CategoryGroup
import com.oxygen.financemanager.feature.home.AccountSummary
import com.oxygen.financemanager.ui.IconDisc
import com.oxygen.financemanager.ui.OxSheet
import com.oxygen.financemanager.ui.SectionHeader
import com.oxygen.financemanager.ui.theme.Oxygen
import com.oxygen.financemanager.ui.theme.OxygenType
import com.oxygen.financemanager.ui.theme.Space
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// ---------- Калькулятор ----------

/**
 * Шторка-калькулятор для суми. «=» рахує, «Підтвердити» приймає результат;
 * якщо дія не завершена (12+5), підтвердження спершу її дораховує.
 * [onConfirm] отримує `null`, коли вираз порожній — тоді сума не змінюється.
 */
@Composable
fun CalculatorSheet(
    initial: String,
    currencySymbol: String,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Oxygen.colors
    // поточна сума — це вже валідний вираз; кома з поля перетворюється на крапку калькулятора
    var expr by rememberSaveable {
        mutableStateOf(initial.replace(',', '.').takeIf { it.toBigDecimalOrNull() != null } ?: "")
    }
    var divideByZero by rememberSaveable { mutableStateOf(false) }
    val separator = DecimalFormatSymbols.getInstance().decimalSeparator
    fun show(text: String) = text.replace('.', separator)

    fun press(key: CalcKey) {
        divideByZero = false
        val next = Calculator.press(expr, key)
        // «=» на ділення на нуль лишає вираз як є — показуємо чому
        if (key == CalcKey.Equals && next == expr && expr.isNotEmpty() && Calculator.evaluate(expr) == null) {
            divideByZero = true
        }
        expr = next
    }

    OxSheet(title = stringResource(R.string.tx_amount), onDismiss = onDismiss) { close ->
        val preview = Calculator.evaluate(expr)?.let(Calculator::format)
        val hasOperator = expr.any(Calculator::isOp)

        // вираз — дрібно зверху, результат — великим; обидва праворуч, як на справжньому калькуляторі
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Text(
                text = if (hasOperator) show(expr) else " ",
                style = OxygenType.bodyLg,
                color = c.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = show(preview ?: expr.ifEmpty { "0" }),
                    style = OxygenType.display,
                    color = if (expr.isEmpty()) c.ink.copy(alpha = 0.35f) else c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(currencySymbol, style = OxygenType.screenTitle, color = c.inkMuted, modifier = Modifier.padding(start = Space.s2))
            }
            Text(
                text = if (divideByZero) stringResource(R.string.calc_divide_by_zero) else " ",
                style = OxygenType.caption,
                color = c.dangerInk,
            )
        }

        val keyRow: @Composable (@Composable RowScope.() -> Unit) -> Unit = { keys ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s2), content = keys)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
            keyRow {
                CalcButton("C", Kind.Tool) { press(CalcKey.Clear) }
                CalcButton(icon = Icons.AutoMirrored.Filled.Backspace, kind = Kind.Tool) { press(CalcKey.Backspace) }
                CalcButton("${Calculator.DIVIDE}", Kind.Op) { press(CalcKey.Op(Calculator.DIVIDE)) }
                CalcButton("${Calculator.TIMES}", Kind.Op) { press(CalcKey.Op(Calculator.TIMES)) }
            }
            for (row in listOf("789", "456", "123")) keyRow {
                row.forEach { d -> CalcButton("$d", Kind.Digit) { press(CalcKey.Digit(d)) } }
                when (row) {
                    "789" -> CalcButton("${Calculator.MINUS}", Kind.Op) { press(CalcKey.Op(Calculator.MINUS)) }
                    "456" -> CalcButton("${Calculator.PLUS}", Kind.Op) { press(CalcKey.Op(Calculator.PLUS)) }
                    else -> CalcButton("=", Kind.Op) { press(CalcKey.Equals) }
                }
            }
            keyRow {
                CalcButton("$separator", Kind.Digit) { press(CalcKey.Dot) }
                CalcButton("0", Kind.Digit) { press(CalcKey.Digit('0')) }
                CalcButton(
                    label = stringResource(R.string.action_confirm),
                    icon = Icons.Default.Check,
                    kind = Kind.Confirm,
                    span = 2f,
                ) {
                    when {
                        expr.isBlank() -> { onConfirm(null); close() }
                        else -> when (val result = Calculator.evaluate(expr)) {
                            null -> divideByZero = true
                            else -> { onConfirm(Calculator.format(result)); close() }
                        }
                    }
                }
            }
        }
    }
}

private enum class Kind { Digit, Op, Tool, Confirm }

@Composable
private fun RowScope.CalcButton(
    label: String? = null,
    kind: Kind,
    icon: ImageVector? = null,
    span: Float = 1f,
    onClick: () -> Unit,
) {
    val c = Oxygen.colors
    val (container, content) = when (kind) {
        Kind.Digit -> c.surfaceVariant to c.ink
        Kind.Op -> c.accentSoft to c.accentInk
        Kind.Tool -> c.surfaceVariant to c.inkMuted
        Kind.Confirm -> c.accent to c.onAccent
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = container,
        contentColor = content,
        modifier = Modifier.weight(span).height(60.dp),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, contentDescription = label ?: stringResource(R.string.calc_backspace), modifier = Modifier.size(22.dp))
            if (label != null) Text(
                text = label,
                style = if (kind == Kind.Digit || kind == Kind.Op) OxygenType.screenTitle else OxygenType.button,
                modifier = Modifier.padding(start = if (icon != null) Space.s2 else 0.dp),
            )
        }
    }
}

// ---------- Рахунок ----------

/** Вибір рахунку картками — як на екрані рахунків: звичайні окремо, скарбнички окремо. */
@Composable
fun AccountSheet(
    title: String,
    accounts: List<AccountSummary>,
    selectedId: Long?,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val (savings, spending) = accounts.partition { it.account.type == AccountType.SAVINGS }
    val headed = savings.isNotEmpty() && spending.isNotEmpty()

    OxSheet(title = title, onDismiss = onDismiss) { close ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(Space.s3),
            verticalArrangement = Arrangement.spacedBy(Space.s3),
            modifier = Modifier.heightIn(max = 560.dp),
        ) {
            listOf(R.string.home_accounts to spending, R.string.home_savings to savings).forEach { (header, group) ->
                if (group.isEmpty()) return@forEach
                if (headed) item(key = "h-$header", span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(stringResource(header), Modifier.padding(top = 0.dp))
                }
                items(group, key = { it.account.id }) { summary ->
                    AccountCard(
                        summary = summary,
                        selected = summary.account.id == selectedId,
                        onClick = {
                            onPick(summary.account.id)
                            close()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

// ---------- Категорія ----------

/**
 * Категорії таблетками, як на екрані категорій. Батьківські згорнуті, праворуч «+N кат.» —
 * тап розгортає підкатегорії в тій самій панелі.
 */
@Composable
fun CategorySheet(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onPick: (Long?) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.tx_category),
    noneLabel: String = stringResource(R.string.home_uncategorised),
) {
    val roots = categories.filter { it.parentId == null }
    val children = categories.filter { it.parentId != null }.groupBy { it.parentId }

    OxSheet(title = title, onDismiss = onDismiss) { close ->
        fun pick(id: Long?) {
            onPick(id)
            close()
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Space.s2),
            modifier = Modifier.heightIn(max = 560.dp),
        ) {
            item(key = "none") {
                CategoryPill(
                    name = noneLabel,
                    category = null,
                    selected = selectedId == null,
                    onClick = { pick(null) },
                )
            }
            items(roots, key = { it.id }) { root ->
                val subs = children[root.id].orEmpty()
                CategoryGroup(
                    root = root,
                    children = subs,
                    selectedId = selectedId,
                    onClick = { pick(it.id) },
                    // вибрана підкатегорія мусить бути видна — її група відкривається розгорнутою
                    initiallyExpanded = subs.any { it.id == selectedId },
                )
            }
        }
    }
}

@Composable
private fun CategoryPill(name: String, category: CategoryEntity?, selected: Boolean, onClick: () -> Unit) {
    val c = Oxygen.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(CircleShape)
            .clickableRow(selected, onClick)
            .padding(start = 8.dp, end = Space.s4, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (category != null) IconDisc(category.icon, category.color, category.name)
        else Box(Modifier.size(40.dp))
        Text(
            text = name,
            style = OxygenType.title.copy(fontWeight = FontWeight.SemiBold),
            color = c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = c.accentInk)
    }
}

@Composable
private fun Modifier.clickableRow(selected: Boolean, onClick: () -> Unit): Modifier {
    val c = Oxygen.colors
    return this
        .background(if (selected) c.accentSoft else c.surfaceVariant.copy(alpha = 0.5f))
        .clickable(onClick = onClick)
}

// ---------- Дата ----------

/**
 * Календар у шторці. Вибір дня одразу застосовується й закриває шторку — окрема кнопка тут зайва.
 * Пікер працює в UTC-мілісекундах, тому конвертація йде через UTC, інакше дата з'їжджає на добу.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSheet(date: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val c = Oxygen.colors
    val initial = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initial)

    OxSheet(title = stringResource(R.string.tx_date), onDismiss = onDismiss) { close ->
        LaunchedEffect(state.selectedDateMillis) {
            val picked = state.selectedDateMillis ?: return@LaunchedEffect
            if (picked != initial) {
                onPick(Instant.ofEpochMilli(picked).atZone(ZoneOffset.UTC).toLocalDate())
                close()
            }
        }
        DatePicker(
            state = state,
            title = null,
            headline = null,
            showModeToggle = false,
            colors = DatePickerDefaults.colors(
                containerColor = Color.Transparent,
                selectedDayContainerColor = c.accent,
                selectedDayContentColor = c.onAccent,
                todayContentColor = c.accentInk,
                todayDateBorderColor = c.accentInk,
                selectedYearContainerColor = c.accent,
                selectedYearContentColor = c.onAccent,
            ),
        )
    }
}

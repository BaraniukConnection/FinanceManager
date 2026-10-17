package com.oxygen.financemanager.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oxygen.financemanager.R
import com.oxygen.financemanager.ui.theme.Elevation
import com.oxygen.financemanager.ui.theme.Oxygen
import com.oxygen.financemanager.ui.theme.OxygenType
import com.oxygen.financemanager.ui.theme.Space
import com.oxygen.financemanager.ui.theme.Size as Sizes
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/*
 * Компоненти Oxygen Theme (тека OxygenTheme/components). Екрани збираються лише з них.
 */

/** 44dp диск для навігації й дій екрана. [neutral] — тільки для закриття/виходу, щоб «піти» відрізнялось від «перейти». */
@Composable
fun OxIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    neutral: Boolean = false,
) {
    val c = Oxygen.colors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = c.surfaceVariant,
        contentColor = if (neutral) c.ink else c.accentInk,
        modifier = modifier.size(Sizes.target),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * Верх кожного екрана. Без власного фону — сторінка «дихає» до самого краю.
 * Назад — завжди зліва; налаштування — завжди останні справа.
 */
@Composable
fun OxTopBar(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = 64.dp)
            .padding(horizontal = Space.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        if (onBack != null) {
            OxIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), onBack)
        }
        Text(
            text = title,
            style = OxygenType.screenTitle,
            color = Oxygen.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = Space.s2),
        )
        actions()
    }
}

/** Велика кнопка створення під великим пальцем. Одна заливка `accent` на екран. */
@Composable
fun OxFab(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Oxygen.colors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = c.accent,
        contentColor = c.onAccent,
        shadowElevation = Elevation.fab,
        modifier = modifier.size(Sizes.fab),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, contentDescription = label, modifier = Modifier.size(35.dp))
        }
    }
}

/**
 * [Danger] — кнопка, що веде до видалення (кільце danger-ink);
 * [DangerFill] — лише фінальне «Видалити» в підтвердженні.
 */
enum class ButtonVariant { Primary, Secondary, Quiet, Danger, DangerFill }

/** Не більше однієї [ButtonVariant.Primary] на екран. У парі: відмова зліва, підтвердження справа. */
@Composable
fun OxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val c = Oxygen.colors
    val container = when (variant) {
        ButtonVariant.Primary -> c.accent
        ButtonVariant.DangerFill -> c.danger
        else -> Color.Transparent
    }
    val content = when (variant) {
        ButtonVariant.Primary -> c.onAccent
        ButtonVariant.Secondary -> c.accentInk
        ButtonVariant.Quiet -> c.inkMuted
        ButtonVariant.Danger -> c.dangerInk
        ButtonVariant.DangerFill -> c.onDanger
    }
    val border = when (variant) {
        ButtonVariant.Secondary -> BorderStroke(2.dp, c.accentInk)
        ButtonVariant.Danger -> BorderStroke(1.5.dp, c.dangerInk)
        else -> null
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = container,
        contentColor = content,
        border = border,
        modifier = modifier.heightIn(min = Sizes.row).alpha(if (enabled) 1f else 0.38f),
    ) {
        Box(Modifier.padding(horizontal = Space.s6), contentAlignment = Alignment.Center) {
            Text(text, style = OxygenType.button, maxLines = 1)
        }
    }
}

/**
 * Пігулка вибору однієї області з короткого набору. Чіпи обирають, а не запускають дії.
 * З [leading] чіп стає більшим — для рахунків і категорій, де іконка допомагає впізнати.
 */
@Composable
fun OxChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = Oxygen.colors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) c.accent else c.surfaceVariant,
        contentColor = if (selected) c.onAccent else c.inkMuted,
    ) {
        Row(
            modifier = if (leading != null) {
                Modifier.padding(start = 6.dp, end = Space.s4, top = 6.dp, bottom = 6.dp)
            } else {
                Modifier.padding(horizontal = 14.dp, vertical = Space.s2)
            },
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Text(
                text = label,
                style = if (selected) OxygenType.label.copy(fontWeight = FontWeight.Bold) else OxygenType.label,
                maxLines = 1,
            )
        }
    }
}

/**
 * Один рядок чіпів з горизонтальним скролом.
 * [required] — рівно один вибраний (тип транзакції); інакше повторний тап знімає вибір (категорія).
 */
@Composable
fun <T> ChipRow(
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    /** Відступ всередині прокрутки: ряд тягнеться до краю екрана, а не обрізається полем. */
    edgePadding: Dp = 0.dp,
    /** Іконка перед підписом; `selected` — щоб на м'ятному фоні вибраного чіпа вона не зникла. */
    leading: (@Composable (option: T, selected: Boolean) -> Unit)? = null,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()).padding(horizontal = edgePadding),
        horizontalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            OxChip(
                label = optionLabel(option),
                selected = isSelected,
                onClick = { onSelect(if (isSelected && !required) null else option) },
                leading = leading?.let { draw -> { draw(option, isSelected) } },
            )
        }
    }
}

/** Розділювач груп: PINNED / OTHERS. Показувати лише коли груп щонайменше дві. */
@Composable
fun SectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = Space.s6, bottom = Space.s1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = OxygenType.overline,
            color = Oxygen.colors.inkMuted,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke(this)
    }
}

/**
 * Провідний диск рядка: колір — `mark-*` користувача.
 * У світлій темі знак малюється `ink`: м'ята чи бурштин на білому нечитабельні.
 */
@Composable
fun IconDisc(icon: String?, mark: Int?, fallbackLabel: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val c = Oxygen.colors
    val tint = mark?.let(::Color)
    val background = tint?.copy(alpha = if (c.isDark) 0.22f else 0.30f) ?: c.accentSoft
    val foreground = when {
        tint == null -> c.accentInk
        c.isDark -> tint
        else -> c.ink
    }
    Box(
        modifier.size(size).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        val vector = icon?.let(::iconFor)
        if (vector != null) {
            Icon(vector, contentDescription = null, tint = foreground, modifier = Modifier.size(22.dp))
        } else {
            Text(fallbackLabel.take(1).uppercase(), style = OxygenType.title, color = foreground)
        }
    }
}

/** Рядок списку: заголовок і підпис обрізаються трьома крапками, ніколи не переносяться. Праворуч — рівно одна річ. */
@Composable
fun OxListItem(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = Oxygen.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.row)
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = Space.s2),
        horizontalArrangement = Arrangement.spacedBy(Space.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = OxygenType.title.copy(fontWeight = FontWeight.SemiBold),
                color = c.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (supporting != null) Text(
                text = supporting,
                style = OxygenType.body,
                color = c.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
    }
}

/** Прев'ю однієї речі користувача. Кнопок усередині картки немає: тап відкриває. */
@Composable
fun OxCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    /** Рамка — лише коли щось означає: вибрано, помилка. */
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val color = Oxygen.colors.surface
    val body: @Composable () -> Unit = { Column(Modifier.padding(Space.s4), content = content) }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = border, shadowElevation = Elevation.card, content = body)
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = border, shadowElevation = Elevation.card, content = body)
    }
}

// ---------- Поля ----------

@Composable
private fun fieldColors() = Oxygen.colors.let { c ->
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.ink,
        unfocusedTextColor = c.ink,
        disabledTextColor = c.ink,
        errorTextColor = c.ink,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
        cursorColor = c.accentInk,
        errorCursorColor = c.dangerInk,
        focusedBorderColor = c.accentInk,
        unfocusedBorderColor = c.inkMuted,
        disabledBorderColor = c.inkMuted,
        errorBorderColor = c.dangerInk,
        focusedTrailingIconColor = c.accentInk,
        unfocusedTrailingIconColor = c.inkMuted,
        disabledTrailingIconColor = c.inkMuted,
        focusedSuffixColor = c.inkMuted,
        unfocusedSuffixColor = c.inkMuted,
        errorSuffixColor = c.inkMuted,
    )
}

/** Підпис над полем, підказка або помилка під ним — як у TextField з документа. */
@Composable
private fun FieldFrame(
    label: String,
    helper: String?,
    error: String?,
    modifier: Modifier,
    field: @Composable () -> Unit,
) {
    val c = Oxygen.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = OxygenType.label, color = c.ink)
        field()
        val note = error ?: helper
        if (note != null) Text(note, style = OxygenType.caption, color = if (error != null) c.dangerInk else c.inkMuted)
    }
}

@Composable
fun OxField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    helper: String? = null,
    error: String? = null,
    isError: Boolean = error != null,
    keyboardType: KeyboardType = KeyboardType.Text,
    suffix: String? = null,
    singleLine: Boolean = true,
) = FieldFrame(label, helper, error, modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        isError = isError,
        textStyle = OxygenType.bodyLg,
        suffix = suffix?.let { { Text(it, style = OxygenType.body) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Випадайка над довільним списком. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Dropdown(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) = FieldFrame(label, helper = null, error = null, modifier = modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = if (selected != null) optionLabel(selected) else "",
            onValueChange = {},
            readOnly = true,
            isError = isError,
            textStyle = OxygenType.bodyLg,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option), style = OxygenType.body) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Дати — як люди їх пишуть: 12.07.2026. */
private val fieldDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/**
 * Поле дати поверх M3 DatePicker. Пікер працює в UTC-мілісекундах, тому конвертація
 * йде через [ZoneOffset.UTC] — інакше вибрана дата з'їжджає на добу у від'ємних поясах.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    date: LocalDate?,
    onPick: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    clearable: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    val c = Oxygen.colors

    FieldFrame(label, helper = null, error = null, modifier = modifier) {
        OutlinedTextField(
            value = date?.let(fieldDate::format).orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            textStyle = OxygenType.bodyLg,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth().clickable { open = true },
        )
    }

    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        // м'ята як текст на білому нечитабельна — кнопки діалогу малюються accent-ink
        val textColors = ButtonDefaults.textButtonColors(contentColor = c.accentInk)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(colors = textColors, onClick = {
                    onPick(state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() })
                    open = false
                }) { Text(stringResource(R.string.action_select), style = OxygenType.button) }
            },
            dismissButton = {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = c.inkMuted),
                    onClick = {
                        if (clearable) onPick(null)
                        open = false
                    },
                ) {
                    Text(
                        stringResource(if (clearable) R.string.action_clear else R.string.action_cancel),
                        style = OxygenType.button,
                    )
                }
            },
        ) { DatePicker(state) }
    }
}

/** Вибір кольору з восьми `mark-*`. Кільце — не декор, а позначка вибору. */
@Composable
fun ColorRow(colors: List<Int>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val ring = Oxygen.colors.ink
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        colors.forEach { value ->
            Box(
                Modifier
                    .size(Sizes.target)
                    .clip(CircleShape)
                    .background(Color(value))
                    .then(if (value == selected) Modifier.border(3.dp, ring, CircleShape) else Modifier)
                    .clickable { onSelect(value) }
            )
        }
    }
}

// ---------- Нижня навігація ----------

/** Велика кнопка створення у формі таблетки: знак і дієслово, щоб дія читалась без здогадок. */
@Composable
fun OxPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Oxygen.colors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = c.accent,
        contentColor = c.onAccent,
        shadowElevation = Elevation.fab,
        modifier = modifier.height(Sizes.row),
    ) {
        Row(
            modifier = Modifier.padding(start = Space.s5, end = Space.s6),
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(26.dp))
            Text(label, style = OxygenType.button, maxLines = 1)
        }
    }
}

class NavTab(val icon: ImageVector, val selectedIcon: ImageVector, val label: String)

/**
 * Плаваюча нижня навігація без підкладки: дві круглі кнопки розділів обабіч таблетки створення.
 * Обраний розділ залитий `accent-soft` з іконкою `accent-ink` — видно, де ти, навіть краєм ока.
 * Свідомий відступ від IslandBar з Oxygen Theme — за рішенням користувача.
 */
@Composable
fun BottomNav(
    tabs: List<NavTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    createLabel: String,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    require(tabs.size == 2) { "BottomNav holds exactly two destinations" }
    val c = Oxygen.colors

    @Composable
    fun Tab(index: Int) {
        val tab = tabs[index]
        val isSelected = index == selected
        val container by animateColorAsState(if (isSelected) c.accentSoft else c.surface, tween(160), label = "tab")
        val content by animateColorAsState(if (isSelected) c.accentInk else c.inkMuted, tween(160), label = "tabIcon")
        Surface(
            onClick = { onSelect(index) },
            shape = CircleShape,
            color = container,
            contentColor = content,
            shadowElevation = Elevation.floating,
            modifier = Modifier.size(Sizes.row),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                    contentDescription = tab.label,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab(0)
        OxPill(createLabel, onCreate)
        Tab(1)
    }
}

// ---------- Інше ----------

/** Налаштування, що діє одразу: підпис зліва, перемикач справа, тапається весь рядок. */
@Composable
fun OxSwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, supporting: String? = null) {
    OxListItem(
        title = label,
        supporting = supporting,
        trailing = { Switch(checked = checked, onCheckedChange = onChange) },
        onClick = { onChange(!checked) },
    )
}

/** Пунктирна рамка — у Compose немає з коробки, тому малюємо PathEffect'ом. */
fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 2.dp) = drawBehind {
    val stroke = Stroke(
        width = width.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx())),
    )
    val inset = stroke.width / 2
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - stroke.width, size.height - stroke.width),
        cornerRadius = CornerRadius(radius.toPx()),
        style = stroke,
    )
}

/**
 * Коротке блокуюче питання. Заголовок каже, що станеться; для видалення він danger-ink.
 * Дії внизу праворуч: відмова, потім підтвердження. Тап поза діалогом — відмова.
 */
@Composable
fun OxConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    val c = Oxygen.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(title, style = OxygenType.screenTitle, color = if (destructive) c.dangerInk else c.accentInk)
        },
        text = { Text(text, style = OxygenType.body, color = c.ink) },
        dismissButton = {
            OxButton(stringResource(R.string.action_cancel), onDismiss, variant = ButtonVariant.Quiet)
        },
        confirmButton = {
            OxButton(
                text = confirmLabel,
                onClick = onConfirm,
                variant = if (destructive) ButtonVariant.DangerFill else ButtonVariant.Primary,
            )
        },
    )
}

// ---------- Панелі й шторки ----------

/** Панель без внутрішніх полів: рядки всередині самі тримають відступи й діляться лініями. */
@Composable
fun OxPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Oxygen.colors.surface,
        shadowElevation = Elevation.card,
    ) { Column(content = content) }
}

@Composable
fun PanelDivider() = HorizontalDivider(color = Oxygen.colors.line, modifier = Modifier.padding(horizontal = Space.s4))

/**
 * Рядок панелі, що відкриває вибір: підпис зверху, значення під ним, шеврон праворуч.
 * Без значення показується підказка; якщо значення обов'язкове й не вибране — danger-ink.
 */
@Composable
fun PanelRow(
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    isError: Boolean = false,
) {
    val c = Oxygen.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 68.dp)
            .padding(horizontal = Space.s4, vertical = Space.s2),
        horizontalArrangement = Arrangement.spacedBy(Space.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(label, style = OxygenType.caption, color = c.inkMuted)
            Text(
                text = value ?: placeholder,
                style = OxygenType.title.copy(fontWeight = FontWeight.SemiBold),
                color = when {
                    value != null -> c.ink
                    isError -> c.dangerInk
                    else -> c.inkMuted
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = c.inkMuted)
    }
}

/** Диск з іконкою інтерфейсу (не вмісту користувача) — для порожніх рядків панелі. */
@Composable
fun UiDisc(icon: ImageVector, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Oxygen.colors.accentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Oxygen.colors.accentInk, modifier = Modifier.size(size * 0.5f))
    }
}

/**
 * Шторка знизу. [content] отримує `close` — він спершу доанімовує закриття, а потім
 * прибирає шторку, тож вона з'їжджає, а не зникає різко.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OxSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val c = Oxygen.colors
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = c.surface,
        contentColor = c.ink,
        scrimColor = c.scrim,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = c.line) },
    ) {
        Column(
            Modifier.padding(start = Space.s4, end = Space.s4, bottom = Space.s4),
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            Text(title, style = OxygenType.screenTitle, color = c.ink)
            content(close)
        }
    }
}

/**
 * Одна довга таблетка, поділена на рівні частини. Вибрана частина — м'ятна «капсула» всередині;
 * поділки видно лише між невибраними, щоб не різали вибір.
 */
@Composable
fun <T> OxSegmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    icon: (T) -> ImageVector,
    modifier: Modifier = Modifier,
) {
    val c = Oxygen.colors
    Surface(shape = CircleShape, color = c.surfaceVariant, modifier = modifier.fillMaxWidth().height(52.dp)) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            options.forEachIndexed { index, option ->
                if (index > 0) {
                    val hidden = option == selected || options[index - 1] == selected
                    VerticalDivider(
                        color = if (hidden) Color.Transparent else c.inkMuted.copy(alpha = 0.3f),
                        modifier = Modifier.height(24.dp),
                    )
                }
                val isSelected = option == selected
                val container by animateColorAsState(if (isSelected) c.accent else Color.Transparent, tween(160), label = "seg")
                val content by animateColorAsState(if (isSelected) c.onAccent else c.inkMuted, tween(160), label = "segText")
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(container)
                        .clickable { onSelect(option) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon(option), contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
                    Text(
                        text = label(option),
                        style = if (isSelected) OxygenType.label.copy(fontWeight = FontWeight.Bold) else OxygenType.label,
                        color = content,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

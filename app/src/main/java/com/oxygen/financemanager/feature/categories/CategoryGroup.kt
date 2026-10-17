package com.oxygen.financemanager.feature.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oxygen.financemanager.R
import com.oxygen.financemanager.data.db.CategoryEntity
import com.oxygen.financemanager.ui.IconDisc
import com.oxygen.financemanager.ui.theme.Elevation
import com.oxygen.financemanager.ui.theme.Oxygen
import com.oxygen.financemanager.ui.theme.OxygenType
import com.oxygen.financemanager.ui.theme.Space

private const val MOTION_MS = 160

/**
 * Категорія з підкатегоріями в одній панелі. Згорнута — як звичайна таблетка, праворуч
 * коротка «+N кат.»; тап по ній розгортає підкатегорії в тій самій панелі, ще тап — згортає.
 * Тап по решті рядка — [onClick] (вибір у шторці, редагування на екрані категорій).
 *
 * [elevated] — панель з тінню на тлі сторінки; без нього — тонована таблетка для шторки.
 */
@Composable
fun CategoryGroup(
    root: CategoryEntity,
    children: List<CategoryEntity>,
    onClick: (CategoryEntity) -> Unit,
    modifier: Modifier = Modifier,
    selectedId: Long? = null,
    elevated: Boolean = false,
    initiallyExpanded: Boolean = false,
) {
    val c = Oxygen.colors
    var expanded by rememberSaveable(root.id) { mutableStateOf(initiallyExpanded) }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = if (elevated) c.surface else c.surfaceVariant.copy(alpha = 0.5f),
        shadowElevation = if (elevated) Elevation.card else 0.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            CategoryRow(
                category = root,
                selected = root.id == selectedId,
                disc = 40.dp,
                onClick = { onClick(root) },
            ) {
                if (children.isNotEmpty()) CountPill(
                    count = children.size,
                    expanded = expanded,
                    // на білій панелі — тонована таблетка, на тонованій — біла, щоб не зливалась
                    idle = if (elevated) c.surfaceVariant else c.surface,
                    onToggle = { expanded = !expanded },
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(MOTION_MS)) + fadeIn(tween(MOTION_MS)),
                exit = shrinkVertically(tween(MOTION_MS)) + fadeOut(tween(MOTION_MS)),
            ) {
                val line = c.line
                // підкатегорії — з відступом і лінією-напрямною, як гілка дерева в Oxygen
                Column(
                    modifier = Modifier
                        .padding(start = 28.dp, end = Space.s2, bottom = Space.s2)
                        .drawBehind { drawLine(line, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1.dp.toPx()) }
                        .padding(start = Space.s3),
                    verticalArrangement = Arrangement.spacedBy(Space.s1),
                ) {
                    children.forEach { child ->
                        CategoryRow(
                            category = child,
                            selected = child.id == selectedId,
                            disc = 32.dp,
                            onClick = { onClick(child) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: CategoryEntity,
    selected: Boolean,
    disc: Dp,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    val c = Oxygen.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (disc > 36.dp) 56.dp else 48.dp)
            .clip(CircleShape)
            .background(if (selected) c.accentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = Space.s3, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconDisc(category.icon, category.color, category.name, size = disc)
        Text(
            text = category.name,
            style = OxygenType.title.copy(fontWeight = FontWeight.SemiBold),
            color = c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing()
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = c.accentInk)
    }
}

/** Коротка таблетка «+N кат.»: згорнуто — нейтральна, розгорнуто — м'ятна, щоб стан було видно. */
@Composable
private fun CountPill(count: Int, expanded: Boolean, idle: Color, onToggle: () -> Unit) {
    val c = Oxygen.colors
    Surface(
        onClick = onToggle,
        shape = CircleShape,
        color = if (expanded) c.accentSoft else idle,
        contentColor = if (expanded) c.accentInk else c.inkMuted,
    ) {
        Text(
            text = stringResource(R.string.category_sub_pill, count),
            style = OxygenType.label,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = Space.s3, vertical = Space.s2),
        )
    }
}

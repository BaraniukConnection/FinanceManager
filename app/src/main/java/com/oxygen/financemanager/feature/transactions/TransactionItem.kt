package com.oxygen.financemanager.feature.transactions

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.oxygen.financemanager.R
import com.oxygen.financemanager.core.formatMinor
import com.oxygen.financemanager.data.db.TransactionType
import com.oxygen.financemanager.feature.home.TransactionRow
import com.oxygen.financemanager.ui.IconDisc
import com.oxygen.financemanager.ui.OxListItem
import com.oxygen.financemanager.ui.theme.Oxygen
import com.oxygen.financemanager.ui.theme.OxygenType
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Дати — як люди їх пишуть: 12.07. */
val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.")

/**
 * Рядок транзакції для Home і для списку транзакцій.
 * [showDate] вимикається там, де дату вже каже заголовок дня.
 */
@Composable
fun TransactionItem(
    row: TransactionRow,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val tx = row.transaction
    val c = Oxygen.colors
    val isTransfer = tx.type == TransactionType.TRANSFER
    val category = row.categoryName ?: stringResource(R.string.home_uncategorised)

    val title = tx.note ?: when {
        isTransfer -> stringResource(R.string.home_transfer_to, row.counterpartyName.orEmpty())
        else -> category
    }
    val date = tx.occurredAt.atZone(ZoneId.systemDefault()).format(shortDate).takeIf { showDate }
    val supporting = listOfNotNull(
        when {
            isTransfer -> "${row.accountName} → ${row.counterpartyName.orEmpty()}"
            tx.note != null -> "$category · ${row.accountName}"
            else -> row.accountName
        },
        date,
    ).joinToString(" · ")

    // червоний в Oxygen — лише для знищення, тож витрата не червона: знак і вага кажуть досить
    val (amount, color) = when {
        isTransfer -> formatMinor(kotlin.math.abs(row.signedMinor), tx.currency) to c.inkMuted
        row.signedMinor < 0 -> "−" + formatMinor(-row.signedMinor, tx.currency) to c.ink
        else -> "+" + formatMinor(row.signedMinor, tx.currency) to c.accentInk
    }

    OxListItem(
        title = title,
        supporting = supporting,
        leading = {
            IconDisc(
                icon = if (isTransfer) "ms:swap_horiz" else row.categoryIcon,
                mark = if (isTransfer) null else row.categoryColor,
                fallbackLabel = title,
            )
        },
        trailing = { Text(amount, style = OxygenType.title, color = color, maxLines = 1) },
        onClick = onClick,
        modifier = modifier,
    )
}

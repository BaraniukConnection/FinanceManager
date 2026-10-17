package com.oxygen.financemanager.feature.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oxygen.financemanager.R
import com.oxygen.financemanager.core.formatMinor
import com.oxygen.financemanager.feature.home.AccountSummary
import com.oxygen.financemanager.ui.IconDisc
import com.oxygen.financemanager.ui.OxCard
import com.oxygen.financemanager.ui.theme.Oxygen
import com.oxygen.financemanager.ui.theme.OxygenType
import com.oxygen.financemanager.ui.theme.Space

/** Однакова висота всіх карток — щоб ряди в каруселі й сітці не стрибали. */
val AccountCardHeight = 164.dp

/** Картка рахунку — однакова на Home і на екрані рахунків. У скарбнички з ціллю — ще й прогрес. */
@Composable
fun AccountCard(
    summary: AccountSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val account = summary.account
    val target = account.targetMinor

    OxCard(
        modifier = modifier.height(AccountCardHeight),
        onClick = onClick,
        border = if (selected) BorderStroke(2.dp, Oxygen.colors.accentInk) else null,
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            IconDisc(account.icon, account.color, account.name, size = 36.dp)
            Column {
                Text(
                    text = account.name,
                    style = OxygenType.title,
                    color = Oxygen.colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatMinor(summary.balanceMinor, account.currency),
                    style = OxygenType.screenTitle,
                    color = Oxygen.colors.ink,
                    maxLines = 1,
                )
                Text(
                    text = if (target != null) {
                        val percent = (summary.balanceMinor * 100 / target).coerceIn(0, 100).toInt()
                        stringResource(R.string.home_goal_progress, percent, formatMinor(target, account.currency))
                    } else {
                        stringResource(accountTypeLabel(account.type))
                    },
                    style = OxygenType.caption,
                    color = Oxygen.colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (target != null) LinearProgressIndicator(
                    progress = { (summary.balanceMinor.toFloat() / target).coerceIn(0f, 1f) },
                    color = Oxygen.colors.accentInk,
                    trackColor = Oxygen.colors.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = Space.s2),
                )
            }
        }
    }
}

package com.oxygen.financemanager.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oxygen.financemanager.R
import com.oxygen.financemanager.data.db.AccountType
import com.oxygen.financemanager.feature.home.AccountSummary
import com.oxygen.financemanager.ui.OxPill
import com.oxygen.financemanager.ui.SectionHeader
import com.oxygen.financemanager.ui.theme.Oxygen
import com.oxygen.financemanager.ui.theme.OxygenType
import com.oxygen.financemanager.ui.theme.Space

/**
 * Рахунки й скарбнички — окремими групами, кожен на картці як на Home.
 * Сітка у дві колонки з проміжками space-3, як радить Oxygen для карток на телефоні.
 */
@Composable
fun AccountsScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val accounts by viewModel.state.collectAsStateWithLifecycle()
    val (savings, spending) = accounts.partition { it.account.type == AccountType.SAVINGS }
    // заголовки груп — лише коли груп дві
    val headed = savings.isNotEmpty() && spending.isNotEmpty()

    Box(modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = Space.s4, end = Space.s4, top = Space.s2, bottom = Space.bottomInset),
            horizontalArrangement = Arrangement.spacedBy(Space.s3),
            verticalArrangement = Arrangement.spacedBy(Space.s3),
        ) {
            if (accounts.isEmpty()) fullLine("empty") {
                Text(
                    text = stringResource(R.string.accounts_empty),
                    style = OxygenType.body,
                    color = Oxygen.colors.inkMuted,
                )
            }
            group("spending", if (headed) R.string.home_accounts else null, spending, onEdit)
            group("savings", if (headed) R.string.home_savings else null, savings, onEdit)
        }

        OxPill(
            label = stringResource(R.string.account_add),
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Space.s6),
        )
    }
}

private fun LazyGridScope.group(key: String, header: Int?, items: List<AccountSummary>, onEdit: (Long) -> Unit) {
    if (items.isEmpty()) return
    if (header != null) fullLine("header-$key") { SectionHeader(stringResource(header)) }
    items(items, key = { it.account.id }) { summary ->
        AccountCard(summary, onClick = { onEdit(summary.account.id) }, modifier = Modifier.fillMaxWidth())
    }
}

private fun LazyGridScope.fullLine(key: String, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }

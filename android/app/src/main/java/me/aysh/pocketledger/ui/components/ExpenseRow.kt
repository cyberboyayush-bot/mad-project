package me.aysh.pocketledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.aysh.pocketledger.data.model.Category
import me.aysh.pocketledger.data.model.Expense
import me.aysh.pocketledger.ui.theme.MoneyStyles
import me.aysh.pocketledger.ui.theme.PocketLedgerTheme
import me.aysh.pocketledger.ui.theme.Spacing
import me.aysh.pocketledger.util.Money
import java.time.Instant
import java.time.LocalDate

/** Title shown for an expense: its note, or the category name when there's no note. */
val Expense.title: String get() = note.ifBlank { category.label }

/** One ledger line: badge, title + subtitle, amount. */
@Composable
fun ExpenseRow(
    expense: Expense,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = Spacing.gutter, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(expense.category)
        Spacer(Modifier.width(Spacing.md + Spacing.xs))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(
                text = expense.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = Money.format(expense.amountPaise),
            style = MoneyStyles.row,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun ExpenseRowPreview() {
    PocketLedgerTheme {
        ExpenseRow(
            expense = Expense("1", 1_24_050, Category.Food, "Dinner with the team at a very long named restaurant", LocalDate.now(), Instant.now()),
            subtitle = "Food · Today",
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun ExpenseRowDarkPreview() {
    PocketLedgerTheme(darkTheme = true) {
        ExpenseRow(
            expense = Expense("2", 10_00_000_00, Category.Transport, "", LocalDate.now(), Instant.now()),
            subtitle = "Transport · Yesterday",
            onClick = {},
        )
    }
}

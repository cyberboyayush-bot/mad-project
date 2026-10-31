package me.aysh.pocketledger.ui.history

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import me.aysh.pocketledger.R
import me.aysh.pocketledger.data.model.Category
import me.aysh.pocketledger.data.model.Expense
import me.aysh.pocketledger.ui.components.ExpenseRow
import me.aysh.pocketledger.ui.components.color
import me.aysh.pocketledger.ui.theme.MoneyStyles
import me.aysh.pocketledger.ui.theme.Spacing
import me.aysh.pocketledger.util.Dates
import me.aysh.pocketledger.util.Money

@Composable
internal fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focus = LocalFocusManager.current
    val scheme = MaterialTheme.colorScheme
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.history_search)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_clear))
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceContainerHigh,
            unfocusedContainerColor = scheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedLeadingIconColor = scheme.onBackground,
            unfocusedLeadingIconColor = scheme.onSurfaceVariant,
            unfocusedPlaceholderColor = scheme.onSurfaceVariant,
            focusedPlaceholderColor = scheme.onSurfaceVariant,
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter)
            .testTag("history_search"),
    )
}

/** "All" plus one chip per category; the selected chip is inked in. */
@Composable
internal fun CategoryChips(selected: Category?, onToggle: (Category) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item(key = "all") {
            LedgerChip(
                label = stringResource(R.string.history_all),
                selected = selected == null,
                dot = null,
                onClick = { selected?.let(onToggle) },
            )
        }
        items(Category.entries, key = { it.key }) { category ->
            LedgerChip(
                label = category.label,
                selected = selected == category,
                dot = category.color,
                onClick = { onToggle(category) },
                modifier = Modifier.testTag("chip_${category.key}"),
            )
        }
    }
}

@Composable
private fun LedgerChip(label: String, selected: Boolean, dot: Color?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) scheme.onBackground else Color.Transparent, label = "chipBg")
    val content by animateColorAsState(if (selected) scheme.background else scheme.onBackground, label = "chipFg")
    val border = BorderStroke(1.dp, if (selected) scheme.onBackground else scheme.outline)
    Row(
        modifier = modifier
            .height(40.dp)
            .background(container, CircleShape)
            .border(border, CircleShape)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Spacer(Modifier.size(8.dp).background(dot, CircleShape))
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(label, style = MaterialTheme.typography.titleSmall, color = content)
    }
}

@Composable
internal fun DayHeader(date: LocalDate, totalPaise: Long, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Spacing.gutter)
            .padding(top = Spacing.lg, bottom = Spacing.sm)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            Dates.relativeDay(date).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(Money.format(totalPaise), style = MoneyStyles.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Expense row that can be swiped right-to-left to delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeableExpenseRow(
    expense: Expense,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberSwipeToDismissBoxState()
    val deleteLabel = stringResource(R.string.history_delete_action)
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onDelete() },
        modifier = modifier.semantics {
            customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
        },
        backgroundContent = {
            val armed = state.targetValue == SwipeToDismissBoxValue.EndToStart
            val bg by animateColorAsState(
                if (armed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer,
                label = "swipeBg",
            )
            val fg = if (armed) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.error
            val iconScale by animateFloatAsState(if (armed) 1.15f else 1f, label = "swipeIcon")
            Box(
                Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(horizontal = Spacing.xl),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(deleteLabel, style = MaterialTheme.typography.labelLarge, color = fg)
                    Spacer(Modifier.width(Spacing.sm))
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = fg, modifier = Modifier.scale(iconScale))
                }
            }
        },
    ) {
        ExpenseRow(
            expense = expense,
            subtitle = expense.category.label,
            onClick = onClick,
            modifier = Modifier.testTag("expense_row_${expense.id}"),
        )
    }
}

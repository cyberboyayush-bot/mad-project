package me.aysh.pocketledger.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aysh.pocketledger.R
import me.aysh.pocketledger.data.model.Category
import me.aysh.pocketledger.data.model.Expense
import me.aysh.pocketledger.ui.components.EmptyState
import me.aysh.pocketledger.ui.components.ErrorState
import me.aysh.pocketledger.ui.components.Hairline
import me.aysh.pocketledger.ui.components.MonthSwitcher
import me.aysh.pocketledger.ui.components.ScreenTitle
import me.aysh.pocketledger.ui.components.SkeletonRows
import me.aysh.pocketledger.ui.theme.Spacing
import me.aysh.pocketledger.util.Dates
import me.aysh.pocketledger.util.Money

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, contentPadding: PaddingValues, onOpenExpense: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryContent(
        state = state,
        contentPadding = contentPadding,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onQueryChange = viewModel::onQueryChange,
        onCategoryToggle = viewModel::onCategoryToggle,
        onClearFilters = viewModel::clearFilters,
        onRetry = viewModel::retry,
        onOpenExpense = onOpenExpense,
        onDelete = viewModel::delete,
    )
}

@Composable
private fun HistoryContent(
    state: HistoryUiState,
    contentPadding: PaddingValues,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCategoryToggle: (Category) -> Unit,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    onOpenExpense: (String) -> Unit,
    onDelete: (Expense) -> Unit,
) {
    val listState = rememberLazyListState()
    val count = state.groups.sumOf { it.expenses.size }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + Spacing.xl),
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_screen"),
    ) {
        item(key = "header") {
            Column {
                ScreenTitle(
                    stringResource(R.string.history_title),
                    Modifier.padding(horizontal = Spacing.gutter).padding(top = Spacing.xl, bottom = Spacing.lg),
                )
                MonthSwitcher(
                    month = state.month,
                    canGoForward = state.canGoForward,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                    previousTag = "history_prev_month",
                    nextTag = "history_next_month",
                    subtitle = when {
                        state.isLoading && state.groups.isEmpty() -> " "
                        count == 0 && state.isFiltered -> stringResource(R.string.history_filtered_none)
                        count == 0 -> stringResource(R.string.history_month_none)
                        state.isFiltered -> pluralStringResource(R.plurals.history_filtered_total, count, Money.format(state.monthTotalPaise), count)
                        else -> pluralStringResource(R.plurals.history_month_total, count, Money.format(state.monthTotalPaise), count)
                    },
                )
                Spacer(Modifier.height(Spacing.lg))
                SearchField(state.query, onQueryChange)
                Spacer(Modifier.height(Spacing.md))
                CategoryChips(state.category, onCategoryToggle)
                Spacer(Modifier.height(Spacing.sm))
                AnimatedVisibility(state.isLoading && state.groups.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
        when {
            state.error != null && state.groups.isEmpty() -> item(key = "error") {
                ErrorState(state.error, onRetry, Modifier.padding(top = Spacing.xl))
            }
            state.isLoading && state.groups.isEmpty() -> item(key = "loading") {
                SkeletonRows(6, Modifier.padding(top = Spacing.xl))
            }
            state.isEmpty && state.isFiltered -> item(key = "noResults") {
                EmptyState(
                    title = stringResource(R.string.history_no_results_title),
                    body = stringResource(R.string.history_no_results_body),
                    actionLabel = stringResource(R.string.history_clear_filters),
                    onAction = onClearFilters,
                )
            }
            state.isEmpty -> item(key = "empty") {
                EmptyState(
                    title = stringResource(R.string.history_empty_title, Dates.month(state.month)),
                    body = stringResource(R.string.history_empty_body),
                )
            }
            else -> state.groups.forEach { group ->
                stickyHeader(key = "day_${group.date}", contentType = "day") {
                    DayHeader(group.date, group.totalPaise)
                }
                itemsIndexed(group.expenses, key = { _, e -> e.id }, contentType = { _, _ -> "row" }) { index, expense ->
                    Column(Modifier.animateItem()) {
                        SwipeableExpenseRow(
                            expense = expense,
                            onClick = { onOpenExpense(expense.id) },
                            onDelete = { onDelete(expense) },
                        )
                        if (index < group.expenses.lastIndex) Hairline(Modifier.padding(start = Spacing.gutter + 56.dp, end = Spacing.gutter))
                    }
                }
            }
        }
    }
}

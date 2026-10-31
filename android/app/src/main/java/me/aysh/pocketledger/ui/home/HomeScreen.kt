package me.aysh.pocketledger.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import me.aysh.pocketledger.R
import me.aysh.pocketledger.ui.components.EmptyState
import me.aysh.pocketledger.ui.components.ErrorBanner
import me.aysh.pocketledger.ui.components.ErrorState
import me.aysh.pocketledger.ui.components.ExpenseRow
import me.aysh.pocketledger.ui.components.Eyebrow
import me.aysh.pocketledger.ui.components.Hairline
import me.aysh.pocketledger.ui.components.LinkButton
import me.aysh.pocketledger.ui.components.SectionHeader
import me.aysh.pocketledger.ui.components.SkeletonBlock
import me.aysh.pocketledger.ui.components.SkeletonRows
import me.aysh.pocketledger.ui.theme.Spacing
import me.aysh.pocketledger.util.Dates

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    contentPadding: PaddingValues,
    onOpenExpense: (String) -> Unit,
    onAddExpense: () -> Unit,
    onSeeAll: () -> Unit,
    onSetBudget: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeBody(
        state = state,
        contentPadding = contentPadding,
        onRefresh = { viewModel.refresh() },
        onOpenExpense = onOpenExpense,
        onAddExpense = onAddExpense,
        onSeeAll = onSeeAll,
        onSetBudget = onSetBudget,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeBody(
    state: HomeUiState,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onOpenExpense: (String) -> Unit,
    onAddExpense: () -> Unit,
    onSeeAll: () -> Unit,
    onSetBudget: () -> Unit,
) {
    val content = state.content
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = state.isRefreshing,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    ) {
        when {
            content == null && state.isLoading -> HomeSkeleton()
            content == null -> ErrorState(
                message = state.error.orEmpty(),
                onRetry = onRefresh,
                modifier = Modifier.padding(top = 120.dp),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + Spacing.xl),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "greeting") { Greeting(content.profile.name) }
                if (state.error != null) {
                    item(key = "error") {
                        ErrorBanner(state.error, Modifier.padding(horizontal = Spacing.lg).padding(bottom = Spacing.md))
                    }
                }
                item(key = "hero") { HomeHero(content.summary, onSetBudget = onSetBudget) }
                if (content.summary.count == 0) {
                    item(key = "empty") {
                        EmptyState(
                            title = stringResource(R.string.home_empty_title),
                            body = stringResource(R.string.home_empty_body, Dates.month(content.summary.month)),
                            actionLabel = stringResource(R.string.home_empty_action),
                            onAction = onAddExpense,
                        )
                    }
                } else {
                    item(key = "breakdown") {
                        Column {
                            Spacer(Modifier.height(Spacing.xl))
                            SectionHeader(stringResource(R.string.home_breakdown))
                            CategoryBreakdown(content.summary.byCategory)
                            Spacer(Modifier.height(Spacing.lg))
                        }
                    }
                    item(key = "recentHeader") {
                        SectionHeader(stringResource(R.string.home_recent)) {
                            LinkButton(
                                text = stringResource(R.string.home_see_all),
                                onClick = onSeeAll,
                                modifier = Modifier.testTag("home_see_all"),
                            )
                        }
                    }
                    itemsIndexed(content.recent, key = { _, e -> e.id }) { index, expense ->
                        Column(Modifier.animateItem()) {
                            ExpenseRow(
                                expense = expense,
                                subtitle = "${expense.category.label} · ${Dates.short(expense.spentOn)}",
                                onClick = { onOpenExpense(expense.id) },
                            )
                            if (index < content.recent.lastIndex) Hairline(Modifier.padding(start = Spacing.gutter + 56.dp, end = Spacing.gutter))
                        }
                    }
                }
            }
        }
    }
}

private val todayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)

@Composable
private fun Greeting(name: String) {
    val hour = LocalTime.now().hour
    val greeting = stringResource(
        when {
            hour < 12 -> R.string.greeting_morning
            hour < 17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        },
    )
    val firstName = name.trim().substringBefore(' ').ifEmpty { name }
    Column(
        Modifier
            .padding(horizontal = Spacing.gutter)
            .padding(top = Spacing.xl, bottom = Spacing.xl),
    ) {
        Eyebrow(LocalDate.now().format(todayFormat))
        Spacer(Modifier.height(Spacing.sm))
        val accent = MaterialTheme.colorScheme.primary
        Text(
            text = buildAnnotatedString {
                append(greeting)
                append('\n')
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = accent)) { append("$firstName.") }
            },
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp, lineHeight = 40.sp),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun HomeSkeleton() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(Modifier.padding(horizontal = Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SkeletonBlock(Modifier.fillMaxWidth(0.4f), height = 10.dp)
            SkeletonBlock(Modifier.fillMaxWidth(0.6f), height = 32.dp)
            SkeletonBlock(Modifier.fillMaxWidth(0.4f), height = 32.dp)
        }
        Spacer(Modifier.height(Spacing.md))
        SkeletonBlock(
            Modifier
                .padding(horizontal = Spacing.lg)
                .fillMaxWidth(),
            height = 236.dp,
            corner = 28.dp,
        )
        Spacer(Modifier.height(Spacing.xl))
        Box(Modifier.padding(horizontal = Spacing.gutter)) { SkeletonBlock(Modifier.fillMaxWidth(0.3f)) }
        SkeletonRows(4, Modifier.padding(top = Spacing.md))
    }
}

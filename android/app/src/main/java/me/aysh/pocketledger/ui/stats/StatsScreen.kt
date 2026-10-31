package me.aysh.pocketledger.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import me.aysh.pocketledger.R
import me.aysh.pocketledger.data.model.CategoryTotal
import me.aysh.pocketledger.data.model.MonthSummary
import me.aysh.pocketledger.ui.components.DisplayAmount
import me.aysh.pocketledger.ui.components.EmptyState
import me.aysh.pocketledger.ui.components.ErrorState
import me.aysh.pocketledger.ui.components.Eyebrow
import me.aysh.pocketledger.ui.components.MonthSwitcher
import me.aysh.pocketledger.ui.components.ScreenTitle
import me.aysh.pocketledger.ui.components.SectionHeader
import me.aysh.pocketledger.ui.components.SkeletonBlock
import me.aysh.pocketledger.ui.components.animatedPaise
import me.aysh.pocketledger.ui.components.color
import me.aysh.pocketledger.ui.theme.LedgerTheme
import me.aysh.pocketledger.ui.theme.MoneyStyles
import me.aysh.pocketledger.ui.theme.Spacing
import me.aysh.pocketledger.util.Dates
import me.aysh.pocketledger.util.Money

@Composable
fun StatsScreen(viewModel: StatsViewModel, contentPadding: PaddingValues) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StatsContent(state, contentPadding, viewModel::previousMonth, viewModel::nextMonth, viewModel::retry)
}

@Composable
private fun StatsContent(
    state: StatsUiState,
    contentPadding: PaddingValues,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
) {
    // While a new month loads, the old charts stay on screen, dimmed.
    val stale = state.isLoading && state.summary != null
    val contentAlpha by animateFloatAsState(if (stale) 0.4f else 1f, label = "stale")
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = contentPadding.calculateBottomPadding() + Spacing.xl)
            .testTag("stats_screen"),
    ) {
        ScreenTitle(
            stringResource(R.string.stats_title),
            Modifier.padding(horizontal = Spacing.gutter).padding(top = Spacing.xl, bottom = Spacing.lg),
        )
        MonthSwitcher(
            month = state.month,
            canGoForward = state.canGoForward,
            onPrevious = onPrevious,
            onNext = onNext,
            previousTag = "stats_prev_month",
            nextTag = "stats_next_month",
        )
        val summary = state.summary
        when {
            summary == null && state.error != null -> ErrorState(state.error, onRetry, Modifier.padding(top = Spacing.xxl))
            summary == null -> StatsSkeleton()
            summary.count == 0 && !state.isLoading -> EmptyState(
                title = stringResource(R.string.stats_empty_title, Dates.month(state.month)),
                body = stringResource(R.string.stats_empty_body),
                modifier = Modifier.padding(top = Spacing.xl),
            )
            else -> Column(Modifier.alpha(contentAlpha)) { StatsBody(state, summary) }
        }
    }
}

@Composable
private fun ColumnScope.StatsBody(state: StatsUiState, summary: MonthSummary) {
    Spacer(Modifier.height(Spacing.xl))
    Column(Modifier.padding(horizontal = Spacing.gutter)) {
        Eyebrow(stringResource(R.string.stats_total))
        DisplayAmount(
            paise = animatedPaise(summary.totalPaise, durationMillis = 700),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { contentDescription = Money.format(summary.totalPaise) },
        )
        Spacer(Modifier.height(Spacing.lg))
        MetricsRow(state)
    }
    Spacer(Modifier.height(Spacing.xl))
    SectionHeader(stringResource(R.string.stats_by_category))
    val sorted = summary.byCategory.filter { it.totalPaise > 0 }.sortedByDescending { it.totalPaise }
    DonutChart(
        totals = sorted,
        animationKey = summary.month to summary.totalPaise,
        description = stringResource(R.string.stats_donut_description),
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(vertical = Spacing.md),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                Money.formatCompact(summary.totalPaise),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                pluralStringResource(R.plurals.stats_categories_count, sorted.size, sorted.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(Modifier.height(Spacing.md))
    val sum = summary.totalPaise.coerceAtLeast(1)
    sorted.forEach { LegendRow(it, sum) }
    Spacer(Modifier.height(Spacing.xl))
    SectionHeader(stringResource(R.string.stats_by_day))
    DailyBarChart(
        month = state.month,
        totals = state.dailyTotals,
        averagePaise = state.dailyAveragePaise,
        averageLabel = "${stringResource(R.string.stats_avg_line)} ${Money.formatCompact(state.dailyAveragePaise)}",
        description = stringResource(R.string.stats_bars_description),
        modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.sm),
    )
}

@Composable
private fun MetricsRow(state: StatsUiState) {
    val change = state.changeVsPrevious
    val previousName = Dates.month(state.month.minusMonths(1))
    val colors = LedgerTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Metric(stringResource(R.string.stats_daily_avg), Money.format(state.dailyAveragePaise / 100 * 100), MaterialTheme.colorScheme.onBackground, Modifier.weight(1f))
        Box(
            Modifier
                .padding(horizontal = Spacing.lg)
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Metric(
            label = stringResource(R.string.stats_vs_previous, previousName),
            value = when {
                change == null -> stringResource(R.string.stats_no_compare)
                change > 0 -> "+$change%"
                change < 0 -> "−${-change}%"
                else -> "0%"
            },
            color = when {
                change == null || change == 0 -> MaterialTheme.colorScheme.onBackground
                change > 0 -> colors.caution
                else -> colors.positive
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Metric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Eyebrow(label)
        Spacer(Modifier.height(Spacing.xs))
        Text(value, style = MoneyStyles.title, color = color, maxLines = 1)
    }
}

@Composable
private fun LegendRow(item: CategoryTotal, sum: Long) {
    val percent = (item.totalPaise * 100.0 / sum).roundToInt()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter, vertical = Spacing.sm + Spacing.xxs)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(item.category.color, CircleShape))
        Spacer(Modifier.width(Spacing.md))
        Text(
            item.category.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text("$percent%", style = MoneyStyles.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.lg))
        Text(
            Money.format(item.totalPaise),
            style = MoneyStyles.row,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.width(112.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun StatsSkeleton() {
    Column(Modifier.padding(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Spacer(Modifier.height(Spacing.sm))
        SkeletonBlock(Modifier.fillMaxWidth(0.3f), height = 10.dp)
        SkeletonBlock(Modifier.fillMaxWidth(0.55f), height = 44.dp)
        Spacer(Modifier.height(Spacing.xl))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SkeletonBlock(Modifier.size(208.dp), height = 208.dp, corner = 104.dp)
        }
    }
}

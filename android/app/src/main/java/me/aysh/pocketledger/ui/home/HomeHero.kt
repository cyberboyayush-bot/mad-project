package me.aysh.pocketledger.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.aysh.pocketledger.R
import me.aysh.pocketledger.data.model.MonthSummary
import me.aysh.pocketledger.ui.components.DisplayAmount
import me.aysh.pocketledger.ui.components.EaseOutQuint
import me.aysh.pocketledger.ui.components.Eyebrow
import me.aysh.pocketledger.ui.components.animatedPaise
import me.aysh.pocketledger.ui.theme.LedgerTheme
import me.aysh.pocketledger.ui.theme.PocketLedgerTheme
import me.aysh.pocketledger.ui.theme.Spacing
import me.aysh.pocketledger.util.Dates
import me.aysh.pocketledger.util.Money
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

/** The green "cover" card: month total, budget ruler and comparison with last month. */
@Composable
fun HomeHero(
    summary: MonthSummary,
    onSetBudget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LedgerTheme.colors
    val monthName = Dates.month(summary.month)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg)
            .background(colors.hero, RoundedCornerShape(28.dp))
            .padding(Spacing.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(stringResource(R.string.home_spent_in, monthName), color = colors.onHeroMuted, modifier = Modifier.weight(1f))
            Text(
                text = pluralStringResource(R.plurals.home_entries, summary.count, summary.count),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onHeroMuted,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        DisplayAmount(
            paise = animatedPaise(summary.totalPaise),
            color = colors.onHero,
            accentColor = colors.onHeroMuted,
            modifier = Modifier.semantics { contentDescription = Money.format(summary.totalPaise) },
        )
        Spacer(Modifier.height(Spacing.md))
        ComparisonPill(summary)
        Spacer(Modifier.height(Spacing.xl))
        if (summary.hasBudget) BudgetRuler(summary) else BudgetPrompt(onSetBudget)
    }
}

@Composable
private fun ComparisonPill(summary: MonthSummary) {
    val colors = LedgerTheme.colors
    val previousName = Dates.month(summary.month.minusMonths(1))
    if (summary.previousTotalPaise <= 0) return
    val change = ((summary.totalPaise - summary.previousTotalPaise) * 100 / summary.previousTotalPaise).toInt()
    val (icon, text) = when {
        change < 0 -> Icons.AutoMirrored.Rounded.TrendingDown to stringResource(R.string.home_vs_last_month_down, abs(change), previousName)
        change > 0 -> Icons.AutoMirrored.Rounded.TrendingUp to stringResource(R.string.home_vs_last_month_up, change, previousName)
        else -> Icons.AutoMirrored.Rounded.TrendingFlat to stringResource(R.string.home_vs_last_month_same, previousName)
    }
    Row(
        modifier = Modifier
            .background(colors.heroTrack, CircleShape)
            .padding(start = Spacing.sm + Spacing.xxs, end = Spacing.md, top = Spacing.xs + Spacing.xxs, bottom = Spacing.xs + Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (change > 0) colors.heroWarning else colors.onHero, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(Spacing.xs + Spacing.xxs))
        Text(text, style = MaterialTheme.typography.labelMedium, color = colors.onHero, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Budget progress drawn as a ledger ruler: evenly spaced ticks, every sixth one taller,
 * filled from the left as the month's spending grows.
 */
@Composable
private fun BudgetRuler(summary: MonthSummary) {
    val colors = LedgerTheme.colors
    val over = summary.remainingPaise < 0
    val percent = (summary.budgetProgress * 100).roundToInt()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(summary.budgetProgress) {
        progress.animateTo(summary.budgetProgress.coerceAtMost(1f), tween(1100, easing = EaseOutQuint))
    }
    val fill = if (over) colors.heroWarning else colors.onHero
    val label = if (over) {
        stringResource(R.string.home_over_by, Money.format(-summary.remainingPaise), Money.format(summary.budgetPaise))
    } else {
        stringResource(R.string.home_left_of, Money.format(summary.remainingPaise), Money.format(summary.budgetPaise))
    }
    val used = stringResource(R.string.home_budget_used, percent)
    Column(Modifier.clearAndSetSemantics { contentDescription = "$label, $used" }) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(22.dp),
        ) {
            val ticks = 49
            val tickWidth = 2.5.dp.toPx()
            val step = (size.width - tickWidth) / (ticks - 1)
            val filledUntil = progress.value * (ticks - 1)
            for (i in 0 until ticks) {
                val major = i % 6 == 0
                val h = if (major) size.height else size.height * 0.55f
                val filled = progress.value > 0f && i <= filledUntil
                drawRoundRect(
                    color = if (filled) fill else colors.heroTrack,
                    topLeft = Offset(i * step, size.height - h),
                    size = Size(tickWidth, h),
                    cornerRadius = CornerRadius(tickWidth / 2),
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (over) colors.heroWarning else colors.onHero,
                modifier = Modifier.weight(1f),
                maxLines = 2,
            )
            Spacer(Modifier.width(Spacing.md))
            Text(used, style = MaterialTheme.typography.labelMedium, color = colors.onHeroMuted)
        }
    }
}

@Composable
private fun BudgetPrompt(onSetBudget: () -> Unit) {
    val colors = LedgerTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.home_no_budget),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onHeroMuted,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(Spacing.md))
        Button(
            onClick = onSetBudget,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.onHero, contentColor = colors.hero),
            modifier = Modifier.heightIn(min = 40.dp),
        ) {
            Text(stringResource(R.string.home_set_budget), style = MaterialTheme.typography.labelLarge)
        }
    }
}

internal val previewSummary = MonthSummary(
    month = YearMonth.now(),
    totalPaise = 12_480_50,
    count = 18,
    previousTotalPaise = 14_200_00,
    budgetPaise = 15_000_00,
    byCategory = emptyList(),
    byDay = emptyList(),
)

@Preview
@Composable
private fun HomeHeroPreview() {
    PocketLedgerTheme { HomeHero(previewSummary, onSetBudget = {}) }
}

@Preview
@Composable
private fun HomeHeroDarkPreview() {
    PocketLedgerTheme(darkTheme = true) {
        HomeHero(previewSummary.copy(budgetPaise = 0), onSetBudget = {}, modifier = Modifier.background(Color.Black))
    }
}

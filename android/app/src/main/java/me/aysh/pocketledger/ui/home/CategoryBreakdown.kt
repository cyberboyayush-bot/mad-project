package me.aysh.pocketledger.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.aysh.pocketledger.data.model.Category
import me.aysh.pocketledger.data.model.CategoryTotal
import me.aysh.pocketledger.ui.components.EaseOutQuint
import me.aysh.pocketledger.ui.components.color
import me.aysh.pocketledger.ui.theme.MoneyStyles
import me.aysh.pocketledger.ui.theme.PocketLedgerTheme
import me.aysh.pocketledger.ui.theme.Spacing
import me.aysh.pocketledger.util.Money
import kotlin.math.roundToInt

/** Segmented bar of every category plus a two-column legend of the top four. */
@Composable
fun CategoryBreakdown(totals: List<CategoryTotal>, modifier: Modifier = Modifier) {
    val sorted = remember(totals) { totals.filter { it.totalPaise > 0 }.sortedByDescending { it.totalPaise } }
    val sum = sorted.sumOf { it.totalPaise }.coerceAtLeast(1)
    val colors = sorted.map { it.category.color }
    val gapColor = MaterialTheme.colorScheme.background
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(900, easing = EaseOutQuint)) }

    Column(modifier.padding(horizontal = Spacing.gutter)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .clearAndSetSemantics { },
        ) {
            val gap = 3.dp.toPx()
            var x = 0f
            val visible = size.width * reveal.value
            sorted.forEachIndexed { i, item ->
                val w = size.width * item.totalPaise / sum
                val drawW = (minOf(x + w, visible) - x).coerceAtLeast(0f)
                if (drawW > 0f) drawRect(colors[i], Offset(x, 0f), Size(drawW, size.height))
                x += w
                if (i < sorted.lastIndex) drawRect(gapColor, Offset(x - gap / 2, 0f), Size(gap, size.height))
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        sorted.take(4).chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs + Spacing.xxs), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                pair.forEach { item -> LegendCell(item, sum, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LegendCell(item: CategoryTotal, sum: Long, modifier: Modifier = Modifier) {
    val percent = (item.totalPaise * 100.0 / sum).roundToInt()
    val amount = Money.format(item.totalPaise)
    Row(
        modifier.semantics(mergeDescendants = true) { contentDescription = "${item.category.label}, $amount, $percent%" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.size(8.dp).background(item.category.color, CircleShape))
        Spacer(Modifier.width(Spacing.sm + Spacing.xxs))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.category.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(Spacing.xs + Spacing.xxs))
                Text("$percent%", style = MoneyStyles.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(amount, style = MoneyStyles.small, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Preview
@Composable
private fun CategoryBreakdownPreview() {
    PocketLedgerTheme {
        CategoryBreakdown(
            listOf(
                CategoryTotal(Category.Food, 4_200_00, 8),
                CategoryTotal(Category.Transport, 1_800_00, 5),
                CategoryTotal(Category.Bills, 3_100_00, 2),
                CategoryTotal(Category.Entertainment, 900_00, 2),
                CategoryTotal(Category.Health, 400_00, 1),
            ),
            Modifier.background(MaterialTheme.colorScheme.background),
        )
    }
}

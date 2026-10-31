package me.aysh.pocketledger.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.aysh.pocketledger.data.model.Category
import me.aysh.pocketledger.data.model.CategoryTotal
import me.aysh.pocketledger.ui.components.EaseOutQuint
import me.aysh.pocketledger.ui.components.color
import me.aysh.pocketledger.ui.theme.MoneyStyles
import me.aysh.pocketledger.ui.theme.PocketLedgerTheme
import me.aysh.pocketledger.util.Money
import java.time.LocalDate
import java.time.YearMonth

/**
 * Category donut: thick ring, butt-ended segments separated by small gaps, sweeping in
 * clockwise from 12 o'clock. [center] is drawn inside the ring.
 */
@Composable
fun DonutChart(
    totals: List<CategoryTotal>,
    animationKey: Any,
    description: String,
    modifier: Modifier = Modifier,
    size: Dp = 208.dp,
    thickness: Dp = 26.dp,
    center: @Composable () -> Unit = {},
) {
    val sum = totals.sumOf { it.totalPaise }.coerceAtLeast(1)
    val colors = totals.map { it.category.color }
    val track = MaterialTheme.colorScheme.outlineVariant
    val sweep = remember(animationKey) { Animatable(0f) }
    LaunchedEffect(animationKey) { sweep.animateTo(1f, tween(1100, easing = EaseOutQuint)) }

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .size(size)
                .semantics { contentDescription = description },
        ) {
            val stroke = thickness.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            val gap = if (totals.size > 1) 1.6f else 0f
            var start = -90f
            val visibleEnd = -90f + 360f * sweep.value
            totals.forEachIndexed { i, item ->
                val full = 360f * item.totalPaise / sum
                val segStart = start + gap / 2
                val segEnd = minOf(start + full - gap / 2, visibleEnd)
                if (segEnd > segStart) {
                    drawArc(colors[i], segStart, segEnd - segStart, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                }
                start += full
            }
        }
        center()
    }
}

/**
 * Daily bars for a month. Today's bar is drawn in the accent colour, future days are left
 * empty, and a dashed line marks the daily average.
 */
@Composable
fun DailyBarChart(
    month: YearMonth,
    totals: List<Long>,
    averagePaise: Long,
    averageLabel: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary
    val bar = scheme.onBackground.copy(alpha = 0.26f)
    val baseline = scheme.outline
    val labelColor = scheme.onSurfaceVariant
    val labelBg = scheme.background
    val measurer = rememberTextMeasurer()
    val labelStyle = MoneyStyles.small.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize, color = labelColor)
    val today = LocalDate.now()
    val todayIndex = if (YearMonth.from(today) == month) today.dayOfMonth - 1 else -1
    val lastVisible = if (todayIndex >= 0) todayIndex else totals.lastIndex
    val max = (totals.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val grow = remember(month, totals) { Animatable(0f) }
    LaunchedEffect(month, totals) { grow.animateTo(1f, tween(1000, easing = EaseOutQuint)) }

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(196.dp)
                .semantics { contentDescription = description },
        ) {
            val labelBand = 22.dp.toPx()
            val topBand = 18.dp.toPx()
            val chartHeight = size.height - labelBand - topBand
            val n = totals.size.coerceAtLeast(1)
            val slot = size.width / n
            val barWidth = (slot * 0.56f).coerceAtMost(10.dp.toPx())
            val bottom = topBand + chartHeight

            // Max value label, top-left.
            val maxText = measurer.measure(Money.formatCompact(max), labelStyle)
            drawText(maxText, topLeft = Offset(0f, 0f))
            drawLine(baseline.copy(alpha = 0.5f), Offset(0f, topBand), Offset(size.width, topBand), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx())))

            totals.forEachIndexed { i, value ->
                if (i > lastVisible) return@forEachIndexed
                val local = ((grow.value - i * 0.35f / n) / 0.65f).coerceIn(0f, 1f)
                val h = if (value > 0) (chartHeight * value / max).coerceAtLeast(3.dp.toPx()) * local else 0f
                val x = i * slot + (slot - barWidth) / 2
                if (h > 0f) {
                    drawRoundRect(
                        color = if (i == todayIndex) accent else bar,
                        topLeft = Offset(x, bottom - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(barWidth / 2),
                    )
                }
            }
            drawLine(baseline, Offset(0f, bottom), Offset(size.width, bottom), 1.dp.toPx())

            if (averagePaise > 0) {
                val y = bottom - chartHeight * (averagePaise.toFloat() / max) * grow.value
                drawLine(accent.copy(alpha = 0.8f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())))
                val avgText = measurer.measure(averageLabel, labelStyle.copy(color = accent))
                val pad = 4.dp.toPx()
                val tx = size.width - avgText.size.width - pad * 2
                val ty = (y - avgText.size.height - pad).coerceAtLeast(0f)
                drawRoundRect(labelBg, Offset(tx, ty), Size(avgText.size.width + pad * 2f, avgText.size.height.toFloat()), CornerRadius(pad))
                drawText(avgText, topLeft = Offset(tx + pad, ty))
            }

            // Day labels: 1, 8, 15, 22 and the last day.
            val marks = listOf(0, 7, 14, 21, totals.lastIndex).distinct()
            marks.forEach { i ->
                val text = measurer.measure((i + 1).toString(), labelStyle.copy(color = if (i == todayIndex) accent else labelColor))
                val cx = i * slot + slot / 2
                val x = (cx - text.size.width / 2).coerceIn(0f, size.width - text.size.width)
                drawText(text, topLeft = Offset(x, bottom + 6.dp.toPx()))
            }
        }
    }
}

@Preview
@Composable
private fun ChartsPreview() {
    PocketLedgerTheme {
        Column(Modifier.background(MaterialTheme.colorScheme.background)) {
            DonutChart(
                totals = listOf(
                    CategoryTotal(Category.Food, 4_200_00, 8),
                    CategoryTotal(Category.Bills, 3_100_00, 2),
                    CategoryTotal(Category.Transport, 1_800_00, 5),
                ),
                animationKey = 1,
                description = "",
            ) { Text("₹9.1K") }
            DailyBarChart(
                month = YearMonth.now(),
                totals = List(30) { (it * 37 % 11) * 10_000L },
                averagePaise = 40_000,
                averageLabel = "avg ₹400",
                description = "",
            )
        }
    }
}

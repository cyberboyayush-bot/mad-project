package me.aysh.pocketledger.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.aysh.pocketledger.R
import me.aysh.pocketledger.data.model.Category
import me.aysh.pocketledger.ui.components.Eyebrow
import me.aysh.pocketledger.ui.components.EaseOutQuint
import me.aysh.pocketledger.ui.components.color
import me.aysh.pocketledger.ui.components.displayAmount
import me.aysh.pocketledger.ui.theme.LedgerTheme
import me.aysh.pocketledger.ui.theme.MoneyStyles
import me.aysh.pocketledger.ui.theme.PocketLedgerTheme
import me.aysh.pocketledger.ui.theme.Spacing
import me.aysh.pocketledger.util.Money

/** A rectangle whose bottom edge is torn like a till receipt. */
private val ReceiptShape = GenericShape { size, _ ->
    val tooth = 14f * (size.width / 300f).coerceAtLeast(0.6f)
    val depth = tooth * 0.55f
    val radius = size.width * 0.07f
    moveTo(0f, radius)
    quadraticTo(0f, 0f, radius, 0f)
    lineTo(size.width - radius, 0f)
    quadraticTo(size.width, 0f, size.width, radius)
    lineTo(size.width, size.height - depth)
    var x = size.width
    var down = true
    while (x > 0f) {
        x = (x - tooth / 2).coerceAtLeast(0f)
        lineTo(x, if (down) size.height else size.height - depth)
        down = !down
    }
    lineTo(0f, size.height - depth)
    close()
}

/**
 * Decorative composition for the welcome screen: a receipt in front of a weekly chart card.
 * Purely visual, so it's hidden from accessibility services.
 */
@Composable
fun WelcomeArt(visible: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        ChartCard(
            visible = visible,
            modifier = Modifier
                .offset(x = 78.dp, y = (-74).dp)
                .rotate(5f),
        )
        ReceiptCard(
            modifier = Modifier
                .offset(x = (-44).dp, y = 30.dp)
                .rotate(-4f),
        )
    }
}

@Composable
private fun ReceiptCard(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val lines = listOf(
        Triple(Category.Food, stringResource(R.string.welcome_receipt_1), 6_000L),
        Triple(Category.Transport, stringResource(R.string.welcome_receipt_2), 50_000L),
        Triple(Category.Shopping, stringResource(R.string.welcome_receipt_3), 1_24_000L),
    )
    val total = lines.sumOf { it.third }
    Column(
        modifier = modifier
            .width(232.dp)
            .shadow(18.dp, ReceiptShape, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
            .background(scheme.surfaceBright, ReceiptShape)
            .border(1.dp, scheme.outlineVariant, ReceiptShape)
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 26.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(stringResource(R.string.welcome_receipt_title), Modifier.weight(1f))
            Box(Modifier.size(6.dp).background(LedgerTheme.colors.positive, CircleShape))
        }
        Spacer(Modifier.height(Spacing.md))
        lines.forEach { (category, label, paise) ->
            Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(category.color, CircleShape))
                Spacer(Modifier.width(Spacing.sm + Spacing.xxs))
                Text(label, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface, maxLines = 1, modifier = Modifier.weight(1f))
                Text(Money.format(paise), style = MoneyStyles.small.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize), color = scheme.onSurface)
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        DashedRule(scheme.outline)
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.welcome_receipt_total), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f).padding(bottom = 6.dp))
            val style = MaterialTheme.typography.headlineMedium
            Text(displayAmount(total, scheme.onSurfaceVariant), style = style, color = scheme.onSurface)
        }
    }
}

@Composable
private fun DashedRule(color: Color) {
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = Offset.Zero,
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        )
    }
}

@Composable
private fun ChartCard(visible: Boolean, modifier: Modifier = Modifier) {
    val colors = LedgerTheme.colors
    val heights = listOf(0.35f, 0.62f, 0.28f, 0.9f, 0.48f, 0.7f, 0.4f)
    val grow = remember { Animatable(0f) }
    LaunchedEffect(visible) { if (visible) grow.animateTo(1f, tween(1100, delayMillis = 250, easing = EaseOutQuint)) }
    Column(
        modifier = modifier
            .width(196.dp)
            .shadow(14.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.2f))
            .background(colors.hero, RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Eyebrow(stringResource(R.string.welcome_chart_caption), color = colors.onHeroMuted)
        Canvas(Modifier.fillMaxWidth().height(84.dp)) {
            val gap = 8.dp.toPx()
            val barWidth = (size.width - gap * (heights.size - 1)) / heights.size
            heights.forEachIndexed { i, h ->
                val barHeight = size.height * h * grow.value
                val highlight = i == 5
                drawRoundRect(
                    color = if (highlight) colors.onHero else colors.heroTrack,
                    topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2),
                )
            }
        }
    }
}

@Preview(widthDp = 380)
@Composable
private fun WelcomeArtPreview() {
    PocketLedgerTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) { WelcomeArt(visible = true) }
    }
}

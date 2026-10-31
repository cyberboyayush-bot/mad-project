package me.aysh.pocketledger.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.aysh.pocketledger.R
import me.aysh.pocketledger.ui.theme.PocketLedgerTheme
import me.aysh.pocketledger.ui.theme.Spacing

/** A blank ledger page: the recurring motif for "nothing here yet". */
@Composable
fun BlankPageArt(modifier: Modifier = Modifier) {
    val rule = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary
    val paper = MaterialTheme.colorScheme.surfaceBright
    Canvas(modifier.size(width = 76.dp, height = 92.dp)) {
        val corner = CornerRadius(10.dp.toPx())
        // Back sheet, offset like a page underneath.
        drawRoundRect(rule.copy(alpha = 0.35f), topLeft = Offset(8.dp.toPx(), 0f), size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()), cornerRadius = corner)
        val front = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx())
        val origin = Offset(0f, 8.dp.toPx())
        drawRoundRect(paper, topLeft = origin, size = front, cornerRadius = corner)
        drawRoundRect(rule, topLeft = origin, size = front, cornerRadius = corner, style = Stroke(1.dp.toPx()))
        // Margin line and ruled lines.
        val marginX = origin.x + 16.dp.toPx()
        drawLine(accent.copy(alpha = 0.5f), Offset(marginX, origin.y + 10.dp.toPx()), Offset(marginX, origin.y + front.height - 10.dp.toPx()), 1.dp.toPx())
        val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
        for (i in 0 until 5) {
            val y = origin.y + 20.dp.toPx() + i * 12.dp.toPx()
            drawLine(rule, Offset(marginX + 6.dp.toPx(), y), Offset(origin.x + front.width - 10.dp.toPx(), y), 1.dp.toPx(), cap = StrokeCap.Round, pathEffect = if (i > 1) dash else null)
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    art: @Composable () -> Unit = { BlankPageArt() },
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        art()
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.xl))
            Button(
                onClick = onAction,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
                modifier = Modifier.height(48.dp),
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.error_generic_title),
        body = message,
        actionLabel = stringResource(R.string.action_retry),
        onAction = onRetry,
        modifier = modifier,
        art = {
            Box(
                Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        },
    )
}

/** Gently pulsing placeholder block for loading skeletons. */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    corner: Dp = 6.dp,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "alpha",
    )
    val color = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(corner))
            .drawBehind { drawRect(color.copy(alpha = alpha)) },
    )
}

/** Skeleton for a list of expense rows. */
@Composable
fun SkeletonRows(count: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        repeat(count) { i ->
            androidx.compose.foundation.layout.Row(
                Modifier.padding(horizontal = Spacing.gutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBlock(Modifier.size(40.dp), height = 40.dp, corner = 13.dp)
                Column(Modifier.padding(start = Spacing.lg).weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SkeletonBlock(Modifier.fillMaxWidth(if (i % 2 == 0) 0.6f else 0.45f))
                    SkeletonBlock(Modifier.fillMaxWidth(0.3f), height = 10.dp)
                }
                SkeletonBlock(Modifier.size(width = 64.dp, height = 14.dp))
            }
        }
    }
}

@Preview
@Composable
private fun EmptyStatePreview() {
    PocketLedgerTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            EmptyState("A fresh page", "Nothing logged in October yet.", actionLabel = "Add expense", onAction = {})
        }
    }
}

@Preview
@Composable
private fun EmptyStateDarkPreview() {
    PocketLedgerTheme(darkTheme = true) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            EmptyState("A fresh page", "Nothing logged in October yet.", actionLabel = "Add expense", onAction = {})
        }
    }
}

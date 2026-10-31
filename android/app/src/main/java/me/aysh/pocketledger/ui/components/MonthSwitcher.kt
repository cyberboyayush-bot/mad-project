package me.aysh.pocketledger.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.aysh.pocketledger.R
import me.aysh.pocketledger.ui.theme.PocketLedgerTheme
import me.aysh.pocketledger.ui.theme.Spacing
import me.aysh.pocketledger.util.Dates
import java.time.YearMonth

/**
 * Month label on the left (serif, slides vertically when changed) and round previous/next
 * buttons on the right. Next is disabled for the current month.
 */
@Composable
fun MonthSwitcher(
    month: YearMonth,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    previousTag: String,
    nextTag: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInVertically(tween(260)) { it * dir / 2 } + fadeIn(tween(260))) togetherWith
                        (slideOutVertically(tween(200)) { -it * dir / 2 } + fadeOut(tween(160))) using
                        SizeTransform(clip = false)
                },
                label = "month",
            ) { m ->
                Text(
                    text = Dates.monthWithYear(m),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            RoundArrow(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                description = stringResource(R.string.previous_month),
                onClick = onPrevious,
                enabled = true,
                modifier = Modifier.testTag(previousTag),
            )
            RoundArrow(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                description = stringResource(R.string.next_month),
                onClick = onNext,
                enabled = canGoForward,
                modifier = Modifier.testTag(nextTag),
            )
        }
    }
}

@Composable
private fun RoundArrow(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outline
    OutlinedIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.dp, if (enabled) outline else outline.copy(alpha = 0.4f)),
        modifier = modifier.size(44.dp),
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(22.dp))
    }
}

@Preview
@Composable
private fun MonthSwitcherPreview() {
    PocketLedgerTheme {
        MonthSwitcher(
            month = YearMonth.of(2026, 10),
            canGoForward = false,
            onPrevious = {},
            onNext = {},
            previousTag = "prev",
            nextTag = "next",
            subtitle = "₹12,400 across 18 entries",
        )
    }
}

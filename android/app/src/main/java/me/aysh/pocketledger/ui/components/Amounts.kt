package me.aysh.pocketledger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import me.aysh.pocketledger.ui.theme.Geist
import me.aysh.pocketledger.ui.theme.PocketLedgerTheme
import me.aysh.pocketledger.util.Money

/**
 * Editorial money display: a small raised ₹ in Geist, large serif rupees and smaller paise.
 * Instrument Serif has no ₹ glyph, so the symbol is always set in Geist. Sizes are in em, so the
 * result scales with whatever style it is drawn in.
 */
fun displayAmount(
    paise: Long,
    symbolColor: Color,
    fractionColor: Color = symbolColor,
    alwaysShowPaise: Boolean = false,
): AnnotatedString {
    val body = Money.format(paise, alwaysShowPaise = alwaysShowPaise, withSymbol = false)
    val whole = body.substringBefore('.')
    val fraction = body.substringAfter('.', missingDelimiterValue = "")
    return buildAnnotatedString {
        withStyle(
            SpanStyle(
                fontFamily = Geist,
                fontWeight = FontWeight.Normal,
                fontSize = 0.38.em,
                baselineShift = BaselineShift(0.34f),
                color = symbolColor,
            ),
        ) { append("₹") }
        append('\u200A')
        append(whole)
        if (fraction.isNotEmpty()) {
            withStyle(SpanStyle(fontSize = 0.5.em, color = fractionColor)) { append(".$fraction") }
        }
    }
}

@Composable
fun DisplayAmount(
    paise: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayLarge,
    color: Color = LocalContentColor.current,
    accentColor: Color = color.copy(alpha = 0.6f),
    textAlign: TextAlign? = null,
    alwaysShowPaise: Boolean = false,
) {
    val formatted = Money.format(paise)
    // Shrinks in steps for very large amounts (₹10,00,000.00) instead of clipping.
    BasicText(
        text = displayAmount(paise, accentColor, alwaysShowPaise = alwaysShowPaise),
        style = style.merge(color = color, textAlign = textAlign ?: TextAlign.Unspecified),
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = style.fontSize * 0.5f, maxFontSize = style.fontSize, stepSize = 2.sp),
        modifier = modifier.semantics { contentDescription = formatted },
    )
}

/**
 * Counts from the previously shown value to [target]. The last shown value is saved, so coming
 * back to a tab doesn't replay the animation, but new data animates smoothly.
 */
@Composable
fun animatedPaise(target: Long, durationMillis: Int = 900): Long {
    var shown by rememberSaveable { mutableLongStateOf(0L) }
    val from = remember(target) { shown }
    val progress = remember(target) { Animatable(if (from == target) 1f else 0f) }
    LaunchedEffect(target) {
        progress.animateTo(1f, tween(durationMillis, easing = FastOutSlowInEasing))
        shown = target
    }
    if (progress.value >= 1f) return target
    // Whole rupees while counting, so the paise don't flicker.
    return (from + ((target - from) * progress.value.toDouble()).toLong()) / 100 * 100
}

@Preview
@Composable
private fun DisplayAmountPreview() {
    PocketLedgerTheme {
        DisplayAmount(paise = 1_234_567, color = MaterialTheme.colorScheme.onBackground)
    }
}

package me.aysh.pocketledger.ui.expense

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import me.aysh.pocketledger.ui.theme.Geist

/**
 * The editor's big centred amount. The raw input ("125000.5") is shown as "₹ 1,25,000.5" via a
 * visual transformation, so the ViewModel keeps a plain number while the user sees Indian grouping.
 */
@Composable
internal fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    enabled: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Step the size down as the number grows so ₹10,00,000.00 still fits on narrow phones.
    val base = MaterialTheme.typography.displayLarge
    val scale = when {
        value.length <= 7 -> 1f
        value.length <= 9 -> 0.8f
        else -> 0.68f
    }
    val style = base.copy(fontSize = base.fontSize * scale, lineHeight = base.lineHeight * scale, textAlign = TextAlign.Center)
    val ink = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val transformation = remember(style.fontSize, muted) { AmountTransformation(style.fontSize, muted) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = style.copy(color = ink),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        visualTransformation = transformation,
        modifier = modifier.fillMaxWidth(),
    )
}

private class AmountTransformation(private val fontSize: TextUnit, private val muted: Color) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val integer = raw.substringBefore('.')
        val hasDot = raw.contains('.')
        val fraction = raw.substringAfter('.', "")

        // originalToTransformed[i] = position in the output of original offset i.
        val o2t = IntArray(raw.length + 1)
        val out = StringBuilder()
        val symbolStyle = SpanStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = fontSize * 0.38f, baselineShift = BaselineShift(0.34f), color = muted)
        val prefix = "₹\u200A"
        val annotated = buildAnnotatedString {
            withStyle(symbolStyle) { append(prefix) }
            out.append(prefix)
            integer.forEachIndexed { d, c ->
                val remaining = integer.length - d
                if (d > 0 && (remaining == 3 || (remaining > 3 && (remaining - 3) % 2 == 0))) {
                    append(','); out.append(',')
                }
                o2t[d] = out.length
                append(c); out.append(c)
            }
            o2t[integer.length] = out.length
            if (hasDot) {
                append('.'); out.append('.')
                fraction.forEachIndexed { k, c ->
                    o2t[integer.length + 1 + k] = out.length
                    append(c); out.append(c)
                }
                o2t[raw.length] = out.length
            }
            if (raw.isEmpty()) withStyle(SpanStyle(color = muted.copy(alpha = 0.5f))) { append('0') }
        }

        // transformedToOriginal: count how many original characters sit before each output offset.
        // o2t[i] for i < raw.length is exactly where original char i was placed.
        val t2o = IntArray(annotated.length + 1) { t -> (0 until raw.length).count { o2t[it] < t } }

        return TransformedText(
            annotated,
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = o2t[offset.coerceIn(0, raw.length)]
                override fun transformedToOriginal(offset: Int) = t2o[offset.coerceIn(0, annotated.length)]
            },
        )
    }

    override fun equals(other: Any?) = other is AmountTransformation && other.fontSize == fontSize && other.muted == muted
    override fun hashCode() = fontSize.hashCode() * 31 + muted.hashCode()
}

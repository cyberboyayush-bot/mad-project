package me.aysh.pocketledger.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import me.aysh.pocketledger.R

/*
 * Two bundled typefaces:
 *  - Instrument Serif: editorial display face for headlines and the big hero amounts.
 *  - Geist: a crisp grotesk for UI text. It has tabular figures and a proper ₹ glyph,
 *    so every money value in lists and charts lines up in columns.
 */

private fun geist(weight: Int) = Font(
    resId = R.font.geist,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Geist = FontFamily(geist(400), geist(500), geist(600), geist(700))

val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Tabular numerals so amounts align and don't jiggle while animating. */
const val TABULAR = "tnum"

private val base = TextStyle(fontFamily = Geist)
private val serif = TextStyle(fontFamily = InstrumentSerif, fontWeight = FontWeight.Normal)

val LedgerTypography = Typography(
    displayLarge = serif.copy(fontSize = 64.sp, lineHeight = 64.sp, letterSpacing = (-0.02).em),
    displayMedium = serif.copy(fontSize = 52.sp, lineHeight = 54.sp, letterSpacing = (-0.02).em),
    displaySmall = serif.copy(fontSize = 42.sp, lineHeight = 46.sp, letterSpacing = (-0.015).em),
    headlineLarge = serif.copy(fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.01).em),
    headlineMedium = serif.copy(fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.01).em),
    headlineSmall = serif.copy(fontSize = 25.sp, lineHeight = 30.sp),
    titleLarge = base.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em),
    titleMedium = base.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.005).em),
    titleSmall = base.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = base.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = base.copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = base.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = base.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = base.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.em),
)

/** Money styles used outside the Material type scale. */
object MoneyStyles {
    val row = base.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR)
    val small = base.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR)
    val title = base.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR, letterSpacing = (-0.01).em)
}

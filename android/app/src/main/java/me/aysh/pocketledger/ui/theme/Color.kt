package me.aysh.pocketledger.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * "Ledger paper" palette: warm paper, soft ink and a single deep green accent.
 * Every colour used in the UI comes from here, either through the Material scheme or [LedgerColors].
 */

// Light: paper and ink
private val Paper = Color(0xFFF7F5F0)
private val PaperRaised = Color(0xFFFCFBF8)
private val PaperSunken = Color(0xFFEFECE4)
private val PaperDeep = Color(0xFFE7E3D9)
private val Ink = Color(0xFF1B1B18)
private val InkMuted = Color(0xFF68645B)
private val Rule = Color(0xFFD8D3C7)
private val RuleSoft = Color(0xFFE6E2D8)

// Brand green
private val Forest = Color(0xFF14532D)
private val ForestDeep = Color(0xFF0F3F22)
private val Moss = Color(0xFFDCE7DC)
private val Fern = Color(0xFF8CCB9F)
private val FernContainer = Color(0xFF1D3B27)

// Dark: warm near-black
private val Night = Color(0xFF121210)
private val NightRaised = Color(0xFF1A1A17)
private val NightHigh = Color(0xFF22221E)
private val NightHighest = Color(0xFF2A2A25)
private val Chalk = Color(0xFFECE9E1)
private val ChalkMuted = Color(0xFF9E998D)
private val NightRule = Color(0xFF38372F)
private val NightRuleSoft = Color(0xFF2B2A25)

internal val LightScheme: ColorScheme = lightColorScheme(
    primary = Forest,
    onPrimary = Paper,
    primaryContainer = Moss,
    onPrimaryContainer = ForestDeep,
    inversePrimary = Fern,
    secondary = Ink,
    onSecondary = Paper,
    secondaryContainer = PaperDeep,
    onSecondaryContainer = Ink,
    tertiary = Color(0xFF8A6116),
    onTertiary = Paper,
    tertiaryContainer = Color(0xFFF3E4C4),
    onTertiaryContainer = Color(0xFF3D2A05),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperSunken,
    onSurfaceVariant = InkMuted,
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF26261F),
    inverseOnSurface = Paper,
    error = Color(0xFFA8372A),
    onError = Paper,
    errorContainer = Color(0xFFF6E1DB),
    onErrorContainer = Color(0xFF5E1810),
    outline = Rule,
    outlineVariant = RuleSoft,
    scrim = Color(0x99000000),
    surfaceBright = PaperRaised,
    surfaceDim = PaperSunken,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = PaperRaised,
    surfaceContainer = PaperRaised,
    surfaceContainerHigh = PaperSunken,
    surfaceContainerHighest = PaperDeep,
)

internal val DarkScheme: ColorScheme = darkColorScheme(
    primary = Fern,
    onPrimary = Color(0xFF0B2414),
    primaryContainer = FernContainer,
    onPrimaryContainer = Color(0xFFC7E8D0),
    inversePrimary = Forest,
    secondary = Chalk,
    onSecondary = Night,
    secondaryContainer = NightHighest,
    onSecondaryContainer = Chalk,
    tertiary = Color(0xFFE2BC6E),
    onTertiary = Color(0xFF2E2004),
    tertiaryContainer = Color(0xFF3C2F14),
    onTertiaryContainer = Color(0xFFF3E4C4),
    background = Night,
    onBackground = Chalk,
    surface = Night,
    onSurface = Chalk,
    surfaceVariant = NightHigh,
    onSurfaceVariant = ChalkMuted,
    surfaceTint = Color.Transparent,
    inverseSurface = Chalk,
    inverseOnSurface = Night,
    error = Color(0xFFE48C76),
    onError = Color(0xFF3A0D06),
    errorContainer = Color(0xFF3A1D17),
    onErrorContainer = Color(0xFFF6D3CA),
    outline = NightRule,
    outlineVariant = NightRuleSoft,
    scrim = Color(0xCC000000),
    surfaceBright = NightHighest,
    surfaceDim = Night,
    surfaceContainerLowest = Color(0xFF0D0D0B),
    surfaceContainerLow = NightRaised,
    surfaceContainer = NightRaised,
    surfaceContainerHigh = NightHigh,
    surfaceContainerHighest = NightHighest,
)

/** Brand colours that Material's scheme has no slot for. */
@Immutable
data class LedgerColors(
    /** The deep green "cover" used by the hero card and brand moments. */
    val hero: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val heroTrack: Color,
    /** Over-budget tint that stays legible on the green hero. */
    val heroWarning: Color,
    /** Spending went down / within budget. */
    val positive: Color,
    /** Spending went up / over budget. */
    val caution: Color,
    val categories: CategoryPalette,
)

@Immutable
data class CategoryPalette(
    val food: Color,
    val transport: Color,
    val shopping: Color,
    val bills: Color,
    val entertainment: Color,
    val health: Color,
    val education: Color,
    val other: Color,
)

internal val LightLedgerColors = LedgerColors(
    hero = Forest,
    onHero = Paper,
    onHeroMuted = Color(0xB3F7F5F0),
    heroTrack = Color(0x33F7F5F0),
    heroWarning = Color(0xFFF5B48C),
    positive = Color(0xFF1F6B3D),
    caution = Color(0xFFA8372A),
    categories = CategoryPalette(
        food = Color(0xFFC0623B),
        transport = Color(0xFF3B6C90),
        shopping = Color(0xFF9A4D77),
        bills = Color(0xFFA67D22),
        entertainment = Color(0xFFB8434D),
        health = Color(0xFF2B7D6F),
        education = Color(0xFF4F5A9E),
        other = Color(0xFF7A766B),
    ),
)

internal val DarkLedgerColors = LedgerColors(
    hero = Color(0xFF173A24),
    onHero = Chalk,
    onHeroMuted = Color(0xB3ECE9E1),
    heroTrack = Color(0x2EECE9E1),
    heroWarning = Color(0xFFF0A983),
    positive = Fern,
    caution = Color(0xFFE48C76),
    categories = CategoryPalette(
        food = Color(0xFFE08F69),
        transport = Color(0xFF85ADCC),
        shopping = Color(0xFFCB8DB4),
        bills = Color(0xFFDDB963),
        entertainment = Color(0xFFE5868F),
        health = Color(0xFF72C2B3),
        education = Color(0xFF9EA6DD),
        other = Color(0xFFADA89B),
    ),
)

val LocalLedgerColors = staticCompositionLocalOf { LightLedgerColors }

package me.aysh.pocketledger.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Calm ease-out used for entrances. */
val EaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/** Flips to true right after first composition, to kick off entrance animations. */
@Composable
fun rememberEntrance(): Boolean {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    return started
}

/**
 * Staggered fade + rise. [index] orders elements; each starts a little after the previous one.
 */
@Composable
fun Modifier.reveal(visible: Boolean, index: Int = 0, distance: Int = 18): Modifier {
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 700, delayMillis = 70 * index, easing = EaseOutQuint),
        label = "reveal",
    )
    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * distance.dp.toPx()
    }
}

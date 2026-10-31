package me.aysh.pocketledger.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.aysh.pocketledger.R
import me.aysh.pocketledger.ui.components.Hairline
import me.aysh.pocketledger.ui.navigation.Tab
import me.aysh.pocketledger.ui.theme.LedgerTheme
import me.aysh.pocketledger.ui.theme.PocketLedgerTheme

private data class TabSpec(val tab: Tab, val label: Int, val selected: ImageVector, val unselected: ImageVector, val tag: String)

private val Tabs = listOf(
    TabSpec(Tab.Home, R.string.tab_home, Icons.Rounded.Home, Icons.Outlined.Home, "tab_home"),
    TabSpec(Tab.History, R.string.tab_history, Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth, "tab_history"),
    TabSpec(Tab.Stats, R.string.tab_stats, Icons.Rounded.PieChart, Icons.Outlined.PieChart, "tab_stats"),
    TabSpec(Tab.Profile, R.string.tab_profile, Icons.Rounded.Person, Icons.Outlined.Person, "tab_profile"),
)

/** Slot of each tab in the five-column bar; the add button owns the middle slot. */
private fun slotOf(tab: Tab) = when (tab) {
    Tab.Home -> 0
    Tab.History -> 1
    Tab.Stats -> 3
    Tab.Profile -> 4
}

/**
 * Paper-coloured bar with a hairline on top. A short ink rule slides along the top edge to
 * mark the current tab, and the add button sits in the middle as a green disc.
 */
@Composable
fun LedgerBottomBar(
    current: Tab,
    onSelect: (Tab) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        Hairline()
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(68.dp),
        ) {
            val slot = maxWidth / 5
            val indicatorWidth = 22.dp
            val x by animateDpAsState(
                targetValue = slot * slotOf(current) + (slot - indicatorWidth) / 2,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                label = "indicator",
            )
            Box(
                Modifier
                    .offset(x = x)
                    .size(width = indicatorWidth, height = 2.dp)
                    .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp)),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tabs.take(2).forEach { TabItem(it, current == it.tab, { onSelect(it.tab) }, Modifier.weight(1f)) }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { AddButton(onAdd) }
                Tabs.drop(2).forEach { TabItem(it, current == it.tab, { onSelect(it.tab) }, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TabItem(spec: TabSpec, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200),
        label = "tabColor",
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 36.dp),
            )
            .testTag(spec.tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) spec.selected else spec.unselected,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(spec.label),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "press")
    val colors = LedgerTheme.colors
    Box(
        Modifier
            .scale(scale)
            .size(54.dp)
            .shadow(8.dp, CircleShape, ambientColor = colors.hero, spotColor = colors.hero)
            .clip(CircleShape)
            .background(colors.hero)
            .clickable(
                onClick = onClick,
                role = Role.Button,
                interactionSource = interaction,
                indication = ripple(color = colors.onHero),
            )
            .testTag("add_expense"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = stringResource(R.string.add_expense),
            tint = colors.onHero,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Preview
@Composable
private fun BottomBarPreview() {
    PocketLedgerTheme {
        Box(Modifier.width(400.dp)) { LedgerBottomBar(Tab.History, {}, {}) }
    }
}

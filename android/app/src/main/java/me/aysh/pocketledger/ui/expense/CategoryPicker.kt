package me.aysh.pocketledger.ui.expense

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.aysh.pocketledger.data.model.Category
import me.aysh.pocketledger.ui.components.color
import me.aysh.pocketledger.ui.components.icon
import me.aysh.pocketledger.ui.theme.PocketLedgerTheme
import me.aysh.pocketledger.ui.theme.Spacing

/** Four-by-two grid of category tiles; the selected tile takes on its category colour. */
@Composable
internal fun CategoryPicker(
    selected: Category,
    onSelect: (Category) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Category.entries.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                row.forEach { category ->
                    CategoryTile(
                        category = category,
                        selected = category == selected,
                        enabled = enabled,
                        onClick = { onSelect(category) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(
    category: Category,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = category.color
    val shape = RoundedCornerShape(18.dp)
    val container by animateColorAsState(if (selected) tint.copy(alpha = 0.14f) else Color.Transparent, label = "tileBg")
    val borderColor by animateColorAsState(if (selected) tint else MaterialTheme.colorScheme.outlineVariant, label = "tileBorder")
    val borderWidth by animateDpAsState(if (selected) 1.5.dp else 1.dp, label = "tileBorderWidth")
    val iconBg by animateColorAsState(if (selected) tint else tint.copy(alpha = 0.12f), label = "iconBg")
    val iconTint by animateColorAsState(if (selected) MaterialTheme.colorScheme.background else tint, label = "iconTint")
    Column(
        modifier = modifier
            .clip(shape)
            .background(container)
            .border(borderWidth, borderColor, shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = Spacing.md, horizontal = Spacing.xs)
            .testTag("category_${category.key}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(iconBg, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(category.icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            category.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
private fun CategoryPickerPreview() {
    PocketLedgerTheme {
        CategoryPicker(Category.Transport, {}, enabled = true, modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp).fillMaxWidth())
    }
}

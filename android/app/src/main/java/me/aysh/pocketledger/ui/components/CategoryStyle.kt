package me.aysh.pocketledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.aysh.pocketledger.data.model.Category
import me.aysh.pocketledger.ui.theme.LedgerTheme

/** The one place that decides how a category looks. */
val Category.icon: ImageVector
    get() = when (this) {
        Category.Food -> Icons.Rounded.Restaurant
        Category.Transport -> Icons.Rounded.DirectionsBus
        Category.Shopping -> Icons.Rounded.ShoppingBag
        Category.Bills -> Icons.AutoMirrored.Rounded.ReceiptLong
        Category.Entertainment -> Icons.Rounded.ConfirmationNumber
        Category.Health -> Icons.Rounded.MonitorHeart
        Category.Education -> Icons.Rounded.School
        Category.Other -> Icons.Rounded.Category
    }

/** Category colour, tuned separately for light and dark themes. */
val Category.color: Color
    @Composable get() {
        val p = LedgerTheme.colors.categories
        return when (this) {
            Category.Food -> p.food
            Category.Transport -> p.transport
            Category.Shopping -> p.shopping
            Category.Bills -> p.bills
            Category.Entertainment -> p.entertainment
            Category.Health -> p.health
            Category.Education -> p.education
            Category.Other -> p.other
        }
    }

/** Rounded-square badge: category icon on a soft wash of its colour. */
@Composable
fun CategoryBadge(
    category: Category,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = size * 0.5f,
) {
    val tint = category.color
    Box(
        modifier = modifier
            .size(size)
            .background(tint.copy(alpha = 0.14f), RoundedCornerShape(size * 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(category.icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

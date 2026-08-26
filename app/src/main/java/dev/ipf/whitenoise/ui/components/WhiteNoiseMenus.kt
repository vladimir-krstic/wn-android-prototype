package dev.ipf.whitenoise.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R

/** Null selection denotes a command; non-null selection denotes a mutually exclusive choice. */
data class WhiteNoiseMenuItem(
    val label: String,
    val onClick: () -> Unit,
    @param:DrawableRes val icon: Int? = null,
    val selected: Boolean? = null,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
    val modifier: Modifier = Modifier,
)

/** Google's standard-color Expressive menu. Call beside its trigger inside the anchor's Box. */
@Composable
fun WhiteNoiseDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<WhiteNoiseMenuItem>,
    modifier: Modifier = Modifier,
    anchorSpacing: Dp = 0.dp,
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        // Outside the group's surface, so native above/below fitting includes the gap.
        // A positive Y offset alone would move an above-anchor menu toward its row.
        modifier = Modifier.padding(vertical = anchorSpacing),
    ) {
        DropdownMenuGroup(shapes = MenuDefaults.groupShapes(), modifier = modifier) {
            // The new popup supplies placement/motion, but unlike baseline DropdownMenu it does
            // not add scrolling. Keep the scroll inside the group so its corners clip the rows.
            Column(Modifier.verticalScroll(rememberScrollState())) {
                items.forEachIndexed { index, item ->
                    val shapes = MenuDefaults.itemShape(index, items.size)
                    val colors = if (item.destructive) {
                        MenuDefaults.selectableItemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        MenuDefaults.selectableItemColors()
                    }
                    val icon: (@Composable () -> Unit)? = item.icon?.let { resource ->
                        { MenuIcon(resource) }
                    }
                    val onClick = {
                        onDismissRequest()
                        item.onClick()
                    }
                    if (item.selected == null) {
                        DropdownMenuItem(
                            onClick = onClick,
                            text = { Text(item.label) },
                            shape = shapes.shape,
                            modifier = item.modifier,
                            leadingIcon = icon,
                            enabled = item.enabled,
                            colors = colors,
                        )
                    } else {
                        DropdownMenuItem(
                            selected = item.selected,
                            onClick = onClick,
                            text = { Text(item.label) },
                            shapes = shapes,
                            modifier = item.modifier,
                            leadingIcon = icon,
                            selectedLeadingIcon = { MenuIcon(R.drawable.ic_check) },
                            enabled = item.enabled,
                            colors = colors,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuIcon(@DrawableRes resource: Int) {
    Icon(
        painter = painterResource(resource),
        contentDescription = null,
        modifier = Modifier.size(MenuDefaults.LeadingIconSize),
    )
}

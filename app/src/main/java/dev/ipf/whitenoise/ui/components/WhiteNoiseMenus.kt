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
import androidx.compose.material3.DropdownMenuPopupPositionProvider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
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

enum class WhiteNoiseMenuPlacement {
    MaterialAdaptive,
    AboveAnchor,
}

/** Google's standard-color Expressive menu. Call beside its trigger inside the anchor's Box. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiteNoiseDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<WhiteNoiseMenuItem>,
    modifier: Modifier = Modifier,
    anchorSpacing: Dp = 0.dp,
    placement: WhiteNoiseMenuPlacement = WhiteNoiseMenuPlacement.MaterialAdaptive,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
) {
    val popupPositionProvider = when (placement) {
        WhiteNoiseMenuPlacement.MaterialAdaptive ->
            MenuDefaults.rememberDropdownMenuPopupPositionProvider(MenuAnchorPosition.Below)
        WhiteNoiseMenuPlacement.AboveAnchor -> rememberAboveAnchorMenuPositionProvider(anchorSpacing)
    }
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        popupPositionProvider = popupPositionProvider,
        modifier = if (placement == WhiteNoiseMenuPlacement.MaterialAdaptive) {
            Modifier.padding(vertical = anchorSpacing)
        } else {
            Modifier
        },
    ) {
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShapes(),
            modifier = modifier,
            shadowElevation = shadowElevation,
        ) {
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

/**
 * Material's stock provider keeps a 48 dp window-edge margin. A bottom composer trigger therefore
 * falls back to that margin instead of remaining attached. This provider retains Material's popup
 * and group while giving bottom-anchored composer menus the requested exact above-trigger gap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberAboveAnchorMenuPositionProvider(
    spacing: Dp,
): DropdownMenuPopupPositionProvider {
    val density = LocalDensity.current
    return remember(density, spacing) {
        AboveAnchorMenuPositionProvider(
            gapPx = with(density) { spacing.roundToPx() },
            edgeMarginPx = with(density) { 8.dp.roundToPx() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class AboveAnchorMenuPositionProvider(
    private val gapPx: Int,
    private val edgeMarginPx: Int,
) : DropdownMenuPopupPositionProvider {
    private var resolvedTransformOrigin by mutableStateOf(TransformOrigin.Center)

    override val transformOrigin: TransformOrigin
        get() = resolvedTransformOrigin

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val availableWidth = windowSize.width - (edgeMarginPx * 2)
        val preferredX = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.left
        } else {
            anchorBounds.right - popupContentSize.width
        }
        val x = if (popupContentSize.width >= availableWidth) {
            (windowSize.width - popupContentSize.width) / 2
        } else {
            preferredX.coerceIn(
                edgeMarginPx,
                windowSize.width - edgeMarginPx - popupContentSize.width,
            )
        }

        val aboveY = anchorBounds.top - gapPx - popupContentSize.height
        val belowY = anchorBounds.bottom + gapPx
        val fitsAbove = aboveY >= edgeMarginPx
        val fitsBelow = belowY + popupContentSize.height <= windowSize.height - edgeMarginPx
        val opensAbove = fitsAbove || !fitsBelow
        val y = when {
            opensAbove -> aboveY.coerceAtLeast(edgeMarginPx)
            fitsBelow -> belowY
            else -> ((windowSize.height - popupContentSize.height) / 2).coerceAtLeast(0)
        }
        val anchorCenterX = anchorBounds.left + anchorBounds.width / 2f
        val originX = if (popupContentSize.width == 0) {
            0.5f
        } else {
            ((anchorCenterX - x) / popupContentSize.width).coerceIn(0f, 1f)
        }
        resolvedTransformOrigin = TransformOrigin(originX, if (opensAbove) 1f else 0f)
        return IntOffset(x, y)
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

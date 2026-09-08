package dev.ipf.whitenoise.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/** Positional Material corners, with the preceding row owning the shared divider. */
internal data class ConnectedRowShape(val corners: Shape, val first: Boolean, val last: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        corners.createOutline(size, layoutDirection, density)
}

/** Native-weight perimeter with one physical-pixel divider, including independently lazy rows. */
internal fun Modifier.connectedRowBorder(shape: ConnectedRowShape, color: Color): Modifier = drawWithCache {
    val corners = when (val outline = shape.createOutline(size, layoutDirection, this)) {
        is Outline.Rounded -> outline.roundRect
        is Outline.Rectangle -> RoundRect(outline.rect)
        is Outline.Generic -> error("Connected rows require rounded or rectangular Material corners")
    }
    // Match Compose's pixel-rounded 1 dp border and inset its full width inside the shape.
    val strokeWidth = ceil(1.dp.toPx())
    val inset = strokeWidth / 2f
    val left = inset
    val right = size.width - inset
    val top = inset
    val bottom = size.height - inset
    val tl = (corners.topLeftCornerRadius.x - inset).coerceAtLeast(0f)
    val tr = (corners.topRightCornerRadius.x - inset).coerceAtLeast(0f)
    val br = (corners.bottomRightCornerRadius.x - inset).coerceAtLeast(0f)
    val bl = (corners.bottomLeftCornerRadius.x - inset).coerceAtLeast(0f)
    val path = Path().apply {
        if (shape.first) {
            moveTo(left, top + tl)
            if (tl > 0f) arcTo(Rect(left, top, left + 2 * tl, top + 2 * tl), 180f, 90f, false)
            lineTo(right - tr, top)
            if (tr > 0f) arcTo(Rect(right - 2 * tr, top, right, top + 2 * tr), 270f, 90f, false)
        } else moveTo(right, 0f)
        if (shape.last) {
            lineTo(right, bottom - br)
            if (br > 0f) arcTo(Rect(right - 2 * br, bottom - 2 * br, right, bottom), 0f, 90f, false)
            lineTo(left + bl, bottom)
            if (bl > 0f) arcTo(Rect(left, bottom - 2 * bl, left + 2 * bl, bottom), 90f, 90f, false)
        } else {
            lineTo(right, size.height)
            moveTo(left, size.height)
        }
        lineTo(left, if (shape.first) top + tl else 0f)
        if (shape.first && shape.last) close()
    }
    onDrawWithContent {
        drawContent()
        if (!shape.last) {
            drawLine(color, Offset(left, size.height - 0.5f), Offset(right, size.height - 0.5f), strokeWidth = 1f)
        }
        drawPath(path, color, style = Stroke(width = strokeWidth))
    }
}

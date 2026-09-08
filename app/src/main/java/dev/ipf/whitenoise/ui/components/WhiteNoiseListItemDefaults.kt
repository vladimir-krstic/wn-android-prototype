package dev.ipf.whitenoise.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.ui.theme.ConnectedRowShape
import dev.ipf.whitenoise.ui.theme.isAmoledOutline

/** Keep row boundaries stable while Material controls supply selection and interaction feedback. */
@OptIn(ExperimentalMaterial3Api::class)
object WhiteNoiseListItemDefaults {
    val segmentedGap
        @Composable get() = if (isAmoledOutline()) 0.dp else ListItemDefaults.SegmentedGap

    @Composable
    fun shapes(): ListItemShapes = ListItemDefaults.shapes().withStableShape()

    @Composable
    fun segmentedShapes(index: Int, count: Int): ListItemShapes {
        val defaults = ListItemDefaults.segmentedShapes(index, count)
        if (!isAmoledOutline()) return defaults.withStableShape()
        val corners = defaults.shape as CornerBasedShape
        val shape = corners.copy(
            topStart = if (index == 0) corners.topStart else CornerSize(0.dp),
            topEnd = if (index == 0) corners.topEnd else CornerSize(0.dp),
            bottomStart = if (index == count - 1) corners.bottomStart else CornerSize(0.dp),
            bottomEnd = if (index == count - 1) corners.bottomEnd else CornerSize(0.dp),
        )
        return defaults.copy(shape = ConnectedRowShape(shape, first = index == 0, last = index == count - 1)).withStableShape()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun ListItemShapes.withStableShape(): ListItemShapes = copy(
    selectedShape = shape,
    pressedShape = shape,
    focusedShape = shape,
    hoveredShape = shape,
    draggedShape = shape,
)

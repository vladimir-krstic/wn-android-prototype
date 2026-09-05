package dev.ipf.whitenoise.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.runtime.Composable

/** Keep row boundaries stable while Material controls supply selection and interaction feedback. */
@OptIn(ExperimentalMaterial3Api::class)
object WhiteNoiseListItemDefaults {
    @Composable
    fun shapes(): ListItemShapes = ListItemDefaults.shapes().withStableShape()

    @Composable
    fun segmentedShapes(index: Int, count: Int): ListItemShapes =
        ListItemDefaults.segmentedShapes(index, count).withStableShape()
}

@OptIn(ExperimentalMaterial3Api::class)
private fun ListItemShapes.withStableShape(): ListItemShapes = copy(
    selectedShape = shape,
    pressedShape = shape,
    focusedShape = shape,
    hoveredShape = shape,
    draggedShape = shape,
)

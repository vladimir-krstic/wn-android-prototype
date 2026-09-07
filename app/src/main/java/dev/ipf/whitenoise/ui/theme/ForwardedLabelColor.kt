package dev.ipf.whitenoise.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs

private val labelGrays = (0..255).map { Color(it, it, it) }

/** Prefer quiet gray, moving only as far as needed to keep small text readable. */
internal fun forwardedLabelColor(container: Color, content: Color): Color {
    val background = container.luminance()
    val preferred = if (content.luminance() > background) 153 else 102
    return labelGrays.withIndex().filter { (_, color) ->
        val foreground = color.luminance()
        (maxOf(background, foreground) + 0.05f) / (minOf(background, foreground) + 0.05f) >= 4.5f
    }.minBy { abs(it.index - preferred) }.value
}

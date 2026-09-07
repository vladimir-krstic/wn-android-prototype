package dev.ipf.whitenoise.ui.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

private val selectionNeutrals = (0..255).map { value ->
    Color(value, value, value).let { it to it.luminance() }
}

private fun contrast(a: Float, b: Float) = (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)

/** Native selection colors resolved against the actual message surface, including color overrides. */
internal fun messageSelectionColors(container: Color, text: Color, surrounding: Color): TextSelectionColors {
    val backgroundLuminance = container.luminance()
    val textLuminance = text.compositeOver(container).luminance()
    val surroundingLuminance = surrounding.luminance()
    // Handles protrude outside the bubble, so both adjacent surfaces matter.
    val handle = selectionNeutrals.maxBy { (_, luminance) ->
        minOf(contrast(luminance, backgroundLuminance), contrast(luminance, surroundingLuminance))
    }.first
    val readable = selectionNeutrals.filter { (_, luminance) -> contrast(textLuminance, luminance) >= 4.5f }
    val distinct = readable.filter { (_, luminance) -> contrast(luminance, backgroundLuminance) >= 3f }
    val highlight = (distinct.minByOrNull { (_, luminance) -> contrast(luminance, backgroundLuminance) }
        // Some custom midtone bubbles cannot provide both ratios without changing their text color.
        // Preserve readable text and use the strongest available selection distinction there.
        ?: readable.maxBy { (_, luminance) -> contrast(luminance, backgroundLuminance) }).first
    return TextSelectionColors(handleColor = handle, backgroundColor = highlight)
}

@Composable
internal fun MessageSelectionColors(container: Color, text: Color, content: @Composable () -> Unit) {
    val surrounding = MaterialTheme.colorScheme.surface
    val colors = remember(container, text, surrounding) { messageSelectionColors(container, text, surrounding) }
    CompositionLocalProvider(LocalTextSelectionColors provides colors, content = content)
}

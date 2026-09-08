package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

internal data class InlineFooterGeometry(val width: Int, val height: Int, val x: Int, val y: Int)

/** Keep the text untouched; use the final line's real occupied edge, including bidi layout. */
internal fun inlineFooterGeometry(
    textWidth: Int, textHeight: Int, lastLineRight: Int, lastBaseline: Int,
    footerWidth: Int, footerHeight: Int, footerBaseline: Int,
    maxWidth: Int, minWidth: Int, gap: Int,
): InlineFooterGeometry {
    val required = lastLineRight + gap + footerWidth
    val inline = required <= maxWidth && lastBaseline >= footerBaseline
    val width = maxOf(textWidth, footerWidth, minWidth, if (inline) required else 0).coerceAtMost(maxWidth)
    val y = if (inline) lastBaseline - footerBaseline else textHeight + gap / 2
    return InlineFooterGeometry(width, maxOf(textHeight, y + footerHeight), width - footerWidth, y)
}

private class FooterTextLayout { var result: TextLayoutResult? = null }

/** Measurement-only holder avoids a second frame or appending selectable spacer characters. */
@Composable
internal fun MessageTextWithFooter(
    footer: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    text: @Composable ((TextLayoutResult) -> Unit) -> Unit,
) {
    if (footer == null) {
        Box(modifier) { text {} }
        return
    }
    val measured = remember { FooterTextLayout() }
    Layout(
        modifier = modifier,
        content = {
            text { measured.result = it }
            Box { DisableSelection { footer() } }
        },
        measurePolicy = object : MeasurePolicy {
            override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
                (measurables[0].maxIntrinsicWidth(height).toLong() + measurables[1].maxIntrinsicWidth(height) + 8.dp.roundToPx())
                    .coerceAtMost(Constraints.Infinity.toLong()).toInt()

            override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
                maxOf(measurables[0].minIntrinsicWidth(height), measurables[1].minIntrinsicWidth(height))

            override fun MeasureScope.measure(children: List<Measurable>, constraints: Constraints): MeasureResult {
                val loose = constraints.copy(minWidth = 0, minHeight = 0)
                val footerPlaceable = children[1].measure(loose)
                val body = children[0].measure(loose)
                val result = measured.result
                val lastLine = (result?.lineCount ?: 1) - 1
                val footerBaseline = footerPlaceable[LastBaseline].takeIf { it != androidx.compose.ui.layout.AlignmentLine.Unspecified }
                    ?: footerPlaceable.height
                val geometry = inlineFooterGeometry(
                    textWidth = body.width, textHeight = body.height,
                    lastLineRight = result?.let { ceil(it.getLineRight(lastLine)).toInt() } ?: body.width,
                    lastBaseline = result?.let { it.getLineBaseline(lastLine).toInt() } ?: -1,
                    footerWidth = footerPlaceable.width, footerHeight = footerPlaceable.height,
                    footerBaseline = footerBaseline, maxWidth = constraints.maxWidth,
                    minWidth = constraints.minWidth, gap = 8.dp.roundToPx(),
                )
                return layout(geometry.width, geometry.height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
                    // Physical placement keeps timestamps at the right edge for both message directions.
                    body.place(0, 0)
                    footerPlaceable.place(geometry.x, geometry.y)
                }
            }
        },
    )
}

@Composable
internal fun MessageFooterRow(footer: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = AbsoluteAlignment.CenterRight) {
        DisableSelection { footer() }
    }
}

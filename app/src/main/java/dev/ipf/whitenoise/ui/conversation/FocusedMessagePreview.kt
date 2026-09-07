package dev.ipf.whitenoise.ui.conversation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import dev.ipf.whitenoise.model.DocumentBlock
import dev.ipf.whitenoise.model.MessageDocuments
import kotlin.math.roundToInt

internal val LocalFocusedMessagePreview = staticCompositionLocalOf { false }
internal const val FocusedPreviewTextLines = 5
internal const val FocusedPreviewMediaScale = 0.75f

/** A document excerpt is display-only; all commands still use the original message. */
internal fun focusedMessagePreviewText(source: String): AnnotatedString {
    val blocks = MessageDocuments.parse(source).blocks
    val paragraph = blocks.singleOrNull() as? DocumentBlock.Paragraph
        ?: return AnnotatedString(MessageDocuments.plainText(blocks))
    return buildAnnotatedString {
        paragraph.runs.forEach { run ->
            val decorations = buildList {
                if (run.style.strike) add(TextDecoration.LineThrough)
                if (run.destination != null) add(TextDecoration.Underline)
            }
            withStyle(SpanStyle(
                fontWeight = if (run.style.strong) FontWeight.Bold else null,
                fontStyle = if (run.style.emphasis) FontStyle.Italic else null,
                fontFamily = if (run.style.code) FontFamily.Monospace else null,
                textDecoration = decorations.takeIf { it.isNotEmpty() }?.let(TextDecoration::combine),
            )) { append(run.source.text) }
        }
    }
}

@Composable
internal fun FocusedMessageText(source: String, messageId: String) {
    Text(
        text = remember(source) { focusedMessagePreviewText(source) },
        style = MaterialTheme.typography.bodyLarge,
        maxLines = FocusedPreviewTextLines,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag("message.actions.excerpt.$messageId"),
    )
}

/** Scale only the media artwork, preserving its proportions and reporting its real bounds. */
@Composable
internal fun FocusedMediaPreview(content: @Composable () -> Unit) {
    if (!LocalFocusedMessagePreview.current) {
        content()
        return
    }
    Layout(content = content) { measurables, constraints ->
        val originalWidth = (constraints.maxWidth / FocusedPreviewMediaScale).roundToInt()
        val media = measurables.single().measure(Constraints(maxWidth = originalWidth, maxHeight = Constraints.Infinity))
        val width = (media.width * FocusedPreviewMediaScale).roundToInt().coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (media.height * FocusedPreviewMediaScale).roundToInt().coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            media.placeWithLayer(0, 0) {
                scaleX = FocusedPreviewMediaScale
                scaleY = FocusedPreviewMediaScale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

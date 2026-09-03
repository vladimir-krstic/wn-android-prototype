package dev.ipf.whitenoise.ui.conversation

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import dev.ipf.whitenoise.model.InlineMessageMarkup
import dev.ipf.whitenoise.model.Person

@Composable
internal fun InlineMessageText(
    text: String,
    people: List<Person>,
    onOpenPerson: (String) -> Unit,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    val uriHandler = LocalUriHandler.current
    val contentColor = if (color == Color.Unspecified) LocalContentColor.current else color
    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = contentColor, textDecoration = TextDecoration.Underline),
    )
    val mentionStyles = TextLinkStyles(
        style = SpanStyle(color = contentColor, fontWeight = FontWeight.SemiBold),
    )
    val annotated = buildAnnotatedString {
        InlineMessageMarkup.segments(text).forEach { segment ->
            val segmentStyle = SpanStyle(
                fontWeight = if (segment.strong) FontWeight.Bold else null,
                fontStyle = if (segment.emphasized) FontStyle.Italic else null,
            )
            withStyle(segmentStyle) {
                if (segment.destination != null) {
                    withLink(
                        LinkAnnotation.Url(
                            url = segment.destination,
                            styles = linkStyles,
                        ) {
                            runCatching { uriHandler.openUri((it as LinkAnnotation.Url).url) }
                        },
                    ) {
                        append(segment.text)
                    }
                } else {
                    appendMentions(segment.text, people, mentionStyles, onOpenPerson)
                }
            }
        }
    }
    Text(text = annotated, color = contentColor, style = style)
}

private fun AnnotatedString.Builder.appendMentions(
    text: String,
    people: List<Person>,
    styles: TextLinkStyles,
    onOpenPerson: (String) -> Unit,
) {
    var cursor = 0
    while (cursor < text.length) {
        val match = people.asSequence()
            .flatMap { person -> sequenceOf(person.name, person.displayName).distinct().map { name -> Triple(person, "@$name", text.indexOf("@$name", startIndex = cursor)) } }
            .filter { it.third >= 0 }
            .minWithOrNull(compareBy<Triple<Person, String, Int>> { it.third }.thenByDescending { it.second.length })
        if (match == null) {
            append(text.substring(cursor))
            break
        }
        val (person, mention, index) = match
        if (index > cursor) append(text.substring(cursor, index))
        withLink(
            LinkAnnotation.Clickable(
                tag = person.id,
                styles = styles,
                linkInteractionListener = { onOpenPerson(person.id) },
            ),
        ) {
            append(mention)
        }
        cursor = index + mention.length
    }
}

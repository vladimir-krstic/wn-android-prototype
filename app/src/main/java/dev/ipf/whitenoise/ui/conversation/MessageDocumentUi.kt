package dev.ipf.whitenoise.ui.conversation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

internal const val MessageSourceAnnotation = "white-noise-source-range"

internal fun selectedMessagePassage(source: String, fragments: List<AnnotatedString>): MessagePassage? {
    val ranges = fragments.flatMap { it.getStringAnnotations(MessageSourceAnnotation, 0, it.length) }
        .mapNotNull { annotation -> annotation.item.split(':').takeIf { it.size == 2 }?.let {
            val start = it[0].toIntOrNull(); val end = it[1].toIntOrNull()
            if (start == null || end == null || start !in source.indices || end !in (start + 1)..source.length) null else start to end
        } }
    if (ranges.isEmpty()) return null
    return MessagePassage(fragments.joinToString("\n") { it.text }, ranges.minOf { it.first }, ranges.maxOf { it.second })
}

@Composable
internal fun MessageDocumentContent(
    document: MessageDocument, people: List<Person>, onOpenPerson: (String) -> Unit,
    modifier: Modifier = Modifier, annotateSource: Boolean = false,
) {
    var pendingLink by rememberSaveable(document.source) { mutableStateOf<String?>(null) }
    var failedLink by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    val context = LocalContext.current
    val open: (String, Boolean) -> Unit = { destination, named ->
        if (named) pendingLink = destination
        else if (runCatching { uri.openUri(destination) }.isFailure) failedLink = true
    }
    DocumentBlocks(document.blocks, people, onOpenPerson, open, annotateSource, modifier)
    pendingLink?.let { destination ->
        AlertDialog(onDismissRequest = { pendingLink = null }, title = { Text(stringResource(R.string.message_open_link)) },
            text = { Text(destination) }, confirmButton = {
                TextButton(onClick = { pendingLink = null; if (runCatching { uri.openUri(destination) }.isFailure) failedLink = true }) { Text(stringResource(R.string.message_open)) }
            }, dismissButton = {
                Row {
                    TextButton(onClick = { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("Link", destination)); pendingLink = null }) { Text(stringResource(R.string.message_copy_link)) }
                    TextButton(onClick = { pendingLink = null }) { Text(stringResource(R.string.cancel)) }
                }
            })
    }
    if (failedLink) AlertDialog(onDismissRequest = { failedLink = false }, text = { Text(stringResource(R.string.message_link_unavailable)) },
        confirmButton = { TextButton(onClick = { failedLink = false }) { Text(stringResource(R.string.close)) } })
}

@Composable
private fun DocumentBlocks(blocks: List<DocumentBlock>, people: List<Person>, onPerson: (String) -> Unit,
    onLink: (String, Boolean) -> Unit, annotate: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
        blocks.forEachIndexed { index, block -> key(index, block) {
            when (block) {
                is DocumentBlock.Paragraph -> DocumentText(block.runs, people, onPerson, onLink, annotate)
                is DocumentBlock.Heading -> {
                    val typography = MaterialTheme.typography
                    val style = when (block.level) {
                        1 -> typography.headlineSmall
                        2 -> typography.titleLarge
                        3 -> typography.bodyLarge.copy(fontSize = typography.bodyLarge.fontSize * 1.25f)
                        4 -> typography.bodyLarge.copy(fontSize = typography.bodyLarge.fontSize * 1.125f)
                        5 -> typography.bodyLarge
                        else -> typography.bodyMedium
                    }.copy(fontWeight = FontWeight.SemiBold)
                    DocumentText(block.runs, people, onPerson, onLink, annotate, Modifier.semantics { heading() }, style)
                }
                is DocumentBlock.Code -> Surface(color = LocalContentColor.current.copy(alpha = 0.08f), shape = MaterialTheme.shapes.small) {
                    Column(Modifier.padding(WhiteNoiseSpacing.Related)) {
                        if (block.language.isNotBlank()) DisableSelection { Text(block.language, style = MaterialTheme.typography.labelSmall) }
                        DocumentText(listOf(DocumentRun(block.source, DocumentStyle(code = true))), people, onPerson, onLink, annotate,
                            Modifier.horizontalScroll(rememberScrollState()), MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
                    }
                }
                is DocumentBlock.Quote -> Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                    Box(Modifier.width(2.dp).fillMaxHeight().background(LocalContentColor.current.copy(alpha = 0.4f)))
                    DocumentBlocks(block.blocks, people, onPerson, onLink, annotate, Modifier.weight(1f))
                }
                is DocumentBlock.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                    block.items.forEach { item -> Row(horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                        DisableSelection {
                            val marker = when { item.checked == true -> "☑"; item.checked == false -> "☐"; item.number != null -> "${item.number}."; else -> "•" }
                            val state = item.checked?.let { stringResource(if (it) R.string.message_task_done else R.string.message_task_not_done) }
                            Text(marker, Modifier.semantics { state?.let { stateDescription = it } }, fontFamily = FontFamily.Monospace)
                        }
                        DocumentBlocks(item.blocks, people, onPerson, onLink, annotate, Modifier.weight(1f))
                    } }
                }
                is DocumentBlock.Table -> Column(Modifier.horizontalScroll(rememberScrollState()).testTag("message.document.table")) {
                    (listOf(block.header) + block.rows).forEachIndexed { rowIndex, cells ->
                        Row {
                            cells.forEachIndexed { cellIndex, runs ->
                                DocumentText(runs, people, onPerson, onLink, annotate,
                                    Modifier.width(160.dp).padding(WhiteNoiseSpacing.Related),
                                    if (rowIndex == 0) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodyLarge,
                                    when (block.alignments.getOrNull(cellIndex)) { DocumentAlignment.Center -> TextAlign.Center; DocumentAlignment.End -> TextAlign.End; else -> TextAlign.Start })
                            }
                        }
                        HorizontalDivider(color = LocalContentColor.current.copy(alpha = 0.25f))
                    }
                }
                is DocumentBlock.Details -> {
                    var expanded by rememberSaveable(block) { mutableStateOf(block.initiallyOpen) }
                    Column {
                        DisableSelection {
                            val state = stringResource(if (expanded) R.string.message_expanded else R.string.message_collapsed)
                            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.semantics { stateDescription = state },
                                colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current)) {
                                Text(block.summary.joinToString("") { it.source.text })
                            }
                        }
                        if (expanded) DocumentBlocks(block.blocks, people, onPerson, onLink, annotate)
                    }
                }
                is DocumentBlock.Blank -> Spacer(Modifier.height(WhiteNoiseSpacing.Related * (block.count - 1).coerceAtLeast(0)))
                DocumentBlock.Divider -> HorizontalDivider(color = LocalContentColor.current.copy(alpha = 0.25f))
            }
        } }
    }
}

@Composable
private fun DocumentText(runs: List<DocumentRun>, people: List<Person>, onPerson: (String) -> Unit,
    onLink: (String, Boolean) -> Unit, annotate: Boolean, modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge, align: TextAlign? = null) {
    val color = LocalContentColor.current
    val text = buildAnnotatedString {
        runs.forEach { run ->
            val start = length
            val span = SpanStyle(fontWeight = if (run.style.strong) FontWeight.Bold else null,
                fontStyle = if (run.style.emphasis) FontStyle.Italic else null,
                fontFamily = if (run.style.code) FontFamily.Monospace else null,
                textDecoration = if (run.style.strike) TextDecoration.LineThrough else null,
                background = if (run.style.code) color.copy(alpha = 0.08f) else Color.Unspecified)
            withStyle(span) {
                if (run.destination != null) withLink(LinkAnnotation.Url(run.destination,
                    TextLinkStyles(SpanStyle(color = color, textDecoration = TextDecoration.Underline))) { onLink(run.destination, run.namedLink) }) { append(run.source.text) }
                else append(run.source.text)
            }
            if (run.destination == null && !run.style.code) {
                val mentionRanges = mutableListOf<IntRange>()
                people.flatMap { person -> listOf(person.name, person.displayName).distinct().map { person to "@$it" } }
                    .sortedByDescending { it.second.length }.forEach { (person, mention) ->
                        var cursor = run.source.text.indexOf(mention)
                        while (cursor >= 0) {
                            val end = cursor + mention.length
                            if (mentionRanges.none { range -> cursor in range || (end - 1) in range }) {
                                addLink(LinkAnnotation.Clickable(person.id,
                                    TextLinkStyles(SpanStyle(color = color, fontWeight = FontWeight.SemiBold))) { onPerson(person.id) }, start + cursor, start + end)
                                mentionRanges += cursor until end
                            }
                            cursor = run.source.text.indexOf(mention, end)
                        }
                    }
            }
            if (annotate) run.source.offsets.indices.forEach { i ->
                addStringAnnotation(MessageSourceAnnotation, "${run.source.offsets[i]}:${run.source.ends[i]}", start + i, start + i + 1)
            }
        }
    }
    Text(text, modifier, color = color, style = style, textAlign = align)
}

package dev.ipf.whitenoise.ui.conversation

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.MediaLayout
import dev.ipf.whitenoise.model.ConversationMediaKey
import dev.ipf.whitenoise.model.MessageAttachment
import dev.ipf.whitenoise.model.MessageAttachmentKind
import dev.ipf.whitenoise.model.Person
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

private data class VisualFrame(
    val attachment: MessageAttachment,
    val image: ProfileAvatar?,
    val imageIndex: Int,
)

@Composable
internal fun TimelineAttachmentContent(
    attachments: List<MessageAttachment>,
    outgoing: Boolean,
    onOpenMedia: (ConversationMediaKey) -> Unit,
    modifier: Modifier = Modifier,
    messageId: String? = null,
    searchQuery: String = "",
    voiceTranscript: String? = null,
    voiceTranscriptVisible: Boolean = false,
    people: List<Person> = emptyList(),
    onOpenPerson: ((String) -> Unit)? = null,
) {
    if (attachments.isEmpty()) return
    val visualAttachments = attachments.filter(MessageAttachment::isVisual)
    val singleMediaSize = rememberTimelineSingleMediaSize(attachments.singleOrNull())
    Column(
        modifier = modifier.width(richContentCanvasWidthDp(attachments, singleMediaSize).dp),
        verticalArrangement = Arrangement.spacedBy(ConversationMessageMetrics.RichContentSpacing),
    ) {
        if (visualAttachments.isNotEmpty()) {
            TimelineMediaGrid(
                messageId = messageId,
                attachments = visualAttachments,
                onClick = onOpenMedia,
                searchQuery = searchQuery,
            )
        }
        attachments.filterNot(MessageAttachment::isVisual).forEach { attachment ->
            when (attachment.kind) {
                MessageAttachmentKind.Voice -> VoiceMessageCard(
                    attachment = attachment,
                    outgoing = outgoing,
                    transcript = voiceTranscript ?: attachment.transcript,
                    transcriptVisible = voiceTranscriptVisible,
                )
                MessageAttachmentKind.Link -> LinkMessageCard(attachment, outgoing, searchQuery)
                MessageAttachmentKind.File,
                MessageAttachmentKind.Contact,
                -> DocumentOrContactCard(attachment, outgoing, searchQuery, people, onOpenPerson)
                else -> Unit
            }
        }
    }
}

@Composable
private fun TimelineMediaGrid(
    messageId: String?,
    attachments: List<MessageAttachment>,
    onClick: (ConversationMediaKey) -> Unit,
    searchQuery: String,
) {
    val frames = attachments.flatMap { attachment ->
        if (attachment.images.isEmpty()) listOf(VisualFrame(attachment, null, 0))
        else attachment.images.mapIndexed { index, image -> VisualFrame(attachment, image, index) }
    }
    val visible = frames.take(MediaLayout.visibleCount(frames.size))
    val overflow = MediaLayout.overflowCount(frames.size)
    val singleSize = rememberTimelineSingleMediaSize(visible.singleOrNull()?.attachment)
    val attachmentCountDescription = pluralStringResource(
        R.plurals.media_attachment_count,
        frames.size,
        frames.size,
    )
    // The grid owns the group count while every visible tile preserves its exact viewer frame.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ConversationRichContentShape)
            .testTag("conversation.media.grid.${messageId ?: attachments.joinToString { it.id }}")
            .semantics { contentDescription = attachmentCountDescription },
        verticalArrangement = Arrangement.spacedBy(ConversationMessageMetrics.GallerySpacing),
    ) {
        when (MediaLayout.forCount(frames.size)) {
            dev.ipf.whitenoise.model.MediaGridLayout.Single -> MediaTile(
                visible.first(),
                Modifier.width(singleSize!!.widthDp.dp)
                    .height(singleSize.heightDp.dp)
                    .clipToBounds(),
                searchQuery = searchQuery,
                contentScale = if (visible.first().attachment.kind == MessageAttachmentKind.Gif) {
                    ContentScale.Crop
                } else {
                    ContentScale.FillHeight
                },
                onClick = { frame ->
                    messageId?.let { onClick(ConversationMediaKey(it, frame.attachment.id, frame.imageIndex)) }
                },
            )
            dev.ipf.whitenoise.model.MediaGridLayout.Two -> Row(
                modifier = Modifier.fillMaxWidth().height(127.dp),
                horizontalArrangement = Arrangement.spacedBy(ConversationMessageMetrics.GallerySpacing),
            ) {
                visible.forEach { frame ->
                    MediaTile(
                        frame,
                        Modifier.weight(1f).fillMaxSize(),
                        searchQuery = searchQuery,
                        onClick = {
                            messageId?.let { id -> onClick(ConversationMediaKey(id, frame.attachment.id, frame.imageIndex)) }
                        },
                    )
                }
            }
            dev.ipf.whitenoise.model.MediaGridLayout.Three -> Row(
                modifier = Modifier.fillMaxWidth().height(170.dp),
                horizontalArrangement = Arrangement.spacedBy(ConversationMessageMetrics.GallerySpacing),
            ) {
                MediaTile(
                    visible[0],
                    Modifier.width(170.dp).height(170.dp),
                    searchQuery = searchQuery,
                    onClick = { frame ->
                        messageId?.let { onClick(ConversationMediaKey(it, frame.attachment.id, frame.imageIndex)) }
                    },
                )
                Column(
                    Modifier.weight(1f).fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(ConversationMessageMetrics.GallerySpacing),
                ) {
                    visible.drop(1).forEach { frame ->
                        MediaTile(
                            frame,
                            Modifier.fillMaxWidth().weight(1f),
                            searchQuery = searchQuery,
                            onClick = {
                                messageId?.let { id -> onClick(ConversationMediaKey(id, frame.attachment.id, frame.imageIndex)) }
                            },
                        )
                    }
                }
            }
            dev.ipf.whitenoise.model.MediaGridLayout.Four -> Column(
                modifier = Modifier.fillMaxWidth().height(256.dp),
                verticalArrangement = Arrangement.spacedBy(ConversationMessageMetrics.GallerySpacing),
            ) {
                visible.chunked(2).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(ConversationMessageMetrics.GallerySpacing),
                    ) {
                        row.forEach { frame ->
                            MediaTile(
                                frame,
                                Modifier.weight(1f).fillMaxSize(),
                                searchQuery = searchQuery,
                                onClick = {
                                    messageId?.let { id -> onClick(ConversationMediaKey(id, frame.attachment.id, frame.imageIndex)) }
                                },
                            )
                        }
                    }
                }
            }
            dev.ipf.whitenoise.model.MediaGridLayout.Five,
            dev.ipf.whitenoise.model.MediaGridLayout.FiveWithOverflow,
            -> Column(
                modifier = Modifier.fillMaxWidth().height(213.dp),
                verticalArrangement = Arrangement.spacedBy(ConversationMessageMetrics.GallerySpacing),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ConversationMessageMetrics.GallerySpacing),
                ) {
                    visible.take(2).forEach { frame ->
                        MediaTile(
                            frame,
                            Modifier.weight(1f).height(127.dp),
                            searchQuery = searchQuery,
                            onClick = {
                                messageId?.let { id -> onClick(ConversationMediaKey(id, frame.attachment.id, frame.imageIndex)) }
                            },
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ConversationMessageMetrics.GallerySpacing),
                ) {
                    visible.drop(2).forEachIndexed { index, frame ->
                        MediaTile(
                            frame,
                            Modifier.weight(1f).height(84.dp),
                            overflow = if (index == visible.drop(2).lastIndex) overflow else 0,
                            searchQuery = searchQuery,
                            onClick = {
                                messageId?.let { id -> onClick(ConversationMediaKey(id, frame.attachment.id, frame.imageIndex)) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaTile(
    frame: VisualFrame,
    modifier: Modifier,
    overflow: Int = 0,
    searchQuery: String,
    contentScale: ContentScale = ContentScale.Crop,
    onClick: (VisualFrame) -> Unit,
) {
    val opensViewer = frame.attachment.isAvailable && (
        frame.attachment.kind == MessageAttachmentKind.Photo ||
            frame.attachment.kind == MessageAttachmentKind.Photos ||
            frame.attachment.kind == MessageAttachmentKind.Video
        )
    Box(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .testTag("conversation.media.tile.${frame.attachment.id}.${frame.imageIndex}")
            .then(
                if (opensViewer) {
                    Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) {
                        onClick(frame)
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        frame.image?.let { ComposerImage(it, Modifier.fillMaxSize(), contentScale) }
            ?: SearchHighlightedText(
                text = frame.attachment.label,
                query = searchQuery,
                modifier = Modifier.align(Alignment.Center).padding(8.dp),
            )
        if (frame.attachment.kind == MessageAttachmentKind.Video && frame.attachment.isAvailable) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f),
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_arrow),
                    contentDescription = stringResource(R.string.play),
                    modifier = Modifier.padding(10.dp).size(24.dp),
                )
            }
            frame.attachment.durationSeconds?.let { seconds ->
                Text(
                    text = "%d:%02d".format(seconds / 60, seconds % 60),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(
                            MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.8f),
                            MaterialTheme.shapes.extraSmall,
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        if (frame.attachment.kind == MessageAttachmentKind.Video && !frame.attachment.isAvailable && overflow == 0) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning),
                    contentDescription = frame.attachment.label,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        }
        if (overflow > 0) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.58f)),
                contentAlignment = Alignment.Center,
            ) { Text("+$overflow", color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.titleLarge) }
        }
    }
}

@Composable
private fun LinkMessageCard(attachment: MessageAttachment, outgoing: Boolean, searchQuery: String) {
    val uriHandler = LocalUriHandler.current
    val destination = attachment.externalUri?.takeIf { attachment.isAvailable }
    val container = if (outgoing) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val content = if (outgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val secondaryContent = if (outgoing) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("conversation.attachment.${attachment.id}")
            .clip(ConversationRichContentShape)
            .then(
                if (destination != null) {
                    Modifier.clickable { runCatching { uriHandler.openUri(destination) } }
                } else {
                    Modifier
                },
            ),
        shape = ConversationRichContentShape,
        color = container,
        contentColor = content,
    ) {
        Column {
            attachment.images.firstOrNull()?.let {
                ComposerImage(
                    it,
                    Modifier.fillMaxWidth().height(ConversationMessageMetrics.LinkImageHeight),
                )
            }
            Column(
                Modifier.padding(ConversationMessageMetrics.RichComponentInset),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                attachment.linkDomain?.let {
                    SearchHighlightedText(
                        text = it,
                        query = searchQuery,
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SearchHighlightedText(
                    text = attachment.linkTitle ?: attachment.label,
                    query = searchQuery,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                attachment.linkSummary?.let {
                    Text(
                        it,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryContent,
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentOrContactCard(
    attachment: MessageAttachment,
    outgoing: Boolean,
    searchQuery: String,
    people: List<Person>,
    onOpenPerson: ((String) -> Unit)?,
) {
    val context = LocalContext.current
    val bundled = bundledResource(attachment.label)
    val canOpenFile = attachment.kind == MessageAttachmentKind.File && attachment.isAvailable && (
        attachment.externalUri?.let {
            runCatching { it.toUri().scheme == "content" }.getOrDefault(false)
        } == true || bundled != null
        )
    val person = attachment.contactPersonId?.let { id -> people.firstOrNull { it.id == id } }
    val canOpenPerson = attachment.kind == MessageAttachmentKind.Contact && person != null && onOpenPerson != null
    val openAction: (() -> Unit)? = when {
        canOpenPerson -> ({ onOpenPerson(person.id) })
        canOpenFile -> ({
            if (bundled != null) {
                openBundledResource(context, bundled.first, bundled.second, bundled.third)
            } else {
                openContentUri(context, attachment.externalUri.orEmpty())
            }
        })
        else -> null
    }
    val container = if (outgoing) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val content = if (outgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("conversation.attachment.${attachment.id}")
            .clip(ConversationRichContentShape)
            .then(if (openAction != null) Modifier.clickable(onClick = openAction) else Modifier),
        shape = ConversationRichContentShape,
        color = container,
        contentColor = content,
    ) {
        Row(
            Modifier.padding(ConversationMessageMetrics.RichComponentInset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        ) {
            attachment.images.firstOrNull()?.let {
                ComposerImage(it, Modifier.size(40.dp).clip(CircleShape))
            } ?: run {
                Icon(
                    painter = painterResource(
                        if (attachment.kind == MessageAttachmentKind.File) {
                            R.drawable.ic_description
                        } else {
                            R.drawable.ic_person
                        },
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                SearchHighlightedText(
                    text = person?.displayName ?: attachment.label.removePrefix("Contact: "),
                    query = searchQuery,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val metadata = when {
                    person != null -> person.shortPublicKey
                    attachment.kind == MessageAttachmentKind.File -> fileMetadata(attachment)
                    else -> null
                }
                metadata?.let {
                    Text(
                        text = it,
                        color = content.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val trailingIcon = when {
                attachment.kind == MessageAttachmentKind.File && !attachment.isAvailable -> R.drawable.ic_warning
                openAction != null -> R.drawable.ic_chevron_right
                else -> null
            }
            trailingIcon?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = content.copy(alpha = 0.72f),
                )
            }
        }
    }
}

private fun fileMetadata(attachment: MessageAttachment): String {
    val fileType = attachment.label.substringAfterLast('.', missingDelimiterValue = "").uppercase()
    val bytes = attachment.fileSizeBytes ?: 0
    val size = when {
        bytes >= 1_000_000 -> "${bytes / 1_000_000} MB"
        bytes >= 1_000 -> "${bytes / 1_000} kB"
        else -> "$bytes B"
    }
    return if (fileType.isBlank()) size else "$fileType • $size"
}

@Composable
private fun VoiceMessageCard(
    attachment: MessageAttachment,
    outgoing: Boolean,
    transcript: String?,
    transcriptVisible: Boolean,
) {
    val duration = attachment.durationSeconds?.coerceAtLeast(1) ?: 8
    var isPlaying by remember(attachment.id) { mutableStateOf(false) }
    var progress by remember(attachment.id) { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (isPlaying && progress < 1f) {
            delay(100)
            progress = (progress + 0.1f / duration).coerceAtMost(1f)
        }
        if (progress >= 1f) {
            progress = 0f
            isPlaying = false
        }
    }
    val container = if (outgoing) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val content = if (outgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val secondaryContent = if (outgoing) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("conversation.attachment.${attachment.id}"),
        shape = ConversationRichContentShape,
        color = container,
        contentColor = content,
    ) {
        Column(
            Modifier.padding(ConversationMessageMetrics.RichComponentInset),
            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                FilledTonalIconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.testTag("conversation.voice.play.${attachment.id}"),
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                        ),
                        contentDescription = stringResource(
                            if (isPlaying) R.string.pause else R.string.play,
                        ),
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f),
                    color = if (outgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    trackColor = content.copy(alpha = 0.24f),
                    drawStopIndicator = {},
                )
                Text(
                    formatMessageDuration(
                        if (progress > 0f) (duration * (1f - progress)).roundToInt() else duration,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = secondaryContent,
                )
            }
            if (transcriptVisible && transcript != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = if (outgoing) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = content,
                ) {
                    Column(Modifier.padding(WhiteNoiseSpacing.Related)) {
                        Text(
                            stringResource(R.string.transcribed),
                            style = MaterialTheme.typography.labelMedium,
                            color = secondaryContent,
                        )
                        Text(transcript, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

internal fun formatMessageDuration(seconds: Int): String {
    val total = seconds.coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

@Stable
internal class ReadAloudController {
    var ready by mutableStateOf(false)
        private set
    var activeMessageId by mutableStateOf<String?>(null)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var activePassage by mutableStateOf<dev.ipf.whitenoise.model.MessagePassage?>(null)
        private set
    private var utteranceGeneration = 0L
    private var activeUtteranceId: String? = null

    private var engine: TextToSpeech? = null
    private var closed = true
    private val mainHandler = Handler(Looper.getMainLooper())

    fun initialize(context: Context) {
        shutdown()
        closed = false
        var created: TextToSpeech? = null
        created = TextToSpeech(context.applicationContext) { status ->
            val initialized = created
            if (closed || initialized == null || engine !== initialized) return@TextToSpeech
            val languageStatus = if (status == TextToSpeech.SUCCESS) {
                initialized.setLanguage(Locale.getDefault())
            } else {
                TextToSpeech.ERROR
            }
            postUpdate { ready = languageStatus >= TextToSpeech.LANG_AVAILABLE }
        }
        engine = created
        created.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = postUpdate {
                if (utteranceId == activeUtteranceId) progress = 0f
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                postUpdate {
                    if (utteranceId == activeUtteranceId) {
                        val spokenTextLength = utteranceId
                            ?.let(utteranceLengths::get)
                            ?.coerceAtLeast(1)
                            ?: 1
                        progress = (end.toFloat() / spokenTextLength).coerceIn(0f, 1f)
                    }
                }
            }

            override fun onDone(utteranceId: String?) = finish(utteranceId)

            @Deprecated("Platform callback")
            override fun onError(utteranceId: String?) = finish(utteranceId)
        })
    }

    private val utteranceLengths = mutableMapOf<String, Int>()

    fun toggle(messageId: String, text: String) {
        if (!ready) return
        if (activeMessageId == messageId) {
            stop()
            return
        }
        speak(messageId, text, null)
    }

    fun speakPassage(messageId: String, passage: dev.ipf.whitenoise.model.MessagePassage) {
        if (ready && passage.text.isNotBlank()) speak(messageId, passage.text, passage)
    }

    private fun speak(messageId: String, text: String, passage: dev.ipf.whitenoise.model.MessagePassage?) {
        val currentEngine = engine ?: return
        val utteranceId = "$messageId:${++utteranceGeneration}"
        activeUtteranceId?.let(utteranceLengths::remove)
        activeUtteranceId = utteranceId
        utteranceLengths[utteranceId] = text.length
        val result = currentEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.SUCCESS) {
            activeMessageId = messageId
            activePassage = passage
            progress = 0f
        } else {
            utteranceLengths.remove(utteranceId)
            activeUtteranceId = null
            activeMessageId = null
            activePassage = null
            progress = 0f
        }
    }

    fun stop() {
        engine?.stop()
        activeUtteranceId?.let(utteranceLengths::remove)
        activeUtteranceId = null
        activeMessageId = null
        activePassage = null
        progress = 0f
    }

    fun shutdown() {
        closed = true
        engine?.stop()
        engine?.shutdown()
        engine = null
        utteranceLengths.clear()
        ready = false
        activeMessageId = null
        activeUtteranceId = null
        activePassage = null
        progress = 0f
    }

    private fun finish(utteranceId: String?) = postUpdate {
        utteranceId?.let(utteranceLengths::remove)
        if (utteranceId == activeUtteranceId) {
            activeMessageId = null
            activeUtteranceId = null
            activePassage = null
            progress = 0f
        }
    }

    private fun postUpdate(update: () -> Unit) {
        mainHandler.post {
            if (!closed) update()
        }
    }
}

@Composable
internal fun rememberReadAloudController(): ReadAloudController {
    val context = LocalContext.current
    val controller = remember { ReadAloudController() }
    DisposableEffect(context, controller) {
        controller.initialize(context)
        onDispose {
            controller.shutdown()
        }
    }
    return controller
}

@Composable
internal fun ReadAloudProgress(messageId: String, controller: ReadAloudController) {
    if (controller.activeMessageId != messageId) return
    val progress = controller.progress
    val percentage = (progress * 100).toInt()
    val progressDescription = stringResource(R.string.reading_aloud_progress, percentage)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .testTag("conversation.readAloud.progress.$messageId")
            .semantics {
                contentDescription = progressDescription
                liveRegion = LiveRegionMode.Polite
            },
        horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_volume_up),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f),
            drawStopIndicator = {},
        )
    }
}

internal fun openContentUri(context: Context, value: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, value.toUri()).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }.onFailure { showAttachmentOpenFailure(context) }
}

private fun bundledResource(label: String): Triple<Int, String, String>? = when {
    label.contains("Project Brief", ignoreCase = true) ->
        Triple(R.raw.project_brief, "project-brief.pdf", "application/pdf")
    label.contains("Project Notes", ignoreCase = true) ->
        Triple(R.raw.project_notes, "project-notes.pdf", "application/pdf")
    label.contains("Trail Plan", ignoreCase = true) ->
        Triple(R.raw.trail_plan, "trail-plan.pdf", "application/pdf")
    label.contains("Weekend Notes", ignoreCase = true) ->
        Triple(R.raw.weekend_notes, "weekend-notes.pdf", "application/pdf")
    label.contains("Review Notes.docx", ignoreCase = true) ->
        Triple(R.raw.project_notes, "review-notes.pdf", "application/pdf")
    label.contains("Budget.xlsx", ignoreCase = true) ->
        Triple(R.raw.weekend_notes, "budget.pdf", "application/pdf")
    label.contains("Assets.zip", ignoreCase = true) ->
        Triple(R.raw.trail_plan, "assets.pdf", "application/pdf")
    label.contains("Read Me.txt", ignoreCase = true) ->
        Triple(R.raw.project_brief, "read-me.pdf", "application/pdf")
    label.contains("Conversation Outline.docx", ignoreCase = true) ->
        Triple(R.raw.project_brief, "conversation-outline.pdf", "application/pdf")
    label.contains("Launch Checklist.xlsx", ignoreCase = true) ->
        Triple(R.raw.project_notes, "launch-checklist.pdf", "application/pdf")
    label.contains("Reference Images.zip", ignoreCase = true) ->
        Triple(R.raw.trail_plan, "reference-images.pdf", "application/pdf")
    label.contains("Review Notes.txt", ignoreCase = true) ->
        Triple(R.raw.weekend_notes, "review-notes.pdf", "application/pdf")
    else -> null
}

internal fun openBundledResource(context: Context, resourceId: Int, fileName: String, mimeType: String) {
    runCatching {
        val directory = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(directory, fileName)
        if (!file.exists()) {
            context.resources.openRawResource(resourceId).use { input ->
                file.outputStream().use(input::copyTo)
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }.onFailure { showAttachmentOpenFailure(context) }
}

private fun showAttachmentOpenFailure(context: Context) {
    Toast.makeText(context, R.string.attachment_open_error, Toast.LENGTH_LONG).show()
}

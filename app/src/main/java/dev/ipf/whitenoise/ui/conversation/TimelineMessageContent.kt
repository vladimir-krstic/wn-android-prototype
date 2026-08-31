package dev.ipf.whitenoise.ui.conversation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.MediaLayout
import dev.ipf.whitenoise.model.MessageAttachment
import dev.ipf.whitenoise.model.MessageAttachmentKind
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.VoiceMessageFixture
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

private data class VisualFrame(
    val attachment: MessageAttachment,
    val image: ProfileAvatar?,
)

@Composable
internal fun TimelineAttachmentContent(
    attachments: List<MessageAttachment>,
    outgoing: Boolean,
    onOpenMedia: (List<MessageAttachment>) -> Unit,
    searchQuery: String = "",
) {
    if (attachments.isEmpty()) return
    val visualAttachments = attachments.filter(MessageAttachment::isVisual)
    if (visualAttachments.isNotEmpty()) {
        TimelineMediaGrid(
            attachments = visualAttachments,
            onClick = { onOpenMedia(visualAttachments) },
            searchQuery = searchQuery,
        )
    }
    attachments.filterNot(MessageAttachment::isVisual).forEach { attachment ->
        when (attachment.kind) {
            MessageAttachmentKind.Voice -> VoiceMessageCard(attachment, outgoing)
            MessageAttachmentKind.Link -> LinkMessageCard(attachment, outgoing, searchQuery)
            MessageAttachmentKind.File,
            MessageAttachmentKind.Contact,
            -> DocumentOrContactCard(attachment, outgoing, searchQuery)
            else -> Unit
        }
    }
}

@Composable
private fun TimelineMediaGrid(
    attachments: List<MessageAttachment>,
    onClick: () -> Unit,
    searchQuery: String,
) {
    val frames = attachments.flatMap { attachment ->
        if (attachment.images.isEmpty()) listOf(VisualFrame(attachment, null))
        else attachment.images.map { VisualFrame(attachment, it) }
    }
    val visible = frames.take(MediaLayout.visibleCount(frames.size))
    val overflow = MediaLayout.overflowCount(frames.size)
    val attachmentCountDescription = pluralStringResource(
        R.plurals.media_attachment_count,
        frames.size,
        frames.size,
    )
    // Material count layouts preserve reading order and share one viewer target.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .semantics { contentDescription = attachmentCountDescription }
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        when (MediaLayout.forCount(frames.size)) {
            dev.ipf.whitenoise.model.MediaGridLayout.Single -> MediaTile(
                visible.first(),
                Modifier.fillMaxWidth().height(220.dp),
                searchQuery = searchQuery,
            )
            dev.ipf.whitenoise.model.MediaGridLayout.Two -> Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                visible.forEach { MediaTile(it, Modifier.weight(1f).aspectRatio(1f), searchQuery = searchQuery) }
            }
            dev.ipf.whitenoise.model.MediaGridLayout.Three -> Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                MediaTile(visible[0], Modifier.weight(1f).height(210.dp), searchQuery = searchQuery)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MediaTile(visible[1], Modifier.fillMaxWidth().height(104.dp), searchQuery = searchQuery)
                    MediaTile(visible[2], Modifier.fillMaxWidth().height(104.dp), searchQuery = searchQuery)
                }
            }
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    visible.take(2).forEach {
                        MediaTile(it, Modifier.weight(1f).aspectRatio(1.35f), searchQuery = searchQuery)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    visible.drop(2).forEachIndexed { index, frame ->
                        MediaTile(
                            frame,
                            Modifier.weight(1f).aspectRatio(1f),
                            overflow = if (index == visible.drop(2).lastIndex) overflow else 0,
                            searchQuery = searchQuery,
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
) {
    Box(modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainer)) {
        frame.image?.let { ComposerImage(it, Modifier.fillMaxSize()) }
            ?: SearchHighlightedText(
                text = frame.attachment.label,
                query = searchQuery,
                modifier = Modifier.align(Alignment.Center).padding(8.dp),
            )
        if (frame.attachment.kind == MessageAttachmentKind.Video) {
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
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
    ) {
        Row(
            Modifier.padding(WhiteNoiseSpacing.Related),
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        ) {
            attachment.images.firstOrNull()?.let {
                ComposerImage(it, Modifier.size(56.dp).clip(MaterialTheme.shapes.small))
            }
            Column(Modifier.weight(1f)) {
                SearchHighlightedText(
                    text = attachment.linkTitle ?: attachment.label,
                    query = searchQuery,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                attachment.linkDomain?.let {
                    SearchHighlightedText(
                        text = it,
                        query = searchQuery,
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryContent,
                    )
                }
                attachment.linkSummary?.let {
                    Text(
                        it,
                        maxLines = 2,
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
) {
    val context = LocalContext.current
    val bundled = bundledResource(attachment.label)
    val canOpen = attachment.externalUri?.let {
        runCatching { it.toUri().scheme == "content" }.getOrDefault(false)
    } == true || bundled != null
    val container = if (outgoing) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val content = if (outgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
    ) {
        Row(
            Modifier.padding(WhiteNoiseSpacing.Related),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        ) {
            attachment.images.firstOrNull()?.let {
                ComposerImage(it, Modifier.size(48.dp).clip(CircleShape))
            } ?: Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (outgoing) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = content,
            ) {
                Icon(
                    painter = painterResource(
                        if (attachment.kind == MessageAttachmentKind.File) {
                            R.drawable.ic_description
                        } else {
                            R.drawable.ic_person
                        },
                    ),
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                )
            }
            SearchHighlightedText(
                text = attachment.label,
                query = searchQuery,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (canOpen) {
                TextButton(onClick = {
                    if (bundled != null) {
                        openBundledResource(context, bundled.first, bundled.second, bundled.third)
                    } else {
                        openContentUri(context, attachment.externalUri.orEmpty())
                    }
                }) {
                    Text(stringResource(R.string.open_attachment, attachment.label), color = content)
                }
            }
        }
    }
}

@Composable
private fun VoiceMessageCard(attachment: MessageAttachment, outgoing: Boolean) {
    val duration = attachment.durationSeconds?.coerceAtLeast(1) ?: 8
    var isPlaying by remember(attachment.id) { mutableStateOf(false) }
    var progress by remember(attachment.id) { mutableFloatStateOf(0f) }
    var transcriptVisible by remember(attachment.id) { mutableStateOf(false) }
    var localTranscript by remember(attachment.id) { mutableStateOf(attachment.transcript) }
    val context = LocalContext.current
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
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
    ) {
        Column(
            Modifier.padding(WhiteNoiseSpacing.Related),
            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                FilledTonalIconButton(onClick = { isPlaying = !isPlaying }) {
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
                )
                Text(
                    "0:${duration.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.labelMedium,
                    color = secondaryContent,
                )
            }
            val transcript = localTranscript
            if (transcript == null) {
                if (!outgoing) {
                    TextButton(
                        onClick = {
                            localTranscript = VoiceMessageFixture.transcript
                            transcriptVisible = true
                        },
                    ) {
                        Text(stringResource(R.string.transcribe), color = content)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { transcriptVisible = !transcriptVisible }) {
                        Text(
                            stringResource(
                                if (transcriptVisible) R.string.hide_transcript else R.string.show_transcript,
                            ),
                            color = content,
                        )
                    }
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Transcript", transcript))
                    }) { Text(stringResource(R.string.copy_transcript), color = content) }
                }
                if (transcriptVisible) {
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
}

@Stable
internal class ReadAloudController {
    var ready by mutableStateOf(false)
        private set
    var activeMessageId by mutableStateOf<String?>(null)
        private set
    var progress by mutableFloatStateOf(0f)
        private set

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
                if (utteranceId == activeMessageId) progress = 0f
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                postUpdate {
                    if (utteranceId == activeMessageId) {
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
        val currentEngine = engine ?: return
        utteranceLengths[messageId] = text.length
        val result = currentEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null, messageId)
        if (result == TextToSpeech.SUCCESS) {
            activeMessageId = messageId
            progress = 0f
        } else {
            utteranceLengths.remove(messageId)
            activeMessageId = null
            progress = 0f
        }
    }

    fun stop() {
        engine?.stop()
        activeMessageId?.let(utteranceLengths::remove)
        activeMessageId = null
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
        progress = 0f
    }

    private fun finish(utteranceId: String?) = postUpdate {
        utteranceId?.let(utteranceLengths::remove)
        if (utteranceId == activeMessageId) {
            activeMessageId = null
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
internal fun ReadAloudAction(messageId: String, text: String, controller: ReadAloudController) {
    val speaking = controller.activeMessageId == messageId
    val progress = if (speaking) controller.progress else 0f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(
            onClick = { controller.toggle(messageId, text) },
            enabled = controller.ready,
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_volume_up),
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp).size(18.dp),
            )
            Text(stringResource(if (speaking) R.string.stop_reading else R.string.read_aloud))
        }
        if (speaking) {
            val percentage = (progress * 100).toInt()
            val progressDescription = stringResource(R.string.reading_aloud_progress, percentage)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = progressDescription
                        liveRegion = LiveRegionMode.Polite
                    },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadOnlyMediaViewer(
    attachments: List<MessageAttachment>,
    onDismiss: () -> Unit,
    initialAttachmentIndex: Int = 0,
) {
    val visualAttachments = attachments.filter(MessageAttachment::isVisual)
    val frames = visualAttachments.flatMap { attachment ->
        if (attachment.images.isEmpty()) listOf(VisualFrame(attachment, null))
        else attachment.images.map { VisualFrame(attachment, it) }
    }
    if (frames.isEmpty()) return
    val safeAttachmentIndex = initialAttachmentIndex.coerceIn(0, visualAttachments.lastIndex)
    val initialPage = visualAttachments.take(safeAttachmentIndex).sumOf { attachment ->
        maxOf(attachment.images.size, 1)
    }
    val pagerState = rememberPagerState(initialPage = initialPage) { frames.size }
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.media_viewer)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.close),
                            )
                        }
                    },
                    actions = {
                        Text(
                            "${pagerState.currentPage + 1}/${frames.size}",
                            modifier = Modifier.padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { contentPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                pageSpacing = WhiteNoiseSpacing.Section,
            ) { index ->
                    val frame = frames[index]
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        frame.image?.let {
                            ComposerImage(
                                image = it,
                                modifier = Modifier.fillMaxSize().padding(WhiteNoiseSpacing.CompactScreenMargin),
                                contentScale = ContentScale.Fit,
                            )
                        }
                            ?: Text(frame.attachment.label)
                        if (frame.attachment.kind == MessageAttachmentKind.Video) {
                            Button(
                                onClick = {
                                    val uri = frame.attachment.externalUri
                                    if (uri?.startsWith("content:") == true) {
                                        openContentUri(context, uri)
                                    } else {
                                        openBundledResource(
                                            context,
                                            R.raw.chat_trail_clip,
                                            "chat-trail-clip.mp4",
                                            "video/mp4",
                                        )
                                    }
                                },
                                modifier = Modifier.align(Alignment.BottomCenter).padding(WhiteNoiseSpacing.Section),
                            ) { Text(stringResource(R.string.open_video)) }
                        }
                    }
                }
        }
    }
}

private fun openContentUri(context: Context, value: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, value.toUri()).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }
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
    else -> null
}

private fun openBundledResource(context: Context, resourceId: Int, fileName: String, mimeType: String) {
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
    }
}

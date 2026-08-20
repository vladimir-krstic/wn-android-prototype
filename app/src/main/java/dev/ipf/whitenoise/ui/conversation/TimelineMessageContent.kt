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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
) {
    if (attachments.isEmpty()) return
    val visualAttachments = attachments.filter(MessageAttachment::isVisual)
    if (visualAttachments.isNotEmpty()) {
        TimelineMediaGrid(
            attachments = visualAttachments,
            onClick = { onOpenMedia(visualAttachments) },
        )
    }
    attachments.filterNot(MessageAttachment::isVisual).forEach { attachment ->
        when (attachment.kind) {
            MessageAttachmentKind.Voice -> VoiceMessageCard(attachment, outgoing)
            MessageAttachmentKind.Link -> LinkMessageCard(attachment, outgoing)
            MessageAttachmentKind.File,
            MessageAttachmentKind.Contact,
            -> DocumentOrContactCard(attachment, outgoing)
            else -> Unit
        }
    }
}

@Composable
private fun TimelineMediaGrid(
    attachments: List<MessageAttachment>,
    onClick: () -> Unit,
) {
    val frames = attachments.flatMap { attachment ->
        if (attachment.images.isEmpty()) listOf(VisualFrame(attachment, null))
        else attachment.images.map { VisualFrame(attachment, it) }
    }
    val visible = frames.take(MediaLayout.visibleCount(frames.size))
    val overflow = MediaLayout.overflowCount(frames.size)
    // Material count layouts preserve reading order and share one viewer target.
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        when (MediaLayout.forCount(frames.size)) {
            dev.ipf.whitenoise.model.MediaGridLayout.Single -> MediaTile(visible.first(), Modifier.fillMaxWidth().height(220.dp))
            dev.ipf.whitenoise.model.MediaGridLayout.Two -> Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                visible.forEach { MediaTile(it, Modifier.weight(1f).aspectRatio(1f)) }
            }
            dev.ipf.whitenoise.model.MediaGridLayout.Three -> Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                MediaTile(visible[0], Modifier.weight(1f).height(210.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MediaTile(visible[1], Modifier.fillMaxWidth().height(104.dp))
                    MediaTile(visible[2], Modifier.fillMaxWidth().height(104.dp))
                }
            }
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    visible.take(2).forEach { MediaTile(it, Modifier.weight(1f).aspectRatio(1.35f)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    visible.drop(2).forEachIndexed { index, frame ->
                        MediaTile(
                            frame,
                            Modifier.weight(1f).aspectRatio(1f),
                            overflow = if (index == visible.drop(2).lastIndex) overflow else 0,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaTile(frame: VisualFrame, modifier: Modifier, overflow: Int = 0) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface)) {
        frame.image?.let { ComposerImage(it, Modifier.fillMaxSize()) }
            ?: Text(frame.attachment.label, Modifier.align(Alignment.Center).padding(8.dp))
        if (frame.attachment.kind == MessageAttachmentKind.Video) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
            ) { Text("▶", Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
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
private fun LinkMessageCard(attachment: MessageAttachment, outgoing: Boolean) {
    val container = if (outgoing) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = container,
    ) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            attachment.images.firstOrNull()?.let {
                ComposerImage(it, Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)))
            }
            Column(Modifier.weight(1f)) {
                Text(attachment.linkTitle ?: attachment.label, fontWeight = FontWeight.SemiBold)
                attachment.linkDomain?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                attachment.linkSummary?.let {
                    Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun DocumentOrContactCard(attachment: MessageAttachment, outgoing: Boolean) {
    val context = LocalContext.current
    val bundled = bundledResource(attachment.label)
    val canOpen = attachment.externalUri?.let {
        runCatching { it.toUri().scheme == "content" }.getOrDefault(false)
    } == true || bundled != null
    val container = if (outgoing) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = container,
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            attachment.images.firstOrNull()?.let {
                ComposerImage(it, Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)))
            } ?: Text(if (attachment.kind == MessageAttachmentKind.File) "▤" else "●")
            Text(attachment.label, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (canOpen) {
                TextButton(onClick = {
                    if (bundled != null) {
                        openBundledResource(context, bundled.first, bundled.second, bundled.third)
                    } else {
                        openContentUri(context, attachment.externalUri.orEmpty())
                    }
                }) {
                    Text(stringResource(R.string.open_attachment, attachment.label))
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
    val container = if (outgoing) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = container,
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { isPlaying = !isPlaying }) {
                    Text(stringResource(if (isPlaying) R.string.pause else R.string.play))
                }
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f))
                Text("0:${duration.toString().padStart(2, '0')}", style = MaterialTheme.typography.labelMedium)
            }
            attachment.transcript?.let { transcript ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { transcriptVisible = !transcriptVisible }) {
                        Text(stringResource(if (transcriptVisible) R.string.hide_transcript else R.string.show_transcript))
                    }
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Transcript", transcript))
                    }) { Text(stringResource(R.string.copy_transcript)) }
                }
                if (transcriptVisible) Text(transcript, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
internal fun ReadAloudAction(text: String) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    val engine = remember {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) tts?.language = Locale.getDefault()
        }
        tts
    }
    DisposableEffect(engine) {
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = updateSpeaking(true)
            override fun onDone(utteranceId: String?) = updateSpeaking(false)
            @Deprecated("Platform callback")
            override fun onError(utteranceId: String?) = updateSpeaking(false)
            private fun updateSpeaking(value: Boolean) {
                Handler(Looper.getMainLooper()).post { speaking = value }
            }
        })
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }
    TextButton(
        onClick = {
            if (speaking) engine.stop() else engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "white-noise-message")
            speaking = !speaking
        },
        enabled = ready,
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) { Text(stringResource(if (speaking) R.string.stop_reading else R.string.read_aloud)) }
}

@Composable
internal fun ReadOnlyMediaViewer(
    attachments: List<MessageAttachment>,
    onDismiss: () -> Unit,
) {
    val frames = attachments.filter(MessageAttachment::isVisual).flatMap { attachment ->
        if (attachment.images.isEmpty()) listOf(VisualFrame(attachment, null))
        else attachment.images.map { VisualFrame(attachment, it) }
    }
    if (frames.isEmpty()) return
    val pagerState = rememberPagerState { frames.size }
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(vertical = 24.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                    Text(stringResource(R.string.media_viewer), style = MaterialTheme.typography.titleLarge)
                    Text("${pagerState.currentPage + 1}/${frames.size}")
                }
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { index ->
                    val frame = frames[index]
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        frame.image?.let { ComposerImage(it, Modifier.fillMaxSize().padding(16.dp)) }
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
                                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                            ) { Text(stringResource(R.string.open_video)) }
                        }
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

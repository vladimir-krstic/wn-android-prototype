@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package dev.ipf.whitenoise.ui.conversation

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseEmptyState
import dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn as LazyColumn
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Stable
internal class LibraryAudioController(context: Context) {
    var state by mutableStateOf(LibraryAudioState())
        private set
    private val player = ExoPlayer.Builder(context.applicationContext).build().apply {
        setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),true)
        setHandleAudioBecomingNoisy(true)
    }
    private var closed = false
    init { player.addListener(object : Player.Listener { override fun onEvents(player: Player, events: Player.Events) = update() }) }
    fun begin(key: String): Long {
        player.stop(); player.clearMediaItems(); state = state.start(key); return state.revision
    }
    fun ready(revision: Long, uri: String): Boolean {
        if (closed || state.revision != revision || state.phase != LibraryAudioPhase.Loading) return false
        player.setMediaItem(MediaItem.Builder().setUri(uri).setMediaId("${state.key}:$revision").build())
        player.prepare(); player.playWhenReady = true; return true
    }
    fun fail(revision: Long) { state = state.failed(revision) }
    fun toggle() {
        if (closed) return
        if (player.isPlaying || player.playWhenReady) player.pause() else { if (player.playbackState == Player.STATE_ENDED) player.seekTo(0); player.play() }
        update()
    }
    fun seek(value: Long) { player.seekTo(value.coerceIn(0,state.durationMillis)); update() }
    fun pause() { if (!closed) { if (state.phase == LibraryAudioPhase.Loading && player.currentMediaItem == null) clear() else { player.pause(); update() } } }
    fun clear() { if (!closed) { player.stop(); player.clearMediaItems(); state = state.clear() } }
    fun update() {
        if (closed || player.currentMediaItem?.mediaId != "${state.key}:${state.revision}") return
        val phase = when {
            player.playerError != null -> LibraryAudioPhase.Failed
            player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_IDLE -> LibraryAudioPhase.Loading
            player.playbackState == Player.STATE_ENDED -> LibraryAudioPhase.Ended
            player.isPlaying -> LibraryAudioPhase.Playing
            else -> LibraryAudioPhase.Paused
        }
        state = state.observed(state.revision,player.currentPosition,player.duration.takeIf { it != C.TIME_UNSET } ?: 0,phase)
    }
    fun close() { closed = true; player.release(); state = state.clear() }
}

@Composable
internal fun SharedContentLibrary(content: List<SharedContentItem>, category: SharedContentCategory, media: List<ConversationMediaItem>, onOpenMedia: (ConversationMediaSelection) -> Unit, onGoToMessage: (String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val audio = remember { LibraryAudioController(context) }
    val scope = rememberCoroutineScope()
    val currentContent = rememberUpdatedState(content)
    var playingAttachment by remember { mutableStateOf<MessageAttachment?>(null) }
    var filter by rememberSaveable(category) { mutableStateOf(SharedMediaFilter.All) }
    val rows = remember(content,filter,category) { if (category == SharedContentCategory.Media) SharedContentProjection.filtered(content,filter) else content }
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.ROOT
    val zone = ZoneId.systemDefault()
    val sections = remember(rows,zone) { SharedContentProjection.months(rows,zone) }
    val monthFormat = remember(locale) { DateTimeFormatter.ofPattern("LLLL yyyy",locale) }
    val fileDialog = LocalAttachmentAccess.current.presented
    DisposableEffect(audio,owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) audio.pause() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); audio.close() }
    }
    LaunchedEffect(fileDialog,content) {
        if (fileDialog) audio.pause()
        if (audio.state.key != null && content.none { it.id == audio.state.key && it.attachment == playingAttachment && it.attachment.bytesAvailable }) audio.clear()
    }
    LaunchedEffect(audio.state.key,audio.state.phase) {
        while (audio.state.phase == LibraryAudioPhase.Playing) { delay(200); audio.update() }
    }
    fun play(item: SharedContentItem) {
        if (audio.state.key == item.id && audio.state.phase in setOf(LibraryAudioPhase.Playing,LibraryAudioPhase.Paused,LibraryAudioPhase.Ended)) { audio.toggle(); return }
        playingAttachment = item.attachment
        val revision = audio.begin(item.id)
        scope.launch {
            val file = exportAttachment(context,item.attachment,AttachmentExportKey(item.attachment.id))
            val valid = currentContent.value.any { it.id == item.id && it.attachment == item.attachment && it.attachment.bytesAvailable }
            if (!valid || !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) { file?.file?.delete(); if (audio.state.revision == revision) audio.clear(); return@launch }
            if (file == null) audio.fail(revision) else if (!audio.ready(revision,file.file.toUri().toString())) file.file.delete()
        }
    }
    Column(Modifier.fillMaxSize()) {
        if (category == SharedContentCategory.Media) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            SharedMediaFilter.entries.forEach { value -> FilterChip(filter == value,{ filter = value },label = { Text(stringResource(when (value) {
                SharedMediaFilter.All -> R.string.shared_filter_all; SharedMediaFilter.Images -> R.string.shared_filter_images; SharedMediaFilter.Videos -> R.string.shared_filter_videos
            })) },modifier = Modifier.testTag("shared.filter.${value.name}")) }
        }
        if (rows.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment = Alignment.Center) {
            WhiteNoiseEmptyState(stringResource(R.string.no_shared_content),stringResource(if (category == SharedContentCategory.Voice) R.string.shared_voice_empty else R.string.shared_content_empty_detail))
        } else if (category == SharedContentCategory.Media) LazyVerticalGrid(columns = GridCells.Adaptive(100.dp),modifier = Modifier.weight(1f).fillMaxWidth().testTag("shared.media.grid"),contentPadding = PaddingValues(4.dp),horizontalArrangement = Arrangement.spacedBy(4.dp),verticalArrangement = Arrangement.spacedBy(4.dp)) {
            sections.forEach { section ->
                item(key = "month-${section.key}",span = { GridItemSpan(maxLineSpan) }) { Text(section.key.format(monthFormat),Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin).semantics { heading() },style = MaterialTheme.typography.titleSmall) }
                items(section.items,key = { it.id }) { item ->
                    val available = media.any { it.key == item.mediaKey }
                    Box(Modifier.aspectRatio(1f).testTag("conversation.shared.media.${item.id}").clickable(role = Role.Button) {
                        if (available) onOpenMedia(ConversationMediaSelection(media,checkNotNull(item.mediaKey))) else onGoToMessage(item.messageId)
                    },contentAlignment = Alignment.BottomStart) {
                        if (item.attachment.kind == MessageAttachmentKind.Gif && item.attachment.bytesAvailable) AnimatedAttachmentImage(item.attachment,Modifier.fillMaxSize())
                        else item.attachment.images.firstOrNull()?.let { ComposerImage(it,Modifier.fillMaxSize()) }
                        Surface(Modifier.fillMaxWidth(),color = MaterialTheme.colorScheme.surface.copy(alpha = .86f)) {
                            Column(Modifier.padding(horizontal = 8.dp,vertical = 4.dp)) {
                                Text(item.senderName,style = MaterialTheme.typography.labelSmall,maxLines = 1,overflow = TextOverflow.Ellipsis)
                                if (!available) Text(stringResource(R.string.shared_media_unavailable),style = MaterialTheme.typography.labelSmall)
                                else if (item.attachment.kind == MessageAttachmentKind.Video) Text(stringResource(R.string.shared_filter_videos),style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        } else LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding = PaddingValues(bottom = WhiteNoiseSpacing.Section)) {
            sections.forEach { section ->
                item(key = "month-${section.key}") { Text(section.key.format(monthFormat),Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin).semantics { heading() },style = MaterialTheme.typography.titleSmall) }
                items(section.items,key = { it.id }) { item ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin,vertical = WhiteNoiseSpacing.Related)) {
                        if (category == SharedContentCategory.Voice) LibraryAudioRow(item,audio.state,{ play(item) },audio::seek)
                        else TimelineAttachmentContent(listOf(item.attachment),false,{},messageId = item.messageId)
                        Text("${item.senderName} · ${item.sentLabel}",style = MaterialTheme.typography.labelMedium,color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.attachment.kind == MessageAttachmentKind.File) Text(item.attachment.fileSizeBytes?.let { android.text.format.Formatter.formatShortFileSize(context,it.toLong()) } ?: stringResource(R.string.shared_file_size_unknown),style = MaterialTheme.typography.labelSmall)
                        TextButton({ onGoToMessage(item.messageId) },modifier = Modifier.testTag("shared.message.${item.id}")) { Text(stringResource(R.string.go_to_message)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryAudioRow(item: SharedContentItem, state: LibraryAudioState, onPlay: () -> Unit, onSeek: (Long) -> Unit) {
    val selected = state.key == item.id
    val loading = selected && state.phase == LibraryAudioPhase.Loading
    val failed = selected && state.phase == LibraryAudioPhase.Failed
    var seek by remember(item.id) { mutableStateOf<Float?>(null) }
    Surface(shape = MaterialTheme.shapes.large,color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.Related)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onPlay,enabled = !loading,modifier = Modifier.testTag("shared.voice.play.${item.id}")) {
                    if (loading) CircularProgressIndicator(Modifier.size(24.dp)) else Icon(painterResource(if (selected && state.phase == LibraryAudioPhase.Playing) R.drawable.ic_pause else R.drawable.ic_play_arrow),stringResource(if (selected && state.phase == LibraryAudioPhase.Playing) R.string.pause else R.string.play))
                }
                Text(item.attachment.label,Modifier.weight(1f),style = MaterialTheme.typography.titleSmall)
                val duration = if (selected && state.durationMillis > 0) (state.durationMillis/1000).toInt() else item.attachment.durationSeconds
                duration?.let { Text(formatMessageDuration(it),style = MaterialTheme.typography.labelSmall) }
            }
            if (loading) Text(stringResource(R.string.shared_voice_loading),style = MaterialTheme.typography.labelSmall)
            if (failed) Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.shared_voice_error),Modifier.weight(1f),color = MaterialTheme.colorScheme.error)
                TextButton(onPlay) { Text(stringResource(R.string.attachment_retry)) }
            }
            if (selected && state.durationMillis > 0 && !failed) {
                val description = stringResource(R.string.shared_voice_seek)
                Slider(value = seek ?: state.positionMillis.toFloat(),onValueChange = { seek = it },onValueChangeFinished = { seek?.let { onSeek(it.toLong()) }; seek = null },
                    valueRange = 0f..state.durationMillis.toFloat(),modifier = Modifier.semantics { contentDescription = description }.testTag("shared.voice.seek.${item.id}"),
                    track = { SliderDefaults.Track(it,drawStopIndicator = {}) })
            }
        }
    }
}

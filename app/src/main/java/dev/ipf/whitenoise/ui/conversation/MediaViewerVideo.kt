@file:androidx.annotation.OptIn(
    androidx.media3.common.util.UnstableApi::class,
    androidx.media3.common.util.ExperimentalApi::class,
)

package dev.ipf.whitenoise.ui.conversation

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.material3.Player as VideoPlayer
import androidx.media3.ui.compose.material3.PlayerDefaults
import androidx.media3.ui.compose.material3.buttons.MuteButton
import androidx.media3.ui.compose.material3.buttons.PlayPauseButton
import androidx.media3.ui.compose.material3.buttons.PlaybackSpeedToggleButton
import androidx.media3.ui.compose.material3.buttons.SeekBackButton
import androidx.media3.ui.compose.material3.buttons.SeekForwardButton
import androidx.media3.ui.compose.state.rememberProgressStateWithTickCount
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.ConversationMediaItem
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

/** One foreground video at a time; offscreen pager pages only display their poster. */
@Composable
internal fun MediaViewerVideo(
    item: ConversationMediaItem,
    active: Boolean,
    controlsVisible: Boolean,
    bottomControlsInset: Dp,
    onToggleControls: () -> Unit,
    onShowControls: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val player = rememberForegroundVideoPlayer(item, active)
    val positionLabel = stringResource(R.string.video_playback_position)
    val speedLabel = stringResource(R.string.video_playback_speed)
    val showControls by rememberUpdatedState(onShowControls)
    val currentPageActive by rememberUpdatedState(active)
    var buffering by remember(player) { mutableStateOf(false) }
    var playing by remember(player) { mutableStateOf(false) }
    DisposableEffect(player) {
        fun update() {
            buffering = player?.playbackState == Player.STATE_BUFFERING
            playing = player?.isPlaying == true
            if (currentPageActive && player != null &&
                (!player.playWhenReady || player.playbackState == Player.STATE_ENDED ||
                    player.playerError != null)
            ) {
                showControls()
            }
        }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = update()
        }
        player?.addListener(listener)
        update()
        onDispose { player?.removeListener(listener) }
    }

    val view = LocalView.current
    DisposableEffect(view, playing) {
        val previous = view.keepScreenOn
        if (playing) view.keepScreenOn = true
        onDispose { if (playing) view.keepScreenOn = previous }
    }

    BoxWithConstraints(modifier) {
        if (player == null) {
            item.image?.let {
                ComposerImage(it, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            return@BoxWithConstraints
        }
        val compactHeight = maxHeight < 480.dp
        VideoPlayer(
            player = player,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onToggleControls) { detectTapGestures(onTap = { onToggleControls() }) }
                .testTag("conversation.media.viewer.video"),
            contentScale = ContentScale.Fit,
            // SurfaceView animations synchronize with the UI from API 24 onward.
            surfaceType = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                SURFACE_TYPE_TEXTURE_VIEW
            } else {
                SURFACE_TYPE_SURFACE_VIEW
            },
            shutter = {
                item.image?.let {
                    ComposerImage(it, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
            },
            showControls = controlsVisible && active,
            topControls = null,
            centerControls = { controlledPlayer, visible ->
                if (!compactHeight) {
                    PlayerDefaults.CenterControls(
                        player = controlledPlayer,
                        visible = visible,
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                        // Gallery paging already owns previous/next media navigation.
                        backSecondary = {},
                        forwardSecondary = {},
                    )
                }
            },
            bottomControls = { controlledPlayer, visible ->
                PlayerDefaults.BottomControls(
                    player = controlledPlayer,
                    visible = visible,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = bottomControlsInset)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .testTag("conversation.media.viewer.video.controls"),
                    above = {
                        if (buffering) {
                            LinearProgressIndicator(
                                Modifier.fillMaxWidth().testTag("conversation.media.viewer.video.buffering"),
                            )
                        }
                    },
                    progressSlider = {
                        VideoPlaybackSlider(
                            controlledPlayer,
                            Modifier
                                .testTag("conversation.media.viewer.video.seek")
                                .semantics { contentDescription = positionLabel },
                        )
                    },
                    below = {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                if (compactHeight) 0.dp else WhiteNoiseSpacing.Related,
                                Alignment.End,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (compactHeight) {
                                SeekBackButton(controlledPlayer)
                                PlayPauseButton(controlledPlayer)
                                SeekForwardButton(controlledPlayer)
                                Spacer(Modifier.weight(1f))
                            }
                            PlaybackSpeedToggleButton(
                                controlledPlayer,
                                Modifier.semantics { contentDescription = speedLabel },
                            )
                            MuteButton(controlledPlayer)
                        }
                    },
                )
            },
        )
    }
}

/** Media3 owns progress and seeking; Material owns the slider with its end marker omitted. */
@Composable
private fun VideoPlaybackSlider(player: Player?, modifier: Modifier = Modifier) {
    var widthPx by remember { mutableIntStateOf(0) }
    val progress = rememberProgressStateWithTickCount(player, totalTickCount = widthPx)
    var scrubPosition by remember(player) { mutableStateOf<Float?>(null) }
    Slider(
        value = scrubPosition ?: progress.currentPositionProgress,
        onValueChange = { scrubPosition = it },
        onValueChangeFinished = {
            scrubPosition?.let(progress::updateCurrentPositionProgress)
            scrubPosition = null
        },
        modifier = modifier.onSizeChanged { widthPx = it.width },
        enabled = progress.changingProgressEnabled,
        track = { state ->
            SliderDefaults.Track(
                sliderState = state,
                enabled = progress.changingProgressEnabled,
                drawStopIndicator = null,
            )
        },
    )
}

@Composable
internal fun rememberForegroundVideoPlayer(item: ConversationMediaItem, active: Boolean): ExoPlayer? {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val key = item.key.stableId
    var position by rememberSaveable(key) { mutableLongStateOf(0L) }
    var volume by rememberSaveable(key) { mutableFloatStateOf(1f) }
    var speed by rememberSaveable(key) { mutableFloatStateOf(1f) }
    var player by remember(key) { mutableStateOf<ExoPlayer?>(null) }
    val externalUri = item.attachment.externalUri

    DisposableEffect(context, lifecycle, key, externalUri, active) {
        fun release() {
            player?.let {
                position = it.currentPosition.coerceAtLeast(0L)
                volume = it.volume
                speed = it.playbackParameters.speed
                player = null
                it.release()
            }
        }

        fun update() {
            if (active && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                if (player == null) {
                    // Device-owned content URIs and the existing bundled clip only.
                    // An invalid URI fails in the player instead of opening another app.
                    val uri = when {
                        externalUri == null ->
                            "android.resource://${context.packageName}/${R.raw.chat_trail_clip}".toUri()
                        externalUri.startsWith("content:") -> externalUri.toUri()
                        else -> Uri.EMPTY
                    }
                    player = ExoPlayer.Builder(context)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(C.USAGE_MEDIA)
                                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                                .build(),
                            true,
                        )
                        .setHandleAudioBecomingNoisy(true)
                        .build()
                        .apply {
                            setMediaItem(MediaItem.fromUri(uri), position)
                            this.volume = volume
                            setPlaybackSpeed(speed)
                            playWhenReady = false
                            prepare()
                        }
                }
            } else {
                release()
            }
        }

        val observer = LifecycleEventObserver { _, _ -> update() }
        lifecycle.addObserver(observer)
        update()
        onDispose {
            lifecycle.removeObserver(observer)
            release()
        }
    }
    return player
}

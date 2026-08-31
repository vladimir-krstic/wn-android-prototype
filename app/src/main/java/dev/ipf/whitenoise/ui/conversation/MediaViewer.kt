package dev.ipf.whitenoise.ui.conversation

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.ConversationMediaItem
import dev.ipf.whitenoise.model.ConversationMediaSelection
import dev.ipf.whitenoise.model.MessageAttachmentKind
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import dev.ipf.whitenoise.ui.components.drawableResource
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import java.io.File
import java.io.OutputStream
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ReadOnlyMediaViewer(
    selection: ConversationMediaSelection,
    onDismiss: () -> Unit,
    onForward: (ConversationMediaItem) -> Unit,
    onGoToMessage: (ConversationMediaItem) -> Unit,
) {
    if (selection.items.isEmpty()) return
    val pagerState = rememberPagerState(initialPage = selection.initialIndex) { selection.items.size }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val touchExplorationEnabled = remember(context) {
        context.getSystemService(AccessibilityManager::class.java)?.isTouchExplorationEnabled == true
    }
    var chromeVisible by remember { mutableStateOf(true) }
    var moreExpanded by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var pendingSave by remember { mutableStateOf<ConversationMediaItem?>(null) }

    fun resetZoom() {
        scale = 1f
        offset = Offset.Zero
    }

    fun saveTo(uri: android.net.Uri?) {
        val item = pendingSave
        pendingSave = null
        if (uri == null || item == null) return
        coroutineScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        copyMedia(context, item, output)
                    } ?: error("No output stream")
                }.isSuccess
            }
            Toast.makeText(
                context,
                if (saved) R.string.media_saved else R.string.media_save_error,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    val imageSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg"),
        ::saveTo,
    )
    val videoSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("video/mp4"),
        ::saveTo,
    )

    LaunchedEffect(pagerState.currentPage) {
        resetZoom()
        moreExpanded = false
    }

    fun dismissOrReset() {
        if (scale > 1.01f) resetZoom() else onDismiss()
    }
    BackHandler(onBack = ::dismissOrReset)

    Dialog(
        onDismissRequest = ::dismissOrReset,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val currentItem = selection.items[pagerState.currentPage]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().testTag("conversation.media.viewer.pager"),
                pageSpacing = WhiteNoiseSpacing.Section,
                userScrollEnabled = scale <= 1.01f,
            ) { page ->
                val item = selection.items[page]
                val isCurrentPage = pagerState.currentPage == page
                val stillImage = item.attachment.kind != MessageAttachmentKind.Video
                val zoomInLabel = stringResource(R.string.zoom_in)
                val zoomOutLabel = stringResource(R.string.zoom_out)
                val resetZoomLabel = stringResource(R.string.reset_zoom)
                val zoomState = stringResource(R.string.zoom_level, (scale * 100).roundToInt())
                val canToggleChrome = !touchExplorationEnabled
                val fittedContentSize = remember(item.image, viewportSize) {
                    fittedMediaContentSize(context, item.image, viewportSize)
                }
                val transformableState = rememberTransformableState { zoom, pan, _ ->
                    val nextScale = (scale * zoom).coerceIn(1f, 4f)
                    if (nextScale <= 1.01f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = nextScale
                        offset = clampMediaOffset(
                            offset + pan,
                            nextScale,
                            viewportSize,
                            fittedContentSize,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { if (isCurrentPage) viewportSize = it }
                        .pointerInput(item.key, canToggleChrome) {
                            detectTapGestures(
                                onTap = {
                                    if (canToggleChrome) chromeVisible = !chromeVisible
                                },
                                onDoubleTap = {
                                    if (stillImage && isCurrentPage) {
                                        scale = if (scale > 1.01f) 1f else 2f
                                        offset = Offset.Zero
                                    }
                                },
                            )
                        }
                        .then(
                            if (stillImage && isCurrentPage) {
                                Modifier.transformable(
                                    state = transformableState,
                                    canPan = { scale > 1.01f },
                                    lockRotationOnZoomPan = true,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .testTag("conversation.media.viewer.page.$page")
                        .semantics {
                            contentDescription = item.attachment.label
                            if (stillImage && isCurrentPage) {
                                stateDescription = zoomState
                                customActions = listOf(
                                    CustomAccessibilityAction(zoomInLabel) {
                                        scale = (scale + 0.5f).coerceAtMost(4f)
                                        offset = clampMediaOffset(
                                            offset,
                                            scale,
                                            viewportSize,
                                            fittedContentSize,
                                        )
                                        true
                                    },
                                    CustomAccessibilityAction(zoomOutLabel) {
                                        scale = (scale - 0.5f).coerceAtLeast(1f)
                                        if (scale <= 1.01f) offset = Offset.Zero
                                        else offset = clampMediaOffset(
                                            offset,
                                            scale,
                                            viewportSize,
                                            fittedContentSize,
                                        )
                                        true
                                    },
                                    CustomAccessibilityAction(resetZoomLabel) {
                                        resetZoom()
                                        true
                                    },
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    item.image?.let { image ->
                        ComposerImage(
                            image = image,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    if (isCurrentPage && stillImage) {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offset.x
                                        translationY = offset.y
                                    }
                                },
                            contentScale = ContentScale.Fit,
                        )
                    } ?: Text(item.attachment.label)
                    if (item.attachment.kind == MessageAttachmentKind.Video) {
                        Button(
                            onClick = { openVideo(context, item) },
                            modifier = Modifier.align(Alignment.Center),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_play_arrow),
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.open_video))
                        }
                    }
                }
            }

            if (chromeVisible) {
                MediaViewerTopChrome(
                    item = currentItem,
                    position = pagerState.currentPage + 1,
                    count = selection.items.size,
                    moreExpanded = moreExpanded,
                    onMoreExpandedChange = { moreExpanded = it },
                    onClose = onDismiss,
                    onSave = {
                        pendingSave = currentItem
                        if (currentItem.attachment.kind == MessageAttachmentKind.Video) {
                            videoSaveLauncher.launch(currentItem.suggestedFileName)
                        } else {
                            imageSaveLauncher.launch(currentItem.suggestedFileName)
                        }
                    },
                    onGoToMessage = { onGoToMessage(currentItem) },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                MediaViewerBottomChrome(
                    onShare = {
                        coroutineScope.launch {
                            val shared = shareMedia(context, currentItem)
                            if (!shared) {
                                Toast.makeText(context, R.string.media_share_error, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onForward = { onForward(currentItem) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun MediaViewerTopChrome(
    item: ConversationMediaItem,
    position: Int,
    count: Int,
    moreExpanded: Boolean,
    onMoreExpandedChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onGoToMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close),
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                    item.senderName,
                    modifier = Modifier.testTag("conversation.media.viewer.sender"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.media_viewer_metadata, item.sentLabel, position, count),
                    modifier = Modifier.testTag("conversation.media.viewer.position"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { onMoreExpandedChange(true) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.more_options),
                    )
                }
                WhiteNoiseDropdownMenu(
                    expanded = moreExpanded,
                    onDismissRequest = { onMoreExpandedChange(false) },
                    items = listOf(
                        WhiteNoiseMenuItem(
                            label = stringResource(R.string.save),
                            icon = R.drawable.ic_download,
                            onClick = onSave,
                        ),
                        WhiteNoiseMenuItem(
                            label = stringResource(R.string.go_to_message),
                            icon = R.drawable.ic_settings_chat_bubble_outline,
                            onClick = onGoToMessage,
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun MediaViewerBottomChrome(
    onShare: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onShare,
                modifier = Modifier.testTag("conversation.media.viewer.share"),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = stringResource(R.string.share),
                )
            }
            IconButton(
                onClick = onForward,
                modifier = Modifier.testTag("conversation.media.viewer.forward"),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_forward),
                    contentDescription = stringResource(R.string.forward),
                )
            }
        }
    }
}

private fun clampMediaOffset(
    offset: Offset,
    scale: Float,
    viewportSize: IntSize,
    contentSize: IntSize,
): Offset {
    if (scale <= 1f || viewportSize == IntSize.Zero || contentSize == IntSize.Zero) return Offset.Zero
    val maxX = ((contentSize.width * scale - viewportSize.width) / 2f).coerceAtLeast(0f)
    val maxY = ((contentSize.height * scale - viewportSize.height) / 2f).coerceAtLeast(0f)
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY),
    )
}

private fun fittedMediaContentSize(
    context: Context,
    image: ProfileAvatar?,
    viewportSize: IntSize,
): IntSize {
    if (viewportSize == IntSize.Zero) return IntSize.Zero
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    when (image) {
        is ProfileAvatar.DeviceImage -> BitmapFactory.decodeByteArray(
            image.bytes,
            0,
            image.bytes.size,
            options,
        )
        is ProfileAvatar.Asset -> BitmapFactory.decodeResource(
            context.resources,
            image.asset.drawableResource,
            options,
        )
        is ProfileAvatar.WebImage -> BitmapFactory.decodeResource(
            context.resources,
            image.asset.drawableResource,
            options,
        )
        ProfileAvatar.Monogram,
        null,
        -> return viewportSize
    }
    if (options.outWidth <= 0 || options.outHeight <= 0) return viewportSize
    val fit = min(
        viewportSize.width.toFloat() / options.outWidth,
        viewportSize.height.toFloat() / options.outHeight,
    )
    return IntSize(
        width = (options.outWidth * fit).roundToInt(),
        height = (options.outHeight * fit).roundToInt(),
    )
}

private fun openVideo(context: Context, item: ConversationMediaItem) {
    val uri = item.attachment.externalUri
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
}

private suspend fun shareMedia(context: Context, item: ConversationMediaItem): Boolean {
    val uri = withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(directory, item.suggestedFileName)
            file.outputStream().use { copyMedia(context, item, it) }
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        }.getOrNull()
    } ?: return false
    return runCatching {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, item.attachment.label, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_media)))
    }.isSuccess
}

private fun copyMedia(context: Context, item: ConversationMediaItem, output: OutputStream) {
    if (item.attachment.kind == MessageAttachmentKind.Video) {
        val external = item.attachment.externalUri
        val input = if (external?.startsWith("content:") == true) {
            context.contentResolver.openInputStream(android.net.Uri.parse(external))
        } else {
            context.resources.openRawResource(R.raw.chat_trail_clip)
        } ?: error("Media is unavailable")
        input.use { it.copyTo(output) }
        return
    }
    val bitmap = when (val image = item.image) {
        is ProfileAvatar.DeviceImage -> BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)
        is ProfileAvatar.Asset -> BitmapFactory.decodeResource(context.resources, image.asset.drawableResource)
        is ProfileAvatar.WebImage -> BitmapFactory.decodeResource(context.resources, image.asset.drawableResource)
        ProfileAvatar.Monogram,
        null,
        -> null
    } ?: error("Media is unavailable")
    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output))
}

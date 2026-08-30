package dev.ipf.whitenoise.ui.conversation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet as ModalBottomSheet
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.AvatarAsset
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatTimelineEntry
import dev.ipf.whitenoise.model.ComposerAttachmentSizing
import dev.ipf.whitenoise.model.ComposerExpansionPolicy
import dev.ipf.whitenoise.model.ComposerVoiceReducer
import dev.ipf.whitenoise.model.ComposerVoiceState
import dev.ipf.whitenoise.model.ComposerWaveformPolicy
import dev.ipf.whitenoise.model.LinkPreviewDetector
import dev.ipf.whitenoise.model.MessageAttachment
import dev.ipf.whitenoise.model.MessageAttachmentKind
import dev.ipf.whitenoise.model.Person
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.VoiceDraftSubmission
import dev.ipf.whitenoise.model.VoiceMessageFixture
import dev.ipf.whitenoise.model.VoiceMessageFormat
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuPlacement
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import dev.ipf.whitenoise.ui.components.drawableResource
import dev.ipf.whitenoise.ui.onboarding.AvatarImageProcessor
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

private val ComposerVoiceStateSaver: Saver<ComposerVoiceState, Any> = listSaver(
    save = { state ->
        when (state) {
            ComposerVoiceState.Idle -> listOf("idle")
            is ComposerVoiceState.Recording -> listOf(
                "review",
                ((state.elapsedTenths + 9) / 10).coerceAtLeast(1),
                false,
                "",
                VoiceMessageFormat.Voice.name,
                0,
            )
            is ComposerVoiceState.Review -> listOf(
                "review",
                state.durationSeconds,
                state.transcript != null,
                state.transcript.orEmpty(),
                state.format.name,
                state.playbackTenths,
            )
        }
    },
    restore = { values ->
        if (values.firstOrNull() == "review") {
            ComposerVoiceReducer.restore(ComposerVoiceState.Review(
                durationSeconds = values[1] as Int,
                transcript = (values[3] as String).takeIf { values[2] as Boolean },
                format = VoiceMessageFormat.valueOf(values[4] as String),
                playbackTenths = values[5] as Int,
            ))
        } else {
            ComposerVoiceState.Idle
        }
    },
)

private class MentionVisualTransformation(
    mentionNames: List<String>,
    private val background: Color,
) : VisualTransformation {
    private val regex = mentionNames
        .filter(String::isNotBlank)
        .sortedByDescending(String::length)
        .takeIf(List<String>::isNotEmpty)
        ?.joinToString("|") { Regex.escape(it) }
        ?.let { Regex("(?<!\\S)@(?:$it)(?=\\s|$)", RegexOption.IGNORE_CASE) }

    override fun filter(text: AnnotatedString): TransformedText {
        val styled = AnnotatedString.Builder(text)
        regex?.findAll(text.text)?.forEach { match ->
            styled.addStyle(
                SpanStyle(background = background),
                match.range.first,
                match.range.last + 1,
            )
        }
        return TransformedText(styled.toAnnotatedString(), OffsetMapping.Identity)
    }
}

private fun Modifier.composerExpansionGesture(
    enabled: Boolean,
    onStart: () -> Float,
    onDrag: (startProgress: Float, translationY: Float) -> Unit,
    onEnd: (velocityY: Float) -> Unit,
    onCancel: () -> Unit,
): Modifier = if (!enabled) {
    this
} else {
    pointerInput(enabled) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val pointerId: PointerId = down.id
            val origin = down.position
            val velocityTracker = VelocityTracker().apply { addPosition(down.uptimeMillis, origin) }
            var verticalGesture = false
            var horizontalGesture = false
            var startProgress = 0f
            var completed = false
            while (!completed) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == pointerId }
                if (change == null) {
                    if (verticalGesture) onCancel()
                    completed = true
                    continue
                }
                velocityTracker.addPosition(change.uptimeMillis, change.position)
                val translation = change.position - origin
                if (!verticalGesture && !horizontalGesture && translation.getDistance() >= viewConfiguration.touchSlop) {
                    if (abs(translation.y) > abs(translation.x)) {
                        verticalGesture = true
                        startProgress = onStart()
                    } else {
                        horizontalGesture = true
                    }
                }
                if (verticalGesture) {
                    change.consume()
                    onDrag(startProgress, translation.y)
                }
                if (!change.pressed) {
                    if (verticalGesture) onEnd(velocityTracker.calculateVelocity().y)
                    completed = true
                }
            }
        }
    }
}

private fun Modifier.voiceLongPressGesture(
    enabled: Boolean,
    onLongPress: () -> Unit,
): Modifier = if (!enabled) {
    this
} else {
    pointerInput(enabled) {
        coroutineScope outer@{
            val movementTolerance = 32.dp.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var eligible = true
                var longPressed = false
                val longPressJob = this@outer.launch {
                    delay(400)
                    if (eligible) {
                        longPressed = true
                        onLongPress()
                    }
                }
                var pressed = true
                while (pressed) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    pressed = change?.pressed == true
                    if (change == null || (change.position - down.position).getDistance() > movementTolerance) {
                        eligible = false
                        longPressJob.cancel()
                    }
                    if (longPressed) change?.consume()
                }
                eligible = false
                longPressJob.cancel()
            }
        }
    }
}

/** Keeps the app-shell outside-tap observer from clearing a focus acquired by this editor tap. */
private fun Modifier.consumeEditorTapAtFinalPass(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final).consume()
        waitForUpOrCancellation(pass = PointerEventPass.Final)?.consume()
    }
}

@Composable
private fun ComposerLeadingAction(
    voiceState: ComposerVoiceState,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    addAttachmentDescription: String,
    enabled: Boolean,
    menuItems: List<WhiteNoiseMenuItem>,
    onCancelVoice: () -> Unit,
) {
    val isAddAction = voiceState == ComposerVoiceState.Idle
    val containerColor = when {
        !isAddAction -> MaterialTheme.colorScheme.surfaceContainerHigh
        enabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }
    val contentColor = when {
        !isAddAction -> MaterialTheme.colorScheme.onSurface
        enabled -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    Box(Modifier.size(48.dp).testTag("conversation.attachment.add")) {
        Surface(
            modifier = Modifier.fillMaxSize().testTag("conversation.attachment.add.surface"),
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor,
        ) {
            IconButton(
                onClick = {
                    if (voiceState is ComposerVoiceState.Review) {
                        onCancelVoice()
                    } else {
                        onMenuExpandedChange(true)
                    }
                },
                enabled = enabled,
            ) {
                Icon(
                    painter = painterResource(
                        if (voiceState is ComposerVoiceState.Review) R.drawable.ic_close else R.drawable.ic_add,
                    ),
                    contentDescription = if (voiceState is ComposerVoiceState.Review) {
                        stringResource(R.string.cancel)
                    } else {
                        addAttachmentDescription
                    },
                    tint = contentColor,
                )
            }
        }
        if (voiceState == ComposerVoiceState.Idle) {
            WhiteNoiseDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onMenuExpandedChange(false) },
                items = menuItems,
                modifier = Modifier.testTag("conversation.attachment.menu"),
                anchorSpacing = 10.dp,
                placement = WhiteNoiseMenuPlacement.AboveAnchor,
            )
        }
    }
}

@Composable
private fun ComposerSendButton(
    onClick: () -> Unit,
    enabled: Boolean,
    description: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(48.dp), contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (enabled) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_upward),
                        contentDescription = description,
                        modifier = Modifier.size(20.dp).testTag("conversation.send.icon"),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniWaveformGlyph(color: Color, modifier: Modifier = Modifier) {
    val heights = listOf(6.dp, 12.dp, 18.dp, 12.dp, 6.dp)
    Row(
        modifier = modifier.size(24.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        heights.forEach { height ->
            Box(Modifier.width(2.dp).height(height).background(color, CircleShape))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullConversationComposer(
    profile: Profile,
    chat: Chat,
    onDraftTextChanged: (String) -> Unit,
    onAddAttachments: (List<MessageAttachment>) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSuppressLink: (String?) -> Unit,
    onCancelReply: () -> Unit,
    onSendDraft: () -> Boolean,
    onSendVoice: (VoiceDraftSubmission) -> Boolean,
    modifier: Modifier = Modifier,
    onCompactHeightChanged: (Int) -> Unit = {},
    onExpansionPresentationChanged: (Boolean, Float) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val addAttachmentDescription = stringResource(R.string.add_attachment)
    val recordVoiceDescription = stringResource(R.string.record_voice_message)
    val expandMessageLabel = stringResource(R.string.expand_message)
    val collapseMessageLabel = stringResource(R.string.collapse_message)
    val hideKeyboardLabel = stringResource(R.string.hide_keyboard)
    val coroutineScope = rememberCoroutineScope()
    val messageFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    var attachmentMenuOpen by remember { mutableStateOf(false) }
    var contactPickerOpen by remember { mutableStateOf(false) }
    var mediaViewerAttachmentId by remember { mutableStateOf<String?>(null) }
    var isExpanded by rememberSaveable(chat.id) { mutableStateOf(false) }
    val expansionProgress = remember(chat.id) { Animatable(if (isExpanded) 1f else 0f) }
    var isDraggingExpansion by remember { mutableStateOf(false) }
    var isSettlingExpansion by remember { mutableStateOf(false) }
    var compactHeightPx by remember(chat.id) { mutableIntStateOf(0) }
    var isPreparing by remember { mutableStateOf(false) }
    var attachmentError by remember { mutableStateOf(false) }
    var preparationJob by remember { mutableStateOf<Job?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var attachmentCounter by rememberSaveable(chat.id) { mutableIntStateOf(0) }
    var voiceState by rememberSaveable(chat.id, stateSaver = ComposerVoiceStateSaver) {
        mutableStateOf<ComposerVoiceState>(ComposerVoiceState.Idle)
    }

    fun nextId(prefix: String): String {
        attachmentCounter += 1
        return "${chat.id}-$prefix-$attachmentCounter"
    }

    fun prepareVisualUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        preparationJob?.cancel()
        preparationJob = coroutineScope.launch {
            isPreparing = true
            attachmentError = false
            val prepared = mutableListOf<MessageAttachment>()
            for (uri in uris) {
                val type = context.contentResolver.getType(uri).orEmpty()
                if (type.startsWith("video/")) {
                    prepared += MessageAttachment(
                        id = nextId("video"),
                        kind = MessageAttachmentKind.Video,
                        label = displayName(context, uri, "Video"),
                        images = listOf(ProfileAvatar.Asset(AvatarAsset.GardenClub)),
                        externalUri = uri.toString(),
                    )
                } else {
                    val bytes = runCatching {
                        AvatarImageProcessor.prepare(context.contentResolver, uri)
                    }.getOrNull()
                    if (bytes == null) {
                        attachmentError = true
                    } else {
                        prepared += MessageAttachment(
                            id = nextId("photo"),
                            kind = MessageAttachmentKind.Photo,
                            label = displayName(context, uri, "Photo"),
                            images = listOf(ProfileAvatar.DeviceImage(bytes)),
                        )
                    }
                }
            }
            if (prepared.isNotEmpty()) onAddAttachments(prepared)
            isPreparing = false
        }
    }

    val visualPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20),
    ) { uris -> prepareVisualUris(uris) }
    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val attachments = uris.map { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            MessageAttachment(
                id = nextId("file"),
                kind = MessageAttachmentKind.File,
                label = displayName(context, uri, "Document"),
                externalUri = uri.toString(),
            )
        }
        onAddAttachments(attachments)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = cameraUri
        if (saved && uri != null) prepareVisualUris(listOf(uri))
    }

    DisposableEffect(Unit) {
        onDispose {
            preparationJob?.cancel()
            if (context.findActivity()?.isChangingConfigurations != true) {
                voiceState = ComposerVoiceState.Idle
            }
        }
    }
    LaunchedEffect(voiceState is ComposerVoiceState.Recording) {
        while (voiceState is ComposerVoiceState.Recording) {
            delay(100)
            voiceState = ComposerVoiceReducer.tick(voiceState)
        }
    }
    LaunchedEffect((voiceState as? ComposerVoiceState.Review)?.isPlaying) {
        while ((voiceState as? ComposerVoiceState.Review)?.isPlaying == true) {
            delay(100)
            voiceState = ComposerVoiceReducer.advancePlayback(voiceState)
        }
    }
    LaunchedEffect((voiceState as? ComposerVoiceState.Review)?.isTranscribing) {
        if ((voiceState as? ComposerVoiceState.Review)?.isTranscribing == true) {
            delay(450)
            voiceState = ComposerVoiceReducer.finishTranscription(
                voiceState,
                VoiceMessageFixture.transcript,
            )
        }
    }
    LaunchedEffect(chat.draftReplyMessageId) {
        if (chat.draftReplyMessageId != null) messageFocusRequester.requestFocus()
    }

    val linkPreview = LinkPreviewDetector.first(chat.draftText)
        ?.takeIf { chat.draftAttachments.isEmpty() && voiceState == ComposerVoiceState.Idle }
        ?.takeUnless { it.url == chat.suppressedDraftLinkUrl }
    val replyMessage = chat.draftReplyMessageId?.let { id ->
        chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().firstOrNull { it.message.id == id }?.message
    }
    val mentionMatch = if (chat.isGroup) Regex("(?:^|\\s)@([^\\s]*)$").find(chat.draftText) else null
    val mentionQuery = mentionMatch?.groupValues?.getOrNull(1).orEmpty()
    val mentionPeople = if (mentionMatch == null) {
        emptyList()
    } else {
        val memberIds = chat.members.map { it.personId }.toSet()
        profile.people.filter { person ->
            person.id in memberIds && person.id != profile.id &&
                (mentionQuery.isBlank() || person.name.contains(mentionQuery, ignoreCase = true))
        }.take(5)
    }
    val mentionNames = remember(chat.members, profile.people) {
        val memberIds = chat.members.mapTo(mutableSetOf()) { it.personId }
        profile.people.filter { it.id in memberIds }.map(Person::name)
    }
    val sendable = chat.draftText.isNotBlank() || chat.draftAttachments.isNotEmpty() || linkPreview != null
    val isExpansionEnabled = when (val state = voiceState) {
        ComposerVoiceState.Idle -> true
        is ComposerVoiceState.Recording -> false
        is ComposerVoiceState.Review -> state.transcript != null && state.format != VoiceMessageFormat.Voice
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val expandedTopGapPx = with(density) { ComposerExpansionPolicy.ExpandedTopGapDp.dp.roundToPx() }
        val expandedHeightPx = (constraints.maxHeight - expandedTopGapPx).coerceAtLeast(compactHeightPx)
        val travelPx = (expandedHeightPx - compactHeightPx).coerceAtLeast(1)
        val usesFlexibleLayout = compactHeightPx > 0 &&
            (isExpanded || expansionProgress.value > 0f || isDraggingExpansion || isSettlingExpansion)
        val presentedHeightPx = if (usesFlexibleLayout) {
            compactHeightPx + (travelPx * expansionProgress.value).roundToInt()
        } else {
            compactHeightPx
        }

        suspend fun settle(expanded: Boolean, initialVelocity: Float = 0f) {
            if (isExpanded != expanded) isExpanded = expanded
            isSettlingExpansion = true
            expansionProgress.animateTo(
                targetValue = if (expanded) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                initialVelocity = initialVelocity,
            )
            isSettlingExpansion = false
        }

        LaunchedEffect(isExpansionEnabled) {
            if (!isExpansionEnabled && (isExpanded || expansionProgress.value > 0f)) settle(false)
        }
        LaunchedEffect(expansionProgress, travelPx, isDraggingExpansion, isSettlingExpansion) {
            snapshotFlow {
                Triple(expansionProgress.value, isDraggingExpansion, isSettlingExpansion)
            }.collect { (progress, dragging, settling) ->
                onExpansionPresentationChanged(
                    dragging || settling || progress > 0f,
                    travelPx * progress,
                )
            }
        }
        BackHandler(enabled = isExpanded && !attachmentMenuOpen) {
            coroutineScope.launch { settle(false) }
        }

        val expansionModifier = Modifier.composerExpansionGesture(
            enabled = isExpansionEnabled && compactHeightPx > 0 && travelPx > 1,
            onStart = {
                isDraggingExpansion = true
                isSettlingExpansion = false
                coroutineScope.launch { expansionProgress.stop() }
                expansionProgress.value
            },
            onDrag = { startProgress, translationY ->
                coroutineScope.launch {
                    expansionProgress.snapTo(
                        ComposerExpansionPolicy.clampProgress(startProgress - (translationY / travelPx)),
                    )
                }
            },
            onEnd = { velocityY ->
                isDraggingExpansion = false
                val projectedTravelDp = with(density) { (velocityY * 0.5f).toDp().value }
                val destination = ComposerExpansionPolicy.destinationExpanded(
                    expansionProgress.value,
                    projectedTravelDp,
                )
                coroutineScope.launch { settle(destination, initialVelocity = -velocityY / travelPx) }
            },
            onCancel = {
                isDraggingExpansion = false
                coroutineScope.launch { settle(isExpanded) }
            },
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(
                    if (usesFlexibleLayout) {
                        Modifier.height(with(density) { presentedHeightPx.toDp() })
                    } else {
                        Modifier
                    },
                )
                .onSizeChanged { size ->
                    if (!usesFlexibleLayout && size.height != compactHeightPx) {
                        compactHeightPx = size.height
                        onCompactHeightChanged(size.height)
                    }
                }
                .testTag("conversation.composer.host"),
        ) {
            if (mentionMatch != null && mentionPeople.isNotEmpty() && !usesFlexibleLayout) {
                MentionSuggestions(
                    people = mentionPeople,
                    onSelect = { person ->
                        val range = mentionMatch.range
                        val prefix = chat.draftText.substring(0, range.first)
                        val separator = if (prefix.isNotEmpty() && !prefix.endsWith(' ')) " " else ""
                        onDraftTextChanged("$prefix$separator@${person.name} ")
                    },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (usesFlexibleLayout) Modifier.weight(1f) else Modifier)
                    .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                verticalAlignment = Alignment.Bottom,
            ) {
                if (voiceState !is ComposerVoiceState.Recording) {
                    ComposerLeadingAction(
                        voiceState = voiceState,
                        menuExpanded = attachmentMenuOpen,
                        onMenuExpandedChange = { attachmentMenuOpen = it },
                        addAttachmentDescription = addAttachmentDescription,
                        enabled = !isPreparing,
                        menuItems = listOf(
                        WhiteNoiseMenuItem(
                            label = stringResource(R.string.camera),
                            icon = R.drawable.ic_camera,
                            onClick = {
                                val directory = File(context.cacheDir, "camera").apply { mkdirs() }
                                val file = File.createTempFile("white-noise-", ".jpg", directory)
                                cameraUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.files",
                                    file,
                                )
                                camera.launch(cameraUri!!)
                            },
                        ),
                        WhiteNoiseMenuItem(
                            label = stringResource(R.string.photos_and_videos),
                            icon = R.drawable.ic_image,
                            onClick = {
                                visualPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                                )
                            },
                        ),
                        WhiteNoiseMenuItem(
                            label = stringResource(R.string.files),
                            icon = R.drawable.ic_description,
                            onClick = { documentPicker.launch(arrayOf("*/*")) },
                        ),
                        WhiteNoiseMenuItem(
                            label = stringResource(R.string.contact),
                            icon = R.drawable.ic_person,
                            onClick = { contactPickerOpen = true },
                        ),
                        ),
                        onCancelVoice = {
                            voiceState = ComposerVoiceState.Idle
                            coroutineScope.launch { settle(false) }
                        },
                    )
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .then(if (usesFlexibleLayout) Modifier.fillMaxHeight() else Modifier)
                        .then(expansionModifier)
                        .semantics {
                            customActions = buildList {
                                if (isExpansionEnabled) {
                                    add(
                                        CustomAccessibilityAction(
                                            if (isExpanded) collapseMessageLabel else expandMessageLabel,
                                        ) {
                                            coroutineScope.launch { settle(!isExpanded) }
                                            true
                                        },
                                    )
                                    if (isExpanded) {
                                        add(CustomAccessibilityAction(hideKeyboardLabel) {
                                            focusManager.clearFocus()
                                            true
                                        })
                                    }
                                }
                            }
                        }
                        .testTag("conversation.composer.surface"),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.then(if (usesFlexibleLayout) Modifier.fillMaxHeight() else Modifier),
                    ) {
                        if (chat.draftAttachments.isNotEmpty() && voiceState == ComposerVoiceState.Idle) {
                            DraftAttachmentShelf(
                                attachments = chat.draftAttachments,
                                onPreview = { mediaViewerAttachmentId = it },
                                onRemove = onRemoveAttachment,
                            )
                            if (chat.draftAttachments.any(MessageAttachment::isVisual)) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                        linkPreview?.let { preview ->
                            DraftLinkPreview(
                                title = preview.title,
                                domain = preview.domain,
                                summary = preview.summary,
                                image = preview.image,
                                onRemove = { onSuppressLink(preview.url) },
                            )
                        }
                        replyMessage?.takeIf { voiceState == ComposerVoiceState.Idle }?.let { message ->
                            DraftReplyQuote(
                                author = if (message.authorId == profile.id) {
                                    stringResource(R.string.you)
                                } else {
                                    profile.people.firstOrNull { it.id == message.authorId }?.name
                                        ?: stringResource(R.string.unknown_person)
                                },
                                text = message.text.ifBlank { message.attachments.firstOrNull()?.label.orEmpty() },
                                onCancel = onCancelReply,
                            )
                        }
                        when (val state = voiceState) {
                            ComposerVoiceState.Idle -> ComposerTextInput(
                                text = chat.draftText,
                                onTextChanged = onDraftTextChanged,
                                onSend = {
                                    if (onSendDraft()) coroutineScope.launch { settle(false) }
                                },
                                onRecord = {
                                    focusManager.clearFocus()
                                    voiceState = ComposerVoiceReducer.start(voiceState)
                                    coroutineScope.launch { settle(false) }
                                },
                                sendable = sendable,
                                enabled = !isPreparing,
                                expanded = usesFlexibleLayout,
                                hasAttachments = chat.draftAttachments.isNotEmpty(),
                                focusRequester = messageFocusRequester,
                                mentionNames = mentionNames,
                                recordVoiceDescription = recordVoiceDescription,
                                modifier = if (usesFlexibleLayout) Modifier.weight(1f) else Modifier,
                            )
                            is ComposerVoiceState.Recording -> RecordingComposer(
                                elapsedTenths = state.elapsedTenths,
                                onStop = { voiceState = ComposerVoiceReducer.stop(voiceState) },
                            )
                            is ComposerVoiceState.Review -> VoiceReviewComposer(
                                state = state,
                                expanded = usesFlexibleLayout,
                                onStateChanged = { selected ->
                                    voiceState = selected
                                    if (selected.format == VoiceMessageFormat.Voice && isExpanded) {
                                        coroutineScope.launch { settle(false) }
                                    }
                                },
                                onSend = {
                                    val submission = VoiceDraftSubmission(
                                        format = state.format,
                                        transcript = state.transcript.orEmpty(),
                                        durationSeconds = state.durationSeconds,
                                    )
                                    if (onSendVoice(submission)) {
                                        voiceState = ComposerVoiceState.Idle
                                        coroutineScope.launch { settle(false) }
                                    }
                                },
                                modifier = if (usesFlexibleLayout) Modifier.weight(1f) else Modifier,
                            )
                        }
                        if (isPreparing) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin, vertical = 4.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                                horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text(stringResource(R.string.preparing_attachment))
                            }
                        }
                        if (attachmentError) {
                            Text(
                                stringResource(R.string.attachment_error),
                                modifier = Modifier
                                    .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin, vertical = 4.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }

    if (contactPickerOpen) {
        ContactPickerSheet(
            people = profile.people,
            onDismiss = { contactPickerOpen = false },
            onSelect = { person ->
                onAddAttachments(
                    listOf(
                        MessageAttachment(
                            id = nextId("contact"),
                            kind = MessageAttachmentKind.Contact,
                            label = "Contact: ${person.name}",
                            images = listOf(person.avatar),
                        ),
                    ),
                )
                contactPickerOpen = false
            },
        )
    }
    mediaViewerAttachmentId?.let { initialAttachmentId ->
        DraftMediaViewer(
            attachments = chat.draftAttachments.filter(MessageAttachment::isVisual),
            initialAttachmentId = initialAttachmentId,
            onDismiss = { mediaViewerAttachmentId = null },
            onApplyExcluded = { excludedIds ->
                excludedIds.forEach(onRemoveAttachment)
                mediaViewerAttachmentId = null
            },
        )
    }
}

@Composable
private fun ComposerTextInput(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onRecord: () -> Unit,
    sendable: Boolean,
    enabled: Boolean,
    expanded: Boolean,
    hasAttachments: Boolean,
    focusRequester: FocusRequester,
    mentionNames: List<String>,
    recordVoiceDescription: String,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val mentionBackground = MaterialTheme.colorScheme.surfaceContainerHighest
    val mentionTransformation = remember(mentionNames, mentionBackground) {
        MentionVisualTransformation(mentionNames, mentionBackground)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (expanded) Modifier.fillMaxHeight() else Modifier),
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChanged,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .consumeEditorTapAtFinalPass()
                .focusRequester(focusRequester)
                .heightIn(min = 48.dp)
                .then(if (expanded) Modifier.fillMaxHeight() else Modifier)
                .testTag("conversation.composer.editor"),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            keyboardActions = KeyboardActions(),
            minLines = 1,
            maxLines = if (expanded) Int.MAX_VALUE else ComposerExpansionPolicy.compactLineLimit(hasAttachments),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = mentionTransformation,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (expanded) Modifier.fillMaxHeight() else Modifier)
                        .padding(start = 14.dp, top = 12.dp, bottom = 12.dp),
                    contentAlignment = if (expanded) Alignment.TopStart else Alignment.CenterStart,
                ) {
                    if (text.isEmpty()) {
                        Text(
                            stringResource(R.string.message),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (sendable) {
            ComposerSendButton(
                onClick = onSend,
                enabled = enabled,
                description = stringResource(R.string.send),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .voiceLongPressGesture(
                        enabled = enabled,
                        onLongPress = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRecord()
                        },
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = recordVoiceDescription
                        onClick(label = recordVoiceDescription) {
                            onRecord()
                            true
                        }
                    }
                    .testTag("conversation.voice"),
                contentAlignment = Alignment.Center,
            ) {
                MiniWaveformGlyph(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("conversation.voice.icon"),
                )
            }
        }
    }
}

@Composable
private fun MentionSuggestions(people: List<Person>, onSelect: (Person) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WhiteNoiseSpacing.Related, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            people.forEach { person ->
                ListItem(
                    headlineContent = { Text(person.name) },
                    leadingContent = { ProfileAvatar(person.name, person.avatar, Modifier.size(36.dp), contentDescription = null) },
                    modifier = Modifier.clickable { onSelect(person) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun DraftAttachmentShelf(
    attachments: List<MessageAttachment>,
    onPreview: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val description = pluralStringResource(
        R.plurals.draft_attachment_count,
        attachments.size,
        attachments.size,
    )
    val hasVisual = attachments.any(MessageAttachment::isVisual)
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                if (hasVisual) ComposerAttachmentSizing.VisualShelfHeightDp.dp
                else ComposerAttachmentSizing.UtilityShelfHeightDp.dp,
            )
            .testTag("conversation.composer.attachments")
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(
            horizontal = WhiteNoiseSpacing.Related,
            vertical = WhiteNoiseSpacing.Related,
        ),
        horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        verticalAlignment = Alignment.Bottom,
    ) {
        items(attachments, key = MessageAttachment::id) { attachment ->
            val removeDescription = stringResource(R.string.remove_attachment, attachment.label)
            val isVisual = attachment.isVisual()
            val cardSize = if (isVisual) {
                visualAttachmentSize(attachment)
            } else {
                ComposerAttachmentSizing.forKind(attachment.kind)
            }
            Surface(
                modifier = Modifier
                    .height(cardSize.heightDp.dp)
                    .width(cardSize.widthDp.dp)
                    .then(if (isVisual) Modifier.clickable { onPreview(attachment.id) } else Modifier)
                    .semantics { contentDescription = attachment.label },
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Box {
                    DraftAttachmentContent(
                        attachment = attachment,
                        modifier = Modifier.fillMaxSize(),
                    )
                    DraftRemoveButton(
                        onClick = { onRemove(attachment.id) },
                        description = removeDescription,
                        highContrast = isVisual,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
        }
    }
}

@Composable
private fun visualAttachmentSize(attachment: MessageAttachment) = remember(attachment) {
    val ratio = when (val image = attachment.images.firstOrNull()) {
        is ProfileAvatar.DeviceImage -> {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size, options)
            if (options.outHeight > 0) options.outWidth.toFloat() / options.outHeight else 4f / 3f
        }
        else -> 4f / 3f
    }
    ComposerAttachmentSizing.forKind(attachment.kind, ratio)
}

@Composable
private fun DraftAttachmentContent(
    attachment: MessageAttachment,
    modifier: Modifier = Modifier,
) {
    when {
        attachment.isVisual() -> Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            attachment.images.firstOrNull()?.let { image ->
                ComposerImage(image, Modifier.fillMaxSize())
            } ?: Text(
                attachment.label,
                modifier = Modifier.align(Alignment.Center).padding(WhiteNoiseSpacing.Related),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (attachment.kind == MessageAttachmentKind.Video) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f),
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_arrow),
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(20.dp),
                    )
                }
            }
            if (attachment.kind == MessageAttachmentKind.Gif) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(WhiteNoiseSpacing.Related),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f),
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ) {
                    Text(
                        stringResource(R.string.gif),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        attachment.kind == MessageAttachmentKind.Contact -> Column(
            modifier = modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            attachment.images.firstOrNull()?.let { image ->
                ComposerImage(image, Modifier.size(44.dp).clip(CircleShape))
                Spacer(Modifier.height(WhiteNoiseSpacing.Related))
            }
            Text(
                attachment.label.removePrefix("Contact: "),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        else -> Column(
            modifier = modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_description),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(WhiteNoiseSpacing.Related))
            Text(
                attachment.label,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DraftRemoveButton(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    highContrast: Boolean = false,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = if (highContrast) {
                MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            contentColor = if (highContrast) {
                MaterialTheme.colorScheme.inverseOnSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            border = BorderStroke(
                1.dp,
                if (highContrast) MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = description,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun VoiceWaveform(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    liveTick: Int? = null,
    progress: Float = 1f,
    attenuateQuietSamples: Boolean = false,
) {
    val waveformHeight = 24.dp
    val barWidth = 2.dp
    val barSpacing = 2.dp
    BoxWithConstraints(modifier = modifier.height(waveformHeight)) {
        val barCount = ((maxWidth + barSpacing) / (barWidth + barSpacing))
            .toInt()
            .coerceAtLeast(1)
        val samples = remember(liveTick, barCount) {
            if (liveTick == null) {
                ComposerWaveformPolicy.reviewWindow(barCount)
            } else {
                ComposerWaveformPolicy.liveWindow(liveTick, barCount)
            }
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(barSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            samples.forEachIndexed { index, sample ->
                val progressAlpha = if ((index + 1f) / samples.size <= progress) 1f else 0.25f
                val quietAlpha = if (attenuateQuietSamples) sample.coerceIn(0.28f, 1f) else 1f
                Box(
                    Modifier
                        .width(barWidth)
                        .height((waveformHeight * sample).coerceAtLeast(3.dp))
                        .background(color.copy(alpha = progressAlpha * quietAlpha), CircleShape),
                )
            }
        }
    }
}

@Composable
private fun DraftLinkPreview(
    title: String,
    domain: String,
    summary: String,
    image: ProfileAvatar?,
    onRemove: () -> Unit,
) {
    val removeDescription = stringResource(R.string.remove_link_preview)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WhiteNoiseSpacing.Related, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(WhiteNoiseSpacing.Related),
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            image?.let { ComposerImage(it, Modifier.size(56.dp).clip(MaterialTheme.shapes.small)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    domain,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    summary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DraftRemoveButton(onClick = onRemove, description = removeDescription)
        }
    }
}

@Composable
private fun DraftReplyQuote(author: String, text: String, onCancel: () -> Unit) {
    val cancelDescription = stringResource(R.string.cancel_reply)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WhiteNoiseSpacing.Related, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.padding(WhiteNoiseSpacing.Related),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
            Column(Modifier.weight(1f)) {
                Text(author, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DraftRemoveButton(onClick = onCancel, description = cancelDescription)
        }
    }
}

@Composable
private fun RecordingComposer(elapsedTenths: Int, onStop: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
    ) {
        VoiceWaveform(
            modifier = Modifier
                .weight(1f)
                .testTag("conversation.voice.recording.waveform"),
            color = MaterialTheme.colorScheme.error,
            liveTick = ComposerWaveformPolicy.visualTick(elapsedTenths),
            attenuateQuietSamples = true,
        )
        Text(
            formatDuration(elapsedTenths / 10),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
        )
        IconButton(
            onClick = onStop,
            modifier = Modifier
                .size(48.dp)
                .testTag("conversation.voice.stop"),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_stop),
                contentDescription = stringResource(R.string.stop_recording),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp).testTag("conversation.voice.stop.icon"),
            )
        }
    }
}

@Composable
private fun VoiceReviewComposer(
    state: ComposerVoiceState.Review,
    expanded: Boolean,
    onStateChanged: (ComposerVoiceState.Review) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var formatMenuOpen by remember { mutableStateOf(false) }
    val showsPlayback = state.format != VoiceMessageFormat.Text
    val showsTranscript = state.transcript != null && state.format != VoiceMessageFormat.Voice
    val remainingTenths = (state.durationSeconds * 10 - state.playbackTenths).coerceAtLeast(0)
    val sendDescription = stringResource(
        when (state.format) {
            VoiceMessageFormat.Voice -> R.string.send_voice_message
            VoiceMessageFormat.Text -> R.string.send_text_message
            VoiceMessageFormat.Both -> R.string.send_voice_and_text
        },
    )
    val transcribeDescription = stringResource(R.string.transcribe_recording)
    val transcribingDescription = stringResource(R.string.transcribing)
    Box(
        modifier = modifier.fillMaxWidth().then(if (expanded) Modifier.fillMaxHeight() else Modifier),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (expanded) Modifier.fillMaxHeight() else Modifier)
                .padding(bottom = 48.dp),
        ) {
            if (state.transcript != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box {
                        val formatDescription =
                            "${stringResource(R.string.message_format)}: ${state.format.label}"
                        CompactComposerTextAction(
                            onClick = { formatMenuOpen = true },
                            contentDescription = formatDescription,
                            targetTestTag = "conversation.voice.format",
                            visualTestTag = "conversation.voice.format.visual",
                            modifier = Modifier
                                .heightIn(min = 48.dp),
                        ) {
                            Text(
                                state.format.label,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Icon(
                                painterResource(R.drawable.ic_expand_more),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        WhiteNoiseDropdownMenu(
                            expanded = formatMenuOpen,
                            onDismissRequest = { formatMenuOpen = false },
                            items = VoiceMessageFormat.entries.map { option ->
                                WhiteNoiseMenuItem(
                                    label = option.label,
                                    selected = option == state.format,
                                    onClick = {
                                        onStateChanged(
                                            ComposerVoiceReducer.selectFormat(
                                                state,
                                                option,
                                            ) as ComposerVoiceState.Review,
                                        )
                                    },
                                )
                            },
                            modifier = Modifier.testTag("conversation.voice.format.menu"),
                            anchorSpacing = 2.dp,
                            placement = WhiteNoiseMenuPlacement.AboveAnchor,
                        )
                    }
                }
            }
            if (showsPlayback) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            onStateChanged(
                                ComposerVoiceReducer.togglePlayback(state) as ComposerVoiceState.Review,
                            )
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("conversation.voice.play.container"),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(
                                        if (state.isPlaying) {
                                            R.drawable.ic_pause
                                        } else {
                                            R.drawable.ic_play_arrow
                                        },
                                    ),
                                    contentDescription = stringResource(
                                        if (state.isPlaying) R.string.pause else R.string.play,
                                    ),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                    VoiceWaveform(
                        modifier = Modifier.weight(1f),
                        progress = state.playbackTenths.toFloat() /
                            (state.durationSeconds * 10).coerceAtLeast(1),
                    )
                    Text(
                        formatDuration((remainingTenths + 9) / 10),
                        modifier = Modifier.padding(end = 12.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    )
                }
            }
            if (showsTranscript) {
                BasicTextField(
                    value = state.transcript.orEmpty(),
                    onValueChange = {
                        onStateChanged(
                            ComposerVoiceReducer.editTranscript(state, it) as ComposerVoiceState.Review,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .consumeEditorTapAtFinalPass()
                        .then(
                            if (expanded) Modifier.weight(1f)
                            else Modifier.heightIn(max = 208.dp),
                        )
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("conversation.voice.transcript"),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    minLines = 1,
                    maxLines = if (expanded) Int.MAX_VALUE else ComposerExpansionPolicy.CompactTranscriptLines,
                    decorationBox = { editor ->
                        Box(
                            modifier = if (expanded) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopStart,
                        ) {
                            if (state.transcript.isNullOrEmpty()) {
                                Text(
                                    stringResource(R.string.transcript),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            editor()
                        }
                    },
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Box(Modifier.align(Alignment.Center)) {
                if (state.transcript == null) {
                    val transcribeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                    CompactComposerTextAction(
                        onClick = {
                            onStateChanged(
                                ComposerVoiceReducer.beginTranscription(state) as ComposerVoiceState.Review,
                            )
                        },
                        enabled = !state.isTranscribing,
                        contentDescription = if (state.isTranscribing) {
                            transcribingDescription
                        } else {
                            transcribeDescription
                        },
                        targetTestTag = "conversation.voice.transcribe",
                        visualTestTag = "conversation.voice.transcribe.visual",
                    ) {
                        if (state.isTranscribing) {
                            CircularProgressIndicator(
                                Modifier.size(18.dp),
                                color = transcribeColor,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings_chat_bubble_outline),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .testTag("conversation.voice.transcribe.icon"),
                                tint = transcribeColor,
                            )
                        }
                        Text(
                            stringResource(
                                if (state.isTranscribing) R.string.transcribing else R.string.transcribe,
                            ),
                            modifier = Modifier.testTag("conversation.voice.transcribe.label"),
                            color = transcribeColor,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            ComposerSendButton(
                onClick = onSend,
                enabled = ComposerVoiceReducer.canSend(state),
                description = sendDescription,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun CompactComposerTextAction(
    onClick: () -> Unit,
    contentDescription: String,
    targetTestTag: String,
    visualTestTag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription }
            .testTag(targetTestTag)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .testTag(visualTestTag)
                .clip(CircleShape)
                .indication(interactionSource, ripple())
                .heightIn(min = 32.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

private fun formatDuration(seconds: Int): String =
    "${seconds.coerceAtLeast(0) / 60}:${(seconds.coerceAtLeast(0) % 60).toString().padStart(2, '0')}"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ContactPickerSheet(
    people: List<Person>,
    onDismiss: () -> Unit,
    onSelect: (Person) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredPeople = remember(people, query) {
        val value = query.trim()
        if (value.isEmpty()) {
            people
        } else {
            people.filter {
                it.name.contains(value, ignoreCase = true) ||
                    it.publicKey.contains(value, ignoreCase = true)
            }
        }
    }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(),
        sheetState = sheetState,
    ) {
        Column(Modifier.fillMaxSize().testTag("conversation.contact.sheet")) {
            WhiteNoiseSheetHeader(stringResource(R.string.share_contact), onClose = onDismiss)
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)
                    .heightIn(min = 56.dp)
                    .testTag("conversation.contact.search"),
                placeholder = { Text(stringResource(R.string.name_or_public_key)) },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_search), contentDescription = null)
                },
                trailingIcon = if (query.isEmpty()) {
                    null
                } else {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                painterResource(R.drawable.ic_close),
                                stringResource(R.string.clear_search),
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(
                    start = WhiteNoiseSpacing.CompactScreenMargin,
                    top = WhiteNoiseSpacing.Related,
                    end = WhiteNoiseSpacing.CompactScreenMargin,
                    bottom = WhiteNoiseSpacing.Section,
                ),
            ) {
                if (filteredPeople.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_results),
                            modifier = Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.Section),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    itemsIndexed(filteredPeople, key = { _, person -> person.id }) { index, person ->
                        val shapes = ListItemDefaults.segmentedShapes(
                            index = index,
                            count = filteredPeople.size,
                            defaultShapes = ListItemDefaults.shapes(
                                shape = RoundedCornerShape(0.dp),
                            ),
                        )
                        ListItem(
                            onClick = { onSelect(person) },
                            modifier = Modifier.testTag("conversation.contact.${person.id}"),
                            leadingContent = {
                                ProfileAvatar(
                                    person.name,
                                    person.avatar,
                                    Modifier.size(48.dp),
                                    contentDescription = null,
                                )
                            },
                            supportingContent = {
                                Text(
                                    person.shortPublicKey,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            shapes = shapes,
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            ),
                            content = {
                                Text(person.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                        )
                        if (index != filteredPeople.lastIndex) {
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftMediaViewer(
    attachments: List<MessageAttachment>,
    initialAttachmentId: String,
    onDismiss: () -> Unit,
    onApplyExcluded: (Set<String>) -> Unit,
) {
    if (attachments.isEmpty()) return
    var includedIds by remember(attachments) { mutableStateOf(attachments.map { it.id }.toSet()) }
    val initialPage = attachments.indexOfFirst { it.id == initialAttachmentId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { attachments.size }
    val coroutineScope = rememberCoroutineScope()
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
                    title = { Text(stringResource(R.string.preview_media)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.cancel_media_changes),
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                onApplyExcluded(attachments.map { it.id }.toSet() - includedIds)
                            },
                        ) {
                            Text(stringResource(R.string.done))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { contentPadding ->
            Column(Modifier.fillMaxSize().padding(contentPadding)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    pageSpacing = WhiteNoiseSpacing.Section,
                ) { page ->
                    val attachment = attachments[page]
                    val included = attachment.id in includedIds
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        attachment.images.firstOrNull()?.let {
                            ComposerImage(
                                image = it,
                                modifier = Modifier.fillMaxSize().padding(WhiteNoiseSpacing.CompactScreenMargin),
                                contentScale = ContentScale.Fit,
                            )
                        } ?: Text(attachment.label)
                        FilterChip(
                            selected = included,
                            onClick = {
                                includedIds = if (included) {
                                    includedIds - attachment.id
                                } else {
                                    includedIds + attachment.id
                                }
                            },
                            label = {
                                Text(
                                    stringResource(
                                        if (included) R.string.media_included else R.string.media_excluded,
                                    ),
                                )
                            },
                            leadingIcon = if (included) ({
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }) else null,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(WhiteNoiseSpacing.Section),
                        )
                    }
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(88.dp),
                    contentPadding = PaddingValues(
                        horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                        vertical = WhiteNoiseSpacing.Related,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) {
                    itemsIndexed(attachments, key = { _, item -> item.id }) { index, attachment ->
                        val selected = pagerState.currentPage == index
                        val positionDescription = stringResource(
                            R.string.media_item_position,
                            index + 1,
                            attachments.size,
                        )
                        Surface(
                            modifier = Modifier
                                .size(72.dp)
                                .semantics { contentDescription = positionDescription }
                                .clickable {
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                },
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            border = if (selected) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            },
                        ) {
                            attachment.images.firstOrNull()?.let {
                                ComposerImage(
                                    image = it,
                                    modifier = Modifier.padding(4.dp).clip(MaterialTheme.shapes.small),
                                )
                            } ?: Box(contentAlignment = Alignment.Center) {
                                Text(
                                    attachment.label,
                                    maxLines = 2,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ComposerImage(
    image: ProfileAvatar,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    when (image) {
        is ProfileAvatar.Asset -> Image(
            painterResource(image.asset.drawableResource),
            null,
            modifier,
            contentScale = contentScale,
        )
        is ProfileAvatar.WebImage -> Image(
            painterResource(image.asset.drawableResource),
            null,
            modifier,
            contentScale = contentScale,
        )
        is ProfileAvatar.DeviceImage -> {
            val bitmap = remember(image) {
                BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)?.asImageBitmap()
            }
            if (bitmap == null) Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
            else Image(bitmap, null, modifier, contentScale = contentScale)
        }
        ProfileAvatar.Monogram -> Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

internal fun MessageAttachment.isVisual(): Boolean = when (kind) {
    MessageAttachmentKind.Photo,
    MessageAttachmentKind.Photos,
    MessageAttachmentKind.Video,
    MessageAttachmentKind.Gif,
    -> true
    else -> false
}

private fun displayName(
    context: Context,
    uri: Uri,
    fallback: String,
): String {
    val cursor = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )
    return cursor?.use {
        if (it.moveToFirst()) it.getString(0) else null
    } ?: fallback
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

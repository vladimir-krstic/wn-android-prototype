package dev.ipf.whitenoise.ui.conversation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn as LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt
import kotlin.math.min
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatKind
import dev.ipf.whitenoise.model.ChatMessage
import dev.ipf.whitenoise.model.ChatTimelineEntry
import dev.ipf.whitenoise.model.ComposerAvailability
import dev.ipf.whitenoise.model.ComposerExpansionPolicy
import dev.ipf.whitenoise.model.ConversationItem
import dev.ipf.whitenoise.model.ConversationMediaKey
import dev.ipf.whitenoise.model.ConversationMediaProjection
import dev.ipf.whitenoise.model.ConversationMediaSelection
import dev.ipf.whitenoise.model.ConversationProjection
import dev.ipf.whitenoise.model.ConversationSearch
import dev.ipf.whitenoise.model.DisappearingDuration
import dev.ipf.whitenoise.model.MessageAttachment
import dev.ipf.whitenoise.model.MessageAttachmentKind
import dev.ipf.whitenoise.model.MessageAction
import dev.ipf.whitenoise.model.MessageActionPolicy
import dev.ipf.whitenoise.model.MessageDeletionScope
import dev.ipf.whitenoise.model.MessageDeliveryState
import dev.ipf.whitenoise.model.MessageDeletionState
import dev.ipf.whitenoise.model.MessageSpeechActionState
import dev.ipf.whitenoise.model.Person
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.ReactionCatalog
import dev.ipf.whitenoise.model.VoiceMessageFormat
import dev.ipf.whitenoise.model.VoiceDraftSubmission
import dev.ipf.whitenoise.model.VoiceMessageFixture
import dev.ipf.whitenoise.model.composerAvailability
import dev.ipf.whitenoise.model.visibleText
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseCompactSearchField
import dev.ipf.whitenoise.ui.components.WhiteNoiseOutlinedButton
import dev.ipf.whitenoise.ui.components.drawableResource
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

private fun Modifier.blockPointerInput(enabled: Boolean): Modifier = if (!enabled) {
    this
} else {
    pointerInput(enabled) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent().changes.forEach { it.consume() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConversationScreen(
    profile: Profile,
    chat: Chat,
    onBack: () -> Unit,
    onSend: (String) -> Boolean,
    onRetry: (String) -> Unit,
    onAcceptInvitation: () -> Unit,
    onDeclineInvitation: () -> Unit,
    modifier: Modifier = Modifier,
    onDraftTextChanged: (String) -> Unit = {},
    onAddDraftAttachments: (List<MessageAttachment>) -> Unit = {},
    onRemoveDraftAttachment: (String) -> Unit = {},
    onSuppressDraftLink: (String?) -> Unit = {},
    onCancelDraftReply: () -> Unit = {},
    onSendDraft: () -> Boolean = { onSend(chat.draftText) },
    onSendVoice: (VoiceDraftSubmission) -> Boolean = { false },
    onReply: (String) -> Boolean = { false },
    onReaction: (String, String, Boolean) -> Boolean = { _, _, _ -> false },
    onQuickReactionsChanged: (List<String>) -> Boolean = { false },
    onDeleteMessages: (Set<String>, MessageDeletionScope) -> Boolean = { _, _ -> false },
    onForwardMessages: (Set<String>, List<String>) -> Boolean = { _, _ -> false },
    onForwardMedia: (ConversationMediaKey, List<String>, String) -> Boolean = { _, _, _ -> false },
    onOpenMessageDetails: (String) -> Unit = {},
    onOpenChatInfo: () -> Unit = {},
    onOpenDeveloperTools: (() -> Unit)? = null,
    initialSearch: Boolean = false,
    initialMessageId: String? = null,
) {
    val items = remember(chat.timeline) { ConversationProjection.items(chat) }
    val listState = rememberLazyListState()
    var showDeclineConfirmation by remember { mutableStateOf(false) }
    var viewerSelection by remember { mutableStateOf<ConversationMediaSelection?>(null) }
    var forwardMediaKey by remember { mutableStateOf<ConversationMediaKey?>(null) }
    var focusedMessageId by remember { mutableStateOf<String?>(null) }
    var isSelecting by remember { mutableStateOf(false) }
    var selectedMessageIds by remember { mutableStateOf(emptySet<String>()) }
    var deleteMessageIds by remember { mutableStateOf<Set<String>?>(null) }
    var forwardMessageIds by remember { mutableStateOf<Set<String>?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var emojiMessageId by remember { mutableStateOf<String?>(null) }
    var showConfigureReactions by remember { mutableStateOf(false) }
    var configureReactionSlot by remember { mutableStateOf<Int?>(null) }
    var configureDraft by remember(profile.quickReactions) { mutableStateOf(profile.quickReactions) }
    var isSearching by rememberSaveable(chat.id) { mutableStateOf(initialSearch) }
    var searchQuery by rememberSaveable(chat.id) { mutableStateOf("") }
    var searchResultIndex by rememberSaveable(chat.id) { mutableIntStateOf(0) }
    var pendingInitialMessageId by rememberSaveable(chat.id, initialMessageId) {
        mutableStateOf(initialMessageId)
    }
    var initialViewportSettled by rememberSaveable(chat.id) { mutableStateOf(false) }
    var pendingEndSettlement by remember(chat.id) { mutableStateOf(false) }
    var compactComposerHeightPx by remember(chat.id) { mutableIntStateOf(0) }
    var composerPresentationActive by remember(chat.id) { mutableStateOf(false) }
    var composerTravelPx by remember(chat.id) { mutableFloatStateOf(0f) }
    var pushTimelineWithComposer by remember(chat.id) { mutableStateOf(false) }
    val messageBounds = remember(chat.id) { mutableStateMapOf<String, Rect>() }
    val context = LocalContext.current
    val readAloudController = rememberReadAloudController()
    var localVoiceTranscripts by remember(chat.id) { mutableStateOf(emptyMap<String, String>()) }
    var visibleVoiceTranscriptIds by remember(chat.id) { mutableStateOf(emptySet<String>()) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val composerAvailability = chat.composerAvailability(profile)
    val showsAvailableComposer = !isSearching && !isSelecting &&
        composerAvailability == ComposerAvailability.Available
    val messages = remember(chat.timeline) {
        chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().map(ChatTimelineEntry.Message::message)
    }
    val selectedMessages = messages.filter { it.id in selectedMessageIds }
    val searchResults = remember(chat.timeline, profile.people, searchQuery) {
        ConversationSearch.results(chat, profile, searchQuery)
    }
    val searchResultMessageIds = remember(searchResults) {
        searchResults.mapTo(mutableSetOf(), dev.ipf.whitenoise.model.ConversationSearchResult::messageId)
    }
    val currentSearchMessageId = searchResults.getOrNull(searchResultIndex)?.messageId

    fun settleAfterNextTimelineItem(previousCount: Int) {
        coroutineScope.launch {
            snapshotFlow { listState.layoutInfo.totalItemsCount }
                .first { it > previousCount }
            withFrameNanos { }
            val lastIndex = listState.layoutInfo.totalItemsCount - 1
            if (lastIndex >= 0) listState.scrollToItem(lastIndex)
        }
    }

    fun closeSearch() {
        isSearching = false
        searchQuery = ""
        searchResultIndex = 0
    }

    fun beginReply(messageId: String): Boolean {
        val accepted = onReply(messageId)
        if (accepted) {
            val itemIndex = items.indexOfFirst {
                it is ConversationItem.MessageItem && it.message.id == messageId
            }
            if (itemIndex >= 0) {
                coroutineScope.launch {
                    repeat(3) { withFrameNanos { } }
                    listState.animateScrollToItem(itemIndex)
                }
            }
        }
        return accepted
    }

    fun resolvedVoiceTranscript(message: ChatMessage): String? =
        message.attachments.firstOrNull { it.kind == MessageAttachmentKind.Voice }?.transcript
            ?: localVoiceTranscripts[message.id]

    fun speechActionState(message: ChatMessage): MessageSpeechActionState = MessageSpeechActionState(
        transcriptAvailable = resolvedVoiceTranscript(message) != null,
        transcriptVisible = message.id in visibleVoiceTranscriptIds,
        reading = readAloudController.activeMessageId == message.id,
        canReadAloud = readAloudController.ready,
    )

    BackHandler(enabled = isSearching, onBack = ::closeSearch)
    BackHandler(enabled = isSelecting) {
        isSelecting = false
        selectedMessageIds = emptySet()
    }

    fun handleAction(message: ChatMessage, action: MessageAction) {
        focusedMessageId = null
        when (action) {
            MessageAction.RetrySend -> onRetry(message.id)
            MessageAction.Reply -> beginReply(message.id)
            MessageAction.Forward -> forwardMessageIds = setOf(message.id)
            MessageAction.Copy -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Message", message.visibleText(profile.id)))
            }
            MessageAction.ReadAloud -> readAloudController.toggle(
                message.id,
                message.visibleText(profile.id),
            )
            MessageAction.StopReading -> readAloudController.stop()
            MessageAction.Transcribe -> {
                localVoiceTranscripts = localVoiceTranscripts +
                    (message.id to VoiceMessageFixture.transcript)
                visibleVoiceTranscriptIds = visibleVoiceTranscriptIds + message.id
            }
            MessageAction.ShowTranscript -> {
                visibleVoiceTranscriptIds = visibleVoiceTranscriptIds + message.id
            }
            MessageAction.HideTranscript -> {
                visibleVoiceTranscriptIds = visibleVoiceTranscriptIds - message.id
            }
            MessageAction.CopyTranscript -> {
                val transcript = resolvedVoiceTranscript(message)
                    ?: message.text.takeIf {
                        message.attachments.any { attachment ->
                            attachment.kind == MessageAttachmentKind.Voice
                        }
                    }
                    .orEmpty()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Transcript", transcript))
            }
            MessageAction.Select -> {
                isSelecting = true
                selectedMessageIds = setOf(message.id)
            }
            MessageAction.Info -> onOpenMessageDetails(message.id)
            MessageAction.Delete -> deleteMessageIds = setOf(message.id)
        }
    }

    LaunchedEffect(
        chat.id,
        items.size,
        compactComposerHeightPx,
        showsAvailableComposer,
        pendingInitialMessageId,
    ) {
        if (initialViewportSettled) return@LaunchedEffect
        val target = pendingInitialMessageId
        val targetIndex = target?.let { messageId ->
            items.indexOfFirst {
                it is ConversationItem.MessageItem && it.message.id == messageId
            }
        } ?: -1
        if (targetIndex >= 0) {
            listState.scrollToItem(targetIndex)
            pendingInitialMessageId = null
            initialViewportSettled = true
        } else if (items.isEmpty()) {
            initialViewportSettled = true
        } else if (showsAvailableComposer && compactComposerHeightPx == 0) {
            return@LaunchedEffect
        } else if (items.isNotEmpty()) {
            withFrameNanos { }
            listState.scrollToItem(items.lastIndex)
            withFrameNanos { }
            listState.scrollToItem(items.lastIndex)
            initialViewportSettled = true
        }
    }
    LaunchedEffect(pendingEndSettlement, items.size, compactComposerHeightPx) {
        if (!pendingEndSettlement || items.isEmpty() || compactComposerHeightPx == 0) {
            return@LaunchedEffect
        }
        withFrameNanos { }
        listState.scrollToItem(items.lastIndex)
        withFrameNanos { }
        listState.scrollToItem(items.lastIndex)
        pendingEndSettlement = false
    }
    LaunchedEffect(currentSearchMessageId) {
        val messageId = currentSearchMessageId ?: return@LaunchedEffect
        val itemIndex = items.indexOfFirst {
            it is ConversationItem.MessageItem && it.message.id == messageId
        }
        if (itemIndex >= 0) listState.animateScrollToItem(itemIndex)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            when {
                isSelecting -> SelectionTopBar(
                    count = selectedMessageIds.size,
                    onClose = {
                        isSelecting = false
                        selectedMessageIds = emptySet()
                    },
                )
                isSearching -> ConversationSearchTopBar(
                    query = searchQuery,
                    onQueryChanged = {
                        searchQuery = it
                        searchResultIndex = 0
                    },
                    onClose = ::closeSearch,
                )
                else -> ConversationTopBar(
                    chat = chat,
                    onBack = onBack,
                    onInfo = onOpenChatInfo,
                    onDeveloperTools = onOpenDeveloperTools,
                )
            }
        },
        bottomBar = {
            when {
                isSearching -> SearchResultsBottomBar(
                    count = searchResults.size,
                    current = searchResultIndex,
                    onOlder = {
                        searchResultIndex = (searchResultIndex + 1).coerceAtMost(searchResults.lastIndex)
                    },
                    onNewer = { searchResultIndex = (searchResultIndex - 1).coerceAtLeast(0) },
                )
                isSelecting -> SelectionBottomBar(
                    selectedCount = selectedMessageIds.size,
                    canForward = MessageActionPolicy.canForward(selectedMessages),
                    onDelete = { deleteMessageIds = selectedMessageIds },
                    onForward = { forwardMessageIds = selectedMessageIds },
                )
                composerAvailability != ComposerAvailability.Available -> ConversationBottomBar(
                    profile = profile,
                    chat = chat,
                    onAccept = onAcceptInvitation,
                    onDecline = { showDeclineConfirmation = true },
                    onCheckRelays = onOpenChatInfo,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { contentPadding ->
        val appliedContentPadding = if (showsAvailableComposer) {
            val leftPadding = contentPadding.calculateLeftPadding(layoutDirection)
            val rightPadding = contentPadding.calculateRightPadding(layoutDirection)
            PaddingValues(
                start = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) {
                    leftPadding
                } else {
                    rightPadding
                },
                top = contentPadding.calculateTopPadding(),
                end = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) {
                    rightPadding
                } else {
                    leftPadding
                },
                bottom = 0.dp,
            )
        } else {
            contentPadding
        }
        val bottomSafePadding = if (showsAvailableComposer && !WindowInsets.isImeVisible) {
            contentPadding.calculateBottomPadding()
        } else {
            0.dp
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(appliedContentPadding)
                .consumeWindowInsets(appliedContentPadding)
                .then(if (showsAvailableComposer) Modifier.imePadding() else Modifier),
        ) {
            AdaptiveContent(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("conversation.timeline")
                    .graphicsLayer {
                        translationY = if (pushTimelineWithComposer) -composerTravelPx else 0f
                    }
                    .blockPointerInput(composerPresentationActive)
                    .then(
                        if (composerPresentationActive) Modifier.clearAndSetSemantics { } else Modifier,
                    ),
                state = listState,
                contentPadding = PaddingValues(
                    start = WhiteNoiseSpacing.CompactScreenMargin,
                    top = WhiteNoiseSpacing.Related,
                    end = WhiteNoiseSpacing.CompactScreenMargin,
                    bottom = WhiteNoiseSpacing.Related + bottomSafePadding +
                        with(density) { compactComposerHeightPx.toDp() },
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
                userScrollEnabled = !composerPresentationActive,
            ) {
                items.forEach { item ->
                    when (item) {
                        is ConversationItem.DayHeader -> stickyHeader(key = item.id) {
                            DayHeader(item.label, visible = !composerPresentationActive)
                        }
                        is ConversationItem.EventItem -> item(key = item.id, contentType = "event") {
                            TimelineInformation(item.entry.text)
                        }
                        is ConversationItem.NoticeItem -> item(key = item.id, contentType = "notice") {
                            TimelineInformation(item.entry.text, isNotice = true)
                        }
                        is ConversationItem.MessageItem -> item(key = item.id, contentType = "message") {
                            val resultPosition = searchResults.indexOfFirst {
                                it.messageId == item.message.id
                            }
                            val isCurrentSearchResult = isSearching &&
                                item.message.id == currentSearchMessageId
                            val searchPosition = resultPosition.takeIf {
                                isCurrentSearchResult && it >= 0
                            }?.let {
                                pluralStringResource(
                                    R.plurals.match_position,
                                    searchResults.size,
                                    it + 1,
                                    searchResults.size,
                                )
                            }
                            MessageRow(
                                profile = profile,
                                chat = chat,
                                item = item,
                                speechActionState = speechActionState(item.message),
                                onRetry = { onRetry(item.message.id) },
                                onOpenMedia = { key ->
                                    viewerSelection = ConversationMediaProjection.selection(chat, profile, key)
                                },
                                isSelectionMode = isSelecting,
                                selected = item.message.id in selectedMessageIds,
                                searchAlpha = conversationSearchMessageAlpha(
                                    isSearching = isSearching,
                                    query = searchQuery,
                                    isResult = item.message.id in searchResultMessageIds,
                                ),
                                searchQuery = searchQuery.takeIf { isSearching }.orEmpty(),
                                searchPosition = searchPosition,
                                onToggleSelection = {
                                    selectedMessageIds = if (item.message.id in selectedMessageIds) {
                                        selectedMessageIds - item.message.id
                                    } else {
                                        selectedMessageIds + item.message.id
                                    }
                                },
                                onShowActions = { focusedMessageId = item.message.id },
                                onAccessibilityAction = { action -> handleAction(item.message, action) },
                                onSwipeReply = { beginReply(item.message.id) },
                                onReaction = { emoji -> onReaction(item.message.id, emoji, false) },
                                readAloudController = readAloudController,
                                onPositioned = { bounds ->
                                    if (messageBounds[item.message.id] != bounds) {
                                        messageBounds[item.message.id] = bounds
                                    }
                                },
                            )
                        }
                    }
                }
            }
            }
            if (showsAvailableComposer) {
                AdaptiveContent(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                ) {
                    FullConversationComposer(
                        profile = profile,
                        chat = chat,
                        onDraftTextChanged = onDraftTextChanged,
                        onAddAttachments = onAddDraftAttachments,
                        onRemoveAttachment = onRemoveDraftAttachment,
                        onSuppressLink = onSuppressDraftLink,
                        onCancelReply = onCancelDraftReply,
                        onSendDraft = {
                            val previousCount = listState.layoutInfo.totalItemsCount
                            onSendDraft().also { sent ->
                                if (sent) settleAfterNextTimelineItem(previousCount)
                            }
                        },
                        onSendVoice = { submission ->
                            val previousCount = listState.layoutInfo.totalItemsCount
                            onSendVoice(submission).also { sent ->
                                if (sent) settleAfterNextTimelineItem(previousCount)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        onCompactHeightChanged = { measuredHeight ->
                            val wasAtBottom = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ==
                                listState.layoutInfo.totalItemsCount - 1
                            if (compactComposerHeightPx != measuredHeight) {
                                compactComposerHeightPx = measuredHeight
                                if (wasAtBottom || !initialViewportSettled) {
                                    pendingEndSettlement = true
                                }
                            }
                        },
                        onExpansionPresentationChanged = { active, travel ->
                            if (active && !composerPresentationActive) {
                                pushTimelineWithComposer = ComposerExpansionPolicy.shouldPushTimeline(
                                    listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ==
                                        listState.layoutInfo.totalItemsCount - 1,
                                )
                            }
                            if (!active) pushTimelineWithComposer = false
                            composerPresentationActive = active
                            composerTravelPx = travel
                        },
                    )
                }
            }
        }
    }

    if (showDeclineConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeclineConfirmation = false },
            title = { Text(stringResource(R.string.decline_invitation_title)) },
            text = {
                Text(
                    stringResource(
                        if (chat.isGroup) R.string.decline_group_detail else R.string.decline_direct_detail,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeclineConfirmation = false
                    onDeclineInvitation()
                }) {
                    Text(stringResource(R.string.decline), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeclineConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    viewerSelection?.let { selection ->
        ReadOnlyMediaViewer(
            selection = selection,
            onDismiss = { viewerSelection = null },
            onForward = { item ->
                forwardMediaKey = item.key
                viewerSelection = null
            },
            onGoToMessage = { item ->
                viewerSelection = null
                val itemIndex = items.indexOfFirst {
                    it is ConversationItem.MessageItem && it.message.id == item.message.id
                }
                if (itemIndex >= 0) coroutineScope.launch { listState.animateScrollToItem(itemIndex) }
            },
        )
    }

    items.filterIsInstance<ConversationItem.MessageItem>()
        .firstOrNull { it.message.id == focusedMessageId }
        ?.let { focusedItem ->
        val message = focusedItem.message
        FocusedMessageActionsOverlay(
            profile = profile,
            chat = chat,
            item = focusedItem,
            speechActionState = speechActionState(message),
            sourceBounds = messageBounds[message.id],
            onDismiss = { focusedMessageId = null },
            onReaction = { emoji, remove ->
                onReaction(message.id, emoji, remove)
                focusedMessageId = null
            },
            onMoreReactions = {
                emojiMessageId = message.id
                focusedMessageId = null
                showEmojiPicker = true
            },
            onAction = { handleAction(message, it) },
            readAloudController = readAloudController,
        )
    }
    if (showEmojiPicker) {
        EmojiPickerSheet(
            onDismiss = {
                showEmojiPicker = false
                configureReactionSlot = null
            },
            onEmoji = { emoji ->
                val slot = configureReactionSlot
                if (slot != null) {
                    configureDraft = ReactionCatalog.replaceQuick(configureDraft, slot, emoji)
                    configureReactionSlot = null
                    showEmojiPicker = false
                    showConfigureReactions = true
                } else {
                    emojiMessageId?.let { onReaction(it, emoji, false) }
                    showEmojiPicker = false
                }
            },
            onConfigure = if (configureReactionSlot == null) ({
                configureDraft = profile.quickReactions
                showEmojiPicker = false
                showConfigureReactions = true
            }) else null,
        )
    }
    if (showConfigureReactions) {
        ConfigureReactionsSheet(
            current = configureDraft,
            onDismiss = { showConfigureReactions = false },
            onApply = {
                onQuickReactionsChanged(it)
                showConfigureReactions = false
            },
            onPickSlot = { index, draft ->
                configureDraft = draft
                configureReactionSlot = index
                showConfigureReactions = false
                showEmojiPicker = true
            },
        )
    }
    forwardMessageIds?.let { ids ->
        ForwardMessagesSheet(
            profile = profile,
            sourceChatId = chat.id,
            onDismiss = { forwardMessageIds = null },
            onForward = { targets, _ ->
                if (onForwardMessages(ids, targets)) {
                    forwardMessageIds = null
                    isSelecting = false
                    selectedMessageIds = emptySet()
                }
            },
        )
    }
    forwardMediaKey?.let { key ->
        ForwardMessagesSheet(
            profile = profile,
            sourceChatId = chat.id,
            onDismiss = { forwardMediaKey = null },
            allowsAccompanyingMessage = true,
            onForward = { targets, message ->
                if (onForwardMedia(key, targets, message)) forwardMediaKey = null
            },
        )
    }
    deleteMessageIds?.let { ids ->
        DeleteMessagesDialog(
            messages = messages.filter { it.id in ids },
            profileId = profile.id,
            onDismiss = { deleteMessageIds = null },
            onDelete = { scope ->
                if (onDeleteMessages(ids, scope)) {
                    deleteMessageIds = null
                    isSelecting = false
                    selectedMessageIds = emptySet()
                }
            },
        )
    }
}

@Composable
private fun FocusedMessageActionsOverlay(
    profile: Profile,
    chat: Chat,
    item: ConversationItem.MessageItem,
    speechActionState: MessageSpeechActionState,
    sourceBounds: Rect?,
    onDismiss: () -> Unit,
    onReaction: (String, Boolean) -> Unit,
    onMoreReactions: () -> Unit,
    onAction: (MessageAction) -> Unit,
    readAloudController: ReadAloudController,
) {
    val message = item.message
    val outgoing = message.authorId == profile.id
    val selectedReaction = message.reactions.firstOrNull { profile.id in it.personIds }?.emoji
    val selectedDescription = stringResource(R.string.selection_state_selected)
    val notSelectedDescription = stringResource(R.string.selection_state_not_selected)
    val messageActionsTitle = stringResource(R.string.message_actions)
    val closeLabel = stringResource(R.string.close)
    val quickReactions = remember(profile.quickReactions, selectedReaction) {
        ReactionCatalog.quickStrip(profile.quickReactions, selectedReaction)
    }
    val actions = remember(message, profile.id, speechActionState) {
        MessageActionPolicy.available(message, profile.id, speechActionState)
    }
    val density = LocalDensity.current
    val dismissInteraction = remember { MutableInteractionSource() }
    val contentInteraction = remember { MutableInteractionSource() }
    var dialogHeightPx by remember { mutableIntStateOf(0) }
    var contentHeightPx by remember { mutableIntStateOf(0) }
    val topInsetPx = WindowInsets.safeDrawing.getTop(density)
    val bottomInsetPx = WindowInsets.safeDrawing.getBottom(density)
    val marginPx = with(density) { WhiteNoiseSpacing.CompactScreenMargin.roundToPx() }
    val availableHeightDp = with(density) {
        (dialogHeightPx - topInsetPx - bottomInsetPx - marginPx * 2).coerceAtLeast(0).toDp()
    }
    val desiredTop = ((sourceBounds?.center?.y ?: (dialogHeightPx / 2f)) - contentHeightPx / 2f).roundToInt()
    val minimumTop = topInsetPx + marginPx
    val maximumTop = (dialogHeightPx - bottomInsetPx - marginPx - contentHeightPx).coerceAtLeast(minimumTop)
    val contentTop = desiredTop.coerceIn(minimumTop, maximumTop)
    val previewDescription = buildString {
        append(if (outgoing) profile.name else profile.people.firstOrNull { it.id == message.authorId }?.name ?: chat.title)
        val visible = message.visibleText(profile.id)
        if (visible.isNotBlank()) append(", $visible")
        message.attachments.forEach { append(", ${it.label}") }
        append(", ${message.timeLabel}")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { dialogHeightPx = it.height }
                .background(Color.Black.copy(alpha = 0.42f))
                .semantics {
                    paneTitle = messageActionsTitle
                    customActions = listOf(
                        CustomAccessibilityAction(closeLabel) {
                            onDismiss()
                            true
                        },
                    )
                }
                .clickable(
                    interactionSource = dismissInteraction,
                    indication = null,
                    onClick = onDismiss,
                )
                .testTag("message.actions.overlay"),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, contentTop) }
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)
                    .heightIn(max = availableHeightDp)
                    .verticalScroll(rememberScrollState())
                    .onSizeChanged { contentHeightPx = it.height }
                    .clickable(
                        interactionSource = contentInteraction,
                        indication = null,
                        onClick = {},
                    ),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (outgoing) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 392.dp)
                            .testTag("message.actions.reactions"),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        tonalElevation = 3.dp,
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            items(quickReactions, key = { it }) { emoji ->
                                val selected = emoji == selectedReaction
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable { onReaction(emoji, selected) }
                                        .semantics {
                                            contentDescription = emoji
                                            stateDescription = if (selected) selectedDescription else notSelectedDescription
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = CircleShape,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            Color.Transparent
                                        },
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(emoji, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }
                            }
                            item {
                                IconButton(
                                    onClick = onMoreReactions,
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_add),
                                        contentDescription = stringResource(R.string.more_reactions),
                                    )
                                }
                            }
                        }
                    }
                }
                ScaleToFitHeight(
                    maximumHeight = 320.dp,
                    transformOrigin = TransformOrigin(if (outgoing) 1f else 0f, 0f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .blockPointerInput(true)
                        .testTag("message.actions.preview")
                        .clearAndSetSemantics { contentDescription = previewDescription },
                ) {
                    MessageRow(
                        profile = profile,
                        chat = chat,
                        item = item,
                        speechActionState = speechActionState,
                        onRetry = {},
                        onOpenMedia = {},
                        isSelectionMode = false,
                        selected = false,
                        searchAlpha = 1f,
                        searchQuery = "",
                        searchPosition = null,
                        onToggleSelection = {},
                        onShowActions = {},
                        onAccessibilityAction = {},
                        onSwipeReply = { false },
                        onReaction = {},
                        readAloudController = readAloudController,
                        contextPreview = true,
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (outgoing) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Surface(
                        modifier = Modifier
                            .widthIn(min = 248.dp, max = 300.dp)
                            .testTag("message.actions.menu"),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        tonalElevation = 3.dp,
                    ) {
                        Column {
                            actions.forEach { action ->
                                val destructive = action == MessageAction.Delete
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clickable { onAction(action) }
                                        .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        painter = painterResource(actionIcon(action)),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = if (destructive) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                    Text(
                                        text = actionLabel(action),
                                        color = if (destructive) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScaleToFitHeight(
    maximumHeight: Dp,
    transformOrigin: TransformOrigin,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val maximumHeightPx = with(LocalDensity.current) { maximumHeight.roundToPx() }
    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->
        val placeable = measurables.single().measure(
            constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity),
        )
        val scale = if (placeable.height <= 0) {
            1f
        } else {
            min(1f, maximumHeightPx.toFloat() / placeable.height)
        }
        val reportedHeight = (placeable.height * scale).roundToInt()
        val reportedWidth = placeable.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val constrainedHeight = reportedHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(reportedWidth, constrainedHeight) {
            placeable.placeRelativeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                this.transformOrigin = transformOrigin
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationTopBar(
    chat: Chat,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    onDeveloperTools: (() -> Unit)?,
) {
    val memberCount = chat.members.size
    val memberLabel = pluralStringResource(R.plurals.group_member_count, memberCount, memberCount)
    val hasTimer = chat.disappearingDuration != DisappearingDuration.Off
    val fullDescription = buildString {
        append(chat.title)
        if (chat.isGroup) append(", $memberLabel")
        if (chat.disappearingDuration != DisappearingDuration.Off) {
            append(", ${stringResource(R.string.disappearing_header, chat.disappearingDuration.label)}")
        }
    }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        title = {
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClick = onInfo,
                    )
                    .semantics(mergeDescendants = true) { contentDescription = fullDescription }
                    .testTag("conversation.header.identity"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ProfileAvatar(
                    chat.title,
                    chat.avatar,
                    Modifier.size(40.dp).testTag("conversation.header.avatar"),
                    contentDescription = null,
                )
                Column(
                    modifier = Modifier.testTag("conversation.header.text"),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy((-2).dp),
                ) {
                    Text(
                        chat.title,
                        modifier = Modifier.testTag("conversation.header.title"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (chat.isGroup || hasTimer) {
                        Row(
                            modifier = Modifier.testTag("conversation.header.metadata"),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (chat.isGroup) {
                                Text(
                                    memberLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (hasTimer) Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (hasTimer) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_timer),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    chat.disappearingDuration.compactLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        actions = {
            onDeveloperTools?.let { openDeveloperTools ->
                IconButton(
                    onClick = openDeveloperTools,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bug_report),
                        contentDescription = stringResource(R.string.conversation_debug),
                    )
                }
            }
        },
        scrollBehavior = dev.ipf.whitenoise.ui.components.LocalWhiteNoiseHeaderScroll.current,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(count: Int, onClose: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close_selection),
                )
            }
        },
        title = { Text(pluralStringResource(R.plurals.selected_count, count, count)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationSearchTopBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchDescription = stringResource(R.string.search_messages)
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.close_search),
                )
            }
        },
        title = {
            WhiteNoiseCompactSearchField(
                value = query,
                onValueChange = onQueryChanged,
                placeholder = stringResource(R.string.messages),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("conversation.searchField")
                    .semantics { contentDescription = searchDescription },
            )
        },
        scrollBehavior = dev.ipf.whitenoise.ui.components.LocalWhiteNoiseHeaderScroll.current,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun DayHeader(label: String, visible: Boolean = true) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (visible) 1f else 0f)
            .padding(vertical = WhiteNoiseSpacing.FormField),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.semantics { heading() },
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TimelineInformation(text: String, isNotice: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isNotice) WhiteNoiseSpacing.Section else WhiteNoiseSpacing.Related),
        contentAlignment = Alignment.Center,
    ) {
        if (isNotice) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .semantics(mergeDescendants = true) { contentDescription = text },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Text(
                text = text,
                modifier = Modifier.widthIn(max = 440.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MessageRow(
    profile: Profile,
    chat: Chat,
    item: ConversationItem.MessageItem,
    speechActionState: MessageSpeechActionState,
    onRetry: () -> Unit,
    onOpenMedia: (ConversationMediaKey) -> Unit,
    isSelectionMode: Boolean,
    selected: Boolean,
    searchAlpha: Float,
    searchQuery: String,
    searchPosition: String?,
    onToggleSelection: () -> Unit,
    onShowActions: () -> Unit,
    onAccessibilityAction: (MessageAction) -> Unit,
    onSwipeReply: () -> Boolean,
    onReaction: (String) -> Unit,
    readAloudController: ReadAloudController,
    onPositioned: (Rect) -> Unit = {},
    contextPreview: Boolean = false,
) {
    val message = item.message
    val outgoing = message.authorId == profile.id
    val author = profile.people.firstOrNull { it.id == message.authorId }
    val authorName = if (outgoing) stringResource(R.string.you) else author?.name ?: chat.title
    val verticalPadding = when {
        item.startsCluster && !contextPreview -> WhiteNoiseSpacing.FormField
        else -> 0.dp
    }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val directionMultiplier = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) 1f else -1f
    val replyThresholdPx = with(density) { 64.dp.toPx() }
    val replyMaximumPx = with(density) { 96.dp.toPx() }
    var rawSwipeDistance by remember(message.id) { mutableFloatStateOf(0f) }
    var displayedSwipeDistance by remember(message.id) { mutableFloatStateOf(0f) }
    var swipeReady by remember(message.id) { mutableStateOf(false) }
    val showTime = item.endsCluster
    val hasMetadata = message.reactions.isNotEmpty() || showTime
    val metadataGeometry = messageMetadataGeometry(
        hasReactions = message.reactions.isNotEmpty(),
        hasTimestamp = showTime,
    )
    val metadataReservePx = if (hasMetadata) metadataGeometry.reservePx else 0
    val showActionsLabel = stringResource(R.string.show_message_actions)
    val selectedState = stringResource(
        if (selected) R.string.selection_state_selected else R.string.selection_state_not_selected,
    )
    val accessibilityActions = MessageActionPolicy.available(
        message,
        profile.id,
        speechActionState,
    ).map { action ->
        CustomAccessibilityAction(actionLabel(action)) {
            onAccessibilityAction(action)
            true
        }
    }
    val availableActions = MessageActionPolicy.available(message, profile.id, speechActionState)
    val failedOutgoing = outgoing && message.deliveryState == MessageDeliveryState.Failed
    val retryLabel = stringResource(R.string.not_delivered_retry)
    val canSwipeReply = !contextPreview && !isSelectionMode && MessageAction.Reply in availableActions
    val swipeState = rememberDraggableState { physicalDelta ->
        val semanticDelta = physicalDelta * directionMultiplier
        rawSwipeDistance = (rawSwipeDistance + semanticDelta).coerceAtLeast(0f)
        displayedSwipeDistance = if (rawSwipeDistance <= replyThresholdPx) {
            rawSwipeDistance
        } else {
            replyThresholdPx + (rawSwipeDistance - replyThresholdPx) * 0.25f
        }.coerceAtMost(replyMaximumPx)
        val nowReady = rawSwipeDistance >= replyThresholdPx
        if (nowReady && !swipeReady) {
            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
        swipeReady = nowReady
    }
    val swipeModifier = Modifier.draggable(
        state = swipeState,
        orientation = Orientation.Horizontal,
        enabled = canSwipeReply,
        onDragStopped = {
            if (swipeReady) onSwipeReply()
            val returnAnimation = Animatable(displayedSwipeDistance)
            returnAnimation.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            ) {
                displayedSwipeDistance = value
            }
            rawSwipeDistance = 0f
            displayedSwipeDistance = 0f
            swipeReady = false
        },
    )
    val interactionModifier = if (contextPreview) {
        Modifier
    } else if (isSelectionMode) {
        Modifier.toggleable(
            value = selected,
            role = Role.Checkbox,
            onValueChange = { onToggleSelection() },
        )
    } else {
        Modifier.combinedClickable(
            enabled = !message.isDeleted,
            onClickLabel = if (failedOutgoing) retryLabel else null,
            onClick = {
                if (failedOutgoing) onRetry()
            },
            onLongClickLabel = showActionsLabel,
            onLongClick = {
                if (!message.isDeleted) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onShowActions()
                }
            },
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(searchAlpha)
            .testTag("conversation.message.${message.id}")
            .padding(top = verticalPadding)
            .background(
                color = if (isSelectionMode && selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                } else {
                    Color.Transparent
                },
            )
            .then(interactionModifier)
            .then(swipeModifier)
            .onGloballyPositioned { onPositioned(it.boundsInWindow()) }
            .semantics {
                if (isSelectionMode) stateDescription = selectedState
                searchPosition?.let { stateDescription = it }
                customActions = accessibilityActions
            },
    ) {
        if (canSwipeReply && displayedSwipeDistance > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(48.dp)
                    .graphicsLayer {
                        alpha = (displayedSwipeDistance / replyThresholdPx).coerceIn(0f, 1f)
                        scaleX = alpha
                        scaleY = alpha
                    }
                    .testTag("conversation.message.swipeReply.${message.id}"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_reply),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = displayedSwipeDistance * directionMultiplier },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .testTag("conversation.selection.control.${message.id}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Checkbox(checked = selected, onCheckedChange = null)
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    if (chat.isGroup && !outgoing) {
                        if (item.endsCluster) {
                            GroupAuthorAvatar(
                                authorName,
                                author,
                                Modifier
                                    .offset { IntOffset(0, -metadataReservePx) }
                                    .size(30.dp)
                                    .testTag("conversation.message.avatar.${message.id}"),
                            )
                        } else {
                            Spacer(Modifier.width(30.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Column(
                        horizontalAlignment = if (outgoing) Alignment.End else Alignment.Start,
                        modifier = Modifier.widthIn(max = 340.dp),
                    ) {
                        if (chat.isGroup && !outgoing && item.startsCluster) {
                            SearchHighlightedText(
                                text = authorName,
                                query = searchQuery,
                                modifier = Modifier
                                    .padding(start = 12.dp, bottom = 3.dp)
                                    .testTag("conversation.message.author.${message.id}"),
                                color = groupAuthorColor(author?.publicKey ?: message.authorId),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        MessageBubbleWithMetadata(
                            profile = profile,
                            message = message,
                            outgoing = outgoing,
                            item = item,
                            authorName = authorName,
                            onOpenMedia = onOpenMedia,
                            searchQuery = searchQuery,
                            readAloudController = readAloudController,
                            voiceTranscript = if (speechActionState.transcriptAvailable) {
                                message.attachments
                                    .firstOrNull { it.kind == MessageAttachmentKind.Voice }
                                    ?.transcript
                                    ?: VoiceMessageFixture.transcript
                            } else {
                                null
                            },
                            voiceTranscriptVisible = speechActionState.transcriptVisible,
                            showTime = showTime,
                            onReaction = onReaction,
                            onShowActions = onShowActions,
                            trimInvisibleReactionTarget = contextPreview,
                            modifier = Modifier.widthIn(max = 340.dp),
                        )
                    }
                }
            }
        }
    }
}

private val ReactionVisibleOverlap = 9.dp
private val ReactionPillMinimumHeight = 23.dp
private val ReactionPillMinimumWidth = 31.dp
private val ReactionPillSpacing = 3.dp
private val ReactionContentSpacing = 2.dp
private val ReactionEmojiFontMetricAllowance = 2.sp
private val MessageMetadataTargetHeight = 48.dp
private val TimestampTopGap = 2.dp
private const val MaximumVisibleReactionTypes = 4

private data class MessageMetadataGeometry(
    val overlapPx: Int,
    val reservePx: Int,
    val invisibleTargetBottomPx: Int,
    val timestampVerticalOffsetPx: Int,
    val timestampTopGapPx: Int,
)

@Composable
private fun messageMetadataGeometry(
    hasReactions: Boolean,
    hasTimestamp: Boolean,
): MessageMetadataGeometry {
    val density = LocalDensity.current
    val targetHeightPx = with(density) { MessageMetadataTargetHeight.roundToPx() }
    val timestampHeightPx = with(density) {
        MaterialTheme.typography.labelSmall.lineHeight.toPx().roundToInt()
    }
    val timestampTopGapPx = with(density) { TimestampTopGap.roundToPx() }
    val overlapPx: Int
    val reservePx: Int
    val invisibleTargetBottomPx: Int
    val timestampVerticalOffsetPx: Int
    if (hasReactions) {
        val minimumPillHeightPx = with(density) { ReactionPillMinimumHeight.roundToPx() }
        val textHeightPx = with(density) {
            (
                MaterialTheme.typography.labelMedium.lineHeight.toPx() +
                    ReactionEmojiFontMetricAllowance.toPx()
            ).roundToInt()
        }
        val pillHeightPx = maxOf(minimumPillHeightPx, textHeightPx)
        val targetInsetPx = ((targetHeightPx - pillHeightPx).coerceAtLeast(0)) / 2
        overlapPx = targetInsetPx + with(density) { ReactionVisibleOverlap.roundToPx() }
        val timestampBaseTopPx = ((targetHeightPx - timestampHeightPx).coerceAtLeast(0)) / 2
        timestampVerticalOffsetPx = overlapPx + timestampTopGapPx - timestampBaseTopPx
        val timestampBottomPx = overlapPx + timestampTopGapPx + timestampHeightPx
        val pillBottomPx = targetInsetPx + pillHeightPx
        val visibleMetadataBottomPx = if (hasTimestamp) {
            maxOf(timestampBottomPx, pillBottomPx)
        } else {
            pillBottomPx
        }
        reservePx = maxOf(
            (targetHeightPx - overlapPx).coerceAtLeast(0),
            if (hasTimestamp) timestampTopGapPx + timestampHeightPx else 0,
        )
        invisibleTargetBottomPx = (targetHeightPx - visibleMetadataBottomPx).coerceAtLeast(0)
    } else {
        overlapPx = 0
        reservePx = timestampTopGapPx + timestampHeightPx
        invisibleTargetBottomPx = 0
        timestampVerticalOffsetPx = 0
    }
    return MessageMetadataGeometry(
        overlapPx = overlapPx,
        reservePx = reservePx,
        invisibleTargetBottomPx = invisibleTargetBottomPx,
        timestampVerticalOffsetPx = timestampVerticalOffsetPx,
        timestampTopGapPx = timestampTopGapPx,
    )
}

@Composable
private fun MessageBubbleWithMetadata(
    profile: Profile,
    message: ChatMessage,
    outgoing: Boolean,
    item: ConversationItem.MessageItem,
    authorName: String,
    onOpenMedia: (ConversationMediaKey) -> Unit,
    searchQuery: String,
    readAloudController: ReadAloudController,
    voiceTranscript: String?,
    voiceTranscriptVisible: Boolean,
    showTime: Boolean,
    onReaction: (String) -> Unit,
    onShowActions: () -> Unit,
    trimInvisibleReactionTarget: Boolean,
    modifier: Modifier = Modifier,
) {
    val hasReactions = message.reactions.isNotEmpty()
    val hasMetadata = hasReactions || showTime
    val metadataGeometry = messageMetadataGeometry(
        hasReactions = hasReactions,
        hasTimestamp = showTime,
    )
    val overlapPx = metadataGeometry.overlapPx
    val contextTrimPx = if (trimInvisibleReactionTarget && hasReactions) {
        metadataGeometry.invisibleTargetBottomPx
    } else {
        0
    }

    SubcomposeLayout(modifier = modifier) { constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        if (!hasMetadata) {
            val bubble = subcompose("bubble") {
                MessageBubble(
                    profile = profile,
                    message = message,
                    outgoing = outgoing,
                    item = item,
                    authorName = authorName,
                    onOpenMedia = onOpenMedia,
                    searchQuery = searchQuery,
                    readAloudController = readAloudController,
                    voiceTranscript = voiceTranscript,
                    voiceTranscriptVisible = voiceTranscriptVisible,
                )
            }.single().measure(looseConstraints)
            return@SubcomposeLayout layout(bubble.width, bubble.height) {
                bubble.placeRelative(0, 0)
            }
        }

        val candidateLimits = if (hasReactions) {
            MaximumVisibleReactionTypes downTo 0
        } else {
            0..0
        }
        val metadataMeasureConstraints = looseConstraints.copy(maxWidth = Constraints.Infinity)
        var selectedLimit = 0
        var selectedMetadataWidth = 0
        for (maximumReactionPills in candidateLimits) {
            val summary = ReactionCatalog.summary(
                message.reactions,
                profile.id,
                maximumReactionPills,
            )
            val candidate = subcompose("metadata.measure.$maximumReactionPills") {
                MessageMetadataContent(
                    messageId = message.id,
                    timeLabel = message.timeLabel,
                    outgoing = outgoing,
                    deliveryState = message.deliveryState,
                    showTime = showTime,
                    summary = summary,
                    fillWidth = false,
                    measurementOnly = true,
                    timestampVerticalOffsetPx = metadataGeometry.timestampVerticalOffsetPx,
                    onReaction = onReaction,
                    onShowActions = onShowActions,
                )
            }.single().measure(metadataMeasureConstraints)
            selectedLimit = maximumReactionPills
            selectedMetadataWidth = candidate.width
            if (candidate.width <= constraints.maxWidth || maximumReactionPills == 0) break
        }

        val requiredBubbleWidth = selectedMetadataWidth.coerceAtMost(constraints.maxWidth)
        val bubble = subcompose("bubble") {
            MessageBubble(
                profile = profile,
                message = message,
                outgoing = outgoing,
                item = item,
                authorName = authorName,
                onOpenMedia = onOpenMedia,
                searchQuery = searchQuery,
                readAloudController = readAloudController,
                voiceTranscript = voiceTranscript,
                voiceTranscriptVisible = voiceTranscriptVisible,
            )
        }.single().measure(
            looseConstraints.copy(
                minWidth = maxOf(looseConstraints.minWidth, requiredBubbleWidth),
            ),
        )
        val width = bubble.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val summary = ReactionCatalog.summary(message.reactions, profile.id, selectedLimit)
        val metadata = subcompose("metadata.final.$selectedLimit") {
            MessageMetadataContent(
                messageId = message.id,
                timeLabel = message.timeLabel,
                outgoing = outgoing,
                deliveryState = message.deliveryState,
                showTime = showTime,
                summary = summary,
                fillWidth = true,
                timestampVerticalOffsetPx = metadataGeometry.timestampVerticalOffsetPx,
                onReaction = onReaction,
                onShowActions = onShowActions,
            )
        }.single().measure(
            Constraints(
                minWidth = width,
                maxWidth = width,
                minHeight = 0,
                maxHeight = constraints.maxHeight,
            ),
        )
        val reportedReservePx = if (hasReactions) {
            (metadata.height - overlapPx - contextTrimPx).coerceAtLeast(0)
        } else {
            metadataGeometry.timestampTopGapPx + metadata.height
        }
        val metadataY = if (hasReactions) {
            bubble.height - overlapPx
        } else {
            bubble.height + metadataGeometry.timestampTopGapPx
        }
        val height = (bubble.height + reportedReservePx)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            bubble.placeRelative(0, 0)
            metadata.placeRelative(0, metadataY)
        }
    }
}

@Composable
private fun MessageBubble(
    profile: Profile,
    message: ChatMessage,
    outgoing: Boolean,
    item: ConversationItem.MessageItem,
    authorName: String,
    onOpenMedia: (ConversationMediaKey) -> Unit,
    searchQuery: String,
    readAloudController: ReadAloudController,
    voiceTranscript: String?,
    voiceTranscriptVisible: Boolean,
) {
    val text = message.visibleText(profile.id)
    val description = buildString {
        append(authorName)
        if (text.isNotBlank()) append(", $text")
        message.attachments.forEach { append(", ${it.label}") }
        append(", ${message.timeLabel}")
        message.reactions.forEach { append(", ${it.emoji}, ${it.personIds.size}") }
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (outgoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (outgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .testTag("conversation.message.bubble.${message.id}")
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            if (message.replyToMessageId != null) {
                ReplyQuote(
                    profile = profile,
                    item = item,
                    outgoing = outgoing,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                TimelineAttachmentContent(
                    attachments = message.attachments,
                    outgoing = outgoing,
                    messageId = message.id,
                    onOpenMedia = onOpenMedia,
                    searchQuery = searchQuery,
                    voiceTranscript = voiceTranscript,
                    voiceTranscriptVisible = voiceTranscriptVisible,
                )
                if (message.attachments.any { it.voiceFormat == VoiceMessageFormat.Both } && text.isNotBlank()) {
                    Text(
                        stringResource(R.string.transcribed),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (text.isNotBlank()) {
                    if (searchQuery.isNotBlank() && message.deletionState == MessageDeletionState.None) {
                        SearchHighlightedText(
                            text = text,
                            query = searchQuery,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        Text(
                            text = text,
                            fontStyle = if (message.deletionState == MessageDeletionState.None) {
                                FontStyle.Normal
                            } else {
                                FontStyle.Italic
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    ReadAloudProgress(message.id, readAloudController)
                }
            }
        }
    }
}

@Composable
private fun ReplyQuote(
    profile: Profile,
    item: ConversationItem.MessageItem,
    outgoing: Boolean,
    modifier: Modifier = Modifier,
) {
    val source = item.resolvedReply
    val author = source?.let { message ->
        if (message.authorId == profile.id) stringResource(R.string.you) else {
            profile.people.firstOrNull { it.id == message.authorId }?.name
                ?: stringResource(R.string.unknown_person)
        }
    }
    val body = when {
        item.hasUnavailableReply -> stringResource(R.string.original_message_unavailable)
        source != null -> source.visibleText(profile.id).ifBlank { source.attachments.firstOrNull()?.label.orEmpty() }
        else -> stringResource(R.string.original_message_unavailable)
    }
    val content = if (outgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val secondary = if (outgoing) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    ConversationQuoteBlock(
        author = author ?: stringResource(R.string.original_message_unavailable),
        excerpt = body,
        containerColor = if (outgoing) {
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = content,
        secondaryColor = secondary,
        accentColor = content,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth().padding(bottom = 6.dp),
        testTagPrefix = "conversation.message.quote",
    )
}

@Composable
private fun MessageMetadataContent(
    messageId: String,
    timeLabel: String,
    outgoing: Boolean,
    deliveryState: MessageDeliveryState,
    showTime: Boolean,
    summary: List<ReactionCatalog.SummaryItem>,
    fillWidth: Boolean,
    measurementOnly: Boolean = false,
    timestampVerticalOffsetPx: Int,
    onReaction: (String) -> Unit,
    onShowActions: () -> Unit,
) {
    val widthModifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier
    val arrangement = when {
        summary.isNotEmpty() && showTime -> Arrangement.SpaceBetween
        outgoing -> Arrangement.End
        else -> Arrangement.Start
    }
    Row(
        modifier = widthModifier
            .padding(horizontal = 12.dp)
            .then(
                if (measurementOnly) {
                    Modifier.clearAndSetSemantics { }
                } else {
                    Modifier.testTag("conversation.message.metadata.$messageId")
                },
            ),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!outgoing && showTime) {
            MessageTime(
                messageId = messageId,
                timeLabel = timeLabel,
                outgoing = false,
                deliveryState = deliveryState,
                testTagEnabled = !measurementOnly,
                verticalOffsetPx = timestampVerticalOffsetPx,
            )
        }
        if (!outgoing && showTime && summary.isNotEmpty()) Spacer(Modifier.width(4.dp))
        if (summary.isNotEmpty()) {
            ReactionRow(
                messageId = messageId,
                summary = summary,
                interactive = !measurementOnly,
                onReaction = onReaction,
                onShowActions = onShowActions,
            )
        }
        if (outgoing && showTime && summary.isNotEmpty()) Spacer(Modifier.width(4.dp))
        if (outgoing && showTime) {
            MessageTime(
                messageId = messageId,
                timeLabel = timeLabel,
                outgoing = true,
                deliveryState = deliveryState,
                testTagEnabled = !measurementOnly,
                verticalOffsetPx = timestampVerticalOffsetPx,
            )
        }
    }
}

@Composable
private fun MessageTime(
    messageId: String,
    timeLabel: String,
    outgoing: Boolean,
    deliveryState: MessageDeliveryState,
    testTagEnabled: Boolean,
    verticalOffsetPx: Int,
) {
    val failed = outgoing && deliveryState == MessageDeliveryState.Failed
    val failedLabel = if (failed) stringResource(R.string.not_delivered_retry) else null
    val timestampColor = MaterialTheme.colorScheme.outline
    val deliveryLabel = if (outgoing) {
        stringResource(
            when (deliveryState) {
                MessageDeliveryState.Sending -> R.string.sending
                MessageDeliveryState.Sent -> R.string.sent
                MessageDeliveryState.Failed -> R.string.not_delivered
            },
        )
    } else {
        null
    }
    Row(
        modifier = Modifier
            .offset { IntOffset(0, verticalOffsetPx) }
            .then(
                if (testTagEnabled) {
                    Modifier.testTag("conversation.message.time.$messageId")
                } else {
                    Modifier
                },
            )
            .semantics(mergeDescendants = true) {
                contentDescription = failedLabel
                    ?: listOfNotNull(deliveryLabel, timeLabel).joinToString(", ")
            },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (outgoing) {
            when (deliveryState) {
                MessageDeliveryState.Sending -> CircularProgressIndicator(
                    modifier = Modifier
                        .size(14.dp)
                        .then(
                            if (testTagEnabled) {
                                Modifier.testTag("conversation.message.delivery.$messageId")
                            } else {
                                Modifier
                            },
                        ),
                    color = timestampColor,
                    strokeWidth = 1.5.dp,
                )

                MessageDeliveryState.Sent -> Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(timestampColor)
                        .then(
                            if (testTagEnabled) {
                                Modifier.testTag("conversation.message.delivery.$messageId")
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier
                            .size(10.dp)
                            .then(
                                if (testTagEnabled) {
                                    Modifier.testTag("conversation.message.delivery.icon.$messageId")
                                } else {
                                    Modifier
                                },
                            ),
                        tint = MaterialTheme.colorScheme.surface,
                    )
                }

                MessageDeliveryState.Failed -> Icon(
                    painter = painterResource(R.drawable.ic_warning),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .then(
                            if (testTagEnabled) {
                                Modifier.testTag("conversation.message.delivery.$messageId")
                            } else {
                                Modifier
                            },
                        ),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        Text(
            text = failedLabel ?: timeLabel,
            color = if (failed) {
                MaterialTheme.colorScheme.error
            } else {
                timestampColor
            },
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ReactionRow(
    messageId: String,
    summary: List<ReactionCatalog.SummaryItem>,
    interactive: Boolean,
    onReaction: (String) -> Unit,
    onShowActions: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val showActionsLabel = stringResource(R.string.show_message_actions)
    Row(
        horizontalArrangement = Arrangement.spacedBy(ReactionPillSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        summary.forEachIndexed { index, reaction ->
            val description = if (reaction.emoji != null) {
                pluralStringResource(
                    R.plurals.reaction_count,
                    reaction.personCount,
                    reaction.emoji,
                    reaction.personCount,
                )
            } else {
                pluralStringResource(
                    R.plurals.more_reaction_types,
                    reaction.omittedTypeCount,
                    reaction.personCount,
                    reaction.omittedTypeCount,
                )
            }
            val selectedState = stringResource(
                if (reaction.selected) R.string.selection_state_selected else R.string.selection_state_not_selected,
            )
            Box(
                modifier = Modifier
                    .heightIn(min = MessageMetadataTargetHeight)
                    .then(
                        if (interactive) {
                            Modifier
                                .testTag("conversation.reaction.$messageId.$index")
                                .combinedClickable(
                                    onClick = { reaction.emoji?.let(onReaction) ?: onShowActions() },
                                    onLongClickLabel = showActionsLabel,
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onShowActions()
                                    },
                                )
                                .semantics {
                                    contentDescription = description
                                    stateDescription = selectedState
                                }
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = if (interactive) {
                        Modifier
                            .heightIn(min = ReactionPillMinimumHeight)
                            .widthIn(min = ReactionPillMinimumWidth)
                            .testTag("conversation.reaction.pill.$messageId.$index")
                    } else {
                        Modifier
                            .heightIn(min = ReactionPillMinimumHeight)
                            .widthIn(min = ReactionPillMinimumWidth)
                    }
                        .clip(CircleShape)
                        .background(
                            if (reaction.selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        )
                        .border(
                            1.dp,
                            if (reaction.selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(ReactionContentSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = reaction.emoji ?: "+${reaction.omittedTypeCount}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (reaction.emoji != null && reaction.personCount > 1) {
                            Text(
                                text = reaction.personCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupAuthorAvatar(name: String, person: Person?, modifier: Modifier = Modifier) {
    if (person != null && person.avatar != dev.ipf.whitenoise.model.ProfileAvatar.Monogram) {
        ProfileAvatar(name, person.avatar, modifier, contentDescription = null)
    } else {
        Box(
            modifier = modifier.clip(CircleShape).background(groupAuthorColor(person?.publicKey ?: name)),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun groupAuthorColor(seed: String): Color {
    val light = listOf(
        0xFFB3261E, 0xFF9A4600, 0xFF2E6B2F, 0xFF006B5F, 0xFF00639A,
        0xFF4F5AA8, 0xFF7D5260, 0xFF984061, 0xFF765849,
    )
    val dark = listOf(
        0xFFFFB4AB, 0xFFFFB77D, 0xFFA8D5A2, 0xFF53DBC7, 0xFF8ECBFF,
        0xFFBEC2FF, 0xFFFFB0C8, 0xFFFFB0C8, 0xFFE8BEAA,
    )
    val palette = if (isSystemInDarkTheme()) dark else light
    return Color(palette[seed.sumOf(Char::code) % palette.size])
}

@Composable
private fun ConversationBottomBar(
    profile: Profile,
    chat: Chat,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCheckRelays: () -> Unit,
) {
    val availability = chat.composerAvailability(profile)
    when (availability) {
        ComposerAvailability.PendingInvitation -> InvitationActions(chat, onDecline, onAccept)
        ComposerAvailability.Available -> Unit
        ComposerAvailability.Left -> ConversationStatus(
            if (chat.isGroup) stringResource(R.string.left_group_status) else stringResource(R.string.left_chat_status),
        )
        ComposerAvailability.Removed -> ConversationStatus(stringResource(R.string.removed_group_status))
        ComposerAvailability.Blocked -> ConversationStatus(
            stringResource(R.string.blocked_chat_detail),
            title = stringResource(R.string.messaging_unavailable),
        )
        ComposerAvailability.MissingRelays -> ConversationRecovery(
            title = stringResource(R.string.chat_relays_required_title),
            detail = stringResource(R.string.chat_relays_missing_detail),
            actionLabel = stringResource(R.string.check_chat_relays),
            onAction = onCheckRelays,
        )
    }
}

@Composable
private fun InvitationActions(chat: Chat, onDecline: () -> Unit, onAccept: () -> Unit) {
    LifecycleBottomSurface {
        Text(
            stringResource(
                R.string.invited_to_chat_by,
                chat.invitationInviterName ?: stringResource(R.string.someone),
            ),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        ) {
            WhiteNoiseOutlinedButton(
                onClick = onDecline,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.decline), color = MaterialTheme.colorScheme.error)
            }
            WhiteNoiseButton(
                onClick = onAccept,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.accept))
            }
        }
    }
}

@Composable
private fun ConversationStatus(text: String, title: String? = null) {
    LifecycleBottomSurface {
        title?.let {
            Text(
                it,
                modifier = Modifier.align(Alignment.CenterHorizontally).semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Text(
            text,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ConversationRecovery(
    title: String,
    detail: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    LifecycleBottomSurface {
        Text(
            title,
            modifier = Modifier.align(Alignment.CenterHorizontally).semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        WhiteNoiseButton(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun LifecycleBottomSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(WhiteNoiseSpacing.CompactScreenMargin),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                content = content,
            )
        }
    }
}

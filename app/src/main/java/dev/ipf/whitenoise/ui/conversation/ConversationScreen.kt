package dev.ipf.whitenoise.ui.conversation

import dev.ipf.whitenoise.model.ConversationHistory
import dev.ipf.whitenoise.model.ConversationReading
import dev.ipf.whitenoise.model.HistoryOperation
import dev.ipf.whitenoise.model.HistoryScenario
import dev.ipf.whitenoise.model.HistoryPhase
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.material3.FilledTonalButton

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ripple
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold as Scaffold
import dev.ipf.whitenoise.ui.theme.colorFromOpaqueArgb
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
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
import androidx.compose.ui.platform.LocalView
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
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
import dev.ipf.whitenoise.model.plainVisibleText
import dev.ipf.whitenoise.model.visibleText
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.SignalEmoji
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseCompactSearchField
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuGroup
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import dev.ipf.whitenoise.ui.components.WhiteNoiseOutlinedButton
import dev.ipf.whitenoise.ui.components.drawableResource
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

private val FocusedMessageBackdropBlurRadius = 24.dp
private const val FocusedMessageBackdropSurfaceAlpha = 0.88f
private val FocusedReactionRailInset = 4.dp
private val FocusedReactionItemSpacing = 4.dp
private val FocusedReactionStateLayerSize = 40.dp
private val FocusedReactionSelectedFillSize = 36.dp
private val FocusedReactionEmojiSize = 28.dp
private val FocusedOverlayShadowSafeInset = 8.dp
private const val PinnedDayHeaderSurfaceAlpha = 0.82f
private val ReplySwipeThreshold = 64.dp
private val ReplySwipeMaximum = 96.dp
private val ReplySwipeIconTravel = 10.dp
private val ReplySwipeIconTargetSize = 48.dp
private val ReplySwipeIconSize = 24.dp
private const val ReplySwipeIconRevealStart = 0.05f
private const val ReplySwipeIconReadyScale = 1.2f
private const val ReplySwipeIconPulseScale = 1.5f

private fun Modifier.interceptBubbleLongPress(
    enabled: Boolean,
    onLongPress: () -> Unit,
): Modifier = if (!enabled) {
    this
} else {
    pointerInput(onLongPress) {
        awaitEachGesture {
            val pass = PointerEventPass.Initial
            val down = awaitFirstDown(requireUnconsumed = false, pass = pass)
            val endedBeforeLongPress = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val event = awaitPointerEvent(pass)
                    val change = event.changes.firstOrNull { it.id == down.id }
                        ?: return@withTimeoutOrNull true
                    if (!change.pressed) return@withTimeoutOrNull true
                    if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                        return@withTimeoutOrNull true
                    }
                }
            }
            if (endedBeforeLongPress == null) {
                onLongPress()
                do {
                    val event = awaitPointerEvent(pass)
                    event.changes.forEach { it.consume() }
                } while (event.changes.any { it.pressed })
            }
        }
    }
}

internal fun resistedReplySwipeDistance(
    rawDistance: Float,
    threshold: Float,
    maximum: Float,
): Float {
    val distance = rawDistance.coerceAtLeast(0f)
    if (distance <= threshold) return distance
    if (threshold <= 0f || maximum <= threshold) return maximum.coerceAtLeast(0f)

    val overdrag = distance - threshold
    val resistedRange = maximum - threshold
    return threshold + (resistedRange * overdrag / (overdrag + threshold))
}

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
    forwardProfiles: List<Profile> = listOf(profile),
    onForwardToProfile: ((Set<String>, String, List<String>) -> Boolean)? = null,
    onForwardMediaToProfile: ((ConversationMediaKey, String, List<String>, String) -> Boolean)? = null,
    onRetryMessageDeletion: (Long) -> Unit = {},
    onDismissMessageDeletion: (Long) -> Unit = {},
    onForwardMessages: (Set<String>, List<String>) -> Boolean = { _, _ -> false },
    onForwardMedia: (ConversationMediaKey, List<String>, String) -> Boolean = { _, _, _ -> false },
    onOpenMessageDetails: (String) -> Unit = {},
    onOpenChatInfo: () -> Unit = {},
    onOpenPersonProfile: (String) -> Unit = {},
    onRetryNostrEvent: (messageId: String, referenceId: String, revision: Int) -> Unit = { _, _, _ -> },
    onOpenDeveloperTools: (() -> Unit)? = null,
    initialSearch: Boolean = false,
    initialMessageId: String? = null,
    notificationRequestId: Long? = null,
    onNotificationBoundaryCaptured: () -> Unit = {},
    searchRequestId: Long = 0,
    onHistoryScenario: (HistoryOperation) -> HistoryScenario = { HistoryScenario.Success },
    onMessagesVisible: (Set<String>) -> Unit = {},
    onReadThroughMention: (String) -> Unit = {},
    onEditMessage: (String, String, Int) -> Boolean = { _, _, _ -> false },
    onAdvanceMessageEdit: (String, Long) -> Unit = { _, _ -> },
    onRetryMessageEdit: (String) -> Unit = {},
    onDiscardMessageEdit: (String) -> Unit = {},
    onInterruptMessageEdits: () -> Unit = {},
) {
    DictationOriginHost(profile, chat)
    val retentionController = LocalRetention.current
    LaunchedEffect(profile.id, chat.id, retentionController?.example) { retentionController?.open(dev.ipf.whitenoise.model.GroupOwner(profile.id, chat.id)) }
    val groupStateController = LocalGroupLifecycle.current
    val groupRosterController = LocalGroupWork.current
    LaunchedEffect(profile.id, chat.id, groupStateController?.stateScenario, groupRosterController?.rosterScenario) {
        val owner = dev.ipf.whitenoise.model.GroupOwner(profile.id, chat.id)
        groupStateController?.open(owner)
        groupRosterController?.openRoster(owner)
    }
    val history = rememberConversationHistory(profile, chat)
    val readState = remember(chat, profile.id) {
        ConversationReading.reconcile(chat.readState ?: ConversationReading.initial(chat, profile.id), chat, profile.id)
    }
    val speechEntryUnreadIds = remember(profile.id, chat.id) { readState.unreadIds.toSet() }
    // History has captured the entry boundary above; committing now must not move that marker.
    dev.ipf.whitenoise.ui.settings.NotificationReadBoundary(notificationRequestId,profile.id,chat.id,onNotificationBoundaryCaptured)
    val items = remember(chat.timeline, history.windowIds, history.boundaryId, history.request, history.scanning, history.scanFailed) {
        buildList {
            fun control(id: String) { add(ConversationItem.NoticeItem(ChatTimelineEntry.Notice(id, ""))) }
            if (ConversationHistory.hasOlder(chat, history.windowIds) || history.request?.operation == HistoryOperation.Older) control("history.older")
            ConversationProjection.items(chat, history.windowIds).forEach { item ->
                if (item is ConversationItem.MessageItem && item.id == history.boundaryId) control("history.unread")
                add(item)
            }
            if (ConversationHistory.hasNewer(chat, history.windowIds) || history.request?.operation == HistoryOperation.Newer) control("history.newer")
        }
    }
    val listState = rememberLazyListState()
    val dayHeaderIndices = remember(items) {
        items.indices.filter { items[it] is ConversationItem.DayHeader }
    }
    val pinnedDayHeader by remember(items, dayHeaderIndices, listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            var topVisibleItemIndex: Int? = null
            var topVisibleItemOffset = Int.MAX_VALUE
            visibleItems.forEach { itemInfo ->
                if (
                    itemInfo.offset + itemInfo.size > layoutInfo.viewportStartOffset &&
                    itemInfo.offset < layoutInfo.viewportEndOffset &&
                    itemInfo.offset < topVisibleItemOffset
                ) {
                    topVisibleItemIndex = itemInfo.index
                    topVisibleItemOffset = itemInfo.offset
                }
            }
            pinnedConversationDayHeaderIndex(
                dayHeaderIndices = dayHeaderIndices,
                topVisibleItemIndex = topVisibleItemIndex,
                isHeaderVisible = { headerIndex ->
                    visibleItems.any { itemInfo ->
                        itemInfo.index == headerIndex &&
                            itemInfo.offset + itemInfo.size > layoutInfo.viewportStartOffset &&
                            itemInfo.offset < layoutInfo.viewportEndOffset
                    }
                },
            )?.let { items[it] as? ConversationItem.DayHeader }
        }
    }
    var showDeclineConfirmation by remember { mutableStateOf(false) }
    var viewerSelection by remember { mutableStateOf<ConversationMediaSelection?>(null) }
    var forwardMediaKey by remember { mutableStateOf<ConversationMediaKey?>(null) }
    var focusedMessageId by remember { mutableStateOf<String?>(null) }
    var highlightedMessageId by remember(chat.id) { mutableStateOf<String?>(null) }
    var isSelecting by remember { mutableStateOf(false) }
    var selectedMessageIds by remember { mutableStateOf(emptySet<String>()) }
    var deleteMessageIds by remember { mutableStateOf<Set<String>?>(null) }
    var deleteStartFailed by remember { mutableStateOf(false) }
    val operationCovered = LocalMessageOperationCover.current
    var forwardMessageIds by remember { mutableStateOf<Set<String>?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var emojiMessageId by remember { mutableStateOf<String?>(null) }
    var showConfigureReactions by remember { mutableStateOf(false) }
    var configureReactionSlot by remember { mutableStateOf<Int?>(null) }
    var configureDraft by remember(profile.quickReactions) { mutableStateOf(profile.quickReactions) }
    var isSearching by rememberSaveable(chat.id) { mutableStateOf(initialSearch) }
    var editMessageId by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var exportMessageId by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var exportSharing by rememberSaveable(profile.id, chat.id) { mutableStateOf(false) }
    var readerMessageId by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var historyMessageId by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var readerStartsSelection by rememberSaveable(profile.id, chat.id) { mutableStateOf(false) }
    var searchQuery by rememberSaveable(chat.id) { mutableStateOf("") }
    var pinnedSearchMessageId by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var preSearchAnchor by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var preSearchOffset by rememberSaveable(profile.id, chat.id) { mutableIntStateOf(0) }
    var handledSearchRequest by rememberSaveable(profile.id, chat.id) { mutableStateOf(0L) }
    var pendingInitialMessageId by rememberSaveable(profile.id, chat.id, initialMessageId, notificationRequestId) {
        mutableStateOf(initialMessageId ?: history.boundaryId)
    }
    var initialViewportSettled by rememberSaveable(chat.id) { mutableStateOf(false) }
    var sentLocationTarget by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var pendingEndSettlement by remember(chat.id) { mutableStateOf(false) }
    var compactComposerHeightPx by remember(chat.id) { mutableIntStateOf(0) }
    var composerOverlayActive by remember(chat.id) { mutableStateOf(false) }
    var composerPresentationActive by remember(chat.id) { mutableStateOf(false) }
    var composerTravelPx by remember(chat.id) { mutableFloatStateOf(0f) }
    var pushTimelineWithComposer by remember(chat.id) { mutableStateOf(false) }
    val messageBounds = remember(chat.id) { mutableStateMapOf<String, Rect>() }
    val context = LocalContext.current
    val readAloudController = rememberReadAloudController()
    val standaloneSpeechProfile = rememberUpdatedState(profile.copy(chats = profile.chats.filterNot { it.id == chat.id } + chat))
    if (LocalReadAloudController.current == null) androidx.compose.runtime.SideEffect {
        readAloudController.profile = { standaloneSpeechProfile.value }; readAloudController.reconcile()
    }
    var speechEntryOpened by remember(readAloudController, profile.id, chat.id) { mutableStateOf(false) }
    LaunchedEffect(chat, readAloudController.ready, readAloudController.isForeground, profile.settings.speech, initialViewportSettled) {
        if (readAloudController.ready && !speechEntryOpened) {
            readAloudController.openChat(profile.id, chat, speechEntryUnreadIds); speechEntryOpened = true
        }
        if (initialViewportSettled) readAloudController.observeChat(chat)
    }
    val speechSession = readAloudController.session?.takeIf { it.owner.profileId == profile.id && it.owner.chatId == chat.id }
    val attachmentReaderPresented = LocalAttachmentAccess.current.presented
    LaunchedEffect(attachmentReaderPresented) { if (attachmentReaderPresented) readAloudController.stop() }
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
    ConversationHistoryScan(history, profile, chat, searchQuery.takeIf { isSearching }.orEmpty(), onHistoryScenario)
    val searchResults = history.scan ?: remember(chat.timeline, history.windowIds, profile.people, searchQuery) {
        ConversationSearch.results(chat.copy(timeline = ConversationHistory.loaded(chat, history.windowIds)), profile, searchQuery)
    }
    val searchResultIndex = ConversationHistory.searchCursor(searchResults, pinnedSearchMessageId)
    LaunchedEffect(searchResults, history.scanning) {
        if (pinnedSearchMessageId == null || (!history.scanning && searchResults.none { it.messageId == pinnedSearchMessageId })) {
            pinnedSearchMessageId = searchResults.firstOrNull()?.messageId
        }
    }
    val searchResultMessageIds = remember(searchResults) {
        searchResults.mapTo(mutableSetOf(), dev.ipf.whitenoise.model.ConversationSearchResult::messageId)
    }
    val currentSearchMessageId = pinnedSearchMessageId?.takeIf { id -> searchResults.any { it.messageId == id } }

    fun settleAfterNextTimelineItem(previousCount: Int) {
        coroutineScope.launch {
            snapshotFlow { listState.layoutInfo.totalItemsCount }
                .first { it > previousCount }
            withFrameNanos { }
            val lastIndex = listState.layoutInfo.totalItemsCount - 1
            if (lastIndex >= 0) listState.scrollToItem(lastIndex)
        }
    }

    fun startSearch() {
        val anchor = listState.layoutInfo.visibleItemsInfo.firstOrNull { info -> items.getOrNull(info.index) is ConversationItem.MessageItem }
        preSearchAnchor = anchor?.let { items[it.index].id }
        preSearchOffset = anchor?.let { -it.offset } ?: 0
        history.cancel()
        isSearching = true
    }
    fun closeSearch() {
        isSearching = false
        searchQuery = ""
        pinnedSearchMessageId = null
        history.cancel()
        preSearchAnchor?.let { history.target(chat, it, onHistoryScenario(HistoryOperation.Target), offset = preSearchOffset, highlight = false) }
    }
    LaunchedEffect(searchRequestId) {
        if (searchRequestId > handledSearchRequest) { handledSearchRequest = searchRequestId; startSearch() }
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

    fun openReplyTarget(messageId: String) {
        history.target(chat, messageId, onHistoryScenario(HistoryOperation.Target))
    }

    fun resolvedVoiceTranscript(message: ChatMessage): String? =
        message.attachments.firstOrNull { it.kind == MessageAttachmentKind.Voice }?.transcript
            ?: localVoiceTranscripts[message.id]

    fun speechActionState(message: ChatMessage): MessageSpeechActionState = MessageSpeechActionState(
        transcriptAvailable = resolvedVoiceTranscript(message) != null,
        transcriptVisible = message.id in visibleVoiceTranscriptIds,
        reading = speechSession != null && readAloudController.activeMessageId == message.id,
        canReadAloud = readAloudController.ready,
    )

    BackHandler(enabled = isSearching, onBack = ::closeSearch)
    BackHandler(enabled = isSelecting) {
        isSelecting = false
        selectedMessageIds = emptySet()
    }

    fun handleAction(message: ChatMessage, action: MessageAction) {
        focusedMessageId = null
        if (action in setOf(MessageAction.Edit, MessageAction.EditHistory, MessageAction.OpenMessage, MessageAction.SelectText)) {
            history.cancel()
            pendingInitialMessageId = null
            initialViewportSettled = true
        }
        when (action) {
            MessageAction.RetrySend -> onRetry(message.id)
            MessageAction.Edit -> { readerMessageId = null; editMessageId = message.id }
            MessageAction.EditHistory -> { readerMessageId = null; historyMessageId = message.id }
            MessageAction.RetryEdit -> onRetryMessageEdit(message.id)
            MessageAction.DiscardEdit -> onDiscardMessageEdit(message.id)
            MessageAction.OpenMessage, MessageAction.SelectText -> {
                readerStartsSelection = action == MessageAction.SelectText
                readerMessageId = message.id
            }
            MessageAction.Reply -> beginReply(message.id)
            MessageAction.Share, MessageAction.SaveAttachments -> {
                exportMessageId = message.id; exportSharing = action == MessageAction.Share
            }
            MessageAction.Forward -> forwardMessageIds = setOf(message.id)
            MessageAction.Copy -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Message", message.plainVisibleText(profile.id)))
            }
            MessageAction.CopyMarkdown -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Message", message.text))
            }
            MessageAction.ReadAloud -> readAloudController.startConversation(profile, chat, message.id)
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
        } else if (target != null) {
            if (history.request == null && history.readyTarget == null) history.target(chat, target, onHistoryScenario(HistoryOperation.Target))
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
        if (!pendingEndSettlement || items.isEmpty() || (showsAvailableComposer && compactComposerHeightPx == 0)) {
            return@LaunchedEffect
        }
        withFrameNanos { }
        listState.scrollToItem(items.lastIndex)
        withFrameNanos { }
        listState.scrollToItem(items.lastIndex)
        pendingEndSettlement = false
    }
    LaunchedEffect(sentLocationTarget, chat.timeline) {
        val target = sentLocationTarget ?: return@LaunchedEffect
        if (chat.timeline.any { it.id == target }) {
            history.target(chat, target, dev.ipf.whitenoise.model.HistoryScenario.Success, highlight = false)
            sentLocationTarget = null
        }
    }
    LaunchedEffect(currentSearchMessageId) {
        val messageId = currentSearchMessageId ?: return@LaunchedEffect
        if (isSearching) history.target(chat, messageId, onHistoryScenario(HistoryOperation.Target))
    }
    val readyTarget = history.readyTarget
    LaunchedEffect(readyTarget?.id) {
        val target = readyTarget ?: return@LaunchedEffect
        val index = items.indexOfFirst { it is ConversationItem.MessageItem && it.id == target.targetId && !it.message.isDeleted }
        if (index < 0) return@LaunchedEffect
        listState.scrollToItem(index, target.scrollOffset)
        repeat(2) { withFrameNanos { } }
        if (history.readyTarget?.id != target.id) return@LaunchedEffect
        if (listState.layoutInfo.visibleItemsInfo.none { it.key == target.targetId }) {
            history.request = target.copy(phase = HistoryPhase.Failed)
            history.readyTarget = null
            return@LaunchedEffect
        }
        pendingInitialMessageId = null
        initialViewportSettled = true
        if (history.jump.pendingId == target.targetId) history.jump = history.jump.copy(pendingId = null, stackActive = true)
        if (target.markThrough) onReadThroughMention(target.targetId!!)
        if (target.highlight) {
            highlightedMessageId = target.targetId
            delay(1_400)
            if (highlightedMessageId == target.targetId) highlightedMessageId = null
        }
        if (history.readyTarget?.id == target.id) history.readyTarget = null
    }
    val tailWasLoaded = ConversationProjection.orderedEntries(chat).lastOrNull { it.id in history.observedIds }?.id in history.windowIds
    val nearTail = initialViewportSettled && tailWasLoaded && !listState.canScrollForward
    LaunchedEffect(chat.timeline) {
        val follow = nearTail && !isSearching && !isSelecting && focusedMessageId == null
        val hadArrival = chat.timeline.any { it.id !in history.observedIds }
        history.reconcileArrivals(chat, profile.id, follow)
        if (hadArrival && follow) pendingEndSettlement = true
    }
    LaunchedEffect(readState, nearTail) { history.jump = ConversationReading.jump(history.jump, readState, chat, nearTail) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, profile.id, chat.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                onInterruptMessageEdits()
                // A cancelled entry target must not hold the restored viewport unsettled.
                pendingInitialMessageId = null
                initialViewportSettled = true
                pendingEndSettlement = false
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val currentItems by rememberUpdatedState(items)
    val currentVisibleCallback by rememberUpdatedState(onMessagesVisible)
    val canRead by rememberUpdatedState((speechSession?.following != true || speechSession.phase == dev.ipf.whitenoise.model.SpeechPhase.Completed) && initialViewportSettled && !isSearching && !isSelecting && focusedMessageId == null &&
        editMessageId == null && readerMessageId == null && historyMessageId == null && exportMessageId == null &&
        !attachmentReaderPresented && !operationCovered && viewerSelection == null && forwardMediaKey == null && forwardMessageIds == null && deleteMessageIds == null &&
        !showEmojiPicker && !showConfigureReactions && configureReactionSlot == null && !showDeclineConfirmation &&
        !composerPresentationActive && !composerOverlayActive && history.request == null && history.readyTarget == null)
    val relatedPx = with(density) { WhiteNoiseSpacing.Related.roundToPx() }
    LaunchedEffect(profile.id, chat.id, listState, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow {
                if (!canRead || listState.isScrollInProgress) emptySet() else {
                    val layout = listState.layoutInfo
                    val start = layout.viewportStartOffset
                    val end = layout.viewportEndOffset - (layout.afterContentPadding - relatedPx).coerceAtLeast(0)
                    layout.visibleItemsInfo.filter { info ->
                        val item = currentItems.getOrNull(info.index)
                        item is ConversationItem.MessageItem && !item.message.isDeleted &&
                            ConversationReading.actuallyVisible(info.offset, info.size, start, end)
                    }.mapNotNull { currentItems.getOrNull(it.index)?.id }.toSet()
                }
            }.distinctUntilChanged().collect { ids -> if (ids.isNotEmpty()) currentVisibleCallback(ids) }
        }
    }

    val pendingEdits = messages.mapNotNull { message -> message.editAttempt?.takeIf { it.phase == dev.ipf.whitenoise.model.MessageEditPhase.Pending }?.let { message.id to it.id } }
    LaunchedEffect(speechSession?.id, speechSession?.current?.item?.id, speechSession?.following,
        initialViewportSettled, readerMessageId != null, isSearching, isSelecting, focusedMessageId) {
        val speech = speechSession ?: return@LaunchedEffect
        if (!speech.following || speech.phase in setOf(dev.ipf.whitenoise.model.SpeechPhase.Unavailable, dev.ipf.whitenoise.model.SpeechPhase.Completed) ||
            !initialViewportSettled || isSearching || isSelecting || focusedMessageId != null) return@LaunchedEffect
        if (readerMessageId != null) readerMessageId = speech.current.item.id
        else history.target(chat, speech.current.item.id, HistoryScenario.Success, highlight = false)
    }
    LaunchedEffect(profile.id, chat.id, pendingEdits, lifecycle) {
        if (pendingEdits.isNotEmpty()) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(450)
            pendingEdits.forEach { (messageId, requestId) -> onAdvanceMessageEdit(messageId, requestId) }
        }
    }
    DisposableEffect(profile.id, chat.id) { onDispose { onInterruptMessageEdits() } }
    LaunchedEffect(messages) {
        val available = messages.filterNot { it.isDeleted }.map { it.id }.toSet()
        if (editMessageId !in available) editMessageId = null
        if (readerMessageId !in available) readerMessageId = null
        if (historyMessageId !in available) historyMessageId = null
        if (focusedMessageId !in available) focusedMessageId = null
        if (exportMessageId !in available) exportMessageId = null
        selectedMessageIds = selectedMessageIds.intersect(available)
        if (selectedMessageIds.isEmpty()) isSelecting = false
        forwardMessageIds = forwardMessageIds?.intersect(available)?.takeIf { it.isNotEmpty() }
        deleteMessageIds = deleteMessageIds?.intersect(available)?.takeIf { it.isNotEmpty() }
        if (forwardMediaKey?.messageId !in available) forwardMediaKey = null
        if (viewerSelection?.initialKey?.messageId !in available) viewerSelection = null
        localVoiceTranscripts = localVoiceTranscripts.filterKeys { it in available }
        visibleVoiceTranscriptIds = visibleVoiceTranscriptIds.intersect(available)
    }
    CompositionLocalProvider(LocalSpeechOwner provides dev.ipf.whitenoise.model.SpeechOwner(profile.id, chat.id), LocalMessageReading provides MessageReadingActions(
        collapse = chat.collapseLongMessages && !isSearching,
        canWrite = chat.composerAvailability(profile) == ComposerAvailability.Available,
        open = { history.cancel(); pendingInitialMessageId = null; initialViewportSettled = true; readerStartsSelection = false; readerMessageId = it },
        history = { history.cancel(); pendingInitialMessageId = null; initialViewportSettled = true; historyMessageId = it },
        retry = onRetryMessageEdit, discard = onDiscardMessageEdit,
    )) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (focusedMessageId != null) {
                    Modifier.blur(FocusedMessageBackdropBlurRadius)
                } else {
                    Modifier
                },
            ),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column {
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
                        history.cancel()
                        searchQuery = it
                        pinnedSearchMessageId = null
                        pendingInitialMessageId = null
                        initialViewportSettled = true
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
            AdaptiveContent(Modifier.padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)) {
                Column {
                    history.request?.takeIf { it.operation == HistoryOperation.Target }?.let { request ->
                        HistoryTargetFeedback(request, history::retry) { history.cancel(); pendingInitialMessageId = null }
                    }
                    if (isSearching && (history.scanning || history.scanFailed)) HistorySearchFeedback(history.scanning) { history.scanRetry++ }
                    chat.messageDeletion?.let { operation -> MessageDeletionNotice(operation,
                        onRetry = { onRetryMessageDeletion(operation.id) }, onDismiss = { onDismissMessageDeletion(operation.id) }) }
                }
            }
            }
        },
        bottomBar = {
            when {
                isSearching -> SearchResultsBottomBar(
                    count = searchResults.size,
                    current = searchResultIndex,
                    onOlder = {
                        pinnedSearchMessageId = searchResults.getOrNull((searchResultIndex + 1).coerceAtMost(searchResults.lastIndex))?.messageId
                    },
                    onNewer = { pinnedSearchMessageId = searchResults.getOrNull((searchResultIndex - 1).coerceAtLeast(0))?.messageId },
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
        floatingActionButton = {
            if (!isSearching && !isSelecting && focusedMessageId == null && !composerPresentationActive) {
                val mentions = ConversationReading.mentions(readState, chat, profile)
                Column(Modifier.imePadding().padding(bottom = with(density) { compactComposerHeightPx.toDp() }), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                    if (mentions.isNotEmpty()) FilledTonalButton(onClick = {
                        history.target(chat, mentions.first(), onHistoryScenario(HistoryOperation.Target), markThrough = true)
                    }, modifier = Modifier.testTag("history.jumpMention")) { Text(stringResource(R.string.history_jump_mention)) }
                    if (!nearTail && initialViewportSettled) FilledTonalButton(onClick = {
                        val unread = history.jump.pendingId
                        val target = unread ?: ConversationProjection.orderedEntries(chat).filterIsInstance<ChatTimelineEntry.Message>().lastOrNull { !it.message.isDeleted }?.id
                        if (target != null) {
                            history.target(chat, target, onHistoryScenario(HistoryOperation.Target))
                        }
                    }, modifier = Modifier.testTag("history.jumpUnread")) {
                        Text(stringResource(if (history.jump.pendingId != null) R.string.history_jump_unread else R.string.history_jump_latest))
                    }
                }
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
                    modifier = Modifier.observeSpeechScroll(readAloudController, speechSession != null)
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
                            is ConversationItem.DayHeader -> item(key = item.id, contentType = "day") {
                                InlineDayHeader(
                                    label = item.label,
                                    id = item.id,
                                    visible = !composerPresentationActive,
                                )
                            }
                            is ConversationItem.EventItem -> item(key = item.id, contentType = "event") {
                                TimelineInformation(item.entry.text)
                            }
                            is ConversationItem.NoticeItem -> item(key = item.id, contentType = "notice") {
                                when (item.id) {
                                    "history.older", "history.newer" -> {
                                        val operation = if (item.id == "history.older") HistoryOperation.Older else HistoryOperation.Newer
                                        HistoryPageControl(operation, history.request, {
                                            pendingInitialMessageId = null
                                            initialViewportSettled = true
                                            history.page(operation, onHistoryScenario(operation))
                                        }, history::retry)
                                    }
                                    "history.unread" -> TimelineInformation(stringResource(R.string.history_unread_boundary))
                                    else -> TimelineInformation(item.entry.text, isNotice = true)
                                }
                            }
                            is ConversationItem.MessageItem -> item(
                                key = item.id,
                                contentType = "message",
                            ) {
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
                                if (profile.developerTools.isEnabled && profile.developerTools.streamingDebug && !item.message.isDeleted) {
                                    dev.ipf.whitenoise.model.DeveloperInspection.streamDetails(item.message)?.let { details ->
                                        Text(details, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin).testTag("conversation.stream_debug.${item.id}"))
                                    }
                                }
                                MessageRow(
                                    profile = profile,
                                    chat = chat,
                                    item = item,
                                    speechActionState = speechActionState(item.message),
                                    onRetry = { onRetry(item.message.id) },
                                    onOpenMedia = { key ->
                                        viewerSelection = ConversationMediaProjection.selection(
                                            chat,
                                            profile,
                                            key,
                                        )
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
                                    onAccessibilityAction = { action ->
                                        handleAction(item.message, action)
                                    },
                                    onSwipeReply = { beginReply(item.message.id) },
                                    onReaction = { emoji ->
                                        onReaction(item.message.id, emoji, false)
                                    },
                                    onOpenPersonProfile = onOpenPersonProfile,
                                    onRetryNostrEvent = { referenceId, revision ->
                                        onRetryNostrEvent(item.message.id, referenceId, revision)
                                    },
                                    sourceHighlighted = highlightedMessageId == item.message.id,
                                    onOpenReplyTarget = ::openReplyTarget,
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
                if (!composerPresentationActive) {
                    pinnedDayHeader?.let { dayHeader ->
                        PinnedDayHeader(
                            label = dayHeader.label,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
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
                        onLocationSent = { sentLocationTarget = it },
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
                        onOverlayPresentationChanged = { composerOverlayActive = it },
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

    val viewerMedia = remember(chat.timeline, profile.id, profile.people) { ConversationMediaProjection.items(chat, profile) }
    LaunchedEffect(viewerMedia) { if (viewerMedia.isEmpty()) viewerSelection = null }
    viewerSelection?.takeIf { viewerMedia.isNotEmpty() }?.let { selection ->
        ReadOnlyMediaViewer(
            selection = selection.copy(items = viewerMedia),
            onDismiss = { viewerSelection = null },
            onForward = { item ->
                forwardMediaKey = item.key
                viewerSelection = null
            },
            onGoToMessage = { item ->
                viewerSelection = null
                openReplyTarget(item.message.id)
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
    LaunchedEffect(chat.messageDeletion?.id, chat.messageDeletion?.revision) {
        chat.messageDeletion?.takeIf { !it.isRunning }?.let { operation ->
            selectedMessageIds = operation.failed.map { it.messageId }.filter { id -> messages.any { it.id == id } }.toSet()
            isSelecting = selectedMessageIds.isNotEmpty()
        }
    }
    forwardMessageIds?.let { ids ->
        ForwardMessagesSheet(
            profile = profile,
            sourceChatId = chat.id,
            onDismiss = { forwardMessageIds = null },
            destinationProfiles = forwardProfiles,
            onForwardToProfile = { destination, targets, _ ->
                val started = onForwardToProfile?.invoke(ids, destination, targets) ?: onForwardMessages(ids, targets)
                if (started) { forwardMessageIds = null; isSelecting = false; selectedMessageIds = emptySet() }
                started
            },
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
            destinationProfiles = forwardProfiles,
            onForwardToProfile = { destination, targets, message ->
                val started = onForwardMediaToProfile?.invoke(key, destination, targets, message) ?: onForwardMedia(key, targets, message)
                if (started) { forwardMediaKey = null; viewerSelection = null }
                started
            },
            onForward = { targets, message ->
                if (onForwardMedia(key, targets, message)) forwardMediaKey = null
            },
        )
    }
    deleteMessageIds?.let { ids ->
        DeleteMessagesDialog(
            messages = messages.filter { it.id in ids },
            profileId = profile.id,
            onDismiss = { deleteMessageIds = null; deleteStartFailed = false },
            remoteIds = messages.filter { dev.ipf.whitenoise.model.MessageDeletion.canDeleteForEveryone(it, profile, chat) }.map { it.id }.toSet(),
            busy = chat.messageDeletion?.isRunning == true,
            startFailed = deleteStartFailed,
            onDelete = { scope ->
                deleteStartFailed = false
                if (onDeleteMessages(ids, scope)) {
                    deleteMessageIds = null
                    isSelecting = false
                    selectedMessageIds = emptySet()
                } else deleteStartFailed = true
            },
        )
    }
    messages.firstOrNull { it.id == editMessageId && !it.isDeleted }?.let { message ->
        MessageEditDialog(profile.id, message, { editMessageId = null }) { text, revision -> onEditMessage(message.id, text, revision) }
    }
    messages.firstOrNull { it.id == historyMessageId && !it.isDeleted }?.let { message ->
        MessageEditHistoryDialog(message) { historyMessageId = null }
    }
    exportMessageId?.let { id ->
        val current = messages.firstOrNull { it.id == id }
        if (current == null || current.isDeleted) LaunchedEffect(id, current) { exportMessageId = null }
        else MessageAttachmentExportSheet(current, exportSharing, profile.people) { exportMessageId = null }
    }
    messages.firstOrNull { it.id == readerMessageId && !it.isDeleted }?.let { message ->
        MessageReaderDialog(profile, chat, message, readerStartsSelection, speechActionState(message), readAloudController,
            onDismiss = { readerMessageId = null }, onAction = { action ->
                if (action !in setOf(MessageAction.Copy, MessageAction.CopyMarkdown, MessageAction.ReadAloud, MessageAction.StopReading, MessageAction.RetryEdit, MessageAction.DiscardEdit)) readerMessageId = null
                handleAction(message, action)
            }, onReact = { readerMessageId = null; focusedMessageId = message.id }, onPerson = { readerMessageId = null; onOpenPersonProfile(it) })
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val actions = remember(message, profile.id, speechActionState, chat.composerAvailability(profile)) {
        MessageActionPolicy.available(message, profile.id, speechActionState, chat.composerAvailability(profile) == ComposerAvailability.Available)
    }
    val density = LocalDensity.current
    val dismissInteraction = remember { MutableInteractionSource() }
    val reactionRailInteraction = remember { MutableInteractionSource() }
    val menuInteraction = remember { MutableInteractionSource() }
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
        append(if (outgoing) profile.name else profile.people.firstOrNull { it.id == message.authorId }?.displayName ?: chat.title)
        val visible = message.plainVisibleText(profile.id)
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
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.setDimAmount(0f)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { dialogHeightPx = it.height }
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLowest.copy(
                            alpha = FocusedMessageBackdropSurfaceAlpha,
                        ),
                    )
                    .testTag("message.actions.backdrop"),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, contentTop) }
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)
                    .heightIn(max = availableHeightDp)
                    .verticalScroll(rememberScrollState())
                    .onSizeChanged { contentHeightPx = it.height },
                verticalArrangement = Arrangement.Top,
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FocusedOverlayShadowSafeInset)
                        .testTag("message.actions.shadowGutter.top"),
                )
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (outgoing) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 392.dp)
                            .clickable(
                                interactionSource = reactionRailInteraction,
                                indication = null,
                                onClick = {},
                            )
                            .testTag("message.actions.reactions"),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MenuDefaults.groupStandardContainerColor,
                        tonalElevation = MenuDefaults.TonalElevation,
                        shadowElevation = MenuDefaults.ShadowElevation,
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(FocusedReactionRailInset),
                            horizontalArrangement = Arrangement.spacedBy(FocusedReactionItemSpacing),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            itemsIndexed(quickReactions, key = { _, emoji -> emoji }) { index, emoji ->
                                val selected = emoji == selectedReaction
                                val interactionSource = remember(emoji) { MutableInteractionSource() }
                                val pressed by interactionSource.collectIsPressedAsState()
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            role = Role.Button,
                                            onClick = { onReaction(emoji, selected) },
                                        )
                                        .semantics {
                                            contentDescription = emoji
                                            stateDescription = if (selected) selectedDescription else notSelectedDescription
                                        }
                                        .testTag("message.actions.reaction.target.$index"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(FocusedReactionStateLayerSize)
                                            .clip(CircleShape)
                                            .background(
                                                if (pressed) {
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                                } else {
                                                    Color.Transparent
                                                },
                                            )
                                            .indication(interactionSource, ripple())
                                            .testTag("message.actions.reaction.stateLayer.$index"),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(FocusedReactionSelectedFillSize),
                                            shape = CircleShape,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                Color.Transparent
                                            },
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                SignalEmoji(
                                                    emoji = emoji,
                                                    modifier = Modifier
                                                        .size(FocusedReactionEmojiSize)
                                                        .testTag("message.actions.reaction.emoji.$index"),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                val interactionSource = remember { MutableInteractionSource() }
                                val pressed by interactionSource.collectIsPressedAsState()
                                val moreReactionsDescription = stringResource(R.string.more_reactions)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                            role = Role.Button,
                                            onClick = onMoreReactions,
                                        )
                                        .semantics {
                                            contentDescription = moreReactionsDescription
                                        }
                                        .testTag("message.actions.reaction.more"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(FocusedReactionStateLayerSize)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                            .background(
                                                if (pressed) {
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                                } else {
                                                    Color.Transparent
                                                },
                                            )
                                            .indication(interactionSource, ripple())
                                            .testTag("message.actions.reaction.more.stateLayer"),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_more_horiz),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(WhiteNoiseSpacing.Related))
                ScaleToFitHeight(
                    maximumHeight = 320.dp,
                    transformOrigin = TransformOrigin(if (outgoing) 1f else 0f, 0f),
                    modifier = Modifier
                        .fillMaxWidth()
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
                Spacer(modifier = Modifier.height(WhiteNoiseSpacing.Related))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (outgoing) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    WhiteNoiseMenuGroup(
                        items = actions.map { action ->
                            WhiteNoiseMenuItem(
                                label = actionLabel(action),
                                onClick = { onAction(action) },
                                icon = actionIcon(action),
                                destructive = action == MessageAction.Delete,
                            )
                        },
                        modifier = Modifier
                            .widthIn(min = 248.dp, max = 300.dp)
                            .clickable(
                                interactionSource = menuInteraction,
                                indication = null,
                                onClick = {},
                            )
                            .testTag("message.actions.menu"),
                        shadowElevation = MenuDefaults.ShadowElevation,
                    )
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FocusedOverlayShadowSafeInset)
                        .testTag("message.actions.shadowGutter.bottom"),
                )
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
            append(", ${stringResource(R.string.disappearing_header, retentionLabel(chat.disappearingDuration))}")
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
                    chat.visibleAvatar,
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

internal inline fun pinnedConversationDayHeaderIndex(
    dayHeaderIndices: List<Int>,
    topVisibleItemIndex: Int?,
    isHeaderVisible: (Int) -> Boolean,
): Int? {
    val topIndex = topVisibleItemIndex ?: return null
    val activeHeaderIndex = dayHeaderIndices.lastOrNull { it <= topIndex } ?: return null
    return activeHeaderIndex.takeUnless(isHeaderVisible)
}

@Composable
private fun InlineDayHeader(
    label: String,
    id: String,
    visible: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (visible) 1f else 0f)
            .padding(vertical = WhiteNoiseSpacing.FormField),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            modifier = Modifier
                .testTag("conversation.date.inline.$id")
                .padding(horizontal = 12.dp, vertical = 3.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PinnedDayHeader(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = WhiteNoiseSpacing.FormField),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceDim.copy(
                alpha = PinnedDayHeaderSurfaceAlpha,
            ),
            modifier = Modifier
                .testTag("conversation.date.pinned")
                .semantics { heading() },
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
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
    onOpenPersonProfile: (String) -> Unit = {},
    onRetryNostrEvent: (referenceId: String, revision: Int) -> Unit = { _, _ -> },
    sourceHighlighted: Boolean = false,
    onOpenReplyTarget: (String) -> Unit = {},
    readAloudController: ReadAloudController,
    onPositioned: (Rect) -> Unit = {},
    contextPreview: Boolean = false,
) {
    val message = item.message
    val outgoing = message.authorId == profile.id
    val author = profile.people.firstOrNull { it.id == message.authorId }
    val authorName = if (outgoing) stringResource(R.string.you) else author?.displayName ?: chat.title
    val verticalPadding = when {
        item.startsCluster && !contextPreview -> WhiteNoiseSpacing.FormField
        else -> 0.dp
    }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val directionMultiplier = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) 1f else -1f
    val replyThresholdPx = with(density) { ReplySwipeThreshold.toPx() }
    val replyMaximumPx = with(density) { ReplySwipeMaximum.toPx() }
    val replyIconTravelPx = with(density) { ReplySwipeIconTravel.toPx() }
    val replyIconTargetSizePx = with(density) { ReplySwipeIconTargetSize.toPx() }
    var rawSwipeDistance by remember(message.id) { mutableFloatStateOf(0f) }
    var swipeReady by remember(message.id) { mutableStateOf(false) }
    val replyPulseScale = remember(message.id) { Animatable(1f) }
    var rowBoundsInRoot by remember(message.id) { mutableStateOf<Rect?>(null) }
    var bubbleBoundsInRoot by remember(message.id) { mutableStateOf<Rect?>(null) }
    val displayedSwipeDistance = resistedReplySwipeDistance(
        rawDistance = rawSwipeDistance,
        threshold = replyThresholdPx,
        maximum = replyMaximumPx,
    )
    val showTime = item.endsCluster || message.retention != null
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
        canWrite = chat.composerAvailability(profile) == ComposerAvailability.Available,
    ).map { action ->
        CustomAccessibilityAction(actionLabel(action)) {
            onAccessibilityAction(action)
            true
        }
    }
    val availableActions = MessageActionPolicy.available(message, profile.id, speechActionState, chat.composerAvailability(profile) == ComposerAvailability.Available)
    val failedOutgoing = !message.isDeleted && outgoing && message.deliveryState == MessageDeliveryState.Failed
    val retryLabel = stringResource(R.string.not_delivered_retry)
    val messageInteractionSource = remember(message.id) { MutableInteractionSource() }
    val contextPreviewInteraction = remember(message.id) { MutableInteractionSource() }
    val showMessageActions = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (message.isDeleted) onAccessibilityAction(MessageAction.Delete) else onShowActions()
    }
    val canSwipeReply = !contextPreview && !isSelectionMode && MessageAction.Reply in availableActions
    val swipeState = rememberDraggableState { physicalDelta ->
        val semanticDelta = physicalDelta * directionMultiplier
        rawSwipeDistance = (rawSwipeDistance + semanticDelta).coerceAtLeast(0f)
        val nowReady = rawSwipeDistance >= replyThresholdPx
        if (nowReady && !swipeReady) {
            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            coroutineScope.launch {
                replyPulseScale.stop()
                replyPulseScale.snapTo(1f)
                replyPulseScale.animateTo(
                    targetValue = ReplySwipeIconPulseScale,
                    animationSpec = tween(durationMillis = 100),
                )
                replyPulseScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 100),
                )
            }
        } else if (!nowReady && swipeReady) {
            coroutineScope.launch {
                replyPulseScale.stop()
                replyPulseScale.snapTo(1f)
            }
        }
        swipeReady = nowReady
    }
    val swipeModifier = Modifier.draggable(
        state = swipeState,
        orientation = Orientation.Horizontal,
        enabled = canSwipeReply,
        onDragStopped = {
            val shouldReply = swipeReady
            swipeReady = false
            if (shouldReply) onSwipeReply()
            val returnAnimation = Animatable(rawSwipeDistance)
            returnAnimation.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            ) {
                rawSwipeDistance = value.coerceAtLeast(0f)
            }
            rawSwipeDistance = 0f
            replyPulseScale.stop()
            replyPulseScale.snapTo(1f)
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
        Modifier
            .then(
                if (failedOutgoing) {
                    Modifier.clickable(
                        interactionSource = messageInteractionSource,
                        indication = null,
                        onClickLabel = retryLabel,
                        onClick = onRetry,
                    )
                } else {
                    Modifier
                },
            )
            .semantics {
                if (availableActions.isNotEmpty()) {
                    onLongClick(showActionsLabel) {
                        showMessageActions()
                        true
                    }
                }
            }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(searchAlpha)
            .testTag("conversation.message.${message.id}")
            .padding(top = verticalPadding)
            .background(
                color = when {
                    isSelectionMode && selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                    sourceHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                    else -> Color.Transparent
                },
            )
            .then(interactionModifier)
            .then(swipeModifier)
            .onGloballyPositioned {
                if (rawSwipeDistance == 0f) rowBoundsInRoot = it.boundsInRoot()
                onPositioned(it.boundsInWindow())
            }
            .semantics {
                if (isSelectionMode) stateDescription = selectedState
                searchPosition?.let { stateDescription = it }
                customActions = accessibilityActions
            },
    ) {
        if (sourceHighlighted) {
            Box(
                Modifier
                    .matchParentSize()
                    .testTag("conversation.message.highlight.${message.id}"),
            )
        }
        val rowBounds = rowBoundsInRoot
        val bubbleBounds = bubbleBoundsInRoot
        if (
            canSwipeReply &&
            displayedSwipeDistance > 0f &&
            rowBounds != null &&
            bubbleBounds != null
        ) {
            val iconProgress = (displayedSwipeDistance / replyThresholdPx).coerceIn(0f, 1f)
            val iconBaseScale = 1f + ((ReplySwipeIconReadyScale - 1f) * iconProgress)
            val iconStartOffset = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) {
                bubbleBounds.left - rowBounds.left
            } else {
                rowBounds.right - bubbleBounds.right
            }
            val iconTopOffset = bubbleBounds.center.y - rowBounds.top - (replyIconTargetSizePx / 2f)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            x = iconStartOffset.roundToInt(),
                            y = iconTopOffset.roundToInt(),
                        )
                    }
                    .size(ReplySwipeIconTargetSize)
                    .graphicsLayer {
                        alpha = if (iconProgress >= ReplySwipeIconRevealStart) iconProgress else 0f
                        translationX = replyIconTravelPx * iconProgress * directionMultiplier
                        scaleX = iconBaseScale * replyPulseScale.value
                        scaleY = iconBaseScale * replyPulseScale.value
                    }
                    .testTag("conversation.message.swipeReply.${message.id}"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_reply_swipe),
                    contentDescription = null,
                    modifier = Modifier.size(ReplySwipeIconSize),
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
                            chatBubbleColors = chat.bubbleColors,
                            message = message,
                            outgoing = outgoing,
                            item = item,
                            authorName = authorName,
                            onOpenMedia = onOpenMedia,
                            onOpenPersonProfile = onOpenPersonProfile,
                            memberIds = chat.members.mapTo(mutableSetOf()) { it.personId }.takeIf { chat.isGroup },
                            onRetryNostrEvent = onRetryNostrEvent,
                            onOpenReplyTarget = onOpenReplyTarget,
                            canOpenReplyTarget = message.replyToMessageId != null,
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
                            onBubbleLongPress = if (
                                !contextPreview && !isSelectionMode && availableActions.isNotEmpty()
                            ) {
                                showMessageActions
                            } else {
                                null
                            },
                            messageInteractionSource = if (contextPreview) null else messageInteractionSource,
                            trimInvisibleReactionTarget = contextPreview,
                            onBubblePositioned = { bounds ->
                                if (rawSwipeDistance == 0f) bubbleBoundsInRoot = bounds
                            },
                            modifier = Modifier
                                .widthIn(max = 340.dp)
                                .then(
                                    if (contextPreview) {
                                        Modifier.clickable(
                                            interactionSource = contextPreviewInteraction,
                                            indication = null,
                                            onClick = {},
                                        )
                                    } else {
                                        Modifier
                                    },
                                ),
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
        val pillHeightPx = minimumPillHeightPx
        val targetInsetPx = ((targetHeightPx - pillHeightPx).coerceAtLeast(0)) / 2
        overlapPx = targetInsetPx + with(density) { ReactionVisibleOverlap.roundToPx() }
        timestampVerticalOffsetPx = overlapPx + timestampTopGapPx
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
    chatBubbleColors: dev.ipf.whitenoise.model.ChatBubbleColorOverrides,
    message: ChatMessage,
    outgoing: Boolean,
    item: ConversationItem.MessageItem,
    authorName: String,
    onOpenMedia: (ConversationMediaKey) -> Unit,
    onOpenPersonProfile: (String) -> Unit,
    memberIds: Set<String>?,
    onRetryNostrEvent: (referenceId: String, revision: Int) -> Unit,
    onOpenReplyTarget: (String) -> Unit,
    canOpenReplyTarget: Boolean,
    searchQuery: String,
    readAloudController: ReadAloudController,
    voiceTranscript: String?,
    voiceTranscriptVisible: Boolean,
    showTime: Boolean,
    onReaction: (String) -> Unit,
    onShowActions: () -> Unit,
    onBubbleLongPress: (() -> Unit)?,
    messageInteractionSource: MutableInteractionSource?,
    trimInvisibleReactionTarget: Boolean,
    onBubblePositioned: (Rect) -> Unit,
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
                    chatBubbleColors = chatBubbleColors,
                    message = message,
                    outgoing = outgoing,
                    item = item,
                    authorName = authorName,
                    onOpenMedia = onOpenMedia,
                    onOpenPersonProfile = onOpenPersonProfile,
                    memberIds = memberIds,
                    onRetryNostrEvent = onRetryNostrEvent,
                    onOpenReplyTarget = onOpenReplyTarget,
                    canOpenReplyTarget = canOpenReplyTarget,
                    searchQuery = searchQuery,
                    readAloudController = readAloudController,
                    voiceTranscript = voiceTranscript,
                    voiceTranscriptVisible = voiceTranscriptVisible,
                    onLongPress = onBubbleLongPress,
                    messageInteractionSource = messageInteractionSource,
                    onPositioned = onBubblePositioned,
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
                    retentionMessage = message,
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
                    onLongPress = null,
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
                chatBubbleColors = chatBubbleColors,
                message = message,
                outgoing = outgoing,
                item = item,
                authorName = authorName,
                onOpenMedia = onOpenMedia,
                onOpenPersonProfile = onOpenPersonProfile,
                memberIds = memberIds,
                onRetryNostrEvent = onRetryNostrEvent,
                onOpenReplyTarget = onOpenReplyTarget,
                canOpenReplyTarget = canOpenReplyTarget,
                searchQuery = searchQuery,
                readAloudController = readAloudController,
                voiceTranscript = voiceTranscript,
                voiceTranscriptVisible = voiceTranscriptVisible,
                onLongPress = onBubbleLongPress,
                messageInteractionSource = messageInteractionSource,
                onPositioned = onBubblePositioned,
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
                retentionMessage = message,
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
                onLongPress = onBubbleLongPress,
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
    chatBubbleColors: dev.ipf.whitenoise.model.ChatBubbleColorOverrides,
    message: ChatMessage,
    outgoing: Boolean,
    item: ConversationItem.MessageItem,
    authorName: String,
    onOpenMedia: (ConversationMediaKey) -> Unit,
    onOpenPersonProfile: (String) -> Unit,
    memberIds: Set<String>?,
    onRetryNostrEvent: (referenceId: String, revision: Int) -> Unit,
    onOpenReplyTarget: (String) -> Unit,
    canOpenReplyTarget: Boolean,
    searchQuery: String,
    readAloudController: ReadAloudController,
    voiceTranscript: String?,
    voiceTranscriptVisible: Boolean,
    onLongPress: (() -> Unit)?,
    messageInteractionSource: MutableInteractionSource?,
    onPositioned: (Rect) -> Unit,
) {
    val authoredText = if (message.isDeleted || searchQuery.isNotBlank()) message.visibleText(profile.id) else dev.ipf.whitenoise.model.MessageEditing.displayedText(message)
    val eventOnly = searchQuery.isBlank() && message.nostrEvents.isNotEmpty() &&
        authoredText.trim() in message.nostrEvents.map { it.authoredReference.trim() }
    val text = authoredText.takeUnless { eventOnly }.orEmpty()
    val plainText = dev.ipf.whitenoise.model.InlineMessageMarkup.plainText(text)
    val description = buildString {
        append(authorName)
        if (plainText.isNotBlank()) append(", $plainText")
        if (!message.isDeleted) {
            message.attachments.forEach { append(", ${it.label}") }
        }
        append(", ${message.timeLabel}")
        message.reactions.forEach { append(", ${it.emoji}, ${it.personIds.size}") }
    }
    message.agentOperation?.takeIf { !message.isDeleted && searchQuery.isBlank() }?.let { operation ->
        AgentOperationCard(
            messageId = message.id,
            operation = operation,
            onLongPress = onLongPress,
            modifier = Modifier
                .testTag("conversation.message.bubble.${message.id}")
                .onGloballyPositioned { onPositioned(it.boundsInRoot()) }
                .semantics(mergeDescendants = true) { contentDescription = description },
        )
        return
    }
    val bubbleShape = MaterialTheme.shapes.large
    val currentLongPress = rememberUpdatedState(onLongPress)
    val bubbleLongPress = remember(message.id) {
        {
            currentLongPress.value?.invoke()
            Unit
        }
    }
    val defaultBubbleColors = dev.ipf.whitenoise.ui.theme.LocalDefaultMessageBubbleColors.current
    val globalBubbleColors = profile.settings.colors
        .forTheme(dev.ipf.whitenoise.ui.theme.LocalAppearanceColorTheme.current)
    val bubbleOverride = dev.ipf.whitenoise.model.AppearanceColorPolicy.effectiveBubble(
        chatOverride = if (outgoing) chatBubbleColors.mineArgb else chatBubbleColors.otherArgb,
        globalOverride = if (outgoing) globalBubbleColors.mineBubbleArgb else globalBubbleColors.otherBubbleArgb,
    )
    val readableBubble = dev.ipf.whitenoise.model.AppearanceColorPolicy.readable(bubbleOverride)
    val bubbleContainerColor = readableBubble?.containerArgb
        ?.let(::colorFromOpaqueArgb)
        ?: if (outgoing) defaultBubbleColors.mineContainer else defaultBubbleColors.otherContainer
    val bubbleContentColor = readableBubble?.contentArgb
        ?.let(::colorFromOpaqueArgb)
        ?: if (outgoing) defaultBubbleColors.mineContent else defaultBubbleColors.otherContent
    val hasRichContent = !message.isDeleted &&
        (message.replyToMessageId != null || message.attachments.isNotEmpty() || message.nostrEvents.isNotEmpty())
    val singleMediaSize = rememberTimelineSingleMediaSize(message.attachments.singleOrNull())
    val richCanvasWidth = richContentCanvasWidthDp(message.attachments, singleMediaSize).dp
    Surface(
        shape = bubbleShape,
        color = bubbleContainerColor,
        contentColor = bubbleContentColor,
        modifier = Modifier
            .testTag("conversation.message.bubble.${message.id}")
            .interceptBubbleLongPress(
                enabled = onLongPress != null,
                onLongPress = bubbleLongPress,
            )
            .onGloballyPositioned { onPositioned(it.boundsInRoot()) }
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Box {
            if (hasRichContent) {
                Column(
                    modifier = Modifier
                        .padding(ConversationMessageMetrics.RichOuterInset)
                        .width(richCanvasWidth),
                    verticalArrangement = Arrangement.spacedBy(
                        ConversationMessageMetrics.RichContentSpacing,
                    ),
                ) {
                    if (message.replyToMessageId != null) {
                        ReplyQuote(
                            profile = profile,
                            item = item,
                            outgoing = outgoing,
                            onOpenReplyTarget = onOpenReplyTarget,
                            canOpenReplyTarget = canOpenReplyTarget,
                        )
                    }
                    if (message.attachments.isNotEmpty()) {
                        TimelineAttachmentContent(
                            attachments = message.attachments,
                            outgoing = outgoing,
                            messageId = message.id,
                            onOpenMedia = onOpenMedia,
                            searchQuery = searchQuery,
                            voiceTranscript = voiceTranscript,
                            voiceTranscriptVisible = voiceTranscriptVisible,
                            people = profile.people,
                            onOpenPerson = onOpenPersonProfile,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (message.nostrEvents.isNotEmpty()) {
                        NostrEventCards(
                            message = message,
                            profile = profile,
                            onRetry = onRetryNostrEvent,
                            onOpenPerson = onOpenPersonProfile,
                        )
                    }
                    if (text.isNotBlank()) {
                        MessageBubbleText(
                            profile = profile,
                            message = message,
                            text = text,
                            plainText = plainText,
                            searchQuery = searchQuery,
                            onOpenPersonProfile = onOpenPersonProfile,
                            memberIds = memberIds,
                            readAloudController = readAloudController,
                            showTranscriptLabel = message.attachments.any {
                                it.voiceFormat == VoiceMessageFormat.Both
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = ConversationMessageMetrics.RichTextHorizontalAdjustment,
                                    end = ConversationMessageMetrics.RichTextHorizontalAdjustment,
                                    bottom = ConversationMessageMetrics.RichTextBottomAdjustment,
                                ),
                        )
                    }
                }
            } else {
                MessageBubbleText(
                    profile = profile,
                    message = message,
                    text = text,
                    plainText = plainText,
                    searchQuery = searchQuery,
                    onOpenPersonProfile = onOpenPersonProfile,
                    memberIds = memberIds,
                    readAloudController = readAloudController,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            if (messageInteractionSource != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(bubbleShape)
                        .indication(
                            messageInteractionSource,
                            ripple(color = bubbleContentColor),
                        )
                        .testTag("conversation.message.pressLayer.${message.id}"),
                )
            }
        }
    }
}

@Composable
private fun MessageBubbleText(
    profile: Profile,
    message: ChatMessage,
    text: String,
    plainText: String,
    searchQuery: String,
    onOpenPersonProfile: (String) -> Unit,
    memberIds: Set<String>?,
    readAloudController: ReadAloudController,
    modifier: Modifier = Modifier,
    showTranscriptLabel: Boolean = false,
) {
    var unavailableProfile by rememberSaveable(message.id) {
        mutableStateOf<dev.ipf.whitenoise.model.NostrProfileOccurrence?>(null)
    }
    Column(modifier = modifier) {
        if (showTranscriptLabel) {
            Text(
                stringResource(R.string.transcribed),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        val location = remember(message, text) { dev.ipf.whitenoise.model.LocationSharing.fromMessage(message.copy(text = text)) }
        if (location != null && searchQuery.isBlank()) {
            LocationMessageCard(location)
        } else if (searchQuery.isNotBlank() && message.deletionState == MessageDeletionState.None) {
            SearchHighlightedText(
                text = plainText,
                query = searchQuery,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else if (message.deletionState == MessageDeletionState.None) {
            val reading = LocalMessageReading.current
            val document = remember(text) { dev.ipf.whitenoise.model.MessageDocuments.parse(text) }
            val limit = with(LocalDensity.current) { (MaterialTheme.typography.bodyLarge.lineHeight * 52).toDp() }
            val limitPx = with(LocalDensity.current) { limit.roundToPx() }
            var overflow by remember(text, limitPx) { mutableStateOf(false) }
            Box(if (reading.collapse) Modifier.heightIn(max = limit).clipToBounds() else Modifier) {
                MessageDocumentContent(document, profile.people, onOpenPersonProfile,
                    Modifier.wrapContentHeight(Alignment.Top, unbounded = true).onSizeChanged { overflow = it.height > limitPx },
                    spokenRange = readAloudController.session?.takeIf { it.owner == LocalSpeechOwner.current && it.current.item.id == message.id && it.current.item.authored == text }
                        ?.passage?.let { it.sourceStart until it.sourceEnd },
                    followSpeech = readAloudController.session?.following == true,
                    memberIds = memberIds,
                    onOpenProfileReference = { occurrence ->
                        val person = profile.people.firstOrNull { it.publicKey == occurrence.publicKey }
                        if (person != null) onOpenPersonProfile(person.id) else unavailableProfile = occurrence
                    })
            }
            if (overflow && reading.collapse) TextButton(onClick = { reading.open(message.id) }, modifier = Modifier.testTag("message.readMore.${message.id}"),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.LocalContentColor.current)) {
                Text(stringResource(R.string.message_read_more))
            }
        } else {
            Text(
                text = text,
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (!message.isDeleted) MessageEditStatus(message)
        ReadAloudProgress(message.id, readAloudController)
    }
    unavailableProfile?.let { occurrence ->
        UnavailableNostrProfileDialog(occurrence) { unavailableProfile = null }
    }
}

@Composable
private fun ReplyQuote(
    profile: Profile,
    item: ConversationItem.MessageItem,
    outgoing: Boolean,
    onOpenReplyTarget: (String) -> Unit,
    canOpenReplyTarget: Boolean,
    modifier: Modifier = Modifier,
) {
    val source = item.resolvedReply
    val author = source?.let { message ->
        if (message.authorId == profile.id) stringResource(R.string.you) else {
            profile.people.firstOrNull { it.id == message.authorId }?.displayName
                ?: stringResource(R.string.unknown_person)
        }
    }
    val body = when {
        item.hasUnavailableReply -> stringResource(R.string.original_message_unavailable)
        source != null -> source.plainVisibleText(profile.id).ifBlank {
            source.attachments.firstOrNull()?.label.orEmpty()
        }
        else -> stringResource(R.string.original_message_unavailable)
    }
    val content = if (outgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val secondary = if (outgoing) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val quoteShape = ConversationRichContentShape
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
        shape = quoteShape,
        modifier = modifier
            .fillMaxWidth()
            .clip(quoteShape)
            .then(
                if (canOpenReplyTarget) {
                    Modifier.clickable(role = Role.Button) {
                        item.message.replyToMessageId?.let(onOpenReplyTarget)
                    }
                } else {
                    Modifier
                },
            )
            .testTag("conversation.message.quote.target.${item.message.id}"),
        testTagPrefix = "conversation.message.quote.${item.message.id}",
    )
}

@Composable
private fun MessageMetadataContent(
    retentionMessage: ChatMessage,
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
    onLongPress: (() -> Unit)?,
) {
    val currentLongPress = rememberUpdatedState(onLongPress)
    val metadataLongPress = remember(messageId) {
        {
            currentLongPress.value?.invoke()
            Unit
        }
    }
    val widthModifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier
    val arrangement = when {
        summary.isNotEmpty() && showTime -> Arrangement.SpaceBetween
        outgoing -> Arrangement.End
        else -> Arrangement.Start
    }
    Row(
        modifier = widthModifier
            .padding(horizontal = 12.dp)
            .interceptBubbleLongPress(
                enabled = !measurementOnly && onLongPress != null,
                onLongPress = metadataLongPress,
            )
            .then(
                if (measurementOnly) {
                    Modifier.clearAndSetSemantics { }
                } else {
                    Modifier.testTag("conversation.message.metadata.$messageId")
                },
            ),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.Top,
    ) {
        if (!outgoing && showTime) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MessageExpiryIndicator(retentionMessage, semanticsEnabled = !measurementOnly)
                if (retentionMessage.retention != null) Spacer(Modifier.width(4.dp))
            MessageTime(
                messageId = messageId,
                timeLabel = timeLabel,
                outgoing = false,
                deliveryState = deliveryState,
                testTagEnabled = !measurementOnly,
                verticalOffsetPx = timestampVerticalOffsetPx,
            )
            }
        }
        if (!outgoing && showTime && summary.isNotEmpty()) Spacer(Modifier.width(4.dp))
        if (summary.isNotEmpty()) {
            ReactionRow(
                messageId = messageId,
                summary = summary,
                interactive = !measurementOnly,
                onReaction = onReaction,
                onShowActions = onShowActions,
                onLongPress = metadataLongPress.takeIf { onLongPress != null },
            )
        }
        if (outgoing && showTime && summary.isNotEmpty()) Spacer(Modifier.width(4.dp))
        if (outgoing && showTime) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MessageExpiryIndicator(retentionMessage, semanticsEnabled = !measurementOnly)
                if (retentionMessage.retention != null) Spacer(Modifier.width(4.dp))
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
    val deliveryLabel = if (outgoing || deliveryState == MessageDeliveryState.Streaming) {
        stringResource(
            when (deliveryState) {
                MessageDeliveryState.Streaming -> R.string.message_streaming
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
        if (outgoing || deliveryState == MessageDeliveryState.Streaming) {
            when (deliveryState) {
                MessageDeliveryState.Sending, MessageDeliveryState.Streaming -> CircularProgressIndicator(
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
    onLongPress: (() -> Unit)?,
) {
    val showActionsLabel = stringResource(R.string.show_message_actions)
    Row(
        horizontalArrangement = Arrangement.spacedBy(ReactionPillSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        summary.forEachIndexed { index, reaction ->
            val interactionSource = remember(messageId, index, reaction.emoji) {
                MutableInteractionSource()
            }
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
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { reaction.emoji?.let(onReaction) ?: onShowActions() },
                                )
                                .semantics {
                                    contentDescription = description
                                    stateDescription = selectedState
                                    if (onLongPress != null) {
                                        onLongClick(showActionsLabel) {
                                            onLongPress()
                                            true
                                        }
                                    }
                                }
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .then(
                            if (interactive) {
                                Modifier.testTag("conversation.reaction.pill.$messageId.$index")
                            } else {
                                Modifier
                            },
                        )
                        .height(ReactionPillMinimumHeight)
                        .then(
                            if (reaction.emoji != null && reaction.personCount == 1) {
                                Modifier.requiredWidth(ReactionPillMinimumWidth)
                            } else {
                                Modifier.widthIn(min = ReactionPillMinimumWidth)
                            },
                        )
                        .clip(CircleShape)
                        .indication(
                            interactionSource,
                            ripple(color = MaterialTheme.colorScheme.onSurface),
                        )
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
                        modifier = Modifier.padding(
                            horizontal = if (reaction.emoji != null && reaction.personCount == 1) 3.dp else 7.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(ReactionContentSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (reaction.emoji != null) {
                            SignalEmoji(
                                emoji = reaction.emoji,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Text(
                                text = "+${reaction.omittedTypeCount}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp,
                                ),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (reaction.emoji != null && reaction.personCount > 1) {
                            Text(
                                text = reaction.personCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp,
                                ),
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
    val rosterController = LocalGroupWork.current
    val lifecycleController = LocalGroupLifecycle.current
    LaunchedEffect(profile.id, chat.id, lifecycleController?.stateScenario) { lifecycleController?.open(dev.ipf.whitenoise.model.GroupOwner(profile.id, chat.id)) }
    when (availability) {
        ComposerAvailability.PendingInvitation -> InvitationActions(chat, onDecline, onAccept)
        ComposerAvailability.Available -> Unit
        ComposerAvailability.MembershipUnknown -> ConversationRecovery(
            title = stringResource(R.string.group_membership_check), detail = stringResource(R.string.group_roster_unknown),
            actionLabel = stringResource(R.string.lifecycle_retry),
            onAction = { rosterController?.retryRoster(dev.ipf.whitenoise.model.GroupOwner(profile.id, chat.id)) },
        )
        ComposerAvailability.Unrecoverable -> ConversationStatus(stringResource(R.string.group_frozen_notice))
        ComposerAvailability.Disbanding -> ConversationStatus(stringResource(R.string.group_disband_pending))
        ComposerAvailability.Disbanded -> ConversationStatus(stringResource(R.string.group_ended_notice))
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

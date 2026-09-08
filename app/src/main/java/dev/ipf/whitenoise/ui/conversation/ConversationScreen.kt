package dev.ipf.whitenoise.ui.conversation

import dev.ipf.whitenoise.ui.theme.amoledOutline
import dev.ipf.whitenoise.ui.theme.outlineSelectionColor
import dev.ipf.whitenoise.ui.theme.isAmoledOutline
import dev.ipf.whitenoise.ui.theme.amoledOutlineBorder
import dev.ipf.whitenoise.ui.theme.amoledMessageBorder
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.minimumInteractiveComponentSize

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.semantics.liveRegion
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
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Chat
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
    onSetMessagePinned: (String, Boolean) -> Boolean = { _, _ -> false },
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
    onEditMessage: (String, String, Int) -> Boolean = { _, _, _ -> false },
    onAdvanceMessageEdit: (String, Long) -> Unit = { _, _ -> },
    onRetryMessageEdit: (String) -> Unit = {},
    onDiscardMessageEdit: (String) -> Unit = {},
    onInterruptMessageEdits: () -> Unit = {},
    onTranslationPreferences: (dev.ipf.whitenoise.model.TranslationPreferences) -> Unit = {},
    onTranslationScenario: () -> dev.ipf.whitenoise.model.TranslationScenario = { dev.ipf.whitenoise.model.TranslationScenario.Ready },
) {
    val floatingMessages = LocalFloatingMessages.current
    val floatingComposerHeight = LocalFloatingComposerHeight.current
    val floatingObscured = LocalFloatingObscured.current

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
    var highlightedDateId by remember(chat.id) { mutableStateOf<String?>(null) }
    var dateHighlightDescription by remember(chat.id) { mutableStateOf<String?>(null) }
    var showJumpDate by rememberSaveable(profile.id, chat.id) { mutableStateOf(false) }
    var dateAnchorId by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    val dateZoneId by rememberSaveable(profile.id, chat.id) { mutableStateOf(java.time.ZoneId.systemDefault().id) }
    val dateIndex = remember(chat.timeline, dateZoneId) { dev.ipf.whitenoise.model.ConversationDates(chat, java.time.ZoneId.of(dateZoneId)) }
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
    var selectingTextId by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var selectingTextSource by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var historyMessageId by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var readerStartsSelection by rememberSaveable(profile.id, chat.id) { mutableStateOf(false) }
    var searchQuery by rememberSaveable(chat.id) { mutableStateOf("") }
    var pinnedSearchMessageId by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var preSearchAnchor by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var preSearchOffset by rememberSaveable(profile.id, chat.id) { mutableIntStateOf(0) }
    var handledSearchRequest by rememberSaveable(profile.id, chat.id) { mutableLongStateOf(0L) }
    var pendingInitialMessageId by rememberSaveable(profile.id, chat.id, initialMessageId, notificationRequestId) {
        mutableStateOf(initialMessageId ?: history.boundaryId)
    }
    var initialViewportSettled by rememberSaveable(chat.id) { mutableStateOf(false) }
    var sentLocationTarget by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var pendingEndSettlement by remember(chat.id) { mutableStateOf(false) }
    var compactComposerHeightPx by remember(chat.id) { mutableIntStateOf(0) }
    androidx.compose.runtime.SideEffect { floatingComposerHeight(compactComposerHeightPx.toFloat()) }
    androidx.compose.runtime.DisposableEffect(chat.id) {
        onDispose { floatingComposerHeight(0f); floatingObscured(false) }
    }
    var composerOverlayActive by remember(chat.id) { mutableStateOf(false) }
    var composerPresentationActive by remember(chat.id) { mutableStateOf(false) }
    androidx.compose.runtime.SideEffect { floatingObscured(composerOverlayActive || composerPresentationActive || focusedMessageId != null) }
    var composerTravelPx by remember(chat.id) { mutableFloatStateOf(0f) }
    var pushTimelineWithComposer by remember(chat.id) { mutableStateOf(false) }
    val messageBounds = remember(chat.id) { mutableStateMapOf<String, Rect>() }
    val context = LocalContext.current
    val dateLocale = androidx.core.os.ConfigurationCompat.getLocales(androidx.compose.ui.platform.LocalConfiguration.current)[0]
        ?: java.util.Locale.ROOT
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
    val translations = remember(profile.id, chat.id) { TranslationController(profile.id, chat.id) }
    var translationMessageId by remember(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var translationPendingGeneration by remember(profile.id, chat.id) { mutableStateOf<Long?>(null) }
    var translationSource by remember(profile.id, chat.id) { mutableStateOf<ChatMessage?>(null) }
    val translationVisibleIds by remember(items, listState) { derivedStateOf { listState.layoutInfo.visibleItemsInfo.mapNotNull { (items.getOrNull(it.index) as? ConversationItem.MessageItem)?.message?.id }.toSet() } }
    SideEffect { translations.observe(messages.filter { it.id in history.windowIds }, profile.settings.translation, translationVisibleIds, onTranslationScenario) }
    TranslationWork(translations)
    fun displayedMessage(message: ChatMessage) = message.copy(text = if (isSearching) message.text else translations.text(message))
    val readingText = speechSession?.current?.item
    LaunchedEffect(readingText, translations.entries, isSearching) {
        if (readingText != null && speechSession?.owner?.chatId == chat.id) {
            messages.firstOrNull { it.id == readingText.id }?.let {
                if (displayedMessage(it).text != readingText.authored) readAloudController.stop()
            }
        }
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

    var selectedPinId by rememberSaveable(profile.id, chat.id) { mutableStateOf<String?>(null) }
    var showPinnedMessages by rememberSaveable(profile.id, chat.id) { mutableStateOf(false) }
    val pinnedMessages = dev.ipf.whitenoise.model.MessagePins.entries(chat)
    val pinIndex = pinnedMessages.indexOfFirst { it.id == selectedPinId }.coerceAtLeast(0)
    fun openPinnedMessage(messageId: String) {
        isSearching = false
        searchQuery = ""
        pinnedSearchMessageId = null
        isSelecting = false
        selectedMessageIds = emptySet()
        selectingTextId = null
        focusedMessageId = null
        selectedPinId = messageId
        pendingInitialMessageId = null
        initialViewportSettled = true
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

    fun handleAction(sourceMessage: ChatMessage, action: MessageAction) {
        val message = messages.firstOrNull { it.id == sourceMessage.id } ?: sourceMessage
        focusedMessageId = null
        selectingTextId = null
        if (action in setOf(MessageAction.Edit, MessageAction.EditHistory, MessageAction.OpenMessage, MessageAction.SelectText)) {
            history.cancel()
            pendingInitialMessageId = null
            initialViewportSettled = true
        }
        when (action) {
            MessageAction.KeepOnScreen -> floatingMessages?.keep(profile.id, chat.id, message.id)
            MessageAction.Translate -> { readerMessageId = null; translationPendingGeneration = null; translationSource = message; translationMessageId = message.id }
            MessageAction.RetrySend -> onRetry(message.id)
            MessageAction.Edit -> { readerMessageId = null; editMessageId = message.id }
            MessageAction.EditHistory -> { readerMessageId = null; historyMessageId = message.id }
            MessageAction.RetryEdit -> onRetryMessageEdit(message.id)
            MessageAction.DiscardEdit -> onDiscardMessageEdit(message.id)
            MessageAction.OpenMessage -> {
                readerStartsSelection = false
                readerMessageId = message.id
            }
            MessageAction.SelectText -> {
                readerMessageId = null
                isSelecting = false
                selectedMessageIds = emptySet()
                readAloudController.follow(false)
                selectingTextSource = displayedMessage(message).text
                selectingTextId = message.id
            }
            MessageAction.Reply -> beginReply(message.id)
            MessageAction.Share, MessageAction.SaveAttachments -> {
                exportMessageId = message.id; exportSharing = action == MessageAction.Share
            }
            MessageAction.Forward -> forwardMessageIds = setOf(message.id)
            MessageAction.Pin, MessageAction.Unpin -> {
                if (onSetMessagePinned(message.id, action == MessageAction.Pin)) selectedPinId = message.id
            }
            MessageAction.Copy -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Message", displayedMessage(message).plainVisibleText(profile.id)))
            }
            MessageAction.CopyMarkdown -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Message", displayedMessage(message).text))
            }
            MessageAction.ReadAloud -> readAloudController.startDisplayedMessage(profile, chat, displayedMessage(message))
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
            val dividerIndex = items.indexOfFirst { it.id == "history.unread" }
            val landingIndex = if (initialMessageId == null && target == history.boundaryId && dividerIndex >= 0) dividerIndex else targetIndex
            listState.scrollToItem(landingIndex)
            pendingInitialMessageId = null
            initialViewportSettled = true
        } else if (target != null) {
            if (history.request == null && history.readyTarget == null) history.target(chat, target, onHistoryScenario(HistoryOperation.Target), highlight = initialMessageId != null)
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
        if (isSearching && !showJumpDate && history.request?.dateJump != true && history.readyTarget?.dateJump != true) history.target(chat, messageId, onHistoryScenario(HistoryOperation.Target))
    }
    val readyTarget = history.readyTarget
    LaunchedEffect(readyTarget?.id) {
        val target = readyTarget ?: return@LaunchedEffect
        val index = items.indexOfFirst { it.id == target.targetId && ((it is ConversationItem.MessageItem && !it.message.isDeleted) || (target.dateJump && it is ConversationItem.EventItem)) }
        if (index < 0) return@LaunchedEffect
        val dividerIndex = items.indexOfFirst { it.id == "history.unread" }
        val landingAtDivider = !initialViewportSettled && initialMessageId == null &&
            pendingInitialMessageId == history.boundaryId && target.targetId == history.boundaryId && dividerIndex >= 0
        if (target.dateJump) {
            isSearching = false
            searchQuery = ""
            pinnedSearchMessageId = null
            preSearchAnchor = null
            // Let the normal chat header/composer settle before placing the target.
            repeat(2) { withFrameNanos { } }
            if (history.readyTarget?.id != target.id) return@LaunchedEffect
        }
        val dateHeaderIndex = dayHeaderIndices.lastOrNull { it < index }
        val landingIndex = when {
            landingAtDivider -> dividerIndex
            target.dateJump && dateHeaderIndex == index - 1 -> dateHeaderIndex
            else -> index
        }
        listState.scrollToItem(landingIndex, target.scrollOffset)
        repeat(2) { withFrameNanos { } }
        if (history.readyTarget?.id != target.id) return@LaunchedEffect
        if (listState.layoutInfo.visibleItemsInfo.none { it.key == target.targetId || (landingAtDivider && it.key == "history.unread") }) {
            history.request = target.copy(phase = HistoryPhase.Failed)
            history.readyTarget = null
            return@LaunchedEffect
        }
        pendingInitialMessageId = null
        initialViewportSettled = true
        if (target.highlight) {
            highlightedMessageId = target.targetId
            if (target.dateJump) {
                highlightedDateId = dateHeaderIndex?.let { items[it].id }
                dateHighlightDescription = dateIndex.entries.firstOrNull { it.id == target.targetId }?.date?.format(
                    java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL)
                        .withLocale(dateLocale))
            }
            try {
                delay(1_400)
            } finally {
                if (highlightedMessageId == target.targetId) {
                    highlightedMessageId = null
                    highlightedDateId = null
                    dateHighlightDescription = null
                }
            }
        }
        if (history.readyTarget?.id == target.id) history.readyTarget = null
    }
    val tailWasLoaded = ConversationProjection.orderedEntries(chat).lastOrNull { it.id in history.observedIds }?.id in history.windowIds
    val hasNewerHistory = ConversationHistory.hasNewer(chat, history.windowIds)
    val tailJump = remember(profile.id, chat.id, density.density, density.fontScale) {
        dev.ipf.whitenoise.model.ConversationTailJump()
    }
    var farFromTail by remember(profile.id, chat.id) { mutableStateOf(false) }
    LaunchedEffect(listState, items, hasNewerHistory, tailJump) {
        val keys = items.map { it.id }
        val related = with(density) { WhiteNoiseSpacing.Related.roundToPx() }
        snapshotFlow { listState.layoutInfo }.collect { layout ->
            farFromTail = tailJump.update(
                keys = keys,
                rows = layout.visibleItemsInfo.map {
                    dev.ipf.whitenoise.model.ConversationTailJump.Row(it.key as String, it.offset, it.size)
                },
                viewportPx = layout.viewportEndOffset - layout.viewportStartOffset -
                    (layout.afterContentPadding - related).coerceAtLeast(0),
                viewportEndPx = layout.viewportEndOffset,
                afterPaddingPx = layout.afterContentPadding,
                spacingPx = layout.mainAxisItemSpacing,
                widthPx = layout.viewportSize.width,
                hasNewer = hasNewerHistory,
            )
        }
    }
    val nearTail = initialViewportSettled && tailWasLoaded && !listState.canScrollForward
    LaunchedEffect(chat.timeline) {
        val follow = nearTail && !isSearching && !isSelecting && selectingTextId == null && focusedMessageId == null
        val hadArrival = chat.timeline.any { it.id !in history.observedIds }
        history.reconcileArrivals(chat, profile.id, follow)
        if (hadArrival && follow) pendingEndSettlement = true
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, profile.id, chat.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                selectingTextId = null
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
        editMessageId == null && readerMessageId == null && selectingTextId == null && translationMessageId == null && historyMessageId == null && exportMessageId == null &&
        !attachmentReaderPresented && !operationCovered && viewerSelection == null && forwardMediaKey == null && forwardMessageIds == null && deleteMessageIds == null &&
        !showJumpDate && !showPinnedMessages && !showEmojiPicker && !showConfigureReactions && configureReactionSlot == null && !showDeclineConfirmation &&
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
        initialViewportSettled, readerMessageId != null, isSearching, isSelecting, selectingTextId, focusedMessageId) {
        val speech = speechSession ?: return@LaunchedEffect
        if (!speech.following || speech.phase in setOf(dev.ipf.whitenoise.model.SpeechPhase.Unavailable, dev.ipf.whitenoise.model.SpeechPhase.Completed) ||
            !initialViewportSettled || isSearching || isSelecting || selectingTextId != null || focusedMessageId != null) return@LaunchedEffect
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
    val selectingTextMessage = messages.firstOrNull { it.id == selectingTextId && !it.isDeleted && displayedMessage(it).text == selectingTextSource && it.editAttempt == null }
    LaunchedEffect(selectingTextId, selectingTextMessage) {
        if (selectingTextMessage == null) selectingTextId = null
    }
    CompositionLocalProvider(LocalMessageTranslation provides translations, LocalSpeechOwner provides dev.ipf.whitenoise.model.SpeechOwner(profile.id, chat.id), LocalMessageReading provides MessageReadingActions(
        collapse = chat.collapseLongMessages && !isSearching,
        canWrite = chat.composerAvailability(profile) == ComposerAvailability.Available,
        selectingTextId = selectingTextMessage?.id,
        dismissTextSelection = { selectingTextId = null },
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
                    onJumpDate = {
                        history.cancel()
                        dateAnchorId = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                            items.getOrNull(info.index).let { it is ConversationItem.MessageItem || it is ConversationItem.EventItem }
                        }?.key as? String
                        showJumpDate = true
                    },
                )
                else -> ConversationTopBar(
                    chat = chat,
                    onBack = onBack,
                    onInfo = onOpenChatInfo,
                    onDeveloperTools = onOpenDeveloperTools,
                )
            }
            if (!isSelecting && !isSearching && pinnedMessages.isNotEmpty()) {
                PinnedMessageBanner(profile, chat, pinnedMessages[pinIndex], pinIndex, pinnedMessages.size,
                    onOpen = { openPinnedMessage(pinnedMessages[pinIndex].id) },
                    onNext = { selectedPinId = pinnedMessages[(pinIndex + 1) % pinnedMessages.size].id },
                    onUnpin = { onSetMessagePinned(pinnedMessages[pinIndex].id, false) },
                    onViewAll = { showPinnedMessages = true })
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
                // Scaffold already supplies 16 dp below its FAB slot; avoid adding that gap
                // again above the measured composer. Preserve the native 48 dp touch target.
                val composerClearance = (with(density) { compactComposerHeightPx.toDp() } -
                    WhiteNoiseSpacing.CompactScreenMargin).coerceAtLeast(0.dp)
                Column(Modifier.imePadding().padding(bottom = composerClearance),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                    if (!nearTail && initialViewportSettled && farFromTail) SmallFloatingActionButton(onClick = {
                        val target = ConversationProjection.orderedEntries(chat).filterIsInstance<ChatTimelineEntry.Message>().lastOrNull { !it.message.isDeleted }?.id
                        if (target != null) {
                            history.target(chat, target, onHistoryScenario(HistoryOperation.Target), highlight = false)
                        }
                    }, modifier = Modifier.minimumInteractiveComponentSize().size(40.dp).amoledOutline(CircleShape).testTag("history.jumpLatest"),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp,
                        ),
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surfaceDim.copy(alpha = PinnedDayHeaderSurfaceAlpha),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_down),
                            contentDescription = stringResource(R.string.history_jump_latest),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { contentPadding ->
        val overlaysBottomContent = showsAvailableComposer || isSearching
        val appliedContentPadding = if (overlaysBottomContent) {
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
        // Search controls float above the transcript. Keep their measured clearance
        // inside the scrolling content, excluding the IME handled by the viewport.
        val transcriptBottomPadding = if (isSearching) {
            (contentPadding.calculateBottomPadding() -
                WindowInsets.ime.asPaddingValues().calculateBottomPadding()).coerceAtLeast(0.dp)
        } else {
            bottomSafePadding + with(density) { compactComposerHeightPx.toDp() }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(appliedContentPadding)
                .consumeWindowInsets(appliedContentPadding)
                .then(if (overlaysBottomContent) Modifier.imePadding() else Modifier),
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
                        bottom = WhiteNoiseSpacing.Related + transcriptBottomPadding,
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
                                    highlightDescription = dateHighlightDescription.takeIf { highlightedDateId == item.id },
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
                                    "history.unread" -> UnreadMessagesDivider(history.unreadDivider?.messageIds?.size ?: 0)
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
                                    onShowActions = { selectingTextId = null; focusedMessageId = item.message.id },
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
                            highlightDescription = dateHighlightDescription.takeIf { highlightedDateId == dayHeader.id },
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
                        writingToolsEnabled = editMessageId == null && readerMessageId == null && selectingTextId == null,
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
                                if ((wasAtBottom || !initialViewportSettled) &&
                                    history.readyTarget?.dateJump != true && history.request?.dateJump != true) {
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

    if (showJumpDate) ConversationDatePicker(dateIndex, dateAnchorId,
        onDismiss = { showJumpDate = false },
        onJump = { day ->
            dateIndex.target(day)?.let { target ->
                showJumpDate = false
                pendingInitialMessageId = null
                initialViewportSettled = true
                pendingEndSettlement = false
                history.target(chat, target.id, onHistoryScenario(HistoryOperation.Target), dateJump = true)
            }
        })

    val translationMessage = messages.firstOrNull { it.id == translationMessageId && it == translationSource && dev.ipf.whitenoise.model.TranslationExamples.eligible(it) && it.id in history.windowIds }
    if (translationMessageId != null && translationMessage == null) LaunchedEffect(translationMessageId) { translationMessageId = null }
    val translationResult = translationMessage?.let(translations::state)
    LaunchedEffect(translationPendingGeneration, translationResult) {
        if (translationPendingGeneration != null && translationResult?.generation == translationPendingGeneration &&
            translationResult?.phase == dev.ipf.whitenoise.model.TranslationPhase.Ready) {
            translationPendingGeneration = null; translationMessageId = null
        }
    }
    if (translationMessage != null) MessageTranslationSheet(translationMessage, translationResult,
        onSelect = { language ->
            onTranslationPreferences(profile.settings.translation.copy(lastManual = language))
            translations.request(translationMessage.id, language, scenario = onTranslationScenario())
            translationPendingGeneration = translations.state(translationMessage)?.generation
        },
        onToggle = { translations.toggle(translationMessage.id); translationPendingGeneration = null; translationMessageId = null },
        onRetry = { translations.retry(translationMessage.id); translationPendingGeneration = translations.state(translationMessage)?.generation },
        onDownload = { translations.download(translationMessage.id); translationPendingGeneration = translations.state(translationMessage)?.generation },
        onCancel = { translations.cancel(translationMessage.id); translationPendingGeneration = null; translationMessageId = null },
        onDismiss = {
            if (translationResult?.phase in setOf(dev.ipf.whitenoise.model.TranslationPhase.Loading, dev.ipf.whitenoise.model.TranslationPhase.Downloading)) translations.cancel(translationMessage.id)
            translationPendingGeneration = null; translationMessageId = null
        })
    if (showPinnedMessages) PinnedMessagesSheet(profile, chat,
        onDismiss = { showPinnedMessages = false },
        onOpen = { showPinnedMessages = false; openPinnedMessage(it) },
        onUnpin = { onSetMessagePinned(it, false) })

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
        MessageReaderDialog(profile, chat, displayedMessage(message), readerStartsSelection, speechActionState(message), readAloudController,
            onDismiss = { readerMessageId = null }, onAction = { action ->
                if (action !in setOf(MessageAction.Copy, MessageAction.CopyMarkdown, MessageAction.ReadAloud, MessageAction.StopReading, MessageAction.RetryEdit, MessageAction.DiscardEdit)) readerMessageId = null
                handleAction(message, action)
            }, onReact = { readerMessageId = null; focusedMessageId = message.id }, onPerson = { readerMessageId = null; onOpenPersonProfile(it) })
    }
    }
    // Registered after the composer: selection consumes Back before expansion or navigation.
    BackHandler(enabled = selectingTextId != null) { selectingTextId = null }
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
    val actions = remember(message, profile.id, speechActionState, chat) {
        MessageActionPolicy.available(message, profile.id, speechActionState, chat.composerAvailability(profile) == ComposerAvailability.Available, canPin = dev.ipf.whitenoise.model.MessagePins.canManage(chat, profile.id), pinned = message.id in chat.pinnedMessageIds)
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
        (dialogHeightPx - topInsetPx - marginPx).coerceAtLeast(0).toDp()
    }
    val bottomContentInset = with(density) { (bottomInsetPx + marginPx).toDp() }
    val desiredTop = ((sourceBounds?.center?.y ?: (dialogHeightPx / 2f)) - contentHeightPx / 2f).roundToInt()
    val minimumTop = topInsetPx + marginPx
    val maximumTop = (dialogHeightPx - contentHeightPx).coerceAtLeast(minimumTop)
    val contentTop = desiredTop.coerceIn(minimumTop, maximumTop)
    val forwardedLabel = stringResource(R.string.message_forwarded)
    val previewDescription = buildString {
        append(if (outgoing) profile.name else profile.people.firstOrNull { it.id == message.authorId }?.displayName ?: chat.title)
        if (message.isForwarded && !message.isDeleted) append(", $forwardedLabel")
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
                    .testTag("message.actions.scroll")
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
                        border = amoledOutlineBorder(),
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
                                                outlineSelectionColor(MaterialTheme.colorScheme.primaryContainer)
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("message.actions.preview")
                        .clearAndSetSemantics { contentDescription = previewDescription },
                ) {
                    CompositionLocalProvider(LocalFocusedMessagePreview provides true) {
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
                Spacer(Modifier.height(bottomContentInset))
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
private fun SelectionTopBar(onClose: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close_selection),
                )
            }
        },
        title = { Text(stringResource(R.string.select_messages)) },
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
    onJumpDate: () -> Unit,
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
        actions = {
            IconButton(onClick = { keyboardController?.hide(); onJumpDate() }, modifier = Modifier.testTag("conversation.search.calendar")) {
                Icon(painterResource(R.drawable.ic_calendar_month), stringResource(R.string.jump_to_date))
            }
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
    highlightDescription: String? = null,
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
                .background(if (highlightDescription != null) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, CircleShape)
                .padding(horizontal = 12.dp, vertical = 3.dp)
                .semantics { heading(); if (highlightDescription != null) { stateDescription = highlightDescription; liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite } },
            style = MaterialTheme.typography.labelMedium,
            color = if (highlightDescription != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PinnedDayHeader(
    label: String,
    modifier: Modifier = Modifier,
    highlightDescription: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = WhiteNoiseSpacing.FormField),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            border = amoledOutlineBorder(),
            shape = CircleShape,
            color = if (highlightDescription != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceDim.copy(
                alpha = PinnedDayHeaderSurfaceAlpha,
            ),
            modifier = Modifier
                .testTag("conversation.date.pinned")
                .semantics { heading(); if (highlightDescription != null) { stateDescription = highlightDescription; liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite } },
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (highlightDescription != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun UnreadMessagesDivider(count: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("history.unread")
            .padding(top = WhiteNoiseSpacing.Section, bottom = WhiteNoiseSpacing.FormField)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Text(
            text = pluralStringResource(R.plurals.unread_count, count, count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
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
                border = amoledOutlineBorder(),
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

/** Message content for the floating card, without direction alignment or an enclosing bubble. */
@Composable
internal fun ReadOnlyMessageContent(profile: Profile, chat: Chat, message: ChatMessage) {
    val item = remember(chat, message.id) {
        ConversationProjection.items(chat).filterIsInstance<ConversationItem.MessageItem>().firstOrNull { it.id == message.id }
    }
    val controller = LocalReadAloudController.current ?: remember { ReadAloudController() }
    val text = dev.ipf.whitenoise.model.MessageEditing.displayedText(message)
    val operation = message.agentOperation
    CompositionLocalProvider(LocalMessageReading provides MessageReadingActions(collapse = false, canWrite = false)) {
        Column(Modifier.fillMaxWidth().testTag("floating.content.${message.id}"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item != null && message.replyToMessageId != null) ReplyQuote(profile, item, outgoing = false,
                onOpenReplyTarget = {}, canOpenReplyTarget = false)
            if (message.attachments.isNotEmpty()) TimelineAttachmentContent(message.attachments, outgoing = false,
                onOpenMedia = {}, messageId = message.id, people = profile.people, modifier = Modifier.fillMaxWidth())
            if (message.nostrEvents.isNotEmpty()) NostrEventCards(message, profile, onRetry = { _, _ -> }, onOpenPerson = {})
            if (operation != null) {
                Text(operation.name, style = MaterialTheme.typography.titleSmall)
                Text(operation.summary, style = MaterialTheme.typography.bodyMedium)
                operation.result?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                operation.statusDetail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (text.isNotBlank()) MessageBubbleText(containerColor = MaterialTheme.colorScheme.surfaceContainer,
                profile = profile, message = message, text = text, plainText = dev.ipf.whitenoise.model.InlineMessageMarkup.plainText(text),
                searchQuery = "", onOpenPersonProfile = {}, memberIds = null, readAloudController = controller,
                modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Reuse the full transcript renderer; the enclosing result owns interaction and semantics. */
@Composable
internal fun ReadOnlyMessageBubble(profile: Profile, chat: Chat, item: ConversationItem.MessageItem, searchQuery: String = "") {
    val controller = LocalReadAloudController.current ?: remember { ReadAloudController() }
    CompositionLocalProvider(LocalMessageReading provides MessageReadingActions(collapse = false, canWrite = false)) {
        MessageRow(profile = profile, chat = chat, item = item,
            speechActionState = MessageSpeechActionState(), onRetry = {}, onOpenMedia = {},
            isSelectionMode = false, selected = false, searchAlpha = 1f, searchQuery = searchQuery, searchPosition = null,
            onToggleSelection = {}, onShowActions = {}, onAccessibilityAction = {}, onSwipeReply = { false },
            onReaction = {}, readAloudController = controller, contextPreview = true)
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
    val selectingText = !contextPreview && LocalMessageReading.current.selectingTextId == message.id
    val outgoing = message.authorId == profile.id
    val author = profile.people.firstOrNull { it.id == message.authorId }
    val authorName = if (outgoing) stringResource(R.string.you) else author?.displayName ?: chat.title
    val verticalPadding = when {
        item.startsCluster && !contextPreview -> WhiteNoiseSpacing.ConversationCluster
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
    val hasMetadata = message.reactions.isNotEmpty()
    val metadataGeometry = messageMetadataGeometry(
        hasReactions = message.reactions.isNotEmpty(),
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
        canPin = dev.ipf.whitenoise.model.MessagePins.canManage(chat, profile.id),
        pinned = message.id in chat.pinnedMessageIds,
    ).map { action ->
        CustomAccessibilityAction(actionLabel(action)) {
            onAccessibilityAction(action)
            true
        }
    }
    val availableActions = MessageActionPolicy.available(message, profile.id, speechActionState, chat.composerAvailability(profile) == ComposerAvailability.Available, canPin = dev.ipf.whitenoise.model.MessagePins.canManage(chat, profile.id), pinned = message.id in chat.pinnedMessageIds)
    val failedOutgoing = !message.isDeleted && outgoing && message.deliveryState == MessageDeliveryState.Failed
    val retryLabel = stringResource(R.string.not_delivered_retry)
    val messageInteractionSource = remember(message.id) { MutableInteractionSource() }
    val contextPreviewInteraction = remember(message.id) { MutableInteractionSource() }
    val showMessageActions = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (message.isDeleted) onAccessibilityAction(MessageAction.Delete) else onShowActions()
    }
    val canSwipeReply = !contextPreview && !isSelectionMode && !selectingText && MessageAction.Reply in availableActions
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
    val interactionModifier = if (contextPreview || selectingText) {
        Modifier
    } else if (isSelectionMode) {
        Modifier.toggleable(
            value = selected,
            role = Role.Checkbox,
            interactionSource = messageInteractionSource,
            indication = null,
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
                                !contextPreview && !isSelectionMode && !selectingText && availableActions.isNotEmpty()
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
private const val MaximumVisibleReactionTypes = 4

private data class MessageMetadataGeometry(
    val overlapPx: Int,
    val reservePx: Int,
    val invisibleTargetBottomPx: Int,
)

@Composable
private fun messageMetadataGeometry(hasReactions: Boolean): MessageMetadataGeometry {
    if (!hasReactions) return MessageMetadataGeometry(0, 0, 0)
    val density = LocalDensity.current
    val target = with(density) { MessageMetadataTargetHeight.roundToPx() }
    val pill = with(density) { ReactionPillMinimumHeight.roundToPx() }
    val inset = ((target - pill).coerceAtLeast(0)) / 2
    val overlap = inset + with(density) { ReactionVisibleOverlap.roundToPx() }
    return MessageMetadataGeometry(overlap, (target - overlap).coerceAtLeast(0),
        (target - inset - pill).coerceAtLeast(0))
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
    val hasMetadata = hasReactions
    val metadataGeometry = messageMetadataGeometry(
        hasReactions = hasReactions,
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
                    showTime = showTime,
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
                    alignEnd = if (showTime) !outgoing else outgoing,
                    summary = summary,
                    fillWidth = false,
                    measurementOnly = true,
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
                showTime = showTime,
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
                alignEnd = if (showTime) !outgoing else outgoing,
                summary = summary,
                fillWidth = true,
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
        val reportedReservePx = (metadata.height - overlapPx - contextTrimPx).coerceAtLeast(0)
        val metadataY = bubble.height - overlapPx
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
    showTime: Boolean,
) {
    val selectingText = LocalMessageReading.current.selectingTextId == message.id
    val translation = LocalMessageTranslation.current
    val authoredText = if (message.isDeleted || searchQuery.isNotBlank()) message.visibleText(profile.id)
        else if (translation?.state(message)?.phase == dev.ipf.whitenoise.model.TranslationPhase.Ready) translation.text(message)
        else dev.ipf.whitenoise.model.MessageEditing.displayedText(message)
    val eventOnly = !selectingText && searchQuery.isBlank() && message.nostrEvents.isNotEmpty() &&
        authoredText.trim() in message.nostrEvents.map { it.authoredReference.trim() }
    val text = authoredText.takeUnless { eventOnly }.orEmpty()
    val plainText = dev.ipf.whitenoise.model.InlineMessageMarkup.plainText(text)
    val forwardedLabel = stringResource(R.string.message_forwarded)
    val description = buildString {
        append(authorName)
        if (message.isForwarded && !message.isDeleted) append(", $forwardedLabel")
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
            outgoing = outgoing,
            isForwarded = message.isForwarded,
            onLongPress = onLongPress,
            footer = if (showTime) ({ MessageBubbleTime(message, outgoing, MaterialTheme.colorScheme.surfaceContainerHigh) }) else null,
            modifier = Modifier
                .testTag("conversation.message.bubble.${message.id}")
                .onGloballyPositioned { onPositioned(it.boundsInRoot()) }
                .then(if (selectingText) Modifier else Modifier.semantics(mergeDescendants = true) { contentDescription = description }),
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
        theme = dev.ipf.whitenoise.ui.theme.LocalAppearanceColorTheme.current,
    )
    val readableBubble = dev.ipf.whitenoise.model.AppearanceColorPolicy.readable(bubbleOverride)
    val bubbleContainerColor = readableBubble?.containerArgb
        ?.let(::colorFromOpaqueArgb)
        ?: if (outgoing) defaultBubbleColors.mineContainer else defaultBubbleColors.otherContainer
    val bubbleContentColor = readableBubble?.contentArgb
        ?.let(::colorFromOpaqueArgb)
        ?: if (outgoing) defaultBubbleColors.mineContent else defaultBubbleColors.otherContent
    val footer: (@Composable () -> Unit)? = if (showTime) {
        { MessageBubbleTime(message, outgoing, bubbleContainerColor) }
    } else null
    val hasRichContent = !message.isDeleted &&
        (message.replyToMessageId != null || message.attachments.isNotEmpty() || message.nostrEvents.isNotEmpty())
    val singleMediaSize = rememberTimelineSingleMediaSize(message.attachments.singleOrNull())
    val previewMediaScale = if (LocalFocusedMessagePreview.current && message.attachments.any { it.isVisual() }) FocusedPreviewMediaScale else 1f
    val richCanvasWidth = richContentCanvasWidthDp(message.attachments, singleMediaSize).dp * previewMediaScale
    Surface(
        border = amoledMessageBorder(outgoing),
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
            .then(if (selectingText) Modifier else Modifier.semantics(mergeDescendants = true) { contentDescription = description }),
    ) {
        dev.ipf.whitenoise.ui.theme.MessageSelectionColors(bubbleContainerColor, bubbleContentColor) {
            Box(propagateMinConstraints = true) {
                if (hasRichContent) {
                    Column(
                        modifier = Modifier
                            .padding(ConversationMessageMetrics.RichOuterInset)
                            .width(richCanvasWidth),
                        verticalArrangement = Arrangement.spacedBy(
                            ConversationMessageMetrics.RichContentSpacing,
                        ),
                    ) {
                        if (message.isForwarded) {
                            ForwardedMessageLabel(
                                message.id, selectingText, bubbleContainerColor,
                                Modifier.padding(horizontal = ConversationMessageMetrics.RichTextHorizontalAdjustment),
                            )
                        }
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
                                footer = footer,
                                containerColor = bubbleContainerColor,
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
                        } else if (footer != null) {
                            MessageFooterRow(footer, Modifier.padding(start = ConversationMessageMetrics.RichTextHorizontalAdjustment,
                                end = ConversationMessageMetrics.RichTextHorizontalAdjustment, bottom = ConversationMessageMetrics.RichTextBottomAdjustment))
                        }
                    }
                } else {
                    MessageBubbleText(
                        footer = footer,
                        containerColor = bubbleContainerColor,
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
}

@Composable
private fun MessageBubbleText(
    containerColor: Color,
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
    footer: (@Composable () -> Unit)? = null,
) {
    var unavailableProfile by rememberSaveable(message.id) {
        mutableStateOf<dev.ipf.whitenoise.model.NostrProfileOccurrence?>(null)
    }
    Column(modifier = modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max)) {
        if (message.isForwarded && !message.isDeleted && message.replyToMessageId == null &&
            message.attachments.isEmpty() && message.nostrEvents.isEmpty()) {
            ForwardedMessageLabel(
                message.id, LocalMessageReading.current.selectingTextId == message.id, containerColor,
                Modifier.padding(bottom = ConversationMessageMetrics.ForwardedLabelGap),
            )
        }
        if (showTranscriptLabel) {
            Text(
                stringResource(R.string.transcribed),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        val reading = LocalMessageReading.current
        val selectingText = reading.selectingTextId == message.id
        val translationState = LocalMessageTranslation.current?.state(message)
        val hasStatusBelow = !message.isDeleted && (
            message.editAttempt != null || message.editHistory != null ||
                (searchQuery.isBlank() && !selectingText && translationState?.phase == dev.ipf.whitenoise.model.TranslationPhase.Ready &&
                    !translationState.showOriginal && translationState.source != translationState.target) ||
                readAloudController.activeMessageId == message.id
            )
        val bodyFooter = footer.takeUnless { hasStatusBelow }
        val body: @Composable () -> Unit = {
            val location = remember(message, text) { dev.ipf.whitenoise.model.LocationSharing.fromMessage(message.copy(text = text)) }
            if (LocalFocusedMessagePreview.current && !message.isDeleted) {
                MessageTextWithFooter(bodyFooter, Modifier.fillMaxWidth()) { onLayout -> FocusedMessageText(text, message.id, onLayout) }
            } else if (location != null && searchQuery.isBlank() && !selectingText) {
                LocationMessageCard(location)
                bodyFooter?.let { MessageFooterRow(it, Modifier.padding(top = 4.dp)) }
            } else if (searchQuery.isNotBlank() && message.deletionState == MessageDeletionState.None) {
                MessageTextWithFooter(bodyFooter, Modifier.fillMaxWidth()) { onLayout ->
                    SearchHighlightedText(
                        text = plainText, query = searchQuery,
                        style = MaterialTheme.typography.bodyLarge, onTextLayout = onLayout,
                    )
                }
            } else if (message.deletionState == MessageDeletionState.None) {
                val document = remember(text) { dev.ipf.whitenoise.model.MessageDocuments.parse(text) }
                val limit = with(LocalDensity.current) { (MaterialTheme.typography.bodyLarge.lineHeight * 52).toDp() }
                val limitPx = with(LocalDensity.current) { limit.roundToPx() }
                var overflow by remember(text, limitPx) { mutableStateOf(false) }
                var measuredWidth by remember(text, limitPx) { mutableIntStateOf(-1) }
                Box(if (reading.collapse && !selectingText) Modifier.heightIn(max = limit).clipToBounds() else Modifier) {
                    MessageDocumentContent(document, profile.people, onOpenPersonProfile,
                        Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true).onSizeChanged {
                            // Removing the clipped footer must not toggle Read more back off.
                            overflow = it.height > limitPx || (overflow && measuredWidth == it.width)
                            measuredWidth = it.width
                        },
                        spokenRange = readAloudController.session?.takeIf { it.owner == LocalSpeechOwner.current && it.current.item.id == message.id && it.current.item.authored == text }
                            ?.passage?.let { it.sourceStart until it.sourceEnd },
                        followSpeech = !selectingText && readAloudController.session?.following == true,
                        annotateSource = selectingText,
                        footer = bodyFooter.takeUnless { overflow && reading.collapse && !selectingText },
                        memberIds = memberIds,
                        onOpenProfileReference = { occurrence ->
                            val person = profile.people.firstOrNull { it.publicKey == occurrence.publicKey }
                            if (person != null) onOpenPersonProfile(person.id) else unavailableProfile = occurrence
                        })
                }
                if (overflow && reading.collapse && !selectingText) TextButton(onClick = { reading.open(message.id) }, modifier = Modifier.testTag("message.readMore.${message.id}"),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.LocalContentColor.current)) {
                    Text(stringResource(R.string.message_read_more))
                }
                if (overflow && reading.collapse && !selectingText) bodyFooter?.let { MessageFooterRow(it) }
            } else {
                MessageTextWithFooter(bodyFooter, Modifier.fillMaxWidth()) { onLayout ->
                    Text(text = text, fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.bodyLarge, onTextLayout = onLayout)
                }
            }
        }
        if (selectingText) {
            InlineMessageSelection(message.id, reading.dismissTextSelection, body)
        } else body()
        TranslationBubbleStatus(message, !message.isDeleted && searchQuery.isBlank() && !selectingText, containerColor)
        if (!message.isDeleted) MessageEditStatus(message)
        ReadAloudProgress(message.id, readAloudController)
        if (hasStatusBelow) footer?.let { MessageFooterRow(it, Modifier.padding(top = 4.dp)) }
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
    val content = if (outgoing && !isAmoledOutline()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val secondary = if (outgoing && !isAmoledOutline()) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val quoteShape = ConversationRichContentShape
    ConversationQuoteBlock(
        author = author ?: stringResource(R.string.original_message_unavailable),
        excerpt = body,
        containerColor = if (outgoing && !isAmoledOutline()) {
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
    messageId: String,
    alignEnd: Boolean,
    summary: List<ReactionCatalog.SummaryItem>,
    fillWidth: Boolean,
    measurementOnly: Boolean = false,
    onReaction: (String) -> Unit,
    onShowActions: () -> Unit,
    onLongPress: (() -> Unit)?,
) {
    val currentLongPress = rememberUpdatedState(onLongPress)
    val metadataLongPress = remember(messageId) { { currentLongPress.value?.invoke(); Unit } }
    Row(
        modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .padding(horizontal = 12.dp)
            .interceptBubbleLongPress(!measurementOnly && onLongPress != null, metadataLongPress)
            .then(if (measurementOnly) Modifier.clearAndSetSemantics { }
                else Modifier.testTag("conversation.message.metadata.$messageId")),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        ReactionRow(
            messageId = messageId, summary = summary, interactive = !measurementOnly,
            onReaction = onReaction, onShowActions = onShowActions,
            onLongPress = metadataLongPress.takeIf { onLongPress != null },
        )
    }
}

@Composable
private fun MessageBubbleTime(message: ChatMessage, outgoing: Boolean, containerColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        MessageExpiryIndicator(message, color = dev.ipf.whitenoise.ui.theme.forwardedLabelColor(
            containerColor, androidx.compose.material3.LocalContentColor.current))
        MessageTime(message.id, message.timeLabel, outgoing, message.deliveryState,
            testTagEnabled = true, verticalOffsetPx = 0, containerColor = containerColor)
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
    containerColor: Color = MaterialTheme.colorScheme.surface,
) {
    val failed = outgoing && deliveryState == MessageDeliveryState.Failed
    val failedLabel = if (failed) stringResource(R.string.not_delivered_retry) else null
    val timestampColor = dev.ipf.whitenoise.ui.theme.forwardedLabelColor(containerColor, androidx.compose.material3.LocalContentColor.current)
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
                        tint = containerColor,
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

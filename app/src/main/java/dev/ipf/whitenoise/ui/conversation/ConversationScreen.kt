package dev.ipf.whitenoise.ui.conversation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatKind
import dev.ipf.whitenoise.model.ChatMessage
import dev.ipf.whitenoise.model.ChatTimelineEntry
import dev.ipf.whitenoise.model.ComposerAvailability
import dev.ipf.whitenoise.model.ConversationItem
import dev.ipf.whitenoise.model.ConversationProjection
import dev.ipf.whitenoise.model.ConversationSearch
import dev.ipf.whitenoise.model.DisappearingDuration
import dev.ipf.whitenoise.model.MessageAttachment
import dev.ipf.whitenoise.model.MessageAction
import dev.ipf.whitenoise.model.MessageActionPolicy
import dev.ipf.whitenoise.model.MessageDeletionScope
import dev.ipf.whitenoise.model.MessageDeliveryState
import dev.ipf.whitenoise.model.MessageDeletionState
import dev.ipf.whitenoise.model.Person
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.ReactionCatalog
import dev.ipf.whitenoise.model.VoiceMessageFormat
import dev.ipf.whitenoise.model.composerAvailability
import dev.ipf.whitenoise.model.visibleText
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.drawableResource

@OptIn(ExperimentalMaterial3Api::class)
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
    onSendVoice: (VoiceMessageFormat, String) -> Boolean = { _, _ -> false },
    onReply: (String) -> Boolean = { false },
    onReaction: (String, String, Boolean) -> Boolean = { _, _, _ -> false },
    onQuickReactionsChanged: (List<String>) -> Boolean = { false },
    onDeleteMessages: (Set<String>, MessageDeletionScope) -> Boolean = { _, _ -> false },
    onForwardMessages: (Set<String>, List<String>) -> Boolean = { _, _ -> false },
    onOpenMessageDetails: (String) -> Unit = {},
    onOpenChatInfo: () -> Unit = {},
    onOpenDeveloperTools: (() -> Unit)? = null,
    initialSearch: Boolean = false,
) {
    val items = remember(chat.timeline) { ConversationProjection.items(chat) }
    val listState = rememberLazyListState()
    var showDeclineConfirmation by remember { mutableStateOf(false) }
    var showRelayInformation by remember { mutableStateOf(false) }
    var viewerAttachments by remember { mutableStateOf<List<MessageAttachment>?>(null) }
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
    val context = LocalContext.current
    val messages = remember(chat.timeline) {
        chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().map(ChatTimelineEntry.Message::message)
    }
    val selectedMessages = messages.filter { it.id in selectedMessageIds }
    val searchResults = remember(chat.timeline, profile.people, searchQuery) {
        ConversationSearch.results(chat, profile, searchQuery)
    }
    val currentSearchMessageId = searchResults.getOrNull(searchResultIndex)?.messageId

    fun handleAction(message: ChatMessage, action: MessageAction) {
        focusedMessageId = null
        when (action) {
            MessageAction.RetrySend -> onRetry(message.id)
            MessageAction.Reply -> onReply(message.id)
            MessageAction.Forward -> forwardMessageIds = setOf(message.id)
            MessageAction.Copy -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Message", message.visibleText(profile.id)))
            }
            MessageAction.Select -> {
                isSelecting = true
                selectedMessageIds = setOf(message.id)
            }
            MessageAction.Info -> onOpenMessageDetails(message.id)
            MessageAction.Delete -> deleteMessageIds = setOf(message.id)
        }
    }

    LaunchedEffect(chat.id, items.size) {
        if (items.isNotEmpty()) listState.scrollToItem(items.lastIndex)
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
                    onClose = {
                        isSearching = false
                        searchQuery = ""
                        searchResultIndex = 0
                    },
                )
                else -> ConversationTopBar(
                    chat = chat,
                    onBack = onBack,
                    onSearch = { isSearching = true },
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
                else -> ConversationBottomBar(
                    profile = profile,
                    chat = chat,
                    onDraftTextChanged = onDraftTextChanged,
                    onAddDraftAttachments = onAddDraftAttachments,
                    onRemoveDraftAttachment = onRemoveDraftAttachment,
                    onSuppressDraftLink = onSuppressDraftLink,
                    onCancelDraftReply = onCancelDraftReply,
                    onSendDraft = onSendDraft,
                    onSendVoice = onSendVoice,
                    onAccept = onAcceptInvitation,
                    onDecline = { showDeclineConfirmation = true },
                    onCheckRelays = { showRelayInformation = true },
                )
            }
        },
    ) { contentPadding ->
        AdaptiveContent(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
            ) {
                items.forEach { item ->
                    when (item) {
                        is ConversationItem.DayHeader -> stickyHeader(key = item.id) {
                            DayHeader(item.label)
                        }
                        is ConversationItem.EventItem -> item(key = item.id, contentType = "event") {
                            TimelineInformation(item.entry.text)
                        }
                        is ConversationItem.NoticeItem -> item(key = item.id, contentType = "notice") {
                            TimelineInformation(item.entry.text, isNotice = true)
                        }
                        is ConversationItem.MessageItem -> item(key = item.id, contentType = "message") {
                            MessageRow(
                                profile = profile,
                                chat = chat,
                                item = item,
                                onRetry = { onRetry(item.message.id) },
                                onOpenMedia = { viewerAttachments = it },
                                isSelectionMode = isSelecting,
                                selected = item.message.id in selectedMessageIds,
                                searchAlpha = if (
                                    !isSearching || searchQuery.isBlank() ||
                                    item.message.id == currentSearchMessageId
                                ) 1f else 0.38f,
                                searchPosition = searchResults.indexOfFirst {
                                    it.messageId == item.message.id
                                }.takeIf { it >= 0 }?.let { "${it + 1} of ${searchResults.size} matches" },
                                onToggleSelection = {
                                    selectedMessageIds = if (item.message.id in selectedMessageIds) {
                                        selectedMessageIds - item.message.id
                                    } else {
                                        selectedMessageIds + item.message.id
                                    }
                                },
                                onShowActions = { focusedMessageId = item.message.id },
                                onAccessibilityAction = { action -> handleAction(item.message, action) },
                                onReaction = { emoji -> onReaction(item.message.id, emoji, false) },
                            )
                        }
                    }
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

    if (showRelayInformation) {
        AlertDialog(
            onDismissRequest = { showRelayInformation = false },
            title = { Text(stringResource(R.string.chat_relays_required_title)) },
            text = { Text(stringResource(R.string.chat_relays_missing_detail)) },
            confirmButton = {
                TextButton(onClick = { showRelayInformation = false }) {
                    Text(stringResource(R.string.done))
                }
            },
        )
    }

    viewerAttachments?.let { attachments ->
        ReadOnlyMediaViewer(
            attachments = attachments,
            onDismiss = { viewerAttachments = null },
        )
    }

    messages.firstOrNull { it.id == focusedMessageId }?.let { message ->
        MessageActionsSheet(
            profile = profile,
            message = message,
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
            onForward = { targets ->
                if (onForwardMessages(ids, targets)) {
                    forwardMessageIds = null
                    isSelecting = false
                    selectedMessageIds = emptySet()
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationTopBar(
    chat: Chat,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onInfo: () -> Unit,
    onDeveloperTools: (() -> Unit)?,
) {
    val memberCount = chat.members.size
    val memberLabel = pluralStringResource(R.plurals.group_member_count, memberCount, memberCount)
    val hasTimer = chat.disappearingDuration != DisappearingDuration.Off
    val searchDescription = stringResource(R.string.search_messages)
    val fullDescription = buildString {
        append(chat.title)
        if (chat.isGroup) append(", $memberLabel")
        if (chat.disappearingDuration != DisappearingDuration.Off) {
            append(", ${stringResource(R.string.disappearing_header, chat.disappearingDuration.label)}")
        }
    }
    CenterAlignedTopAppBar(
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
                    .clickable(onClick = onInfo)
                    .semantics(mergeDescendants = true) { contentDescription = fullDescription },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProfileAvatar(chat.title, chat.avatar, Modifier.size(40.dp), contentDescription = null)
                Column(horizontalAlignment = Alignment.Start) {
                    Text(chat.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (chat.isGroup || hasTimer) {
                        Row(
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
                    modifier = Modifier.semantics { contentDescription = "Conversation debug" },
                ) {
                    Text(
                        "⚙",
                        modifier = Modifier.clearAndSetSemantics { },
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            IconButton(
                onClick = onSearch,
                modifier = Modifier.semantics { contentDescription = searchDescription },
            ) {
                Text(
                    "⌕",
                    modifier = Modifier.clearAndSetSemantics { },
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(count: Int, onClose: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(pluralStringResource(R.plurals.selected_count, count, count)) },
        actions = {
            TextButton(onClick = onClose) { Text(stringResource(R.string.close_selection)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationSearchTopBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClose: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.messages)) },
                singleLine = true,
            )
        },
        actions = {
            TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun DayHeader(label: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 2.dp,
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun TimelineInformation(text: String, isNotice: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = if (isNotice) 24.dp else 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            modifier = Modifier
                .widthIn(max = 360.dp)
                .semantics { contentDescription = text },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = if (isNotice) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun MessageRow(
    profile: Profile,
    chat: Chat,
    item: ConversationItem.MessageItem,
    onRetry: () -> Unit,
    onOpenMedia: (List<MessageAttachment>) -> Unit,
    isSelectionMode: Boolean,
    selected: Boolean,
    searchAlpha: Float,
    searchPosition: String?,
    onToggleSelection: () -> Unit,
    onShowActions: () -> Unit,
    onAccessibilityAction: (MessageAction) -> Unit,
    onReaction: (String) -> Unit,
) {
    val message = item.message
    val outgoing = message.authorId == profile.id
    val author = profile.people.firstOrNull { it.id == message.authorId }
    val authorName = if (outgoing) stringResource(R.string.you) else author?.name ?: chat.title
    val verticalPadding = when {
        item.startsCluster -> 8.dp
        else -> 0.dp
    }
    val haptics = LocalHapticFeedback.current
    val showActionsLabel = stringResource(R.string.show_message_actions)
    val selectedState = stringResource(
        if (selected) R.string.selection_state_selected else R.string.selection_state_not_selected,
    )
    val accessibilityActions = MessageActionPolicy.available(message, profile.id).map { action ->
        CustomAccessibilityAction(actionLabel(action)) {
            onAccessibilityAction(action)
            true
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(searchAlpha)
            .padding(top = verticalPadding)
            .combinedClickable(
                enabled = isSelectionMode || !message.isDeleted,
                onClick = { if (isSelectionMode) onToggleSelection() },
                onLongClickLabel = showActionsLabel,
                onLongClick = {
                    if (!isSelectionMode && !message.isDeleted) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onShowActions()
                    }
                },
            )
            .semantics {
                if (isSelectionMode) stateDescription = selectedState
                searchPosition?.let { contentDescription = it }
                customActions = accessibilityActions
            },
        horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (isSelectionMode) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) { Text(if (selected) "✓" else "○") }
            }
            Spacer(Modifier.width(4.dp))
        }
        if (chat.isGroup && !outgoing) {
            if (item.endsCluster) {
                GroupAuthorAvatar(authorName, author, Modifier.size(30.dp))
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
                Text(
                    authorName,
                    modifier = Modifier.padding(start = 12.dp, bottom = 3.dp),
                    color = groupAuthorColor(author?.publicKey ?: message.authorId),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            MessageBubble(
                profile = profile,
                message = message,
                outgoing = outgoing,
                item = item,
                authorName = authorName,
                onOpenMedia = onOpenMedia,
            )
            if (message.reactions.isNotEmpty()) {
                ReactionRow(message = message, profileId = profile.id, onReaction = onReaction)
            }
            if (item.endsCluster) {
                if (message.deliveryState == MessageDeliveryState.Failed && outgoing) {
                    TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text(
                            stringResource(R.string.not_delivered_retry),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                } else {
                    Text(
                        message.timeLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
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
    onOpenMedia: (List<MessageAttachment>) -> Unit,
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
        shape = RoundedCornerShape(18.dp),
        color = if (outgoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (outgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (message.replyToMessageId != null) {
                ReplyQuote(profile, item, outgoing)
            }
            TimelineAttachmentContent(
                attachments = message.attachments,
                outgoing = outgoing,
                onOpenMedia = onOpenMedia,
            )
            if (message.attachments.any { it.voiceFormat == VoiceMessageFormat.Both } && text.isNotBlank()) {
                Text(
                    stringResource(R.string.transcribed),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    fontStyle = if (message.deletionState == MessageDeletionState.None) FontStyle.Normal else FontStyle.Italic,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (!outgoing && message.deletionState == MessageDeletionState.None) {
                    ReadAloudAction(text)
                }
            }
        }
    }
}

@Composable
private fun ReplyQuote(profile: Profile, item: ConversationItem.MessageItem, outgoing: Boolean) {
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
    val overlay = if (outgoing) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface
    Surface(
        color = overlay,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(
                author?.let { stringResource(R.string.reply_from, it) }
                    ?: stringResource(R.string.original_message_unavailable),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(body, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReactionRow(message: ChatMessage, profileId: String, onReaction: (String) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        message.reactions.forEach { reaction ->
            val selected = profileId in reaction.personIds
            Surface(
                modifier = Modifier.combinedClickable(
                    onClick = { onReaction(reaction.emoji) },
                    onLongClick = {},
                ),
                shape = RoundedCornerShape(50),
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = "${reaction.emoji} ${reaction.personIds.size}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
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
    onDraftTextChanged: (String) -> Unit,
    onAddDraftAttachments: (List<MessageAttachment>) -> Unit,
    onRemoveDraftAttachment: (String) -> Unit,
    onSuppressDraftLink: (String?) -> Unit,
    onCancelDraftReply: () -> Unit,
    onSendDraft: () -> Boolean,
    onSendVoice: (VoiceMessageFormat, String) -> Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCheckRelays: () -> Unit,
) {
    val availability = chat.composerAvailability(profile)
    Surface(tonalElevation = 3.dp) {
        when (availability) {
            ComposerAvailability.PendingInvitation -> InvitationActions(chat, onDecline, onAccept)
            ComposerAvailability.Available -> FullConversationComposer(
                profile = profile,
                chat = chat,
                onDraftTextChanged = onDraftTextChanged,
                onAddAttachments = onAddDraftAttachments,
                onRemoveAttachment = onRemoveDraftAttachment,
                onSuppressLink = onSuppressDraftLink,
                onCancelReply = onCancelDraftReply,
                onSendDraft = onSendDraft,
                onSendVoice = onSendVoice,
            )
            ComposerAvailability.Left -> ConversationStatus(
                if (chat.isGroup) stringResource(R.string.left_group_status) else stringResource(R.string.left_chat_status),
            )
            ComposerAvailability.Removed -> ConversationStatus(stringResource(R.string.removed_group_status))
            ComposerAvailability.Blocked -> ConversationStatus(
                stringResource(R.string.blocked_chat_detail),
                title = stringResource(R.string.messaging_unavailable),
            )
            ComposerAvailability.MissingRelays -> Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.chat_relays_missing_detail), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onCheckRelays) { Text(stringResource(R.string.check_chat_relays)) }
            }
        }
    }
}

@Composable
private fun InvitationActions(chat: Chat, onDecline: () -> Unit, onAccept: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(
                R.string.invited_to_chat_by,
                chat.invitationInviterName ?: stringResource(R.string.someone),
            ),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.decline), color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.accept))
            }
        }
    }
}

@Composable
private fun ConversationStatus(text: String, title: String? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        title?.let { Text(it, fontWeight = FontWeight.SemiBold) }
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

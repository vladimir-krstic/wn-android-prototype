package dev.ipf.whitenoise.ui.conversation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn as LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
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
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseOutlinedButton
import dev.ipf.whitenoise.ui.components.drawableResource
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

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
                    onCheckRelays = onOpenChatInfo,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { contentPadding ->
        AdaptiveContent(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                    vertical = WhiteNoiseSpacing.Related,
                ),
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
                            val resultPosition = searchResults.indexOfFirst {
                                it.messageId == item.message.id
                            }
                            val searchPosition = resultPosition.takeIf { it >= 0 }?.let {
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
                                onRetry = { onRetry(item.message.id) },
                                onOpenMedia = { viewerAttachments = it },
                                isSelectionMode = isSelecting,
                                selected = item.message.id in selectedMessageIds,
                                searchAlpha = if (
                                    !isSearching || searchQuery.isBlank() ||
                                    item.message.id == currentSearchMessageId
                                ) 1f else 0.38f,
                                searchQuery = searchQuery.takeIf { isSearching }.orEmpty(),
                                isCurrentSearchResult = isSearching &&
                                    item.message.id == currentSearchMessageId,
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
                    .clickable(role = Role.Button, onClick = onInfo)
                    .semantics(mergeDescendants = true) { contentDescription = fullDescription },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                ProfileAvatar(chat.title, chat.avatar, Modifier.size(36.dp), contentDescription = null)
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        chat.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
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
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bug_report),
                        contentDescription = stringResource(R.string.conversation_debug),
                    )
                }
            }
            IconButton(onClick = onSearch) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = searchDescription,
                )
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
    val searchDescription = stringResource(R.string.search_messages)
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close),
                )
            }
        },
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = searchDescription },
                placeholder = { Text(stringResource(R.string.messages)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                    )
                },
                trailingIcon = if (query.isNotEmpty()) ({
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.clear_search),
                        )
                    }
                }) else null,
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        },
        scrollBehavior = dev.ipf.whitenoise.ui.components.LocalWhiteNoiseHeaderScroll.current,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun DayHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
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
    onRetry: () -> Unit,
    onOpenMedia: (List<MessageAttachment>) -> Unit,
    isSelectionMode: Boolean,
    selected: Boolean,
    searchAlpha: Float,
    searchQuery: String,
    isCurrentSearchResult: Boolean,
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
        item.startsCluster -> WhiteNoiseSpacing.FormField
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
    val interactionModifier = if (isSelectionMode) {
        Modifier.toggleable(
            value = selected,
            role = Role.Checkbox,
            onValueChange = { onToggleSelection() },
        )
    } else {
        Modifier.combinedClickable(
            enabled = !message.isDeleted,
            onClick = {},
            onLongClickLabel = showActionsLabel,
            onLongClick = {
                if (!message.isDeleted) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onShowActions()
                }
            },
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(searchAlpha)
            .background(
                color = if (isSelectionMode && selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else {
                    Color.Transparent
                },
                shape = MaterialTheme.shapes.large,
            )
            .padding(top = verticalPadding)
            .then(interactionModifier)
            .semantics {
                if (isSelectionMode) stateDescription = selectedState
                searchPosition?.let { stateDescription = it }
                customActions = accessibilityActions
            },
        horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (isSelectionMode) {
            Checkbox(checked = selected, onCheckedChange = null)
            Spacer(Modifier.width(WhiteNoiseSpacing.Related))
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
                searchQuery = searchQuery,
                isCurrentSearchResult = isCurrentSearchResult,
            )
            if (message.reactions.isNotEmpty()) {
                ReactionRow(
                    message = message,
                    profileId = profile.id,
                    onReaction = onReaction,
                    onShowActions = onShowActions,
                )
            }
            if (item.endsCluster) {
                if (message.deliveryState == MessageDeliveryState.Failed && outgoing) {
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_warning),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.width(4.dp))
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
    searchQuery: String,
    isCurrentSearchResult: Boolean,
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
        border = if (isCurrentSearchResult) {
            BorderStroke(
                2.dp,
                if (outgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            )
        } else {
            null
        },
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
                if (searchQuery.isNotBlank() && message.deletionState == MessageDeletionState.None) {
                    HighlightedSearchText(
                        text = text,
                        query = searchQuery,
                        outgoing = outgoing,
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
                if (!outgoing && message.deletionState == MessageDeletionState.None) {
                    ReadAloudAction(text)
                }
            }
        }
    }
}

@Composable
private fun HighlightedSearchText(text: String, query: String, outgoing: Boolean) {
    val highlightContainer = if (outgoing) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val highlightContent = if (outgoing) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onPrimary
    }
    val annotated = remember(text, query, highlightContainer, highlightContent) {
        buildAnnotatedString {
            var cursor = 0
            while (cursor < text.length) {
                val match = text.indexOf(query, startIndex = cursor, ignoreCase = true)
                if (match < 0) {
                    append(text.substring(cursor))
                    break
                }
                append(text.substring(cursor, match))
                withStyle(
                    SpanStyle(
                        color = highlightContent,
                        background = highlightContainer,
                        fontWeight = FontWeight.SemiBold,
                    ),
                ) {
                    append(text.substring(match, match + query.length))
                }
                cursor = match + query.length
            }
        }
    }
    Text(text = annotated, style = MaterialTheme.typography.bodyLarge)
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
        shape = MaterialTheme.shapes.medium,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReactionRow(
    message: ChatMessage,
    profileId: String,
    onReaction: (String) -> Unit,
    onShowActions: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val showActionsLabel = stringResource(R.string.show_message_actions)
    FlowRow(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        message.reactions.forEach { reaction ->
            val selected = profileId in reaction.personIds
            val description = pluralStringResource(
                R.plurals.reaction_count,
                reaction.personIds.size,
                reaction.emoji,
                reaction.personIds.size,
            )
            val selectedState = stringResource(
                if (selected) R.string.selection_state_selected else R.string.selection_state_not_selected,
            )
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .combinedClickable(
                    onClick = { onReaction(reaction.emoji) },
                        onLongClickLabel = showActionsLabel,
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onShowActions()
                        },
                    )
                    .semantics {
                        contentDescription = description
                        stateDescription = selectedState
                    },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Text(
                        text = "${reaction.emoji} ${reaction.personIds.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
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
    when (availability) {
        ComposerAvailability.PendingInvitation -> InvitationActions(chat, onDecline, onAccept)
        ComposerAvailability.Available -> Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            AdaptiveContent {
                FullConversationComposer(
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
            }
        }
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

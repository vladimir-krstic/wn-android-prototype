package dev.ipf.whitenoise.ui.chats

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatDeliveryState
import dev.ipf.whitenoise.model.ChatProjection
import dev.ipf.whitenoise.model.ChatScope
import dev.ipf.whitenoise.model.MuteDuration
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseEmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    uiState: AppUiState,
    onNewMessage: () -> Unit,
    onOpenChat: (String) -> Unit,
    onMarkUnread: (String, Boolean) -> Unit,
    onTogglePin: (String) -> Unit,
    onMute: (String, MuteDuration?) -> Unit,
    onArchive: (String, Boolean) -> Unit,
    onLeave: (String) -> Boolean,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSettings: () -> Unit = {},
    onProfileRelays: () -> Unit = {},
) {
    val profile = uiState.activeProfile
    val searchChatsDescription = stringResource(R.string.search_chats)
    val closeSearchDescription = stringResource(R.string.close_search)
    var scopeName by rememberSaveable { mutableStateOf(ChatScope.Chats.name) }
    val scope = ChatScope.valueOf(scopeName)
    var query by rememberSaveable { mutableStateOf("") }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var filterMenuOpen by remember { mutableStateOf(false) }
    var actionChat by remember { mutableStateOf<Chat?>(null) }
    var muteChat by remember { mutableStateOf<Chat?>(null) }
    var leaveChat by remember { mutableStateOf<Chat?>(null) }
    var deleteChat by remember { mutableStateOf<Chat?>(null) }
    var soleAdminChat by remember { mutableStateOf<Chat?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val rows = remember(profile?.chats, scope, query) {
        ChatProjection.rows(profile?.chats.orEmpty(), scope, query)
    }

    fun closeSearch() {
        focusManager.clearFocus()
        keyboardController?.hide()
        isSearching = false
        query = ""
    }

    BackHandler(enabled = isSearching, onBack = ::closeSearch)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Crossfade(
                targetState = isSearching,
                label = "Chats search mode",
            ) { searching ->
                if (searching) {
                    ChatsSearchTopBar(
                        query = query,
                        onQueryChange = { query = it },
                        onClose = ::closeSearch,
                        closeSearchDescription = closeSearchDescription,
                    )
                } else {
                    ChatsTopBar(
                        profile = profile,
                        scope = scope,
                        filterMenuOpen = filterMenuOpen,
                        onFilterMenuOpenChange = { filterMenuOpen = it },
                        onScopeChange = {
                            scopeName = it.name
                            filterMenuOpen = false
                        },
                        onSettings = onSettings,
                        onSearch = { isSearching = true },
                        searchChatsDescription = searchChatsDescription,
                    )
                }
            }
        },
        floatingActionButton = {
            if (scope == ChatScope.Chats) {
                ExtendedFloatingActionButton(
                    onClick = if (profile?.chatRelayUrls.isNullOrEmpty()) onProfileRelays else onNewMessage,
                    text = {
                        Text(
                            if (profile?.chatRelayUrls.isNullOrEmpty()) "Check Relays"
                            else stringResource(R.string.new_message),
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(
                                if (profile?.chatRelayUrls.isNullOrEmpty()) {
                                    R.drawable.ic_warning
                                } else {
                                    R.drawable.ic_add
                                },
                            ),
                            contentDescription = null,
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { contentPadding ->
        AdaptiveContent(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
        ) {
            if (rows.isEmpty()) {
                ChatEmptyState(
                    scope = scope,
                    isSearchEmpty = query.isNotBlank(),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(rows, key = Chat::id) { chat ->
                        ChatRow(
                            chat = chat,
                            onOpen = {
                                closeSearch()
                                onOpenChat(chat.id)
                            },
                            onActions = { actionChat = chat },
                        )
                    }
                }
            }
        }
    }

    actionChat?.let { chat ->
        ChatActionsSheet(
            chat = chat,
            onDismiss = { actionChat = null },
            onMarkUnread = {
                onMarkUnread(chat.id, !chat.isUnread)
                actionChat = null
            },
            onTogglePin = {
                onTogglePin(chat.id)
                actionChat = null
            },
            onMute = {
                actionChat = null
                if (chat.muteDuration == null) muteChat = chat else onMute(chat.id, null)
            },
            onArchive = {
                onArchive(chat.id, !chat.isArchived)
                actionChat = null
            },
            onLeave = {
                actionChat = null
                if (chat.isSoleAdmin(profile?.id.orEmpty())) soleAdminChat = chat else leaveChat = chat
            },
            onDelete = {
                actionChat = null
                deleteChat = chat
            },
        )
    }

    muteChat?.let { chat ->
        MuteDurationSheet(
            onDismiss = { muteChat = null },
            onSelect = {
                onMute(chat.id, it)
                muteChat = null
            },
        )
    }

    leaveChat?.let { chat ->
        AlertDialog(
            onDismissRequest = { leaveChat = null },
            title = { Text(stringResource(R.string.leave_group_title)) },
            text = { Text(stringResource(R.string.leave_group_detail)) },
            confirmButton = {
                TextButton(onClick = {
                    onLeave(chat.id)
                    leaveChat = null
                }) { Text(stringResource(R.string.leave_group)) }
            },
            dismissButton = {
                TextButton(onClick = { leaveChat = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    soleAdminChat?.let {
        AlertDialog(
            onDismissRequest = { soleAdminChat = null },
            title = { Text(stringResource(R.string.sole_admin_title)) },
            text = { Text(stringResource(R.string.sole_admin_detail)) },
            confirmButton = {
                TextButton(onClick = { soleAdminChat = null }) { Text(stringResource(R.string.done)) }
            },
        )
    }

    deleteChat?.let { chat ->
        AlertDialog(
            onDismissRequest = { deleteChat = null },
            title = { Text(stringResource(R.string.delete_chat_title)) },
            text = { Text(stringResource(R.string.delete_chat_detail)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(chat.id)
                    deleteChat = null
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteChat = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatsTopBar(
    profile: Profile?,
    scope: ChatScope,
    filterMenuOpen: Boolean,
    onFilterMenuOpenChange: (Boolean) -> Unit,
    onScopeChange: (ChatScope) -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    searchChatsDescription: String,
) {
    val filterDescription = stringResource(R.string.filter_chats)
    val scopeTitle = if (scope == ChatScope.Chats) stringResource(R.string.chats) else scope.label
    val selectedScopeDescription = stringResource(R.string.selected_chat_scope, scopeTitle)
    val settingsDescription = profile?.let {
        stringResource(R.string.open_settings_for, it.name)
    }
    TopAppBar(
        title = {
            Text(
                text = scopeTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        navigationIcon = {
            profile?.let {
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.semantics {
                        settingsDescription?.let { description ->
                            contentDescription = description
                        }
                    },
                ) {
                    ProfileAvatar(
                        name = it.name,
                        avatar = it.avatar,
                        modifier = Modifier.size(40.dp),
                        contentDescription = null,
                    )
                }
            }
        },
        actions = {
            Box {
                if (scope == ChatScope.Chats) {
                    IconButton(
                        onClick = { onFilterMenuOpenChange(true) },
                        modifier = Modifier.semantics {
                            contentDescription = filterDescription
                            stateDescription = selectedScopeDescription
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_filter_list),
                            contentDescription = null,
                        )
                    }
                } else {
                    FilledTonalIconButton(
                        onClick = { onFilterMenuOpenChange(true) },
                        modifier = Modifier.semantics {
                            contentDescription = filterDescription
                            stateDescription = selectedScopeDescription
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_filter_list),
                            contentDescription = null,
                        )
                    }
                }
                DropdownMenu(
                    expanded = filterMenuOpen,
                    onDismissRequest = { onFilterMenuOpenChange(false) },
                ) {
                    ChatScope.entries.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(candidate.label) },
                            onClick = { onScopeChange(candidate) },
                            trailingIcon = {
                                if (candidate == scope) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier.semantics {
                                selected = candidate == scope
                            },
                        )
                    }
                }
            }
            IconButton(onClick = onSearch) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = searchChatsDescription,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatsSearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    closeSearchDescription: String,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.search_chats)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                    )
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.clear_search),
                            )
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = closeSearchDescription,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun ChatRow(
    chat: Chat,
    onOpen: () -> Unit,
    onActions: () -> Unit,
) {
    val actionsDescription = stringResource(R.string.actions_for, chat.title)
    val unreadLabel = when {
        chat.unreadCount > 99 -> stringResource(R.string.unread_count_capped)
        chat.unreadCount > 0 -> pluralStringResource(
            R.plurals.unread_count,
            chat.unreadCount,
            chat.unreadCount,
        )
        else -> stringResource(R.string.manually_unread)
    }
    ListItem(
        headlineContent = {
            Text(
                text = chat.title,
                fontWeight = if (chat.isUnread) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = chat.displayPreview,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val indicators = buildList {
                    if (chat.isPinned) add(stringResource(R.string.pinned))
                    if (chat.muteDuration != null) add(stringResource(R.string.muted))
                    if (chat.disappearingDuration != dev.ipf.whitenoise.model.DisappearingDuration.Off) {
                        add(stringResource(R.string.disappearing_messages, chat.disappearingDuration.label))
                    }
                    if (chat.deliveryState == ChatDeliveryState.Failed) add(stringResource(R.string.failed_to_send))
                }
                if (indicators.isNotEmpty()) {
                    Text(
                        text = indicators.joinToString(" · "),
                        color = if (chat.deliveryState == ChatDeliveryState.Failed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        leadingContent = {
            ProfileAvatar(chat.title, chat.avatar, Modifier.size(52.dp), contentDescription = null)
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(chat.timestamp, style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.isUnread) {
                        Badge(modifier = Modifier.semantics { contentDescription = unreadLabel }) {
                            if (chat.unreadCount > 0) Text(if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString())
                        }
                    }
                    IconButton(
                        onClick = onActions,
                        modifier = Modifier.semantics { contentDescription = actionsDescription },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert),
                            contentDescription = null,
                        )
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .semantics { role = Role.Button },
    )
}

@Composable
private fun ChatEmptyState(
    scope: ChatScope,
    isSearchEmpty: Boolean,
    modifier: Modifier = Modifier,
) {
    val title: String
    val detail: String
    if (isSearchEmpty) {
        title = stringResource(R.string.no_results)
        detail = stringResource(R.string.no_results_detail)
    } else {
        title = stringResource(
            when (scope) {
                ChatScope.Chats -> R.string.no_chats
                ChatScope.Unread -> R.string.no_unread_chats
                ChatScope.Archived -> R.string.no_archived_chats
                ChatScope.Left -> R.string.no_left_chats
            },
        )
        detail = stringResource(
            when (scope) {
                ChatScope.Chats -> R.string.no_chats_detail
                ChatScope.Unread -> R.string.no_unread_chats_detail
                ChatScope.Archived -> R.string.no_archived_chats_detail
                ChatScope.Left -> R.string.no_left_chats_detail
            },
        )
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        WhiteNoiseEmptyState(title = title, detail = detail)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatActionsSheet(
    chat: Chat,
    onDismiss: () -> Unit,
    onMarkUnread: () -> Unit,
    onTogglePin: () -> Unit,
    onMute: () -> Unit,
    onArchive: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(chat.title, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.titleLarge)
            ActionRow(if (chat.isUnread) stringResource(R.string.mark_read) else stringResource(R.string.mark_unread), onMarkUnread)
            if (!chat.isArchived && !chat.hasEndedMembership) {
                ActionRow(if (chat.isPinned) stringResource(R.string.unpin) else stringResource(R.string.pin), onTogglePin)
            }
            ActionRow(if (chat.muteDuration == null) stringResource(R.string.mute) else stringResource(R.string.unmute), onMute)
            ActionRow(if (chat.isArchived) stringResource(R.string.unarchive) else stringResource(R.string.archive), onArchive)
            if (chat.isGroup && chat.membership == dev.ipf.whitenoise.model.ChatMembership.Active) {
                ActionRow(stringResource(R.string.leave_group), onLeave, destructive = true)
            }
            if (chat.hasEndedMembership) {
                ActionRow(stringResource(R.string.delete_chat), onDelete, destructive = true)
            }
        }
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit, destructive: Boolean = false) {
    ListItem(
        headlineContent = {
            Text(label, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuteDurationSheet(onDismiss: () -> Unit, onSelect: (MuteDuration) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(stringResource(R.string.mute_for), modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge)
            MuteDuration.entries.forEach { duration -> ActionRow(duration.label, { onSelect(duration) }) }
        }
    }
}

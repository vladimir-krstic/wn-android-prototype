package dev.ipf.whitenoise.ui.chats

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatListAction
import dev.ipf.whitenoise.model.ChatListActionPolicy
import dev.ipf.whitenoise.model.ChatListUndo
import dev.ipf.whitenoise.model.ChatProjection
import dev.ipf.whitenoise.model.ChatScope
import dev.ipf.whitenoise.model.MuteDuration
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.LocalWhiteNoiseHeaderScroll
import dev.ipf.whitenoise.ui.components.MuteDurationDialog
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import dev.ipf.whitenoise.ui.components.WhiteNoiseEmptyState
import dev.ipf.whitenoise.ui.components.WhiteNoiseContentMaxWidth
import dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn as LazyColumn
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold as Scaffold
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
    onUndo: (ChatListUndo) -> Unit = {},
) {
    val profile = uiState.activeProfile
    val searchChatsDescription = stringResource(R.string.search_chats)
    val closeSearchDescription = stringResource(R.string.close_search)
    var scopeName by rememberSaveable { mutableStateOf(ChatScope.Chats.name) }
    val scope = ChatScope.valueOf(scopeName)
    var query by rememberSaveable { mutableStateOf("") }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var filterMenuOpen by remember { mutableStateOf(false) }
    var muteChat by remember(profile?.id) { mutableStateOf<Chat?>(null) }
    var leaveChat by remember(profile?.id) { mutableStateOf<Chat?>(null) }
    var deleteChat by remember(profile?.id) { mutableStateOf<Chat?>(null) }
    var soleAdminChat by remember(profile?.id) { mutableStateOf<Chat?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val rows = remember(profile?.chats, scope, query) {
        ChatProjection.rows(profile?.chats.orEmpty(), scope, query)
    }

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val resources = LocalResources.current
    val layoutDirection = LocalLayoutDirection.current
    var undoJob by remember { mutableStateOf<Job?>(null) }
    var menuTarget by remember(profile?.id, scope, query, isSearching) { mutableStateOf<ChatMenuTarget?>(null) }
    LaunchedEffect(rows.map { it.id }) {
        if (rows.none { it.id == menuTarget?.chatId }) menuTarget = null
    }
    DisposableEffect(profile?.id) {
        onDispose { undoJob?.cancel(); snackbarHostState.currentSnackbarData?.dismiss() }
    }

    fun performAction(target: ChatMenuTarget, action: ChatListAction) {
        menuTarget = null
        if (target.profileId != profile?.id) return
        val chat = profile.chats.firstOrNull { it.id == target.chatId } ?: return
        if (action !in ChatListActionPolicy.all(chat)) return
        val undo = ChatListUndo.capture(profile.id, chat, action)
        when (action) {
            ChatListAction.Read -> onMarkUnread(chat.id, false)
            ChatListAction.Unread -> onMarkUnread(chat.id, true)
            ChatListAction.Pin, ChatListAction.Unpin -> onTogglePin(chat.id)
            ChatListAction.Mute -> muteChat = chat
            ChatListAction.Unmute -> onMute(chat.id, null)
            ChatListAction.Archive -> onArchive(chat.id, true)
            ChatListAction.Unarchive -> onArchive(chat.id, false)
            ChatListAction.Leave -> if (chat.isSoleAdmin(profile.id)) soleAdminChat = chat else leaveChat = chat
            ChatListAction.Delete -> deleteChat = chat
        }
        undoJob?.cancel()
        snackbarHostState.currentSnackbarData?.dismiss()
        if (action in setOf(ChatListAction.Read, ChatListAction.Unread, ChatListAction.Archive, ChatListAction.Unarchive)) {
            undoJob = coroutineScope.launch {
                val message = when (action) {
                    ChatListAction.Read -> R.string.chat_marked_read
                    ChatListAction.Unread -> R.string.chat_marked_unread
                    ChatListAction.Archive -> R.string.chat_archived
                    else -> R.string.chat_unarchived
                }
                if (snackbarHostState.showSnackbar(resources.getString(message), resources.getString(R.string.undo)) == SnackbarResult.ActionPerformed) {
                    menuTarget = null
                    onUndo(undo)
                }
            }
        }
    }

    fun closeSearch() {
        focusManager.clearFocus()
        keyboardController?.hide()
        isSearching = false
        query = ""
    }

    BackHandler(enabled = isSearching, onBack = ::closeSearch)
    BackHandler(enabled = menuTarget != null) { menuTarget = null }

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AdaptiveContent(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
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
                            onFilterMenuOpenChange = { menuTarget = null; filterMenuOpen = it },
                            onScopeChange = {
                                scopeName = it.name
                                filterMenuOpen = false
                            },
                            onSettings = { menuTarget = null; onSettings() },
                            onSearch = { isSearching = true },
                            searchChatsDescription = searchChatsDescription,
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isSearching) {
                // Scaffold owns safe-area/16 dp FAB placement and Snackbar clearance.
                // On wider windows only add the same centered-pane inset as the list.
                BoxWithConstraints {
                    val paneInset = ((maxWidth - WhiteNoiseContentMaxWidth) / 2).coerceAtLeast(0.dp)
                    val canCreate = !profile?.chatRelayUrls.isNullOrEmpty()
                    FloatingActionButton(
                        onClick = { menuTarget = null; if (canCreate) onNewMessage() else onProfileRelays() },
                        modifier = Modifier.padding(end = paneInset).testTag("chats.newMessage"),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(
                            painterResource(if (canCreate) R.drawable.ic_edit else R.drawable.ic_warning),
                            contentDescription = if (canCreate) stringResource(R.string.new_message) else "Check Relays",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        AdaptiveContent(
            modifier = Modifier.fillMaxSize()
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                )
                .consumeWindowInsets(contentPadding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("chats.list"),
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() +
                    if (isSearching) WhiteNoiseSpacing.CompactScreenMargin
                    else 56.dp + WhiteNoiseSpacing.CompactScreenMargin * 2),
            ) {
                if (rows.isEmpty()) {
                    item {
                        ChatEmptyState(scope, query.isNotBlank(), Modifier.fillParentMaxSize())
                    }
                }
                items(rows, key = Chat::id) { chat ->
                    val target = ChatMenuTarget(profile?.id.orEmpty(), chat.id)
                    ChatContextMenuRow(
                        chat = chat,
                        expanded = menuTarget == target,
                        onOpen = { menuTarget = null; closeSearch(); onOpenChat(chat.id) },
                        onShowMenu = { filterMenuOpen = false; menuTarget = target },
                        onDismissMenu = { if (menuTarget == target) menuTarget = null },
                        onAction = { performAction(target, it) },
                    )
                }
            }
        }
    }

    muteChat?.let { chat ->
        MuteDurationDialog(
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
                text = if (scope == ChatScope.Chats) "" else scopeTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            profile?.let {
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.padding(start = WhiteNoiseSpacing.Related).semantics {
                        settingsDescription?.let { description ->
                            contentDescription = description
                        }
                    },
                ) {
                    ProfileAvatar(
                        name = it.name,
                        avatar = it.avatar,
                        modifier = Modifier.size(40.dp).testTag("chats.profileAvatar"),
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
                            selected = false
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_filter_list),
                            contentDescription = null,
                        )
                    }
                } else {
                    FilledIconButton(
                        onClick = { onFilterMenuOpenChange(true) },
                        modifier = Modifier.semantics {
                            contentDescription = filterDescription
                            stateDescription = selectedScopeDescription
                            selected = true
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_filter_list),
                            contentDescription = null,
                        )
                    }
                }
                WhiteNoiseDropdownMenu(
                    expanded = filterMenuOpen,
                    onDismissRequest = { onFilterMenuOpenChange(false) },
                    modifier = Modifier.testTag("chats.filterMenu"),
                    items = ChatScope.entries.map { candidate ->
                        WhiteNoiseMenuItem(
                            label = candidate.label,
                            onClick = { onScopeChange(candidate) },
                            selected = candidate == scope,
                        )
                    },
                )
            }
            IconButton(onClick = onSearch) {
                Icon(painterResource(R.drawable.ic_search), contentDescription = searchChatsDescription)
            }
        },
        scrollBehavior = LocalWhiteNoiseHeaderScroll.current,
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
        scrollBehavior = LocalWhiteNoiseHeaderScroll.current,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
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

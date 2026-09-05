package dev.ipf.whitenoise.ui.chats

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import dev.ipf.whitenoise.model.GlobalSearch
import dev.ipf.whitenoise.model.GlobalSearchFilters
import dev.ipf.whitenoise.model.GlobalVoiceRequest
import dev.ipf.whitenoise.model.GlobalVoiceScenario
import dev.ipf.whitenoise.model.PeopleSearchScenario
import dev.ipf.whitenoise.model.PeopleSearchStatus
import dev.ipf.whitenoise.model.Person
import dev.ipf.whitenoise.model.ChatFolders
import dev.ipf.whitenoise.model.ChatOrganization
import dev.ipf.whitenoise.model.ChatBulkAction
import dev.ipf.whitenoise.model.ChatBatchAttempt
import dev.ipf.whitenoise.model.ChatBatchPhase
import dev.ipf.whitenoise.model.ChatConnectionPhase
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
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
import dev.ipf.whitenoise.state.AppUpdateController
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.LocalWhiteNoiseHeaderScroll
import dev.ipf.whitenoise.ui.components.MuteDurationDialog
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseCompactSearchField
import dev.ipf.whitenoise.ui.components.WhiteNoiseEmptyState
import dev.ipf.whitenoise.ui.components.WhiteNoiseContentMaxWidth
import dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn as LazyColumn
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold as Scaffold
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import dev.ipf.whitenoise.ui.updates.AppUpdateIconButton
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
    onOpenSearchMessage: (String, String) -> Boolean = { _, _ -> false },
    onOpenSearchPerson: (Person) -> Unit = {},
    peopleScenario: PeopleSearchScenario = PeopleSearchScenario.Success,
    onVoiceScenario: () -> GlobalVoiceScenario = { GlobalVoiceScenario.Device },
    onFolders: () -> Unit = {},
    onMovePin: (String, Int) -> Unit = { _, _ -> },
    onCreateFolder: (String) -> String? = { null },
    onBeginBatch: ((List<String>, ChatBulkAction, String?, Boolean) -> Boolean)? = null,
    batchAttempt: ChatBatchAttempt? = null,
    onAdvanceBatch: (Long, Int, ChatBatchPhase) -> Boolean = { _, _, _ -> false },
    onRetryBatch: () -> Unit = {},
    onDismissBatch: () -> Unit = {},
    onOpenGroup: (String) -> Unit = {},
    onRetryConnection: () -> Unit = {},
    onAdvanceConnection: (String, Long, ChatConnectionPhase) -> Boolean = { _, _, _ -> false },
    appUpdates: AppUpdateController? = null,
) {
    val profile = uiState.activeProfile
    val searchChatsDescription = stringResource(R.string.search_chats)
    val closeSearchDescription = stringResource(R.string.close_search)
    var scopeName by rememberSaveable(profile?.id) { mutableStateOf(ChatScope.Chats.name) }
    val scope = ChatScope.valueOf(scopeName)
    var query by rememberSaveable(profile?.id) { mutableStateOf("") }
    var isSearching by rememberSaveable(profile?.id) { mutableStateOf(false) }
    var filterMenuOpen by remember(profile?.id) { mutableStateOf(false) }
    var searchFilters by rememberSaveable(profile?.id, stateSaver = GlobalSearchFilterSaver) { mutableStateOf(GlobalSearchFilters()) }
    var searchFilterCategory by rememberSaveable(profile?.id) { mutableStateOf<String?>(null) }
    var lookupRetried by rememberSaveable(profile?.id, query) { mutableStateOf(false) }
    var lookupPending by remember(profile?.id, query) { mutableStateOf(false) }
    var voiceGeneration by rememberSaveable(profile?.id) { mutableStateOf(0L) }
    var voiceRequest by rememberSaveable(profile?.id, stateSaver = GlobalVoiceRequestSaver) { mutableStateOf<GlobalVoiceRequest?>(null) }
    // Keep the outstanding platform launch separate from the editable/profile-owned search.
    // Closing search must not let a late result be mistaken for a newer request.
    var platformVoiceRequest by rememberSaveable(stateSaver = GlobalVoiceRequestSaver) { mutableStateOf<GlobalVoiceRequest?>(null) }
    val voiceLauncher = rememberLauncherForActivityResult(VoiceSearchContract()) { result ->
        val request = platformVoiceRequest
        platformVoiceRequest = null
        if (request != null && voiceRequest?.id == request.id && voiceRequest?.profileId == request.profileId) {
            when (result) {
                is VoiceSearchResult.Recognized -> {
                    GlobalSearch.voiceResult(request, profile?.id, query, isSearching, result.text)?.let { query = it }
                    voiceRequest = null
                }
                VoiceSearchResult.Cancelled -> voiceRequest = null
                VoiceSearchResult.Unavailable -> {
                    voiceRequest = if (request.profileId == profile?.id && request.originalQuery == query && isSearching) {
                        request.copy(scenario = GlobalVoiceScenario.Unavailable)
                    } else null
                }
            }
        }
    }
    val globalActive = isSearching && (query.isNotBlank() || searchFilters.active)
    val globalResults = remember(profile, query, searchFilters, globalActive) {
        if (profile != null && globalActive) GlobalSearch.results(profile, query, searchFilters) else null
    }
    val showPeople = globalActive && query.isNotBlank() && !searchFilters.active
    val people = remember(profile, query, peopleScenario, lookupRetried, showPeople) {
        if (profile != null && showPeople) GlobalSearch.people(profile, query, if (lookupRetried) PeopleSearchScenario.Success else peopleScenario) else null
    }
    val hasLookupFeedback = lookupPending || people?.status in setOf(PeopleSearchStatus.AddressNotFound, PeopleSearchStatus.Unavailable, PeopleSearchStatus.Partial) ||
        (people?.status == PeopleSearchStatus.InvalidIdentifier && GlobalSearch.identifierIntent(query))
    LaunchedEffect(profile?.id, profile?.chats) {
        profile?.let { searchFilters = searchFilters.reconcile(it) }
    }
    LaunchedEffect(profile?.id, query, lookupRetried, showPeople) {
        lookupPending = showPeople && GlobalSearch.identifierIntent(query)
        if (lookupPending) { delay(300); lookupPending = false }
    }
    var muteChat by remember(profile?.id) { mutableStateOf<Chat?>(null) }
    var leaveChat by remember(profile?.id) { mutableStateOf<Chat?>(null) }
    var deleteIds by rememberSaveable(profile?.id) { mutableStateOf(emptyList<String>()) }
    var folderTargets by rememberSaveable(profile?.id) { mutableStateOf(emptyList<String>()) }
    var selectedIds by rememberSaveable(profile?.id) { mutableStateOf(emptyList<String>()) }
    var folderId by rememberSaveable(profile?.id) { mutableStateOf<String?>(null) }
    val selectedFolder = profile?.chatFolders?.firstOrNull { it.id == folderId }
    LaunchedEffect(profile?.id, folderId, selectedFolder?.id) {
        if (folderId != null && selectedFolder == null) { folderId = null; scopeName = ChatScope.Chats.name }
    }
    var soleAdminChat by remember(profile?.id) { mutableStateOf<Chat?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val rows = remember(profile?.chats, scope, query, selectedFolder, globalResults) {
        if (globalResults != null) globalResults.chats
        else if (selectedFolder == null) ChatProjection.rows(profile?.chats.orEmpty(), scope, query)
        else ChatFolders.rows(profile.chats, selectedFolder, query)
    }

    val visibleSelected = ChatOrganization.reconcile(selectedIds, rows)
    val selecting = visibleSelected.isNotEmpty()
    val ownedAttempt = batchAttempt?.takeIf { it.profileId == profile?.id }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(profile?.id, profile?.chatConnection, lifecycle) {
        val owner = profile ?: return@LaunchedEffect
        val state = owner.chatConnection
        if (state.phase == ChatConnectionPhase.Connecting || state.phase == ChatConnectionPhase.CatchingUp) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                delay(700)
                onAdvanceConnection(owner.id, state.generation, state.phase)
            }
        }
    }
    LaunchedEffect(rows.map { it.id }) { selectedIds = ChatOrganization.reconcile(selectedIds, rows) }

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
        if (action !in ChatOrganization.actions(chat, profile.chats)) return
        val undo = ChatListUndo.capture(profile.id, chat, action)
        when (action) {
            ChatListAction.Read -> onMarkUnread(chat.id, false)
            ChatListAction.Unread -> onMarkUnread(chat.id, true)
            ChatListAction.Pin, ChatListAction.Unpin -> onTogglePin(chat.id)
            ChatListAction.Mute -> muteChat = chat
            ChatListAction.Unmute -> onMute(chat.id, null)
            ChatListAction.Archive -> onArchive(chat.id, true)
            ChatListAction.Unarchive -> onArchive(chat.id, false)
            ChatListAction.Leave -> if (chat.isSoleAdmin(profile.id) && chat.members.any { it.personId != profile.id }) soleAdminChat = chat else leaveChat = chat
            ChatListAction.Delete -> deleteIds = listOf(chat.id)
            ChatListAction.Select -> { selectedIds = listOf(chat.id); focusManager.clearFocus(); keyboardController?.hide() }
            ChatListAction.MoveUp -> onMovePin(chat.id, -1)
            ChatListAction.MoveDown -> onMovePin(chat.id, 1)
            ChatListAction.Folder -> folderTargets = listOf(chat.id)
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
        searchFilters = GlobalSearchFilters()
        searchFilterCategory = null
        filterMenuOpen = false
        voiceRequest = null
    }
    fun startVoice() {
        val owner = profile ?: return
        if (platformVoiceRequest != null) return
        focusManager.clearFocus()
        keyboardController?.hide()
        val request = GlobalVoiceRequest(++voiceGeneration, owner.id, query, onVoiceScenario())
        voiceRequest = request
        if (request.scenario == GlobalVoiceScenario.Device) {
            platformVoiceRequest = request
            try {
                voiceLauncher.launch(Unit)
            } catch (_: ActivityNotFoundException) {
                platformVoiceRequest = null
                voiceRequest = request.copy(scenario = GlobalVoiceScenario.Unavailable)
            } catch (_: SecurityException) {
                platformVoiceRequest = null
                voiceRequest = request.copy(scenario = GlobalVoiceScenario.Unavailable)
            }
        }
    }

    BackHandler(enabled = isSearching, onBack = ::closeSearch)
    BackHandler(enabled = selecting) { selectedIds = emptyList() }
    BackHandler(enabled = menuTarget != null) { menuTarget = null }

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AdaptiveContent(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
                if (selecting) ChatSelectionBar(onClose = { selectedIds = emptyList() }) else Crossfade(
                    targetState = isSearching,
                    label = "Chats search mode",
                ) { searching ->
                    if (searching) {
                        ChatsSearchTopBar(
                            query = query,
                            onQueryChange = { query = it },
                            onClose = ::closeSearch,
                            closeSearchDescription = closeSearchDescription,
                            onVoice = ::startVoice,
                            voicePending = platformVoiceRequest != null,
                            filters = searchFilters,
                            filterMenuOpen = filterMenuOpen,
                            onFilterMenuOpenChange = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                filterMenuOpen = it
                            },
                            onFilterCategory = { searchFilterCategory = it },
                            onClearFilters = { searchFilters = GlobalSearchFilters() },
                        )
                    } else {
                        Column {
                            ChatsTopBar(
                                profile = profile,
                                updateState = appUpdates?.state,
                                onSettings = { menuTarget = null; onSettings() },
                                onSearch = { isSearching = true },
                                searchChatsDescription = searchChatsDescription,
                            )
                            ChatFolderPills(
                                profile = profile,
                                scope = scope,
                                folderId = selectedFolder?.id,
                                onFolderChange = { menuTarget = null; folderId = it },
                                onScopeChange = {
                                    menuTarget = null
                                    folderId = null
                                    scopeName = it.name
                                },
                                onFolders = { menuTarget = null; onFolders() },
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (selecting) AdaptiveContent(
                Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                ChatSelectionBottomBar(
                    selected = rows.filter { it.id in visibleSelected },
                    onSelectAll = { selectedIds = rows.map { it.id } },
                    onAction = { action ->
                        when (action) {
                            ChatBulkAction.Delete -> deleteIds = visibleSelected
                            ChatBulkAction.Folder -> folderTargets = visibleSelected
                            else -> onBeginBatch?.invoke(visibleSelected, action, null, false)
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isSearching && !selecting) {
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
                    if (isSearching || selecting) WhiteNoiseSpacing.CompactScreenMargin
                    else 56.dp + WhiteNoiseSpacing.CompactScreenMargin * 2),
            ) {
                if (isSearching && !selecting && profile != null && searchFilters.active) item(key = "search-filters") {
                    GlobalSearchFilterBar(profile, searchFilters, onChange = { searchFilters = it })
                }
                profile?.let { owner ->
                    item(key = "connection") { ChatConnectionBanner(owner, onRetryConnection, onProfileRelays) }
                }
                if (globalActive && !selecting && rows.isNotEmpty()) item(key = "chat-results-heading") { GlobalSearchHeading(stringResource(R.string.global_chats)) }
                if (rows.isEmpty() && (!globalActive || (globalResults?.messages.isNullOrEmpty() && people?.people.isNullOrEmpty() && !hasLookupFeedback))) {
                    item {
                        if (globalActive) {
                            WhiteNoiseEmptyState(stringResource(R.string.global_no_matches), stringResource(R.string.global_no_matches_detail))
                        } else if (selectedFolder != null && query.isBlank()) Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            WhiteNoiseEmptyState(stringResource(R.string.chat_folder_empty), stringResource(R.string.chat_folder_empty_detail))
                        } else ChatEmptyState(scope, query.isNotBlank(), Modifier.fillParentMaxSize())
                    }
                }
                items(rows, key = Chat::id) { chat ->
                    val target = ChatMenuTarget(profile?.id.orEmpty(), chat.id)
                    ChatContextMenuRow(
                        chat = chat,
                        expanded = menuTarget == target,
                        onOpen = {
                            menuTarget = null
                            if (selecting) selectedIds = if (chat.id in selectedIds) selectedIds - chat.id else selectedIds + chat.id
                            else { focusManager.clearFocus(); keyboardController?.hide(); onOpenChat(chat.id) }
                        },
                        onShowMenu = {
                            filterMenuOpen = false
                            if (selecting) selectedIds = if (chat.id in selectedIds) selectedIds - chat.id else selectedIds + chat.id
                            else menuTarget = target
                        },
                        selecting = selecting,
                        checked = chat.id in visibleSelected,
                        availableActions = ChatOrganization.actions(chat, profile?.chats.orEmpty()),
                        onDismissMenu = { if (menuTarget == target) menuTarget = null },
                        onAction = { performAction(target, it) },
                    )
                }
                if (globalActive && !selecting) {
                    val messages = globalResults?.messages.orEmpty()
                    if (messages.isNotEmpty()) item(key = "messages-heading") { GlobalSearchHeading(stringResource(R.string.global_messages)) }
                    items(messages, key = { "message:${it.chatId}:${it.message.id}" }) { result ->
                        GlobalMessageRow(result, query) {
                            focusManager.clearFocus(); keyboardController?.hide()
                            if (!onOpenSearchMessage(result.chatId, result.message.id)) coroutineScope.launch {
                                snackbarHostState.showSnackbar(resources.getString(R.string.global_message_unavailable))
                            }
                        }
                    }
                    if (people != null) {
                        if (people.people.isNotEmpty() || hasLookupFeedback) item(key = "people-heading") { GlobalSearchHeading(stringResource(R.string.folder_people)) }
                        item(key = "people-feedback") { GlobalPeopleFeedback(people.status, GlobalSearch.identifierIntent(query), lookupPending) { lookupRetried = true } }
                        if (!lookupPending) items(people.people, key = { "person:${it.person.id}" }) { result ->
                            GlobalPersonRow(result, unknown = people.status == PeopleSearchStatus.NoProfile) { person ->
                                focusManager.clearFocus(); keyboardController?.hide(); onOpenSearchPerson(person)
                            }
                        }
                    }
                }
            }
        }
    }
    searchFilterCategory?.let { category ->
        if (profile != null) GlobalSearchFilterPicker(profile, category, searchFilters,
            onChange = { searchFilters = it }, onDismiss = { searchFilterCategory = null })
    }
    voiceRequest?.takeIf { it.scenario != GlobalVoiceScenario.Device }?.let { request ->
        GlobalVoiceDialog(request, onComplete = { completed ->
            if (voiceRequest?.id == completed.id) {
                GlobalSearch.voiceResult(completed, profile?.id, query, isSearching, GlobalSearch.voicePhrase)?.let { query = it }
                voiceRequest = null
            }
        }, onRetry = ::startVoice, onDismiss = { voiceRequest = null })
    }

    val notifications = dev.ipf.whitenoise.ui.settings.LocalNotificationControls.current
    muteChat?.let { chat ->
        MuteDurationDialog(
            onDismiss = { muteChat = null },
            selectedDuration = chat.muteDuration,
            onCustomSelect = notifications?.let { controller -> { until: Long ->
                controller.request(dev.ipf.whitenoise.state.NotificationChange.Mute(dev.ipf.whitenoise.model.MuteDuration.Custom,until),chat.id,profile?.id); muteChat = null
            } },
            nowMillis = { notifications?.nowMillis ?: dev.ipf.whitenoise.model.MessageForwarding.nowMillis },
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
            text = { Text(stringResource(if (chat.members.singleOrNull()?.personId == profile?.id) R.string.group_delete_solo_detail else R.string.leave_group_detail)) },
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

    soleAdminChat?.let { chat ->
        AlertDialog(
            onDismissRequest = { soleAdminChat = null },
            title = { Text(stringResource(R.string.sole_admin_title)) },
            text = { Text(stringResource(R.string.sole_admin_detail)) },
            confirmButton = {
                TextButton(onClick = { soleAdminChat = null; onOpenGroup(chat.id) }) { Text(stringResource(R.string.group_transfer_admin)) }
            },
        )
    }

    if (profile != null && deleteIds.isNotEmpty()) ChatDeleteConfirmation(
        chats = profile.chats.filter { it.id in deleteIds },
        onDismiss = { deleteIds = emptyList() },
        onConfirm = { leave ->
            if (onBeginBatch == null) deleteIds.forEach(onDelete)
            else onBeginBatch(deleteIds, ChatBulkAction.Delete, null, leave)
            deleteIds = emptyList()
        },
    )
    if (profile != null && folderTargets.isNotEmpty()) ChatFolderPicker(profile,
        onDismiss = { folderTargets = emptyList() }, onCreate = onCreateFolder,
        onSelect = { folder ->
            onBeginBatch?.invoke(folderTargets, ChatBulkAction.Folder, folder, false)
            folderTargets = emptyList()
        },
    )
    ownedAttempt?.let { attempt ->
        ChatBatchProgress(attempt, onAdvanceBatch,
            onDismiss = { selectedIds = ChatOrganization.reconcile(attempt.failedIds, rows); onDismissBatch() },
            onRetry = onRetryBatch, onOpenGroup = onOpenGroup)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatsTopBar(
    profile: Profile?,
    updateState: dev.ipf.whitenoise.model.AppUpdateState?,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    searchChatsDescription: String,
) {
    val settingsDescription = profile?.let {
        stringResource(R.string.open_settings_for, it.name)
    }
    TopAppBar(
        title = {},
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
            updateState?.let { AppUpdateIconButton(it, onOpenSettings = onSettings) }
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

/** Saved folders keep their management order; selecting an active pill never clears it. */
@Composable
private fun ChatFolderPills(
    profile: Profile?,
    scope: ChatScope,
    folderId: String?,
    onFolderChange: (String) -> Unit,
    onScopeChange: (ChatScope) -> Unit,
    onFolders: () -> Unit,
) {
    val folders = profile?.chatFolders.orEmpty()
    val selectedIndex = when {
        folderId != null -> folders.indexOfFirst { it.id == folderId } + 1
        scope == ChatScope.Left -> folders.size + 1
        else -> 0
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    LaunchedEffect(profile?.id, selectedIndex, folders.map { it.id }) {
        val layout = listState.layoutInfo
        val item = layout.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
        if (item == null || item.offset < layout.viewportStartOffset ||
            item.offset + item.size > layout.viewportEndOffset
        ) listState.scrollToItem(selectedIndex)
    }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().testTag("chats.folders"),
        contentPadding = PaddingValues(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
        horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "scope:chats") {
            ChatFolderPill(chatScopeLabel(ChatScope.Chats), folderId == null && scope == ChatScope.Chats,
                "chats.scope.chats", { onScopeChange(ChatScope.Chats) })
        }
        items(folders, key = { "folder:${it.id}" }) { folder ->
            ChatFolderPill(folder.name, folder.id == folderId,
                "chats.folder.${folder.id}", { onFolderChange(folder.id) })
        }
        item(key = "scope:left") {
            ChatFolderPill(chatScopeLabel(ChatScope.Left), folderId == null && scope == ChatScope.Left,
                "chats.scope.left", { onScopeChange(ChatScope.Left) })
        }
        item(key = "manage") {
            TextButton(onClick = onFolders, modifier = Modifier.testTag("chats.manageFolders")) {
                Icon(painterResource(R.drawable.ic_folder), contentDescription = null,
                    modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.chat_folders), modifier = Modifier.padding(start = WhiteNoiseSpacing.Related))
            }
        }
    }
}

@Composable
private fun ChatFolderPill(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.testTag(tag),
        shape = CircleShape,
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun chatScopeLabel(scope: ChatScope): String = stringResource(
    when (scope) {
        ChatScope.Chats -> R.string.chats
        ChatScope.Unread -> R.string.unread
        ChatScope.Archived -> R.string.archived
        ChatScope.Left -> R.string.left_chats
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatsSearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    closeSearchDescription: String,
    onVoice: () -> Unit,
    voicePending: Boolean,
    filters: GlobalSearchFilters,
    filterMenuOpen: Boolean,
    onFilterMenuOpenChange: (Boolean) -> Unit,
    onFilterCategory: (String) -> Unit,
    onClearFilters: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        if (!voicePending) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    TopAppBar(
        title = {
            WhiteNoiseCompactSearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.search_chats),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chats.searchField")
                    .focusRequester(focusRequester),
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
        actions = {
            Box {
                ChatFilterIconButton(
                    active = filters.active,
                    description = stringResource(R.string.global_filters),
                    modifier = Modifier.testTag("global.filterButton"),
                    onClick = { onFilterMenuOpenChange(true) },
                )
                GlobalSearchFilterMenu(
                    expanded = filterMenuOpen, filters = filters,
                    onDismiss = { onFilterMenuOpenChange(false) },
                    onCategory = onFilterCategory, onClear = onClearFilters,
                )
            }
            IconButton(onClick = onVoice, enabled = !voicePending) {
                Icon(painterResource(R.drawable.ic_mic), stringResource(R.string.global_voice))
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

private val GlobalVoiceRequestSaver = listSaver<GlobalVoiceRequest?, Any>(
    save = { request ->
        request?.let { listOf(it.id, it.profileId, it.originalQuery, it.scenario.name) } ?: emptyList()
    },
    restore = { values ->
        if (values.isEmpty()) null else GlobalVoiceRequest(
            values[0] as Long, values[1] as String, values[2] as String,
            GlobalVoiceScenario.valueOf(values[3] as String),
        )
    },
)

/** Advanced search filters retain a native icon and anchored menu. */
@Composable
private fun ChatFilterIconButton(
    active: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantics = modifier.semantics {
        contentDescription = description
        selected = active
    }
    val icon: @Composable () -> Unit = {
        Icon(painterResource(R.drawable.ic_filter_list), contentDescription = null)
    }
    if (active) FilledIconButton(onClick = onClick, modifier = semantics, content = icon)
    else IconButton(onClick = onClick, modifier = semantics, content = icon)
}

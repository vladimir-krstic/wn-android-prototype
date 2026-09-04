@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.ipf.whitenoise.ui.chats

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.conversation.LocalGroupWork
import dev.ipf.whitenoise.ui.conversation.GroupCreateStatus
import dev.ipf.whitenoise.ui.conversation.GroupEmojiImageDialog

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn as LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import dev.ipf.whitenoise.ui.components.whiteNoiseVerticalScroll
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatMembership
import dev.ipf.whitenoise.model.GroupRole
import dev.ipf.whitenoise.model.Person
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.ui.components.AvatarPhotoButton
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.LocalWhiteNoiseHeaderScroll
import dev.ipf.whitenoise.ui.components.WhiteNoiseCompactSearchField
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
import dev.ipf.whitenoise.ui.components.WhiteNoiseEmptyState
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.onboarding.AvatarImageProcessor
import dev.ipf.whitenoise.ui.onboarding.AvatarWebImagePicker
import dev.ipf.whitenoise.ui.settings.ProfileBanner
import dev.ipf.whitenoise.ui.settings.ProfileImageViewer
import dev.ipf.whitenoise.ui.settings.IdentifierCopyCapsule
import dev.ipf.whitenoise.ui.settings.SettingsAction
import dev.ipf.whitenoise.ui.settings.SettingsBottomAction
import dev.ipf.whitenoise.ui.settings.SettingsDivider
import dev.ipf.whitenoise.ui.settings.SettingsExplainer
import dev.ipf.whitenoise.ui.settings.SettingsGroup
import dev.ipf.whitenoise.ui.settings.SettingsLink
import dev.ipf.whitenoise.ui.settings.SettingsScaffold
import dev.ipf.whitenoise.ui.settings.SettingsSection
import dev.ipf.whitenoise.ui.settings.copyToClipboard
import dev.ipf.whitenoise.model.matchesPeopleQuery
import dev.ipf.whitenoise.model.PeopleDiscovery
import dev.ipf.whitenoise.model.PeopleSearchScenario
import dev.ipf.whitenoise.model.PeopleSearchResult
import dev.ipf.whitenoise.model.PeopleSearchStatus
import dev.ipf.whitenoise.model.PersonSource
import dev.ipf.whitenoise.model.GroupContactAction
import dev.ipf.whitenoise.model.GroupContactScenario
import dev.ipf.whitenoise.model.GroupContactResult
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NewChatScreen(
    profile: Profile,
    onBack: () -> Unit,
    onNewGroup: () -> Unit,
    onPerson: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchScenario: PeopleSearchScenario = PeopleSearchScenario.Success,
    onResolvedPerson: (Person) -> Unit = { onPerson(it.id) },
) {
    var query by rememberSaveable(profile.id) { mutableStateOf("") }
    var retry by remember(profile.id) { mutableIntStateOf(0) }
    var resolving by remember(profile.id, query, retry) { mutableStateOf(query.isNotBlank()) }
    var searchResult by remember(profile.id, query, retry) {
        mutableStateOf(PeopleSearchResult(PeopleDiscovery.local(profile, query), PeopleSearchStatus.Ready))
    }
    LaunchedEffect(profile.id, profile.people, query, retry) {
        resolving = query.isNotBlank()
        if (resolving) delay(600)
        searchResult = PeopleDiscovery.resolve(profile, query, if (retry > 0) PeopleSearchScenario.Success else searchScenario)
        resolving = false
    }
    val bottomScrollClearance = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()
    SettingsScaffold(
        title = stringResource(R.string.new_message),
        onBack = onBack,
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("new_message.list"),
            contentPadding = PaddingValues(
                bottom = bottomScrollClearance + WhiteNoiseSpacing.Section,
            ),
        ) {
            item {
                NewMessageSearchField(query = query, onQueryChange = { query = it })
            }
            item {
                SettingsGroup(modifier = Modifier.padding(top = WhiteNoiseSpacing.Related)) {
                    SettingsLink(
                        title = stringResource(R.string.new_group),
                        onClick = onNewGroup,
                        leading = {
                            Icon(
                                painter = painterResource(R.drawable.ic_group_add),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                    )
                }
            }
            item { PeopleSearchFeedback(profile, searchResult.status, resolving, onRetry = { retry++ }) }
            val sourceGroups = if (query.isBlank()) mapOf(null to searchResult.people) else searchResult.people.groupBy { it.source }
            sourceGroups.forEach { (source, results) ->
                item {
                    SettingsSection(stringResource(when (source) {
                        null -> R.string.people
                        PersonSource.Chats -> R.string.people_source_chats
                        PersonSource.Following -> R.string.people_source_following
                        PersonSource.Network -> R.string.people_source_network
                        PersonSource.Local -> R.string.people_source_local
                    }))
                }
                itemsIndexed(results, key = { _, result -> result.person.id }) { index, result ->
                    CreationPersonRow(
                        person = result.person, onClick = { onResolvedPerson(result.person) },
                        groupIndex = index, groupCount = results.size, showDivider = index != results.lastIndex,
                    )
                }
            }

        }
    }
}

@Composable
fun PersonProfileScreen(
    profile: Profile,
    person: Person,
    onBack: () -> Unit,
    onMessage: () -> Boolean,
    onToggleFollow: () -> Unit,
    onToggleBlock: () -> Unit,
    modifier: Modifier = Modifier,
    showMessageAction: Boolean = true,
    groupRole: GroupRole? = null,
    canManageGroup: Boolean = false,
    onToggleAdmin: () -> Boolean = { false },
    onRemoveFromGroup: () -> Boolean = { false },
    onOpenRelays: () -> Unit = {},
    onGroupsInCommon: () -> Unit = {},
    onAddToGroup: (String) -> Boolean = { false },
    onSavePrivateContact: (String, String) -> Boolean = { _, _ -> false },
    onStartGroup: () -> Unit = {},
    contextGroup: Chat? = null,
    groupScenario: GroupContactScenario = GroupContactScenario.Success,
    onRetryRoster: () -> Unit = {},
    onApplyGroups: (List<String>, GroupContactAction) -> GroupContactResult = { ids, _ ->
        val completed = ids.filter(onAddToGroup); GroupContactResult(completed, ids - completed.toSet())
    },
) {
    var showPrivateDetails by rememberSaveable(profile.id, person.id) { mutableStateOf(false) }
    var groupAction by remember(profile.id, person.id) { mutableStateOf<GroupContactAction?>(null) }
    if (showPrivateDetails) PrivateContactDialog(person, { showPrivateDetails = false }, onSavePrivateContact)
    groupAction?.let { action -> ContactGroupsSheet(profile, person, action, groupScenario,
        onDismiss = { groupAction = null }, onRetryRoster = onRetryRoster, onApply = onApplyGroups) }
    val context = LocalContext.current
    var showRelayError by remember { mutableStateOf(false) }
    var showBlockConfirmation by remember { mutableStateOf(false) }
    var showRoleConfirmation by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }
    var copied by rememberSaveable(person.id) { mutableStateOf(false) }
    val groups = remember(profile.chats, person.id) {
        profile.groupsSharedWith(person.id)
    }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }
    val roleLabel = groupRole?.let {
        stringResource(if (it == GroupRole.Admin) R.string.admin else R.string.member)
    }
    SettingsScaffold(
        title = roleLabel?.let { stringResource(R.string.person_profile_with_role, it) }
            ?: stringResource(R.string.person_profile),
        onBack = onBack,
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showMessageAction) {
                val hasChatRelays = profile.chatRelayUrls.isNotEmpty()
                CreationBottomAction(
                    label = stringResource(
                        if (hasChatRelays) R.string.message else R.string.check_chat_relays,
                    ),
                    onClick = {
                        if (hasChatRelays) {
                            if (!onMessage()) showRelayError = true
                        } else {
                            onOpenRelays()
                        }
                    },
                    icon = if (hasChatRelays) {
                        R.drawable.ic_settings_chat_bubble_outline
                    } else {
                        R.drawable.ic_warning
                    },
                )
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .whiteNoiseVerticalScroll(rememberScrollState())
                    .padding(vertical = WhiteNoiseSpacing.Section)
                    .testTag("person_profile.content"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PersonIdentityHeader(
                    person = person,
                    copied = copied,
                    showIdentityValues = person.about.isBlank(),
                    onCopy = {
                        copyToClipboard(context, "Public key", person.publicKey)
                        copied = true
                    },
                )
                if (person.about.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .padding(top = WhiteNoiseSpacing.FormField)
                            .fillMaxWidth()
                            .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)
                            .testTag("person_profile.about"),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = person.about,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(WhiteNoiseSpacing.CompactScreenMargin),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                            textAlign = TextAlign.Center,
                        )
                    }
                    PersonIdentityValues(
                        person = person,
                        copied = copied,
                        onCopy = {
                            copyToClipboard(context, "Public key", person.publicKey)
                            copied = true
                        },
                        modifier = Modifier.padding(top = WhiteNoiseSpacing.Related),
                    )
                }
                if (contextGroup != null) {
                    dev.ipf.whitenoise.ui.conversation.GroupRosterPanel(profile, contextGroup)
                    dev.ipf.whitenoise.ui.conversation.GroupMemberWorkPanel(profile, contextGroup)
                }
                if (groupRole != null) {
                    SettingsSection(stringResource(R.string.profile_actions))
                } else {
                    Spacer(Modifier.height(WhiteNoiseSpacing.Section))
                }
                SettingsGroup {
                    if (groups.isNotEmpty()) {
                        SettingsLink(
                            title = stringResource(R.string.groups_in_common),
                            onClick = onGroupsInCommon,
                            leading = { GroupAvatarStack(groups) },
                        )
                        SettingsDivider()
                    }
                    SettingsAction(title = stringResource(R.string.contact_private_details), onClick = { showPrivateDetails = true })
                    SettingsDivider()
                    SettingsAction(title = stringResource(R.string.contact_start_group), onClick = onStartGroup,
                        leading = { ProfileActionIcon(R.drawable.ic_group_add) })
                    SettingsDivider()
                    SettingsAction(title = stringResource(R.string.contact_add_groups), onClick = { groupAction = GroupContactAction.Invite })
                    SettingsDivider()
                    SettingsAction(title = stringResource(R.string.contact_promote_groups), onClick = { groupAction = GroupContactAction.Promote })
                    SettingsDivider()
                    SettingsAction(
                        title = stringResource(if (person.isFollowing) R.string.unfollow else R.string.follow),
                        onClick = onToggleFollow,
                        leading = { ProfileActionIcon(R.drawable.ic_settings_person_add) },
                    )
                    SettingsDivider()
                    SettingsAction(
                        title = stringResource(if (person.isBlocked) R.string.unblock else R.string.block),
                        destructive = !person.isBlocked,
                        onClick = {
                            if (person.isBlocked) onToggleBlock() else showBlockConfirmation = true
                        },
                        leading = {
                            ProfileActionIcon(
                                if (person.isBlocked) R.drawable.ic_check else R.drawable.ic_close,
                                destructive = !person.isBlocked,
                            )
                        },
                    )
                }
                if (canManageGroup && groupRole != null) {
                    SettingsSection(stringResource(R.string.group_actions))
                    SettingsGroup {
                        SettingsAction(
                            title = stringResource(
                                if (groupRole == GroupRole.Admin) R.string.remove_admin else R.string.make_admin,
                            ),
                            onClick = { showRoleConfirmation = true },
                            leading = { ProfileActionIcon(R.drawable.ic_person) },
                        )
                        SettingsDivider()
                        SettingsAction(
                            title = stringResource(R.string.remove_from_group),
                            destructive = true,
                            onClick = { showRemoveConfirmation = true },
                            leading = { ProfileActionIcon(R.drawable.ic_delete, destructive = true) },
                        )
                    }
                }
            }
        }
    }
    if (showRelayError) {
        AlertDialog(
            onDismissRequest = { showRelayError = false },
            title = { Text(stringResource(R.string.chat_relays_required_title)) },
            text = { Text(stringResource(R.string.chat_relays_required_detail)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRelayError = false
                        onOpenRelays()
                    },
                ) { Text(stringResource(R.string.check_chat_relays)) }
            },
            dismissButton = {
                TextButton(onClick = { showRelayError = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    if (showBlockConfirmation) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmation = false },
            title = { Text(stringResource(R.string.block_person_question, person.displayName)) },
            text = { Text(stringResource(R.string.block_person_detail)) },
            confirmButton = {
                TextButton(onClick = { showBlockConfirmation = false; onToggleBlock() }) {
                    Text(stringResource(R.string.block), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showBlockConfirmation = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (showRoleConfirmation && groupRole != null) {
        AlertDialog(
            onDismissRequest = { showRoleConfirmation = false },
            title = {
                Text(
                    stringResource(
                        if (groupRole == GroupRole.Admin) R.string.remove_admin_question else R.string.make_admin_question,
                        person.displayName,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { if (onToggleAdmin()) showRoleConfirmation = false }) {
                    Text(stringResource(if (groupRole == GroupRole.Admin) R.string.remove_admin else R.string.make_admin))
                }
            },
            dismissButton = { TextButton(onClick = { showRoleConfirmation = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirmation = false },
            title = { Text(stringResource(R.string.remove_member_question, person.displayName)) },
            confirmButton = {
                TextButton(onClick = { if (onRemoveFromGroup()) onBack() }) {
                    Text(stringResource(R.string.remove_from_group), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showRemoveConfirmation = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

}

@Composable
fun GroupsInCommonScreen(
    profile: Profile,
    person: Person,
    onBack: () -> Unit,
    onOpenGroup: (String) -> Unit,
    onAddToGroup: (String) -> Boolean,
    modifier: Modifier = Modifier,
    groupScenario: GroupContactScenario = GroupContactScenario.Success,
    onRetryRoster: () -> Unit = {},
    onApplyGroups: (List<String>, GroupContactAction) -> GroupContactResult = { ids, _ ->
        val completed = ids.filter(onAddToGroup); GroupContactResult(completed, ids - completed.toSet())
    },
) {
    var showAddToGroup by rememberSaveable(person.id) { mutableStateOf(false) }
    val groups = remember(profile.chats, person.id) { profile.groupsSharedWith(person.id) }
    SettingsScaffold(
        title = stringResource(R.string.groups_in_common),
        onBack = onBack,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("groups_in_common.list"),
            contentPadding = PaddingValues(vertical = WhiteNoiseSpacing.Section),
        ) {
            item {
                SettingsGroup {
                    if (groups.isEmpty()) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    stringResource(R.string.no_groups_in_common),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    } else {
                        groups.forEachIndexed { index, group ->
                            GroupLink(group = group, onClick = { onOpenGroup(group.id) })
                            if (index != groups.lastIndex) SettingsDivider()
                        }
                        SettingsDivider()
                    }
                    SettingsAction(
                        title = stringResource(R.string.add_to_another_group),
                        onClick = { showAddToGroup = true },
                        leading = { ProfileActionIcon(R.drawable.ic_group_add) },
                    )
                }
            }
        }
    }
    if (showAddToGroup) {
        ContactGroupsSheet(profile, person, GroupContactAction.Invite, groupScenario,
            onDismiss = { showAddToGroup = false }, onRetryRoster = onRetryRoster, onApply = onApplyGroups)
    }
}

@Composable
fun NewGroupScreen(
    profile: Profile,
    onBack: () -> Unit,
    onContinue: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    initialPersonId: String? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedIds by rememberSaveable(profile.id, initialPersonId) {
        mutableStateOf(listOfNotNull(initialPersonId).filter { id -> profile.people.any { it.id == id } })
    }
    val selectablePeople = remember(profile.people, profile.id, query) {
        profile.people
            .filter { it.id != profile.id && it.id != "white-noise-support" }
            .filter { it.matches(query) }
            .sortedBy(Person::displayName)
    }
    fun toggle(personId: String) {
        selectedIds = if (personId in selectedIds) selectedIds - personId else selectedIds + personId
    }
    SettingsScaffold(
        title = stringResource(R.string.new_group),
        onBack = onBack,
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            CreationBottomAction(
                onClick = { onContinue(selectedIds) },
                enabled = true,
                label = stringResource(if (selectedIds.isEmpty()) R.string.group_solo_create else R.string.continue_action),
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("new_group.list"),
            contentPadding = PaddingValues(bottom = WhiteNoiseSpacing.Section),
        ) {
            item { PeopleSearchField(query = query, onQueryChange = { query = it }) }
            if (selectedIds.isNotEmpty()) {
                item { SettingsSection(stringResource(R.string.selected_people)) }
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                        contentPadding = PaddingValues(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                    ) {
                        items(selectedIds, key = { it }) { id ->
                            val person = profile.people.first { it.id == id }
                            SelectedPerson(
                                person = person,
                                onRemove = { toggle(id) },
                            )
                        }
                    }
                }
            }
            item { SettingsSection(stringResource(R.string.people)) }
            itemsIndexed(selectablePeople, key = { _, person -> person.id }) { index, person ->
                CreationPersonRow(
                    person = person,
                    onClick = { toggle(person.id) },
                    selected = person.id in selectedIds,
                    groupIndex = index,
                    groupCount = selectablePeople.size,
                    showDivider = index != selectablePeople.lastIndex,
                )
            }
            if (selectablePeople.isEmpty()) {
                item {
                    WhiteNoiseEmptyState(
                        title = stringResource(R.string.no_results),
                        detail = stringResource(R.string.no_results_detail),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
fun GroupSetupScreen(
    profile: Profile,
    selectedPersonIds: List<String>,
    onBack: () -> Unit,
    onCreate: (String, String, ProfileAvatar) -> Boolean,
    modifier: Modifier = Modifier,
    onOpenRelays: () -> Unit = {},
    creationOrigin: String? = null,
) {
    val workController = LocalGroupWork.current
    val work = workController?.creation?.takeIf { it.profileId == profile.id && it.origin == creationOrigin }
    val editable = work == null
    var timer by rememberSaveable(profile.id) { mutableStateOf(DisappearingDuration.Off) }
    var timerOpen by rememberSaveable(profile.id) { mutableStateOf(false) }
    var emojiOpen by rememberSaveable(profile.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val groupName = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState() }
    val description = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState() }
    var avatar by remember { mutableStateOf<ProfileAvatar>(ProfileAvatar.Monogram) }
    var webChoiceId by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var webPickerOpen by remember { mutableStateOf(false) }
    var processingJob by remember { mutableStateOf<Job?>(null) }
    var processingGeneration by remember { mutableIntStateOf(0) }
    var isPreparingPhoto by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf(false) }
    var relayError by remember { mutableStateOf(false) }
    val presentedAvatar = work?.draft?.image ?: avatar
    val selectedPeople = selectedPersonIds.mapNotNull { id -> profile.people.firstOrNull { it.id == id } }

    fun prepare(uri: android.net.Uri) {
        val generation = ++processingGeneration
        processingJob?.cancel()
        processingJob = coroutineScope.launch {
            isPreparingPhoto = true
            photoError = false
            try {
                val bytes = AvatarImageProcessor.prepare(context.contentResolver, uri)
                if (generation != processingGeneration) return@launch
                if (bytes == null) {
                    photoError = true
                } else {
                    avatar = ProfileAvatar.DeviceImage(bytes)
                    webChoiceId = null
                    photoError = false
                }
            } finally {
                if (generation == processingGeneration) isPreparingPhoto = false
            }
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::prepare) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(::prepare) }
    DisposableEffect(Unit) { onDispose { processingJob?.cancel() } }

    SettingsScaffold(
        title = stringResource(R.string.set_up_group),
        onBack = onBack,
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            CreationBottomAction(
                onClick = {
                    val accepted = if (workController != null && creationOrigin != null) workController.beginCreate(
                        profile.id, creationOrigin, GroupEditDraft(groupName.text.toString(), description.text.toString(), avatar, ProfileAvatar.Monogram), selectedPersonIds, timer,
                    ) else onCreate(groupName.text.toString(), description.text.toString(), avatar)
                    if (!accepted) relayError = true
                },
                enabled = groupName.text.isNotBlank() &&
                    editable && !isPreparingPhoto,
                label = stringResource(R.string.create_group),
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .whiteNoiseVerticalScroll(rememberScrollState())
                    .padding(vertical = WhiteNoiseSpacing.Section)
                    .testTag("group_setup.content"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) {
                    ProfileAvatar(
                        name = groupName.text.toString(),
                        avatar = presentedAvatar,
                        modifier = Modifier
                            .size(120.dp)
                            .testTag("group_setup.avatar"),
                        emptyMonogramIcon = R.drawable.ic_group,
                        contentDescription = stringResource(R.string.group_photo),
                    )
                    Box {
                        AvatarPhotoButton(
                            hasPhoto = presentedAvatar != ProfileAvatar.Monogram,
                            onClick = { menuOpen = true },
                            enabled = editable && !isPreparingPhoto,
                            modifier = Modifier.testTag("group_setup.photoAction"),
                        )
                        WhiteNoiseDropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            items = buildList {
                                add(
                                    WhiteNoiseMenuItem(
                                        label = stringResource(R.string.choose_photos),
                                        icon = R.drawable.ic_image,
                                        onClick = {
                                            photoPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                            )
                                        },
                                    ),
                                )
                                add(
                                    WhiteNoiseMenuItem(
                                        label = stringResource(R.string.choose_files),
                                        icon = R.drawable.ic_description,
                                        onClick = { filePicker.launch(arrayOf("image/*")) },
                                    ),
                                )
                                add(
                                    WhiteNoiseMenuItem(
                                        label = stringResource(R.string.find_web_image),
                                        icon = R.drawable.ic_search,
                                        onClick = { webPickerOpen = true },
                                    ),
                                )
                                add(WhiteNoiseMenuItem(label = stringResource(R.string.group_emoji_create), icon = R.drawable.ic_add, onClick = { emojiOpen = true }))
                                if (avatar != ProfileAvatar.Monogram) {
                                    add(
                                        WhiteNoiseMenuItem(
                                            label = stringResource(R.string.remove_photo),
                                            icon = R.drawable.ic_delete,
                                            destructive = true,
                                            onClick = {
                                                processingJob?.cancel()
                                                isPreparingPhoto = false
                                                avatar = ProfileAvatar.Monogram
                                                webChoiceId = null
                                                photoError = false
                                            },
                                        ),
                                    )
                                }
                            },
                        )
                    }
                    if (isPreparingPhoto) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                        ) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.preparing_photo))
                        }
                    }
                    if (photoError) {
                        Text(
                            text = stringResource(R.string.photo_error),
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = WhiteNoiseSpacing.CompactScreenMargin,
                            top = WhiteNoiseSpacing.Section,
                            end = WhiteNoiseSpacing.CompactScreenMargin,
                        ),
                    verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
                ) {
                    WhiteNoiseTextField(
                        state = groupName,
                        enabled = editable,
                        modifier = Modifier.fillMaxWidth().testTag("group_setup.name"),
                        label = { Text(stringResource(R.string.group_name)) },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )
                    WhiteNoiseTextField(
                        state = description,
                        enabled = editable,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.group_description)) },
                        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 6),
                    )
                }
                dev.ipf.whitenoise.ui.settings.SettingsGroup(Modifier.padding(top = WhiteNoiseSpacing.Section)) {
                    dev.ipf.whitenoise.ui.settings.SettingsLink(stringResource(R.string.disappearing_messages_title), value = timer.label, onClick = { timerOpen = true }, enabled = editable)
                }
                if (work != null) GroupCreateStatus(work, { workController.retryCreate(work.id) }, { workController.skipFailedTimer(work.id) })
                SettingsSection(stringResource(R.string.members))
                if (selectedPeople.isEmpty()) SettingsExplainer(stringResource(R.string.group_solo_detail))
                selectedPeople.forEachIndexed { index, person ->
                    CreationPersonRow(
                        person = person,
                        onClick = null,
                        groupIndex = index,
                        groupCount = selectedPeople.size,
                        showDivider = index != selectedPeople.lastIndex,
                    )
                }
                SettingsExplainer(
                    pluralStringResource(
                        R.plurals.relay_count,
                        profile.chatRelayUrls.size,
                        profile.chatRelayUrls.size,
                    ),
                )
                Spacer(Modifier.height(WhiteNoiseSpacing.Section))
            }
        }
    }
    if (emojiOpen) GroupEmojiImageDialog({ emojiOpen = false }) { image ->
        processingGeneration++; processingJob?.cancel(); isPreparingPhoto = false
        avatar = image; photoError = false; webChoiceId = null; emojiOpen = false
    }
    if (timerOpen) dev.ipf.whitenoise.ui.settings.SpeechSettingsChoices(stringResource(R.string.disappearing_messages_title), DisappearingDuration.entries.map { duration ->
        dev.ipf.whitenoise.ui.settings.SpeechSettingOption(duration.label, timer == duration) { timer = duration; timerOpen = false }
    }, { timerOpen = false })
    if (webPickerOpen) {
        AvatarWebImagePicker(
            currentChoiceId = webChoiceId,
            onDismiss = { webPickerOpen = false },
            onUseImage = { choice ->
                processingJob?.cancel()
                isPreparingPhoto = false
                avatar = ProfileAvatar.WebImage(choice.asset, choice.id)
                webChoiceId = choice.id
                photoError = false
                webPickerOpen = false
            },
        )
    }
    if (relayError) {
        AlertDialog(
            onDismissRequest = { relayError = false },
            title = { Text(stringResource(R.string.chat_relays_required_title)) },
            text = { Text(stringResource(R.string.chat_relays_required_detail)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        relayError = false
                        onOpenRelays()
                    },
                ) { Text(stringResource(R.string.check_chat_relays)) }
            },
            dismissButton = {
                TextButton(onClick = { relayError = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
internal fun PersonIdentityHeader(
    person: Person,
    copied: Boolean,
    showIdentityValues: Boolean,
    onCopy: () -> Unit,
    testTagPrefix: String = "person_profile",
) {
    var viewing by remember(person.id, person.avatar, person.banner) { mutableStateOf<ProfileAvatar?>(null) }
    viewing?.let { ProfileImageViewer(person.id, person.displayName, it, onDismiss = { viewing = null }) }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val avatarSize = (maxWidth * 0.32f).coerceIn(104.dp, 152.dp)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            person.banner?.let { image ->
                ProfileBanner(image, onOpen = { viewing = image }, modifier = Modifier.padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin).padding(bottom = WhiteNoiseSpacing.FormField))
            }
            ProfileAvatar(
                name = person.displayName,
                avatar = person.avatar,
                modifier = Modifier
                    .size(avatarSize)
                    .testTag("$testTagPrefix.avatar")
                    .then(if (person.avatar != ProfileAvatar.Monogram) Modifier.clickable(onClickLabel = stringResource(R.string.profile_view_photo), role = Role.Button) { viewing = person.avatar } else Modifier),
                contentDescription = stringResource(R.string.profile_photo_for, person.displayName),
            )
            Text(
                text = person.displayName,
                modifier = Modifier
                    .padding(
                        start = WhiteNoiseSpacing.CompactScreenMargin,
                        top = WhiteNoiseSpacing.FormField,
                        end = WhiteNoiseSpacing.CompactScreenMargin,
                    )
                    .testTag("$testTagPrefix.name"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (showIdentityValues) {
                PersonIdentityValues(
                    person = person,
                    copied = copied,
                    onCopy = onCopy,
                    testTagPrefix = testTagPrefix,
                )
            }
        }
    }
}

@Composable
private fun PersonIdentityValues(
    person: Person,
    copied: Boolean,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "person_profile",
) {
    val copyState = stringResource(if (copied) R.string.copied else R.string.not_copied)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (person.nostrAddress.isNotBlank()) {
            VerifiedPersonAddress(person)
        }
        if (person.lightningAddress.isNotBlank()) Text(
            stringResource(R.string.profile_lightning_display, person.lightningAddress),
            modifier = Modifier.padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin, vertical = WhiteNoiseSpacing.Related).testTag("person_profile.lightning"),
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        IdentifierCopyCapsule(
            value = person.publicKey,
            copied = copied,
            onCopy = onCopy,
            copyContentDescription = stringResource(R.string.copy_public_key),
            copiedContentDescription = stringResource(R.string.copied),
            notCopiedStateDescription = copyState,
            copiedStateDescription = copyState,
            targetTestTag = "$testTagPrefix.copy_public_key",
            visualTestTag = "$testTagPrefix.copy_public_key.visual",
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun VerifiedPersonAddress(person: Person) {
    Row(
        modifier = Modifier.padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = person.nostrAddress,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
        )
        if (person.isNostrAddressVerified) {
            Icon(
                painter = painterResource(R.drawable.ic_verified_filled),
                contentDescription = stringResource(R.string.verified),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileActionIcon(
    resource: Int,
    destructive: Boolean = false,
) {
    Icon(
        painter = painterResource(resource),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun GroupAvatarStack(groups: List<Chat>) {
    val previews = groups.take(3)
    val remaining = (groups.size - previews.size).coerceAtLeast(0)
    val slotSpacing = 22.dp
    val slotCount = previews.size + if (remaining > 0) 1 else 0
    Box(
        modifier = Modifier
            .width(32.dp + slotSpacing * (slotCount - 1).coerceAtLeast(0))
            .height(32.dp),
    ) {
        previews.forEachIndexed { index, group ->
            Surface(
                modifier = Modifier.offset(x = slotSpacing * index),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                ProfileAvatar(
                    name = group.title,
                    avatar = group.avatar,
                    modifier = Modifier.size(32.dp),
                    contentDescription = null,
                )
            }
        }
        if (remaining > 0) {
            Surface(
                modifier = Modifier.offset(x = slotSpacing * previews.size).size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+$remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupLink(
    group: Chat,
    onClick: () -> Unit,
) {
    val memberCount = group.members.size
    SettingsLink(
        title = group.title,
        subtitle = pluralStringResource(R.plurals.group_member_count, memberCount, memberCount),
        onClick = onClick,
        leading = {
            ProfileAvatar(
                name = group.title,
                avatar = group.avatar,
                modifier = Modifier.size(48.dp),
                contentDescription = null,
            )
        },
    )
}

@Composable
private fun SelectedPerson(
    person: Person,
    onRemove: () -> Unit,
) {
    val removeDescription = stringResource(R.string.remove_person, person.displayName)
    val interactionSource = remember(person.id) { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onRemove,
            )
            .semantics { contentDescription = removeDescription }
            .testTag("new_group.selected.${person.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(72.dp)) {
            ProfileAvatar(
                person.displayName,
                person.avatar,
                Modifier
                    .size(64.dp)
                    .align(Alignment.BottomStart)
                    .clip(CircleShape)
                    .indication(interactionSource, ripple())
                    .testTag("new_group.selected.visual.${person.id}"),
                contentDescription = null,
            )
            Surface(
                modifier = Modifier.size(24.dp).align(Alignment.TopEnd),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Text(
            text = person.displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CreationPersonRow(
    person: Person,
    onClick: (() -> Unit)?,
    selected: Boolean? = null,
    groupIndex: Int? = null,
    groupCount: Int = 0,
    showDivider: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PersonRow(
            person = person,
            onClick = onClick,
            selected = selected,
            groupIndex = groupIndex,
            groupCount = groupCount,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (groupIndex != null) {
                        Modifier.padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)
                    } else {
                        Modifier
                    },
                )
                .testTag("creation.person.${person.id}"),
        )
        if (showDivider) {
            SettingsDivider(
                modifier = Modifier
                    .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)
                    .testTag("creation.person.${person.id}.divider"),
            )
        }
    }
}

@Composable
private fun PersonRow(
    person: Person,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    groupIndex: Int? = null,
    groupCount: Int = 0,
) {
    val grouped = groupIndex != null && groupCount > 0
    val shapes = if (grouped) {
        ListItemDefaults.segmentedShapes(
            index = groupIndex,
            count = groupCount,
            defaultShapes = ListItemDefaults.shapes(shape = RoundedCornerShape(0.dp)),
        ).let { positionalShapes ->
            // Membership is conveyed by the trailing check, not by breaking the group geometry.
            positionalShapes.copy(selectedShape = positionalShapes.shape)
        }
    } else {
        ListItemDefaults.shapes()
    }
    val containerColor = if (grouped) {
        MaterialTheme.colorScheme.surfaceContainerLowest
    } else {
        Color.Transparent
    }
    val colors = ListItemDefaults.colors(
        containerColor = containerColor,
        selectedContainerColor = containerColor,
    )
    val headline: @Composable () -> Unit = {
        Text(
            text = person.displayName,
            modifier = Modifier.testTag("creation.person.${person.id}.name"),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    val supporting: @Composable () -> Unit = {
        Text(person.shortPublicKey, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    val leading: @Composable () -> Unit = {
        ProfileAvatar(
            person.displayName,
            person.avatar,
            Modifier.size(48.dp).testTag("creation.person.${person.id}.avatar"),
            contentDescription = null,
        )
    }
    val trailing: (@Composable () -> Unit)? = if (selected == true) {
        {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    } else {
        null
    }
    when {
        selected != null && onClick != null -> ListItem(
            checked = selected,
            onCheckedChange = { onClick() },
            modifier = modifier,
            leadingContent = leading,
            trailingContent = trailing,
            supportingContent = supporting,
            shapes = shapes,
            colors = colors,
            content = headline,
        )
        onClick != null -> ListItem(
            onClick = onClick,
            modifier = modifier,
            leadingContent = leading,
            trailingContent = trailing,
            supportingContent = supporting,
            shapes = shapes,
            colors = colors,
            content = headline,
        )
        else -> ListItem(
            modifier = modifier,
            leadingContent = leading,
            trailingContent = trailing,
            supportingContent = supporting,
            shapes = shapes,
            colors = colors,
            content = headline,
        )
    }
}

@Composable
private fun NewMessageSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                vertical = WhiteNoiseSpacing.Related,
            )
            .testTag("new_message.searchField"),
        placeholder = { Text(stringResource(R.string.name_or_npub)) },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
            )
        },
        trailingIcon = if (query.isEmpty()) {
            null
        } else {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.clear_search),
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun PeopleSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    WhiteNoiseCompactSearchField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(R.string.name_or_npub),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                vertical = WhiteNoiseSpacing.Related,
            )
            .testTag("creation.searchField"),
    )
}

@Composable
private fun CreationBottomAction(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: Int? = null,
) {
    val scrolled = (LocalWhiteNoiseHeaderScroll.current?.state?.overlappedFraction ?: 0f) > 0f
    val containerColor by animateColorAsState(
        targetValue = if (scrolled) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "CreationBottomActionContainer",
    )
    SettingsBottomAction(
        modifier = Modifier.testTag("creation.bottomAction"),
        color = containerColor,
        tonalElevation = 0.dp,
    ) {
        WhiteNoiseButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .testTag("creation.primaryAction"),
        ) {
            icon?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(WhiteNoiseSpacing.Related))
            }
            Text(label)
        }
    }
}

private fun Profile.groupsSharedWith(personId: String): List<Chat> = chats.filter { chat ->
    chat.isGroup &&
        chat.membership == ChatMembership.Active &&
        chat.members.any { it.personId == id } &&
        chat.members.any { it.personId == personId }
}

private fun Person.matches(query: String): Boolean = matchesPeopleQuery(query)

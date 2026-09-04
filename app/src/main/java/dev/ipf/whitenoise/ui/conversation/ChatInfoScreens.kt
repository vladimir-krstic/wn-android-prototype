@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.ipf.whitenoise.ui.conversation

import dev.ipf.whitenoise.model.*

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn as LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import dev.ipf.whitenoise.ui.components.trackWhiteNoiseHeader
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import dev.ipf.whitenoise.ui.components.whiteNoiseVerticalScroll
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet as ModalBottomSheet
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
import dev.ipf.whitenoise.ui.components.MuteDurationDialog
import androidx.compose.material3.RadioButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatMembership
import dev.ipf.whitenoise.model.ConversationMediaKey
import dev.ipf.whitenoise.model.ConversationMediaProjection
import dev.ipf.whitenoise.model.ConversationMediaSelection
import dev.ipf.whitenoise.model.DisappearingDuration
import dev.ipf.whitenoise.model.GroupRole
import dev.ipf.whitenoise.model.GroupMember
import dev.ipf.whitenoise.model.MuteDuration
import dev.ipf.whitenoise.model.Person
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.SharedContentCategory
import dev.ipf.whitenoise.model.SharedContentProjection
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.chats.PersonIdentityHeader
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseEmptyState
import dev.ipf.whitenoise.ui.components.WhiteNoiseTopBar
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.onboarding.AvatarImageProcessor
import dev.ipf.whitenoise.ui.settings.SettingsSection
import dev.ipf.whitenoise.ui.settings.copyToClipboard
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInfoScreen(
    profile: Profile,
    chat: Chat,
    onBack: () -> Unit,
    onAbout: (String) -> Unit,
    onMember: (String) -> Unit,
    onSharedContent: (SharedContentCategory) -> Unit,
    onRelays: () -> Unit,
    onSearch: () -> Unit,
    onEditGroup: () -> Unit,
    onAddPeople: () -> Unit,
    onMute: (MuteDuration?) -> Unit,
    onDisappearing: (DisappearingDuration) -> Unit,
    onArchive: () -> Unit,
    onLeave: () -> Boolean,
    onDeveloperTools: () -> Unit,
    modifier: Modifier = Modifier,
    onCreateFolder: (String) -> String? = { null },
    onAddToFolder: (String) -> Boolean = { false },
    onCollapseLongMessages: (Boolean) -> Unit = {},
) {
    var folderPicker by rememberSaveable(profile.id, chat.id) { mutableStateOf(false) }
    var folderFailed by rememberSaveable(profile.id, chat.id) { mutableStateOf(false) }
    if (folderPicker) dev.ipf.whitenoise.ui.chats.ChatFolderPicker(profile,
        onDismiss = { folderPicker = false }, onCreate = onCreateFolder,
        onSelect = { if (onAddToFolder(it)) folderPicker = false else folderFailed = true },
        errorMessage = if (folderFailed) stringResource(R.string.folder_assignment_failed) else null)
    var muteSheet by remember { mutableStateOf(false) }
    var disappearingSheet by remember { mutableStateOf(false) }
    var leaveConfirmation by remember { mutableStateOf(false) }
    var onlyAdminWarning by remember { mutableStateOf(false) }
    val directPersonId = (chat.kind as? dev.ipf.whitenoise.model.ChatKind.Direct)?.personId
    val directPerson = profile.people.firstOrNull { it.id == directPersonId }
    val owner = GroupOwner(profile.id, chat.id)
    val workController = LocalGroupWork.current
    val canAdmin = chat.hasGroupAdmin(profile.id)
    val canCommit = chat.hasAuthoritativeGroupAdmin(profile.id) && workController?.locked(owner) != true
    val counts = remember(chat.timeline) { SharedContentProjection.counts(chat, profile) }
    val title = stringResource(if (chat.isGroup) R.string.group_info else R.string.chat_info)
    val technicalActions = listOf(
        ChatInfoAction(
            title = stringResource(R.string.relays),
            subtitle = pluralStringResource(
                R.plurals.chat_relay_count,
                chat.relayUrls.size,
                chat.relayUrls.size,
            ),
            icon = R.drawable.ic_tune,
            onClick = onRelays,
        ),
        ChatInfoAction(
            title = stringResource(R.string.developer_tools),
            icon = R.drawable.ic_bug_report,
            onClick = onDeveloperTools,
        ),
    )
    val lifecycleActions = buildList {
        add(ChatInfoAction(title = stringResource(R.string.chat_add_folder), icon = R.drawable.ic_add,
            onClick = { folderFailed = false; folderPicker = true }))
        add(
            ChatInfoAction(
                title = stringResource(if (chat.isArchived) R.string.unarchive else R.string.archive),
                icon = R.drawable.ic_archive,
                showChevron = false,
                onClick = onArchive,
            ),
        )
        if (chat.membership == ChatMembership.Active) {
            add(
                ChatInfoAction(
                    title = stringResource(if (chat.isGroup) R.string.leave_group else R.string.leave_chat),
                    icon = R.drawable.ic_logout,
                    destructive = true,
                    showChevron = false,
                    onClick = { leaveConfirmation = true },
                ),
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().semantics { paneTitle = title },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            WhiteNoiseTopBar(
                title = "",
                onBack = onBack,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            )
        },
    ) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("chat_info.list"),
                contentPadding = PaddingValues(bottom = WhiteNoiseSpacing.Section),
            ) {
                item(key = "identity") {
                    ChatInfoIdentity(chat = chat, directPerson = directPerson)
                }
                item(key = "quick_actions") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                                vertical = WhiteNoiseSpacing.Related,
                            ),
                        horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                    ) {
                        if (directPerson != null) {
                            QuickInfoAction(
                                label = stringResource(R.string.about),
                                icon = R.drawable.ic_person,
                                modifier = Modifier.weight(1f),
                                onClick = { onAbout(directPerson.id) },
                            )
                        }
                        QuickInfoAction(
                            label = stringResource(if (chat.muteDuration == null) R.string.mute else R.string.unmute),
                            icon = if (chat.muteDuration == null) {
                                R.drawable.ic_notifications_off
                            } else {
                                R.drawable.ic_volume_up
                            },
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (chat.muteDuration == null) muteSheet = true else onMute(null)
                            },
                        )
                        QuickInfoAction(
                            label = stringResource(R.string.disappearing),
                            icon = R.drawable.ic_timer,
                            state = chat.disappearingDuration.label,
                            modifier = Modifier.weight(1f),
                            onClick = { disappearingSheet = true },
                        )
                        QuickInfoAction(
                            label = stringResource(R.string.search),
                            icon = R.drawable.ic_search,
                            modifier = Modifier.weight(1f),
                            onClick = onSearch,
                        )
                    }
                }
                item(key = "shared_heading") { SettingsSection(stringResource(R.string.shared_in_chat)) }
                item(key = "shared_content") {
                    ChatInfoActionGroup(
                        actions = SharedContentCategory.entries.map { category ->
                            ChatInfoAction(
                                title = stringResource(
                                    when (category) {
                                        SharedContentCategory.Media -> R.string.photos_and_videos
                                        SharedContentCategory.Links -> R.string.links
                                        SharedContentCategory.Documents -> R.string.documents
                                        SharedContentCategory.Voice -> R.string.shared_voice
                                    },
                                ),
                                subtitle = pluralStringResource(
                                    R.plurals.shared_item_count,
                                    counts.getValue(category),
                                    counts.getValue(category),
                                ),
                                icon = when (category) {
                                    SharedContentCategory.Media -> R.drawable.ic_image
                                    SharedContentCategory.Links -> R.drawable.ic_link
                                    SharedContentCategory.Documents -> R.drawable.ic_description
                                    SharedContentCategory.Voice -> R.drawable.ic_mic
                                },
                                onClick = { onSharedContent(category) },
                            )
                        },
                    )
                }
                item(key = "actions_heading") {
                    SettingsSection(stringResource(if (chat.isGroup) R.string.advanced else R.string.chat_actions))
                }
                item(key = "collapse_long_messages") {
                    dev.ipf.whitenoise.ui.settings.SettingsGroup {
                        dev.ipf.whitenoise.ui.settings.SettingsSwitch(
                            title = stringResource(R.string.message_collapse_long), checked = chat.collapseLongMessages,
                            onCheckedChange = onCollapseLongMessages, subtitle = stringResource(R.string.message_collapse_long_detail),
                        )
                        dev.ipf.whitenoise.ui.settings.SettingsDivider()
                        dev.ipf.whitenoise.ui.settings.ChatAutoReadSetting(profile, chat)
                    }
                }
                item(key = "technical_actions") {
                    ChatInfoActionGroup(
                        actions = if (chat.isGroup) technicalActions else technicalActions + lifecycleActions,
                    )
                }
                if (chat.isGroup) {
                    item(key = "members_heading") { SettingsSection(stringResource(R.string.members)) }
                    item(key = "roster_status") { GroupRosterPanel(profile, chat) }
                    item(key = "member_work") { GroupMemberWorkPanel(profile, chat) }
                    itemsIndexed(chat.members, key = { _, member -> "member.${member.personId}" }) { index, member ->
                        ChatInfoMemberRow(
                            profile = profile,
                            member = member,
                            index = index,
                            count = chat.members.size,
                            onMember = onMember,
                            pending = workController?.memberWork?.get(owner)?.let { it.running && member.personId in it.personIds } == true,
                        )
                    }
                    if (canAdmin) {
                        item(key = "management") {
                            ChatInfoActionGroup(
                                actions = listOf(
                                    ChatInfoAction(
                                        title = stringResource(R.string.edit_group),
                                        icon = R.drawable.ic_edit,
                                        onClick = onEditGroup,
                                        enabled = canCommit,
                                    ),
                                    ChatInfoAction(
                                        title = stringResource(R.string.add_people),
                                        icon = R.drawable.ic_group_add,
                                        onClick = onAddPeople,
                                        enabled = chat.canPresentMemberAdministration(profile.id) && workController?.locked(owner) != true,
                                    ),
                                ),
                                modifier = Modifier.padding(top = WhiteNoiseSpacing.Section),
                            )
                        }
                    }
                    item(key = "lifecycle") {
                        ChatInfoActionGroup(
                            actions = lifecycleActions,
                            modifier = Modifier.padding(top = WhiteNoiseSpacing.Section),
                        )
                    }
                }
            }
        }
    }

    if (muteSheet) {
        MuteDurationDialog(
            onDismiss = { muteSheet = false },
            selectedDuration = chat.muteDuration,
            onSelect = { onMute(it); muteSheet = false },
        )
    }
    if (disappearingSheet) {
        ModalBottomSheet(onDismissRequest = { disappearingSheet = false }) {
            WhiteNoiseSheetHeader(stringResource(R.string.disappearing_messages_title))
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = WhiteNoiseSpacing.Related)) {
                DisappearingDuration.entries.forEach { duration ->
                    val selected = duration == chat.disappearingDuration
                    ListItem(
                        headlineContent = { Text(duration.label) },
                        trailingContent = {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                modifier = Modifier.clearAndSetSemantics { },
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = selected,
                                role = Role.RadioButton,
                                onValueChange = {
                                    onDisappearing(duration)
                                    disappearingSheet = false
                                },
                            ),
                    )
                }
            }
        }
    }
    if (leaveConfirmation) {
        AlertDialog(
            onDismissRequest = { leaveConfirmation = false },
            title = {
                Text(
                    stringResource(
                        if (chat.isGroup) R.string.leave_group_question else R.string.leave_chat_question,
                    ),
                )
            },
            text = { Text(stringResource(R.string.leave_history_detail)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        leaveConfirmation = false
                        if (!onLeave()) onlyAdminWarning = true
                    },
                ) {
                    Text(
                        stringResource(if (chat.isGroup) R.string.leave_group else R.string.leave_chat),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { leaveConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    if (onlyAdminWarning) {
        AlertDialog(
            onDismissRequest = { onlyAdminWarning = false },
            title = { Text(stringResource(R.string.cant_leave_group)) },
            text = { Text(stringResource(R.string.only_admin_detail)) },
            confirmButton = {
                TextButton(onClick = { onlyAdminWarning = false }) {
                    Text(stringResource(R.string.done))
                }
            },
        )
    }
}

@Composable
private fun ChatInfoIdentity(chat: Chat, directPerson: Person?) {
    val context = LocalContext.current
    var copied by rememberSaveable(chat.id, directPerson?.id) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = WhiteNoiseSpacing.Section, bottom = WhiteNoiseSpacing.FormField),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
    ) {
        if (directPerson != null && !chat.isGroup) {
            PersonIdentityHeader(
                person = directPerson,
                copied = copied,
                showIdentityValues = true,
                onCopy = {
                    copyToClipboard(context, "Public key", directPerson.publicKey)
                    copied = true
                },
                testTagPrefix = "chat_info",
            )
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val avatarSize = (maxWidth * 0.32f).coerceIn(104.dp, 152.dp)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ProfileAvatar(
                        name = chat.title,
                        avatar = chat.visibleAvatar,
                        modifier = Modifier.size(avatarSize).testTag("chat_info.avatar"),
                        contentDescription = stringResource(R.string.profile_photo_for, chat.title),
                    )
                    Text(
                        text = chat.title,
                        modifier = Modifier.padding(top = WhiteNoiseSpacing.FormField).testTag("chat_info.name"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    if (chat.description.isNotBlank()) {
                        Text(
                            text = chat.description,
                            modifier = Modifier.widthIn(max = 440.dp).padding(top = WhiteNoiseSpacing.Related),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    if (chat.isGroup) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.group_member_count,
                                chat.members.size,
                                chat.members.size,
                            ),
                            modifier = Modifier.padding(top = WhiteNoiseSpacing.Related),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        if (chat.membership != ChatMembership.Active) {
            val status = when (chat.membership) {
                ChatMembership.Invited -> stringResource(R.string.invitation_pending)
                ChatMembership.Left -> stringResource(
                    if (chat.isGroup) R.string.left_group_status else R.string.left_chat_status,
                )
                ChatMembership.Removed -> stringResource(R.string.removed_group_status)
                ChatMembership.Active -> ""
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = status,
                    modifier = Modifier.padding(
                        horizontal = WhiteNoiseSpacing.FormField,
                        vertical = 4.dp,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun QuickInfoAction(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: String? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(56.dp).semantics {
                state?.let { stateDescription = it }
            },
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = label,
            modifier = Modifier.clearAndSetSemantics { },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private data class ChatInfoAction(
    val title: String,
    @param:DrawableRes val icon: Int,
    val onClick: () -> Unit,
    val subtitle: String? = null,
    val destructive: Boolean = false,
    val showChevron: Boolean = true,
    val enabled: Boolean = true,
)

@Composable
private fun ChatInfoActionGroup(actions: List<ChatInfoAction>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        actions.forEachIndexed { index, action ->
            val contentColor = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            ListItem(
                onClick = action.onClick,
                enabled = action.enabled,
                content = { Text(action.title) },
                supportingContent = action.subtitle?.let { { Text(it) } },
                leadingContent = {
                    Icon(painterResource(action.icon), contentDescription = null, modifier = Modifier.size(24.dp))
                },
                trailingContent = if (action.showChevron) {
                    { Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null) }
                } else {
                    null
                },
                shapes = ListItemDefaults.segmentedShapes(index, actions.size),
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    contentColor = contentColor,
                    leadingContentColor = contentColor,
                ),
            )
        }
    }
}

@Composable
private fun ChatInfoMemberRow(
    profile: Profile,
    member: GroupMember,
    index: Int,
    count: Int,
    onMember: (String) -> Unit,
    pending: Boolean = false,
) {
    val isSelf = member.personId == profile.id
    val person = if (isSelf) null else profile.people.firstOrNull { it.id == member.personId }
    val name = if (isSelf) stringResource(R.string.you) else person?.displayName ?: member.personId
    val headline: @Composable () -> Unit = { Text(name) }
    val supporting: @Composable () -> Unit = {
        Text(stringResource(if (pending) R.string.group_member_updating else if (member.role == GroupRole.Admin) R.string.admin else R.string.member))
    }
    val leading: @Composable () -> Unit = {
        ProfileAvatar(
            name = name,
            avatar = if (isSelf) profile.avatar else person?.avatar ?: dev.ipf.whitenoise.model.ProfileAvatar.Monogram,
            modifier = Modifier.size(48.dp),
            contentDescription = null,
        )
    }
    val rowModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)
        .padding(bottom = if (index == count - 1) 0.dp else 2.dp)
        .testTag("chat_info.member.${member.personId}")
    val shapes = ListItemDefaults.segmentedShapes(index, count)
    val colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    if (person == null) {
        ListItem(
            modifier = rowModifier,
            content = headline,
            supportingContent = supporting,
            leadingContent = leading,
            shapes = shapes,
            colors = colors,
        )
    } else {
        ListItem(
            onClick = { onMember(person.id) },
            modifier = rowModifier,
            content = headline,
            supportingContent = supporting,
            leadingContent = leading,
            trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null) },
            shapes = shapes,
            colors = colors,
        )
    }
}

@Composable
private fun InfoSectionLabel(label: String) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = WhiteNoiseSpacing.CompactScreenMargin,
                end = WhiteNoiseSpacing.CompactScreenMargin,
                top = WhiteNoiseSpacing.Section,
                bottom = WhiteNoiseSpacing.Related,
            ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun InfoGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        content = { Column(content = content) },
    )
}

@Composable
private fun InfoActionRow(
    title: String,
    onClick: () -> Unit,
    @DrawableRes icon: Int? = null,
    subtitle: String? = null,
    destructive: Boolean = false,
    showChevron: Boolean = true,
) {
    val headlineColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    ListItem(
        headlineContent = { Text(title, color = headlineColor) },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = icon?.let {
            {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    tint = headlineColor,
                )
            }
        },
        trailingContent = if (showChevron) {
            {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
fun SharedContentScreen(
    profile: Profile,
    chat: Chat,
    category: SharedContentCategory,
    onBack: () -> Unit,
    onForwardMedia: (ConversationMediaKey, List<String>, String) -> Boolean = { _, _, _ -> false },
    forwardProfiles: List<Profile> = listOf(profile),
    onForwardMediaToProfile: ((ConversationMediaKey, String, List<String>, String) -> Boolean)? = null,
    onGoToMessage: (String) -> Unit = {},
) {
    val content = remember(chat.timeline, profile.id, profile.people, category) { SharedContentProjection.items(chat, profile, category) }
    val media = remember(chat.timeline, profile.id, profile.people) { ConversationMediaProjection.items(chat, profile) }
    var viewerSelection by remember { mutableStateOf<ConversationMediaSelection?>(null) }
    var forwardMediaKey by remember { mutableStateOf<ConversationMediaKey?>(null) }
    val title = stringResource(when (category) {
        SharedContentCategory.Media -> R.string.photos_and_videos
        SharedContentCategory.Links -> R.string.links
        SharedContentCategory.Documents -> R.string.documents
        SharedContentCategory.Voice -> R.string.shared_voice
    })
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing, topBar = { WhiteNoiseTopBar(title, onBack) }) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            SharedContentLibrary(content, category, media, { viewerSelection = it }, onGoToMessage)
        }
    }
    LaunchedEffect(media) { if (media.isEmpty()) viewerSelection = null }
    viewerSelection?.takeIf { media.isNotEmpty() }?.let { selection ->
        ReadOnlyMediaViewer(selection = selection.copy(items = media), onDismiss = { viewerSelection = null },
            onForward = { item -> forwardMediaKey = item.key; viewerSelection = null },
            onGoToMessage = { item -> viewerSelection = null; onGoToMessage(item.message.id) })
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
}

@Composable
fun EditGroupScreen(
    chat: Chat,
    onBack: () -> Unit,
    onSave: (String, String, ProfileAvatar) -> Boolean,
    profile: Profile? = null,
) { GroupEditorScreen(chat, profile, onBack, onSave) }

@Composable
fun AddGroupMembersScreen(
    profile: Profile,
    chat: Chat,
    onBack: () -> Unit,
    onAdd: (List<String>) -> Boolean,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val controller = LocalGroupWork.current
    val owner = GroupOwner(profile.id, chat.id)
    val work = controller?.memberWork?.get(owner)
    val existing = chat.members.map { it.personId }.toSet() + work?.takeIf { it.running && it.action == GroupMemberAction.Invite }?.personIds.orEmpty()
    val canCommit = chat.hasAuthoritativeGroupAdmin(profile.id) && controller?.locked(owner) != true
    var failed by rememberSaveable(profile.id, chat.id) { mutableStateOf(false) }
    val people = remember(profile.people, profile.id, existing, query) {
        profile.people
            .filter { it.id !in existing && it.id != profile.id }
            .filter { it.name.contains(query, ignoreCase = true) }
            .sortedBy(Person::displayName)
    }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { WhiteNoiseTopBar(stringResource(R.string.add_people), onBack) },
        bottomBar = {
            InfoBottomAction(
                label = stringResource(R.string.add_people),
                enabled = selected.isNotEmpty() && canCommit,
                onClick = { if (onAdd(selected.toList())) onBack() else failed = true },
            )
        },
    ) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = WhiteNoiseSpacing.Section),
            ) {
                item { GroupRosterPanel(profile, chat) }
                item { GroupMemberWorkPanel(profile, chat) }
                if (failed) item { StateError(stringResource(R.string.group_members_failed)) }
                item {
                    InfoSearchField(query = query, onQueryChange = { query = it })
                }
                items(people, key = Person::id) { person ->
                    val checked = person.id in selected
                    ListItem(
                        headlineContent = {
                            Text(person.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = {
                            Text(
                                person.shortPublicKey,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingContent = {
                            ProfileAvatar(
                                person.displayName,
                                person.avatar,
                                Modifier.size(48.dp),
                                contentDescription = null,
                            )
                        },
                        trailingContent = {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null,
                                modifier = Modifier.clearAndSetSemantics { },
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (checked) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                Color.Transparent
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = {
                                    selected = if (checked) {
                                        selected - person.id
                                    } else {
                                        selected + person.id
                                    }
                                },
                            ),
                    )
                }
                if (people.isEmpty()) {
                    item {
                        WhiteNoiseEmptyState(
                            title = stringResource(R.string.no_people_to_add),
                            detail = stringResource(
                                if (query.isBlank()) {
                                    R.string.everyone_is_already_here
                                } else {
                                    R.string.no_results_detail
                                },
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRelaysScreen(
    chat: Chat,
    onBack: () -> Unit,
    onAdd: (String) -> Boolean,
    onRemove: (String) -> Boolean,
    onRestore: () -> Boolean,
) {
    var addDialog by remember { mutableStateOf(false) }
    val relayDraft = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState() }
    var relayError by remember { mutableStateOf(false) }
    var removeRelay by remember { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { WhiteNoiseTopBar(stringResource(R.string.chat_relays), onBack) },
        bottomBar = {
            InfoBottomAction(
                label = stringResource(R.string.add_relay),
                onClick = { addDialog = true },
            )
        },
    ) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = WhiteNoiseSpacing.Section),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.chat_relays_explanation),
                        modifier = Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (chat.relayUrls.isEmpty()) {
                    item {
                        WhiteNoiseEmptyState(
                            title = stringResource(R.string.no_chat_relays),
                            detail = stringResource(R.string.no_chat_relays_detail),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    item {
                        InfoGroup {
                            chat.relayUrls.forEach { relay ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            relay,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    trailingContent = {
                                        IconButton(onClick = { removeRelay = relay }) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_delete),
                                                contentDescription = stringResource(
                                                    R.string.remove_named_relay,
                                                    relay,
                                                ),
                                                tint = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                )
                            }
                        }
                    }
                }
                item { InfoSectionLabel(stringResource(R.string.defaults)) }
                item {
                    InfoGroup {
                        InfoActionRow(
                            title = stringResource(R.string.restore_default_relays),
                            subtitle = stringResource(R.string.restore_chat_relays_detail),
                            icon = R.drawable.ic_refresh,
                            showChevron = false,
                            onClick = { onRestore() },
                        )
                    }
                }
            }
        }
    }

    if (addDialog) {
        AlertDialog(
            onDismissRequest = {
                addDialog = false
                relayError = false
            },
            title = { Text(stringResource(R.string.add_relay)) },
            text = {
                WhiteNoiseTextField(
                    state = relayDraft,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.relay_url)) },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    isError = relayError,
                    errorMessage = stringResource(R.string.invalid_or_duplicate_relay),
                    supportingText = if (relayError) {
                        { Text(stringResource(R.string.invalid_or_duplicate_relay)) }
                    } else {
                        null
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (onAdd(relayDraft.text.toString())) {
                            relayDraft.edit { replace(0, length, "") }
                            relayError = false
                            addDialog = false
                        } else {
                            relayError = true
                        }
                    },
                ) {
                    Text(stringResource(R.string.add_relay))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        addDialog = false
                        relayError = false
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    removeRelay?.let { relay ->
        AlertDialog(
            onDismissRequest = { removeRelay = null },
            title = { Text(stringResource(R.string.remove_relay_question)) },
            text = {
                if (chat.relayUrls.size == 1) {
                    Text(stringResource(R.string.remove_final_relay_detail))
                } else {
                    Text(relay)
                }
            },
            confirmButton = {
                TextButton(onClick = { if (onRemove(relay)) removeRelay = null }) {
                    Text(
                        stringResource(R.string.remove_relay),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { removeRelay = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun InfoSearchField(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                vertical = WhiteNoiseSpacing.Related,
            ),
        placeholder = { Text(stringResource(R.string.name_or_npub)) },
        leadingIcon = {
            Icon(painterResource(R.drawable.ic_search), contentDescription = null)
        },
        trailingIcon = if (query.isBlank()) {
            null
        } else {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painterResource(R.drawable.ic_close),
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
private fun InfoBottomAction(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(WhiteNoiseSpacing.PinnedActionInset),
        contentAlignment = Alignment.Center,
    ) {
        WhiteNoiseButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
        ) {
            Text(label)
        }
    }
}

@Composable
private fun StateError(message: String) {
    Text(
        text = message,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

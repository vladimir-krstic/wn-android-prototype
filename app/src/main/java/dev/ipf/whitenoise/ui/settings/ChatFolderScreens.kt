@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.ipf.whitenoise.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn as LazyColumn
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import dev.ipf.whitenoise.ui.components.WhiteNoiseEmptyState
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

@Composable
fun ChatFoldersScreen(profile: Profile, onBack: () -> Unit, onCreate: () -> Unit, onEdit: (String) -> Unit,
    onMove: (String, Int) -> Unit, onDelete: (String) -> Boolean, onRestore: () -> Unit) {
    var deleteId by rememberSaveable(profile.id) { mutableStateOf<String?>(null) }
    SettingsScaffold(stringResource(R.string.chat_folders), onBack,
        topBarActions = { IconButton(onClick = onCreate) { Icon(painterResource(R.drawable.ic_add), stringResource(R.string.chat_new_folder)) } },
    ) {
        LazyColumn(contentPadding = PaddingValues(vertical = WhiteNoiseSpacing.CompactScreenMargin), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            if (profile.chatFolders.isEmpty()) item {
                WhiteNoiseEmptyState(stringResource(R.string.folder_none), stringResource(R.string.folder_none_detail), Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin))
            }
            items(profile.chatFolders, key = { it.id }) { folder ->
                FolderManageRow(folder, ChatFolders.rows(profile.chats, folder).size,
                    canMoveUp = profile.chatFolders.first().id != folder.id, canMoveDown = profile.chatFolders.last().id != folder.id,
                    onEdit = { onEdit(folder.id) }, onMove = { onMove(folder.id, it) }, onDelete = { deleteId = folder.id })
            }
            item {
                SettingsGroup { SettingsAction(stringResource(R.string.folder_restore), onClick = onRestore,
                    enabled = ChatFolders.defaults.any { default -> profile.chatFolders.none { it.id == default.id } }) }
                SettingsExplainer(stringResource(R.string.folder_restore_hint))
            }
        }
    }
    profile.chatFolders.firstOrNull { it.id == deleteId }?.let { folder ->
        AlertDialog(onDismissRequest = { deleteId = null }, title = { Text(stringResource(R.string.folder_delete_title, folder.name)) },
            text = { Text(stringResource(R.string.folder_delete_detail)) },
            confirmButton = { TextButton(onClick = { onDelete(folder.id); deleteId = null }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun FolderManageRow(folder: ChatFolder, count: Int, canMoveUp: Boolean, canMoveDown: Boolean,
    onEdit: () -> Unit, onMove: (Int) -> Unit, onDelete: () -> Unit) {
    var menu by remember(folder.id) { mutableStateOf(false) }
    val choices = buildList {
        add(WhiteNoiseMenuItem(stringResource(R.string.folder_edit), onClick = onEdit))
        if (canMoveUp) add(WhiteNoiseMenuItem(stringResource(R.string.chat_move_up), onClick = { onMove(-1) }))
        if (canMoveDown) add(WhiteNoiseMenuItem(stringResource(R.string.chat_move_down), onClick = { onMove(1) }))
        add(WhiteNoiseMenuItem(stringResource(R.string.delete), onClick = onDelete, destructive = true))
    }
    SettingsGroup {
        ListItem(onClick = onEdit, onLongClick = { menu = true },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            modifier = Modifier.testTag("folder.row.${folder.id}").semantics {
                customActions = choices.map { choice -> CustomAccessibilityAction(choice.label) { choice.onClick(); true } }
            },
            supportingContent = { Text(pluralStringResource(R.plurals.folder_chat_count, count, count) + folder.description.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()) },
            trailingContent = {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.actions_for, folder.name)) }
                    WhiteNoiseDropdownMenu(menu, { menu = false }, items = choices.map { choice -> choice.copy(onClick = { menu = false; choice.onClick() }) })
                }
            },
        ) { Text(folder.name, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
fun ChatFolderEditScreen(profile: Profile, folderId: String?, onBack: () -> Unit, onSave: (ChatFolderDraft) -> Boolean) {
    val initial = remember(profile.id, folderId) { ChatFolderDraft.from(profile.chatFolders.firstOrNull { it.id == folderId }) }
    val name = rememberSaveable(profile.id, folderId, saver = TextFieldState.Saver) { TextFieldState(initial.name) }
    val description = rememberSaveable(profile.id, folderId, saver = TextFieldState.Saver) { TextFieldState(initial.description) }
    val keyword = rememberSaveable(profile.id, folderId, saver = TextFieldState.Saver) { TextFieldState(initial.rule.keyword) }
    var chatIds by rememberSaveable(profile.id, folderId) { mutableStateOf(initial.chatIds.toList()) }
    var peopleIds by rememberSaveable(profile.id, folderId) { mutableStateOf(initial.rule.personIds.toList()) }
    var unread by rememberSaveable(profile.id, folderId) { mutableStateOf(initial.rule.unreadOnly) }
    var groups by rememberSaveable(profile.id, folderId) { mutableStateOf(initial.rule.groupsOnly) }
    var archived by rememberSaveable(profile.id, folderId) { mutableStateOf(initial.rule.archivedOnly) }
    var muted by rememberSaveable(profile.id, folderId) { mutableStateOf(initial.rule.includeMuted) }
    var picker by rememberSaveable(profile.id, folderId) { mutableStateOf<String?>(null) }
    var discard by rememberSaveable(profile.id, folderId) { mutableStateOf(false) }
    var failed by rememberSaveable(profile.id, folderId) { mutableStateOf(false) }
    val draft = ChatFolderDraft(name.text.toString(), description.text.toString(), chatIds.toSet(), ChatFolderRule(peopleIds.toSet(), keyword.text.toString(), unread, groups, archived, muted))
    val missing = folderId != null && profile.chatFolders.none { it.id == folderId }
    val dirty = draft != initial
    val preview = ChatFolders.preview(profile, draft)
    fun back() { if (dirty) discard = true else onBack() }
    BackHandler(onBack = ::back)
    SettingsScaffold(stringResource(if (folderId == null) R.string.chat_new_folder else R.string.folder_edit), ::back,
        modifier = Modifier.imePadding(),
        topBarActions = { TextButton(enabled = name.text.isNotBlank() && !missing, onClick = { if (onSave(draft)) onBack() else failed = true }) { Text(stringResource(R.string.save)) } },
    ) {
        LazyColumn(modifier = Modifier.testTag("folder.editorList"), contentPadding = PaddingValues(bottom = WhiteNoiseSpacing.Section)) {
            item {
                Column(Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField)) {
                    WhiteNoiseTextField(name, Modifier.fillMaxWidth().testTag("folder.name"), label = { Text(stringResource(R.string.chat_folder_name)) }, lineLimits = TextFieldLineLimits.SingleLine)
                    WhiteNoiseTextField(description, Modifier.fillMaxWidth().testTag("folder.description"), label = { Text(stringResource(R.string.folder_description)) }, lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 2, maxHeightInLines = 4))
                    if (missing || failed) Text(stringResource(if (missing) R.string.folder_unavailable else R.string.folder_save_failed), color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
            item {
                SettingsGroup { SettingsLink(stringResource(R.string.folder_included_chats), value = chatIds.size.toString(), onClick = { picker = "chats" }) }
                SettingsExplainer(stringResource(R.string.folder_manual_hint))
                SettingsSection(stringResource(R.string.folder_rules))
                SettingsGroup { SettingsLink(stringResource(R.string.folder_people), value = peopleIds.size.toString(), onClick = { picker = "people" }) }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin)) {
                    WhiteNoiseTextField(keyword, Modifier.fillMaxWidth().testTag("folder.keyword"), label = { Text(stringResource(R.string.folder_keyword)) },
                        lineLimits = TextFieldLineLimits.SingleLine, supportingText = { Text(stringResource(R.string.folder_keyword_hint)) })
                }
            }
            item {
                SettingsGroup {
                    SettingsSwitch(stringResource(R.string.folder_unread), unread, { unread = it }); SettingsDivider()
                    SettingsSwitch(stringResource(R.string.folder_groups), groups, { groups = it }); SettingsDivider()
                    SettingsSwitch(stringResource(R.string.folder_archived), archived, { archived = it }); SettingsDivider()
                    SettingsSwitch(stringResource(R.string.folder_muted), muted, { muted = it })
                }
                SettingsExplainer(stringResource(R.string.folder_rule_hint))
                SettingsSection(stringResource(R.string.folder_preview))
                SettingsGroup { SettingsLink(stringResource(R.string.folder_preview), subtitle = pluralStringResource(R.plurals.folder_chat_count, preview.size, preview.size), onClick = { picker = "preview" }) }
            }
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text(stringResource(R.string.folder_discard_title)) },
        text = { Text(stringResource(R.string.folder_discard_detail)) },
        confirmButton = { TextButton(onClick = onBack) { Text(stringResource(R.string.folder_discard)) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(stringResource(R.string.folder_keep_editing)) } })
    picker?.let { mode ->
        val choices = when (mode) {
            "people" -> profile.people.map { FolderChoice(it.id, it.displayName, it.avatar) }
            "preview" -> preview.map { FolderChoice(it.id, it.title, it.avatar) }
            else -> profile.chats.sortedWith(ChatOrganization.order).map { FolderChoice(it.id, it.title, it.avatar) }
        }
        FolderChoiceDialog(
            title = stringResource(when (mode) { "people" -> R.string.folder_people; "preview" -> R.string.folder_preview; else -> R.string.folder_included_chats }),
            choices = choices, selected = if (mode == "people") peopleIds else chatIds, preview = mode == "preview",
            onToggle = { id ->
                if (mode == "people") peopleIds = if (id in peopleIds) peopleIds - id else peopleIds + id
                else chatIds = if (id in chatIds) chatIds - id else chatIds + id
            }, onDismiss = { picker = null },
        )
    }
}

private data class FolderChoice(val id: String, val title: String, val avatar: ProfileAvatar)

@Composable
private fun FolderChoiceDialog(title: String, choices: List<FolderChoice>, selected: List<String>, preview: Boolean,
    onToggle: (String) -> Unit, onDismiss: () -> Unit) {
    val query = rememberSaveable(title, saver = TextFieldState.Saver) { TextFieldState() }
    val visible = choices.filter { it.title.contains(query.text.toString().trim(), ignoreCase = true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                WhiteNoiseTextField(query, Modifier.fillMaxWidth().testTag("folder.pickerSearch"), label = { Text(stringResource(R.string.folder_search)) }, lineLimits = TextFieldLineLimits.SingleLine)
                if (visible.isEmpty()) Text(stringResource(R.string.no_results), Modifier.padding(vertical = WhiteNoiseSpacing.FormField))
                visible.forEach { choice ->
                    Row(Modifier.fillMaxWidth().then(if (preview) Modifier else Modifier.toggleable(choice.id in selected, role = Role.Checkbox, onValueChange = { onToggle(choice.id) }))
                        .testTag("folder.choice.${choice.id}").padding(vertical = WhiteNoiseSpacing.Related), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                        if (!preview) Checkbox(choice.id in selected, null)
                        ProfileAvatar(choice.title, choice.avatar, Modifier.size(40.dp), contentDescription = null)
                        Text(choice.title, Modifier.weight(1f))
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

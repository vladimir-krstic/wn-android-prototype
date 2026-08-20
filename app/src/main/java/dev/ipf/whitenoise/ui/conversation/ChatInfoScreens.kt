package dev.ipf.whitenoise.ui.conversation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatMembership
import dev.ipf.whitenoise.model.DisappearingDuration
import dev.ipf.whitenoise.model.GroupRole
import dev.ipf.whitenoise.model.MuteDuration
import dev.ipf.whitenoise.model.Person
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.SharedContentCategory
import dev.ipf.whitenoise.model.SharedContentProjection
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseTopBar
import dev.ipf.whitenoise.ui.onboarding.AvatarImageProcessor
import kotlinx.coroutines.Job
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
    modifier: Modifier = Modifier,
) {
    var muteSheet by remember { mutableStateOf(false) }
    var disappearingSheet by remember { mutableStateOf(false) }
    var leaveConfirmation by remember { mutableStateOf(false) }
    var onlyAdminWarning by remember { mutableStateOf(false) }
    val directPersonId = (chat.kind as? dev.ipf.whitenoise.model.ChatKind.Direct)?.personId
    val directPerson = profile.people.firstOrNull { it.id == directPersonId }
    val activeRole = chat.members.firstOrNull { it.personId == profile.id }?.role
    val canAdmin = chat.isGroup && activeRole == GroupRole.Admin && chat.membership == ChatMembership.Active
    val counts = remember(chat.timeline) { SharedContentProjection.counts(chat, profile) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            WhiteNoiseTopBar(
                stringResource(if (chat.isGroup) R.string.group_info else R.string.chat_info),
                onBack,
            )
        },
    ) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProfileAvatar(chat.title, chat.avatar, Modifier.size(112.dp))
                        Text(chat.title, style = MaterialTheme.typography.headlineMedium)
                        if (chat.isGroup) {
                            Text(pluralStringResource(R.plurals.group_member_count, chat.members.size, chat.members.size))
                            if (chat.description.isNotBlank()) Text(chat.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            directPerson?.nostrAddress?.takeIf(String::isNotBlank)?.let { Text(it) }
                            directPerson?.let { Text(it.shortPublicKey, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        if (directPerson != null) QuickInfoAction(stringResource(R.string.about)) { onAbout(directPerson.id) }
                        QuickInfoAction(stringResource(if (chat.muteDuration == null) R.string.mute else R.string.unmute)) {
                            if (chat.muteDuration == null) muteSheet = true else onMute(null)
                        }
                        QuickInfoAction(stringResource(R.string.disappearing)) { disappearingSheet = true }
                        QuickInfoAction(stringResource(R.string.search)) { onSearch() }
                    }
                }
                item { SectionHeader(stringResource(R.string.shared_in_chat)) }
                items(SharedContentCategory.entries) { category ->
                    val label = when (category) {
                        SharedContentCategory.Media -> stringResource(R.string.photos_and_videos)
                        SharedContentCategory.Links -> stringResource(R.string.links)
                        SharedContentCategory.Documents -> stringResource(R.string.documents)
                    }
                    ListItem(
                        headlineContent = { Text(label) },
                        supportingContent = { Text(counts.getValue(category).toString()) },
                        trailingContent = { Text("›") },
                        modifier = Modifier.clickable { onSharedContent(category) },
                    )
                }
                if (chat.isGroup) {
                    item { SectionHeader(stringResource(R.string.members)) }
                    items(chat.members, key = { it.personId }) { member ->
                        val person = if (member.personId == profile.id) null else profile.people.firstOrNull { it.id == member.personId }
                        val name = if (member.personId == profile.id) stringResource(R.string.you) else person?.name ?: member.personId
                        ListItem(
                            headlineContent = { Text(name) },
                            supportingContent = { Text(stringResource(if (member.role == GroupRole.Admin) R.string.admin else R.string.member)) },
                            leadingContent = { ProfileAvatar(name, person?.avatar ?: profile.avatar, Modifier.size(44.dp), contentDescription = null) },
                            trailingContent = { if (person != null) Text("›") },
                            modifier = Modifier.then(if (person != null) Modifier.clickable { onMember(person.id) } else Modifier),
                        )
                    }
                    if (canAdmin) {
                        item { ListItem(headlineContent = { Text(stringResource(R.string.edit_group)) }, modifier = Modifier.clickable(onClick = onEditGroup)) }
                        item { ListItem(headlineContent = { Text(stringResource(R.string.add_people)) }, modifier = Modifier.clickable(onClick = onAddPeople)) }
                    }
                }
                item { SectionHeader(stringResource(if (chat.isGroup) R.string.advanced else R.string.chat_actions)) }
                item { ListItem(headlineContent = { Text(stringResource(R.string.relays)) }, trailingContent = { Text("›") }, modifier = Modifier.clickable(onClick = onRelays)) }
                item { ListItem(headlineContent = { Text(stringResource(if (chat.isArchived) R.string.unarchive else R.string.archive)) }, modifier = Modifier.clickable(onClick = onArchive)) }
                if (chat.membership == ChatMembership.Active) {
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(if (chat.isGroup) R.string.leave_group else R.string.leave_chat), color = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.clickable { leaveConfirmation = true },
                        )
                    }
                }
            }
        }
    }
    if (muteSheet) {
        ModalBottomSheet(onDismissRequest = { muteSheet = false }) {
            MuteDuration.entries.forEach { duration ->
                ListItem(
                    headlineContent = { Text(duration.label) },
                    modifier = Modifier.clickable { onMute(duration); muteSheet = false },
                )
            }
        }
    }
    if (disappearingSheet) {
        ModalBottomSheet(onDismissRequest = { disappearingSheet = false }) {
            DisappearingDuration.entries.forEach { duration ->
                ListItem(
                    headlineContent = { Text(duration.label) },
                    trailingContent = { if (duration == chat.disappearingDuration) Text("✓") },
                    modifier = Modifier.clickable { onDisappearing(duration); disappearingSheet = false },
                )
            }
        }
    }
    if (leaveConfirmation) {
        AlertDialog(
            onDismissRequest = { leaveConfirmation = false },
            title = { Text(stringResource(if (chat.isGroup) R.string.leave_group_question else R.string.leave_chat_question)) },
            text = { Text(stringResource(R.string.leave_history_detail)) },
            confirmButton = {
                TextButton(onClick = {
                    leaveConfirmation = false
                    if (!onLeave()) onlyAdminWarning = true
                }) { Text(stringResource(if (chat.isGroup) R.string.leave_group else R.string.leave_chat), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { leaveConfirmation = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (onlyAdminWarning) {
        AlertDialog(
            onDismissRequest = { onlyAdminWarning = false },
            title = { Text(stringResource(R.string.leave_group)) },
            text = { Text(stringResource(R.string.only_admin_detail)) },
            confirmButton = { TextButton(onClick = { onlyAdminWarning = false }) { Text(stringResource(R.string.done)) } },
        )
    }
}

@Composable
private fun QuickInfoAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.size(84.dp)) { Text(label, maxLines = 2) }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        label,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun SharedContentScreen(
    profile: Profile,
    chat: Chat,
    category: SharedContentCategory,
    onBack: () -> Unit,
) {
    val content = remember(chat.timeline, category) { SharedContentProjection.items(chat, profile, category) }
    var viewerOpen by remember { mutableStateOf(false) }
    val title = when (category) {
        SharedContentCategory.Media -> stringResource(R.string.photos_and_videos)
        SharedContentCategory.Links -> stringResource(R.string.links)
        SharedContentCategory.Documents -> stringResource(R.string.documents)
    }
    Scaffold(topBar = { WhiteNoiseTopBar(title, onBack) }) { padding ->
        if (content.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_shared_content), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (category == SharedContentCategory.Media) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(content, key = { it.id }) { item ->
                    Box(
                        Modifier.aspectRatio(1f).clickable { viewerOpen = true },
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        item.attachment.images.firstOrNull()?.let { ComposerImage(it, Modifier.fillMaxSize()) }
                        Text(item.senderName, Modifier.fillMaxWidth().padding(4.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(content, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { TimelineAttachmentContent(listOf(item.attachment), false, onOpenMedia = {}) },
                        supportingContent = { Text("${item.senderName} · ${item.sentLabel}") },
                    )
                }
            }
        }
    }
    if (viewerOpen) {
        ReadOnlyMediaViewer(content.map { it.attachment }, onDismiss = { viewerOpen = false })
    }
}

@Composable
fun EditGroupScreen(
    chat: Chat,
    onBack: () -> Unit,
    onSave: (String, String, ProfileAvatar) -> Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable(chat.id) { mutableStateOf(chat.title) }
    var description by rememberSaveable(chat.id) { mutableStateOf(chat.description) }
    var avatar by remember(chat.avatar) { mutableStateOf(chat.avatar) }
    var job by remember { mutableStateOf<Job?>(null) }
    var error by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            job?.cancel()
            job = scope.launch {
                val bytes = runCatching { AvatarImageProcessor.prepare(context.contentResolver, uri) }.getOrNull()
                if (bytes == null) error = true else avatar = ProfileAvatar.DeviceImage(bytes)
            }
        }
    }
    DisposableEffect(Unit) { onDispose { job?.cancel() } }
    Scaffold(
        topBar = { WhiteNoiseTopBar(stringResource(R.string.edit_group), onBack) },
        bottomBar = {
            Button(
                onClick = { if (onSave(name, description, avatar)) onBack() else error = true },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            ) { Text(stringResource(R.string.save)) }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileAvatar(name, avatar, Modifier.size(112.dp))
            Row {
                TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text(stringResource(if (avatar == ProfileAvatar.Monogram) R.string.add_photo else R.string.change_photo))
                }
                if (avatar != ProfileAvatar.Monogram) TextButton(onClick = { avatar = ProfileAvatar.Monogram }) {
                    Text(stringResource(R.string.remove_photo), color = MaterialTheme.colorScheme.error)
                }
            }
            TextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.group_name)) }, singleLine = true)
            TextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.group_description)) }, minLines = 3)
            if (error) Text(stringResource(R.string.group_name_required), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun AddGroupMembersScreen(
    profile: Profile,
    chat: Chat,
    onBack: () -> Unit,
    onAdd: (List<String>) -> Boolean,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val existing = chat.members.map { it.personId }.toSet()
    val people = profile.people.filter { it.id !in existing && it.id != profile.id && it.name.contains(query, true) }
    Scaffold(
        topBar = { WhiteNoiseTopBar(stringResource(R.string.add_people), onBack) },
        bottomBar = {
            Button(
                onClick = { if (onAdd(selected.toList())) onBack() },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            ) { Text(stringResource(R.string.add_people)) }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { TextField(query, { query = it }, Modifier.fillMaxWidth().padding(16.dp), placeholder = { Text(stringResource(R.string.name_or_npub)) }) }
            items(people, key = Person::id) { person ->
                val checked = person.id in selected
                ListItem(
                    headlineContent = { Text(person.name) },
                    supportingContent = { Text(person.shortPublicKey, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = { ProfileAvatar(person.name, person.avatar, Modifier.size(44.dp), contentDescription = null) },
                    trailingContent = { Text(if (checked) "✓" else "○") },
                    modifier = Modifier.clickable { selected = if (checked) selected - person.id else selected + person.id },
                )
            }
        }
    }
}

@Composable
fun ChatRelaysScreen(
    chat: Chat,
    onBack: () -> Unit,
    onAdd: (String) -> Boolean,
    onRemove: (String) -> Boolean,
    onRestore: () -> Boolean,
) {
    var addDialog by remember { mutableStateOf(false) }
    var relayDraft by rememberSaveable { mutableStateOf("") }
    var relayError by remember { mutableStateOf(false) }
    var removeRelay by remember { mutableStateOf<String?>(null) }
    Scaffold(
        topBar = { WhiteNoiseTopBar(stringResource(R.string.chat_relays), onBack) },
        bottomBar = {
            Button(
                onClick = { addDialog = true },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            ) { Text(stringResource(R.string.add_relay)) }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { Text(stringResource(R.string.chat_relays_explanation), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(chat.relayUrls, key = { it }) { relay ->
                ListItem(
                    headlineContent = { Text(relay) },
                    trailingContent = { TextButton(onClick = { removeRelay = relay }) { Text(stringResource(R.string.delete)) } },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.restore_default_relays)) },
                    modifier = Modifier.clickable { onRestore() },
                )
            }
        }
    }
    if (addDialog) {
        AlertDialog(
            onDismissRequest = { addDialog = false },
            title = { Text(stringResource(R.string.add_relay)) },
            text = {
                Column {
                    TextField(relayDraft, { relayDraft = it; relayError = false }, label = { Text(stringResource(R.string.relay_url)) })
                    if (relayError) Text(stringResource(R.string.invalid_or_duplicate_relay), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (onAdd(relayDraft)) { relayDraft = ""; addDialog = false } else relayError = true
                }) { Text(stringResource(R.string.add_relay)) }
            },
            dismissButton = { TextButton(onClick = { addDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    removeRelay?.let { relay ->
        AlertDialog(
            onDismissRequest = { removeRelay = null },
            title = { Text(stringResource(R.string.remove_relay_question)) },
            text = { if (chat.relayUrls.size == 1) Text(stringResource(R.string.remove_final_relay_detail)) },
            confirmButton = {
                TextButton(onClick = { if (onRemove(relay)) removeRelay = null }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { removeRelay = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

package dev.ipf.whitenoise.ui.chats

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.ChatMembership
import dev.ipf.whitenoise.model.GroupRole
import dev.ipf.whitenoise.model.Person
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseTopBar
import dev.ipf.whitenoise.ui.onboarding.AvatarImageProcessor
import dev.ipf.whitenoise.ui.onboarding.AvatarWebImagePicker
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.Normalizer

@Composable
fun NewChatScreen(
    profile: Profile,
    onBack: () -> Unit,
    onNewGroup: () -> Unit,
    onPerson: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val people = remember(profile.people, profile.id, query) {
        profile.people
            .filter { it.id != profile.id && it.id != "white-noise-support" }
            .filter { it.matches(query) }
            .sortedBy(Person::name)
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { WhiteNoiseTopBar(stringResource(R.string.new_message), onBack) },
    ) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        label = { Text(stringResource(R.string.name_or_npub)) },
                        singleLine = true,
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.new_group), fontWeight = FontWeight.SemiBold) },
                        leadingContent = { MonogramBadge("＋") },
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onNewGroup),
                    )
                }
                item {
                    Text(
                        stringResource(R.string.people),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(people, key = Person::id) { person ->
                    PersonRow(person = person, onClick = { onPerson(person.id) })
                }
                if (people.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.no_results),
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
) {
    var showRelayError by remember { mutableStateOf(false) }
    var showBlockConfirmation by remember { mutableStateOf(false) }
    var showRoleConfirmation by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }
    val groups = remember(profile.chats, person.id) {
        profile.chats.filter { chat ->
            chat.isGroup &&
                chat.membership == ChatMembership.Active &&
                chat.members.any { it.personId == profile.id } &&
                chat.members.any { it.personId == person.id }
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            WhiteNoiseTopBar(
                groupRole?.let {
                    "${stringResource(R.string.person_profile)} (${stringResource(if (it == GroupRole.Admin) R.string.admin else R.string.member)})"
                } ?: stringResource(R.string.person_profile),
                onBack,
            )
        },
        bottomBar = {
            if (showMessageAction) {
                Button(
                    onClick = { if (!onMessage()) showRelayError = true },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                ) { Text(stringResource(R.string.message)) }
            }
        },
    ) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProfileAvatar(person.name, person.avatar, Modifier.size(112.dp))
                Text(person.name, style = MaterialTheme.typography.headlineMedium)
                Text(person.shortPublicKey, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (person.nostrAddress.isNotBlank()) {
                    Text(
                        if (person.isNostrAddressVerified) "${person.nostrAddress} · ${stringResource(R.string.verified)}" else person.nostrAddress,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (person.about.isNotBlank()) Text(person.about, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onToggleFollow) {
                        Text(stringResource(if (person.isFollowing) R.string.unfollow else R.string.follow))
                    }
                    TextButton(onClick = {
                        if (person.isBlocked) onToggleBlock() else showBlockConfirmation = true
                    }) {
                        Text(
                            stringResource(if (person.isBlocked) R.string.unblock else R.string.block),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Text(
                    stringResource(R.string.groups_in_common),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (groups.isEmpty()) stringResource(R.string.no_groups_in_common) else groups.joinToString { it.title },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (canManageGroup && groupRole != null) {
                    Text(
                        stringResource(R.string.group_actions),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = { showRoleConfirmation = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(if (groupRole == GroupRole.Admin) R.string.remove_admin else R.string.make_admin))
                    }
                    TextButton(onClick = { showRemoveConfirmation = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.remove_from_group), color = MaterialTheme.colorScheme.error)
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
                TextButton(onClick = { showRelayError = false }) { Text(stringResource(R.string.done)) }
            },
        )
    }
    if (showBlockConfirmation) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmation = false },
            title = { Text(stringResource(R.string.block)) },
            text = { Text(stringResource(R.string.blocked_chat_detail)) },
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
                        person.name,
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
            title = { Text(stringResource(R.string.remove_member_question, person.name)) },
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
fun NewGroupScreen(
    profile: Profile,
    onBack: () -> Unit,
    onContinue: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val selectablePeople = remember(profile.people, profile.id, query) {
        profile.people
            .filter { it.id != profile.id && it.id != "white-noise-support" }
            .filter { it.matches(query) }
            .sortedBy(Person::name)
    }
    fun toggle(personId: String) {
        selectedIds = if (personId in selectedIds) selectedIds - personId else selectedIds + personId
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { WhiteNoiseTopBar(stringResource(R.string.new_group), onBack) },
        bottomBar = {
            Button(
                onClick = { onContinue(selectedIds) },
                enabled = selectedIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp),
            ) { Text(stringResource(R.string.continue_action)) }
        },
    ) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        label = { Text(stringResource(R.string.name_or_npub)) },
                        singleLine = true,
                    )
                }
                if (selectedIds.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.selected_people),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        ) {
                            items(selectedIds, key = { it }) { id ->
                                val person = profile.people.first { it.id == id }
                                val removeDescription = stringResource(R.string.remove_person, person.name)
                                FilterChip(
                                    selected = true,
                                    onClick = { toggle(id) },
                                    label = { Text(person.name) },
                                    trailingIcon = {
                                        Text(
                                            "×",
                                            modifier = Modifier.semantics {
                                                contentDescription = removeDescription
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.people),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(selectablePeople, key = Person::id) { person ->
                    PersonRow(
                        person = person,
                        onClick = { toggle(person.id) },
                        trailing = if (person.id in selectedIds) "✓" else null,
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
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var groupName by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var avatar by remember { mutableStateOf<ProfileAvatar>(ProfileAvatar.Monogram) }
    var webChoiceId by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var webPickerOpen by remember { mutableStateOf(false) }
    var processingJob by remember { mutableStateOf<Job?>(null) }
    var photoError by remember { mutableStateOf(false) }
    var relayError by remember { mutableStateOf(false) }
    val selectedPeople = selectedPersonIds.mapNotNull { id -> profile.people.firstOrNull { it.id == id } }

    fun prepare(uri: android.net.Uri) {
        processingJob?.cancel()
        processingJob = coroutineScope.launch {
            val bytes = runCatching { AvatarImageProcessor.prepare(context.contentResolver, uri) }.getOrNull()
            if (bytes == null) {
                photoError = true
            } else {
                avatar = ProfileAvatar.DeviceImage(bytes)
                webChoiceId = null
                photoError = false
            }
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::prepare) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(::prepare) }
    DisposableEffect(Unit) { onDispose { processingJob?.cancel() } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { WhiteNoiseTopBar(stringResource(R.string.set_up_group), onBack) },
        bottomBar = {
            Button(
                onClick = { if (!onCreate(groupName, description, avatar)) relayError = true },
                enabled = groupName.isNotBlank() && selectedPeople.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp),
            ) { Text(stringResource(R.string.create_group)) }
        },
    ) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProfileAvatar(groupName.ifBlank { stringResource(R.string.new_group) }, avatar, Modifier.size(112.dp))
                TextButton(onClick = { menuOpen = true }) {
                    Text(stringResource(if (avatar == ProfileAvatar.Monogram) R.string.add_photo else R.string.change_photo))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.choose_photos)) },
                        onClick = {
                            menuOpen = false
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.choose_files)) },
                        onClick = {
                            menuOpen = false
                            filePicker.launch(arrayOf("image/*"))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.find_web_image)) },
                        onClick = {
                            menuOpen = false
                            webPickerOpen = true
                        },
                    )
                    if (avatar != ProfileAvatar.Monogram) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.remove_photo), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                avatar = ProfileAvatar.Monogram
                                webChoiceId = null
                                menuOpen = false
                            },
                        )
                    }
                }
                if (photoError) Text(stringResource(R.string.photo_error), color = MaterialTheme.colorScheme.error)
                TextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.group_name)) },
                    singleLine = true,
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.group_description)) },
                    minLines = 3,
                    maxLines = 6,
                )
                Text(
                    stringResource(R.string.members),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                )
                selectedPeople.forEach { person ->
                    PersonRow(person, onClick = {}, modifier = Modifier.fillMaxWidth())
                }
                Text(
                    pluralStringResource(
                        R.plurals.relay_count,
                        profile.chatRelayUrls.size,
                        profile.chatRelayUrls.size,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (webPickerOpen) {
        AvatarWebImagePicker(
            currentChoiceId = webChoiceId,
            onDismiss = { webPickerOpen = false },
            onUseImage = { choice ->
                avatar = ProfileAvatar.WebImage(choice.asset, choice.id)
                webChoiceId = choice.id
                webPickerOpen = false
            },
        )
    }
    if (relayError) {
        AlertDialog(
            onDismissRequest = { relayError = false },
            title = { Text(stringResource(R.string.chat_relays_required_title)) },
            text = { Text(stringResource(R.string.chat_relays_required_detail)) },
            confirmButton = { TextButton(onClick = { relayError = false }) { Text(stringResource(R.string.done)) } },
        )
    }
}

@Composable
private fun PersonRow(
    person: Person,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    ListItem(
        headlineContent = { Text(person.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(person.shortPublicKey, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { ProfileAvatar(person.name, person.avatar, Modifier.size(48.dp), contentDescription = null) },
        trailingContent = { trailing?.let { Text(it, fontWeight = FontWeight.Bold) } },
        modifier = modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun MonogramBadge(text: String) {
    androidx.compose.material3.Surface(
        modifier = Modifier.size(48.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text, style = MaterialTheme.typography.titleLarge)
        }
    }
}

private fun Person.matches(query: String): Boolean {
    val needle = query.searchNormalized()
    return needle.isEmpty() || name.searchNormalized().contains(needle) || publicKey.searchNormalized().contains(needle)
}

private fun String.searchNormalized(): String = Normalizer
    .normalize(trim().lowercase(), Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")

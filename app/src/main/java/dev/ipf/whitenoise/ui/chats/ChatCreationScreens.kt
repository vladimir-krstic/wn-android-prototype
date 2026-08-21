package dev.ipf.whitenoise.ui.chats

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
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
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseEmptyState
import dev.ipf.whitenoise.ui.components.WhiteNoiseTopBar
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.onboarding.AvatarImageProcessor
import dev.ipf.whitenoise.ui.onboarding.AvatarWebImagePicker
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = WhiteNoiseSpacing.Section),
            ) {
                item {
                    PeopleSearchField(query = query, onQueryChange = { query = it })
                }
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                                vertical = WhiteNoiseSpacing.Related,
                            ),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    stringResource(R.string.new_group),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_group_add),
                                    contentDescription = null,
                                )
                            },
                            trailingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_chevron_right),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onNewGroup),
                        )
                    }
                }
                item {
                    CreationSectionLabel(stringResource(R.string.people))
                }
                items(people, key = Person::id) { person ->
                    PersonRow(person = person, onClick = { onPerson(person.id) })
                }
                if (people.isEmpty()) {
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
                stringResource(R.string.person_profile),
                onBack,
            )
        },
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
                )
            }
        },
    ) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                        vertical = WhiteNoiseSpacing.Section,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                ProfileAvatar(person.name, person.avatar, Modifier.size(120.dp))
                Text(person.name, style = MaterialTheme.typography.headlineMedium)
                groupRole?.let { role ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Text(
                            text = stringResource(
                                if (role == GroupRole.Admin) R.string.admin else R.string.member,
                            ),
                            modifier = Modifier.padding(
                                horizontal = WhiteNoiseSpacing.FormField,
                                vertical = 4.dp,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                if (person.nostrAddress.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            person.nostrAddress,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (person.isNostrAddressVerified) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = stringResource(R.string.verified),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text(
                        text = person.shortPublicKey,
                        modifier = Modifier.padding(
                            horizontal = WhiteNoiseSpacing.FormField,
                            vertical = WhiteNoiseSpacing.Related,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                    )
                }
                if (person.about.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = WhiteNoiseSpacing.Related),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(
                            text = person.about,
                            modifier = Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        )
                    }
                }
                if (groupRole != null) {
                    CreationSectionLabel(stringResource(R.string.profile_actions))
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = if (groupRole == null) WhiteNoiseSpacing.FormField else 0.dp,
                        ),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column {
                        ProfileActionRow(
                            title = stringResource(
                                if (person.isFollowing) R.string.unfollow else R.string.follow,
                            ),
                            onClick = onToggleFollow,
                        )
                        ProfileActionRow(
                            title = stringResource(
                                if (person.isBlocked) R.string.unblock else R.string.block,
                            ),
                            destructive = !person.isBlocked,
                            onClick = {
                                if (person.isBlocked) onToggleBlock() else showBlockConfirmation = true
                            },
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.groups_in_common)) },
                            supportingContent = {
                                Text(
                                    if (groups.isEmpty()) {
                                        stringResource(R.string.no_groups_in_common)
                                    } else {
                                        groups.joinToString { it.title }
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
                if (canManageGroup && groupRole != null) {
                    CreationSectionLabel(stringResource(R.string.group_actions))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column {
                            ProfileActionRow(
                                title = stringResource(
                                    if (groupRole == GroupRole.Admin) {
                                        R.string.remove_admin
                                    } else {
                                        R.string.make_admin
                                    },
                                ),
                                onClick = { showRoleConfirmation = true },
                            )
                            ProfileActionRow(
                                title = stringResource(R.string.remove_from_group),
                                destructive = true,
                                onClick = { showRemoveConfirmation = true },
                            )
                        }
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
            title = { Text(stringResource(R.string.block_person_question, person.name)) },
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
            CreationBottomAction(
                onClick = { onContinue(selectedIds) },
                enabled = selectedIds.isNotEmpty(),
                label = stringResource(R.string.continue_action),
            )
        },
    ) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = WhiteNoiseSpacing.Section),
            ) {
                item {
                    PeopleSearchField(query = query, onQueryChange = { query = it })
                }
                if (selectedIds.isNotEmpty()) {
                    item {
                        CreationSectionLabel(stringResource(R.string.selected_people))
                    }
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                            contentPadding = PaddingValues(
                                horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                            ),
                        ) {
                            items(selectedIds, key = { it }) { id ->
                                val person = profile.people.first { it.id == id }
                                val removeDescription = stringResource(R.string.remove_person, person.name)
                                InputChip(
                                    selected = true,
                                    onClick = { toggle(id) },
                                    label = { Text(person.name) },
                                    avatar = {
                                        ProfileAvatar(
                                            person.name,
                                            person.avatar,
                                            Modifier.size(24.dp),
                                            contentDescription = null,
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_close),
                                            contentDescription = null,
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
                    CreationSectionLabel(stringResource(R.string.people))
                }
                items(selectablePeople, key = Person::id) { person ->
                    PersonRow(
                        person = person,
                        onClick = { toggle(person.id) },
                        selected = person.id in selectedIds,
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
}

@Composable
fun GroupSetupScreen(
    profile: Profile,
    selectedPersonIds: List<String>,
    onBack: () -> Unit,
    onCreate: (String, String, ProfileAvatar) -> Boolean,
    modifier: Modifier = Modifier,
    onOpenRelays: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val groupName = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState() }
    val description = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState() }
    var avatar by remember { mutableStateOf<ProfileAvatar>(ProfileAvatar.Monogram) }
    var webChoiceId by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var webPickerOpen by remember { mutableStateOf(false) }
    var processingJob by remember { mutableStateOf<Job?>(null) }
    var isPreparingPhoto by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf(false) }
    var relayError by remember { mutableStateOf(false) }
    val selectedPeople = selectedPersonIds.mapNotNull { id -> profile.people.firstOrNull { it.id == id } }

    fun prepare(uri: android.net.Uri) {
        processingJob?.cancel()
        processingJob = coroutineScope.launch {
            isPreparingPhoto = true
            photoError = false
            val bytes = runCatching { AvatarImageProcessor.prepare(context.contentResolver, uri) }.getOrNull()
            if (bytes == null) {
                photoError = true
            } else {
                avatar = ProfileAvatar.DeviceImage(bytes)
                webChoiceId = null
                photoError = false
            }
            isPreparingPhoto = false
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
            CreationBottomAction(
                onClick = {
                    if (
                        !onCreate(
                            groupName.text.toString(),
                            description.text.toString(),
                            avatar,
                        )
                    ) {
                        relayError = true
                    }
                },
                enabled = groupName.text.isNotBlank() &&
                    selectedPeople.isNotEmpty() &&
                    !isPreparingPhoto,
                label = stringResource(R.string.create_group),
            )
        },
    ) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                        vertical = WhiteNoiseSpacing.Section,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Section),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) {
                    GroupAvatarPreview(
                        name = groupName.text.toString(),
                        avatar = avatar,
                        modifier = Modifier.size(120.dp),
                    )
                    Box {
                        FilledTonalButton(
                            onClick = { menuOpen = true },
                            enabled = !isPreparingPhoto,
                        ) {
                            Text(
                                stringResource(
                                    if (avatar == ProfileAvatar.Monogram) {
                                        R.string.add_photo
                                    } else {
                                        R.string.change_photo
                                    },
                                ),
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.choose_photos)) },
                                onClick = {
                                    menuOpen = false
                                    photoPicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                                        ),
                                    )
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
                                    text = {
                                        Text(
                                            stringResource(R.string.remove_photo),
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        processingJob?.cancel()
                                        isPreparingPhoto = false
                                        avatar = ProfileAvatar.Monogram
                                        webChoiceId = null
                                        photoError = false
                                        menuOpen = false
                                    },
                                )
                            }
                        }
                    }
                    if (isPreparingPhoto) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(stringResource(R.string.preparing_photo))
                        }
                    }
                    if (photoError) {
                        Text(
                            text = stringResource(R.string.photo_error),
                            modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                            },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
                ) {
                    WhiteNoiseTextField(
                        state = groupName,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.group_name)) },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )
                    WhiteNoiseTextField(
                        state = description,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.group_description)) },
                        lineLimits = TextFieldLineLimits.MultiLine(
                            minHeightInLines = 3,
                            maxHeightInLines = 6,
                        ),
                    )
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    CreationSectionLabel(stringResource(R.string.members))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column {
                            selectedPeople.forEach { person ->
                                PersonRow(person = person, onClick = null)
                            }
                        }
                    }
                    Text(
                        pluralStringResource(
                            R.plurals.relay_count,
                            profile.chatRelayUrls.size,
                            profile.chatRelayUrls.size,
                        ),
                        modifier = Modifier.padding(
                            start = WhiteNoiseSpacing.CompactScreenMargin,
                            end = WhiteNoiseSpacing.CompactScreenMargin,
                            top = WhiteNoiseSpacing.Related,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
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
private fun PersonRow(
    person: Person,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
) {
    val interactionModifier = when {
        selected != null && onClick != null -> Modifier.toggleable(
            value = selected,
            role = Role.Checkbox,
            onValueChange = { onClick() },
        )
        onClick != null -> Modifier.clickable(onClick = onClick)
        else -> Modifier
    }
    ListItem(
        headlineContent = { Text(person.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(person.shortPublicKey, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { ProfileAvatar(person.name, person.avatar, Modifier.size(48.dp), contentDescription = null) },
        trailingContent = {
            if (selected == true) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .then(interactionModifier),
    )
}

@Composable
private fun PeopleSearchField(
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
            ),
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
private fun CreationSectionLabel(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = WhiteNoiseSpacing.CompactScreenMargin,
                end = WhiteNoiseSpacing.CompactScreenMargin,
                top = WhiteNoiseSpacing.FormField,
                bottom = WhiteNoiseSpacing.Related,
            ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun CreationBottomAction(
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
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
        ) {
            Text(label)
        }
    }
}

@Composable
private fun ProfileActionRow(
    title: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                color = if (destructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Composable
private fun GroupAvatarPreview(
    name: String,
    avatar: ProfileAvatar,
    modifier: Modifier = Modifier,
) {
    val groupPhotoDescription = stringResource(R.string.group_photo)
    if (name.isBlank() && avatar == ProfileAvatar.Monogram) {
        Surface(
            modifier = modifier.semantics {
                contentDescription = groupPhotoDescription
            },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_group),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
    } else {
        ProfileAvatar(
            name = name,
            avatar = avatar,
            modifier = modifier,
            contentDescription = groupPhotoDescription,
        )
    }
}

private fun Person.matches(query: String): Boolean {
    val needle = query.searchNormalized()
    return needle.isEmpty() || name.searchNormalized().contains(needle) || publicKey.searchNormalized().contains(needle)
}

private fun String.searchNormalized(): String = Normalizer
    .normalize(trim().lowercase(), Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")

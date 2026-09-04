package dev.ipf.whitenoise.ui.conversation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import dev.ipf.whitenoise.ui.onboarding.AvatarImageProcessor
import dev.ipf.whitenoise.ui.onboarding.AvatarWebImagePicker
import androidx.compose.foundation.rememberScrollState
import dev.ipf.whitenoise.ui.components.whiteNoiseVerticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.model.ProfileAvatar as Avatar
import dev.ipf.whitenoise.ui.components.*
import dev.ipf.whitenoise.ui.settings.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun GroupEditorScreen(chat: Chat, profile: Profile?, onBack: () -> Unit, onSave: (String, String, Avatar) -> Boolean) {
    val controller = LocalGroupWork.current
    val owner = GroupOwner(profile?.id.orEmpty(), chat.id)
    val retained = controller?.editWork?.get(owner)
    var base by remember(owner) { mutableStateOf(retained?.expected ?: GroupEditDraft.from(chat)) }
    var draft by remember(owner) { mutableStateOf(retained?.draft ?: base) }
    val name = rememberSaveable(owner.profileId, owner.chatId, saver = TextFieldState.Saver) { TextFieldState(draft.name) }
    val description = rememberSaveable(owner.profileId, owner.chatId, saver = TextFieldState.Saver) { TextFieldState(draft.description) }
    val formDraft = draft.copy(name = name.text.toString(), description = description.text.toString())
    val work = controller?.editWork?.get(owner)
    var privatePreparing by remember(owner) { mutableStateOf(false) }
    var publicPreparing by remember(owner) { mutableStateOf(false) }
    val busy = work?.phase == GroupWorkPhase.Applying
    val canEdit = !busy && (profile == null || chat.hasAuthoritativeGroupAdmin(profile.id)) && (controller?.locked(owner) != true)
    var failed by remember(owner) { mutableStateOf(false) }
    LaunchedEffect(work?.id, work?.phase) {
        if (work?.phase == GroupWorkPhase.Complete) { controller?.dismissEdit(owner, work.id); onBack() }
    }
    SettingsScaffold(stringResource(R.string.edit_group), onBack, bottomBar = {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            WhiteNoiseButton(onClick = {
                if (controller != null && profile != null) failed = !controller.beginEdit(owner, base, formDraft)
                else { failed = !onSave(formDraft.name, formDraft.description, formDraft.image); if (!failed) onBack() }
            }, enabled = canEdit && !privatePreparing && !publicPreparing && formDraft.name.isNotBlank() && formDraft != base,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(WhiteNoiseSpacing.PinnedActionInset)) {
                Text(stringResource(if (busy) R.string.group_saving else R.string.save))
            }
        }
    }) {
        Column(Modifier.fillMaxSize().whiteNoiseVerticalScroll(rememberScrollState()).padding(vertical = WhiteNoiseSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Section)) {
            if (profile != null) GroupRosterPanel(profile, chat)
            GroupImageField(owner, stringResource(R.string.group_private_image), stringResource(R.string.group_private_image_detail), draft.image, canEdit,
                allowEmoji = true, scenario = controller?.imageScenario ?: GroupImageScenario.Success, onPreparing = { privatePreparing = it }) { draft = draft.copy(image = it) }
            GroupImageField(owner, stringResource(R.string.group_public_image), stringResource(R.string.group_public_image_detail), draft.publicImage, canEdit,
                allowEmoji = false, onPreparing = { publicPreparing = it }) { draft = draft.copy(publicImage = it) }
            Column(Modifier.fillMaxWidth().padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField)) {
                WhiteNoiseTextField(state = name, label = { Text(stringResource(R.string.group_name)) }, enabled = canEdit, lineLimits = TextFieldLineLimits.SingleLine, modifier = Modifier.fillMaxWidth().testTag("group_edit.name"))
                WhiteNoiseTextField(state = description, label = { Text(stringResource(R.string.group_description)) }, enabled = canEdit, lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 6), modifier = Modifier.fillMaxWidth())
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (failed || work?.phase == GroupWorkPhase.Failed) {
                    Text(stringResource(when (work?.failure) { GroupWorkFailure.Upload -> R.string.group_image_save_failed; GroupWorkFailure.SourceChanged, GroupWorkFailure.Interrupted -> R.string.group_edit_changed; else -> R.string.couldnt_save_group }), color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                        if (work != null && work.failure == GroupWorkFailure.SourceChanged) TextButton(onClick = {
                            base = GroupEditDraft.from(chat); controller?.dismissEdit(owner, work.id); failed = false
                        }, enabled = canEdit) { Text(stringResource(R.string.group_review_changes)) }
                        else if (work != null) TextButton(onClick = { failed = controller?.retryEdit(owner, work.id) != true }, enabled = canEdit) { Text(stringResource(R.string.dictation_retry)) }
                        if (work != null) TextButton(onClick = { controller?.dismissEdit(owner, work.id); failed = false }) { Text(stringResource(R.string.batch_dismiss)) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GroupImageField(owner: GroupOwner, title: String, detail: String, image: Avatar, enabled: Boolean,
    allowEmoji: Boolean, scenario: GroupImageScenario = GroupImageScenario.Success, onPreparing: (Boolean) -> Unit = {}, onImage: (Avatar) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val liveEnabled by rememberUpdatedState(enabled)
    val liveOnImage by rememberUpdatedState(onImage)
    val liveOnPreparing by rememberUpdatedState(onPreparing)
    var menu by rememberSaveable(owner, title) { mutableStateOf(false) }
    var web by rememberSaveable(owner, title) { mutableStateOf(false) }
    var emoji by rememberSaveable(owner, title) { mutableStateOf(false) }
    var job by remember(owner, title) { mutableStateOf<Job?>(null) }
    var revision by remember(owner, title) { mutableIntStateOf(0) }
    var preparing by remember(owner, title) { mutableStateOf(false) }
    var preparationFailed by remember(owner, title) { mutableStateOf(false) }
    var loadRetry by remember(owner, title, scenario) { mutableIntStateOf(0) }
    var loading by remember(owner, title, scenario) { mutableStateOf(scenario in setOf(GroupImageScenario.Loading, GroupImageScenario.LoadFailure)) }
    var loadFailed by remember(owner, title, scenario) { mutableStateOf(false) }
    var replacedWhileLoading by remember(owner, title, scenario) { mutableStateOf(false) }
    LaunchedEffect(owner, title, scenario, loadRetry) {
        if (scenario in setOf(GroupImageScenario.Loading, GroupImageScenario.LoadFailure)) {
            loading = true; loadFailed = false; delay(600)
            loading = false; loadFailed = !replacedWhileLoading && scenario == GroupImageScenario.LoadFailure && loadRetry == 0
        }
    }
    SideEffect { liveOnPreparing(preparing) }
    fun choose(value: Avatar) { revision++; job?.cancel(); preparing = false; preparationFailed = false; if (liveEnabled) { replacedWhileLoading = true; loading = false; loadFailed = false; liveOnImage(value) } }
    fun prepare(uri: android.net.Uri) {
        val request = ++revision; job?.cancel()
        job = scope.launch {
            preparing = true; preparationFailed = false
            try {
                val bytes = AvatarImageProcessor.prepare(context.contentResolver, uri)
                if (request == revision && liveEnabled) { if (bytes == null) preparationFailed = true else { replacedWhileLoading = true; loading = false; loadFailed = false; liveOnImage(Avatar.DeviceImage(bytes)) } }
            } finally { if (request == revision) preparing = false }
        }
    }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { if (it != null && liveEnabled) prepare(it) }
    val file = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { if (it != null && liveEnabled) prepare(it) }
    DisposableEffect(owner, title) { onDispose { revision++; job?.cancel(); liveOnPreparing(false) } }
    Column(Modifier.fillMaxWidth().testTag(if (allowEmoji) "group.image.private" else "group.image.public").padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(detail, style = MaterialTheme.typography.bodyMedium)
        ProfileAvatar(title, if (loading || loadFailed) Avatar.Monogram else image, Modifier.size(120.dp), contentDescription = title, emptyMonogramIcon = R.drawable.ic_group)
        if (loading || preparing) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(stringResource(if (loading) R.string.group_image_loading else R.string.preparing_photo)) }
        if (loadFailed) { Text(stringResource(R.string.group_image_failed), color = MaterialTheme.colorScheme.error); TextButton(onClick = { loadRetry++ }) { Text(stringResource(R.string.dictation_retry)) } }
        if (preparationFailed) Text(stringResource(R.string.photo_error), color = MaterialTheme.colorScheme.error)
        Box {
            AvatarPhotoButton(hasPhoto = image != Avatar.Monogram, onClick = { menu = true }, enabled = enabled && !preparing)
            WhiteNoiseDropdownMenu(menu, { menu = false }, buildList {
                add(WhiteNoiseMenuItem(label = stringResource(R.string.choose_photos), icon = R.drawable.ic_image, onClick = { photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }))
                add(WhiteNoiseMenuItem(label = stringResource(R.string.choose_files), icon = R.drawable.ic_description, onClick = { file.launch(arrayOf("image/*")) }))
                add(WhiteNoiseMenuItem(label = stringResource(R.string.find_web_image), icon = R.drawable.ic_search, onClick = { web = true }))
                if (allowEmoji) add(WhiteNoiseMenuItem(label = stringResource(R.string.group_emoji_create), icon = R.drawable.ic_add, onClick = { emoji = true }))
                if (image != Avatar.Monogram) add(WhiteNoiseMenuItem(label = stringResource(R.string.remove_photo), icon = R.drawable.ic_delete, onClick = { choose(Avatar.Monogram) }, destructive = true))
            })
        }
    }
    if (web) AvatarWebImagePicker(null, { web = false }) { value -> choose(Avatar.WebImage(value.asset, value.id)); web = false }
    if (emoji) GroupEmojiImageDialog({ emoji = false }) { choose(it); emoji = false }
}

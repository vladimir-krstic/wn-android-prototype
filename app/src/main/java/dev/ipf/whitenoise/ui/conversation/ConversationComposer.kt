package dev.ipf.whitenoise.ui.conversation

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.AvatarAsset
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatTimelineEntry
import dev.ipf.whitenoise.model.LinkPreviewDetector
import dev.ipf.whitenoise.model.MessageAttachment
import dev.ipf.whitenoise.model.MessageAttachmentKind
import dev.ipf.whitenoise.model.Person
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.VoiceMessageFixture
import dev.ipf.whitenoise.model.VoiceMessageFormat
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.drawableResource
import dev.ipf.whitenoise.ui.onboarding.AvatarImageProcessor
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullConversationComposer(
    profile: Profile,
    chat: Chat,
    onDraftTextChanged: (String) -> Unit,
    onAddAttachments: (List<MessageAttachment>) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSuppressLink: (String?) -> Unit,
    onCancelReply: () -> Unit,
    onSendDraft: () -> Boolean,
    onSendVoice: (VoiceMessageFormat, String) -> Boolean,
) {
    val context = LocalContext.current
    val addAttachmentDescription = stringResource(R.string.add_attachment)
    val recordVoiceDescription = stringResource(R.string.record_voice_message)
    val coroutineScope = rememberCoroutineScope()
    val messageFocusRequester = remember { FocusRequester() }
    var attachmentMenuOpen by remember { mutableStateOf(false) }
    var contactPickerOpen by remember { mutableStateOf(false) }
    var gifPickerOpen by remember { mutableStateOf(false) }
    var mediaViewerAttachmentId by remember { mutableStateOf<String?>(null) }
    var isExpanded by rememberSaveable(chat.id) { mutableStateOf(false) }
    var isPreparing by remember { mutableStateOf(false) }
    var attachmentError by remember { mutableStateOf(false) }
    var preparationJob by remember { mutableStateOf<Job?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var attachmentCounter by rememberSaveable(chat.id) { mutableIntStateOf(0) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var voiceReviewOpen by remember { mutableStateOf(false) }

    fun nextId(prefix: String): String {
        attachmentCounter += 1
        return "${chat.id}-$prefix-$attachmentCounter"
    }

    fun prepareVisualUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        preparationJob?.cancel()
        preparationJob = coroutineScope.launch {
            isPreparing = true
            attachmentError = false
            val prepared = mutableListOf<MessageAttachment>()
            for (uri in uris) {
                val type = context.contentResolver.getType(uri).orEmpty()
                if (type.startsWith("video/")) {
                    prepared += MessageAttachment(
                        id = nextId("video"),
                        kind = MessageAttachmentKind.Video,
                        label = displayName(context, uri, "Video"),
                        images = listOf(ProfileAvatar.Asset(AvatarAsset.GardenClub)),
                        externalUri = uri.toString(),
                    )
                } else {
                    val bytes = runCatching {
                        AvatarImageProcessor.prepare(context.contentResolver, uri)
                    }.getOrNull()
                    if (bytes == null) {
                        attachmentError = true
                    } else {
                        prepared += MessageAttachment(
                            id = nextId("photo"),
                            kind = MessageAttachmentKind.Photo,
                            label = displayName(context, uri, "Photo"),
                            images = listOf(ProfileAvatar.DeviceImage(bytes)),
                        )
                    }
                }
            }
            if (prepared.isNotEmpty()) onAddAttachments(prepared)
            isPreparing = false
        }
    }

    val visualPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(7),
    ) { uris -> prepareVisualUris(uris) }
    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val attachments = uris.map { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            MessageAttachment(
                id = nextId("file"),
                kind = MessageAttachmentKind.File,
                label = displayName(context, uri, "Document"),
                externalUri = uri.toString(),
            )
        }
        onAddAttachments(attachments)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = cameraUri
        if (saved && uri != null) prepareVisualUris(listOf(uri))
    }

    DisposableEffect(Unit) {
        onDispose { preparationJob?.cancel() }
    }
    LaunchedEffect(isRecording) {
        if (!isRecording) return@LaunchedEffect
        recordingSeconds = 0
        while (isRecording && recordingSeconds < 59) {
            delay(1_000)
            if (isRecording) recordingSeconds += 1
        }
    }
    LaunchedEffect(chat.draftReplyMessageId) {
        if (chat.draftReplyMessageId != null) messageFocusRequester.requestFocus()
    }

    val linkPreview = LinkPreviewDetector.first(chat.draftText)
        ?.takeUnless { it.url == chat.suppressedDraftLinkUrl }
    val replyMessage = chat.draftReplyMessageId?.let { id ->
        chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().firstOrNull { it.message.id == id }?.message
    }
    val mentionMatch = if (chat.isGroup) Regex("(?:^|\\s)@([^\\s]*)$").find(chat.draftText) else null
    val mentionQuery = mentionMatch?.groupValues?.getOrNull(1).orEmpty()
    val mentionPeople = if (mentionMatch == null) {
        emptyList()
    } else {
        val memberIds = chat.members.map { it.personId }.toSet()
        profile.people.filter { person ->
            person.id in memberIds && person.id != profile.id &&
                (mentionQuery.isBlank() || person.name.contains(mentionQuery, ignoreCase = true))
        }.take(5)
    }
    val sendable = chat.draftText.isNotBlank() || chat.draftAttachments.isNotEmpty() || linkPreview != null

    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = WhiteNoiseSpacing.Related, vertical = WhiteNoiseSpacing.Related)
                .then(if (isExpanded) Modifier.heightIn(min = 300.dp, max = 560.dp) else Modifier),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().then(if (isExpanded) Modifier.fillMaxHeight() else Modifier),
                shape = if (isExpanded) MaterialTheme.shapes.large else MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column {
                    if (chat.draftAttachments.isNotEmpty()) {
                        DraftAttachmentShelf(
                            attachments = chat.draftAttachments,
                            onPreview = { mediaViewerAttachmentId = it },
                            onRemove = onRemoveAttachment,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    linkPreview?.let { preview ->
                        DraftLinkPreview(
                            title = preview.title,
                            domain = preview.domain,
                            summary = preview.summary,
                            image = preview.image,
                            onRemove = { onSuppressLink(preview.url) },
                        )
                    }
                    replyMessage?.let { message ->
                        DraftReplyQuote(
                            author = if (message.authorId == profile.id) {
                                stringResource(R.string.you)
                            } else {
                                profile.people.firstOrNull { it.id == message.authorId }?.name
                                    ?: stringResource(R.string.unknown_person)
                            },
                            text = message.text.ifBlank { message.attachments.firstOrNull()?.label.orEmpty() },
                            onCancel = onCancelReply,
                        )
                    }
                    if (mentionMatch != null && mentionPeople.isNotEmpty()) {
                        MentionSuggestions(
                            people = mentionPeople,
                            onSelect = { person ->
                                val range = mentionMatch.range
                                val prefix = chat.draftText.substring(0, range.first)
                                val separator = if (prefix.isNotEmpty() && !prefix.endsWith(' ')) " " else ""
                                onDraftTextChanged("$prefix$separator@${person.name} ")
                            },
                        )
                    }
                    if (isRecording) {
                        RecordingComposer(
                            seconds = recordingSeconds,
                            onCancel = { isRecording = false },
                            onStop = {
                                isRecording = false
                                voiceReviewOpen = true
                            },
                        )
                    } else {
                        ComposerInputRow(
                            text = chat.draftText,
                            onTextChanged = onDraftTextChanged,
                            onAddAttachment = { attachmentMenuOpen = true },
                            onSend = onSendDraft,
                            onRecord = { isRecording = true },
                            sendable = sendable,
                            enabled = !isPreparing,
                            isExpanded = isExpanded,
                            focusRequester = messageFocusRequester,
                            addAttachmentDescription = addAttachmentDescription,
                            recordVoiceDescription = recordVoiceDescription,
                        )
                        if (chat.draftText.contains('\n') || chat.draftAttachments.isNotEmpty() || isExpanded) {
                            TextButton(
                                onClick = { isExpanded = !isExpanded },
                                modifier = Modifier.align(Alignment.End).padding(end = WhiteNoiseSpacing.Related),
                            ) {
                                Text(
                                    stringResource(
                                        if (isExpanded) R.string.collapse_message else R.string.expand_message,
                                    ),
                                )
                            }
                        }
                    }
                    if (isPreparing) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin, vertical = 4.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite },
                            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.preparing_attachment))
                        }
                    }
                    if (attachmentError) {
                        Text(
                            stringResource(R.string.attachment_error),
                            modifier = Modifier
                                .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin, vertical = 4.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }

    if (attachmentMenuOpen) {
        ModalBottomSheet(onDismissRequest = { attachmentMenuOpen = false }) {
            Column(Modifier.fillMaxWidth().padding(bottom = WhiteNoiseSpacing.CompactScreenMargin)) {
                Text(
                    stringResource(R.string.add_attachment),
                    modifier = Modifier.padding(
                        horizontal = WhiteNoiseSpacing.Section,
                        vertical = WhiteNoiseSpacing.Related,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
                AttachmentAction(R.drawable.ic_camera, stringResource(R.string.camera)) {
                    attachmentMenuOpen = false
                    val directory = File(context.cacheDir, "camera").apply { mkdirs() }
                    val file = File.createTempFile("white-noise-", ".jpg", directory)
                    cameraUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.files",
                        file,
                    )
                    camera.launch(cameraUri!!)
                }
                AttachmentAction(R.drawable.ic_image, stringResource(R.string.photos_and_videos)) {
                    attachmentMenuOpen = false
                    visualPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                    )
                }
                AttachmentAction(R.drawable.ic_description, stringResource(R.string.file)) {
                    attachmentMenuOpen = false
                    documentPicker.launch(arrayOf("*/*"))
                }
                AttachmentAction(R.drawable.ic_person, stringResource(R.string.contact)) {
                    attachmentMenuOpen = false
                    contactPickerOpen = true
                }
                AttachmentAction(R.drawable.ic_image, stringResource(R.string.gif)) {
                    attachmentMenuOpen = false
                    gifPickerOpen = true
                }
            }
        }
    }
    if (contactPickerOpen) {
        ContactPickerSheet(
            people = profile.people,
            onDismiss = { contactPickerOpen = false },
            onSelect = { person ->
                onAddAttachments(
                    listOf(
                        MessageAttachment(
                            id = nextId("contact"),
                            kind = MessageAttachmentKind.Contact,
                            label = "Contact: ${person.name}",
                            images = listOf(person.avatar),
                        ),
                    ),
                )
                contactPickerOpen = false
            },
        )
    }
    if (gifPickerOpen) {
        GifPickerSheet(
            onDismiss = { gifPickerOpen = false },
            onSelect = { asset, label ->
                onAddAttachments(
                    listOf(
                        MessageAttachment(
                            id = nextId("gif"),
                            kind = MessageAttachmentKind.Gif,
                            label = label,
                            images = listOf(ProfileAvatar.Asset(asset)),
                        ),
                    ),
                )
                gifPickerOpen = false
            },
        )
    }
    mediaViewerAttachmentId?.let { initialAttachmentId ->
        DraftMediaViewer(
            attachments = chat.draftAttachments.filter(MessageAttachment::isVisual),
            initialAttachmentId = initialAttachmentId,
            onDismiss = { mediaViewerAttachmentId = null },
            onApplyExcluded = { excludedIds ->
                excludedIds.forEach(onRemoveAttachment)
                mediaViewerAttachmentId = null
            },
        )
    }
    if (voiceReviewOpen) {
        VoiceReviewSheet(
            onDismiss = { voiceReviewOpen = false },
            onSend = { format, transcript ->
                if (onSendVoice(format, transcript)) voiceReviewOpen = false
            },
        )
    }
}

@Composable
private fun ColumnScope.ComposerInputRow(
    text: String,
    onTextChanged: (String) -> Unit,
    onAddAttachment: () -> Unit,
    onSend: () -> Boolean,
    onRecord: () -> Unit,
    sendable: Boolean,
    enabled: Boolean,
    isExpanded: Boolean,
    focusRequester: FocusRequester,
    addAttachmentDescription: String,
    recordVoiceDescription: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isExpanded) Modifier.weight(1f) else Modifier)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onAddAttachment, enabled = enabled) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = addAttachmentDescription,
            )
        }
        TextField(
            value = text,
            onValueChange = onTextChanged,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .then(if (isExpanded) Modifier.fillMaxHeight() else Modifier),
            placeholder = { Text(stringResource(R.string.message)) },
            minLines = if (isExpanded) 8 else 1,
            maxLines = if (isExpanded) 20 else 10,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            keyboardActions = KeyboardActions(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )
        if (sendable) {
            FilledIconButton(
                onClick = { onSend() },
                enabled = enabled,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = stringResource(R.string.send),
                )
            }
        } else {
            IconButton(onClick = onRecord, enabled = enabled) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = recordVoiceDescription,
                )
            }
        }
    }
}

@Composable
private fun MentionSuggestions(people: List<Person>, onSelect: (Person) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WhiteNoiseSpacing.Related, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            people.forEach { person ->
                ListItem(
                    headlineContent = { Text(person.name) },
                    leadingContent = { ProfileAvatar(person.name, person.avatar, Modifier.size(36.dp), contentDescription = null) },
                    modifier = Modifier.clickable { onSelect(person) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun DraftAttachmentShelf(
    attachments: List<MessageAttachment>,
    onPreview: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val description = pluralStringResource(
        R.plurals.draft_attachment_count,
        attachments.size,
        attachments.size,
    )
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(
            horizontal = WhiteNoiseSpacing.Related,
            vertical = WhiteNoiseSpacing.Related,
        ),
        horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
    ) {
        items(attachments, key = MessageAttachment::id) { attachment ->
            val removeDescription = stringResource(R.string.remove_attachment, attachment.label)
            val isVisual = attachment.isVisual()
            Surface(
                modifier = Modifier
                    .height(104.dp)
                    .widthIn(min = 104.dp, max = 160.dp)
                    .then(if (isVisual) Modifier.clickable { onPreview(attachment.id) } else Modifier)
                    .semantics { contentDescription = attachment.label },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Box {
                    DraftAttachmentContent(
                        attachment = attachment,
                        modifier = Modifier.fillMaxSize(),
                    )
                    DraftRemoveButton(
                        onClick = { onRemove(attachment.id) },
                        description = removeDescription,
                        highContrast = isVisual,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
        }
    }
}

@Composable
private fun DraftAttachmentContent(
    attachment: MessageAttachment,
    modifier: Modifier = Modifier,
) {
    when {
        attachment.isVisual() -> Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            attachment.images.firstOrNull()?.let { image ->
                ComposerImage(image, Modifier.fillMaxSize())
            } ?: Text(
                attachment.label,
                modifier = Modifier.align(Alignment.Center).padding(WhiteNoiseSpacing.Related),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (attachment.kind == MessageAttachmentKind.Video) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f),
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_arrow),
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(20.dp),
                    )
                }
            }
            if (attachment.kind == MessageAttachmentKind.Gif) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(WhiteNoiseSpacing.Related),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f),
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ) {
                    Text(
                        stringResource(R.string.gif),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        attachment.kind == MessageAttachmentKind.Contact -> Column(
            modifier = modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            attachment.images.firstOrNull()?.let { image ->
                ComposerImage(image, Modifier.size(44.dp).clip(CircleShape))
                Spacer(Modifier.height(WhiteNoiseSpacing.Related))
            }
            Text(
                attachment.label.removePrefix("Contact: "),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        else -> Column(
            modifier = modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_description),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(WhiteNoiseSpacing.Related))
            Text(
                attachment.label,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DraftRemoveButton(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    highContrast: Boolean = false,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = if (highContrast) {
                MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            contentColor = if (highContrast) {
                MaterialTheme.colorScheme.inverseOnSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            border = BorderStroke(
                1.dp,
                if (highContrast) MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = description,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun VoiceWaveform(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val heights = listOf(8, 16, 24, 32, 24, 14, 28, 18, 10, 22, 30, 18, 8)
    Row(
        modifier = modifier.height(36.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        heights.forEach { barHeight ->
            Box(
                Modifier
                    .width(3.dp)
                    .height(barHeight.dp)
                    .background(color, CircleShape),
            )
        }
    }
}

@Composable
private fun DraftLinkPreview(
    title: String,
    domain: String,
    summary: String,
    image: ProfileAvatar?,
    onRemove: () -> Unit,
) {
    val removeDescription = stringResource(R.string.remove_link_preview)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WhiteNoiseSpacing.Related, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(WhiteNoiseSpacing.Related),
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            image?.let { ComposerImage(it, Modifier.size(56.dp).clip(MaterialTheme.shapes.small)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    domain,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    summary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DraftRemoveButton(onClick = onRemove, description = removeDescription)
        }
    }
}

@Composable
private fun DraftReplyQuote(author: String, text: String, onCancel: () -> Unit) {
    val cancelDescription = stringResource(R.string.cancel_reply)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WhiteNoiseSpacing.Related, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.padding(WhiteNoiseSpacing.Related),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
            Column(Modifier.weight(1f)) {
                Text(author, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DraftRemoveButton(onClick = onCancel, description = cancelDescription)
        }
    }
}

@Composable
private fun RecordingComposer(seconds: Int, onCancel: () -> Unit, onStop: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WhiteNoiseSpacing.Related, vertical = WhiteNoiseSpacing.Related),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
    ) {
        VoiceWaveform(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            "0:${seconds.coerceAtMost(59).toString().padStart(2, '0')}",
            style = MaterialTheme.typography.labelLarge,
        )
        TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        FilledTonalButton(onClick = onStop) { Text(stringResource(R.string.stop_recording)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceReviewSheet(
    onDismiss: () -> Unit,
    onSend: (VoiceMessageFormat, String) -> Unit,
) {
    var hasTranscript by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf(VoiceMessageFixture.transcript) }
    var formatName by remember { mutableStateOf(VoiceMessageFormat.Both.name) }
    var formatMenuOpen by remember { mutableStateOf(false) }
    val format = VoiceMessageFormat.valueOf(formatName)
    val sendEnabled = format == VoiceMessageFormat.Voice || transcript.isNotBlank()
    val effectiveFormat = if (hasTranscript) format else VoiceMessageFormat.Voice
    val sendLabel = stringResource(
        when (effectiveFormat) {
            VoiceMessageFormat.Voice -> R.string.send_voice_message
            VoiceMessageFormat.Text -> R.string.send_text_message
            VoiceMessageFormat.Both -> R.string.send_voice_and_text
        },
    )
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WhiteNoiseSpacing.Section, vertical = WhiteNoiseSpacing.CompactScreenMargin),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
            ) {
                Text(
                    stringResource(R.string.voice_message_review),
                    style = MaterialTheme.typography.titleLarge,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(
                        modifier = Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                    ) {
                        VoiceWaveform(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "0:08",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                if (!hasTranscript) {
                    FilledTonalButton(
                        onClick = {
                            hasTranscript = true
                            formatName = VoiceMessageFormat.Both.name
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.transcribe))
                    }
                } else {
                    Box {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { formatMenuOpen = true },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                                    vertical = 12.dp,
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.message_format),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(format.label, style = MaterialTheme.typography.bodyLarge)
                                }
                                Icon(
                                    painter = painterResource(R.drawable.ic_expand_more),
                                    contentDescription = null,
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = formatMenuOpen,
                            onDismissRequest = { formatMenuOpen = false },
                        ) {
                            VoiceMessageFormat.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    trailingIcon = if (option == format) ({
                                        Icon(
                                            painter = painterResource(R.drawable.ic_check),
                                            contentDescription = null,
                                        )
                                    }) else null,
                                    onClick = {
                                        formatName = option.name
                                        formatMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                    if (format != VoiceMessageFormat.Voice) {
                        TextField(
                            value = transcript,
                            onValueChange = { transcript = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.transcript)) },
                            minLines = 3,
                            maxLines = 8,
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
                WhiteNoiseButton(
                    onClick = { onSend(effectiveFormat, transcript) },
                    enabled = sendEnabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(sendLabel)
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(stringResource(R.string.discard))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactPickerSheet(
    people: List<Person>,
    onDismiss: () -> Unit,
    onSelect: (Person) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
            contentPadding = PaddingValues(bottom = WhiteNoiseSpacing.Section),
        ) {
            item {
                Text(
                    stringResource(R.string.choose_contact),
                    Modifier.padding(
                        horizontal = WhiteNoiseSpacing.Section,
                        vertical = WhiteNoiseSpacing.Related,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            items(people.take(12), key = Person::id) { person ->
                ListItem(
                    headlineContent = { Text(person.name) },
                    leadingContent = {
                        ProfileAvatar(
                            person.name,
                            person.avatar,
                            Modifier.size(48.dp),
                            contentDescription = null,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onSelect(person) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GifPickerSheet(onDismiss: () -> Unit, onSelect: (AvatarAsset, String) -> Unit) {
    val gifs = listOf(
        AvatarAsset.Marmot to "Marmot looking around",
        AvatarAsset.Badger to "Badger waving",
        AvatarAsset.Fox to "Fox celebrating",
    )
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
            contentPadding = PaddingValues(bottom = WhiteNoiseSpacing.Section),
        ) {
            item {
                Text(
                    stringResource(R.string.choose_gif),
                    Modifier.padding(
                        horizontal = WhiteNoiseSpacing.Section,
                        vertical = WhiteNoiseSpacing.Related,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            items(gifs, key = { it.second }) { (asset, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    leadingContent = {
                        Image(
                            painterResource(asset.drawableResource),
                            null,
                            Modifier.size(56.dp).clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onSelect(asset, label) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftMediaViewer(
    attachments: List<MessageAttachment>,
    initialAttachmentId: String,
    onDismiss: () -> Unit,
    onApplyExcluded: (Set<String>) -> Unit,
) {
    if (attachments.isEmpty()) return
    var includedIds by remember(attachments) { mutableStateOf(attachments.map { it.id }.toSet()) }
    val initialPage = attachments.indexOfFirst { it.id == initialAttachmentId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { attachments.size }
    val coroutineScope = rememberCoroutineScope()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.preview_media)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.cancel_media_changes),
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                onApplyExcluded(attachments.map { it.id }.toSet() - includedIds)
                            },
                        ) {
                            Text(stringResource(R.string.done))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { contentPadding ->
            Column(Modifier.fillMaxSize().padding(contentPadding)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    pageSpacing = WhiteNoiseSpacing.Section,
                ) { page ->
                    val attachment = attachments[page]
                    val included = attachment.id in includedIds
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        attachment.images.firstOrNull()?.let {
                            ComposerImage(
                                image = it,
                                modifier = Modifier.fillMaxSize().padding(WhiteNoiseSpacing.CompactScreenMargin),
                                contentScale = ContentScale.Fit,
                            )
                        } ?: Text(attachment.label)
                        FilterChip(
                            selected = included,
                            onClick = {
                                includedIds = if (included) {
                                    includedIds - attachment.id
                                } else {
                                    includedIds + attachment.id
                                }
                            },
                            label = {
                                Text(
                                    stringResource(
                                        if (included) R.string.media_included else R.string.media_excluded,
                                    ),
                                )
                            },
                            leadingIcon = if (included) ({
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }) else null,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(WhiteNoiseSpacing.Section),
                        )
                    }
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(88.dp),
                    contentPadding = PaddingValues(
                        horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                        vertical = WhiteNoiseSpacing.Related,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) {
                    itemsIndexed(attachments, key = { _, item -> item.id }) { index, attachment ->
                        val selected = pagerState.currentPage == index
                        val positionDescription = stringResource(
                            R.string.media_item_position,
                            index + 1,
                            attachments.size,
                        )
                        Surface(
                            modifier = Modifier
                                .size(72.dp)
                                .semantics { contentDescription = positionDescription }
                                .clickable {
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                },
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            border = if (selected) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            },
                        ) {
                            attachment.images.firstOrNull()?.let {
                                ComposerImage(
                                    image = it,
                                    modifier = Modifier.padding(4.dp).clip(MaterialTheme.shapes.small),
                                )
                            } ?: Box(contentAlignment = Alignment.Center) {
                                Text(
                                    attachment.label,
                                    maxLines = 2,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentAction(iconRes: Int, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

@Composable
internal fun ComposerImage(
    image: ProfileAvatar,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    when (image) {
        is ProfileAvatar.Asset -> Image(
            painterResource(image.asset.drawableResource),
            null,
            modifier,
            contentScale = contentScale,
        )
        is ProfileAvatar.WebImage -> Image(
            painterResource(image.asset.drawableResource),
            null,
            modifier,
            contentScale = contentScale,
        )
        is ProfileAvatar.DeviceImage -> {
            val bitmap = remember(image) {
                BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)?.asImageBitmap()
            }
            if (bitmap == null) Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
            else Image(bitmap, null, modifier, contentScale = contentScale)
        }
        ProfileAvatar.Monogram -> Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

internal fun MessageAttachment.isVisual(): Boolean = when (kind) {
    MessageAttachmentKind.Photo,
    MessageAttachmentKind.Photos,
    MessageAttachmentKind.Video,
    MessageAttachmentKind.Gif,
    -> true
    else -> false
}

private fun displayName(
    context: android.content.Context,
    uri: Uri,
    fallback: String,
): String {
    val cursor = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )
    return cursor?.use {
        if (it.moveToFirst()) it.getString(0) else null
    } ?: fallback
}

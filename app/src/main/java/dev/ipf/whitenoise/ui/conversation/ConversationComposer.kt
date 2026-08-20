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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import dev.ipf.whitenoise.ui.components.drawableResource
import dev.ipf.whitenoise.ui.onboarding.AvatarImageProcessor
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
    var mediaViewerOpen by remember { mutableStateOf(false) }
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

    Surface(tonalElevation = 3.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .then(if (isExpanded) Modifier.heightIn(min = 300.dp, max = 560.dp) else Modifier),
        ) {
            if (chat.draftAttachments.isNotEmpty()) {
                DraftAttachmentShelf(
                    attachments = chat.draftAttachments,
                    onPreview = { mediaViewerOpen = true },
                    onRemove = onRemoveAttachment,
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IconButton(onClick = { attachmentMenuOpen = true }) {
                        Text(
                            "+",
                            modifier = Modifier.semantics {
                                contentDescription = addAttachmentDescription
                            },
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    TextField(
                        value = chat.draftText,
                        onValueChange = onDraftTextChanged,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(messageFocusRequester)
                            .then(if (isExpanded) Modifier.fillMaxHeight() else Modifier),
                        placeholder = { Text(stringResource(R.string.message)) },
                        minLines = if (isExpanded) 8 else 1,
                        maxLines = if (isExpanded) 20 else 10,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        keyboardActions = KeyboardActions(),
                    )
                    if (sendable) {
                        Button(onClick = { onSendDraft() }) { Text(stringResource(R.string.send)) }
                    } else {
                        IconButton(onClick = { isRecording = true }) {
                            Text(
                                "▥",
                                modifier = Modifier.semantics {
                                    contentDescription = recordVoiceDescription
                                },
                            )
                        }
                    }
                }
                if (chat.draftText.contains('\n') || chat.draftAttachments.isNotEmpty() || isExpanded) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.align(Alignment.End).padding(end = 8.dp),
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.preparing_photo))
                }
            }
            if (attachmentError) {
                Text(
                    stringResource(R.string.attachment_error),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    if (attachmentMenuOpen) {
        ModalBottomSheet(onDismissRequest = { attachmentMenuOpen = false }) {
            Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                AttachmentAction(stringResource(R.string.camera)) {
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
                AttachmentAction(stringResource(R.string.photos_and_videos)) {
                    attachmentMenuOpen = false
                    visualPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                    )
                }
                AttachmentAction(stringResource(R.string.file)) {
                    attachmentMenuOpen = false
                    documentPicker.launch(arrayOf("*/*"))
                }
                AttachmentAction(stringResource(R.string.contact)) {
                    attachmentMenuOpen = false
                    contactPickerOpen = true
                }
                AttachmentAction(stringResource(R.string.gif)) {
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
    if (mediaViewerOpen) {
        DraftMediaViewer(
            attachments = chat.draftAttachments.filter(MessageAttachment::isVisual),
            onDismiss = { mediaViewerOpen = false },
            onApplyExcluded = { excludedIds ->
                excludedIds.forEach(onRemoveAttachment)
                mediaViewerOpen = false
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
private fun MentionSuggestions(people: List<Person>, onSelect: (Person) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column {
            people.forEach { person ->
                ListItem(
                    headlineContent = { Text(person.name) },
                    leadingContent = { ProfileAvatar(person.name, person.avatar, Modifier.size(36.dp), contentDescription = null) },
                    modifier = Modifier.clickable { onSelect(person) },
                )
            }
        }
    }
}

@Composable
private fun DraftAttachmentShelf(
    attachments: List<MessageAttachment>,
    onPreview: () -> Unit,
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
            .height(112.dp)
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(attachments, key = MessageAttachment::id) { attachment ->
            val removeDescription = stringResource(R.string.remove_attachment, attachment.label)
            Surface(
                modifier = Modifier
                    .height(96.dp)
                    .widthIn(min = 104.dp, max = 160.dp)
                    .then(if (attachment.isVisual()) Modifier.clickable(onClick = onPreview) else Modifier),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box {
                    attachment.images.firstOrNull()?.let { image ->
                        ComposerImage(image, Modifier.fillMaxSize())
                    } ?: Text(
                        attachment.label,
                        modifier = Modifier.align(Alignment.Center).padding(12.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(
                        onClick = { onRemove(attachment.id) },
                        modifier = Modifier.align(Alignment.TopEnd).background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            CircleShape,
                        ),
                    ) {
                        Text(
                            "×",
                            modifier = Modifier.semantics {
                                contentDescription = removeDescription
                            },
                        )
                    }
                }
            }
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            image?.let { ComposerImage(it, Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))) }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(domain, style = MaterialTheme.typography.labelMedium)
                Text(summary, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRemove) {
                Text(
                    "×",
                    modifier = Modifier.semantics {
                        contentDescription = removeDescription
                    },
                )
            }
        }
    }
}

@Composable
private fun DraftReplyQuote(author: String, text: String, onCancel: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(author, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(
                onClick = onCancel,
                modifier = Modifier.semantics { contentDescription = "Cancel reply" },
            ) {
                Text("×", modifier = Modifier.clearAndSetSemantics { })
            }
        }
    }
}

@Composable
private fun RecordingComposer(seconds: Int, onCancel: () -> Unit, onStop: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("▂▄▆█▆▄▂▄▆", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
        Text("0:${seconds.coerceAtMost(59).toString().padStart(2, '0')}")
        TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        Button(onClick = onStop) { Text(stringResource(R.string.stop_recording)) }
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.voice_message_review), style = MaterialTheme.typography.titleLarge)
            Text("▂▄▆█▆▄▂▄▆█▆▄▂", style = MaterialTheme.typography.titleLarge)
            Text("0:08", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!hasTranscript) {
                TextButton(
                    onClick = {
                        hasTranscript = true
                        formatName = VoiceMessageFormat.Both.name
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text(stringResource(R.string.transcribe)) }
            } else {
                Box {
                    TextButton(onClick = { formatMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${stringResource(R.string.message_format)}: ${format.label} ▾")
                    }
                    DropdownMenu(expanded = formatMenuOpen, onDismissRequest = { formatMenuOpen = false }) {
                        VoiceMessageFormat.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(if (option == format) "✓ ${option.label}" else option.label) },
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
                        minLines = 3,
                        maxLines = 8,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.discard))
                }
                Button(
                    onClick = { onSend(if (hasTranscript) format else VoiceMessageFormat.Voice, transcript) },
                    enabled = sendEnabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(
                            when (if (hasTranscript) format else VoiceMessageFormat.Voice) {
                                VoiceMessageFormat.Voice -> R.string.send_voice_message
                                VoiceMessageFormat.Text -> R.string.send_text_message
                                VoiceMessageFormat.Both -> R.string.send_voice_and_text
                            },
                        ),
                    )
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
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(stringResource(R.string.choose_contact), Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge)
            people.take(12).forEach { person ->
                ListItem(
                    headlineContent = { Text(person.name) },
                    leadingContent = { ProfileAvatar(person.name, person.avatar, Modifier.size(44.dp), contentDescription = null) },
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
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(stringResource(R.string.choose_gif), Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge)
            gifs.forEach { (asset, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    leadingContent = {
                        Image(painterResource(asset.drawableResource), null, Modifier.size(52.dp), contentScale = ContentScale.Crop)
                    },
                    modifier = Modifier.clickable { onSelect(asset, label) },
                )
            }
        }
    }
}

@Composable
private fun DraftMediaViewer(
    attachments: List<MessageAttachment>,
    onDismiss: () -> Unit,
    onApplyExcluded: (Set<String>) -> Unit,
) {
    if (attachments.isEmpty()) return
    var includedIds by remember(attachments) { mutableStateOf(attachments.map { it.id }.toSet()) }
    val pagerState = rememberPagerState { attachments.size }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(vertical = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Text(stringResource(R.string.preview_media), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = {
                        onApplyExcluded(attachments.map { it.id }.toSet() - includedIds)
                    }) { Text(stringResource(R.string.done)) }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    pageSpacing = 32.dp,
                ) { page ->
                    val attachment = attachments[page]
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        attachment.images.firstOrNull()?.let {
                            ComposerImage(it, Modifier.fillMaxSize().padding(16.dp))
                        } ?: Text(attachment.label)
                        val included = attachment.id in includedIds
                        TextButton(
                            onClick = {
                                includedIds = if (included) includedIds - attachment.id else includedIds + attachment.id
                            },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                        ) {
                            Text(if (included) "✓ ${stringResource(R.string.include_media)}" else stringResource(R.string.exclude_media))
                        }
                    }
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(76.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(attachments, key = MessageAttachment::id) { attachment ->
                        attachment.images.firstOrNull()?.let {
                            ComposerImage(it, Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentAction(label: String, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(label) }, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick))
}

@Composable
internal fun ComposerImage(image: ProfileAvatar, modifier: Modifier = Modifier) {
    when (image) {
        is ProfileAvatar.Asset -> Image(
            painterResource(image.asset.drawableResource),
            null,
            modifier,
            contentScale = ContentScale.Crop,
        )
        is ProfileAvatar.WebImage -> Image(
            painterResource(image.asset.drawableResource),
            null,
            modifier,
            contentScale = ContentScale.Crop,
        )
        is ProfileAvatar.DeviceImage -> {
            val bitmap = remember(image) {
                BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)?.asImageBitmap()
            }
            if (bitmap == null) Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
            else Image(bitmap, null, modifier, contentScale = ContentScale.Fit)
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

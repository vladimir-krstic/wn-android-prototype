package dev.ipf.whitenoise.ui.conversation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.ChatMessage
import dev.ipf.whitenoise.model.ConversationMediaItem
import dev.ipf.whitenoise.model.ConversationMediaKey
import dev.ipf.whitenoise.model.MessageAttachment
import dev.ipf.whitenoise.model.MessageAttachmentKind
import dev.ipf.whitenoise.model.MessageDocuments
import dev.ipf.whitenoise.model.NostrEventCard
import dev.ipf.whitenoise.model.NostrEventKind
import dev.ipf.whitenoise.model.NostrEventReference
import dev.ipf.whitenoise.model.NostrEventState
import dev.ipf.whitenoise.model.NostrProfileOccurrence
import dev.ipf.whitenoise.model.Person
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.shortenedReference
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

@Composable
internal fun NostrEventCards(
    message: ChatMessage,
    profile: Profile,
    onRetry: (referenceId: String, revision: Int) -> Unit,
    onOpenPerson: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (message.nostrEvents.isEmpty()) return
    val context = LocalContext.current
    var openedCard by remember(message.id) { mutableStateOf<NostrEventCard?>(null) }
    var openedReference by remember(message.id) { mutableStateOf<String?>(null) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ConversationMessageMetrics.RichContentSpacing),
    ) {
        message.nostrEvents.take(3).forEach { reference ->
            key(reference.id) {
                NostrEventCardSurface(
                    reference = reference,
                    profile = profile,
                    onRetry = { onRetry(reference.id, reference.revision) },
                    onCopy = { copyEventReference(context, reference.authoredReference) },
                    onOpen = { card ->
                        openedCard = card
                        openedReference = reference.authoredReference
                    },
                )
            }
        }
    }
    val card = openedCard
    if (card != null) {
        when (card.kind) {
            NostrEventKind.Article,
            NostrEventKind.Document,
            -> NostrArticleReader(
                card = card,
                profile = profile,
                onOpenPerson = onOpenPerson,
                onDismiss = { openedCard = null; openedReference = null },
            )
            NostrEventKind.Video -> NostrLocalVideoViewer(
                card = card,
                onDismiss = { openedCard = null; openedReference = null },
            )
            else -> NostrEventDetails(
                card = card,
                reference = openedReference.orEmpty(),
                profile = profile,
                onDismiss = { openedCard = null; openedReference = null },
            )
        }
    }
}

@Composable
private fun NostrEventCardSurface(
    reference: NostrEventReference,
    profile: Profile,
    onRetry: () -> Unit,
    onCopy: () -> Unit,
    onOpen: (NostrEventCard) -> Unit,
) {
    val status = reference.state.statusText()
    val content = LocalContentColor.current
    Surface(
        color = Color.Transparent,
        contentColor = content,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, content.copy(alpha = 0.28f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("conversation.nostr_event.${reference.id}")
            .semantics { stateDescription = status },
    ) {
        Column(
            modifier = Modifier.padding(WhiteNoiseSpacing.Related),
            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        ) {
            when (val state = reference.state) {
                NostrEventState.Loading -> {
                    Text(stringResource(R.string.nostr_event_loading), style = MaterialTheme.typography.labelLarge)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                is NostrEventState.Loaded -> LoadedNostrEvent(
                    card = state.card,
                    profile = profile,
                    onOpen = { onOpen(state.card) },
                )
                else -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_warning),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(status, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text(
                shortenedReference(reference.authoredReference.removePrefix("nostr:")),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                TextButton(
                    onClick = onCopy,
                    colors = ButtonDefaults.textButtonColors(contentColor = content),
                    modifier = Modifier.testTag("conversation.nostr_event.copy.${reference.id}"),
                ) { Text(stringResource(R.string.nostr_event_copy)) }
                if (reference.canRetry) {
                    TextButton(
                        onClick = onRetry,
                        colors = ButtonDefaults.textButtonColors(contentColor = content),
                        modifier = Modifier.testTag("conversation.nostr_event.retry.${reference.id}"),
                    ) { Text(stringResource(R.string.nostr_retry)) }
                }
            }
        }
    }
}

@Composable
private fun LoadedNostrEvent(card: NostrEventCard, profile: Profile, onOpen: () -> Unit) {
    val author = profile.people.firstOrNull { it.publicKey == card.authorPublicKey }?.displayName
        ?: stringResource(R.string.nostr_event_unknown_author)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(card.kind.icon()), contentDescription = null, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(author, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                Text(stringResource(card.kind.label()), style = MaterialTheme.typography.labelSmall)
            }
            TextButton(
                onClick = onOpen,
                colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
                modifier = Modifier.testTag("conversation.nostr_event.open"),
            ) { Text(stringResource(card.kind.openLabel())) }
        }
        card.image?.let {
            ComposerImage(
                image = it,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
        card.title?.takeIf(String::isNotBlank)?.let {
            Text(it, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
        }
        Text(card.summary, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        if (card.metadata.isNotEmpty()) {
            Text(card.metadata.joinToString(" · "), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun UnavailableNostrProfileDialog(
    occurrence: NostrProfileOccurrence,
    onDismiss: () -> Unit,
) {
    var attempts by rememberSaveable(occurrence.publicKey) { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nostr_profile_unavailable_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                Text(
                    stringResource(
                        if (attempts == 0) R.string.nostr_profile_unavailable_body else R.string.nostr_profile_retry_failed,
                    ),
                )
                Text(
                    shortenedReference(occurrence.encodedReference),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (attempts > 0) {
                    Text(
                        stringResource(R.string.nostr_profile_checked_again),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { attempts++ }, modifier = Modifier.testTag("nostr_profile.retry")) {
                Text(stringResource(R.string.nostr_retry))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun NostrArticleReader(
    card: NostrEventCard,
    profile: Profile,
    onOpenPerson: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val body = card.readerBody ?: card.summary
    val document = remember(body) { MessageDocuments.parse(body) }
    var unavailableProfile by rememberSaveable(card.title, card.authorPublicKey) {
        mutableStateOf<NostrProfileOccurrence?>(null)
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().testTag("nostr_event.reader"),
            topBar = {
                TopAppBar(
                    title = { Text(card.title ?: stringResource(R.string.nostr_event_article)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                        }
                    },
                )
            },
        ) { padding ->
            AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(WhiteNoiseSpacing.CompactScreenMargin),
                    verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
                ) {
                    Text(
                        profile.people.firstOrNull { it.publicKey == card.authorPublicKey }?.displayName
                            ?: stringResource(R.string.nostr_event_unknown_author),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    MessageDocumentContent(
                        document,
                        profile.people,
                        onOpenPerson,
                        onOpenProfileReference = { occurrence ->
                            val person = profile.people.firstOrNull { it.publicKey == occurrence.publicKey }
                            if (person != null) onOpenPerson(person.id) else unavailableProfile = occurrence
                        },
                    )
                }
            }
        }
    }
    unavailableProfile?.let { occurrence ->
        UnavailableNostrProfileDialog(occurrence) { unavailableProfile = null }
    }
}

@Composable
private fun NostrLocalVideoViewer(card: NostrEventCard, onDismiss: () -> Unit) {
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    val title = card.title ?: stringResource(R.string.nostr_event_video)
    val message = remember(title) { ChatMessage("nostr-video-message", "nostr-author", 0, "Today", 0, "Now") }
    val attachment = remember(title) {
        MessageAttachment("nostr-video", MessageAttachmentKind.Video, title, durationSeconds = 8)
    }
    val item = remember(message, attachment) {
        ConversationMediaItem(
            ConversationMediaKey(message.id, attachment.id, 0),
            message,
            attachment,
            null,
            "",
            "",
        )
    }
    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().testTag("nostr_event.video_viewer")) {
            MediaViewerVideo(
                item = item,
                active = true,
                controlsVisible = controlsVisible,
                bottomControlsInset = 0.dp,
                onToggleControls = { controlsVisible = !controlsVisible },
                onShowControls = { controlsVisible = true },
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart).padding(WhiteNoiseSpacing.CompactScreenMargin)) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.close), tint = Color.White)
            }
        }
    }
}

@Composable
private fun NostrEventDetails(
    card: NostrEventCard,
    reference: String,
    profile: Profile,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(card.kind.icon()), contentDescription = null) },
        title = { Text(card.title ?: stringResource(card.kind.label())) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                card.image?.let {
                    ComposerImage(it, Modifier.fillMaxWidth().height(160.dp), androidx.compose.ui.layout.ContentScale.Crop)
                }
                Text(card.summary)
                if (card.metadata.isNotEmpty()) Text(card.metadata.joinToString(" · "), style = MaterialTheme.typography.labelMedium)
                Text(
                    profile.people.firstOrNull { it.publicKey == card.authorPublicKey }?.displayName
                        ?: stringResource(R.string.nostr_event_unknown_author),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(shortenedReference(reference.removePrefix("nostr:")), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun NostrEventState.statusText(): String = stringResource(
    when (this) {
        NostrEventState.Loading -> R.string.nostr_event_loading
        is NostrEventState.Loaded -> R.string.nostr_event_loaded
        NostrEventState.NotFound -> R.string.nostr_event_not_found
        NostrEventState.Invalid -> R.string.nostr_event_invalid
        NostrEventState.Unavailable -> R.string.nostr_event_unavailable
        NostrEventState.Failed -> R.string.nostr_event_failed
    },
)

private fun NostrEventKind.label(): Int = when (this) {
    NostrEventKind.Note -> R.string.nostr_event_note
    NostrEventKind.Article -> R.string.nostr_event_article
    NostrEventKind.Image -> R.string.nostr_event_image
    NostrEventKind.Video -> R.string.nostr_event_video
    NostrEventKind.Document -> R.string.nostr_event_document
    NostrEventKind.Event -> R.string.nostr_event_generic
}

private fun NostrEventKind.openLabel(): Int = when (this) {
    NostrEventKind.Article,
    NostrEventKind.Document,
    -> R.string.nostr_event_read
    NostrEventKind.Video -> R.string.nostr_event_play
    else -> R.string.nostr_event_open
}

private fun NostrEventKind.icon(): Int = when (this) {
    NostrEventKind.Image -> R.drawable.ic_image
    NostrEventKind.Video -> R.drawable.ic_play_arrow
    NostrEventKind.Article,
    NostrEventKind.Document,
    -> R.drawable.ic_description
    else -> R.drawable.ic_link
}

private fun copyEventReference(context: Context, reference: String) {
    val value = if (reference.startsWith("nostr:", ignoreCase = true)) reference else "nostr:$reference"
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(ClipData.newPlainText("Event reference", value))
}

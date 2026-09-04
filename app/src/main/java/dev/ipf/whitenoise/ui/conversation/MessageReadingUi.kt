package dev.ipf.whitenoise.ui.conversation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.ConfigurationCompat
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal data class MessageReadingActions(
    val collapse: Boolean = true,
    val canWrite: Boolean = true,
    val open: (String) -> Unit = {},
    val history: (String) -> Unit = {},
    val retry: (String) -> Unit = {},
    val discard: (String) -> Unit = {},
)
internal val LocalMessageReading = staticCompositionLocalOf { MessageReadingActions() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageEditDialog(profileId: String, message: ChatMessage, onDismiss: () -> Unit, onSave: (String, Int) -> Boolean) {
    var value by rememberSaveable(profileId, message.id, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(message.text)) }
    val baseRevision = rememberSaveable(profileId, message.id) { message.editHistory?.revisions?.size ?: 0 }
    var unavailable by rememberSaveable { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(Modifier.fillMaxSize().testTag("message.edit"), contentWindowInsets = WindowInsets.safeDrawing,
            topBar = { TopAppBar(title = { Text(stringResource(R.string.message_edit_title)) }, navigationIcon = {
                IconButton(onClick = onDismiss) { Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.cancel)) }
            }, actions = {
                TextButton(onClick = { if (onSave(value.text, baseRevision)) onDismiss() else unavailable = true },
                    enabled = MessageEditing.canSave(message, value.text) && !message.isDeleted) { Text(stringResource(R.string.save)) }
            }) }) { padding ->
            AdaptiveContent(Modifier.fillMaxSize().padding(padding).imePadding()) {
                Column(Modifier.fillMaxSize().padding(WhiteNoiseSpacing.CompactScreenMargin), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                    if (unavailable) Text(stringResource(R.string.message_edit_unavailable), color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    TextField(value = value, onValueChange = { value = it; unavailable = false }, modifier = Modifier.fillMaxWidth().weight(1f).testTag("message.edit.input"),
                        label = { Text(stringResource(R.string.message)) }, textStyle = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
internal fun MessageEditStatus(message: ChatMessage) {
    val actions = LocalMessageReading.current
    val attempt = message.editAttempt
    val ink = LocalContentColor.current
    val buttonColors = ButtonDefaults.textButtonColors(contentColor = ink)
    if (attempt != null) Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("message.edit.status.${message.id}")) {
        if (attempt.phase == MessageEditPhase.Pending) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = ink, trackColor = ink.copy(alpha = 0.2f))
            Text(stringResource(R.string.message_edit_saving), style = MaterialTheme.typography.labelMedium)
        } else {
            Text(stringResource(when (attempt.failure) {
                MessageEditFailure.Unavailable -> R.string.message_edit_unavailable
                MessageEditFailure.Interrupted -> R.string.message_edit_interrupted
                else -> R.string.message_edit_failed
            }), style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                TextButton(onClick = { actions.retry(message.id) }, enabled = actions.canWrite, colors = buttonColors) { Text(stringResource(R.string.message_retry_edit)) }
                TextButton(onClick = { actions.discard(message.id) }, colors = buttonColors) { Text(stringResource(R.string.message_discard_edit)) }
            }
        }
    }
    if (message.editHistory != null) TextButton(onClick = { actions.history(message.id) }, modifier = Modifier.testTag("message.edit.history.${message.id}"), colors = buttonColors) {
        Text(stringResource(R.string.message_edited), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun revisionTime(value: Long): String {
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.ROOT
    val zone = ZoneId.systemDefault()
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale).withZone(zone).format(Instant.ofEpochMilli(value)) + " (${zone.id})"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageEditHistoryDialog(message: ChatMessage, onDismiss: () -> Unit) {
    val history = message.editHistory ?: return
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(Modifier.fillMaxSize().testTag("message.history"), contentWindowInsets = WindowInsets.safeDrawing, topBar = {
            TopAppBar(title = { Text(stringResource(R.string.message_edit_history)) }, navigationIcon = {
                IconButton(onClick = onDismiss) { Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back)) }
            })
        }) { padding -> AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(WhiteNoiseSpacing.CompactScreenMargin),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField)) {
                val rows = history.revisions.mapIndexed { index, revision -> Triple(stringResource(R.string.message_revision, index + 1), revision.text, revision.timestampMillis) }.reversed() +
                    Triple(stringResource(R.string.message_original), history.original, history.originalTimestampMillis)
                rows.forEach { (label, text, time) ->
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    Text(revisionTime(time), style = MaterialTheme.typography.labelMedium)
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        SelectionContainer { Text(text, Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin)) }
                    }
                }
            }
        } }
    }
}

private data object MessageSelectionSpeechKey
private data object MessageSelectionFromHereKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageReaderDialog(profile: Profile, chat: Chat, message: ChatMessage, initialSelection: Boolean,
    speech: MessageSpeechActionState, readAloud: ReadAloudController, onDismiss: () -> Unit,
    onAction: (MessageAction) -> Unit, onReact: () -> Unit, onPerson: (String) -> Unit) {
    ReadAloudModal(readAloud)
    val selection = rememberSelectionState()
    var chooser by remember { mutableStateOf(false) }
    val document = remember(message.text) { MessageDocuments.parse(message.text) }
    val passage = selectedMessagePassage(message.text, selection.selectedTexts)
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    fun leave() { if (selection.selectedTexts.isNotEmpty()) selection.clear() else onDismiss() }
    fun speak() { selectedMessagePassage(message.text, selection.selectedTexts)?.let { readAloud.speakPassage(profile, chat, message.id, it) } }
    fun fromHere() { selectedMessagePassage(message.text, selection.selectedTexts)?.let {
        readAloud.startConversation(profile, chat, message.id, sourceOffset = it.sourceStart); selection.clear()
    } }
    LaunchedEffect(selection.selectedTexts.isNotEmpty()) { if (selection.selectedTexts.isNotEmpty()) readAloud.follow(false) }
    LaunchedEffect(message.text) { selection.clear() }
    if (chooser) SpeechSentenceChooser(message, onRead = { offset ->
        chooser = false; selection.clear(); readAloud.startConversation(profile, chat, message.id, sourceOffset = offset)
    }, onDismiss = { chooser = false })
    LaunchedEffect(Unit) { if (initialSelection) { repeat(2) { withFrameNanos { } }; selection.extendSelectionByWord() } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, decorFitsSystemWindows = false)) {
        BackHandler(onBack = ::leave)
        Scaffold(Modifier.fillMaxSize().testTag("message.reader"), contentWindowInsets = WindowInsets.safeDrawing, topBar = {
            TopAppBar(title = { Column {
                Text(if (message.authorId == profile.id) profile.name else profile.people.firstOrNull { it.id == message.authorId }?.displayName ?: chat.title,
                    style = MaterialTheme.typography.titleMedium)
                Text(message.timeLabel, style = MaterialTheme.typography.labelSmall)
            } }, navigationIcon = { IconButton(onClick = ::leave) { Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back)) } }, actions = {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.message_actions)) }
                    val actions = MessageActionPolicy.available(message, profile.id, speech, chat.composerAvailability(profile) == ComposerAvailability.Available)
                        .filterNot { it == MessageAction.OpenMessage || it == MessageAction.Select }
                    WhiteNoiseDropdownMenu(expanded = menu, onDismissRequest = { menu = false }, items = actions.map { action ->
                        WhiteNoiseMenuItem(label = actionLabel(action), icon = actionIcon(action), onClick = {
                            menu = false
                            if (action == MessageAction.SelectText) selection.extendSelectionByWord() else onAction(action)
                        })
                    } + if (chat.composerAvailability(profile) == ComposerAvailability.Available) listOf(WhiteNoiseMenuItem(
                        label = stringResource(R.string.message_react), icon = R.drawable.ic_add, onClick = { menu = false; onReact() })) else emptyList())
                }
            })
        }, bottomBar = {
            ReadAloudReaderBar {
                ReadAloudTransport(readAloud, onReturn = { onDismiss(); readAloud.returnToSource() }, onResumeFollowing = {})
                TextButton(onClick = { chooser = true }, enabled = readAloud.ready, modifier = Modifier.testTag("speech.choose")) {
                    Text(stringResource(R.string.speech_choose_sentence))
                }
                if (passage != null) FlowRow(horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                    TextButton(onClick = { (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("Message", passage.text)) }, modifier = Modifier.testTag("message.selection.copy")) {
                        Text(stringResource(R.string.message_selection_copy))
                    }
                    TextButton(onClick = ::fromHere, enabled = readAloud.ready, modifier = Modifier.testTag("message.selection.fromHere")) {
                        Text(stringResource(R.string.speech_from_here))
                    }
                    TextButton(onClick = ::speak, enabled = readAloud.ready, modifier = Modifier.testTag("message.selection.speak")) {
                        Text(stringResource(R.string.message_selection_read))
                    }
                }
            }
        }) { padding -> AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            val speakLabel = stringResource(R.string.read_aloud)
            val fromHereLabel = stringResource(R.string.speech_from_here)
            SelectionContainer(state = selection, modifier = Modifier.fillMaxSize().appendTextContextMenuComponents {
                if (readAloud.ready) {
                    separator(); item(MessageSelectionSpeechKey, speakLabel) { speak(); close() }
                    item(MessageSelectionFromHereKey, fromHereLabel) { fromHere(); close() }
                }
            }) {
                MessageDocumentContent(document, profile.people, onPerson,
                    Modifier.fillMaxSize().observeSpeechScroll(readAloud).verticalScroll(rememberScrollState()).padding(WhiteNoiseSpacing.CompactScreenMargin), annotateSource = true,
                    spokenRange = readAloud.session?.takeIf { it.owner == SpeechOwner(profile.id, chat.id) && it.current.item.id == message.id && it.current.item.authored == message.text }
                        ?.passage?.let { it.sourceStart until it.sourceEnd }, followSpeech = readAloud.session?.following == true && passage == null)
            }
        } }
    }
}

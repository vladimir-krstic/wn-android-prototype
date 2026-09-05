@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.ipf.whitenoise.ui.chats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

@Composable
internal fun ChatSelectionBar(onClose: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.select_chats)) },
        navigationIcon = { IconButton(onClick = onClose) { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.chat_selection_close)) } },
    )
}

@Composable
internal fun ChatSelectionBottomBar(selected: List<Chat>, onSelectAll: () -> Unit, onAction: (ChatBulkAction) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val archive = ChatOrganization.archiveAction(selected)
    Row(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin, vertical = WhiteNoiseSpacing.Related)
            .testTag("chats.selectionControls"),
        horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            FilledTonalIconButton(onClick = { menu = true }) {
                Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.more_options))
            }
            WhiteNoiseDropdownMenu(expanded = menu, onDismissRequest = { menu = false }, items =
                listOf(ChatBulkAction.Read, ChatBulkAction.Unread, archive, ChatBulkAction.Folder, ChatBulkAction.Delete).map { action ->
                    WhiteNoiseMenuItem(label = stringResource(action.labelResource), onClick = { menu = false; onAction(action) }, destructive = action == ChatBulkAction.Delete)
                })
        }
        Surface(
            modifier = Modifier.weight(1f).testTag("chats.selectionCount").semantics { liveRegion = LiveRegionMode.Polite },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Text(
                pluralStringResource(R.plurals.selected_count, selected.size, selected.size),
                modifier = Modifier.padding(WhiteNoiseSpacing.Related),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            FilledTonalButton(onClick = onSelectAll) {
                Text(stringResource(R.string.chat_select_all), textAlign = TextAlign.Center)
            }
        }
    }
}

internal val ChatBulkAction.labelResource get() = when (this) {
    ChatBulkAction.Read -> R.string.mark_read
    ChatBulkAction.Unread -> R.string.mark_unread
    ChatBulkAction.Archive -> R.string.archive
    ChatBulkAction.Unarchive -> R.string.unarchive
    ChatBulkAction.Folder -> R.string.chat_add_folder
    ChatBulkAction.Delete -> R.string.delete
}

@Composable
internal fun ChatDeleteConfirmation(chats: List<Chat>, onDismiss: () -> Unit, onConfirm: (Boolean) -> Unit) {
    var leave by rememberSaveable { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(pluralStringResource(R.plurals.chat_delete_count, chats.size, chats.size)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                Text(stringResource(R.string.chat_delete_local_detail))
                if (chats.any { it.membership == ChatMembership.Active && it.groupLifecycle != GroupLifecycle.Disbanded }) {
                    Row(Modifier.fillMaxWidth().toggleable(leave, role = Role.Checkbox, onValueChange = { leave = it }), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(leave, null)
                        Text(stringResource(R.string.chat_delete_also_leave), Modifier.weight(1f))
                    }
                    Text(stringResource(if (leave) R.string.chat_delete_leave_detail else R.string.chat_delete_stay_detail), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(leave) }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun ChatFolderPicker(profile: Profile, onDismiss: () -> Unit, onCreate: (String) -> String?, onSelect: (String) -> Unit, errorMessage: String? = null) {
    val name = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState() }
    var creating by rememberSaveable { mutableStateOf(profile.chatFolders.isEmpty()) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_add_folder)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                if (errorMessage != null) Text(errorMessage, color = MaterialTheme.colorScheme.error)
                profile.chatFolders.forEach { folder ->
                    TextButton(onClick = { onSelect(folder.id) }, modifier = Modifier.fillMaxWidth()) { Text(folder.name) }
                }
                if (creating) WhiteNoiseTextField(name, Modifier.fillMaxWidth().testTag("chat.folderName"),
                    label = { Text(stringResource(R.string.chat_folder_name)) }, lineLimits = TextFieldLineLimits.SingleLine)
                else TextButton(onClick = { creating = true }) { Text(stringResource(R.string.chat_new_folder)) }
            }
        },
        confirmButton = {
            if (creating) TextButton(enabled = name.text.isNotBlank(), onClick = { onCreate(name.text.toString())?.let(onSelect) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun ChatBatchProgress(attempt: ChatBatchAttempt, onAdvance: (Long, Int, ChatBatchPhase) -> Boolean,
    onDismiss: () -> Unit, onRetry: () -> Unit, onOpenGroup: (String) -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(attempt.id, attempt.index, attempt.phase, lifecycle) {
        if (attempt.isBusy) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(250)
            onAdvance(attempt.id, attempt.index, attempt.phase)
        }
    }
    AlertDialog(onDismissRequest = { if (!attempt.isBusy) onDismiss() },
        title = { Text(stringResource(if (attempt.isBusy) R.string.chat_updating else if (attempt.failedIds.isNotEmpty()) R.string.chat_action_incomplete else R.string.chat_action_results)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                if (attempt.isBusy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(when (attempt.phase) {
                        ChatBatchPhase.Leaving -> R.string.chat_leaving_progress
                        ChatBatchPhase.Deleting -> R.string.chat_deleting_progress
                        else -> R.string.chat_applying_progress
                    }, attempt.index + 1, attempt.targets.size))
                } else {
                    Text(pluralStringResource(R.plurals.chat_action_result_count, attempt.results.size, attempt.completedCount, attempt.results.size))
                    attempt.results.filter { it.failure != null }.forEach { result ->
                        Text(result.title.ifBlank { stringResource(R.string.chat_unavailable_title) }, style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(when (result.failure) {
                            ChatBatchFailure.NeedsAdmin -> R.string.chat_delete_admin_required
                            ChatBatchFailure.LeaveFailed -> R.string.chat_delete_leave_failed
                            ChatBatchFailure.DeleteFailed -> if (result.leftBeforeDeletion) R.string.chat_delete_after_leave_failed else R.string.chat_delete_local_failed
                            else -> R.string.chat_action_failed
                        }), color = MaterialTheme.colorScheme.error)
                        if (result.failure == ChatBatchFailure.NeedsAdmin) TextButton(onClick = { onDismiss(); onOpenGroup(result.chatId) }) { Text(stringResource(R.string.chat_open_group)) }
                    }
                }
            }
        },
        confirmButton = { if (!attempt.isBusy) TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
        dismissButton = { if (!attempt.isBusy && attempt.failedIds.isNotEmpty()) TextButton(onClick = onRetry) { Text(stringResource(R.string.people_retry)) } },
    )
}

@Composable
internal fun ChatConnectionBanner(profile: Profile, onRetry: () -> Unit, onRelays: () -> Unit) {
    val phase = profile.chatConnection.phase
    val roleState = ProfileRelayFixtures.availability(profile.settings.relays, RelayRole.ChatMessages)
    val relayIssue = profile.chatRelayUrls.isEmpty() || roleState != RelayRoleAvailability.Available
    if (phase == ChatConnectionPhase.Online && !relayIssue) return
    dev.ipf.whitenoise.ui.components.WhiteNoiseCallout(
        modifier = Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin).testTag("chats.connection")
            .semantics { liveRegion = LiveRegionMode.Polite },
        icon = if (relayIssue || phase == ChatConnectionPhase.Offline || phase == ChatConnectionPhase.Failed)
            R.drawable.ic_warning else R.drawable.ic_info,
    ) {
        if (phase != ChatConnectionPhase.Online) {
            Text(stringResource(when (phase) {
                ChatConnectionPhase.Offline -> R.string.chat_connection_offline
                ChatConnectionPhase.Connecting -> R.string.chat_connection_connecting
                ChatConnectionPhase.CatchingUp -> R.string.chat_connection_catching_up
                else -> R.string.chat_connection_failed
            }))
            if (phase == ChatConnectionPhase.Connecting || phase == ChatConnectionPhase.CatchingUp) LinearProgressIndicator(Modifier.fillMaxWidth())
            else TextButton(onClick = onRetry) { Text(stringResource(R.string.people_retry)) }
        }
        if (relayIssue) {
            Text(stringResource(if (roleState == RelayRoleAvailability.Unassigned || profile.chatRelayUrls.isEmpty()) R.string.chat_relays_unassigned else R.string.chat_relays_unavailable))
            TextButton(onClick = onRelays) { Text(stringResource(R.string.chat_check_relays)) }
        }
    }
}

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.ipf.whitenoise.ui.chats

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseEmptyState
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

@Composable
internal fun PeopleSearchFeedback(profile: Profile, status: PeopleSearchStatus, resolving: Boolean, onRetry: () -> Unit) {
    val context = LocalContext.current
    val inviteText = stringResource(R.string.people_invite_text, profile.name, "https://whitenoise.chat/${profile.publicKey}")
    val inviteTitle = stringResource(R.string.people_invite)
    var shareFailed by remember { mutableStateOf(false) }
    val textId = when {
        resolving -> R.string.people_resolving
        else -> when (status) {
            PeopleSearchStatus.Ready -> null
            PeopleSearchStatus.InvalidIdentifier -> R.string.people_invalid_identifier
            PeopleSearchStatus.AddressNotFound -> R.string.people_address_not_found
            PeopleSearchStatus.NoResults -> R.string.no_results_detail
            PeopleSearchStatus.NoProfile -> R.string.people_no_profile
            PeopleSearchStatus.Partial -> R.string.people_partial
            PeopleSearchStatus.Unavailable -> R.string.people_unavailable
        }
    }
    if (textId != null) Column(
        modifier = Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin).testTag("people.search_status"),
        verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
    ) {
        if (resolving) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (!resolving && status == PeopleSearchStatus.NoResults) {
            WhiteNoiseEmptyState(title = stringResource(R.string.no_results), detail = stringResource(textId), modifier = Modifier.fillMaxWidth())
        } else Text(stringResource(textId), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!resolving && status in setOf(PeopleSearchStatus.Partial, PeopleSearchStatus.Unavailable, PeopleSearchStatus.AddressNotFound)) {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.people_retry)) }
        }
        if (!resolving && status in setOf(PeopleSearchStatus.NoResults, PeopleSearchStatus.AddressNotFound, PeopleSearchStatus.NoProfile)) {
            TextButton(onClick = {
                shareFailed = runCatching {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, inviteText)
                    }
                    context.startActivity(Intent.createChooser(intent, inviteTitle))
                }.isFailure
            }) { Text(stringResource(R.string.people_invite)) }
            if (shareFailed) Text(stringResource(R.string.people_share_unavailable), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun PrivateContactDialog(person: Person, onDismiss: () -> Unit, onSave: (String, String) -> Boolean) {
    val nickname = remember(person.id) { TextFieldState(person.nickname) }
    val notes = remember(person.id) { TextFieldState(person.privateNotes) }
    var failed by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.contact_private_details)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField)) {
                Text(stringResource(R.string.contact_published_name, person.name))
                WhiteNoiseTextField(nickname, Modifier.fillMaxWidth().testTag("contact.nickname"), label = { Text(stringResource(R.string.contact_nickname)) }, lineLimits = TextFieldLineLimits.SingleLine,
                    supportingText = { Text(stringResource(R.string.contact_nickname_limit)) })
                WhiteNoiseTextField(notes, Modifier.fillMaxWidth().testTag("contact.notes"), label = { Text(stringResource(R.string.contact_notes)) }, lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 6))
                Text(stringResource(R.string.contact_private_hint), style = MaterialTheme.typography.bodySmall)
                if (failed) Text(stringResource(R.string.contact_save_failed), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = { if (onSave(nickname.text.toString(), notes.text.toString())) onDismiss() else failed = true }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun ContactGroupsSheet(
    profile: Profile, person: Person, action: GroupContactAction,
    scenario: GroupContactScenario, onDismiss: () -> Unit, onRetryRoster: () -> Unit,
    onApply: (List<String>, GroupContactAction) -> GroupContactResult,
) {
    var selected by rememberSaveable(profile.id, person.id, action) { mutableStateOf(emptyList<String>()) }
    var resolving by remember(profile.id, person.id, action, scenario) { mutableStateOf(true) }
    var retry by remember { mutableIntStateOf(0) }
    var confirm by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<GroupContactResult?>(null) }
    val query = rememberSaveable(profile.id, person.id, action, saver = TextFieldState.Saver) { TextFieldState() }
    LaunchedEffect(profile.id, person.id, action, scenario, retry) { resolving = true; delay(600); resolving = false }
    val unresolved = GroupContactPolicy.unresolved(profile, scenario)
    val eligible = GroupContactPolicy.eligible(profile, person.id, action).filterNot { it.id in unresolved }
    val allowed = eligible.map(Chat::id).toSet()
    LaunchedEffect(allowed) { selected = selected.filter { it in allowed } }
    val groups = eligible.filter { it.title.contains(query.text.toString().trim(), true) }
    val title = stringResource(if (action == GroupContactAction.Invite) R.string.contact_add_groups else R.string.contact_promote_groups)
    WhiteNoiseModalBottomSheet(onDismissRequest = onDismiss) {
        WhiteNoiseSheetHeader(title = title, onClose = onDismiss)
        LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false), contentPadding = PaddingValues(horizontal = WhiteNoiseSpacing.CompactScreenMargin)) {
            item {
                WhiteNoiseTextField(query, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.contact_find_groups)) }, lineLimits = TextFieldLineLimits.SingleLine)
            }
            if (resolving) item {
                Column(Modifier.padding(vertical = WhiteNoiseSpacing.FormField)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.contact_roster_loading))
                }
            } else {
                if (unresolved.isNotEmpty()) item {
                    Text(stringResource(R.string.contact_roster_partial), Modifier.padding(top = WhiteNoiseSpacing.FormField))
                    TextButton(onClick = { onRetryRoster(); retry++ }) { Text(stringResource(R.string.people_retry)) }
                }
                if (groups.isEmpty()) item { Text(stringResource(R.string.contact_no_eligible_groups), Modifier.padding(vertical = WhiteNoiseSpacing.FormField)) }
                items(groups, key = Chat::id) { group ->
                    ListItem(
                        leadingContent = { Checkbox(checked = group.id in selected, onCheckedChange = null) },
                        modifier = Modifier.fillMaxWidth().testTag("contact.group.${group.id}").toggleable(
                            value = group.id in selected, role = Role.Checkbox,
                            onValueChange = { checked -> selected = if (checked) selected + group.id else selected - group.id },
                        ),
                    ) { Text(group.title) }
                }
            }
            result?.let { outcome -> item {
                Text(stringResource(R.string.contact_group_result, outcome.completed.size, outcome.failed.size), Modifier.padding(vertical = WhiteNoiseSpacing.FormField), color = if (outcome.failed.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
            } }
        }
        WhiteNoiseButton(
            onClick = { confirm = true }, enabled = !resolving && selected.any { it in allowed },
            modifier = Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.PinnedActionInset).testTag("contact.groups.apply"),
        ) { Text(title) }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(title) },
        text = { Text(pluralStringResource(if (action == GroupContactAction.Invite) R.plurals.contact_invite_confirm else R.plurals.contact_promote_confirm, selected.size, person.displayName, selected.size)) },
        confirmButton = { TextButton(onClick = {
            confirm = false
            val outcome = onApply(selected.filter { it in allowed }, action)
            result = outcome
            selected = outcome.failed
            if (outcome.failed.isEmpty()) onDismiss()
        }) { Text(stringResource(R.string.contact_confirm)) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun CreatedChatOpenDialog(onOpen: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.created_chat_title)) },
        text = { Text(stringResource(R.string.created_chat_detail)) },
        confirmButton = { TextButton(onClick = onOpen) { Text(stringResource(R.string.created_chat_open)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatMessage
import dev.ipf.whitenoise.model.MessageAction
import dev.ipf.whitenoise.model.MessageActionPolicy
import dev.ipf.whitenoise.model.MessageDeletionScope
import dev.ipf.whitenoise.model.MessageDeliveryState
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ReactionCatalog
import dev.ipf.whitenoise.model.composerAvailability
import dev.ipf.whitenoise.model.ComposerAvailability
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.ProfileAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageActionsSheet(
    profile: Profile,
    message: ChatMessage,
    onDismiss: () -> Unit,
    onReaction: (String, Boolean) -> Unit,
    onMoreReactions: () -> Unit,
    onAction: (MessageAction) -> Unit,
) {
    val selectedReaction = message.reactions.firstOrNull { profile.id in it.personIds }?.emoji
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ReactionCatalog.quickStrip(profile.quickReactions, selectedReaction).forEach { emoji ->
                    FilterChip(
                        selected = emoji == selectedReaction,
                        onClick = { onReaction(emoji, emoji == selectedReaction) },
                        label = { Text(emoji) },
                    )
                }
            }
            TextButton(onClick = onMoreReactions, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.more_reactions))
            }
            MessageActionPolicy.available(message, profile.id).forEach { action ->
                ListItem(
                    headlineContent = { Text(actionLabel(action)) },
                    modifier = Modifier.clickable { onAction(action) },
                )
            }
        }
    }
}

@Composable
internal fun actionLabel(action: MessageAction): String = stringResource(
    when (action) {
        MessageAction.RetrySend -> R.string.retry_send
        MessageAction.Reply -> R.string.reply
        MessageAction.Forward -> R.string.forward
        MessageAction.Copy -> R.string.copy
        MessageAction.Select -> R.string.select
        MessageAction.Info -> R.string.info
        MessageAction.Delete -> R.string.delete
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EmojiPickerSheet(
    onDismiss: () -> Unit,
    onEmoji: (String) -> Unit,
    onConfigure: (() -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ReactionCatalog.categories.keys.first()) }
    val values = if (query.isBlank()) ReactionCatalog.categories.getValue(category) else ReactionCatalog.all
    val filtered = values.filter { query.isBlank() || it.contains(query) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.search_emoji)) },
                    singleLine = true,
                )
                onConfigure?.let { configure ->
                    TextButton(onClick = configure) { Text(stringResource(R.string.configure_reactions)) }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ReactionCatalog.categories.keys.forEach { name ->
                            FilterChip(
                                selected = category == name,
                                onClick = { category = name; query = "" },
                                label = { Text(name.take(1)) },
                            )
                        }
                    }
                }
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        modifier = Modifier.fillMaxWidth().size(360.dp),
                        userScrollEnabled = false,
                    ) {
                        items(filtered) { emoji ->
                            TextButton(onClick = { onEmoji(emoji) }, contentPadding = PaddingValues(0.dp)) {
                                Text(emoji, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfigureReactionsSheet(
    current: List<String>,
    onDismiss: () -> Unit,
    onApply: (List<String>) -> Unit,
    onPickSlot: (Int, List<String>) -> Unit,
) {
    var draft by remember(current) { mutableStateOf(current) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.configure_reactions), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.configure_reactions_guidance))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                draft.forEachIndexed { index, emoji ->
                    TextButton(onClick = { onPickSlot(index, draft) }) { Text(emoji, style = MaterialTheme.typography.titleLarge) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { draft = ReactionCatalog.defaults }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.reset))
                }
                Button(onClick = { onApply(draft) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.done))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ForwardMessagesSheet(
    profile: Profile,
    sourceChatId: String,
    onDismiss: () -> Unit,
    onForward: (List<String>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    val chats = profile.chats.filter {
        it.id != sourceChatId && it.composerAvailability(profile) == ComposerAvailability.Available &&
            (query.isBlank() || it.title.contains(query, ignoreCase = true))
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Text(stringResource(R.string.forward), Modifier.padding(20.dp), style = MaterialTheme.typography.titleLarge)
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.search_chats)) },
                singleLine = true,
            )
            LazyColumn(Modifier.weight(1f, fill = false)) {
                items(chats, key = Chat::id) { chat ->
                    val checked = chat.id in selected
                    ListItem(
                        headlineContent = { Text(chat.title) },
                        supportingContent = { Text(chat.displayPreview, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { ProfileAvatar(chat.title, chat.avatar, Modifier.size(42.dp), contentDescription = null) },
                        trailingContent = { Text(if (checked) "✓" else "○") },
                        modifier = Modifier.clickable {
                            selected = if (checked) selected - chat.id else if (selected.size < 5) selected + chat.id else selected
                        },
                    )
                }
            }
            Button(
                onClick = { onForward(selected.toList()) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(
                    if (selected.size <= 1) stringResource(R.string.forward)
                    else pluralStringResource(R.plurals.forward_to_chats, selected.size, selected.size),
                )
            }
        }
    }
}

@Composable
internal fun DeleteMessagesDialog(
    messages: List<ChatMessage>,
    profileId: String,
    onDismiss: () -> Unit,
    onDelete: (MessageDeletionScope) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (messages.size == 1) stringResource(R.string.delete_message_question)
                else pluralStringResource(R.plurals.delete_selected_question, messages.size, messages.size),
            )
        },
        text = { Text(stringResource(R.string.delete_for_me_explanation)) },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (MessageActionPolicy.canDeleteForEveryone(messages, profileId)) {
                    TextButton(onClick = { onDelete(MessageDeletionScope.ForEveryone) }) {
                        Text(stringResource(R.string.delete_for_everyone), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = { onDelete(MessageDeletionScope.ForMe) }) {
                    Text(stringResource(R.string.delete_for_me), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun SelectionBottomBar(
    selectedCount: Int,
    canForward: Boolean,
    onDelete: () -> Unit,
    onForward: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onDelete, enabled = selectedCount > 0) {
                Text(stringResource(R.string.delete_selected_messages))
            }
            Text(pluralStringResource(R.plurals.selected_count, selectedCount, selectedCount), fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onForward, enabled = canForward) {
                Text(stringResource(R.string.forward_selected_messages))
            }
        }
    }
}

@Composable
internal fun SearchResultsBottomBar(
    count: Int,
    current: Int,
    onOlder: () -> Unit,
    onNewer: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onOlder, enabled = count > 0 && current < count - 1) {
                Text(stringResource(R.string.previous_match))
            }
            Text(
                if (count == 0) stringResource(R.string.matches_zero)
                else pluralStringResource(R.plurals.match_position, count, current + 1, count),
            )
            OutlinedButton(onClick = onNewer, enabled = count > 0 && current > 0) {
                Text(stringResource(R.string.next_match))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailsScreen(
    profile: Profile,
    chat: Chat,
    message: ChatMessage,
    onBack: () -> Unit,
) {
    val outgoing = message.authorId == profile.id
    val sender = profile.people.firstOrNull { it.id == message.authorId }?.name ?: chat.title
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                    }
                },
                title = { Text(stringResource(R.string.message_details)) },
            )
        },
    ) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (message.text.isNotBlank()) Text(message.text)
                            TimelineAttachmentContent(message.attachments, outgoing, onOpenMedia = {})
                        }
                    }
                }
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(if (outgoing) R.string.sent else R.string.received)) },
                        supportingContent = { Text("${message.dayLabel}, ${message.timeLabel}") },
                    )
                }
                if (!outgoing) {
                    item { ListItem(headlineContent = { Text(stringResource(R.string.sent_from)) }, supportingContent = { Text(sender) }) }
                } else {
                    val recipients = if (chat.isGroup) {
                        chat.members.filterNot { it.personId == profile.id }.mapNotNull { member ->
                            profile.people.firstOrNull { it.id == member.personId }?.name
                        }
                    } else listOf(chat.title)
                    items(recipients) { name ->
                        ListItem(
                            headlineContent = { Text(name) },
                            supportingContent = {
                                Text(
                                    stringResource(
                                        when (message.deliveryState) {
                                            MessageDeliveryState.Sending -> R.string.sending
                                            MessageDeliveryState.Failed -> R.string.not_delivered
                                            MessageDeliveryState.Sent -> R.string.sent
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet as ModalBottomSheet
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
import dev.ipf.whitenoise.ui.components.WhiteNoiseScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatMessage
import dev.ipf.whitenoise.model.ComposerAvailability
import dev.ipf.whitenoise.model.MessageAction
import dev.ipf.whitenoise.model.MessageActionPolicy
import dev.ipf.whitenoise.model.MessageDeletionScope
import dev.ipf.whitenoise.model.MessageDeliveryState
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.model.ReactionCatalog
import dev.ipf.whitenoise.model.composerAvailability
import dev.ipf.whitenoise.model.visibleText
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.components.ProfileAvatar
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing

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
    val actions = MessageActionPolicy.available(message, profile.id)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        WhiteNoiseSheetHeader(stringResource(R.string.message_actions))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(bottom = WhiteNoiseSpacing.CompactScreenMargin),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                MessageContextPreview(profile, message)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                    horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) {
                    items(
                        ReactionCatalog.quickStrip(profile.quickReactions, selectedReaction),
                        key = { it },
                    ) { emoji ->
                        FilterChip(
                            selected = emoji == selectedReaction,
                            onClick = { onReaction(emoji, emoji == selectedReaction) },
                            label = {
                                Text(emoji, style = MaterialTheme.typography.titleMedium)
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                    item {
                        FilledTonalButton(
                            onClick = onMoreReactions,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = null,
                                modifier = Modifier.padding(end = WhiteNoiseSpacing.Related).size(18.dp),
                            )
                            Text(stringResource(R.string.more_reactions))
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 420.dp),
                ) {
                    items(actions, key = { it.name }) { action ->
                        val destructive = action == MessageAction.Delete
                        ListItem(
                            headlineContent = {
                                Text(
                                    actionLabel(action),
                                    color = if (destructive) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(actionIcon(action)),
                                    contentDescription = null,
                                    tint = if (destructive) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onAction(action) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageContextPreview(profile: Profile, message: ChatMessage) {
    val outgoing = message.authorId == profile.id
    val person = profile.people.firstOrNull { it.id == message.authorId }
    val authorName = if (outgoing) stringResource(R.string.you) else person?.name
        ?: stringResource(R.string.unknown_person)
    val avatar = if (outgoing) profile.avatar else person?.avatar
    val summary = message.visibleText(profile.id).ifBlank {
        message.attachments.firstOrNull()?.label.orEmpty()
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        ) {
            avatar?.let {
                ProfileAvatar(authorName, it, Modifier.size(40.dp), contentDescription = null)
            }
            Column(Modifier.weight(1f)) {
                Text(authorName, style = MaterialTheme.typography.labelLarge)
                Text(
                    summary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

private fun actionIcon(action: MessageAction): Int = when (action) {
    MessageAction.RetrySend -> R.drawable.ic_refresh
    MessageAction.Reply -> R.drawable.ic_reply
    MessageAction.Forward -> R.drawable.ic_forward
    MessageAction.Copy -> R.drawable.ic_content_copy
    MessageAction.Select -> R.drawable.ic_check
    MessageAction.Info -> R.drawable.ic_info
    MessageAction.Delete -> R.drawable.ic_delete
}

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
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.search_emoji)) },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.ic_search), contentDescription = null)
                        },
                        trailingIcon = if (query.isNotEmpty()) ({
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    painterResource(R.drawable.ic_close),
                                    contentDescription = stringResource(R.string.clear_search),
                                )
                            }
                        }) else null,
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                    onConfigure?.let { configure ->
                        FilledTonalIconButton(onClick = configure) {
                            Icon(
                                painterResource(R.drawable.ic_tune),
                                contentDescription = stringResource(R.string.configure_reactions),
                            )
                        }
                    }
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
                    horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) {
                    items(ReactionCatalog.categories.keys.toList(), key = { it }) { name ->
                        FilterChip(
                            selected = category == name && query.isBlank(),
                            onClick = {
                                category = name
                                query = ""
                            },
                            label = { Text(name) },
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.no_emoji_found),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 48.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(min = 160.dp, max = 440.dp),
                        contentPadding = PaddingValues(
                            horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                            vertical = WhiteNoiseSpacing.Related,
                        ),
                    ) {
                        items(filtered, key = { it }) { emoji ->
                            TextButton(
                                onClick = { onEmoji(emoji) },
                                modifier = Modifier.size(48.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
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
        WhiteNoiseSheetHeader(stringResource(R.string.configure_reactions))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = WhiteNoiseSpacing.Section).padding(bottom = WhiteNoiseSpacing.Section),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
            ) {
                Text(
                    stringResource(R.string.configure_reactions_guidance),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) {
                    itemsIndexed(draft, key = { index, _ -> index }) { index, emoji ->
                        val description = stringResource(R.string.reaction_slot_description, index + 1, emoji)
                        Surface(
                            onClick = { onPickSlot(index, draft) },
                            modifier = Modifier.size(56.dp).semantics {
                                contentDescription = description
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                ) {
                    TextButton(
                        onClick = { draft = ReactionCatalog.defaults },
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    ) {
                        Text(stringResource(R.string.reset))
                    }
                    WhiteNoiseButton(
                        onClick = { onApply(draft) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.done))
                    }
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
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
            ) {
                WhiteNoiseSheetHeader(stringResource(R.string.forward))
                Text(
                    stringResource(R.string.forward_selection_limit),
                    modifier = Modifier.padding(horizontal = WhiteNoiseSpacing.Section),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                            vertical = WhiteNoiseSpacing.FormField,
                        ),
                    placeholder = { Text(stringResource(R.string.search_chats)) },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_search), contentDescription = null)
                    },
                    trailingIcon = if (query.isNotEmpty()) ({
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.clear_search),
                            )
                        }
                    }) else null,
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (chats.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(stringResource(R.string.no_results), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.no_results_detail),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 420.dp),
                    ) {
                        items(chats, key = Chat::id) { chat ->
                            val checked = chat.id in selected
                            val enabled = checked || selected.size < 5
                            ListItem(
                                headlineContent = { Text(chat.title) },
                                supportingContent = {
                                    Text(
                                        chat.displayPreview,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                leadingContent = {
                                    ProfileAvatar(
                                        chat.title,
                                        chat.avatar,
                                        Modifier.size(48.dp),
                                        contentDescription = null,
                                    )
                                },
                                trailingContent = {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = null,
                                        enabled = enabled,
                                        modifier = Modifier.clearAndSetSemantics { },
                                    )
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = if (checked) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    },
                                ),
                                modifier = Modifier
                                    .alpha(if (enabled) 1f else 0.38f)
                                    .toggleable(
                                        value = checked,
                                        enabled = enabled,
                                        role = Role.Checkbox,
                                        onValueChange = {
                                            selected = if (checked) selected - chat.id else selected + chat.id
                                        },
                                    ),
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin)) {
                        if (selected.isNotEmpty()) {
                            Text(
                                pluralStringResource(
                                    R.plurals.selected_count,
                                    selected.size,
                                    selected.size,
                                ),
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = WhiteNoiseSpacing.Related),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        WhiteNoiseButton(
                            onClick = { onForward(selected.toList()) },
                            enabled = selected.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (selected.size <= 1) {
                                    stringResource(R.string.forward)
                                } else {
                                    pluralStringResource(
                                        R.plurals.forward_to_chats,
                                        selected.size,
                                        selected.size,
                                    )
                                },
                            )
                        }
                    }
                }
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
                if (messages.size == 1) {
                    stringResource(R.string.delete_message_question)
                } else {
                    pluralStringResource(
                        R.plurals.delete_selected_question,
                        messages.size,
                        messages.size,
                    )
                },
            )
        },
        text = { Text(stringResource(R.string.delete_for_me_explanation)) },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (MessageActionPolicy.canDeleteForEveryone(messages, profileId)) {
                    TextButton(onClick = { onDelete(MessageDeletionScope.ForEveryone) }) {
                        Text(
                            stringResource(R.string.delete_for_everyone),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = { onDelete(MessageDeletionScope.ForMe) }) {
                    Text(stringResource(R.string.delete_for_me), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
internal fun SelectionBottomBar(
    selectedCount: Int,
    canForward: Boolean,
    onDelete: () -> Unit,
    onForward: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(WhiteNoiseSpacing.Related),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            ) {
                IconButton(onClick = onDelete, enabled = selectedCount > 0) {
                    Icon(
                        painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.delete_selected_messages),
                        tint = if (selectedCount > 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                    )
                }
                Text(
                    pluralStringResource(R.plurals.selected_count, selectedCount, selectedCount),
                    modifier = Modifier.weight(1f).semantics {
                        liveRegion = LiveRegionMode.Polite
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                FilledTonalIconButton(onClick = onForward, enabled = canForward) {
                    Icon(
                        painterResource(R.drawable.ic_forward),
                        contentDescription = stringResource(R.string.forward_selected_messages),
                    )
                }
            }
            if (selectedCount > 0 && !canForward) {
                Text(
                    stringResource(R.string.forward_selection_unavailable),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = WhiteNoiseSpacing.Related),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
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
    val countLabel = if (count == 0) {
        stringResource(R.string.matches_zero)
    } else {
        pluralStringResource(R.plurals.match_position, count, current + 1, count)
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .testTag("conversation.searchControls")
                .padding(
                    horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                    vertical = WhiteNoiseSpacing.Related,
                ),
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = onOlder,
                enabled = count > 0 && current < count - 1,
            ) {
                Icon(
                    painterResource(R.drawable.ic_arrow_up),
                    contentDescription = stringResource(R.string.previous_match),
                )
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .testTag("conversation.searchCount")
                    .semantics { liveRegion = LiveRegionMode.Polite },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Text(
                    countLabel,
                    modifier = Modifier.padding(
                        horizontal = WhiteNoiseSpacing.CompactScreenMargin,
                        vertical = 10.dp,
                    ),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            FilledTonalIconButton(
                onClick = onNewer,
                enabled = count > 0 && current > 0,
            ) {
                Icon(
                    painterResource(R.drawable.ic_arrow_down),
                    contentDescription = stringResource(R.string.next_match),
                )
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
    val sender = profile.people.firstOrNull { it.id == message.authorId }
    val senderName = sender?.name ?: chat.title
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                scrollBehavior = dev.ipf.whitenoise.ui.components.LocalWhiteNoiseHeaderScroll.current,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                title = { Text(stringResource(R.string.message_details)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        AdaptiveContent(Modifier.fillMaxSize().padding(padding)) {
            dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn(
                contentPadding = PaddingValues(WhiteNoiseSpacing.CompactScreenMargin),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.FormField),
            ) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(WhiteNoiseSpacing.CompactScreenMargin),
                            verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
                        ) {
                            if (message.text.isNotBlank()) {
                                Text(message.text, style = MaterialTheme.typography.bodyLarge)
                            }
                            TimelineAttachmentContent(message.attachments, outgoing, onOpenMedia = {})
                        }
                    }
                }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column {
                            ListItem(
                                headlineContent = {
                                    Text(stringResource(if (outgoing) R.string.sent else R.string.received))
                                },
                                supportingContent = { Text("${message.dayLabel}, ${message.timeLabel}") },
                                leadingContent = {
                                    Icon(painterResource(R.drawable.ic_info), contentDescription = null)
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            )
                            if (!outgoing) {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.sent_from)) },
                                    supportingContent = { Text(senderName) },
                                    leadingContent = {
                                        if (sender != null) {
                                            ProfileAvatar(
                                                sender.name,
                                                sender.avatar,
                                                Modifier.size(40.dp),
                                                contentDescription = null,
                                            )
                                        } else {
                                            Icon(
                                                painterResource(R.drawable.ic_person),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                )
                            } else {
                                val recipients = if (chat.isGroup) {
                                    chat.members.filterNot { it.personId == profile.id }.mapNotNull { member ->
                                        profile.people.firstOrNull { it.id == member.personId }
                                    }
                                } else {
                                    profile.people.firstOrNull { it.id == chat.id }?.let(::listOf).orEmpty()
                                }
                                recipients.forEach { person ->
                                    ListItem(
                                        headlineContent = { Text(person.name) },
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
                                        leadingContent = {
                                            ProfileAvatar(
                                                person.name,
                                                person.avatar,
                                                Modifier.size(40.dp),
                                                contentDescription = null,
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    )
                                }
                                if (recipients.isEmpty()) {
                                    ListItem(
                                        headlineContent = { Text(chat.title) },
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
                                        leadingContent = {
                                            Icon(
                                                painterResource(R.drawable.ic_person),
                                                contentDescription = null,
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

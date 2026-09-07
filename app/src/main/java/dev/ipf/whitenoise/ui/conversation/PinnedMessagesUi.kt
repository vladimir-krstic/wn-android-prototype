package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import dev.ipf.whitenoise.ui.theme.amoledOutlineBorder

@Composable
private fun pinSnippet(entry: PinnedMessageEntry): String = when (entry.status) {
    PinnedMessageStatus.Deleted -> stringResource(R.string.pin_message_deleted)
    PinnedMessageStatus.Unavailable -> stringResource(R.string.pin_message_unavailable)
    PinnedMessageStatus.Available -> entry.message?.let { MessageDocuments.plainText(it.text) }
        ?.takeIf { it.isNotBlank() } ?: stringResource(R.string.pin_message_attachment)
}

@Composable
internal fun PinnedMessageBanner(
    profile: Profile, chat: Chat, entry: PinnedMessageEntry, index: Int, count: Int,
    onOpen: () -> Unit, onNext: () -> Unit, onUnpin: () -> Unit, onViewAll: () -> Unit,
) {
    var menuOpen by remember(entry.id) { mutableStateOf(false) }
    var previewFocused by remember { mutableStateOf(false) }
    val position = stringResource(R.string.pin_position, index + 1, count)
    val author = when (entry.message?.authorId) {
        profile.id -> stringResource(R.string.you)
        null -> stringResource(R.string.pinned)
        else -> profile.people.firstOrNull { it.id == entry.message.authorId }?.displayName
            ?: if (!chat.isGroup) chat.title else stringResource(R.string.unknown_person)
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer,
        border = amoledOutlineBorder(), modifier = Modifier.fillMaxWidth().testTag("message.pins.banner")) {
        AdaptiveContent {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("message.pins.preview")
                    .then(if (previewFocused) Modifier.border(1.dp, MaterialTheme.colorScheme.outline) else Modifier)
                    .onFocusChanged { previewFocused = it.isFocused }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onNext)
                    .semantics { stateDescription = position }) {
                    Row(Modifier.padding(horizontal = WhiteNoiseSpacing.Related, vertical = WhiteNoiseSpacing.Related),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                        PinPagination(index, count)
                        Column {
                            Text(author, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(pinSnippet(entry), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Box(Modifier.padding(end = WhiteNoiseSpacing.Related)) {
                    IconButton({ menuOpen = true }, Modifier.testTag("message.pins.menu")) {
                        Icon(painterResource(R.drawable.ic_push_pin), stringResource(R.string.pin_options))
                    }
                    WhiteNoiseDropdownMenu(menuOpen, { menuOpen = false }, items = listOf(
                        WhiteNoiseMenuItem(stringResource(R.string.message_unpin), onUnpin, R.drawable.ic_unpin,
                            enabled = MessagePins.canManage(chat, profile.id), modifier = Modifier.testTag("message.pins.menu.unpin")),
                        WhiteNoiseMenuItem(stringResource(R.string.go_to_message), onOpen, R.drawable.ic_reply,
                            enabled = entry.status == PinnedMessageStatus.Available, modifier = Modifier.testTag("message.pins.menu.go")),
                        WhiteNoiseMenuItem(stringResource(R.string.pin_view_all), onViewAll, R.drawable.ic_format_list_bulleted,
                            modifier = Modifier.testTag("message.pins.menu.all")),
                    ))
                }
            }
        }
    }
}

/** A moving window keeps the segments legible even when a chat has many pins. */
@Composable
private fun PinPagination(index: Int, count: Int) {
    val selected = MaterialTheme.colorScheme.onSurface
    val idle = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.width(2.dp).height(32.dp).testTag("message.pins.pagination")) {
        val visible = count.coerceAtMost(5)
        val first = (index - 2).coerceIn(0, (count - visible).coerceAtLeast(0))
        val gap = 2.dp.toPx()
        val segment = (size.height - gap * (visible - 1)) / visible
        repeat(visible) { slot ->
            val top = slot * (segment + gap)
            drawLine(if (first + slot == index) selected else idle,
                Offset(size.width / 2, top + size.width / 2),
                Offset(size.width / 2, top + segment - size.width / 2), size.width, StrokeCap.Round)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PinnedMessagesSheet(profile: Profile, chat: Chat, onDismiss: () -> Unit, onOpen: (String) -> Unit, onUnpin: (String) -> Unit) {
    val pins = MessagePins.entries(chat)
    val canManage = MessagePins.canManage(chat, profile.id)
    val rows = remember(chat) { ConversationProjection.items(chat).filterIsInstance<ConversationItem.MessageItem>().associateBy { it.id } }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
    WhiteNoiseScaffold(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).testTag("message.pins.sheet"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.pinned_messages), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onDismiss) { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.close)) } },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow))
        },
    ) { padding ->
        AdaptiveContent(Modifier.padding(padding)) {
            WhiteNoiseLazyColumn(Modifier.fillMaxSize().testTag("message.pins.list"),
                contentPadding = PaddingValues(WhiteNoiseSpacing.CompactScreenMargin),
                verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Section)) {
                if (!canManage) item(key = "permission") {
                    Text(stringResource(if (chat.isGroup && chat.messagePinPermission != MessagePinPermission.Disabled && chat.membership == ChatMembership.Active)
                        R.string.pin_admins_only else R.string.pin_permission_disabled),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = WhiteNoiseSpacing.FormField))
                }
                if (pins.isEmpty()) item(key = "empty") {
                    Column(Modifier.padding(vertical = WhiteNoiseSpacing.Section), verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                        Text(stringResource(R.string.pin_empty), style = MaterialTheme.typography.titleMedium)
                        if (canManage) Text(stringResource(R.string.pin_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                itemsIndexed(pins, key = { _, pin -> pin.id }) { _, pin ->
                    Column(verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(pin.message?.dayLabel.orEmpty(), Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (canManage) IconButton(onClick = { onUnpin(pin.id) }, modifier = Modifier.testTag("message.pins.unpin.${pin.id}")) {
                                Icon(painterResource(R.drawable.ic_unpin), stringResource(R.string.message_unpin))
                            }
                        }
                        val item = rows[pin.id]
                        if (pin.status == PinnedMessageStatus.Available && item != null) {
                            val description = listOf(pinSnippet(pin), pin.message?.attachments?.joinToString { it.label }.orEmpty(),
                                pin.message?.timeLabel.orEmpty()).filter { it.isNotBlank() }.joinToString(", ")
                            Box(Modifier.fillMaxWidth().testTag("message.pins.bubble.${pin.id}")) {
                                Box(Modifier.clearAndSetSemantics { }) {
                                    ReadOnlyMessageBubble(profile, chat, item.copy(startsCluster = true, endsCluster = true))
                                }
                                Box(Modifier.matchParentSize().testTag("message.pins.row.${pin.id}")
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
                                        role = Role.Button, onClickLabel = stringResource(R.string.go_to_message), onClick = { onOpen(pin.id) })
                                    .semantics { contentDescription = description })
                            }
                        } else {
                            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium,
                                border = amoledOutlineBorder(), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(WhiteNoiseSpacing.CompactScreenMargin)
                                    .testTag("message.pins.row.${pin.id}")
                                    .semantics { disabled() }, verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
                                    Text(pinSnippet(pin), style = MaterialTheme.typography.bodyLarge)
                                    Text(stringResource(R.string.pin_unavailable_hint), style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

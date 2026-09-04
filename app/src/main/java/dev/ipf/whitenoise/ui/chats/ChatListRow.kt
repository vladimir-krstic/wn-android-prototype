package dev.ipf.whitenoise.ui.chats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.offset
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.AttachmentPreview
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatListAction
import dev.ipf.whitenoise.model.ChatListActionPolicy
import dev.ipf.whitenoise.model.ChatListPresentation
import dev.ipf.whitenoise.model.ChatListStatus
import dev.ipf.whitenoise.model.DisappearingDuration
import dev.ipf.whitenoise.ui.components.ProfileAvatar

internal val ChatRowHorizontalInset = 8.dp

@Composable
internal fun ChatListRow(chat: Chat, onOpen: () -> Unit, onActions: () -> Unit, onAction: (ChatListAction) -> Unit, highlighted: Boolean = false, availableActions: List<ChatListAction> = ChatListActionPolicy.all(chat), selecting: Boolean = false, checked: Boolean = false) {
    val preview = ChatListPresentation.from(chat)
    val actions = (if (selecting) emptyList() else availableActions).map { action ->
        CustomAccessibilityAction(stringResource(action.labelResource)) { onAction(action); true }
    }
    val actionsDescription = stringResource(R.string.actions_for, chat.title)
    ListItem(
        onClick = onOpen,
        onLongClick = onActions,
        onLongClickLabel = if (selecting) stringResource(if (checked) R.string.chat_deselect else R.string.select) else actionsDescription,
        content = {
            ChatRowTextLayout(
                title = {
                    Text(
                        chat.title,
                        Modifier.testTag("chat.title.${chat.id}"),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                metadata = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (chat.muteDuration != null) {
                            RowStatusIcon(R.drawable.ic_notifications_off, stringResource(R.string.muted), Modifier.testTag("chat.muted.${chat.id}"))
                        }
                        if (chat.disappearingDuration != DisappearingDuration.Off) {
                            RowStatusIcon(R.drawable.ic_timer, stringResource(R.string.disappearing_messages, chat.disappearingDuration.label), Modifier.testTag("chat.timer.${chat.id}"))
                        }
                        if (chat.hasEndedMembership) {
                            RowStatusIcon(R.drawable.ic_logout, chat.visiblePreview, Modifier.testTag("chat.membership.${chat.id}"))
                        }
                    }
                },
                timestamp = {
                    Text(
                        chat.timestamp,
                        Modifier.testTag("chat.timestamp.${chat.id}"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                },
                preview = {
                    Text(
                        buildAnnotatedString {
                            preview.prefix?.let { prefix ->
                                withStyle(SpanStyle(fontWeight = if (preview.isDraft) FontWeight.Normal else FontWeight.SemiBold)) {
                                    append(prefix)
                                    append(": ")
                                }
                            }
                            if (preview.attachment != null) {
                                appendInlineContent("attachment")
                                append(" ")
                            }
                            append(preview.text)
                        },
                        modifier = Modifier.testTag("chat.preview.${chat.id}"),
                        // These are the same roles as Material's supporting slot. The preview
                        // lives in the content slot to avoid its inherited-baseline measurement.
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        inlineContent = preview.attachment?.let { attachment ->
                            mapOf("attachment" to InlineTextContent(Placeholder(1.em, 1.em, PlaceholderVerticalAlign.TextCenter)) {
                                Icon(painterResource(attachment.iconResource), null, Modifier.fillMaxSize(), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            })
                        }.orEmpty(),
                    )
                },
                status = { ChatStatusIndicator(preview.status, Modifier.testTag("chat.status.${chat.id}")) },
            )
        },
        leadingContent = {
            if (selecting) androidx.compose.material3.Checkbox(checked = checked, onCheckedChange = null)
            else Box {
                ProfileAvatar(chat.title, chat.visibleAvatar, Modifier.size(52.dp).testTag("chat.avatar.${chat.id}"), contentDescription = null)
                if (chat.isPinned) {
                    Surface(modifier = Modifier.size(20.dp).align(Alignment.BottomEnd), shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_push_pin), stringResource(R.string.pinned), Modifier.size(14.dp))
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth().testTag("chat.row.${chat.id}")
            .semantics { customActions = actions; selected = highlighted; role = if (selecting) Role.Checkbox else Role.Button
                if (selecting) toggleableState = if (checked) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off },
        // The anchor's outer inset plus this inner padding keep artwork on the shared
        // 16 dp content edge. Native ListItem owns the tighter 12 dp avatar/text gap.
        contentPadding = PaddingValues(
            start = ChatRowHorizontalInset,
            end = ChatRowHorizontalInset,
            top = ListItemDefaults.ContentPadding.calculateTopPadding(),
            bottom = ListItemDefaults.ContentPadding.calculateBottomPadding(),
        ),
        // Hold the native selected shape after the long-press pointer is released.
        // This is an ordinary clickable row, not a radio-button selection control.
        shapes = ListItemDefaults.shapes(
            shape = if (highlighted) ListItemDefaults.shapes().selectedShape else null,
        ),
        colors = ListItemDefaults.colors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
        ),
    )
}

/**
 * Keep the text region in one ListItem content slot. Material alpha25's supporting-slot
 * baseline query can place an unplaced Row/Box subtree during lazy reuse (RectList crash
 * on Compose UI 1.12.0). Only direct Text baselines are read here, never a container's.
 * Native ListItem still owns interaction, padding, avatar spacing, shape and alignment.
 */
@Composable
private fun ChatRowTextLayout(
    title: @Composable () -> Unit,
    metadata: @Composable () -> Unit,
    timestamp: @Composable () -> Unit,
    preview: @Composable () -> Unit,
    status: @Composable () -> Unit,
) {
    val verticalAlignment = ListItemDefaults.verticalAlignment()
    val padding = ListItemDefaults.ContentPadding
    Layout(contents = listOf(title, metadata, timestamp, preview, status)) { slots, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val trailingGap = 8.dp.roundToPx()
        val time = slots[2].single().measure(loose)
        val icons = slots[1].single().measure(loose.offset(horizontal = -time.width - trailingGap))
        val iconGap = if (icons.width > 0) 4.dp.roundToPx() else 0
        val name = slots[0].single().measure(
            loose.offset(horizontal = -time.width - trailingGap - icons.width - iconGap),
        )

        val nameGroupHeight = maxOf(name.height, icons.height)
        val nameInset = (nameGroupHeight - name.height) / 2
        val baseline = maxOf(nameInset + name[FirstBaseline], time[FirstBaseline])
        val groupY = baseline - nameInset - name[FirstBaseline]
        val timeY = baseline - time[FirstBaseline]
        val headlineHeight = maxOf(groupY + nameGroupHeight, timeY + time.height)
        val badge = slots[4].firstOrNull()?.measure(loose.offset(vertical = -headlineHeight))
        val badgeSpace = if (badge == null) 0 else badge.width + trailingGap
        val message = slots[3].single().measure(
            loose.offset(horizontal = -badgeSpace, vertical = -headlineHeight),
        )
        val textHeight = headlineHeight + maxOf(message.height, badge?.height ?: 0)
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.constrainWidth(
            maxOf(name.width + iconGap + icons.width + trailingGap + time.width, message.width + badgeSpace),
        )
        // Match alpha25 ListTokens' two-/three-line minima. Padding and vertical alignment
        // remain native; text can grow beyond these minima at any font or display scale.
        val minimumHeight = if (message[FirstBaseline] != message[LastBaseline]) 88.dp else 72.dp
        val contentMinimum = (minimumHeight - padding.calculateTopPadding() - padding.calculateBottomPadding()).roundToPx()
        val height = constraints.constrainHeight(maxOf(textHeight, contentMinimum))
        val contentY = verticalAlignment.align(textHeight, height)
        layout(width, height) {
            name.placeRelative(0, contentY + groupY + nameInset)
            icons.placeRelative(name.width + iconGap, contentY + groupY + (nameGroupHeight - icons.height) / 2)
            time.placeRelative(width - time.width, contentY + timeY)
            message.placeRelative(0, contentY + headlineHeight)
            badge?.placeRelative(width - badge.width, contentY + headlineHeight)
        }
    }
}

@Composable
private fun ChatStatusIndicator(status: ChatListStatus, modifier: Modifier = Modifier) {
    if (status == ChatListStatus.None) return
    // Material's content badge has a 16 dp minimum and 4 dp horizontal padding.
    // Measure its label line so manual unread, invitations and errors grow together.
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelSize = rememberTextMeasurer().measure("0", labelStyle).size
    val diameter = with(LocalDensity.current) { maxOf(16.dp, labelSize.height.toDp(), labelSize.width.toDp() + 8.dp) }
    when (status) {
        ChatListStatus.Failure -> {
            Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_error),
                    stringResource(R.string.failed_to_send),
                    // The official error symbol's circle occupies 800 of its 960-unit viewport.
                    // Scale the unchanged asset to match the badge's visible circle, not its artboard.
                    Modifier.size(diameter).graphicsLayer {
                        scaleX = 1.2f; scaleY = 1.2f
                    },
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        else -> {
            val count = (status as? ChatListStatus.UnreadCount)?.count
            val description = when {
                status == ChatListStatus.Invitation -> stringResource(R.string.invitation_pending)
                count == null -> stringResource(R.string.manually_unread)
                count > 99 -> stringResource(R.string.unread_count_capped)
                else -> pluralStringResource(R.plurals.unread_count, count, count)
            }
            Box(modifier.semantics { contentDescription = description }) {
                Badge(
                    modifier = Modifier.sizeIn(minWidth = diameter, minHeight = diameter),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    if (count != null) Text(if (count > 99) "99+" else count.toString())
                }
                if (status == ChatListStatus.Invitation) {
                    // Overlay the icon inside the same native badge; its artboard already includes
                    // optical padding, and must not widen the circle through Badge's text padding.
                    Icon(painterResource(R.drawable.ic_add), null, Modifier.matchParentSize(), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun RowStatusIcon(icon: Int, description: String?, modifier: Modifier = Modifier) {
    Icon(
        painterResource(icon), description, modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val AttachmentPreview.iconResource: Int
    get() = when (this) {
        AttachmentPreview.Photo, is AttachmentPreview.Photos, AttachmentPreview.Gif -> R.drawable.ic_image
        AttachmentPreview.Video -> R.drawable.ic_play_arrow
        AttachmentPreview.VoiceMessage -> R.drawable.ic_mic
        is AttachmentPreview.File -> R.drawable.ic_description
        is AttachmentPreview.Contact -> R.drawable.ic_person
        AttachmentPreview.Link -> R.drawable.ic_link
    }

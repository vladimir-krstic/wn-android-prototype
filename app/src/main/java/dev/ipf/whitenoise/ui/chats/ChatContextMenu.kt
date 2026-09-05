package dev.ipf.whitenoise.ui.chats

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatListAction
import dev.ipf.whitenoise.model.ChatListActionPolicy
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem

internal data class ChatMenuTarget(val profileId: String, val chatId: String)

/** Material owns popup placement, focus, scrolling, RTL and dismissal, including system Back. */
@Composable
internal fun ChatContextMenuRow(
    chat: Chat,
    expanded: Boolean,
    onOpen: () -> Unit,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onAction: (ChatListAction) -> Unit,
    availableActions: List<ChatListAction> = ChatListActionPolicy.all(chat),
    selecting: Boolean = false,
    checked: Boolean = false,
) {
    val dismissCurrentMenu by rememberUpdatedState(onDismissMenu)
    DisposableEffect(chat.id) {
        // A lazy row can lose its anchor without the underlying chat being deleted.
        onDispose { dismissCurrentMenu() }
    }
    Box(Modifier.padding(
        horizontal = ChatRowHorizontalInset,
        vertical = if (selecting) WhiteNoiseSpacing.Related / 2 else 0.dp,
    )) {
        ChatListRow(chat, onOpen, onShowMenu, onAction, highlighted = expanded || checked, availableActions = availableActions, selecting = selecting, checked = checked)
        WhiteNoiseDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissMenu,
            anchorSpacing = 8.dp,
            modifier = Modifier.testTag("chat.menu.${chat.id}"),
            items = availableActions.map { action ->
                WhiteNoiseMenuItem(
                    label = stringResource(action.labelResource),
                    icon = action.iconResource,
                    onClick = { onAction(action) },
                    destructive = action.isDestructive,
                    modifier = Modifier.testTag("chat.action.${action.name}"),
                )
            },
        )
    }
}

internal val ChatListAction.labelResource: Int
    get() = when (this) {
        ChatListAction.Read -> R.string.mark_read
        ChatListAction.Unread -> R.string.mark_unread
        ChatListAction.Pin -> R.string.pin
        ChatListAction.Unpin -> R.string.unpin
        ChatListAction.Mute -> R.string.mute
        ChatListAction.Unmute -> R.string.unmute
        ChatListAction.Archive -> R.string.archive
        ChatListAction.Unarchive -> R.string.unarchive
        ChatListAction.Leave -> R.string.leave_group
        ChatListAction.Delete -> R.string.delete_chat
        ChatListAction.Select -> R.string.select
        ChatListAction.MoveUp -> R.string.chat_move_up
        ChatListAction.MoveDown -> R.string.chat_move_down
        ChatListAction.Folder -> R.string.chat_add_folder
    }

internal val ChatListAction.iconResource: Int
    get() = when (this) {
        ChatListAction.Read -> R.drawable.ic_check
        ChatListAction.Unread -> R.drawable.ic_mark_unread
        ChatListAction.Pin -> R.drawable.ic_push_pin
        ChatListAction.Unpin -> R.drawable.ic_unpin
        ChatListAction.Mute -> R.drawable.ic_notifications_off
        ChatListAction.Unmute -> R.drawable.ic_settings_notifications
        ChatListAction.Archive -> R.drawable.ic_archive
        ChatListAction.Unarchive -> R.drawable.ic_unarchive
        ChatListAction.Leave -> R.drawable.ic_logout
        ChatListAction.Delete -> R.drawable.ic_delete
        ChatListAction.Select -> R.drawable.ic_check
        ChatListAction.MoveUp -> R.drawable.ic_arrow_up
        ChatListAction.MoveDown -> R.drawable.ic_arrow_down
        ChatListAction.Folder -> R.drawable.ic_add
    }

package dev.ipf.whitenoise.screenshots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatsScene() {
    Scaffold(
        modifier = Modifier.semantics { paneTitle = "Messages" },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = {},
                        modifier = Modifier.padding(start = Dimens.spaceLg),
                    ) {
                        PhotoAvatar(
                            resource = R.drawable.profile_avatar_marmota,
                            size = 40.dp,
                            contentDescription = "Marmota profile",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter chats")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = "Search chats")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = CanvasGray,
                    ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {},
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                    )
                },
                text = {
                    Text(
                        text = "New chat",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        },
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = CanvasGray,
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            shape =
                RoundedCornerShape(
                    topStart = Dimens.spaceXxl,
                    topEnd = Dimens.spaceXxl,
                ),
            color = SurfaceWhite,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = Dimens.spaceSm),
            ) {
                items(chatFixtures) { chat ->
                    ChatListItem(chat)
                }
            }
        }
    }
}

@Composable
private fun ChatListItem(chat: ChatFixture) {
    ListItem(
        colors = TransparentListItemColors,
        leadingContent = {
            BadgedBox(
                badge = {
                    if (chat.pinned) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                modifier = Modifier.size(11.dp),
                            )
                        }
                    }
                },
            ) {
                PhotoAvatar(
                    resource = chat.avatar,
                    size = 56.dp,
                    contentDescription = chat.name,
                )
            }
        },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (chat.muted) {
                    Icon(
                        Icons.Default.NotificationsOff,
                        contentDescription = "Muted",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Dimens.spaceSm).size(18.dp),
                    )
                }
            }
        },
        supportingContent = {
            Text(
                text =
                    buildAnnotatedString {
                        chat.previewPrefix?.let { prefix ->
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                append(prefix)
                                append(" ")
                            }
                        }
                        append(chat.preview)
                    },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = chat.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                chat.unread?.let {
                    Spacer(Modifier.size(Dimens.spaceSm))
                    UnreadBadge(it)
                }
            }
        },
    )
}

@Composable
private fun UnreadBadge(value: String) {
    Badge(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Text(value, fontWeight = FontWeight.Bold)
    }
}

package dev.ipf.whitenoise.screenshots

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Image as ImageIcon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationScene(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PhotoAvatar(
                            resource = R.drawable.avatar_fiatjaf,
                            size = 40.dp,
                            contentDescription = "Fiatjaf",
                        )
                        Spacer(Modifier.width(Dimens.spaceSm))
                        Text(
                            text = "Fiatjaf",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More conversation options")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = CanvasGray,
                    ),
            )
        },
        bottomBar = {
            ComposerBar()
        },
        containerColor = CanvasGray,
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            color = SurfaceWhite,
            shape =
                RoundedCornerShape(
                    topStart = Dimens.spaceXxl,
                    topEnd = Dimens.spaceXxl,
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Dimens.spaceLg, vertical = Dimens.spaceSm),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            ) {
                item { DayMarker() }
                item {
                    TextMessageWithTime(
                        text = "I’m moving from Feather to White Noise.",
                        outgoing = true,
                        time = "18:31",
                    )
                }
                item {
                    TextMessageWithTime(
                        text = "Let me know how it goes.",
                        outgoing = false,
                        time = "18:32",
                    )
                }
                item {
                    TextMessageWithTime(
                        text = "Signing in now.\nI’ll send a test next.",
                        outgoing = true,
                        time = "18:33",
                    )
                }
                item {
                    TextMessageWithTime(
                        text = "Switched from Feather to White Noise. Same key, same contacts.",
                        outgoing = true,
                        time = "18:36",
                    )
                }
                item { IncomingReplyMessage() }
                item { ReactedOutgoingMessage() }
                item {
                    TextMessageWithTime(
                        text = "Perfect!",
                        outgoing = false,
                        time = "18:45",
                    )
                }
                item { MediaMessage() }
            }
        }
    }
}

@Composable
private fun DayMarker() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spaceMd),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Today",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MessagePlacement(
    outgoing: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (outgoing) Alignment.End else Alignment.Start,
        content = content,
    )
}

@Composable
private fun TextMessageWithTime(
    text: String,
    outgoing: Boolean,
    time: String,
) {
    MessagePlacement(outgoing = outgoing) {
        Column(
            horizontalAlignment = if (outgoing) Alignment.Start else Alignment.End,
        ) {
            TextMessageBubble(
                text = text,
                outgoing = outgoing,
            )
            MessageTime(
                value = time,
                alignment = if (outgoing) Alignment.Start else Alignment.End,
            )
        }
    }
}

@Composable
private fun TextMessageBubble(
    text: String,
    outgoing: Boolean,
) {
    Surface(
        color =
            if (outgoing) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        contentColor =
            if (outgoing) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        shape =
            if (outgoing) {
                RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
            } else {
                RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
            },
        modifier = Modifier.widthIn(max = 340.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = Dimens.spaceMd, vertical = Dimens.spaceSm),
        )
    }
}

@Composable
private fun IncomingReplyMessage() {
    MessagePlacement(outgoing = false) {
        Column(
            modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(6.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                modifier = Modifier.size(width = 3.dp, height = 36.dp),
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                            ) {}
                            Column(Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = "Marmota",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Switched from Feather to White Noise. Same key, same contacts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                    Text(
                        text = "Yep, I still see you on Primal. No extra setup on my side.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = Dimens.spaceSm),
                    )
                }
            }
            MessageTime("18:37", Alignment.End)
        }
    }
}

@Composable
private fun ReactedOutgoingMessage() {
    MessagePlacement(outgoing = true) {
        Column(
            modifier = Modifier.widthIn(max = 340.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Box(modifier = Modifier.padding(bottom = 14.dp)) {
                TextMessageBubble(
                    text = "Exactly. Moved apps, kept everything. Didn’t have to re-add anyone.",
                    outgoing = true,
                )
                Surface(
                    shape = CircleShape,
                    color = SurfaceWhite,
                    border =
                        BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                        ),
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = Dimens.spaceSm, y = 14.dp)
                            .height(22.dp),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = Dimens.spaceSm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "🔥",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            MessageTime("18:44", Alignment.Start)
        }
    }
}

@Composable
private fun MediaMessage() {
    MessagePlacement(outgoing = false) {
        Column(
            modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(4.dp)) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(164.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(0.82f),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            CollageImage(R.drawable.fiatjaf_media_sloth, Modifier.weight(1f))
                            CollageImage(R.drawable.fiatjaf_media_badger, Modifier.weight(1f))
                            CollageImage(R.drawable.fiatjaf_media_ostrich, Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            CollageImage(R.drawable.fiatjaf_media_fox, Modifier.weight(1.05f))
                            CollageImage(R.drawable.fiatjaf_media_marmot, Modifier.weight(0.95f))
                        }
                    }
                    Text(
                        text = "Portable identity for the win.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = Dimens.spaceSm, vertical = 6.dp),
                    )
                }
            }
            MessageTime("12:29", Alignment.End)
        }
    }
}

@Composable
private fun CollageImage(
    resource: Int,
    modifier: Modifier,
) {
    Image(
        painter = painterResource(resource),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun ColumnScope.MessageTime(
    value: String,
    alignment: Alignment.Horizontal,
) {
    val startInset = if (alignment == Alignment.Start) Dimens.spaceMd else 0.dp
    val endInset = if (alignment == Alignment.End) Dimens.spaceMd else 0.dp

    Text(
        text = value,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .align(alignment)
                .padding(
                    start = startInset,
                    top = Dimens.spaceXxs,
                    end = endInset,
                    bottom = Dimens.spaceXxs,
                ),
    )
}

@Composable
private fun ComposerBar() {
    Surface(
        color = SurfaceWhite,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Dimens.spaceMd, vertical = Dimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddCircleOutline,
                            contentDescription = "Add attachment",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Text(
                        text = "Message",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEmotions,
                            contentDescription = "Emoji",
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ImageIcon,
                            contentDescription = "Add photo",
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(Dimens.spaceSm))
            FilledTonalIconButton(
                onClick = {},
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Voice message",
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R

/** The bubble announces attribution; selection exposes this label separately. */
@Composable
internal fun ForwardedMessageLabel(messageId: String, selectingText: Boolean, containerColor: Color, modifier: Modifier = Modifier) {
    val contentColor = LocalContentColor.current
    val color = remember(containerColor, contentColor) {
        dev.ipf.whitenoise.ui.theme.forwardedLabelColor(containerColor, contentColor)
    }
    Row(
        modifier = modifier.testTag("message.forwarded.$messageId")
            .then(if (selectingText) Modifier else Modifier.clearAndSetSemantics {}),
        horizontalArrangement = Arrangement.spacedBy(ConversationMessageMetrics.ForwardedLabelGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_forward), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(
            stringResource(R.string.message_forwarded),
            style = MaterialTheme.typography.labelMedium,
            fontStyle = FontStyle.Italic,
            color = color,
        )
    }
}

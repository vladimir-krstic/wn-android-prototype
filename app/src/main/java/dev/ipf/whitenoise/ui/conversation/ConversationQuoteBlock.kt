package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R

@Composable
internal fun ConversationQuoteBlock(
    author: String,
    excerpt: String,
    containerColor: Color,
    contentColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    shape: Shape = MaterialTheme.shapes.medium,
    modifier: Modifier = Modifier,
    testTagPrefix: String,
    cancelDescription: String? = null,
    onCancel: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.testTag("$testTagPrefix.container"),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(
                        start = 12.dp,
                        top = 8.dp,
                        end = if (onCancel == null) 12.dp else 48.dp,
                        bottom = 8.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(accentColor)
                        .testTag("$testTagPrefix.bar"),
                )
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        author,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        excerpt,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor,
                    )
                }
            }
            if (onCancel != null && cancelDescription != null) {
                ComposerAccessoryRemoveButton(
                    onClick = onCancel,
                    description = cancelDescription,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}

@Composable
internal fun ComposerAccessoryRemoveButton(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    highContrast: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val background = if (highContrast) {
        MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val content = if (highContrast) {
        MaterialTheme.colorScheme.inverseOnSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = description }
            .testTag("conversation.composer.remove.target"),
        contentAlignment = Alignment.TopEnd,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp, end = 6.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(background)
                .indication(interactionSource, ripple(radius = 10.dp))
                .testTag("conversation.composer.remove.visual"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = content,
            )
        }
    }
}

package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R

private val IdentifierCapsuleWidth = 240.dp

@Composable
internal fun IdentityQrCodeSurface(
    value: String,
    availableWidth: Dp,
    contentDescription: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val matrixSize = (availableWidth * 0.81f).coerceIn(248.dp, 376.dp) - 16.dp
    Surface(
        modifier = modifier
            .size(matrixSize + 24.dp)
            .testTag(testTag),
        shape = MaterialTheme.shapes.large,
        color = Color.White,
    ) {
        ProfileCode(
            value = value,
            modifier = Modifier.padding(12.dp),
            contentDescription = contentDescription,
            marginModules = 0,
        )
    }
}

@Composable
internal fun IdentifierCopyCapsule(
    value: String,
    copied: Boolean,
    onCopy: () -> Unit,
    copyContentDescription: String,
    copiedContentDescription: String,
    notCopiedStateDescription: String,
    copiedStateDescription: String,
    targetTestTag: String,
    visualTestTag: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val actionDescription = if (copied) copiedContentDescription else copyContentDescription
    Box(
        modifier = modifier
            .widthIn(max = 360.dp)
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onCopy,
            )
            .testTag(targetTestTag)
            .semantics {
                role = Role.Button
                contentDescription = actionDescription
                stateDescription = if (copied) {
                    copiedStateDescription
                } else {
                    notCopiedStateDescription
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .width(IdentifierCapsuleWidth)
                .testTag(visualTestTag)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .indication(interactionSource, ripple())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                style = MaterialTheme.typography.bodySmall,
            )
            Icon(
                painter = painterResource(if (copied) R.drawable.ic_check else R.drawable.ic_content_copy),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

package dev.ipf.whitenoise.ui.conversation

import android.graphics.BitmapFactory
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.model.MessageAttachment
import dev.ipf.whitenoise.model.MessageAttachmentKind
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.SingleMediaLayout
import dev.ipf.whitenoise.model.SingleMediaSize
import dev.ipf.whitenoise.ui.components.drawableResource

/** Shared geometry for rich content nested inside the 16 dp conversation bubble. */
internal object ConversationMessageMetrics {
    val RichOuterInset = 6.dp
    val RichContentSpacing = 6.dp
    val RichTextHorizontalAdjustment = 6.dp
    val RichTextBottomAdjustment = 2.dp
    val RichComponentInset = 6.dp
    val GallerySpacing = 2.dp
    val RichComponentCornerRadius = 10.dp
    val LinkImageHeight = 124.dp
    const val GifHeightDp = 188
}

internal val ConversationRichContentShape = RoundedCornerShape(
    ConversationMessageMetrics.RichComponentCornerRadius,
)

/**
 * A lone photo or video keeps its aspect-ratio-derived width. Every other rich-content
 * combination uses one shared canvas so quotes, galleries, and cards keep common edges.
 */
internal fun richContentCanvasWidthDp(
    attachments: List<MessageAttachment>,
    singleMediaSize: SingleMediaSize? = null,
): Int {
    val attachment = attachments.singleOrNull()
        ?: return SingleMediaLayout.MaximumExtentDp
    val isPhotoOrVideo = attachment.kind == MessageAttachmentKind.Photo ||
        attachment.kind == MessageAttachmentKind.Photos ||
        attachment.kind == MessageAttachmentKind.Video
    val frameCount = attachment.images.size.coerceAtLeast(1)
    return if (isPhotoOrVideo && frameCount == 1) {
        (singleMediaSize ?: SingleMediaLayout.size(attachment)).widthDp
    } else {
        SingleMediaLayout.MaximumExtentDp
    }
}

internal fun timelineSingleMediaSize(
    attachment: MessageAttachment,
    imageWidth: Int? = null,
    imageHeight: Int? = null,
): SingleMediaSize =
    if (attachment.kind == MessageAttachmentKind.Gif) {
        SingleMediaSize(
            widthDp = SingleMediaLayout.MaximumExtentDp,
            heightDp = ConversationMessageMetrics.GifHeightDp,
        )
    } else {
        SingleMediaLayout.size(
            imageWidth?.takeIf { it > 0 } ?: attachment.pixelWidth,
            imageHeight?.takeIf { it > 0 } ?: attachment.pixelHeight,
        )
    }

/** Rendered pixels take precedence over synthetic catalog dimensions. */
@Composable
internal fun rememberTimelineSingleMediaSize(attachment: MessageAttachment?): SingleMediaSize? {
    if (attachment == null) return null
    val context = LocalContext.current
    return remember(attachment, context) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        when (val image = attachment.images.firstOrNull()) {
            is ProfileAvatar.DeviceImage -> BitmapFactory.decodeByteArray(
                image.bytes, 0, image.bytes.size, options,
            )
            is ProfileAvatar.Asset -> BitmapFactory.decodeResource(
                context.resources, image.asset.drawableResource, options,
            )
            is ProfileAvatar.WebImage -> BitmapFactory.decodeResource(
                context.resources, image.asset.drawableResource, options,
            )
            ProfileAvatar.Monogram, null -> Unit
        }
        timelineSingleMediaSize(attachment, options.outWidth, options.outHeight)
    }
}

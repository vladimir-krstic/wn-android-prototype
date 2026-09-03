package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.model.MessageAttachment
import dev.ipf.whitenoise.model.MessageAttachmentKind
import dev.ipf.whitenoise.model.SingleMediaLayout
import dev.ipf.whitenoise.model.SingleMediaSize

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
internal fun richContentCanvasWidthDp(attachments: List<MessageAttachment>): Int {
    val attachment = attachments.singleOrNull()
        ?: return SingleMediaLayout.MaximumExtentDp
    val isPhotoOrVideo = attachment.kind == MessageAttachmentKind.Photo ||
        attachment.kind == MessageAttachmentKind.Photos ||
        attachment.kind == MessageAttachmentKind.Video
    val frameCount = attachment.images.size.coerceAtLeast(1)
    return if (isPhotoOrVideo && frameCount == 1) {
        SingleMediaLayout.size(attachment).widthDp
    } else {
        SingleMediaLayout.MaximumExtentDp
    }
}

internal fun timelineSingleMediaSize(attachment: MessageAttachment): SingleMediaSize =
    if (attachment.kind == MessageAttachmentKind.Gif) {
        SingleMediaSize(
            widthDp = SingleMediaLayout.MaximumExtentDp,
            heightDp = ConversationMessageMetrics.GifHeightDp,
        )
    } else {
        SingleMediaLayout.size(attachment)
    }

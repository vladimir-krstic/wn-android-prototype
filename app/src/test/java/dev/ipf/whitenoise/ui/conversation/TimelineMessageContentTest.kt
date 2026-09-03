package dev.ipf.whitenoise.ui.conversation

import dev.ipf.whitenoise.model.MessageAttachment
import dev.ipf.whitenoise.model.MessageAttachmentKind
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.SingleMediaSize
import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineMessageContentTest {
    @Test
    fun `voice durations use minute second formatting`() {
        assertEquals("0:07", formatMessageDuration(7))
        assertEquals("1:22", formatMessageDuration(82))
        assertEquals("0:00", formatMessageDuration(-1))
    }

    @Test
    fun `lone portrait media sets the rich canvas width`() {
        val portrait = MessageAttachment(
            id = "portrait",
            kind = MessageAttachmentKind.Photo,
            label = "Portrait",
            images = listOf(ProfileAvatar.Monogram),
            pixelWidth = 1_080,
            pixelHeight = 1_920,
        )

        assertEquals(192, richContentCanvasWidthDp(listOf(portrait)))
    }

    @Test
    fun `galleries and stacked rich content keep the shared canvas width`() {
        val gallery = MessageAttachment(
            id = "gallery",
            kind = MessageAttachmentKind.Photos,
            label = "Gallery",
            images = listOf(ProfileAvatar.Monogram, ProfileAvatar.Monogram),
            pixelWidth = 1_080,
            pixelHeight = 1_920,
        )
        val file = MessageAttachment(
            id = "file",
            kind = MessageAttachmentKind.File,
            label = "Brief.pdf",
        )

        assertEquals(256, richContentCanvasWidthDp(listOf(gallery)))
        assertEquals(256, richContentCanvasWidthDp(listOf(gallery, file)))
    }

    @Test
    fun `gif uses the fixed rich canvas media frame`() {
        val gif = MessageAttachment(
            id = "gif",
            kind = MessageAttachmentKind.Gif,
            label = "Animation",
            images = listOf(ProfileAvatar.Monogram),
        )

        assertEquals(256, richContentCanvasWidthDp(listOf(gif)))
        assertEquals(SingleMediaSize(256, 188), timelineSingleMediaSize(gif))
    }
}

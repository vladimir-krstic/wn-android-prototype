package dev.ipf.whitenoise.ui.conversation

import dev.ipf.whitenoise.model.MessageAttachment
import dev.ipf.whitenoise.model.MessageAttachmentKind
import dev.ipf.whitenoise.model.ProfileAvatar
import dev.ipf.whitenoise.model.SingleMediaSize
import dev.ipf.whitenoise.model.SingleMediaLayout
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

        assertEquals(144, richContentCanvasWidthDp(listOf(portrait)))
        assertEquals(SingleMediaSize(144, 256), timelineSingleMediaSize(portrait))
    }

    @Test
    fun `single media uses actual image dimensions before catalog dimensions`() {
        val panorama = MessageAttachment(
            id = "panorama",
            kind = MessageAttachmentKind.Photo,
            label = "Ostrich",
            images = listOf(ProfileAvatar.Monogram),
            pixelWidth = 3_000,
            pixelHeight = 700,
        )
        val renderedSize = timelineSingleMediaSize(panorama, imageWidth = 512, imageHeight = 342)

        assertEquals(SingleMediaSize(256, 256), renderedSize)
        assertEquals(256, richContentCanvasWidthDp(listOf(panorama), renderedSize))
        val portrait = panorama.copy(kind = MessageAttachmentKind.Video)
        val portraitSize = timelineSingleMediaSize(portrait, imageWidth = 800, imageHeight = 1_200)
        assertEquals(SingleMediaSize(171, 256), portraitSize)
        assertEquals(171, richContentCanvasWidthDp(listOf(portrait), portraitSize))
        assertEquals(SingleMediaSize(256, 256), timelineSingleMediaSize(panorama, -1, -1))
    }

    @Test
    fun `wide photos retain full media height when their width reaches the bubble limit`() {
        listOf(1_200 to 800, 1_920 to 1_080, 3_000 to 700).forEach { (width, height) ->
            assertEquals(SingleMediaSize(256, 256), SingleMediaLayout.size(width, height))
        }
        assertEquals(SingleMediaSize(171, 256), SingleMediaLayout.size(800, 1_200))
        assertEquals(SingleMediaSize(60, 256), SingleMediaLayout.size(700, 3_000))
    }

    @Test
    fun `single media preserves small source protection and safe missing dimensions`() {
        assertEquals(SingleMediaSize(192, 192), SingleMediaLayout.size(96, 96))
        assertEquals(SingleMediaSize(192, 192), SingleMediaLayout.size(96, 64))
        assertEquals(SingleMediaSize(256, 256), SingleMediaLayout.size(null, 100))
        assertEquals(SingleMediaSize(256, 256), SingleMediaLayout.size(100, 0))
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

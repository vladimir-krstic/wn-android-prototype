package dev.ipf.whitenoise.model

import java.io.ByteArrayOutputStream
import org.junit.Assert.*
import org.junit.Test

class AttachmentModelsTest {
    private fun advance(state: AttachmentTransfer): AttachmentTransfer = state.advance(state.revision)
    @Test fun transferQueuesProgressesAndCompletesExactlyOnce() {
        var state = AttachmentTransfer()
        assertTrue(state.running)
        state = advance(state); assertEquals(AttachmentTransferPhase.Active, state.phase)
        repeat(3) { state = advance(state) }
        assertEquals(75, state.progress)
        state = advance(state); assertEquals(AttachmentTransferPhase.Available, state.phase)
        assertEquals(state, advance(state)); assertFalse(state.retryable)
    }
    @Test fun cancellationInvalidatesInflightCompletionAndRetryHasNewRevision() {
        val active = advance(AttachmentTransfer()); val cancelled = active.cancel()
        assertEquals(cancelled, cancelled.advance(active.revision))
        assertFalse(cancelled.running); assertTrue(cancelled.retryable)
        val retry = cancelled.retry(); assertTrue(retry.running); assertEquals(2, retry.attempt)
        assertEquals(retry, retry.advance(active.revision)); assertEquals(0, retry.progress)
    }
    @Test fun failuresAndCacheMissRecoverButExpiredAndInvalidAreTerminal() {
        AttachmentTransferScenario.entries.forEach { scenario ->
            var state = AttachmentTransfer(scenario = scenario)
            repeat(4) { state = advance(state) }
            when (scenario) {
                AttachmentTransferScenario.Success -> assertTrue(state.running)
                AttachmentTransferScenario.Failure, AttachmentTransferScenario.CacheMiss -> {
                    assertTrue(state.retryable); state = state.retry(); repeat(5) { state = advance(state) }
                    assertEquals(AttachmentTransferPhase.Available, state.phase)
                }
                AttachmentTransferScenario.Expired, AttachmentTransferScenario.Invalid -> {
                    assertFalse(state.retryable); assertEquals(state, state.retry()); assertFalse(state.running)
                }
            }
        }
    }
    @Test fun transferDoesNotInventUnavailableBytes() {
        val a = MessageAttachment("a", MessageAttachmentKind.File, "Missing", isAvailable = false)
        assertFalse(a.bytesAvailable)
        assertFalse(a.copy(transfer = AttachmentTransfer(phase = AttachmentTransferPhase.Available)).bytesAvailable)
        assertFalse(a.copy(isAvailable = true, transfer = AttachmentTransfer()).bytesAvailable)
        assertTrue(a.copy(isAvailable = true).bytesAvailable)
    }
    @Test fun selectedContactFieldsAreExplicitAndDoNotBecomePersonIdentity() {
        val contact = SharedDeviceContact("Ada Example", "+1 555 100 1000", "ada@example.com")
        val chosen = contact.selected(true, false, true)
        assertEquals("Ada Example\nada@example.com", chosen.text)
        assertFalse(chosen.vCard().contains("TEL")); assertTrue(chosen.vCard().contains("EMAIL:ada@example.com"))
        val attachment = chosen.attachment("contact")!!
        assertNull(attachment.contactPersonId); assertEquals("text/vcard", attachment.mimeType)
        assertEquals(chosen.vCard().toByteArray().size, attachment.fileSizeBytes)
        assertNull(contact.selected(false, false, false).attachment("empty"))
    }
    @Test fun contactEscapePreventsInjectedVcardProperties() {
        val vcard = SharedDeviceContact("Ada;Example,\\\r\nTEL:secret", "+1\r\nEND:VCARD").vCard()
        assertTrue(vcard.contains("Ada\\;Example\\,\\\\\\nTEL:secret"))
        assertEquals(1, vcard.split("\r\n").count { it == "END:VCARD" })
        assertFalse(vcard.split("\r\n").any { it == "TEL:secret" })
    }
    @Test fun exportKeysPreserveAlbumsAndExcludeProfileAndLinkCards() {
        val message = ChatMessage("m", "p", 1, "Today", 1, "Now", attachments = listOf(
            MessageAttachment("album", MessageAttachmentKind.Photos, "Trip", images = listOf(ProfileAvatar.Monogram, ProfileAvatar.Monogram)),
            MessageAttachment("person", MessageAttachmentKind.Contact, "Profile", contactPersonId = "ada"),
            SharedDeviceContact("Ada", "123").attachment("device")!!,
            MessageAttachment("link", MessageAttachmentKind.Link, "Link"),
        ))
        assertEquals(listOf(AttachmentExportKey("album", 0), AttachmentExportKey("album", 1), AttachmentExportKey("device")), AttachmentExports.keys(message))
        assertTrue(AttachmentExports.keys(message.copy(deletionState = MessageDeletionState.DeletedByOther)).isEmpty())
    }
    @Test fun mixedShareMimeIsHonest() {
        assertEquals("image/png", AttachmentExports.mimeType(listOf("image/png", "image/png")))
        assertEquals("image/*", AttachmentExports.mimeType(listOf("image/jpeg", "image/png")))
        assertEquals("*/*", AttachmentExports.mimeType(listOf("image/jpeg", "application/pdf")))
    }
    @Test fun gifKeepsAnimationBlocksAndStripsCommentsAndNonRenderingExtensions() {
        val source = java.io.File("src/main/res/raw/chat_animation.gif").readBytes()
        val comment = byteArrayOf(0x21, 0xfe.toByte(), 3) + "GPS".toByteArray() + byteArrayOf(0)
        val withComment = source.dropLast(1).toByteArray() + comment + source.takeLast(1).toByteArray()
        assertArrayEquals(source, PhotoMetadata.strippedGif(source))
        assertArrayEquals(source, PhotoMetadata.strippedGif(withComment))
        assertNull(PhotoMetadata.strippedGif(source.dropLast(2).toByteArray()))
    }
    @Test fun adobeColorTransformUsesFallbackRatherThanChangingOriginalColors() {
        assertNull(PhotoMetadata.strippedOriginal(jpeg(segment(0xee, "Adobe".toByteArray()))))
    }

    private fun segment(marker: Int, content: ByteArray): ByteArray = byteArrayOf(255.toByte(), marker.toByte(), ((content.size + 2) shr 8).toByte(), (content.size + 2).toByte()) + content
    private fun jpeg(extra: ByteArray = byteArrayOf()): ByteArray = byteArrayOf(255.toByte(), 216.toByte()) + extra + segment(0xc0, byteArrayOf(1)) + segment(0xda, byteArrayOf(1)) + byteArrayOf(2, 3, 255.toByte(), 217.toByte())
    @Test fun originalJpegDropsExifXmpCommentsAndEmbeddedThumbnail() {
        val clean = jpeg()
        val source = jpeg(segment(0xe1, "EXIF GPS DEVICE".toByteArray()) + segment(0xe0, "THUMBNAIL".toByteArray()) + segment(0xed, "IPTC".toByteArray()) + segment(0xfe, "owner".toByteArray()))
        assertArrayEquals(clean, PhotoMetadata.strippedOriginal(source))
    }
    @Test fun truncatedOrUnsupportedOriginalNeverPassesThrough() {
        assertNull(PhotoMetadata.strippedOriginal(jpeg().dropLast(1).toByteArray()))
        assertNull(PhotoMetadata.strippedOriginal("GIF89a".toByteArray()))
        assertNull(PhotoMetadata.strippedOriginal(byteArrayOf(255.toByte(),216.toByte(),255.toByte(),225.toByte(),127,127)))
        assertNull(PhotoMetadata.strippedOriginal(jpeg().dropLast(2).toByteArray() + segment(0xe1,"private".toByteArray()) + byteArrayOf(255.toByte(),217.toByte())))
    }
    private fun chunk(type: String, bytes: ByteArray): ByteArray = ByteArrayOutputStream().apply {
        repeat(4) { write((bytes.size ushr ((3-it)*8)) and 255) }; write(type.toByteArray()); write(bytes); write(byteArrayOf(0,0,0,0))
    }.toByteArray()
    @Test fun pngOriginalDropsTextExifAndUnknownAncillaryChunksWithoutChangingImageChunks() {
        val signature = byteArrayOf(137.toByte(),80,78,71,13,10,26,10)
        val header = chunk("IHDR", ByteArray(13)); val image = chunk("IDAT", byteArrayOf(1,2,3)); val end = chunk("IEND", byteArrayOf())
        val source = signature + header + chunk("eXIf", "GPS".toByteArray()) + chunk("tEXt", "Author".toByteArray()) + image + end
        assertArrayEquals(signature + header + image + end, PhotoMetadata.strippedOriginal(source))
        assertNull(PhotoMetadata.strippedOriginal(signature + header + chunk("UNKN", byteArrayOf()) + image + end))
        assertNull(PhotoMetadata.strippedOriginal(source + byteArrayOf(1)))
    }
}

package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class AttachmentReadingModelsTest {
    private fun file(name: String, mime: String? = null) = MessageAttachment("file",MessageAttachmentKind.File,name,mimeType = mime)
    @Test fun candidateUsesNormalizedMetadataWithoutTrustingBytes() {
        assertEquals(TextAttachmentFormat.Markdown,TextAttachments.candidate(file("folder/NOTES.MD","application/octet-stream"))!!.format)
        assertEquals("NOTES.MD",TextAttachments.candidate(file("folder/NOTES.MD"))!!.name)
        assertEquals("text/plain",TextAttachments.candidate(file("notes.txt","not a mime"))!!.mime)
        assertNotNull(TextAttachments.candidate(file("data"," Application/vnd.api+json; charset=utf-8 ")))
        assertNull(TextAttachments.candidate(file("photo.png","image/png")))
        assertNull(TextAttachments.candidate(file("notes.txt").copy(kind = MessageAttachmentKind.Photo)))
        assertEquals("safe.txt",TextAttachments.safeName("C:\\folder\\\u202esafe.txt\n"))
    }
    @Test fun strictDecoderAcceptsUtf8AndBomTaggedUtf16IncludingEmptyFiles() {
        val text="Map 🦊\nДетаљи\tReturn\r\n"
        val samples=listOf(text.toByteArray(),byteArrayOf(0xef.toByte(),0xbb.toByte(),0xbf.toByte())+text.toByteArray(),
            byteArrayOf(0xff.toByte(),0xfe.toByte())+text.toByteArray(Charsets.UTF_16LE),byteArrayOf(0xfe.toByte(),0xff.toByte())+text.toByteArray(Charsets.UTF_16BE))
        samples.forEach { assertEquals(text,(TextAttachments.decode(it) as TextAttachmentResult.Ready).text) }
        assertEquals("",(TextAttachments.decode(byteArrayOf()) as TextAttachmentResult.Ready).text)
    }
    @Test fun invalidEncodingBinaryControlsAndByteLimitHaveDistinctOutcomes() {
        assertEquals(TextAttachmentResult.Failed(TextAttachmentFailure.InvalidEncoding),TextAttachments.decode(byteArrayOf(0xc3.toByte(),0x28)))
        assertEquals(TextAttachmentResult.Failed(TextAttachmentFailure.InvalidEncoding),TextAttachments.decode(byteArrayOf(0xff.toByte(),0xfe.toByte(),0x41)))
        assertEquals(TextAttachmentResult.Failed(TextAttachmentFailure.Binary),TextAttachments.decode(byteArrayOf(65,0,66)))
        assertEquals(TextAttachmentResult.Failed(TextAttachmentFailure.TooLarge),TextAttachments.decode(ByteArray(TextAttachments.MaximumBytes+1)))
        assertTrue(TextAttachments.decode(ByteArray(TextAttachments.MaximumBytes) { 65 }) is TextAttachmentResult.Ready)
    }
    @Test fun markdownFormattingHasVisibleBoundsButRetainsCompleteCopySource() {
        val text="# A\n\n"+"A line\n".repeat(3000)
        val preview=TextAttachments.presentation(text,TextAttachmentFormat.Markdown)
        assertTrue(preview.truncated); assertEquals(text,preview.source); assertTrue(preview.document!!.source.length < text.length)
        val long="a".repeat(TextAttachments.MaximumFormattedCharacters-1)+"🦊tail"
        val limited=TextAttachments.presentation(long,TextAttachmentFormat.Markdown)
        assertTrue(limited.truncated); assertFalse(limited.document!!.source.last().isHighSurrogate())
        val plain = TextAttachments.presentation(text,TextAttachmentFormat.PlainText)
        assertTrue(plain.truncated); assertEquals(text,plain.source); assertEquals(text,plain.speech)
        assertTrue(plain.preview.length <= TextAttachments.MaximumFormattedCharacters)
    }
    @Test fun speechChunksPreserveEveryCharacterAndSurrogatePairWithinPlatformLimit() {
        val text=("Trail 🦊 and river\n".repeat(900))+"end"
        val chunks=SpeechTextChunks.split(text,41)
        assertEquals(text,chunks.joinToString("")); assertTrue(chunks.all { it.length <= 41 && !it.last().isHighSurrogate() && !it.first().isLowSurrogate() })
        assertEquals(emptyList<String>(),SpeechTextChunks.split("",4000))
        assertEquals(listOf("🦊","🦊"),SpeechTextChunks.split("🦊🦊",2))
    }
    @Test fun packageMimeAndGenericFilenameCannotConfuseConflictingContentTypes() {
        assertTrue(PackageAttachments.candidate(file("app.apk","application/octet-stream")))
        assertTrue(PackageAttachments.candidate(file("download",PackageAttachments.Mime)))
        assertFalse(PackageAttachments.candidate(file("notes.apk","text/plain")))
        assertFalse(PackageAttachments.candidate(file("image.zip","application/octet-stream")))
    }
    @Test fun packageOutcomesCoverInvalidDistributionPermissionAndInstallerGates() {
        val attachment=file("app.apk")
        assertEquals(PackageOpenOutcome.InvalidPackage,PackageAttachments.outcome(attachment,false,true,true,true))
        assertEquals(PackageOpenOutcome.RestrictedDistribution,PackageAttachments.outcome(attachment,true,false,true,true))
        assertEquals(PackageOpenOutcome.PermissionRequired,PackageAttachments.outcome(attachment,true,true,false,true))
        assertEquals(PackageOpenOutcome.NoInstaller,PackageAttachments.outcome(attachment,true,true,true,false))
        assertEquals(PackageOpenOutcome.Ready,PackageAttachments.outcome(attachment,true,true,true,true))
        assertEquals(PackageOpenOutcome.OrdinaryFile,PackageAttachments.outcome(file("a.pdf"),false,false,false,false))
    }
    private fun chat(): Pair<Profile,Chat> {
        val profile=ProfileFixtures.marmota;val base=profile.chats.first()
        fun message(id:String,date:String,vararg attachments:MessageAttachment)=ChatTimelineEntry.Message(ChatMessage(id,profile.id,1,"Today",1,"Now",attachments=attachments.toList(),createdAtMillis=Instant.parse(date).toEpochMilli()))
        return profile to base.copy(timeline=listOf(
            message("old","2026-06-30T23:30:00Z",file("notes.txt"),file("clip.opus","audio/opus")),
            message("new","2026-08-02T09:00:00Z",MessageAttachment("album",MessageAttachmentKind.Photos,"Trip",images=listOf(ProfileAvatar.Asset(AvatarAsset.Fox),ProfileAvatar.Asset(AvatarAsset.Marmot))),MessageAttachment("gif",MessageAttachmentKind.Gif,"Animation")),
            message("missing","2026-07-20T09:00:00Z",MessageAttachment("unavailable",MessageAttachmentKind.Video,"Video",isAvailable=false)),
        ))
    }
    @Test fun sharedCategoriesKeepAudioSeparateAndShowUnavailableMediaWithExactAlbumKeys() {
        val (profile,chat)=chat();val media=SharedContentProjection.items(chat,profile,SharedContentCategory.Media)
        assertEquals(listOf("new","new","new","missing"),media.map { it.messageId })
        assertEquals(listOf(0,1,0,0),media.map { it.mediaKey!!.imageIndex });assertEquals(media.size,media.map { it.id }.distinct().size)
        assertEquals(1,SharedContentProjection.filtered(media,SharedMediaFilter.Videos).size)
        assertEquals(3,SharedContentProjection.filtered(media,SharedMediaFilter.Images).size)
        assertEquals(listOf("clip.opus"),SharedContentProjection.items(chat,profile,SharedContentCategory.Voice).map { it.attachment.label })
        assertEquals(listOf("notes.txt"),SharedContentProjection.items(chat,profile,SharedContentCategory.Documents).map { it.attachment.label })
    }
    @Test fun monthGroupingUsesAuthoritativeEpochAndRequestedTimeZone() {
        val (profile,chat)=chat();val voice=SharedContentProjection.items(chat,profile,SharedContentCategory.Voice)
        assertEquals("2026-06",SharedContentProjection.months(voice,ZoneOffset.UTC).single().key.toString())
        assertEquals("2026-07",SharedContentProjection.months(voice,ZoneOffset.ofHours(2)).single().key.toString())
        val media=SharedContentProjection.items(chat,profile,SharedContentCategory.Media)
        assertEquals(listOf("2026-08","2026-07"),SharedContentProjection.months(media,ZoneOffset.UTC).map { it.key.toString() })
    }
    @Test fun deletedAndExpiredSourcesDisappearFromLibraryAndViewer() {
        val (profile,chat)=chat()
        val hidden=chat.copy(timeline=chat.timeline.mapIndexed { index,entry ->
            val message=(entry as ChatTimelineEntry.Message).message
            entry.copy(message=if(index==0) message.copy(deletionState=MessageDeletionState.DeletedByCurrentProfile) else message.copy(expiresAtMillis=MessageForwarding.nowMillis))
        })
        assertTrue(SharedContentCategory.entries.all { SharedContentProjection.items(hidden,profile,it).isEmpty() })
        assertTrue(ConversationMediaProjection.items(hidden,profile).isEmpty())
    }
    @Test fun bodyUrlsAreDistinctAndKeepTheirSourceWhenPreviewIsAbsent() {
        val (profile,chat)=chat(); val message=ChatMessage("urls",profile.id,1,"Today",1,"Now","[Trail](https://example.org/trail) and https://example.org/river. https://example.org/trail")
        val items=SharedContentProjection.items(chat.copy(timeline=listOf(ChatTimelineEntry.Message(message))),profile,SharedContentCategory.Links)
        assertEquals(listOf("https://example.org/trail","https://example.org/river"),items.map { it.attachment.externalUri })
        assertTrue(items.all { it.messageId == "urls" })
    }
    @Test fun audioSelectionAndClearRejectOldLoadAndPlaybackCallbacks() {
        val first=LibraryAudioState().start("a");val second=first.start("b")
        assertEquals(second,second.failed(first.revision));assertEquals(second,second.observed(first.revision,100,300,LibraryAudioPhase.Playing))
        val playing=second.observed(second.revision,2000,1000,LibraryAudioPhase.Playing)
        assertEquals(1000L,playing.positionMillis)
        val cleared=playing.clear();assertNull(cleared.key);assertEquals(cleared,cleared.failed(second.revision))
    }
    @Test fun viewerSourceGuardRejectsRemovalAndReplacementButAllowsMetadataUpdates() {
        val (profile,chat)=chat()
        val items=ConversationMediaProjection.items(chat,profile)
        val original=items.first()
        val selection=ConversationMediaSelection(items,original.key)
        assertTrue(selection.containsSource(original))
        assertFalse(selection.copy(items=items.drop(1)).containsSource(original))
        assertFalse(selection.copy(items=listOf(original.copy(attachment=original.attachment.copy(isAvailable=false)))).containsSource(original))
        assertFalse(selection.copy(items=listOf(original.copy(attachment=original.attachment.copy(externalUri="content://replacement")))).containsSource(original))
        assertTrue(selection.copy(items=listOf(original.copy(senderName="Updated name"))).containsSource(original))
    }

}

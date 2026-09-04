package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class IncomingSharingTest {
    private val photo = MessageAttachment("p",MessageAttachmentKind.Photo,"Photo",images=listOf(ProfileAvatar.Monogram))
    private val file = MessageAttachment("f",MessageAttachmentKind.File,"Document.txt",localSource=AttachmentLocalSource.PlainText)
    @Test fun providerMimeOverridesIntentAndAudioUsesTheDocumentShelf() {
        val prepared=IncomingSharing.prepare(IncomingPayload(" Text ",listOf(IncomingStream("photo",photo,"image/jpeg"),IncomingStream("audio",file,"audio/wav"),IncomingStream("fallback",photo)),"image/png")).content!!
        assertEquals("Text",prepared.text); assertEquals(2,prepared.media.size); assertEquals(1,prepared.documents.size)
        assertEquals("audio/wav",prepared.documents.single().mimeType)
    }
    @Test fun duplicateStreamsArePreparedOnlyOnceAndEmptyMalformedUnavailableAreDistinct() {
        val stream=IncomingStream("file",file,"text/plain")
        assertEquals(1,IncomingSharing.prepare(IncomingPayload(streams=listOf(stream,stream))).content!!.documents.size)
        assertEquals(IncomingContentFailure.Empty,IncomingSharing.prepare(IncomingPayload("  ")).failure)
        assertEquals(IncomingContentFailure.Invalid,IncomingSharing.prepare(IncomingPayload(streams=listOf(stream.copy(identity="")))).failure)
        assertEquals(IncomingContentFailure.Unavailable,IncomingSharing.prepare(IncomingPayload(streams=listOf(stream.copy(readable=false)))).failure)
        assertEquals(IncomingContentFailure.TooLarge,IncomingSharing.prepare(IncomingPayload(streams=listOf(stream.copy(failure=IncomingContentFailure.TooLarge)))).failure)
    }
    @Test fun independentShelfCapsIncludeExistingDraftAndReportEveryDroppedFile() {
        val chat=Chat("c",0,ChatKind.Direct("p"),"Chat",draftText="Keep me",draftReplyMessageId="reply",draftAttachments=List(9){ photo.copy(id="old-$it") }+List(8){ file.copy(id="old-file-$it") })
        val input=PreparedIncoming("New text",List(3){photo.copy(id="photo-$it")},List(5){file.copy(id="file-$it")})
        val result=IncomingSharing.stage(chat,input,1)
        assertEquals(5,result.dropped); assertEquals(20,result.chat.draftAttachments.size)
        assertEquals("Keep me\nNew text",result.chat.draftText); assertEquals("reply",result.chat.draftReplyMessageId)
        assertEquals(chat.timeline,result.chat.timeline); assertEquals(chat.draftAttachments,result.chat.draftAttachments.take(17))
    }
    @Test fun albumFramesCountAgainstTheMediaShelfAndFullShelvesDoNotEraseContent() {
        val album=photo.copy(kind=MessageAttachmentKind.Photos,images=List(10){ProfileAvatar.Monogram})
        val chat=Chat("c",0,ChatKind.Direct("p"),"Chat",draftAttachments=listOf(album))
        val result=IncomingSharing.stage(chat,PreparedIncoming("",listOf(photo),listOf(file)),9)
        assertEquals(1,result.dropped); assertEquals(listOf(album),result.chat.draftAttachments.take(1)); assertEquals(2,result.chat.draftAttachments.size)
    }
    @Test fun everySupportedDeveloperPayloadUsesTheNormalPreparationPath() {
        val p=ProfileFixtures.marmota
        for (kind in listOf(IncomingExample.Text,IncomingExample.Photo,IncomingExample.Video,IncomingExample.Audio,IncomingExample.Document,IncomingExample.Mixed)) {
            val entry=IncomingExamples.entry(kind,p,listOf(p)) as IncomingEntry.Share
            assertNotNull(IncomingSharing.prepare(entry.payload).content)
        }
    }
    @Test fun archivedIsEligibleButBlockedInvitationAndTerminalGroupsAreNot() {
        val profile=ProfileFixtures.marmota; val chat=profile.chats.first { IncomingSharing.canStage(profile,it) }
        assertTrue(IncomingSharing.canStage(profile,chat.copy(isArchived=true)))
        assertFalse(IncomingSharing.canStage(profile,chat.copy(membership=ChatMembership.Invited)))
        assertFalse(IncomingSharing.canStage(profile,chat.copy(membership=ChatMembership.Left)))
        val group=Chat("g",0,ChatKind.Group,"Ended",members=listOf(GroupMember(profile.id,GroupRole.Admin)),groupLifecycle=GroupLifecycle.Disbanded)
        assertFalse(IncomingSharing.canStage(profile,group))
    }
}

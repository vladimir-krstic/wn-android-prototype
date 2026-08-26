package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class ChatListPresentationTest {
    private val base = Chat("chat", 0, ChatKind.Group, "A chat", preview = "Existing message", previewAuthor = "You", timestamp = "6h")

    @Test fun draftsAndSearchUseTheSameCurrentText() {
        val chat = base.copy(draftText = "Unsent text")
        assertEquals("Draft: Unsent text", chat.displayPreview)
        assertEquals(listOf(chat), ChatProjection.rows(listOf(chat), ChatScope.Chats, "unsent"))
        assertTrue(ChatProjection.rows(listOf(chat), ChatScope.Chats, "Existing message").isEmpty())
        assertEquals("6h", chat.timestamp)
    }

    @Test fun membershipOverridesDraftsAndAttachmentAndAuthor() {
        for (membership in listOf(ChatMembership.Left, ChatMembership.Removed, ChatMembership.Invited)) {
            val chat = base.copy(membership = membership, draftText = "Stale draft", attachmentPreview = AttachmentPreview.Photo)
            val row = ChatListPresentation.from(chat)
            assertEquals(chat.visiblePreview, row.searchableText)
            assertNull(row.prefix)
            assertNull(row.attachment)
            assertFalse(row.isDraft)
        }
    }

    @Test fun senderAttachmentAndFailureStayInTheProjection() {
        val chat = base.copy(preview = "", attachmentPreview = AttachmentPreview.File("Plan.pdf"), deliveryState = ChatDeliveryState.Failed)
        val row = ChatListPresentation.from(chat)
        assertEquals("You: Plan.pdf", row.searchableText)
        assertEquals(chat.attachmentPreview, row.attachment)
        assertTrue(row.showsFailure)
    }

    @Test fun invitationTakesPrecedenceOverFailureAndUnread() {
        val chat = base.copy(membership = ChatMembership.Invited, deliveryState = ChatDeliveryState.Failed, unreadCount = 4, isMarkedUnread = true)
        assertEquals(ChatListStatus.Invitation, ChatListPresentation.from(chat).status)
        assertTrue(chat.isUnread)
    }

    @Test fun visibleFailureSuppressesUnreadWithoutMutatingIt() {
        val chat = base.copy(deliveryState = ChatDeliveryState.Failed, unreadCount = 3, isMarkedUnread = true)
        assertEquals(ChatListStatus.Failure, ChatListPresentation.from(chat).status)
        assertEquals(3, chat.unreadCount)
        assertTrue(chat.isMarkedUnread)
        assertEquals(ChatListAction.Read, ChatListActionPolicy.all(chat).first())
        assertEquals(listOf(chat), ChatProjection.rows(listOf(chat), ChatScope.Unread))
    }

    @Test fun countsWinOverManualUnreadAndEmptyStateHasNoIndicator() {
        assertEquals(ChatListStatus.UnreadCount(101), ChatListPresentation.from(base.copy(unreadCount = 101, isMarkedUnread = true)).status)
        assertEquals(ChatListStatus.ManuallyUnread, ChatListPresentation.from(base.copy(isMarkedUnread = true)).status)
        assertEquals(ChatListStatus.None, ChatListPresentation.from(base).status)
    }

    @Test fun draftAndEndedMembershipHideStaleFailureButRetainUnreadIndicators() {
        val failed = base.copy(deliveryState = ChatDeliveryState.Failed, unreadCount = 7)
        assertEquals(ChatListStatus.UnreadCount(7), ChatListPresentation.from(failed.copy(draftText = "Draft")).status)
        assertEquals(ChatListStatus.UnreadCount(7), ChatListPresentation.from(failed.copy(membership = ChatMembership.Left)).status)
        assertEquals(ChatListStatus.ManuallyUnread, ChatListPresentation.from(failed.copy(membership = ChatMembership.Removed, unreadCount = 0, isMarkedUnread = true)).status)
    }

    @Test fun attachmentOnlyDraftUsesItsLabelAndSuppressesOldFailure() {
        val chat = base.copy(draftAttachments = listOf(MessageAttachment("photo", MessageAttachmentKind.Photo, "Photo")), deliveryState = ChatDeliveryState.Failed)
        val row = ChatListPresentation.from(chat)
        assertEquals("Draft: Photo", row.searchableText)
        assertFalse(row.showsFailure)
    }

    @Test fun scopesIncludeEndedMembershipWithoutLeakingArchive() {
        val ended = base.copy(membership = ChatMembership.Left, unreadCount = 2)
        val archived = base.copy(id = "archive", membership = ChatMembership.Removed, isArchived = true, unreadCount = 1)
        assertEquals(listOf(ended), ChatProjection.rows(listOf(ended, archived), ChatScope.Chats))
        assertEquals(listOf(ended), ChatProjection.rows(listOf(ended, archived), ChatScope.Unread))
        assertEquals(listOf(ended), ChatProjection.rows(listOf(ended, archived), ChatScope.Left))
        assertEquals(listOf(archived), ChatProjection.rows(listOf(ended, archived), ChatScope.Archived))
    }

    @Test fun activeGroupActionsHaveTheApprovedOrder() {
        assertEquals(listOf(ChatListAction.Unread, ChatListAction.Pin), ChatListActionPolicy.leading(base))
        assertEquals(listOf(ChatListAction.Mute, ChatListAction.Archive, ChatListAction.Leave), ChatListActionPolicy.trailing(base))
        assertEquals(listOf(ChatListAction.Read, ChatListAction.Unpin), ChatListActionPolicy.leading(base.copy(unreadCount = 3, isPinned = true)))
        assertEquals(ChatListAction.Unmute, ChatListActionPolicy.trailing(base.copy(muteDuration = MuteDuration.OneDay)).first())
    }

    @Test fun archivedReadChatsHaveNoLeadingActionsAndArchivedUnreadOnlyRead() {
        assertTrue(ChatListActionPolicy.leading(base.copy(isArchived = true)).isEmpty())
        assertEquals(listOf(ChatListAction.Read), ChatListActionPolicy.leading(base.copy(isArchived = true, unreadCount = 2)))
        assertEquals(listOf(ChatListAction.Unarchive, ChatListAction.Leave), ChatListActionPolicy.trailing(base.copy(isArchived = true)))
    }

    @Test fun endedInvitedAndDirectActionsRespectEligibility() {
        assertEquals(listOf(ChatListAction.Archive, ChatListAction.Delete), ChatListActionPolicy.trailing(base.copy(membership = ChatMembership.Left)))
        assertEquals(listOf(ChatListAction.Archive), ChatListActionPolicy.trailing(base.copy(membership = ChatMembership.Invited)))
        assertEquals(listOf(ChatListAction.Mute, ChatListAction.Archive), ChatListActionPolicy.trailing(base.copy(kind = ChatKind.Direct("person"))))
        assertTrue(ChatListActionPolicy.leading(base.copy(membership = ChatMembership.Removed)).contains(ChatListAction.Pin))
    }

    @Test fun readUndoRestoresCountsWithoutReplacingNewDraftOrMuteState() {
        val original = base.copy(unreadCount = 7, isMarkedUnread = true)
        val undo = ChatListUndo.capture("profile", original, ChatListAction.Read)
        val updated = original.copy(unreadCount = 0, isMarkedUnread = false, draftText = "New draft", muteDuration = MuteDuration.OneHour)
        val restored = undo.restore(updated)
        assertEquals(7, restored.unreadCount)
        assertTrue(restored.isMarkedUnread)
        assertEquals("New draft", restored.draftText)
        assertEquals(MuteDuration.OneHour, restored.muteDuration)
        val newerUnread = updated.copy(unreadCount = 1)
        assertEquals(newerUnread, undo.restore(newerUnread))
    }

    @Test fun archiveUndoRestoresPinButNotUnrelatedFields() {
        val original = base.copy(isPinned = true)
        val undo = ChatListUndo.capture("profile", original, ChatListAction.Archive)
        val archived = original.copy(isArchived = true, isPinned = false, preview = "Updated", unreadCount = 3)
        val restored = undo.restore(archived)
        assertFalse(restored.isArchived)
        assertTrue(restored.isPinned)
        assertEquals("Updated", restored.preview)
        assertEquals(3, restored.unreadCount)
        assertEquals(original, undo.restore(original))
    }

    @Test fun unarchiveAndManualUnreadUndoRestoreOnlyTheirOwnFields() {
        val archived = base.copy(isArchived = true)
        val undoArchive = ChatListUndo.capture("profile", archived, ChatListAction.Unarchive)
        val changed = archived.copy(isArchived = false, draftText = "Keep me", unreadCount = 4)
        assertEquals(changed.copy(isArchived = true), undoArchive.restore(changed))
        val undoUnread = ChatListUndo.capture("profile", base, ChatListAction.Unread)
        val manuallyUnread = base.copy(isMarkedUnread = true, muteDuration = MuteDuration.Always)
        assertEquals(manuallyUnread.copy(isMarkedUnread = false), undoUnread.restore(manuallyUnread))
    }
}

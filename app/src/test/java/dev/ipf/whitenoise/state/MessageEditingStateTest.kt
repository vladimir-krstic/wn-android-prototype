package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class MessageEditingStateTest {
    private val chatId = "fiatjaf"
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); sendText(chatId, "Original authored body") }
    private fun AppViewModel.message(id: String? = null) = chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>()
        .let { rows -> if (id == null) rows.last().message else rows.first { it.id == id }.message }
    private fun AppViewModel.start(text: String, id: String = message().id): Long {
        assertTrue(beginMessageEdit(uiState.activeProfileId!!, chatId, id, text))
        return message(id).editAttempt!!.id
    }
    private fun AppViewModel.finish(id: String, request: Long) = advanceMessageEdit(uiState.activeProfileId!!, chatId, id, request)

    @Test fun acceptedRevisionsKeepOriginalAndEveryAcceptedTextInChronologicalOrder() {
        val vm = model(); val original = vm.message(); val owner = vm.uiState.activeProfileId!!
        val request = vm.start("  First revision  ")
        assertEquals(original.text, vm.message().text)
        assertEquals("First revision", MessageEditing.displayedText(vm.message()))
        assertTrue(vm.finish(original.id, request)); assertFalse(vm.finish(original.id, request))
        val second = vm.start("Second revision"); assertTrue(vm.finish(original.id, second))
        val edited = vm.message()
        assertEquals("Second revision", edited.visibleText(owner)); assertEquals(original.timeLabel, edited.timeLabel)
        assertEquals(original.attachments, edited.attachments); assertEquals(original.text, edited.editHistory!!.original)
        assertEquals(listOf("First revision", "Second revision"), edited.editHistory.revisions.map { it.text })
        assertTrue(edited.editHistory.revisions.zipWithNext().all { (a, b) -> a.timestampMillis < b.timestampMillis })
    }
    @Test fun failedEditRestoresAcceptedBodyAndRetryAddsExactlyOneRevision() {
        val vm = model(); val id = vm.message().id; val owner = vm.uiState.activeProfileId!!
        vm.setDeveloperToolsEnabled(true); vm.selectMessageEditScenario(MessageEditScenario.SaveFails)
        val request = vm.start("Changed")
        assertFalse(vm.finish(id, request)); assertEquals("Original authored body", MessageEditing.displayedText(vm.message()))
        assertNull(vm.message().editHistory); assertEquals(MessageEditFailure.SaveFailed, vm.message().editAttempt!!.failure)
        assertTrue(vm.retryMessageEdit(owner, chatId, id)); val retry = vm.message().editAttempt!!.id
        assertFalse(vm.finish(id, request)); assertTrue(vm.finish(id, retry))
        assertEquals(1, vm.message().editHistory!!.revisions.size)
    }
    @Test fun discardAndSupersedingEditPreventOldCompletionFromChangingTheMessage() {
        val vm = model(); val id = vm.message().id; val owner = vm.uiState.activeProfileId!!
        val first = vm.start("A"); val second = vm.start("B")
        assertFalse(vm.finish(id, first)); assertEquals("B", vm.message().editAttempt!!.text)
        assertTrue(vm.discardMessageEdit(owner, chatId, id)); assertFalse(vm.finish(id, second))
        assertEquals("Original authored body", vm.message().text); assertNull(vm.message().editHistory)
    }
    @Test fun blankUnchangedStaleEditorAndOtherAuthorsCannotSubmit() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!; val id = vm.message().id
        assertFalse(vm.beginMessageEdit(owner, chatId, id, "  "))
        assertFalse(vm.beginMessageEdit(owner, chatId, id, " Original authored body "))
        val incoming = vm.chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().first { it.message.authorId != owner }
        assertFalse(vm.beginMessageEdit(owner, chatId, incoming.id, "Changed"))
        val request = vm.start("Accepted"); vm.finish(id, request)
        assertFalse(vm.beginMessageEdit(owner, chatId, id, "Stale draft", expectedRevision = 0))
    }
    @Test fun losingWritableChatBeforeCompletionProducesRetryableUnavailableWithoutAccepting() {
        val vm = model(); val id = vm.message().id; val request = vm.start("Changed")
        vm.leaveChat(chatId)
        assertFalse(vm.finish(id, request)); assertEquals(MessageEditFailure.Unavailable, vm.message().editAttempt!!.failure)
        assertFalse(vm.retryMessageEdit(vm.uiState.activeProfileId!!, chatId, id)); assertNull(vm.message().editHistory)
    }
    @Test fun profileRoundTripInvalidatesPendingRequestAndRetainsRetryableDraft() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!; val id = vm.message().id; val request = vm.start("Changed")
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertFalse(vm.advanceMessageEdit(owner, chatId, id, request))
        vm.selectProfile(owner)
        assertFalse(vm.finish(id, request)); assertEquals(MessageEditFailure.Interrupted, vm.message().editAttempt!!.failure)
        assertTrue(vm.retryMessageEdit(owner, chatId, id)); assertTrue(vm.finish(id, vm.message().editAttempt!!.id))
    }
    @Test fun deletionRemovesOriginalRevisionsAndPendingEditAndCannotBeResurrected() {
        val vm = model(); val id = vm.message().id
        vm.finish(id, vm.start("Revision")); val pending = vm.start("Another revision")
        assertTrue(vm.deleteMessages(chatId, setOf(id), MessageDeletionScope.ForEveryone))
        assertFalse(vm.finish(id, pending)); assertTrue(vm.message().isDeleted)
        assertNull(vm.message().editHistory); assertNull(vm.message().editAttempt)
    }
    @Test fun editPreservesOrdinaryComposerAndAcceptedTextFlowsToRepliesSearchPreviewAndForward() {
        val vm = model(); val id = vm.message().id; val owner = vm.uiState.activeProfileId!!
        vm.updateDraftText(chatId, "Unsent draft"); vm.setDraftReply(chatId, id)
        val before = vm.chat(chatId)!!
        vm.finish(id, vm.start("Distinct revised words"))
        val after = vm.chat(chatId)!!
        assertEquals(before.draftText, after.draftText); assertEquals(before.draftReplyMessageId, after.draftReplyMessageId)
        assertEquals(before.draftAttachments, after.draftAttachments); assertEquals("Distinct revised words", after.preview)
        assertEquals(id, ConversationSearch.results(after, vm.uiState.activeProfile!!, "Distinct revised").single().messageId)
        assertTrue(vm.forwardMessages(chatId, setOf(id), listOf("maya-chen")))
        val copy = vm.chat("maya-chen")!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message
        assertEquals("Distinct revised words", copy.text); assertEquals(owner, copy.authorId)
        assertNull(copy.editHistory); assertNull(copy.editAttempt)
    }
    @Test fun collapsePreferenceIsChatAndProfileOwnedAndRetainedOnReturn() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!
        vm.setCollapseLongMessages("wrong", chatId, false); assertTrue(vm.chat(chatId)!!.collapseLongMessages)
        vm.setCollapseLongMessages(owner, chatId, false); assertFalse(vm.chat(chatId)!!.collapseLongMessages)
        assertTrue(vm.chat("maya-chen")!!.collapseLongMessages)
        vm.completeSignIn(OnboardingOrigin.AddProfile); vm.selectProfile(owner)
        assertFalse(vm.chat(chatId)!!.collapseLongMessages)
    }
}

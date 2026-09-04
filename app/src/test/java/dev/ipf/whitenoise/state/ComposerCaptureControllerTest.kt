package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import org.junit.Assert.*
import org.junit.Test

class ComposerCaptureControllerTest {
    private val owner = ComposerCaptureOwner("p", "c")
    private var current = Profile("p", "Name", "public", settings = ProfileSettings(dictation = DictationPreferences(disclosureAccepted = true)),
        developerTools = DeveloperToolsState(isEnabled = true), chats = listOf(Chat("c", 0, ChatKind.Direct("other"), "Chat", relayUrls = listOf("wss://relay.example.com"), draftText = "Hello")))
    private var activeId: String? = current.id
    private var signedIn = true
    private var rejectWrite = false
    private val sends = mutableListOf<String>()
    private lateinit var c: ComposerCaptureController
    private fun setup(): ComposerCaptureController {
        c = ComposerCaptureController({ listOf(current) }, { activeId }, { signedIn && it == current.id },
            { target, text -> if (target != owner || rejectWrite) false else { edit(text); true } },
            { target, draft, text -> if (target != owner || rejectWrite || current.chats.single().draftText != draft.text) false else { sends += text; edit(""); true } },
            { id, reduce -> if (id == current.id) current = current.copy(settings = current.settings.copy(dictation = reduce(current.settings.dictation))) })
        c.open(owner); return c
    }
    private fun edit(text: String) {
        current = current.copy(chats = listOf(current.chats.single().copy(draftText = text))); c.reconcile()
    }
    private fun start() { assertTrue(c.begin(owner, current.chats.single().draftText, 5, 5)); advance() }
    private fun advance() { c.attempts[owner]!!.let { c.advance(owner, it.id, it.revision) } }
    private fun speak() { repeat(15) { advance() } }
    private fun finish() { c.finish(owner, c.attempts[owner]!!.id); advance() }
    @Test fun defaultPasteUsesCapturedSelectionAndFinishesOnce() {
        setup(); start(); speak(); val id = c.attempts[owner]!!.id; finish()
        assertEquals("Hello ${DictationExamples.transcript}", current.chats.single().draftText)
        assertEquals(DictationPhase.Complete, c.attempts[owner]!!.phase); assertTrue(sends.isEmpty())
        val text = current.chats.single().draftText; c.advance(owner, id, 0); assertEquals(text, current.chats.single().draftText)
    }
    @Test fun explicitlyEnabledSendUsesCapturedPreferencesAndSendsOnce() {
        current = current.copy(settings = current.settings.copy(dictation = DictationPreferences(delivery = DictationDeliveryMode.Send, disclosureAccepted = true)))
        setup(); start(); speak()
        c.changePreferences("p") { it.copy(delivery = DictationDeliveryMode.Paste) }
        finish(); assertEquals(listOf("Hello ${DictationExamples.transcript}"), sends)
        assertEquals("", current.chats.single().draftText); finish(); assertEquals(1, sends.size)
    }
    @Test fun changingAndRevertingDraftRequiresReviewInsteadOfAutomaticSend() {
        current = current.copy(settings = current.settings.copy(dictation = DictationPreferences(delivery = DictationDeliveryMode.Send, disclosureAccepted = true)))
        setup(); start(); speak(); edit("Changed"); edit("Hello"); finish()
        assertTrue(sends.isEmpty()); assertEquals(DictationPhase.Review, c.attempts[owner]!!.phase)
        assertEquals("Hello", current.chats.single().draftText)
    }
    @Test fun membershipLossRetainsAvailableTextAndNeverSendsWhenMembershipReturns() {
        setup(); start(); speak()
        current = current.copy(chats = listOf(current.chats.single().copy(membership = ChatMembership.Left))); c.reconcile()
        assertEquals(DictationPhase.Review, c.attempts[owner]!!.phase); assertNull(c.lease)
        current = current.copy(chats = listOf(current.chats.single().copy(membership = ChatMembership.Active))); c.reconcile(); finish()
        assertEquals(DictationPhase.Review, c.attempts[owner]!!.phase); assertTrue(sends.isEmpty())
    }
    @Test fun leavingTheChatAndProfileSwitchRetainTextWithoutReassigningIt() {
        setup(); start(); speak(); c.close(owner); activeId = "other"; c.reconcile()
        assertEquals(DictationPhase.Review, c.attempts[owner]!!.phase); assertFalse(c.insertAtEnd(owner, c.attempts[owner]!!.id))
        activeId = "p"; c.open(owner); assertTrue(c.insertAtEnd(owner, c.attempts[owner]!!.id)); assertTrue(sends.isEmpty())
    }
    @Test fun backgroundKeepsPartialTextAndCannotResumeRecognitionAutomatically() {
        setup(); start(); repeat(5) { advance() }; c.background(); val retained = c.attempts[owner]!!
        assertEquals(DictationPhase.Review, retained.phase); assertTrue(retained.retainedText.isNotBlank())
        c.open(owner); c.advance(owner, retained.id, retained.revision); assertEquals(retained, c.attempts[owner])
    }
    @Test fun signedOutProfilesLoseCaptureAndReviewText() {
        setup(); start(); speak(); c.background(); signedIn = false; c.reconcile()
        assertTrue(c.attempts.isEmpty()); assertNull(c.lease)
    }
    @Test fun failedCommitRetainsTextAndExplicitInsertUsesLatestDraftWithoutSending() {
        setup(); c.chooseScenario(DictationScenario.CommitFailure); start(); speak(); finish()
        val review = c.attempts[owner]!!; assertEquals(DictationReviewReason.CommitRejected, review.reviewReason)
        edit("My revised draft"); rejectWrite = true; assertFalse(c.insertAtEnd(owner, review.id))
        assertEquals(review, c.attempts[owner]); rejectWrite = false
        assertTrue(c.insertAtEnd(owner, review.id)); assertEquals("My revised draft ${review.retainedText}", current.chats.single().draftText)
        assertFalse(c.insertAtEnd(owner, review.id)); assertTrue(sends.isEmpty())
    }
    @Test fun oldCallbacksCannotCompleteOrCancelReplacementSession() {
        setup(); start(); val old = c.attempts[owner]!!; c.cancel(owner, old.id); start(); val replacement = c.attempts[owner]!!
        c.advance(owner, old.id, old.revision); c.cancel(owner, old.id); assertEquals(replacement, c.attempts[owner])
    }
    @Test fun voiceAndDictationShareAnOwnedLeaseAndStaleVoiceReleaseCannotClearDictation() {
        setup(); assertTrue(c.acquireVoice(owner, 100)); assertTrue(c.begin(owner, "Hello", 5, 5))
        assertEquals(DictationFailure.MicrophoneBusy, c.attempts[owner]!!.failure)
        c.releaseVoice(owner, 99); assertNotNull(c.lease); c.releaseVoice(owner, 100); assertNull(c.lease)
        start(); val lease = c.lease; assertFalse(c.acquireVoice(owner, 101)); c.releaseVoice(owner, 100); assertEquals(lease, c.lease)
    }
    @Test fun disclosureCancellationDoesNotAcquireMicrophoneOrAcceptConsent() {
        current = current.copy(settings = current.settings.copy(dictation = DictationPreferences()))
        setup(); assertTrue(c.begin(owner, "Hello", 5, 5)); val request = c.attempts[owner]!!
        assertEquals(DictationPhase.Disclosure, request.phase); assertNull(c.lease)
        c.cancel(owner, request.id); c.acceptDisclosure(owner, request.id)
        assertFalse(current.settings.dictation.disclosureAccepted); assertNull(c.lease)
    }
    @Test fun exactMissingSelectedServiceDoesNotUseAnotherInstalledService() {
        setup(); c.chooseScenario(DictationScenario.WrongService); start()
        assertEquals(DictationFailure.ServiceMissing, c.attempts[owner]!!.failure); assertNull(c.lease)
    }
    @Test fun partialServiceFailureRetainsTextForReview() {
        setup(); c.chooseScenario(DictationScenario.PartialThenFailure); start(); repeat(20) { advance() }
        assertEquals(DictationPhase.Review, c.attempts[owner]!!.phase)
        assertEquals(DictationFailure.Network, c.attempts[owner]!!.failure); assertTrue(c.attempts[owner]!!.retainedText.isNotBlank())
    }
    @Test fun silencePreferenceEndsAndPastesAfterTheLastSpeechBoundary() {
        current = current.copy(settings = current.settings.copy(dictation = DictationPreferences(3_000, disclosureAccepted = true)))
        setup(); start(); repeat(44) { advance() }; assertEquals(DictationPhase.Listening, c.attempts[owner]!!.phase)
        advance(); assertEquals(DictationPhase.Processing, c.attempts[owner]!!.phase)
        advance(); assertEquals(DictationPhase.Complete, c.attempts[owner]!!.phase)
    }
    @Test fun mismatchedDraftAtStartCannotCaptureAStaleSelection() {
        setup(); assertFalse(c.begin(owner, "Old text", 0, 3)); assertTrue(c.attempts.isEmpty()); assertNull(c.lease)
    }
}

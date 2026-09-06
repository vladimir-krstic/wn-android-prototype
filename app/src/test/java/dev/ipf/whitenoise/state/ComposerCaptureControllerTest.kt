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
    private fun inline(start: Int = 5, end: Int = start): Long {
        assertTrue(c.beginInline(owner, current.chats.single().draftText, start, end))
        return c.inlineDictation!!.id
    }
    @Test fun inlinePartialsReplaceTheUtteranceWithoutDuplicatingOrSending() {
        setup(); val id = inline()
        c.inlineReady(owner, id)
        c.inlineResult(owner, id, "good", false)
        assertEquals("Hello good", current.chats.single().draftText)
        c.inlineResult(owner, id, "good morning", false)
        assertEquals("Hello good morning", current.chats.single().draftText)
        assertEquals(InlineDictationPhase.Listening, c.inlineDictation!!.phase)
        assertEquals(18, c.insertion!!.value.cursor)
        assertTrue(sends.isEmpty())
    }
    @Test fun inlinePauseThenResumeUsesEditedTextAndNewSelection() {
        setup(); val oldId = inline()
        c.inlineResult(owner, oldId, "world", false); c.pauseInline(owner)
        edit("Hello dear world")
        val nextId = inline(6, 10)
        c.inlineResult(owner, oldId, "late result", true)
        assertEquals("Hello dear world", current.chats.single().draftText)
        c.inlineResult(owner, nextId, "bright", false)
        assertEquals("Hello bright world", current.chats.single().draftText)
        assertEquals(12, c.insertion!!.value.cursor)
    }
    @Test fun inlineFinalContinuesAtUtteranceEndAndNeverUsesLegacyAutoSend() {
        current = current.copy(settings = current.settings.copy(dictation = DictationPreferences(delivery = DictationDeliveryMode.Send)))
        setup(); val id = inline()
        assertTrue(c.attempts.isEmpty())
        c.inlineResult(owner, id, "world", true)
        val nextId = c.inlineDictation!!.id
        assertNotEquals(id, nextId)
        c.inlineResult(owner, id, "duplicate", true)
        c.inlineResult(owner, nextId, "again", false)
        assertEquals("Hello world again", current.chats.single().draftText)
        assertTrue(sends.isEmpty())
        c.endInline(owner)
        assertNull(c.inlineDictation); assertNull(c.lease)
        assertEquals("Hello world again", current.chats.single().draftText)
    }
    @Test fun inlineEditingDuringRecognitionPausesBeforeLateResultsCanOverwriteIt() {
        setup(); val id = inline(); c.inlineResult(owner, id, "world", false)
        edit("My correction")
        c.inlineResult(owner, id, "late", true)
        assertEquals("My correction", current.chats.single().draftText)
        assertEquals(InlineDictationPhase.Paused, c.inlineDictation!!.phase)
        assertNull(c.lease)
    }
    @Test fun inlineBackgroundAndNavigationPauseWithoutAutomaticRestart() {
        setup(); val id = inline(); c.inlineResult(owner, id, "world", false)
        c.background(); c.open(owner); c.inlineResult(owner, id, "late", true)
        assertFalse(c.inlineDictation!!.capturing); assertNull(c.lease)
        assertEquals("Hello world", current.chats.single().draftText)
        val resumed = inline(11); c.close(owner); c.open(owner)
        c.inlineResult(owner, resumed, "late", false)
        assertFalse(c.inlineDictation!!.capturing)
        assertEquals("Hello world", current.chats.single().draftText)
    }
    @Test fun inlineMembershipLossAndSignoutReleaseMicrophone() {
        setup(); val id = inline()
        current = current.copy(chats = listOf(current.chats.single().copy(membership = ChatMembership.Left)))
        c.reconcile(); c.inlineResult(owner, id, "late", false)
        assertNull(c.lease); assertFalse(c.inlineDictation!!.capturing)
        assertEquals("Hello", current.chats.single().draftText)
        signedIn = false; c.reconcile(); assertNull(c.inlineDictation)
    }
    @Test fun inlineFailureRetainsPartialsAndRetryRejectsOldProviderCallbacks() {
        setup(); val id = inline(); c.inlineResult(owner, id, "world", false)
        c.inlineFailure(owner, id, DictationFailure.Network)
        assertEquals(DictationFailure.Network, c.inlineDictation!!.failure); assertNull(c.lease)
        val resumed = inline(11)
        c.inlineFailure(owner, id, DictationFailure.Unknown)
        assertEquals(resumed, c.inlineDictation!!.id); assertNull(c.inlineDictation!!.failure)
        c.inlineResult(owner, resumed, "again", false)
        assertEquals("Hello world again", current.chats.single().draftText)
    }
    @Test fun emptyProviderEndpointsNeverShowFailureOrStopListening() {
        setup(); var id = inline(); c.inlineReady(owner, id)
        repeat(50) {
            if (it % 2 == 0) c.inlineRecognitionError(owner, id, DictationFailure.NoSpeech)
            else c.inlineResult(owner, id, "  ", final = true)
            val next = c.inlineDictation!!.id
            assertNotEquals(id, next)
            id = next; c.inlineReady(owner, id)
            assertNull(c.inlineDictation!!.failure)
            assertTrue(c.inlineDictation!!.capturing); assertNotNull(c.lease)
            assertEquals("Hello", current.chats.single().draftText)
        }
        c.inlineResult(owner, id, "finally", final = false)
        assertEquals("Hello finally", current.chats.single().draftText)
        assertNull(c.inlineDictation!!.failure); assertTrue(c.inlineDictation!!.capturing)
        assertTrue(sends.isEmpty())
    }
    @Test fun noSpeechFailureEntryAlsoContinuesSilently() {
        setup(); val id = inline(); c.inlineReady(owner, id)
        c.inlineFailure(owner, id, DictationFailure.NoSpeech)
        assertNotEquals(id, c.inlineDictation!!.id)
        assertNull(c.inlineDictation!!.failure); assertTrue(c.inlineDictation!!.capturing)
        assertNotNull(c.lease)
    }
    @Test fun silenceAfterFinalTextKeepsListeningAcrossTheNextUtterance() {
        setup(); val id = inline(); c.inlineReady(owner, id)
        c.inlineResult(owner, id, "world", final = true)
        var next = c.inlineDictation!!.id
        repeat(10) {
            c.inlineReady(owner, next)
            c.inlineRecognitionError(owner, next, DictationFailure.NoSpeech)
            next = c.inlineDictation!!.id
            assertTrue(c.inlineDictation!!.capturing); assertNull(c.inlineDictation!!.failure)
            assertEquals("Hello world", current.chats.single().draftText)
        }
        c.inlineResult(owner, next, "again", final = true)
        assertEquals("Hello world again", current.chats.single().draftText)
        assertNotNull(c.lease); assertTrue(sends.isEmpty())
    }
    @Test fun genericProviderErrorRetainsPartialsAndRejectsLateCallbacks() {
        setup(); val id = inline(); c.inlineReady(owner, id)
        c.inlineResult(owner, id, "world", final = false)
        c.inlineRecognitionError(owner, id, DictationFailure.Unknown)
        val next = c.inlineDictation!!.id
        assertNull(c.inlineDictation!!.failure); assertTrue(c.inlineDictation!!.capturing)
        c.inlineResult(owner, id, "late", final = true)
        c.inlineFailure(owner, id, DictationFailure.NoSpeech)
        assertEquals(next, c.inlineDictation!!.id)
        assertEquals("Hello world", current.chats.single().draftText)
    }
    @Test fun genericEmptyProviderErrorsRetryWithoutNoSpeechFeedback() {
        setup(); var id = inline()
        repeat(10) {
            c.inlineReady(owner, id)
            c.inlineRecognitionError(owner, id, DictationFailure.Unknown)
            id = c.inlineDictation!!.id
            assertNull(c.inlineDictation!!.failure); assertTrue(c.inlineDictation!!.capturing)
        }
        assertEquals("Hello", current.chats.single().draftText)
    }
    @Test fun pauseAndResumeRejectOldEmptyCallbacks() {
        setup(); val id = inline(); c.inlineReady(owner, id)
        c.inlineResult(owner, id, "world", final = false); c.pauseInline(owner)
        c.inlineRecognitionError(owner, id, DictationFailure.NoSpeech)
        assertFalse(c.inlineDictation!!.capturing); assertNull(c.lease)
        val resumed = inline(11); c.inlineReady(owner, resumed)
        c.inlineRecognitionError(owner, id, DictationFailure.NoSpeech)
        assertEquals(resumed, c.inlineDictation!!.id); assertTrue(c.inlineDictation!!.capturing)
        assertNull(c.inlineDictation!!.failure)
        assertEquals("Hello world", current.chats.single().draftText)
    }
    @Test fun inlineAndVoiceCannotHoldMicrophoneAtTheSameTime() {
        setup(); assertTrue(c.acquireVoice(owner, 100))
        assertFalse(c.beginInline(owner, "Hello", 5, 5))
        c.releaseVoice(owner, 100); inline()
        assertFalse(c.acquireVoice(owner, 101))
        c.pauseInline(owner); assertTrue(c.acquireVoice(owner, 101))
    }

}

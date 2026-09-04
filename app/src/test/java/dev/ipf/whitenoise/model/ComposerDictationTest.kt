package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class ComposerDictationTest {
    private val owner = ComposerCaptureOwner("p", "c")
    private fun draft(text: String, start: Int = text.length, end: Int = start) = DictationDraft(text, 0, start, end, emptyList(), null, null, PhotoQuality.High)
    private fun attempt(preferences: DictationPreferences = DictationPreferences(disclosureAccepted = true)) = DictationAttempt(1, owner, draft(""), preferences)
    @Test fun preferencesDefaultToManualAndPasteAndNormalizeUnknownSilence() {
        val p = DictationPreferences(); assertNull(p.finishAfterSilenceMillis); assertEquals(DictationDeliveryMode.Paste, p.delivery); assertFalse(p.disclosureAccepted)
        assertNull(p.withSilence(2_000).finishAfterSilenceMillis)
        DictationPreferences.silenceChoices.forEach { assertEquals(it, p.withSilence(it).finishAfterSilenceMillis) }
    }
    @Test fun exactSelectedServiceDoesNotFallBackToAnotherInstalledProvider() {
        val selected = DictationService.parse(" example.voice/.Recognition ")!!
        assertEquals(DictationService("example.voice", "example.voice.Recognition"), selected)
        assertTrue(DictationService.available(selected, listOf(selected)))
        assertFalse(DictationService.available(selected, listOf(DictationService("other", "other.Recognition"))))
        listOf(null, "", "service", "/Service", "package/", "a/b/c", "a/ b").forEach { assertNull(DictationService.parse(it)) }
    }
    @Test fun disclosureMustBeAcceptedBeforeReadinessAndCannotBeAcceptedTwice() {
        val pending = attempt(DictationPreferences()); assertEquals(pending, pending.ready(pending.revision))
        val accepted = pending.acceptDisclosure(); assertEquals(DictationPhase.Preparing, accepted.phase)
        assertEquals(accepted, accepted.acceptDisclosure()); assertEquals(accepted, accepted.ready(pending.revision))
    }
    @Test fun insertionReplacesCapturedSelectionAndSetsCursorBeforeRemainingText() {
        assertEquals(DictationTextResult("Meet at nine tomorrow", 12), DictationText.insert(draft("Meet at noon tomorrow", 8, 12), "nine"))
        assertEquals(DictationTextResult("Hello world.", 11), DictationText.insert(draft("Hello.", 5), "world"))
        assertEquals("Start new end", DictationText.insert(draft("Start OLD end", 9, 6), "new").text)
    }
    @Test fun insertionDoesNotSplitSurrogatesCombiningMarksFlagsOrJoinedEmoji() {
        for (cluster in listOf("👩🏽‍💻", "🇷🇸", "e\u0301", "😀")) {
            val source = "A $cluster Z"
            val replacement = DictationText.insert(draft(source, 3, source.length - 2), "word")
            assertEquals("A word Z", replacement.text)
            val caret = DictationText.insert(draft(source, 3), "word")
            assertTrue(caret.text.contains(cluster)); assertFalse(caret.text.any { it == '\uFFFD' })
        }
    }
    @Test fun appendPreservesCurrentDraftAndDoesNotAddDuplicateWhitespace() {
        assertEquals(DictationTextResult("Hello world", 11), DictationText.append("Hello", " world "))
        assertEquals("Hello\nworld", DictationText.append("Hello\n", "world").text)
        assertEquals("Hello", DictationText.append("Hello", " ").text)
    }
    @Test fun manualFinishIgnoresSilenceAndProviderSegmentsDoNotFinishIt() {
        var a = attempt().ready(0); a = a.segment(a.revision, "A sentence.", final = true)
        a = a.tick(a.revision, 30_000); assertEquals(DictationPhase.Listening, a.phase)
        assertEquals(DictationPhase.Processing, a.finish().phase)
    }
    @Test fun silenceFinishesAtTheCapturedBoundaryAndSpeakingResetsIt() {
        var a = attempt(DictationPreferences(3_000, disclosureAccepted = true)).ready(0)
        a = a.segment(a.revision, "First", final = false); a = a.tick(a.revision, 2_900)
        assertEquals(DictationPhase.Listening, a.phase)
        a = a.segment(a.revision, "First sentence", final = true); a = a.tick(a.revision, 2_900)
        assertEquals(DictationPhase.Listening, a.phase)
        a = a.tick(a.revision, 100); assertEquals(DictationPhase.Processing, a.phase); assertEquals("First sentence", a.retainedText)
    }
    @Test fun emptySilenceDoesNotInventSpeechAndPartialFailureRetainsText() {
        var a = attempt(DictationPreferences(3_000, disclosureAccepted = true)).ready(0)
        a = a.tick(a.revision, 10_000); assertEquals(DictationPhase.Listening, a.phase)
        a = a.segment(a.revision, "Some words", final = false)
        val failed = a.fail(a.revision, DictationFailure.Network)
        assertEquals(DictationPhase.Review, failed.phase); assertEquals("Some words", failed.retainedText)
        assertEquals(DictationReviewReason.RecognitionFailure, failed.reviewReason)
    }
    @Test fun revisionGuardsRejectRepeatedSegmentsTimersAndErrors() {
        val ready = attempt().ready(0); val segment = ready.segment(ready.revision, "Once", true)
        assertEquals(segment, segment.segment(ready.revision, "Again", true))
        assertEquals(segment, segment.tick(ready.revision, 100)); assertEquals(segment, segment.fail(ready.revision, DictationFailure.Network))
    }
    @Test fun cancellationAndCompletionClearPrivateTextAndRejectFurtherCallbacks() {
        val ready = attempt().ready(0); val spoken = ready.segment(ready.revision, "Private words", true)
        for (terminal in listOf(spoken.cancel(), spoken.complete())) {
            assertEquals("", terminal.retainedText); assertEquals(terminal, terminal.ready(terminal.revision))
            assertEquals(terminal, terminal.segment(terminal.revision, "Late words", true))
            assertEquals(terminal, terminal.interrupt())
        }
    }
    @Test fun interruptedCaptureRetainsTextButEmptyPendingCaptureCancels() {
        assertEquals(DictationPhase.Cancelled, attempt().interrupt().phase)
        val a = attempt().ready(0).let { it.segment(it.revision, "Words", false) }
        assertEquals(DictationPhase.Review, a.interrupt().phase); assertEquals("Words", a.interrupt().retainedText)
    }
    @Test fun deliveryRequiresOriginalOwnerPayloadRevisionAndEligibility() {
        val ready = attempt().ready(0); val a = ready.segment(ready.revision, "Words", true).finish()
        assertTrue(a.deliveryAllowed(a.draft, owner, true))
        assertFalse(a.deliveryAllowed(a.draft.copy(revision = 1), owner, true))
        assertFalse(a.deliveryAllowed(a.draft.copy(text = "Changed"), owner, true))
        assertFalse(a.deliveryAllowed(a.draft, owner.copy(chatId = "elsewhere"), true))
        assertFalse(a.deliveryAllowed(a.draft, owner, false))
    }
}

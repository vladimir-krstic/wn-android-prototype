package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class WritingToolsTest {
    private fun draft(text: String = "i recieve teh note tommorow.") = WritingDraft("me", "chat", text, text.length, text.length)
    private fun preview(draft: WritingDraft, selected: Boolean = false, operation: WritingOperation = WritingOperation.Proofread): WritingSession {
        val session = WritingTools.choose(WritingTools.begin(1, draft, 2, selected, WritingScenario.Ready)!!, operation)
        return WritingTools.finish(session, draft, 2)
    }
    @Test fun previewPreservesOriginalAndApplyChangesOnlyBoundDraft() {
        val source = draft()
        val session = preview(source)
        assertEquals(source.text, session.original)
        assertEquals("I receive the note tomorrow.", session.suggestion)
        val applied = WritingTools.apply(session, source, 2)!!
        assertEquals(session.suggestion, applied.text)
        assertEquals(applied.text.length, applied.selectionStart)
        assertEquals(applied.selectionStart, applied.selectionEnd)
    }
    @Test fun reversedSelectionPreservesSurroundingTextAndSelectsReplacement() {
        val source = draft("Prefix teh suffix").copy(selectionStart = 10, selectionEnd = 7)
        val applied = WritingTools.apply(preview(source, true), source, 2)!!
        assertEquals("Prefix the suffix", applied.text)
        assertEquals(7, applied.selectionStart)
        assertEquals(10, applied.selectionEnd)
    }
    @Test fun wholeDraftActionIgnoresExistingSelectionAndPlacesCursorAtEnd() {
        val source = draft().copy(selectionStart = 0, selectionEnd = 1)
        val session = preview(source)
        assertFalse(session.selectionOnly)
        val result = WritingTools.apply(session, source, 2)!!
        assertEquals(result.text.length, result.selectionStart)
        assertEquals(result.selectionStart, result.selectionEnd)
    }
    @Test fun rewriteAndSummaryAreDeterministic() {
        assertEquals("could you send this when you can?", preview(draft("can you send this asap?"), operation = WritingOperation.Rewrite).suggestion)
        assertEquals("Meeting at noon.", preview(draft("Meeting at noon. Bring lunch. We can walk afterwards."), operation = WritingOperation.Summarize).suggestion)
    }
    @Test fun sameTextAtANewerRevisionCannotApply() {
        val source = draft()
        assertNull(WritingTools.apply(preview(source), source, 3))
    }
    @Test fun otherProfileChatSelectionOrReplyCannotReceiveResult() {
        val source = draft()
        val session = preview(source)
        listOf(source.copy(profileId = "other"), source.copy(chatId = "other"), source.copy(selectionEnd = 0),
            source.copy(context = "new-reply"), source.copy(text = "new draft"), source.copy(editable = false)).forEach {
            assertNull(WritingTools.apply(session, it, 2))
        }
    }
    @Test fun lateCompletionIsStaleAndDropsSuggestion() {
        val source = draft()
        val loading = WritingTools.choose(WritingTools.begin(1, source, 2, false, WritingScenario.Ready)!!, WritingOperation.Proofread)
        val stale = WritingTools.finish(loading, source.copy(chatId = "other"), 2)
        assertEquals(WritingPhase.Stale, stale.phase)
        assertNull(stale.suggestion)
    }
    @Test fun protectedMarkdownLinksAndMentionsKeepTheirSource() {
        val text = "teh **teh** [teh](https://example.com/teh) `teh` https://example.com/teh\n@teh Person"
        assertEquals("the **teh** [teh](https://example.com/teh) `teh` https://example.com/teh\n@teh Person", preview(draft(text)).suggestion)
        assertEquals(text, preview(draft(text), operation = WritingOperation.Summarize).suggestion)
    }
    @Test fun selectingPartOfAMentionOrLinkDoesNotCorruptIt() {
        val source = draft("@teh Person").copy(selectionStart = 1, selectionEnd = 4)
        val session = preview(source, true)
        assertEquals("teh", session.suggestion)
        assertNull(WritingTools.apply(session, source, 2))
    }
    @Test fun emptyOrNonEditableDraftCannotStart() {
        assertNull(WritingTools.begin(1, draft("  "), 0, false, WritingScenario.Ready))
        assertNull(WritingTools.begin(1, draft().copy(editable = false), 0, false, WritingScenario.Ready))
        assertNull(WritingTools.begin(1, draft(), 0, true, WritingScenario.Ready))
    }
    @Test fun partialSurrogateSelectionCannotStart() {
        assertNull(WritingTools.begin(1, draft("🙂 teh").copy(selectionStart = 1, selectionEnd = 4), 0, true, WritingScenario.Ready))
    }
    @Test fun inputLimitAppliesToTheSelectedPassage() {
        val text = "teh" + "x".repeat(WritingTools.MaximumInput)
        assertEquals(WritingPhase.TooLong, WritingTools.begin(1, draft(text), 0, false, WritingScenario.Ready)!!.phase)
        assertEquals(WritingPhase.Choose, WritingTools.begin(1, draft(text).copy(selectionStart = 0, selectionEnd = 3), 0, true, WritingScenario.Ready)!!.phase)
    }
    @Test fun unavailableTimeoutAndRefusalNeverProduceText() {
        val source = draft()
        listOf(WritingScenario.Unavailable, WritingScenario.Timeout, WritingScenario.Refused, WritingScenario.TooLong).forEach { scenario ->
            val session = WritingTools.finish(WritingTools.choose(WritingTools.begin(1, source, 2, false, scenario)!!, WritingOperation.Rewrite), source, 2)
            assertNull(session.suggestion)
            assertNull(WritingTools.apply(session, source, 2))
        }
    }
    @Test fun externalProviderRequiresDisclosureBeforeAnyResult() {
        val source = draft()
        val disclosure = WritingTools.choose(WritingTools.begin(1, source, 2, false, WritingScenario.NetworkConsent)!!, WritingOperation.Rewrite)
        assertEquals(WritingPhase.NetworkConsent, disclosure.phase)
        assertEquals(disclosure, WritingTools.finish(disclosure, source, 2))
        assertNull(WritingTools.apply(disclosure, source, 2))
    }
    @Test fun downloadRequiresAnExplicitActionBeforeProgress() {
        val source = draft()
        val required = WritingTools.choose(WritingTools.begin(1, source, 2, false, WritingScenario.DownloadRequired)!!, WritingOperation.Proofread)
        assertEquals(WritingPhase.DownloadRequired, required.phase)
        assertEquals(required, WritingTools.finish(required, source, 2))
        assertEquals(WritingPhase.Loading, WritingTools.finish(required.copy(phase = WritingPhase.Downloading), source, 2).phase)
    }
    @Test fun unchangedAndRtlInputCanBeDiscardedWithoutAReplacement() {
        val source = draft("مرحبا، كيف حالك؟")
        val session = preview(source)
        assertEquals(source.text, session.suggestion)
        assertNull(WritingTools.apply(session, source, 2))
    }
}

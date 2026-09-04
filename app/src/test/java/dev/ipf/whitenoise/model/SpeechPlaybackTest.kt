package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class SpeechPlaybackTest {
    private val owner = SpeechOwner("profile", "chat")
    private fun queue(count: Int = 12, start: Int = 0, window: Int = 3) = SpeechSession.create(1, owner,
        (0 until count).map { SpeechItem("m$it", "First sentence. Second sentence.") }, "m$start", locale = Locale.US, windowSize = window)!!
    private fun only(text: String, limit: Int = 100) = SpeechSession.create(1, owner, listOf(SpeechItem("m", text)), "m",
        locale = Locale.US, chunkLimit = limit)!!

    @Test fun windowPreparesOnlyTheBoundedMessagesAroundTheStart() {
        val queue = queue(count = 100, start = 50, window = 8)
        assertEquals(8, queue.window.size); assertEquals(46, queue.windowStart)
        assertEquals("m50", queue.current.item.id); assertEquals(100, queue.catalog.size)
    }
    @Test fun pauseFreezesSentenceAndResumeInvalidatesEveryEarlierCallback() {
        val started = only("A long sentence with many words before its end.", 10)
        val advanced = started.done(started.token!!)
        assertEquals(1, advanced.chunkIndex)
        val old = advanced.token!!; val paused = advanced.pause()
        assertEquals(SpeechPhase.Paused, paused.phase); assertEquals(1, paused.chunkIndex)
        assertEquals(paused, paused.done(old)); assertEquals(paused, paused.range(old, 4)); assertEquals(paused, paused.fail(old))
        val resumed = paused.resume()
        assertEquals(started.chunk, resumed.chunk); assertNotEquals(old, resumed.token)
        assertEquals(resumed, resumed.done(old))
    }
    @Test fun duplicateDoneAndOldSessionCallbacksCannotSkipText() {
        val start = queue(); val token = start.token!!; val next = start.done(token)
        assertEquals(1, next.sentenceIndex); assertEquals(next, next.done(token))
        assertEquals(start, start.done(token.copy(sessionId = 0)))
    }
    @Test fun optionalRangeCallbacksAreMonotonicAndRejectOutOfBounds() {
        val start = queue(); val token = start.token!!
        val changed = start.range(token, 4)
        assertTrue(changed.progress > 0f); assertEquals(changed, changed.range(token, 2))
        assertEquals(changed, changed.range(token, 100_000)); assertEquals(changed, changed.range(token, -1))
        assertTrue(changed.done(token).progress > changed.progress)
    }
    @Test fun longEngineChunksRemainLosslessAndNeverSplitSurrogates() {
        val text = "A🦫 " .repeat(30) + "end."
        val session = only(text, 10)
        assertEquals(text, session.chunks.joinToString(""))
        assertTrue(session.chunks.all { it.length <= 10 && !it.first().isLowSurrogate() && !it.last().isHighSurrogate() })
    }
    @Test fun pausedSentenceAndMessageNavigationDoNotSpeakUntilResume() {
        val next = queue().pause().move(SpeechMove.NextSentence).move(SpeechMove.NextMessage)
        assertEquals(1, next.messageIndex); assertEquals(0, next.sentenceIndex); assertEquals(SpeechPhase.Paused, next.phase)
        assertNull(next.token); assertNotNull(next.resume().token)
    }
    @Test fun previousSentenceCrossesToLastSentenceOfPreviousMessage() {
        val session = queue(start = 1).move(SpeechMove.PreviousSentence)
        assertEquals(0, session.messageIndex); assertEquals(1, session.sentenceIndex)
    }
    @Test fun previousMessageRestartsThePreviousMessage() {
        val session = queue(start = 1).seek(1).move(SpeechMove.PreviousMessage)
        assertEquals(0, session.messageIndex); assertEquals(0, session.sentenceIndex)
    }
    @Test fun firstBoundaryReplaysFirstSentenceAndNewerEndCompletes() {
        val start = queue(1); val restarted = start.move(SpeechMove.PreviousSentence)
        assertEquals(0, restarted.messageIndex); assertNotEquals(start.token, restarted.token)
        val finished = start.move(SpeechMove.NextMessage)
        assertEquals(SpeechPhase.Completed, finished.phase); assertEquals(1f, finished.progress); assertNull(finished.token)
        assertNotNull(finished.returnTarget)
    }
    @Test fun newerEdgeLoadsTheNextWindowWithoutDuplicatingCursor() {
        val start = queue(start = 1, window = 2).move(SpeechMove.NextMessage)
        assertEquals(SpeechPhase.Loading, start.phase); assertEquals(1, start.messageIndex)
        val settled = start.settleEdge(start.revision, false)
        assertEquals(2, settled.messageIndex); assertEquals(2, settled.windowStart); assertEquals(2, settled.window.size)
        assertEquals(settled, settled.settleEdge(start.revision, false))
    }
    @Test fun earlierEdgeSelectsTheLastSentenceAndRetainsPause() {
        val session = queue(start = 5, window = 2).pause().move(SpeechMove.PreviousMessage).move(SpeechMove.PreviousSentence)
        assertEquals(SpeechPhase.Loading, session.phase)
        val settled = session.settleEdge(session.revision, false)
        assertEquals(3, settled.messageIndex); assertEquals(1, settled.sentenceIndex); assertEquals(SpeechPhase.Paused, settled.phase)
    }
    @Test fun edgeFailureRetainsSourceAndRetriesTheSameRequest() {
        val loading = queue(start = 1, window = 2).move(SpeechMove.NextMessage)
        val failed = loading.settleEdge(loading.revision, true)
        assertEquals(SpeechPhase.EdgeError, failed.phase); assertEquals(loading.current, failed.current)
        val retry = failed.retryEdge()
        assertEquals(failed.edge, retry.edge); assertNotEquals(failed.revision, retry.revision)
        assertEquals(retry, retry.settleEdge(loading.revision, false))
        assertEquals(2, retry.settleEdge(retry.revision, false).messageIndex)
    }
    @Test fun controlsAndCallbacksCannotMoveADeferringHistoryRequest() {
        val initial = queue(start = 1, window = 2); val token = initial.token!!
        val loading = initial.move(SpeechMove.NextMessage)
        assertEquals(loading, loading.move(SpeechMove.PreviousMessage)); assertEquals(loading, loading.seek(1))
        assertEquals(loading, loading.done(token)); assertEquals(loading, loading.fail(token))
    }
    @Test fun engineFailureKeepsTheCursorAndResumeReplaysItsSentence() {
        val session = queue().move(SpeechMove.NextSentence)
        val failed = session.fail(session.token!!)
        assertEquals(SpeechPhase.EngineError, failed.phase); assertEquals(session.passage, failed.passage)
        assertNotEquals(session.token, failed.resume().token); assertEquals(session.chunk, failed.resume().chunk)
    }
    @Test fun unavailableSourceHasNoReturnHighlightOrNavigation() {
        val session = queue().unavailable()
        assertNull(session.returnTarget); assertNull(session.passage); assertNull(session.token)
        assertEquals(session, session.resume()); assertEquals(session, session.move(SpeechMove.NextMessage))
    }
    @Test fun explicitSeekCanResumeWhileTransportSeekPreservesPause() {
        val paused = queue().pause()
        assertEquals(SpeechPhase.Paused, paused.seek(1).phase)
        assertEquals(SpeechPhase.Speaking, paused.seek(1, startPlaying = true).phase)
        assertEquals(paused, paused.seek(-1)); assertEquals(paused, paused.seek(2))
    }
    @Test fun sourceOffsetDistinguishesRepeatedMarkdownSentences() {
        val source = "**Same sentence.**\n\n*Same sentence.*"
        val session = SpeechSession.create(1, owner, listOf(SpeechItem("m", source)), "m", sourceOffset = source.lastIndexOf("Same"), locale = Locale.US)!!
        assertEquals(1, session.sentenceIndex)
        assertEquals(source.lastIndexOf("Same"), session.passage!!.sourceStart)
        assertEquals("Same sentence.", session.sentence.text)
    }
    @Test fun renderedProjectionDropsFormattingAndKeepsBlockBoundaries() {
        val text = "# Heading\n\n> Quoted\n\n- [x] Checked\n- Other\n\n| Key | Value |\n| --- | --- |\n| one | two |\n\n```txt\ncode\n```"
        val projected = SpeechDocuments.project(text)
        assertEquals(listOf("Heading", "Quoted", "Checked", "Other", "Key", "Value", "one", "two", "code"),
            SpeechDocuments.sentences(projected, Locale.US).map { it.text })
        assertTrue(projected.offsets.filter { it >= 0 }.all { it in text.indices })
    }
    @Test fun entitiesAndEmojiHighlightTheirFullAuthoredRange() {
        val source = "**Look &#x1F9AB; &amp; listen.**"
        val sentence = SpeechDocuments.sentences(SpeechDocuments.project(source), Locale.US).single()
        assertEquals("Look 🦫 & listen.", sentence.text)
        val entityStart = source.indexOf("&#")
        assertEquals(2, sentence.offsets.count { it == entityStart })
        assertTrue(SpeechDocuments.overlaps(sentence, entityStart until source.indexOf(';') + 1))
        assertEquals(source.indexOf("Look"), SpeechDocuments.range(sentence)!!.first)
    }
    @Test fun detailsAndLinksSpeakVisibleLabelsWithoutDestinationSyntax() {
        val source = "<details>\n<summary>Summary</summary>\n[Named link](https://example.org)\n</details>"
        val sentences = SpeechDocuments.sentences(SpeechDocuments.project(source), Locale.US)
        assertEquals(listOf("Summary", "Named link"), sentences.map { it.text })
        assertEquals(source.indexOf("Named"), SpeechDocuments.range(sentences.last())!!.first)
    }
    @Test fun selectedPassageDoesNotContinueIntoOtherTextOrMessages() {
        val text = "Before. **Read these words.** After."
        val start = text.indexOf("Read"); val end = text.indexOf("** After")
        val session = SpeechSession.create(1, owner, listOf(SpeechItem("m", text), SpeechItem("next", "Never here.")), "m",
            locale = Locale.US, selection = MessagePassage("Read these words.", start, end))!!
        assertEquals(1, session.catalog.size); assertEquals("Read these words.", session.chunk)
        assertEquals(start, session.passage!!.sourceStart)
        assertEquals(SpeechPhase.Completed, session.done(session.token!!).phase)
    }
    @Test fun plainFilesDoNotReinterpretMarkdownOrLoseLiteralCharacters() {
        val text = "**literal** <details> not markup."
        val session = SpeechSession.create(1, owner.copy(chatId = null, attachmentRequestId = "a"), listOf(SpeechItem("a", text, plain = true)), "a")!!
        assertEquals(text, session.chunk); assertNull(session.returnTarget)
    }
    @Test fun emptyMissingAndDuplicateStartCandidatesAreHandledDeterministically() {
        assertNull(SpeechSession.create(1, owner, listOf(SpeechItem("m", "  ")), "m"))
        assertNull(SpeechSession.create(1, owner, listOf(SpeechItem("m", "Text")), "missing"))
        val session = SpeechSession.create(1, owner, listOf(SpeechItem("m", "First."), SpeechItem("m", "Second.")), "m")!!
        assertEquals(1, session.catalog.size); assertEquals("First.", session.chunk)
    }
    @Test fun followStateNeverRequeuesOrChangesSpeechProgress() {
        val session = queue().range(queue().token!!, 4)
        val unfollowed = session.copy(following = false)
        assertEquals(session.token, unfollowed.token); assertEquals(session.progress, unfollowed.progress)
    }
    @Test fun authoredEligibilityIncludesBothDirectionsAndCaptionsButExcludesVoiceDeletedExpired() {
        val message = ChatMessage("m", "p", 1, "Today", 1, "Now", "Text")
        assertTrue(SpeechOwnership.eligible(message)); assertTrue(SpeechOwnership.eligible(message.copy(authorId = "other")))
        assertTrue(SpeechOwnership.eligible(message.copy(attachments = listOf(MessageAttachment("a", MessageAttachmentKind.Photo, "Photo")))))
        assertFalse(SpeechOwnership.eligible(message.copy(text = " ")))
        assertFalse(SpeechOwnership.eligible(message.copy(deletionState = MessageDeletionState.DeletedByOther)))
        assertFalse(SpeechOwnership.eligible(message.copy(expiresAtMillis = MessageForwarding.nowMillis)))
        assertFalse(SpeechOwnership.eligible(message.copy(attachments = listOf(MessageAttachment("a", MessageAttachmentKind.Voice, "Voice")))))
    }
    @Test fun longSentenceHighlightFollowsChunkAndNativeWordOffsetsThenFreezesOnPause() {
        val start = only("First long sentence with several words and a final period.", 12)
        val next = start.done(start.token!!)
        val prefix = start.chunk.length
        assertEquals(prefix, next.passage!!.sourceStart)
        val word = next.range(next.token!!, 5, 2)
        assertEquals(prefix + 2, word.passage!!.sourceStart)
        assertEquals(prefix + 5, word.passage!!.sourceEnd)
        val paused = word.pause()
        assertEquals(word.passage, paused.passage); assertEquals(word.progress, paused.progress)
        val resumed = paused.resume()
        assertEquals(0, resumed.passage!!.sourceStart)
        assertEquals(0f, resumed.progress)
    }
    @Test fun titleAbbreviationsStayWithNamesButNeverCrossAuthoredBlocks() {
        val source = SourceText.from("Dr. Smith arrived. Prof. Jones stayed.\nMr.\nNew block")
        val sentences = SpeechDocuments.sentences(source, Locale.US)
        assertEquals(listOf("Dr. Smith arrived.", "Prof. Jones stayed.", "Mr.", "New block"), sentences.map { it.text })
        assertEquals("Dr.", SpeechDocuments.sentences(SourceText.from("Dr."), Locale.US).single().text)
    }

    @Test fun completionEndsHighlightAndFollowingButRetainsExplicitSourceReturn() {
        val completed = queue(1).move(SpeechMove.NextMessage)
        assertEquals(SpeechPhase.Completed, completed.phase); assertFalse(completed.following)
        assertNull(completed.passage); assertNotNull(completed.returnTarget)
        assertEquals(SpeechPhase.Speaking, completed.seek(0).phase)
    }

}

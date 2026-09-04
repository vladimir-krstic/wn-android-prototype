package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class TranscriptStateTest {
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private fun owner(vm: AppViewModel) = GroupOwner(vm.uiState.activeProfileId!!, "fiatjaf")
    private fun ready(vm: AppViewModel): TranscriptWork {
        assertTrue(vm.transcript.begin(owner(vm)))
        while (vm.transcript.work!!.phase == TranscriptPhase.Reading) vm.transcript.advance(vm.transcript.work!!.id)
        val w = vm.transcript.work!!; assertEquals(TranscriptPhase.Encoding, w.phase)
        vm.transcript.encoded(w.id, ConversationTranscript.encode(w.source, w.entries)); return vm.transcript.work!!
    }
    @Test fun fullTranscriptContainsOlderHistoryBeyondUiWindowAndDeduplicatesStableIds() {
        val vm = model(); val p = vm.uiState.activeProfile!!; val chat = p.chats.first { it.id == "fiatjaf" }
        val message = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().first().message
        val entries = (0..409).map { ChatTimelineEntry.Message(message.copy(id = "m$it", createdAtMillis = 10_000L + it)) }
        val source = TranscriptSource.capture(p, chat.copy(timeline = entries.reversed() + entries.first()))
        val ordered = ConversationTranscript.ordered(source)
        assertEquals(410, ordered.size); assertEquals("m0", ordered.first().id); assertEquals("m409", ordered.last().id)
        val document = ConversationTranscript.encode(source, ordered)
        assertTrue(document.contains("\"event_count\":410")); assertTrue(document.indexOf("\"id\":\"m0\"") < document.indexOf("\"id\":\"m409\""))
    }
    @Test fun timestampTiesUseMessageIdRatherThanRenderedOrder() {
        val vm = model(); val p = vm.uiState.activeProfile!!; val chat = vm.chat("fiatjaf")!!; val m = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().first().message
        val source = TranscriptSource.capture(p, chat.copy(timeline = listOf(ChatTimelineEntry.Message(m.copy(id = "z", createdAtMillis = 1234)), ChatTimelineEntry.Message(m.copy(id = "a", createdAtMillis = 1234)))))
        assertEquals(listOf("a", "z"), ConversationTranscript.ordered(source).map { it.id })
    }
    @Test fun pendingEditAndDraftNeverReplaceAcceptedAuthoredContent() {
        val vm = model(); val owner = owner(vm); vm.sendText(owner.chatId, "Original accepted text")
        val message = vm.chat(owner.chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message
        vm.beginMessageEdit(owner.profileId, owner.chatId, message.id, "Accepted revision")
        vm.advanceMessageEdit(owner.profileId, owner.chatId, message.id, vm.chat(owner.chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message.editAttempt!!.id)
        vm.beginMessageEdit(owner.profileId, owner.chatId, message.id, "PRIVATE PENDING EDIT")
        vm.updateDraftText(owner.chatId, "PRIVATE UNSENT DRAFT")
        val w = ready(vm); val doc = w.document!!
        assertTrue(doc.contains("Original accepted text")); assertTrue(doc.contains("Accepted revision")); assertTrue(doc.contains(vm.uiState.activeProfile!!.publicKey))
        assertFalse(doc.contains("PRIVATE PENDING EDIT")); assertFalse(doc.contains("PRIVATE UNSENT DRAFT"))
    }
    @Test fun deletedMessageDoesNotLeakOriginalOrAcceptedRevisionBody() {
        val vm = model(); val p = vm.uiState.activeProfile!!; val chat = vm.chat("fiatjaf")!!; val m = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().first().message
        val deleted = m.copy(text = "SECRET", deletionState = MessageDeletionState.DeletedByCurrentProfile,
            editHistory = MessageEditHistory("ORIGINAL SECRET", 1, listOf(MessageRevision(1, "REVISION SECRET", 2))))
        val source = TranscriptSource.capture(p, chat.copy(timeline = listOf(ChatTimelineEntry.Message(deleted))))
        val doc = ConversationTranscript.encode(source, ConversationTranscript.ordered(source))
        assertFalse(doc.contains("SECRET")); assertTrue(doc.contains("DeletedByCurrentProfile")); assertTrue(doc.contains(m.id))
    }
    @Test fun jsonEscapesQuotesBackslashesControlsAndUnicodeWithoutChangingText() {
        assertEquals("\"a\\\"b\\\\c\\n\\t\\u0001\\ud83d\\ude00\"", ConversationTranscript.json("a\"b\\c\n\t\u0001😀"))
    }
    @Test fun savingOnlyCompletesAfterTheDestinationWrite() {
        val vm = model(); val w = ready(vm)
        assertEquals(TranscriptPhase.Ready, w.phase); vm.transcript.save(w.id)
        assertEquals(TranscriptPhase.ChoosingDestination, vm.transcript.work!!.phase)
        assertNotNull(vm.transcript.takeForWriting(w.id)); assertEquals(TranscriptPhase.Writing, vm.transcript.work!!.phase)
        assertNull(vm.transcript.takeForWriting(w.id)); assertTrue(vm.transcript.saved(w.id, true)); assertEquals(TranscriptPhase.Saved, vm.transcript.work!!.phase)
        assertNull(vm.transcript.work!!.document)
    }
    @Test fun preparationCanCancelAndLateCompletionCannotResurrectIt() {
        val vm = model(); vm.transcript.begin(owner(vm)); val w = vm.transcript.work!!
        vm.transcript.cancel(w.id); vm.transcript.advance(w.id); vm.transcript.encoded(w.id, "stale")
        assertEquals(TranscriptPhase.Cancelled, vm.transcript.work!!.phase); assertNull(vm.transcript.work!!.document)
    }
    @Test fun systemPickerCancellationDoesNotReportSaved() {
        val vm = model(); val w = ready(vm); vm.transcript.save(w.id); vm.transcript.cancel(w.id)
        assertEquals(TranscriptPhase.Cancelled, vm.transcript.work!!.phase); assertNull(vm.transcript.takeForWriting(w.id))
    }
    @Test fun changedSourceInvalidatesPreparedExportBeforeWriting() {
        val vm = model(); val w = ready(vm); vm.transcript.save(w.id); vm.sendText("fiatjaf", "History changed")
        assertNull(vm.transcript.takeForWriting(w.id)); assertEquals(TranscriptFailure.SourceUnavailable, vm.transcript.work!!.failure)
    }
    @Test fun profileRoundTripRejectsOldPickerAndDoesNotReattachItToNewExport() {
        val vm = model(); val owner = owner(vm); val w = ready(vm); vm.transcript.save(w.id)
        vm.completeSignIn(OnboardingOrigin.AddProfile); vm.selectProfile(owner.profileId)
        assertNull(vm.transcript.takeForWriting(w.id)); val fresh = ready(vm); assertNotEquals(w.id, fresh.id)
        vm.transcript.save(fresh.id); assertNull(vm.transcript.takeForWriting(w.id)); assertNotNull(vm.transcript.takeForWriting(fresh.id))
    }
    @Test fun changedProfileDuringWriteRejectsSuccessSoHandoffCanRemovePartialOutput() {
        val vm = model(); val w = ready(vm); vm.transcript.save(w.id); vm.transcript.takeForWriting(w.id)
        vm.completeSignIn(OnboardingOrigin.AddProfile); assertFalse(vm.transcript.saved(w.id, true)); assertNull(vm.transcript.work)
    }
    @Test fun writeFailureAndInterruptedWriteAreRetryableWithoutFalseSuccess() {
        val vm = model(); val w = ready(vm); vm.transcript.save(w.id); vm.transcript.takeForWriting(w.id); vm.transcript.saved(w.id, false)
        assertEquals(TranscriptFailure.Write, vm.transcript.work!!.failure); assertTrue(vm.transcript.retry(w.id))
        assertEquals(TranscriptPhase.Reading, vm.transcript.work!!.phase)
        vm.transcript.cancel(vm.transcript.work!!.id); val fresh = ready(vm); vm.transcript.save(fresh.id); vm.transcript.takeForWriting(fresh.id)
        vm.transcript.interruptWriting(); assertEquals(TranscriptFailure.Write, vm.transcript.work!!.failure); assertFalse(vm.transcript.saved(fresh.id, true))
    }
    @Test fun unavailableAndPreparationFailureRemainDistinctAndRetryUsesNewSnapshot() {
        for (scenario in listOf(TranscriptScenario.SourceUnavailable, TranscriptScenario.PreparationFailure)) {
            val vm = model(); vm.transcript.choose(scenario); vm.transcript.begin(owner(vm)); val id = vm.transcript.work!!.id
            vm.transcript.advance(id); assertEquals(if (scenario == TranscriptScenario.SourceUnavailable) TranscriptFailure.SourceUnavailable else TranscriptFailure.Preparation, vm.transcript.work!!.failure)
            assertTrue(vm.transcript.retry(id)); assertNotEquals(id, vm.transcript.work!!.id)
        }
    }
    @Test fun emptyHistoryIsAValidDocument() {
        val vm = model(); val p = vm.uiState.activeProfile!!; val chat = vm.chat("fiatjaf")!!.copy(timeline = emptyList())
        val source = TranscriptSource.capture(p, chat); assertTrue(ConversationTranscript.encode(source, ConversationTranscript.ordered(source)).contains("\"event_count\":0"))
    }
    @Test fun missingSaveDestinationIsAnExplicitFailure() {
        val vm = model(); val w = ready(vm); vm.transcript.save(w.id); vm.transcript.destinationFailed(w.id)
        assertEquals(TranscriptFailure.Destination, vm.transcript.work!!.failure); assertNull(vm.transcript.takeForWriting(w.id))
    }
    @Test fun onlyOneExportOwnsPreparationAndWriting() {
        val vm = model(); assertTrue(vm.transcript.begin(owner(vm))); assertFalse(vm.transcript.begin(owner(vm)))
        vm.transcript.cancel(vm.transcript.work!!.id); val w = ready(vm); vm.transcript.save(w.id); vm.transcript.takeForWriting(w.id)
        assertFalse(vm.transcript.begin(owner(vm))); vm.transcript.cancel(w.id); assertEquals(TranscriptPhase.Writing, vm.transcript.work!!.phase)
    }
    @Test fun readerAdvancesAllThreePagesBeforeEncodingAndNeverUsesUiWindow() {
        val vm = model(); val p = vm.uiState.activeProfile!!; val chat = vm.chat("fiatjaf")!!
        val m = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().first().message
        val full = chat.copy(timeline = (1..405).map { ChatTimelineEntry.Message(m.copy(id = "entry-$it", createdAtMillis = it.toLong())) })
        val controller = TranscriptController { p.copy(chats = listOf(full)) }
        assertTrue(controller.begin(GroupOwner(p.id, chat.id))); val id = controller.work!!.id
        controller.advance(id); assertEquals(200, controller.work!!.readCount); assertEquals(TranscriptPhase.Reading, controller.work!!.phase)
        controller.advance(id); assertEquals(400, controller.work!!.readCount); controller.advance(id)
        assertEquals(405, controller.work!!.entries.size); assertEquals(TranscriptPhase.Encoding, controller.work!!.phase)
    }
    @Test fun endedAndFrozenHistoryRemainExportable() {
        val vm = model(); val p = vm.uiState.activeProfile!!; val id = vm.createGroup("Trail", "", ProfileAvatar.Monogram, emptyList())!!
        val owner = GroupOwner(p.id, id)
        for (state in listOf(GroupStateScenario.Frozen, GroupStateScenario.Ended)) {
            vm.groupLifecycle.chooseState(state); vm.groupLifecycle.open(owner)
            assertTrue(vm.transcript.begin(owner)); val w = vm.transcript.work!!; vm.transcript.advance(w.id)
            assertEquals(TranscriptPhase.Encoding, vm.transcript.work!!.phase); vm.transcript.cancel(w.id)
        }
    }

}

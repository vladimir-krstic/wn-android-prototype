package dev.ipf.whitenoise.ui.conversation

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.AppViewModel
import org.junit.Assert.*
import org.junit.Test

class RetentionSpeechTest {
    @Test fun expiryStopsActiveSpeechAndRejectsOldReturnAndCompletionCallbacks() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
        val chatId = vm.createGroup("Trail", "", ProfileAvatar.Monogram, emptyList())!!
        vm.setChatDisappearing(chatId, DisappearingDuration.ThirtySeconds); vm.sendText(chatId, "Read this message.")
        val message = vm.chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message
        var spoken = 0; var returns = 0
        val speech = ReadAloudController().apply {
            profile = { vm.uiState.activeProfile }; attachTestOutput({ _, _ -> spoken++; true }); onSource = { returns++ }
        }
        speech.startConversation(vm.uiState.activeProfile!!, vm.chat(chatId)!!, message.id)
        val token = speech.session!!.token!!; val target = speech.session!!.returnTarget!!
        vm.retention.advanceExampleClock(30_000); speech.reconcile()
        assertEquals(SpeechPhase.Unavailable, speech.session!!.phase); assertNull(speech.session!!.returnTarget)
        speech.done(token); speech.returnToSource(target); speech.resume()
        assertEquals(1, spoken); assertEquals(0, returns); assertEquals(SpeechPhase.Unavailable, speech.session!!.phase)
    }
    @Test fun expiredQueuedMessageIsNeverSubmittedAfterTheCurrentMessage() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
        val chatId = vm.createGroup("Trail", "", ProfileAvatar.Monogram, emptyList())!!
        vm.sendText(chatId, "Keep reading."); val first = vm.chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().id
        vm.setChatDisappearing(chatId, DisappearingDuration.ThirtySeconds); vm.sendText(chatId, "Temporary text.")
        val spoken = mutableListOf<String>()
        val speech = ReadAloudController().apply { profile = { vm.uiState.activeProfile }; attachTestOutput({ text, _ -> spoken += text; true }) }
        speech.startConversation(vm.uiState.activeProfile!!, vm.chat(chatId)!!, first)
        vm.retention.advanceExampleClock(30_000); speech.move(SpeechMove.NextMessage)
        assertFalse(spoken.any { it.contains("Temporary") }); assertEquals(SpeechPhase.Unavailable, speech.session!!.phase)
    }
}

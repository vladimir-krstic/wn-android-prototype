package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class GlobalSearchStateTest {
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
    @Test fun exactTargetOpensAndOnlyMarksItsChatRead() {
        val vm = model(); val owner = vm.uiState.activeProfile!!
        val chat = owner.chats.first { it.timeline.any { entry -> entry is ChatTimelineEntry.Message && !entry.message.isDeleted } }
        val message = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().first { !it.message.isDeleted }.message
        vm.markChatUnread(chat.id, true)
        assertTrue(vm.openGlobalSearchMessage(owner.id, chat.id, message.id))
        assertFalse(vm.uiState.activeProfile!!.chats.first { it.id == chat.id }.isUnread)
        assertEquals(chat.timeline, vm.uiState.activeProfile!!.chats.first { it.id == chat.id }.timeline)
        assertEquals(owner.chats.filterNot { it.id == chat.id }, vm.uiState.activeProfile!!.chats.filterNot { it.id == chat.id })
    }
    @Test fun missingAndDeletedTargetsCannotOpenOrMarkChatRead() {
        val vm = model(); val owner = vm.uiState.activeProfile!!
        val chat = owner.chats.first { it.timeline.any { entry -> entry is ChatTimelineEntry.Message } }
        val message = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().first().message
        vm.deleteMessages(chat.id, setOf(message.id), MessageDeletionScope.ForMe); vm.markChatUnread(chat.id, true)
        val before = vm.uiState
        assertFalse(vm.openGlobalSearchMessage(owner.id, chat.id, message.id)); assertFalse(vm.openGlobalSearchMessage(owner.id, "gone", message.id))
        assertFalse(vm.openGlobalSearchMessage(owner.id, chat.id, "gone")); assertEquals(before, vm.uiState)
    }
    @Test fun staleOwnerCannotOpenSearchResultAfterSwitch() {
        val vm = model(); val owner = vm.uiState.activeProfile!!
        val chat = owner.chats.first { it.timeline.any { entry -> entry is ChatTimelineEntry.Message } }
        val message = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().first().message
        vm.completeSignIn(OnboardingOrigin.AddProfile); val before = vm.uiState
        assertFalse(vm.openGlobalSearchMessage(owner.id, chat.id, message.id)); assertEquals(before, vm.uiState)
    }
    @Test fun voiceScenarioRequiresDeveloperToolsAndStaleOwnerDoesNotConsumeIt() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!
        vm.selectGlobalVoiceScenario(GlobalVoiceScenario.Unavailable)
        assertEquals(GlobalVoiceScenario.Success, vm.nextGlobalVoiceScenario)
        vm.setDeveloperToolsEnabled(true); vm.selectGlobalVoiceScenario(GlobalVoiceScenario.Cancelled)
        assertEquals(GlobalVoiceScenario.Unavailable, vm.consumeGlobalVoiceScenario("wrong"))
        assertEquals(GlobalVoiceScenario.Cancelled, vm.consumeGlobalVoiceScenario(owner))
        assertEquals(GlobalVoiceScenario.Success, vm.consumeGlobalVoiceScenario(owner))
    }
}

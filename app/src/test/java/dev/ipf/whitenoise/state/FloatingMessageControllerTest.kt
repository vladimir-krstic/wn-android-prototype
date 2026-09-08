package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class FloatingMessageControllerTest {
    private val first = ChatMessage("one", "other", 3, "Today", 600, "10:00 AM", "First")
    private val second = first.copy(id = "two", text = "Second")
    private fun profile(id: String) = Profile(id, id, "key-$id", chats = listOf(
        Chat("chat", 0, ChatKind.Direct("other"), "Chat", timeline = listOf(first, second).map { ChatTimelineEntry.Message(it) })
    ))
    private var profiles = listOf(profile("a"), profile("b"))
    private var active: String? = "a"
    private var signedIn = setOf("a", "b")
    private var now = 1_000L
    private val controller = FloatingMessageController({ profiles }, { active }, { signedIn }, { now })
    private fun updateMessage(transform: (ChatMessage) -> ChatMessage) {
        profiles = profiles.map { profile -> profile.copy(chats = profile.chats.map { chat ->
            chat.copy(timeline = chat.timeline.map { row -> if (row is ChatTimelineEntry.Message) ChatTimelineEntry.Message(transform(row.message)) else row })
        }) }
    }

    @Test fun addingSelectsNewestAndDuplicatesKeepOriginalOrder() {
        assertTrue(controller.keep("a", "chat", "one"))
        controller.keep("a", "chat", "two")
        assertEquals("two", controller.selected!!.messageId)
        controller.keep("a", "chat", "one")
        assertEquals(listOf("one", "two"), controller.entries.map { it.message.id })
        assertEquals("one", controller.selected!!.messageId)
        controller.remove(controller.selected!!)
        assertEquals("two", controller.selected!!.messageId)
        controller.remove(controller.selected!!)
        assertNull(controller.selected)
        assertTrue(controller.entries.isEmpty())
    }

    @Test fun accountsWithIdenticalMessageIdsHaveSeparateStacksAndSelections() {
        controller.keep("a", "chat", "one")
        controller.keep("a", "chat", "two")
        val aSelection = controller.selected!!
        active = "b"
        assertTrue(controller.entries.isEmpty())
        assertFalse(controller.keep("a", "chat", "one"))
        controller.keep("b", "chat", "one")
        controller.remove(aSelection)
        controller.select(aSelection)
        assertEquals("b", controller.selected!!.profileId)
        controller.clear()
        active = "a"
        assertEquals(aSelection, controller.selected)
        assertEquals(2, controller.entries.size)
    }

    @Test fun resolvesEditsLiveAndDropsDeletedExpiredAndMissingSources() {
        controller.keep("a", "chat", "one")
        controller.keep("a", "chat", "two")
        updateMessage { if (it.id == "one") it.copy(text = "Edited") else it }
        assertEquals("Edited", controller.entries.first().message.text)
        updateMessage { if (it.id == "two") it.copy(deletionState = MessageDeletionState.DeletedByOther) else it }
        controller.reconcile()
        assertEquals("one", controller.selected!!.messageId)
        updateMessage { it.copy(expiresAtMillis = 2_000L) }
        now = 2_000L
        assertTrue(controller.entries.isEmpty())
        controller.reconcile()
        assertNull(controller.selected)
        assertFalse(controller.keep("a", "chat", "one"))
        assertFalse(controller.keep("a", "missing", "one"))
        assertFalse(controller.keep("a", "chat", "missing"))
        now = 1_000L
        controller.keep("a", "chat", "one")
        profiles = profiles.map { it.copy(chats = emptyList()) }
        controller.reconcile()
        assertTrue(controller.entries.isEmpty())
    }

    @Test fun signingOutClearsStackRatherThanRestoringItOnSignIn() {
        controller.keep("a", "chat", "one")
        signedIn = setOf("b")
        controller.reconcile()
        signedIn = setOf("a", "b")
        assertTrue(controller.entries.isEmpty())
    }

    @Test fun appSignOutClearsReferencesImmediatelyWithoutAUiEffect() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
        val owner = vm.uiState.activeProfileId!!
        assertTrue(vm.floatingMessages.keep(owner, "maya-chen", "maya-shared-photo"))
        vm.signOutActiveProfile(wipeData = false)
        vm.completeSignIn(OnboardingOrigin.Initial)
        assertTrue(vm.floatingMessages.entries.isEmpty())
    }

    @Test fun keepingAMessageNeverChangesSharedPinPermissionsOrChatPins() {
        val before = profiles
        controller.keep("a", "chat", "one")
        assertEquals(before, profiles)
        assertTrue(MessageAction.KeepOnScreen in MessageActionPolicy.available(first, "a"))
    }
}

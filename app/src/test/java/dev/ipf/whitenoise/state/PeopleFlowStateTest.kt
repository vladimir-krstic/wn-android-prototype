package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class PeopleFlowStateTest {
    private fun signedIn() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }

    @Test fun privateDetailsAreIsolatedAndRestorePublishedName() {
        val vm = signedIn()
        val owner = vm.uiState.activeProfile!!
        val person = owner.people.first()
        val chatId = vm.openOrCreateDirectChat(person.id)!!
        assertTrue(vm.savePrivateContact(owner.id, person.id, "Local Friend", "Private note"))
        assertEquals(person.name, vm.person(person.id)!!.name)
        assertEquals("Local Friend", vm.person(person.id)!!.displayName)
        assertEquals("Local Friend", vm.chat(chatId)!!.title)
        assertTrue(ChatProjection.rows(vm.uiState.activeProfile!!.chats, ChatScope.Chats, "Local Friend").any { it.id == chatId })
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertFalse(vm.savePrivateContact(owner.id, person.id, "Wrong owner", ""))
        assertFalse(vm.uiState.activeProfile!!.people.any { it.nickname == "Local Friend" })
        vm.selectProfile(owner.id)
        assertEquals("Private note", vm.person(person.id)!!.privateNotes)
        assertTrue(vm.savePrivateContact(owner.id, person.id, "", ""))
        assertEquals(person.name, vm.chat(chatId)!!.title)
    }
    @Test fun nicknameUpdatesExistingContactPayloadAndDraftWithoutNotes() {
        val vm = signedIn(); val profile = vm.uiState.activeProfile!!; val person = profile.people.first()
        val id = vm.openOrCreateDirectChat(person.id)!!
        vm.addDraftAttachments(id, listOf(MessageAttachment("contact-test", MessageAttachmentKind.Contact, "Contact: ${person.name}", contactPersonId = person.id)))
        vm.savePrivateContact(profile.id, person.id, "Known Friend", "NEVER SHARE")
        assertEquals("Contact: Known Friend", vm.chat(id)!!.draftAttachments.single().label)
        assertFalse(vm.chat(id)!!.draftAttachments.single().label.contains("NEVER SHARE"))
    }
    @Test fun discoveredPersonIsDeduplicatedAndStaleOwnerIsRejected() {
        val vm = signedIn(); val owner = vm.uiState.activeProfileId!!; val person = PeopleDiscovery.directory.first()
        assertEquals(person.id, vm.acceptDiscoveredPerson(owner, person))
        assertEquals(person.id, vm.acceptDiscoveredPerson(owner, person.copy(id = "alias")))
        assertEquals(1, vm.uiState.activeProfile!!.people.count { it.publicKey == person.publicKey })
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertNull(vm.acceptDiscoveredPerson(owner, PeopleDiscovery.directory.last()))
    }
    @Test fun createdGroupIsNotDuplicatedWhenOpeningIsRetried() {
        val vm = signedIn(); val person = vm.uiState.activeProfile!!.people.first().id
        vm.setCreatedChatUnavailable(true)
        val first = vm.startGroupConversation("New group", "", ProfileAvatar.Monogram, listOf(person), "setup")!!
        assertTrue(vm.createdChatProjectionUnavailable)
        assertEquals(first, vm.startGroupConversation("Changed draft", "", ProfileAvatar.Monogram, listOf(person), "setup"))
        assertEquals(1, vm.uiState.activeProfile!!.chats.count { it.id == first.chatId })
        assertEquals(first.chatId, vm.completeCreatedChatOpen(first.id, "setup"))
        assertNull(vm.completeCreatedChatOpen(first.id, "setup"))
    }
    @Test fun pendingOpenCannotCrossRouteOrProfileAndDoesNotUndoCreation() {
        val vm = signedIn(); val owner = vm.uiState.activeProfileId!!; val person = vm.uiState.activeProfile!!.people.first().id
        val first = vm.startDirectConversation(person, "person")!!
        assertNull(vm.completeCreatedChatOpen(first.id, "wrong-route"))
        vm.reconcileCreatedChatOrigin("another")
        assertNull(vm.createdChatOpen)
        assertNotNull(vm.chat(first.chatId))
        val second = vm.startDirectConversation(person, "person")!!
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertNull(vm.completeCreatedChatOpen(second.id, "person"))
        vm.selectProfile(owner)
        assertNull(vm.createdChatOpen)
    }
    @Test fun groupEligibilityRequiresActiveAuthoritativeAdminAndTargetMembership() {
        val vm = signedIn(); val profile = vm.uiState.activeProfile!!
        val target = profile.people.first().id
        val id = vm.createGroup("Eligible", "", ProfileAvatar.Monogram, listOf(target))!!
        val chat = vm.chat(id)!!
        assertTrue(GroupContactPolicy.eligible(vm.uiState.activeProfile!!, target, GroupContactAction.Promote).any { it.id == id })
        assertFalse(GroupContactPolicy.eligible(vm.uiState.activeProfile!!, target, GroupContactAction.Invite).any { it.id == id })
        for (invalid in listOf(chat.copy(membership = ChatMembership.Left), chat.copy(members = chat.members.map { it.copy(role = GroupRole.Member) }))) {
            assertTrue(GroupContactPolicy.eligible(profile.copy(chats = listOf(invalid)), target, GroupContactAction.Promote).isEmpty())
        }
    }
    @Test fun partialGroupApplicationRetriesOnlyFailuresAndPreservesSuccess() {
        val vm = signedIn(); val profile = vm.uiState.activeProfile!!
        val target = PeopleDiscovery.directory.first(); vm.acceptDiscoveredPerson(profile.id, target)
        val member = profile.people.first().id
        val ids = (1..2).map { vm.createGroup("Group $it", "", ProfileAvatar.Monogram, listOf(member))!! }
        vm.selectGroupContactScenario(GroupContactScenario.PartialApply)
        val result = vm.applyContactToGroups(profile.id, target.id, ids, GroupContactAction.Invite)
        assertEquals(listOf(ids.first()), result.completed); assertEquals(listOf(ids.last()), result.failed)
        assertTrue(vm.chat(ids.first())!!.members.any { it.personId == target.id })
        val retry = vm.applyContactToGroups(profile.id, target.id, result.failed, GroupContactAction.Invite)
        assertTrue(retry.failed.isEmpty())
        assertTrue(ids.all { vm.chat(it)!!.members.count { it.personId == target.id } == 1 })
    }
    @Test fun rosterFailureAndStaleOwnershipCannotMutateGroups() {
        val vm = signedIn(); val profile = vm.uiState.activeProfile!!; val target = profile.people.first().id
        val id = vm.createGroup("Promote", "", ProfileAvatar.Monogram, listOf(target))!!
        vm.selectGroupContactScenario(GroupContactScenario.UnavailableRoster)
        assertEquals(listOf(id), vm.applyContactToGroups(profile.id, target, listOf(id), GroupContactAction.Promote).failed)
        vm.retryContactRoster(profile.id)
        assertEquals(listOf(id), vm.applyContactToGroups(profile.id, target, listOf(id), GroupContactAction.Promote).completed)
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertEquals(listOf(id), vm.applyContactToGroups(profile.id, target, listOf(id), GroupContactAction.Invite).failed)
    }
}

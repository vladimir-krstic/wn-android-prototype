package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class GroupAdministrationEventTest {
    private val vm = AppViewModel().apply {
        completeSignIn(OnboardingOrigin.Initial)
        setDeveloperToolsEnabled(true)
    }
    private val owner = GroupOwner(vm.uiState.activeProfileId!!,
        vm.createGroup("Trail", "", ProfileAvatar.Monogram, listOf("maya-chen"))!!)
    private fun chat() = vm.chat(owner.chatId)!!
    private fun step() = vm.groupLifecycle.work[owner]!!.let { vm.groupLifecycle.advance(owner, it.id, it.stage) }
    private fun events() = chat().timeline.filterIsInstance<ChatTimelineEntry.Event>()

    @Test fun transferRecordsChronologicalNamedEventsAndPreservesOtherChatsAndProfiles() {
        val before = vm.uiState.profiles
        val history = chat().timeline
        val name = vm.uiState.activeProfile!!.name
        assertTrue(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Transfer, "maya-chen"))
        assertEquals(history, chat().timeline)
        step()
        val grant = events().last()
        assertEquals("Maya Chen is now an admin.", grant.text)
        assertTrue(chat().members.any { it.personId == "maya-chen" && it.role == GroupRole.Admin })
        assertEquals(history, chat().timeline.dropLast(1))
        assertEquals(grant, ConversationProjection.orderedEntries(chat()).last())
        step()
        val stepDown = events().last()
        assertEquals("$name is no longer an admin.", stepDown.text)
        assertNotEquals(grant.id, stepDown.id)
        assertEquals(grant.dayOrdinal, stepDown.dayOrdinal)
        assertTrue(grant.minuteOfDay < stepDown.minuteOfDay)
        for (profile in before) {
            val after = vm.uiState.profiles.first { it.id == profile.id }
            if (profile.id != owner.profileId) assertEquals(profile, after)
            else assertEquals(profile.chats.filterNot { it.id == owner.chatId }, after.chats.filterNot { it.id == owner.chatId })
        }
    }

    @Test fun failedGrantAddsNothingAndRetryAddsOneEvent() {
        val history = chat().timeline
        vm.groupLifecycle.choose(GroupLifecycleScenario.GrantFailure)
        assertTrue(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Transfer, "maya-chen"))
        step()
        assertEquals(history, chat().timeline)
        val failed = vm.groupLifecycle.work[owner]!!
        assertTrue(vm.groupLifecycle.retry(owner, failed.id))
        step()
        val accepted = chat().timeline
        assertEquals(history.size + 1, accepted.size)
        vm.groupLifecycle.advance(owner, failed.id, GroupLifecycleStage.Grant)
        assertEquals(accepted, chat().timeline)
    }

    @Test fun stepDownRetryKeepsAcceptedGrantWithoutDuplicateEvents() {
        val size = chat().timeline.size
        vm.groupLifecycle.choose(GroupLifecycleScenario.StepDownFailure)
        assertTrue(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Transfer, "maya-chen"))
        step()
        val accepted = chat().timeline
        step()
        assertEquals(accepted, chat().timeline)
        val failed = vm.groupLifecycle.work[owner]!!
        assertTrue(vm.groupLifecycle.retry(owner, failed.id))
        step()
        assertEquals(size + 2, chat().timeline.size)
        val completed = chat().timeline
        vm.groupLifecycle.advance(owner, failed.id, failed.stage)
        assertEquals(completed, chat().timeline)
        assertEquals(1, events().count { it.text == "Maya Chen is now an admin." })
    }

    @Test fun leaveFailureAndRetryRetainAdminHistoryForDepartedMember() {
        vm.groupLifecycle.choose(GroupLifecycleScenario.LeaveFailure)
        assertTrue(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Transfer, "maya-chen", thenLeave = true))
        step(); step()
        val history = chat().timeline
        step()
        assertEquals(ChatMembership.Active, chat().membership)
        assertEquals(history, chat().timeline)
        assertTrue(vm.groupLifecycle.retry(owner, vm.groupLifecycle.work[owner]!!.id))
        step()
        assertEquals(ChatMembership.Left, chat().membership)
        assertEquals(history, chat().timeline)
        assertTrue(events().any { it.text == "Maya Chen is now an admin." })
    }

    @Test fun missingDirectoryMemberUsesHumanReadableFallback() {
        val fixtureOwner = GroupOwner(owner.profileId, "catalog-group-sole-admin")
        assertTrue(vm.groupLifecycle.begin(fixtureOwner, GroupLifecycleAction.Transfer, "member-profile-pending"))
        val work = vm.groupLifecycle.work[fixtureOwner]!!
        vm.groupLifecycle.advance(fixtureOwner, work.id, work.stage)
        assertEquals("Member is now an admin.",
            vm.chat(fixtureOwner.chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Event>().last().text)
    }
}

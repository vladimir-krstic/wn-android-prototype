package dev.ipf.whitenoise

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.chats.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PeopleFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    @Test fun searchShowsNetworkOriginAndSelectsResolvedPerson() {
        var picked: Person? = null
        rule.setContent { WhiteNoiseTheme { NewChatScreen(ProfileFixtures.marmota, {}, {}, {}, onResolvedPerson = { picked = it }) } }
        rule.onNodeWithTag("new_message.searchField").performTextInput("River")
        rule.onNodeWithText("Network results").assertIsDisplayed()
        rule.onNodeWithTag("creation.person.river-song").performClick()
        rule.runOnIdle { assertEquals("river-song", picked?.id) }
    }
    @Test fun searchFailureRetriesAndInvalidIdentifierHasNoResultAction() {
        rule.setContent { WhiteNoiseTheme { NewChatScreen(ProfileFixtures.marmota, {}, {}, {}, searchScenario = PeopleSearchScenario.Unavailable) } }
        rule.onNodeWithTag("new_message.searchField").performTextInput("River")
        rule.onNodeWithText("Try Again").performClick()
        rule.onNodeWithText("River Song").assertIsDisplayed()
        rule.onNodeWithTag("new_message.searchField").performTextReplacement("npub1bad")
        rule.onNodeWithText("Enter a valid public key, profile link or address.").assertIsDisplayed()
        rule.onAllNodes(hasTestTag("creation.person.river-song")).assertCountEquals(0)
    }
    @Test fun unknownAddressCanShareInvitation() {
        rule.setContent { WhiteNoiseTheme { NewChatScreen(ProfileFixtures.marmota, {}, {}, {}) } }
        rule.onNodeWithTag("new_message.searchField").performTextInput("unknown@whitenoise.example")
        rule.onNodeWithText("Share invitation").assertHasClickAction()
    }
    @Test fun contactEditorKeepsPublishedNameAndSavesPrivateValues() {
        val person = ProfileFixtures.marmota.people.first()
        var values: Pair<String, String>? = null
        var open by mutableStateOf(true)
        rule.setContent { WhiteNoiseTheme { if (open) PrivateContactDialog(person, { open = false }) { name, notes -> values = name to notes; true } } }
        rule.onNodeWithText("Published name: ${person.name}").assertIsDisplayed()
        rule.onNodeWithTag("contact.nickname").performTextInput("Local friend")
        rule.onNodeWithTag("contact.notes").performTextInput("Private note")
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals("Local friend" to "Private note", values); assertFalse(open) }
    }
    @Test fun startGroupPreselectsPersonAndStillAllowsAddingOthers() {
        val profile = ProfileFixtures.marmota
        val id = profile.people.first().id
        var selected = emptyList<String>()
        rule.setContent { WhiteNoiseTheme { NewGroupScreen(profile, {}, { selected = it }, initialPersonId = id) } }
        rule.onNodeWithTag("new_group.selected.$id").assertExists()
        rule.onNodeWithTag("creation.primaryAction").performClick()
        rule.runOnIdle { assertEquals(listOf(id), selected) }
    }
    @Test fun partialGroupResultRetainsOnlyFailedSelectionsForRetry() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
        val profileId = vm.uiState.activeProfileId!!
        val target = PeopleDiscovery.directory.first()
        vm.acceptDiscoveredPerson(profileId, target)
        val member = vm.uiState.activeProfile!!.people.first().id
        val ids = (1..2).map { vm.createGroup("Test $it", "", ProfileAvatar.Monogram, listOf(member))!! }
        vm.selectGroupContactScenario(GroupContactScenario.PartialApply)
        var closed = false
        rule.setContent { WhiteNoiseTheme {
            ContactGroupsSheet(vm.uiState.activeProfile!!, target, GroupContactAction.Invite, vm.groupContactScenario,
                onDismiss = { closed = true }, onRetryRoster = { vm.retryContactRoster(profileId) },
                onApply = { groups, action -> vm.applyContactToGroups(profileId, target.id, groups, action) })
        } }
        ids.forEach { rule.onNodeWithTag("contact.group.$it").performScrollTo().performClick().assertIsOn() }
        rule.onNodeWithTag("contact.groups.apply").performClick()
        rule.onNodeWithText("Confirm").performClick()
        rule.onNodeWithTag("contact.group.${ids.last()}").performScrollTo().assertIsOn()
        rule.onNodeWithTag("contact.groups.apply").performClick()
        rule.onNodeWithText("Confirm").performClick()
        rule.runOnIdle { assertTrue(closed); assertTrue(ids.all { vm.chat(it)!!.members.any { it.personId == target.id } }) }
    }
    @Test fun pendingCreatedChatOpensTheSavedId() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true); setCreatedChatUnavailable(true) }
        val request = vm.startGroupConversation("Created once", "", ProfileAvatar.Monogram, listOf(vm.uiState.activeProfile!!.people.first().id), "setup")!!
        var opened: String? = null
        rule.setContent { WhiteNoiseTheme { CreatedChatOpenDialog(onOpen = { opened = vm.completeCreatedChatOpen(request.id, "setup") }, onDismiss = vm::cancelCreatedChatOpen) } }
        rule.onNodeWithText("Chat created").assertIsDisplayed()
        rule.onNodeWithText("Open chat").performClick()
        rule.runOnIdle { assertEquals(request.chatId, opened); assertEquals(1, vm.uiState.activeProfile!!.chats.count { it.id == request.chatId }) }
    }
}

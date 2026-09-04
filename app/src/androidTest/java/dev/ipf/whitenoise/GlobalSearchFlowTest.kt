package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.*
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.chats.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlobalSearchFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val person = Person("friend", "Friend")
    private fun message(id: String, author: String = "friend", text: String = "Trailhead tomorrow") = ChatMessage(id, author, 3, "Today", 600, "10:00", text)
    private val profile = Profile("me", "My Profile", "key", people = listOf(person), chats = listOf(
        Chat("a", 0, ChatKind.Direct(person.id), "First Chat", timeline = listOf(ChatTimelineEntry.Message(message("a1")))),
        Chat("b", 1, ChatKind.Group, "Second Chat", timeline = listOf(ChatTimelineEntry.Message(message("b1", "me")))),
    ))
    private fun state(profile: Profile) = AppUiState(listOf(profile), profile.id, setOf(profile.id))
    @Composable private fun SearchScreen(owner: Profile = profile, peopleScenario: PeopleSearchScenario = PeopleSearchScenario.Success,
        voice: () -> GlobalVoiceScenario = { GlobalVoiceScenario.Success }, onPerson: (Person) -> Unit = {}) {
        ChatsScreen(state(owner), {}, {}, { _, _ -> }, {}, { _, _ -> }, { _, _ -> }, { false }, {},
            peopleScenario = peopleScenario, onVoiceScenario = voice, onOpenSearchPerson = onPerson)
    }
    private fun search(query: String) {
        rule.onNodeWithContentDescription("Search Chats").performClick()
        if (query.isNotEmpty()) rule.onNodeWithTag("chats.searchField").performTextInput(query)
    }
    private fun openFilters(category: String) {
        rule.onNodeWithTag("chats.list").performScrollToNode(hasTestTag("global.filters"))
        rule.onNodeWithText("Filters").performClick()
        rule.onNodeWithText(category).performClick()
    }

    @Test fun exactResultNavigationPreservesQueryAndChatFilterOnBack() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); dismissDiagnosticsPrompt(uiState.activeProfileId!!) }
        val chat = vm.uiState.activeProfile!!.chats.first { it.timeline.any { entry -> entry is ChatTimelineEntry.Message && !entry.message.isDeleted && entry.message.text.isNotBlank() } }
        val target = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().first { !it.message.isDeleted && it.message.text.isNotBlank() }.message
        val query = GlobalSearch.normalize(InlineMessageMarkup.plainText(target.text)).split(' ').take(3).joinToString(" ")
        lateinit var nav: NavHostController
        rule.setContent { nav = rememberNavController(); WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) } }
        rule.runOnIdle { nav.navigate(AppRoute.SignedIn) }
        search(query); openFilters("Chats")
        rule.onNodeWithTag("global.filterSearch").performTextInput(chat.title)
        rule.onNodeWithTag("global.choice.${chat.id}").performClick().assertIsOn()
        rule.onNodeWithText("Done").performClick()
        val tag = "global.message.${chat.id}.${target.id}"
        rule.onNodeWithTag("chats.list").performScrollToNode(hasTestTag(tag)); rule.onNodeWithTag(tag).performClick()
        rule.runOnIdle {
            val route = nav.currentBackStackEntry!!.toRoute<AppRoute.Conversation>()
            assertEquals(chat.id, route.chatId); assertEquals(target.id, route.targetMessageId)
            nav.popBackStack()
        }
        rule.onNodeWithTag("chats.searchField").assertTextContains(query)
        rule.onNodeWithTag("chats.list").performScrollToNode(hasTestTag("global.filters"))
        rule.onNodeWithTag("global.filters").performScrollToNode(hasContentDescription("Remove filter: Chat: ${chat.title}"))
        rule.onNodeWithContentDescription("Remove filter: Chat: ${chat.title}").assertExists()
    }
    @Test fun multipleChatAndSenderChoicesHaveCheckboxSemanticsAndRemovableChips() {
        rule.setContent { WhiteNoiseTheme { SearchScreen() } }
        search(""); openFilters("Chats")
        rule.onNodeWithTag("global.choice.a").performClick().assertIsOn(); rule.onNodeWithTag("global.choice.b").performClick().assertIsOn()
        rule.onNodeWithText("Done").performClick(); openFilters("Senders")
        rule.onNodeWithTag("global.choice.friend").performClick().assertIsOn(); rule.onNodeWithText("Done").performClick()
        rule.onNodeWithTag("chats.list").performScrollToNode(hasTestTag("global.message.a.a1"))
        rule.onNodeWithTag("global.message.a.a1").assertExists(); rule.onNodeWithTag("global.message.b.b1").assertDoesNotExist()
        rule.onNodeWithTag("chats.list").performScrollToNode(hasTestTag("global.filters"))
        rule.onNodeWithTag("global.filters").performScrollToNode(hasContentDescription("Remove filter: Sender: Friend"))
        rule.onNodeWithContentDescription("Remove filter: Sender: Friend").performClick()
        rule.onNodeWithTag("chats.list").performScrollToNode(hasTestTag("global.message.b.b1")); rule.onNodeWithTag("global.message.b.b1").assertExists()
        rule.onNodeWithTag("chats.list").performScrollToNode(hasTestTag("global.filters"))
        rule.onNodeWithTag("global.filters").performScrollToNode(hasText("Clear All")); rule.onNodeWithText("Clear All").performClick()
        rule.onNodeWithText("Clear All").assertDoesNotExist()
    }
    @Test fun queryAndFiltersRestoreThenResetOnProfileSwitch() {
        var owner by mutableStateOf(profile)
        val restore = StateRestorationTester(rule)
        restore.setContent { WhiteNoiseTheme { SearchScreen(owner) } }
        search("Trailhead"); openFilters("Content")
        rule.onNodeWithTag("global.content.Text").performClick(); rule.onNodeWithText("Done").performClick()
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("chats.searchField").assertTextContains("Trailhead")
        rule.onNodeWithTag("global.filters").performScrollToNode(hasContentDescription("Remove filter: Text"))
        rule.onNodeWithContentDescription("Remove filter: Text").assertExists()
        rule.runOnIdle { owner = profile.copy(id = "other") }
        rule.onNodeWithTag("chats.searchField").assertDoesNotExist()
        search(""); rule.onNodeWithText("Clear All").assertDoesNotExist()
    }
    @Test fun removedChatsReconcileFilterAndEmptyContentQueryHasUsefulNoMatches() {
        var owner by mutableStateOf(profile)
        rule.setContent { WhiteNoiseTheme { SearchScreen(owner) } }
        search(""); openFilters("Chats"); rule.onNodeWithTag("global.choice.a").performClick(); rule.onNodeWithText("Done").performClick()
        rule.runOnIdle { owner = profile.copy(chats = profile.chats.drop(1)) }
        rule.onNodeWithText("Clear All").assertDoesNotExist()
        openFilters("Content"); rule.onNodeWithTag("global.content.ImagesVideo").performClick(); rule.onNodeWithText("Done").performClick()
        rule.onNodeWithText("No matches").assertIsDisplayed()
        rule.onNodeWithText("Try another search or remove a filter.").assertIsDisplayed()
    }
    @Test fun nativeCustomRangeCannotApplyBeforeSelectionAndCancelLeavesFiltersUntouched() {
        var applied = false; var dismissed = false
        rule.setContent { WhiteNoiseTheme { GlobalDateRangeDialog(GlobalSearchFilters(), { dismissed = true }) { _, _ -> applied = true } } }
        rule.onNodeWithTag("global.dateRange").assertExists(); rule.onNodeWithText("Apply").assertIsNotEnabled()
        rule.onNodeWithText("Cancel").performClick(); rule.runOnIdle { assertTrue(dismissed); assertFalse(applied) }
    }
    @Test fun nativeCustomRangeAppliesInclusiveExistingCivilDates() {
        val start = GlobalSearchClock.today.minusDays(6).toEpochDay(); val end = GlobalSearchClock.today.toEpochDay()
        var selected: Pair<Long, Long>? = null
        rule.setContent { WhiteNoiseTheme { GlobalDateRangeDialog(GlobalSearchFilters(date = GlobalSearchDate.Custom, fromDay = start, toDay = end), {}) { a, b -> selected = a to b } } }
        rule.onNodeWithText("Apply").assertIsEnabled().performClick(); rule.runOnIdle { assertEquals(start to end, selected) }
    }
    @Test fun failedLookupKeepsRetryVisibleAndUnknownProfileUsesReadableName() {
        var scenario by mutableStateOf(PeopleSearchScenario.Unavailable); var opened: Person? = null
        rule.setContent { WhiteNoiseTheme { SearchScreen(peopleScenario = scenario, onPerson = { opened = it }) } }
        search("river@whitenoise.example")
        rule.waitUntil(3_000) { rule.onAllNodesWithText("Try Again").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("No matches").assertDoesNotExist(); rule.onNodeWithText("Try Again").assertIsDisplayed().performClick()
        rule.waitUntil(3_000) { rule.onAllNodesWithTag("global.person.river-song").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("global.person.river-song").assertExists()
        rule.runOnIdle { scenario = PeopleSearchScenario.Success }
        val key = "npub1" + "p".repeat(58)
        rule.onNodeWithTag("chats.searchField").performTextReplacement(key)
        rule.waitUntil(3_000) { rule.onAllNodesWithText("Unknown profile").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Unknown profile").performClick()
        rule.runOnIdle { assertEquals("Unknown profile", opened!!.name); assertEquals(key, opened.publicKey) }
    }
    @Test fun voiceFailureRetryAndCancellationPreserveOrReplaceOnlyExpectedQuery() {
        var next = GlobalVoiceScenario.Unavailable
        rule.setContent { WhiteNoiseTheme { SearchScreen(voice = { next.also { next = GlobalVoiceScenario.Success } }) } }
        search("Existing query"); rule.onNodeWithContentDescription("Voice Search").performClick()
        rule.onNodeWithText("Cancel").performClick(); rule.onNodeWithTag("chats.searchField").assertTextContains("Existing query")
        rule.runOnIdle { next = GlobalVoiceScenario.Unavailable }
        rule.onNodeWithContentDescription("Voice Search").performClick(); rule.onNodeWithText("Try Again").performClick()
        rule.waitUntil(3_000) { rule.onAllNodes(hasTestTag("chats.searchField") and hasText("trailhead")).fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { next = GlobalVoiceScenario.Cancelled }
        rule.onNodeWithContentDescription("Voice Search").performClick()
        rule.waitUntil(3_000) { rule.onAllNodesWithText("Getting your search…").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithTag("chats.searchField").assertTextContains("trailhead")
    }
}

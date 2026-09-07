package dev.ipf.whitenoise

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.conversation.ConversationDatePicker
import dev.ipf.whitenoise.ui.conversation.ConversationScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationDateFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val profile = ProfileFixtures.marmota.copy(id = "me", name = "Me", people = listOf(Person("friend", "Friend")))
    private fun chat() = Chat("dates", 0, ChatKind.Group, "Dates",
        members = listOf(GroupMember("me", GroupRole.Admin), GroupMember("friend", GroupRole.Member)),
        relayUrls = profile.chatRelayUrls, draftText = "Keep this draft",
        timeline = listOf(ChatTimelineEntry.Event("joined", "Friend joined the group", 3, "Today", 500)) +
            (0 until 60).map { ChatTimelineEntry.Message(ChatMessage("m$it", "friend", 3, "Today", 600 + it, "10:00", "Update $it")) })
    @Composable private fun Screen(search: Boolean = true) {
        ConversationScreen(profile, chat(), {}, { true }, {}, {}, {}, initialSearch = search)
    }

    @Test fun calendarLivesInSearchAndCancelPreservesQueryAndResult() {
        rule.setContent { WhiteNoiseTheme { Screen() } }
        rule.onNodeWithTag("conversation.searchField").performTextInput("Update 45")
        rule.waitUntil(4_000) { rule.onAllNodesWithTag("conversation.message.m45").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("conversation.search.calendar").assertContentDescriptionEquals("Jump to date").performClick()
        rule.onNodeWithTag("conversation.datePicker.cancel").performClick()
        rule.onNodeWithTag("conversation.searchField").assertTextContains("Update 45")
        rule.onNodeWithTag("conversation.message.m45").assertIsDisplayed()
        rule.onNodeWithTag("conversation.search.calendar").performClick()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("conversation.datePicker").assertDoesNotExist()
        rule.onNodeWithTag("conversation.searchField").assertTextContains("Update 45")
    }

    @Test fun jumpReplacesSearchWithFirstSystemEventAndKeepsDraft() {
        rule.setContent { WhiteNoiseTheme { Screen() } }
        rule.onNodeWithTag("conversation.search.calendar").performClick()
        rule.onNodeWithTag("conversation.datePicker.jump").assertIsEnabled().performClick()
        rule.waitUntil(4_000) { rule.onAllNodesWithTag("conversation.searchField").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("Friend joined the group").assertIsDisplayed()
        rule.onNodeWithText("Keep this draft").assertExists()
        rule.onNodeWithTag("conversation.search.calendar").assertDoesNotExist()
    }

    @Test fun pickerRestoresAndEmptyHistoryCannotConfirm() {
        val restore = StateRestorationTester(rule)
        val empty = ConversationDates(chat().copy(timeline = emptyList()), ZoneOffset.UTC)
        var jumped = false
        restore.setContent { WhiteNoiseTheme { ConversationDatePicker(empty, null, {}, { jumped = true }) } }
        rule.onNodeWithText("No messages to jump to.").assertExists()
        rule.onNodeWithTag("conversation.datePicker.jump").assertIsNotEnabled()
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("conversation.datePicker.jump").assertIsNotEnabled()
        rule.runOnIdle { assertFalse(jumped) }
    }

    @Test fun openCalendarSurvivesStateRestorationAndBackReturnsToSearch() {
        val restore = StateRestorationTester(rule)
        restore.setContent { WhiteNoiseTheme { Screen() } }
        rule.onNodeWithTag("conversation.search.calendar").performClick()
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("conversation.datePicker.calendar").assertExists()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("conversation.searchField").assertExists()
    }
}

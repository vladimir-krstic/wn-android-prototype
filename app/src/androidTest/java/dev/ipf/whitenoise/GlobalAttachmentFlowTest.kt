package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.ui.chats.ChatsScreen
import dev.ipf.whitenoise.ui.chats.GlobalAttachmentBrowser
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlobalAttachmentFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val photo = MessageAttachment("photo", MessageAttachmentKind.Photo, "Trail photo", images = listOf(ProfileAvatar.Monogram))
    private val profile = Profile("owner", "Owner", "key", chats = (0..30).map { i ->
        Chat("c$i", i, ChatKind.Direct("friend"), "Chat $i", timeline = listOf(ChatTimelineEntry.Message(
            ChatMessage("m$i", "friend", 3, "Today", 600 + i, "10:00", attachments = listOf(photo)))))
    })
    @Composable private fun Screen(owner: Profile = profile, scenario: () -> GlobalLibraryScenario = { GlobalLibraryScenario.Ready }, go: (String, String) -> Boolean = { _, _ -> true }) {
        ChatsScreen(AppUiState(listOf(owner), owner.id, setOf(owner.id)), {}, {}, { _, _ -> }, {}, { _, _ -> }, { _, _ -> }, { false }, {},
            onLibraryScenario = scenario, onOpenSearchMessage = go)
    }
    private fun browse() {
        rule.onNodeWithContentDescription("Search Chats").performClick()
        rule.onNodeWithTag("global.library.mode.ImagesVideo").performClick()
        rule.waitUntil(4_000) { rule.onAllNodesWithTag("global.library.loading").fetchSemanticsNodes().isEmpty() }
    }
    private val filters = GlobalSearchFilters(content = setOf(GlobalSearchContent.ImagesVideo))
    private fun row(owner: Profile = profile, chat: String = "c30") = GlobalAttachments.results(owner, "", filters).first { it.chatId == chat }

    @Test fun itemOpensMessageAndBackKeepsQueryFiltersAndScrolledItem() {
        rule.setContent {
            WhiteNoiseTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "library") {
                    composable("library") { Screen(go = { chat, message -> nav.navigate("message/$chat/$message"); true }) }
                    composable("message/{chat}/{message}") { entry ->
                        BackHandler { nav.popBackStack() }
                        Text("Source ${entry.arguments?.getString("chat")} ${entry.arguments?.getString("message")}")
                    }
                }
            }
        }
        browse()
        rule.onNodeWithTag("chats.searchField").performTextInput("Trail")
        rule.waitUntil(4_000) { rule.onAllNodesWithTag("global.library.loading").fetchSemanticsNodes().isEmpty() }
        val item = row(chat = "c3")
        rule.onNodeWithTag("global.library.results").performScrollToNode(hasTestTag("global.library.item.${item.id}"))
        rule.onNodeWithTag("global.library.item.${item.id}").performClick()
        rule.onNodeWithText("Source c3 m3").assertExists()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("global.library.item.${item.id}").assertIsDisplayed()
        rule.onNodeWithTag("global.library.mode.ImagesVideo").assertIsSelected()
        rule.onNodeWithTag("chats.searchField").assertTextContains("Trail")
    }

    @Test fun sourceActionKeepsExactChatAndMessageIdentity() {
        var opened: Pair<String, String>? = null
        rule.setContent { WhiteNoiseTheme { Screen(go = { chat, message -> opened = chat to message; true }) } }
        browse()
        val item = row()
        rule.onNodeWithTag("global.library.item.${item.id}").performClick()
        rule.runOnIdle { assertEquals("c30" to "m30", opened) }
    }

    @Test fun failedAndPartialResultsRetryWithoutChangingFilters() {
        var scenario = GlobalLibraryScenario.Failed
        rule.setContent { WhiteNoiseTheme { Screen(scenario = { scenario }) } }
        browse()
        rule.onNodeWithText("Couldn’t load files and media.").assertExists()
        rule.onNodeWithTag("global.library.retry").performClick()
        rule.waitUntil(4_000) { rule.onAllNodesWithTag("global.library.results").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("global.library.mode.ImagesVideo").assertIsSelected()
        rule.runOnIdle { scenario = GlobalLibraryScenario.Partial }
        rule.onNodeWithTag("chats.searchField").performTextInput("Trail")
        rule.waitUntil(4_000) { rule.onAllNodesWithText("Some results could not be loaded.").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("global.library.results").assertExists()
        rule.onNodeWithTag("global.library.retry").performClick()
        rule.waitUntil(4_000) { rule.onAllNodesWithText("Some results could not be loaded.").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithTag("chats.searchField").assertTextContains("Trail")
    }

    @Test fun deletionRemovesResultAndAccountSwitchClearsLibrary() {
        var owner by mutableStateOf(profile)
        rule.setContent { WhiteNoiseTheme { Screen(owner) } }
        browse()
        val item = row()
        rule.onNodeWithTag("global.library.item.${item.id}").assertExists()
        rule.runOnIdle { owner = owner.copy(chats = owner.chats.filterNot { it.id == item.chatId }) }
        rule.onNodeWithTag("global.library.item.${item.id}").assertDoesNotExist()
        rule.runOnIdle { owner = profile.copy(id = "other", chats = emptyList()) }
        rule.onNodeWithTag("global.library").assertDoesNotExist()
        rule.onNodeWithTag("chats.searchField").assertDoesNotExist()
    }

    @Test fun restoredFiltersAndScrollReturnToTheSameResult() {
        val restore = StateRestorationTester(rule)
        restore.setContent { WhiteNoiseTheme { Screen() } }
        browse()
        val item = row(chat = "c2")
        rule.onNodeWithTag("global.library.results").performScrollToNode(hasTestTag("global.library.item.${item.id}"))
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("global.library.mode.ImagesVideo").assertIsSelected()
        rule.onNodeWithTag("global.library.item.${item.id}").assertIsDisplayed()
    }

    @Test fun unavailableTileHidesAttachmentLabelAndOpensItsMessage() {
        val owner = profile.copy(chats = listOf(profile.chats.last().copy(timeline = listOf(ChatTimelineEntry.Message(
            ChatMessage("m30", "friend", 3, "Today", 630, "10:00", attachments = listOf(photo.copy(isAvailable = false, label = "Hidden preview"))))))))
        var opened: Pair<String, String>? = null
        rule.setContent { WhiteNoiseTheme { Screen(owner, go = { chat, message -> opened = chat to message; true }) } }
        browse()
        rule.onNodeWithText("Hidden preview").assertDoesNotExist()
        rule.onNodeWithTag("global.library.item.${row(owner).id}").performClick()
        rule.runOnIdle { assertEquals("c30" to "m30", opened) }
        rule.onNodeWithTag("conversation.media.viewer.pager").assertDoesNotExist()
        rule.onNodeWithTag("global.library.item.${row(owner).id}").assertExists()
    }

    @Test fun fileAndAudioCardsContainMetadataAndOpenTheirMessageWithoutNestedActions() {
        val file = AttachmentReadingExamples.attachments().first { it.localSource == AttachmentLocalSource.PlainText }
        val audio = MessageAttachment("audio", MessageAttachmentKind.Voice, "Voice message", durationSeconds = 12)
        val owner = profile.copy(chats = listOf(profile.chats.last().copy(timeline = listOf(ChatTimelineEntry.Message(
            ChatMessage("m30", "friend", 3, "Today", 630, "10:00", attachments = listOf(file, audio)))))))
        var opened: Pair<String, String>? = null
        rule.setContent { WhiteNoiseTheme { Screen(owner, go = { chat, message -> opened = chat to message; true }) } }
        rule.onNodeWithContentDescription("Search Chats").performClick()
        listOf(GlobalSearchContent.Files, GlobalSearchContent.VoiceAudio).forEach { type ->
            rule.onNodeWithTag("global.library.modes").performScrollToNode(hasTestTag("global.library.mode.${type.name}"))
            rule.onNodeWithTag("global.library.mode.${type.name}").performClick()
            val item = GlobalAttachments.results(owner, "", GlobalSearchFilters(content = setOf(type))).single()
            val tag = "global.library.item.${item.id}"
            rule.waitUntil(4_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
            if (type == GlobalSearchContent.Files) {
                rule.onNodeWithTag(tag).assertTextContains(item.attachment.label)
            } else {
                rule.onNodeWithText("Voice message").assertDoesNotExist()
                rule.onNodeWithTag(tag).assertTextContains("0:12")
            }
            rule.onNodeWithTag(tag).assertTextContains("From: Chat 30 · 10:00")
            rule.onNodeWithText("Go to Message").assertDoesNotExist()
            rule.onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true).assertCountEquals(0)
            rule.runOnIdle { opened = null }
            rule.onNodeWithTag(tag).performClick()
            rule.runOnIdle { assertEquals("c30" to "m30", opened) }
            rule.onNodeWithTag("attachment.reader.body").assertDoesNotExist()
        }
    }

    @Test fun narrowRtlLargeTextKeepsMetadataAndSourceActionsInEveryTheme() {
        val owner = profile.copy(chats = profile.chats.takeLast(1))
        var theme by mutableStateOf(AppearancePreference.Light)
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f), LocalLayoutDirection provides LayoutDirection.Rtl) {
                WhiteNoiseTheme(appearance = theme) {
                    Box(Modifier.width(320.dp)) {
                        GlobalAttachmentBrowser(owner, "", filters, GlobalSearch.results(owner, "", filters), 0.dp, { _, _ -> })
                    }
                }
            }
        }
        rule.waitUntil(4_000) { rule.onAllNodesWithTag("global.library.results").fetchSemanticsNodes().isNotEmpty() }
        val item = row(owner)
        AppearancePreference.entries.forEach { value ->
            rule.runOnIdle { theme = value }
            rule.onNodeWithTag("global.library.results").performScrollToNode(hasTestTag("global.library.item.${item.id}"))
            rule.onNodeWithTag("global.library.item.${item.id}").assertIsDisplayed().assertIsEnabled().assertHeightIsAtLeast(48.dp)
        }
    }
}

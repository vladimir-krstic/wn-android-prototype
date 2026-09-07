package dev.ipf.whitenoise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.*
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.conversation.PinnedMessagesSheet
import dev.ipf.whitenoise.ui.conversation.PinnedMessageBanner
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PinnedMessageFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val chatId = "maya-chen"
    private val textId = "maya-shared-text"
    private val photoId = "maya-shared-photo"
    private fun model() = AppViewModel().apply {
        completeSignIn(OnboardingOrigin.Initial)
        dismissDiagnosticsPrompt(uiState.activeProfileId!!)
    }
    private fun open(vm: AppViewModel, restore: StateRestorationTester? = null): NavHostController {
        lateinit var nav: NavHostController
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            nav = rememberNavController()
            WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) }
        }
        if (restore == null) rule.setContent(content) else restore.setContent(content)
        rule.runOnIdle { nav.navigate(AppRoute.Conversation(chatId, targetMessageId = textId)) }
        rule.onNodeWithTag("message.pins.banner").assertExists()
        return nav
    }
    private fun openList() {
        rule.onNodeWithTag("message.pins.menu").performClick()
        rule.onNodeWithTag("message.pins.menu.all").performClick()
    }
    private fun action(id: String, label: String) {
        rule.onNodeWithTag("conversation.timeline").performScrollToNode(hasTestTag("conversation.message.$id"))
        val action = rule.onNodeWithTag("conversation.message.$id").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first { it.label == label }
        rule.runOnIdle { assertTrue(action.action()) }
    }

    @Test fun bannerCyclesAndFullListReturnsToExactSourceKeepingDraft() {
        val vm = model(); open(vm)
        rule.runOnIdle { vm.updateDraftText(chatId, "Keep this draft") }
        rule.onNodeWithTag("message.pins.preview").performClick()
        rule.onNodeWithTag("message.pins.preview").assertTextContains("This is the entrance.")
        rule.onNodeWithTag("message.pins.preview").performClick()
        openList()
        rule.onNodeWithTag("message.pins.sheet").assertExists()
        rule.onNodeWithTag("message.pins.list").performScrollToNode(hasTestTag("message.pins.row.$photoId"))
        rule.onNodeWithTag("message.pins.row.$photoId").performClick()
        rule.onNodeWithTag("message.pins.sheet").assertDoesNotExist()
        rule.onNodeWithTag("conversation.message.$photoId").assertIsDisplayed()
        rule.runOnIdle { assertEquals("Keep this draft", vm.chat(chatId)!!.draftText) }
        openList()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("message.pins.banner").assertExists()
    }

    @Test fun listUsesFullPhotoAndTextBubblesWithOriginalMetadata() {
        val vm = model(); open(vm); openList()
        rule.onNodeWithTag("message.pins.bubble.$textId").assertExists()
        rule.onNodeWithTag("message.pins.row.$textId").assertContentDescriptionContains(vm.message(chatId, textId)!!.text, substring = true)
        rule.onNodeWithTag("message.pins.list").performScrollToNode(hasTestTag("message.pins.row.$photoId"))
        rule.onNodeWithTag("message.pins.bubble.$photoId").assertExists()
        rule.onNode(hasTestTag("conversation.media.tile.maya-shared-entrance.0") and
            hasAnyAncestor(hasTestTag("message.pins.bubble.$photoId")), useUnmergedTree = true).assertExists()
        rule.onNode(hasText("This is the entrance.") and
            hasAnyAncestor(hasTestTag("message.pins.bubble.$photoId")), useUnmergedTree = true).assertExists()
        rule.onNode(hasText("9:12 AM") and
            hasAnyAncestor(hasTestTag("message.pins.bubble.$photoId")), useUnmergedTree = true).assertExists()
    }

    @Test fun dropdownHasSourceActionsAndUnpinsTheDisplayedMessage() {
        val vm = model(); open(vm)
        rule.onNodeWithTag("message.pins.open").assertDoesNotExist()
        rule.onNodeWithTag("message.pins.previous").assertDoesNotExist()
        rule.onNodeWithTag("message.pins.next").assertDoesNotExist()
        rule.onNodeWithTag("message.pins.preview").performClick()
        rule.onNodeWithTag("message.pins.menu").performClick()
        rule.onNodeWithTag("message.pins.menu.unpin").assertIsEnabled()
        rule.onNodeWithTag("message.pins.menu.go").performClick()
        rule.onNodeWithTag("conversation.message.$photoId").assertIsDisplayed()
        rule.onNodeWithTag("message.pins.menu").performClick()
        rule.onNodeWithTag("message.pins.menu.unpin").performClick()
        rule.runOnIdle { assertEquals(listOf(textId), vm.chat(chatId)!!.pinnedMessageIds) }
        rule.onNodeWithTag("message.pins.preview").performClick()
        rule.onNodeWithTag("message.pins.preview").assertTextContains(vm.message(chatId, textId)!!.text)
    }

    @Test fun messageActionsUnpinAndRepinAndListCanBecomeEmpty() {
        val vm = model(); open(vm)
        action(textId, "Unpin")
        rule.runOnIdle { assertFalse(textId in vm.chat(chatId)!!.pinnedMessageIds) }
        action(textId, "Pin")
        rule.runOnIdle { assertEquals(listOf(photoId, textId), vm.chat(chatId)!!.pinnedMessageIds) }
        openList()
        rule.onNodeWithTag("message.pins.unpin.$textId").performClick()
        rule.onNodeWithTag("message.pins.list").performScrollToNode(hasTestTag("message.pins.unpin.$photoId"))
        rule.onNodeWithTag("message.pins.unpin.$photoId").performClick()
        rule.onNodeWithText("No pinned messages").assertIsDisplayed()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("message.pins.banner").assertDoesNotExist()
        rule.onNodeWithTag("message.pins.open").assertDoesNotExist()
    }

    @Test fun currentPinAndListRestoreWithoutChangingTheSource() {
        val vm = model(); val restoration = StateRestorationTester(rule); open(vm, restoration)
        rule.onNodeWithTag("message.pins.preview").performClick()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("message.pins.preview").assertTextContains("This is the entrance.")
        openList()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("message.pins.sheet").assertExists()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("message.pins.banner").assertExists()
    }

    @Test fun deletedAndUnavailablePinsDoNotOpenAndStillOfferUnpin() {
        val vm = model(); val initial = vm.chat(chatId)!!
        val chat = mutableStateOf(initial.copy(pinnedMessageIds = listOf(textId, "missing"), timeline = initial.timeline.map {
            if (it is ChatTimelineEntry.Message && it.id == textId) it.copy(message = it.message.copy(deletionState = MessageDeletionState.DeletedByOther)) else it
        }))
        val opened = mutableListOf<String>()
        rule.setContent { WhiteNoiseTheme { PinnedMessagesSheet(vm.uiState.activeProfile!!, chat.value, {}, opened::add,
            { id -> chat.value = MessagePins.setPinned(chat.value, vm.uiState.activeProfileId!!, id, false) }) } }
        rule.onNodeWithTag("message.pins.row.$textId").assertIsNotEnabled()
        rule.onNodeWithTag("message.pins.row.missing").assertIsNotEnabled()
        rule.onNodeWithText("Message deleted").assertExists()
        rule.onNodeWithText("Message unavailable").assertExists()
        rule.onNodeWithTag("message.pins.unpin.missing").performClick()
        rule.onNodeWithTag("message.pins.row.missing").assertDoesNotExist()
        rule.runOnIdle { assertTrue(opened.isEmpty()) }
    }

    @Test fun fullWidthStripCyclesWithoutOpeningAndMirrorsPaginationAtLargeType() {
        val vm = model(); val profile = vm.uiState.activeProfile!!; val chat = vm.chat(chatId)!!
        val pins = MessagePins.entries(chat)
        val index = androidx.compose.runtime.mutableIntStateOf(0)
        val appearance = mutableStateOf(AppearancePreference.Light)
        val direction = mutableStateOf(LayoutDirection.Ltr)
        var opened = 0
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f), LocalLayoutDirection provides direction.value) {
                WhiteNoiseTheme(appearance = appearance.value) {
                    Box(Modifier.width(320.dp)) {
                        PinnedMessageBanner(profile, chat, pins[index.intValue], index.intValue, pins.size,
                            onOpen = { opened++ }, onNext = { index.intValue = (index.intValue + 1) % pins.size },
                            onUnpin = {}, onViewAll = {})
                    }
                }
            }
        }
        AppearancePreference.entries.forEach { theme ->
            listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { layout ->
                rule.runOnIdle { appearance.value = theme; direction.value = layout }
                rule.onNodeWithTag("message.pins.banner").assertWidthIsEqualTo(320.dp)
                rule.onNodeWithTag("message.pins.menu").assertHeightIsAtLeast(48.dp)
                val rail = rule.onNodeWithTag("message.pins.pagination", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                val menu = rule.onNodeWithTag("message.pins.menu").fetchSemanticsNode().boundsInRoot
                if (layout == LayoutDirection.Ltr) assertTrue(rail.right < menu.left) else assertTrue(rail.left > menu.right)
                rule.onNodeWithTag("message.pins.preview").performClick()
                rule.runOnIdle { assertEquals(0, opened) }
            }
        }
    }

    @Test fun permissionDisabledListRemainsReadableAtLargeRtlTypeInEveryTheme() {
        val vm = model(); val profile = vm.uiState.activeProfile!!
        val group = profile.chats.first { it.id == "catalog-group-member" }
        val appearance = mutableStateOf(AppearancePreference.Light)
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f), LocalLayoutDirection provides LayoutDirection.Rtl) {
                WhiteNoiseTheme(appearance = appearance.value) {
                    Box(Modifier.width(320.dp)) { PinnedMessagesSheet(profile, group, {}, {}, {}) }
                }
            }
        }
        AppearancePreference.entries.forEach { theme ->
            rule.runOnIdle { appearance.value = theme }
            rule.onNodeWithTag("message.pins.list").performScrollToIndex(0)
            rule.onNodeWithText("Only admins can pin or unpin messages in this group.").assertExists()
            rule.onNodeWithTag("message.pins.unpin.ROLE-02").assertDoesNotExist()
            rule.onNodeWithTag("message.pins.list").performScrollToNode(hasTestTag("message.pins.row.unavailable-pin"))
            rule.onNodeWithTag("message.pins.row.unavailable-pin").assertIsNotEnabled()
        }
    }
}

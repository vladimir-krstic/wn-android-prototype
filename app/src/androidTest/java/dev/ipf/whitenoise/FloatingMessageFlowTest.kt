package dev.ipf.whitenoise

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.*
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Host gate compiles these tests; execution requires explicit device-test authorization. */
@RunWith(AndroidJUnit4::class)
class FloatingMessageFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val textId = "maya-shared-text"
    private val photoId = "maya-shared-photo"
    private fun model() = AppViewModel().apply {
        completeSignIn(OnboardingOrigin.Initial)
        dismissDiagnosticsPrompt(uiState.activeProfileId!!)
    }
    private fun open(vm: AppViewModel): NavHostController {
        lateinit var nav: NavHostController
        rule.setContent {
            nav = rememberNavController()
            WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) }
        }
        rule.runOnIdle { nav.navigate(AppRoute.Conversation("maya-chen", targetMessageId = textId)) }
        rule.onNodeWithTag("conversation.timeline").assertExists()
        return nav
    }
    private fun keepThroughMessageAction() {
        rule.onNodeWithTag("conversation.timeline").performScrollToNode(hasTestTag("conversation.message.$textId"))
        val action = rule.onNodeWithTag("conversation.message.$textId").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first { it.label == "Keep on screen" }
        rule.runOnIdle { assertTrue(action.action()) }
        rule.onNodeWithTag("floating.card").assertExists()
    }

    @Test fun keepPersistsAcrossChatAndListAndOpensOriginal() {
        val vm = model(); val nav = open(vm)
        keepThroughMessageAction()
        rule.runOnIdle { nav.navigate(AppRoute.SignedIn) }
        rule.onNodeWithTag("floating.card").assertExists()
        rule.runOnIdle { nav.navigate(AppRoute.Conversation("fiatjaf")) }
        rule.onNode(hasText("The riverside path is open again.", substring = true) and
            hasAnyAncestor(hasTestTag("floating.card")), useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("floating.header").performTouchInput { longClick(center) }
        rule.onNodeWithTag("floating.menu").performClick()
        rule.onNodeWithTag("floating.open").performClick()
        rule.waitForIdle()
        rule.runOnIdle { assertEquals("maya-chen", nav.currentBackStackEntry!!.toRoute<AppRoute.Conversation>().chatId) }
        rule.onNodeWithTag("conversation.message.$textId").assertExists()
    }

    @Test fun onlySwipesCycleWhileTapsPreserveSelectionInBothStates() {
        val vm = model(); open(vm)
        rule.runOnIdle {
            val owner = vm.uiState.activeProfileId!!
            vm.floatingMessages.keep(owner, "maya-chen", textId)
            vm.floatingMessages.keep(owner, "maya-chen", photoId)
        }
        rule.onNodeWithTag("floating.pressLayer").assertDoesNotExist()
        rule.onNodeWithTag("floating.author", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("floating.source", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("floating.drag").assertDoesNotExist()
        val paginationBounds = rule.onNodeWithTag("floating.pagination", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val previewBounds = rule.onNodeWithTag("floating.preview", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(previewBounds.top, paginationBounds.top, 1f)
        assertTrue(paginationBounds.right <= previewBounds.left)
        val paginationTop = paginationBounds.top
        rule.onNodeWithTag("floating.preview").performTouchInput { click() }
        rule.waitForIdle()
        assertEquals(paginationTop, rule.onNodeWithTag("floating.pagination", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top, 1f)
        rule.runOnIdle { assertEquals(photoId, vm.floatingMessages.selected!!.messageId) }
        rule.onNodeWithTag("floating.header").performTouchInput { click(centerLeft + androidx.compose.ui.geometry.Offset(24f, 0f)) }
        rule.runOnIdle { assertEquals(photoId, vm.floatingMessages.selected!!.messageId) }
        rule.onNodeWithTag("floating.preview").performTouchInput { swipeRight() }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(textId, vm.floatingMessages.selected!!.messageId) }
        rule.onNodeWithTag("floating.preview").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(photoId, vm.floatingMessages.selected!!.messageId) }
        // Last -> first wraps forward, then first -> last wraps backward.
        rule.onNodeWithTag("floating.preview").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(textId, vm.floatingMessages.selected!!.messageId) }
        rule.onNodeWithTag("floating.preview").performTouchInput { swipeRight() }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(photoId, vm.floatingMessages.selected!!.messageId) }
        rule.onNodeWithTag("floating.preview").performTouchInput { longClick() }
        rule.onNodeWithTag("floating.expanded").assertExists()
        rule.onNodeWithTag("floating.sheet").assertDoesNotExist()
        rule.onNodeWithTag("floating.author", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("floating.source", useUnmergedTree = true).assertExists()
        val menu = rule.onNodeWithTag("floating.menu").fetchSemanticsNode().boundsInRoot
        val collapse = rule.onNodeWithTag("floating.collapse").fetchSemanticsNode().boundsInRoot
        assertTrue(menu.right <= collapse.left)
        rule.onNodeWithTag("floating.preview", useUnmergedTree = true).performTouchInput { click(center) }
        rule.runOnIdle { assertEquals(photoId, vm.floatingMessages.selected!!.messageId) }
        rule.onNodeWithTag("floating.preview", useUnmergedTree = true).performTouchInput { swipeRight() }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(textId, vm.floatingMessages.selected!!.messageId) }
        rule.onNodeWithTag("floating.expanded").assertExists()
        rule.onNodeWithTag("floating.preview", useUnmergedTree = true).performTouchInput { swipeRight() }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(photoId, vm.floatingMessages.selected!!.messageId) }
        rule.onNodeWithTag("floating.preview", useUnmergedTree = true).performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(textId, vm.floatingMessages.selected!!.messageId) }
        rule.onNodeWithTag("floating.preview", useUnmergedTree = true).performTouchInput { longClick(center) }
        rule.onNodeWithTag("floating.expanded").assertDoesNotExist()
        rule.runOnIdle { assertEquals(textId, vm.floatingMessages.selected!!.messageId) }
        rule.onNodeWithTag("floating.menu").performClick()
        rule.onNodeWithTag("floating.remove").performClick()
        rule.runOnIdle { assertEquals(photoId, vm.floatingMessages.selected!!.messageId) }
        rule.onNodeWithTag("floating.menu").performClick()
        rule.onNodeWithTag("floating.clear").performClick()
        rule.onNodeWithTag("floating.card").assertDoesNotExist()
    }
    @Test fun singleMessageExpandsInPlaceFromHeaderWithoutBubbleOrPagination() {
        val vm = model(); open(vm)
        keepThroughMessageAction()
        rule.onNodeWithTag("floating.pagination").assertDoesNotExist()
        rule.onNodeWithTag("floating.expand").assertDoesNotExist()
        val compactLayout = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        rule.onNodeWithTag("floating.text", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(compactLayout) }
        val before = rule.onNodeWithTag("floating.card").fetchSemanticsNode().boundsInRoot
        val host = rule.onNodeWithTag("floating.host").fetchSemanticsNode().boundsInRoot
        val inset = 24f * rule.activity.resources.displayMetrics.density
        assertEquals(host.left + inset, before.left, 2f)
        assertEquals(host.right - inset, before.right, 2f)
        rule.onNodeWithTag("floating.header").performTouchInput { longClick(center) }
        rule.onNodeWithTag("floating.expanded").assertExists()
        rule.onNodeWithTag("floating.sheet").assertDoesNotExist()
        rule.onNodeWithTag("floating.content.$textId", useUnmergedTree = true).assertExists()
        val expandedLayout = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        rule.onNode(hasText("The riverside path is open again.", substring = true) and
            hasAnyAncestor(hasTestTag("floating.content.$textId")), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(expandedLayout) }
        assertEquals(compactLayout.single().layoutInput.style.fontSize, expandedLayout.single().layoutInput.style.fontSize)
        rule.onAllNodes(hasTestTag("conversation.message.bubble.$textId") and
            hasAnyAncestor(hasTestTag("floating.card")), useUnmergedTree = true).assertCountEquals(0)
        val after = rule.onNodeWithTag("floating.card").fetchSemanticsNode().boundsInRoot
        assertEquals(before.width, after.width, 1f)
        rule.onNodeWithTag("floating.header").performTouchInput {
            val start = androidx.compose.ui.geometry.Offset(width * 0.4f, height * 0.5f)
            swipe(start, start - androidx.compose.ui.geometry.Offset(0f, 60f), 300)
        }
        rule.waitForIdle()
        val moved = rule.onNodeWithTag("floating.card").fetchSemanticsNode().boundsInRoot
        assertTrue(moved.top < after.top)
        rule.onNodeWithTag("floating.collapse").performClick()
        rule.onNodeWithTag("floating.expanded").assertDoesNotExist()
        rule.onNodeWithTag("floating.header").performTouchInput { longClick(center) }
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("floating.expanded").assertDoesNotExist()
        rule.onNodeWithTag("floating.card").assertExists()
    }

}

package dev.ipf.whitenoise

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
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

@RunWith(AndroidJUnit4::class)
class ForwardedMessageFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val chatId = "maya-chen"
    private val textId = "maya-shared-text"
    private val photoId = "maya-shared-photo"
    private fun model() = AppViewModel().apply {
        completeSignIn(OnboardingOrigin.Initial)
        dismissDiagnosticsPrompt(uiState.activeProfileId!!)
    }
    private fun open(vm: AppViewModel, id: String) {
        lateinit var nav: NavHostController
        rule.setContent { nav = rememberNavController(); WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) } }
        rule.runOnIdle { nav.navigate(AppRoute.Conversation(chatId, targetMessageId = id)) }
        scroll(id)
    }
    private fun scroll(id: String) {
        rule.onNodeWithTag("conversation.timeline").performScrollToNode(hasTestTag("conversation.message.$id"))
    }
    private fun action(id: String, label: String) {
        val action = rule.onNodeWithTag("conversation.message.$id").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first { it.label == label }
        rule.runOnIdle { assertTrue(action.action()) }
    }
    private fun marker(id: String) = rule.onNodeWithTag("message.forwarded.$id", useUnmergedTree = true)

    @Test fun textAnnouncesForwardingOnceAndCopyAndPassageSelectionKeepItOutOfTheBody() {
        val vm = model(); open(vm, textId)
        marker(textId).assertExists()
        val bubble = rule.onNodeWithTag("conversation.message.bubble.$textId")
        val description = bubble.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()
        assertEquals(1, Regex("Forwarded").findAll(description).count())
        val bounds = bubble.fetchSemanticsNode().boundsInRoot
        action(textId, "Copy")
        rule.runOnIdle {
            val clipboard = rule.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals(vm.message(chatId, textId)!!.text, clipboard.primaryClip!!.getItemAt(0).text.toString())
        }
        action(textId, "Select text")
        marker(textId).assertExists()
        rule.onNodeWithTag("message.inlineSelection.$textId").assertExists()
        rule.onNode(hasText("Forwarded") and hasAnyAncestor(hasTestTag("message.inlineSelection.$textId"))).assertDoesNotExist()
        assertEquals(bounds.height, bubble.fetchSemanticsNode().boundsInRoot.height, 1f)
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        marker(textId).assertExists()
        rule.onNodeWithTag("conversation.header.identity").performClick()
        rule.onNodeWithText("Search").performClick()
        rule.onNodeWithTag("conversation.searchField").performTextInput("riverside")
        scroll(textId)
        marker(textId).assertExists()
    }

    @Test fun photoKeepsItsLabelAfterViewingAndDeletionShowsOnlyTheTombstone() {
        val vm = model(); open(vm, photoId)
        marker(photoId).assertExists()
        rule.onNodeWithTag("conversation.media.tile.maya-shared-entrance.0").performClick()
        rule.onNodeWithTag("conversation.media.viewer.pager").assertExists()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        marker(photoId).assertExists()
        rule.runOnIdle { assertTrue(vm.deleteMessages(chatId, setOf(photoId), MessageDeletionScope.ForEveryone)) }
        marker(photoId).assertDoesNotExist()
        scroll("maya-17")
        marker("maya-17").assertDoesNotExist()
    }

    @Test fun labelFitsNarrowRtlBubblesAtLargeTypeAcrossEveryTheme() {
        val vm = model()
        val appearance = mutableStateOf(AppearancePreference.Light)
        lateinit var nav: NavHostController
        rule.setContent {
            nav = rememberNavController()
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f), LocalLayoutDirection provides LayoutDirection.Rtl) {
                WhiteNoiseTheme(appearance = appearance.value) {
                    Box(Modifier.width(280.dp)) { WhiteNoiseNavHost(nav, vm) }
                }
            }
        }
        rule.runOnIdle { nav.navigate(AppRoute.Conversation(chatId, targetMessageId = textId)) }
        AppearancePreference.entries.forEach { theme ->
            rule.runOnIdle { appearance.value = theme }
            scroll(textId)
            marker(textId).assertExists()
            val label = marker(textId).fetchSemanticsNode().boundsInRoot
            val bubble = rule.onNodeWithTag("conversation.message.bubble.$textId").fetchSemanticsNode().boundsInRoot
            assertTrue(label.left >= bubble.left && label.right <= bubble.right)
        }
    }
}

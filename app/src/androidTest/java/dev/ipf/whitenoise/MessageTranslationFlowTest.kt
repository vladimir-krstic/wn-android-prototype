package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.*
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.conversation.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageTranslationFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val chatId = "maya-chen"
    private val id = "maya-translation-hello"
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); dismissDiagnosticsPrompt(uiState.activeProfileId!!) }
    private fun open(vm: AppViewModel, restoration: StateRestorationTester? = null): NavHostController {
        lateinit var nav: NavHostController
        val content: @Composable () -> Unit = { nav = rememberNavController(); WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) } }
        if (restoration == null) rule.setContent(content) else restoration.setContent(content)
        rule.runOnIdle { nav.navigate(AppRoute.Conversation(chatId, targetMessageId = id)) }
        return nav
    }
    private fun translate() {
        rule.onNodeWithTag("conversation.timeline").performScrollToNode(hasTestTag("conversation.message.$id"))
        val action = rule.onNodeWithTag("conversation.message.$id").fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == "Translate" }
        rule.runOnIdle { assertTrue(action.action()) }
    }
    private fun waitResult() = rule.waitUntil(5_000) {
        rule.onAllNodesWithTag("translation.status.$id", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() &&
            rule.onAllNodesWithTag("translation.sheet").fetchSemanticsNodes().isEmpty()
    }
    private fun original(vm: AppViewModel) = vm.chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().first { it.id == id }.message.text

    @Test fun manualTogglePreservesOriginalAndRemembersLanguage() {
        val vm = model(); open(vm); translate()
        rule.onNodeWithTag("translation.input").assertTextEquals("Hola. ¿Cómo estás?")
        rule.onNodeWithTag("translation.detected").assertTextEquals("Detected language: Spanish")
        rule.onNodeWithTag("translation.language.Spanish").assertDoesNotExist()
        rule.onNodeWithTag("translation.language.English").performClick(); waitResult()
        rule.onNodeWithText("Hello. How are you?", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("translation.toggle.$id", useUnmergedTree = true).assertDoesNotExist()
        translate()
        rule.onNodeWithTag("translation.toggle.$id", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Hola. ¿Cómo estás?", useUnmergedTree = true).assertExists()
        rule.runOnIdle { assertEquals("Hola. ¿Cómo estás?", original(vm)); assertEquals(TranslationLanguage.English, vm.uiState.activeProfile!!.settings.translation.lastManual) }
    }
    @Test fun languagePickerBackDoesNotTranslateOrChangePreference() {
        val vm = model(); open(vm); translate()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("translation.status.$id", useUnmergedTree = true).assertDoesNotExist()
        rule.runOnIdle { assertNull(vm.uiState.activeProfile!!.settings.translation.lastManual) }
    }
    @Test fun navigationAndRecreationDiscardManualRendition() {
        val vm = model(); val restoration = StateRestorationTester(rule); open(vm, restoration); translate()
        rule.onNodeWithTag("translation.language.English").performClick(); waitResult()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("translation.status.$id", useUnmergedTree = true).assertDoesNotExist()
        rule.runOnIdle { assertEquals("Hola. ¿Cómo estás?", original(vm)) }
    }
    @Test fun globalSettingsEnableAutoAndPerChatOffWins() {
        val vm = model(); val nav = open(vm)
        rule.runOnIdle { nav.navigate(AppRoute.Translation) }
        rule.onNodeWithText("Auto-translate").performClick()
        rule.runOnIdle { assertTrue(vm.uiState.activeProfile!!.settings.translation.automatic); nav.popBackStack() }
        waitResult()
        rule.runOnIdle { vm.updateTranslationPreferences(vm.uiState.activeProfileId!!, vm.uiState.activeProfile!!.settings.translation.copy(
            chats = mapOf(chatId to ChatTranslationPreference(TranslationOverride.Off)))) }
        rule.onNodeWithTag("translation.status.$id", useUnmergedTree = true).assertDoesNotExist()
    }
    @Test fun manualErrorRetainsOriginalAndRetryRecovers() {
        val vm = model().apply { setDeveloperToolsEnabled(true); selectTranslationScenario(TranslationScenario.Timeout) }
        open(vm); translate(); rule.onNodeWithTag("translation.language.English").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Translation took too long. Try again.", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { assertEquals("Hola. ¿Cómo estás?", original(vm)) }
        rule.onNodeWithText("Retry", useUnmergedTree = true).performClick(); waitResult()
    }
    @Test fun rtlLargeTextAcrossThemesKeepsSheetOriginalActionAvailable() {
        var theme by mutableStateOf(AppearancePreference.Light)
        val source = ChatMessage("message", "them", 1, "Today", 600, "10:00 AM", "Hola.")
        val c = TranslationController("me", "chat").apply { observe(listOf(source), TranslationPreferences()); request(source.id, TranslationLanguage.Arabic); complete(source.id, entries.getValue(source.id).generation) }
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f), LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalMessageTranslation provides c) { WhiteNoiseTheme(theme) {
                    MessageTranslationSheet(source, c.state(source), onSelect = {}, onToggle = { c.toggle(source.id) },
                        onRetry = {}, onDownload = {}, onCancel = {}, onDismiss = {})
                } }
        }
        AppearancePreference.entries.forEach { appearance ->
            rule.runOnIdle { theme = appearance }
            rule.onNodeWithTag("translation.toggle.message", useUnmergedTree = true).assertIsDisplayed().performClick()
        }
    }
}

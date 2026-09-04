package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.*
import dev.ipf.whitenoise.ui.share.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncomingShareInteractionTest {
    @get:Rule val rule=createAndroidComposeRule<EmptyTestActivity>()
    private val vm=AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private fun group()=vm.createGroup("Trail", "", ProfileAvatar.Monogram,emptyList())!!
    private fun step() { vm.incoming.work!!.let { vm.incoming.advance(it.id,it.phase,it.attempt) } }
    private fun start(entry: IncomingEntry = IncomingEntry.Share(IncomingPayload("Plans for Saturday"))) : Long {
        val id=vm.incoming.receive(entry)!!; step(); return id
    }
    private fun show() { rule.setContent { WhiteNoiseTheme { IncomingShareScreen(vm.incoming) } } }
    private fun select(chat: String) {
        rule.onNodeWithTag("incoming.list").performScrollToNode(hasTestTag("incoming.chat.$chat"))
        rule.onNodeWithTag("incoming.chat.$chat").performClick()
    }
    @Test fun stagedShareNeedsAnExplicitSelectionAndPreservesDraftUntilAccepted() {
        val chat=group(); vm.updateDraftText(chat,"Existing"); start(); show()
        rule.onNodeWithText("Share to").assertExists(); rule.onNodeWithTag("incoming.stage").assertIsNotEnabled()
        select(chat); rule.onNodeWithTag("incoming.stage").assertIsEnabled()
        rule.runOnIdle { assertEquals("Existing",vm.chat(chat)!!.draftText) }
        rule.onNodeWithTag("incoming.stage").performClick(); rule.runOnIdle { step() }
        rule.runOnIdle { assertEquals("Existing\nPlans for Saturday",vm.chat(chat)!!.draftText) }
    }
    @Test fun searchAndCheckboxSelectionRemainAccessible() {
        val chat=group(); start(); show()
        rule.onNodeWithTag("incoming.search").performTextInput("Trail")
        select(chat); rule.onNodeWithTag("incoming.chat.$chat").assertIsOn()
        rule.onNodeWithText("Share to 1 chat").assertExists()
    }
    @Test fun choosingAnotherProfileClearsTheSelectionWithoutActivatingIt() {
        val original=vm.uiState.activeProfileId!!; val chat=group()
        vm.completeSignIn(OnboardingOrigin.AddProfile); val other=vm.uiState.activeProfile!!; group(); vm.selectProfile(original)
        start(); show(); select(chat); rule.onNodeWithTag("incoming.profile").performClick()
        rule.onNodeWithText(other.name).performClick()
        rule.onNodeWithTag("incoming.stage").assertIsNotEnabled()
        rule.runOnIdle { assertEquals(original,vm.uiState.activeProfileId); assertEquals(other.id,vm.incoming.work!!.selectedProfileId) }
    }
    @Test fun emptySearchHasAnHonestEmptyState() {
        start(); show(); rule.onNodeWithTag("incoming.search").performTextInput("No matching chat anywhere")
        rule.onNodeWithText("No chats to share to").assertExists(); rule.onNodeWithTag("incoming.stage").assertIsNotEnabled()
    }
    @Test fun backFirstClearsSearchThenCancelsThePendingRequest() {
        val chat=group(); start(); show(); rule.onNodeWithTag("incoming.search").performTextInput("Trail")
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("incoming.search").assert(androidx.compose.ui.test.SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed(); assertNull(vm.incoming.work); assertTrue(vm.chat(chat)!!.draftText.isEmpty()) }
    }
    @Test fun invalidDirectShareShowsFallbackPickerInsteadOfAssumingAnOwner() {
        start(IncomingEntry.Share(IncomingPayload("Hello"),IncomingTarget("wrong-profile","fiatjaf"))); show()
        rule.onNodeWithText("That chat isn’t available for sharing. Choose a destination.").assertExists()
        rule.onNodeWithTag("incoming.stage").assertIsNotEnabled()
    }
    @Test fun unavailableFilesExplainThatTheyMustBeSharedAgain() {
        start(IncomingExamples.entry(IncomingExample.Unavailable,vm.uiState.activeProfile!!,vm.uiState.signedInProfiles)); show()
        rule.onNodeWithText("The shared files aren’t available. Share them again from the original app.").assertExists()
        rule.onNodeWithText("Retry").assertDoesNotExist(); rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertNull(vm.incoming.work) }
    }
    @Test fun openingFailureStatesThatDraftsWereSavedAndOffersOnlyTheRemainingRetry() {
        val chat=group(); vm.incoming.choose(IncomingScenario.OpenFailure); val id=start()
        vm.incoming.toggle(id,chat); vm.incoming.submit(id); step(); vm.incoming.opening(id); show()
        rule.onNodeWithText("Your drafts were saved, but the chat couldn’t open. Retry to open it.").assertExists()
        rule.onNodeWithText("Retry").performClick()
        rule.runOnIdle { assertEquals(IncomingPhase.Opening,vm.incoming.work!!.phase); assertEquals("Plans for Saturday",vm.chat(chat)!!.draftText) }
    }
    @Test fun missingShortcutHasAnExplicitChatsRecoveryAction() {
        start(IncomingEntry.Conversation(IncomingTarget("missing-profile","missing-chat"))); show()
        rule.onNodeWithText("This profile is no longer signed in.").assertExists()
        rule.onNodeWithText("Go to chats").performClick()
        rule.runOnIdle { assertTrue(vm.incoming.opening(vm.incoming.work!!.id)!!.chatList) }
    }
}

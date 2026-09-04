package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.state.ComposerCaptureController
import dev.ipf.whitenoise.ui.conversation.*
import dev.ipf.whitenoise.ui.settings.DictationSettingsScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DictationCaptureTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val owner = ComposerCaptureOwner("p", "c")
    private var profile by mutableStateOf(Profile("p", "Name", "public", developerTools = DeveloperToolsState(isEnabled = true),
        chats = listOf(Chat("c", 0, ChatKind.Direct("other"), "Chat", relayUrls = listOf("wss://relay.example.com")))))
    private lateinit var capture: ComposerCaptureController
    private val sent = mutableListOf<String>()
    private fun changeDraft(text: String) { profile = profile.copy(chats = listOf(profile.chats.single().copy(draftText = text))); capture.reconcile() }
    private fun show(settings: Boolean = false) {
        capture = ComposerCaptureController({ listOf(profile) }, { profile.id }, { it == profile.id },
            { _, text -> changeDraft(text); true }, { _, _, text -> sent += text; changeDraft(""); true },
            { _, reduce -> profile = profile.copy(settings = profile.settings.copy(dictation = reduce(profile.settings.dictation))) })
        val speech = ReadAloudController().apply { attachTestOutput({ _, _ -> true }) }
        rule.setContent { WhiteNoiseTheme { CompositionLocalProvider(LocalReadAloudController provides speech) {
            ComposerCaptureHost(capture) {
                if (settings) DictationSettingsScreen(profile, {})
                else ConversationScreen(profile, profile.chats.single(), {}, { false }, {}, {}, {}, onDraftTextChanged = ::changeDraft)
            }
        } } }
    }
    private fun startThroughMenu() {
        rule.onNodeWithContentDescription("Add Attachment").performClick()
        rule.onNodeWithText("Dictation", substring = false).performScrollTo().performClick()
    }
    private fun recognize() {
        rule.runOnIdle {
            val a = capture.attempts.getValue(owner)
            if (a.phase == DictationPhase.Disclosure) capture.acceptDisclosure(owner, a.id)
            val prepared = capture.attempts.getValue(owner)
            if (prepared.phase == DictationPhase.Preparing) capture.advance(owner, prepared.id, prepared.revision)
            repeat(15) { capture.attempts.getValue(owner).let { capture.advance(owner, it.id, it.revision) } }
        }
    }
    @Test fun composerEntryDisclosesProcessingAndCancelDoesNotChangeDraft() {
        show(); startThroughMenu()
        rule.onNodeWithText("External speech recognition").assertIsDisplayed()
        rule.onNodeWithText("Cancel", substring = false).performClick()
        rule.runOnIdle { assertFalse(profile.settings.dictation.disclosureAccepted); assertNull(capture.lease); assertEquals("", profile.chats.single().draftText) }
    }
    @Test fun listeningKeepsEditorAvailableAndEditedDraftRequiresReview() {
        show(); startThroughMenu(); recognize()
        rule.onNodeWithTag("dictation.controls").assertExists()
        rule.onNodeWithTag("conversation.composer.editor").performTextInput("My draft")
        rule.onNodeWithText("Done", substring = false).performClick()
        rule.runOnIdle { capture.attempts.getValue(owner).let { capture.advance(owner, it.id, it.revision) } }
        rule.onNodeWithText("Review dictated text").assertIsDisplayed()
        rule.onNodeWithText("Insert at end").performClick()
        rule.runOnIdle { assertTrue(profile.chats.single().draftText.startsWith("My draft ")); assertTrue(sent.isEmpty()) }
    }
    @Test fun reviewBackKeepsTextAndComposerMenuReopensIt() {
        show(); startThroughMenu(); recognize()
        rule.runOnIdle { capture.background() }
        rule.onNodeWithText("Review dictated text").assertExists()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithContentDescription("Add Attachment").performClick()
        rule.onNodeWithText("Review dictated text").performScrollTo().performClick()
        rule.onNodeWithTag("dictation.transcript").assertExists()
        rule.onNodeWithText("Discard", substring = false).performClick()
        rule.runOnIdle { assertEquals("", capture.attempts.getValue(owner).retainedText) }
    }
    @Test fun permanentDenialShowsAndroidSettingsRecovery() {
        show(); rule.runOnIdle { capture.chooseScenario(DictationScenario.PermissionPermanentlyDenied) }
        startThroughMenu(); recognize()
        rule.onNodeWithText("Allow microphone access in Android Settings.").assertExists()
        rule.onNodeWithText("Open Android Settings").assertExists()
        rule.runOnIdle { assertNull(capture.lease) }
    }
    @Test fun settingsSilenceAndSendAreExplicitChoices() {
        show(settings = true)
        rule.onNodeWithText("Finish dictation").performClick()
        rule.onNodeWithText("After 5 seconds of silence").performClick()
        rule.onNodeWithText("When finished").performClick()
        rule.onNodeWithText("Send message", substring = false).performClick()
        rule.onNodeWithText("If the draft, membership or session changes, keep the text for review.").assertExists()
        rule.runOnIdle { assertEquals(5_000L, profile.settings.dictation.finishAfterSilenceMillis); assertEquals(DictationDeliveryMode.Send, profile.settings.dictation.delivery) }
    }
    @Test fun membershipLossKeepsCopyableReviewAndDisablesInsertion() {
        show(); startThroughMenu(); recognize()
        rule.runOnIdle { profile = profile.copy(chats = listOf(profile.chats.single().copy(membership = ChatMembership.Left))); capture.reconcile() }
        rule.onNodeWithText("Review dictated text").assertExists()
        rule.onNodeWithText("Insert at end").assertIsNotEnabled()
        rule.onNodeWithText("Copy", substring = false).assertIsEnabled()
    }
    @Test fun physicalVoiceTapLocksAndExplicitStopMovesToReview() {
        show(); rule.onNodeWithTag("conversation.voice").performTouchInput { click() }
        rule.onNodeWithText("Recording locked").assertExists()
        rule.runOnIdle { assertEquals(ComposerCaptureMode.Voice, capture.lease!!.mode) }
        rule.onNodeWithContentDescription("Stop Recording").performClick()
        rule.onNodeWithText("Transcribe", substring = false).assertExists()
        rule.runOnIdle { assertNull(capture.lease); assertTrue(sent.isEmpty()) }
    }
    @Test fun heldVoiceReleaseReviewsWithoutSending() {
        show(); rule.onNodeWithTag("conversation.voice").performTouchInput { longClick(durationMillis = 700) }
        rule.onNodeWithText("Transcribe", substring = false).assertExists()
        rule.runOnIdle { assertNull(capture.lease); assertTrue(sent.isEmpty()) }
    }
    @Test fun voiceBackCancelsOnlyItsOwnedCapture() {
        show(); rule.onNodeWithTag("conversation.voice").performTouchInput { click() }
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("conversation.voice").assertExists()
        rule.runOnIdle { assertNull(capture.lease); assertTrue(sent.isEmpty()) }
    }
    @Test fun tooShortRecordingShowsRetryAndRetainsDraft() {
        show(); rule.runOnIdle { capture.chooseVoiceScenario(VoiceCaptureScenario.TooShort) }
        rule.onNodeWithTag("conversation.voice").performTouchInput { click() }
        rule.onNodeWithContentDescription("Stop Recording").performClick()
        rule.onNodeWithText("Hold longer to record.").assertExists()
        rule.onNodeWithText("Retry", substring = false).assertExists()
        rule.runOnIdle { assertNull(capture.lease); assertTrue(sent.isEmpty()) }
    }
}

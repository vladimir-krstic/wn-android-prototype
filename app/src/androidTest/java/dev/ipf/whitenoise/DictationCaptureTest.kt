package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
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
        rule.setContent { WhiteNoiseTheme { CompositionLocalProvider(LocalReadAloudController provides speech, LocalPlatformDictationEnabled provides false) {
            ComposerCaptureHost(capture) {
                if (settings) DictationSettingsScreen(profile, {})
                else ConversationScreen(profile, profile.chats.single(), {}, { false }, {}, {}, {}, onDraftTextChanged = ::changeDraft)
            }
        } } }
    }
    private fun startThroughComposer() {
        rule.onNodeWithTag("conversation.dictation.start").performClick()
    }
    private fun recognize(text: String = "Hello world") {
        rule.runOnIdle {
            val id = capture.inlineDictation!!.id
            capture.inlineReady(owner, id)
            capture.inlineResult(owner, id, text, final = false)
        }
    }
    @Test fun startsInlineWithoutDisclosurePreviewOrVoiceRecordButton() {
        show()
        rule.onNodeWithContentDescription("Add Attachment").performClick()
        rule.onNodeWithText("Dictation", substring = false).assertDoesNotExist()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        val idle = rule.onNodeWithTag("conversation.composer.surface").fetchSemanticsNode().boundsInRoot
        startThroughComposer()
        val dictation = rule.onNodeWithTag("conversation.composer.surface").fetchSemanticsNode().boundsInRoot
        assertTrue(dictation.width > idle.width)
        rule.onNodeWithText("External speech recognition").assertDoesNotExist()
        rule.onNodeWithText("Message", substring = false).assertDoesNotExist()
        rule.onNodeWithText("Cancel", substring = false).assertDoesNotExist()
        rule.onNodeWithText("Done", substring = false).assertDoesNotExist()
        rule.onNodeWithTag("conversation.voice").assertDoesNotExist()
        rule.onNodeWithTag("dictation.listening").assertExists()
        rule.onNodeWithTag("dictation.pause").assertExists()
        val pause = rule.onNodeWithTag("dictation.pause").fetchSemanticsNode().boundsInRoot
        val listening = rule.onNodeWithTag("dictation.listening").fetchSemanticsNode().boundsInRoot
        assertTrue(pause.right <= listening.left)
        assertEquals(with(rule.density) { 28.dp.toPx() }, listening.center.x - pause.center.x, 1f)
        rule.onNodeWithContentDescription("Cancel").assertDoesNotExist()
        rule.onNodeWithTag("conversation.attachment.add").assertDoesNotExist()
        rule.onNodeWithTag("dictation.pause").performClick()
        rule.onNodeWithContentDescription("Cancel").assertDoesNotExist()
        rule.onNodeWithTag("conversation.attachment.add").assertExists()
        rule.onNodeWithTag("conversation.voice").assertExists()
        rule.onNodeWithContentDescription("Resume dictation").assertIsEnabled()
        assertEquals(idle.width, rule.onNodeWithTag("conversation.composer.surface").fetchSemanticsNode().boundsInRoot.width, 1f)
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.runOnIdle { assertNull(capture.inlineDictation); assertNull(capture.lease) }
        rule.onNodeWithTag("conversation.voice").assertExists()
    }
    @Test fun editingAfterDictationRestoresAddInWideComposerAndVoiceWhenCleared() {
        show(); startThroughComposer(); recognize()
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        editor.performTextReplacement("One\nTwo\nThree\nFour")
        rule.runOnIdle { assertFalse(capture.inlineDictation!!.capturing); assertNull(capture.lease) }
        val add = rule.onNodeWithTag("conversation.attachment.add").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val emoji = rule.onNodeWithTag("conversation.composer.emoji").fetchSemanticsNode().boundsInRoot
        val mic = rule.onNodeWithTag("conversation.dictation.start").fetchSemanticsNode().boundsInRoot
        assertEquals(with(rule.density) { 32.dp.toPx() }, emoji.center.x - add.center.x, 1f)
        assertEquals(mic.bottom, emoji.bottom, 1f)
        rule.onNodeWithTag("conversation.voice").assertDoesNotExist()
        editor.performTextReplacement("")
        rule.onNodeWithTag("conversation.attachment.add").assertIsDisplayed()
        val voice = rule.onNodeWithTag("conversation.voice").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val idleMic = rule.onNodeWithTag("conversation.dictation.start").fetchSemanticsNode().boundsInRoot
        assertEquals(with(rule.density) { 40.dp.toPx() }, voice.center.x - idleMic.center.x, 1f)
        rule.onNodeWithText("Message", substring = false).assertExists()
    }
    @Test fun pauseEditSelectionAndResumeInsertDirectlyInTheSameEditor() {
        show(); startThroughComposer(); recognize()
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        editor.assertTextEquals("Hello world" + DictationDots)
        rule.onNodeWithTag("dictation.pause").performClick()
        editor.performTextReplacement("Hello dear world")
        editor.performTextInputSelection(TextRange(6, 10))
        startThroughComposer(); recognize("bright")
        editor.assertTextEquals("Hello bright" + DictationDots + " world")
        rule.onNodeWithTag("dictation.pause").performClick()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        editor.assertTextEquals("Hello bright world")
        rule.runOnIdle { assertTrue(sent.isEmpty()); assertNull(capture.lease) }
    }
    private fun displayedEditorText(): String {
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithTag("conversation.composer.editor")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.single().layoutInput.text.text
    }
    @Test fun animatedIndicatorFollowsTheInsertionPointAndNeverEntersTheDraft() {
        show(); startThroughComposer()
        assertEquals(DictationDots, displayedEditorText())
        rule.onNodeWithTag("conversation.composer.editor").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Transcribing…"))
        recognize()
        assertEquals("Hello world" + DictationDots, displayedEditorText())
        rule.onNodeWithTag("dictation.pause").performClick()
        assertEquals("Hello world", displayedEditorText())
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        editor.performTextInputSelection(TextRange(6))
        startThroughComposer()
        assertEquals("Hello " + DictationDots + "world", displayedEditorText())
        recognize("dear")
        assertEquals("Hello dear " + DictationDots + "world", displayedEditorText())
        rule.runOnIdle { assertEquals("Hello dear world", profile.chats.single().draftText) }
        rule.onNodeWithTag("dictation.pause").performClick()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        editor.assertTextEquals("Hello dear world")
        rule.runOnIdle { assertNull(capture.inlineDictation); assertTrue(sent.isEmpty()) }
    }
    @Test fun movingTheCursorPausesRecognitionAndLateResultsCannotOverwriteEdits() {
        show(); startThroughComposer(); recognize()
        var id = 0L
        rule.runOnIdle { id = capture.inlineDictation!!.id }
        val editor = rule.onNodeWithTag("conversation.composer.editor")
        editor.performTextInputSelection(TextRange(0))
        rule.onNodeWithTag("dictation.pause").assertDoesNotExist()
        rule.runOnIdle { capture.inlineResult(owner, id, "late speech", true); assertNull(capture.lease) }
        editor.assertTextEquals("Hello world")
    }
    @Test fun backgroundKeepsTextAndRequiresExplicitResume() {
        show(); startThroughComposer(); recognize()
        rule.runOnIdle { capture.background() }
        rule.onNodeWithTag("conversation.composer.editor").assertTextEquals("Hello world")
        rule.onNodeWithTag("dictation.pause").assertDoesNotExist()
        rule.onNodeWithTag("conversation.dictation.start").assertIsEnabled()
        rule.onNodeWithTag("conversation.voice").assertDoesNotExist()
        rule.runOnIdle { assertNull(capture.lease); assertTrue(sent.isEmpty()) }
    }
    @Test fun backPausesDictationAndKeepsTheEditableDraft() {
        show(); startThroughComposer(); recognize()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("conversation.composer.editor").assertTextEquals("Hello world")
        rule.onNodeWithContentDescription("Resume dictation").assertIsEnabled()
        rule.runOnIdle { assertNull(capture.lease); assertFalse(capture.inlineDictation!!.capturing) }
    }
    @Test fun emptyRecognitionNeverShowsNoSpeechFeedbackAndPauseStillWorks() {
        show(); startThroughComposer()
        rule.runOnIdle {
            repeat(20) {
                val id = capture.inlineDictation!!.id
                capture.inlineReady(owner, id)
                capture.inlineRecognitionError(owner, id, DictationFailure.NoSpeech)
            }
            assertTrue(capture.inlineDictation!!.capturing)
            assertNull(capture.inlineDictation!!.failure)
        }
        rule.mainClock.advanceTimeBy(120_000)
        rule.onNodeWithTag("dictation.error").assertDoesNotExist()
        rule.onNodeWithText("No speech was recognized.").assertDoesNotExist()
        rule.onNodeWithTag("dictation.pause").performClick()
        rule.onNodeWithContentDescription("Resume dictation").assertIsEnabled()
        rule.onNodeWithContentDescription("Cancel").assertDoesNotExist()
        rule.onNodeWithTag("dictation.error").assertDoesNotExist()
        rule.runOnIdle { assertEquals("", profile.chats.single().draftText); assertNull(capture.lease) }
    }
    @Test fun permanentDenialShowsInlineAndroidSettingsRecovery() {
        show(); startThroughComposer()
        rule.runOnIdle { capture.inlineFailure(owner, capture.inlineDictation!!.id, DictationFailure.PermissionPermanentlyDenied) }
        rule.onNodeWithText("Allow microphone access in Android Settings.").assertExists()
        rule.onNodeWithContentDescription("Open Android Settings").assertExists()
        rule.onNodeWithTag("conversation.dictation.start").assertIsEnabled()
        rule.runOnIdle { assertNull(capture.lease) }
    }
    @Test fun settingsDescribeInlineEditingWithoutAutoSendOrFinishOptions() {
        show(settings = true)
        rule.onNodeWithText("Finish dictation").assertDoesNotExist()
        rule.onNodeWithText("When finished").assertDoesNotExist()
        rule.onNodeWithText("Speech appears directly in your message.", substring = true).assertExists()
        rule.onNodeWithText("Open Android Settings").assertExists()
    }
    @Test fun membershipLossRetainsTextAndStopsRecognition() {
        show(); startThroughComposer(); recognize()
        rule.runOnIdle {
            profile = profile.copy(chats = listOf(profile.chats.single().copy(membership = ChatMembership.Left)))
            capture.reconcile()
            assertFalse(capture.inlineDictation!!.capturing)
            assertNull(capture.lease)
            assertEquals("Hello world", profile.chats.single().draftText)
        }
    }
    @Test fun physicalVoiceTapLocksAndExplicitStopMovesToReview() {
        show(); rule.onNodeWithTag("conversation.voice").performTouchInput { click() }
        rule.onNodeWithText("Recording locked").assertDoesNotExist()
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

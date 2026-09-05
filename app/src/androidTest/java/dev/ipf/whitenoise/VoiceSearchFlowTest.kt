package dev.ipf.whitenoise

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.app.ActivityOptionsCompat
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.ui.chats.ChatsScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** The registry supplies Android results without launching a recognizer or using a microphone. */
class VoiceSearchFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private var profile by mutableStateOf(ProfileFixtures.marmota)
    private var launchedRequestCode = 0
    private val launches = mutableListOf<Intent>()
    private var missingRecognizer = false
    private val registry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
            if (missingRecognizer) throw ActivityNotFoundException()
            launchedRequestCode = requestCode
            launches += contract.createIntent(rule.activity, input)
        }
    }
    private val registryOwner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = registry
    }
    private fun content(restore: StateRestorationTester? = null) {
        val screen: @androidx.compose.runtime.Composable () -> Unit = {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                WhiteNoiseTheme {
                    ChatsScreen(
                        AppUiState(listOf(profile), profile.id, setOf(profile.id)),
                        {}, {}, { _, _ -> }, {}, { _, _ -> }, { _, _ -> }, { false }, {},
                    )
                }
            }
        }
        if (restore == null) rule.setContent(screen) else restore.setContent(screen)
        rule.onNodeWithContentDescription("Search Chats").performClick()
        rule.onNodeWithTag("chats.searchField").performTextInput("existing search")
    }
    private fun speak() = rule.onNodeWithContentDescription("Voice Search").performClick()
    private fun result(code: Int = Activity.RESULT_OK, vararg phrases: String) = rule.runOnIdle {
        registry.dispatchResult(launchedRequestCode, code, Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, arrayListOf(*phrases)))
    }

    @Test fun microphoneUsesNativeRecognizerAndItsActualPhrase() {
        content(); speak()
        rule.runOnIdle {
            assertEquals(1, launches.size)
            assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, launches.single().action)
            assertEquals(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, launches.single().getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL))
        }
        rule.onNodeWithContentDescription("Voice Search").assertIsNotEnabled()
        rule.onNodeWithText("Getting your search…").assertDoesNotExist()
        result(phrases = arrayOf("  tomorrow at noon  ", "unused alternative"))
        rule.onNodeWithTag("chats.searchField").assertTextContains("tomorrow at noon")
        rule.onNodeWithContentDescription("Voice Search").assertIsEnabled()
    }

    @Test fun cancellationAndEmptyResultsNeverEraseTheExistingSearch() {
        content(); speak(); result(Activity.RESULT_CANCELED)
        rule.onNodeWithTag("chats.searchField").assertTextContains("existing search")
        speak(); result(phrases = arrayOf(" "))
        rule.onNodeWithText("Voice search is unavailable. Try again or type your search.").assertIsDisplayed()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithTag("chats.searchField").assertTextContains("existing search")
    }

    @Test fun missingRecognizerOffersRetryAndSuccessfulRetryUsesTheResult() {
        content()
        rule.runOnIdle { missingRecognizer = true }
        speak()
        rule.onNodeWithText("Voice search is unavailable. Try again or type your search.").assertIsDisplayed()
        rule.runOnIdle { missingRecognizer = false }
        rule.onNodeWithText("Try Again").performClick()
        result(phrases = arrayOf("family"))
        rule.onNodeWithTag("chats.searchField").assertTextContains("family")
    }

    @Test fun editedQueryAndChangedProfileRejectLateResults() {
        content(); speak()
        rule.onNodeWithTag("chats.searchField").performTextReplacement("newer search")
        result(phrases = arrayOf("old voice result"))
        rule.onNodeWithTag("chats.searchField").assertTextContains("newer search")
        speak()
        rule.runOnIdle { profile = profile.copy(id = "other-profile") }
        result(phrases = arrayOf("previous profile result"))
        rule.onNodeWithContentDescription("Search Chats").performClick()
        rule.onNodeWithTag("chats.searchField").assert(hasText("previous profile result").not())
    }

    @Test fun closingSearchRejectsTheOutstandingResult() {
        content(); speak()
        rule.onNodeWithContentDescription("Close search").performClick()
        rule.onNodeWithContentDescription("Search Chats").performClick()
        rule.onNodeWithTag("chats.searchField").performTextInput("new search session")
        rule.onNodeWithContentDescription("Voice Search").assertIsNotEnabled()
        result(phrases = arrayOf("previous search session result"))
        rule.onNodeWithTag("chats.searchField").assertTextContains("new search session")
        rule.onNodeWithContentDescription("Voice Search").assertIsEnabled()
    }

    @Test fun recognitionSurvivesRecreationWithoutLaunchingTwice() {
        val restore = StateRestorationTester(rule)
        content(restore); speak()
        restore.emulateSavedInstanceStateRestore()
        rule.runOnIdle { assertEquals(1, launches.size) }
        result(phrases = arrayOf("returned after recreation"))
        rule.onNodeWithTag("chats.searchField").assertTextContains("returned after recreation")
    }
}

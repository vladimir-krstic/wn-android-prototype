package dev.ipf.whitenoise

import android.view.View
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.ui.onboarding.WelcomeScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingKeyboardTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun welcomeGeometryDoesNotFollowTheOutgoingKeyboardInsets() {
        lateinit var composeView: View
        lateinit var imeInsets: WindowInsets
        composeRule.setContent {
            val view = LocalView.current
            val ime = WindowInsets.ime
            SideEffect {
                composeView = view
                imeInsets = ime
            }
            WhiteNoiseTheme { WelcomeScreen(OnboardingOrigin.Initial, {}, {}, {}) }
        }
        val originalInsets = composeRule.runOnIdle {
            checkNotNull(ViewCompat.getRootWindowInsets(composeView))
        }

        fun dispatchImeHeight(height: Int) {
            composeRule.runOnIdle {
                ViewCompat.dispatchApplyWindowInsets(
                    composeView,
                    WindowInsetsCompat.Builder(originalInsets)
                        .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, height))
                        .setVisible(WindowInsetsCompat.Type.ime(), height > 0)
                        .build(),
                )
            }
            composeRule.waitForIdle()
        }

        try {
            dispatchImeHeight(0)
            val markBefore = composeRule.onNodeWithContentDescription("White Noise")
                .fetchSemanticsNode().boundsInRoot
            val signInBefore = composeRule.onNodeWithText("Sign In").fetchSemanticsNode().boundsInRoot
            val signUpBefore = composeRule.onNodeWithText("Sign Up").fetchSemanticsNode().boundsInRoot

            dispatchImeHeight(600)
            composeRule.runOnIdle { assertEquals(600, imeInsets.getBottom(composeRule.density)) }

            assertEquals(markBefore, composeRule.onNodeWithContentDescription("White Noise").fetchSemanticsNode().boundsInRoot)
            assertEquals(signInBefore, composeRule.onNodeWithText("Sign In").fetchSemanticsNode().boundsInRoot)
            assertEquals(signUpBefore, composeRule.onNodeWithText("Sign Up").fetchSemanticsNode().boundsInRoot)
        } finally {
            composeRule.runOnIdle {
                ViewCompat.dispatchApplyWindowInsets(composeView, originalInsets)
                ViewCompat.requestApplyInsets(composeView)
            }
        }
    }

    @Test
    fun signInBackReturnsToTheSameWelcomeLayoutAfterEditing() {
        assertBackRestoresWelcome("Sign In", hasSetTextAction())
    }

    @Test
    fun signUpBackReturnsToTheSameWelcomeLayoutAfterEditing() {
        assertBackRestoresWelcome("Sign Up", hasSetTextAction().and(hasText("Marmota")))
    }

    private fun assertBackRestoresWelcome(destination: String, editor: SemanticsMatcher) {
        val markBefore = composeRule.onNodeWithContentDescription("White Noise")
            .fetchSemanticsNode().boundsInRoot
        val signUpBefore = composeRule.onNodeWithText("Sign Up").fetchSemanticsNode().boundsInRoot

        composeRule.onNodeWithText(destination).performClick()
        composeRule.onNode(editor).performClick().assertIsFocused()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("White Noise").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == false
        }

        assertEquals(markBefore, composeRule.onNodeWithContentDescription("White Noise").fetchSemanticsNode().boundsInRoot)
        assertEquals(signUpBefore, composeRule.onNodeWithText("Sign Up").fetchSemanticsNode().boundsInRoot)
    }
}

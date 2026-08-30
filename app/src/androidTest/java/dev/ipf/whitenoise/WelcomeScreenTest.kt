package dev.ipf.whitenoise

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun welcomeActionsAreVisible() {
        composeRule.onNodeWithText("Sign In")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(56.dp)
        composeRule.onNodeWithText("Sign Up")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(56.dp)
    }

    @Test
    fun welcomeMarkScalesWithWidthAndCentersBetweenSafeTopAndActions() {
        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val signInBounds = composeRule.onNodeWithText("Sign In")
            .fetchSemanticsNode().boundsInRoot
        val markBounds = composeRule.onNodeWithContentDescription("White Noise")
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val safeInsets = checkNotNull(
            ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView),
        ).getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val density = composeRule.activity.resources.displayMetrics.density
        val safeLeft = rootBounds.left + safeInsets.left
        val safeRight = rootBounds.right - safeInsets.right
        val safeTop = rootBounds.top + safeInsets.top
        val markAspectRatio = 598f / 460f
        val maxMarkHeight = (signInBounds.top - safeTop - 32f * density).coerceAtLeast(0f)

        assertEquals(
            minOf((safeRight - safeLeft) / 2f, 260f * density, maxMarkHeight * markAspectRatio),
            markBounds.width,
            1f,
        )
        assertEquals(markBounds.width / markAspectRatio, markBounds.height, 1f)
        assertEquals((safeLeft + safeRight) / 2f, markBounds.center.x, 1f)
        assertEquals(
            (safeTop + signInBounds.top) / 2f,
            markBounds.center.y,
            1f,
        )
    }

    @Test
    fun signInOpensTheSecureCredentialScreen() {
        composeRule.onNodeWithText("Sign In").performClick()
        composeRule.onNodeWithText("Private Key").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Paste private key").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Scan QR Code")
            .assertIsDisplayed()
            .assertWidthIsEqualTo(56.dp)
            .assertHeightIsEqualTo(56.dp)
    }

    @Test
    fun signInPasteActionBecomesClearAfterEntry() {
        composeRule.onNodeWithText("Sign In").performClick()

        composeRule.onNodeWithContentDescription("Paste private key").performClick()

        composeRule.onNodeWithContentDescription("Clear private key").assertIsDisplayed()
    }

    @Test
    fun signUpOpensProfileCreation() {
        composeRule.onNodeWithText("Sign Up").performClick()
        composeRule.onNodeWithText("Name").assertIsDisplayed()
        composeRule.onNodeWithText("About").assertIsDisplayed()
    }

    @Test
    fun signUpFormScrollsIndependentlyBehindThePinnedAction() {
        composeRule.onNodeWithText("Sign Up").performClick()

        composeRule.onNode(
            hasScrollAction()
                .and(hasAnyDescendant(hasText("Name")))
                .and(hasAnyDescendant(hasText("About"))),
            useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding.sign_up.action").assertIsDisplayed()
    }

    @Test
    fun unhandledBackgroundTapClearsTextInputFocus() {
        composeRule.onNodeWithText("Sign Up").performClick()
        val nameField = composeRule.onNode(hasSetTextAction().and(hasText("Marmota")))

        nameField.performClick().assertIsFocused()
        composeRule.onAllNodes(isRoot())[0].performTouchInput {
            click(Offset(x = 1f, y = center.y))
        }

        nameField.assertIsNotFocused()
    }

    @Test
    fun signUpPhotoActionExposesTheThreeAcceptedSources() {
        composeRule.onNodeWithText("Sign Up").performClick()
        composeRule.onNodeWithText("Add Photo").performClick()

        composeRule.onNodeWithText("Choose from Photos").assertIsDisplayed()
        composeRule.onNodeWithText("Choose from Files").assertIsDisplayed()
        composeRule.onNodeWithText("Find Image on Web").assertIsDisplayed()
    }
}

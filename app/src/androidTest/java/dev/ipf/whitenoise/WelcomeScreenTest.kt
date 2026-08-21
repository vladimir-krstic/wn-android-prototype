package dev.ipf.whitenoise

import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
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
    fun welcomeMarkMatchesTheCenteredSystemSplashGeometry() {
        val rootBounds = composeRule.onRoot().getUnclippedBoundsInRoot()
        val markBounds = composeRule.onNodeWithContentDescription("White Noise")
            .assertWidthIsEqualTo(149.5.dp)
            .assertHeightIsEqualTo(115.dp)
            .getUnclippedBoundsInRoot()

        assertEquals(
            (rootBounds.left.value + rootBounds.right.value) / 2f,
            (markBounds.left.value + markBounds.right.value) / 2f,
            1f,
        )
        assertEquals(
            (rootBounds.top.value + rootBounds.bottom.value) / 2f,
            (markBounds.top.value + markBounds.bottom.value) / 2f,
            1f,
        )
    }

    @Test
    fun signInOpensTheSecureCredentialScreen() {
        composeRule.onNodeWithText("Sign In").performClick()
        composeRule.onNodeWithText("Private Key").assertIsDisplayed()
        composeRule.onNodeWithText("Scan QR Code")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun signUpOpensProfileCreation() {
        composeRule.onNodeWithText("Sign Up").performClick()
        composeRule.onNodeWithText("Name").assertIsDisplayed()
        composeRule.onNodeWithText("About").assertIsDisplayed()
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

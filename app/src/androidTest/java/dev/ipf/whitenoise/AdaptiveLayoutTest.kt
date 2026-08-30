package dev.ipf.whitenoise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.ui.chats.ChatsScreen
import dev.ipf.whitenoise.ui.conversation.ConversationScreen
import dev.ipf.whitenoise.ui.chats.GroupSetupScreen
import dev.ipf.whitenoise.ui.onboarding.SignUpScreen
import dev.ipf.whitenoise.ui.onboarding.WelcomeScreen
import dev.ipf.whitenoise.ui.settings.SupportScreen
import dev.ipf.whitenoise.ui.settings.SettingsScreen
import dev.ipf.whitenoise.ui.settings.SignOutSheet
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdaptiveLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<EmptyTestActivity>()

    @Test
    fun welcomeCapsLogoAndActionWidthsOnExpandedWindows() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f)) {
                WhiteNoiseTheme {
                    Box(
                        Modifier
                            .requiredSize(width = 900.dp, height = 800.dp)
                            .consumeWindowInsets(WindowInsets.safeDrawing),
                    ) {
                        WelcomeScreen(OnboardingOrigin.Initial, {}, {}, {})
                    }
                }
            }
        }

        val markBounds = composeRule.onNodeWithContentDescription("White Noise")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val signInBounds = composeRule.onNodeWithText("Sign In")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val signUpBounds = composeRule.onNodeWithText("Sign Up")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot

        assertEquals(260f, markBounds.width, 1f)
        assertEquals(488f, signInBounds.width, 1f)
        assertEquals(signInBounds.width, signUpBounds.width, 1f)
        assertEquals(signInBounds.center.x, markBounds.center.x, 1f)
        assertTrue(markBounds.bottom < signInBounds.top)
    }

    @Test
    fun addProfileWelcomeShrinksLogoAboveActionsAtShortHeightAndLargeRtlText() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                WhiteNoiseTheme {
                    Box(
                        Modifier
                            .requiredSize(width = 640.dp, height = 360.dp)
                            .consumeWindowInsets(WindowInsets.safeDrawing),
                    ) {
                        WelcomeScreen(OnboardingOrigin.AddProfile, {}, {}, {})
                    }
                }
            }
        }

        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        val titleBounds = composeRule.onNodeWithText("Add Profile")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val markBounds = composeRule.onNodeWithContentDescription("White Noise")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val signInBounds = composeRule.onNodeWithText("Sign In")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val signUpBounds = composeRule.onNodeWithText("Sign Up")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot

        assertTrue(markBounds.width > 0f && markBounds.width < 260f)
        assertEquals(markBounds.width * 460f / 598f, markBounds.height, 1f)
        assertTrue(markBounds.top > titleBounds.bottom)
        assertTrue(markBounds.bottom < signInBounds.top)
        assertTrue(signInBounds.bottom < signUpBounds.top)
    }

    @Test
    fun settingsSurfaceKeepsPrimaryContentAtLargeTextAndRtl() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                WhiteNoiseTheme { SupportScreen(ProfileFixtures.marmota, {}, {}, {}) }
            }
        }

        composeRule.onNodeWithText("Chat with support").assertIsDisplayed()
        composeRule.onNodeWithText("Start Chat").assertIsDisplayed()
    }

    @Test
    fun settingsRootKeepsProfileAndDestinationHierarchyAtLargeTextAndRtl() {
        val profile = ProfileFixtures.marmota
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                WhiteNoiseTheme {
                    SettingsScreen(
                        uiState = AppUiState(listOf(profile), profile.id, setOf(profile.id)),
                        onBack = {},
                        onSelectProfile = {},
                        onAddProfile = {},
                        onShareConnect = {},
                        onEditProfile = {},
                        onProfileKeys = {},
                        onNotifications = {},
                        onAppearance = {},
                        onPrivacy = {},
                        onDataUsage = {},
                        onRelays = {},
                        onSupport = {},
                        onDonate = {},
                        onDeveloperTools = {},
                        onSignOut = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open Share & Connect for Marmota").assertIsDisplayed()
        composeRule.onNodeWithText("Profile Keys").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun destructiveSheetKeepsConfirmationAndPrimaryActionAtLargeTextAndRtl() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                WhiteNoiseTheme {
                    SignOutSheet(ProfileFixtures.marmota, onDismiss = {}, onComplete = {})
                }
            }
        }

        composeRule.onNodeWithText("Profile name").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sign Out").assertIsDisplayed()
    }

    @Test
    fun chatsSurfaceConstrainsContentAtExpandedWidth() {
        val profile = ProfileFixtures.marmota
        composeRule.setContent {
            WhiteNoiseTheme {
                Box(Modifier.requiredWidth(900.dp)) {
                    ChatsScreen(
                        uiState = AppUiState(listOf(profile), profile.id, setOf(profile.id)),
                        onNewMessage = {},
                        onOpenChat = {},
                        onMarkUnread = { _, _ -> },
                        onTogglePin = {},
                        onMute = { _, _ -> },
                        onArchive = { _, _ -> },
                        onLeave = { false },
                        onDelete = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule.onNodeWithText("Chats").assertIsDisplayed()
        composeRule.onNodeWithText("Direct - Text & Delivery").assertIsDisplayed()
    }

    @Test
    fun conversationSurfaceKeepsTitleAtLargeTextAndRtl() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "fiatjaf" }
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                WhiteNoiseTheme {
                    ConversationScreen(profile, chat, {}, { true }, {}, {}, {})
                }
            }
        }

        composeRule.onNodeWithText("Fiatjaf").assertIsDisplayed()
    }

    @Test
    fun signUpKeepsProfileFieldsReachableAtLargeTextAndRtl() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                WhiteNoiseTheme {
                    SignUpScreen(
                        initialName = "Marmota",
                        onBack = {},
                        onSignUp = { _, _, _ -> },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Add Photo").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding.sign_up.action").assertIsDisplayed()
        composeRule.onNodeWithText("About").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun groupSetupKeepsMembersReachableAtLargeTextAndRtl() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                WhiteNoiseTheme {
                    GroupSetupScreen(
                        profile = ProfileFixtures.marmota,
                        selectedPersonIds = listOf("maya-chen"),
                        onBack = {},
                        onCreate = { _, _, _ -> true },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Members").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Maya Chen").performScrollTo().assertIsDisplayed()
    }
}

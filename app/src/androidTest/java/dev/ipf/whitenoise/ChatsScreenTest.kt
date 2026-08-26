package dev.ipf.whitenoise

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.ui.chats.ChatsScreen
import dev.ipf.whitenoise.ui.chats.GroupSetupScreen
import dev.ipf.whitenoise.ui.chats.NewChatScreen
import dev.ipf.whitenoise.ui.chats.NewGroupScreen
import dev.ipf.whitenoise.ui.chats.PersonProfileScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun populatedChatsExposeCatalogAndNativeCreationAction() {
        val profile = ProfileFixtures.marmota
        composeRule.setContent {
            WhiteNoiseTheme {
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
                )
            }
        }

        composeRule.onNodeWithText("Direct - Text & Delivery").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("New Message").assertIsDisplayed()
        composeRule.onNodeWithText("Read All").assertIsNotDisplayed()
    }

    @Test
    fun topBarOpensSettingsAndKeepsSearchAndScopesOnDemand() {
        val profile = ProfileFixtures.marmota
        var settingsOpened = false
        composeRule.setContent {
            WhiteNoiseTheme {
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
                    onSettings = { settingsOpened = true },
                )
            }
        }

        composeRule.onNodeWithText("Search Chats").assertIsNotDisplayed()
        composeRule.onNodeWithContentDescription("Open Settings for Marmota").performClick()
        composeRule.runOnIdle { check(settingsOpened) }

        composeRule.onNodeWithContentDescription("Filter Chats").performClick()
        composeRule.onNode(hasText("Chats").and(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)))
            .assertIsSelected()
        composeRule.onNodeWithText("Unread").performClick()
        composeRule.onNodeWithText("Unread").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Filter Chats").performClick()
        composeRule.onNode(hasText("Unread").and(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)))
            .assertIsSelected().performClick()

        composeRule.onNodeWithContentDescription("Search Chats").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("not a fixture")
        composeRule.onNodeWithText("No Results").assertIsDisplayed()
    }

    @Test
    fun newMessageStartsWithGroupThenPeople() {
        composeRule.setContent {
            WhiteNoiseTheme {
                NewChatScreen(
                    profile = ProfileFixtures.marmota,
                    onBack = {},
                    onNewGroup = {},
                    onPerson = {},
                )
            }
        }

        composeRule.onNodeWithText("New Group").assertIsDisplayed()
        composeRule.onNodeWithText("People").assertIsDisplayed()
    }

    @Test
    fun newMessageSearchKeepsTheGroupActionAndUsesTheSharedEmptyState() {
        composeRule.setContent {
            WhiteNoiseTheme {
                NewChatScreen(
                    profile = ProfileFixtures.marmota,
                    onBack = {},
                    onNewGroup = {},
                    onPerson = {},
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("not a person")
        composeRule.onNodeWithText("New Group").assertIsDisplayed()
        composeRule.onNodeWithText("No Results").assertIsDisplayed()
        composeRule.onNodeWithText("Check the spelling or try a different search.").assertIsDisplayed()
    }

    @Test
    fun groupSelectionRequiresAndPreservesAtLeastOnePerson() {
        var continuedWith = emptyList<String>()
        composeRule.setContent {
            WhiteNoiseTheme {
                NewGroupScreen(
                    profile = ProfileFixtures.marmota,
                    onBack = {},
                    onContinue = { continuedWith = it },
                )
            }
        }

        composeRule.onNodeWithText("Continue").assertIsNotEnabled()
        composeRule.onNodeWithText("Maya Chen").performClick()
        composeRule.onNodeWithText("Continue").assertIsEnabled().performClick()
        composeRule.runOnIdle { check(continuedWith == listOf("maya-chen")) }
    }

    @Test
    fun groupSetupKeepsCreateDisabledUntilTheRequiredNameIsEntered() {
        var createdName: String? = null
        composeRule.setContent {
            WhiteNoiseTheme {
                GroupSetupScreen(
                    profile = ProfileFixtures.marmota,
                    selectedPersonIds = listOf("maya-chen"),
                    onBack = {},
                    onCreate = { name, _, _ ->
                        createdName = name
                        true
                    },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Group photo").assertIsDisplayed()
        composeRule.onNodeWithText("Create Group").assertIsNotEnabled()
        composeRule.onNodeWithText("Group Name").performTextInput("Night Owls")
        composeRule.onNodeWithText("Create Group").assertIsEnabled().performClick()
        composeRule.runOnIdle { check(createdName == "Night Owls") }
    }

    @Test
    fun groupSetupExposesTheAcceptedPhotoSources() {
        composeRule.setContent {
            WhiteNoiseTheme {
                GroupSetupScreen(
                    profile = ProfileFixtures.marmota,
                    selectedPersonIds = listOf("maya-chen"),
                    onBack = {},
                    onCreate = { _, _, _ -> true },
                )
            }
        }

        composeRule.onNodeWithText("Add Photo").performClick()
        composeRule.onNodeWithText("Choose from Photos").assertIsDisplayed()
        composeRule.onNodeWithText("Choose from Files").assertIsDisplayed()
        composeRule.onNodeWithText("Find Image on Web").assertIsDisplayed()
        composeRule.onNodeWithText("Find Image on Web").performClick()
        composeRule.onNodeWithText("Choose from Photos").assertDoesNotExist()
        composeRule.onNodeWithText("Search privacy").assertIsDisplayed()
    }

    @Test
    fun personProfileOffersRelayRecoveryWhenMessagingIsUnavailable() {
        val profile = ProfileFixtures.marmota.copy(chatRelayUrls = emptyList())
        val person = profile.people.first { it.id == "maya-chen" }
        var relaysOpened = false
        composeRule.setContent {
            WhiteNoiseTheme {
                PersonProfileScreen(
                    profile = profile,
                    person = person,
                    onBack = {},
                    onMessage = { false },
                    onToggleFollow = {},
                    onToggleBlock = {},
                    onOpenRelays = { relaysOpened = true },
                )
            }
        }

        composeRule.onNodeWithText("Check Chat Relays").assertIsDisplayed().performClick()
        composeRule.runOnIdle { check(relaysOpened) }
    }
}

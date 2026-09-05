package dev.ipf.whitenoise

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.ui.chats.ChatsScreen
import dev.ipf.whitenoise.ui.chats.GroupSetupScreen
import dev.ipf.whitenoise.ui.chats.GroupsInCommonScreen
import dev.ipf.whitenoise.ui.chats.NewChatScreen
import dev.ipf.whitenoise.ui.chats.NewGroupScreen
import dev.ipf.whitenoise.ui.chats.PersonProfileScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<EmptyTestActivity>()

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

        composeRule.onNodeWithTag("chats.scope.chats").assertIsSelected()
        composeRule.onNodeWithTag("chats.folder.system:unread").performClick().assertIsSelected()
        composeRule.onNodeWithTag("chats.folder.system:unread").performClick().assertIsSelected()

        composeRule.onNodeWithContentDescription("Search Chats").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("not a fixture")
        composeRule.onNodeWithText("No matches").assertIsDisplayed()
    }

    @Test
    fun newMessageStartsWithGroupThenPeople() {
        var groupedColor = Color.Unspecified
        var dividerColor = Color.Unspecified
        composeRule.setContent {
            WhiteNoiseTheme {
                val colors = MaterialTheme.colorScheme
                SideEffect {
                    groupedColor = colors.surfaceContainerLowest
                    dividerColor = colors.surfaceContainerLow
                }
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
        composeRule.onNodeWithTag("new_message.searchField").assertHeightIsEqualTo(56.dp)

        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val list = composeRule.onNodeWithTag("new_message.list").fetchSemanticsNode().boundsInRoot
        val row = composeRule.onNodeWithTag("creation.person.aisha-rahman")
            .fetchSemanticsNode().boundsInRoot
        val avatar = composeRule.onNodeWithTag("creation.person.aisha-rahman.avatar", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val name = composeRule.onNodeWithTag("creation.person.aisha-rahman.name", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val outerInset = with(composeRule.density) { 16.dp.toPx() }
        val avatarTextGap = with(composeRule.density) { 12.dp.toPx() }
        val pixels = composeRule.onNodeWithTag("creation.person.aisha-rahman")
            .captureToImage().toPixelMap()
        val rowColor = pixels[pixels.width / 2, pixels.height / 2]
        val divider = composeRule.onNodeWithTag("creation.person.aisha-rahman.divider")
        val dividerPixels = divider.captureToImage().toPixelMap()
        val actualDividerColor = dividerPixels[dividerPixels.width / 2, dividerPixels.height / 2]
        check(kotlin.math.abs(root.bottom - list.bottom) < 1f)
        check(kotlin.math.abs((row.left - list.left) - outerInset) < 1f)
        check(kotlin.math.abs((avatar.left - row.left) - outerInset) < 1f)
        check(kotlin.math.abs((name.left - avatar.right) - avatarTextGap) < 1f)
        divider.assertHeightIsEqualTo(2.dp)
        check(kotlin.math.abs(groupedColor.red - rowColor.red) < .01f)
        check(kotlin.math.abs(groupedColor.green - rowColor.green) < .01f)
        check(kotlin.math.abs(groupedColor.blue - rowColor.blue) < .01f)
        check(kotlin.math.abs(dividerColor.red - actualDividerColor.red) < .01f)
        check(kotlin.math.abs(dividerColor.green - actualDividerColor.green) < .01f)
        check(kotlin.math.abs(dividerColor.blue - actualDividerColor.blue) < .01f)
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
        composeRule.onNodeWithTag("creation.searchField").assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithTag("new_group.list").performScrollToNode(hasText("Maya Chen"))
        composeRule.onNodeWithText("Maya Chen").performClick()
        composeRule.onNodeWithTag("new_group.list")
            .performScrollToNode(hasTestTag("new_group.selected.maya-chen"))
        val target = composeRule.onNodeWithTag("new_group.selected.maya-chen")
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val visual = composeRule.onNodeWithTag(
            "new_group.selected.visual.maya-chen",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        assertTrue(visual.width < target.width)
        assertTrue(visual.height < target.height)
        composeRule.onNodeWithText("Continue").assertIsEnabled().performClick()
        composeRule.runOnIdle { check(continuedWith == listOf("maya-chen")) }
    }

    @Test
    fun selectedPersonAvatarStripRemovesTheSelection() {
        composeRule.setContent {
            WhiteNoiseTheme {
                NewGroupScreen(
                    profile = ProfileFixtures.marmota,
                    onBack = {},
                    onContinue = {},
                )
            }
        }

        composeRule.onNodeWithTag("new_group.list").performScrollToNode(hasText("Maya Chen"))
        composeRule.onNodeWithText("Maya Chen").performClick()
        composeRule.onNodeWithTag("new_group.list")
            .performScrollToNode(hasTestTag("new_group.selected.maya-chen"))
        composeRule.onNodeWithTag("new_group.selected.maya-chen").performClick()
        composeRule.onNodeWithText("Continue").assertIsNotEnabled()
    }

    @Test
    fun newGroupUsesCompactGroupedSearchPeopleAndScrolledActionSurface() {
        var canvas = Color.Unspecified
        var grouped = Color.Unspecified
        var scrolled = Color.Unspecified
        composeRule.setContent {
            WhiteNoiseTheme {
                val colors = MaterialTheme.colorScheme
                SideEffect {
                    canvas = colors.surfaceContainerLow
                    grouped = colors.surfaceContainerLowest
                    scrolled = colors.surfaceContainer
                }
                NewGroupScreen(
                    profile = ProfileFixtures.marmota,
                    onBack = {},
                    onContinue = {},
                )
            }
        }

        fun sampledCornerColor(tag: String): Color {
            val pixels = composeRule.onNodeWithTag(tag).captureToImage().toPixelMap()
            return pixels[2, 2]
        }
        fun sampledBottomStartColor(tag: String): Color {
            val pixels = composeRule.onNodeWithTag(tag).captureToImage().toPixelMap()
            return pixels[2, pixels.height - 3]
        }
        fun sampledCenterColor(tag: String): Color {
            val pixels = composeRule.onNodeWithTag(tag).captureToImage().toPixelMap()
            return pixels[pixels.width / 2, pixels.height / 2]
        }
        fun sampledTopCenterColor(tag: String): Color {
            val pixels = composeRule.onNodeWithTag(tag).captureToImage().toPixelMap()
            return pixels[pixels.width / 2, 2]
        }
        fun assertColor(expected: Color, actual: Color) {
            check(kotlin.math.abs(expected.red - actual.red) < .01f)
            check(kotlin.math.abs(expected.green - actual.green) < .01f)
            check(kotlin.math.abs(expected.blue - actual.blue) < .01f)
        }

        val listNode = composeRule.onNodeWithTag("new_group.list")
        val list = listNode.fetchSemanticsNode().boundsInRoot
        val search = composeRule.onNodeWithTag("creation.searchField")
        val searchBounds = search.fetchSemanticsNode().boundsInRoot
        search.assertHeightIsEqualTo(48.dp)
        check(kotlin.math.abs((searchBounds.left - list.left) - with(composeRule.density) { 16.dp.toPx() }) < 1f)
        val searchColor = sampledTopCenterColor("creation.searchField")
        assertColor(canvas, sampledCornerColor("creation.bottomAction"))

        listNode.performScrollToNode(hasText("Maya Chen"))
        val row = composeRule.onNodeWithTag("creation.person.maya-chen")
            .fetchSemanticsNode().boundsInRoot
        val avatar = composeRule.onNodeWithTag("creation.person.maya-chen.avatar", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val name = composeRule.onNodeWithTag("creation.person.maya-chen.name", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val divider = composeRule.onNodeWithTag("creation.person.maya-chen.divider")
        val outerInset = with(composeRule.density) { 16.dp.toPx() }
        val avatarTextGap = with(composeRule.density) { 12.dp.toPx() }

        divider.assertHeightIsEqualTo(2.dp)
        check(kotlin.math.abs((row.left - list.left) - outerInset) < 1f)
        check(kotlin.math.abs((avatar.left - row.left) - outerInset) < 1f)
        check(kotlin.math.abs((name.left - avatar.right) - avatarTextGap) < 1f)
        assertColor(grouped, searchColor)
        assertColor(grouped, sampledCenterColor("creation.person.maya-chen"))
        assertColor(grouped, sampledBottomStartColor("creation.person.maya-chen"))
        assertColor(canvas, sampledCenterColor("creation.person.maya-chen.divider"))
        composeRule.onNodeWithTag("creation.person.maya-chen").performClick().assertIsOn()
        composeRule.waitForIdle()
        assertColor(grouped, sampledBottomStartColor("creation.person.maya-chen"))
        composeRule.onNodeWithTag("new_group.list").performScrollToIndex(8)
        composeRule.waitForIdle()
        assertColor(scrolled, sampledCornerColor("creation.bottomAction"))
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
        composeRule.onNodeWithTag("group_setup.avatar").assertHeightIsEqualTo(120.dp)
        composeRule.onNodeWithTag("group_setup.photoAction").assertHeightIsEqualTo(40.dp)
        composeRule.onNodeWithText("?").assertDoesNotExist()
        composeRule.onNodeWithText("Group Details").assertDoesNotExist()
        composeRule.onNodeWithTag("creation.primaryAction").assertIsNotEnabled()
        val groupName = composeRule.onNodeWithText("Group Name")
        groupName.performTextReplacement("Night Owls")
        groupName.assertTextContains("Night Owls")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("creation.primaryAction")
            .assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.runOnIdle {
            check(createdName == "Night Owls") {
                "Expected the normalized group name, got $createdName"
            }
        }
    }

    @Test
    fun groupSetupUsesGroupedMemberRowsAndScrolledActionSurface() {
        var resting = Color.Unspecified
        var scrolled = Color.Unspecified
        var grouped = Color.Unspecified
        composeRule.setContent {
            WhiteNoiseTheme {
                val colors = MaterialTheme.colorScheme
                SideEffect {
                    resting = colors.surfaceContainerLow
                    scrolled = colors.surfaceContainer
                    grouped = colors.surfaceContainerLowest
                }
                GroupSetupScreen(
                    profile = ProfileFixtures.marmota,
                    selectedPersonIds = listOf("maya-chen", "elias-moreno", "mina-park"),
                    onBack = {},
                    onCreate = { _, _, _ -> true },
                )
            }
        }

        fun sampledCornerColor(tag: String): Color {
            val pixels = composeRule.onNodeWithTag(tag).captureToImage().toPixelMap()
            return pixels[2, 2]
        }
        fun sampledCenterColor(tag: String): Color {
            val pixels = composeRule.onNodeWithTag(tag).captureToImage().toPixelMap()
            return pixels[pixels.width / 2, pixels.height / 2]
        }
        fun assertColor(expected: Color, actual: Color) {
            check(kotlin.math.abs(expected.red - actual.red) < .01f)
            check(kotlin.math.abs(expected.green - actual.green) < .01f)
            check(kotlin.math.abs(expected.blue - actual.blue) < .01f)
        }

        assertColor(resting, sampledCornerColor("creation.bottomAction"))
        composeRule.onNodeWithText("Members").performScrollTo()
        composeRule.onNodeWithTag("creation.person.maya-chen").performScrollTo()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("creation.person.maya-chen.divider")
            .assertHeightIsEqualTo(2.dp)
        val content = composeRule.onNodeWithTag("group_setup.content")
            .fetchSemanticsNode().boundsInRoot
        val row = composeRule.onNodeWithTag("creation.person.maya-chen")
            .fetchSemanticsNode().boundsInRoot
        val avatar = composeRule.onNodeWithTag("creation.person.maya-chen.avatar", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val name = composeRule.onNodeWithTag("creation.person.maya-chen.name", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val outerInset = with(composeRule.density) { 16.dp.toPx() }
        val avatarTextGap = with(composeRule.density) { 12.dp.toPx() }
        check(kotlin.math.abs((row.left - content.left) - outerInset) < 1f)
        check(kotlin.math.abs((avatar.left - row.left) - outerInset) < 1f)
        check(kotlin.math.abs((name.left - avatar.right) - avatarTextGap) < 1f)
        assertColor(grouped, sampledCenterColor("creation.person.maya-chen"))
        assertColor(resting, sampledCenterColor("creation.person.maya-chen.divider"))
        composeRule.onNodeWithTag("group_setup.content").performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        assertColor(scrolled, sampledCornerColor("creation.bottomAction"))
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

    @Test
    fun personProfileReusesIdentityAndOpensGroupsInCommon() {
        val profile = ProfileFixtures.marmota
        val person = profile.people.first { it.id == "maya-chen" }
        var groupsOpened = false
        composeRule.setContent {
            WhiteNoiseTheme {
                PersonProfileScreen(
                    profile = profile,
                    person = person,
                    onBack = {},
                    onMessage = { true },
                    onToggleFollow = {},
                    onToggleBlock = {},
                    onGroupsInCommon = { groupsOpened = true },
                )
            }
        }

        composeRule.onNodeWithText("User Profile").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Copy public key").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Copied").assertIsDisplayed()
        composeRule.onNodeWithText("Groups in Common").performScrollTo().performClick()
        composeRule.runOnIdle { check(groupsOpened) }
        composeRule.onNodeWithText("Remove Contact").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun personProfileBalancesIdentitySpacingAndUsesTonalAboutSurface() {
        val profile = ProfileFixtures.marmota
        val person = profile.people.first { it.id == "maya-chen" }
        var expectedAboutColor = Color.Unspecified
        composeRule.setContent {
            WhiteNoiseTheme {
                val aboutColor = MaterialTheme.colorScheme.surfaceContainerHigh
                SideEffect {
                    expectedAboutColor = aboutColor
                }
                PersonProfileScreen(
                    profile = profile,
                    person = person,
                    onBack = {},
                    onMessage = { true },
                    onToggleFollow = {},
                    onToggleBlock = {},
                )
            }
        }

        val avatar = composeRule.onNodeWithTag("person_profile.avatar", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val name = composeRule.onNodeWithTag("person_profile.name", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val aboutNode = composeRule.onNodeWithTag("person_profile.about", useUnmergedTree = true)
        val about = aboutNode.fetchSemanticsNode().boundsInRoot
        val content = composeRule.onNodeWithTag("person_profile.content").fetchSemanticsNode().boundsInRoot
        val expectedGap = with(composeRule.density) { WhiteNoiseSpacing.FormField.toPx() }
        val minAvatar = with(composeRule.density) { 104.dp.toPx() }
        val maxAvatar = with(composeRule.density) { 152.dp.toPx() }
        val expectedAvatar = (content.width * .32f).coerceIn(minAvatar, maxAvatar)

        check(kotlin.math.abs(avatar.width - expectedAvatar) < 1.5f)
        check(kotlin.math.abs(name.top - avatar.bottom - expectedGap) < 1.5f)
        check(kotlin.math.abs(about.top - name.bottom - expectedGap) < 1.5f)

        val pixels = aboutNode.captureToImage().toPixelMap()
        val sampledAboutColor = pixels[pixels.width / 2, 2]
        check(kotlin.math.abs(sampledAboutColor.red - expectedAboutColor.red) < .01f)
        check(kotlin.math.abs(sampledAboutColor.green - expectedAboutColor.green) < .01f)
        check(kotlin.math.abs(sampledAboutColor.blue - expectedAboutColor.blue) < .01f)
    }

    @Test
    fun personWithoutSharedGroupsCanBeAddedThroughTheNativeSheetFlow() {
        val profile = ProfileFixtures.marmota
        val person = profile.people.first { it.id == "tim" }
        var addedTo: String? = null
        composeRule.setContent {
            WhiteNoiseTheme {
                PersonProfileScreen(
                    profile = profile,
                    person = person,
                    onBack = {},
                    onMessage = { true },
                    onToggleFollow = {},
                    onToggleBlock = {},
                    onAddToGroup = {
                        addedTo = it
                        true
                    },
                )
            }
        }

        composeRule.onNodeWithText("Add to groups").performScrollTo().performClick()
        composeRule.onNodeWithTag("contact.group.catalog-group-sole-admin").performScrollTo().performClick()
        composeRule.onNodeWithTag("contact.groups.apply").performClick()
        composeRule.onNodeWithText("Add Tim to this group? They may receive group messages after joining.").assertIsDisplayed()
        composeRule.onNodeWithText("Confirm").performClick()
        composeRule.runOnIdle { check(addedTo == "catalog-group-sole-admin") }
    }

    @Test
    fun groupsInCommonUsesNavigableGroupRowsAndAddAnotherAction() {
        val profile = ProfileFixtures.marmota
        val person = profile.people.first { it.id == "maya-chen" }
        var openedGroup: String? = null
        composeRule.setContent {
            WhiteNoiseTheme {
                GroupsInCommonScreen(
                    profile = profile,
                    person = person,
                    onBack = {},
                    onOpenGroup = { openedGroup = it },
                    onAddToGroup = { false },
                )
            }
        }

        composeRule.onNodeWithText("Weekend Walks").performClick()
        composeRule.runOnIdle { check(openedGroup == "weekend-walks") }
        composeRule.onNodeWithText("Add to Another Group").performScrollTo().assertIsDisplayed()
    }
}

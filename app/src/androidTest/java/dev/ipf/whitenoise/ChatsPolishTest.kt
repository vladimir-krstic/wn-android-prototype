package dev.ipf.whitenoise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.model.ChatScope
import dev.ipf.whitenoise.model.ChatProjection
import dev.ipf.whitenoise.model.ChatDeliveryState
import dev.ipf.whitenoise.model.ChatMembership
import dev.ipf.whitenoise.model.GroupMember
import dev.ipf.whitenoise.model.GroupRole
import dev.ipf.whitenoise.model.MuteDuration
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.chats.ChatsScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatsPolishTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }

    @Composable private fun Chats(vm: AppViewModel, onNew: () -> Unit = {}, onRecovery: () -> Unit = {}) {
        ChatsScreen(
            vm.uiState, onNew, {}, vm::markChatUnread, vm::toggleChatPin, vm::setChatMute,
            vm::setChatArchived, vm::leaveChat, { vm.deleteEndedChat(it) },
            onProfileRelays = onRecovery, onUndo = vm::undoChatListAction,
        )
    }

    @Test fun toolbarHasNoDefaultTitleAndFabExposesIconOnlyCreation() {
        val vm = model()
        var created = false
        rule.setContent { WhiteNoiseTheme { Chats(vm, onNew = { created = true }) } }
        rule.onNodeWithText("Chats").assertDoesNotExist()
        rule.onNodeWithText("New Message").assertDoesNotExist()
        rule.onNodeWithTag("chats.newMessage").assertIsDisplayed()
        rule.onNodeWithContentDescription("New Message").performClick()
        rule.runOnIdle { assertTrue(created) }
        rule.onNodeWithContentDescription("Filter Chats").performClick()
        rule.onNodeWithText("Unread").performClick()
        rule.onNodeWithContentDescription("Filter Chats").assertIsSelected()
        rule.onNodeWithText("Unread").assertIsDisplayed()
        rule.onNodeWithContentDescription("New Message").assertIsDisplayed()
    }

    @Test fun menuMarksUnreadAndUndoRestoresTheReadState() {
        val vm = model()
        val id = "catalog-direct-text"
        rule.setContent { WhiteNoiseTheme { Chats(vm) } }
        rule.onNodeWithTag("chat.row.$id").performTouchInput { longClick() }
        rule.onNodeWithText("Mark as Unread").performClick()
        rule.onNodeWithTag("chat.menu.$id").assertDoesNotExist()
        rule.waitUntil { vm.chat(id)!!.isMarkedUnread }
        rule.onNodeWithText("Undo").performClick()
        rule.runOnIdle { assertFalse(vm.chat(id)!!.isMarkedUnread) }
    }

    @Test fun longPressHighlightsRowAndBackDismissesWithoutNavigation() {
        val vm = model()
        val id = "catalog-direct-text"
        val before = vm.chat(id)
        rule.setContent { WhiteNoiseTheme { Chats(vm) } }
        rule.onNodeWithTag("chat.row.$id").performTouchInput { longClick() }
        rule.onNodeWithTag("chat.row.$id").assertIsSelected()
        rule.onNodeWithText("Archive").assertIsDisplayed()
        rule.runOnIdle { assertEquals(before, vm.chat(id)) }
        rule.onNodeWithTag("chat.menu.$id").performKeyInput { pressKey(Key.Back) }
        rule.onNodeWithText("Archive").assertDoesNotExist()
        rule.onNodeWithTag("chat.row.$id").assertIsNotSelected()
        rule.onNodeWithContentDescription("New Message").assertIsDisplayed()
    }

    @Test fun onlyOneMenuSurvivesAndScopeChangeClosesIt() {
        val vm = model()
        rule.setContent { WhiteNoiseTheme { Chats(vm) } }
        rule.onNodeWithTag("chat.row.catalog-direct-text").performSemanticsAction(SemanticsActions.OnLongClick)
        rule.onNodeWithTag("chat.row.catalog-direct-dates").performSemanticsAction(SemanticsActions.OnLongClick)
        rule.onNodeWithTag("chat.menu.catalog-direct-text").assertDoesNotExist()
        rule.onAllNodesWithText("Archive").assertCountEquals(1)
        rule.onNodeWithContentDescription("Filter Chats").performClick()
        rule.onNodeWithText("Left").performClick()
        rule.onNodeWithText("Archive").assertDoesNotExist()
    }

    @Test fun horizontalSwipesDoNothingAndDestructiveMenuCommandsStillRequireConfirmation() {
        val original = dev.ipf.whitenoise.model.ProfileFixtures.marmota
        val group = original.chats.first { it.isGroup && !it.isArchived && !it.hasEndedMembership }
            .copy(id = "active-group", membership = ChatMembership.Active, isPinned = false,
                members = listOf(GroupMember(original.id, GroupRole.Member)))
        val ended = group.copy(id = "ended-group", originalOrder = group.originalOrder + 1, membership = dev.ipf.whitenoise.model.ChatMembership.Left)
        val profile = original.copy(chats = listOf(group, ended))
        var destructiveActions = 0
        rule.setContent {
            WhiteNoiseTheme {
                ChatsScreen(
                    dev.ipf.whitenoise.state.AppUiState(listOf(profile), profile.id, setOf(profile.id)),
                    {}, {}, { _, _ -> }, {}, { _, _ -> }, { _, _ -> },
                    { destructiveActions++; true }, { destructiveActions++ },
                )
            }
        }
        rule.onNodeWithTag("chat.row.active-group").performTouchInput { swipeLeft(durationMillis = 500) }
        rule.onNodeWithTag("chat.row.active-group").performTouchInput { swipeRight(durationMillis = 500) }
        rule.onNodeWithText("Leave Group").assertDoesNotExist()
        rule.onNodeWithTag("chat.row.active-group").performTouchInput { longClick() }
        rule.onNodeWithText("Leave Group").performClick()
        rule.onNodeWithText("Leave this group?").assertIsDisplayed()
        rule.runOnIdle { assertEquals(0, destructiveActions) }
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithTag("chat.row.ended-group").performTouchInput { longClick() }
        rule.onNodeWithText("Delete Chat").performClick()
        rule.onNodeWithText("Delete this chat?").assertIsDisplayed()
        rule.runOnIdle { assertEquals(0, destructiveActions) }
        rule.onNodeWithText("Delete").performClick()
        rule.runOnIdle { assertEquals(1, destructiveActions) }
    }

    @Test fun longPressAndTalkBackExposeSameActionsAndArchiveUndoRestoresPin() {
        val vm = model()
        val id = "catalog-direct-text"
        rule.setContent { WhiteNoiseTheme { Chats(vm) } }
        rule.onNodeWithTag("chat.row.$id").performTouchInput { longClick() }
        rule.onNodeWithText("Unpin").assertIsDisplayed()
        rule.onNodeWithText("Archive").performClick()
        rule.runOnIdle { assertTrue(vm.chat(id)!!.isArchived); assertFalse(vm.chat(id)!!.isPinned) }
        rule.onNodeWithText("Undo").performClick()
        rule.runOnIdle { assertFalse(vm.chat(id)!!.isArchived); assertTrue(vm.chat(id)!!.isPinned) }
        val actions = rule.onNodeWithTag("chat.row.$id").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        rule.runOnIdle {
            assertTrue(actions.any { it.label == "Unpin" })
            actions.first { it.label == "Mark as Unread" }.action()
        }
        rule.runOnIdle { assertTrue(vm.chat(id)!!.isMarkedUnread) }
    }

    @Test fun rtlMenusRetainActionOrderAndLargeDarkRowsGrow() {
        val vm = model()
        val large = mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(1f, if (large.value) 2f else 1f),
            ) {
                WhiteNoiseTheme(AppearancePreference.Dark) {
                    Box(Modifier.requiredSize(360.dp, 640.dp).consumeWindowInsets(WindowInsets.safeDrawing)) { Chats(vm) }
                }
            }
        }
        val id = "catalog-direct-text"
        val normalHeight = rule.onNodeWithTag("chat.row.$id").fetchSemanticsNode().boundsInRoot.height
        rule.runOnIdle { large.value = true }
        val largeHeight = rule.onNodeWithTag("chat.row.$id").fetchSemanticsNode().boundsInRoot.height
        assertTrue(largeHeight > normalHeight)
        rule.onNodeWithTag("chat.row.$id").performTouchInput { longClick() }
        val unread = rule.onNodeWithText("Mark as Unread").fetchSemanticsNode().boundsInRoot
        val pin = rule.onNodeWithText("Unpin").fetchSemanticsNode().boundsInRoot
        assertTrue(unread.top < pin.top)
        rule.onNodeWithText("Mark as Unread").performClick()
        rule.waitUntil { vm.chat(id)!!.isMarkedUnread }
    }

    @Test fun avatarsAlignWithinCompactAndExpandedPanesAndListReachesBottom() {
        val vm = model()
        val width = mutableStateOf(360.dp)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                WhiteNoiseTheme(AppearancePreference.Light) {
                    Box(Modifier.requiredSize(width.value, 640.dp).consumeWindowInsets(WindowInsets.safeDrawing).testTag("viewport")) { Chats(vm) }
                }
            }
        }
        for (size in listOf(360.dp, 900.dp)) {
            rule.runOnIdle { width.value = size }
            val toolbarAvatar = rule.onNodeWithTag("chats.profileAvatar", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val rowAvatar = rule.onNodeWithTag("chat.avatar.catalog-direct-text", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val list = rule.onNodeWithTag("chats.list").fetchSemanticsNode().boundsInRoot
            assertEquals(toolbarAvatar.left, rowAvatar.left, 1f)
            assertEquals(list.left + 16f, rowAvatar.left, 1f)
            assertEquals(40f, toolbarAvatar.width, 1f)
            assertEquals(52f, rowAvatar.width, 1f)
            assertTrue(list.width <= 680f)
            assertEquals(rule.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot.bottom, list.bottom, 1f)
            val fab = rule.onNodeWithTag("chats.newMessage").fetchSemanticsNode().boundsInRoot
            assertEquals(list.right - 16f, fab.right, 1f)
            assertEquals(list.bottom - 16f, fab.bottom, 1f)
            assertEquals(56f, fab.width, 1f)
            assertEquals(56f, fab.height, 1f)
        }
        val last = ChatProjection.rows(vm.uiState.activeProfile!!.chats, ChatScope.Chats).last()
        rule.onNodeWithTag("chats.list").performScrollToIndex(ChatProjection.rows(vm.uiState.activeProfile!!.chats, ChatScope.Chats).lastIndex)
        rule.onNodeWithTag("chat.row.${last.id}").assertExists()
        val lastRow = rule.onNodeWithTag("chat.row.${last.id}").fetchSemanticsNode().boundsInRoot
        val fab = rule.onNodeWithTag("chats.newMessage").fetchSemanticsNode().boundsInRoot
        assertTrue(lastRow.bottom <= fab.top)
    }

    @Test fun searchHidesFabAndRecoveryUsesItsSameSlot() {
        val profile = ProfileFixtures.marmota.copy(chatRelayUrls = emptyList())
        var recovery = false
        rule.setContent { WhiteNoiseTheme {
            ChatsScreen(AppUiState(listOf(profile), profile.id, setOf(profile.id)), {}, {}, { _, _ -> }, {},
                { _, _ -> }, { _, _ -> }, { true }, {}, onProfileRelays = { recovery = true })
        } }
        rule.onNodeWithContentDescription("Check Relays").performClick()
        rule.runOnIdle { assertTrue(recovery) }
        rule.onNodeWithContentDescription("Search Chats").performClick()
        rule.onNodeWithTag("chats.newMessage").assertDoesNotExist()
        rule.onNodeWithContentDescription("Close search").performClick()
        rule.onNodeWithTag("chats.newMessage").assertIsDisplayed()
    }

    @Test fun removedAnchorAndProfileSwitchDismissMenus() {
        val vm = model()
        val id = "catalog-direct-text"
        rule.setContent { WhiteNoiseTheme { Chats(vm) } }
        rule.onNodeWithTag("chat.row.$id").performTouchInput { longClick() }
        rule.runOnIdle { vm.setChatArchived(id, true) }
        rule.onNodeWithTag("chat.menu.$id").assertDoesNotExist()
        rule.onNodeWithTag("chat.row.catalog-direct-dates").performTouchInput { longClick() }
        rule.runOnIdle { vm.completeSignIn(OnboardingOrigin.AddProfile) }
        rule.onNodeWithTag("chat.menu.catalog-direct-dates").assertDoesNotExist()
    }

    @Test fun muteUsesCompactDialogAndCancelDoesNotMutate() {
        val vm = model()
        val id = "catalog-direct-text"
        rule.setContent { WhiteNoiseTheme { Chats(vm) } }
        rule.onNodeWithTag("chat.row.$id").performTouchInput { longClick() }
        rule.onNodeWithText("Mute").performClick()
        rule.onNodeWithTag("mute.duration.dialog").assertIsDisplayed()
        rule.onNodeWithTag("chat.menu.$id").assertDoesNotExist()
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertNull(vm.chat(id)!!.muteDuration) }
        rule.onNodeWithTag("chat.row.$id").performTouchInput { longClick() }
        rule.onNodeWithText("Mute").performClick()
        rule.onNodeWithText("8 Hours").performClick()
        rule.runOnIdle { assertEquals(MuteDuration.EightHours, vm.chat(id)!!.muteDuration) }
    }

    @Test fun soleAdminMenuCannotBypassProtection() {
        val original = ProfileFixtures.marmota
        val group = original.chats.first { it.isGroup }.copy(id = "sole-admin", isArchived = false,
            membership = ChatMembership.Active, members = listOf(GroupMember(original.id, GroupRole.Admin)))
        val profile = original.copy(chats = listOf(group))
        var left = false
        rule.setContent { WhiteNoiseTheme {
            ChatsScreen(AppUiState(listOf(profile), profile.id, setOf(profile.id)), {}, {}, { _, _ -> }, {},
                { _, _ -> }, { _, _ -> }, { left = true; true }, {})
        } }
        rule.onNodeWithTag("chat.row.sole-admin").performTouchInput { longClick() }
        rule.onNodeWithText("Leave Group").performClick()
        rule.onNodeWithText("Promote Another Admin").assertIsDisplayed()
        rule.runOnIdle { assertFalse(left) }
    }

    @Test fun singleDigitManualInvitationAndErrorShareOneFootprintAtBothFontScales() {
        val original = ProfileFixtures.marmota
        val base = original.chats.first().copy(isPinned = false, unreadCount = 0, isMarkedUnread = false, deliveryState = ChatDeliveryState.None)
        val profile = original.copy(chats = listOf(
            base.copy(id = "count", unreadCount = 3),
            base.copy(id = "manual", isMarkedUnread = true),
            base.copy(id = "error", deliveryState = ChatDeliveryState.Failed, isMarkedUnread = true),
            base.copy(id = "invitation", membership = ChatMembership.Invited),
            base.copy(id = "many", unreadCount = 100),
        ))
        val font = mutableStateOf(1f)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, font.value)) {
                WhiteNoiseTheme { Box(Modifier.requiredSize(400.dp, 900.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                    ChatsScreen(AppUiState(listOf(profile), profile.id, setOf(profile.id)), {}, {}, { _, _ -> }, {},
                        { _, _ -> }, { _, _ -> }, { true }, {})
                } }
            }
        }
        var normalDiameter = 0f
        for (scale in listOf(1f, 2f)) {
            rule.runOnIdle { font.value = scale }
            val count = rule.onNodeWithTag("chat.status.count", true).fetchSemanticsNode().boundsInRoot
            for (id in listOf("manual", "invitation", "error")) {
                val status = rule.onNodeWithTag("chat.status.$id", true).fetchSemanticsNode().boundsInRoot
                assertEquals(count.size, status.size)
                assertEquals(count.right, status.right, 1f)
            }
            if (scale == 1f) normalDiameter = count.height else assertTrue(count.height > normalDiameter)
            val many = rule.onNodeWithTag("chat.status.many", true).fetchSemanticsNode().boundsInRoot
            assertTrue(many.width > count.width)
            rule.onNodeWithText("99+", useUnmergedTree = true).assertExists()
            rule.onAllNodesWithContentDescription("Marked unread", useUnmergedTree = true).assertCountEquals(1)
            rule.onAllNodesWithContentDescription("Invitation pending", useUnmergedTree = true).assertCountEquals(1)
        }
    }

    @Test fun nativeMenuFitsNearTopAndBottomAndKeepsTheSameOrder() {
        val vm = model()
        rule.setContent { WhiteNoiseTheme { Chats(vm) } }
        val rows = ChatProjection.rows(vm.uiState.activeProfile!!.chats, ChatScope.Chats)
        for (index in listOf(0, rows.lastIndex)) {
            rule.onNodeWithTag("chats.list").performScrollToIndex(index)
            val id = rows[index].id
            rule.onNodeWithTag("chat.row.$id").performTouchInput { longClick() }
            val menu = rule.onNodeWithTag("chat.menu.$id").fetchSemanticsNode()
            val location = IntArray(2)
            rule.runOnIdle { rule.activity.window.decorView.getLocationOnScreen(location) }
            assertTrue(menu.positionOnScreen.y >= location[1])
            assertTrue(menu.positionOnScreen.y + menu.size.height <= location[1] + rule.activity.window.decorView.height)
            val labels = dev.ipf.whitenoise.model.ChatListActionPolicy.all(rows[index])
            val positions = labels.map { action -> rule.onNodeWithTag("chat.action.${action.name}").fetchSemanticsNode().positionOnScreen.y }
            assertEquals(positions.sorted(), positions)
            rule.onNodeWithTag("chat.menu.$id").performKeyInput { pressKey(Key.Back) }
        }
    }

    @Test fun outsideTapDismissesPopupAndRemovesHighlight() {
        val vm = model()
        val id = "catalog-direct-text"
        rule.setContent { WhiteNoiseTheme { Chats(vm) } }
        rule.onNodeWithTag("chat.row.$id").performTouchInput { longClick() }
        // Deliver an outside touch to the native popup window, not to the Activity behind it.
        rule.onNodeWithTag("chat.menu.$id").performTouchInput { click(Offset(-20f, -20f)) }
        rule.onNodeWithTag("chat.menu.$id").assertDoesNotExist()
        rule.onNodeWithTag("chat.row.$id").assertIsNotSelected()
    }
}

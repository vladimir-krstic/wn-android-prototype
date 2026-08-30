package dev.ipf.whitenoise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.layout.SubcomposeSlotReusePolicy
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.model.AttachmentPreview
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatDeliveryState
import dev.ipf.whitenoise.model.ChatKind
import dev.ipf.whitenoise.model.ChatMembership
import dev.ipf.whitenoise.model.ChatProjection
import dev.ipf.whitenoise.model.ChatScope
import dev.ipf.whitenoise.model.DisappearingDuration
import dev.ipf.whitenoise.model.MuteDuration
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.chats.ChatContextMenuRow
import dev.ipf.whitenoise.ui.chats.ChatsScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the reuse/premeasure path from the Pixel's RectList crash, not just static rows. */
@RunWith(AndroidJUnit4::class)
class ChatListScrollRegressionTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val plain = Chat("plain", 0, ChatKind.Group, "Chat", preview = "One line", timestamp = "Now")

    @Test fun repeatedFlingAndJumpScrollingKeepsChatsAndMenusUsable() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
        val before = vm.uiState.activeProfile!!.chats
        val projected = ChatProjection.rows(before, ChatScope.Chats)
        val width = mutableStateOf(360.dp)
        val largeRtl = mutableStateOf(false)
        var opened = 0
        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, if (largeRtl.value) 2f else 1f),
                LocalLayoutDirection provides if (largeRtl.value) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                WhiteNoiseTheme(if (largeRtl.value) AppearancePreference.Dark else AppearancePreference.Light) {
                    Box(Modifier.requiredSize(width.value, 640.dp).consumeWindowInsets(WindowInsets.safeDrawing)) {
                        ChatsScreen(
                            vm.uiState, {}, { opened++ }, vm::markChatUnread, vm::toggleChatPin,
                            vm::setChatMute, vm::setChatArchived, vm::leaveChat, { vm.deleteEndedChat(it) },
                            onUndo = vm::undoChatListAction,
                        )
                    }
                }
            }
        }
        for (paneWidth in listOf(360.dp, 900.dp)) for (rtl in listOf(false, true)) {
            rule.runOnIdle { width.value = paneWidth; largeRtl.value = rtl }
            val list = rule.onNodeWithTag("chats.list")
            // Flings keep native prefetch enabled; jumps recycle many differently sized rows.
            list.performScrollToIndex(0)
            repeat(4) { list.performTouchInput { swipeUp(durationMillis = 120) } }
            repeat(4) { list.performTouchInput { swipeDown(durationMillis = 120) } }
            for (index in listOf(12, 28, projected.lastIndex, 42, 6, 0)) {
                list.performScrollToIndex(index)
                rule.onNodeWithTag("chat.row.${projected[index].id}").assertIsDisplayed()
            }
            val first = rule.onNodeWithTag("chat.row.${projected.first().id}")
            first.performTouchInput { longClick() }
            first.assertIsSelected()
            rule.onNodeWithTag("chat.menu.${projected.first().id}").performKeyInput { pressKey(Key.Back) }
            first.assertIsNotSelected().performClick()
            rule.onNodeWithContentDescription("New Message").assertIsDisplayed()
            rule.runOnIdle { assertEquals(before, vm.uiState.activeProfile!!.chats) }
        }
        rule.runOnIdle { assertEquals(4, opened) }
    }

    @Test fun recycledRowsCanBePremeasuredThenPlacedOrCanceledWithoutAnEarlyPlacementCrash() {
        val samples = listOf(
            plain,
            plain.copy(preview = "First line\nSecond line", unreadCount = 3),
            plain.copy(attachmentPreview = AttachmentPreview.File("Brief.pdf"), previewAuthor = "You", isMarkedUnread = true),
            plain.copy(muteDuration = MuteDuration.Always, disappearingDuration = DisappearingDuration.OneWeek),
            plain.copy(membership = ChatMembership.Invited),
            plain.copy(deliveryState = ChatDeliveryState.Failed, unreadCount = 3),
            plain.copy(membership = ChatMembership.Left),
        )
        val layoutState = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        val visible = mutableStateOf(plain)
        val width = mutableStateOf(320.dp)
        val largeRtl = mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, if (largeRtl.value) 2f else 1f),
                LocalLayoutDirection provides if (largeRtl.value) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                WhiteNoiseTheme(if (largeRtl.value) AppearancePreference.Dark else AppearancePreference.Light) {
                    SubcomposeLayout(layoutState, Modifier.requiredSize(width.value, 320.dp)) { constraints ->
                        val chat = visible.value
                        val row = subcompose(chat.id) { ChatContextMenuRow(chat, false, {}, {}, {}, {}) }
                            .single().measure(constraints.copy(minHeight = 0))
                        layout(constraints.maxWidth, constraints.maxHeight) { row.placeRelative(0, 0) }
                    }
                }
            }
        }
        for (paneWidth in listOf(320.dp, 680.dp)) for (rtl in listOf(false, true)) {
            rule.runOnIdle { width.value = paneWidth; largeRtl.value = rtl }
            for ((index, sample) in samples.withIndex()) {
                val chat = sample.copy(id = "reused-${paneWidth.value}-$rtl-$index")
                val handle = rule.runOnIdle {
                    layoutState.precompose(chat.id) { ChatContextMenuRow(chat, false, {}, {}, {}, {}) }.also {
                        assertEquals(1, it.placeablesCount)
                        it.premeasure(0, Constraints(maxWidth = paneWidth.value.toInt()))
                        assertTrue(it.getSize(0).height >= 72)
                        visible.value = chat
                    }
                }
                // Promotion uses the premeasured slot; the prior visible slot is reused next time.
                rule.onNodeWithTag("chat.title.${chat.id}", true).assertTextEquals(chat.title)
                rule.onNodeWithTag("chat.row.${chat.id}")
                    .assert(hasClickAction()).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
                rule.runOnIdle {
                    handle.dispose() // Already promoted, so this must not remove the active row.
                    val canceled = chat.copy(id = "canceled-${chat.id}")
                    val canceledHandle = layoutState.precompose(canceled.id) {
                        ChatContextMenuRow(canceled, false, {}, {}, {}, {})
                    }
                    try {
                        canceledHandle.premeasure(0, Constraints(maxWidth = paneWidth.value.toInt()))
                    } finally {
                        canceledHandle.dispose()
                    }
                }
                rule.onNodeWithTag("chat.row.${chat.id}").assertIsDisplayed()
            }
        }
    }

    @Test fun singleAndMultilineRowsKeepNativeMinimumHeightsAndPreviewStatusAlignment() {
        val single = plain.copy(id = "single", unreadCount = 3)
        val multi = plain.copy(id = "multi", preview = "First line\nSecond line", deliveryState = ChatDeliveryState.Failed)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                WhiteNoiseTheme {
                    Column(Modifier.requiredSize(320.dp, 400.dp)) {
                        listOf(single, multi).forEach { ChatContextMenuRow(it, false, {}, {}, {}, {}) }
                    }
                }
            }
        }
        for ((chat, height) in listOf(single to 72f, multi to 88f)) {
            val row = rule.onNodeWithTag("chat.row.${chat.id}").fetchSemanticsNode().boundsInRoot
            val preview = rule.onNodeWithTag("chat.preview.${chat.id}", true).fetchSemanticsNode().boundsInRoot
            val status = rule.onNodeWithTag("chat.status.${chat.id}", true).fetchSemanticsNode().boundsInRoot
            assertEquals(height, row.height, 1f)
            assertEquals(preview.top, status.top, 1f)
            assertEquals(row.right - 8f, status.right, 1f)
        }
    }
}

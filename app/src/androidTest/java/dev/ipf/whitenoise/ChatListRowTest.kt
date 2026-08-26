package dev.ipf.whitenoise

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatKind
import dev.ipf.whitenoise.model.ChatMembership
import dev.ipf.whitenoise.model.DisappearingDuration
import dev.ipf.whitenoise.model.MuteDuration
import dev.ipf.whitenoise.ui.chats.ChatContextMenuRow
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatListRowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val base = Chat("short", 0, ChatKind.Group, "Hi", preview = "A message", timestamp = "30m")

    @Test fun metadataStaysWithTheTitleAndReservesTheTimestampAcrossSizesAndDirections() {
        val withIcons = base.copy(muteDuration = MuteDuration.Always, disappearingDuration = DisappearingDuration.OneWeek)
        val rows = listOf(
            withIcons,
            withIcons.copy(id = "long", title = "Direct – Disappearing & Muted with a very long localized conversation name ".repeat(2)),
            base.copy(id = "left", title = "Left", membership = ChatMembership.Left),
            base.copy(id = "removed", title = "Removed", membership = ChatMembership.Removed),
        )
        val width = mutableStateOf(320.dp)
        val fontScale = mutableStateOf(1f)
        val direction = mutableStateOf(LayoutDirection.Ltr)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale.value), LocalLayoutDirection provides direction.value) {
                WhiteNoiseTheme(if (direction.value == LayoutDirection.Ltr) AppearancePreference.Light else AppearancePreference.Dark) {
                    Column(Modifier.requiredSize(width.value, 900.dp)) {
                        rows.forEach { ChatContextMenuRow(it, false, {}, {}, {}, {}) }
                    }
                }
            }
        }
        for (paneWidth in listOf(320.dp, 680.dp)) for (scale in listOf(1f, 2f)) for (layoutDirection in LayoutDirection.entries) {
            rule.runOnIdle { width.value = paneWidth; fontScale.value = scale; direction.value = layoutDirection }
            for (chat in rows) {
                val title = rule.onNodeWithTag("chat.title.${chat.id}", true).assertTextEquals(chat.title)
                val titleBounds = title.fetchSemanticsNode().boundsInRoot
                val titleLayout = title.textLayout()
                val timestamp = rule.onNodeWithTag("chat.timestamp.${chat.id}", true).assertTextEquals(chat.timestamp)
                val timestampBounds = timestamp.fetchSemanticsNode().boundsInRoot
                val rowBounds = rule.onNodeWithTag("chat.row.${chat.id}").fetchSemanticsNode().boundsInRoot
                val icons = if (chat.hasEndedMembership) listOf("membership") else listOf("muted", "timer")
                var previous = titleBounds
                for (icon in icons) {
                    val bounds = rule.onNodeWithTag("chat.$icon.${chat.id}", true).fetchSemanticsNode().boundsInRoot
                    val gap = if (layoutDirection == LayoutDirection.Ltr) bounds.left - previous.right else previous.left - bounds.right
                    assertEquals("$icon follows the title/previous icon", 4f, gap, 1f)
                    assertEquals(titleBounds.center.y, bounds.center.y, 1f)
                    assertEquals(16f, bounds.width, 1f)
                    previous = bounds
                }
                val timeGap = if (layoutDirection == LayoutDirection.Ltr) timestampBounds.left - previous.right else previous.left - timestampBounds.right
                assertTrue("Metadata must not crowd the timestamp", timeGap >= 7f)
                val endInset = if (layoutDirection == LayoutDirection.Ltr) rowBounds.right - timestampBounds.right else timestampBounds.left - rowBounds.left
                assertEquals(8f, endInset, 1f) // Plus the anchor's 8 dp outer inset.
                assertEquals(titleBounds.top + titleLayout.firstBaseline, timestampBounds.top + timestamp.textLayout().firstBaseline, 1f)
                assertEquals(1, titleLayout.lineCount)
                assertEquals(chat.id == "long", titleLayout.isLineEllipsized(0))
            }
        }
    }

    @Test fun invitationBadgeUsesPrimaryCircleAndOnPrimaryPlusInBothThemesAndFontScales() {
        val appearance = mutableStateOf(AppearancePreference.Light)
        val fontScale = mutableStateOf(1f)
        var container = Color.Unspecified
        var content = Color.Unspecified
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale.value)) {
                WhiteNoiseTheme(appearance.value) {
                    container = MaterialTheme.colorScheme.primary
                    content = MaterialTheme.colorScheme.onPrimary
                    Column(Modifier.requiredSize(320.dp, 400.dp)) {
                        ChatContextMenuRow(base.copy(id = "invitation", membership = ChatMembership.Invited), false, {}, {}, {}, {})
                    }
                }
            }
        }
        for (theme in listOf(AppearancePreference.Light, AppearancePreference.Dark)) for (scale in listOf(1f, 2f)) {
            rule.runOnIdle { appearance.value = theme; fontScale.value = scale }
            val badge = rule.onNodeWithTag("chat.status.invitation", true)
                .assertContentDescriptionEquals("Invitation pending").assert(hasClickAction().not())
            val pixels = badge.captureToImage().toPixelMap()
            assertEquals(pixels.width, pixels.height)
            // The native badge fills the circle; the unchanged Add symbol is centered within it.
            for ((expected, actual) in listOf(container to pixels[1, pixels.height / 2], content to pixels[pixels.width / 2, pixels.height / 2])) {
                assertEquals(expected.red, actual.red, .01f)
                assertEquals(expected.green, actual.green, .01f)
                assertEquals(expected.blue, actual.blue, .01f)
            }
        }
    }

    @Test fun highlightIsInsetAndRoundedWithoutMovingTheTighterAvatarAndTextLayout() {
        val width = mutableStateOf(320.dp)
        val largeRtl = mutableStateOf(false)
        val expanded = mutableStateOf(false)
        var surface = Color.Unspecified
        var highlight = Color.Unspecified
        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, if (largeRtl.value) 2f else 1f),
                LocalLayoutDirection provides if (largeRtl.value) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                WhiteNoiseTheme(if (largeRtl.value) AppearancePreference.Dark else AppearancePreference.Light) {
                    surface = MaterialTheme.colorScheme.surface
                    highlight = MaterialTheme.colorScheme.surfaceContainerHigh
                    Column(Modifier.requiredSize(width.value, 240.dp).background(surface).testTag("pane")) {
                        ChatContextMenuRow(base, expanded.value, {}, { expanded.value = true }, { expanded.value = false }, {})
                    }
                }
            }
        }
        for (paneWidth in listOf(320.dp, 680.dp)) for (rtl in listOf(false, true)) {
            rule.runOnIdle { width.value = paneWidth; largeRtl.value = rtl }
            val row = rule.onNodeWithTag("chat.row.${base.id}")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                .assertIsNotSelected()
            val parts = listOf("avatar", "title", "preview", "timestamp")
            val before = parts.associateWith { rule.onNodeWithTag("chat.$it.${base.id}", true).fetchSemanticsNode().boundsInRoot }
            val avatar = before.getValue("avatar")
            val title = before.getValue("title")
            val preview = before.getValue("preview")
            val pane = rule.onNodeWithTag("pane").fetchSemanticsNode().boundsInRoot
            assertEquals(16f, if (rtl) pane.right - avatar.right else avatar.left - pane.left, 1f)
            assertEquals(12f, if (rtl) avatar.left - title.right else title.left - avatar.right, 1f)
            assertEquals(if (rtl) title.right else title.left, if (rtl) preview.right else preview.left, 1f)
            row.performTouchInput { longClick() }
            row.assertIsSelected()
            val rowBounds = row.fetchSemanticsNode().boundsInRoot
            assertEquals(pane.left + 8f, rowBounds.left, 1f)
            assertEquals(pane.right - 8f, rowBounds.right, 1f)
            parts.forEach { assertEquals(before.getValue(it), rule.onNodeWithTag("chat.$it.${base.id}", true).fetchSemanticsNode().boundsInRoot) }
            val pixels = rule.onNodeWithTag("pane").captureToImage().toPixelMap()
            val top = (rowBounds.top - pane.top).toInt()
            val middleY = (rowBounds.center.y - pane.top).toInt()
            // Side gutters and rounded corners expose the screen; the top-center is highlighted.
            assertColor(surface, pixels[2, middleY])
            assertColor(surface, pixels[pixels.width - 3, middleY])
            assertColor(surface, pixels[9, top + 1])
            assertColor(surface, pixels[pixels.width - 10, top + 1])
            assertColor(highlight, pixels[pixels.width / 2, top + 1])
            rule.onNodeWithTag("chat.menu.${base.id}").performKeyInput { pressKey(Key.Back) }
            row.assertIsNotSelected()
        }
    }

    @Test fun contextMenuHasAnEightDpGapAboveAndBelowAcrossPaneWidthsAndLargeRtlText() {
        val width = mutableStateOf(320.dp)
        val bottom = mutableStateOf(false)
        val largeRtl = mutableStateOf(false)
        val expanded = mutableStateOf(false)
        val chat = base.copy(membership = ChatMembership.Left)
        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, if (largeRtl.value) 2f else 1f),
                LocalLayoutDirection provides if (largeRtl.value) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                WhiteNoiseTheme(if (largeRtl.value) AppearancePreference.Dark else AppearancePreference.Light) {
                    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
                        Box(Modifier.widthIn(max = width.value).fillMaxSize().padding(vertical = 24.dp)) {
                            Box(Modifier.align(if (bottom.value) Alignment.BottomStart else Alignment.TopStart)) {
                                ChatContextMenuRow(chat, expanded.value, {}, { expanded.value = true }, { expanded.value = false }, {})
                            }
                        }
                    }
                }
            }
        }
        for (paneWidth in listOf(320.dp, 680.dp)) for (rtl in listOf(false, true)) for (atBottom in listOf(false, true)) {
            rule.runOnIdle { width.value = paneWidth; largeRtl.value = rtl; bottom.value = atBottom }
            rule.onNodeWithTag("chat.row.${chat.id}").performTouchInput { longClick() }
            val row = rule.onNodeWithTag("chat.row.${chat.id}").fetchSemanticsNode()
            val menu = rule.onNodeWithTag("chat.menu.${chat.id}").fetchSemanticsNode()
            // Popup and Activity have different roots: compare screen coordinates, not root bounds.
            val gap = if (atBottom) row.positionOnScreen.y - menu.positionOnScreen.y - menu.size.height
                else menu.positionOnScreen.y - row.positionOnScreen.y - row.size.height
            assertEquals(8f, gap, 1f)
            val rowStart = row.positionOnScreen.x + if (rtl) row.size.width else 0
            val menuStart = menu.positionOnScreen.x + if (rtl) menu.size.width else 0
            assertEquals(rowStart, menuStart, 1f)
            rule.onNodeWithTag("chat.menu.${chat.id}").performKeyInput { pressKey(Key.Back) }
        }
    }

    private fun assertColor(expected: Color, actual: Color) {
        assertEquals(expected.red, actual.red, .01f)
        assertEquals(expected.green, actual.green, .01f)
        assertEquals(expected.blue, actual.blue, .01f)
    }

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single()
    }
}

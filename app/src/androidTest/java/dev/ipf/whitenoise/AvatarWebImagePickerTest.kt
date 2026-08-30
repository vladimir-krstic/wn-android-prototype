package dev.ipf.whitenoise

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.AvatarWebImageCatalog
import dev.ipf.whitenoise.model.AvatarWebImageChoice
import dev.ipf.whitenoise.ui.onboarding.AvatarWebImagePickerContent
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AvatarWebImagePickerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<EmptyTestActivity>()

    @Test
    fun toolbarAndModeButtonsKeepNativeTargetsAndCompactMargins() {
        showPicker()

        val content = composeRule.onNodeWithTag(ContentTag).fetchSemanticsNode().boundsInRoot
        val close = composeRule.onNodeWithContentDescription("Close")
            .assertTouchHeightIsEqualTo(48.dp).fetchSemanticsNode().boundsInRoot
        val done = composeRule.onNodeWithText("Done")
            .assertIsNotEnabled().assertTouchHeightIsEqualTo(48.dp).fetchSemanticsNode().boundsInRoot
        val search = composeRule.onNodeWithText("Search")
            .assertIsSelected().assertTouchHeightIsEqualTo(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .fetchSemanticsNode().boundsInRoot
        val url = composeRule.onNodeWithText("URL")
            .assertIsNotSelected().assertTouchHeightIsEqualTo(48.dp).fetchSemanticsNode().boundsInRoot
        val title = composeRule.onNodeWithText("Find Image on Web").fetchSemanticsNode().boundsInRoot

        assertEquals(16f, content.right - done.right, 1f)
        assertEquals(close.center.y, done.center.y, 1f)
        assertEquals(32f, done.center.y - content.top, 1f)
        assertTrue(title.left >= close.right && title.right <= done.left)
        assertEquals(16f, search.left - content.left, 1f)
        assertEquals(16f, content.right - url.right, 1f)
        assertEquals(8f, url.left - search.right, 1f)
        assertEquals(search.width, url.width, 1f)
    }

    @Test
    fun toolbarAndModeMarginsMirrorInRtl() {
        showPicker(layoutDirection = LayoutDirection.Rtl)

        val content = composeRule.onNodeWithTag(ContentTag).fetchSemanticsNode().boundsInRoot
        val done = composeRule.onNodeWithText("Done").fetchSemanticsNode().boundsInRoot
        val search = composeRule.onNodeWithText("Search").fetchSemanticsNode().boundsInRoot
        val url = composeRule.onNodeWithText("URL").fetchSemanticsNode().boundsInRoot

        assertEquals(16f, done.left - content.left, 1f)
        assertEquals(16f, content.right - search.right, 1f)
        assertEquals(16f, url.left - content.left, 1f)
        assertEquals(8f, search.left - url.right, 1f)
    }

    @Test
    fun resultsHaveSectionSpacingAndSelectionCommitsOnlyWithDone() {
        var committed: AvatarWebImageChoice? = null
        showPicker(onUseImage = { committed = it })
        val results = AvatarWebImageCatalog.results("one")
        composeRule.onNode(hasSetTextAction()).performTextReplacement("one")

        val field = composeRule.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot
        val firstImage = composeRule.onNodeWithContentDescription(results.first().accessibilityLabel)
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(24f, firstImage.top - field.bottom, 1f)

        composeRule.onNodeWithContentDescription(results[0].accessibilityLabel).performClick()
            .assertIsSelected()
        composeRule.onNodeWithContentDescription(results[1].accessibilityLabel).performClick()
            .assertIsSelected()
        composeRule.onNodeWithContentDescription(results[0].accessibilityLabel).assertIsNotSelected()
        composeRule.runOnIdle { assertNull(committed) }

        composeRule.onNodeWithText("Done").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(results[1], committed) }
    }

    @Test
    fun urlValidationAndPreviewKeepDoneTiedToTheActiveMode() {
        val choice = AvatarWebImageCatalog.choices.first()
        showPicker(currentChoiceId = choice.id)
        composeRule.onNodeWithText("URL").performClick().assertIsSelected()
        composeRule.onNode(hasSetTextAction())
            .assertTextContains(AvatarWebImageCatalog.displayUrl(choice))
            .performTextReplacement("not a web address")
        composeRule.onNodeWithText("Enter a valid web address.").assertIsDisplayed()
        composeRule.onNodeWithText("Done").assertIsNotEnabled()

        composeRule.onNode(hasSetTextAction())
            .performTextReplacement(AvatarWebImageCatalog.displayUrl(choice))
        composeRule.onNodeWithText("Done").assertIsEnabled()
        val field = composeRule.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot
        val heading = composeRule.onNodeWithText("Preview").fetchSemanticsNode().boundsInRoot
        val preview = composeRule.onNodeWithContentDescription(choice.accessibilityLabel)
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(24f, heading.top - field.bottom, 1f)
        assertEquals(8f, preview.top - heading.bottom, 1f)
    }

    @Test
    fun searchHasAnExplicitKeyboardActionAndLabeledClearControl() {
        showPicker()
        val field = composeRule.onNode(hasSetTextAction())
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.ImeAction, ImeAction.Search))
        field.performTextReplacement("one")
        field.performClick()
        field.performImeAction()
        field.assertIsNotFocused()
        composeRule.onNodeWithContentDescription("Clear search")
            .assertTouchHeightIsEqualTo(48.dp).performClick()

        composeRule.onNodeWithText("Enter a search to find an image.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clear search").assertDoesNotExist()
        composeRule.onNodeWithText("Done").assertIsNotEnabled()
    }

    @Test
    fun modeQueryUrlAndSelectionSurviveRestorationWithoutCommitting() {
        val restoration = StateRestorationTester(composeRule)
        var committed: AvatarWebImageChoice? = null
        restoration.setContent {
            PickerLayout(onUseImage = { committed = it })
        }
        val choice = AvatarWebImageCatalog.results("one").first()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("one")
        composeRule.onNodeWithContentDescription(choice.accessibilityLabel).performClick()
        composeRule.onNodeWithText("URL").performClick()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("unfinished address")

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("URL").assertIsSelected()
        composeRule.onNode(hasSetTextAction()).assertTextContains("unfinished address")
        composeRule.onNodeWithText("Done").assertIsNotEnabled()
        composeRule.onNodeWithText("Search").performClick().assertIsSelected()
        composeRule.onNode(hasSetTextAction()).assertTextContains("one")
        composeRule.onNodeWithContentDescription(choice.accessibilityLabel).assertIsSelected()
        composeRule.onNodeWithText("Done").assertIsEnabled()
        composeRule.runOnIdle { assertNull(committed) }
    }

    @Test
    fun shortLargeTextRtlLayoutKeepsBothModesScrollableAndActionsReachable() {
        val choice = AvatarWebImageCatalog.choices.first()
        showPicker(
            currentChoiceId = choice.id,
            height = 320,
            fontScale = 2f,
            layoutDirection = LayoutDirection.Rtl,
        )
        composeRule.onNodeWithText("URL").performClick()
        composeRule.onNode(hasSetTextAction()).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(choice.accessibilityLabel)
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Done").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithContentDescription("Close").assertIsDisplayed()

        composeRule.onNodeWithText("Search").performClick()
        composeRule.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("one")
        val result = AvatarWebImageCatalog.results("one").last()
        composeRule.onNode(hasScrollToIndexAction()).performScrollToKey(result.id)
        composeRule.onNodeWithContentDescription(result.accessibilityLabel)
            .assertIsDisplayed().performClick().assertIsSelected()
        composeRule.onNodeWithText("Done").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithContentDescription("Close").assertIsDisplayed()
    }

    @Test
    fun expandedLayoutKeepsThreeSquareColumnsAndTheSameSectionGap() {
        showPicker(width = 640, height = 800)
        composeRule.onNode(hasSetTextAction()).performTextReplacement("one")
        val results = AvatarWebImageCatalog.results("one")
        val first = composeRule.onNodeWithContentDescription(results[0].accessibilityLabel)
            .fetchSemanticsNode().boundsInRoot
        val second = composeRule.onNodeWithContentDescription(results[1].accessibilityLabel)
            .fetchSemanticsNode().boundsInRoot
        val third = composeRule.onNodeWithContentDescription(results[2].accessibilityLabel)
            .fetchSemanticsNode().boundsInRoot
        val field = composeRule.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot

        assertEquals(first.width, first.height, 1f)
        assertEquals(first.top, second.top, 1f)
        assertEquals(first.top, third.top, 1f)
        assertEquals(2f, second.left - first.right, 1f)
        assertEquals(2f, third.left - second.right, 1f)
        assertEquals(24f, first.top - field.bottom, 1f)
    }

    private fun showPicker(
        currentChoiceId: String? = null,
        width: Int = 360,
        height: Int = 640,
        fontScale: Float = 1f,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onUseImage: (AvatarWebImageChoice) -> Unit = {},
    ) {
        composeRule.setContent {
            PickerLayout(currentChoiceId, width, height, fontScale, layoutDirection, onUseImage)
        }
    }

    @Composable
    private fun PickerLayout(
        currentChoiceId: String? = null,
        width: Int = 360,
        height: Int = 640,
        fontScale: Float = 1f,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        onUseImage: (AvatarWebImageChoice) -> Unit = {},
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(density = 1f, fontScale = fontScale),
            LocalLayoutDirection provides layoutDirection,
        ) {
            WhiteNoiseTheme {
                AvatarWebImagePickerContent(
                    currentChoiceId = currentChoiceId,
                    onDismiss = {},
                    onUseImage = onUseImage,
                    modifier = Modifier.requiredSize(width.dp, height.dp).testTag(ContentTag),
                )
            }
        }
    }

    private companion object {
        const val ContentTag = "web-image-picker-content"
    }
}

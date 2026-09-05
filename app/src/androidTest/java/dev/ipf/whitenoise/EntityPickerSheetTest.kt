package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.ui.components.WhiteNoiseEntityPickerSheet
import dev.ipf.whitenoise.ui.components.WhiteNoisePickerItem
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntityPickerSheetTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    @Test fun filteredMultiSelectionSurvivesRecreationAndDoneRemainsReachable() {
        val restoration = StateRestorationTester(rule)
        var changes = 0
        var done = 0
        restoration.setContent {
            var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
            WhiteNoiseTheme {
                WhiteNoiseEntityPickerSheet(
                    title = "Included Chats",
                    items = (0..80).map { WhiteNoisePickerItem("$it", "Chat $it") },
                    selectedIds = selected.toSet(), multiple = true,
                    onSelect = { id -> changes++; selected = if (id in selected) selected - id else selected + id },
                    onDismiss = {}, onDone = { done++ },
                )
            }
        }
        rule.onNodeWithTag("entity.search").performTextInput("Chat 80")
        rule.onNodeWithTag("entity.choice.80").performClick().assertIsOn()
        rule.runOnIdle { assertEquals(1, changes) }
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("entity.search").assertTextContains("Chat 80")
        rule.onNodeWithTag("entity.choice.80").assertIsOn()
        rule.onNodeWithText("Done").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(1, done); assertEquals(1, changes) }
    }

    @Test fun previewHasNoSelectionActionAndCanSearchToEmptyState() {
        var closed = 0
        rule.setContent {
            WhiteNoiseTheme {
                WhiteNoiseEntityPickerSheet("Preview", listOf(WhiteNoisePickerItem("a", "Maya")),
                    onDismiss = { closed++ }, onDone = { closed++ })
            }
        }
        rule.onNodeWithTag("entity.choice.a").assertHasNoClickAction()
        rule.onNodeWithTag("entity.search").performTextInput("no match")
        rule.onNodeWithText(rule.activity.getString(R.string.no_results)).assertExists()
        rule.onNodeWithContentDescription("Close").performClick()
        rule.runOnIdle { assertEquals(1, closed) }
    }

    @Test fun singleSelectionRetainsDisabledEligibilityAndFiresOnce() {
        val selected = mutableListOf<String>()
        rule.setContent {
            WhiteNoiseTheme {
                WhiteNoiseEntityPickerSheet("Select admin", listOf(
                    WhiteNoisePickerItem("disabled", "Unavailable", enabled = false),
                    WhiteNoisePickerItem("member", "Maya"),
                ), onDismiss = {}, onSelect = selected::add)
            }
        }
        rule.onNodeWithTag("entity.choice.disabled").assertIsNotEnabled()
        rule.onNodeWithTag("entity.choice.member").performClick()
        rule.runOnIdle { assertEquals(listOf("member"), selected) }
    }
}

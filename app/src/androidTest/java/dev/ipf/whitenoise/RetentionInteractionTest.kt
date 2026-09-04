package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.*
import dev.ipf.whitenoise.ui.conversation.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RetentionInteractionTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private val profile get() = vm.uiState.activeProfile!!
    private var picked: DisappearingDuration? = null
    private var dismissed = false
    private fun show(content: @Composable () -> Unit) {
        // Explicit completions keep owned state assertions independent of frame timing.
        rule.setContent { WhiteNoiseTheme { CompositionLocalProvider(LocalRetention provides vm.retention) { content() } } }
    }
    private fun picker(editable: Boolean = true) = show { RetentionPicker(DisappearingDuration.Off, editable, { dismissed = true }, { picked = it }) }
    private fun group() = GroupOwner(profile.id, vm.createGroup("Trail", "", ProfileAvatar.Monogram, listOf("maya-chen"))!!)
    private fun step(owner: GroupOwner) = rule.runOnIdle { vm.retention.work.getValue(owner).let { vm.retention.advance(owner, it.id, it.phase) } }
    @Test fun presetsAreStagedAndCancelDoesNotSave() {
        picker(); rule.onNodeWithText("30 seconds").performScrollTo().performClick()
        rule.runOnIdle { assertNull(picked) }
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertTrue(dismissed); assertNull(picked) }
    }
    @Test fun saveAppliesTheSelectedPreset() {
        picker(); rule.onNodeWithText("8 hours").performScrollTo().performClick()
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(DisappearingDuration.EightHours, picked) }
    }
    @Test fun customUnitsValidateAndReturnToStagedSelection() {
        picker(); rule.onNodeWithText("Custom time").performScrollTo().performClick()
        rule.onNodeWithTag("retention.custom.value").performTextReplacement("0")
        rule.onNodeWithText("Set").assertIsNotEnabled()
        rule.onNodeWithTag("retention.custom.value").performTextReplacement("12")
        rule.onNodeWithText("Unit: Minutes").performClick()
        rule.onNodeWithText("Months").performScrollTo().performClick()
        rule.onNodeWithTag("retention.custom.value").performTextReplacement("13")
        rule.onNodeWithText("Set").assertIsNotEnabled()
        rule.onNodeWithTag("retention.custom.value").performTextReplacement("12")
        rule.onNodeWithText("Set").performClick()
        rule.runOnIdle { assertNull(picked) }
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { assertEquals(CustomRetentionInput("12", RetentionUnit.Months).duration, picked) }
    }
    @Test fun memberCanReadHelpButCannotChangeTheTimer() {
        picker(editable = false)
        rule.onNodeWithText("Only admins can change this").performScrollTo().assertExists()
        rule.onNodeWithText("30 seconds").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Save").assertDoesNotExist()
        rule.onNodeWithText("Done").performClick(); rule.runOnIdle { assertTrue(dismissed); assertNull(picked) }
    }
    @Test fun destructiveConfirmationNamesPruningAndCancelKeepsPolicy() {
        val owner = group(); vm.retention.begin(owner, DisappearingDuration.OneDay)
        show { RetentionConfirmation(profile, vm.chat(owner.chatId)!!); RetentionWorkPanel(profile, vm.chat(owner.chatId)!!) }
        rule.onNodeWithText("Messages older than 1 day will be permanently removed for everyone.").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertEquals(DisappearingDuration.Off, vm.chat(owner.chatId)!!.disappearingDuration); assertNull(vm.retention.work[owner]) }
    }
    @OptIn(ExperimentalTestApi::class)
    @Test fun chatInfoConfirmationDoesNotDependOnTheStatusRowBeingVisible() {
        val owner = group(); vm.retention.begin(owner, DisappearingDuration.OneDay)
        show { DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 320.dp))) {
            ChatInfoScreen(profile, vm.chat(owner.chatId)!!, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, { true }, {})
        } }
        rule.onNodeWithText("Set disappearing timer?").assertIsDisplayed()
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertNull(vm.retention.work[owner]) }
    }
    @Test fun acceptedTimerAndRefreshRetryAreDistinctFromFailedSave() {
        val owner = group(); vm.retention.choose(RetentionScenario.RefreshFailure); vm.retention.begin(owner, DisappearingDuration.OneDay)
        show { RetentionConfirmation(profile, vm.chat(owner.chatId)!!); RetentionWorkPanel(profile, vm.chat(owner.chatId)!!) }
        rule.onNodeWithText("Set timer").performClick(); step(owner); step(owner)
        rule.onNodeWithText("The timer was updated, but history couldn’t refresh. Retry to refresh it.").assertExists()
        rule.runOnIdle { assertEquals(DisappearingDuration.OneDay, vm.chat(owner.chatId)!!.disappearingDuration) }
        rule.onNodeWithText("Retry").performClick(); step(owner)
        rule.runOnIdle { assertEquals(RetentionPhase.Complete, vm.retention.work.getValue(owner).phase) }
    }
    @Test fun receivedIndicatorHasWaitingSemanticsThenRemainingDuration() {
        val owner = group(); vm.retention.chooseExample(RetentionExample.Waiting); vm.retention.open(owner)
        val id = vm.chat(owner.chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().id
        show { vm.message(owner.chatId, id)?.let { MessageExpiryIndicator(it) } }
        rule.onNodeWithTag("retention.message.$id").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Waiting for you to read this message"))
        rule.runOnIdle { vm.markConversationVisible(owner.profileId, owner.chatId, setOf(id)) }
        rule.onNodeWithTag("retention.message.$id").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "30 seconds remaining"))
    }
    @Test fun expiredIndicatorAndItsMessageAreRemovedAtTheDeadline() {
        val owner = group(); vm.retention.chooseExample(RetentionExample.NearExpiry); vm.retention.open(owner)
        val id = vm.chat(owner.chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().id
        show { vm.message(owner.chatId, id)?.let { MessageExpiryIndicator(it) } }
        rule.onNodeWithTag("retention.message.$id").assertExists()
        rule.runOnIdle { vm.retention.advanceExampleClock(5_000) }
        rule.onNodeWithTag("retention.message.$id").assertDoesNotExist()
    }
}

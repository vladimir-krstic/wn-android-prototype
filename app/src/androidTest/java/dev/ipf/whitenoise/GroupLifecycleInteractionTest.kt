package dev.ipf.whitenoise

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.conversation.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupLifecycleInteractionTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private val profile get() = vm.uiState.activeProfile!!
    private fun group(solo: Boolean = false) = GroupOwner(profile.id, vm.createGroup("Trail", "", ProfileAvatar.Monogram, if (solo) emptyList() else listOf("maya-chen"))!!)
    private fun show(owner: GroupOwner, transcript: Boolean = false) {
        rule.setContent { WhiteNoiseTheme {
            CompositionLocalProvider(LocalGroupWork provides vm.groupWork, LocalGroupLifecycle provides vm.groupLifecycle, LocalTranscript provides vm.transcript) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    vm.chat(owner.chatId)?.let { if (transcript) TranscriptPanel(profile, it) else GroupLifecyclePanel(profile, it, {}) }
                }
            }
        } }
    }
    private fun step(owner: GroupOwner) = rule.runOnIdle { vm.groupLifecycle.work[owner]!!.let { vm.groupLifecycle.advance(owner, it.id, it.stage) } }
    @Test fun transferRequiresPickingMemberAndExplicitConfirmation() {
        val owner = group(); show(owner)
        rule.onNodeWithText("Transfer administration").performClick()
        rule.onNodeWithText("Maya Chen").performClick()
        rule.onNodeWithText("Maya Chen will become an admin, and you’ll become a member.").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertNull(vm.groupLifecycle.work[owner]); assertEquals(1, vm.chat(owner.chatId)!!.members.count { it.role == GroupRole.Admin }) }
    }
    @Test fun partialTransferExplainsAcceptedGrantAndOffersStageRetry() {
        val owner = group(); vm.groupLifecycle.choose(GroupLifecycleScenario.StepDownFailure)
        vm.groupLifecycle.begin(owner, GroupLifecycleAction.Transfer, "maya-chen"); show(owner); step(owner); step(owner)
        rule.onNodeWithText("Administration was granted, but you’re still an admin.").assertExists()
        rule.onNodeWithText("Retry").assertIsEnabled().performClick(); step(owner)
        rule.onNodeWithText("Administration transferred. You’re now a member.").assertExists()
        rule.onNodeWithText("Transfer administration").assertDoesNotExist()
    }
    @Test fun soleMemberGetsDeletionConsequenceAndSafeCancel() {
        val owner = group(solo = true); show(owner)
        rule.onNodeWithText("Delete group").performClick()
        rule.onNodeWithText("You’re the only member. Leaving deletes this group and its history from this device.").assertExists()
        rule.onNodeWithText("Cancel").performClick(); rule.runOnIdle { assertNotNull(vm.chat(owner.chatId)) }
    }
    @Test fun disbandRequiresSeparateEnableAndDestructiveConfirmation() {
        val owner = group(); show(owner)
        rule.onNodeWithText("Disband group").assertDoesNotExist()
        rule.onNodeWithText("Enable disbanding").performClick()
        rule.onAllNodesWithText("Enable disbanding").onLast().performClick(); step(owner)
        rule.onNodeWithText("Disband group").performClick()
        rule.onNodeWithText("This permanently ends the group for everyone. Nobody can send new messages afterwards. This cannot be undone.").assertExists()
        rule.onNodeWithText("Cancel").performClick(); rule.runOnIdle { assertEquals(GroupLifecycle.Active, vm.chat(owner.chatId)!!.groupLifecycle) }
    }
    @Test fun unsupportedMemberBlockerIsHumanReadableAndDisablesEnabling() {
        val owner = group(); vm.groupLifecycle.chooseState(GroupStateScenario.Unsupported); show(owner)
        rule.onNodeWithText("Some members need to update White Noise before this group can be disbanded.").assertExists()
        rule.onNodeWithText("Enable disbanding").assertIsNotEnabled()
    }
    @Test fun frozenGroupOffersRecoveryAndKeepsHistory() {
        val owner = group(); val history = vm.chat(owner.chatId)!!.timeline; vm.groupLifecycle.chooseState(GroupStateScenario.Frozen); show(owner)
        rule.onNodeWithText("This group is temporarily frozen while White Noise verifies and repairs its state.").assertExists()
        rule.onNodeWithText("Retry group recovery").performClick(); step(owner)
        rule.runOnIdle { assertEquals(history, vm.chat(owner.chatId)!!.timeline); assertEquals(GroupLifecycle.Active, vm.chat(owner.chatId)!!.groupLifecycle) }
    }
    @Test fun endedGroupRetainsHistoryExplanationAndOnlyLocalDelete() {
        val owner = group(); vm.groupLifecycle.chooseState(GroupStateScenario.Ended); show(owner)
        rule.onNodeWithText("This group has ended. You can still read its history.").assertExists()
        rule.onNodeWithText("Delete chat from this device").assertIsEnabled()
        rule.onNodeWithText("Leave Group").assertDoesNotExist(); rule.onNodeWithText("Transfer administration").assertDoesNotExist()
    }
    @Test fun transcriptPreparationHasCancellationAndNeverClaimsSaved() {
        val owner = group(); show(owner, transcript = true)
        rule.onNodeWithText("Export transcript").performClick()
        rule.onNodeWithText("Preparing transcript…").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Transcript export cancelled").assertExists(); rule.onNodeWithText("Transcript saved").assertDoesNotExist()
    }
    @Test fun transcriptWriteFailureIsVisibleAndRetryRestartsPreparation() {
        val owner = group(); show(owner, transcript = true)
        rule.runOnIdle {
            vm.transcript.begin(owner); val id = vm.transcript.work!!.id; vm.transcript.advance(id)
            val w = vm.transcript.work!!; vm.transcript.encoded(id, ConversationTranscript.encode(w.source, w.entries))
            vm.transcript.save(id); vm.transcript.takeForWriting(id); vm.transcript.saved(id, false)
        }
        rule.onNodeWithText("Couldn’t save the transcript. Choose a destination and try again.").assertExists()
        rule.onNodeWithText("Retry").performClick(); rule.onNodeWithText("Preparing transcript…").assertExists()
    }
}

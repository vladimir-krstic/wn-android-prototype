package dev.ipf.whitenoise

import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.chats.GroupSetupScreen
import dev.ipf.whitenoise.ui.chats.NewGroupScreen
import dev.ipf.whitenoise.ui.conversation.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupWorkInteractionTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private val profile get() = vm.uiState.activeProfile!!
    private var backs = 0
    private fun group() = GroupOwner(profile.id, vm.createGroup("Trail", "Details", ProfileAvatar.Monogram, listOf("maya-chen"))!!)
    private fun show(content: @Composable () -> Unit) {
        // Owned completions are driven explicitly so assertions do not depend on frame timing.
        rule.setContent { WhiteNoiseTheme { CompositionLocalProvider(LocalGroupWork provides vm.groupWork) { content() } } }
    }
    private fun createStep() { rule.runOnIdle { vm.groupWork.creation!!.let { vm.groupWork.advanceCreate(it.id, it.phase) } } }
    @Test fun emptySelectionHasAnExplicitContinueAction() {
        var selected: List<String>? = null
        show { NewGroupScreen(profile, {}, { selected = it }) }
        rule.onNodeWithText("Create without other members").assertIsEnabled().performClick()
        rule.runOnIdle { assertEquals(emptyList<String>(), selected) }
    }
    @Test fun setupAllowsSoloGroupAndCapturesTimerThenDisablesSubmittedDetails() {
        show { GroupSetupScreen(profile, emptyList(), {}, { _, _, _ -> false }, creationOrigin = "setup") }
        rule.onNodeWithTag("group_setup.name").performTextInput("Solo")
        rule.onNodeWithText("Disappearing messages", substring = false).performScrollTo().performClick()
        rule.onNodeWithText("1 day", substring = false).performScrollTo().performClick()
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("Create Group").performClick()
        rule.onNodeWithTag("group_setup.name").assertIsNotEnabled()
        rule.onNodeWithText("Creating group…").assertExists()
        rule.runOnIdle { assertEquals(DisappearingDuration.OneDay, vm.groupWork.creation!!.timer) }
    }
    @Test fun timerFailureOffersRetryOrOpeningTheSameCreatedGroup() {
        vm.groupWork.chooseCreate(GroupCreateScenario.TimerFailure)
        show { GroupSetupScreen(profile, emptyList(), {}, { _, _, _ -> false }, creationOrigin = "setup") }
        rule.onNodeWithTag("group_setup.name").performTextInput("Solo")
        rule.onNodeWithText("Disappearing messages", substring = false).performScrollTo().performClick()
        rule.onNodeWithText("1 day", substring = false).performScrollTo().performClick()
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("Create Group").performClick(); createStep(); createStep()
        rule.onNodeWithText("Retry timer").performScrollTo().assertIsEnabled()
        val id = vm.groupWork.creation!!.chatId
        rule.onNodeWithText("Open group").performScrollTo().performClick(); createStep()
        rule.runOnIdle { assertEquals(id, vm.groupWork.creation!!.chatId); assertFalse(vm.groupWork.creation!!.timerApplied) }
    }
    @Test fun warmRosterPickerShowsChoicesButDisablesCommitUntilVerified() {
        val owner = group(); vm.groupWork.chooseRoster(GroupRosterScenario.WarmLoading)
        show { AddGroupMembersScreen(profile, vm.chat(owner.chatId)!!, {}, { vm.groupWork.beginMembers(owner, GroupMemberAction.Invite, it) }) }
        rule.onNodeWithText("Checking members…").assertExists()
        rule.onAllNodesWithText("Add People").filter(hasClickAction()).onFirst().assertIsNotEnabled()
        rule.runOnIdle { vm.groupWork.advanceRoster(owner, vm.groupWork.rosterLoads[owner]!!.id) }
        rule.onNodeWithText("Checking members…").assertDoesNotExist()
    }
    @Test fun rosterFailureExposesRetryAndDoesNotEnableCachedAdminCommands() {
        val owner = group(); vm.groupWork.chooseRoster(GroupRosterScenario.Failed)
        show { GroupRosterPanel(profile, vm.chat(owner.chatId)!!) }
        rule.runOnIdle { vm.groupWork.advanceRoster(owner, vm.groupWork.rosterLoads[owner]!!.id) }
        rule.onNodeWithText("Couldn’t load members.").assertExists()
        rule.onNodeWithText("Retry").performClick()
        rule.onNodeWithText("Checking members…").assertExists()
        rule.runOnIdle { assertFalse(vm.setGroupMemberAdmin(owner.chatId, "maya-chen", true)) }
    }
    @Test fun pendingInviteAndFailedRetryRemainVisibleWithTargetIdentity() {
        val owner = group(); vm.groupWork.chooseMutation(GroupMutationScenario.Failure)
        vm.groupWork.beginMembers(owner, GroupMemberAction.Invite, listOf("theo-grant"))
        show { GroupMemberWorkPanel(profile, vm.chat(owner.chatId)!!) }
        rule.onNodeWithText("Invitation pending").assertExists()
        rule.onNodeWithText(vm.person("theo-grant")!!.displayName).assertExists()
        rule.runOnIdle { vm.groupWork.memberWork[owner]!!.let { vm.groupWork.advanceMembers(owner, it.id, it.phase) } }
        rule.onNodeWithText("Couldn’t update members.").assertExists()
        rule.onNodeWithText("Retry").performClick()
        rule.onNodeWithText("Invitation pending").assertExists()
    }
    @Test fun editorSeparatesPrivateAndPublicPreviewAndSavesMetadataAfterCompletion() {
        val owner = group()
        show { EditGroupScreen(vm.chat(owner.chatId)!!, { backs++ }, { _, _, _ -> false }, profile) }
        rule.onNodeWithTag("group.image.private").assertExists()
        rule.onNodeWithTag("group.image.public").assertExists()
        rule.onNodeWithTag("group_edit.name").performScrollTo().performTextReplacement("Trail notes")
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithText("Saving group…").assertExists()
        rule.runOnIdle { assertEquals(0, backs); vm.groupWork.advanceEdit(owner, vm.groupWork.editWork[owner]!!.id) }
        rule.runOnIdle { assertEquals(1, backs); assertEquals("Trail notes", vm.chat(owner.chatId)!!.title) }
    }
    @Test fun failedEditorSaveRetainsDraftAndRetryDoesNotRequireReentry() {
        val owner = group(); vm.groupWork.chooseImage(GroupImageScenario.SaveFailure)
        show { EditGroupScreen(vm.chat(owner.chatId)!!, { backs++ }, { _, _, _ -> false }, profile) }
        rule.onNodeWithTag("group_edit.name").performScrollTo().performTextReplacement("Keep this name")
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle { vm.groupWork.advanceEdit(owner, vm.groupWork.editWork[owner]!!.id) }
        rule.onNodeWithText("Retry", substring = false).performScrollTo().performClick()
        rule.runOnIdle { vm.groupWork.advanceEdit(owner, vm.groupWork.editWork[owner]!!.id) }
        rule.runOnIdle { assertEquals("Keep this name", vm.chat(owner.chatId)!!.title); assertEquals(1, backs) }
    }
    @Test fun emojiImageUsesTheExistingPickerAndCancelAppliesNothing() {
        var image: ProfileAvatar? = null
        show { GroupEmojiImageDialog({ backs++ }, { image = it }) }
        rule.onNodeWithText("Use image").assertIsNotEnabled()
        rule.onAllNodesWithText("Create from emoji").filter(hasClickAction()).onFirst().performClick()
        rule.onNodeWithTag("emoji.picker").assertExists()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertEquals(1, backs); assertNull(image) }
    }
    @Test fun platformRendererProducesOneOwnedOpaqueImageAndRejectsUnsupportedGlyphs() {
        val result = GroupEmojiRenderer.render(listOf("🌲", "🦊")) { _, _ -> true } as GroupEmojiRender.Ready
        val bitmap = BitmapFactory.decodeByteArray(result.image.bytes, 0, result.image.bytes.size)
        try { assertEquals(512, bitmap.width); assertEquals(512, bitmap.height); assertEquals(0xff3c4043.toInt(), bitmap.getPixel(0, 0)) } finally { bitmap.recycle() }
        assertEquals(listOf("🌲", "🦊"), result.emojis)
        assertEquals(GroupEmojiRender.Unsupported, GroupEmojiRenderer.render(listOf("🌲")) { _, _ -> false })
        assertEquals(GroupEmojiRender.Failed, GroupEmojiRenderer.render(emptyList()))
    }
}

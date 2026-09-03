package dev.ipf.whitenoise

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.chats.PersonProfileScreen
import dev.ipf.whitenoise.ui.settings.EditProfileScreen
import dev.ipf.whitenoise.ui.settings.ProfileImageActions
import dev.ipf.whitenoise.ui.settings.ProfileImageViewer
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileEditorFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    @Test fun suggestedNameOnlyChangesTheDraftUntilSave() {
        val profile = ProfileFixtures.marmota
        var submitted: ProfileEditDraft? = null
        rule.setContent { WhiteNoiseTheme {
            EditProfileScreen(profile, {}, { _, _, _ -> false }, { false }, onSaveDraft = { submitted = it; true })
        } }
        rule.onNodeWithText("Edit").performClick()
        rule.onNodeWithTag("profile.suggest_name").performScrollTo().performClick()
        rule.runOnIdle { assertNull(submitted) }
        rule.onNodeWithTag("profile.save").performClick()
        rule.runOnIdle { assertNotNull(submitted); assertNotEquals(profile.name, submitted!!.name) }
    }
    @Test fun invalidLightningDisablesSaveAndProvidesAnError() {
        rule.setContent { WhiteNoiseTheme { EditProfileScreen(ProfileFixtures.marmota, {}, { _, _, _ -> true }, { true }) } }
        rule.onNodeWithText("Edit").performClick()
        rule.onNodeWithTag("profile.lightning_field").performScrollTo().performTextInput("alice@invalid")
        rule.onNodeWithText("Enter a valid name@domain Lightning address.").assertIsDisplayed()
        rule.onNodeWithTag("profile.save").assertIsNotEnabled()
    }
    @Test fun unresolvedLightningPreservesAllPublishedFieldsAndRetryCanSave() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true); selectProfileSaveScenario(ProfileSaveScenario.UnresolvedLightning) }
        val before = vm.uiState.activeProfile!!
        rule.setContent { WhiteNoiseTheme {
            EditProfileScreen(vm.uiState.activeProfile!!, {}, { _, _, _ -> false }, { false },
                onSaveDraft = { vm.beginProfileSave(before.id, it) }, saveAttempt = vm.profileSaveAttempt,
                onAdvanceSave = vm::advanceProfileSave, onCancelSave = { vm.cancelProfileSave(before.id) })
        } }
        rule.onNodeWithText("Edit").performClick()
        rule.onNodeWithTag("profile.name_field").performScrollTo().performTextReplacement("Changed draft")
        rule.onNodeWithTag("profile.lightning_field").performScrollTo().performTextInput("friend@payments.example")
        rule.onNodeWithTag("profile.save").performClick()
        rule.onNodeWithText("Address could not be verified. Check your Lightning address or try again.").performScrollTo().assertIsDisplayed()
        rule.runOnIdle { assertEquals(before, vm.uiState.activeProfile) }
        rule.onNodeWithTag("profile.save").performClick()
        rule.onNodeWithText("Edit").assertIsDisplayed()
        rule.runOnIdle { assertEquals("Changed draft", vm.uiState.activeProfile!!.name); assertEquals("friend@payments.example", vm.uiState.activeProfile!!.lightningAddress) }
    }
    @Test fun ownAvatarOpensViewerWithoutEnteringEdit() {
        rule.setContent { WhiteNoiseTheme { EditProfileScreen(ProfileFixtures.marmota, {}, { _, _, _ -> true }, { true }) } }
        rule.onNodeWithTag("profile.avatar").performClick()
        rule.onNodeWithTag("profile.image_viewer").assertExists()
        rule.onNodeWithText("Edit profile").assertHasClickAction()
        rule.onNodeWithText("Zoom In").assertHasClickAction()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Edit").assertIsDisplayed()
        rule.onNodeWithTag("profile.save").assertDoesNotExist()
    }
    @Test fun otherProfileShowsLightningAndBannerWithoutAnEditAction() {
        val person = PeopleDiscovery.directory.first().copy(avatar = ProfileAvatar.Asset(AvatarAsset.Fox))
        rule.setContent { WhiteNoiseTheme {
            PersonProfileScreen(ProfileFixtures.marmota, person, {}, { true }, {}, {})
        } }
        rule.onNodeWithTag("person_profile.lightning").performScrollTo().assertTextEquals("Lightning: river@payments.example")
        rule.onNodeWithTag("profile.banner").performScrollTo().performClick()
        rule.onNodeWithTag("profile.image_viewer").assertExists()
        rule.onNodeWithText("Edit profile").assertDoesNotExist()
    }
    @Test fun failedBannerSelectionRetainsOldImageAndCanRetry() {
        val original = ProfileAvatar.Asset(AvatarAsset.GardenClub)
        var chosen: ProfileAvatar? by mutableStateOf(original)
        var fail = true
        rule.setContent { WhiteNoiseTheme {
            ProfileImageActions("owner", chosen, true, true, onChange = { chosen = it }, onBusyChanged = {}, consumeFailure = { fail.also { fail = false } })
        } }
        rule.onNodeWithText("Change banner").performClick()
        rule.onNodeWithText("Find Image on Web").performClick()
        rule.onNodeWithContentDescription("Fox").performClick()
        rule.onNodeWithText("Done").performClick()
        rule.onNodeWithText("This image could not be loaded. Choose another image or try again.").assertIsDisplayed()
        rule.runOnIdle { assertEquals(original, chosen) }
        rule.onNodeWithText("Try Again").performClick()
        rule.runOnIdle { assertTrue(chosen is ProfileAvatar.WebImage) }
    }
    @Test fun removedBannerDraftSurvivesRecreationAndBackRestoresPublishedImage() {
        val banner = ProfileAvatar.Asset(AvatarAsset.GardenClub)
        val profile = ProfileFixtures.marmota.copy(banner = banner)
        val restoration = StateRestorationTester(rule)
        var images: ProfileImageDraft? by mutableStateOf(null)
        restoration.setContent { WhiteNoiseTheme {
            EditProfileScreen(profile, {}, { _, _, _ -> true }, { true }, retainedImages = images,
                onRetainImages = { avatar, image -> images = ProfileImageDraft(profile.id, avatar, image) }, onCancelSave = { images = null })
        } }
        rule.onNodeWithText("Edit").performClick()
        rule.onNodeWithText("Change banner").performScrollTo().performClick()
        rule.onNodeWithText("Remove banner").performClick()
        rule.onNodeWithTag("profile.banner").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("profile.banner").assertDoesNotExist()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithTag("profile.banner").assertExists()
    }
}

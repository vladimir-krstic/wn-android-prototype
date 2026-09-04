package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.pressBack
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.settings.DataUsageScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MediaDownloadInteractionTest {
    @get:Rule val compose = createComposeRule()
    private var profile by mutableStateOf(ProfileFixtures.marmota)
    private fun show() { compose.setContent { WhiteNoiseTheme {
        DataUsageScreen(profile, {}, { profile = profile.copy(settings = it) },
            { profile = profile.copy(settings = profile.settings.copy(automaticDownloadsPaused = it)) })
    } } }
    @Test fun allFourNetworkSwitchesRemainIndependentAndBackKeepsAppliedChoices() {
        show(); compose.onNodeWithText("Photos").performClick()
        compose.onNodeWithTag("download.network.Wifi").assertIsOn()
        compose.onNodeWithTag("download.network.Mobile").assertIsOff().performClick()
        compose.onNodeWithTag("download.network.Roaming").assertIsOff().performClick()
        compose.onNodeWithTag("download.network.Metered").assertIsOff()
        pressBack()
        compose.onNodeWithTag("download.network.options").assertDoesNotExist()
        compose.runOnIdle {
            assertTrue(profile.settings.downloadMatrix.allows(DownloadMediaType.Photos, DownloadNetwork.Mobile))
            assertTrue(profile.settings.downloadMatrix.allows(DownloadMediaType.Photos, DownloadNetwork.Roaming))
            assertFalse(profile.settings.downloadMatrix.allows(DownloadMediaType.Photos, DownloadNetwork.Metered))
        }
    }
    @Test fun stopNeedsConfirmationAndCancelDoesNotPause() {
        show(); compose.onNodeWithText("Stop automatic downloads").performScrollTo().performClick()
        compose.onNodeWithText("Clear waiting automatic downloads for this profile? Active downloads and downloads you started yourself will continue.").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertFalse(profile.settings.automaticDownloadsPaused) }
    }
    @Test fun confirmedStopShowsRestartAndRestartKeepsTheMatrixAndQuality() {
        show(); val before = profile.settings
        compose.onNodeWithText("Stop automatic downloads").performScrollTo().performClick()
        compose.onNodeWithTag("download.stop.confirm").performClick()
        compose.onNodeWithText("Automatic downloads are paused").assertIsDisplayed()
        compose.onNodeWithText("Restart automatic downloads").performClick()
        compose.runOnIdle { assertEquals(before, profile.settings) }
    }
    @Test fun switchingProfileDismissesPendingStopAndNetworkChoice() {
        show(); compose.onNodeWithText("Photos").performClick()
        compose.runOnIdle { profile = profile.copy(id = "another-profile") }
        compose.onNodeWithTag("download.network.options").assertDoesNotExist()
        compose.onNodeWithText("Stop automatic downloads").performScrollTo().performClick()
        compose.runOnIdle { profile = ProfileFixtures.marmota }
        compose.onNodeWithTag("download.stop.confirm").assertDoesNotExist()
        compose.runOnIdle { assertFalse(profile.settings.automaticDownloadsPaused) }
    }
    @Test fun originalQualityIsAvailableWithHonestMediaAndMetadataExplanation() {
        show(); compose.onNodeWithText("Media quality").performScrollTo().performClick()
        compose.onNodeWithTag("choice_dialog.option.3").performClick()
        compose.runOnIdle { assertEquals(SentMediaQuality.Original, profile.settings.sentMediaQuality) }
        compose.onNodeWithText("Applies to new photos and voice messages. Videos and audio files are sent as-is.").performScrollTo().assertIsDisplayed()
    }
    @Test fun expandedQualityContentCanScrollToTheLastChoice() {
        show(); compose.onNodeWithText("Media quality").performScrollTo().performClick()
        compose.onNodeWithTag("choice_dialog.content").assert(hasScrollAction())
        compose.onNodeWithTag("choice_dialog.option.3").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(SentMediaQuality.Original, profile.settings.sentMediaQuality) }
    }
}

package dev.ipf.whitenoise

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.AppRoute
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.conversation.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationSharingTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun model(scenario: LocationScenario? = null): AppViewModel = AppViewModel().apply {
        completeSignIn(OnboardingOrigin.Initial)
        if (scenario != null) { setDeveloperToolsEnabled(true); selectLocationScenario(scenario) }
        openLocation(uiState.activeProfileId!!, "fiatjaf")
    }
    private fun show(vm: AppViewModel) {
        rule.setContent { WhiteNoiseTheme { vm.locationSession?.let { session -> LocationPickerDialog(session) { vm.locationAction(session.id, it) } } ?: Text("Chat") } }
    }
    private fun coordinates(latitude: String = "45.25", longitude: String = "19.84") {
        rule.onNodeWithTag("location.latitude").performTextReplacement(latitude)
        rule.onNodeWithTag("location.longitude").performTextReplacement(longitude)
    }
    @Test fun composerLocationEntryOpensTheOwnedFlowAndCancelsWithoutSending() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); dismissDiagnosticsPrompt(uiState.activeProfileId!!) }
        val before = vm.chat("fiatjaf")!!
        lateinit var nav: androidx.navigation.NavHostController
        rule.setContent { nav = androidx.navigation.compose.rememberNavController(); WhiteNoiseTheme { dev.ipf.whitenoise.navigation.WhiteNoiseNavHost(nav, vm) } }
        rule.runOnIdle { nav.navigate(AppRoute.Conversation("fiatjaf")) }
        rule.onNodeWithContentDescription("Add Attachment").performClick()
        rule.onNodeWithText("Location").performClick()
        rule.onNodeWithTag("location.picker").assertIsDisplayed()
        rule.onNodeWithTag("location.cancel").performScrollTo().performClick()
        rule.runOnIdle { assertNull(vm.locationSession); assertEquals(before.timeline, vm.chat("fiatjaf")!!.timeline) }
    }
    @Test fun explicitZeroCoordinatesReviewAndSendExactlyOneMessage() {
        val vm = model(); val count = vm.chat("fiatjaf")!!.timeline.size; show(vm)
        rule.onNodeWithTag("location.confirm").assertIsNotEnabled()
        coordinates("0", "0")
        rule.onNodeWithTag("location.confirm").performClick()
        rule.onNodeWithTag("location.coordinates").assertTextEquals("0.000000, 0.000000")
        rule.onNodeWithTag("location.confirm").performClick()
        rule.waitUntil(5_000) { vm.locationSession == null }
        rule.runOnIdle { assertEquals(count + 1, vm.chat("fiatjaf")!!.timeline.size) }
    }
    @Test fun coordinateBoundsHaveErrorsAndCannotEnterReview() {
        val vm = model(); show(vm); coordinates("91", "181")
        rule.onNodeWithText("Enter a latitude between −90 and 90.").assertExists()
        rule.onNodeWithText("Enter a longitude between −180 and 180.").assertExists()
        rule.onNodeWithTag("location.confirm").assertIsNotEnabled()
        coordinates("-90", "180"); rule.onNodeWithTag("location.confirm").assertIsEnabled()
    }
    @Test fun deniedCurrentLocationKeepsManualSelectionAvailable() {
        val vm = model(LocationScenario.PermissionDenied); show(vm)
        rule.onNodeWithTag("location.current").performScrollTo().performClick()
        rule.waitUntil(5_000) { vm.locationSession?.failure == LocationFailure.PermissionDenied }
        rule.onNodeWithTag("location.error").performScrollTo().assertTextContains("Location access is denied.")
        rule.onNodeWithTag("location.latitude").performScrollTo(); coordinates()
        rule.onNodeWithTag("location.confirm").performClick()
        rule.runOnIdle { assertEquals(LocationPhase.Review, vm.locationSession!!.phase) }
    }
    @Test fun approximatePointShowsAccuracyAndManualEditingRemovesIt() {
        val vm = model(LocationScenario.Approximate); show(vm)
        rule.onNodeWithTag("location.current").performScrollTo().performClick()
        rule.waitUntil(5_000) { vm.locationSession?.accuracyMeters == 1500 }
        rule.onNodeWithText("Approximate accuracy: 1500 m").assertExists()
        rule.onNodeWithTag("location.latitude").performScrollTo().performTextReplacement("37.421")
        rule.runOnIdle { assertNull(vm.locationSession!!.accuracyMeters) }
    }
    @Test fun currentLocationFailureRetryGetsAPointWithoutSending() {
        val vm = model(LocationScenario.RequestFailure); val count = vm.chat("fiatjaf")!!.timeline.size; show(vm)
        rule.onNodeWithTag("location.current").performScrollTo().performClick()
        rule.waitUntil(5_000) { vm.locationSession?.failure == LocationFailure.RequestFailed }
        rule.onNodeWithTag("location.retry").performScrollTo().performClick()
        rule.waitUntil(5_000) { vm.locationSession?.point != null }
        rule.runOnIdle { assertEquals(count, vm.chat("fiatjaf")!!.timeline.size) }
    }
    @Test fun sendFailurePreservesDraftAndRetryCanComplete() {
        val vm = model(LocationScenario.SendFailure); vm.updateDraftText("fiatjaf", "Keep this draft"); show(vm); coordinates()
        rule.onNodeWithTag("location.confirm").performClick(); rule.onNodeWithTag("location.confirm").performClick()
        rule.waitUntil(5_000) { vm.locationSession?.failure == LocationFailure.SendFailed }
        rule.runOnIdle { assertEquals("Keep this draft", vm.chat("fiatjaf")!!.draftText) }
        rule.onNodeWithTag("location.confirm").performClick()
        rule.waitUntil(5_000) { vm.locationSession == null }
        rule.runOnIdle { assertEquals("Keep this draft", vm.chat("fiatjaf")!!.draftText) }
    }
    @Test fun systemBackReturnsToEditingThenCancels() {
        val vm = model(); show(vm); coordinates()
        rule.onNodeWithTag("location.confirm").performClick()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.runOnIdle { assertEquals(LocationPhase.Editing, vm.locationSession!!.phase); assertEquals(SharedLocation(45.25, 19.84), vm.locationSession!!.point) }
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("location.picker").assertDoesNotExist()
    }
    @Test fun mapProposalUsesOnlyTheSelectedCoordinatesAndNeedsNoLocationGrant() {
        val intent = locationMapIntent(SharedLocation(-12.5, 179.25))!!
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("geo:-12.500000,179.250000?q=-12.500000,179.250000", intent.dataString)
        assertNull(intent.`package`); assertNull(intent.component)
        assertNull(locationMapIntent(SharedLocation(Double.NaN, 0.0)))
    }
    @Test fun missingMapsHandlerOffersCopyAndRetryWithoutAnyAutomaticOpen() {
        val point = SharedLocation(45.25, 19.84); var calls = 0
        rule.setContent { WhiteNoiseTheme { LocationMessageCard(point, onOpenMap = { calls++; MapOpenResult.NoHandler }) } }
        rule.runOnIdle { assertEquals(0, calls) }
        rule.onNodeWithTag("location.message").performClick()
        rule.onNodeWithText("No maps app is available. You can copy the coordinates or link.").assertExists()
        rule.onNodeWithText("Copy coordinates").performClick()
        rule.runOnIdle { assertEquals(point.coordinates, (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip!!.getItemAt(0).text.toString()) }
        rule.onNodeWithText("Copy link").performClick()
        rule.runOnIdle { assertEquals(point.mapsLink, (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip!!.getItemAt(0).text.toString()) }
        rule.onNodeWithText("Retry").performClick(); rule.runOnIdle { assertEquals(2, calls) }
    }
    @OptIn(ExperimentalTestApi::class)
    @Test fun narrowRtlLargeTextKeepsFieldsReviewAndCancelReachable() {
        val vm = model()
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 600.dp))) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 2f), LocalLayoutDirection provides LayoutDirection.Rtl) {
                    WhiteNoiseTheme { vm.locationSession?.let { state -> LocationPickerDialog(state) { vm.locationAction(state.id, it) } } }
                }
            }
        }
        rule.onNodeWithTag("location.latitude").performScrollTo().performTextInput("-90")
        rule.onNodeWithTag("location.longitude").performScrollTo().performTextInput("180")
        rule.onNodeWithTag("location.confirm").assertIsDisplayed().performClick()
        rule.onNodeWithTag("location.coordinates").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("location.cancel").performScrollTo().assertIsDisplayed()
    }
    @Test fun sendingFromAnOlderHistoryTargetRevealsTheLocationCard() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); dismissDiagnosticsPrompt(uiState.activeProfileId!!) }
        val first = vm.chat("fiatjaf")!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().first().id
        lateinit var nav: androidx.navigation.NavHostController
        rule.setContent { nav = androidx.navigation.compose.rememberNavController(); WhiteNoiseTheme { dev.ipf.whitenoise.navigation.WhiteNoiseNavHost(nav, vm) } }
        rule.runOnIdle { nav.navigate(AppRoute.Conversation("fiatjaf", targetMessageId = first)) }
        rule.onNodeWithContentDescription("Add Attachment").performClick()
        rule.onNodeWithText("Location").performClick()
        coordinates(); rule.onNodeWithTag("location.confirm").performClick(); rule.onNodeWithTag("location.confirm").performClick()
        rule.waitUntil(5_000) { vm.locationSession == null }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("location.message").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("location.message").assertIsDisplayed()
    }

    @Test fun accessibleSignControlsSelectNegativeCoordinatesWithoutASignedKeyboard() {
        val vm = model(); show(vm); coordinates("12.5", "179.25")
        rule.onNodeWithContentDescription("Change latitude sign").performClick()
        rule.onNodeWithContentDescription("Change longitude sign").performClick()
        rule.runOnIdle { assertEquals(SharedLocation(-12.5, -179.25), vm.locationSession!!.point) }
        rule.onNodeWithContentDescription("Change latitude sign").performClick()
        rule.runOnIdle { assertEquals(SharedLocation(12.5, -179.25), vm.locationSession!!.point) }
    }

}

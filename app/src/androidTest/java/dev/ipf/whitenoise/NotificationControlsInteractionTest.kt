package dev.ipf.whitenoise

import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.*
import dev.ipf.whitenoise.ui.components.MuteDurationDialog
import dev.ipf.whitenoise.ui.settings.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class NotificationControlsInteractionTest {
    @get:Rule val rule=createAndroidComposeRule<EmptyTestActivity>()
    private val vm=AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private fun group()=vm.createGroup("Quiet conversations","",ProfileAvatar.Monogram,emptyList())!!
    private fun step() { vm.notificationControls.work!!.let { vm.notificationControls.advance(it.id,it.attempt) } }
    private fun scroll(text: String) { rule.onNodeWithTag("settings.list").performScrollToNode(hasText(text)) }
    private fun global() { rule.setContent { WhiteNoiseTheme { CompositionLocalProvider(LocalNotificationControls provides vm.notificationControls) {
        NotificationsScreen(vm.uiState.activeProfile!!,{},vm::updateProfileSettings,NotificationPermissionStatus.Allowed)
        vm.notificationControls.work?.let { NotificationWorkDialog(vm.notificationControls,it) }
    } } } }
    private fun conversation(chat: String, open: ((NotificationCategory,Boolean)->NotificationSettingsOpen)? = null) {
        rule.setContent { WhiteNoiseTheme {
            ConversationNotificationScreen(vm.uiState.activeProfile!!,vm.chat(chat)!!,vm.notificationControls,{},open)
            vm.notificationControls.work?.let { NotificationWorkDialog(vm.notificationControls,it) }
        } }
    }
    @Test fun localDeliveryFailureKeepsPreviousSwitchAndOffersRetry() {
        vm.notificationControls.choose(NotificationScenario.SaveFailure); global()
        rule.onNodeWithText("Local notifications").performClick()
        rule.runOnIdle { step(); assertTrue(vm.uiState.activeProfile!!.settings.localNotifications) }
        rule.onNodeWithText("Notification settings couldn’t be updated. Your previous settings are unchanged.").assertExists()
        rule.onNodeWithText("Retry").performClick(); rule.runOnIdle { step() }
        rule.onNodeWithText("Local notifications").assertIsOff()
        rule.onNodeWithText("Native push").assertIsNotEnabled()
    }
    @Test fun missingPushProviderShowsTheAvailabilityReason() {
        vm.notificationControls.chooseEnvironment(NotificationEnvironment(push=PushAvailability.ProviderNotInitialized)); global()
        rule.onNodeWithText("Native push").assertIsNotEnabled().assertIsOff()
        rule.onNodeWithText("The push provider isn’t available. Reopen White Noise to try again.").assertExists()
    }
    @Test fun rejectedBackgroundStartKeepsAcceptedLocalEnableAndHasRetry() {
        vm.updateProfileSettings(vm.uiState.activeProfile!!.settings.copy(localNotifications=false,nativePushNotifications=false))
        vm.notificationControls.choose(NotificationScenario.ServiceRejected); global()
        scroll("Keep connected in the background"); rule.onNodeWithText("Keep connected in the background").performClick()
        rule.runOnIdle { step() }
        rule.onNodeWithText("The background connection couldn’t start. Local notifications are on. Retry to keep connected.").assertExists()
        rule.onNodeWithText("Retry").performClick(); rule.runOnIdle { step(); assertTrue(vm.notificationControls.backgroundConnection) }
    }
    @Test fun notifyForChangesWhileMutedWithoutUnmutingTheChat() {
        val c=group(); vm.setChatMute(c,MuteDuration.Always); conversation(c)
        rule.onNodeWithText("Notify for").performClick(); rule.onNodeWithTag("notification.mode.MentionsOnly").performClick()
        rule.runOnIdle { step(); assertEquals(MuteDuration.Always,vm.chat(c)!!.muteDuration) }
        rule.onNodeWithText("Mentions only").assertExists(); rule.onNodeWithText("Mute").assertIsOn()
    }
    @Test fun vibrationSelectionIsStagedAndCancelPreservesTheSavedChoice() {
        val c=group(); conversation(c)
        rule.onNodeWithText("Vibration pattern").performClick()
        rule.onNodeWithTag("notification.vibration.Double").performClick(); rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertEquals(VibrationChoice.SystemDefault,vm.chat(c)!!.vibration) }
        rule.onNodeWithText("Vibration pattern").performClick(); rule.onNodeWithTag("notification.vibration.Double").performClick()
        rule.onNodeWithText("Save").performClick(); rule.runOnIdle { step() }
        rule.onNodeWithText("Double").assertExists()
    }
    @Test fun unavailableVibrationPreviewDoesNotSaveItsSelection() {
        val c=group(); conversation(c); rule.onNodeWithText("Vibration pattern").performClick()
        rule.onNodeWithTag("notification.vibration.Short").performClick(); rule.onNodeWithTag("notification.vibration.preview").performClick()
        rule.runOnIdle { vm.notificationControls.preview!!.let { vm.notificationControls.advancePreview(it.id,it.phase) } }
        rule.onNodeWithText("Vibration preview isn’t available on this device.").assertExists()
        rule.onNodeWithText("Cancel").performClick(); rule.runOnIdle { assertNull(vm.notificationControls.preview); assertEquals(VibrationChoice.SystemDefault,vm.chat(c)!!.vibration) }
    }
    @Test fun categoryScopeSwitchAndAndroidFallbackAreSeparateActions() {
        val c=group(); var requested: Pair<NotificationCategory,Boolean>?=null
        conversation(c) { category,custom -> requested=category to custom; NotificationSettingsOpen.AppFallback }
        scroll("Mentions")
        rule.onNode(hasAnyAncestor(hasTestTag("notification.category.Mentions")) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role,Role.Switch)).performClick()
        rule.runOnIdle { step(); assertTrue(NotificationCategory.Mentions in vm.chat(c)!!.customNotificationCategories) }
        rule.onNodeWithText("Mentions").performClick()
        rule.runOnIdle { assertEquals(NotificationCategory.Mentions to true,requested) }
        rule.onNodeWithText("This category’s settings aren’t available. Android app notification settings were opened instead.").assertExists()
    }
    @Test fun customMuteDateAndTimeCommitOnlyAfterFinalAcceptance() {
        var open by mutableStateOf(true); var selected: Long?=null; val now=MessageForwarding.nowMillis
        rule.setContent { WhiteNoiseTheme { if (open) MuteDurationDialog(onDismiss={open=false},onSelect={},
            onCustomSelect={selected=it;open=false},nowMillis={now},zone=ZoneOffset.UTC) } }
        rule.onNodeWithTag("mute.duration.Custom").performClick(); rule.onNodeWithTag("mute.custom.date.next").performClick()
        rule.runOnIdle { assertNull(selected) }
        rule.onNodeWithTag("mute.custom.confirm").performClick()
        rule.runOnIdle { assertNotNull(selected); assertTrue(selected!!>now) }
    }
    @Test fun customMuteRejectsATimeThatElapsedWhileThePickerWasOpen() {
        var now=MessageForwarding.nowMillis; var selected: Long?=null
        rule.setContent { WhiteNoiseTheme { MuteDurationDialog(onDismiss={},onSelect={},onCustomSelect={selected=it},nowMillis={now},zone=ZoneOffset.UTC) } }
        rule.onNodeWithTag("mute.duration.Custom").performClick(); rule.onNodeWithTag("mute.custom.date.next").performClick()
        rule.runOnIdle { now+=7_200_000 }; rule.onNodeWithTag("mute.custom.confirm").performClick()
        rule.onNodeWithText("Choose a future date and time.").assertExists()
        rule.onNodeWithText("Cancel").performClick(); rule.onNodeWithTag("mute.duration.dialog").assertExists()
        rule.runOnIdle { assertNull(selected) }
    }
    @Test @SdkSuppress(minSdkVersion=Build.VERSION_CODES.O)
    fun androidCategoryIntentUsesOnlyThePublicActionAndExactCategory() {
        val intent=notificationCategoryIntent(rule.activity,NotificationCategory.Mentions.channelId)
        assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS,intent.action)
        assertEquals(rule.activity.packageName,intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        assertEquals("mentions",intent.getStringExtra(Settings.EXTRA_CHANNEL_ID))
        assertFalse(intent.hasExtra(Settings.EXTRA_CONVERSATION_ID))
    }
}

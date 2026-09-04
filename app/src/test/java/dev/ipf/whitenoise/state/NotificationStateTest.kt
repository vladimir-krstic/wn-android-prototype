package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class NotificationStateTest {
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private fun group(vm: AppViewModel) = vm.createGroup("Notifications","",ProfileAvatar.Monogram,emptyList())!!
    private fun step(vm: AppViewModel) { vm.notificationControls.work!!.let { vm.notificationControls.advance(it.id,it.attempt) } }
    private fun delivery(vm: AppViewModel, kind: NotificationDelivery, enabled: Boolean) = vm.notificationControls.request(NotificationChange.Delivery(kind,enabled))!!
    @Test fun failedDeliveryDoesNotChangePreferenceAndRetryAppliesOnce() {
        val vm=model(); vm.notificationControls.choose(NotificationScenario.SaveFailure)
        val id=delivery(vm,NotificationDelivery.Local,false); step(vm)
        assertTrue(vm.uiState.activeProfile!!.settings.localNotifications)
        assertEquals(NotificationFailure.Save,vm.notificationControls.work!!.failure)
        assertTrue(vm.notificationControls.retry(id)); step(vm)
        assertFalse(vm.uiState.activeProfile!!.settings.localNotifications)
        vm.notificationControls.advance(id,1); assertNull(vm.notificationControls.work)
    }
    @Test fun disablingLocalStopsBothDependentDeliveryPreferences() {
        val vm=model(); delivery(vm,NotificationDelivery.Background,true); step(vm)
        assertTrue(vm.notificationControls.backgroundConnection)
        delivery(vm,NotificationDelivery.Local,false); step(vm)
        val settings=vm.uiState.activeProfile!!.settings
        assertFalse(settings.localNotifications); assertFalse(settings.nativePushNotifications); assertFalse(vm.notificationControls.backgroundConnection)
    }
    @Test fun rejectedBackgroundStartRetainsAcceptedLocalEnableAndRetriesOnlyConnection() {
        val vm=model(); vm.updateProfileSettings(vm.uiState.activeProfile!!.settings.copy(localNotifications=false,nativePushNotifications=false))
        vm.notificationControls.choose(NotificationScenario.ServiceRejected)
        val id=delivery(vm,NotificationDelivery.Background,true); step(vm)
        assertTrue(vm.uiState.activeProfile!!.settings.localNotifications)
        assertFalse(vm.notificationControls.backgroundConnection)
        assertEquals(NotificationFailure.ServiceRejected,vm.notificationControls.work!!.failure)
        vm.notificationControls.retry(id); step(vm); assertTrue(vm.notificationControls.backgroundConnection)
    }
    @Test fun lostPermissionAndPushDependenciesPreventStaleEnable() {
        val vm=model(); vm.updateProfileSettings(vm.uiState.activeProfile!!.settings.copy(nativePushNotifications=false))
        delivery(vm,NotificationDelivery.Push,true); vm.notificationControls.observePermission(false); step(vm)
        assertEquals(NotificationFailure.Permission,vm.notificationControls.work!!.failure)
        assertFalse(vm.uiState.activeProfile!!.settings.nativePushNotifications)
        vm.notificationControls.observePermission(true)
        PushAvailability.entries.filter { it!=PushAvailability.Available }.forEach {
            vm.notificationControls.chooseEnvironment(NotificationEnvironment(push=it)); delivery(vm,NotificationDelivery.Push,true); step(vm)
            assertEquals(NotificationFailure.PushUnavailable,vm.notificationControls.work!!.failure)
        }
    }
    @Test fun profileRoundTripSignOutAndWipeRevokePendingChanges() {
        for (wipe in listOf(false,true)) {
            val vm=model(); val id=delivery(vm,NotificationDelivery.Local,false)
            vm.signOutActiveProfile(wipe); vm.notificationControls.advance(id,0); assertNull(vm.notificationControls.work)
        }
        val vm=model(); val original=vm.uiState.activeProfileId!!; val id=delivery(vm,NotificationDelivery.Local,false)
        vm.completeSignIn(OnboardingOrigin.AddProfile); vm.selectProfile(original); vm.notificationControls.advance(id,0)
        assertNull(vm.notificationControls.work); assertTrue(vm.uiState.activeProfile!!.settings.localNotifications)
    }
    @Test fun replacementNavigationAndChangedPreferenceCannotCommitOldWork() {
        val vm=model(); val c=group(vm); vm.notificationControls.observeRoute("info")
        val id=vm.notificationControls.request(NotificationChange.Mode(NotifyFor.MentionsOnly),c)!!
        val replacement=vm.notificationControls.request(NotificationChange.Vibration(VibrationChoice.Double),c)!!
        vm.notificationControls.advance(id,0); assertEquals(NotifyFor.AllMessages,vm.chat(c)!!.notifyFor)
        vm.notificationControls.observeRoute("chats"); vm.notificationControls.advance(replacement,0); assertEquals(VibrationChoice.SystemDefault,vm.chat(c)!!.vibration)
        vm.notificationControls.request(NotificationChange.Mute(MuteDuration.OneHour),c)
        vm.setChatMute(c,MuteDuration.Always); step(vm)
        assertEquals(NotificationFailure.Changed,vm.notificationControls.work!!.failure); assertEquals(MuteDuration.Always,vm.chat(c)!!.muteDuration)
    }
    @Test fun notifyChoiceCanChangeWhileMutedAndTheSameForegroundClockExpiresMute() {
        val vm=model(); val c=group(vm); vm.setChatMute(c,MuteDuration.OneHour)
        vm.notificationControls.request(NotificationChange.Mode(NotifyFor.MentionsOnly),c); step(vm)
        assertEquals(MuteDuration.OneHour,vm.chat(c)!!.muteDuration)
        vm.retention.advanceExampleClock(3_600_000)
        assertNull(vm.chat(c)!!.muteDuration); assertEquals(NotifyFor.MentionsOnly,vm.chat(c)!!.notifyFor)
    }
    @Test fun elapsedCustomChoiceCannotCommitEvenAfterRetry() {
        val vm=model(); val c=group(vm); val until=vm.retention.nowMillis+1000
        val id=vm.notificationControls.request(NotificationChange.Mute(MuteDuration.Custom,until),c)!!
        vm.retention.advanceExampleClock(1000); step(vm)
        assertEquals(NotificationFailure.ExpiredTime,vm.notificationControls.work!!.failure)
        vm.notificationControls.retry(id); step(vm); assertNull(vm.chat(c)!!.muteDuration)
    }
    @Test fun backgroundRuntimeStopRevertsStateAndHasOwnedRetry() {
        val vm=model(); delivery(vm,NotificationDelivery.Background,true); step(vm)
        vm.notificationControls.stopBackground(); assertFalse(vm.notificationControls.backgroundConnection)
        val id=vm.notificationControls.work!!.id; assertEquals(NotificationFailure.ServiceStopped,vm.notificationControls.work!!.failure)
        vm.notificationControls.retry(id); step(vm); assertTrue(vm.notificationControls.backgroundConnection)
    }
    @Test fun previewIsCancellableAndUnavailableWithoutADeviceCapability() {
        val vm=model(); val c=group(vm); val id=vm.notificationControls.preview(c,VibrationChoice.Double)!!
        vm.notificationControls.advancePreview(id,VibrationPreviewPhase.Preparing)
        assertEquals(VibrationPreviewPhase.Unavailable,vm.notificationControls.preview!!.phase)
        vm.notificationControls.chooseEnvironment(NotificationEnvironment(previewAvailable=true))
        val fresh=vm.notificationControls.preview(c,VibrationChoice.Short)!!
        vm.notificationControls.advancePreview(id,VibrationPreviewPhase.Playing)
        assertEquals(VibrationPreviewPhase.Preparing,vm.notificationControls.preview!!.phase)
        vm.notificationControls.advancePreview(fresh,VibrationPreviewPhase.Preparing); assertEquals(VibrationPreviewPhase.Playing,vm.notificationControls.preview!!.phase)
        vm.notificationControls.cancelPreview(); vm.notificationControls.advancePreview(fresh,VibrationPreviewPhase.Playing); assertNull(vm.notificationControls.preview)
    }
    @Test fun staleScreenCallbacksCannotBindTheirChoiceToANewProfile() {
        val vm=model(); val original=vm.uiState.activeProfileId!!
        vm.completeSignIn(OnboardingOrigin.AddProfile); val c=group(vm)
        assertNull(vm.notificationControls.request(NotificationChange.Mute(MuteDuration.Always),c,original))
        assertNull(vm.notificationControls.request(NotificationChange.Delivery(NotificationDelivery.Local,false),expectedProfileId=original))
        assertNull(vm.notificationControls.preview(c,VibrationChoice.Double,original))
        assertTrue(vm.uiState.activeProfile!!.settings.localNotifications); assertNull(vm.chat(c)!!.muteDuration)
    }
    @Test fun unrelatedDraftEditsSurviveNotificationUpdatesAndLostChatsRevokeThem() {
        val vm=model(); val c=group(vm)
        vm.notificationControls.request(NotificationChange.Vibration(VibrationChoice.Double),c)
        vm.updateDraftText(c,"Keep the new draft"); step(vm)
        assertEquals("Keep the new draft",vm.chat(c)!!.draftText); assertEquals(VibrationChoice.Double,vm.chat(c)!!.vibration)
        val id=vm.notificationControls.request(NotificationChange.Mode(NotifyFor.MentionsOnly),c)!!
        assertTrue(vm.leaveChat(c)); vm.notificationControls.advance(id,0); assertNull(vm.notificationControls.work)
    }
    @Test fun failedLocalEnableCannotBeReportedAsAcceptedOnServiceRejection() {
        val vm=model(); val p=vm.uiState.activeProfile!!.copy(settings=ProfileSettings(localNotifications=false))
        val controller=NotificationController({p},{true},{0},{_,_->false},{_,_->false})
        controller.choose(NotificationScenario.ServiceRejected)
        val id=controller.request(NotificationChange.Delivery(NotificationDelivery.Background,true))!!
        controller.advance(id,0); assertEquals(NotificationFailure.Save,controller.work!!.failure)
        assertFalse(p.settings.localNotifications)
    }
    @Test fun backgroundPreferenceIsAppWideAndOnlyAppEraseResetsIt() {
        val vm=model(); val original=vm.uiState.activeProfileId!!
        delivery(vm,NotificationDelivery.Background,true); step(vm)
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertTrue(vm.notificationControls.backgroundConnection)
        vm.updateProfileSettings(vm.uiState.activeProfile!!.settings.copy(localNotifications=false,nativePushNotifications=false))
        assertTrue(NotificationControls.backgroundEnabled(vm.notificationControls.backgroundConnection,true))
        vm.selectProfile(original); vm.signOutActiveProfile(false)
        assertTrue(vm.notificationControls.backgroundConnection)
        assertTrue(vm.eraseAppData(WipeConfirmationPhrase.make(vm.uiState.profiles.map(Profile::id))))
        assertFalse(vm.notificationControls.backgroundConnection)
    }
}

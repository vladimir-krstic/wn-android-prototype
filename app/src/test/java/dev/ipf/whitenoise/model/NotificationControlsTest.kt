package dev.ipf.whitenoise.model

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class NotificationControlsTest {
    private val chat = Chat("chat",0,ChatKind.Direct("person"),"Chat",notifyFor = NotifyFor.MentionsOnly)
    @Test fun permissionAndAvailabilityGateEffectivePreferencesWithoutErasingThem() {
        val settings = ProfileSettings()
        assertFalse(NotificationControls.localEnabled(settings,false))
        assertFalse(NotificationControls.backgroundEnabled(true,false))
        PushAvailability.entries.filter { it != PushAvailability.Available }.forEach { assertFalse(NotificationControls.pushEnabled(settings,true,it)) }
        assertTrue(NotificationControls.pushEnabled(settings,true,PushAvailability.Available))
        assertTrue(settings.nativePushNotifications)
    }
    @Test fun categoryPoliciesKeepPrimaryCustomAndGlobalOnlyCategoriesOutOfChatScope() {
        assertFalse(NotificationCategory.AppUpdates in NotificationCategory.global(false))
        assertTrue(NotificationCategory.AppUpdates in NotificationCategory.global(true))
        assertEquals(NotificationCategory.DirectMessages,NotificationCategory.forChat(chat).first())
        assertEquals(NotificationCategory.GroupMessages,NotificationCategory.forChat(chat.copy(kind=ChatKind.Group)).first())
        assertTrue(NotificationControls.usesCustom(chat,NotificationCategory.DirectMessages))
        assertNull(NotificationControls.scope(chat,NotificationCategory.GroupMembership,true))
        assertNull(NotificationControls.scope(chat,NotificationCategory.DirectMessages,false))
        val custom=NotificationControls.scope(chat,NotificationCategory.Mentions,true)!!
        assertTrue(NotificationControls.usesCustom(custom,NotificationCategory.Mentions))
        assertFalse(NotificationControls.usesCustom(NotificationControls.scope(custom,NotificationCategory.Mentions,false)!!,NotificationCategory.Mentions))
    }
    @Test fun androidOverrideDoesNotRewriteSelectedVibration() {
        val off=NotificationControls.effectiveVibration(VibrationChoice.Double,AndroidVibrationOverride.Off)
        assertFalse(off.enabled); assertEquals(VibrationChoice.Double,off.selected)
        assertNull(NotificationControls.effectiveVibration(VibrationChoice.Short,AndroidVibrationOverride.Custom).pattern)
        val different=NotificationControls.effectiveVibration(VibrationChoice.Short,AndroidVibrationOverride.Long)
        assertTrue(different.overridden); assertEquals(VibrationChoice.Long,different.pattern)
    }
    @Test fun temporaryMuteAndExpiryRetainMentionsChoiceAndOtherChatContent() {
        val original=chat.copy(draftText="Keep",draftReplyMessageId="reply")
        val muted=NotificationControls.mute(original,MuteDuration.OneHour,1000)!!
        assertEquals(3_601_000L,muted.mutedUntilMillis)
        assertEquals(muted,NotificationControls.expire(muted,3_600_999L))
        assertEquals(original,NotificationControls.expire(muted,3_601_000L))
        assertEquals(original,NotificationControls.mute(muted,null,2000))
    }
    @Test fun changingDurationAndAlwaysNeverOverwriteRestoreChoice() {
        val first=NotificationControls.mute(chat,MuteDuration.OneHour,1000)!!
        val longer=NotificationControls.mute(first,MuteDuration.OneWeek,2000)!!
        assertEquals(604_802_000L,longer.mutedUntilMillis)
        val always=NotificationControls.mute(longer,MuteDuration.Always,3000)!!
        assertNull(always.mutedUntilMillis); assertEquals(NotifyFor.MentionsOnly,always.notifyFor)
        assertEquals(always,NotificationControls.expire(always,Long.MAX_VALUE))
    }
    @Test fun customMuteRequiresFutureTimeAndProtectsAgainstOverflow() {
        assertNull(NotificationControls.mute(chat,MuteDuration.Custom,1000))
        assertNull(NotificationControls.mute(chat,MuteDuration.Custom,1000,1000))
        assertNull(NotificationControls.mute(chat,MuteDuration.OneHour,Long.MAX_VALUE))
        val custom=NotificationControls.mute(chat,MuteDuration.Custom,1000,1001)!!
        assertEquals(1001L,custom.mutedUntilMillis)
    }
    @Test fun customCalendarTimeUsesTheSelectedZoneAndRejectsElapsedInstants() {
        val date=LocalDate.of(2026,3,29); val zone=ZoneId.of("Europe/Belgrade")
        val result=NotificationControls.customUntil(date,LocalTime.of(2,30),zone,0)!!
        assertEquals(Instant.parse("2026-03-29T01:30:00Z").toEpochMilli(),result)
        assertNull(NotificationControls.customUntil(date,LocalTime.of(2,30),zone,result))
        assertNotEquals(result,NotificationControls.customUntil(date,LocalTime.of(2,30),ZoneOffset.UTC,0))
    }
}

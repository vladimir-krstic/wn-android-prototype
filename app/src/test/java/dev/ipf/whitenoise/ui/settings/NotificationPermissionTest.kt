package dev.ipf.whitenoise.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionTest {
    @Test
    fun runtimePermissionProjectsFirstRequestAllowedAndBlockedStates() {
        assertEquals(
            NotificationPermissionStatus.NotRequested,
            projectNotificationPermissionStatus(
                runtimePermissionRequired = true,
                permissionGranted = false,
                notificationsEnabled = false,
                userDecisionRecorded = false,
            ),
        )
        assertEquals(
            NotificationPermissionStatus.Allowed,
            projectNotificationPermissionStatus(
                runtimePermissionRequired = true,
                permissionGranted = true,
                notificationsEnabled = true,
                userDecisionRecorded = true,
            ),
        )
        assertEquals(
            NotificationPermissionStatus.Blocked,
            projectNotificationPermissionStatus(
                runtimePermissionRequired = true,
                permissionGranted = false,
                notificationsEnabled = false,
                userDecisionRecorded = true,
            ),
        )
        assertEquals(
            NotificationPermissionStatus.Blocked,
            projectNotificationPermissionStatus(
                runtimePermissionRequired = true,
                permissionGranted = true,
                notificationsEnabled = false,
                userDecisionRecorded = true,
            ),
        )
    }

    @Test
    fun preRuntimePermissionDevicesFollowSystemNotificationAvailability() {
        assertEquals(
            NotificationPermissionStatus.Allowed,
            projectNotificationPermissionStatus(
                runtimePermissionRequired = false,
                permissionGranted = true,
                notificationsEnabled = true,
                userDecisionRecorded = false,
            ),
        )
        assertEquals(
            NotificationPermissionStatus.Blocked,
            projectNotificationPermissionStatus(
                runtimePermissionRequired = false,
                permissionGranted = true,
                notificationsEnabled = false,
                userDecisionRecorded = false,
            ),
        )
    }
}

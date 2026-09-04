package dev.ipf.whitenoise

import dev.ipf.whitenoise.navigation.AppRoute
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowPrivacyPolicyTest {
    private fun secure(
        paused: Boolean = false,
        hideRecents: Boolean = false,
        blockScreenshots: Boolean = false,
        chatSurface: Boolean = false,
        appLock: Boolean = false,
        backgroundShield: Boolean = false,
        requireAuthentication: Boolean = false,
    ) = WindowPrivacyPolicy.shouldSecure(
        paused = paused,
        hideScreenInRecents = hideRecents,
        blockScreenshotsInChats = blockScreenshots,
        chatPrivacySurface = chatSurface,
        appLockProtects = appLock,
        appLockShieldsBackground = backgroundShield,
        requireAuthentication = requireAuthentication,
    )

    @Test
    fun hideRecentsProtectsOnlyWhilePaused() {
        assertFalse(secure(hideRecents = true))
        assertTrue(secure(paused = true, hideRecents = true))
    }

    @Test
    fun screenshotPreferenceProtectsChatSurfacesOnly() {
        assertFalse(secure(blockScreenshots = true))
        assertTrue(secure(blockScreenshots = true, chatSurface = true))
    }

    @Test
    fun lockAndAuthenticationProtectionRemainIndependent() {
        assertTrue(secure(appLock = true))
        assertTrue(secure(backgroundShield = true))
        assertTrue(secure(paused = true, requireAuthentication = true))
    }

    @Test
    fun chatPrivacyRoutesIncludeConversationAndItsDetailSurfaces() {
        assertTrue(isChatPrivacyRoute(AppRoute.Conversation::class.qualifiedName + "/{chatId}"))
        assertTrue(isChatPrivacyRoute(AppRoute.MessageDetails::class.qualifiedName + "/{chatId}/{messageId}"))
        assertTrue(isChatPrivacyRoute(AppRoute.ChatInfo::class.qualifiedName + "/{chatId}"))
        assertFalse(isChatPrivacyRoute(AppRoute.SignedIn::class.qualifiedName))
        assertFalse(isChatPrivacyRoute(AppRoute.PrivacySecurity::class.qualifiedName))
    }
}

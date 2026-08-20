package dev.ipf.whitenoise.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AppRouteTest {
    @Test
    fun welcomeIsTheDeterministicStartDestination() {
        assertEquals(AppRoute.Welcome(), AppRoute.startDestination)
    }

    @Test
    fun groupSetupRetainsStableSelectionOrder() {
        assertEquals(
            listOf("maya-chen", "elias-moreno"),
            AppRoute.GroupSetup(listOf("maya-chen", "elias-moreno")).selectedPersonIds,
        )
    }

    @Test
    fun profileExitCanRequestTheSettingsProfileSwitcher() {
        assertEquals(true, AppRoute.Settings(showProfileSwitcher = true).showProfileSwitcher)
    }

    @Test
    fun conversationDiagnosticsRetainOnlyTheScopedChatArgument() {
        assertEquals("fiatjaf", AppRoute.Diagnostics("fiatjaf").chatId)
        assertEquals("fiatjaf", AppRoute.ConversationDebug("fiatjaf").chatId)
    }
}

package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpAboutPolicyTest {
    @Test
    fun reviewedDestinationsUseExactHttpsHostsAndPaths() {
        HelpExternalDestination.entries.forEach { destination ->
            val target = HelpAboutPolicy.target(destination)
            assertTrue(HelpAboutPolicy.isAllowed(destination, target.url))
        }
        assertEquals(
            "https://github.com/marmot-protocol/whitenoise-android/issues/new/choose",
            HelpAboutPolicy.target(HelpExternalDestination.BugReport).url,
        )
    }

    @Test
    fun destinationValidationRejectsDowngradeAuthorityAndSiblingPaths() {
        listOf(
            "http://github.com/marmot-protocol/whitenoise-android/issues/new/choose",
            "https://user@github.com/marmot-protocol/whitenoise-android/issues/new/choose",
            "https://github.com:443/marmot-protocol/whitenoise-android/issues/new/choose",
            "https://github.example/marmot-protocol/whitenoise-android/issues/new/choose",
            "https://github.com/marmot-protocol/whitenoise-android/issues/news",
        ).forEach { url ->
            assertFalse(HelpAboutPolicy.isAllowed(HelpExternalDestination.BugReport, url))
        }
        assertFalse(
            HelpAboutPolicy.isAllowed(
                HelpExternalDestination.PrivacyPolicy,
                "https://www.whitenoise.chat/privacy-policy",
            ),
        )
    }

    @Test
    fun bugReportHandoffExcludesEverySensitiveAppOwnedCategory() {
        assertEquals(BugReportExcludedData.entries.toSet(), HelpAboutPolicy.bugReportExcludedData)
        assertTrue(BugReportExcludedData.Messages in HelpAboutPolicy.bugReportExcludedData)
        assertTrue(BugReportExcludedData.Keys in HelpAboutPolicy.bugReportExcludedData)
        assertTrue(BugReportExcludedData.DiagnosticLogs in HelpAboutPolicy.bugReportExcludedData)
        assertTrue(BugReportExcludedData.AuditLogs in HelpAboutPolicy.bugReportExcludedData)
    }
}

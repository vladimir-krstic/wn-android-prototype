package dev.ipf.whitenoise

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import dev.ipf.whitenoise.model.HelpExternalDestination
import dev.ipf.whitenoise.ui.settings.AboutLicensesScreen
import dev.ipf.whitenoise.ui.settings.BugReportScreen
import dev.ipf.whitenoise.ui.settings.HelpScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HelpAboutInteractionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun helpOffersBugReportAndAboutAsSeparateDestinations() {
        var bug = false
        var about = false
        compose.setContent {
            WhiteNoiseTheme {
                HelpScreen(onBack = {}, onReportBug = { bug = true }, onAbout = { about = true })
            }
        }
        compose.onNodeWithText("Report a bug").assertIsDisplayed().performClick()
        compose.onNodeWithText("About & licenses").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertTrue(bug)
            assertTrue(about)
        }
    }

    @Test
    fun bugReportReviewNamesExcludedDataBeforeExternalHandoff() {
        val opened = mutableListOf<HelpExternalDestination>()
        compose.setContent {
            WhiteNoiseTheme {
                BugReportScreen(
                    onBack = {},
                    onOpenExternal = { destination -> opened += destination; true },
                )
            }
        }
        compose.onNodeWithText("Nothing is attached").assertIsDisplayed()
        compose.onNodeWithText("messages", substring = true, ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText("keys", substring = true, ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText("diagnostic logs", substring = true, ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithTag("help.bug.open").performClick()
        compose.runOnIdle { assertEquals(listOf(HelpExternalDestination.BugReport), opened) }
    }

    @Test
    fun unavailableBrowserOffersRetryAndCancel() {
        var attempts = 0
        compose.setContent {
            WhiteNoiseTheme {
                BugReportScreen(onBack = {}, onOpenExternal = { attempts++; false })
            }
        }
        compose.onNodeWithTag("help.bug.open").performClick()
        compose.onNodeWithText("Couldn’t open GitHub").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        compose.runOnIdle { assertEquals(2, attempts) }
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Couldn’t open GitHub").assertDoesNotExist()
    }

    @Test
    fun aboutShowsBuildAndRoutesLegalActions() {
        val external = mutableListOf<HelpExternalDestination>()
        var licenses = 0
        compose.setContent {
            WhiteNoiseTheme {
                AboutLicensesScreen(
                    versionName = "0.1",
                    buildNumber = "1",
                    onBack = {},
                    onOpenExternal = { external += it; true },
                    onOpenLicenses = { licenses++; true },
                )
            }
        }
        compose.onNodeWithText("Version").assertIsDisplayed()
        compose.onNodeWithText("0.1").assertIsDisplayed()
        compose.onNodeWithText("Build").assertIsDisplayed()
        compose.onNodeWithTag("settings.list").performScrollToNode(hasText("Open source licenses"))
        compose.onNodeWithText("Open source licenses").performClick()
        compose.onNodeWithText("Privacy policy").performClick()
        compose.runOnIdle {
            assertEquals(1, licenses)
            assertEquals(listOf(HelpExternalDestination.PrivacyPolicy), external)
        }
    }

    @Test
    fun aboutReportsUnavailableLicenseSurface() {
        compose.setContent {
            WhiteNoiseTheme {
                AboutLicensesScreen("0.1", "1", {}, onOpenLicenses = { false })
            }
        }
        compose.onNodeWithText("Open source licenses").performClick()
        compose.onNodeWithText("Couldn’t open licenses").assertIsDisplayed()
    }

    @Test
    fun largeTypeKeepsBugReportActionAndBackReachable() {
        var back = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides androidx.compose.ui.unit.Density(density.density, 2f)) {
                WhiteNoiseTheme { BugReportScreen(onBack = { back = true }, onOpenExternal = { true }) }
            }
        }
        compose.onNodeWithTag("help.bug.open").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle { assertTrue(back) }
    }
}

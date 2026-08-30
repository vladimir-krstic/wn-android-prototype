package dev.ipf.whitenoise

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.navigation.AppRoute
import dev.ipf.whitenoise.state.AppViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationRecoveryTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<EmptyTestActivity>()

    @Test
    fun orphanedSignedInBackStackReturnsProcessLocalStateToWelcome() {
        val viewModel = AppViewModel()
        lateinit var navController: NavHostController
        composeRule.setContent {
            val controller = rememberNavController()
            SideEffect { navController = controller }
            WhiteNoiseApp(navController = controller, appViewModel = viewModel)
        }

        composeRule.onNodeWithText("Sign In").assertIsDisplayed()
        composeRule.runOnIdle { navController.navigate(AppRoute.SignedIn) }
        composeRule.waitUntil {
            navController.currentDestination?.route
                ?.substringBefore('/')
                ?.substringBefore('?') == AppRoute.Welcome::class.qualifiedName
        }

        composeRule.onNodeWithText("Sign In").assertIsDisplayed()
        composeRule.onNodeWithText("Sign Up").assertIsDisplayed()
    }
}

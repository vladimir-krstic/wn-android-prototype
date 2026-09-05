package dev.ipf.whitenoise

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.navigation.*
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.conversation.ChatInfoScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import dev.ipf.whitenoise.model.ProfileFixtures
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupMembersFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    @Test fun previewOpensFullRosterAndBackRestoresGroupInfo() {
        val vm = AppViewModel().apply {
            completeSignIn(OnboardingOrigin.Initial)
            dismissDiagnosticsPrompt(uiState.activeProfileId!!)
        }
        val profile = vm.uiState.activeProfile!!
        val chat = profile.chats.first { it.id == "catalog-group-colors" }
        assertTrue(chat.members.size > 5)
        lateinit var nav: NavHostController
        rule.setContent { nav = rememberNavController(); WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) } }
        rule.runOnIdle { nav.navigate(AppRoute.ChatInfo(chat.id)) }
        rule.onNodeWithTag("chat_info.list").performScrollToNode(hasText("See all"))
        rule.onNodeWithTag("chat_info.member.${chat.members[4].personId}").assertExists()
        rule.onNodeWithTag("chat_info.member.${chat.members[5].personId}").assertDoesNotExist()
        rule.onNodeWithText("See all").performClick()
        rule.onNodeWithTag("group_members.screen").assertExists()
        val lastTag = "chat_info.member.${chat.members.last().personId}"
        rule.onNodeWithTag("group_members.list").performScrollToNode(hasTestTag(lastTag))
        rule.onNodeWithTag(lastTag).assertIsDisplayed()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithTag("chat_info.all_members").assertIsDisplayed()
        rule.onNodeWithText("See all").performClick()
        rule.runOnIdle { vm.completeSignIn(OnboardingOrigin.AddProfile) }
        rule.onNodeWithTag("group_members.screen").assertDoesNotExist()
    }

    @Test fun fiveMembersDoNotNeedAnotherPage() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-group-messages" }
        rule.setContent { WhiteNoiseTheme {
            ChatInfoScreen(profile, chat, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, { true }, {})
        } }
        rule.onNodeWithTag("chat_info.list").performScrollToNode(hasTestTag("chat_info.member.${chat.members.last().personId}"))
        rule.onNodeWithTag("chat_info.member.${chat.members.last().personId}").assertIsDisplayed()
        rule.onNodeWithText("See all").assertDoesNotExist()
    }
}

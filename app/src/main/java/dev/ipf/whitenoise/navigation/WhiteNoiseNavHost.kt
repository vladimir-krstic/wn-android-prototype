package dev.ipf.whitenoise.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.model.GroupRole
import dev.ipf.whitenoise.model.SharedContentCategory
import dev.ipf.whitenoise.model.ProfileExitDestination
import dev.ipf.whitenoise.model.ConversationDebugPolicy
import dev.ipf.whitenoise.ui.chats.ChatsScreen
import dev.ipf.whitenoise.ui.chats.GroupSetupScreen
import dev.ipf.whitenoise.ui.chats.NewChatScreen
import dev.ipf.whitenoise.ui.chats.NewGroupScreen
import dev.ipf.whitenoise.ui.chats.PersonProfileScreen
import dev.ipf.whitenoise.ui.conversation.ConversationScreen
import dev.ipf.whitenoise.ui.conversation.MessageDetailsScreen
import dev.ipf.whitenoise.ui.conversation.ChatInfoScreen
import dev.ipf.whitenoise.ui.conversation.SharedContentScreen
import dev.ipf.whitenoise.ui.conversation.EditGroupScreen
import dev.ipf.whitenoise.ui.conversation.AddGroupMembersScreen
import dev.ipf.whitenoise.ui.conversation.ChatRelaysScreen
import dev.ipf.whitenoise.ui.onboarding.SignInScreen
import dev.ipf.whitenoise.ui.onboarding.SignUpScreen
import dev.ipf.whitenoise.ui.onboarding.WelcomeScreen
import dev.ipf.whitenoise.ui.settings.AppearanceScreen
import dev.ipf.whitenoise.ui.settings.DataUsageScreen
import dev.ipf.whitenoise.ui.settings.DonateScreen
import dev.ipf.whitenoise.ui.settings.EditProfileScreen
import dev.ipf.whitenoise.ui.settings.NotificationsScreen
import dev.ipf.whitenoise.ui.settings.PrivacySecurityScreen
import dev.ipf.whitenoise.ui.settings.ProfileKeysScreen
import dev.ipf.whitenoise.ui.settings.ProfileRelayDetailsScreen
import dev.ipf.whitenoise.ui.settings.ProfileRelaysScreen
import dev.ipf.whitenoise.ui.settings.SettingsScreen
import dev.ipf.whitenoise.ui.settings.ShareConnectScreen
import dev.ipf.whitenoise.ui.settings.SupportScreen
import dev.ipf.whitenoise.ui.settings.ConversationDebugScreen
import dev.ipf.whitenoise.ui.settings.DeveloperToolsScreen
import dev.ipf.whitenoise.ui.settings.DiagnosticsScreen
import dev.ipf.whitenoise.ui.settings.KeyPackagesScreen
import dev.ipf.whitenoise.ui.settings.ManageProfilesScreen

@Composable
fun WhiteNoiseNavHost(
    navController: NavHostController,
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState = appViewModel.uiState

    fun showSignedInRoot() {
        navController.navigate(AppRoute.SignedIn) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun openConversation(chatId: String, clearsCreationFlow: Boolean) {
        appViewModel.openChat(chatId)
        navController.navigate(AppRoute.Conversation(chatId)) {
            if (clearsCreationFlow) popUpTo<AppRoute.SignedIn> { inclusive = false }
            launchSingleTop = true
        }
    }

    fun finishProfileExit(destination: ProfileExitDestination) {
        val route: AppRoute = when (destination) {
            ProfileExitDestination.ProfileSwitcher -> AppRoute.Settings(showProfileSwitcher = true)
            ProfileExitDestination.Welcome -> AppRoute.Welcome()
        }
        navController.navigate(route) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.startDestination,
        modifier = modifier,
    ) {
        composable<AppRoute.Welcome> { entry ->
            val route = entry.toRoute<AppRoute.Welcome>()
            WelcomeScreen(
                origin = route.origin,
                onSignIn = { navController.navigate(AppRoute.SignIn(route.origin)) },
                onSignUp = { navController.navigate(AppRoute.SignUp(route.origin)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<AppRoute.SignIn> { entry ->
            val route = entry.toRoute<AppRoute.SignIn>()
            SignInScreen(
                onBack = { navController.popBackStack() },
                onSignIn = {
                    appViewModel.completeSignIn(route.origin)
                    showSignedInRoot()
                },
            )
        }
        composable<AppRoute.SignUp> { entry ->
            val route = entry.toRoute<AppRoute.SignUp>()
            SignUpScreen(
                initialName = if (route.origin == OnboardingOrigin.Initial) "Marmota" else "Pebble",
                onBack = { navController.popBackStack() },
                onSignUp = { name, about, avatar ->
                    appViewModel.completeSignUp(route.origin, name, about, avatar)
                    showSignedInRoot()
                },
            )
        }
        composable<AppRoute.SignedIn> {
            ChatsScreen(
                uiState = uiState,
                onNewMessage = { navController.navigate(AppRoute.NewChat) },
                onOpenChat = { openConversation(it, clearsCreationFlow = false) },
                onMarkUnread = appViewModel::markChatUnread,
                onTogglePin = appViewModel::toggleChatPin,
                onMute = appViewModel::setChatMute,
                onArchive = appViewModel::setChatArchived,
                onLeave = appViewModel::leaveChat,
                onDelete = { appViewModel.deleteEndedChat(it) },
                onSettings = { navController.navigate(AppRoute.Settings()) },
                onProfileRelays = { navController.navigate(AppRoute.ProfileRelays) },
            )
        }
        composable<AppRoute.Settings> { entry ->
            val route = entry.toRoute<AppRoute.Settings>()
            SettingsScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onSelectProfile = appViewModel::selectProfile,
                onAddProfile = { navController.navigate(AppRoute.Welcome(OnboardingOrigin.AddProfile)) },
                onShareConnect = { navController.navigate(AppRoute.ShareConnect) },
                onEditProfile = { navController.navigate(AppRoute.EditProfile) },
                onProfileKeys = { navController.navigate(AppRoute.ProfileKeys) },
                onNotifications = { navController.navigate(AppRoute.Notifications) },
                onAppearance = { navController.navigate(AppRoute.Appearance) },
                onPrivacy = { navController.navigate(AppRoute.PrivacySecurity) },
                onDataUsage = { navController.navigate(AppRoute.DataUsage) },
                onRelays = { navController.navigate(AppRoute.ProfileRelays) },
                onSupport = { navController.navigate(AppRoute.Support) },
                onDonate = { navController.navigate(AppRoute.Donate) },
                onManageProfiles = { navController.navigate(AppRoute.ManageProfiles) },
                onDeveloperTools = { navController.navigate(AppRoute.DeveloperTools) },
                onSignOut = { wipeData ->
                    appViewModel.signOutActiveProfile(wipeData)?.let(::finishProfileExit)
                },
                initiallyShowSwitcher = route.showProfileSwitcher,
            )
        }
        composable<AppRoute.ShareConnect> {
            uiState.activeProfile?.let { ShareConnectScreen(it, onBack = { navController.popBackStack() }) }
        }
        composable<AppRoute.EditProfile> {
            uiState.activeProfile?.let { profile ->
                EditProfileScreen(
                    profile = profile,
                    onBack = { navController.popBackStack() },
                    onSave = appViewModel::updateActiveProfileDetails,
                    onSaveAddress = appViewModel::updateNostrAddress,
                )
            }
        }
        composable<AppRoute.ProfileKeys> {
            uiState.activeProfile?.let { ProfileKeysScreen(it, onBack = { navController.popBackStack() }) }
        }
        composable<AppRoute.Notifications> {
            uiState.activeProfile?.let { profile ->
                NotificationsScreen(profile, onBack = { navController.popBackStack() }, onChange = appViewModel::updateProfileSettings)
            }
        }
        composable<AppRoute.Appearance> {
            uiState.activeProfile?.let { profile ->
                AppearanceScreen(profile, onBack = { navController.popBackStack() }, onChange = appViewModel::updateProfileSettings)
            }
        }
        composable<AppRoute.PrivacySecurity> {
            uiState.activeProfile?.let { profile ->
                PrivacySecurityScreen(
                    profile = profile,
                    allProfileIds = uiState.profiles.map { it.id },
                    onBack = { navController.popBackStack() },
                    onChange = appViewModel::updateProfileSettings,
                    onEraseAppData = { confirmation ->
                        if (appViewModel.eraseAppData(confirmation)) finishProfileExit(ProfileExitDestination.Welcome)
                    },
                )
            }
        }
        composable<AppRoute.DataUsage> {
            uiState.activeProfile?.let { profile ->
                DataUsageScreen(profile, onBack = { navController.popBackStack() }, onChange = appViewModel::updateProfileSettings)
            }
        }
        composable<AppRoute.ProfileRelays> {
            uiState.activeProfile?.let { profile ->
                ProfileRelaysScreen(
                    profile = profile,
                    onBack = { navController.popBackStack() },
                    onRelay = { navController.navigate(AppRoute.ProfileRelayDetails(it)) },
                    onAdd = { value, roles -> appViewModel.addProfileRelay(value, roles) },
                    onConnected = {
                        appViewModel.setProfileRelayConnectionStatus(
                            it,
                            dev.ipf.whitenoise.model.RelayConnectionStatus.Connected,
                        )
                    },
                    onRestore = appViewModel::restoreProfileRelays,
                )
            }
        }
        composable<AppRoute.ProfileRelayDetails> { entry ->
            val route = entry.toRoute<AppRoute.ProfileRelayDetails>()
            uiState.activeProfile?.settings?.relays?.firstOrNull { it.id == route.relayId }?.let { relay ->
                ProfileRelayDetailsScreen(
                    relay = relay,
                    onBack = { navController.popBackStack() },
                    onSetRole = { role, enabled -> appViewModel.setProfileRelayRole(relay.id, role, enabled) },
                    onRemove = { appViewModel.removeProfileRelay(relay.id) },
                )
            }
        }
        composable<AppRoute.Support> {
            uiState.activeProfile?.let { profile ->
                SupportScreen(
                    profile = profile,
                    onBack = { navController.popBackStack() },
                    onStart = {
                        appViewModel.openOrCreateSupportChat()?.let {
                            openConversation(it, clearsCreationFlow = false)
                        }
                    },
                    onRelays = { navController.navigate(AppRoute.ProfileRelays) },
                )
            }
        }
        composable<AppRoute.Donate> {
            DonateScreen(onBack = { navController.popBackStack() })
        }
        composable<AppRoute.ManageProfiles> {
            uiState.activeProfile?.let { profile ->
                ManageProfilesScreen(
                    profiles = uiState.profiles,
                    activeProfileId = profile.id,
                    onBack = { navController.popBackStack() },
                    onRemove = appViewModel::removeStoredProfile,
                )
            }
        }
        composable<AppRoute.DeveloperTools> {
            uiState.activeProfile?.let { profile ->
                DeveloperToolsScreen(
                    profile = profile,
                    onBack = { navController.popBackStack() },
                    onEnabled = appViewModel::setDeveloperToolsEnabled,
                    onDebugMode = appViewModel::setDebugMode,
                    onDiagnostics = { navController.navigate(AppRoute.Diagnostics()) },
                    onKeyPackages = { navController.navigate(AppRoute.KeyPackages) },
                    onTelemetry = appViewModel::setAnonymousTelemetry,
                    onAuditLogging = appViewModel::setAuditLogging,
                    onClearAuditLogs = appViewModel::clearAuditLogs,
                )
            }
        }
        composable<AppRoute.Diagnostics> { entry ->
            val route = entry.toRoute<AppRoute.Diagnostics>()
            uiState.activeProfile?.let { profile ->
                DiagnosticsScreen(
                    profile = profile,
                    diagnosticSummary = route.chatId?.let(appViewModel::conversationDebugSnapshot)?.diagnosticSummary,
                    onBack = { navController.popBackStack() },
                    onTest = appViewModel::runDiagnosticTest,
                    onClear = appViewModel::clearDiagnosticEvents,
                )
            }
        }
        composable<AppRoute.KeyPackages> {
            uiState.activeProfile?.let { profile ->
                KeyPackagesScreen(
                    profile = profile,
                    onBack = { navController.popBackStack() },
                    onPublish = appViewModel::publishKeyPackage,
                )
            }
        }
        composable<AppRoute.ConversationDebug> { entry ->
            val route = entry.toRoute<AppRoute.ConversationDebug>()
            uiState.activeProfile?.let { profile ->
                ConversationDebugScreen(
                    profile = profile,
                    chat = appViewModel.chat(route.chatId),
                    snapshot = appViewModel.conversationDebugSnapshot(route.chatId),
                    onBack = { navController.popBackStack() },
                    onOpenDeveloperTools = { navController.navigate(AppRoute.DeveloperTools) },
                    onDiagnostics = { navController.navigate(AppRoute.Diagnostics(route.chatId)) },
                )
            }
        }
        composable<AppRoute.NewChat> {
            uiState.activeProfile?.let { profile ->
                NewChatScreen(
                    profile = profile,
                    onBack = { navController.popBackStack() },
                    onNewGroup = { navController.navigate(AppRoute.NewGroup) },
                    onPerson = { navController.navigate(AppRoute.PersonProfile(it)) },
                )
            }
        }
        composable<AppRoute.PersonProfile> { entry ->
            val route = entry.toRoute<AppRoute.PersonProfile>()
            val profile = uiState.activeProfile
            val person = appViewModel.person(route.personId)
            if (profile != null && person != null) {
                val contextChat = route.chatId?.let(appViewModel::chat)
                val member = contextChat?.members?.firstOrNull { it.personId == person.id }
                val actorIsAdmin = contextChat?.members?.firstOrNull { it.personId == profile.id }?.role == GroupRole.Admin
                PersonProfileScreen(
                    profile = profile,
                    person = person,
                    onBack = { navController.popBackStack() },
                    onMessage = {
                        val chatId = appViewModel.openOrCreateDirectChat(person.id)
                        if (chatId != null) openConversation(chatId, clearsCreationFlow = true)
                        chatId != null
                    },
                    onToggleFollow = { appViewModel.toggleFollowing(person.id) },
                    onToggleBlock = { appViewModel.toggleBlocked(person.id) },
                    showMessageAction = contextChat == null,
                    groupRole = member?.role,
                    canManageGroup = contextChat?.isGroup == true && actorIsAdmin && person.id != profile.id,
                    onToggleAdmin = {
                        val chat = contextChat ?: return@PersonProfileScreen false
                        appViewModel.setGroupMemberAdmin(chat.id, person.id, member?.role != GroupRole.Admin)
                    },
                    onRemoveFromGroup = {
                        val chat = contextChat ?: return@PersonProfileScreen false
                        appViewModel.removeGroupMember(chat.id, person.id)
                    },
                    onOpenRelays = { navController.navigate(AppRoute.ProfileRelays) },
                )
            }
        }
        composable<AppRoute.NewGroup> {
            uiState.activeProfile?.let { profile ->
                NewGroupScreen(
                    profile = profile,
                    onBack = { navController.popBackStack() },
                    onContinue = { navController.navigate(AppRoute.GroupSetup(it)) },
                )
            }
        }
        composable<AppRoute.GroupSetup> { entry ->
            val route = entry.toRoute<AppRoute.GroupSetup>()
            uiState.activeProfile?.let { profile ->
                GroupSetupScreen(
                    profile = profile,
                    selectedPersonIds = route.selectedPersonIds,
                    onBack = { navController.popBackStack() },
                    onCreate = { name, description, avatar ->
                        val chatId = appViewModel.createGroup(
                            name = name,
                            description = description,
                            avatar = avatar,
                            selectedPersonIds = route.selectedPersonIds,
                        )
                        if (chatId != null) openConversation(chatId, clearsCreationFlow = true)
                        chatId != null
                    },
                    onOpenRelays = { navController.navigate(AppRoute.ProfileRelays) },
                )
            }
        }
        composable<AppRoute.Conversation> { entry ->
            val route = entry.toRoute<AppRoute.Conversation>()
            appViewModel.chat(route.chatId)?.let { chat ->
                val profile = uiState.activeProfile ?: return@let
                ConversationScreen(
                    profile = profile,
                    chat = chat,
                    onBack = { navController.popBackStack() },
                    onSend = { appViewModel.sendText(chat.id, it) },
                    onRetry = { appViewModel.retryMessage(chat.id, it) },
                    onAcceptInvitation = { appViewModel.acceptInvitation(chat.id) },
                    onDeclineInvitation = {
                        if (appViewModel.declineInvitation(chat.id)) {
                            navController.popBackStack<AppRoute.SignedIn>(inclusive = false)
                        }
                    },
                    onDraftTextChanged = { appViewModel.updateDraftText(chat.id, it) },
                    onAddDraftAttachments = { appViewModel.addDraftAttachments(chat.id, it) },
                    onRemoveDraftAttachment = { appViewModel.removeDraftAttachment(chat.id, it) },
                    onSuppressDraftLink = { appViewModel.suppressDraftLink(chat.id, it) },
                    onCancelDraftReply = { appViewModel.cancelDraftReply(chat.id) },
                    onSendDraft = { appViewModel.sendDraft(chat.id) },
                    onSendVoice = { format, transcript ->
                        appViewModel.sendVoice(chat.id, format, transcript)
                    },
                    onReply = { appViewModel.setDraftReply(chat.id, it) },
                    onReaction = { messageId, emoji, remove ->
                        appViewModel.setMessageReaction(chat.id, messageId, emoji, remove)
                    },
                    onQuickReactionsChanged = appViewModel::setQuickReactions,
                    onDeleteMessages = { ids, scope -> appViewModel.deleteMessages(chat.id, ids, scope) },
                    onForwardMessages = { ids, targets -> appViewModel.forwardMessages(chat.id, ids, targets) },
                    onOpenMessageDetails = { messageId ->
                        navController.navigate(AppRoute.MessageDetails(chat.id, messageId))
                    },
                    onOpenChatInfo = { navController.navigate(AppRoute.ChatInfo(chat.id)) },
                    onOpenDeveloperTools = if (ConversationDebugPolicy.showsToolbarAction(profile, chat.id)) {
                        { navController.navigate(AppRoute.ConversationDebug(chat.id)) }
                    } else {
                        null
                    },
                    initialSearch = route.openSearch,
                )
            }
        }
        composable<AppRoute.ChatInfo> { entry ->
            val route = entry.toRoute<AppRoute.ChatInfo>()
            val profile = uiState.activeProfile
            val chat = appViewModel.chat(route.chatId)
            if (profile != null && chat != null) {
                ChatInfoScreen(
                    profile = profile,
                    chat = chat,
                    onBack = { navController.popBackStack() },
                    onAbout = { navController.navigate(AppRoute.PersonProfile(it, chat.id)) },
                    onMember = { navController.navigate(AppRoute.PersonProfile(it, chat.id)) },
                    onSharedContent = { category ->
                        navController.navigate(AppRoute.SharedContent(chat.id, category.name))
                    },
                    onRelays = { navController.navigate(AppRoute.ChatRelays(chat.id)) },
                    onSearch = {
                        navController.navigate(AppRoute.Conversation(chat.id, openSearch = true)) {
                            popUpTo<AppRoute.Conversation> { inclusive = true }
                        }
                    },
                    onEditGroup = { navController.navigate(AppRoute.EditGroup(chat.id)) },
                    onAddPeople = { navController.navigate(AppRoute.AddGroupMembers(chat.id)) },
                    onMute = { appViewModel.setChatMute(chat.id, it) },
                    onDisappearing = { appViewModel.setChatDisappearing(chat.id, it) },
                    onArchive = { appViewModel.setChatArchived(chat.id, !chat.isArchived) },
                    onLeave = { appViewModel.leaveChat(chat.id) },
                )
            }
        }
        composable<AppRoute.SharedContent> { entry ->
            val route = entry.toRoute<AppRoute.SharedContent>()
            val profile = uiState.activeProfile
            val chat = appViewModel.chat(route.chatId)
            val category = runCatching { SharedContentCategory.valueOf(route.category) }.getOrNull()
            if (profile != null && chat != null && category != null) {
                SharedContentScreen(profile, chat, category, onBack = { navController.popBackStack() })
            }
        }
        composable<AppRoute.EditGroup> { entry ->
            val route = entry.toRoute<AppRoute.EditGroup>()
            appViewModel.chat(route.chatId)?.let { chat ->
                EditGroupScreen(
                    chat = chat,
                    onBack = { navController.popBackStack() },
                    onSave = { name, description, avatar -> appViewModel.editGroup(chat.id, name, description, avatar) },
                )
            }
        }
        composable<AppRoute.AddGroupMembers> { entry ->
            val route = entry.toRoute<AppRoute.AddGroupMembers>()
            val profile = uiState.activeProfile
            val chat = appViewModel.chat(route.chatId)
            if (profile != null && chat != null) {
                AddGroupMembersScreen(
                    profile,
                    chat,
                    onBack = { navController.popBackStack() },
                    onAdd = { appViewModel.addGroupMembers(chat.id, it) },
                )
            }
        }
        composable<AppRoute.ChatRelays> { entry ->
            val route = entry.toRoute<AppRoute.ChatRelays>()
            appViewModel.chat(route.chatId)?.let { chat ->
                ChatRelaysScreen(
                    chat = chat,
                    onBack = { navController.popBackStack() },
                    onAdd = { appViewModel.addChatRelay(chat.id, it) },
                    onRemove = { appViewModel.removeChatRelay(chat.id, it) },
                    onRestore = { appViewModel.restoreChatRelays(chat.id) },
                )
            }
        }
        composable<AppRoute.MessageDetails> { entry ->
            val route = entry.toRoute<AppRoute.MessageDetails>()
            val profile = uiState.activeProfile
            val chat = appViewModel.chat(route.chatId)
            val message = appViewModel.message(route.chatId, route.messageId)
            if (profile != null && chat != null && message != null) {
                MessageDetailsScreen(
                    profile = profile,
                    chat = chat,
                    message = message,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

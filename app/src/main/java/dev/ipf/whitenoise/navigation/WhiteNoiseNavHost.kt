package dev.ipf.whitenoise.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.model.GroupRole
import dev.ipf.whitenoise.model.AccessMethod
import dev.ipf.whitenoise.model.AccessPhase
import kotlinx.coroutines.delay
import dev.ipf.whitenoise.model.SharedContentCategory
import dev.ipf.whitenoise.model.ProfileExitDestination
import dev.ipf.whitenoise.model.ConversationDebugPolicy
import dev.ipf.whitenoise.ui.chats.CreatedChatOpenDialog
import dev.ipf.whitenoise.ui.chats.ChatsScreen
import dev.ipf.whitenoise.ui.chats.GroupSetupScreen
import dev.ipf.whitenoise.ui.chats.GroupsInCommonScreen
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
import dev.ipf.whitenoise.ui.onboarding.PrivateKeyQrScannerSheet
import dev.ipf.whitenoise.ui.onboarding.SignInScreen
import dev.ipf.whitenoise.ui.onboarding.SignUpScreen
import dev.ipf.whitenoise.ui.onboarding.WelcomeScreen
import dev.ipf.whitenoise.ui.settings.AppearanceScreen
import dev.ipf.whitenoise.ui.settings.LanguageScreen
import dev.ipf.whitenoise.ui.settings.DataUsageScreen
import dev.ipf.whitenoise.ui.settings.DonateScreen
import dev.ipf.whitenoise.ui.settings.EditProfileScreen
import dev.ipf.whitenoise.ui.settings.NotificationsScreen
import dev.ipf.whitenoise.ui.settings.PrivacySecurityScreen
import dev.ipf.whitenoise.ui.settings.ProfileKeysScreen
import dev.ipf.whitenoise.ui.settings.ProfileRelayDetailsScreen
import dev.ipf.whitenoise.ui.settings.ProfileRelaysScreen
import dev.ipf.whitenoise.ui.settings.ChatFoldersScreen
import dev.ipf.whitenoise.ui.settings.ChatFolderEditScreen
import dev.ipf.whitenoise.ui.settings.SettingsScreen
import dev.ipf.whitenoise.ui.settings.ShareConnectScreen
import dev.ipf.whitenoise.ui.settings.SupportScreen
import dev.ipf.whitenoise.ui.settings.ConversationDebugScreen
import dev.ipf.whitenoise.ui.settings.DeveloperToolsScreen
import dev.ipf.whitenoise.ui.settings.DiagnosticsScreen
import dev.ipf.whitenoise.ui.settings.KeyPackagesScreen
import dev.ipf.whitenoise.ui.settings.ManageProfilesScreen
import dev.ipf.whitenoise.ui.settings.DiagnosticsImprovementsScreen
import dev.ipf.whitenoise.ui.settings.DiagnosticsPromptHost
import dev.ipf.whitenoise.ui.settings.ProfileExitReportDialog
import dev.ipf.whitenoise.ui.conversation.MessageOperationsHost

@Composable
fun WhiteNoiseNavHost(
    navController: NavHostController,
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState = appViewModel.uiState
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val signInPrivateKey = remember { TextFieldState() }
    var scannedPrivateKey by remember { mutableStateOf<String?>(null) }
    var scannerUnavailable by remember { mutableStateOf(false) }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(uiState.activeProfileId, currentBackStackEntry?.id) {
        val entry = currentBackStackEntry ?: return@LaunchedEffect
        appViewModel.reconcileCreatedChatOrigin(entry.id)
        if (entry.destination.route != AppRoute.SignedIn::class.qualifiedName) appViewModel.dismissChatBatch()
        if (entry.destination.route != AppRoute.EditProfile::class.qualifiedName) appViewModel.cancelProfileSave()
    }
    LaunchedEffect(uiState.activeProfileId, currentBackStackEntry?.destination?.route) {
        val destination = currentBackStackEntry?.destination ?: return@LaunchedEffect
        val routeName = destination.route?.substringBefore('/')?.substringBefore('?')
        val isOnboarding = routeName in onboardingRouteNames
        if (routeName != AppRoute.SignIn::class.qualifiedName) {
            signInPrivateKey.edit { replace(0, length, "") }
            scannedPrivateKey = null
        }
        if (uiState.activeProfile == null && !isOnboarding) {
            navController.navigate(AppRoute.Welcome()) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun returnFromOnboardingForm() {
        appViewModel.cancelAccess()
        signInPrivateKey.edit { replace(0, length, "") }
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        navController.popBackStack()
    }

    fun showSignedInRoot() {
        signInPrivateKey.edit { replace(0, length, "") }
        scannedPrivateKey = null
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
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

    val accessAttempt = appViewModel.accessAttempt
    BackHandler(enabled = accessAttempt != null && accessAttempt.phase != AccessPhase.RecoveryConsent) {
        appViewModel.cancelAccess()
    }
    LaunchedEffect(accessAttempt?.id, accessAttempt?.phase, currentBackStackEntry) {
        val attempt = accessAttempt ?: return@LaunchedEffect
        val entry = currentBackStackEntry ?: return@LaunchedEffect
        val expectedRoute = when (attempt.method) {
            AccessMethod.Retained -> AppRoute.Welcome::class.qualifiedName
            AccessMethod.CreateProfile -> AppRoute.SignUp::class.qualifiedName
            AccessMethod.PrivateKey, AccessMethod.Amber -> AppRoute.SignIn::class.qualifiedName
        }
        val actualRoute = entry.destination.route?.substringBefore('/')?.substringBefore('?')
        if (actualRoute != expectedRoute) {
            appViewModel.cancelAccess()
            return@LaunchedEffect
        }
        if (attempt.phase.isBusy) entry.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(2_000)
            if (appViewModel.advanceAccess(attempt.id, attempt.phase)) showSignedInRoot()
        }
    }

    appViewModel.profileExitReport?.let { ProfileExitReportDialog(it, appViewModel::dismissProfileExitReport) }
    appViewModel.createdChatOpen?.takeIf { appViewModel.createdChatProjectionUnavailable }?.let { request ->
        CreatedChatOpenDialog(
            onOpen = {
                appViewModel.completeCreatedChatOpen(request.id, currentBackStackEntry?.id.orEmpty())?.let {
                    openConversation(it, clearsCreationFlow = true)
                }
            },
            onDismiss = {
                appViewModel.cancelCreatedChatOpen()
                navController.navigate(AppRoute.SignedIn) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )
    }

    uiState.activeProfile?.let { profile ->
        profile.chats.forEach { chat -> androidx.compose.runtime.key(profile.id, chat.id) {
            androidx.compose.runtime.CompositionLocalProvider(dev.ipf.whitenoise.ui.conversation.LocalAttachmentEnvironment provides
                dev.ipf.whitenoise.ui.conversation.AttachmentEnvironment(transfer = { messageId, attachmentId, action, revision ->
                    appViewModel.attachmentTransferAction(profile.id, chat.id, messageId, attachmentId, action, revision)
                })) { dev.ipf.whitenoise.ui.conversation.AttachmentTransferHost(chat) }
        } }
    }

    MessageOperationsHost(
        profile = uiState.activeProfile, forward = uiState.activeProfileId?.let { appViewModel.messageForwards[it] },
        onAdvanceForward = { id, revision -> uiState.activeProfileId?.let { appViewModel.advanceMessageForward(it, id, revision) } },
        onAdvanceDelete = { chat, id, revision -> uiState.activeProfileId?.let { appViewModel.advanceMessageDeletion(it, chat, id, revision) } },
        onRetry = { id -> uiState.activeProfileId?.let { appViewModel.retryMessageForward(it, id) } },
        onCancel = { id -> uiState.activeProfileId?.let { appViewModel.cancelMessageForward(it, id) } },
        onDismiss = { id -> uiState.activeProfileId?.let { appViewModel.dismissMessageForward(it, id) } },
        modifier = modifier,
        onAutomaticRetry = { id, revision -> uiState.activeProfileId?.let { appViewModel.retryMessageForward(it, id, automatic = true, expectedRevision = revision) } },
    ) { operationModifier ->
    NavHost(
        navController = navController,
        startDestination = AppRoute.startDestination,
        modifier = operationModifier,
    ) {
        composable<AppRoute.Welcome> { entry ->
            val route = entry.toRoute<AppRoute.Welcome>()
            WelcomeScreen(
                origin = route.origin,
                onSignIn = {
                    appViewModel.cancelAccess()
                    signInPrivateKey.edit { replace(0, length, "") }
                    scannedPrivateKey = null
                    scannerUnavailable = false
                    navController.navigate(AppRoute.SignIn(route.origin))
                },
                onSignUp = { appViewModel.cancelAccess(); navController.navigate(AppRoute.SignUp(route.origin)) },
                onBack = ::returnFromOnboardingForm,
                retainedProfiles = uiState.retainedProfiles,
                attempt = accessAttempt,
                onContinueProfile = { appViewModel.beginRetainedSignIn(route.origin, it) },
                onRetry = appViewModel::retryAccess,
                onRecover = appViewModel::confirmAccessRecovery,
                onCancel = appViewModel::cancelAccess,
            )
        }
        composable<AppRoute.SignIn> { entry ->
            val route = entry.toRoute<AppRoute.SignIn>()
            var scannerOpen by rememberSaveable { mutableStateOf(false) }
            SignInScreen(
                onBack = ::returnFromOnboardingForm,
                onScan = { scannerOpen = true },
                privateKey = signInPrivateKey,
                scannedPrivateKey = scannedPrivateKey,
                scannerUnavailable = scannerUnavailable,
                onScannedPrivateKeyConsumed = {
                    scannedPrivateKey = null
                },
                onScannerUnavailableConsumed = {
                    scannerUnavailable = false
                },
                onSignIn = {
                    appViewModel.beginPrivateKeySignIn(route.origin, signInPrivateKey.text.toString())
                },
                onAmberSignIn = { appViewModel.beginAmberSignIn(route.origin) },
                attempt = accessAttempt,
                onRetry = appViewModel::retryAccess,
                onRecover = appViewModel::confirmAccessRecovery,
                onCancel = appViewModel::cancelAccess,
            )

            if (scannerOpen) {
                PrivateKeyQrScannerSheet(
                    onDismiss = { scannerOpen = false },
                    onCodeScanned = { payload ->
                        scannerOpen = false
                        scannedPrivateKey = payload
                    },
                    onUnavailable = {
                        scannerOpen = false
                        scannerUnavailable = true
                    },
                )
            }
        }
        composable<AppRoute.SignUp> { entry ->
            val route = entry.toRoute<AppRoute.SignUp>()
            SignUpScreen(
                initialName = if (route.origin == OnboardingOrigin.Initial) "Marmota" else "Pebble",
                onBack = ::returnFromOnboardingForm,
                onSignUp = { name, about, avatar ->
                    appViewModel.beginProfileCreation(route.origin, name, about, avatar)
                },
                attempt = accessAttempt,
                onRetry = appViewModel::retryAccess,
                onRecover = appViewModel::confirmAccessRecovery,
                onCancel = appViewModel::cancelAccess,
            )
        }
        composable<AppRoute.SignedIn> { entry ->
            val lifecycleState by entry.lifecycle.currentStateFlow.collectAsState()
            ChatsScreen(
                uiState = uiState,
                onNewMessage = { navController.navigate(AppRoute.NewChat) },
                onOpenChat = { openConversation(it, clearsCreationFlow = false) },
                onOpenSearchMessage = { chatId, messageId ->
                    val owner = uiState.activeProfileId
                    if (owner != null && appViewModel.openGlobalSearchMessage(owner, chatId, messageId)) {
                        navController.navigate(AppRoute.Conversation(chatId, targetMessageId = messageId)); true
                    } else false
                },
                onOpenSearchPerson = { person -> uiState.activeProfileId?.let { owner ->
                    appViewModel.acceptDiscoveredPerson(owner, person)?.let { navController.navigate(AppRoute.PersonProfile(it)) }
                } },
                peopleScenario = appViewModel.peopleSearchScenario,
                onVoiceScenario = { uiState.activeProfileId?.let(appViewModel::consumeGlobalVoiceScenario) ?: dev.ipf.whitenoise.model.GlobalVoiceScenario.Unavailable },
                onMarkUnread = appViewModel::markChatUnread,
                onTogglePin = appViewModel::toggleChatPin,
                onMute = appViewModel::setChatMute,
                onArchive = appViewModel::setChatArchived,
                onLeave = appViewModel::leaveChat,
                onDelete = { appViewModel.deleteEndedChat(it) },
                onSettings = { navController.navigate(AppRoute.Settings()) },
                onProfileRelays = { navController.navigate(AppRoute.ProfileRelays) },
                onUndo = appViewModel::undoChatListAction,
                onFolders = { uiState.activeProfileId?.let { navController.navigate(AppRoute.Folders(it)) } },
                onMovePin = { id, delta -> uiState.activeProfileId?.let { appViewModel.movePinnedChat(it, id, delta) } },
                onCreateFolder = { name -> uiState.activeProfileId?.let { appViewModel.createChatFolder(it, name) } },
                onBeginBatch = { ids, action, folder, leave -> uiState.activeProfileId?.let { appViewModel.beginChatBatch(it, ids, action, folder, leave) } ?: false },
                batchAttempt = appViewModel.chatBatchAttempt,
                onAdvanceBatch = appViewModel::advanceChatBatch,
                onRetryBatch = { appViewModel.retryChatBatch() },
                onDismissBatch = appViewModel::dismissChatBatch,
                onOpenGroup = { navController.navigate(AppRoute.ChatInfo(it)) },
                onRetryConnection = { uiState.activeProfileId?.let(appViewModel::retryChatConnection) },
                onAdvanceConnection = appViewModel::advanceChatConnection,
            )
            DiagnosticsPromptHost(
                uiState = uiState,
                chatsResumed = lifecycleState == Lifecycle.State.RESUMED,
                onAnalytics = { id, enabled -> appViewModel.setAnalyticsEnabled(id, enabled) },
                onLogging = { id, enabled -> appViewModel.setDiagnosticLoggingEnabled(id, enabled) },
                onDismiss = appViewModel::dismissDiagnosticsPrompt,
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
                onDeveloperTools = { navController.navigate(AppRoute.DeveloperTools) },
                onSignOut = { appViewModel.beginProfileExit(it) },
                exitAttempt = appViewModel.profileExitAttempt,
                onAdvanceExit = { id, step -> appViewModel.advanceProfileExit(id, step)?.let(::finishProfileExit) },
                onRetryExit = appViewModel::retryProfileExit,
                onDismissExit = appViewModel::dismissProfileExit,
                onFolders = { uiState.activeProfileId?.let { navController.navigate(AppRoute.Folders(it)) } },
                initiallyShowSwitcher = route.showProfileSwitcher,
            )
        }
        composable<AppRoute.Folders> { entry ->
            val route = entry.toRoute<AppRoute.Folders>()
            val profile = uiState.activeProfile?.takeIf { it.id == route.profileId }
            if (profile == null) LaunchedEffect(route.profileId) { showSignedInRoot() }
            else ChatFoldersScreen(profile, onBack = { navController.popBackStack() },
                onCreate = { navController.navigate(AppRoute.EditFolder(profile.id)) },
                onEdit = { navController.navigate(AppRoute.EditFolder(profile.id, it)) },
                onMove = { id, delta -> appViewModel.moveChatFolder(profile.id, id, delta) },
                onDelete = { appViewModel.deleteChatFolder(profile.id, it) },
                onRestore = { appViewModel.restoreChatFolders(profile.id) })
        }
        composable<AppRoute.EditFolder> { entry ->
            val route = entry.toRoute<AppRoute.EditFolder>()
            val profile = uiState.activeProfile?.takeIf { it.id == route.profileId }
            if (profile == null) LaunchedEffect(route.profileId) { showSignedInRoot() }
            else ChatFolderEditScreen(profile, route.folderId, onBack = { navController.popBackStack() },
                onSave = { appViewModel.saveChatFolder(profile.id, route.folderId, it) != null })
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
                    onSaveDraft = { appViewModel.beginProfileSave(profile.id, it) },
                    saveAttempt = appViewModel.profileSaveAttempt,
                    onAdvanceSave = appViewModel::advanceProfileSave,
                    onCancelSave = { appViewModel.cancelProfileSave(profile.id) },
                    consumeImageFailure = { appViewModel.consumeProfileImageFailure(profile.id) },
                    retainedImages = appViewModel.profileImageDraft,
                    onRetainImages = { avatar, banner -> appViewModel.retainProfileImages(profile.id, avatar, banner) },
                )
            }
        }
        composable<AppRoute.ProfileKeys> {
            uiState.activeProfile?.let { profile -> ProfileKeysScreen(
                profile, onBack = { navController.popBackStack() },
                onRetryKey = { appViewModel.retryLocalKeyAccess(profile.id) },
            ) }
        }
        composable<AppRoute.Notifications> {
            uiState.activeProfile?.let { profile ->
                NotificationsScreen(profile, onBack = { navController.popBackStack() }, onChange = appViewModel::updateProfileSettings)
            }
        }
        composable<AppRoute.Appearance> {
            uiState.activeProfile?.let { profile ->
                AppearanceScreen(
                    profile = profile,
                    onBack = { navController.popBackStack() },
                    onChange = appViewModel::updateProfileSettings,
                    onLanguage = { navController.navigate(AppRoute.Language) },
                )
            }
        }
        composable<AppRoute.Language> {
            uiState.activeProfile?.let { profile ->
                LanguageScreen(
                    profile = profile,
                    onBack = { navController.popBackStack() },
                    onChange = appViewModel::updateProfileSettings,
                )
            }
        }
        composable<AppRoute.PrivacySecurity> {
            uiState.activeProfile?.let { profile ->
                PrivacySecurityScreen(
                    profile = profile,
                    allProfileIds = uiState.profiles.map { it.id },
                    onBack = { navController.popBackStack() },
                    onChange = appViewModel::updateProfileSettings,
                    onDiagnosticsImprovements = { navController.navigate(AppRoute.DiagnosticsImprovements) },
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
        composable<AppRoute.DiagnosticsImprovements> {
            uiState.activeProfile?.let { profile ->
                DiagnosticsImprovementsScreen(
                    profile,
                    onBack = { navController.popBackStack() },
                    onAnalytics = { appViewModel.setAnalyticsEnabled(profile.id, it) },
                    onLogging = { appViewModel.setDiagnosticLoggingEnabled(profile.id, it) },
                    onClear = { appViewModel.clearDiagnosticRecords(profile.id) },
                )
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
                    accessScenario = appViewModel.nextAccessScenario,
                    onAccessScenario = appViewModel::selectAccessScenario,
                    onStartupFailure = appViewModel::previewStartupFailure,
                    peopleSearchScenario = appViewModel.peopleSearchScenario,
                    onPeopleSearchScenario = appViewModel::selectPeopleSearchScenario,
                    groupContactScenario = appViewModel.groupContactScenario,
                    onGroupContactScenario = appViewModel::selectGroupContactScenario,
                    createdChatUnavailable = appViewModel.nextCreatedChatUnavailable,
                    onCreatedChatUnavailable = appViewModel::setCreatedChatUnavailable,
                    historyScenario = appViewModel.nextHistoryScenario,
                    onHistoryScenario = appViewModel::selectHistoryScenario,
                    messageEditScenario = appViewModel.nextMessageEditScenario,
                    onMessageEditScenario = appViewModel::selectMessageEditScenario,
                    messageDeleteScenario = appViewModel.nextMessageDeleteScenario,
                    onMessageDeleteScenario = appViewModel::selectMessageDeleteScenario,
                    messageForwardScenario = appViewModel.nextMessageForwardScenario,
                    recentMediaAccess = appViewModel.recentMediaAccess,
                    onRecentMediaAccess = appViewModel::selectRecentMediaAccess,
                    attachmentTransferScenario = appViewModel.attachmentTransferScenario,
                    onAttachmentTransferScenario = appViewModel::selectAttachmentTransferScenario,
                    photoEditorScenario = appViewModel.nextPhotoEditorScenario,
                    onPhotoEditorScenario = appViewModel::selectPhotoEditorScenario,
                    attachmentAccessScenario = appViewModel.nextAttachmentAccessScenario,
                    onAttachmentAccessScenario = appViewModel::selectAttachmentAccessScenario,
                    onMessageForwardScenario = appViewModel::selectMessageForwardScenario,
                    globalVoiceScenario = appViewModel.nextGlobalVoiceScenario,
                    onGlobalVoiceScenario = appViewModel::selectGlobalVoiceScenario,
                    chatBatchScenario = appViewModel.nextChatBatchScenario,
                    onChatBatchScenario = appViewModel::selectChatBatchScenario,
                    onChatConnectionScenario = appViewModel::selectChatConnectionScenario,
                    profileSaveScenario = appViewModel.nextProfileSaveScenario,
                    onProfileSaveScenario = appViewModel::selectProfileSaveScenario,
                    profileImageFails = appViewModel.nextProfileImageFails,
                    onProfileImageFails = appViewModel::selectProfileImageFailure,
                    exitScenario = appViewModel.nextProfileExitScenario,
                    onExitScenario = appViewModel::selectProfileExitScenario,
                    onLocalKeyAvailable = appViewModel::setLocalKeyAvailable,
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
                    onAddArrival = { streaming -> appViewModel.addConversationArrival(profile.id, route.chatId, streaming) },
                    onAddReadingExample = { appViewModel.addMessageReadingExample(profile.id, route.chatId) },
                    onAddAttachmentExamples = { appViewModel.addAttachmentReadingExamples(profile.id, route.chatId) },
                )
            }
        }
        composable<AppRoute.NewChat> {
            uiState.activeProfile?.let { profile ->
                NewChatScreen(
                    profile = profile,
                    onBack = { navController.popBackStack() },
                    onNewGroup = { navController.navigate(AppRoute.NewGroup()) },
                    onPerson = { navController.navigate(AppRoute.PersonProfile(it)) },
                    searchScenario = appViewModel.peopleSearchScenario,
                    onResolvedPerson = { person ->
                        appViewModel.acceptDiscoveredPerson(profile.id, person)?.let { navController.navigate(AppRoute.PersonProfile(it)) }
                    },
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
                        val request = if (appViewModel.uiState.activeProfileId == profile.id) appViewModel.startDirectConversation(person.id, entry.id) else null
                        if (request != null && !appViewModel.createdChatProjectionUnavailable) {
                            appViewModel.completeCreatedChatOpen(request.id, entry.id)?.let { openConversation(it, clearsCreationFlow = true) }
                        }
                        request != null
                    },
                    onSavePrivateContact = { nickname, notes -> appViewModel.savePrivateContact(profile.id, person.id, nickname, notes) },
                    onStartGroup = { navController.navigate(AppRoute.NewGroup(person.id)) },
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
                    onGroupsInCommon = {
                        navController.navigate(AppRoute.GroupsInCommon(person.id))
                    },
                    onAddToGroup = { chatId -> appViewModel.addGroupMembers(chatId, listOf(person.id)) },
                    groupScenario = appViewModel.groupContactScenario,
                    onRetryRoster = { appViewModel.retryContactRoster(profile.id) },
                    onApplyGroups = { ids, action -> appViewModel.applyContactToGroups(profile.id, person.id, ids, action) },
                )
            }
        }
        composable<AppRoute.GroupsInCommon> { entry ->
            val route = entry.toRoute<AppRoute.GroupsInCommon>()
            val profile = uiState.activeProfile
            val person = appViewModel.person(route.personId)
            if (profile != null && person != null) {
                GroupsInCommonScreen(
                    profile = profile,
                    person = person,
                    onBack = { navController.popBackStack() },
                    onOpenGroup = { openConversation(it, clearsCreationFlow = false) },
                    onAddToGroup = { chatId -> appViewModel.addGroupMembers(chatId, listOf(person.id)) },
                    groupScenario = appViewModel.groupContactScenario,
                    onRetryRoster = { appViewModel.retryContactRoster(profile.id) },
                    onApplyGroups = { ids, action -> appViewModel.applyContactToGroups(profile.id, person.id, ids, action) },
                )
            }
        }
        composable<AppRoute.NewGroup> { entry ->
            val route = entry.toRoute<AppRoute.NewGroup>()
            uiState.activeProfile?.let { profile ->
                NewGroupScreen(
                    initialPersonId = route.initialPersonId,
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
                        val request = if (appViewModel.uiState.activeProfileId == profile.id) appViewModel.startGroupConversation(
                            name, description, avatar, route.selectedPersonIds, entry.id,
                        ) else null
                        if (request != null && !appViewModel.createdChatProjectionUnavailable) {
                            appViewModel.completeCreatedChatOpen(request.id, entry.id)?.let { openConversation(it, clearsCreationFlow = true) }
                        }
                        request != null
                    },
                    onOpenRelays = { navController.navigate(AppRoute.ProfileRelays) },
                )
            }
        }
        composable<AppRoute.Conversation> { entry ->
            val route = entry.toRoute<AppRoute.Conversation>()
            appViewModel.chat(route.chatId)?.let { chat ->
                val profile = uiState.activeProfile ?: return@let
                val searchRequest by entry.savedStateHandle.getStateFlow("conversationSearchRequest", 0L).collectAsState()
                androidx.compose.runtime.key(profile.id, chat.id) {
                androidx.compose.runtime.CompositionLocalProvider(dev.ipf.whitenoise.ui.conversation.LocalAttachmentEnvironment provides
                    dev.ipf.whitenoise.ui.conversation.AttachmentEnvironment(
                        recentAccess = appViewModel.recentMediaAccess,
                        editorSession = appViewModel.photoEditorSession?.takeIf { it.profileId == profile.id && it.chatId == chat.id },
                        openEditor = { attachmentId, imageIndex -> appViewModel.openPhotoEditor(profile.id, chat.id, attachmentId, imageIndex) },
                        editorEvent = appViewModel::photoEditorAction,
                        replacePhotos = { expected, quality, prepared -> appViewModel.replaceDraftPhotos(profile.id, chat.id, expected, quality, prepared) },
                        transfer = { messageId, attachmentId, action, revision -> appViewModel.attachmentTransferAction(profile.id, chat.id, messageId, attachmentId, action, revision) },
                    )) {
                dev.ipf.whitenoise.ui.conversation.AttachmentReaderScope(profile, chat,
                    nextScenario = { appViewModel.consumeAttachmentAccessScenario(profile.id) },
                    onPerson = { navController.navigate(AppRoute.PersonProfile(it, chat.id)) }) {
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
                    onAddDraftAttachments = { if (appViewModel.uiState.activeProfileId == profile.id) appViewModel.addDraftAttachments(chat.id, it) },
                    onRemoveDraftAttachment = { appViewModel.removeDraftAttachment(chat.id, it) },
                    onSuppressDraftLink = { appViewModel.suppressDraftLink(chat.id, it) },
                    onCancelDraftReply = { appViewModel.cancelDraftReply(chat.id) },
                    onSendDraft = { appViewModel.sendDraft(chat.id) },
                    onSendVoice = { submission -> appViewModel.sendVoice(chat.id, submission) },
                    onReply = { appViewModel.setDraftReply(chat.id, it) },
                    onReaction = { messageId, emoji, remove ->
                        appViewModel.setMessageReaction(chat.id, messageId, emoji, remove)
                    },
                    onQuickReactionsChanged = appViewModel::setQuickReactions,
                    onDeleteMessages = { ids, scope -> appViewModel.beginMessageDeletion(profile.id, chat.id, ids, scope) },
                    onForwardMessages = { ids, targets -> appViewModel.forwardMessages(chat.id, ids, targets) },
                    onForwardMedia = { key, targets, message ->
                        appViewModel.forwardMediaFrame(chat.id, key, targets, message)
                    },
                    onOpenMessageDetails = { messageId ->
                        navController.navigate(AppRoute.MessageDetails(chat.id, messageId))
                    },
                    onOpenChatInfo = { navController.navigate(AppRoute.ChatInfo(chat.id)) },
                    onOpenPersonProfile = { personId ->
                        navController.navigate(AppRoute.PersonProfile(personId, chat.id))
                    },
                    onOpenDeveloperTools = if (ConversationDebugPolicy.showsToolbarAction(profile, chat.id)) {
                        { navController.navigate(AppRoute.ConversationDebug(chat.id)) }
                    } else {
                        null
                    },
                    initialSearch = route.openSearch,
                    initialMessageId = route.targetMessageId,
                    searchRequestId = searchRequest,
                    onHistoryScenario = { appViewModel.consumeHistoryScenario(profile.id, it) },
                    onMessagesVisible = { appViewModel.markConversationVisible(profile.id, chat.id, it) },
                    onReadThroughMention = { appViewModel.markConversationThrough(profile.id, chat.id, it) },
                    onEditMessage = { id, text, revision -> appViewModel.beginMessageEdit(profile.id, chat.id, id, text, revision) },
                    onAdvanceMessageEdit = { id, request -> appViewModel.advanceMessageEdit(profile.id, chat.id, id, request) },
                    onRetryMessageEdit = { appViewModel.retryMessageEdit(profile.id, chat.id, it) },
                    onDiscardMessageEdit = { appViewModel.discardMessageEdit(profile.id, chat.id, it) },
                    onInterruptMessageEdits = { appViewModel.interruptMessageEdits(profile.id, chat.id) },
                    forwardProfiles = uiState.signedInProfiles,
                    onForwardToProfile = { ids, destination, targets -> appViewModel.beginMessageForward(profile.id, chat.id, ids, destination, targets) },
                    onForwardMediaToProfile = { key, destination, targets, text -> appViewModel.beginMessageForward(profile.id, chat.id, setOf(key.messageId), destination, targets, key, text) },
                    onRetryMessageDeletion = { appViewModel.retryMessageDeletion(profile.id, chat.id, it) },
                    onDismissMessageDeletion = { appViewModel.dismissMessageDeletion(profile.id, chat.id, it) },
                )
                }
                }
                }
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
                        val conversation = runCatching { navController.getBackStackEntry<AppRoute.Conversation>() }.getOrNull()
                            ?.takeIf { it.toRoute<AppRoute.Conversation>().chatId == chat.id }
                        if (conversation != null) {
                            val value = conversation.savedStateHandle.get<Long>("conversationSearchRequest") ?: 0L
                            conversation.savedStateHandle["conversationSearchRequest"] = value + 1
                            navController.popBackStack<AppRoute.Conversation>(inclusive = false)
                        } else navController.navigate(AppRoute.Conversation(chat.id, openSearch = true))
                    },
                    onEditGroup = { navController.navigate(AppRoute.EditGroup(chat.id)) },
                    onAddPeople = { navController.navigate(AppRoute.AddGroupMembers(chat.id)) },
                    onMute = { appViewModel.setChatMute(chat.id, it) },
                    onDisappearing = { appViewModel.setChatDisappearing(chat.id, it) },
                    onArchive = { appViewModel.setChatArchived(chat.id, !chat.isArchived) },
                    onLeave = { appViewModel.leaveChat(chat.id) },
                    onDeveloperTools = { navController.navigate(AppRoute.ConversationDebug(chat.id)) },
                    onCreateFolder = { appViewModel.createChatFolder(profile.id, it) },
                    onAddToFolder = { appViewModel.assignChatFolder(profile.id, chat.id, it) },
                    onCollapseLongMessages = { appViewModel.setCollapseLongMessages(profile.id, chat.id, it) },
                )
            }
        }
        composable<AppRoute.SharedContent> { entry ->
            val route = entry.toRoute<AppRoute.SharedContent>()
            val profile = uiState.activeProfile
            val chat = appViewModel.chat(route.chatId)
            val category = runCatching { SharedContentCategory.valueOf(route.category) }.getOrNull()
            if (profile != null && chat != null && category != null) {
                dev.ipf.whitenoise.ui.conversation.AttachmentReaderScope(profile, chat,
                    nextScenario = { appViewModel.consumeAttachmentAccessScenario(profile.id) },
                    onPerson = { navController.navigate(AppRoute.PersonProfile(it, chat.id)) }) {
                SharedContentScreen(
                    profile = profile,
                    chat = chat,
                    category = category,
                    onBack = { navController.popBackStack() },
                    forwardProfiles = uiState.signedInProfiles,
                    onForwardMediaToProfile = { key, destination, targets, text -> appViewModel.beginMessageForward(profile.id, chat.id, setOf(key.messageId), destination, targets, key, text) },
                    onForwardMedia = { key, targets, message ->
                        appViewModel.beginMessageForward(profile.id, chat.id, setOf(key.messageId), profile.id, targets, key, message)
                    },
                    onGoToMessage = { messageId ->
                        navController.navigate(
                            AppRoute.Conversation(chat.id, targetMessageId = messageId),
                        ) {
                            popUpTo<AppRoute.Conversation> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
                }
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
}

private val onboardingRouteNames = setOfNotNull(
    AppRoute.Welcome::class.qualifiedName,
    AppRoute.SignIn::class.qualifiedName,
    AppRoute.SignUp::class.qualifiedName,
)

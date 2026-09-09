package dev.ipf.whitenoise

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ipf.whitenoise.navigation.WhiteNoiseNavHost
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.settings.AppLocale
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import dev.ipf.whitenoise.model.AppFontFamily
import dev.ipf.whitenoise.model.AppFontSize
import dev.ipf.whitenoise.model.AppearanceColorPreferences
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.model.LanguagePreference
import dev.ipf.whitenoise.model.StartupPhase
import dev.ipf.whitenoise.ui.onboarding.StartupScreen
import kotlinx.coroutines.delay

@Composable
fun WhiteNoiseApp(
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = viewModel(),
) {
    val sessions: dev.ipf.whitenoise.scenarios.ScenarioSessions = viewModel()
    val run = sessions.run
    androidx.compose.runtime.CompositionLocalProvider(
        dev.ipf.whitenoise.scenarios.LocalScenarioSessions provides sessions,
    ) {
        // Keep the original NavHost and its lifecycle owner attached. Disposing AppLockScope
        // destroys that owner's back-stack lifecycles; those entries cannot be resumed on exit.
        dev.ipf.whitenoise.ui.settings.ProtectedAppContent(
            hidden = run != null,
            lockedContent = {
                if (run != null) androidx.compose.runtime.CompositionLocalProvider(
                    dev.ipf.whitenoise.scenarios.LocalScenarioRun provides run,
                ) {
                    ScenarioApp(run, sessions)
                }
            },
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                dev.ipf.whitenoise.scenarios.LocalScenarioRun provides null,
            ) {
                WhiteNoiseAppContent(navController, appViewModel, active = run == null)
            }
        }
    }
}

@Composable
private fun ScenarioApp(
    run: dev.ipf.whitenoise.scenarios.ScenarioRun,
    sessions: dev.ipf.whitenoise.scenarios.ScenarioSessions,
) {
    androidx.compose.runtime.key(run.generation) {
        val scenarioNav = rememberNavController()
        val scenarioEntry by scenarioNav.currentBackStackEntryAsState()
        // Clear scenario navigation ViewModels as well as app state when a run ends.
        val owner = androidx.compose.runtime.remember(run.generation) {
            object : androidx.lifecycle.ViewModelStoreOwner {
                override val viewModelStore = androidx.lifecycle.ViewModelStore()
            }
        }
        androidx.compose.runtime.DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
        androidx.compose.runtime.CompositionLocalProvider(androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner provides owner) {
            androidx.activity.compose.BackHandler(enabled = scenarioEntry != null && scenarioNav.previousBackStackEntry == null) { sessions.exit() }
            WhiteNoiseTheme {
                androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) { WhiteNoiseAppContent(scenarioNav, run.model) }
                    // Exit resumes the retained catalog entry, including its scroll position.
                    dev.ipf.whitenoise.scenarios.ScenarioControls(run, sessions::restart, sessions::exit)
                }
            }
        }
    }
}

@Composable
private fun WhiteNoiseAppContent(navController: NavHostController, appViewModel: AppViewModel, active: Boolean = true) {
    val scenario = dev.ipf.whitenoise.scenarios.LocalScenarioRun.current
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val startup = appViewModel.startupState
    LaunchedEffect(startup.generation, startup.phase, lifecycle) {
        if (startup.phase == StartupPhase.Loading) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(300)
            appViewModel.advanceStartup(startup.generation)
        }
    }
    val appLock = appViewModel.appLock
    val profile = appViewModel.uiState.activeProfile
    val paused = androidx.compose.runtime.remember(lifecycle) { androidx.compose.runtime.mutableStateOf(!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val settings = profile?.settings
    val hideScreenInRecents = settings?.hideScreenInRecents == true
    val blockScreenshotsInChats = settings?.blockScreenshotsInChats == true
    val chatPrivacySurface = isChatPrivacyRoute(currentBackStackEntry?.destination?.route)
    val currentActive = androidx.compose.runtime.rememberUpdatedState(active)
    SideEffect {
        // Only the visible session may control the shared Activity window.
        if (!active) return@SideEffect
        view.context.findActivity()?.window?.let { window ->
            if (WindowPrivacyPolicy.shouldSecure(
                    paused = paused.value,
                    hideScreenInRecents = hideScreenInRecents,
                    blockScreenshotsInChats = blockScreenshotsInChats,
                    chatPrivacySurface = chatPrivacySurface,
                    appLockProtects = appLock.protects(profile),
                    appLockShieldsBackground = appLock.shieldsBackground,
                    requireAuthentication = settings?.requireDeviceAuthentication == true,
                )) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
    val requireAuthentication = androidx.compose.runtime.rememberUpdatedState(profile?.settings?.requireDeviceAuthentication == true)
    val hideRecents = androidx.compose.runtime.rememberUpdatedState(hideScreenInRecents)
    androidx.compose.runtime.DisposableEffect(lifecycle,view) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                paused.value = true
                if (currentActive.value && (requireAuthentication.value || hideRecents.value)) {
                    view.context.findActivity()?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            } else if (event == Lifecycle.Event.ON_RESUME) paused.value = false
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    AppLocale(settings?.language ?: LanguagePreference.System) {
        WhiteNoiseTheme(
            appearance = settings?.appearance ?: AppearancePreference.System,
            fontSize = settings?.fontSize ?: AppFontSize.Default,
            fontFamily = settings?.fontFamily ?: AppFontFamily.System,
            colors = settings?.colors ?: AppearanceColorPreferences(),
        ) {
            dev.ipf.whitenoise.ui.settings.IncognitoKeyboardScope(profile?.settings?.incognitoKeyboard == true,blocked = !active || appLock.protects(profile) || appLock.shieldsBackground) {
                dev.ipf.whitenoise.ui.settings.AuditLogHost(appViewModel.auditLogs) {
                    dev.ipf.whitenoise.ui.settings.AppLockScope(appLock,profile,
                        onLeaveApp = { view.context.findActivity()?.moveTaskToBack(true) },
                        // Suspending the catalog for a run is not backgrounding the real account.
                        changingConfiguration = { !active || view.context.findActivity()?.isChangingConfigurations == true },
                        credentialOverride = if (scenario?.definition?.id == "app-lock") true else null) {
                        dev.ipf.whitenoise.ui.components.BackgroundKeyboardDismissalHost {
                            if (startup.phase != StartupPhase.Ready) StartupScreen(
                                phase = startup.phase,
                                hasProfiles = appViewModel.uiState.profiles.isNotEmpty(),
                                onRetry = appViewModel::retryStartup,
                                onChooseProfile = appViewModel::recoverStartupProfiles,
                            ) else WhiteNoiseNavHost(
                                navController = navController,
                                appViewModel = appViewModel,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

internal object WindowPrivacyPolicy {
    fun shouldSecure(
        paused: Boolean,
        hideScreenInRecents: Boolean,
        blockScreenshotsInChats: Boolean,
        chatPrivacySurface: Boolean,
        appLockProtects: Boolean,
        appLockShieldsBackground: Boolean,
        requireAuthentication: Boolean,
    ): Boolean =
        appLockProtects ||
            appLockShieldsBackground ||
            (paused && (hideScreenInRecents || requireAuthentication)) ||
            (blockScreenshotsInChats && chatPrivacySurface)
}

internal fun isChatPrivacyRoute(route: String?): Boolean {
    val routeName = route?.substringBefore('/')?.substringBefore('?')
    return routeName in chatPrivacyRoutes
}

private val chatPrivacyRoutes = setOf(
    dev.ipf.whitenoise.navigation.AppRoute.Conversation::class.qualifiedName,
    dev.ipf.whitenoise.navigation.AppRoute.MessageDetails::class.qualifiedName,
    dev.ipf.whitenoise.navigation.AppRoute.ChatInfo::class.qualifiedName,
    dev.ipf.whitenoise.navigation.AppRoute.GroupMembers::class.qualifiedName,
    dev.ipf.whitenoise.navigation.AppRoute.SharedContent::class.qualifiedName,
    dev.ipf.whitenoise.navigation.AppRoute.EditGroup::class.qualifiedName,
    dev.ipf.whitenoise.navigation.AppRoute.AddGroupMembers::class.qualifiedName,
    dev.ipf.whitenoise.navigation.AppRoute.ChatRelays::class.qualifiedName,
    dev.ipf.whitenoise.navigation.AppRoute.ConversationNotifications::class.qualifiedName,
    dev.ipf.whitenoise.navigation.AppRoute.ConversationDebug::class.qualifiedName,
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

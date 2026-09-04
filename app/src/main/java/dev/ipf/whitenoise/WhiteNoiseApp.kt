package dev.ipf.whitenoise

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ipf.whitenoise.navigation.WhiteNoiseNavHost
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import dev.ipf.whitenoise.model.StartupPhase
import dev.ipf.whitenoise.ui.onboarding.StartupScreen
import kotlinx.coroutines.delay

@Composable
fun WhiteNoiseApp(
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel = viewModel(),
) {
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
    SideEffect {
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
                if (requireAuthentication.value || hideRecents.value) {
                    view.context.findActivity()?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            } else if (event == Lifecycle.Event.ON_RESUME) paused.value = false
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    WhiteNoiseTheme(appearance = profile?.settings?.appearance
        ?: dev.ipf.whitenoise.model.AppearancePreference.System) {
        val focusManager = LocalFocusManager.current
        dev.ipf.whitenoise.ui.settings.IncognitoKeyboardScope(profile?.settings?.incognitoKeyboard == true,blocked = appLock.protects(profile) || appLock.shieldsBackground) {
            dev.ipf.whitenoise.ui.settings.AuditLogHost(appViewModel.auditLogs) {
                dev.ipf.whitenoise.ui.settings.AppLockScope(appLock,profile,
                    onLeaveApp = { view.context.findActivity()?.moveTaskToBack(true) },
                    changingConfiguration = { view.context.findActivity()?.isChangingConfigurations == true }) {
                    Box(Modifier.fillMaxSize().clearFocusOnBackgroundTap(focusManager)) {
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
    dev.ipf.whitenoise.navigation.AppRoute.SharedContent::class.qualifiedName,
    dev.ipf.whitenoise.navigation.AppRoute.EditGroup::class.qualifiedName,
    dev.ipf.whitenoise.navigation.AppRoute.AddGroupMembers::class.qualifiedName,
    dev.ipf.whitenoise.navigation.AppRoute.ChatRelays::class.qualifiedName,
    dev.ipf.whitenoise.navigation.AppRoute.ConversationNotifications::class.qualifiedName,
    dev.ipf.whitenoise.navigation.AppRoute.ConversationDebug::class.qualifiedName,
)

/**
 * Clears text focus only after descendants leave a complete tap unconsumed.
 *
 * Material controls and scroll gestures consume their input first. Waiting on the final pointer
 * pass keeps this app-shell behavior passive and prevents it from competing with those controls.
 */
private fun Modifier.clearFocusOnBackgroundTap(
    focusManager: FocusManager,
): Modifier = pointerInput(focusManager) {
    awaitEachGesture {
        awaitFirstDown(
            requireUnconsumed = true,
            pass = PointerEventPass.Final,
        )
        if (waitForUpOrCancellation(pass = PointerEventPass.Final) != null) {
            focusManager.clearFocus()
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

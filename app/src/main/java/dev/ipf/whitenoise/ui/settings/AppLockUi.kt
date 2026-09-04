package dev.ipf.whitenoise.ui.settings

import android.app.KeyguardManager
import android.content.Context
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Profile
import dev.ipf.whitenoise.state.*
import dev.ipf.whitenoise.ui.components.AdaptiveContent
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

internal val LocalAppLock = staticCompositionLocalOf<AppLockController?> { null }

@Composable
internal fun AppLockScope(controller: AppLockController, profile: Profile?, onLeaveApp: () -> Unit,
    changingConfiguration: () -> Boolean = { false }, credentialOverride: Boolean? = null, content: @Composable () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    val currentChanging = rememberUpdatedState(changingConfiguration)
    SideEffect { controller.sync() }
    DisposableEffect(controller,lifecycle,credentialOverride) {
        fun available() = credentialOverride ?: (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure
        fun resume() { controller.credentials(available()); controller.resume(SystemClock.elapsedRealtime()) }
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) resume()
        val observer = LifecycleEventObserver { _, event -> when (event) {
            Lifecycle.Event.ON_START -> resume()
            Lifecycle.Event.ON_STOP -> if (!currentChanging.value()) controller.background(SystemClock.elapsedRealtime())
            else -> Unit
        } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(controller.autoPrompt,controller.foreground,lifecycle) {
        if (controller.autoPrompt && controller.foreground) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { controller.requestUnlock() }
    }
    val request = controller.request
    LaunchedEffect(request?.id,lifecycle) {
        if (request != null) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { delay(450); controller.complete(request.id) }
    }
    CompositionLocalProvider(LocalAppLock provides controller) {
        ProtectedAppContent(controller.protects(profile) || controller.shieldsBackground,
            lockedContent = { AppLockScreen(controller,onLeaveApp) }, content = content)
    }
}

/** Retain navigation/state while removing protected content from placement, semantics and resumed work. */
@Composable
internal fun ProtectedAppContent(hidden: Boolean, lockedContent: @Composable () -> Unit, content: @Composable () -> Unit) {
    val parent = LocalLifecycleOwner.current.lifecycle
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val owner = remember(parent) { object : LifecycleOwner { override val lifecycle = LifecycleRegistry(this) } }
    val currentHidden = rememberUpdatedState(hidden)
    fun syncLifecycle() {
        owner.lifecycle.currentState = if (parent.currentState == Lifecycle.State.DESTROYED) Lifecycle.State.DESTROYED
            else if (currentHidden.value && parent.currentState.isAtLeast(Lifecycle.State.CREATED)) Lifecycle.State.CREATED else parent.currentState
    }
    DisposableEffect(parent,owner) {
        val observer = LifecycleEventObserver { _, _ -> syncLifecycle() }
        parent.addObserver(observer); syncLifecycle()
        onDispose { parent.removeObserver(observer); owner.lifecycle.currentState = Lifecycle.State.DESTROYED }
    }
    SideEffect { syncLifecycle(); if (hidden) { focus.clearFocus(force = true); keyboard?.hide() } }
    Layout(modifier = Modifier.fillMaxSize(),content = {
        Box(if (hidden) Modifier.clearAndSetSemantics { } else Modifier) {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) { content() }
        }
        if (hidden) Box { lockedContent() }
    }) { children,constraints ->
        val visible = (if (hidden) children.last() else children.first()).measure(constraints)
        layout(visible.width,visible.height) { visible.place(0,0) }
    }
}

@Composable
internal fun AppLockScreen(controller: AppLockController, onLeaveApp: () -> Unit = {}) {
    BackHandler { if (controller.phase == AppLockPhase.Authenticating) controller.cancel() else onLeaveApp() }
    Surface(Modifier.fillMaxSize().testTag("app.lock")) {
        AdaptiveContent(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(WhiteNoiseSpacing.CompactScreenMargin),
                verticalArrangement = Arrangement.Center,horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_lock),null,Modifier.size(48.dp))
                Spacer(Modifier.height(WhiteNoiseSpacing.Section))
                Text(stringResource(R.string.app_locked_title),style = MaterialTheme.typography.headlineSmall,textAlign = TextAlign.Center)
                Spacer(Modifier.height(WhiteNoiseSpacing.Related))
                Text(stringResource(R.string.app_locked_body),textAlign = TextAlign.Center)
                controller.failure?.let { failure ->
                    Spacer(Modifier.height(WhiteNoiseSpacing.FormField))
                    Text(stringResource(when(failure) {
                        AppUnlockOutcome.Cancelled -> R.string.app_unlock_cancelled
                        AppUnlockOutcome.Failed -> R.string.app_unlock_failed
                        AppUnlockOutcome.LockedOut -> R.string.app_unlock_locked_out
                        AppUnlockOutcome.Unavailable -> R.string.app_unlock_unavailable
                        AppUnlockOutcome.Success -> R.string.app_unlock_failed
                    }),color = MaterialTheme.colorScheme.error,textAlign = TextAlign.Center,modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                Spacer(Modifier.height(WhiteNoiseSpacing.Section))
                if (controller.phase in setOf(AppLockPhase.Evaluating,AppLockPhase.Authenticating)) {
                    CircularProgressIndicator()
                    Text(stringResource(if (controller.phase == AppLockPhase.Evaluating) R.string.app_unlock_checking else R.string.app_unlock_progress),
                        modifier = Modifier.padding(top = WhiteNoiseSpacing.Related).semantics { liveRegion = LiveRegionMode.Polite })
                    if (controller.phase == AppLockPhase.Authenticating) TextButton(onClick = controller::cancel) { Text(stringResource(R.string.cancel)) }
                } else Button(onClick = { controller.requestUnlock() },enabled = controller.foreground,modifier = Modifier.testTag("app.unlock")) {
                    Text(stringResource(R.string.app_unlock))
                }
            }
        }
    }
}

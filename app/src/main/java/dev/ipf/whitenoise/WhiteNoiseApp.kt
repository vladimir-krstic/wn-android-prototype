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
    val hideScreenInRecents = appViewModel.uiState.activeProfile?.settings?.hideScreenInRecents == true
    SideEffect {
        view.context.findActivity()?.window?.let { window ->
            if (hideScreenInRecents) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
    WhiteNoiseTheme(appearance = appViewModel.uiState.activeProfile?.settings?.appearance
        ?: dev.ipf.whitenoise.model.AppearancePreference.System) {
        val focusManager = LocalFocusManager.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnBackgroundTap(focusManager),
        ) {
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

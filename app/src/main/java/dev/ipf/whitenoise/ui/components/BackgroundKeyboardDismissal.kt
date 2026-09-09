package dev.ipf.whitenoise.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationSource
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** A superseded keyboard dismissal must never clear focus acquired by a later tap. */
internal class BackgroundKeyboardDismissal {
    var requested by mutableStateOf(false)
        private set
    private var generation = 0
    fun begin(): Int { requested = true; return ++generation }
    fun cancel() { generation++; requested = false }
    fun finish(request: Int): Boolean {
        if (!requested || request != generation) return false
        requested = false
        return true
    }
}

internal val LocalBackgroundKeyboardDismissal = staticCompositionLocalOf<BackgroundKeyboardDismissal?> { null }

/** Keep the input connection alive through IME exit instead of changing editor state mid-animation. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BackgroundKeyboardDismissalHost(
    current: WindowInsets = WindowInsets.ime,
    source: WindowInsets = WindowInsets.imeAnimationSource,
    target: WindowInsets = WindowInsets.imeAnimationTarget,
    content: @Composable () -> Unit,
) {
    val dismissal = remember { BackgroundKeyboardDismissal() }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(dismissal) { onDispose { pending?.cancel(); dismissal.cancel() } }
    fun dismissFromBackground() {
        pending?.cancel()
        dismissal.cancel()
        if (keyboard == null || (current.getBottom(density) == 0 &&
                source.getBottom(density) == 0 && target.getBottom(density) == 0)) {
            focus.clearFocus()
        } else {
            val request = dismissal.begin()
            keyboard.hide()
            pending = scope.launch {
                // Wait for onEnd as well as a zero inset: keep the input connection
                // alive while the last keyboard frame is still being drawn.
                withTimeoutOrNull(1_000) {
                    snapshotFlow {
                        current.getBottom(density) == 0 && source.getBottom(density) == 0 &&
                            target.getBottom(density) == 0
                    }.first { it }
                }
                if (dismissal.finish(request)) focus.clearFocus()
            }
        }
    }
    CompositionLocalProvider(LocalBackgroundKeyboardDismissal provides dismissal) {
        Box(Modifier.fillMaxSize().pointerInput(focus, keyboard, current, source, target) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                if (down.isConsumed) {
                    // A new control/editor interaction supersedes the pending focus clear.
                    pending?.cancel()
                    dismissal.cancel()
                } else if (waitForUpOrCancellation(pass = PointerEventPass.Final) != null) {
                    dismissFromBackground()
                }
            }
        }) { content() }
    }
}

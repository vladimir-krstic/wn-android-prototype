package dev.ipf.whitenoise.ui.conversation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

internal object GalleryDismissPolicy {
    fun shouldDismiss(distanceDp: Float, velocityDpPerSecond: Float, heightDp: Float): Boolean {
        if (heightDp <= 0f || distanceDp <= 0f || velocityDpPerSecond <= -1_250f) return false
        val distanceThreshold = min(120f, heightDp * 0.2f)
        return distanceDp >= distanceThreshold ||
            (distanceDp >= 32f && velocityDpPerSecond >= 1_250f)
    }
}

internal class GalleryDismissState(
    private val scope: CoroutineScope,
    private val density: Float,
    private val animationSpec: FiniteAnimationSpec<Float>,
    private val onDismiss: () -> Unit,
) {
    var offsetPx by mutableFloatStateOf(0f)
        private set
    var isDragging by mutableStateOf(false)
        private set
    var isSettling by mutableStateOf(false)
        private set
    var heightPx = 0f
    private var settlingJob: Job? = null
    val isInProgress: Boolean get() = isDragging || isSettling

    fun start() {
        settlingJob?.cancel()
        isSettling = false
        isDragging = true
    }

    fun dragBy(delta: Float) {
        offsetPx = (offsetPx + delta).coerceIn(0f, heightPx.coerceAtLeast(0f))
    }

    fun finish(velocity: Float) {
        settle(
            dismiss = GalleryDismissPolicy.shouldDismiss(
                offsetPx / density,
                velocity / density,
                heightPx / density,
            ),
        )
    }

    fun cancel() {
        if (isDragging) settle(dismiss = false)
    }

    fun reset() {
        settlingJob?.cancel()
        isDragging = false
        isSettling = false
        offsetPx = 0f
    }

    private fun settle(dismiss: Boolean) {
        settlingJob?.cancel()
        isDragging = false
        isSettling = true
        settlingJob = scope.launch {
            animate(offsetPx, if (dismiss) heightPx else 0f, animationSpec = animationSpec) { value, _ ->
                offsetPx = value.coerceAtLeast(0f)
            }
            isSettling = false
            if (dismiss) onDismiss()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun rememberGalleryDismissState(onDismiss: () -> Unit): GalleryDismissState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val animationSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val state = remember(scope, density, animationSpec) {
        GalleryDismissState(scope, density, animationSpec) { currentOnDismiss() }
    }
    DisposableEffect(state) { onDispose { state.reset() } }
    return state
}

/**
 * Locks only a single-finger downward drag. Child paging/scrubbing and multi-touch
 * zoom get first refusal; movement is clipped over the gallery's stationary backdrop.
 */
internal fun Modifier.gallerySwipeToDismiss(state: GalleryDismissState, enabled: Boolean): Modifier =
    clipToBounds()
        .onSizeChanged { state.heightPx = it.height.toFloat() }
        .pointerInput(state, enabled) {
            if (!enabled) return@pointerInput
            try {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (state.isSettling) {
                        down.consume()
                        return@awaitEachGesture
                    }
                    val velocity = VelocityTracker().apply {
                        addPosition(down.uptimeMillis, down.position)
                    }
                    var dragging = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || event.changes.count { it.pressed } > 1 || change.isConsumed) {
                            if (dragging) state.cancel()
                            break
                        }
                        velocity.addPosition(change.uptimeMillis, change.position)
                        if (!change.pressed) {
                            if (dragging) {
                                change.consume()
                                state.finish(velocity.calculateVelocity().y)
                            }
                            break
                        }
                        if (!dragging) {
                            val translation = change.position - down.position
                            if (translation.getDistance() < viewConfiguration.touchSlop) continue
                            if (translation.y <= 0f || abs(translation.x) >= abs(translation.y)) break
                            dragging = true
                            state.start()
                            state.dragBy((translation.y - viewConfiguration.touchSlop).coerceAtLeast(0f))
                        } else {
                            state.dragBy(change.position.y - change.previousPosition.y)
                        }
                        change.consume()
                    }
                }
            } finally {
                state.cancel()
            }
        }
        .graphicsLayer {
            translationY = state.offsetPx
            clip = true
        }

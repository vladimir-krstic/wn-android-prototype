package dev.ipf.whitenoise.ui.conversation

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationSource
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import dev.ipf.whitenoise.model.ComposerKeyboardMotion
import kotlinx.coroutines.delay

internal class ComposerKeyboardInsets(val current: WindowInsets, val source: WindowInsets, val target: WindowInsets)
internal val LocalComposerKeyboardInsets = staticCompositionLocalOf<ComposerKeyboardInsets?> { null }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun composerKeyboardInsets(): ComposerKeyboardInsets {
    LocalComposerKeyboardInsets.current?.let { return it }
    val current = WindowInsets.ime
    val source = WindowInsets.imeAnimationSource
    val target = WindowInsets.imeAnimationTarget
    return remember(current, source, target) { ComposerKeyboardInsets(current, source, target) }
}

/** Keep BoxWithConstraints at the keyboard endpoint size; move it during placement. */
internal fun Modifier.stableComposerKeyboardConstraints(
    insets: ComposerKeyboardInsets,
    navigation: WindowInsets,
): Modifier = layout { measurable, constraints ->
    val currentBottom = maxOf(insets.current.getBottom(this), navigation.getBottom(this))
    val targetBottom = maxOf(insets.target.getBottom(this), navigation.getBottom(this))
    val endpointHeight = (constraints.maxHeight + currentBottom - targetBottom).coerceAtLeast(0)
    val child = measurable.measure(constraints.copy(
        minHeight = if (constraints.hasFixedHeight) endpointHeight else 0,
        maxHeight = endpointHeight,
    ))
    layout(child.width, constraints.maxHeight) {
        child.placeRelative(0, constraints.maxHeight - child.height)
    }
}

/** Invoke only in layout/drawing so IME insets and geometry use the same frame. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun rememberComposerKeyboardProgress(editing: Boolean, keyboardFocusChange: Boolean): () -> Float {
    val motion = remember { ComposerKeyboardMotion(editing) }
    val fallback = remember { Animatable(if (editing) 1f else 0f) }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val softwareKeyboard = configuration.keyboard == Configuration.KEYBOARD_NOKEYS ||
        configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES
    val blocked = dev.ipf.whitenoise.ui.settings.LocalKeyboardInputBlocked.current
    val insets = composerKeyboardInsets()
    LaunchedEffect(editing) {
        fallback.snapTo(motion.progress)
        // Give the software IME time to start. Hardware/programmatic focus still
        // has a bounded fallback when no keyboard animation arrives.
        if (keyboardFocusChange && softwareKeyboard && !blocked) delay(160)
        fallback.animateTo(if (editing) 1f else 0f, tween(160, easing = LinearEasing))
    }
    return {
        motion.sample(editing, insets.current.getBottom(density), insets.source.getBottom(density),
            insets.target.getBottom(density), fallback.value)
    }
}

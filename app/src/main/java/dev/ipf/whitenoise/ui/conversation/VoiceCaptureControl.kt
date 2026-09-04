package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Native tap/key action plus a scoped hold/drag gesture; this node stays mounted while recording. */
@Composable
internal fun VoiceCaptureControl(enabled: Boolean, recording: Boolean, gestureKey: Any, label: String,
    onTap: () -> Unit, onHold: () -> Long?, onDrag: (Long, Float, Float) -> Unit,
    onRelease: (Long) -> Unit, onCancel: (Long) -> Unit, content: @Composable () -> Unit) {
    val liveHold = rememberUpdatedState(onHold); val liveDrag = rememberUpdatedState(onDrag)
    val liveRelease = rememberUpdatedState(onRelease); val liveCancel = rememberUpdatedState(onCancel)
    val direction = LocalLayoutDirection.current
    val timeout = LocalViewConfiguration.current.longPressTimeoutMillis
    IconButton(onClick = onTap, enabled = enabled, modifier = Modifier.size(48.dp)
        .testTag(if (recording) "conversation.voice.stop" else "conversation.voice")
        .semantics { contentDescription = label }
        .pointerInput(enabled, direction, gestureKey) {
            if (!enabled) return@pointerInput
            coroutineScope outer@ {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var eligible = true; var held = false; var request: Long? = null; var terminated = false
                    val pending = this@outer.launch {
                        delay(timeout)
                        if (eligible) { held = true; request = liveHold.value() }
                    }
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val delta = change.position - down.position
                            if (!held && (change.isConsumed || delta.getDistance() > viewConfiguration.touchSlop)) {
                                eligible = false; pending.cancel()
                            }
                            if (event.changes.count { it.pressed } > 1) break
                            if (held) {
                                change.consume()
                                request?.let { liveDrag.value(it,
                                    (if (direction == LayoutDirection.Ltr) -delta.x else delta.x) / density, -delta.y / density) }
                            }
                            if (!change.pressed) {
                                terminated = true
                                if (held) request?.let { liveRelease.value(it) }
                                break
                            }
                        }
                    } finally {
                        eligible = false; pending.cancel()
                        if (!terminated) request?.let { liveCancel.value(it) }
                    }
                }
            }
        }, content = content)
}

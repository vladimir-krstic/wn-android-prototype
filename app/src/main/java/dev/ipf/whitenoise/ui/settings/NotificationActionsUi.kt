package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.NotificationActionKind
import dev.ipf.whitenoise.state.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

internal val LocalNotificationActions = staticCompositionLocalOf<NotificationActionController?> { null }

@Composable
internal fun NotificationReadBoundary(requestId: Long?, profileId: String, chatId: String, commit: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentCommit = rememberUpdatedState(commit)
    if (requestId == null) return
    SideEffect { if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) currentCommit.value() }
    DisposableEffect(requestId,profileId,chatId,lifecycle) {
        val commitThisRoute = currentCommit.value
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) commitThisRoute() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); commitThisRoute() }
    }
}

@Composable
internal fun NotificationActionsHost(controller: NotificationActionController, activeProfileId: String?, locked: Boolean, content: @Composable () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val work = controller.work
    SideEffect { controller.reconcile() }
    LaunchedEffect(work?.id,work?.phase,work?.attempt,locked,lifecycle) {
        if (work?.running == true && !locked) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(450); controller.advance(work.id,work.phase,work.attempt)
        }
    }
    CompositionLocalProvider(LocalNotificationActions provides controller) {
        content()
        if (!locked && work?.presented == true && work.input.card.target.profileId == activeProfileId) NotificationActionStatus(controller)
    }
}

@Composable
internal fun NotificationActionStatus(controller: NotificationActionController) {
    val work = controller.work?.takeIf { it.presented } ?: return
    val completed = work.phase == NotificationActionPhase.Complete
    val title = when (work.input.kind) {
        NotificationActionKind.Reply -> if (work.accepted) R.string.notification_reply_sent else R.string.notification_reply_sending
        NotificationActionKind.React -> if (work.accepted) R.string.notification_reaction_added else R.string.notification_reaction_adding
        NotificationActionKind.MarkRead -> if (work.accepted) R.string.notification_marked_read else R.string.notification_marking_read
    }
    val failed = work.phase == NotificationActionPhase.Failed
    val body = when (work.failure) {
        NotificationActionFailure.Invalid -> R.string.notification_action_invalid
        NotificationActionFailure.Locked -> R.string.notification_action_locked
        NotificationActionFailure.TargetUnavailable -> R.string.notification_action_unavailable
        NotificationActionFailure.Operation -> R.string.notification_action_failed
        NotificationActionFailure.Exhausted -> R.string.notification_action_exhausted
        NotificationActionFailure.Cleanup -> R.string.notification_action_cleanup
        null -> if (work.phase == NotificationActionPhase.Waiting) R.string.notification_action_waiting else R.string.notification_action_processing
    }
    WhiteNoiseAlertDialog(onDismissRequest = { controller.dismiss(work.id) },
        title = { Text(stringResource(if (failed && !work.accepted) R.string.notification_action_title else title)) },
        text = if (completed) null else ({ Column(Modifier.verticalScroll(rememberScrollState()).semantics { liveRegion = LiveRegionMode.Polite },verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            if (work.running) CircularProgressIndicator()
            Text(stringResource(body))
        } }),
        confirmButton = {
            if (failed && work.failure != NotificationActionFailure.Exhausted) TextButton(onClick = { controller.retry(work.id) },modifier = Modifier.testTag("notification.action.retry")) { Text(stringResource(R.string.notification_action_retry)) }
            else TextButton(onClick = { controller.dismiss(work.id) },modifier = Modifier.testTag("notification.action.done")) { Text(stringResource(if (work.accepted) R.string.done else R.string.cancel)) }
        },
        dismissButton = if (failed && work.failure != NotificationActionFailure.Exhausted) ({ TextButton(onClick = { controller.dismiss(work.id) }) { Text(stringResource(R.string.cancel)) } }) else null)
}

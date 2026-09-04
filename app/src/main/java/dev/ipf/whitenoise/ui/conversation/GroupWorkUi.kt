package dev.ipf.whitenoise.ui.conversation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.state.GroupWorkController
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import kotlinx.coroutines.delay

internal val LocalGroupWork = staticCompositionLocalOf<GroupWorkController?> { null }

@Composable
internal fun GroupWorkHost(controller: GroupWorkController,
    lifecycleController: dev.ipf.whitenoise.state.GroupLifecycleController? = null,
    transcriptController: dev.ipf.whitenoise.state.TranscriptController? = null,
    content: @Composable () -> Unit) {
    if (lifecycleController != null) GroupLifecycleHost(lifecycleController)
    if (transcriptController != null) TranscriptHost(transcriptController)
    SideEffect { controller.reconcile() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    controller.rosterLoads.values.forEach { load -> key(load.owner) {
        LaunchedEffect(load.id, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(800); controller.advanceRoster(load.owner, load.id)
        } }
    } }
    controller.memberWork.values.filter { it.running }.forEach { work -> key(work.owner) {
        LaunchedEffect(work.id, work.phase, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(600); controller.advanceMembers(work.owner, work.id, work.phase)
        } }
    } }
    controller.editWork.values.filter { it.phase == GroupWorkPhase.Applying }.forEach { work -> key(work.owner) {
        LaunchedEffect(work.id, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(600); controller.advanceEdit(work.owner, work.id)
        } }
    } }
    controller.creation?.takeIf { it.running }?.let { work ->
        LaunchedEffect(work.id, work.phase, lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(500); controller.advanceCreate(work.id, work.phase)
        } }
    }
    CompositionLocalProvider(LocalGroupWork provides controller, LocalGroupLifecycle provides lifecycleController, LocalTranscript provides transcriptController, content = content)
}

@Composable
internal fun GroupRosterPanel(profile: Profile, chat: Chat) {
    val controller = LocalGroupWork.current ?: return
    if (!chat.isGroup) return
    val owner = remember(profile.id, chat.id) { GroupOwner(profile.id, chat.id) }
    LaunchedEffect(owner, controller.rosterScenario) { controller.openRoster(owner) }
    val status = chat.groupRoster.status
    if (status == GroupRosterStatus.Ready) return
    Column(Modifier.fillMaxWidth().padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)
        .semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
        Text(stringResource(when (status) {
            GroupRosterStatus.Unknown -> R.string.group_roster_unknown
            GroupRosterStatus.Loading -> R.string.group_roster_loading
            GroupRosterStatus.Inconsistent -> R.string.group_roster_changed
            else -> R.string.group_roster_failed
        }))
        if (status == GroupRosterStatus.Loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        else TextButton(onClick = { controller.retryRoster(owner) }) { Text(stringResource(R.string.dictation_retry)) }
    }
}

@Composable
internal fun GroupMemberWorkPanel(profile: Profile, chat: Chat) {
    val controller = LocalGroupWork.current ?: return
    val owner = GroupOwner(profile.id, chat.id)
    val work = controller.memberWork[owner] ?: return
    if (work.phase == GroupWorkPhase.Complete) return
    Column(Modifier.fillMaxWidth().padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
        verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
        work.personIds.forEach { id ->
            val name = profile.people.firstOrNull { it.id == id }?.displayName ?: stringResource(R.string.members)
            Text(name, style = MaterialTheme.typography.titleSmall)
        }
        Text(stringResource(when {
            work.phase == GroupWorkPhase.Converging -> R.string.group_members_converging
            work.phase == GroupWorkPhase.Failed && (work.failure in setOf(GroupWorkFailure.SourceChanged, GroupWorkFailure.Interrupted) || !controller.canRetryMembers(owner, work.id)) -> R.string.group_roster_changed
            work.phase == GroupWorkPhase.Failed -> R.string.group_members_failed
            work.action == GroupMemberAction.Invite -> R.string.group_invitation_pending
            else -> R.string.group_member_updating
        }), color = if (work.phase == GroupWorkPhase.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        if (work.running) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (work.phase == GroupWorkPhase.Failed) Row(horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            TextButton(onClick = { controller.retryMembers(owner, work.id) }, enabled = controller.canRetryMembers(owner, work.id)) { Text(stringResource(R.string.dictation_retry)) }
            TextButton(onClick = { controller.dismissMembers(owner, work.id) }) { Text(stringResource(R.string.batch_dismiss)) }
        }
    }
}

@Composable
internal fun GroupCreateStatus(work: GroupCreateWork, onRetry: () -> Unit, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin),
        verticalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
        Text(stringResource(when (work.phase) {
            GroupCreatePhase.Creating -> R.string.group_creating
            GroupCreatePhase.ApplyingTimer -> R.string.group_timer_applying
            GroupCreatePhase.TimerFailed -> R.string.group_timer_failed
            GroupCreatePhase.Opening -> R.string.group_opening
            GroupCreatePhase.OpenFailed -> R.string.group_open_failed
            else -> R.string.group_create_failed
        }), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (work.running) LinearProgressIndicator(Modifier.fillMaxWidth()) else Row(horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related)) {
            TextButton(onClick = onRetry) { Text(stringResource(if (work.phase == GroupCreatePhase.TimerFailed) R.string.group_retry_timer else if (work.phase == GroupCreatePhase.OpenFailed) R.string.group_retry_open else R.string.dictation_retry)) }
            if (work.phase == GroupCreatePhase.TimerFailed) TextButton(onClick = onOpen) { Text(stringResource(R.string.group_open)) }
        }
    }
}
